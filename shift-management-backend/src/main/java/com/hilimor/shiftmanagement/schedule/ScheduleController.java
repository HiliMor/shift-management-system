package com.hilimor.shiftmanagement.schedule;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleResponse createSchedule(
            Authentication authentication,
            @Valid @RequestBody CreateScheduleRequest request
    ) {
        return scheduleService.createDraftSchedule(authentication.getName(), request);
    }

    @GetMapping("/me/published")
    public List<ScheduleResponse> listMyPublishedSchedules(Authentication authentication) {
        return scheduleService.listPublishedSchedulesForUser(authentication.getName());
    }

    @GetMapping("/me/published/{scheduleId}")
    public PublishedScheduleDetailsResponse getMyPublishedScheduleDetails(
            Authentication authentication,
            @PathVariable Long scheduleId
    ) {
        return scheduleService.getPublishedScheduleDetailsForUser(authentication.getName(), scheduleId);
    }

    @PostMapping("/{scheduleId}/publish")
    public ScheduleResponse publishSchedule(
            Authentication authentication,
            @PathVariable Long scheduleId
    ) {
        return scheduleService.publishSchedule(authentication.getName(), scheduleId);
    }

    @PostMapping("/{scheduleId}/reopen")
    public ScheduleResponse reopenSchedule(
            Authentication authentication,
            @PathVariable Long scheduleId
    ) {
        return scheduleService.reopenSchedule(authentication.getName(), scheduleId);
    }
}
