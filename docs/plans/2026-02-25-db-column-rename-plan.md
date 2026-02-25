<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Database Column Rename: `created` -> `created_at`, `updated` -> `updated_at`

Goal: Make SchemaConventionsTest tests pass with zero permitted violations for:
- `creation timestamp should be called 'created_at' instead of 'created'`
- `update timestamp should be called 'updated_at' instead of 'updated'`

## One-time setup (first group only)

Update the columns query in `SchemaConventionsTest.beforeAll()` to exclude generated columns by adding `AND is_generated = 'NEVER'` to the WHERE clause. Generated columns are backwards-compatibility aliases and should not count as convention violations.

## Setup (at the start of each group)

1. Run SchemaConventionsTest to confirm current state (tests pass with permitted violations)
2. Create a new versioned migration file (next available V number)
3. Run `./list-migrations.sh` to update `migrations.txt` (detects migration numbering conflicts between developers)

## Per-table workflow

For each table:

1. **Migration SQL** - Add to the migration file:
   - For `created` columns:
     ```sql
     ALTER TABLE <table> RENAME COLUMN created TO created_at;
     ALTER TABLE <table> ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;
     ```
   - For `updated` columns:
     ```sql
     DROP TRIGGER IF EXISTS set_timestamp ON <table>;
     ALTER TABLE <table> RENAME COLUMN updated TO updated_at;
     CREATE TRIGGER set_timestamp BEFORE UPDATE ON <table> FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
     ALTER TABLE <table> ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;
     ```
2. **Kotlin code and repeatable migrations** - Find and update all SQL queries referencing these columns for this table, including repeatable migrations (`R__*.sql` views and functions)
3. **Test update** - Remove the table from `permittedViolations` in SchemaConventionsTest
4. **Reset DB** - `docker-compose down -v && docker-compose up -d` (from compose/)
5. **Verify backend** - Run SchemaConventionsTest, then relevant package integration tests
6. **Format** - Run `ktfmtFormat` and `ktlintFormat`
7. **Codegen** - Run `./gradlew codegen` to regenerate frontend types and API clients
8. **Verify frontend** - Run `yarn type-check` from the frontend directory. Fix any TypeScript errors caused by renamed fields.
9. **Commit**

After all tables in the group are done:

10. **Manual review** - Review changes. Full integration tests and e2e tests run in CI before proceeding to next group.

## Task list

Keep this list up-to-date as groups are completed.

### Group 1: setting/holidayperiod (4 tables) ✓

- [x] `setting` (both)
- [x] `holiday_period` (both)
- [x] `holiday_period_questionnaire` (both)
- [x] `holiday_questionnaire_answer` (both)

### Group 2: assistance/document (5 tables) ✓

- [x] `daycare_assistance` (both)
- [x] `other_assistance_measure` (both)
- [x] `preschool_assistance` (both)
- [x] `child_document` (both)
- [x] `document_template` (both)

### Group 3: notes/pairing/webpush (7 tables) ✓

- [x] `child_daily_note` (both)
- [x] `child_sticky_note` (both)
- [x] `group_note` (both)
- [x] `pairing` (both)
- [x] `mobile_device` (both)
- [x] `mobile_device_push_subscription` (both)
- [x] `vapid_jwt` (both)

### Group 4: attendance (5 tables)

- [ ] `staff_attendance` (both)
- [ ] `staff_attendance_external` (both)
- [ ] `staff_attendance_plan` (both)
- [ ] `staff_attendance_realtime` (both)
- [ ] `staff_occupancy_coefficient` (both)

### Group 5: messaging (8 tables)

- [ ] `message` (both)
- [ ] `message_account` (both)
- [ ] `message_content` (both)
- [ ] `message_recipients` (both)
- [ ] `message_thread` (both)
- [ ] `message_thread_children` (both)
- [ ] `message_thread_folder` (both)
- [ ] `message_thread_participant` (both)

### Group 6: daycare (8 tables)

- [ ] `care_area` (both)
- [ ] `daycare` (both)
- [ ] `daycare_acl` (both)
- [ ] `daycare_caretaker` (both)
- [ ] `daycare_group_acl` (both)
- [ ] `daycare_group_placement` (both)
- [ ] `club_term` (both)
- [ ] `preschool_term` (both)

### Group 7: placement/serviceneed (7 tables)

- [ ] `placement_plan` (both)
- [ ] `service_need` (both)
- [ ] `service_need_option` (both)
- [ ] `service_need_option_fee` (both)
- [ ] `service_need_option_voucher_value` (both)
- [ ] `decision` (both)
- [ ] `daily_service_time` (both) — also update `R__child_absences_functions.sql` (`dst.updated` in 3 places)

### Group 8: pis (8 tables)

- [ ] `person` (both)
- [ ] `employee` (both)
- [ ] `employee_pin` (both)
- [ ] `guardian` (created only)
- [ ] `guardian_blocklist` (both)
- [ ] `fridge_child` (updated only)
- [ ] `fridge_partner` (updated only)
- [ ] `child_images` (both)

### Group 9: invoicing (7 tables)

- [ ] `fee_decision` (both)
- [ ] `fee_decision_child` (both)
- [ ] `fee_thresholds` (both)
- [ ] `payment` (both)
- [ ] `voucher_value_decision` (both)
- [ ] `income_notification` (both)
- [ ] `voucher_value_report_snapshot` (created only)

### Group 10: integration (2 tables)

- [ ] `dvv_modification_token` (created only)
- [ ] `koski_study_right` (both)

## To be done later

### Cleanup: drop generated columns

After all groups have been deployed, create a migration that drops all the backwards-compat generated columns (`ALTER TABLE <table> DROP COLUMN created;` / `ALTER TABLE <table> DROP COLUMN updated;`).

### Cleanup: revert SchemaConventionsTest filter

Remove the `AND is_generated = 'NEVER'` filter added in the one-time setup, since the generated columns will no longer exist.

### Cleanup: remove old trigger function

Once no table uses `trigger_refresh_updated()` anymore, remove the old function from `R__triggers.sql`.

## Notes

- Each group becomes its own PR with its own migration file number.
- **Backwards compatibility**: After renaming a column, a generated column with the old name is added as a read-only alias (e.g., `created` becomes `GENERATED ALWAYS AS (created_at) STORED`). This lets old service instances still read the old column names during rolling deploys. Old instances that explicitly write to these columns will fail, but this is less common than reads. These generated columns are dropped in a single cleanup PR after all groups are deployed.
- `trigger_refresh_updated()` (old) and `trigger_refresh_updated_at()` (new) are defined in `R__triggers.sql`
- Tables marked "created only" don't have an `updated` column to rename
- Tables marked "updated only" don't have a `created` column to rename
- Tables marked "both" need both columns renamed
- Repeatable migrations (`R__*.sql`) that reference renamed columns must also be updated — they re-run automatically when their checksum changes
