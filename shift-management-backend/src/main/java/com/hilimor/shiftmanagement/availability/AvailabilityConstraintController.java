package com.hilimor.shiftmanagement.availability;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/availability-constraints")
public class AvailabilityConstraintController {

    private final AvailabilityConstraintService availabilityConstraintService;

    public AvailabilityConstraintController(AvailabilityConstraintService availabilityConstraintService) {
        this.availabilityConstraintService = availabilityConstraintService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AvailabilityConstraintResponse createConstraint(
            Authentication authentication,
            @Valid @RequestBody CreateAvailabilityConstraintRequest request
    ) {
        return availabilityConstraintService.createConstraint(authentication.getName(), request);
    }

    @GetMapping("/me")
    public List<AvailabilityConstraintResponse> listMyConstraints(Authentication authentication) {
        return availabilityConstraintService.listMyConstraints(authentication.getName());
    }

    @DeleteMapping("/{constraintId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyConstraint(
            Authentication authentication,
            @PathVariable Long constraintId
    ) {
        availabilityConstraintService.deleteMyConstraint(authentication.getName(), constraintId);
    }
}
