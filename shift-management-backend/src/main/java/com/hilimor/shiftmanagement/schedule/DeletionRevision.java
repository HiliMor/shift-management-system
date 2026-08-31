package com.hilimor.shiftmanagement.schedule;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class DeletionRevision {

    private DeletionRevision() {
    }

    public record RecordVersion(Long id, Long version) {
    }

    public static String of(String type, RecordVersion parent, List<List<RecordVersion>> children) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(type.getBytes(StandardCharsets.UTF_8));
            add(digest, parent.id());
            add(digest, parent.version());
            // IDs detect replacement even when the number of children stays the same.
            for (List<RecordVersion> group : children) {
                add(digest, group.size());
                group.stream().sorted(Comparator.comparing(RecordVersion::id)).forEach(record -> {
                    add(digest, record.id());
                    add(digest, record.version());
                });
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public static void requireMatch(String submitted, String current) {
        if (submitted == null || !submitted.matches("[0-9a-f]{64}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A deletion revision from a preview is required");
        }
        if (!submitted.equals(current)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Deletion preview is out of date. Review the current data before deleting.");
        }
    }

    private static void add(MessageDigest digest, long value) {
        digest.update(ByteBuffer.allocate(Long.BYTES).putLong(value).array());
    }
}
