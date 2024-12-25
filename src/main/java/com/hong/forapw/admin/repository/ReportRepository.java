package com.hong.forapw.admin.repository;

import com.hong.forapw.admin.constant.ContentType;
import com.hong.forapw.admin.entity.Report;
import com.hong.forapw.admin.constant.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    @EntityGraph(attributePaths = {"offender"})
    @Query("SELECT r FROM Report r WHERE r.id = :id")
    Optional<Report> findByIdWithOffender(@Param("id") Long id);

    @Query("SELECT COUNT(r) > 0 FROM Report r " +
            "JOIN r.reporter rer " +
            "WHERE rer.id = :reporterId AND r.contentId = :contentId AND r.contentType = :contentType")
    boolean existsByReporterIdAndContentId(@Param("reporterId") Long reporterId, @Param("contentId") Long contentId, @Param("contentType") ContentType contentType);

    @EntityGraph(attributePaths = {"reporter"})
    @Query("SELECT r From Report r WHERE (:status IS NULL OR r.status = :status)")
    Page<Report> findAllByStatus(@Param("status") ReportStatus status, Pageable pageable);
}
