package in.atail.moneymanager.controller;

import in.atail.moneymanager.dto.ActivationEmailRequestDTO;
import in.atail.moneymanager.dto.AuthDTO;
import in.atail.moneymanager.dto.ProfileDTO;
import in.atail.moneymanager.dto.RefreshTokenRequestDTO;
import in.atail.moneymanager.dto.TokenResponseDTO;
import in.atail.moneymanager.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping({"/register", "/auth/register"})
    public ResponseEntity<ProfileDTO> registerProfile(@Valid @RequestBody ProfileDTO profileDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profileService.registerProfile(profileDTO));
    }

    @GetMapping({"/activate", "/auth/activate"})
    public ResponseEntity<String> activateProfile(@RequestParam String token) {
        boolean isActivated = profileService.activateProfile(token);
        if (isActivated) {
            return ResponseEntity.ok("Account activated successfully. Please sign in.");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Activation token is invalid or already used.");
    }

    @PostMapping({"/resend-activation", "/auth/resend-activation"})
    public ResponseEntity<Map<String, String>> resendActivationEmail(
            @RequestBody @Valid ActivationEmailRequestDTO requestDTO) {
        profileService.resendActivationEmail(requestDTO.getEmail());
        return ResponseEntity.ok(Map.of("message", "Activation email sent"));
    }

    @PostMapping({"/login", "/auth/login"})
    public ResponseEntity<TokenResponseDTO> login(@RequestBody @Valid AuthDTO authDto) {
        return ResponseEntity.ok(profileService.authenticateAndGenerateToken(authDto));
    }

    @PostMapping({"/refresh-token", "/auth/refresh-token"})
    public ResponseEntity<TokenResponseDTO> refreshToken(@RequestBody @Valid RefreshTokenRequestDTO requestDTO) {
        return ResponseEntity.ok(profileService.refreshAccessToken(requestDTO.getRefreshToken()));
    }

    @PostMapping({"/logout", "/auth/logout"})
    public ResponseEntity<Map<String, String>> logout(@RequestBody @Valid RefreshTokenRequestDTO requestDTO) {
        profileService.logoutCurrentProfile(requestDTO.getRefreshToken());
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping({"/me", "/auth/me"})
    public ResponseEntity<ProfileDTO> getMyProfile() {
        return ResponseEntity.ok(profileService.getMyProfile());
    }
}
