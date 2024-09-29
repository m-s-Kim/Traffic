package com.commerce.backend.service;

import com.commerce.backend.dto.EditArticleDto;
import com.commerce.backend.dto.WriteArticleDto;
import com.commerce.backend.entity.Article;
import com.commerce.backend.entity.Board;
import com.commerce.backend.entity.User;
import com.commerce.backend.exception.ForbiddenException;
import com.commerce.backend.exception.RateLimitException;
import com.commerce.backend.exception.ResourceNotFoundException;
import com.commerce.backend.pojo.WriteArticle;
import com.commerce.backend.repository.ArticleRepository;
import com.commerce.backend.repository.BoardRepository;
import com.commerce.backend.repository.CommentRepository;
import com.commerce.backend.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

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
import java.util.concurrent.ExecutionException;


@Slf4j
@Service
public class ArticleService {

    private final BoardRepository boardRepository;
    private final ArticleRepository articleRepository;
    private final Board board;
    private final UserRepository userRepository;
    private final ElasticSearchService elasticSearchService;
    private final ObjectMapper objectMapper;
    private final RabbitMQSender rabbitMQSender;


    @Autowired
    public ArticleService(BoardRepository boardRepository, ArticleRepository articleRepository, Board board, UserRepository userRepository
        , ElasticSearchService elasticSearchService, ObjectMapper objectMapper, RabbitMQSender rabbitMQSender) {
        this.boardRepository = boardRepository;
        this.articleRepository = articleRepository;
        this.board = board;
        this.userRepository = userRepository;
        this.elasticSearchService = elasticSearchService;
        this.objectMapper = objectMapper;
        this.rabbitMQSender = rabbitMQSender;
    }

    @Transactional
    public Article writeArticle(Long boardId, WriteArticleDto writeArticleDto) throws JsonProcessingException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        if (!this.isCanWriteArticle()) {
            throw new RateLimitException("article not written by rate limit");
        }
        Optional<User> author = userRepository.findByUsername(userDetails.getUsername());
        Optional<Board> board = boardRepository.findById(boardId);
        if (author.isEmpty()) {
            throw new ResourceNotFoundException("author not found");
        }
        if (board.isEmpty()) {
            throw new ResourceNotFoundException("board not found");
        }

        Article article = new Article();
        article.setBoard(board.get());
        article.setAuthor(author.get());
        article.setTitle(writeArticleDto.getTitle());
        article.setContent(writeArticleDto.getContent());
        articleRepository.save(article);
        this.indexArticle(article);

        WriteArticle articleNotification = new WriteArticle();
        articleNotification.setArticleId(article.getId());
        articleNotification.setUserId(author.get().getId());

        rabbitMQSender.send(articleNotification);
        return article;
    }

    public List<Article> firstGetArticle(Long boardId){
        return articleRepository.findTop10ByBoardIdOrderByCreatedDateDesc(boardId);
    }

    public List<Article> getOldArticle(Long boardId, Long articleId) {
        return articleRepository.findTop10ByBoardIdAndArticleIdLessThanOrderByCreatedDateDesc(boardId, articleId);
    }

    public List<Article> getNewArticle(Long boardId, Long articleId) {
        return articleRepository.findTop10ByBoardIdAndArticleIdGreaterThanOrderByCreatedDateDesc(boardId, articleId);
    }


    @Transactional
    public Article editArticle(Long boardId, Long articleId, EditArticleDto dto) throws JsonProcessingException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Optional<User> author = userRepository.findByUsername(userDetails.getUsername());
        Optional<Board> board = boardRepository.findById(boardId);
        if (author.isEmpty()) {
            throw new ResourceNotFoundException("author not found");
        }
        if (board.isEmpty()) {
            throw new ResourceNotFoundException("board not found");
        }
        Optional<Article> article = articleRepository.findById(articleId);
        if (article.isEmpty()) {
            throw new ResourceNotFoundException("article not found");
        }
        if (article.get().getAuthor() != author.get()) {
            throw new ForbiddenException("article author different");
        }
        if (!this.isCanEditArticle()) {
            throw new RateLimitException("article not edited by rate limit");
        }
        if (dto.getTitle() != null) {
            article.get().setTitle(dto.getTitle().get());
        }
        if (dto.getContent() != null) {
            article.get().setContent(dto.getContent().get());
        }
        articleRepository.save(article.get());
        this.indexArticle(article.get());
        return article.get();
    }

    private boolean isCanWriteArticle() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Article latestArticle = articleRepository.findLatestArticleByAuthorUsernameOrderByCreatedDate(userDetails.getUsername());
        if (latestArticle == null) {
            return true;
        }
        return this.isDifferenceMoreThanFiveMinutes(latestArticle.getCreatedDate());
    }

    private boolean isCanEditArticle() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Article latestArticle = articleRepository.findLatestArticleByAuthorUsernameOrderByUpdatedDate(userDetails.getUsername());
        if (latestArticle == null || latestArticle.getUpdatedDate() == null) {
            return true;
        }
        return this.isDifferenceMoreThanFiveMinutes(latestArticle.getUpdatedDate());
    }

    private boolean isDifferenceMoreThanFiveMinutes(LocalDateTime localDateTime) {
        LocalDateTime dateAsLocalDateTime = new Date().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        Duration duration = Duration.between(localDateTime, dateAsLocalDateTime);

        return Math.abs(duration.toMinutes()) > 5;
    }
    @Transactional
    public boolean deleteArticle(Long boardId, Long articleId) throws JsonProcessingException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Optional<User> author = userRepository.findByUsername(userDetails.getUsername());
        Optional<Board> board = boardRepository.findById(boardId);
        if (author.isEmpty()) {
            throw new ResourceNotFoundException("author not found");
        }
        if (board.isEmpty()) {
            throw new ResourceNotFoundException("board not found");
        }
        Optional<Article> article = articleRepository.findById(articleId);
        if (article.isEmpty()) {
            throw new ResourceNotFoundException("article not found");
        }
        if (article.get().getAuthor() != author.get()) {
            throw new ForbiddenException("article author different");
        }
//        if (!this.isCanEditArticle()) {
//            throw new RateLimitException("article not edited by rate limit");
//        }
        article.get().setIsDeleted(true);
        articleRepository.save(article.get());
        this.indexArticle(article.get());
        return true;
    }


    public String indexArticle(Article article) throws JsonProcessingException {
        String articleJson = objectMapper.writeValueAsString(article);
        return elasticSearchService.indexArticleDocument(article.getId().toString(), articleJson).block();
    }

    public List<Article> searchArticle(String keyword) {
        Mono<List<Long>> articleIds = elasticSearchService.articleSearch(keyword);
        try {
            return articleRepository.findAllById(articleIds.toFuture().get());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
