CREATE TABLE IF NOT EXISTS chat_room (
    id UUID PRIMARY KEY,
    chat_id VARCHAR(255) NOT NULL UNIQUE,
    sender_id UUID NOT NULL,
    recipient_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_chat_room_sender FOREIGN KEY (sender_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_room_recipient FOREIGN KEY (recipient_id) REFERENCES app_user (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS chat_message (
    id UUID PRIMARY KEY,
    chat_id VARCHAR(255) NOT NULL,
    sender_id UUID NOT NULL,
    recipient_id UUID NOT NULL,
    content TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    status VARCHAR(50) DEFAULT 'SENT',
    CONSTRAINT fk_chat_message_chat_room FOREIGN KEY (chat_id) REFERENCES chat_room (chat_id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_message_recipient FOREIGN KEY (recipient_id) REFERENCES app_user (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_chat_message_chat_id_time ON chat_message (chat_id, timestamp);
