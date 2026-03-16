package in.atail.moneymanager.controller;

import in.atail.moneymanager.dto.FilterDTO;
import in.atail.moneymanager.service.ExpenseService;
import in.atail.moneymanager.service.IncomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/filter")
@RequiredArgsConstructor
public class FilterController {
    private final ExpenseService expenseService;
    private final IncomeService incomeService;


    @PostMapping
    public ResponseEntity<?> filterTransaction(@RequestBody FilterDTO filterDTO) {
        LocalDate startDate = filterDTO.getStartDate() != null ? filterDTO.getStartDate() : LocalDate.MIN;
        LocalDate endDate = filterDTO.getEndDate() != null ? filterDTO.getEndDate() : LocalDate.now();
        String keyword = filterDTO.getKeyword() != null ? filterDTO.getKeyword() : "";
        String sortField = filterDTO.getSortField() != null ? filterDTO.getSortField() : "date";
        Sort.Direction direction = "desc".equalsIgnoreCase(filterDTO.getSortOrder())?Sort.Direction.DESC:Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortField);
        if("income".equals(filterDTO.getType())){
            return ResponseEntity.ok(incomeService.filterIncome(startDate, endDate, keyword, sort));
        }else if("expense".equals(filterDTO.getType())){
            return ResponseEntity.ok(expenseService.filterExpense(startDate, endDate, keyword, sort));
        }else {
            return ResponseEntity.badRequest().body("无效信息。必须是收入或者支出");
        }
    }
}
