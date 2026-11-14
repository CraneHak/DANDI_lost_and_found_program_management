package org.example.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Set;

/**
 * 신분증·카드 사진은 Vision 모자이크를 거친 사진만 공개할 수 있다.
 * 화면의 확인창은 우회할 수 있으므로 서버에서 공개 시점에 한 번 더 막는다.
 */
public final class SensitiveImagePolicy {

    public static final String MESSAGE = "신분증·카드 사진은 Vision 모자이크를 적용한 뒤에만 공개할 수 있습니다.";

    private static final Set<String> SENSITIVE_TYPES = Set.of("신분증", "카드/티켓", "카드");
    private static final String MOSAIC_PATH = "/vision/mosaic/";

    private SensitiveImagePolicy() {
    }

    /** 물품 종류(쉼표로 여러 개 가능)에 신분증·카드가 있으면 true. 이름에 '카드'가 들어간 것(카드지갑 등)은 해당하지 않는다. */
    public static boolean isSensitive(String itemType) {
        if (itemType == null) {
            return false;
        }
        for (String part : itemType.split(",")) {
            if (SENSITIVE_TYPES.contains(part.trim())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMosaic(String imageUrl) {
        return imageUrl != null && imageUrl.contains(MOSAIC_PATH);
    }

    /** 신분증·카드인데 사진이 있고 모자이크가 아니면 공개를 거부한다. 사진이 없으면 통과한다. */
    public static void requireMasked(String itemType, String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        if (isSensitive(itemType) && !isMosaic(imageUrl)) {
            throw reject();
        }
    }

    /** 파일을 직접 올린 사진은 모자이크가 아니므로, 신분증·카드는 업로드 전에 거부한다. */
    public static void rejectRawUpload(String itemType) {
        if (isSensitive(itemType)) {
            throw reject();
        }
    }

    /** 같은 사진을 다시 보낸 경우(주소 표기만 다름)인지 비교한다. 경로만 보고 판단한다. */
    public static boolean sameImage(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return pathOf(a.trim()).equals(pathOf(b.trim()));
    }

    private static String pathOf(String url) {
        try {
            String path = URI.create(url).getPath();
            return path == null || path.isEmpty() ? url : path;
        } catch (IllegalArgumentException ex) {
            return url;
        }
    }

    private static ResponseStatusException reject() {
        return new ResponseStatusException(HttpStatus.CONFLICT, MESSAGE);
    }
}
