package org.example.notice;

public class NoticeNotFoundException extends RuntimeException {
    public NoticeNotFoundException(Long id) {
        super("알림을 찾을 수 없습니다. id=" + id);
    }
}
