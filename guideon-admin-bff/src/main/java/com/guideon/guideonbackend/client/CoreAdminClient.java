package com.guideon.guideonbackend.client;

import com.guideon.core.dto.AcceptInviteCommand;
import com.guideon.core.dto.AcceptInviteResult;
import com.guideon.core.dto.CreateInviteCommand;
import com.guideon.core.dto.InviteDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Core Service Admin API 호출용 Feign Client
 */
@FeignClient(name = "core-admin", url = "${core.service.url}")
public interface CoreAdminClient {

    @PostMapping("/internal/v1/admin/invites")
    InviteDto createInvite(@RequestBody CreateInviteCommand command);

    @GetMapping("/internal/v1/admin/invites")
    List<InviteDto> listInvites();

    @PostMapping("/internal/v1/admin/invites/{inviteId}/expire")
    void expireInvite(@PathVariable("inviteId") Long inviteId);

    @PostMapping("/internal/v1/admin/invites/accept")
    AcceptInviteResult acceptInvite(@RequestBody AcceptInviteCommand command);
}
