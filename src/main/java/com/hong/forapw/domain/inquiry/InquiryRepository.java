package com.hong.forapw.domain.inquiry;

import com.hong.forapw.domain.inquiry.entity.Inquiry;
import com.hong.forapw.domain.inquiry.constant.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    @EntityGraph(attributePaths = {"answerer"})
    List<Inquiry> findAllByQuestionerId(Long questionerId);

    @EntityGraph(attributePaths = {"questioner"})
    @Query("SELECT i From Inquiry i WHERE (:status IS NULL OR i.status = :status)")
    Page<Inquiry> findByStatusWithUser(@Param("status") InquiryStatus status, Pageable pageable);
}
