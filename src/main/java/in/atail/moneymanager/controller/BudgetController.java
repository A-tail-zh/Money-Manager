package in.atail.moneymanager.controller;

import in.atail.moneymanager.dto.BudgetDTO;
import in.atail.moneymanager.dto.BudgetSummaryDTO;
import in.atail.moneymanager.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<List<BudgetDTO>> getBudgets(
            @RequestParam(required = false) String month) {
        return ResponseEntity.ok(budgetService.getBudgetsForMonth(month));
    }

    @GetMapping("/summary")
    public ResponseEntity<BudgetSummaryDTO> getBudgetSummary(
            @RequestParam(required = false) String month) {
        return ResponseEntity.ok(budgetService.getBudgetSummary(month));
    }

    @PostMapping
    public ResponseEntity<BudgetDTO> createBudget(@RequestBody @Valid BudgetDTO budgetDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.createBudget(budgetDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetDTO> updateBudget(
            @PathVariable Long id,
            @RequestBody @Valid BudgetDTO budgetDTO) {
        return ResponseEntity.ok(budgetService.updateBudget(id, budgetDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        budgetService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }
}
