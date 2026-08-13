package com.hilimor.shiftmanagement.team;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findFirstByNameOrderByIdAsc(String name);
}
