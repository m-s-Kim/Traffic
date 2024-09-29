package com.commerce.backend.controller;

import com.commerce.backend.dto.AdHistoryResult;
import com.commerce.backend.dto.AdvertisementDto;
import com.commerce.backend.dto.EditArticleDto;
import com.commerce.backend.dto.WriteArticleDto;
import com.commerce.backend.entity.Advertisement;
import com.commerce.backend.entity.Article;
import com.commerce.backend.entity.Board;
import com.commerce.backend.service.AdvertisementService;
import com.commerce.backend.service.ArticleService;
import com.commerce.backend.service.CommentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AdvertisementController {

    private final AdvertisementService advertisementService;

    @Autowired
    public AdvertisementController(AdvertisementService advertisementService) {
        this.advertisementService = advertisementService;
    }

    @PostMapping("/admin/ads")
    public ResponseEntity<Advertisement> writeAd(@RequestBody AdvertisementDto advertisementDto) {
        return ResponseEntity.ok(advertisementService.writeAd(advertisementDto));
    }

    @GetMapping("/ads")
    public ResponseEntity<List<Advertisement>> getAdList() {
        return ResponseEntity.ok(advertisementService.getAdList());
    }

    @GetMapping("/ads/{adId}")
    public Object  getAd(@PathVariable Long adId, HttpServletRequest request, @RequestParam(required = false) Boolean isTrueView ) {
        String ip = request.getRemoteAddr();
        Optional<Advertisement> ad = advertisementService.getAd(adId, ip, isTrueView != null && isTrueView);
        if(ad.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ad);
    }

    @PostMapping("/ads/{adId}")
    public Object  clickAd(@PathVariable Long adId, HttpServletRequest request ) {
        String ip = request.getRemoteAddr();
        advertisementService.clickAd(adId, ip);
        return ResponseEntity.ok("click");
    }


    @GetMapping("/ads/history")
    public ResponseEntity<List<AdHistoryResult>> getAdHistory() {
        List<AdHistoryResult> result = advertisementService.getAdViewHistoryGroupedByAdId();
        advertisementService.insertAdViewStat(result);
        return ResponseEntity.ok(result);
    }







}
