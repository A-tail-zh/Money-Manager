package in.atail.moneymanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpendingBehaviorDTO {
    private String month;
    private BigDecimal totalExpense;
    private Integer transactionCount;
    private Integer activeDays;
    private BigDecimal averageDailyExpense;
    private BigDecimal averageTransactionAmount;
    private String topCategory;
    private BigDecimal topCategoryAmount;
    private String largestExpenseName;
    private BigDecimal largestExpenseAmount;
    private BigDecimal weekendExpenseRatio;
    private Map<String, BigDecimal> weekdayTotals;
}
