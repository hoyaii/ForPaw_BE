package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.animal.Animal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {
    Page<Animal> findAll(Pageable pageable);
}
