<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Tampere: archiving through Frends (REST)

Tampere exports documents to the archive through the Frends integration platform. eVaka sends one document per HTTP request and receives an archive ID in the response. The metadata follows Profium's SÄHKE2-based reception schema.

The examples were produced by running eVaka locally with Tampere's configuration. Documents were processed through normal employee operations, and the requests sent by eVaka were captured as sent. The people are fictional, and the personal identity codes are made up. The examples reflect the situation in October 2026 and are not updated automatically when the code changes.

## API

| | |
|---|---|
| Method and path | `POST {baseUrl}/records/add` |
| Content type | `multipart/form-data` |
| Part `xml` | Metadata, `application/xml; charset=utf-8`. Schema: [`profium-collection-sahke2.xsd`](../../../service/src/main/schema/profium-collection-sahke2.xsd), which references [`sahke2.xsd`](../../../service/src/main/schema/sahke2.xsd) |
| Part `content` | Document (PDF). The part's `filename` matches the metadata's `file/@originalId` |
| Headers | `X-API-version: 1.0`, `X-API-transactionid: <eVaka trace ID>`, `Accept: */*` |
| Authentication | HTTP Basic authentication to Frends (`TAMPERE_FRENDS_USERNAME`, `TAMPERE_FRENDS_PASSWORD`). The integration platform adds the `X-API-key` header sent to the archive system |
| Address | `TAMPERE_ARCHIVAL_BASEURL` |
| Timeouts | Connection, read and write: 1 min each |

Each request contains one document: one `collection` and one file.

### Response

eVaka interprets the response as follows (structure: [`frends-response.xsd`](frends-response.xsd)):

- **HTTP 2xx and `<success>`**: eVaka logs success and the archive ID from the response (`records/record/archiveid`) and marks the document as archived. The archive ID is not stored in the database.
- **HTTP 2xx, but the body is not `<success>`**: eVaka logs an error but still marks the document as archived.
- **HTTP 4xx/5xx**: eVaka logs an error (the contents of the `<errors>` body or the raw response), and the document is not marked as archived. For scheduled exports, the job is not retried, but the document becomes eligible again on the next run.

Examples: [`responses/success.xml`](responses/success.xml) and [`responses/error.xml`](responses/error.xml). The `error_code` and `error_summary` values in the error example are illustrative, not actual vendor codes. `frends-response.xsd` is derived from eVaka's response parsing code (`TampereArchivalSchema.kt`); it is not the vendor's official schema.

## Metadata

| Field | Contents |
|---|---|
| `initiator/source_system` | Always `eVaka` |
| `initiator/initiator_id`, `initiator_name` | Export initiator. The system user for scheduled exports; the employee for manual exports (name in Lastname Firstname order) |
| `collection/type` | Always `record` |
| `collection/folder` | Process definition number (records management plan identifier) from the case management process (`case_process.process_definition_number`) |
| `metadata/title` | `<Document name>, <Status>, <Firstname Lastname>, <date of birth dd.mm.yyyy>`. The person is the child, except for fee and voucher value decisions, where it is the head of family |
| `metadata/calculationBaseDate`, `created` | Date sent for placement decisions, approval date for fee and voucher value decisions, publication date for child documents |
| `metadata/agent` | Agents with the role `Laatija` (author) or `Päättäjä` (decision-maker), see the table below |
| `content/file/@name` | `<Document name>.pdf` |
| `content/file/@originalId` | The document's eVaka ID (UUID) |

The personal identity code is not sent in the metadata. It appears only in the PDF.

## Document types

The status and agent role values below are the literal Finnish values sent in the metadata: `Hyväksytty` = accepted, `Hylätty` = rejected, `Lähetetty` = sent, `Mitätöity` = annulled and `Valmis` = completed.

