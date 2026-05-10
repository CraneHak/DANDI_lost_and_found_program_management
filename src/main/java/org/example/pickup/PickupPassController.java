package org.example.pickup;

import org.example.auth.FirebaseAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/pickup-passes")
public class PickupPassController {
    private final PickupPassService pickupPassService;

    public PickupPassController(PickupPassService pickupPassService) {
        this.pickupPassService = pickupPassService;
    }

    @PostMapping
    public PickupPassService.IssuePickupPassResponse issuePickupPass(
            Authentication authentication,
            @RequestBody PickupPassService.IssuePickupPassRequest request
    ) {
        return pickupPassService.issuePass(requireFirebaseToken(authentication), request);
    }

    @PostMapping("/verify")
    public PickupPassService.VerifyPickupPassResponse verifyPickupPass(
            Authentication authentication,
            @RequestBody PickupPassService.VerifyPickupPassRequest request
    ) {
        return pickupPassService.verifyPass(requireFirebaseToken(authentication), request);
    }

    private FirebaseAuthenticationToken requireFirebaseToken(Authentication authentication) {
        if (!(authentication instanceof FirebaseAuthenticationToken token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }
        return token;
    }
}
