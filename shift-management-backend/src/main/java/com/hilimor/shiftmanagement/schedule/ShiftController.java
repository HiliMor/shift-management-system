package com.hilimor.shiftmanagement.schedule;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schedules/{scheduleId}/shifts")
public class ShiftController {

    private final ShiftService shiftService;

    public ShiftController(ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShiftResponse createShift(
            Authentication authentication,
            @PathVariable Long scheduleId,
            @Valid @RequestBody CreateShiftRequest request
    ) {
        return shiftService.createShift(authentication.getName(), scheduleId, request);
    }

    @GetMapping
    public List<ShiftResponse> listShifts(Authentication authentication, @PathVariable Long scheduleId) {
        return shiftService.listShifts(authentication.getName(), scheduleId);
    }

    @PutMapping("/{shiftId}")
    public ShiftResponse updateShift(
            Authentication authentication,
            @PathVariable Long scheduleId,
            @PathVariable Long shiftId,
            @Valid @RequestBody UpdateShiftRequest request
    ) {
        return shiftService.updateShift(authentication.getName(), scheduleId, shiftId, request);
    }

    @DeleteMapping("/{shiftId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteShift(
            Authentication authentication,
            @PathVariable Long scheduleId,
            @PathVariable Long shiftId
    ) {
        shiftService.deleteShift(authentication.getName(), scheduleId, shiftId);
    }
}
