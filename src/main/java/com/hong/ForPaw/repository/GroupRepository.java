package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.Group.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    Optional<Group> findByName(String name);

    Page<Group> findByRegion(String region, Pageable pageable);

    boolean existsByName(String name);
}
