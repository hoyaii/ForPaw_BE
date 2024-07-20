package com.hong.ForPaw.domain.Report;

import com.hong.ForPaw.domain.TimeStamp;
import com.hong.ForPaw.domain.User.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "report_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Report extends TimeStamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offender_id")
    private User offender;

    @Column
    @Enumerated(EnumType.STRING)
    private ContentType contentType;

    @Column
    private Long contentId;

    @Column
    @Enumerated(EnumType.STRING)
    private ReportType type;

    @Column
    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    @Column
    private String reason;

    @Builder
    public Report(User reporter, User offender, ContentType contentType, Long contentId, ReportType type, ReportStatus status, String reason) {
        this.reporter = reporter;
        this.offender = offender;
        this.contentType = contentType;
        this.contentId = contentId;
        this.type = type;
        this.status = status;
        this.reason = reason;
    }

    public void updateStatus(ReportStatus status){
        this.status = status;
    }
}
