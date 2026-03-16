package in.atail.moneymanager.service;

import in.atail.moneymanager.dto.IncomeDTO;
import in.atail.moneymanager.entity.CategoryEntity;
import in.atail.moneymanager.entity.IncomeEntity;
import in.atail.moneymanager.entity.ProfileEntity;
import in.atail.moneymanager.exception.ResourceNotFoundException;
import in.atail.moneymanager.exception.UnauthorizedException;
import in.atail.moneymanager.repository.CategoryRepository;
import in.atail.moneymanager.repository.IncomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import lombok.extern.slf4j.Slf4j;
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
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private final CategoryRepository categoryRepository;
    private final ProfileService profileService;

    //filter incomes
    public List<IncomeDTO> filterIncome(LocalDate startDate, LocalDate endDate, String keyword, Sort sort){
        return incomeRepository.findByProfileIdAndDateBetweenAndNameContainingIgnoreCase(
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
     * 获取当前用户的总收入
     *
     * @return 总收入金额,如果没有记录则返回 0
     */
    public BigDecimal getTotalIncomeForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        BigDecimal total = incomeRepository.findTotalIncomeByProfileId(profile.getId());

        log.debug("用户 {} 的总收入: {}", profile.getId(), total);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * 获取当前用户最近 5 条收入记录
     *
     * @return 最近 5 条收入的 DTO 列表
     */
    public List<IncomeDTO> getLatest5IncomesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();

        List<IncomeDTO> incomes = incomeRepository
                .findTop5ByProfileIdOrderByDateDesc(profile.getId())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        log.debug("获取用户 {} 最近 5 条收入,数量: {}", profile.getId(), incomes.size());
        return incomes;
    }

    /**
     * 删除指定的收入记录
     *
     * @param id 收入记录 ID
     * @throws ResourceNotFoundException 如果收入记录不存在
     * @throws UnauthorizedException 如果当前用户无权删除该记录
     */
    @Transactional
    public void deleteIncome(Long id) {
        log.info("尝试删除收入记录: {}", id);

        ProfileEntity profile = profileService.getCurrentProfile();
        IncomeEntity incomeEntity = incomeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("收入记录不存在"));

        // 权限检查
        if (!incomeEntity.getProfile().getId().equals(profile.getId())) {
            log.warn("用户 {} 尝试删除不属于自己的收入记录 {}",
                    profile.getId(), id);
            throw new UnauthorizedException("无权删除此收入记录");
        }

        incomeRepository.delete(incomeEntity);
        log.info("成功删除收入记录: {}", id);
    }

    /**
     * 获取当前用户本月的所有收入记录
     *
     * @return 本月收入的 DTO 列表
     */
    public List<IncomeDTO> getCurrentMonthIncomesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();

        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = LocalDate.now().withDayOfMonth(
                LocalDate.now().lengthOfMonth()
        );

        List<IncomeDTO> incomes = incomeRepository
                .findByProfileIdAndDateBetween(profile.getId(), startOfMonth, endOfMonth)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        log.debug("获取用户 {} 本月收入,数量: {}", profile.getId(), incomes.size());
        return incomes;
    }

    /**
     * 添加新的收入记录
     *
     * @param incomeDTO 收入信息 DTO
     * @return 保存后的收入 DTO
     * @throws ResourceNotFoundException 如果分类不存在
     */
    @Transactional
    public IncomeDTO addIncome(IncomeDTO incomeDTO) {
        log.info("添加新收入: {}, 金额: {}",
                incomeDTO.getName(), incomeDTO.getAmount());

        ProfileEntity profile = profileService.getCurrentProfile();
        CategoryEntity category = categoryRepository.findById(incomeDTO.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("分类不存在"));

        IncomeEntity saved = incomeRepository.save(
                toEntity(incomeDTO, profile, category)
        );

        log.info("成功添加收入,ID: {}", saved.getId());
        return toDTO(saved);
    }

    /**
     * 将 IncomeDTO 转换为 IncomeEntity
     */
    private IncomeEntity toEntity(IncomeDTO incomeDTO,
                                  ProfileEntity profile,
                                  CategoryEntity category) {
        return IncomeEntity.builder()
                .id(incomeDTO.getId())
                .name(incomeDTO.getName())
                .icon(incomeDTO.getIcon())
                .date(incomeDTO.getDate())
                .amount(incomeDTO.getAmount())
                .profile(profile)
                .category(category)
                .build();
    }

    /**
     * 将 IncomeEntity 转换为 IncomeDTO
     */
    private IncomeDTO toDTO(IncomeEntity entity) {
        return IncomeDTO.builder()
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