# --- !Ups

DROP SCHEMA IF EXISTS test_symbiotic_dman CASCADE;

-- ====================================================================
-- Enable UUID extension
-- ====================================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


-- ====================================================================
-- Create the schema
-- ====================================================================
CREATE SCHEMA IF NOT EXISTS test_symbiotic_dman;

CREATE TYPE GENDER_TYPE AS ENUM ('m', 'f');

-- ====================================================================
-- Defines the document management table
-- ====================================================================
CREATE TABLE test_symbiotic_dman.files (
  id              UUID PRIMARY KEY DEFAULT uuid_generate_v1mc(),
  file_id         UUID    NOT NULL,
  version         INTEGER NOT NULL DEFAULT 1,
  file_name       TEXT    NOT NULL,
  path            TEXT    NOT NULL,
  is_folder       BOOLEAN NOT NULL,
  is_deleted      BOOLEAN NOT NULL,
  accessible_by   JSONB   NOT NULL,
  content_type    TEXT,
  length          NUMERIC,
  owner_id        TEXT,
  owner_type      TEXT,
  created_date    TIMESTAMP WITH TIME ZONE,
  created_by      UUID,
  description     TEXT,
  locked_by       UUID,
  locked_date     TIMESTAMP WITH TIME ZONE,
  custom_metadata JSONB
);

CREATE INDEX files_file_id_index
  ON test_symbiotic_dman.files (file_id);
CREATE INDEX files_file_name_index
  ON test_symbiotic_dman.files (file_name);
CREATE INDEX files_path_index
  ON test_symbiotic_dman.files (path);
CREATE INDEX files_is_folder_index
  ON test_symbiotic_dman.files (is_folder);
CREATE INDEX files_owner_id_index
  ON test_symbiotic_dman.files (owner_id);

-- ====================================================================
-- Support tables for general application functionality
-- ====================================================================
CREATE TABLE test_symbiotic_dman.avatars (
  id           UUID PRIMARY KEY DEFAULT uuid_generate_v1mc(),
  user_id      UUID                     NOT NULL,
  filename     TEXT                     NOT NULL,
  file_type    TEXT,
  length       TEXT,
  created_date TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX avatar_user_id_index
  ON test_symbiotic_dman.avatars (user_id);
CREATE INDEX avatar_filename_index
  ON test_symbiotic_dman.avatars (filename);

CREATE TABLE test_symbiotic_dman.users (
  id                UUID PRIMARY KEY DEFAULT uuid_generate_v1mc(),
  provider_id       TEXT    NOT NULL,
  provider_key      TEXT    NOT NULL,
  version           INTEGER,
  created_by        UUID,
  created_date      TIMESTAMP WITH TIME ZONE,
  modified_by       UUID,
  modified_date     TIMESTAMP WITH TIME ZONE,
  username          TEXT    NOT NULL UNIQUE,
  email             TEXT    NOT NULL,
  first_name        TEXT,
  middle_name       TEXT,
  last_name         TEXT,
  date_of_birth     TIMESTAMP WITH TIME ZONE,
  gender            GENDER_TYPE,
  active            BOOLEAN NOT NULL DEFAULT TRUE,
  avatar_url        TEXT,
  use_social_avatar BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX users_username_index
  ON test_symbiotic_dman.users (username);
CREATE INDEX users_provider_index
  ON test_symbiotic_dman.users (provider_id, provider_key);


CREATE TABLE test_symbiotic_dman.password_info (
  id           SERIAL PRIMARY KEY NOT NULL,
  provider_id  TEXT               NOT NULL,
  provider_key TEXT               NOT NULL,
  hasher       TEXT               NOT NULL,
  password     TEXT               NOT NULL,
  salt         TEXT
);

CREATE INDEX pinfo_provider_index
  ON test_symbiotic_dman.password_info (provider_id, provider_key);

CREATE TABLE test_symbiotic_dman.oauth2info (
  id            SERIAL PRIMARY KEY NOT NULL,
  provider_id   TEXT               NOT NULL,
  provider_key  TEXT               NOT NULL,
  access_token  TEXT               NOT NULL,
  token_type    TEXT,
  expires_in    INTEGER,
  refresh_token TEXT,
  params        JSONB
);

CREATE INDEX oauth2_access_token
  ON test_symbiotic_dman.oauth2info (access_token);


# --- !Downs

DROP SCHEMA test_symbiotic_dman CASCADE;