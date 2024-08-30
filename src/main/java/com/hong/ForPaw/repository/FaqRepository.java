package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.FAQ.FAQ;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaqRepository extends JpaRepository<FAQ, Long> {
}
