package com.ethiorental.backend.property.storage;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Thin wrapper around MinIO.
 * Replace MINIO_ENDPOINT / MINIO_ACCESS_KEY / MINIO_SECRET_KEY in .env
 * or application.properties to point to your MinIO instance.
 */
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket.property-images}")
    private String imageBucket;

    @Value("${minio.bucket.ownership-docs}")
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
            throw new RuntimeException("Failed to delete object: " + objectName, e);
        }
    }

    // ── Pre-signed URL (for direct browser download) ─────────────────────────

    public String getPresignedUrl(String bucket, String objectName, int expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectName)
                    .expiry(expiryMinutes, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate pre-signed URL", e);
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private String upload(MultipartFile file, String bucket, String prefix) {
        ensureBucketExists(bucket);
        String objectName = prefix + UUID.randomUUID() + "_" + file.getOriginalFilename();
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }
        return objectName; // store this path; generate presigned URL on demand
    }

    private void ensureBucketExists(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure bucket exists: " + bucket, e);
        }
    }
}
