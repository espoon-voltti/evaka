-- SPDX-FileCopyrightText: 2017-2020 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

CREATE DATABASE "evaka_local";

-- Migration role to manage the migrations.
CREATE ROLE "evaka_migration_role_local";
GRANT ALL PRIVILEGES ON DATABASE "evaka_local" TO "evaka_migration_role_local" WITH GRANT OPTION;

-- (App) user-level role to connect to the database with least required privileges.
CREATE ROLE "evaka_application_role_local";

-- Migration login user
CREATE ROLE "evaka_migration_local" WITH LOGIN PASSWORD 'flyway'
    IN ROLE "evaka_migration_role_local";

-- App login user
CREATE ROLE "evaka_application_local" WITH LOGIN PASSWORD 'app'
    IN ROLE "evaka_application_role_local";
