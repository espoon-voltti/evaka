<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# 05-maksupaatos-lahetetty

Maksupäätös tilassa SENT.

## Tiedostot ja polut

Jokaisesta asiakirjasta syntyy kaksi tiedostoa samaan SFTP-hakemistoon (ei alihakemistoja). Tiedostonimi on eVakan asiakirjan UUID:

| Tiedosto | Polku SFTP-palvelimella |
|---|---|
| `ec95a645-d8b5-4027-8536-746281a9dd4e.pdf` | `<etuliite>ec95a645-d8b5-4027-8536-746281a9dd4e.pdf` |
| `ec95a645-d8b5-4027-8536-746281a9dd4e.xml` | `<etuliite>ec95a645-d8b5-4027-8536-746281a9dd4e.xml` |
| `ffdcc13f-1f4f-4692-8e76-3219e26f00b8.pdf` | `<etuliite>ffdcc13f-1f4f-4692-8e76-3219e26f00b8.pdf` |
| `ffdcc13f-1f4f-4692-8e76-3219e26f00b8.xml` | `<etuliite>ffdcc13f-1f4f-4692-8e76-3219e26f00b8.xml` |

Kirjoitusjärjestys: ensin PDF, sitten XML (kumpikin omalla SFTP-yhteydellään).

## Metatiedot

| UUID | folder | title | calculationBaseDate | created | initiator | agentit |
|---|---|
| `ec95a645-d8b5-4027-8536-746281a9dd4e` | 04.01.00.12 | Maksupäätös;Lähetetty;Sanna Esimerkki;210489-952E | 2026-10-15 | 2026-10-15 | eVaka | Päättäjä Laskuttaja Lasse |
| `ffdcc13f-1f4f-4692-8e76-3219e26f00b8` | 04.01.00.12 | Maksupäätös;Lähetetty;Sanna Esimerkki;210489-952E | 2027-08-01 | 2026-10-15 | Pääkäyttäjä Päivi | Päättäjä Laskuttaja Lasse |

## Huomiot

- `ec95a645-d8b5-4027-8536-746281a9dd4e`: Ajastetun PlanDocumentArchival-ajon viemä: initiator on järjestelmäkäyttäjä (00000000-…, "eVaka"). Tuotannossa valtaosa viennistä kulkee näin; käsin (ADMIN-käyttäjän arkistointipainike) käynnistetyissä vienneissä initiatorina on kyseinen työntekijä.

Henkilöt ja henkilötunnukset ovat keksittyjä. PDF-tiedostot eivät ole mukana repossa.
