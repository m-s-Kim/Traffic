package com.commerce.backend.batch;


import com.commerce.backend.dto.AdHistoryResult;
import com.commerce.backend.service.AdvertisementService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DailyStatTasks {
    private final AdvertisementService advertisementService;

    public DailyStatTasks(AdvertisementService advertisementService) {
        this.advertisementService = advertisementService;
    }

    @Scheduled(cron = "00 31 13 * * ?")
    public void insertAdViewStatAtMidnight() {
        List<AdHistoryResult> viewResult = advertisementService.getAdViewHistoryGroupedByAdId();
        advertisementService.insertAdViewStat(viewResult);
        List<AdHistoryResult> clickResult = advertisementService.getAdClickHistoryGroupedByAdId();
        advertisementService.insertAdClickStat(clickResult);
    }
}