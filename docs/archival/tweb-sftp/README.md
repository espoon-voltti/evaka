<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Nokia, Pirkkala and Kangasala: archiving to Tweb (SFTP)

This document describes the Tampere region's Tweb archival integration, used by eVaka in Nokia, Pirkkala and Kangasala. eVaka writes a PDF and a metadata XML file for each document to an SFTP server.

The example files were produced in a local development environment with Nokia's configuration through normal employee operations and collected from a local SFTP server. The people and personal identity codes are made up. The description reflects the code in October 2026, and the examples are not updated automatically.

## 1. Architecture overview

- The integration is implemented in `RegionalTwebArchivalClient` (`service/src/main/kotlin/evaka/trevaka/archival/tweb/RegionalTwebArchivalClient.kt`). It implements the core interface `ArchivalIntegrationClient`.
- Each municipality creates the client in its own configuration: `NokiaConfig.kt:150`, `PirkkalaConfig.kt:149` and `KangasalaConfig.kt:151` (`service/src/main/kotlin/evaka/instance/<municipality>/`). The client is created only if `evaka.integration.archival.enabled=true` and the municipality's `archival` settings are provided. Otherwise, `ArchivalIntegrationClient.FailingClient` is used, which throws an exception.
- Files are transferred using the core `SftpClient` (`service/src/main/kotlin/evaka/core/shared/sftp/SftpClient.kt`, JSch).
- The core `ArchivalService` (`service/src/main/kotlin/evaka/core/document/archival/ArchivalService.kt`) fetches the document, its PDF from S3 and its case management process. It calls the client and sets the document's `archived_at` timestamp.
- Metadata uses Profium's "collections" XML (SÄHKE2 elements), generated with JAXB from an XSD.

## 2. SFTP connection and settings

Configuration classes:

- `SftpArchivalProperties(schedule: ArchivalSchedule = ArchivalSchedule(), sftp: SftpProperties)` – `service/src/main/kotlin/evaka/instance/nokia/NokiaProperties.kt`. Pirkkala and Kangasala use the same class (`PirkkalaProperties.kt`, `KangasalaProperties.kt`, field `archival`).
- `SftpProperties(host, port, hostKeys, username, password?, privateKey?, prefix)` – `service/src/main/kotlin/evaka/trevaka/sftp/SftpProperties.kt`.

Environment variables (Spring relaxed binding, `<MUNICIPALITY>` = `NOKIA` / `PIRKKALA` / `KANGASALA`):

| Variable | Meaning |
|---|---|
| `EVAKA_INTEGRATION_ARCHIVAL_ENABLED` | global switch for archiving (`EvakaEnv.archivalEnabled`) |
| `<MUNICIPALITY>_ARCHIVAL_SFTP_HOST`, `_PORT`, `_HOST_KEYS`, `_USERNAME` | SFTP server, port, server public key (base64, without the key type prefix) and username |
| `<MUNICIPALITY>_ARCHIVAL_SFTP_PRIVATE_KEY` or `_PASSWORD` | authentication; production uses a private key |
| `<MUNICIPALITY>_ARCHIVAL_SFTP_PREFIX` | directory prefix prepended directly to the filename (string concatenation, so a trailing `/` is required) |
| `<MUNICIPALITY>_JOB_PLAN_DOCUMENT_ARCHIVAL_ENABLED` / `_CRON` | scheduled job switch and schedule |
| `<MUNICIPALITY>_ARCHIVAL_SCHEDULE_DAILY_DOCUMENT_LIMIT`, `_DECISION_DELAY_DAYS`, `_FEE_DECISION_DELAY_DAYS`, `_VOUCHER_DECISION_DELAY_DAYS`, `_DOCUMENT_DECISION_DELAY_DAYS`, `_DOCUMENT_PLAN_DELAY_DAYS` | scheduling limits (see section 7) |

Production and test environment values (server, credentials, directory prefix, daily limit) are in each municipality's infrastructure configuration, not in this repository.

