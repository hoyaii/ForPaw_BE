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
        // 만료 시간은 5분
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 60 * 5;
        expiration.setTime(expTimeMillis);

        // 현재 시간, userId를 기반으로 키 생성
        String objectKey = createKey(userId);

        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucketName, objectKey)
                        .withMethod(httpMethod)
                        .withExpiration(expiration);
        return amazonS3.generatePresignedUrl(generatePresignedUrlRequest);
    }

    private String createKey(Long userId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String formattedDateTime = LocalDateTime.now().format(formatter);
        String uuid = UUID.randomUUID().toString();

        return userId + "/" + formattedDateTime + "-" + uuid;
    }

    // S3에서 이미지를 삭제하는 메소드를 추가합니다.
    public void deleteImage(String objectKey) {
        amazonS3.deleteObject(bucketName, objectKey);
    }

    // URI에서 도메인 이름을 제거하고 오브젝트 키 부분만 추출
    public String extractObjectKeyFromUri(String s3Uri) {
        int startIndex = s3Uri.indexOf(bucketName) + bucketName.length() + 1;
        return s3Uri.substring(startIndex);
    }
}