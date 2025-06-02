package com.ktb.marong.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController("/api/test-discord")
public class DiscordTestController {
    private static final Logger log = LoggerFactory.getLogger(DiscordTestController.class);

    @GetMapping
    public ResponseEntity<String> testDiscordWebhook() {
        log.error(">> TEST: Discord webhook 연동 확인용 에러 메시지");
        return ResponseEntity.ok("Discord webhook test triggered");
    }
}