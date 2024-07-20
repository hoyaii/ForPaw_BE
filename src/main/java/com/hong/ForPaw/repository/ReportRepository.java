package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.Apply.Apply;
import com.hong.ForPaw.domain.Apply.ApplyStatus;
import com.hong.ForPaw.domain.Report.ContentType;
import com.hong.ForPaw.domain.Report.Report;
import com.hong.ForPaw.domain.Report.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("SELECT COUNT(r) > 0 FROM Report r WHERE r.reporter.id = :reporterId AND r.contentId = :contentId AND r.contentType = :contentType")
    boolean existsByReporterIdAndContent(@Param("reporterId") Long reporterId, @Param("contentId") Long contentId, @Param("contentType") ContentType contentType);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT r From Report r WHERE (:reportStatus IS NULL OR r.reportStatus = :reportStatus)")
    Page<Report> findAllByStatus(@Param("reportStatus") ReportStatus reportStatus, Pageable pageable);
}
