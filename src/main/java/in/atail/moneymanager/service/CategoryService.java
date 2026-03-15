package in.atail.moneymanager.service;

import in.atail.moneymanager.dto.CategoryDTO;
import in.atail.moneymanager.entity.CategoryEntity;
import in.atail.moneymanager.entity.ProfileEntity;
import in.atail.moneymanager.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {


    private final CategoryRepository categoryRepository;
    private final ProfileService profileService;


    /**
     * 保存新的支出/收入类别
     * 检查当前用户下是否存在同名类别，如果存在则抛出异常，否则保存到数据库
     *
     * @param categoryDTO 待保存的类别数据传输对象，包含 name、icon、type 等信息
     * @return CategoryDTO 已保存的类别数据传输对象，包含完整的类别信息（包括自动生成的 id）
     */
    public CategoryDTO saveCategory(CategoryDTO categoryDTO){
        ProfileEntity profile = profileService.getCurrentProfile();
        if(categoryRepository.existsByNameAndProfileId(categoryDTO.getName(), profile.getId())){
            throw new RuntimeException("类别名称: " + categoryDTO.getName() + "已经存在");
        }

        return toDTO(categoryRepository.save(toEntity(categoryDTO, profile)));
    }


    /**
     * 获取当前用户的所有类别列表
     * 从数据库查询当前登录用户创建的所有支出/收入类别，并转换为 DTO 列表返回
     *
     * @return List<CategoryDTO> 当前用户的所有类别数据传输对象列表，每个 DTO 包含完整的类别信息
     */
    public List<CategoryDTO> getCategories(){
        ProfileEntity profile = profileService.getCurrentProfile();
        List<CategoryEntity> categories = categoryRepository.findByProfileId(profile.getId());
        return categories.stream().map(this::toDTO).toList();
    }


    /**
     * 获取当前用户指定类型的类别列表
     * 从数据库查询当前登录用户创建的指定类型的类别，并转换为 DTO 列表返回
     *
     * @param type 指定类别的类型，可选值有 "expense" 和 "income"
     * @return List<CategoryDTO> 当前用户指定类型的类别数据传输对象列表，每个 DTO 仅包含类别名称和图标信息
     */
    public List<CategoryDTO> getCategoriesByTypeForCurrentUser(String type){
        ProfileEntity profile = profileService.getCurrentProfile();
        List<CategoryEntity> categories = categoryRepository.findByTypeAndProfileId(type, profile.getId());
        return categories.stream().map(this::toDTO).toList();
    }


    /**
     * 更新指定类别信息
     * 检查当前用户下是否存在同名类别，如果存在则抛出异常，否则更新数据库中的类别信息
     *
     * @param categoryId 待更新的类别的 ID
     * @param categoryDTO 包含更新后的类别数据传输对象，包含 name、icon、type 等信息
     * @return CategoryDTO 已更新的类别数据传输对象，包含完整的类别信息（包括自动生成的 id）
     */
    public CategoryDTO updateCategory(Long categoryId, CategoryDTO categoryDTO){
        ProfileEntity profile = profileService.getCurrentProfile();
        CategoryEntity category = categoryRepository.findByIdAndProfileId(categoryId, profile.getId())
                .orElseThrow(() -> new RuntimeException("类别不存在"));
        category.setName(categoryDTO.getName());
        category.setIcon(categoryDTO.getIcon());
        category.setType(categoryDTO.getType());
        return toDTO(categoryRepository.save(category));
    }


    /**
     * 将 CategoryDTO 转换为 CategoryEntity
     *
     * @param categoryDTO 待转换的类别数据传输对象，包含 name、icon、type 等信息
     * @param profile 当前登录用户
     * @return CategoryEntity 转换后的类别实体对象
     */
    public CategoryEntity toEntity(CategoryDTO categoryDTO, ProfileEntity profile){
        return CategoryEntity.builder()
                .name(categoryDTO.getName())
                .icon(categoryDTO.getIcon())
                .type(categoryDTO.getType())
                .profile(profile)
                .build();
    }


    /**
     * 将 CategoryEntity 转换为 CategoryDTO
     *
     * @param categoryEntity 待转换的类别实体对象
     * @return CategoryDTO 转换后的类别数据传输对象，包含完整的类别信息
     */
    public CategoryDTO toDTO(CategoryEntity categoryEntity){
        return CategoryDTO.builder()
                .id(categoryEntity.getId())
                .profileId(categoryEntity.getProfile() != null ? categoryEntity.getProfile().getId() : null)
                .name(categoryEntity.getName())
                .icon(categoryEntity.getIcon())
                .type(categoryEntity.getType())
                .createdAt(categoryEntity.getCreatedAt())
                .updatedAt(categoryEntity.getUpdatedAt())
                .build();
    }

}
