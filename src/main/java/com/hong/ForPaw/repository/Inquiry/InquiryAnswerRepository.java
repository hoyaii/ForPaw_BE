package com.hong.ForPaw.repository.Inquiry;

import com.hong.ForPaw.domain.Inquiry.InquiryAnswer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InquiryAnswerRepository extends JpaRepository<InquiryAnswer, Long> {

    @EntityGraph(attributePaths = {"answerer"})
    @Query("SELECT ia FROM InquiryAnswer ia WHERE ia.inquiry.id IN :inquiryIds")
    List<InquiryAnswer> findAllByInquiryIdsWithAnswerer(@Param("inquiryIds") List<Long> inquiryIds);

    @Query("SELECT COUNT(ia) > 0 FROM InquiryAnswer ia WHERE ia.inquiry.id = :inquiryId")
    boolean existsByInquiryId(@Param("inquiryId") Long inquiryId);
}
