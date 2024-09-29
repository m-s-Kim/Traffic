package com.commerce.backend.repository;

import com.commerce.backend.entity.AdViewHistory;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AdViewHistoryRepository extends MongoRepository<AdViewHistory, String> {
}