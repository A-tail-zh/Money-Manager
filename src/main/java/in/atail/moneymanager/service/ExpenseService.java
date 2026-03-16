package in.atail.moneymanager.service;

import in.atail.moneymanager.dto.ExpenseDTO;
import in.atail.moneymanager.entity.CategoryEntity;
import in.atail.moneymanager.entity.ExpenseEntity;
import in.atail.moneymanager.entity.ProfileEntity;
import in.atail.moneymanager.exception.ResourceNotFoundException;
import in.atail.moneymanager.exception.UnauthorizedException;
import in.atail.moneymanager.repository.CategoryRepository;
import in.atail.moneymanager.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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


    //Notifications
    public  List<ExpenseDTO> getExpensesFoeUserOnDate(Long profileId, LocalDate date){
        return expenseRepository.findByProfileIdAndDate(profileId, date).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    //filter expense
    public List<ExpenseDTO> filterExpense(LocalDate startDate, LocalDate endDate, String keyword, Sort  sort){
        return expenseRepository.findByProfileIdAndDateBetweenAndNameContainingIgnoreCase(
                profileService.getCurrentProfile().getId(),
                startDate,
                endDate,
                keyword,
                sort
        ).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }


    /**
     * 获取当前用户的总支出
     *
     * @return 总支出金额,如果没有记录则返回 0
     */
    public BigDecimal getTotalExpenseForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        BigDecimal total = expenseRepository.findTotalExpenseByProfileId(profile.getId());

        log.debug("用户 {} 的总支出: {}", profile.getId(), total);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * 获取当前用户最近 5 条支出记录
     *
     * @return 最近 5 条支出的 DTO 列表
     */
    public List<ExpenseDTO> getLatest5ExpensesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();

        List<ExpenseDTO> expenses = expenseRepository
                .findTop5ByProfileIdOrderByDateDesc(profile.getId())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        log.debug("获取用户 {} 最近 5 条支出,数量: {}", profile.getId(), expenses.size());
        return expenses;
    }

    /**
     * 删除指定的支出记录
     *
     * @param id 支出记录 ID
     * @throws ResourceNotFoundException 如果支出记录不存在
     * @throws UnauthorizedException 如果当前用户无权删除该记录
     */
    @Transactional
    public void deleteExpense(Long id) {
        log.info("尝试删除支出记录: {}", id);

        ProfileEntity profile = profileService.getCurrentProfile();
        ExpenseEntity expenseEntity = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("支出记录不存在"));

        // 权限检查
        if (!expenseEntity.getProfile().getId().equals(profile.getId())) {
            log.warn("用户 {} 尝试删除不属于自己的支出记录 {}",
                    profile.getId(), id);
            throw new UnauthorizedException("无权删除此支出记录");
        }

        expenseRepository.delete(expenseEntity);
        log.info("成功删除支出记录: {}", id);
    }

    /**
     * 获取当前用户本月的所有支出记录
     *
     * @return 本月支出的 DTO 列表
     */
    public List<ExpenseDTO> getCurrentMonthExpensesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();

        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = LocalDate.now().withDayOfMonth(
                LocalDate.now().lengthOfMonth()
        );

        List<ExpenseDTO> expenses = expenseRepository
                .findByProfileIdAndDateBetween(profile.getId(), startOfMonth, endOfMonth)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        log.debug("获取用户 {} 本月支出,数量: {}", profile.getId(), expenses.size());
        return expenses;
    }

    /**
     * 添加新的支出记录
     *
     * @param expenseDTO 支出信息 DTO
     * @return 保存后的支出 DTO
     * @throws ResourceNotFoundException 如果分类不存在
     */
    @Transactional
    public ExpenseDTO addExpense(ExpenseDTO expenseDTO) {
        log.info("添加新支出: {}, 金额: {}",
                expenseDTO.getName(), expenseDTO.getAmount());

        ProfileEntity profile = profileService.getCurrentProfile();
        CategoryEntity category = categoryRepository.findById(expenseDTO.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("分类不存在"));

        ExpenseEntity saved = expenseRepository.save(
                toEntity(expenseDTO, profile, category)
        );

        log.info("成功添加支出,ID: {}", saved.getId());
        return toDTO(saved);
    }

    /**
     * 将 ExpenseDTO 转换为 ExpenseEntity
     */
    private ExpenseEntity toEntity(ExpenseDTO expenseDTO,
                                   ProfileEntity profile,
                                   CategoryEntity category) {
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

    /**
     * 将 ExpenseEntity 转换为 ExpenseDTO
     */
    private ExpenseDTO toDTO(ExpenseEntity entity) {
        return ExpenseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .icon(entity.getIcon())
                .date(entity.getDate())
                .amount(entity.getAmount())
                .categoryName(entity.getCategory() != null ?
                        entity.getCategory().getName() : null)
                .categoryId(entity.getCategory() != null ?
                        entity.getCategory().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}