<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# 01-varhaiskasvatuspaatos-hyvaksytty

Varhaiskasvatuspäätös (DecisionType DAYCARE), huoltaja on hyväksynyt (ACCEPTED).

## Tiedostot ja polut

Jokaisesta asiakirjasta syntyy kaksi tiedostoa samaan SFTP-hakemistoon (ei alihakemistoja). Tiedostonimi on eVakan asiakirjan UUID:

| Tiedosto | Polku SFTP-palvelimella |
|---|---|
| `063239c8-c896-11f1-ba5f-1fd1e9714fac.pdf` | `<etuliite>063239c8-c896-11f1-ba5f-1fd1e9714fac.pdf` |
| `063239c8-c896-11f1-ba5f-1fd1e9714fac.xml` | `<etuliite>063239c8-c896-11f1-ba5f-1fd1e9714fac.xml` |
| `f8d2093e-c895-11f1-ba5f-ef8980fd36ce.pdf` | `<etuliite>f8d2093e-c895-11f1-ba5f-ef8980fd36ce.pdf` |
| `f8d2093e-c895-11f1-ba5f-ef8980fd36ce.xml` | `<etuliite>f8d2093e-c895-11f1-ba5f-ef8980fd36ce.xml` |

Kirjoitusjärjestys: ensin PDF, sitten XML (kumpikin omalla SFTP-yhteydellään).

## Metatiedot

| UUID | folder | title | calculationBaseDate | created | initiator | agentit |
|---|---|
| `063239c8-c896-11f1-ba5f-1fd1e9714fac` | 04.01.00.11 | Varhaiskasvatuspäätös;Hyväksytty;Veeti Esimerkki;300523A9853 | 2027-08-01 | 2026-10-15 | Pääkäyttäjä Päivi | Päättäjä Pääkäyttäjä Päivi |
| `f8d2093e-c895-11f1-ba5f-ef8980fd36ce` | 04.01.00.11 | Varhaiskasvatuspäätös;Hyväksytty;Lumi Esimerkki;110222A974T | 2027-08-01 | 2026-10-15 | Pääkäyttäjä Päivi | Päättäjä Pääkäyttäjä Päivi |

## Huomiot

- `063239c8-c896-11f1-ba5f-1fd1e9714fac`: Sama asiakirjatyyppi palveluseteliyksikköön (Palveluseteli A) – palvelusetelipäätös viedään samalla otsikolla ja tehtäväluokalla kuin kunnallinen varhaiskasvatuspäätös.

Henkilöt ja henkilötunnukset ovat keksittyjä. PDF-tiedostot eivät ole mukana repossa.
