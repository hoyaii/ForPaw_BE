package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.User.Role;
import com.hong.ForPaw.domain.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.nickName = :nickName")
    Optional<User> findByNickName(String nickName);

    @Query("SELECT u.profileURL FROM User u WHERE u.id = :userId AND u.removedAt IS NULL")
    Optional<String> findProfileById(@Param("userId") Long userId);
}
