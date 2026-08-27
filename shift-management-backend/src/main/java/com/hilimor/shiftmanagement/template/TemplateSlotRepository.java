package com.hilimor.shiftmanagement.template;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateSlotRepository extends JpaRepository<TemplateSlot, Long> {

    List<TemplateSlot> findByShiftTemplate_IdOrderByDayOffsetAscStartTimeAsc(Long shiftTemplateId);

    Optional<TemplateSlot> findByIdAndShiftTemplate_Id(Long id, Long shiftTemplateId);
}