Local development environment: `application-<municipality>_evaka.yaml` sets `evaka.integration.archival.enabled: true` in the local profile, but does not provide `archival.sftp` settings. Therefore, `FailingClient` is used locally by default. For the example run, the integration was pointed to the Compose SFTP container (`compose/docker-compose.yml`, service `sftp`, user `foo`, directory `upload`) using environment variables added to `mise.local.toml`:

```
NOKIA_ARCHIVAL_SFTP_HOST=localhost
NOKIA_ARCHIVAL_SFTP_PORT=2229            # EVAKA_SFTP_PORT = 2222 + instance number
NOKIA_ARCHIVAL_SFTP_HOSTKEYS=<base64 portion of compose/sftp/ssh_host_ed25519_key.pub>
NOKIA_ARCHIVAL_SFTP_USERNAME=foo
NOKIA_ARCHIVAL_SFTP_PASSWORD=pass
NOKIA_ARCHIVAL_SFTP_PREFIX=upload/
```

Connection details (`SftpClient.kt`):

- The server key is checked against the `hostKeys` list (`ReadOnlyHostKeyRepository`).
- Connection and read timeouts are 60 s.
- **Each `put` opens its own SSH session**, so one document requires two connections.
- The file is written directly to its final name (`ChannelSftp.put`, default mode OVERWRITE). No temporary file or rename is used.

## 3. Directory structure and filenames

`RegionalTwebArchivalClient.postRecord` (`RegionalTwebArchivalClient.kt:109–131`):

1. `<prefix><uuid>.<extension>` – document content. The extension is inferred from the content's MIME type using Tika (`MimeTypes.forName(contentType).extension`). In practice, the file is always `.pdf`.
2. `<prefix><uuid>.xml` – metadata.

- `<uuid>` is the eVaka document ID: `decision.id`, `fee_decision.id`, `voucher_value_decision.id` or `child_document.id`. The same value appears in the XML as `file/@originalId`. The filename is `file/@name`.
- All files go into the same flat directory. There are no subdirectories by municipality or type. The document type can only be inferred from the XML's `folder` and `title` fields.
- The PDF is written first, followed by the XML. If writing the XML fails, an orphaned PDF remains on the server.
- Exporting again with the same ID (e.g. manually triggered rearchiving) overwrites both files. This was observed during the run: the file timestamps and `archived_at` were updated.

## 4. Metadata format and schema

- The schemas are in the repository: [`profium-collection-sahke2.xsd`](../../../service/src/main/schema/profium-collection-sahke2.xsd) (namespace `http://www.profium.com/reception/2022/03`, root element `collections`) and [`sahke2.xsd`](../../../service/src/main/schema/sahke2.xsd) (namespace `http://www.profium.com/sahke2`). JAXB classes are generated from them by the Gradle task `xjcGenerate` (`service/build.gradle.kts`, `registerXjcTask("xjcGenerate", "src/main/schema")`).
- JAXB produces formatted XML: `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>`, default namespace reception/2022/03 and SÄHKE2 elements with the `ns2:` prefix.
- **Validation:** all 20 generated metadata files validate against the schema (`xmllint --noout --schema service/src/main/schema/profium-collection-sahke2.xsd *.xml` → "validates").
- The XSD requires `created` (minOccurs=1). The code sets it only when the source value is not null (`sentDate`, `approvedAt`, `publishedAt`). In archivable states, these are effectively always set; otherwise, the XML would not conform to the schema.
- The optional XSD fields `description`, `file/@format`, `file/@size` and `collections/@notifyemail` are unused. `agent/corporateName` is also unused (the code comment says "agent corporateName left empty").

Structure:

```
collections
  initiator
    source_system   "eVaka" (constant)
    initiator_id    export initiator's evaka_user.id
    initiator_name  export initiator's name
  collection        (always exactly one)
    type            "record" (constant)
    folder          process definition number (processDefinitionNumber)
    metadata
      ns2:title                type;status;firstname lastname;personal identity code
      ns2:calculationBaseDate  retention calculation base date
      ns2:created              date
      ns2:agent*               { agentRole, agentName }
    content
      file @name="<uuid>.pdf" @originalId="<uuid>"   (always exactly one)
```

## 5. Metadata field meanings and value sources

