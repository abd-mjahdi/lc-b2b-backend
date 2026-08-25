package com.lesieurcristal.b2bportal.storage;

import lombok.extern.slf4j.Slf4j;
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
        S3Properties s3 = requireValid(properties);
        return S3Client.builder()
                .endpointOverride(URI.create(s3.endpoint()))
                .region(Region.of(s3.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3.accessKey(), s3.secretKey())))
                .forcePathStyle(s3.pathStyleAccess())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }

    @Bean
    ObjectStorage objectStorage(S3Client s3Client, S3Properties properties) {
        S3Properties s3 = requireValid(properties);
        ensureBucket(s3Client, s3);
        return new S3ObjectStorage(s3Client, s3.bucket());
    }

    static S3Properties requireValid(S3Properties properties) {
        String endpoint = trim(properties.endpoint());
        String region = trim(properties.region());
        String bucket = trim(properties.bucket());
        String accessKey = trim(properties.accessKey());
        String secretKey = trim(properties.secretKey());
        if (endpoint == null || bucket == null || accessKey == null || secretKey == null) {
            throw new IllegalStateException(
                    "Configuration S3 incomplète : S3_ENDPOINT, S3_BUCKET, S3_ACCESS_KEY et S3_SECRET_KEY sont requis");
        }
        if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
            throw new IllegalStateException(
                    "S3_ENDPOINT doit commencer par http:// ou https:// (reçu : " + endpoint + ")");
        }
        if (region == null) {
            region = "us-east-1";
        }
        return new S3Properties(endpoint, region, bucket, accessKey, secretKey, properties.pathStyleAccess());
    }

    static void ensureBucket(S3Client s3Client, S3Properties properties) {
        String bucket = properties.bucket();
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.info("S3 bucket ready: {}", bucket);
        } catch (NoSuchBucketException missing) {
            createBucket(s3Client, properties, bucket);
        } catch (S3Exception e) {
            if (e.statusCode() == 404 || isNoSuchBucket(e)) {
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
        } catch (S3Exception e) {
            if (e.statusCode() == 409 || isBucketAlreadyPresent(e)) {
                log.info("S3 bucket already present: {}", bucket);
                return;
            }
            throw new IllegalStateException(
                    "Impossible de créer le bucket S3 " + bucket + " sur " + properties.endpoint(), e);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Impossible de créer le bucket S3 " + bucket + " sur " + properties.endpoint(), e);
        }
    }

    private static boolean isNoSuchBucket(S3Exception e) {
        String code = errorCode(e);
        return "NoSuchBucket".equals(code) || "NotFound".equals(code);
    }

    private static boolean isBucketAlreadyPresent(S3Exception e) {
        String code = errorCode(e);
        return "BucketAlreadyOwnedByYou".equals(code) || "BucketAlreadyExists".equals(code);
    }

    private static String errorCode(S3Exception e) {
        if (e.awsErrorDetails() == null || e.awsErrorDetails().errorCode() == null) {
            return "";
        }
        return e.awsErrorDetails().errorCode();
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
