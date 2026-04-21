package in.atail.moneymanager.service;

import in.atail.moneymanager.dto.BudgetDTO;
import in.atail.moneymanager.dto.BudgetSummaryDTO;
import in.atail.moneymanager.entity.BudgetEntity;
import in.atail.moneymanager.entity.CategoryEntity;
import in.atail.moneymanager.entity.ProfileEntity;
import in.atail.moneymanager.exception.ResourceNotFoundException;
import in.atail.moneymanager.repository.BudgetRepository;
import in.atail.moneymanager.repository.CategoryRepository;
import in.atail.moneymanager.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BudgetService {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final ProfileService profileService;

    public List<BudgetDTO> getBudgetsForMonth(String month) {
        Long profileId = profileService.getCurrentProfileId();
        LocalDate budgetMonth = parseMonth(month);
        return budgetRepository.findByProfileIdAndBudgetMonthOrderByCreatedAtAsc(profileId, budgetMonth)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public BudgetSummaryDTO getBudgetSummary(String month) {
        LocalDate budgetMonth = parseMonth(month);
        List<BudgetDTO> budgets = getBudgetsForMonth(formatMonth(budgetMonth));
        BigDecimal totalBudget = budgets.stream()
                .map(BudgetDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSpent = budgets.stream()
                .map(BudgetDTO::getSpent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int exceededCount = (int) budgets.stream().filter(BudgetDTO::getExceeded).count();
        int nearLimitCount = (int) budgets.stream().filter(BudgetDTO::getNearLimit).count();

        return BudgetSummaryDTO.builder()
                .budgetMonth(formatMonth(budgetMonth))
                .totalBudget(totalBudget)
                .totalSpent(totalSpent)
                .remainingBudget(totalBudget.subtract(totalSpent))
                .usagePercentage(calculatePercentage(totalSpent, totalBudget))
                .budgetsCount(budgets.size())
                .exceededBudgetsCount(exceededCount)
                .nearLimitBudgetsCount(nearLimitCount)
                .build();
    }

    @Transactional
    public BudgetDTO createBudget(BudgetDTO budgetDTO) {
        ProfileEntity profile = profileService.getCurrentProfile();
        LocalDate budgetMonth = parseMonth(budgetDTO.getBudgetMonth());
        CategoryEntity category = getExpenseCategory(profile.getId(), budgetDTO.getCategoryId());

        budgetRepository.findByProfileIdAndCategoryIdAndBudgetMonth(profile.getId(), category.getId(), budgetMonth)
                .ifPresent(existingBudget -> {
                    throw new IllegalStateException("A budget for this category and month already exists");
                });

        BudgetEntity budget = BudgetEntity.builder()
                .amount(budgetDTO.getAmount())
                .budgetMonth(budgetMonth)
                .alertThreshold(resolveAlertThreshold(budgetDTO.getAlertThreshold()))
                .category(category)
                .profile(profile)
                .build();

        return toDTO(budgetRepository.save(budget));
    }

    @Transactional
    public BudgetDTO updateBudget(Long id, BudgetDTO budgetDTO) {
        Long profileId = profileService.getCurrentProfileId();
        BudgetEntity budget = budgetRepository.findByIdAndProfileId(id, profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget does not exist"));
        LocalDate budgetMonth = parseMonth(budgetDTO.getBudgetMonth());
        CategoryEntity category = getExpenseCategory(profileId, budgetDTO.getCategoryId());

        budgetRepository.findByProfileIdAndCategoryIdAndBudgetMonth(profileId, category.getId(), budgetMonth)
                .filter(existingBudget -> !existingBudget.getId().equals(id))
                .ifPresent(existingBudget -> {
                    throw new IllegalStateException("A budget for this category and month already exists");
                });

        budget.setAmount(budgetDTO.getAmount());
        budget.setBudgetMonth(budgetMonth);
        budget.setAlertThreshold(resolveAlertThreshold(budgetDTO.getAlertThreshold()));
        budget.setCategory(category);

        return toDTO(budgetRepository.save(budget));
    }

    @Transactional
    public void deleteBudget(Long id) {
        Long profileId = profileService.getCurrentProfileId();
        BudgetEntity budget = budgetRepository.findByIdAndProfileId(id, profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget does not exist"));
        budgetRepository.delete(budget);
    }

    private BudgetDTO toDTO(BudgetEntity budgetEntity) {
        LocalDate startDate = budgetEntity.getBudgetMonth().withDayOfMonth(1);
        LocalDate endDate = budgetEntity.getBudgetMonth().withDayOfMonth(budgetEntity.getBudgetMonth().lengthOfMonth());
        BigDecimal spent = expenseRepository.findTotalExpenseByProfileIdAndCategoryIdAndDateBetween(
                budgetEntity.getProfile().getId(),
                budgetEntity.getCategory().getId(),
                startDate,
                endDate
        );
        BigDecimal safeSpent = spent != null ? spent : BigDecimal.ZERO;
        BigDecimal remaining = budgetEntity.getAmount().subtract(safeSpent);
        BigDecimal usagePercentage = calculatePercentage(safeSpent, budgetEntity.getAmount());
        boolean exceeded = safeSpent.compareTo(budgetEntity.getAmount()) > 0;
        boolean nearLimit = usagePercentage.compareTo(BigDecimal.valueOf(budgetEntity.getAlertThreshold())) >= 0 && !exceeded;

        return BudgetDTO.builder()
                .id(budgetEntity.getId())
                .categoryId(budgetEntity.getCategory().getId())
                .categoryName(budgetEntity.getCategory().getName())
                .amount(budgetEntity.getAmount())
                .budgetMonth(formatMonth(budgetEntity.getBudgetMonth()))
                .alertThreshold(budgetEntity.getAlertThreshold())
                .spent(safeSpent)
                .remaining(remaining)
                .usagePercentage(usagePercentage)
                .exceeded(exceeded)
                .nearLimit(nearLimit)
                .createdAt(budgetEntity.getCreatedAt())
                .updatedAt(budgetEntity.getUpdatedAt())
                .build();
    }

    private CategoryEntity getExpenseCategory(Long profileId, Long categoryId) {
        CategoryEntity category = categoryRepository.findByIdAndProfileId(categoryId, profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Category does not exist"));
        if (!"expense".equalsIgnoreCase(category.getType())) {
            throw new IllegalStateException("Budget must be linked to an expense category");
        }
        return category;
    }

    private LocalDate parseMonth(String month) {
        try {
            YearMonth yearMonth = month == null || month.isBlank()
                    ? YearMonth.now()
                    : YearMonth.parse(month, YEAR_MONTH_FORMATTER);
            return yearMonth.atDay(1);
        } catch (DateTimeParseException ex) {
            throw new IllegalStateException("Month format must be yyyy-MM");
        }
    }

    private String formatMonth(LocalDate budgetMonth) {
        return YearMonth.from(budgetMonth).format(YEAR_MONTH_FORMATTER);
    }

    private Integer resolveAlertThreshold(Integer alertThreshold) {
        return alertThreshold == null ? 80 : alertThreshold;
    }

    private BigDecimal calculatePercentage(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
