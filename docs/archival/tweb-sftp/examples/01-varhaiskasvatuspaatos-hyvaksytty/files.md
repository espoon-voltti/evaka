<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# 01: Accepted daycare decision

Daycare decision (DecisionType DAYCARE), accepted by the guardian (ACCEPTED).

## Files and paths

Each document produces two files in the same SFTP directory (no subdirectories). The filename is the document UUID in eVaka:

| File | Path on the SFTP server |
|---|---|
| `063239c8-c896-11f1-ba5f-1fd1e9714fac.pdf` | `<prefix>063239c8-c896-11f1-ba5f-1fd1e9714fac.pdf` |
| `063239c8-c896-11f1-ba5f-1fd1e9714fac.xml` | `<prefix>063239c8-c896-11f1-ba5f-1fd1e9714fac.xml` |
| `f8d2093e-c895-11f1-ba5f-ef8980fd36ce.pdf` | `<prefix>f8d2093e-c895-11f1-ba5f-ef8980fd36ce.pdf` |
| `f8d2093e-c895-11f1-ba5f-ef8980fd36ce.xml` | `<prefix>f8d2093e-c895-11f1-ba5f-ef8980fd36ce.xml` |

Write order: PDF first, then XML (each using a separate SFTP connection).

## Metadata

| UUID | folder | title | calculationBaseDate | created | initiator | agents |
|---|---|---|---|---|---|---|
| `063239c8-c896-11f1-ba5f-1fd1e9714fac` | 04.01.00.11 | Varhaiskasvatuspäätös;Hyväksytty;Veeti Esimerkki;300523A9853 | 2027-08-01 | 2026-10-15 | Pääkäyttäjä Päivi | Päättäjä Pääkäyttäjä Päivi |
| `f8d2093e-c895-11f1-ba5f-ef8980fd36ce` | 04.01.00.11 | Varhaiskasvatuspäätös;Hyväksytty;Lumi Esimerkki;110222A974T | 2027-08-01 | 2026-10-15 | Pääkäyttäjä Päivi | Päättäjä Pääkäyttäjä Päivi |

## Notes

- `063239c8-c896-11f1-ba5f-1fd1e9714fac`: The same document type for a service voucher unit (Palveluseteli A): the placement decision for the service voucher unit is exported with the same title and process definition number as a municipal daycare decision.

The people and personal identity codes are made up. The PDF files are in the same directory.
