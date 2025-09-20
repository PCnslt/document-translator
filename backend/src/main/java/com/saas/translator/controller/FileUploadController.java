package com.saas.translator.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.EncryptRequest;
import software.amazon.awssdk.services.kms.model.EncryptResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    private final KmsClient kmsClient;
    private final S3Client s3Client;
    private final SqsClient sqsClient;

    @Value("${aws.kms.keyAlias}")
    private String kmsKeyId;

    @Value("${aws.s3.bucketName}")
    private String bucketName;

    @Value("${aws.sqs.queueUrl}")
    private String queueUrl;

    @Autowired
    public FileUploadController(KmsClient kmsClient, S3Client s3Client, SqsClient sqsClient) {
        this.kmsClient = kmsClient;
        this.s3Client = s3Client;
        this.sqsClient = sqsClient;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                        @RequestParam("targetLanguage") String targetLanguage) {
        // Validate file type and size (max 10MB)
        String contentType = file.getContentType();
        if (contentType == null ||
                !(contentType.equals("application/pdf") || contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body("Only PDF and DOCX files are supported.");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("File size exceeds 10MB limit.");
        }

        // TODO: Replace UUID v4 with UUID v7 for job ID generation
        // Placeholder: using UUID v4 until UUID v7 library is integrated
        String jobId = java.util.UUID.randomUUID().toString();

        try {
            byte[] fileBytes = file.getBytes();

            EncryptRequest encryptRequest = EncryptRequest.builder()
                    .keyId(kmsKeyId)
                    .plaintext(SdkBytes.fromByteArray(fileBytes))
                    .build();

            EncryptResponse encryptResponse = kmsClient.encrypt(encryptRequest);
            SdkBytes encryptedBlob = encryptResponse.ciphertextBlob();

            String s3Key = "uploads/" + jobId + "/" + file.getOriginalFilename();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .serverSideEncryption(ServerSideEncryption.AWS_KMS)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(encryptedBlob.asByteArray()));

            String messageBody = "{\"jobId\":\"" + jobId + "\",\"s3Key\":\"" + s3Key + "\",\"targetLanguage\":\"" + targetLanguage + "\"}";

            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .build();

            sqsClient.sendMessage(sendMsgRequest);

            // GDPR-compliant audit logging
            logger.info("File uploaded: jobId={}, s3Key={}, targetLanguage={}", jobId, s3Key, targetLanguage);

        } catch (Exception e) {
            logger.error("Failed to process file upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process file upload: " + e.getMessage());
        }

        return ResponseEntity.ok().body("{\"jobId\":\"" + jobId + "\"}");
    }
}
