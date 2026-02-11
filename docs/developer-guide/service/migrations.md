<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Database Schema & Migrations

Quick reference for eVaka's database schema conventions, some of which are enforced by `SchemaConventionsTest`.

## Basic Conventions

- **Table names**: Use singular, not plural
- **Primary keys**: Always `uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc()`

## Migration Guidelines

**IMPORTANT: Shared migrations across municipalities**

These database migrations are shared across all municipalities using eVaka. Migrations **must not insert data** unless that data is truly universal for all municipalities and environments. Municipality-specific or environment-specific data should be handled outside of Flyway migrations.

### Versioned vs Repeatable Migrations

**Versioned migrations** (`V###__description.sql`):
- Run exactly once in order
- Modify schema structure (CREATE TABLE, ALTER TABLE, CREATE INDEX, etc.)
- Cannot be changed after deployment
- **After adding a new migration, run `./list-migrations.sh`** - This updates `migrations.txt` and causes explicit git conflicts if two developers add migrations simultaneously, ensuring migration numbering conflicts are detected early

**Repeatable migrations** (`R__description.sql`):
- Re-executed whenever their checksum changes
- Must be idempotent (safe to run multiple times)
- Used for database objects that can be dropped and recreated:
  - **Views** - Computed/derived data (ACL views, application views, unit views)
  - **Functions** - Reusable query logic (absence functions, ACL functions)
  - **Triggers** - Automated behaviors (updated_at triggers)
  - **Temporary cleanup** - R__dev_temp.sql for dropping obsolete tables during transitions

**Idempotency patterns:**
```sql
-- Views: DROP then CREATE
DROP VIEW IF EXISTS application_view;
CREATE VIEW application_view AS (...);

-- Functions: DROP then CREATE, or CREATE OR REPLACE
DROP FUNCTION IF EXISTS child_absences_in_range(uuid, daterange);
CREATE FUNCTION child_absences_in_range(...) RETURNS TABLE (...) AS $$ ... $$ LANGUAGE SQL STABLE;

-- OR
CREATE OR REPLACE FUNCTION trigger_refresh_updated_at() RETURNS trigger AS $$ ... $$ LANGUAGE plpgsql;
```

**Best practices:**
- Use `DROP IF EXISTS` for views and functions with different signatures
- Use `CREATE OR REPLACE` for functions/triggers with stable signatures
- Mark functions as `STABLE` when they don't modify data
- Drop dependent objects first (use CASCADE if needed)
- Add comments for complex functions: `COMMENT ON FUNCTION function_name IS 'description';`

## Timestamp Conventions

**Column naming (use `_at` suffix, not old conventions):**
- `created_at`: Set at row creation, never changes. Both technical and user-visible.
- `updated_at`: Auto-updated on ANY row change via trigger. Purely technical, should NOT be user-visible.
- `modified_at` + `modified_by`: User-visible modification tracking. Set at creation (creation = first modification), updated on user changes.

**Requirements:**
- Type: `timestamp with time zone NOT NULL` (never `timestamp without time zone`)
- Default `now()` allowed, but explicit timestamps preferred for testability
- Tables with `updated_at` must have trigger: `CREATE TRIGGER set_timestamp BEFORE UPDATE ON <table> FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();`

## User Tracking Conventions

**Column naming:**
- `created_by`: User who created the row
- `modified_by`: User who last modified the row (set at creation and on user modifications)
- `updated_by`: Do NOT use - use `modified_at` + `modified_by` pair instead

**Requirements:**
- Type: `uuid NOT NULL`
- Foreign key: Must reference `evaka_user(id)` (legacy tables may reference `employee(id)` or `person(id)`, but new code should use `evaka_user`)
- Set at creation time and updated on user modifications (not automatic system updates)
- Use the special hard-coded system internal user when the row is created/updated by the system or the user is unknown for legacy reasons

**Example SQL:**
```sql
created_by uuid NOT NULL REFERENCES evaka_user(id),
modified_by uuid NOT NULL REFERENCES evaka_user(id)
```

**Example usage:**
```kotlin
// From controller (authenticated user)
createdBy = user.evakaUserId,
modifiedBy = user.evakaUserId

// System-initiated or legacy unknown user
createdBy = AuthenticatedUser.SystemInternalUser.evakaUserId
```

## Column Type Conventions

**Text columns:**
- Always use `text`, never `varchar` or `char`
- PostgreSQL `text` has no performance penalty and avoids arbitrary length limits

**Numeric columns:**
- `integer` - Most common, use for counts, IDs, and money (stored in cents)
- `smallint` - Use for small ranges (e.g., day of week, month number)
- `numeric(precision, scale)` - Use for decimal values that need exact precision (e.g., coefficients, percentages)
- **Money must be stored as integer in cents** (e.g., 10.50€ = 1050 cents)

**Arrays:**
```sql
-- Array columns use [] syntax
operation_days integer[] DEFAULT '{1,2,3,4,5}'::integer[],
recipient_ids uuid[] NOT NULL DEFAULT '{}',
categories absence_category[]
```

**ENUM types (predefined value sets):**
```sql
-- Create new ENUM
CREATE TYPE invoice_status AS ENUM ('DRAFT', 'SENT', 'PAID');

-- Extend existing ENUM
ALTER TYPE invoice_status ADD VALUE 'CANCELLED';
ALTER TYPE invoice_status ADD VALUE 'PENDING' BEFORE 'SENT';
```

