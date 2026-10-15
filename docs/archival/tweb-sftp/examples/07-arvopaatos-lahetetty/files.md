<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# 07-arvopaatos-lahetetty

Palvelusetelin arvopäätös tilassa SENT.

## Tiedostot ja polut

Jokaisesta asiakirjasta syntyy kaksi tiedostoa samaan SFTP-hakemistoon (ei alihakemistoja). Tiedostonimi on eVakan asiakirjan UUID:

| Tiedosto | Polku SFTP-palvelimella |
|---|---|
| `6a0cf122-ad19-44c4-af3e-afa376c4b5ce.pdf` | `<etuliite>6a0cf122-ad19-44c4-af3e-afa376c4b5ce.pdf` |
| `6a0cf122-ad19-44c4-af3e-afa376c4b5ce.xml` | `<etuliite>6a0cf122-ad19-44c4-af3e-afa376c4b5ce.xml` |
| `83383876-260f-4a4a-bd74-0512a4889c9a.pdf` | `<etuliite>83383876-260f-4a4a-bd74-0512a4889c9a.pdf` |
| `83383876-260f-4a4a-bd74-0512a4889c9a.xml` | `<etuliite>83383876-260f-4a4a-bd74-0512a4889c9a.xml` |

Kirjoitusjärjestys: ensin PDF, sitten XML (kumpikin omalla SFTP-yhteydellään).

## Metatiedot

| UUID | folder | title | calculationBaseDate | created | initiator | agentit |
|---|---|
| `6a0cf122-ad19-44c4-af3e-afa376c4b5ce` | 04.01.02.59 | Arvopäätös;Lähetetty;Sanna Esimerkki;210489-952E | 2026-10-15 | 2026-10-15 | eVaka | Päättäjä Laskuttaja Lasse |
| `83383876-260f-4a4a-bd74-0512a4889c9a` | 04.01.02.59 | Arvopäätös;Lähetetty;Sanna Esimerkki;210489-952E | 2027-08-01 | 2026-10-15 | Pääkäyttäjä Päivi | Päättäjä Laskuttaja Lasse |

## Huomiot

- `6a0cf122-ad19-44c4-af3e-afa376c4b5ce`: Ajastetun ajon viemä (järjestelmäkäyttäjä initiatorina).

Henkilöt ja henkilötunnukset ovat keksittyjä. PDF-tiedostot ovat samassa hakemistossa.
