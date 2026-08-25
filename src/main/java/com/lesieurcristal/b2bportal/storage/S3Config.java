package com.lesieurcristal.b2bportal.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;

@Slf4j
@Configuration
public class S3Config {

    @Bean(destroyMethod = "close")
    S3Client s3Client(S3Properties properties) {
        return S3Client.builder()
                .endpointOverride(URI.create(properties.endpoint()))
                .region(Region.of(properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())))
                .forcePathStyle(properties.pathStyleAccess())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }

    @Bean
    ObjectStorage objectStorage(S3Client s3Client, S3Properties properties) {
        return new S3ObjectStorage(s3Client, properties.bucket());
    }

    @Bean
    ApplicationRunner s3BucketInitializer(S3Client s3Client, S3Properties properties) {
        return args -> ensureBucket(s3Client, properties);
    }

    static void ensureBucket(S3Client s3Client, S3Properties properties) {
        String bucket = properties.bucket();
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.info("S3 bucket ready: {}", bucket);
        } catch (NoSuchBucketException missing) {
            createBucket(s3Client, properties, bucket);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                createBucket(s3Client, properties, bucket);
                return;
            }
            throw new IllegalStateException(
                    "MinIO/S3 injoignable (" + properties.endpoint() + ") : " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "MinIO/S3 injoignable (" + properties.endpoint() + ") : " + e.getMessage(), e);
        }
    }

    private static void createBucket(S3Client s3Client, S3Properties properties, String bucket) {
        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("S3 bucket created: {}", bucket);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Impossible de créer le bucket S3 " + bucket + " sur " + properties.endpoint(), e);
        }
    }
}
