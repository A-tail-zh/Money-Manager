package in.atail.moneymanager.service;

import in.atail.moneymanager.dto.AuthDTO;
import in.atail.moneymanager.dto.ProfileDTO;
import in.atail.moneymanager.entity.ProfileEntity;
import in.atail.moneymanager.repository.ProfileRepository;
import in.atail.moneymanager.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Value("${app.base-url}")
    private String baseUrl;


    /**
     * 注册用户档案
     * 创建一个新的用户档案实体对象，并保存到数据库中
     * 发送账户激活邮件，并返回用户档案数据传输对象
     *
     * @param profileDTO 待注册的用户档案数据传输对象，包含 id、name、email、password 等信息
     * @return ProfileDTO 注册成功的用户档案数据传输对象，包含 id、name、email、profileImageUrl、createdAt、updatedAt 字段
     */
    @Transactional
    public ProfileDTO registerProfile(ProfileDTO profileDTO) {
        if (profileRepository.existsByEmail(profileDTO.getEmail())) {
            throw new RuntimeException("该邮箱已被注册：" + profileDTO.getEmail());
        }

        ProfileEntity newProfileEntity = toEntity(profileDTO);
        newProfileEntity.setActivityToken(UUID.randomUUID().toString());
        newProfileEntity = profileRepository.save(newProfileEntity);

        String activityLink = baseUrl + "/activate?token=" + newProfileEntity.getActivityToken();
        String body = "请点击链接激活您的账户： " + activityLink;
        String subject = "个人资料激活";
        try {
            emailService.sendEmail(profileDTO.getEmail(), subject, body);
        } catch (Exception e) {
            // 实际项目中应使用 Slf4j 记录日志，而非打印堆栈
            System.err.println("激活邮件发送失败，用户: " + profileDTO.getEmail()
                    + "，原因: " + e.getMessage());
        }

        return toDTO(newProfileEntity);
    }



    /**
     * 将 ProfileDTO 转换为 ProfileEntity
     * 用于将数据传输对象转换为数据库实体对象，包含敏感信息（如密码）
     *
     * @param profileDTO 待转换的用户档案数据传输对象，包含 id、name、email、password 等信息
     * @return ProfileEntity 转换后的用户档案实体对象，包含完整的数据库记录信息
     */
    public ProfileEntity toEntity(ProfileDTO profileDTO) {
        return ProfileEntity.builder()
                // id 不设置，由数据库自增生成
                .name(profileDTO.getName())
                .email(profileDTO.getEmail())
                .password(passwordEncoder.encode(profileDTO.getPassword()))
                .profileImageUrl(profileDTO.getProfileImageUrl())
                // createdAt / updatedAt 由 @CreationTimestamp / @UpdateTimestamp 自动管理
                .build();
    }


    /**
     * 将 ProfileEntity 转换为 ProfileDTO
     * 用于将数据库实体对象转换为数据传输对象，不包含敏感信息（如密码）
     *
     * @param profileEntity 待转换的用户档案实体对象，包含完整的数据库记录信息
     * @return ProfileDTO 转换后的用户档案数据传输对象，包含 id、name、email、profileImageUrl、createdAt、updatedAt 字段
     */
    public ProfileDTO toDTO(ProfileEntity profileEntity) {
        return ProfileDTO.builder()
                .id(profileEntity.getId())
                .name(profileEntity.getName())
                .email(profileEntity.getEmail())
                .profileImageUrl(profileEntity.getProfileImageUrl())
                .createdAt(profileEntity.getCreatedAt())
                .updatedAt(profileEntity.getUpdatedAt())
                .build();
    }


    /**
     * 激活用户档案
     * 根据激活令牌查找用户并将其账户状态设置为已激活
     *
     * @param activityToken 账户激活令牌，用于验证和查找待激活的用户档案
     * @return boolean 如果找到对应的用户档案并成功激活则返回 true，否则返回 false
     */
    @Transactional
    public boolean activateProfile(String activityToken) {
        return profileRepository.findByActivityToken(activityToken)
                .map(profile -> {
                    if (Boolean.TRUE.equals(profile.getIsActive())) {
                        return false;
                    }
                    profile.setIsActive(true);
                    profile.setActivityToken(null);
                    profileRepository.save(profile);
                    return true;
                })
                .orElse(false);
    }


    /**
     * 检查账户是否已激活
     * 根据邮箱地址查询用户档案并返回其激活状态
     *
     * @param email 待检查的用户邮箱地址
     * @return boolean 如果账户存在且已激活则返回 true，否则返回 false
     */
    public boolean isAccountActive(String email) {
        return profileRepository.findByEmail(email)
                .map(ProfileEntity::getIsActive)
                .orElse(false);
    }


    /**
     * 获取当前用户档案
     * 根据当前用户邮箱地址查询用户档案并返回
     *
     * @return ProfileEntity 当前用户档案实体对象，包含完整的数据库记录信息
     */
    public ProfileEntity getCurrentProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return profileRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("用户:"+authentication.getName()+"不存在"));
    }



    /**
     * 获取当前用户公开信息
     * 根据当前用户邮箱地址查询用户档案并返回公开信息
     *
     * @return ProfileDTO 当前用户公开信息数据传输对象，包含 id、name、email、profileImageUrl、createdAt、updatedAt 字段
     */
    public ProfileDTO getMyProfile() {
        return toDTO(getCurrentProfile());
    }


    /**
     * 获取公开用户信息
     * 根据用户邮箱地址查询用户档案并返回公开信息
     *
     * @param email 待查询的用户邮箱地址
     * @return ProfileDTO 公开用户信息数据传输对象，包含 id、name、email、profileImageUrl、createdAt、updatedAt 字段
     */
    public ProfileDTO getPublicProfile(String email) {
        ProfileEntity user = profileRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("用户: " + email + " 不存在"));
        return toDTO(user);
    }


    /**
     * 验证用户并生成令牌
     * 使用用户邮箱和密码进行验证，并生成 JWT 令牌
     *
     * @param authDto 登录信息，包含用户邮箱和密码
     * @return Map<String, Object> 包含生成的令牌和用户公开信息
     */
    public Map<String, Object> authenticateAndGenerateToken(AuthDTO authDto) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authDto.getEmail(), authDto.getPassword()));
            // ✅ 修复：token 是变量引用，不是字符串字面量
            String token = jwtUtil.generateToken(authDto.getEmail());
            return Map.of(
                    "token", token,
                    "user", getPublicProfile(authDto.getEmail())
            );
        } catch (Exception e) {
            throw new RuntimeException("邮箱或密码错误: " + e.getMessage());
        }
    }
}