package com.commerce.backend.repository;

import com.commerce.backend.entity.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {
}
