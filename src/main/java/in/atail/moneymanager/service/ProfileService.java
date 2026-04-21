package in.atail.moneymanager.service;

import in.atail.moneymanager.dto.AuthDTO;
import in.atail.moneymanager.dto.ProfileDTO;
import in.atail.moneymanager.dto.TokenResponseDTO;
import in.atail.moneymanager.entity.ProfileEntity;
import in.atail.moneymanager.entity.RefreshTokenEntity;
import in.atail.moneymanager.exception.ResourceNotFoundException;
import in.atail.moneymanager.exception.UnauthorizedException;
import in.atail.moneymanager.repository.ProfileRepository;
import in.atail.moneymanager.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Value("${money.manager.frontend.url}")
    private String frontendUrl;

    @Transactional
    public ProfileDTO registerProfile(ProfileDTO profileDTO) {
        String normalizedEmail = normalizeEmail(profileDTO.getEmail());
        profileRepository.findByEmail(normalizedEmail).ifPresent(existingProfile -> {
            if (Boolean.TRUE.equals(existingProfile.getIsActive())) {
                throw new IllegalStateException("Email is already registered");
            }
            throw new IllegalStateException("Account exists but is not activated. Please resend the activation email");
        });

        ProfileEntity newProfileEntity = toEntity(profileDTO, normalizedEmail);
        newProfileEntity.setActivityToken(generateActivationToken());
        newProfileEntity = profileRepository.save(newProfileEntity);
        sendActivationEmail(newProfileEntity);

        return toDTO(newProfileEntity);
    }

    public ProfileEntity toEntity(ProfileDTO profileDTO, String normalizedEmail) {
        return ProfileEntity.builder()
                .name(profileDTO.getName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(profileDTO.getPassword()))
                .profileImageUrl(profileDTO.getProfileImageUrl())
                .build();
    }

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

    public boolean isAccountActive(String email) {
        return profileRepository.findByEmail(normalizeEmail(email))
                .map(ProfileEntity::getIsActive)
                .orElse(false);
    }

    public ProfileEntity getCurrentProfile() {
        return profileRepository.findByEmail(getCurrentUserEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Current profile does not exist"));
    }

    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            throw new UnauthorizedException("Authentication is required");
        }
        return authentication.getName();
    }

    public Long getCurrentProfileId() {
        return getCurrentProfile().getId();
    }

    public ProfileDTO getMyProfile() {
        return toDTO(getCurrentProfile());
    }

    public ProfileDTO getPublicProfile(String email) {
        String normalizedEmail = normalizeEmail(email);
        ProfileEntity user = profileRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Profile does not exist: " + normalizedEmail));
        return toDTO(user);
    }

    public TokenResponseDTO authenticateAndGenerateToken(AuthDTO authDto) {
        String normalizedEmail = normalizeEmail(authDto.getEmail());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, authDto.getPassword())
            );
        } catch (AuthenticationException ex) {
            throw new UnauthorizedException("Invalid email or password");
        }

        ProfileEntity profile = profileRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Profile does not exist: " + normalizedEmail));
        if (!Boolean.TRUE.equals(profile.getIsActive())) {
            throw new IllegalStateException("Account is not activated");
        }

        RefreshTokenEntity refreshToken = refreshTokenService.createRefreshToken(profile);
        return toTokenResponse(profile, refreshToken);
    }

    @Transactional
    public TokenResponseDTO refreshAccessToken(String refreshTokenValue) {
        RefreshTokenEntity refreshToken = refreshTokenService.verifyRefreshToken(refreshTokenValue);
        ProfileEntity profile = refreshToken.getProfile();

        if (!Boolean.TRUE.equals(profile.getIsActive())) {
            throw new IllegalStateException("Account is not activated");
        }

        refreshTokenService.revokeRefreshToken(refreshTokenValue);
        RefreshTokenEntity newRefreshToken = refreshTokenService.createRefreshToken(profile);
        return toTokenResponse(profile, newRefreshToken);
    }

    @Transactional
    public void resendActivationEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        ProfileEntity profile = profileRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Profile does not exist: " + normalizedEmail));
        if (Boolean.TRUE.equals(profile.getIsActive())) {
            throw new IllegalStateException("Account is already activated");
        }

        profile.setActivityToken(generateActivationToken());
        sendActivationEmail(profileRepository.save(profile));
    }

    @Transactional
    public void logoutCurrentProfile(String refreshToken) {
        refreshTokenService.revokeRefreshToken(refreshToken, getCurrentProfileId());
    }

    private TokenResponseDTO toTokenResponse(ProfileEntity profile, RefreshTokenEntity refreshToken) {
        return TokenResponseDTO.builder()
                .accessToken(jwtUtil.generateToken(profile.getEmail()))
                .refreshToken(refreshToken.getToken())
                .profile(toDTO(profile))
                .build();
    }

    private void sendActivationEmail(ProfileEntity profile) {
        String activityLink = frontendUrl + "/activate?token=" + profile.getActivityToken();
        String subject = "Activate your Money Manager account";
        String body = "Activate your account using this link: " + activityLink;
        try {
            emailService.sendEmail(profile.getEmail(), subject, body);
        } catch (Exception ex) {
            log.warn("Failed to send activation email to {}", profile.getEmail(), ex);
        }
    }

    private String generateActivationToken() {
        return UUID.randomUUID().toString();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
