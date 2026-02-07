package com.guideon.core.api;

import com.guideon.core.dto.AcceptInviteCommand;
import com.guideon.core.dto.AcceptInviteResult;
import com.guideon.core.dto.CreateInviteCommand;
import com.guideon.core.dto.InviteDto;
import com.guideon.core.service.AdminInviteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin Internal API Controller
 * BFF에서 Feign Client로 호출하는 내부 API
 * 인증 없음 (내부망 전용)
 */
@RestController
@RequestMapping("/internal/v1/admin")
@RequiredArgsConstructor
public class AdminInternalController {

    private final AdminInviteService adminInviteService;

    @PostMapping("/invites")
    public ResponseEntity<InviteDto> createInvite(@RequestBody CreateInviteCommand command) {
        InviteDto invite = adminInviteService.createInvite(command);
        return ResponseEntity.ok(invite);
    }

    @GetMapping("/invites")
    public ResponseEntity<List<InviteDto>> listInvites() {
        List<InviteDto> invites = adminInviteService.listInvites();
        return ResponseEntity.ok(invites);
    }

    @PostMapping("/invites/{inviteId}/expire")
    public ResponseEntity<Void> expireInvite(@PathVariable Long inviteId) {
        adminInviteService.expireInvite(inviteId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/invites/accept")
    public ResponseEntity<AcceptInviteResult> acceptInvite(@RequestBody AcceptInviteCommand command) {
        AcceptInviteResult result = adminInviteService.acceptInvite(command);
        return ResponseEntity.ok(result);
    }
}
