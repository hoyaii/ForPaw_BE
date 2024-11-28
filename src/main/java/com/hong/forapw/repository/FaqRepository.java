package com.hong.forapw.repository;

import com.hong.forapw.domain.faq.FAQ;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaqRepository extends JpaRepository<FAQ, Long> {
}
