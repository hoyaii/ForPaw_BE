package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.District;
import com.hong.ForPaw.domain.Post.Post;
import com.hong.ForPaw.domain.Province;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.domain.User.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.removedAt IS NULL")
    List<User> findAll();

    @Query("SELECT u.nickName FROM User u WHERE u.id = :id AND u.removedAt IS NULL")
    String findNickname(@Param("id") Long id);

    @EntityGraph(attributePaths = {"userStatus"})
    @Query("SELECT u FROM User u WHERE (u.role = 'ADMIN' OR u.role = 'USER') AND u.removedAt IS NULL")
    Page<User> findAllAdminAndUserWithUserStatus(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.id = :id AND u.removedAt IS NULL")
    Optional<User> findById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"userStatus"})
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.removedAt IS NULL")
    Optional<User> findByIdWithUserStatus(@Param("id") Long id);

    @Query("SELECT u.role FROM User u WHERE u.id = :id AND u.removedAt IS NULL")
    Optional<UserRole> findRoleById(@Param("id") Long id);

    @Query("SELECT COUNT(u) FROM User u WHERE u.status.isActive = true AND u.removedAt IS NULL")
    Long countActiveUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.status.isActive = false AND u.removedAt IS NULL")
    Long countInActiveUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdDate >= :date")
    Long countALlWithinDate(LocalDateTime date);

    @Query("SELECT u.district FROM User u WHERE u.id = :id AND u.removedAt IS NULL")
    Optional<District> findDistrictById(@Param("id") Long id);

    @Query("SELECT u.province FROM User u WHERE u.id = :id AND u.removedAt IS NULL")
    Optional<Province> findProvinceById(@Param("id") Long id);

    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithRemoved(@Param("id") Long id);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.removedAt IS NULL")
    Optional<User> findByEmail(@Param("email") String email);

    @EntityGraph(attributePaths = {"status"})
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.removedAt IS NULL")
    Optional<User> findByEmailWithUserStatus(@Param("email") String email);

    @Query("SELECT u.profileURL FROM User u WHERE u.id = :userId AND u.removedAt IS NULL")
    Optional<String> findProfileById(@Param("userId") Long userId);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email")
    boolean existsByEmailWithRemoved(@Param("email") String email);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.nickName = :nickName")
    boolean existsByNickWithRemoved(@Param("nickName") String nickName);

    @Modifying
    @Query("DELETE FROM User u WHERE u.removedAt <= :cutoffDate")
    void deleteAllWithRemovedBefore(LocalDateTime cutoffDate);

    @Query(value =
        "SELECT u.* " +
            "FROM user u " +
            "INNER JOIN user_status us ON u.id = us.user_id " +
            "LEFT JOIN ( " +
            "    SELECT user_id, MAX(date) AS latest_visit " +
            "    FROM visit " +
            "    GROUP BY user_id " +
            ") v ON u.id = v.user_id ", nativeQuery = true)
    List<User> findUserWithLatestVisit();

    // Spring Data JPA 메서드는 외래키 제약 조건 때문에 작동하지 않음 => Post나 Comment를 남겨둬야 하기 때문에 Spring Data JPA 말고 직접 업데이트 쿼리를 날린다
    @Modifying
    @Query("UPDATE User u SET u.removedAt = NOW() WHERE u.id= :id")
    void deleteById(@Param("id") Long id);
}
