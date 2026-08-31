package com.hilimor.shiftmanagement.template;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShiftTemplateController {

    private final ShiftTemplateService shiftTemplateService;

    public ShiftTemplateController(ShiftTemplateService shiftTemplateService) {
        this.shiftTemplateService = shiftTemplateService;
    }

    @PostMapping("/api/teams/{teamId}/templates")
    @ResponseStatus(HttpStatus.CREATED)
    public ShiftTemplateResponse createTemplate(
            Authentication authentication,
            @PathVariable Long teamId,
            @Valid @RequestBody CreateShiftTemplateRequest request
    ) {
        return shiftTemplateService.createTemplate(authentication.getName(), teamId, request);
    }

    @GetMapping("/api/teams/{teamId}/templates")
    public List<ShiftTemplateResponse> listTeamTemplates(
            Authentication authentication,
            @PathVariable Long teamId
    ) {
        return shiftTemplateService.listTeamTemplates(authentication.getName(), teamId);
    }

    @DeleteMapping("/api/templates/{templateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemplate(
            Authentication authentication,
            @PathVariable Long templateId,
            @RequestParam(required = false) String revision
    ) {
        shiftTemplateService.deleteTemplate(authentication.getName(), templateId, revision);
    }

    @GetMapping("/api/templates/{templateId}/deletion-preview")
    public TemplateDeletionPreviewResponse previewTemplateDeletion(Authentication authentication, @PathVariable Long templateId) {
        return shiftTemplateService.previewTemplateDeletion(authentication.getName(), templateId);
    }

    @PostMapping("/api/templates/{templateId}/slots")
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateSlotResponse createSlot(
            Authentication authentication,
            @PathVariable Long templateId,
            @Valid @RequestBody CreateTemplateSlotRequest request
    ) {
        return shiftTemplateService.createSlot(authentication.getName(), templateId, request);
    }

    @GetMapping("/api/templates/{templateId}/slots")
    public List<TemplateSlotResponse> listTemplateSlots(
            Authentication authentication,
            @PathVariable Long templateId
    ) {
        return shiftTemplateService.listTemplateSlots(authentication.getName(), templateId);
    }

    @PostMapping("/api/templates/{templateId}/generate")
    public GenerateTemplateShiftsResponse generateShifts(
            Authentication authentication,
            @PathVariable Long templateId,
            @Valid @RequestBody GenerateTemplateShiftsRequest request
    ) {
        return shiftTemplateService.generateShifts(authentication.getName(), templateId, request);
    }
}
