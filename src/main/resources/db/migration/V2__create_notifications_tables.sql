-- Flyway Migration V2: Create notification management tables (SRS §5.7, §8.1)

-- Notifications table (in-app notification store)
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    recipient_user_id VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    module VARCHAR(100) NOT NULL,
    entity_id VARCHAR(255),
    message VARCHAR(1000) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_notification_recipient ON notifications(recipient_user_id);
CREATE INDEX IF NOT EXISTS idx_notification_read ON notifications(recipient_user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notification_created ON notifications(created_at DESC);

-- Notification templates table
CREATE TABLE IF NOT EXISTS notification_templates (
    id UUID PRIMARY KEY,
    type VARCHAR(100) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    subject VARCHAR(255),
    body_template VARCHAR(2000) NOT NULL,
    CONSTRAINT uq_template_type_channel UNIQUE (type, channel)
);

-- Notification preferences table
CREATE TABLE IF NOT EXISTS notification_preferences (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    CONSTRAINT uq_pref_user_type UNIQUE (user_id, type)
);

CREATE INDEX IF NOT EXISTS idx_pref_user_id ON notification_preferences(user_id);

-- Join table for notification_preferences.enabledChannels
CREATE TABLE IF NOT EXISTS notification_preference_channels (
    preference_id UUID NOT NULL REFERENCES notification_preferences(id) ON DELETE CASCADE,
    channel VARCHAR(50) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_pref_channel_pref_id ON notification_preference_channels(preference_id);
