package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.Animal.Animal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {
    Page<Animal> findAll(Pageable pageable);

    Page<Animal> findByShelterId(Long careRegNo, Pageable pageable);

    boolean existsById(Long id);
}
