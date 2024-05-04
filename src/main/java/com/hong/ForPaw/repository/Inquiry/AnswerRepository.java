package com.hong.ForPaw.repository.Inquiry;

import com.hong.ForPaw.domain.Inquiry.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findAllByInquiryId(Long inquiryId);
}