| Field | Source | Reference |
|---|---|---|
| `initiator_id` / `initiator_name` | `EvakaUser` (ID and name) that triggered the export. Scheduled runs use the system user `00000000-0000-0000-0000-000000000000` / `eVaka`. Manual exports use the ADMIN employee, with the name in "Lastname Firstname" order. | `RegionalTwebArchivalClient.kt:99–107`, `ArchivalService.getUser` |
| `folder` | **Daycare, preschool and club decisions:** `processDefinitionNumber` from the application's case management process. Exception: `PRESCHOOL_DAYCARE` decisions always use the municipality's `APPLICATION_DAYCARE` configuration for 2026 (hardcoded year, as year-specific configuration is not used). **Fee and voucher value decisions:** the process number. **Child documents:** the document template's `processDefinitionNumber`. | `RegionalArchivalDecision.kt:29–41`, `RegionalArchivalFeeDecision.kt:23`, `RegionalArchivalVoucherValueDecision.kt:23`, `RegionalArchivalChildDocument.kt:25` |
| `title` | A `;`-separated string: document type; status; name ("Firstname Lastname"); personal identity code. For placement decisions and child documents, the person is the child; for fee and voucher value decisions, it is the head of family. If the personal identity code is missing, the export fails (`error("No owner ssn available")`). | `hofTitle`/`childTitle`, `RegionalTwebArchivalClient.kt:169–185` |
| `calculationBaseDate` | Placement decisions: `decision.endDate + 1`. Fee decisions: `validDuring.end + 1`. Voucher value decisions: `validTo + 1`. **Child documents: the document template's validity start date** (`template.validity.start`), not the document's own date. | `RegionalArchivalDecision.kt:45`, `RegionalArchivalFeeDecision.kt:27`, `RegionalArchivalVoucherValueDecision.kt:32`, `RegionalArchivalChildDocument.kt:34` |
| `created` | Placement decisions: `sentDate`. Fee and voucher value decisions: `approvedAt` (as a date). Child documents: `publishedAt` (as a date). | same files |
| `agent` | Case process history entries with `enteredBy` of type EMPLOYEE are grouped by employee. The role is **"Päättäjä" (decision-maker)** if the employee has an entry in the `DECIDING` state, otherwise **"Laatija" (author)**. The name is `EvakaUser.name` ("Lastname Firstname"). SYSTEM and CITIZEN entries are skipped. Agents are optional for daycare and preschool decisions (migrated decisions → no agent elements, fixture `decision-metadata-no-agents.xml`). Other types require at least one employee; otherwise, the export fails. | `RegionalTwebArchivalClient.kt:146–167` |
| `file/@name`, `@originalId` | document UUID (+ extension) | `RegionalArchival*.kt` |

Information **not** included in the metadata, even though eVaka has it:

- case identifier (`case_process.case_identifier`, e.g. `1/04.01.00.11/2026`)
- organization (`archiveMetadataOrganization`) and business ID (`metadataBusinessId`)
- retention period (`archiveDurationMonths`)
- publicity class or legal basis for confidentiality
- unit and decision validity period

The receiving system is therefore responsible for calculating the retention period (`folder` + `calculationBaseDate`).

## 6. Supported document types and status values

Status values: `service/src/main/kotlin/evaka/trevaka/archival/DocumentStatus.kt`. Type names: `RegionalArchivalDecision.kt:59–73`.
The Finnish type names and statuses below are literal values sent in `title`: `Hyväksytty` = accepted, `Hylätty` = rejected, `Lähetetty` = sent, `Mitätöity` = annulled and `Valmis` = completed.

