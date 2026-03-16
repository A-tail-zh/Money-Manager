package in.atail.moneymanager.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseDTO {

    private Long id;

    @NotBlank(message = "费用名称不能为空")
    @Size(min = 1, max = 100, message = "费用名称长度必须在1-100之间")
    private String name;

    @Size(max = 50, message = "图标长度不能超过50")
    private String icon;

    private String categoryName;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于0")
    @Digits(integer = 10, fraction = 2, message = "金额格式不正确")
    private BigDecimal amount;

    @PastOrPresent(message = "日期不能是未来日期")
    private LocalDate date;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}