package in.atail.moneymanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryDTO {
    private Long id;
    private Long profileId;
    @NotBlank(message = "分类名称不能为空")
    @Size(min = 1, max = 100, message = "分类名称长度必须在 1 到 100 个字符之间")
    private String name;
    @Size(max = 50, message = "图标长度不能超过 50 个字符")
    private String icon;
    @NotBlank(message = "分类类型不能为空")
    private String type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
