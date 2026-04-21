package in.atail.moneymanager.service;

import in.atail.moneymanager.dto.CategoryAnalyticsDTO;
import in.atail.moneymanager.dto.MonthlyAnalyticsDTO;
import in.atail.moneymanager.dto.SpendingBehaviorDTO;
import in.atail.moneymanager.dto.SpendingTrendDTO;
import in.atail.moneymanager.entity.ExpenseEntity;
import in.atail.moneymanager.entity.IncomeEntity;
import in.atail.moneymanager.repository.ExpenseRepository;
import in.atail.moneymanager.repository.IncomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final DateTimeFormatter MONTH_LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM");

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final ProfileService profileService;

    public MonthlyAnalyticsDTO getMonthlyAnalytics() {
        Long profileId = profileService.getCurrentProfileId();
        List<IncomeEntity> incomes = incomeRepository.findByProfileIdOrderByDateDesc(profileId);
        List<ExpenseEntity> expenses = expenseRepository.findByProfileIdOrderByDateDesc(profileId);

        Map<YearMonth, BigDecimal> incomeMap = new LinkedHashMap<>();
        Map<YearMonth, BigDecimal> expenseMap = new LinkedHashMap<>();

        incomes.forEach(income -> {
            YearMonth key = YearMonth.from(income.getDate());
            incomeMap.merge(key, income.getAmount(), BigDecimal::add);
        });

        expenses.forEach(expense -> {
            YearMonth key = YearMonth.from(expense.getDate());
            expenseMap.merge(key, expense.getAmount(), BigDecimal::add);
        });

        List<YearMonth> months = new ArrayList<>();
        months.addAll(incomeMap.keySet());
        expenseMap.keySet().stream()
                .filter(month -> !months.contains(month))
                .forEach(months::add);
        months.sort(Comparator.naturalOrder());

        List<String> labels = new ArrayList<>();
        List<BigDecimal> incomeValues = new ArrayList<>();
        List<BigDecimal> expenseValues = new ArrayList<>();

        months.forEach(month -> {
            labels.add(month.format(MONTH_LABEL_FORMATTER));
            incomeValues.add(incomeMap.getOrDefault(month, BigDecimal.ZERO));
            expenseValues.add(expenseMap.getOrDefault(month, BigDecimal.ZERO));
        });

        return MonthlyAnalyticsDTO.builder()
                .months(labels)
                .income(incomeValues)
                .expense(expenseValues)
                .build();
    }

    public List<CategoryAnalyticsDTO> getCategoryAnalytics() {
        Long profileId = profileService.getCurrentProfileId();
        List<ExpenseEntity> expenses = expenseRepository.findByProfileIdOrderByDateDesc(profileId);

        Map<String, BigDecimal> categoryTotals = new LinkedHashMap<>();
        expenses.forEach(expense -> {
            String categoryName = expense.getCategory() != null ? expense.getCategory().getName() : "Unknown";
            categoryTotals.merge(categoryName, expense.getAmount(), BigDecimal::add);
        });

        return categoryTotals.entrySet().stream()
                .map(entry -> CategoryAnalyticsDTO.builder()
                        .category(entry.getKey())
                        .amount(entry.getValue())
                        .build())
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .toList();
    }

    public SpendingTrendDTO getSpendingTrend(String month) {
        YearMonth yearMonth = parseMonth(month);
        List<ExpenseEntity> expenses = getExpensesForMonth(yearMonth);
        Map<LocalDate, BigDecimal> dailyTotals = new LinkedHashMap<>();

        yearMonth.atDay(1).datesUntil(yearMonth.atEndOfMonth().plusDays(1))
                .forEach(date -> dailyTotals.put(date, BigDecimal.ZERO));
        expenses.forEach(expense -> dailyTotals.merge(expense.getDate(), expense.getAmount(), BigDecimal::add));

        return SpendingTrendDTO.builder()
                .month(yearMonth.format(YEAR_MONTH_FORMATTER))
                .labels(dailyTotals.keySet().stream().map(date -> String.valueOf(date.getDayOfMonth())).toList())
                .amounts(new ArrayList<>(dailyTotals.values()))
                .build();
    }

    public SpendingBehaviorDTO getSpendingBehavior(String month) {
        YearMonth yearMonth = parseMonth(month);
        List<ExpenseEntity> expenses = getExpensesForMonth(yearMonth);

        BigDecimal totalExpense = expenses.stream()
                .map(ExpenseEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int transactionCount = expenses.size();
        int activeDays = (int) expenses.stream()
                .map(ExpenseEntity::getDate)
                .distinct()
                .count();

        Map<String, BigDecimal> categoryTotals = new LinkedHashMap<>();
        Map<String, BigDecimal> weekdayTotals = initWeekdayTotals();
        Set<DayOfWeek> weekendDays = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        BigDecimal weekendTotal = BigDecimal.ZERO;
        ExpenseEntity largestExpense = null;

        for (ExpenseEntity expense : expenses) {
            String categoryName = expense.getCategory() != null ? expense.getCategory().getName() : "Unknown";
            categoryTotals.merge(categoryName, expense.getAmount(), BigDecimal::add);

            String weekdayKey = expense.getDate().getDayOfWeek().name();
            weekdayTotals.merge(weekdayKey, expense.getAmount(), BigDecimal::add);

            if (weekendDays.contains(expense.getDate().getDayOfWeek())) {
                weekendTotal = weekendTotal.add(expense.getAmount());
            }
            if (largestExpense == null || expense.getAmount().compareTo(largestExpense.getAmount()) > 0) {
                largestExpense = expense;
            }
        }

        Map.Entry<String, BigDecimal> topCategory = categoryTotals.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(Map.entry("N/A", BigDecimal.ZERO));

        return SpendingBehaviorDTO.builder()
                .month(yearMonth.format(YEAR_MONTH_FORMATTER))
                .totalExpense(totalExpense)
                .transactionCount(transactionCount)
                .activeDays(activeDays)
                .averageDailyExpense(divide(totalExpense, BigDecimal.valueOf(yearMonth.lengthOfMonth())))
                .averageTransactionAmount(divide(totalExpense, BigDecimal.valueOf(Math.max(transactionCount, 1))))
                .topCategory(topCategory.getKey())
                .topCategoryAmount(topCategory.getValue())
                .largestExpenseName(largestExpense != null ? largestExpense.getName() : null)
                .largestExpenseAmount(largestExpense != null ? largestExpense.getAmount() : BigDecimal.ZERO)
                .weekendExpenseRatio(divide(weekendTotal.multiply(BigDecimal.valueOf(100)), totalExpense))
                .weekdayTotals(weekdayTotals)
                .build();
    }

    private List<ExpenseEntity> getExpensesForMonth(YearMonth yearMonth) {
        Long profileId = profileService.getCurrentProfileId();
        return expenseRepository.findByProfileIdAndDateBetweenOrderByDateAscCreatedAtAsc(
                profileId,
                yearMonth.atDay(1),
                yearMonth.atEndOfMonth()
        );
    }

    private YearMonth parseMonth(String month) {
        try {
            return month == null || month.isBlank()
                    ? YearMonth.now()
                    : YearMonth.parse(month, YEAR_MONTH_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalStateException("Month format must be yyyy-MM");
        }
    }

    private Map<String, BigDecimal> initWeekdayTotals() {
        Map<String, BigDecimal> weekdayTotals = new LinkedHashMap<>();
        weekdayTotals.put(DayOfWeek.MONDAY.name(), BigDecimal.ZERO);
        weekdayTotals.put(DayOfWeek.TUESDAY.name(), BigDecimal.ZERO);
        weekdayTotals.put(DayOfWeek.WEDNESDAY.name(), BigDecimal.ZERO);
        weekdayTotals.put(DayOfWeek.THURSDAY.name(), BigDecimal.ZERO);
        weekdayTotals.put(DayOfWeek.FRIDAY.name(), BigDecimal.ZERO);
        weekdayTotals.put(DayOfWeek.SATURDAY.name(), BigDecimal.ZERO);
        weekdayTotals.put(DayOfWeek.SUNDAY.name(), BigDecimal.ZERO);
        return weekdayTotals;
    }

    private BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
