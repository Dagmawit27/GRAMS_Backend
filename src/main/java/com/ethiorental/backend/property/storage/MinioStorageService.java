package com.ethiorental.backend.property.storage;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Thin wrapper around MinIO.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket.property-images:property-images}")
    private String imageBucket;

    @Value("${minio.bucket.ownership-docs:ownership-docs}")
    private String docBucket;

    // ── Upload ────────────────────────────────────────────────────────────────

    public String uploadPropertyImage(MultipartFile file, UUID propertyId) {
        return upload(file, imageBucket, "properties/" + propertyId + "/images/");
    }

    public String uploadOwnershipDocument(MultipartFile file, UUID propertyId) {
        return upload(file, docBucket, "properties/" + propertyId + "/documents/");
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void deleteObject(String bucket, String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete object from MinIO: bucket={}, object={}", bucket, objectName, e);
            throw new RuntimeException("Failed to delete object: " + objectName, e);
        }
    }

    // ── Pre-signed URL (for direct browser download) ─────────────────────────

    public String getPresignedUrl(String bucket, String objectName, int expiryMinutes) {
        try {
            String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectName)
                    .expiry(expiryMinutes, TimeUnit.MINUTES)
                    .build());
            log.info("Generated presigned URL for bucket={}, object={}: {}", bucket, objectName, url);
            return url;
        } catch (Exception e) {
            log.error("Failed to generate pre-signed URL for bucket={}, object={}", bucket, objectName, e);
            return objectName;
        }
    }

    public String resolveImageUrl(String objectName) {
        if (objectName == null || objectName.isBlank()) return "";
        if (objectName.startsWith("http://") || objectName.startsWith("https://")) {
            return objectName;
        }
        // Generate presigned URL for direct browser access
        String presignedUrl = getPresignedUrl(imageBucket, objectName, 60 * 24 * 7); // 7-day presigned URL
        log.info("Resolved image URL for object {}: {}", objectName, presignedUrl);
        return presignedUrl;
    }

    public String resolveDocumentUrl(String objectName) {
        if (objectName == null || objectName.isBlank()) return "";
        if (objectName.startsWith("http://") || objectName.startsWith("https://")) {
            return objectName;
        }
        return getPresignedUrl(docBucket, objectName, 60 * 24 * 7); // 7-day presigned URL
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private String upload(MultipartFile file, String bucket, String prefix) {
        ensureBucketExists(bucket);
        String originalFilename = file.getOriginalFilename();
        String safeFilename = (originalFilename != null && !originalFilename.isBlank())
                ? originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_")
                : "file";
        String objectName = prefix + UUID.randomUUID() + "_" + safeFilename;
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(contentType)
                    .build());
            log.info("Successfully uploaded file to MinIO bucket={}, objectName={}", bucket, objectName);
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO bucket={}, objectName={}", bucket, objectName, e);
            throw new RuntimeException("Failed to upload file to MinIO: " + e.getMessage(), e);
        }
        return objectName;
    }

    private void ensureBucketExists(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket: {}", bucket);
            }
        } catch (Exception e) {
            log.error("Failed to ensure MinIO bucket exists: {}", bucket, e);
            throw new RuntimeException("Failed to access MinIO storage (" + bucket + "). Please ensure MinIO docker container is running.", e);
        }
    }
}
