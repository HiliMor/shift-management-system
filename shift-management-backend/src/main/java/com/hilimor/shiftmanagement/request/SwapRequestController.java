package com.hilimor.shiftmanagement.request;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requests")
public class SwapRequestController {

    private final SwapRequestService swapRequestService;

    public SwapRequestController(SwapRequestService swapRequestService) {
        this.swapRequestService = swapRequestService;
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public SwapRequestResponse createTransferRequest(
            Authentication authentication,
            @Valid @RequestBody CreateTransferRequest request
    ) {
        return swapRequestService.createTransferRequest(authentication.getName(), request);
    }

    @PostMapping("/{requestId}/employee-approve")
    public SwapRequestResponse approveByTargetEmployee(
            Authentication authentication,
            @PathVariable Long requestId
    ) {
        return swapRequestService.approveByTargetEmployee(authentication.getName(), requestId);
    }

    @PostMapping("/{requestId}/manager-approve")
    public SwapRequestResponse approveByManager(
            Authentication authentication,
            @PathVariable Long requestId
    ) {
        return swapRequestService.approveByManager(authentication.getName(), requestId);
    }
}
