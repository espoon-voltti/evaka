<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Espoo: archiving to Särmä

This document describes how eVaka transfers documents to Espoo's Särmä archive (Avaintec's X-Archive API). The functional description is available in the wiki (in Finnish): [Särmä archive integration](https://github.com/espoon-voltti/evaka/wiki/S%C3%A4rm%C3%A4-arkisto-integraatio).

The description is based on the code (October 2026) and a local run in which the integration was pointed to a receiver that captured requests as sent. The captured requests are in `examples/`. The people, personal identity codes and process definition numbers (`12.06.01.SL1.RT34`, etc.) are made up. The employee "Pääkäyttäjä Päivi" is a fictional user from eVaka's local development data. The examples are not updated automatically when the code changes.

Code references are paths relative to the repository root.
Abbreviations:

- `espoo/` = `service/src/main/kotlin/evaka/instance/espoo/`
- `core/` = `service/src/main/kotlin/evaka/core/`

## 1. What the Espoo integration exports

**Only child documents (`child_document`).** Espoo's `ArchivalIntegrationClient` implementation
is `SärmäChildDocumentClient` (`espoo/archival/SärmäChildDocumentClient.kt`):

- `uploadChildDocumentToArchive` is implemented (line 59).
- `uploadDecisionToArchive`, `uploadFeeDecisionToArchive` and
  `uploadVoucherValueDecisionToArchive` contain `TODO("Not yet implemented")` (lines 38, 47, 56).
  If archiving a daycare, fee or voucher value decision is triggered in Espoo
  (`POST /employee/decisions/{id}/archive`, etc.), the async job fails with `NotImplementedError`
  and no request is sent to Särmä. Espoo's UI only enables
  `archiveIntegration: { childDocuments: true }` (`frontend/src/lib-customizations/espoo/featureFlags.tsx:44`),
  so the archive buttons for decisions are not shown.

A child document is sent to the archive only if its document template has
`archive_externally = true`. The template must also have a `process_definition_number`.
Otherwise, `SärmäChildDocumentClient` throws an `IllegalStateException`
before making the HTTP call (lines 68–72).

### Document types and their metadata

Metadata is constructed in `espoo/archival/DocumentMetadataUtils.kt`:
`createDocumentDescription` (from line 133) and `createRetentionPolicy` (line 69).
The `documentType` and `documentTypeSpecifier` columns contain the literal Finnish metadata values.

| `ChildDocumentType` | `documentType` | `documentTypeSpecifier` | Retention period (`retentionPolicy`) | Example |
|---|---|---|---|---|
| `VASU`, `MIGRATED_VASU` | Suunnitelma | Varhaiskasvatussuunnitelma | permanent (`InPerpetuity`, KA/13089/07.01.01.03.01/2018) | 01, 02, 12 |
| `LEOPS`, `MIGRATED_LEOPS` | Suunnitelma | Lapsen esiopetuksen oppimissuunnitelma LEOPS | permanent | 03, 13 |
| `HOJKS` | Suunnitelma | Henkilökohtaisen opetuksen järjestämistä koskeva suunnitelma HOJKS | permanent | 04 |
| `PEDAGOGICAL_ASSESSMENT` | Arvio | Pedagoginen arvio | 28 years from birth (`YearsFromCustomerBirthDate`, Basic Education Act, section 16a) | 05, 14 |
| `PEDAGOGICAL_REPORT` | Selvitys | Pedagoginen selvitys | 28 years from birth | 06 |
| `OTHER` | (omitted) | (omitted) | permanent | 07 |
| `CITIZEN_BASIC` | (omitted) | (omitted) | permanent | 08 |
| `OTHER_DECISION` | (omitted) | (omitted) | permanent | 09, 10, 11 |
| `MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION`, `MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION` | (omitted) | (omitted) | permanent | – |

If a type has no value, the `documentType` and `documentTypeSpecifier` elements
are omitted (`mainTextElement` skips null values, `espoo/archival/RecordMetadataXml.kt`).

The metadata does not include the document status (completed/draft) or decision outcome
(accepted/rejected/annulled). The metadata in examples 09–11 differs only in
identifiers. The outcome is visible only in the PDF content.

## 2. API

### Endpoint and method

- `POST {evaka.integration.sarma.url}PUT`. The URL is constructed with
  `archiveEnv.url.resolve("PUT")` (`espoo/archival/SärmäHttpClient.kt:63`).
  The configured URL must therefore end with a slash, for example `https://<host>/archive-core/`,
  resulting in a request to `https://<host>/archive-core/PUT`
  (comment in `core/EvakaEnv.kt:758`).
- Each document is sent in a separate request (`nr_of_instances=1`).

### Content type and multipart parts

`Content-Type: multipart/form-data; boundary=<uuid>` (OkHttp `MultipartBody.FORM`,
`SärmäHttpClient.kt:36–61`). Parts are sent in this order:

| Part | Value | Source |
|---|---|---|
| `protocol_version` | `1.0` | constant |
| `operation_id` | `PUT` | constant |
| `response_format` | `URL-ENCODED` | constant |
| `user_id` | integration user | `evaka.integration.sarma.user_id` |
| `user_role` | e.g. `xa-api-integration` | `evaka.integration.sarma.user_role` |
| `nr_of_instances` | `1` | constant |
| `instance_1_md_model_id` | `standard` | constant |
| `instance_1_md_model_version` | `1.0` | constant |
| `instance_1_md_master_id` | e.g. `yleinen` | `evaka.integration.sarma.master_id` |
| `instance_1_md_master_version` | `1.0` | constant |
| `instance_1_class_id` | the template's process definition number | `document_template.process_definition_number` |
| `instance_1_virtual_archive_id` | e.g. `YLEINEN` | `evaka.integration.sarma.virtual_archive_id` |
| `instance_1_record_payload_location` | `SELF_CONTAINED` | constant |
| `instance_1_md_instance` | metadata XML, `Content-Type: application/xml; charset=utf-8`, no filename | `marshalMetadata(...)` |
| `instance_1_record_payload_content_size` | PDF size in bytes | PDF |
| `instance_1_record_payload_data` | PDF, `Content-Type: application/pdf`, `filename="child-documents/child_document_<id>_v<n>.pdf"` | S3, published version |

Notes:

- The multipart part's `filename` is the S3 key, including the directory. The metadata's `format/fileName`
  contains only the filename (`Paths.get(...).fileName`, `SärmäChildDocumentClient.kt:79`).
- The metadata XML is sent on a single line without indentation. The examples'
  `metadata.xml` files are formatted for readability (`xmllint --format`), while `request.http`
  contains the XML exactly as sent.
- The XML declaration uses single quotes (`<?xml version='1.0' encoding='UTF-8'?>`).
  These come from `XMLStreamWriter`.
- The PDF sent is the document's **latest published version**
  (`getChildDocumentPublishedVersion`, `core/document/archival/ArchivalService.kt:196`).
  For the document completed by a guardian (example 08), this is `_v2.pdf`.

### Headers and authentication

The request contains only OkHttp's default headers: `Content-Type`, `Content-Length`, `Host`,
`Connection: Keep-Alive`, `Accept-Encoding: gzip` and `User-Agent: okhttp/<version>`.
**There is no HTTP-level authentication** (no `Authorization` header and no
client certificate in the code). The user and role are passed in the form fields
`user_id` and `user_role`. Access control therefore relies on network-level restrictions
(the infrastructure configuration is not in this repository).

### Timeouts

OkHttp's `connectTimeout`, `readTimeout` and `writeTimeout` are each 1 minute
(`SärmäHttpClient.kt:19–21`). No overall timeout (`callTimeout`) is configured.

### Response handling

`SärmäChildDocumentClient.kt:97–131`:

1. The HTTP status code and body are logged at info level.
2. The body is parsed as `application/x-www-form-urlencoded`
   (`URLEncodedUtils.parse`). The fields `status_code` (integer) and `instance_ids` are read.
3. If `instance_ids` is missing, an error is logged, but this alone does not prevent success.
4. **Success** requires both HTTP status `200` **and** `status_code=200` in the body.
   - On success, `ArchivalService` sets `child_document.archived_at = now()`
     (`ArchivalService.kt:221`). The `instance_ids` value returned by Särmä is not stored
     in the database; it appears only in the log.
   - Any other combination causes `RuntimeException("Failed to archive document <id>")`.
     `HTTP 200` + `status_code=400` (example 14), an HTML body or an empty body
     is also treated as an error. The document is not marked as archived.
   - A network error or timeout (`IOException`) has the same outcome.
5. All archival jobs are scheduled with `retryCount = 1`, meaning
   **one attempt without retries**. This applies to both manual archiving
   (`ChildDocumentController.kt:696`) and scheduled selection
   (`ChildDocumentArchivalScheduling.kt:50`). A failed job remains in the `async_job` table
   with `retry_count = 0` and is not run again.
   Observed during testing: `ArchiveChildDocument|retry_count 0|initial_retry_count 1|completed_at null`.

The successful response format is taken from `SärmäMockClient`
(`espoo/archival/SärmäMockClient.kt`), as actual Särmä responses are not available in the repository:

```
status_message=Success.&transaction_id=2872934&protocol_version=1.0&status_code=200&instance_ids=354319&
```

The examples' `response.http` files contain responses from the local receiver, not
from the actual Särmä system. The error response contents (`status_code=400`, `status_message=...`) are made up
to illustrate eVaka's response handling logic.

## 3. Metadata XML

The root element is `RecordMetadataInstance`. Namespaces come from the settings:
`ns0` = `evaka.integration.sarma.metadata_main_namespace` and
`at0` = `evaka.integration.sarma.metadata_policy_namespace`.
The examples use `http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0`
and `http://www.avaintec.com/2004/records-schedule-fi/1.0`. These match the XSDs'
`targetNamespace` values. The XML writer is `espoo/archival/RecordMetadataXml.kt` and
the data model is `espoo/archival/model/RecordMetadata.kt`.

| Element | Value / source |
|---|---|
| `metadataMasterVersion/masterName` | `master_id` (same as the multipart field) |
| `metadataMasterVersion/versionNumber` | `1.0` |
| `virtualArchiveId` | `virtual_archive_id` |
| `recordIdentifiers/CaseIdentifier` | `case_process.case_identifier` = `<sequence number>/<process definition number>/<year>` (generated column, `docs/db/schema.sql`). Omitted if the document has no case process. |
| `recordIdentifiers/RecordIdentifier` | `child_document.id` (UUID) |
| `documentDescription/title` | document template name (`document_template.name`) |
| `documentType`, `documentTypeSpecifier` | based on document type, see the table above |
| `registerName` | constant "Varhaiskasvatuksen asiakastietorekisteri" |
| `firstName`, `lastName` | child's name |
| `socialSecurityNumber` | child's personal identity code (omitted if the child has none) |
| `birthDate` | child's date of birth |
| `personalData` | constant `containsSensitivePersonalInformation` |
| `personalDataCollectionReason` | constant "Rekisterinpitäjän lakisääteisten velvoitteiden noudattaminen" |
| `language` | template language in ISO 639-1 format (`fi`/`sv`/`en`) |
| `dataManagement` | constant "Palvelujen tiedonhallinta" |
| `dataSource` | constant "Varhaiskasvatuksen tietovaranto" |
| `Agents/Agent` | One agent for each distinct user who has recorded an entry in the document's process history (`case_process_history.entered_by`). `Role` is always "Henkilökunta" (staff) and `CorporateName` is `archiveMetadataOrganization` = "Espoon kaupungin esiopetus ja varhaiskasvatus" (`espoo/EspooConfig.kt:233`). Decision documents also include the system user "eVaka", which records the COMPLETED state when the Suomi.fi message is sent. A document completed by a guardian also includes the guardian. |
| `format/recordType` | `digital` |
| `format/mimeType` | `application/pdf` |
| `format/fileFormat` | `not applicable` |
| `format/fileName` | PDF filename without the path |
| `creation/originatingSystem` | constant "Varhaiskasvatuksen toiminnanohjausjärjestelmä" |
| `creation/created` | **Case completion date** (same value as `Case_Finished`), not the creation date (`DocumentMetadataUtils.kt:287`). See section 7. |
| `policies/retentionPolicy` | see the document type table |
| `policies/disclosurePolicy` | Confidential template: `InitialTargetState=confidential`, `TimeSpan` = the template's confidentiality period (default 100), `ActionAnnotation` = the template's legal basis for confidentiality. Public template: `public`, `TimeSpan=0`, `ActionAnnotation` "JulkL 24 § 1 mom. 32 k" (default value). `TriggerEvent=YearsFromRecordCreation`, target state `public`. (`DocumentMetadataUtils.kt:26`) |
| `policies/informationSecurityPolicy` | constant `notSecurityClassified` |
| `policies/protectionPolicy` | constant `3` |
| `CaseFile/Case_Created` | Date of the INITIAL entry in the process history, or the document's creation date if no such entry exists. |
| `CaseFile/Case_Finished` | Date of the COMPLETED entry in the process history. If absent, the template's validity end date. If that is also absent, the next 31 July after `Case_Created`. (`DocumentMetadataUtils.kt:227`) |

Process history states (`core/caseprocess/CaseProcess.kt:230`, `updateDocumentCaseProcessHistory`):

- A regular document receives a COMPLETED entry when it is marked as completed.
- A decision document receives a COMPLETED entry only after the Suomi.fi message has been sent
  (`core/document/childdocument/ChildDocumentService.kt:133`).

The case process (`case_process`) is created along with the document only if the template has a process definition number
(`createChildDocument`, `ChildDocumentController.kt:131`). Since transfers to Särmä require a process definition number,
`CaseIdentifier` is effectively always included.

### XSDs

The Särmä XSDs (metadata model `yleinen-1.0.xsd` and its included files `include-extended.xsd` and `include-policies.xsd`) are available from Espoo or the archive system provider. eVaka writes the XML using `XMLStreamWriter` (`espoo/archival/RecordMetadataXml.kt`). The expected XML format is also shown in `service/src/test/resources/archival/expected-metadata.xml`.

All example metadata files validated against `yleinen-1.0.xsd` when the examples were produced.

## 4. How archiving is triggered

Both paths schedule an `AsyncJob.ArchiveChildDocument` async job, handled by
`ArchivalService.uploadChildDocumentToArchive` (`core/document/archival/ArchivalService.kt:183`).
Jobs run in the `archival` pool: by default, concurrency is 4 with no throttling
(`evaka.archival_pool_concurrency`, `evaka.archival_pool_throttle_interval_seconds`,
`core/EvakaEnv.kt:786`).

### a) Manual: `POST /employee/child-documents/{documentId}/archive`

`core/document/childdocument/ChildDocumentController.kt:656`. Requirements:

- `evaka.integration.sarma.enabled` (or `evaka.integration.archival.enabled`) is enabled
  (`core/EvakaEnv.kt:84`).
- The user has the `Action.ChildDocument.ARCHIVE` permission.
- The template has `archive_externally = true`.
- The document status is `COMPLETED`.

The job runs immediately (`runAt = now`, `retryCount = 1`). Examples 01–11 and 14 were produced through this path.

### b) Scheduled selection: `ArchiveEligibleChildDocuments`

- Defined in core (`core/shared/job/ScheduledJobs.kt:317`), **disabled** by default,
  with the `nightly` schedule. It is enabled with
  `evaka.job.archive_eligible_child_documents.enabled=true`, and its schedule can be changed
  with `...cron`. Espoo has no separate schedule: `EspooScheduledJobs` does not include
  archiving. Espoo's production configuration is not in this repository, so it does not
  show whether scheduling is enabled in production.
- Scheduled jobs start only under the Spring profile `production`
  (`core/shared/config/ScheduledJobConfig.kt:32`). Locally, selection was triggered
  by adding a `RunScheduledJob {"job": "ArchiveEligibleChildDocuments"}` row to the `async_job` table.
- Selection (`core/document/archival/ChildDocumentArchivalScheduling.kt`,
  query `getChildDocumentsEligibleForArchival`, `core/document/childdocument/ChildDocumentQueries.kt:796`)
  includes child documents that satisfy:
  - `document_template.archive_externally = true`,
  - `child_document.archived_at IS NULL`,
  - `upper(document_template.validity) <= today - delay`. The delay is
    `evaka.child_document_archival_delay_days`, default 30 (`core/EvakaEnv.kt:737`).
  - Ordered by `created_at`, with an optional limit of `evaka.child_document_archival_limit`
    (default 0 = unlimited).
- Notes on selection criteria:
  - Documents from **templates with no validity end date** (`NULL`)
    never qualify because `upper(...)` is NULL. They can only be archived manually.
  - **The document status is not checked.** See section 7.
- Jobs are scheduled without a user (`user = null`), so `ArchivalService` uses
  the system user. However, this user is not included in the Särmä request as the export initiator.

Examples 12 and 13 were produced through this path.

## 5. Examples (`examples/`)

Each directory contains:

- `request.http`: the full HTTP request. Credentials have been replaced with placeholders
  (`Host` = `<sarma-host>`, `user_id` = `<username>`), and the PDF binary content has been replaced with a reference.
  `Content-Length` is the original value.
- `metadata.xml`: formatted `instance_1_md_instance`.
- `response.http`: the local receiver's response.
- `asiakirja.pdf`: the PDF sent (`instance_1_record_payload_data`).

| Directory | Type | Trigger | What it illustrates |
|---|---|---|---|
| `01-varhaiskasvatussuunnitelma` | VASU, completed | manual | basic case, confidential, permanent retention |
| `02-varhaiskasvatussuunnitelma-ruotsinkielinen` | VASU (sv), completed | manual | `language=sv`. The template language restricts it to Swedish-speaking units. |
| `03-esiopetuksen-oppimissuunnitelma` | LEOPS, completed | manual | template validity end date set. The COMPLETED entry still determines `Case_Finished`. |
| `04-hojks` | HOJKS, completed | manual | |
| `05-pedagoginen-arvio` | PEDAGOGICAL_ASSESSMENT | manual | retention for 28 years from birth |
| `06-pedagoginen-selvitys` | PEDAGOGICAL_REPORT | manual | retention for 28 years from birth |
| `07-muu-asiakirja-julkinen` | OTHER, public template | manual | no `documentType` elements, `disclosurePolicy` = `public` |
| `08-huoltajan-taydentama-asiakirja` | CITIZEN_BASIC | manual | completed by a guardian, PDF is version 2, guardian included in `Agents` |
| `09-paatosasiakirja-hyvaksytty` | OTHER_DECISION, accepted | manual | system user "eVaka" included in `Agents` |
| `10-paatosasiakirja-hylatty` | OTHER_DECISION, rejected | manual | same metadata as for an accepted decision |
| `11-paatosasiakirja-mitatoity` | OTHER_DECISION, accepted and annulled | manual | same metadata; annulment is not reflected in the metadata |
| `12-ajastettu-poiminta-valmis` | VASU, completed, template expired more than 30 days ago | scheduled | requests from the scheduled path use the same format |
| `13-ajastettu-poiminta-luonnos` | LEOPS, **draft** (published version), expired template | scheduled | a draft is sent to the archive. `Case_Finished` = `created` = template end date (2026-07-31), which precedes `Case_Created` (2026-10-15). See section 7. |
| `14-virhevastaus` | PEDAGOGICAL_ASSESSMENT | manual | response `HTTP 200` + `status_code=400`: the document was not marked as archived and the job was not retried |

## 6. Missing examples and why they are missing

- **Daycare, preschool and other application decisions, and fee and voucher value decisions:**
  the Espoo integration does not support them (`TODO`, see section 1).
- **`MIGRATED_VASU`, `MIGRATED_LEOPS` and `MIGRATED_*_ASSISTANCE_NEED_DECISION`:**
  these are created only by data migration and cannot be created through normal UI
  or API operations. Their metadata matches the corresponding types (VASU/LEOPS)
  or decision documents (no `documentType` elements).
- **Document without a process definition number or a published version:** no HTTP request
  is sent.
  - A missing process definition number causes `IllegalStateException` (`SärmäChildDocumentClient.kt:68–72`).
  - A missing published version causes `NotFound` (`ArchivalService.kt:199`). This was observed
    with an unpublished draft during scheduled selection.
- **Actual Särmä responses:** these were not available. The response format is based on
  `SärmäMockClient` and the integration test
  (`service/src/integrationTest/kotlin/evaka/core/document/archival/ArchivalServiceIntegrationTest.kt`).

## 7. Notes on the current implementation

1. **Scheduled selection sends drafts, and the same document may be sent again every night.**
   `getChildDocumentsEligibleForArchival` does not filter by status, unlike the manual path,
   which requires `COMPLETED`.
   - If a draft has a published version, the PDF and metadata are sent to Särmä, and Särmä
     responds successfully (example 13).
   - After this, `markDocumentAsArchived` fails on the database constraint
     `archived_documents_must_be_completed` (`CHECK (archived_at IS NULL OR status = 'COMPLETED')`,
     `docs/db/schema.sql`).
   - `archived_at` remains empty, so the document is selected on every run, creating
     duplicates in Särmä.
   - An unpublished draft fails with `NotFound` on every run.
   - Observed in the local run with the log messages "violates check constraint
     archived_documents_must_be_completed" and "No published version found".
2. **Documents from templates with no validity end date are never selected by the scheduled job**
   (`upper(validity)` is NULL).
3. **`creation/created` is the case completion date, not its creation date**
   (`creation.copy(created = caseFinishDate)`, `DocumentMetadataUtils.kt:287`). If the document
   has no COMPLETED entry, `Case_Finished` (and `created`) may precede `Case_Created`
   (example 13). This may be intentional if Särmä calculates retention from
   `created`, but there is no comment explaining it.
4. **A guardian is included in `Agents` with the role "Henkilökunta" (staff)** (example 08):
   `createAgents` assigns the same role to everyone who has recorded a process history entry.
5. **The decision outcome and annulment are not reflected in the metadata** (examples 09–11).
6. **Särmä's `instance_ids` value is not stored in eVaka.** It appears only in the log, so
   an archived copy can only be matched to an eVaka document using `RecordIdentifier`
   (= `child_document.id`).

## 8. Settings

| Setting (Spring) | Environment variable | Meaning |
|---|---|---|
| `evaka.integration.sarma.enabled` | `EVAKA_INTEGRATION_SARMA_ENABLED` | Enables archiving (`EvakaEnv.archivalEnabled`, alternative to `evaka.integration.archival.enabled`). |
| `evaka.integration.sarma.use_mock_client` | `..._USE_MOCK_CLIENT` | `true` = `SärmäMockClient`, default `false`. Set to `true` in the local profile. |
| `evaka.integration.sarma.url` | `..._URL` | Base URL ending with `/`. |
| `evaka.integration.sarma.user_id` / `user_role` | `..._USER_ID` / `..._USER_ROLE` | Form fields `user_id` / `user_role`. |
| `evaka.integration.sarma.master_id` / `virtual_archive_id` | `..._MASTER_ID` / `..._VIRTUAL_ARCHIVE_ID` | Metadata model and virtual archive. |
| `evaka.integration.sarma.metadata_main_namespace` / `metadata_policy_namespace` | `..._METADATA_MAIN_NAMESPACE` / `..._METADATA_POLICY_NAMESPACE` | XML namespaces. |
| `evaka.job.archive_eligible_child_documents.enabled` / `.cron` | `EVAKA_JOB_ARCHIVE_ELIGIBLE_CHILD_DOCUMENTS_ENABLED` / `_CRON` | Scheduled selection. |
| `evaka.child_document_archival_delay_days` / `_limit` | | Selection delay (30) and maximum count (0 = unlimited). |
| `evaka.archival_pool_concurrency` / `_throttle_interval_seconds` | | Async pool concurrency (4) and throttling (0 s). |

Beans: `EspooConfig.särmäClient` (`espoo/EspooConfig.kt:323`) selects the mock or
HTTP client, and `archivalIntegrationClient` (line 331) selects `SärmäChildDocumentClient`.
If archiving is disabled, `FailingClient` is used.

## 9. How the examples were produced

- Local eVaka (Espoo customizations, `local` profile).
- Environment variables: `EVAKA_INTEGRATION_SARMA_USE_MOCK_CLIENT=false` and
  `EVAKA_INTEGRATION_SARMA_URL=http://127.0.0.1:<port>/archive-core/`.
- The receiver captured requests and returned a response matching `SärmäMockClient`.
- People, placements and document templates were created through the dev API. Documents went through
  the actual employee and guardian APIs (creation, content, state transitions,
  decision proposal, acceptance/rejection/annulment, completion by a guardian).
- Archiving was triggered with `POST /employee/child-documents/{id}/archive`
  or the async job for scheduled selection.
- For scheduled selection, template validity periods were moved into the past using SQL
  (2025-08-01–2026-07-31).
