package in.atail.moneymanager.controller;

import in.atail.moneymanager.dto.CategoryDTO;
import in.atail.moneymanager.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {


    private final CategoryService categoryService;

    /**
     * 保存分类
     *
     * @param categoryDTO 分类数据
     * @return 保存后的分类数据
     */
    @PostMapping
    public ResponseEntity<CategoryDTO> saveCategory(@RequestBody @Valid CategoryDTO categoryDTO){
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.saveCategory(categoryDTO));
    }


    /**
     * 获取所有分类
     *
     * @return 所有分类数据
     */
    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getCategories(){
        return ResponseEntity.ok(categoryService.getCategories());
    }


    /**
     * 根据类型获取分类
     *
     * @param type 分类类型
     * @return 指定类型的分类数据
     */
    @GetMapping("/{type}")
    public ResponseEntity<List<CategoryDTO>> getCategoriesByTypeForCurrentUser(@PathVariable String type){
        return ResponseEntity.ok(categoryService.getCategoriesByTypeForCurrentUser(type));
    }


    /**
     * 更新分类
     *
     * @param categoryId 分类ID
     * @param categoryDTO 分类数据
     * @return 更新后的分类数据
     */
    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryDTO> updateCategory(@PathVariable Long categoryId, @RequestBody @Valid CategoryDTO categoryDTO){
        return ResponseEntity.ok(categoryService.updateCategory(categoryId, categoryDTO));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }
}
