package com.hong.forapw.domain.post;

import com.hong.forapw.domain.TimeStamp;
import com.hong.forapw.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLDelete;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comment_tb")
@SQLDelete(sql = "UPDATE comment_tb SET removed_at = NOW() WHERE id=?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Comment extends TimeStamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id") // 부모 댓글을 가리키는 외래키
    private Comment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<Comment> children = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    // 연관 관계 메서드
    public void addChildComment(Comment child) {
        this.children.add(child);
        child.updateParent(this);
    }

    public void updateParent(Comment parent) {
        this.parent = parent;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    @Builder
    public Comment(User user, Post post, String content) {
        this.user = user;
        this.post = post;
        this.content = content;
    }
}