| Document | Type in `title` | Archivable states → status in `title` | Example |
|---|---|---|---|
| Decision DAYCARE / DAYCARE_PART_TIME | Varhaiskasvatuspäätös | ACCEPTED → Hyväksytty, REJECTED → Hylätty | 01, 02 |
| Decision PRESCHOOL | Esiopetuspäätös | same | 03 |
| Decision PRESCHOOL_DAYCARE | Täydentävän varhaiskasvatuksen päätös | same | 04 |
| Decision PRESCHOOL_CLUB | Esiopetuksen kerhon päätös | same | – |
| Decision CLUB | Kerhopäätös | same (skipped by scheduled jobs, see section 7) | – |
| Decision PREPARATORY_EDUCATION | – | `UnsupportedOperationException` | – |
| Fee decision | Maksupäätös | SENT → Lähetetty, ANNULLED → Mitätöity | 05, 06, 16 |
| Voucher value decision | Arvopäätös | SENT → Lähetetty, ANNULLED → Mitätöity | 07, 08, 16 |
| Child document without a decision (VASU, LEOPS, HOJKS, PEDAGOGICAL_ASSESSMENT, PEDAGOGICAL_REPORT, OTHER, CITIZEN_BASIC, MIGRATED_*) | document template name | COMPLETED → Valmis | 09–12 |
| Child decision document (OTHER_DECISION) | document template name | decision ACCEPTED → Hyväksytty, REJECTED → Hylätty, ANNULLED → Mitätöity | 13–15 |

Other state restrictions:

- A child document is exported only if the template has `archive_externally = true` and the document status is `COMPLETED` (`ChildDocumentController.planArchiveChildDocument`). It must also have a case management process; otherwise, `error("No child document case process available")` is raised. The process is created only if the template has `processDefinitionNumber`.
- Placement decisions must have an application case management process (`ArchivalService.uploadDecisionToArchive` → `NotFound`). A process is created only if the municipality's `archiveMetadataConfigs` returns a configuration for the application type (`CaseProcess.kt:33`).

## 7. Scheduling and delays

- The scheduled job `PlanDocumentArchival` exists in all three municipalities (`<Municipality>ScheduledJobs.kt:33`). Its default schedule is daily at 20:00 (Helsinki time), and it is disabled by default. It is enabled through infrastructure configuration in deployed environments. The job calls `planDocumentArchival` (`service/src/main/kotlin/evaka/trevaka/archival/TrevakaArchivalScheduling.kt`).
- Defaults are defined in `ArchivalSchedule` (`service/src/main/kotlin/evaka/instance/tampere/TampereProperties.kt:50`): `dailyDocumentLimit=0` (0 = no exports), placement decisions 180 days, fee decisions 180, voucher value decisions 180, child decision documents 30 and child plans 180.
- Selection criteria (`TrevakaArchivalScheduling.kt:72–135`):
  - **Placement decisions:** status ACCEPTED/REJECTED, `type <> 'CLUB'`, not archived, `resolved <= today − decisionDelayDays`, and PDF exists.
  - **Fee and voucher value decisions:** status SENT/ANNULLED, not archived, `approved_at <= today − delay`, and PDF exists.
  - **Child plan documents** (`type <> 'OTHER_DECISION'`): COMPLETED, published version has a PDF, `archive_externally`, and **document template validity end date <= today − documentPlanDelayDays**. Templates with no validity end date (`validity.end = null`) never qualify, so their documents are not archived by scheduled jobs.
  - **Child decision documents** (`OTHER_DECISION`): in addition to the preceding criteria, the child must have no placements with `end_date > today − documentDecisionDelayDays`. Decision documents are therefore exported only after all of the child's early childhood education placements have ended and the delay has elapsed.
- The daily quota is distributed among groups in proportion to the number of eligible documents (`distributeProportionally`, integer division rounded down). Within each group, the oldest documents are exported first (`ORDER BY created / resolved / approved_at`, `DocumentArchivalQueries.kt`).
- Async jobs (`ArchiveDecision`, `ArchiveFeeDecision`, `ArchiveVoucherValueDecision`, `ArchiveChildDocument`) are scheduled with `retryCount = 1`, meaning **one attempt without retries** (`TrevakaArchivalScheduling.kt:59`). Scheduled runs use `user = null`, so the initiator is the system user.
- Manual triggers: `POST /employee/decisions/{id}/archive`, `/employee/fee-decisions/{id}/archive`, `/employee/value-decisions/{id}/archive` and `/employee/child-documents/{id}/archive`. These require the ADMIN role (`Action.kt`, `ARCHIVE(HasGlobalRole(ADMIN))`) and use `retryCount = 1`. The endpoints do not check `archived_at`, so an archived document can be exported again (overwriting the files).

