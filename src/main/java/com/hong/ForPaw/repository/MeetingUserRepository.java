package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.Group.MeetingUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeetingUserRepository extends JpaRepository<MeetingUser, Long> {

}
