package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.Group.GroupUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupUserRepository extends JpaRepository<GroupUser, Long> {

}
