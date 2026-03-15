package in.atail.moneymanager.controller;

import in.atail.moneymanager.dto.AuthDTO;
import in.atail.moneymanager.dto.ProfileDTO;
import in.atail.moneymanager.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ProfileController {


    private final ProfileService profileService;


    /**
     * 处理用户注册请求
     * 接收用户提交的 ProfileDTO 数据，调用服务层进行注册，返回创建成功的用户信息
     *
     * @param profileDTO 包含用户注册信息的 DTO 对象，使用@Valid 注解进行参数校验
     * @return ResponseEntity<ProfileDTO> 包含已注册用户信息的响应实体，HTTP 状态码为 201 CREATED
     */
    @PostMapping("/register")
    public ResponseEntity<ProfileDTO> registerProfile(@Valid @RequestBody ProfileDTO profileDTO) {
        ProfileDTO registeredProfile = profileService.registerProfile(profileDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(registeredProfile);
    }

    /**
     * 处理用户激活请求
     * 接收用户提交的激活令牌，调用服务层进行账户激活，返回激活结果信息
     *
     * @param token 激活令牌，使用@RequestParam 注解获取
     * @return ResponseEntity<String> 激活结果信息，HTTP 状态码为 200 OK
     */
    @GetMapping("/activate")
    public ResponseEntity<String> activateProfile(@RequestParam String token) {
        boolean isActivated = profileService.activateProfile(token);
        if (isActivated) {
            return ResponseEntity.ok("账户已成功激活，请前往登录。");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("激活令牌无效或已被使用。");
        }
    }


    /**
     * 处理用户登录请求
     * 接收用户提交的认证信息，调用服务层进行登录验证，返回登录结果信息
     *
     * @param authDto 认证信息，包含邮箱和密码，使用@RequestBody 注解获取
     * @return ResponseEntity<Map<String, Object>> 登录结果信息，HTTP 状态码为 200 OK
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody AuthDTO authDto) {
        try {
            // ✅ 修复：条件逻辑正确，先拦截未激活账户
            if (!profileService.isAccountActive(authDto.getEmail())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "账户未激活，请先前往邮箱激活账户"));
            }
            // ✅ 修复：已激活账户才执行认证
            Map<String, Object> response = profileService.authenticateAndGenerateToken(authDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "邮箱或密码错误"));
        }
    }
}