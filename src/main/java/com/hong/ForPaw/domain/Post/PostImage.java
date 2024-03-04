package com.hong.ForPaw.domain.Post;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@NoArgsConstructor(access =  AccessLevel.PROTECTED)
@Getter
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;

    @Column
    private String imageURL;

    @Builder
    public PostImage(Post post, String imageURL) {
        this.post = post;
        this.imageURL = imageURL;
    }
}