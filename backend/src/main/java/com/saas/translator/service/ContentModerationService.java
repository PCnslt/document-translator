package com.saas.translator.service;

import org.springframework.stereotype.Service;

@Service
public class ContentModerationService {

    // Simple placeholder for content moderation logic
    public boolean isContentAllowed(String text) {
        // Implement real content moderation logic, e.g., call to external moderation API
        // For now, reject if text contains banned words (example)
        String lowerText = text.toLowerCase();
        String[] bannedWords = {"bannedword1", "bannedword2"};
        for (String word : bannedWords) {
            if (lowerText.contains(word)) {
                return false;
            }
        }
        return true;
    }
}
