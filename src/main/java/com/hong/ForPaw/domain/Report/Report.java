package com.hong.ForPaw.domain.Report;

import com.hong.ForPaw.domain.Post.Comment;
import com.hong.ForPaw.domain.Post.Post;
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
    @JoinColumn(name = "user_id")
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @Column
    @Enumerated(EnumType.STRING)
    private ReportType type;

    @Column
    @Enumerated(EnumType.STRING)
    private ReportTargetType targetType;

    @Column
    @Enumerated(EnumType.STRING)
    private RepostStatus status;

    @Column
    private String reason;

    @Builder
    public Report(User reporter, Post post, Comment comment, ReportTargetType targetType, ReportType type, RepostStatus status, String reason) {
        this.reporter = reporter;
        this.post = post;
        this.comment = comment;
        this.targetType = targetType;
        this.type = type;
        this.status = status;
        this.reason = reason;
    }
}
