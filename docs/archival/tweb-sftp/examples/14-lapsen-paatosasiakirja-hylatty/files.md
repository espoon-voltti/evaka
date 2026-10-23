<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# 14: Rejected child decision document

Child decision document (OTHER_DECISION), decision REJECTED.

## Files and paths

Each document produces two files in the same SFTP directory (no subdirectories). The filename is the document UUID in eVaka:

| File | Path on the SFTP server |
|---|---|
| `4dffce5a-c896-11f1-ba5f-5fb1c86eafdb.pdf` | `<prefix>4dffce5a-c896-11f1-ba5f-5fb1c86eafdb.pdf` |
| `4dffce5a-c896-11f1-ba5f-5fb1c86eafdb.xml` | `<prefix>4dffce5a-c896-11f1-ba5f-5fb1c86eafdb.xml` |

Write order: PDF first, then XML (each using a separate SFTP connection).

## Metadata

| UUID | folder | title | calculationBaseDate | created | initiator | agents |
|---|---|---|---|---|---|---|
| `4dffce5a-c896-11f1-ba5f-5fb1c86eafdb` | 04.01.00.22 | Päätös tuen palveluista varhaiskasvatuksessa;Hylätty;Lumi Esimerkki;110222A974T | 2026-08-01 | 2026-10-15 | Pääkäyttäjä Päivi | Laatija Esimies Eemeli, Päättäjä Esimies Essi |

The people and personal identity codes are made up. The PDF files are in the same directory.
