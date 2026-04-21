package in.atail.moneymanager.repository;

import in.atail.moneymanager.entity.ExpenseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<ExpenseEntity, Long> {

    //select * from expense where profile_id = ? order by date desc
    List<ExpenseEntity> findByProfileIdOrderByDateDesc(Long profileId);

    Optional<ExpenseEntity> findByIdAndProfileId(Long id, Long profileId);

    //select * from expense where profile_id = ? order by date desc limit 5
    List<ExpenseEntity> findTop5ByProfileIdOrderByDateDesc(Long profileId);

    @Query("select sum(e.amount) from ExpenseEntity e where e.profile.id = :profileId")
    BigDecimal findTotalExpenseByProfileId(@Param("profileId") Long profileId);

    @Query("""
            select sum(e.amount)
            from ExpenseEntity e
            where e.profile.id = :profileId
              and e.date between :startDate and :endDate
            """)
    BigDecimal findTotalExpenseByProfileIdAndDateBetween(
            @Param("profileId") Long profileId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            select sum(e.amount)
            from ExpenseEntity e
            where e.profile.id = :profileId
              and e.category.id = :categoryId
              and e.date between :startDate and :endDate
            """)
    BigDecimal findTotalExpenseByProfileIdAndCategoryIdAndDateBetween(
            @Param("profileId") Long profileId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    //select * from expense where profile_id = ? and date between ? and ? and name like ?
    List<ExpenseEntity> findByProfileIdAndDateBetweenAndNameContainingIgnoreCase(
            Long profileId, LocalDate startDate, LocalDate endDate, String keyword, Sort  sort);

    //select * from expense where profile_id = ? and date between ? and ?
    Page<ExpenseEntity> findByProfileIdAndDateBetween(Long profileId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    //select * from expense where profile_id = ? and date = ?
    List<ExpenseEntity> findByProfileIdAndDate(Long profileId, LocalDate date);

    List<ExpenseEntity> findByProfileIdAndDateBetweenOrderByDateAscCreatedAtAsc(
            Long profileId, LocalDate startDate, LocalDate endDate);

    boolean existsByCategoryIdAndProfileId(Long categoryId, Long profileId);

}
