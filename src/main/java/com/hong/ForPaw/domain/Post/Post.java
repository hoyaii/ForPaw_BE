package com.hong.ForPaw.domain.Post;

import com.hong.ForPaw.domain.Group.Group;
import com.hong.ForPaw.domain.TimeStamp;
import com.hong.ForPaw.domain.User.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostImage> postImages = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> children = new ArrayList<>();

    @Column
    @Enumerated(EnumType.STRING)
    private PostType postType;

    @Column
    private String title;

    @Column
    private String content;

    @Column
    private Integer answerNum = 0;

    @Column
    private Integer commentNum = 0;

    @Column
    private Integer likeNum = 0;

    @Builder
    public Post(User user, Group group, PostType postType, String title, String content) {
        this.user = user;
        this.group = group;
        this.postType = postType;
        this.title = title;
        this.content = content;
    }

    public void updatePost(String title, String content){
        this.title = title;
        this.content = content;
    }

    // 연관관계 메서드
    public void addImage(PostImage postImage){
        postImages.add(postImage);
        postImage.setPost(this);
    }

    public void addChildPost(Post child){
        this.children.add(child);
        child.updateParent(this);
    }

    public void updateParent(Post parent){
        this.parent =parent;
    }
}