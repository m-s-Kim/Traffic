package com.commerce.backend.repository;

import com.commerce.backend.entity.BalanceGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BalanceGameRepository extends JpaRepository<BalanceGame, Long> {

}