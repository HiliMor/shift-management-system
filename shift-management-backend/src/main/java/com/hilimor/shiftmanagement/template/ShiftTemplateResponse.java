package com.hilimor.shiftmanagement.template;

public record ShiftTemplateResponse(
        Long id,
        Long teamId,
        String teamName,
        String name,
        String description,
        int cycleDays,
        int defaultMinRestHours,
        boolean active
) {

    public static ShiftTemplateResponse from(ShiftTemplate shiftTemplate) {
        return new ShiftTemplateResponse(
                shiftTemplate.getId(),
                shiftTemplate.getTeam().getId(),
                shiftTemplate.getTeam().getName(),
                shiftTemplate.getName(),
                shiftTemplate.getDescription(),
                shiftTemplate.getCycleDays(),
                shiftTemplate.getDefaultMinRestHours(),
                shiftTemplate.isActive()
        );
    }
}
