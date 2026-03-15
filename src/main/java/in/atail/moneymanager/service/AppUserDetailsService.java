package in.atail.moneymanager.service;

import in.atail.moneymanager.entity.ProfileEntity;
import in.atail.moneymanager.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {


    private final ProfileRepository profileRepository;


    /**
     * 根据用户名（邮箱）加载用户详情
     *
     * @param email 用户名（邮箱）
     * @return UserDetails 用户详情
     * @throws UsernameNotFoundException 如果用户不存在则抛出此异常
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        ProfileEntity existingProfile = profileRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        return User.builder()
                .username(existingProfile.getEmail())
                .password(existingProfile.getPassword())
                .authorities(Collections.emptyList())
                .build();
    }
}
