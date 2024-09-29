package com.commerce.backend.controller;

import com.commerce.backend.dto.WriteCommentDto;
import com.commerce.backend.entity.Article;
import com.commerce.backend.entity.Comment;
import com.commerce.backend.service.CommentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/boards")
public class CommentController {
    private final CommentService commentService;

    @Autowired
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/{boardId}/articles/{articleId}/comments")
    public ResponseEntity<Comment> writeComment(@PathVariable Long boardId,
                                                @PathVariable Long articleId,
                                                @RequestBody WriteCommentDto writeCommentDto) throws JsonProcessingException {
        return ResponseEntity.ok(commentService.writeComment(boardId, articleId, writeCommentDto));
    }

//    @GetMapping("/{boardId}/articles/{articleId}")
//    public ResponseEntity<Article> getArticleWithComment(@PathVariable Long boardId, @PathVariable Long articleId) throws JsonProcessingException {
//        CompletableFuture<Article> article = commentService.getArticleWithComment(boardId, articleId);
//        return ResponseEntity.ok(article.resultNow());
//    }


    @PutMapping("/{boardId}/articles/{articleId}/comments/{commentId}")
    public ResponseEntity<Comment> editComment(@PathVariable Long boardId,
                                                @PathVariable Long articleId,
                                                @PathVariable Long commentId,
                                                @RequestBody WriteCommentDto editCommentDto) {
        return ResponseEntity.ok(commentService.editComment(boardId, articleId, commentId, editCommentDto));
    }

    @DeleteMapping("/{boardId}/articles/{articleId}/comments/{commentId}")
    public ResponseEntity<String> deleteComment(@PathVariable Long boardId,
                                               @PathVariable Long articleId,
                                               @PathVariable Long commentId) {
        commentService.deleteComment(boardId, articleId, commentId);
        return ResponseEntity.ok("comment is deleted");
    }

}
