package com.hong.ForPaw.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public URL generatePresignedUrl(Long userId, HttpMethod httpMethod) {
        // 만료 시간 설정 (현재 시간으로부터 5분 후)
        Date expiration = new Date(System.currentTimeMillis() + 1000 * 60 * 5);

        // 현재 시간, userId를 기반으로 키 생성
        String objectKey = createKey(userId);

        GeneratePresignedUrlRequest preSignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, objectKey)
                        .withMethod(httpMethod)
                        .withExpiration(expiration);

        return amazonS3.generatePresignedUrl(preSignedUrlRequest);
    }

    // URL에서 도메인 이름을 제거하고 오브젝트 키 부분만 추출
    public String extractKeyFromUrl(String s3Url) {
        int startIndex = s3Url.indexOf(bucketName) + bucketName.length() + 1;
        return s3Url.substring(startIndex);
    }

    public void deleteObject(String objectKey) {
        amazonS3.deleteObject(bucketName, objectKey);
    }

    private String createKey(Long userId) {
        String formattedNowTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString();
        return userId + "/" + formattedNowTime + "-" + uuid;
    }
}