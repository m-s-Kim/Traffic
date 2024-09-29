package com.commerce.backend.service;

import com.commerce.backend.repository.NoticeRepository;
import com.commerce.backend.repository.UserNotificationHistoryRepository;
import com.commerce.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class NoticeService {

    private final NoticeRepository noticeRepository;

    private final UserRepository userRepository;

    private final UserNotificationHistoryRepository userNotificationHistoryRepository;

    public NoticeService(NoticeRepository noticeRepository, UserRepository userRepository, UserNotificationHistoryRepository userNotificationHistoryRepository) {
        this.noticeRepository = noticeRepository;
        this.userRepository = userRepository;
        this.userNotificationHistoryRepository = userNotificationHistoryRepository;
    }
}
