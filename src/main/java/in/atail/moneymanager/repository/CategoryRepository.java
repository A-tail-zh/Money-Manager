package in.atail.moneymanager.repository;

import in.atail.moneymanager.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    //select * from tbl_categories where profile_id = ?
    List<CategoryEntity> findByProfileId(Long profileId);

    //select * from tbl_categories where id = ? and profile_id = ?
    Optional<CategoryEntity> findByIdAndProfileId(Long id, Long profileId);

    //select * from tbl_categories where type = ? and profile_id = ?
    List<CategoryEntity> findByTypeAndProfileId(String type, Long profileId);

    //select exists(select * from tbl_categories where name = ? and profile_id = ?)
    Boolean existsByNameAndProfileId(String name, Long profileId);
}