## 8. Success and error handling – no acknowledgement

- `postRecord` always returns the constant string **`"static-sftp-response"`** (`RegionalTwebArchivalClient.kt:131`). The value is used only in the log message "Successfully archived … to instance static-sftp-response" (`ArchivalService`) and is not stored.
- **eVaka receives no acknowledgement from the receiving system.** Success only means that both SFTP writes completed without an exception. eVaka does not know whether Tweb/Frends processed the files or accepted the metadata. No return channel (acknowledgement file, email or `notifyemail`) is implemented.
- After a successful write, `ArchivalService` sets `archived_at = now()` in a separate transaction. If this update failed, the files would already be on the server, and the next run would export them again with the same names.
- Error case (observed during the run: SFTP container stopped, manual export): JSch throws `JSchException: java.net.ConnectException: Connection refused`. The async job is marked as "permanently failed (remainingAttempts=0)", and `archived_at` remains unchanged. Nothing is recorded on the document, and no error is shown to the user (the job runs in the background).
- In scheduled use, a failed document remains unarchived, and the next day's run selects it again. Since selection starts with the oldest documents, documents that fail persistently (e.g. missing personal identity code or agents) are selected first every day and consume the quota.

## 9. Examples (`examples/`)

| Directory | Contents |
|---|---|
| 01-varhaiskasvatuspaatos-hyvaksytty | municipal unit and service voucher unit (same type) |
| 02-varhaiskasvatuspaatos-hylatty | guardian rejected the placement |
| 03-esiopetuspaatos-hyvaksytty | |
| 04-taydentavan-varhaiskasvatuksen-paatos-hyvaksytty | from a preschool application; `folder` = daycare process definition number |
| 05-maksupaatos-lahetetty | one manual export and one scheduled export (initiator `eVaka`) |
| 06-maksupaatos-mitatoity | |
| 07-arvopaatos-lahetetty | one manual export and one scheduled export |
| 08-arvopaatos-mitatoity | |
| 09-varhaiskasvatussuunnitelma-valmis | VASU |
| 10-pedagoginen-arvio-valmis | PEDAGOGICAL_ASSESSMENT |
| 11-esiopetuksen-oppimissuunnitelma-valmis | LEOPS |
| 12-pedagoginen-selvitys-valmis | PEDAGOGICAL_REPORT, scheduled export |
| 13–15-lapsen-paatosasiakirja-* | OTHER_DECISION: accepted, rejected, annulled |
| 16-lahetettyna-arkistoitu-myohemmin-mitatoity | see section 11, item 1 |

Each directory contains the XML file(s) as sent with their original filenames, the corresponding PDFs and a `files.md` file listing the filenames and summarizing the metadata.

Generation process: people (also in the VTJ mock) and document templates were created using the dev API. Employee APIs were used for paper application → placement proposal → sending decisions → acceptance/rejection, for confirming and sending fee and voucher value decisions, and for annulling them through an income change. Child documents went through their state transitions, and decision documents were proposed to a decision-maker who accepted, rejected or annulled them. Exports were triggered through the archival endpoints as an ADMIN user. `PlanDocumentArchival` was also run by adding a `RunScheduledJob` job to the async queue, because db-scheduler starts only under the `production` profile (`ScheduledJobConfig.kt`). This is the same job that the production scheduler adds. For scheduled runs, delays were set to zero or negative values so that documents from the same day would qualify.

The process definition numbers come from Nokia's configuration: 04.01.00.11 for daycare applications, 04.01.03.41 for preschool applications, 04.01.00.12 for fee decisions and 04.01.02.59 for voucher value decisions. Child document numbers (04.01.00.20–23, 04.01.03.42) were made up for the example templates, as they come from the template rather than the municipality configuration.

## 10. Missing examples and why they are missing

