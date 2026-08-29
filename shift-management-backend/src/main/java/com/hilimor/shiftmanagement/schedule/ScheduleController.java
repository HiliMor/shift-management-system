package com.hilimor.shiftmanagement.schedule;

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

    @DeleteMapping("/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDraftSchedule(
            Authentication authentication,
            @PathVariable Long scheduleId
    ) {
        scheduleService.deleteDraftSchedule(authentication.getName(), scheduleId);
    }

    @GetMapping("/me/published")
    public List<ScheduleResponse> listMyPublishedSchedules(Authentication authentication) {
        return scheduleService.listPublishedSchedulesForUser(authentication.getName());
    }

    @GetMapping("/me/managed/drafts")
    public List<ScheduleResponse> listMyManagedDraftSchedules(Authentication authentication) {
        return scheduleService.listManagedDraftSchedules(authentication.getName());
    }

    @GetMapping("/me/managed/published")
    public List<ScheduleResponse> listMyManagedPublishedSchedules(Authentication authentication) {
        return scheduleService.listManagedPublishedSchedules(authentication.getName());
    }

    @GetMapping("/me/managed/published/{scheduleId}")
    public PublishedScheduleDetailsResponse getMyManagedPublishedScheduleDetails(
            Authentication authentication,
            @PathVariable Long scheduleId
    ) {
        return scheduleService.getManagedPublishedScheduleDetails(authentication.getName(), scheduleId);
    }

    @GetMapping("/me/published/{scheduleId}")
    public PublishedScheduleDetailsResponse getMyPublishedScheduleDetails(
            Authentication authentication,
            @PathVariable Long scheduleId
    ) {
        return scheduleService.getPublishedScheduleDetailsForUser(authentication.getName(), scheduleId);
    }

    @GetMapping("/{scheduleId}/publication-readiness")
    public SchedulePublicationReadinessResponse getPublicationReadiness(
            Authentication authentication,
            @PathVariable Long scheduleId
    ) {
        return scheduleService.getPublicationReadiness(authentication.getName(), scheduleId);
    }

    @PostMapping("/{scheduleId}/publish")
    public ScheduleResponse publishSchedule(
            Authentication authentication,
            @PathVariable Long scheduleId,
            @RequestBody(required = false) PublishScheduleRequest request
    ) {
        boolean confirmUnfilled = request != null && request.confirmUnfilled();
        return scheduleService.publishSchedule(authentication.getName(), scheduleId, confirmUnfilled);
    }

    @PostMapping("/{scheduleId}/reopen")
    public ScheduleResponse reopenSchedule(
            Authentication authentication,
            @PathVariable Long scheduleId
    ) {
        return scheduleService.reopenSchedule(authentication.getName(), scheduleId);
    }
}
