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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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

    /** 업로드 허용 사진 형식 → 확장자 */
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

    /**
     * 사진만 올린다. 파일명은 S3 키에 쓰지 않는다 (원래 파일명에 이름·학번이 들어갈 수 있음).
     * 저장되는 Content-Type은 항상 사진 형식이라, 사진으로 위장한 웹페이지가 S3 주소로 열리지 않는다.
     */
    public String uploadBytes(byte[] bytes, String contentType, String filename) {
        String resolvedType = resolveImageType(bytes, contentType);
        String key = "lost-items/" + UUID.randomUUID() + "." + IMAGE_TYPES.get(resolvedType);

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

    /**
     * 파일 앞부분(시그니처)으로 사진 형식을 먼저 판별한다. 일부 휴대폰은 형식 정보를 비우거나
     * application/octet-stream으로 보내기 때문에 선언된 형식보다 실제 내용을 우선한다.
     * 시그니처로 모르면(드문 HEIC 등) 선언된 형식이 사진일 때만 받는다.
     */
    static String resolveImageType(byte[] bytes, String declaredType) {
        String sniffed = sniffImageType(bytes);
        if (sniffed != null) {
            return sniffed;
        }
        String declared = declaredType == null ? "" : declaredType.trim().toLowerCase(Locale.ROOT);
        if (IMAGE_TYPES.containsKey(declared)) {
            return declared.equals("image/jpg") ? "image/jpeg" : declared;
        }
        throw new IllegalArgumentException("사진 파일(JPG, PNG, WEBP, GIF, HEIC)만 업로드할 수 있습니다.");
    }

    private static String sniffImageType(byte[] b) {
        if (b == null || b.length < 12) {
            return null;
        }
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') {
            return "image/gif";
        }
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        if (b[4] == 'f' && b[5] == 't' && b[6] == 'y' && b[7] == 'p') {
            String brand = new String(b, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
            if (brand.startsWith("hei") || brand.startsWith("hev") || brand.equals("mif1") || brand.equals("msf1")) {
                return "image/heic";
            }
        }
        return null;
    }

    private static final long MAX_DOWNLOAD_BYTES = 15L * 1024 * 1024;

    /** 이 서비스의 사진 버킷 주소(https)인지 확인한다. 서버가 임의 주소를 열지 못하게 막는 검사. */
    static URI requireOwnBucketUrl(String imageUrl, String bucket, String region) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("사진 주소가 없습니다.");
        }
        URI uri;
        try {
            uri = URI.create(imageUrl.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("사진 주소가 올바르지 않습니다.");
        }
        String expectedHost = bucket + ".s3." + region + ".amazonaws.com";
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getUserInfo() != null
                || !expectedHost.equalsIgnoreCase(uri.getHost())) {
            throw new IllegalArgumentException("이 서비스에 올린 사진만 분석할 수 있습니다.");
        }
        return uri;
    }

    /** 사진 파일 앞부분으로 확장자를 정한다 (Vision 분석·모자이크용). 분석할 수 없는 형식이면 예외. */
    public static String detectImageExtension(byte[] bytes) {
        String type = sniffImageType(bytes);
        if ("image/jpeg".equals(type)) {
            return "jpeg";
        }
        if ("image/png".equals(type)) {
            return "png";
        }
        throw new IllegalArgumentException("분석할 수 없는 사진 형식입니다. (JPG, PNG만 가능)");
    }

    /** 우리 버킷에 올라간 사진을 내려받는다 (관리자가 사진을 다시 고르지 않고 Vision 분석을 하도록). */
    public byte[] downloadByUrl(String imageUrl) throws IOException {
        URI uri = requireOwnBucketUrl(imageUrl, bucket, region);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        try {
            HttpResponse<byte[]> response = client.send(
                    HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new IOException("사진을 불러오지 못했습니다. (HTTP " + response.statusCode() + ")");
            }
            byte[] body = response.body();
            if (body.length > MAX_DOWNLOAD_BYTES) {
                throw new IllegalArgumentException("사진 용량이 너무 큽니다.");
            }
            return body;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("사진을 불러오는 중 중단되었습니다.");
        }
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
