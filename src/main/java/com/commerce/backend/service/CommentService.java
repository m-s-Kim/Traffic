package com.commerce.backend.service;

import com.commerce.backend.dto.WriteCommentDto;
import com.commerce.backend.entity.Article;
import com.commerce.backend.entity.Board;
import com.commerce.backend.entity.Comment;
import com.commerce.backend.entity.User;
import com.commerce.backend.exception.ForbiddenException;
import com.commerce.backend.exception.RateLimitException;
import com.commerce.backend.exception.ResourceNotFoundException;
import com.commerce.backend.pojo.WriteComment;
import com.commerce.backend.repository.ArticleRepository;
import com.commerce.backend.repository.BoardRepository;
import com.commerce.backend.repository.CommentRepository;
import com.commerce.backend.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class CommentService {

    private final BoardRepository boardRepository;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ElasticSearchService elasticSearchService;
    private final ObjectMapper objectMapper;
    private final RabbitMQSender rabbitMQSender;
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public CommentService(BoardRepository boardRepository, ArticleRepository articleRepository, UserRepository userRepository, CommentRepository commentRepository
                      , ElasticSearchService elasticSearchService, ObjectMapper objectMapper, RabbitMQSender rabbitMQSender
                      , RedisTemplate<String, Object> redisTemplate) {
        this.boardRepository = boardRepository;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.elasticSearchService = elasticSearchService;
        this.objectMapper = objectMapper;
        this.rabbitMQSender = rabbitMQSender;
        this.redisTemplate = redisTemplate;

    }

    @Transactional
    public Comment writeComment(Long boardId, Long articleId, WriteCommentDto dto) throws JsonProcessingException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        if (!this.isCanWriteComment()) {
            throw new RateLimitException("comment not written by rate limit");
        }
        Optional<User> author = userRepository.findByUsername(userDetails.getUsername());
        Optional<Board> board = boardRepository.findById(boardId);
        Optional<Article> article = articleRepository.findById(articleId);
        if (author.isEmpty()) {
            throw new ResourceNotFoundException("author not found");
        }
        if (board.isEmpty()) {
            throw new ResourceNotFoundException("board not found");
        }
        if (article.isEmpty()) {
            throw new ResourceNotFoundException("article not found");
        }
        if (article.get().getIsDeleted()) {
            throw new ForbiddenException("article is deleted");
        }
        Comment comment = new Comment();
        comment.setArticle(article.get());
        comment.setAuthor(author.get());
        comment.setContent(dto.getContent());
        commentRepository.save(comment);

        WriteComment writeComment = new WriteComment();
        writeComment.setCommentId(comment.getId());
        rabbitMQSender.send(writeComment);


        // Elasticsearch에서 기존 게시글 문서 조회 후 댓글 추가
        String articleIdStr = article.get().getId().toString();
        elasticSearchService.getArticleDocument(articleIdStr).flatMap(articleDocument -> {
            try {
                // 게시글 JSON 파싱
                JsonNode articleJson = objectMapper.readTree(articleDocument);

                // 댓글 정보를 JSON 형태로 생성
                ObjectNode commentJson = objectMapper.createObjectNode();
                commentJson.put("id", comment.getId());
                commentJson.put("content", comment.getContent());
                commentJson.put("author", comment.getAuthor().getUsername());

                // 기존 comments 배열에 새로운 댓글 추가
                ArrayNode commentsArray = (ArrayNode) articleJson.path("_source").path("comments");
                commentsArray.add(commentJson);

                // 업데이트된 게시글 JSON을 Elasticsearch에 저장
                String updatedArticleJson = objectMapper.writeValueAsString(articleJson.path("_source"));
                return elasticSearchService.indexArticleDocument(articleIdStr, updatedArticleJson);
            } catch (JsonProcessingException e) {
                return Mono.error(new RuntimeException("Error updating article in Elasticsearch", e));
            }
        }).block(); // 비동기 작업을 동기적으로 처리

        return comment;
    }


    @Transactional
    public Comment editComment(Long boardId, Long articleId, Long commentId, WriteCommentDto dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        if (!this.isCanEditComment()) {
            throw new RateLimitException("comment not written by rate limit");
        }
        Optional<User> author = userRepository.findByUsername(userDetails.getUsername());
        Optional<Board> board = boardRepository.findById(boardId);
        Optional<Article> article = articleRepository.findById(articleId);
        if (author.isEmpty()) {
            throw new ResourceNotFoundException("author not found");
        }
        if (board.isEmpty()) {
            throw new ResourceNotFoundException("board not found");
        }
        if (article.isEmpty()) {
            throw new ResourceNotFoundException("article not found");
        }
        if (article.get().getIsDeleted()) {
            throw new ForbiddenException("article is deleted");
        }
        Optional<Comment> comment = commentRepository.findById(commentId);
        if (comment.isEmpty() || comment.get().getIsDeleted()) {
            throw new ResourceNotFoundException("comment not found");
        }
        if (comment.get().getAuthor() != author.get()) {
            throw new ForbiddenException("comment author different");
        }
        if (dto.getContent() != null) {
            comment.get().setContent(dto.getContent());
        }
        commentRepository.save(comment.get());
        return comment.get();
    }

    @Transactional
    public boolean deleteComment(Long boardId, Long articleId, Long commentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        if (!this.isCanEditComment()) {
            throw new RateLimitException("comment not written by rate limit");
        }
        Optional<User> author = userRepository.findByUsername(userDetails.getUsername());
        Optional<Board> board = boardRepository.findById(boardId);
        Optional<Article> article = articleRepository.findById(articleId);

        if (author.isEmpty()) {
            throw new ResourceNotFoundException("author not found");
        }
        if (board.isEmpty()) {
            throw new ResourceNotFoundException("board not found");
        }
        if (article.isEmpty()) {
            throw new ResourceNotFoundException("article not found");
        }
        if (article.get().getIsDeleted()) {
            throw new ForbiddenException("article is deleted");
        }

        Optional<Comment> comment = commentRepository.findById(commentId);

        if (comment.isEmpty() || comment.get().getIsDeleted()) {
            throw new ResourceNotFoundException("comment not found");
        }
        if (comment.get().getAuthor() != author.get()) {
            throw new ForbiddenException("comment author different");
        }

        comment.get().setIsDeleted(true);
        commentRepository.save(comment.get());
        return true;
    }

    private boolean isCanEditComment() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Comment latestComment = commentRepository.findLatestCommentOrderByCreatedDate(userDetails.getUsername());
        if (latestComment == null || latestComment.getUpdatedDate() == null) {
            return true;
        }
        return this.isDifferenceMoreThanOneMinutes(latestComment.getUpdatedDate());
    }

    private boolean isCanWriteComment() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Article latestArticle = articleRepository.findLatestArticleByAuthorUsernameOrderByCreatedDate(userDetails.getUsername());
        if (latestArticle == null) {
            return true;
        }
        return this.isDifferenceMoreThanOneMinutes(latestArticle.getCreatedDate());
    }

    private boolean isCanEditWriteComment() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Article latestArticle = articleRepository.findLatestArticleByAuthorUsernameOrderByUpdatedDate(userDetails.getUsername());
        if (latestArticle == null || latestArticle.getUpdatedDate() == null) {
            return true;
        }
        return this.isDifferenceMoreThanOneMinutes(latestArticle.getUpdatedDate());
    }

    private boolean isDifferenceMoreThanOneMinutes(LocalDateTime localDateTime) {
        LocalDateTime dateAsLocalDateTime = new Date().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        Duration duration = Duration.between(localDateTime, dateAsLocalDateTime);

        return Math.abs(duration.toMinutes()) > 1;
    }


    @Async
    public CompletableFuture<Article> getArticle(Long boardId, Long articleId) throws JsonProcessingException {
        Optional<Board> board = boardRepository.findById(boardId);
        Optional<Article> article = articleRepository.findById(articleId);

        if(board.isEmpty()){
            throw new ResourceNotFoundException("board not found");
        }
        if(article.isEmpty() || article.get().getIsDeleted()){
            throw new ResourceNotFoundException("article not found");
        }

        article.get().setViewCount(article.get().getViewCount() + 1);
        articleRepository.save(article.get());
        String articleJson = objectMapper.writeValueAsString(article);

        elasticSearchService.indexArticleDocument(article.get().getId().toString(), articleJson).block();
        return CompletableFuture.completedFuture(article.get());
    }

    @Async
    public CompletableFuture<List<Comment>> getComments(Long articleId){
        return CompletableFuture.completedFuture(commentRepository.findByArticleId(articleId));
    }

    public CompletableFuture<Article> getArticleWithComment(Long boardId, Long articleId) throws JsonProcessingException {
        CompletableFuture<Article> getArticleRes = this.getArticle(boardId, articleId);
        CompletableFuture<List<Comment>> getCommentsRes = this.getComments(articleId);

        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(getArticleRes, getCommentsRes);

        return CompletableFuture.allOf(getArticleRes, getCommentsRes)
                .thenApply(voidResult -> {
                    try {
                        Article article = getArticleRes.get();
                        List<Comment> comments = getCommentsRes.get();
                        article.setComments(comments);
                        return article;
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        return null;
                    }
                });
    }

}
