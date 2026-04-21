package in.atail.moneymanager.service;

import in.atail.moneymanager.dto.IncomeDTO;
import in.atail.moneymanager.dto.PageDTO;
import in.atail.moneymanager.entity.CategoryEntity;
import in.atail.moneymanager.entity.IncomeEntity;
import in.atail.moneymanager.entity.ProfileEntity;
import in.atail.moneymanager.exception.ResourceNotFoundException;
import in.atail.moneymanager.repository.CategoryRepository;
import in.atail.moneymanager.repository.IncomeRepository;
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
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private final CategoryRepository categoryRepository;
    private final ProfileService profileService;

    public List<IncomeDTO> filterIncome(LocalDate startDate, LocalDate endDate, String keyword, Sort sort) {
        return incomeRepository.findByProfileIdAndDateBetweenAndNameContainingIgnoreCase(
                        profileService.getCurrentProfileId(),
                        startDate,
                        endDate,
                        keyword,
                        sort
                ).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public BigDecimal getTotalIncomeForCurrentUser() {
        Long profileId = profileService.getCurrentProfileId();
        BigDecimal total = incomeRepository.findTotalIncomeByProfileId(profileId);
        log.debug("User {} total income {}", profileId, total);
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal getCurrentMonthIncomeForCurrentUser() {
        Long profileId = profileService.getCurrentProfileId();
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        BigDecimal total = incomeRepository.findTotalIncomeByProfileIdAndDateBetween(profileId, startOfMonth, endOfMonth);
        log.debug("User {} current month income {}", profileId, total);
        return total != null ? total : BigDecimal.ZERO;
    }

    public List<IncomeDTO> getLatest5IncomesForCurrentUser() {
        Long profileId = profileService.getCurrentProfileId();
        List<IncomeDTO> incomes = incomeRepository.findTop5ByProfileIdOrderByDateDesc(profileId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        log.debug("Loaded {} latest incomes for user {}", incomes.size(), profileId);
        return incomes;
    }

    @Transactional
    public void deleteIncome(Long id) {
        Long profileId = profileService.getCurrentProfileId();
        IncomeEntity incomeEntity = incomeRepository.findByIdAndProfileId(id, profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Income record does not exist"));
        incomeRepository.delete(incomeEntity);
        log.info("User {} deleted income {}", profileId, id);
    }

    public PageDTO<IncomeDTO> getCurrentMonthIncomesForCurrentUser(int page, int size, String sortBy, String sortDir) {
        Long profileId = profileService.getCurrentProfileId();
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        Sort sort = Sort.by("asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<IncomeDTO> incomePage = incomeRepository
                .findByProfileIdAndDateBetween(profileId, startOfMonth, endOfMonth, pageable)
                .map(this::toDTO);

        return PageDTO.from(incomePage);
    }

    @Transactional
    public IncomeDTO addIncome(IncomeDTO incomeDTO) {
        ProfileEntity profile = profileService.getCurrentProfile();
        CategoryEntity category = getIncomeCategory(profile.getId(), incomeDTO.getCategoryId());

        IncomeEntity saved = incomeRepository.save(toEntity(incomeDTO, profile, category));
        log.info("User {} added income {}", profile.getId(), saved.getId());
        return toDTO(saved);
    }

    @Transactional
    public IncomeDTO updateIncome(Long id, IncomeDTO incomeDTO) {
        Long profileId = profileService.getCurrentProfileId();
        IncomeEntity incomeEntity = incomeRepository.findByIdAndProfileId(id, profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Income record does not exist"));
        CategoryEntity category = getIncomeCategory(profileId, incomeDTO.getCategoryId());

        incomeEntity.setName(incomeDTO.getName());
        incomeEntity.setIcon(incomeDTO.getIcon());
        incomeEntity.setDate(incomeDTO.getDate());
        incomeEntity.setAmount(incomeDTO.getAmount());
        incomeEntity.setCategory(category);

        IncomeEntity updatedIncome = incomeRepository.save(incomeEntity);
        log.info("User {} updated income {}", profileId, updatedIncome.getId());
        return toDTO(updatedIncome);
    }

    private IncomeEntity toEntity(IncomeDTO incomeDTO, ProfileEntity profile, CategoryEntity category) {
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

    private IncomeDTO toDTO(IncomeEntity entity) {
        return IncomeDTO.builder()
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

    private CategoryEntity getIncomeCategory(Long profileId, Long categoryId) {
        CategoryEntity category = categoryRepository.findByIdAndProfileId(categoryId, profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Category does not exist"));
        if (!"income".equalsIgnoreCase(category.getType())) {
            throw new IllegalStateException("Income records must use an income category");
        }
        return category;
    }
}
