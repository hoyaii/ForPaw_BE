package com.hong.forapw.domain.post.service;

import com.hong.forapw.domain.post.entity.Post;
import com.hong.forapw.domain.post.constant.PostType;
import com.hong.forapw.domain.post.repository.PostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PostScheduledService {

    private final PostRepository postRepository;
    private final PostService postService;
    private final PostCacheService postCacheService;

    /** 매일 06:00, 18:00에 실행되어 오늘의 인기 게시글을 갱신 */
    @Scheduled(cron = "0 0 6,18 * * *")
    public void updateTodayPopularPosts() {
        LocalDate now = LocalDate.now();
        LocalDateTime startOfToday = now.atStartOfDay();
        LocalDateTime endOfToday = now.atTime(LocalTime.MAX);

        postService.refreshPopularPostsWithinRange(startOfToday, endOfToday, PostType.ADOPTION);
        postService.refreshPopularPostsWithinRange(startOfToday, endOfToday, PostType.FOSTERING);
    }

    /** 매일 00:25에 실행되어 Redis에 저장된 조회수를 DB와 동기화 */
    @Scheduled(cron = "0 25 0 * * *")
    public void syncViewNum() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        List<Post> posts = postRepository.findPostIdsWithinDate(oneWeekAgo);

        for (Post post : posts) {
            Long readCount = postCacheService.getPostViewCount(post);

            if (readCount != null) {
                post.updateReadCnt(readCount); // 조회수 갱신
            }
        }
    }
}
