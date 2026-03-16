package in.atail.moneymanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FilterDTO {
    private String type;
    private String keyword;
    private LocalDate startDate;
    private LocalDate endDate;
    private String sortField;// date, name, amount
    private String sortOrder;// asc, desc
}
