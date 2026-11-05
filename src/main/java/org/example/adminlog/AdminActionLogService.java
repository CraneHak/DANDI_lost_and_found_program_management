package org.example.adminlog;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminActionLogService {
    /** 관리자 직접 등록 — 관리자 화면 '등록된 물품 목록'도 이 종류만 모아 보여준다 */
    public static final String REGISTER = "REGISTER";
    public static final String UPDATE_ITEM = "UPDATE_ITEM";
    public static final String DELETE_ITEM = "DELETE_ITEM";
    public static final String REVIEW = "REVIEW";
    public static final String EDIT_REPORT = "EDIT_REPORT";
    public static final String DELETE_REPORT = "DELETE_REPORT";
    public static final String PICKUP_VERIFY = "PICKUP_VERIFY";

    private final AdminActionLogRepository repository;

    public AdminActionLogService(AdminActionLogRepository repository) {
        this.repository = repository;
    }

    /** 호출한 쪽의 트랜잭션에 함께 묶인다 (작업이 롤백되면 기록도 남지 않음) */
    @Transactional
    public void record(String actorUid, String actorEmail, String action, String targetId, String message) {
        AdminActionLog log = new AdminActionLog();
        log.setActorUid(actorUid);
        log.setActorEmail(actorEmail);
        log.setAction(action);
        log.setTargetId(targetId);
        String text = message == null ? "" : message.trim();
        log.setMessage(text.length() > 500 ? text.substring(0, 500) : text);
        repository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AdminActionLog> recent() {
        return repository.findTop200ByOrderByCreatedAtDescIdDesc();
    }
}
