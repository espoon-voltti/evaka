<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Archival integrations: technical descriptions and examples

This directory describes eVaka's current archival integrations: what eVaka sends, in which format and when. The descriptions and examples are intended for municipalities, integration platform providers and archive system providers planning a new integration or evaluating whether an existing one can be used as is.

An overview of archiving and case management metadata is available in the eVaka wiki (in Finnish): [Archiving](https://github.com/espoon-voltti/evaka/wiki/Arkistointi) and [Case management processes and metadata](https://github.com/espoon-voltti/evaka/wiki/Asianhallintaprosessit-ja-metatiedot).

## Implementations

| Municipality | Description | Transfer method | Documents | Receipt acknowledgement |
|---|---|---|---|---|
| Espoo | [espoo-sarma](espoo-sarma/) | HTTP POST (multipart) to Särmä's X-Archive API | child documents | yes, `status_code` and an archive ID in the response |
| Tampere | [tampere-frends-rest](tampere-frends-rest/) | REST (multipart) through the Frends integration platform | placement decisions, fee and voucher value decisions, child documents | yes, an archive ID in the response |
| Nokia, Pirkkala, Kangasala | [tweb-sftp](tweb-sftp/) | SFTP (PDF and metadata XML as separate files) | placement decisions, fee and voucher value decisions, child documents | no, only confirmation that the files were written successfully |

Other municipalities do not have an archival integration enabled (`ArchivalIntegrationClient.FailingClient`).

The examples were produced by running eVaka locally with each municipality's configuration and pointing the integration to a local receiver that captured requests as sent. Documents were processed through normal employee operations. The people are fictional, and the personal identity codes are made up. The examples are not updated automatically and reflect the situation at the time of writing (October 2026). The source code, referenced at the end of each description, is the authoritative source for current behaviour.

Example directory and file names, captured payloads and literal metadata values retain their original language to match the data sent by eVaka.

The `request.http` files use LF line endings. Actual HTTP requests use CRLF line endings in headers and multipart sections.

## Shared model

All integrations implement the same interface, `ArchivalIntegrationClient` (`service/src/main/kotlin/evaka/core/document/archival/`). Each document group has its own method:

| Method | Documents | Metadata source |
|---|---|---|
| `uploadDecisionToArchive` | daycare, preschool and club decisions | the application's case management process (`case_process`) and municipality configuration |
| `uploadFeeDecisionToArchive` | fee decisions | the fee decision's case management process and municipality configuration |
| `uploadVoucherValueDecisionToArchive` | voucher value decisions | the voucher value decision's case management process and municipality configuration |
| `uploadChildDocumentToArchive` | child documents created from templates | the document template's archival settings (process definition number, retention period, confidentiality) and case management process |

eVaka provides the integration with the document details, PDF, person associated with the document, case management process with its history, and export initiator. The integration uses these to construct the metadata message required by the archive system and sends it. When the method returns without an error, eVaka marks the document as archived (`archived_at`). If the method throws an error, the document remains unarchived.

An export can be triggered in two ways:

- **Manually**: an employee clicks the document's archive button ("Arkistoi" in Finnish).
- **On a schedule**: the municipality's scheduled job selects eligible documents and creates background jobs for them. Selection criteria and delays vary by municipality.

Background jobs run in a dedicated `archival` pool. Its concurrency and rate are controlled by `evaka.archival_pool_concurrency` (default 4) and `evaka.archival_pool_throttle_interval_seconds` (default 0).

## Designing a new integration

An integration is easiest to implement when the archive system or integration platform

- accepts one document at a time in a synchronous call (e.g. REST), including both metadata and the PDF
- returns a machine-readable indication of whether receipt succeeded (e.g. an archive ID or error code), so eVaka can mark the document as archived and the material can later be removed from eVaka without manual work (a document rejected by the archive's validation must also be reported as an error)
- provides a test environment that accepts material from both a developer's machine and eVaka's test environment, and allows received documents and their metadata to be inspected through a UI or API.

If the archive system already accepts an existing message format (for example, Tampere's Profium message format through Frends), a new municipality can use the existing integration with configuration changes and municipality-specific process definition numbers.
