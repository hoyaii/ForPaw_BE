package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.Group.GroupUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupUserRepository extends JpaRepository<GroupUser, Long> {

    Optional<GroupUser> findByGroupIdAndUserId(Long groupId, Long userId);
}
