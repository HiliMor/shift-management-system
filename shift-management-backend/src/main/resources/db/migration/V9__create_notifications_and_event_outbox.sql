CREATE TABLE event_outbox (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_event_outbox_event_type_not_blank CHECK (btrim(event_type) <> ''),
    CONSTRAINT chk_event_outbox_attempt_count_non_negative CHECK (attempt_count >= 0)
);

CREATE INDEX idx_event_outbox_unsent_created
    ON event_outbox(created_at)
    WHERE sent_at IS NULL;

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL,
    recipient_id BIGINT NOT NULL REFERENCES users(id),
    type VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    related_entity_type VARCHAR(100),
    related_entity_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    read_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_notifications_event_recipient UNIQUE (event_id, recipient_id),
    CONSTRAINT chk_notifications_type_not_blank CHECK (btrim(type) <> ''),
    CONSTRAINT chk_notifications_title_not_blank CHECK (btrim(title) <> ''),
    CONSTRAINT chk_notifications_message_not_blank CHECK (btrim(message) <> '')
);

CREATE INDEX idx_notifications_recipient_created
    ON notifications(recipient_id, created_at DESC);

CREATE INDEX idx_notifications_recipient_unread
    ON notifications(recipient_id)
    WHERE read_at IS NULL;
