package org.example.common;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 서버가 만든 시각(created_at 등)은 UTC로 저장된다 (Main에서 기본 시간대를 UTC로 고정, RDS도 UTC).
 * API로 내보낼 때 끝에 Z를 붙여 브라우저가 한국 시간으로 바꿔 보여주게 한다.
 * 사용자가 입력한 날짜(storedDate·lostAt 등)는 이 형식을 쓰지 않는다.
 */
public final class ApiTime {
    private static final DateTimeFormatter UTC_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private ApiTime() {
    }

    public static String utc(LocalDateTime serverTime) {
        if (serverTime == null) {
            return null;
        }
        return serverTime.truncatedTo(ChronoUnit.SECONDS).atOffset(ZoneOffset.UTC).format(UTC_ISO);
    }
}
