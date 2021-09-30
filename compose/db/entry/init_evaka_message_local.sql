-- SPDX-FileCopyrightText: 2017-2020 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

--
-- A migration script that generates a database with application and migration
-- roles.
--
-- Note: Do not give real role passwords as input, but give the Flyway variable
-- names, instead (e.g. $\{migration_user_password\}).

CREATE DATABASE "evaka_message_local";

-- Migration role to manage the migrations.
CREATE ROLE "evaka_message_migration_role_local";
GRANT ALL PRIVILEGES ON DATABASE "evaka_message_local" TO "evaka_message_migration_role_local" WITH GRANT OPTION;

-- (App) user-level role to connect to the database with least required privileges.
CREATE ROLE "evaka_message_application_role_local";

-- Migration login user
CREATE ROLE "evaka_message_migration_local" WITH LOGIN PASSWORD 'message-flyway'
  IN ROLE "evaka_message_migration_role_local";

-- App login user
CREATE ROLE "evaka_message_application_local" WITH LOGIN PASSWORD 'message-app'
  IN ROLE "evaka_message_application_role_local";
