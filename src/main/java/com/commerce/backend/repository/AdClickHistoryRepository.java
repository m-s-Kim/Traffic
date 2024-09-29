package com.commerce.backend.repository;

import com.commerce.backend.entity.AdClickHistory;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AdClickHistoryRepository extends MongoRepository<AdClickHistory, String> {
}