**JSON columns:**
- Only use when structure is truly dynamic and cannot be relational
- Always use `jsonb`, never `json`

## Index Conventions

**Naming conventions:**
- `fk$<table>_<columns>` - Foreign key indexes (required for all FK columns)
- `idx$<table>_<columns>` - General performance indexes
- `uniq$<table>_<description>` - Unique constraint indexes

**Foreign key indexing (required):**
- Every column with a foreign key must have at least one index where that column is the first key
- Required for query performance (JOINs and WHERE clauses)

**Partial indexes:**
- Use `WHERE` clause to index only relevant rows (commonly for nullable columns)
- Reduces index size and improves performance

**Special index types:**
- **GIN** (`USING gin`) - For JSONB columns with `jsonb_path_ops`
- **GIST** (`USING gist`) - For range types (tstzrange, daterange, etc.) and range queries
- **CONCURRENTLY** - Create indexes without blocking writes (use for production migrations on existing tables)

**Examples:**
```sql
-- Basic foreign key index
CREATE INDEX fk$child_document_published_version_created_by
    ON child_document_published_version(created_by);

-- Composite index (FK column first)
CREATE INDEX fk$child_document_published_version_document_id_version_number
    ON child_document_published_version(child_document_id, version_number);

-- Unique index
CREATE UNIQUE INDEX uniq$invoice_replaced_invoice_id
    ON invoice (replaced_invoice_id) WHERE replaced_invoice_id IS NOT NULL;

-- Partial index (nullable column)
CREATE INDEX idx$message_recipients_unread
    ON message_recipients (recipient_id, message_id) WHERE read_at IS NULL;

-- JSONB index (GIN)
CREATE INDEX CONCURRENTLY idx$application_doc
    ON application USING gin (document jsonb_path_ops);

-- Range index (GIST)
CREATE INDEX CONCURRENTLY idx$child_attendance_unit_range
    ON child_attendance USING gist(unit_id, tstzrange(arrived, departed));
```

## Date Range Conventions

**Preferred approach (new code):**
- Use separate `start_date date NOT NULL` and `end_date date` (nullable if open-ended)
- Add constraint when end_date is NOT NULL: `CHECK (start_date <= end_date)`
- Construct ranges in queries: `daterange(start_date, end_date, '[]')`

**Indexing:**
- Add start_date/end_date to FK indexes for query performance: `CREATE INDEX idx$<table>_<fk>_dates ON <table>(fk_column, start_date, end_date);`

**Legacy daterange/datemultirange columns:**
- Must have CHECK constraints to prevent infinite bounds
- Lower bound never infinite: `CHECK (NOT lower_inf(column))`
- Finite ranges: `CHECK (NOT (lower_inf(column) OR upper_inf(column)))`

**Examples:**
```sql
-- Separate date columns with constraint
CREATE TABLE absence_application (
    child_id uuid NOT NULL REFERENCES child(id),
    start_date date NOT NULL,
    end_date date NOT NULL,
    CONSTRAINT check$start_date_before_end_date CHECK (start_date <= end_date)
);

-- Composite index with FK and dates (common pattern)
CREATE INDEX placement_child_id_start_date_end_date_idx
    ON placement (child_id, start_date, end_date);

-- Query with range construction and overlap check
SELECT * FROM placement
WHERE child_id = :childId
  AND daterange(start_date, end_date, '[]') && daterange('2024-01-01', '2024-12-31', '[]');
```

## Constraint Conventions

**Naming convention:**
- `check$<description>` (e.g., `check$start_date_before_end_date`, `check$decided_valid`)

**Common patterns:**

**1. Prevent daterange overlaps (EXCLUDE constraint):**
```sql
-- No overlapping periods per child for accepted decisions
ALTER TABLE assistance_need_preschool_decision
    ADD CONSTRAINT check$assistance_need_preschool_decision_no_overlap
    EXCLUDE USING gist (
        child_id WITH =,
        daterange(valid_from, valid_to, '[]') WITH &&
    ) WHERE (status = 'ACCEPTED');
```

**2. Conditional nullability:**

If and only if (field required when condition matches, forbidden otherwise):
```sql
-- Field required for specific status values (using boolean equality)
valid_from date CHECK ((status IN ('ACCEPTED', 'ANNULLED')) = (valid_from IS NOT NULL))
```

One-way implication (if A then B required, else B optional):
```sql
-- If archived, then status must be COMPLETED
CHECK (archived_at IS NULL OR status = 'COMPLETED')

-- If valid_to has value, valid_from must also have value (and be earlier)
valid_to date CHECK (valid_to IS NULL OR (valid_from IS NOT NULL AND valid_to >= valid_from))
```

**3. Field consistency (paired fields both NULL or both NOT NULL):**
```sql
-- Timestamp and user paired
CONSTRAINT answered_consistency
    CHECK ((answered_at IS NULL) = (answered_by IS NULL))
```

**4. Type-based constraints using CASE:**
```sql
-- Different fields valid for different types
CONSTRAINT check$input_data_type CHECK (CASE type
    WHEN 'PRESCHOOL' THEN preparatory_input_data IS NULL
    WHEN 'PREPARATORY' THEN preschool_input_data IS NULL
END)
```

**5. Exactly one field required (using num_nonnulls):**
```sql
-- Exactly one of these FKs must be NOT NULL
CONSTRAINT single_type CHECK (
    num_nonnulls(decision_id, document_id, fee_decision_id, voucher_value_decision_id) = 1
)
```

