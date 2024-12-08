package com.hong.forapw.domain.post;

import com.hong.forapw.domain.group.Group;
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
@Table(name = "post_tb")
@SQLDelete(sql = "UPDATE post_tb SET removed_at = NOW() WHERE id=?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Post extends TimeStamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post parent;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 10)
    private List<PostImage> postImages = new ArrayList<>();

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 10)
    private List<Post> children = new ArrayList<>();

    @Column
    @Enumerated(EnumType.STRING)
    private PostType postType;

    @Column
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column
    private Long answerNum = 0L;

    @Column
    private Long commentNum = 0L;

    @Column
    private Long readCnt = 0L;

    @Column
    private Double hotPoint = 0.0;

    @Column
    private boolean isBlocked;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    @Builder
    public Post(User user, Group group, PostType postType, String title, String content) {
        this.user = user;
        this.group = group;
        this.postType = postType;
        this.title = title;
        this.content = content;
        this.isBlocked = false;
    }

    // 연관관계 메서드
    public void addImage(PostImage postImage) {
        this.postImages.add(postImage);
        postImage.updatePost(this);
    }

    public void addChildPost(Post child) {
        this.children.add(child);
        child.updateParent(this);
    }

    public void updateTitleAndContent(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateParent(Post parent) {
        this.parent = parent;
    }

    public void updateReadCnt(Long readCnt) {
        this.readCnt = readCnt;
    }

    public void updateHotPoint(Double hotPoint) {
        this.hotPoint = hotPoint;
    }

    public void processBlock() {
        this.isBlocked = true;
    }

    public void incrementAnswerNum() {
        this.answerNum++;
    }

    public boolean isNotQuestionType() {
        return postType != PostType.QUESTION;
    }

    public String getFirstImageURL() {
        return postImages.isEmpty() ? null : postImages.get(0).getImageURL();
    }

    public String getWriterNickName(){
        return user.getNickname();
    }

    public String getWriterProfileURL(){
        return user.getProfileURL();
    }

    public String getPostTypeString(){
        return postType.toString().toLowerCase();
    }
}