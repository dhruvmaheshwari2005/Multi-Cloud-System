package com.multicloud.costmonitor.controller;

import com.multicloud.costmonitor.model.SentEmail;
import com.multicloud.costmonitor.repository.SentEmailRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/emails")
public class EmailSandboxController {

    private final SentEmailRepository sentEmailRepository;

    public EmailSandboxController(SentEmailRepository sentEmailRepository) {
        this.sentEmailRepository = sentEmailRepository;
    }

    @GetMapping("/sent")
    public ResponseEntity<List<SentEmail>> getSentEmails() {
        return ResponseEntity.ok(sentEmailRepository.findAllByOrderBySentAtDesc());
    }

    @DeleteMapping("/sent/clear")
    public ResponseEntity<?> clearInbox() {
        sentEmailRepository.deleteAll();
        return ResponseEntity.ok().body("{\"message\":\"Inbox cleared successfully\"}");
    }
}
