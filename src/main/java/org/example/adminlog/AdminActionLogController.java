package org.example.adminlog;

import org.example.common.ApiTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** /api/admin/** 는 SecurityConfig에서 ADMIN만 허용 */
@RestController
@RequestMapping("/api/admin/action-logs")
public class AdminActionLogController {
    private final AdminActionLogService service;

    public AdminActionLogController(AdminActionLogService service) {
        this.service = service;
    }

    @GetMapping
    public List<ActionLogResponse> recent() {
        return service.recent().stream().map(ActionLogResponse::from).toList();
    }

    public record ActionLogResponse(
            String id,
            String action,
            String message,
            String targetId,
            String actorEmail,
            String createdAt
    ) {
        static ActionLogResponse from(AdminActionLog log) {
            return new ActionLogResponse(
                    String.valueOf(log.getId()),
                    log.getAction(),
                    log.getMessage(),
                    log.getTargetId(),
                    log.getActorEmail(),
                    ApiTime.utc(log.getCreatedAt())
            );
        }
    }
}
