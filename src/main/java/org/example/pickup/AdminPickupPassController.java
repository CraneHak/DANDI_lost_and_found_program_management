package org.example.pickup;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 관리자 수령 인증 화면용 — 모든 사용자의 수령 코드. /api/admin/** 는 SecurityConfig에서 ADMIN만 허용. */
@RestController
@RequestMapping("/api/admin/pickup-passes")
public class AdminPickupPassController {
    private final PickupPassService pickupPassService;

    public AdminPickupPassController(PickupPassService pickupPassService) {
        this.pickupPassService = pickupPassService;
    }

    @GetMapping
    public List<PickupPassService.MyPickupPassResponse> allPickupPasses() {
        return pickupPassService.findAllPasses();
    }
}
