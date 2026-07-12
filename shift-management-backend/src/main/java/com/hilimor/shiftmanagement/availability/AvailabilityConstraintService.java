package com.hilimor.shiftmanagement.availability;

import java.time.Instant;
import java.util.List;

import com.hilimor.shiftmanagement.user.User;
import com.hilimor.shiftmanagement.user.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AvailabilityConstraintService {

    private final AvailabilityConstraintRepository availabilityConstraintRepository;
    private final UserRepository userRepository;

    public AvailabilityConstraintService(
            AvailabilityConstraintRepository availabilityConstraintRepository,
            UserRepository userRepository
    ) {
        this.availabilityConstraintRepository = availabilityConstraintRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AvailabilityConstraintResponse createConstraint(
            String username,
            CreateAvailabilityConstraintRequest request
    ) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Availability constraint end time must be after start time");
        }

        User employee = currentUser(username);
        AvailabilityConstraint constraint = new AvailabilityConstraint(
                employee,
                request.startTime(),
                request.endTime(),
                request.reason(),
                Instant.now()
        );

        AvailabilityConstraint savedConstraint = availabilityConstraintRepository.save(constraint);

        return AvailabilityConstraintResponse.from(savedConstraint);
    }

    @Transactional(readOnly = true)
    public List<AvailabilityConstraintResponse> listMyConstraints(String username) {
        User employee = currentUser(username);

        return availabilityConstraintRepository.findByEmployee_IdOrderByStartTime(employee.getId())
                .stream()
                .map(AvailabilityConstraintResponse::from)
                .toList();
    }

    private User currentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Authenticated user not found"));
    }
}
