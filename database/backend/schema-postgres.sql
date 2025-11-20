
CREATE TABLE IF NOT EXISTS app_users (
    id                  BIGSERIAL PRIMARY KEY,
    user_type           VARCHAR(32) NOT NULL,            -- 'STUDENT' | 'TUTOR' | 'ADMIN' (if any)

    email               VARCHAR(255) NOT NULL UNIQUE,
    password            VARCHAR(255) NOT NULL,
    first_name          VARCHAR(120) NOT NULL,
    last_name           VARCHAR(120) NOT NULL,
    birth_date          DATE NOT NULL,
    created_at          TIMESTAMP NULL,

    email_verified      BOOLEAN NOT NULL DEFAULT FALSE,
    verification_token  VARCHAR(255) NULL,

    reset_password_token         VARCHAR(255) NULL,
    reset_password_token_expiry  VARCHAR(64)  NULL,      -- ISO string used by app

    banned              BOOLEAN NOT NULL DEFAULT FALSE,
    email_language      VARCHAR(8) NOT NULL DEFAULT 'pl',

    -- Notification prefs
    pref_email_notifications  BOOLEAN DEFAULT TRUE,
    pref_booking_reminders    BOOLEAN DEFAULT TRUE,
    pref_lesson_reminders     BOOLEAN DEFAULT TRUE,
    pref_change_notifications BOOLEAN DEFAULT TRUE,

    -- Student-only optional fields
    student_phone       VARCHAR(40),
    student_photo_url   TEXT,
    student_about_me    TEXT,
    student_goals       TEXT,
    student_strengths   TEXT,
    student_difficulties TEXT,
    student_preferred_subjects TEXT,
    student_avoid_subjects TEXT,
    student_learning_style VARCHAR(64),
    student_meeting_mode VARCHAR(32),
    student_city        VARCHAR(120),
    student_preferred_tools TEXT,
    student_other_tool  VARCHAR(120),
    student_preferred_days  TEXT,
    student_guardian_name   VARCHAR(160),
    student_guardian_email  VARCHAR(255),
    student_share_profile   BOOLEAN,
    student_school          VARCHAR(160),
    student_grade           VARCHAR(80),
    student_track           VARCHAR(120),
    student_languages       VARCHAR(255),
    student_timezone        VARCHAR(80),
    student_availability_note TEXT,

    -- Tutor-only fields
    tutor_education        VARCHAR(255),
    tutor_experience_years INTEGER,
    tutor_photo_url        TEXT,
    tutor_subjects         TEXT,
    tutor_exam_results     TEXT,
    tutor_hourly_rate      NUMERIC(12,2),
    tutor_lesson_duration  INTEGER,
    tutor_teaching_languages VARCHAR(255),
    tutor_lesson_modes     TEXT,            -- JSON string used by app
    tutor_city             VARCHAR(160),
    tutor_travel_radius    INTEGER,
    tutor_teaching_methods TEXT,
    tutor_bio              TEXT,
    tutor_certificates     TEXT,
    tutor_website          VARCHAR(255),
    tutor_linkedin         VARCHAR(255),
    tutor_max_lessons_per_day INTEGER,
    tutor_buffer_time      INTEGER,
    tutor_preferred_days   TEXT,
    tutor_linked_in        VARCHAR(255),    -- historic field
    auto_accept_bookings   BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_app_users_email ON app_users (email);
CREATE INDEX IF NOT EXISTS idx_app_users_user_type ON app_users (user_type);
CREATE INDEX IF NOT EXISTS idx_app_users_banned ON app_users (banned);

-- Enforce age requirements via CHECK + trigger (covers updates too)
ALTER TABLE app_users
    ADD CONSTRAINT IF NOT EXISTS chk_user_age_by_role
    CHECK (
        (user_type = 'STUDENT' AND birth_date <= (CURRENT_DATE - INTERVAL '13 years'))
        OR (user_type = 'TUTOR' AND birth_date <= (CURRENT_DATE - INTERVAL '18 years'))
        OR (user_type NOT IN ('STUDENT','TUTOR'))
    );

-- Normalize email to lower-case on insert/update
CREATE OR REPLACE FUNCTION fn_normalize_email()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  IF NEW.email IS NOT NULL THEN
    NEW.email := LOWER(NEW.email);
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_app_users_normalize_email ON app_users;
CREATE TRIGGER trg_app_users_normalize_email
BEFORE INSERT OR UPDATE OF email ON app_users
FOR EACH ROW
EXECUTE FUNCTION fn_normalize_email();

-- Calendars (external iCal/Google etc.) linked to users
CREATE TABLE IF NOT EXISTS calendars (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    calendar_url TEXT NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS idx_calendars_user ON calendars (user_id);


CREATE TABLE IF NOT EXISTS lessons (
    id              BIGSERIAL PRIMARY KEY,
    student_id      BIGINT NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    tutor_id        BIGINT NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,

    start_time      TIMESTAMP NOT NULL,
    end_time        TIMESTAMP NOT NULL,
    duration_minutes INTEGER,

    status          VARCHAR(32) NOT NULL DEFAULT 'REQUESTED',
    delivery_mode   VARCHAR(32) NOT NULL DEFAULT 'ONLINE',

    meeting_link    TEXT,
    google_event_id VARCHAR(255),
    notes           TEXT,
    
    -- Onsite location (for ONSITE delivery mode)
    onsite_city     VARCHAR(120),
    onsite_postal_code VARCHAR(20),
    onsite_street   VARCHAR(255),
    onsite_building VARCHAR(50),
    onsite_apartment VARCHAR(50),

    -- Reschedule proposal flow
    proposed_start_time TIMESTAMP,
    proposed_end_time   TIMESTAMP,
    proposed_by         VARCHAR(16),                -- 'STUDENT' | 'TUTOR'
    proposal_notes      TEXT,
    proposal_created_at TIMESTAMP,
    proposal_accepted_at TIMESTAMP,
    proposal_rejected_at TIMESTAMP,

    -- Reminders
    reminder_sent_at TIMESTAMP,

    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_lessons_tutor_time ON lessons (tutor_id, start_time);
CREATE INDEX IF NOT EXISTS idx_lessons_student_time ON lessons (student_id, start_time);
CREATE INDEX IF NOT EXISTS idx_lessons_status ON lessons (status);

-- Keep updated_at fresh on updates
CREATE OR REPLACE FUNCTION fn_touch_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  NEW.updated_at := NOW();
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_lessons_touch ON lessons;
CREATE TRIGGER trg_lessons_touch
BEFORE UPDATE ON lessons
FOR EACH ROW
EXECUTE FUNCTION fn_touch_updated_at();

-- Reviews (student and tutor feedback kept in one row per lesson)
CREATE TABLE IF NOT EXISTS reviews (
    id                      BIGSERIAL PRIMARY KEY,
    lesson_id               BIGINT NOT NULL UNIQUE REFERENCES lessons(id) ON DELETE CASCADE,
    student_id              BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    tutor_id                BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,

    -- Student's review about tutor and platform
    tutor_rating            INTEGER,                 -- 1..5
    platform_rating         INTEGER,                 -- 1..5
    comment                 VARCHAR(400),
    student_review_at       TIMESTAMP,

    -- Tutor's review about student and platform
    student_behavior_rating INTEGER,                 -- 1..5
    tutor_platform_rating   INTEGER,                 -- 1..5
    tutor_comment           VARCHAR(400),
    tutor_review_at         TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_reviews_tutor ON reviews (tutor_id);
CREATE INDEX IF NOT EXISTS idx_reviews_student ON reviews (student_id);

-- Helper views (optional)
CREATE OR REPLACE VIEW v_lessons_basic AS
SELECT l.id,
       l.start_time,
       l.end_time,
       l.status,
       l.delivery_mode,
       l.tutor_id,
       l.student_id
FROM lessons l;

-- Contact messages
CREATE TABLE IF NOT EXISTS contact_messages (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    subject         VARCHAR(255),
    message         TEXT NOT NULL,
    replied         BOOLEAN DEFAULT FALSE,
    admin_reply     TEXT,
    replied_at      TIMESTAMP,
    created_at      TIMESTAMP DEFAULT NOW()
);


DO $$
BEGIN
   IF NOT EXISTS (
       SELECT 1 FROM pg_roles WHERE rolname = 'eduscheduler_app'
   ) THEN
       CREATE ROLE eduscheduler_app LOGIN PASSWORD 'changeMe';
   END IF;
END$$;

-- Allow connecting and basic usage
GRANT CONNECT ON DATABASE current_database() TO eduscheduler_app;
GRANT USAGE ON SCHEMA public TO eduscheduler_app;

-- Table and sequence privileges
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO eduscheduler_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO eduscheduler_app;

-- Ensure future objects inherit privileges
ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO eduscheduler_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT USAGE, SELECT ON SEQUENCES TO eduscheduler_app;