| Example | Document | Process definition number | Statuses in title | Agents |
|---|---|---|---|---|
| [01](examples/01-varhaiskasvatuspaatos-hyvaksytty), [02](examples/02-varhaiskasvatuspaatos-hylatty) | Daycare decision (including part-time daycare) | 12.06.01.17 | Hyväksytty, Hylätty | – |
| [03](examples/03-esiopetuspaatos-hyvaksytty) | Preschool decision | 12.06.01.17 | Hyväksytty, Hylätty | – |
| [04](examples/04-taydentavan-varhaiskasvatuksen-paatos-hyvaksytty) | Supplementary daycare decision (daycare supplementing preschool) | 12.06.01.17 | Hyväksytty, Hylätty | – |
| [05](examples/05-maksupaatos-lahetetty), [06](examples/06-maksupaatos-mitatoity) | Fee decision | 12.06.01.23 | Lähetetty, Mitätöity | Päättäjä = decision handler |
| [07](examples/07-arvopaatos-lahetetty), [08](examples/08-arvopaatos-mitatoity) | Voucher value decision | 12.06.01.23 | Lähetetty, Mitätöity | Päättäjä = decision handler |
| [09](examples/09-varhaiskasvatussuunnitelma-valmis) | Early childhood education and care plan | from the document template* | Valmis | Laatija |
| [10](examples/10-esiopetuksen-oppimissuunnitelma-valmis) | Preschool learning plan | from the document template* | Valmis | Laatija |
| [11](examples/11-pedagoginen-arvio-valmis) | Pedagogical assessment | from the document template* | Valmis | Laatija |
| [12](examples/12-paatosasiakirja-tuki-hyvaksytty), [13](examples/13-paatosasiakirja-tuki-hylatty) | Decision document (e.g. a support decision) | from the document template* | Hyväksytty, Hylätty, Mitätöity | Laatija, Päättäjä |

The process definition numbers for placement, fee and voucher value decisions come from Tampere's configuration (`TampereConfig.archiveMetadataConfigs`). A placement decision uses its application's process definition number.

\* For child documents, the process definition number, retention period and document name come from the document template maintained by the municipality's administrator. The templates used in the examples (12.06.01.11 for plans and 12.06.01.26 for decisions) are illustrative. Only templates with external archiving enabled are exported. Child document agents are derived from the process history: each employee who handled the document is a `Laatija`, and the employee who made the decision is a `Päättäjä`.

Three cases are missing from the examples:

- **Club decision** and **preschool club decision**: the structure is the same as in decisions 01–04 (titles "Kerhopäätös" and "Esiopetuksen kerhon päätös"). Club decisions are not included in scheduled exports but can be exported manually.
- **Annulled decision document**: the structure is the same as in example 12, with "Mitätöity" as the status in the title.
- **Preparatory education**: preparatory education decisions are not supported.

## When documents are exported

The scheduled export selects up to the daily document limit each day. The quota is distributed among document groups in proportion to their counts. Default delays (settings `tampere.archival.schedule.*`):

| Document | Eligible after |
|---|---|
| Daycare and preschool decisions (accepted/rejected, excluding club decisions) | 180 days after the decision was resolved |
| Fee and voucher value decisions (sent/annulled) | 180 days after approval |
| Plans and other child documents (completed) | 180 days after the document template's validity ends |
| Child decision documents | 30 days after the child's last placement ends |

The scheduled job (`archiveEligibleDocuments`) is disabled by default. When enabled, it runs daily at 20:00. The daily limit also defaults to 0, in which case the job selects no documents. An employee can also trigger an export manually for an individual document.

Once a document has been marked as archived, it is not exported again by the scheduled job. If an archived fee or voucher value decision is later annulled, the archive retains the status "Lähetetty" (sent). Comparing a date against a timestamp means a document becomes eligible only on the day after the delay has elapsed.

## Example structure

```
examples/<nn-type>/
  metadata.xml    the "xml" multipart part as sent
  asiakirja.pdf   the "content" multipart part
  request.http    the full HTTP request (credentials replaced with placeholders, PDF replaced with a reference)
responses/        example responses
frends-response.xsd
```

Validate the metadata with:

```sh
xmllint --noout --schema service/src/main/schema/profium-collection-sahke2.xsd docs/archival/tampere-frends-rest/examples/*/metadata.xml
```

## Source code

- Message construction: `service/src/main/kotlin/evaka/instance/tampere/archival/` (`TampereArchivalClient.kt` and the document-type-specific `TampereArchival*.kt` files)
- Status values in titles: `service/src/main/kotlin/evaka/trevaka/archival/DocumentStatus.kt`
- Scheduling and delays: `service/src/main/kotlin/evaka/trevaka/archival/TrevakaArchivalScheduling.kt`
- Settings: `service/src/main/kotlin/evaka/instance/tampere/TampereConfig.kt` and `TampereProperties.kt`
- Integration tests: `service/src/integrationTest/kotlin/evaka/instance/tampere/archival/TampereArchivalClientTest.kt`
