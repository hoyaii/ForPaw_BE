package com.hong.ForPaw.repository.Inquiry;

import com.hong.ForPaw.domain.Inquiry.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    List<Inquiry> findAllByUserId(Long userId);
}
