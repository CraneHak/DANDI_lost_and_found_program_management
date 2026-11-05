package org.example.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class S3Service {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region}")
    private String region;

    public S3Service(
            @Value("${cloud.aws.credentials.access-key}") String accessKey,
            @Value("${cloud.aws.credentials.secret-key}") String secretKey,
            @Value("${cloud.aws.region}") String region
    ) {
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    /** 업로드 허용 형식 → 확장자. 사진 외 파일(HTML·SVG 등)은 받지 않는다. */
    private static final Map<String, String> IMAGE_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif",
            "image/heic", "heic",
            "image/heif", "heif"
    );

    public String upload(MultipartFile file) throws IOException {
        return uploadBytes(file.getBytes(), file.getContentType(), file.getOriginalFilename());
    }

    /** filename은 받지만 S3 키에 쓰지 않는다 (원본 파일명에 이름·학번이 들어갈 수 있음). */
    public String uploadBytes(byte[] bytes, String contentType, String filename) {
        String resolvedType = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
        String ext = IMAGE_TYPES.get(resolvedType);
        if (ext == null) {
            throw new IllegalArgumentException("사진 파일(JPG, PNG, WEBP, GIF, HEIC)만 업로드할 수 있습니다.");
        }
        String key = "lost-items/" + UUID.randomUUID() + "." + ext;

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(resolvedType)
                        .build(),
                software.amazon.awssdk.core.sync.RequestBody.fromBytes(bytes)
        );

        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    /** S3 URL이면 객체 삭제 (DB 삭제 시 고아 파일 방지, 실패 시 무시) */
    public void deleteByUrlIfPresent(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank() || !imageUrl.contains(".amazonaws.com/")) {
            return;
        }
        String prefix = ".amazonaws.com/";
        int idx = imageUrl.indexOf(prefix);
        if (idx < 0) {
            return;
        }
        String key = imageUrl.substring(idx + prefix.length());
        if (key.isBlank()) {
            return;
        }
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build()
            );
        } catch (RuntimeException ignored) {
            // S3 삭제 실패해도 DB 삭제는 진행
        }
    }
}
