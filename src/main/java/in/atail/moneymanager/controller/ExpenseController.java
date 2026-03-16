package in.atail.moneymanager.controller;

import in.atail.moneymanager.dto.ExpenseDTO;
import in.atail.moneymanager.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/expenses")
public class ExpenseController {
    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ExpenseDTO> addExpense(@RequestBody @Valid ExpenseDTO expenseDTO){
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.addExpense(expenseDTO));
    }

    @GetMapping
    public  ResponseEntity<List<ExpenseDTO>> getCurrentMonthExpensesForCurrentUser(){
        return ResponseEntity.ok(expenseService.getCurrentMonthExpensesForCurrentUser());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id){
        expenseService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }


}
