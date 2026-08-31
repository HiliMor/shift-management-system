package com.hilimor.shiftmanagement.template;

public record TemplateDeletionPreviewResponse(
        ShiftTemplateResponse template,
        int slotCount,
        String revision
) {
}
