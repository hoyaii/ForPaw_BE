package com.hong.ForPaw.repository.Inquiry;

import com.hong.ForPaw.domain.Inquiry.InquiryAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryAnswerRepository extends JpaRepository<InquiryAnswer, Long> {

    List<InquiryAnswer> findAllByInquiryId(Long inquiryId);
}
