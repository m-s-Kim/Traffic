//package com.commerce.backend.controller;
//
//import com.commerce.backend.entity.BalanceGame;
//import com.commerce.backend.service.BalanceGameService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.Map;
//@RestController
//@RequestMapping("/api/games")
//public class BalanceGameController {
//    @Autowired
//    private BalanceGameService balanceGameService;
//
//    @PostMapping("/create")
//    public ResponseEntity<BalanceGame> createGame(@RequestBody Map<String, String> payload) {
//        BalanceGame game = balanceGameService.createGame(payload.get("question"), payload.get("optionA"), payload.get("optionB"));
//        return ResponseEntity.ok(game);
//    }
//
//    @GetMapping
//    public List<BalanceGame> getGames() {
//        return balanceGameService.getGames();
//    }
//
//    @PostMapping("/vote/{gameId}")
//    public ResponseEntity<BalanceGame> vote(@PathVariable Long gameId, @RequestParam String option) {
//        BalanceGame game = balanceGameService.vote(gameId, option);
//        return ResponseEntity.ok(game);
//    }
//}
