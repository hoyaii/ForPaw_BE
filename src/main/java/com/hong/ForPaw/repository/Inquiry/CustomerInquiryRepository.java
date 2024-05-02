package com.hong.ForPaw.repository.Inquiry;

import com.hong.ForPaw.domain.Inquiry.CustomerInquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerInquiryRepository extends JpaRepository<CustomerInquiry, Long> {
}
