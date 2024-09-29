package com.commerce.backend.repository;

import java.time.LocalDateTime;
import java.util.List;


import com.commerce.backend.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    @Query("SELECT n FROM Notice n WHERE n.createdDate >= :startDate ORDER BY n.createdDate")
    List<Notice> findByCreatedDate(@Param("startDate") LocalDateTime startDate);
}