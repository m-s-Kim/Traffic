package com.commerce.backend.entity;

import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
public class HotArticle implements Serializable {
    private Long id;

    private String title;

    private String content;

    private String authorName;

    private LocalDateTime createdDate;

    private LocalDateTime updatedDate;

    private Long viewCount = 0L;
}