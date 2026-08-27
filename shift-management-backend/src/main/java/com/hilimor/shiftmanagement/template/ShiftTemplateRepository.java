package com.hilimor.shiftmanagement.template;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftTemplateRepository extends JpaRepository<ShiftTemplate, Long> {

    List<ShiftTemplate> findByTeam_IdOrderByName(Long teamId);

    Optional<ShiftTemplate> findByTeam_IdAndName(Long teamId, String name);

    Optional<ShiftTemplate> findByTeam_IdAndActiveTrue(Long teamId);

    boolean existsByTeam_IdAndName(Long teamId, String name);
}
