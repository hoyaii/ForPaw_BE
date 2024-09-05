package com.hong.ForPaw.service;

import com.hong.ForPaw.controller.DTO.AlarmRequest;
import com.hong.ForPaw.controller.DTO.PostRequest;
import com.hong.ForPaw.controller.DTO.PostResponse;
import com.hong.ForPaw.core.errors.CustomException;
import com.hong.ForPaw.core.errors.ExceptionCode;
import com.hong.ForPaw.domain.Alarm.AlarmType;
import com.hong.ForPaw.domain.Post.*;
import com.hong.ForPaw.domain.Report.ContentType;
import com.hong.ForPaw.domain.Report.Report;
import com.hong.ForPaw.domain.Report.ReportStatus;
import com.hong.ForPaw.domain.User.UserRole;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.repository.Post.*;
import com.hong.ForPaw.repository.ReportRepository;
import com.hong.ForPaw.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
    private final RedisService redisService;
    private final S3Service s3Service;
    private final BrokerService brokerService;
    private final EntityManager entityManager;
    public static final Long POST_EXP = 1000L * 60 * 60 * 24 * 90; // 세 달
    public static final Long POST_READ_EXP = 60L * 60 * 24 * 360; // 1년
    private static final String POST_SCREENED = "이 게시글은 커뮤니티 규정을 위반하여 숨겨졌습니다.";
    private static final String POST_READ_KEY_PREFIX = "user:readPosts:";
    private static final String POST_LIKE_NUM_KEY_PREFIX = "postLikeNum";
    private static final String POST_VIEW_NUM_PREFIX = "postViewNum";
    private static final String COMMENT_LIKE_NUM_KEY_PREFIX = "commentLikeNum";
    private static final String COMMENT_DELETED = "삭제된 댓글 입니다.";

    @Transactional
    public PostResponse.CreatePostDTO createPost(PostRequest.CreatePostDTO requestDTO, Long userId){
        User userRef = entityManager.getReference(User.class, userId);

        // 질문글이면 에러
        if(requestDTO.type() == PostType.ANSWER){
            throw new CustomException(ExceptionCode.IS_QUESTION_TYPE);
        }

        // 입양 스토리와 임시보호 스토리는 image가 반드시 하나 이상 들어와야 함
        if (requestDTO.images().isEmpty() && requestDTO.type() != PostType.QUESTION) {
            throw new CustomException(ExceptionCode.POST_MUST_CONTAIN_IMAGE);
        }

        List<PostImage> postImages = requestDTO.images().stream()
                .map(postImageDTO -> PostImage.builder()
                        .imageURL(postImageDTO.imageURL())
                        .build())
                .toList();

        // Post 객체 저장
        Post post = Post.builder()
                .user(userRef)
                .postType(requestDTO.type())
                .title(requestDTO.title())
                .content(requestDTO.content())
                .build();

        postImages.forEach(post::addImage); // 연관관계 설정

        postRepository.save(post);

        // 3개월 동안만 좋아요를 할 수 있다
        redisService.storeValue(POST_LIKE_NUM_KEY_PREFIX, post.getId().toString(), "0", POST_EXP);

        // 조회 수
        redisService.storeValue(POST_VIEW_NUM_PREFIX, post.getId().toString(), "0", POST_EXP);

        return new PostResponse.CreatePostDTO(post.getId());
    }

    @Transactional
    public PostResponse.CreateAnswerDTO createAnswer(PostRequest.CreateAnswerDTO requestDTO, Long parentPostId, Long userId){
        // 존재하지 않는 질문글에 답변을 달려고 하면 에러
        Post parentPost = postRepository.findByIdWithUser(parentPostId).orElseThrow(
                () -> new CustomException(ExceptionCode.POST_NOT_FOUND)
        );

        // 질문글에만 답변을 달 수 있다
        if(!parentPost.getPostType().equals(PostType.QUESTION)){
            throw new CustomException(ExceptionCode.NOT_QUESTION_TYPE);
        }

        User userRef = entityManager.getReference(User.class, userId);

        List<PostImage> postImages = requestDTO.images().stream()
                .map(postImageDTO -> PostImage.builder()
                        .imageURL(postImageDTO.imageURL())
                        .build())
                .toList();

        // Post 객체 저장
        Post post = Post.builder()
                .user(userRef)
                .postType(PostType.ANSWER)
                .title(parentPost.getTitle() + "(답변)")
                .content(requestDTO.content())
                .build();

        postImages.forEach(post::addImage); // 연관관계 설정
        parentPost.addChildPost(post);

        postRepository.save(post);

        // 답변수 증가
        postRepository.incrementAnswerNum(parentPostId);

        // 알림 생성
        String content = "새로운 답변: " + requestDTO.content();
        String redirectURL = "/community/question/" + parentPostId;
        createAlarm(parentPost.getUser().getId(), content, redirectURL, AlarmType.ANSWER);

        return new PostResponse.CreateAnswerDTO(post.getId());
    }

    @Transactional(readOnly = true)
    public PostResponse.FindPostListDTO findPostListByType(Pageable pageable, PostType postType) {
        // 유저를 패치조인하여 조회
        Page<Post> postPage = postRepository.findByPostTypeWithUser(postType, pageable);

        List<PostResponse.PostDTO> postDTOS = postPage.getContent().stream()
                .map(post -> {
                    String imageURL = post.getPostImages().isEmpty() ? null : post.getPostImages().get(0).getImageURL();
                    Long likeNum = getCachedPostLikeNum(POST_LIKE_NUM_KEY_PREFIX, post.getId());

                    return new PostResponse.PostDTO(
                            post.getId(),
                            post.getUser().getNickName(),
                            post.getTitle(),
                            post.getContent(),
                            post.getCreatedDate(),
                            post.getCommentNum(),
                            likeNum,
                            imageURL);
                })
                .toList();

        return new PostResponse.FindPostListDTO(postDTOS, postPage.isLast());
    }

    @Transactional(readOnly = true)
    public PostResponse.FindPostListDTO findPopularPostListByType(Pageable pageable, PostType postType) {
        // Post를 패치조인하여 조회
        Page<PopularPost> popularPostPage = popularPostRepository.findByPostTypeWithPost(postType, pageable);

        List<PostResponse.PostDTO> postDTOS = popularPostPage.getContent().stream()
                .map(PopularPost::getPost)
                .map(post -> {
                    String imageURL = post.getPostImages().isEmpty() ? null : post.getPostImages().get(0).getImageURL();
                    Long likeNum = getCachedPostLikeNum(POST_LIKE_NUM_KEY_PREFIX, post.getId());

                    return new PostResponse.PostDTO(
                            post.getId(),
                            post.getUser().getNickName(),
                            post.getTitle(),
                            post.getContent(),
                            post.getCreatedDate(),
                            post.getCommentNum(),
                            likeNum,
                            imageURL);
                })
                .toList();

        return new PostResponse.FindPostListDTO(postDTOS, popularPostPage.isLast());
    }

    @Transactional(readOnly = true)
    public PostResponse.FindQnaListDTO findQuestionList(Pageable pageable){
        // 유저를 패치조인하여 조회
        Page<Post> postPage = postRepository.findByPostTypeWithUser(PostType.QUESTION, pageable);

        List<PostResponse.QnaDTO> qnaDTOS = postPage.getContent().stream()
                .map(post -> new PostResponse.QnaDTO(
                        post.getId(),
                        post.getUser().getNickName(),
                        post.getUser().getProfileURL(),
                        post.getTitle(),
                        post.getContent(),
                        post.getCreatedDate(),
                        post.getAnswerNum()))
                .collect(Collectors.toList());

        return new PostResponse.FindQnaListDTO(qnaDTOS, postPage.isLast());
    }

    @Transactional(readOnly = true)
    public PostResponse.FindMyPostListDTO findMyPostList(Long userId, Pageable pageable){
        // 유저를 패치조인하여 조회
        List<PostType> postTypes = List.of(PostType.ADOPTION, PostType.FOSTERING);
        Page<Post> postPage = postRepository.findPostsByUserIdAndTypesWithUser(userId, postTypes, pageable);

        List<PostResponse.MyPostDTO> postDTOS = postPage.getContent().stream()
                .map(post -> {
                    String imageURL = post.getPostImages().isEmpty() ? null : post.getPostImages().get(0).getImageURL();
                    Long likeNum = getCachedPostLikeNum(POST_LIKE_NUM_KEY_PREFIX, post.getId());

                    return new PostResponse.MyPostDTO(
                            post.getId(),
                            post.getUser().getNickName(),
                            post.getTitle(),
                            post.getContent(),
                            post.getCreatedDate(),
                            post.getCommentNum(),
                            likeNum,
                            imageURL,
                            post.getPostType().toString().toLowerCase());
                })
                .toList();

        return new PostResponse.FindMyPostListDTO(postDTOS, postPage.isLast());
    }

    @Transactional(readOnly = true)
    public PostResponse.FindQnaListDTO findMyQuestionList(Long userId, Pageable pageable){
        // 유저를 패치조인하여 조회
        List<PostType> postTypes = List.of(PostType.QUESTION);
        Page<Post> postPage = postRepository.findPostsByUserIdAndTypesWithUser(userId, postTypes, pageable);

        List<PostResponse.QnaDTO> qnaDTOS = postPage.getContent().stream()
                .map(post -> new PostResponse.QnaDTO(
                        post.getId(),
                        post.getUser().getNickName(),
                        post.getUser().getProfileURL(),
                        post.getTitle(),
                        post.getContent(),
                        post.getCreatedDate(),
                        post.getAnswerNum()))
                .toList();

        return new PostResponse.FindQnaListDTO(qnaDTOS, postPage.isLast());
    }

    @Transactional(readOnly = true)
    public PostResponse.FindQnaListDTO findMyAnswerList(Long userId, Pageable pageable) {
        // 유저를 패치조인하여 조회
        Page<Post> postPage = postRepository.findQnaOfAnswerByUserIdWithUser(userId, pageable);

        // 중복 제거를 위해 Set을 사용하여 중복된 Post 객체를 필터링
        Set<Post> uniquePosts = new HashSet<>(postPage.getContent());

        List<PostResponse.QnaDTO> qnaDTOS = uniquePosts.stream()
                .map(post -> new PostResponse.QnaDTO(
                        post.getId(),
                        post.getUser().getNickName(),
                        post.getUser().getProfileURL(),
                        post.getTitle(),
                        post.getContent(),
                        post.getCreatedDate(),
                        post.getAnswerNum()))
                .toList();

        return new PostResponse.FindQnaListDTO(qnaDTOS, postPage.isLast());
    }

    @Transactional(readOnly = true)
    public PostResponse.FindMyCommentListDTO findMyCommentList(Long userId, Pageable pageable){
        // Post를 패치조인하여 조회
        Page<Comment> commentPage = commentRepository.findByUserIdWithPost(userId, pageable);

        List<PostResponse.MyCommentDTO> myCommentDTOS = commentPage.getContent().stream()
                .map(comment -> new PostResponse.MyCommentDTO(
                        comment.getId(),
                        comment.getPost().getId(),
                        comment.getPost().getPostType().getValue(),
                        comment.getContent(),
                        comment.getCreatedDate(),
                        comment.getPost().getTitle(),
                        comment.getPost().getCommentNum()
                ))
                .toList();

        return new PostResponse.FindMyCommentListDTO(myCommentDTOS, commentPage.isLast());
    }

    @Transactional(readOnly = true)
    public PostResponse.FindPostByIdDTO findPostById(Long postId, Long userId){
        // user를 패치조인 해서 조회
        Post post = postRepository.findByIdWithUser(postId).orElseThrow(
                () -> new CustomException(ExceptionCode.POST_NOT_FOUND)
        );

        boolean isMine = post.getUser().getId().equals(userId);

        // 질문글을 조회하기 위한 API 아님 => 질문글이면 에러 리턴
        if(post.getPostType().equals(PostType.QUESTION)){
            throw new CustomException(ExceptionCode.IS_QUESTION_TYPE);
        }

        // 가림 처리된 게시글이면 에러 리턴
        if(post.getTitle().equals(POST_SCREENED)){
            throw new CustomException(ExceptionCode.SCREENED_POST);
        }

        // 게시글 이미지 DTO
        List<PostResponse.PostImageDTO> postImageDTOS = post.getPostImages().stream()
                .map(postImage -> new PostResponse.PostImageDTO(postImage.getId(), postImage.getImageURL()))
                .collect(Collectors.toList());

        // 댓글과 대댓글을 모두 담고 있는 Comment 리스트를 CommentDTO로 가공하면서, 대댓글이 댓글 밑으로 들어가게 처리
        List<Comment> comments = commentRepository.findByPostIdWithUserAndParentAndRemoved(postId);
        List<Long> likedCommentIds = commentLikeRepository.findCommentIdsByUserId(userId);
        List<PostResponse.CommentDTO> commentDTOS = converCommentToCommentDTO(comments, likedCommentIds);

        // 좋아요 수
        Long likeNum = getCachedPostLikeNum(POST_LIKE_NUM_KEY_PREFIX, postId);

        // 좋아요 여부
        boolean isLike = postLikeRepository.existsByPostIdAndUserId(postId, userId);

        // 조회 수 증가
        redisService.incrementValue(POST_VIEW_NUM_PREFIX, postId.toString(), 1L);

        // 공지 사항의 경우, 게시글 읽음 여부를 저장 (1년동안)
        if(post.getPostType().equals(PostType.NOTICE)){
            String key = POST_READ_KEY_PREFIX + userId;
            redisService.addSetElement(key, postId, POST_READ_EXP);
        }

        return new PostResponse.FindPostByIdDTO(post.getUser().getNickName(), post.getUser().getProfileURL(), post.getTitle(), post.getContent(), post.getCreatedDate(), post.getCommentNum(), likeNum, isMine, isLike, postImageDTOS, commentDTOS);
    }

    @Transactional(readOnly = true)
    public PostResponse.FindQnaByIdDTO findQnaById(Long postId, Long userId){
        // user, postImages를 패치조인 해서 조회
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new CustomException(ExceptionCode.POST_NOT_FOUND)
        );

        boolean isMineForQuestion = post.getUser().getId().equals(userId);

        // 질문 게시글에 대해서만 조회 가능
        if(!post.getPostType().equals(PostType.QUESTION)){
            throw new CustomException(ExceptionCode.NOT_QUESTION_TYPE);
        }

        // 가림 처리된 게시글이면 에러 리턴
        if(post.getTitle().equals(POST_SCREENED)){
            throw new CustomException(ExceptionCode.SCREENED_POST);
        }

        // 게시글 이미지 DTO
        List<PostResponse.PostImageDTO> postImageDTOS = post.getPostImages().stream()
                .map(postImage -> new PostResponse.PostImageDTO(postImage.getId(), postImage.getImageURL()))
                .collect(Collectors.toList());

        // 답변 게시글 DTO
        List<PostResponse.AnswerDTO> answerDTOS = postRepository.findByParentIdWithUser(postId).stream()
                .map(answer -> {
                    List<PostResponse.PostImageDTO> answerImageDTOS = answer.getPostImages().stream()
                            .map(postImage -> new PostResponse.PostImageDTO(postImage.getId(), postImage.getImageURL()))
                            .collect(Collectors.toList());

                    boolean isMineForAnswer = answer.getUser().getId().equals(userId);
                    return new PostResponse.AnswerDTO(
                            answer.getId(),
                            answer.getUser().getNickName(),
                            answer.getUser().getProfileURL(),
                            answer.getContent(),
                            answer.getCreatedDate(),
                            answerImageDTOS,
                            isMineForAnswer);
                })
                .collect(Collectors.toList());

        // 조회 수 증가
        redisService.incrementValue(POST_VIEW_NUM_PREFIX, postId.toString(), 1L);

        return new PostResponse.FindQnaByIdDTO(post.getUser().getNickName(), post.getUser().getProfileURL(), post.getTitle(), post.getContent(), post.getCreatedDate(), postImageDTOS, answerDTOS, isMineForQuestion);
    }

    @Transactional(readOnly = true)
    public PostResponse.FindAnswerByIdDTO findAnswerById(Long postId, Long userId){
        // user, postImages를 패치조인 해서 조회
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new CustomException(ExceptionCode.POST_NOT_FOUND)
        );

        boolean isMine = post.getUser().getId().equals(userId);

        if(!post.getPostType().equals(PostType.ANSWER)){
            throw new CustomException(ExceptionCode.NOT_ANSWER_TYPE);
        }

        // 게시글 이미지 DTO
        List<PostResponse.PostImageDTO> postImageDTOS = post.getPostImages().stream()
                .map(postImage -> new PostResponse.PostImageDTO(postImage.getId(), postImage.getImageURL()))
                .toList();

        return new PostResponse.FindAnswerByIdDTO(post.getUser().getNickName(), post.getContent(), post.getCreatedDate(), postImageDTOS, isMine);
    }

    @Transactional
    public void updatePost(PostRequest.UpdatePostDTO requestDTO, User user, Long postId){
        // 존재하지 않는 글이면 에러
        Post post = postRepository.findByIdWithUser(postId).orElseThrow(
                () -> new CustomException(ExceptionCode.POST_NOT_FOUND)
        );

        // 수정 권한 체크
        checkPostAuthority(post.getUser().getId(), user);

        // 제목, 본문 업데이트
        post.updateTitleAndContent(requestDTO.title(), requestDTO.content());

        // 유지할 이미지를 제외한 모든 이미지 DB와 S3에서 삭제
        if (requestDTO.retainedImageIds() != null && !requestDTO.retainedImageIds().isEmpty()) {
            postImageRepository.deleteByPostIdAndIdNotIn(postId, requestDTO.retainedImageIds());
        } else {
            postImageRepository.deleteByPostId(postId);
        }

        //List<PostImage> postImages = post.getPostImages();
        //deleteImagesInS3(requestDTO.retainedImageIds(), postImages);

        // 새 이미지 추가
        List<PostImage> newImages = requestDTO.newImages().stream()
                .map(postImageDTO -> PostImage.builder()
                        .post(post)
                        .imageURL(postImageDTO.imageURL())
                        .build())
                .collect(Collectors.toList());

        postImageRepository.saveAll(newImages);
    }

    @Transactional
    public void deletePost(Long postId, User user){
        // 존재하지 않은 포스트면 에러
        Post post = postRepository.findByIdWithUserAndParent(postId).orElseThrow(
                () -> new CustomException(ExceptionCode.POST_NOT_FOUND)
        );

        // 수정 권한 체크
        checkPostAuthority(post.getUser().getId(), user);

        // S3에 저장된 이미지 삭제
        /*post.getPostImages().forEach(
                postImage -> {
                    String objectKey = s3Service.extractObjectKeyFromUri(postImage.getImageURL());
                    s3Service.deleteImage(objectKey);
                }
        );*/

        postLikeRepository.deleteAllByPostId(postId);
        commentLikeRepository.deleteByPostId(postId);
        popularPostRepository.deleteByPostId(postId);
        postImageRepository.deleteByPostId(postId);
        commentRepository.deleteByPostId(postId); // soft-delete
        postRepository.delete(post); // soft-delete
    }

    @Transactional
    public void deleteAnswer(Long answerId, User user){
        // 존재하지 않은 포스트면 에러
        Post post = postRepository.findByIdWithUserAndParent(answerId).orElseThrow(
                () -> new CustomException(ExceptionCode.POST_NOT_FOUND)
        );

        // 답변 타입이 아니면 에러
        if(!post.getPostType().equals(PostType.ANSWER)){
            throw new CustomException(ExceptionCode.NOT_ANSWER_TYPE);
        }

        // 수정 권한 체크
        checkPostAuthority(post.getUser().getId(), user);

        // 답변글이라서 부모(질문글)이 존재한다면, 답변 수 감소
        Post parent = post.getParent();
        if(parent != null){
            postRepository.decrementAnswerNum(parent.getId());
        }

        postImageRepository.deleteByPostId(answerId);
        postRepository.deleteById(answerId);
    }

    @Transactional
    public void likePost(Long postId, Long userId){
        // 존재하지 않는 글이면 에러
        Long postWriterId = postRepository.findUserIdById(postId).orElseThrow(
                () -> new CustomException(ExceptionCode.POST_NOT_FOUND)
        );

        // 자기 자신의 글에는 좋아요를 할 수 없다.
        if (postWriterId.equals(userId)) {
            throw new CustomException(ExceptionCode.CANT_LIKE_MY_POST);
        }

        Optional<PostLike> postLikeOP = postLikeRepository.findByUserIdAndPostId(userId, postId);

        // 이미 좋아요를 눌렀다면, 취소하는 액션이니 게시글의 좋아요 수를 감소시키고 하고, postLike 엔티티 삭제
        if(postLikeOP.isPresent()){
            checkExpiration(postLikeOP.get().getCreatedDate());

            postLikeRepository.delete(postLikeOP.get());
            redisService.decrementValue(POST_LIKE_NUM_KEY_PREFIX, postId.toString(), 1L);
        }
        else { // 좋아요를 누르지 않았다면, 좋아요 수를 증가키고, 엔티티 저장
            User userRef = entityManager.getReference(User.class, userId);
            Post postRef = entityManager.getReference(Post.class, postId);

            PostLike postLike = PostLike.builder().user(userRef).post(postRef).build();

            postLikeRepository.save(postLike);
            redisService.incrementValue(POST_LIKE_NUM_KEY_PREFIX, postId.toString(), 1L);
        }
    }

    @Transactional
    public PostResponse.CreateCommentDTO createComment(PostRequest.CreateCommentDTO requestDTO, Long userId, Long postId){
        // 존재하지 않는 글이면 에러
        Long writerId = postRepository.findUserIdById(postId).orElseThrow(
                () -> new CustomException(ExceptionCode.POST_NOT_FOUND)
        );

        // 질문글에는 댓글을 달 수 없다 (댓글 대신 답변이 달리니)
        PostType postType = postRepository.findPostTypeById(postId).orElseThrow(
                () -> new CustomException(ExceptionCode.POST_NOT_FOUND)
        );

        if(postType.equals(PostType.QUESTION)){
            throw new CustomException(ExceptionCode.NOT_QUESTION_TYPE);
        }

        User userRef = entityManager.getReference(User.class, userId);
        Post postRef = entityManager.getReference(Post.class, postId);

        Comment comment = Comment.builder()
                .user(userRef)
                .post(postRef)
                .content(requestDTO.content())
                .build();

        commentRepository.save(comment);

        // 게시글의 댓글 수 증가
        postRepository.incrementCommentNum(postId);

        // 3개월 동안만 좋아요를 할 수 있다
        redisService.storeValue(COMMENT_LIKE_NUM_KEY_PREFIX, comment.getId().toString(), "0", POST_EXP);

        // 알람 생성
        String content = "새로운 댓글: " + requestDTO.content();
        String queryParam = postType.name().toLowerCase();
        String redirectURL = "/community/" + postId + "?type=" + queryParam;
        createAlarm(writerId, content, redirectURL, AlarmType.COMMENT);

        return new PostResponse.CreateCommentDTO(comment.getId());
    }

    @Transactional
    public PostResponse.CreateCommentDTO createReply(PostRequest.CreateCommentDTO requestDTO, Long postId, Long userId, Long parentCommentId){
        // 존재하지 않는 댓글이면 에러
        Comment parentComment = commentRepository.findByIdWithUser(parentCommentId).orElseThrow(
                () -> new CustomException(ExceptionCode.COMMENT_NOT_FOUND)
        );

        // 대댓글에 댓글을 달 수 없다
        if(parentComment.getParent() != null){
            throw new CustomException(ExceptionCode.CANT_REPLY_TO_REPLY);
        }

        // 해당 글의 댓글이 아니면 에러
        checkPostOwnComment(parentComment, postId);

        // 작성자
        User userRef = entityManager.getReference(User.class, userId);
        Post postRef = entityManager.getReference(Post.class, postId);

        Comment comment = Comment.builder()
                .user(userRef)
                .post(postRef)
                .content(requestDTO.content())
                .build();

        parentComment.addChildComment(comment);
        commentRepository.save(comment);

        // 게시글의 댓글 수 증가
        postRepository.incrementCommentNum(postId);

        // 3개월 동안만 좋아요를 할 수 있다
        redisService.storeValue(COMMENT_LIKE_NUM_KEY_PREFIX, comment.getId().toString(), "0", POST_EXP);

        // 알람 생성
        String content = "새로운 대댓글: " + requestDTO.content();
        PostType postType = postRepository.findPostTypeById(postId).get();
        String queryParam = postType.name().toLowerCase();
        String redirectURL = "/community/" + postId + "?type=" + queryParam;
        createAlarm(parentComment.getUser().getId(), content, redirectURL, AlarmType.COMMENT);

        return new PostResponse.CreateCommentDTO(comment.getId());
    }

    @Transactional
    public void updateComment(PostRequest.UpdateCommentDTO requestDTO, Long postId, Long commentId, User user){
        // 존재하지 않는 댓글이면 에러
        Comment comment = commentRepository.findByIdWithUser(commentId).orElseThrow(
                () -> new CustomException(ExceptionCode.COMMENT_NOT_FOUND)
        );

        // 해당 글의 댓글이 아니면 에러
        checkPostOwnComment(comment, postId);

        // 수정 권한 체크
        checkCommentAuthority(comment.getUser().getId(), user);

        comment.updateContent(requestDTO.content());
    }

    // soft delete를 구현하기 전에는 부모 댓글 삭제시, 대댓글까지 모두 삭제 되도록 구현
    @Transactional
    public void deleteComment(Long postId, Long commentId, User user){
        // 존재하지 않는 댓글이면 에러
        Comment comment = commentRepository.findByIdWithUser(commentId).orElseThrow(
                () -> new CustomException(ExceptionCode.COMMENT_NOT_FOUND)
        );

        // 해당 글의 댓글이 아니면 에러
        checkPostOwnComment(comment, postId);

        // 수정 권한 체크
        checkCommentAuthority(comment.getUser().getId(), user);

        // 댓글은 soft-delete 처리
        comment.updateContent(COMMENT_DELETED);
        commentRepository.deleteById(commentId);
        commentLikeRepository.deleteAllByCommentId(commentId);

        // 게시글의 댓글 수 감소
        long childNum = comment.getChildren().size();
        postRepository.decrementCommentNum(postId, 1L + childNum);
    }

    @Transactional
    public void likeComment(Long commentId, Long userId){
        // 존재하지 않는 댓글인지 체크
        Long commentWriterId = commentRepository.findUserIdById(commentId).orElseThrow(
                () -> new CustomException(ExceptionCode.COMMENT_NOT_FOUND)
        );

        // 자기 자신의 댓글에는 좋아요를 할 수 없다.
        if(commentWriterId.equals(userId)){
            throw new CustomException(ExceptionCode.CANT_LIKE_MY_COMMENT);
        }

        Optional<CommentLike> commentLikeOP = commentLikeRepository.findByUserIdAndCommentId(userId, commentId);

        // 이미 좋아요를 눌렀다면, 취소하는 액션이니 게시글의 좋아요 수를 감소시키고 하고, postLike 엔티티 삭제
        if(commentLikeOP.isPresent()){
            checkExpiration(commentLikeOP.get().getCreatedDate());

            commentLikeRepository.delete(commentLikeOP.get());
            redisService.decrementValue(COMMENT_LIKE_NUM_KEY_PREFIX, commentId.toString(), 1L);
        }
        else{ // 좋아요를 누르지 않았다면, 좋아요 수를 증가키고, 엔티티 저장
            User userRef = entityManager.getReference(User.class, userId);
            Comment commentRef = entityManager.getReference(Comment.class, commentId);

            CommentLike commentLike = CommentLike.builder().user(userRef).comment(commentRef).build();

            commentLikeRepository.save(commentLike);
            redisService.incrementValue(COMMENT_LIKE_NUM_KEY_PREFIX, commentId.toString(), 1L);
        }
    }

    @Transactional
    public void submitReport(@RequestBody PostRequest.SubmitReport requestDTO, Long userId){
        User reporter = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        // 이미 신고함
        if(reportRepository.existsByReporterIdAndContentId(userId, requestDTO.contentId(), requestDTO.contentType())){
            throw new CustomException(ExceptionCode.ALREADY_REPORTED);
        }

        User offender ;
        if(requestDTO.contentType() == ContentType.POST) {
            offender = postRepository.findUserById(requestDTO.contentId()).orElseThrow(
                    () -> new CustomException(ExceptionCode.POST_NOT_FOUND)
            );
        } else if (requestDTO.contentType() == ContentType.COMMENT) {
            offender = commentRepository.findUserById(requestDTO.contentId()).orElseThrow(
                    () -> new CustomException(ExceptionCode.COMMENT_NOT_FOUND)
            );
        } else{ // 잘못된 컨텐츠 타입
            throw new CustomException(ExceptionCode.WRONG_REPORT_TARGET);
        }

        // 자신의 컨텐츠에는 신고할 수 없음
        if(offender.getId().equals(userId)){
            throw new CustomException(ExceptionCode.CANNOT_REPORT_OWN_CONTENT);
        }

        Report report = Report.builder()
                .reporter(reporter)
                .offender(offender)
                .contentType(requestDTO.contentType())
                .contentId(requestDTO.contentId())
                .type(requestDTO.reportType())
                .status(ReportStatus.PROCESSING)
                .reason(requestDTO.reason())
                .build();

        reportRepository.save(report);
    }

    @Scheduled(cron = "0 0 6,18 * * *")
    @Transactional
    public void updateTodayPopularPosts() {
        LocalDate now = LocalDate.now();
        LocalDateTime startOfToday = now.atStartOfDay();
        LocalDateTime endOfToday = now.atTime(LocalTime.MAX);

        // 인기 입양 글 업데이트
        List<Post> adoptionPosts = postRepository.findByDateAndType(startOfToday, endOfToday, PostType.ADOPTION);
        processPopularPosts(adoptionPosts, PostType.ADOPTION);

        // 인기 임시보호 글 업데이트
        List<Post> fosteringPosts = postRepository.findByDateAndType(startOfToday, endOfToday, PostType.FOSTERING);
        processPopularPosts(fosteringPosts, PostType.FOSTERING);
    }

    @Scheduled(cron = "0 25 0 * * *")
    @Transactional
    public void syncViewNum() {
        LocalDateTime oneWeeksAgo = LocalDateTime.now().minus(1, ChronoUnit.WEEKS);
        List<Post> posts = postRepository.findPostIdsWithinDate(oneWeeksAgo);

        for (Post post : posts) {
            Long readCnt = redisService.getValueInLongWithNull(POST_VIEW_NUM_PREFIX, post.getId().toString());

            if (readCnt != null) {
                post.updateReadCnt(readCnt);
            }
        }
    }

    private void processPopularPosts(List<Post> posts, PostType postType){
        // 포인트가 10이 넘으면 popularPosts 리스트에 추가
        List<Post> popularPosts = posts.stream()
                .peek(post -> {
                    double hotPoint = getCachedPostViewNum(POST_VIEW_NUM_PREFIX, post.getId(), post::getReadCnt) * 0.001 + post.getCommentNum() + getCachedPostLikeNum(POST_LIKE_NUM_KEY_PREFIX, post.getId()) * 5;
                    post.updateHotPoint(hotPoint);
                })
                .filter(post -> post.getHotPoint() > 10.0)
                .collect(Collectors.toList());

        // popularPosts 리스트의 개수가 5개 미만이라면, 5개가 되도록 채우기
        if (popularPosts.size() < 5) {
            List<Post> remainingPosts = posts.stream()
                    .filter(post -> !popularPosts.contains(post))
                    .sorted(Comparator.comparingDouble(Post::getHotPoint).reversed())
                    .toList();

            popularPosts.addAll(remainingPosts.stream()
                    .limit(5 - popularPosts.size())
                    .toList());
        }

        // popularPosts 리스트의 post는 popularPost 엔티티로 저장됨
        popularPosts.forEach(post -> {
            PopularPost popularPost = PopularPost.builder()
                    .post(post)
                    .postType(postType)
                    .build();
            popularPostRepository.save(popularPost);
        });
    }

    private void checkPostAuthority(Long writerId, User accessor){
        // 관리자면 수정 가능
        if(accessor.getRole().equals(UserRole.ADMIN) || accessor.getRole().equals(UserRole.SUPER)){
            return;
        }

        if(!writerId.equals(accessor.getId())){
            throw new CustomException(ExceptionCode.USER_FORBIDDEN);
        }
    }

    private void checkCommentAuthority(Long writerId, User accessor) {
        // 관리자면 수정 가능
        if(accessor.getRole().equals(UserRole.ADMIN) || accessor.getRole().equals(UserRole.SUPER)){
            return;
        }

        if(!writerId.equals(accessor.getId())){
            throw new CustomException(ExceptionCode.USER_FORBIDDEN);
        }
    }

    private void checkExpiration(LocalDateTime date){
        LocalDate likeDate =  date.toLocalDate();
        LocalDate now = LocalDate.now();
        Period period = Period.between(likeDate, now);

        // 기간이 3개월 이상이면 좋아요를 할 수 없다
        if (period.getMonths() >= 3 || period.getYears() > 0) {
            throw new CustomException(ExceptionCode.POST_LIKE_EXPIRED);
        }
    }

    private void checkPostOwnComment(Comment comment, Long postId){
        Long commentPostId = comment.getPost().getId();

        if(!commentPostId.equals(postId)){
            throw new CustomException(ExceptionCode.NOT_POSTS_COMMENT);
        }
    }

    private List<PostResponse.CommentDTO> converCommentToCommentDTO(List<Comment> comments, List<Long> likedComments) {
        // CommentDTO는 특정 게시글에 대한 모든 댓글 및 대댓글을 들고 있음
        List<PostResponse.CommentDTO> commentDTOS = new ArrayList<>();

        // map을 사용해서 대댓글을 해당 부모 댓글에 추가
        Map<Long, PostResponse.CommentDTO> commentMap = new HashMap<>();

        comments.forEach(comment -> {
            Long likeNum = getCachedCommentLikeNum(COMMENT_LIKE_NUM_KEY_PREFIX, comment.getId());

            // 부모 댓글이면, CommentDTO로 변환해서 commentDTOS 리스트에 추가
            if (comment.getParent() == null) {
                PostResponse.CommentDTO commentDTO = new PostResponse.CommentDTO(
                        comment.getId(),
                        comment.getUser().getNickName(),
                        comment.getUser().getProfileURL(),
                        comment.getContent(),
                        comment.getCreatedDate(),
                        comment.getUser().getProvince(),
                        likeNum,
                        likedComments.contains(comment.getId()),
                        new ArrayList<>());

                commentDTOS.add(commentDTO);
                commentMap.put(comment.getId(), commentDTO);
            } else { // 자식 댓글이면, ReplyDTO로 변환해서 부모 댓글의 replies 리스트에 추가
                if (comment.getRemovedAt() != null) { // 삭제된 대댓글
                    // 마지막 대댓글이 아니면, '삭제된 댓글입니다' 처리. 마지막 대댓글이면, 보이지 않음
                    if (!commentRepository.existsByParentIdAndDateAfter(comment.getParent().getId(), comment.getCreatedDate())) {
                        return;
                    }
                }

                PostResponse.ReplyDTO replyDTO = new PostResponse.ReplyDTO(
                        comment.getId(),
                        comment.getUser().getNickName(),
                        comment.getUser().getProfileURL(),
                        comment.getParent().getUser().getNickName(),
                        comment.getContent(),
                        comment.getCreatedDate(),
                        comment.getUser().getProvince(),
                        likeNum,
                        likedComments.contains(comment.getId()));

                Long parentId = comment.getParent().getId();
                commentMap.get(parentId).replies().add(replyDTO);
            }
        });

        return commentDTOS;
    }

    private Long getCachedPostLikeNum(String keyPrefix, Long key) {
        Long likeNum = redisService.getValueInLongWithNull(keyPrefix, key.toString());

        if (likeNum == null) {
            likeNum = postRepository.countLikesByPostId(key);
            redisService.storeValue(keyPrefix, key.toString(), likeNum.toString(), POST_EXP);
        }

        return likeNum;
    }

    private Long getCachedCommentLikeNum(String keyPrefix, Long key) {
        Long likeNum = redisService.getValueInLongWithNull(keyPrefix, key.toString());

        if (likeNum == null) {
            likeNum = commentRepository.countLikesByCommentId(key);
            redisService.storeValue(keyPrefix, key.toString(), likeNum.toString(), POST_EXP);
        }

        return likeNum;
    }

    private Long getCachedPostViewNum(String keyPrefix, Long key, Supplier<Long> dbFallback) {
        Long viewNum = redisService.getValueInLongWithNull(keyPrefix, key.toString());

        if (viewNum == null) {
            viewNum = dbFallback.get();
        }

        return viewNum;
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