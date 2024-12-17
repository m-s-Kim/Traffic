package com.commerce.backend.service;

import com.commerce.backend.entity.BalanceGame;
import com.commerce.backend.repository.BalanceGameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BalanceGameService {
    @Autowired
    private BalanceGameRepository balanceGameRepository;

    public BalanceGame createGame(String question, String optionA, String optionB) {
        BalanceGame game = new BalanceGame();
        game.setQuestion(question);
        game.setOptionA(optionA);
        game.setOptionB(optionB);
        return balanceGameRepository.save(game);
    }

    public List<BalanceGame> getGames() {
        return balanceGameRepository.findAll();
    }

    public BalanceGame vote(Long gameId, String selectedOption) {
        BalanceGame game = balanceGameRepository.findById(gameId).orElseThrow();
        if (selectedOption.equals("A")) {
            game.setVotesA(game.getVotesA() + 1);
        } else {
            game.setVotesB(game.getVotesB() + 1);
        }
        return balanceGameRepository.save(game);
    }
}