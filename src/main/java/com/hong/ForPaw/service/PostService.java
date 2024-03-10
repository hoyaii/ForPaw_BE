package com.hong.ForPaw.service;

import com.hong.ForPaw.controller.DTO.PostRequest;
import com.hong.ForPaw.controller.DTO.PostResponse;
import com.hong.ForPaw.core.errors.CustomException;
import com.hong.ForPaw.core.errors.ExceptionCode;
import com.hong.ForPaw.domain.Alarm.AlarmType;
import com.hong.ForPaw.domain.Post.*;
import com.hong.ForPaw.domain.User.Role;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostReadStatusRepository postReadStatusRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final AlarmRepository alarmRepository;
    private final UserRepository userRepository;
    private final AlarmService alarmService;
    private final EntityManager entityManager;

    @Transactional
    public PostResponse.CreatePostDTO createPost(PostRequest.CreatePostDTO requestDTO, Long userId){

        User userRef = entityManager.getReference(User.class, userId);

        List<PostImage> postImages = requestDTO.images().stream()
                .map(postImageDTO -> PostImage.builder()
                        .imageURL(postImageDTO.imageURL())
                        .build())
                .collect(Collectors.toList());

        Post post = Post.builder()
                .user(userRef)
                .postType(requestDTO.type())
                .title(requestDTO.title())
                .content(requestDTO.content())
                .build();

        // PostImage 객체들에 Post 참조 설정
        postImages.forEach(post::addImage);
        postRepository.save(post);

        return new PostResponse.CreatePostDTO(post.getId());
    }

    @Transactional
    public PostResponse.FindAllPostDTO findPostList(){
        // 페이지네이션은 0페이지에 5개만 보내줌
        Pageable pageable = createPageable(0, 5, "id");

        // 입양 스토리 글 찾기
        List<PostResponse.PostDTO> adoptionPosts = getPostDTOsByType(PostType.adoption, pageable);

        // 임시 보호 글 찾기
        List<PostResponse.PostDTO> protectionPosts = getPostDTOsByType(PostType.protection, pageable);

        // 질문해요 글 찾기
        List<PostResponse.PostDTO> questionPosts = getPostDTOsByType(PostType.question, pageable);

        return new PostResponse.FindAllPostDTO(adoptionPosts, protectionPosts, questionPosts);
    }

    @Transactional
    public PostResponse.FindAdoptionPostDTO findAdoptionPost(Integer page, Integer size, String sort){

        Pageable pageable = createPageable(page, size, sort);
        List<PostResponse.PostDTO> adoptPostDTOS = getPostDTOsByType(PostType.adoption, pageable);

        if(adoptPostDTOS.isEmpty()){
            throw new CustomException(ExceptionCode.SEARCH_NOT_FOUND);
        }

        return new PostResponse.FindAdoptionPostDTO(adoptPostDTOS);
    }

    @Transactional
    public PostResponse.FindProtectionPostDTO findProtectionPost(Integer page, Integer size, String sort){

        Pageable pageable = createPageable(page, size, sort);
        List<PostResponse.PostDTO> adoptPostDTOS = getPostDTOsByType(PostType.protection, pageable);

        if(adoptPostDTOS.isEmpty()){
            throw new CustomException(ExceptionCode.SEARCH_NOT_FOUND);
        }

        return new PostResponse.FindProtectionPostDTO(adoptPostDTOS);
    }

    @Transactional
    public PostResponse.FindQuestionPostDTO findQuestionPost(Integer page, Integer size, String sort){

        Pageable pageable = createPageable(page, size, sort);
        List<PostResponse.PostDTO> adoptPostDTOS = getPostDTOsByType(PostType.question, pageable);

        if(adoptPostDTOS.isEmpty()){
            throw new CustomException(ExceptionCode.SEARCH_NOT_FOUND);
        }

        return new PostResponse.FindQuestionPostDTO(adoptPostDTOS);
    }

    @Transactional
    public PostResponse.FindPostByIdDTO findPostById(Long postId, Long userId){
        // 존재하지 않는 글인지 체크
        checkPostExist(postId);

        // 특정 게시글에 대한 모든 댓글 및 대댓글을 조회
        List<Comment> comments = commentRepository.findAllCommentsAndRepliesByPostId(postId);

        // CommentDTO는 부모 댓글과 부모의 대댓글을 함께 담고 있음
        List<PostResponse.CommentDTO> commentDTOS = new ArrayList<>();
        Map<Long, PostResponse.CommentDTO> commentMap = new HashMap<>(); // 대댓글을 해당 부모 댓글에 쉽게 추가하기 위해 만든 Map

        // 댓글과 대댓글 엔티티를 CommentDTO로 변환
        comments.forEach(comment -> {
            PostResponse.ReplyDTO replyDTO = new PostResponse.ReplyDTO(comment.getId(), comment.getUser().getNickName(), comment.getContent(), comment.getCreatedDate(), comment.getUser().getRegion());

            // 부모 댓글이면, CommentDTO로 변환해서 commentDTOS 리스트에 추가
            if (comment.getParent() == null) {
                PostResponse.CommentDTO commentDTO = new PostResponse.CommentDTO(comment.getId(), comment.getUser().getNickName(), comment.getContent(), comment.getCreatedDate(), comment.getUser().getRegion(), new ArrayList<>());
                commentDTOS.add(commentDTO);
                commentMap.put(comment.getId(), commentDTO);
            }
            else { // 자식 댓글이면, 부모 댓글의 replies 리스트에 추가
                commentMap.get(comment.getParent().getId()).replies().add(replyDTO);
            }
        });

        Post postRef = entityManager.getReference(Post.class, postId);
        User userRef = entityManager.getReference(User.class, userId);

        // 게시글 읽음 처리 (화면에서 게시글 확인 여부가 필요한 곳이 있음)
        PostReadStatus postReadStatus = PostReadStatus.builder()
                .post(postRef)
                .user(userRef)
                .build();

        postReadStatusRepository.save(postReadStatus);
        return new PostResponse.FindPostByIdDTO(commentDTOS);
    }

    @Transactional
    public void updatePost(PostRequest.UpdatePostDTO requestDTO, User user, Long postId){
        // 존재하지 않는 글이면 에러
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new CustomException(ExceptionCode.POST_NOT_FOUND)
        );

        // 수정 권한 체크
        checkPostAuthority(post.getUser(), user);

        post.updatePost(requestDTO.title(), requestDTO.content());

        // 유지할 이미지를 제외한 모든 이미지 삭제
        if (requestDTO.retainedImageIds() != null && !requestDTO.retainedImageIds().isEmpty()) {
            postImageRepository.deleteByPostIdAndIdNotIn(postId, requestDTO.retainedImageIds());
        } else {
            postImageRepository.deleteByPostId(postId);
        }

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
        // 존재하지 않는 글인지 체크
        checkPostExist(postId);

        // 수정 권한 체크
        User writer = postRepository.findUserByPostId(postId);
        checkPostAuthority(writer, user);

        postLikeRepository.deleteAllByPostId(postId);
        postReadStatusRepository.deleteAllByPostId(postId);
        commentRepository.deleteAllByPostId(postId);
        commentLikeRepository.deleteAllByPostId(postId);
        postRepository.deleteById(postId);
    }

    @Transactional
    public void likePost(Long postId, Long userId){
        // 존재하지 않는 글인지 체크
        checkPostExist(postId);

        // 자기 자신의 글에는 좋아요를 할 수 없다.
        if (postRepository.isOwnPost(postId, userId)) {
            throw new CustomException(ExceptionCode.POST_CANT_LIKE);
        }

        Optional<PostLike> postLikeOP = postLikeRepository.findByUserIdAndPostId(userId, postId);

        // 이미 좋아요를 눌렀다면, 취소하는 액션이니 게시글의 좋아요 수를 감소시키고 하고, postLike 엔티티 삭제
        if(postLikeOP.isPresent()){
            postRepository.decrementLikeNumById(postId);
            postLikeRepository.delete(postLikeOP.get());
        }
        else { // 좋아요를 누르지 않았다면, 좋아요 수를 증가키고, 엔티티 저장
            User userRef = entityManager.getReference(User.class, userId);
            Post postRef = entityManager.getReference(Post.class, postId);

            PostLike postLike = PostLike.builder().user(userRef).post(postRef).build();

            postRepository.incrementLikeNumById(postId);
            postLikeRepository.save(postLike);
        }
    }

    @Transactional
    public PostResponse.CreateCommentDTO createComment(PostRequest.CreateCommentDTO requestDTO, Long userId, Long postId){
        // 존재하지 않는 글이면 에러
        checkPostExist(postId);

        User userRef = entityManager.getReference(User.class, userId);
        Post postRef = entityManager.getReference(Post.class, postId);

        Comment comment = Comment.builder()
                .user(userRef)
                .post(postRef)
                .content(requestDTO.content())
                .build();

        commentRepository.save(comment);

        // 게시글의 댓글 수 증가
        postRepository.incrementCommentNumById(postId);

        // 게시글 작성자의 userId를 구해서, 프록시 객체 생성
        Long postUserId = postRepository.findUserIdByPostId(postId).get(); // 이미 앞에서 존재하는 글임을 체크함
        User postUserRef = entityManager.getReference(User.class, postUserId);

        // 알람 생성
        String redirectURL = "post/"+postId+"/entire";
        alarmService.send(postUserRef, AlarmType.comment, "새로운 댓글: " + requestDTO.content(), redirectURL);

        return new PostResponse.CreateCommentDTO(comment.getId());
    }

    @Transactional
    public PostResponse.CreateCommentDTO createReply(PostRequest.CreateCommentDTO requestDTO, Long postId, Long userId, Long parentCommentId){
        // 존재하지 않는 댓글에 대댓글을 달려고 하면 에러
        Comment parent = commentRepository.findById(parentCommentId).orElseThrow(
                () -> new CustomException(ExceptionCode.COMMENT_NOT_FOUND)
        );
        // 작성자
        User userRef = entityManager.getReference(User.class, userId);

        Comment comment = Comment.builder()
                .user(userRef)
                .post(parent.getPost())
                .content(requestDTO.content())
                .build();

        parent.addChildComment(comment);
        commentRepository.save(comment);

        // 게시글의 댓글 수 증가
        postRepository.incrementCommentNumById(postId);

        // 알람 생성
        User parentCommentUserRef = entityManager.getReference(User.class, parent.getUser().getId()); // 작성자
        String redirectURL = "post/"+postId+"/entire";
        alarmService.send(parentCommentUserRef, AlarmType.comment, "새로운 대댓글: " + requestDTO.content(), redirectURL);

        return new PostResponse.CreateCommentDTO(comment.getId());
    }

    @Transactional
    public void updateComment(PostRequest.UpdateCommentDTO requestDTO, Long commentId, User user){
        // 존재하지 않는 댓글이면 에러
        Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new CustomException(ExceptionCode.COMMENT_NOT_FOUND)
        );

        // 수정 권한 체크
        checkCommentAuthority(comment.getUser(), user);

        comment.updateComment(requestDTO.content());
    }

    // soft delete를 구현하기 전에는 부모 댓글 삭제시, 대댓글까지 모두 삭제 되도록 구현
    @Transactional
    public void deleteComment(Long postId, Long commentId, User user){
        // 존재하지 않는 댓글이면 에러
        Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new CustomException(ExceptionCode.COMMENT_NOT_FOUND)
        );

        // 수정 권한 체크
        checkCommentAuthority(comment.getUser(), user);

        // 댓글 및 관련 대댓글 삭제 (CascadeType.ALL에 의해 처리됨)
        commentRepository.deleteById(commentId);
        commentLikeRepository.deleteAllByCommentId(commentId);

        // 게시글의 댓글 수 감소
        Integer childNum = comment.getChildren().size();
        postRepository.decrementCommentNumById(postId, childNum + 1);
    }

    @Transactional
    public void likeComment(Long commentId, Long userId){
        // 존재하지 않는 댓글인지 체크
        checkCommentExist(commentId);

        // 자기 자신의 댓글에는 좋아요를 할 수 없다.
        if(commentRepository.isOwnComment(commentId, userId)){
            throw new CustomException(ExceptionCode.COMMENT_CANT_LIKE);
        }

        Optional<CommentLike> commentLikeOP = commentLikeRepository.findByUserIdAndCommentId(userId, commentId);

        // 이미 좋아요를 눌렀다면, 취소하는 액션이니 게시글의 좋아요 수를 감소시키고 하고, postLike 엔티티 삭제
        if(commentLikeOP.isPresent()){
            commentRepository.decrementLikeNumById(commentId);
            commentLikeRepository.delete(commentLikeOP.get());
        }
        else{ // 좋아요를 누르지 않았다면, 좋아요 수를 증가키고, 엔티티 저장
            User userRef = entityManager.getReference(User.class, userId);
            Comment commentRef = entityManager.getReference(Comment.class, commentId);

            CommentLike commentLike = CommentLike.builder().user(userRef).comment(commentRef).build();

            commentRepository.incrementLikeNumById(commentId);
            commentLikeRepository.save(commentLike);
        }
    }

    public List<PostResponse.PostDTO> getPostDTOsByType(PostType postType, Pageable pageable){
        // N+1 문제 방지를 위해 이미지까지 다 불러옴
        Page<Post> postPage = postRepository.findByPostTypeWithImages(postType, pageable);

        List<PostResponse.PostDTO> postDTOS = postPage.getContent().stream()
                .map(post -> {
                    List<PostResponse.PostImageDTO> postImageDTOS = post.getPostImages().stream()
                            .map(postImage -> new PostResponse.PostImageDTO(postImage.getId(), postImage.getImageURL()))
                            .collect(Collectors.toList());

                    return new PostResponse.PostDTO(post.getId(), post.getTitle(), post.getContent(), post.getCreatedDate(), post.getCommentNum(), post.getLikeNum(), postImageDTOS);
                })
                .collect(Collectors.toList());

        return postDTOS;
    }

    private Pageable createPageable(int page, int size, String sortProperty) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortProperty));
    }

    private void checkPostAuthority(User writer, User accessor){
        // 관리자면 수정 가능
        if(accessor.getRole().equals(Role.ADMIN)){
            return;
        }

        if(!writer.getId().equals(accessor.getId())){
            throw new CustomException(ExceptionCode.USER_FORBIDDEN);
        }
    }

    private void checkCommentAuthority(User writer, User accessor) {
        // 관리자면 수정 가능
        if(accessor.getRole().equals(Role.ADMIN)){
            return;
        }

        if(!writer.getId().equals(accessor.getId())){
            throw new CustomException(ExceptionCode.USER_FORBIDDEN);
        }
    }

    private void checkPostExist(Long postId){

        if (!postRepository.existsById(postId)) {
            throw new CustomException(ExceptionCode.POST_NOT_FOUND);
        }
    }

    private void checkCommentExist(Long commentId){

        if(!commentRepository.existsById(commentId)){
            throw new CustomException(ExceptionCode.COMMENT_NOT_FOUND);
        }
    }
}