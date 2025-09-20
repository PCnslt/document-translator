package com.saas.translator.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class TranslationService {

    private final WebClient webClient;

    @Autowired
    public TranslationService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://api-inference.huggingface.co/models/facebook/mbart-large-50-many-to-many-mmt").build();
    }

    public Mono<String> translateText(String text, String sourceLang, String targetLang) {
        // Prepare request payload
        var payload = new TranslationRequest(text, sourceLang, targetLang);

        return webClient.post()
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .retryBackoff(3, java.time.Duration.ofSeconds(1))
                .onErrorResume(e -> Mono.error(new RuntimeException("Translation service failed", e)));
    }

    private static class TranslationRequest {
        public String inputs;
        public TranslationRequestParameters parameters;

        public TranslationRequest(String text, String sourceLang, String targetLang) {
            this.inputs = text;
            this.parameters = new TranslationRequestParameters(sourceLang, targetLang);
        }
    }

    private static class TranslationRequestParameters {
        public String source_language;
        public String target_language;

        public TranslationRequestParameters(String sourceLang, String targetLang) {
            this.source_language = sourceLang;
            this.target_language = targetLang;
        }
    }
}
