package in.atail.moneymanager.controller;

import in.atail.moneymanager.dto.IncomeDTO;
import in.atail.moneymanager.service.IncomeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/incomes")
public class IncomeController {

    private final IncomeService incomeService;

    @PostMapping
    public ResponseEntity<IncomeDTO> addIncome(@RequestBody @Valid IncomeDTO incomeDTO){
        return ResponseEntity.status(HttpStatus.CREATED).body(incomeService.addIncome(incomeDTO));
    }

    @GetMapping
    public ResponseEntity<List<IncomeDTO>> getCurrentMonthIncomesForCurrentUser(){
        return ResponseEntity.ok(incomeService.getCurrentMonthIncomesForCurrentUser());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncome(@PathVariable Long id){
        incomeService.deleteIncome(id);
        return ResponseEntity.noContent().build();
    }
}
