package in.atail.moneymanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetSummaryDTO {
    private String budgetMonth;
    private BigDecimal totalBudget;
    private BigDecimal totalSpent;
    private BigDecimal remainingBudget;
    private BigDecimal usagePercentage;
    private Integer budgetsCount;
    private Integer exceededBudgetsCount;
    private Integer nearLimitBudgetsCount;
}
