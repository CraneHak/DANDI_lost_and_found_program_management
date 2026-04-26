package org.example.report;

public class ReportNotFoundException extends RuntimeException {
    public ReportNotFoundException(Long id) {
        super("신고 항목을 찾을 수 없습니다. id=" + id);
    }
}
