package in.atail.moneymanager.repository;

import in.atail.moneymanager.entity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfileRepository extends JpaRepository<ProfileEntity, Long> {

    //select * from profile where email = ?
    Optional<ProfileEntity> findByEmail(String email);

    //select * from profile where activity_token = ?
    Optional<ProfileEntity> findByActivityToken(String activityToken);

    //select exists(select * from profile where email = ?)
    boolean existsByEmail(String email);

}
