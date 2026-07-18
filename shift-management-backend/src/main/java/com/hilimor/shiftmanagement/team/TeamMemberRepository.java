package com.hilimor.shiftmanagement.team;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    boolean existsByUser_IdAndTeam_Id(Long userId, Long teamId);

    boolean existsByUser_IdAndTeam_IdAndActiveTrue(Long userId, Long teamId);

    Optional<TeamMember> findByUser_IdAndTeam_IdAndActiveTrue(Long userId, Long teamId);

    List<TeamMember> findByUser_IdAndActiveTrue(Long userId);

    List<TeamMember> findByTeam_IdAndActiveTrue(Long teamId);
}
