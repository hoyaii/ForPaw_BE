package com.hong.ForPaw.domain.Post;

import com.hong.ForPaw.domain.Group.Group;
import com.hong.ForPaw.domain.TimeStamp;
import com.hong.ForPaw.domain.User.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
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

    @Column
    @Enumerated(EnumType.STRING)
    private Type type;

    @Column
    private String title;

    @Column
    private String content;

    @Column
    private Integer commentNum = 0;

    @Column
    private Integer likeNum = 0;

    @Column
    private Integer viewNum = 0;

    @Builder
    public Post(User user, Group group, Type type, String title, String content) {
        this.user = user;
        this.group = group;
        this.type = type;
        this.title = title;
        this.content = content;
    }

    public void updatePost(String title, String content){
        this.title = title;
        this.content = content;
    }

    public void updateLikeNum(Integer likeNum){
        this.likeNum = likeNum;
    }

    public void updateCommentNum(Integer commentNum){
        this.commentNum = commentNum;
    }
}
