package com.hong.forapw.service;

import com.hong.forapw.controller.dto.AlarmRequest;
import com.hong.forapw.controller.dto.PostRequest;
import com.hong.forapw.controller.dto.PostResponse;
import com.hong.forapw.core.errors.CustomException;
import com.hong.forapw.core.errors.ExceptionCode;
import com.hong.forapw.core.utils.mapper.PostMapper;
import com.hong.forapw.domain.alarm.AlarmType;
import com.hong.forapw.domain.post.*;
import com.hong.forapw.domain.report.ContentType;
import com.hong.forapw.domain.report.Report;
import com.hong.forapw.domain.report.ReportStatus;
import com.hong.forapw.domain.user.User;
import com.hong.forapw.repository.post.*;
import com.hong.forapw.repository.ReportRepository;
import com.hong.forapw.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

import static com.hong.forapw.core.utils.mapper.PostMapper.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final ReportRepository reportRepository;
    private final PopularPostRepository popularPostRepository;
    private final UserRepository userRepository;
    private final PostCacheService postCacheService;
    private final S3Service s3Service;
    private final BrokerService brokerService;
    private final EntityManager entityManager;

    private static final String POST_SCREENED = "이 게시글은 커뮤니티 규정을 위반하여 숨겨졌습니다.";
    private static final String COMMENT_DELETED = "삭제된 댓글 입니다.";

    @Transactional
    public PostResponse.CreatePostDTO createPost(PostRequest.CreatePostDTO requestDTO, Long userId) {
        validatePostRequest(requestDTO);

        List<PostImage> postImages = buildPostImages(requestDTO.images());
        Post post = buildPost(userId, requestDTO);
        setPostRelationships(post, postImages);
        postRepository.save(post);

        postCacheService.initializePostCache(post.getId());

        return new PostResponse.CreatePostDTO(post.getId());
    }

    @Transactional
    public PostResponse.CreateAnswerDTO createAnswer(PostRequest.CreateAnswerDTO requestDTO, Long questionPostId, Long userId) {
        Post questionPost = postRepository.findByIdWithUser(questionPostId).orElseThrow(
                () -> new CustomException(ExceptionCode.POST_NOT_FOUND)
        );
        validateQuestionPostType(questionPost);

        List<PostImage> answerImages = buildPostImages(requestDTO.images());
        Post answerPost = buildAnswerPost(questionPost, userId, requestDTO);
        setAnswerPostRelationships(answerPost, answerImages, questionPost);
        postRepository.save(answerPost);

        questionPost.incrementAnswerNum();
        sendNewAnswerAlarm(questionPost, requestDTO.content(), questionPostId);

        return new PostResponse.CreateAnswerDTO(answerPost.getId());
    }

    @Transactional(readOnly = true)
    public PostResponse.FindPostListDTO findPostsByType(Pageable pageable, PostType postType) {
        Page<Post> postPage = postRepository.findByPostTypeWithUser(postType, pageable);
        List<PostResponse.PostDTO> postDTOS = postPage.getContent().stream()
                .map(post -> toPostDTO(post, postCacheService.getPostLikeCount(post.getId())))
                .toList();

        return new PostResponse.FindPostListDTO(postDTOS, postPage.isLast());
    }

    @Transactional(readOnly = true)
    public PostResponse.FindPostListDTO findPopularPostsByType(Pageable pageable, PostType postType) {
        Page<PopularPost> popularPostPage = popularPostRepository.findByPostTypeWithPost(postType, pageable);
        List<PostResponse.PostDTO> postDTOS = popularPostPage.getContent().stream()
                .map(PopularPost::getPost)
                .map(post -> toPostDTO(post, postCacheService.getPostLikeCount(post.getId())))
                .toList();

        return new PostResponse.FindPostListDTO(postDTOS, popularPostPage.isLast());
    }

    @Transactional(readOnly = true)
    public PostResponse.FindQnaListDTO findQuestions(Pageable pageable) {
        Page<Post> questionPage = postRepository.findByPostTypeWithUser(PostType.QUESTION, pageable);
        List<PostResponse.QnaDTO> qnaDTOS = questionPage.getContent().stream()
                .map(PostMapper::toQnaDTO)
                .toList();

        return new PostResponse.FindQnaListDTO(qnaDTOS, questionPage.isLast());
    }

    @Transactional(readOnly = true)
    public PostResponse.FindMyPostListDTO findMyPosts(Long userId, Pageable pageable) {
        List<PostType> postTypes = List.of(PostType.ADOPTION, PostType.FOSTERING);
        Page<Post> postPage = postRepository.findPostsByUserIdAndTypesWithUser(userId, postTypes, pageable);
        List<PostResponse.MyPostDTO> postDTOS = postPage.getContent().stream()
                .map(post -> toMyPostDTO(post, postCacheService.getPostLikeCount(post.getId())))
                .toList();

        return new PostResponse.FindMyPostListDTO(postDTOS, postPage.isLast());
    }

    @Transactional(readOnly = true)
    public PostResponse.FindQnaListDTO findMyQuestions(Long userId, Pageable pageable) {
        List<PostType> postTypes = List.of(PostType.QUESTION);
        Page<Post> questionPage = postRepository.findPostsByUserIdAndTypesWithUser(userId, postTypes, pageable);
        List<PostResponse.QnaDTO> qnaDTOS = questionPage.getContent().stream()
                .map(PostMapper::toQnaDTO)
                .toList();

        return new PostResponse.FindQnaListDTO(qnaDTOS, questionPage.isLast());
    }

    @Transactional(readOnly = true)
    public PostResponse.FindQnaListDTO findQuestionsAnsweredByMe(Long userId, Pageable pageable) {
        Page<Post> questionPage = postRepository.findQnaOfAnswerByUserIdWithUser(userId, pageable);
        Set<Post> question = new HashSet<>(questionPage.getContent()); // 중복 제거
        List<PostResponse.QnaDTO> qnaDTOS = question.stream()
                .map(PostMapper::toQnaDTO)
                .toList();

        return new PostResponse.FindQnaListDTO(qnaDTOS, questionPage.isLast());
    }

    @Transactional(readOnly = true)
    public PostResponse.FindMyCommentListDTO findMyComments(Long userId, Pageable pageable) {
        Page<Comment> myCommentPage = commentRepository.findByUserIdWithPost(userId, pageable);
        List<PostResponse.MyCommentDTO> myCommentDTOS = myCommentPage.getContent().stream()
                .map(PostMapper::toMyCommentDTO)
                .toList();

        return new PostResponse.FindMyCommentListDTO(myCommentDTOS, myCommentPage.isLast());
    }

    @Transactional(readOnly = true)
    public PostResponse.FindPostByIdDTO findPostById(Long postId, Long userId) {
        Post post = postRepository.findByIdWithUser(postId).orElseThrow(
                () -> new CustomException(ExceptionCode.POST_NOT_FOUND)
        );
        validatePost(post);

        List<Comment> comments = commentRepository.findByPostIdWithUserAndParentAndRemoved(postId);
        List<Long> likedCommentIds = commentLikeRepository.findCommentIdsByUserId(userId);
        List<PostResponse.CommentDTO> commentDTOS = convertToCommentDTO(comments, likedCommentIds);
        List<PostResponse.PostImageDTO> postImageDTOS = toPostImageDTOs(post);

        postCacheService.incrementPostViewCount(postId);
        postCacheService.markNoticePostAsRead(post, userId, postId);

        return new PostResponse.FindPostByIdDTO(post.getUser().getNickname(), post.getUser().getProfileURL(), post.getTitle(), post.getContent(), post.getCreatedDate(), post.getCommentNum(), postCacheService.getPostLikeCount(postId), post.isOwner(userId), isPostLiked(postId, userId), postImageDTOS, commentDTOS);
    }

    @Transactional(readOnly = true)
    public PostResponse.FindQnaByIdDTO findQnaById(Long qnaId, Long userId) {
        Post qna = postRepository.findById(qnaId).orElseThrow(
                () -> new CustomException(ExceptionCode.POST_NOT_FOUND)
        );
        validateQna(qna);

        List<Post> answers = postRepository.findByParentIdWithUser(qnaId);
        List<PostResponse.AnswerDTO> answerDTOS = toAnswerDTOs(answers, userId);
        List<PostResponse.PostImageDTO> qnaImageDTOS =toPostImageDTOs(qna);

        postCacheService.incrementPostViewCount(qnaId);

        return new PostResponse.FindQnaByIdDTO(qna.getWriterNickName(), qna.getWriterProfileURL(), qna.getTitle(), qna.getContent(), qna.getCreatedDate(), qnaImageDTOS, answerDTOS, qna.isOwner(userId));
    }

    @Transactional(readOnly = true)
    public PostResponse.FindAnswerByIdDTO findAnswerById(Long postId, Long userId) {
        Post answer = postRepository.findById(postId).orElseThrow(
                () -> new CustomException(ExceptionCode.POST_NOT_FOUND)
        );
        validateAnswer(answer);

        List<PostResponse.PostImageDTO> postImageDTOS =toPostImageDTOs(answer);
        return new PostResponse.FindAnswerByIdDTO(answer.getWriterNickName(), answer.getContent(), answer.getCreatedDate(), postImageDTOS, answer.isOwner(userId));
    }

    @Transactional
    public void updatePost(PostRequest.UpdatePostDTO requestDTO, User user, Long postId) {
        Post post = postRepository.findByIdWithUser(postId).orElseThrow(
                () -> new CustomException(ExceptionCode.POST_NOT_FOUND)
        );
        checkAccessorAuthority(user, post.getWriterId());

        post.updateContent(requestDTO.title(), requestDTO.content());

        removeUnretainedImages(requestDTO.retainedImageIds(), postId);
        saveNewPostImages(requestDTO.newImages(), post);
    }

    @Transactional
    public void deletePost(Long postId, User user) {
        Post post = postRepository.findByIdWithUserAndParent(postId).orElseThrow(
                () -> new CustomException(ExceptionCode.POST_NOT_FOUND)
        );
        checkAccessorAuthority(user, post.getWriterId());

        postLikeRepository.deleteAllByPostId(postId);
        commentLikeRepository.deleteByPostId(postId);
        popularPostRepository.deleteByPostId(postId);
        postImageRepository.deleteByPostId(postId);
        commentRepository.deleteByPostId(postId); // soft-delete
        postRepository.delete(post); // soft-delete
    }

    @Transactional
    public void deleteAnswer(Long answerId, User user) {
        Post answer = postRepository.findByIdWithUserAndParent(answerId).orElseThrow(
                () -> new CustomException(ExceptionCode.POST_NOT_FOUND)
        );
        validateAnswer(answer);
        checkAccessorAuthority(user, answer.getWriterId());

        decrementAnswerCount(answer);
        postImageRepository.deleteByPostId(answerId);
        postRepository.deleteById(answerId);
    }

    @Transactional
    public PostResponse.CreateCommentDTO createComment(PostRequest.CreateCommentDTO requestDTO, Long userId, Long postId) {
        Post post = postRepository.findByIdWithUser(postId).orElseThrow(
                () -> new CustomException(ExceptionCode.POST_NOT_FOUND)
        );
        validatePostType(post);

        Comment comment = buildComment(requestDTO.content(), postId, userId);
        commentRepository.save(comment);

        incrementCommentCount(postId);
        postCacheService.initializeCommentCache(comment.getId());
        sendNewCommentAlarm(requestDTO.content(), postId, post.getPostType(), post.getWriterId());

        return new PostResponse.CreateCommentDTO(comment.getId());
    }

    @Transactional
    public PostResponse.CreateCommentDTO createReply(PostRequest.CreateCommentDTO requestDTO, Long postId, Long userId, Long parentCommentId) {
        Comment parentComment = commentRepository.findByIdWithPost(parentCommentId).orElseThrow(
                () -> new CustomException(ExceptionCode.COMMENT_NOT_FOUND)
        );
        validateParentComment(parentComment, userId);

        Comment reply = buildComment(requestDTO.content(), postId, userId);
        parentComment.addChildComment(reply);
        commentRepository.save(reply);

        incrementCommentCount(postId);
        postCacheService.initializeCommentCache(reply.getId());
        sendNewReplyAlarm(requestDTO.content(), postId, parentComment);

        return new PostResponse.CreateCommentDTO(reply.getId());
    }

    @Transactional
    public void updateComment(PostRequest.UpdateCommentDTO requestDTO, Long postId, Long commentId, User user) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new CustomException(ExceptionCode.COMMENT_NOT_FOUND)
        );
        validateCommentBelongsToPost(comment, postId);
        checkAccessorAuthority(user, comment.getWriterId());

        comment.updateContent(requestDTO.content());
    }

    @Transactional
    public void deleteComment(Long postId, Long commentId, User user) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new CustomException(ExceptionCode.COMMENT_NOT_FOUND)
        );
        validateCommentBelongsToPost(comment, postId);
        checkAccessorAuthority(user, comment.getWriterId());

        comment.updateContent(COMMENT_DELETED);
        commentRepository.deleteById(commentId);
        commentLikeRepository.deleteAllByCommentId(commentId);

        decrementCommentCount(comment, postId);
    }

    @Transactional
    public void submitReport(PostRequest.SubmitReport requestDTO, Long userId) {
        User reporter = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );
        validateReportRequest(requestDTO.contentId(), requestDTO.contentType(), userId);

        User reportedUser = findOffender(requestDTO);
        validateNotSelfReport(userId, reportedUser);

        Report report = buildReport(requestDTO, reporter, reportedUser);
        reportRepository.save(report);
    }

    @Scheduled(cron = "0 0 6,18 * * *")
    @Transactional
    public void updateTodayPopularPosts() {
        LocalDate now = LocalDate.now();
        LocalDateTime startOfToday = now.atStartOfDay();
        LocalDateTime endOfToday = now.atTime(LocalTime.MAX);

        List<Post> adoptionPosts = postRepository.findByDateAndType(startOfToday, endOfToday, PostType.ADOPTION);
        processPopularPosts(adoptionPosts, PostType.ADOPTION);

        List<Post> fosteringPosts = postRepository.findByDateAndType(startOfToday, endOfToday, PostType.FOSTERING);
        processPopularPosts(fosteringPosts, PostType.FOSTERING);
    }

    @Scheduled(cron = "0 25 0 * * *")
    @Transactional
    public void syncViewNum() {
        LocalDateTime oneWeeksAgo = LocalDateTime.now().minusWeeks(1);
        List<Post> posts = postRepository.findPostIdsWithinDate(oneWeeksAgo);

        for (Post post : posts) {
            Long readCnt = postCacheService.getPostViewCount(post);

            if (readCnt != null) {
                post.updateReadCnt(readCnt);
            }
        }
    }

    private void validatePostRequest(PostRequest.CreatePostDTO requestDTO) {
        if (requestDTO.type() == PostType.ANSWER) {
            throw new CustomException(ExceptionCode.NOT_QUESTION_TYPE);
        }

        if (requestDTO.type().isImageRequired() && requestDTO.images().isEmpty()) {
            throw new CustomException(ExceptionCode.POST_MUST_CONTAIN_IMAGE);
        }
    }

    private Post buildPost(Long userId, PostRequest.CreatePostDTO requestDTO) {
        User userRef = entityManager.getReference(User.class, userId);
        return Post.builder()
                .user(userRef)
                .postType(requestDTO.type())
                .title(requestDTO.title())
                .content(requestDTO.content())
                .build();
    }

    private void setPostRelationships(Post post, List<PostImage> postImages) {
        postImages.forEach(post::addImage);
    }

    private List<PostImage> buildPostImages(List<PostRequest.PostImageDTO> imageDTOs) {
        return imageDTOs.stream()
                .map(postImageDTO -> PostImage.builder()
                        .imageURL(postImageDTO.imageURL())
                        .build())
                .toList();
    }

    private void validateQuestionPostType(Post questionPost) {
        if (questionPost.isNotQuestionType()) {
            throw new CustomException(ExceptionCode.NOT_QUESTION_TYPE);
        }
    }

    private Post buildAnswerPost(Post questionPost, Long userId, PostRequest.CreateAnswerDTO requestDTO) {
        User userRef = entityManager.getReference(User.class, userId);
        return Post.builder()
                .user(userRef)
                .postType(PostType.ANSWER)
                .title(questionPost.getTitle() + "(답변)")
                .content(requestDTO.content())
                .build();
    }

    private void setAnswerPostRelationships(Post answerPost, List<PostImage> answerImages, Post questionPost) {
        answerImages.forEach(answerPost::addImage); // 이미지와 답변 게시물의 연관 설정
        questionPost.addChildPost(answerPost); // 질문 게시물과 답변 게시물의 부모-자식 관계 설정
    }

    private void sendNewAnswerAlarm(Post questionPost, String answerContent, Long questionPostId) {
        String content = "새로운 답변: " + answerContent;
        String redirectURL = "/community/question/" + questionPostId;

        createAlarm(questionPost.getUser().getId(), content, redirectURL, AlarmType.ANSWER);
    }

    private void validatePost(Post post) {
        if (post.isQuestionType()) {
            throw new CustomException(ExceptionCode.NOT_QUESTION_TYPE);
        }

        if (post.isScreened()) {
            throw new CustomException(ExceptionCode.SCREENED_POST);
        }
    }

    private boolean isPostLiked(Long postId, Long userId) {
        return postLikeRepository.existsByPostIdAndUserId(postId, userId);
    }

    private void validateQna(Post qna) {
        if (qna.isNotQuestionType()) {
            throw new CustomException(ExceptionCode.NOT_QUESTION_TYPE);
        }

        if (qna.isScreened()) {
            throw new CustomException(ExceptionCode.SCREENED_POST);
        }
    }

    private void validateAnswer(Post answer) {
        if (answer.isNotAnswerType()) {
            throw new CustomException(ExceptionCode.NOT_ANSWER_TYPE);
        }
    }

    private void removeUnretainedImages(List<Long> retainedImageIds, Long postId) {
        if (retainedImageIds != null && !retainedImageIds.isEmpty()) {
            postImageRepository.deleteByPostIdAndIdNotIn(postId, retainedImageIds);
        } else {
            postImageRepository.deleteByPostId(postId);
        }
    }

    private void saveNewPostImages(List<PostRequest.PostImageDTO> newImageDTOs, Post post) {
        List<PostImage> newPostImages = newImageDTOs.stream()
                .map(request -> PostImage.builder()
                        .post(post)
                        .imageURL(request.imageURL())
                        .build())
                .collect(Collectors.toList());

        postImageRepository.saveAll(newPostImages);
    }

    private void decrementAnswerCount(Post answer) {
        Post question = answer.getParent();
        if (question != null) {
            postRepository.decrementAnswerNum(question.getId());
        }
    }

    private void validatePostType(Post post) {
        if (post.isQuestionType()) {
            throw new CustomException(ExceptionCode.NOT_QUESTION_TYPE);
        }
    }

    private Comment buildComment(String content, Long postId, Long userId) {
        Post postReference = entityManager.getReference(Post.class, postId);
        User userReference = entityManager.getReference(User.class, userId);
        return Comment.builder()
                .user(userReference)
                .post(postReference)
                .content(content)
                .build();
    }

    private void incrementCommentCount(Long postId) {
        postRepository.incrementCommentNum(postId);
    }

    private void sendNewCommentAlarm(String commentContent, Long postId, PostType postType, Long writerId) {
        String content = "새로운 댓글: " + commentContent;
        String queryParam = postType.name().toLowerCase();
        String redirectURL = "/community/" + postId + "?type=" + queryParam;
        createAlarm(writerId, content, redirectURL, AlarmType.COMMENT);
    }

    private void validateParentComment(Comment parentComment, Long postId) {
        if (parentComment.isReply()) {
            throw new CustomException(ExceptionCode.CANT_REPLY_TO_REPLY);
        }

        if (parentComment.isNotBelongToPost(postId)) {
            throw new CustomException(ExceptionCode.NOT_POSTS_COMMENT);
        }
    }

    private void sendNewReplyAlarm(String replyContent, Long postId, Comment parentComment) {
        String content = "새로운 대댓글: " + replyContent;
        String queryParam = parentComment.getPostTypeName().toLowerCase();
        String redirectURL = "/community/" + postId + "?type=" + queryParam;
        createAlarm(parentComment.getWriterId(), content, redirectURL, AlarmType.COMMENT);
    }

    private void decrementCommentCount(Comment comment, Long postId) {
        long replyCount = comment.getReplyCount();
        postRepository.decrementCommentNum(postId, 1L + replyCount);
    }

    private void validateReportRequest(Long contentId, ContentType contentType, Long userId) {
        if (reportRepository.existsByReporterIdAndContentId(userId, contentId, contentType)) {
            throw new CustomException(ExceptionCode.ALREADY_REPORTED);
        }

        if (contentType.isNotValidTypeForReport()) {
            throw new CustomException(ExceptionCode.WRONG_REPORT_TARGET);
        }
    }

    private Report buildReport(PostRequest.SubmitReport requestDTO, User reporter, User reportedUser) {
        return Report.builder()
                .reporter(reporter)
                .offender(reportedUser)
                .contentType(requestDTO.contentType())
                .contentId(requestDTO.contentId())
                .type(requestDTO.reportType())
                .status(ReportStatus.PROCESSING)
                .reason(requestDTO.reason())
                .build();
    }

    private User findOffender(PostRequest.SubmitReport requestDTO) {
        if (requestDTO.contentType() == ContentType.POST) {
            return postRepository.findUserById(requestDTO.contentId())
                    .orElseThrow(() -> new CustomException(ExceptionCode.POST_NOT_FOUND));
        } else if (requestDTO.contentType() == ContentType.COMMENT) {
            return commentRepository.findUserById(requestDTO.contentId())
                    .orElseThrow(() -> new CustomException(ExceptionCode.COMMENT_NOT_FOUND));
        }
        throw new CustomException(ExceptionCode.WRONG_REPORT_TARGET);
    }

    private void validateNotSelfReport(Long userId, User reportedUser) {
        if (reportedUser.isSameUser(userId)) {
            throw new CustomException(ExceptionCode.CANNOT_REPORT_OWN_CONTENT);
        }
    }

    private void processPopularPosts(List<Post> posts, PostType postType) {
        posts.forEach(this::updatePostHotPoint);

        List<Post> popularPosts = selectPopularPosts(posts);
        fillPopularPostsIfNecessary(posts, popularPosts);
        savePopularPosts(popularPosts, postType);
    }

    private void updatePostHotPoint(Post post) {
        double hotPoint = calculateHotPoint(post);
        post.updateHotPoint(hotPoint);
    }

    private double calculateHotPoint(Post post) {
        double viewPoints = postCacheService.getPostViewCount(post.getId(), post) * 0.001;
        double commentPoints = post.getCommentNum();
        double likePoints = postCacheService.getPostLikeCount(post.getId()) * 5;
        return viewPoints + commentPoints + likePoints;
    }

    private List<Post> selectPopularPosts(List<Post> posts) {
        return posts.stream()
                .filter(post -> post.getHotPoint() > 10.0)
                .collect(Collectors.toList());
    }

    private void fillPopularPostsIfNecessary(List<Post> allPosts, List<Post> popularPosts) {
        if (popularPosts.size() >= 5) return;
        List<Post> remainingPosts = allPosts.stream()
                .filter(post -> !popularPosts.contains(post))
                .sorted(Comparator.comparingDouble(Post::getHotPoint).reversed())
                .toList();

        popularPosts.addAll(remainingPosts.stream()
                .limit(5 - popularPosts.size())
                .toList());
    }

    private void savePopularPosts(List<Post> popularPosts, PostType postType) {
        popularPosts.forEach(post -> {
            PopularPost popularPost = PopularPost.builder()
                    .post(post)
                    .postType(postType)
                    .build();
            popularPostRepository.save(popularPost);
        });
    }

    private void checkAccessorAuthority(User accessor, Long writerId) {
        if (accessor.isAdmin()) {
            return;
        }
        if (accessor.isNotSameUser(writerId)) {
            throw new CustomException(ExceptionCode.USER_FORBIDDEN);
        }
    }

    private void checkExpiration(LocalDateTime date) {
        LocalDate likeDate = date.toLocalDate();
        LocalDate now = LocalDate.now();
        Period period = Period.between(likeDate, now);

        // 기간이 3개월 이상이면 좋아요를 할 수 없다
        if (period.getMonths() >= 3 || period.getYears() > 0) {
            throw new CustomException(ExceptionCode.POST_LIKE_EXPIRED);
        }
    }

    private void validateCommentBelongsToPost(Comment comment, Long postId) {
        if (comment.isNotBelongToPost(postId)) {
            throw new CustomException(ExceptionCode.NOT_POSTS_COMMENT);
        }
    }

    private List<PostResponse.CommentDTO> convertToCommentDTO(List<Comment> comments, List<Long> likedCommentIds) {
        List<PostResponse.CommentDTO> parentComments = new ArrayList<>();
        Map<Long, PostResponse.CommentDTO> parentCommentMap = new HashMap<>(); // ParentComment Id로 빠르게 ParentComment를 찾을 용도

        comments.forEach(comment -> {
            Long likeCount = postCacheService.getCommentLikeCount(comment.getId());
            if (comment.isNotReply()) {
                PostResponse.CommentDTO parentCommentDTO = toParentCommentDTO(comment, likeCount, likedCommentIds.contains(comment.getId()));
                parentComments.add(parentCommentDTO);
                parentCommentMap.put(comment.getId(), parentCommentDTO);
            } else {
                handleChildComment(comment, parentCommentMap, likedCommentIds, likeCount);
            }
        });

        return parentComments;
    }

    private void handleChildComment(Comment childComment, Map<Long, PostResponse.CommentDTO> parentCommentMap, List<Long> likedCommentIds, Long likeCount) {
        if (childComment.isDeleted() && isLastChildComment(childComment)) {
            return; // 삭제된 댓글이 마지막 대댓글이면, 보이지 않고. 반면, 마지막 대댓글이 아니면, '삭제된 댓글입니다' 처리
        }

        PostResponse.CommentDTO parentCommentDTO = parentCommentMap.get(childComment.getParentId());
        PostResponse.ReplyDTO childCommentDTO = toReplyDTO(childComment, likedCommentIds.contains(childComment.getId()), likeCount);
        parentCommentDTO.replies().add(childCommentDTO);
    }

    private boolean isLastChildComment(Comment comment) {
        return !commentRepository.existsByParentIdAndDateAfter(comment.getParent().getId(), comment.getCreatedDate());
    }

    private void createAlarm(Long userId, String content, String redirectURL, AlarmType alarmType) {
        AlarmRequest.AlarmDTO alarmDTO = new AlarmRequest.AlarmDTO(
                userId,
                content,
                redirectURL,
                LocalDateTime.now(),
                alarmType);
        brokerService.produceAlarmToUser(userId, alarmDTO);
    }

    private void deleteImagesInS3(List<Long> retainedImageIds, List<PostImage> postImages) {
        postImages.stream()
                .filter(postImage -> !retainedImageIds.contains(postImage.getId()))
                .forEach(postImage -> {
                    String objectKey = s3Service.extractKeyFromUrl(postImage.getImageURL());
                    s3Service.deleteObject(objectKey);
                });
    }
}