- **Club decision (CLUB):** scheduled jobs skip CLUB decisions (`type <> 'CLUB'`). Nokia and Kangasala also lack an `APPLICATION_CLUB` configuration, so club applications have no case management process and cannot be exported even manually. Pirkkala has the configuration (4801.23), so manual export would be possible, but it was not run.
- **PRESCHOOL_CLUB, DAYCARE_PART_TIME:** the format is the same as for other decisions; only the type in `title` varies (see section 6). Not produced.
- **PREPARATORY_EDUCATION:** unsupported (exception).
- **HOJKS, OTHER, CITIZEN_BASIC, MIGRATED_*:** the format is the same as in examples 09–12 (template name; Valmis). Not produced separately.
- **Migrated decision without agents:** not produced. The integration test fixture `service/src/integrationTest/resources/tweb-archival-client/decision-metadata-no-agents.xml` shows the format.
- **Pirkkala and Kangasala:** no run was performed because the code is identical; only process definition numbers and municipality names differ (section 12).
- **SFTP error:** tested (section 8), but it does not produce a file.

## 11. Notes on the current implementation

1. **A decision archived as sent is not exported again when annulled.** If a fee or voucher value decision has already been archived in the SENT state and is later annulled (replaced by another decision), `archived_at` is already set and no new export occurs. The archive retains "Lähetetty" (sent) in the title (example 16). The default delay of 180 days reduces this risk but does not eliminate it.
2. **A child document's `calculationBaseDate` is the document template's validity start date**, so it is the same for all documents using that template. In the examples, it is 2026-08-01 even for decisions made on 2026-10-15.
3. **Plan documents from templates with no validity end date are never archived by scheduled jobs** (`upper(validity)` is null → the condition is not met).
4. **Date and timestamp comparison:** `resolved` and `approved_at` are `timestamptz` columns and are compared against a `LocalDate` (= midnight). A document therefore qualifies only on *decision date + delay + 1*. During the run, a delay of 0 did not select a fee decision from the same day; a delay of −1 did.
5. **The `folder` for a supplementary daycare decision** is read from the daycare application configuration with the hardcoded year 2026 (`RegionalArchivalDecision.kt:36`), not from the decision's own process.
6. **`created` is missing from the XML** if the source value is null. This would violate the XSD (minOccurs=1), but in archivable states the value is effectively always present.
7. **The personal identity code appears in plain text in `title`.** Without it, the export fails and is attempted again daily (section 8).
8. **Agents:** for fee and voucher value decisions, "Päättäjä" is the finance user who confirmed the decisions. For plan documents, all employees are "Laatija", including the one who marked the document as completed. Names use "Lastname Firstname" order, while the person's name in `title` uses "Firstname Lastname" order.
9. **The case identifier is not sent**, even though eVaka generates it (section 5).
10. **Two separate SFTP sessions** per document, no atomic write and no acknowledgement (sections 2, 3 and 8).

## 12. Differences between municipalities

The code (`RegionalTwebArchivalClient` and `PlanDocumentArchival`) is identical for all three municipalities. Differences are in `FeatureConfig` (`<Municipality>Config.kt`, `archiveMetadataConfigs`) and infrastructure environment variables.

| | Nokia | Pirkkala | Kangasala |
|---|---|---|---|
| `archiveMetadataOrganization` (not exported) | Nokian kaupunki, varhaiskasvatus ja esiopetus | Pirkkalan kunta, varhaiskasvatus | Kangasalan kaupunki, varhaiskasvatus |
| APPLICATION_DAYCARE → `folder` (also PRESCHOOL_DAYCARE) | 04.01.00.11 | 4801.101 | 46.00.63 |
| APPLICATION_PRESCHOOL | 04.01.03.41 | **not configured** → preschool application decisions have no process and cannot be archived | 41.01.05 |
| APPLICATION_CLUB | no | 4801.23 (scheduled jobs still skip CLUB decisions) | no |
| FEE_DECISION | 04.01.00.12 | 4801.102 | 46.00.64 |
| VOUCHER_VALUE_DECISION | 04.01.02.59 | 4803.03 | 46.02.07 |
| archiveDurationMonths (not exported) | applications 120, finance decisions 180 | 144 / 180 | applications 1440 (120 years), finance decisions 120 |
| Environment variable prefix | `NOKIA_` | `PIRKKALA_` | `KANGASALA_` |

For child documents, `folder` comes from the document template, so it is maintained by the municipality's administrator rather than defined in the code.
