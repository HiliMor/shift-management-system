package com.hilimor.shiftmanagement.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.hilimor.shiftmanagement.schedule.DeletionRevision.RecordVersion;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class DeletionRevisionTest {
    private final RecordVersion parent = new RecordVersion(1L, 0L);
    private final RecordVersion first = new RecordVersion(2L, 0L);
    private final RecordVersion second = new RecordVersion(3L, 0L);

    @Test
    void queryOrderingDoesNotChangeRevision() {
        assertThat(DeletionRevision.of("schedule", parent, List.of(List.of(first, second))))
                .isEqualTo(DeletionRevision.of("schedule", parent, List.of(List.of(second, first))));
    }

    @Test
    void parentAndEveryChildIdentityVersionAndGroupArePartOfRevision() {
        String original = DeletionRevision.of("schedule", parent, List.of(List.of(first), List.of(second)));
        assertThat(DeletionRevision.of("template", parent, List.of(List.of(first), List.of(second)))).isNotEqualTo(original);
        assertThat(DeletionRevision.of("schedule", new RecordVersion(4L, 0L), List.of(List.of(first), List.of(second)))).isNotEqualTo(original);
        assertThat(DeletionRevision.of("schedule", new RecordVersion(1L, 1L), List.of(List.of(first), List.of(second)))).isNotEqualTo(original);
        assertThat(DeletionRevision.of("schedule", parent, List.of(List.of(new RecordVersion(2L, 1L)), List.of(second)))).isNotEqualTo(original);
        assertThat(DeletionRevision.of("schedule", parent, List.of(List.of(new RecordVersion(4L, 0L)), List.of(second)))).isNotEqualTo(original);
        assertThat(DeletionRevision.of("schedule", parent, List.of(List.of(second), List.of(first)))).isNotEqualTo(original);
    }

    @Test
    void missingOrMalformedRevisionIsBadRequestAndDifferentRevisionIsConflict() {
        String current = DeletionRevision.of("schedule", parent, List.of());
        DeletionRevision.requireMatch(current, current);
        for (String invalid : new String[]{null, "", "*", "0", "G".repeat(64)}) {
            assertThatThrownBy(() -> DeletionRevision.requireMatch(invalid, current))
                    .isInstanceOfSatisfying(ResponseStatusException.class, error ->
                            assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
        assertThatThrownBy(() -> DeletionRevision.requireMatch("0".repeat(64), current))
                .isInstanceOfSatisfying(ResponseStatusException.class, error ->
                        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }
}
