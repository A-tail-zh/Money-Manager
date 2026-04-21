package in.atail.moneymanager.service;

import in.atail.moneymanager.dto.ExpenseDTO;
import in.atail.moneymanager.dto.PageDTO;
import in.atail.moneymanager.entity.CategoryEntity;
import in.atail.moneymanager.entity.ExpenseEntity;
import in.atail.moneymanager.entity.ProfileEntity;
import in.atail.moneymanager.exception.ResourceNotFoundException;
import in.atail.moneymanager.repository.CategoryRepository;
import in.atail.moneymanager.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final ProfileService profileService;

    public List<ExpenseDTO> getExpensesFoeUserOnDate(Long profileId, LocalDate date) {
        return expenseRepository.findByProfileIdAndDate(profileId, date).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<ExpenseDTO> filterExpense(LocalDate startDate, LocalDate endDate, String keyword, Sort sort) {
        return expenseRepository.findByProfileIdAndDateBetweenAndNameContainingIgnoreCase(
                        profileService.getCurrentProfileId(),
                        startDate,
                        endDate,
                        keyword,
                        sort
                ).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public BigDecimal getTotalExpenseForCurrentUser() {
        Long profileId = profileService.getCurrentProfileId();
        BigDecimal total = expenseRepository.findTotalExpenseByProfileId(profileId);
        log.debug("User {} total expense {}", profileId, total);
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal getCurrentMonthExpenseForCurrentUser() {
        Long profileId = profileService.getCurrentProfileId();
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        BigDecimal total = expenseRepository.findTotalExpenseByProfileIdAndDateBetween(profileId, startOfMonth, endOfMonth);
        log.debug("User {} current month expense {}", profileId, total);
        return total != null ? total : BigDecimal.ZERO;
    }

    public List<ExpenseDTO> getLatest5ExpensesForCurrentUser() {
        Long profileId = profileService.getCurrentProfileId();
        List<ExpenseDTO> expenses = expenseRepository.findTop5ByProfileIdOrderByDateDesc(profileId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        log.debug("Loaded {} latest expenses for user {}", expenses.size(), profileId);
        return expenses;
    }

    @Transactional
    public void deleteExpense(Long id) {
        Long profileId = profileService.getCurrentProfileId();
        ExpenseEntity expenseEntity = expenseRepository.findByIdAndProfileId(id, profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense record does not exist"));
        expenseRepository.delete(expenseEntity);
        log.info("User {} deleted expense {}", profileId, id);
    }

    public PageDTO<ExpenseDTO> getCurrentMonthExpensesForCurrentUser(int page, int size, String sortBy, String sortDir) {
        Long profileId = profileService.getCurrentProfileId();
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        Sort sort = Sort.by("asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ExpenseDTO> expensePage = expenseRepository
                .findByProfileIdAndDateBetween(profileId, startOfMonth, endOfMonth, pageable)
                .map(this::toDTO);

        return PageDTO.from(expensePage);
    }

    @Transactional
    public ExpenseDTO addExpense(ExpenseDTO expenseDTO) {
        ProfileEntity profile = profileService.getCurrentProfile();
        CategoryEntity category = getExpenseCategory(profile.getId(), expenseDTO.getCategoryId());

        ExpenseEntity saved = expenseRepository.save(toEntity(expenseDTO, profile, category));
        log.info("User {} added expense {}", profile.getId(), saved.getId());
        return toDTO(saved);
    }

    @Transactional
    public ExpenseDTO updateExpense(Long id, ExpenseDTO expenseDTO) {
        Long profileId = profileService.getCurrentProfileId();
        ExpenseEntity expenseEntity = expenseRepository.findByIdAndProfileId(id, profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense record does not exist"));
        CategoryEntity category = getExpenseCategory(profileId, expenseDTO.getCategoryId());

        expenseEntity.setName(expenseDTO.getName());
        expenseEntity.setIcon(expenseDTO.getIcon());
        expenseEntity.setDate(expenseDTO.getDate());
        expenseEntity.setAmount(expenseDTO.getAmount());
        expenseEntity.setCategory(category);

        ExpenseEntity updatedExpense = expenseRepository.save(expenseEntity);
        log.info("User {} updated expense {}", profileId, updatedExpense.getId());
        return toDTO(updatedExpense);
    }

    private ExpenseEntity toEntity(ExpenseDTO expenseDTO, ProfileEntity profile, CategoryEntity category) {
        return ExpenseEntity.builder()
                .id(expenseDTO.getId())
                .name(expenseDTO.getName())
                .icon(expenseDTO.getIcon())
                .date(expenseDTO.getDate())
                .amount(expenseDTO.getAmount())
                .profile(profile)
                .category(category)
                .build();
    }

    private ExpenseDTO toDTO(ExpenseEntity entity) {
        return ExpenseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .icon(entity.getIcon())
                .date(entity.getDate())
                .amount(entity.getAmount())
                .categoryName(entity.getCategory() != null ? entity.getCategory().getName() : null)
                .categoryId(entity.getCategory() != null ? entity.getCategory().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private CategoryEntity getExpenseCategory(Long profileId, Long categoryId) {
        CategoryEntity category = categoryRepository.findByIdAndProfileId(categoryId, profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Category does not exist"));
        if (!"expense".equalsIgnoreCase(category.getType())) {
            throw new IllegalStateException("Expense records must use an expense category");
        }
        return category;
    }
}
