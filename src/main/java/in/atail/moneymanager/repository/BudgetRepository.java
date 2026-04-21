package in.atail.moneymanager.repository;

import in.atail.moneymanager.entity.BudgetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<BudgetEntity, Long> {
    List<BudgetEntity> findByProfileIdAndBudgetMonthOrderByCreatedAtAsc(Long profileId, LocalDate budgetMonth);

    Optional<BudgetEntity> findByIdAndProfileId(Long id, Long profileId);

    Optional<BudgetEntity> findByProfileIdAndCategoryIdAndBudgetMonth(Long profileId, Long categoryId, LocalDate budgetMonth);
}
