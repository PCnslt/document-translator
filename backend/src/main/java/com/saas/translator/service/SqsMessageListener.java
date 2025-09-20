package com.saas.translator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;

import java.util.List;

@Service
public class SqsMessageListener {

    private final ContentModerationService contentModerationService;
    private final TranslationService translationService;
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String queueUrl;

    public SqsMessageListener(ContentModerationService contentModerationService, TranslationService translationService, SqsClient sqsClient, String queueUrl) {
        this.contentModerationService = contentModerationService;
        this.translationService = translationService;
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
        this.objectMapper = new ObjectMapper();
    }

    public void pollMessages() {
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(20)
                .build();

        List<Message> messages = sqsClient.receiveMessage(receiveRequest).messages();

        for (Message message : messages) {
            try {
                JsonNode jsonNode = objectMapper.readTree(message.body());
                String jobId = jsonNode.get("jobId").asText();
                String s3Key = jsonNode.get("s3Key").asText();
                String targetLanguage = jsonNode.get("targetLanguage").asText();

                System.out.println("Received SQS message: jobId=" + jobId + ", s3Key=" + s3Key + ", targetLanguage=" + targetLanguage);

                // Download file from S3, decrypt if needed
                String documentText = ""; // Placeholder for extracted text
                try {
                    // Download file bytes from S3
                    byte[] fileBytes = downloadFileFromS3(s3Key);

                    // Decrypt file bytes if needed (assuming encrypted with KMS)
                    byte[] decryptedBytes = decryptFileBytes(fileBytes);

                    // Extract text from document (PDF/DOCX)
                    documentText = extractTextFromDocument(decryptedBytes);

                    // Content moderation check
                    boolean isAllowed = contentModerationService.isContentAllowed(documentText);
                    if (!isAllowed) {
                        System.err.println("Content moderation failed for jobId=" + jobId);
                        // TODO: Update job status to FAILED in DynamoDB
                        return;
                    }

                    // Assume source language is English for now
                    String sourceLanguage = "en";

                    // Call translation service and block to get result
                    String translatedText = translationService.translateText(documentText, sourceLanguage, targetLanguage).block();

                    // TODO: Store translation result and update job status in DynamoDB
                    // TODO: Implement updateJobStatus method to store translation result and update job status
                    // Placeholder implementation
                    System.out.println("Job " + jobId + " completed with translation.");

                } catch (Exception e) {
                    System.err.println("Error processing jobId=" + jobId + ": " + e.getMessage());
                    // TODO: Send message to dead-letter queue if needed
                    System.err.println("Job " + jobId + " failed.");
                }
                // Delete message after processing
                DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build();
                sqsClient.deleteMessage(deleteRequest);
            } catch (Exception e) {
                System.err.println("Failed to process SQS message: " + e.getMessage());
                // TODO: Send message to dead-letter queue if needed
            }
        }
    }

    private byte[] downloadFileFromS3(String s3Key) {
        // TODO: Implement S3 file download logic
        return new byte[0];
    }

    private byte[] decryptFileBytes(byte[] encryptedBytes) {
        // TODO: Implement KMS decryption logic
        return encryptedBytes;
    }

    private String extractTextFromDocument(byte[] documentBytes) {
        // TODO: Implement text extraction from PDF/DOCX
        return "";
    }
}
