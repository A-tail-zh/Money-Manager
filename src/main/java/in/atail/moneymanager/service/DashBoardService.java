package in.atail.moneymanager.service;

import in.atail.moneymanager.dto.ExpenseDTO;
import in.atail.moneymanager.dto.IncomeDTO;
import in.atail.moneymanager.dto.RecentTransactionDTO;
import in.atail.moneymanager.entity.ProfileEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Stream.concat;

@Service
@RequiredArgsConstructor
public class DashBoardService {
    private final IncomeService incomeService;
    private final ExpenseService expenseService;
    private final ProfileService profileService;


    public Map<String,Object> getDashboardData() {
        ProfileEntity profile = profileService.getCurrentProfile();
        Map<String,Object> returnValue = new HashMap<>();
        List<IncomeDTO> latestIncomes = incomeService.getLatest5IncomesForCurrentUser();
        List<ExpenseDTO> latestExpenses = expenseService.getLatest5ExpensesForCurrentUser();
        List<RecentTransactionDTO> recentTransactions = concat(latestIncomes.stream().map(income->
                RecentTransactionDTO.builder()
                        .id(income.getId())
                        .name(income.getName())
                        .profileId(profile.getId())
                        .icon(income.getIcon())
                        .amount(income.getAmount())
                        .date(income.getDate())
                        .createdAt(income.getCreatedAt())
                        .updatedAt(income.getUpdatedAt()).
                        type("income").build()),
                latestExpenses.stream().map(expense->
                RecentTransactionDTO.builder()
                        .id(expense.getId())
                        .name(expense.getName())
                        .profileId(profile.getId())
                        .icon(expense.getIcon())
                        .amount(expense.getAmount())
                        .date(expense.getDate())
                        .createdAt(expense.getCreatedAt())
                        .updatedAt(expense.getUpdatedAt()).type("expense").build()))
                .sorted((a,b)->{
                            int cmp =  b.getDate().compareTo(a.getDate());
                            if(cmp == 0 && a.getCreatedAt() != null && b.getCreatedAt() != null){
                                cmp =  b.getCreatedAt().compareTo(a.getCreatedAt());
                            }
                            return cmp;
        }).collect(Collectors.toList());
        returnValue.put("totalBalance",
                incomeService.getTotalIncomeForCurrentUser()
                        .subtract(expenseService.getTotalExpenseForCurrentUser()));
        returnValue.put("totalIncome", incomeService.getTotalIncomeForCurrentUser());
        returnValue.put("totalExpense", expenseService.getTotalExpenseForCurrentUser());
        returnValue.put("recent5Expenses", latestExpenses);
        returnValue.put("recent5Incomes", latestIncomes);
        returnValue.put("recentTransactions", recentTransactions);
        return returnValue;
    }


}
