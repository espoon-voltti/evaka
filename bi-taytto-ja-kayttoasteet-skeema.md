<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Täyttö- ja käyttöasteiden BI-vienti: CSV-tiedostojen skeema

Tämä dokumentti kuvaa CSV-tiedostot, jotka eVaka vie Espoon BI-järjestelmää varten
S3-ämpäriin. Tiedostojen avulla lasketaan täyttö- ja käyttöasteet dokumentin
`bi-taytto-ja-kayttoasteet-vienti.md` mukaisesti.

## 1. Vienti S3:een

eVaka vie tiedostot kerran yössä. Vienti alkaa eVakan yöajojen aikaan, oletuksena klo
00.10 Suomen aikaa. Kukin tiedosto tallennetaan avaimella:

```
<prefix>/<vientipäivä>/<tiedosto>.csv
```

- `<prefix>` on eVakan asetus `espoo.integration.bi_s3.prefix`.
- `<vientipäivä>` on viennin päivämäärä Suomen aikaa muodossa `YYYY-MM-DD`.
- Esimerkki: `bi/2026-09-11/placement.csv`.

Kukin tiedosto viedään omana taustatehtävänään. Tiedostot valmistuvat siksi eri aikaan.
Epäonnistunut tehtävä yritetään uudelleen kerran. Erillistä valmistumismerkkiä ei ole:
lukijan pitää tarkistaa, että kaikki alla luetellut 14 tiedostoa ovat olemassa. Jos
tiedosto puuttuu, sen vienti epäonnistui.

| Tiedosto | Sisältö | eVakan lähdetaulu |
|---|---|---|
| `absence_SNAPSHOT.csv` | Päivämääräikkuna (luku 2) | `absence` |
| `assistance_factor.csv` | Koko taulu | `assistance_factor` |
| `backup_care.csv` | Koko taulu | `backup_care` |
| `care_area.csv` | Koko taulu | `care_area` |
| `daycare.csv` | Koko taulu | `daycare` |
| `daycare_caretaker.csv` | Koko taulu | `daycare_caretaker` |
| `daycare_group.csv` | Koko taulu | `daycare_group` |
| `daycare_group_placement.csv` | Koko taulu | `daycare_group_placement` |
| `person.csv` | Koko taulu | `person` |
| `placement.csv` | Koko taulu | `placement` |
| `service_need.csv` | Koko taulu | `service_need` |
| `service_need_option.csv` | Koko taulu | `service_need_option` |
| `staff_attendance_external.csv` | Koko taulu | `staff_attendance_external` |
| `staff_attendance_realtime_SNAPSHOT.csv` | Päivämääräikkuna (luku 2) | `staff_attendance_realtime` |

"Koko taulu" tarkoittaa, että tiedostossa ovat kaikki taulun rivit viennin hetkellä.
Lukija voi korvata edellisen päivän tiedot kokonaan uudella tiedostolla.

## 2. Päivämääräikkunatiedostot (`_SNAPSHOT`)

Poissaolo- ja henkilökunnan läsnäolotaulut ovat suuria, joten niistä viedään vain
päivämääräikkuna. Olkoon `D` vientipäivä ja `N` eVakan asetus
`espoo.integration.bi_s3.window_days`. Tiedosto sisältää kaikki rivit, joiden päivä on
välillä `D − N` … `D − 1` (molemmat päivät mukana). Vientipäivä `D` ei ole mukana.

| Tiedosto | Päivä, jonka mukaan rivi valitaan |
|---|---|
| `absence_SNAPSHOT.csv` | `date` |
| `staff_attendance_realtime_SNAPSHOT.csv` | `arrived`-aikaleiman päivä Suomen aikaa |

Lukijan pitää käsitellä tiedosto näin:

1. Poista omasta aineistosta kaikki rivit, joiden päivä on välillä `D − N` … `D − 1`.
2. Lisää tiedoston rivit.

Näin eVakassa poistetut rivit poistuvat myös lukijalta. Päivälle, jolla ei ole rivejä,
tiedostossa ei ole rivejä. Siksi välin rajat pitää laskea vientipäivästä ja `N`:stä, ei
tiedoston sisällöstä. Välin ulkopuoliset rivit säilyvät lukijalla ennallaan.

Ensimmäinen vienti tehdään suurella `N`:n arvolla, jotta myös vanhempi historia siirtyy.
Sen jälkeen `N` palautetaan normaaliin arvoonsa.

Henkilökunnan läsnäolo, joka on viennin hetkellä vielä kesken, on tiedostossa tyhjällä
`departed`-arvolla. Seuraavan yön tiedosto korvaa rivin.

## 3. CSV-muoto

- Merkistö UTF-8, kenttien erotin pilkku (`,`), rivin loppu CRLF (`\r\n`).
- Ensimmäinen rivi on otsikkorivi, jossa ovat sarakkeiden nimet.
- Sarakkeet ovat aakkosjärjestyksessä. Tunnista sarake otsikkorivin nimen perusteella,
  älä sijainnin perusteella. Sarakkeita voi tulla lisää myöhemmin.
- Arvo on lainausmerkeissä (`"`), jos siinä on muitakin merkkejä kuin kirjaimia ja
  numeroita. Esimerkiksi UUID-, päivämäärä- ja desimaaliarvot ovat lainausmerkeissä.
  Arvon sisällä oleva lainausmerkki on kahdennettu (`""`). Käytä CSV-jäsennintä, joka
  tukee RFC 4180 -muotoista lainausta.
- Tyhjä kenttä tarkoittaa NULL-arvoa. `text`-sarakkeissa tyhjä kenttä voi olla myös tyhjä
  merkkijono. Näitä kahta ei voi erottaa toisistaan.

Tyyppien esitysmuoto CSV:ssä (lainausmerkit jäsennyksen jälkeen poistettuina):

| Tyyppi | Muoto | Esimerkki |
|---|---|---|
| `uuid` | UUID | `8d3285ed-805a-4a56-9513-884e5416c917` |
| `text` | Merkkijono | `Test Daycare` |
| `integer` | Kokonaisluku | `35` |
| `numeric` | Desimaaliluku, desimaalierotin piste | `1.5`, `7.00` |
| `boolean` | `t` = tosi, `f` = epätosi | `t` |
| `date` | `YYYY-MM-DD` | `2026-09-10` |
| `timestamptz` | Aikaleima aikavyöhykepoikkeamalla. Sekunnin osia on 0–6 numeroa. Poikkeama on Suomen ajan mukainen (`+02` tai `+03`). | `2026-09-10 08:00:00+03`, `2026-09-11 09:29:38.596535+03` |
| `daterange` | `[alku,loppu)`. Alkupäivä on mukana, loppupäivä ei ole mukana (loppu on viimeistä päivää seuraava päivä). Tyhjä loppu tarkoittaa, että väli on voimassa toistaiseksi. | `[2026-08-11,2027-03-12)` = 11.8.2026–11.3.2027 |
| `timerange` | `"(alku,loppu)"` kellonaikoina `HH:MM:SS`. Lainausmerkit ovat osa arvoa. Loppuaika ei ole mukana. Loppuaika `00:00:00` tarkoittaa keskiyötä päivän lopussa. | `"(08:00:00,16:00:00)"` |
| `point` | `(x,y)` | `(24.65,60.2)` |
| enum | Yksi luvun 5 arvoista | `DAYCARE` |
| `X[]` (taulukko) | PostgreSQL-taulukko: `{arvo1,arvo2}`. Tyhjä taulukko on `{}`. Puuttuva alkio on `NULL`. | `{CENTRE,FAMILY}` |

Sarakkeet `created`, `updated` ja `modified` ovat rivin luonti- ja muokkausaikoja
eVakassa. Sarakkeet, joiden nimi päättyy `_by`, sisältävät eVakan käyttäjän tunnisteen.
Käyttäjiä ei viedä.

## 4. Tiedostot ja sarakkeet

Sarake "Tyhjä" kertoo, voiko sarakkeessa olla NULL-arvo.

### 4.1 `absence_SNAPSHOT.csv`

Lapsen poissaolot. Yksi rivi on yhden päivän poissaolo yhdessä poissaolokategoriassa.

| Sarake | Tyyppi | Tyhjä | Huomio |
|---|---|---|---|
| `absence_type` | enum `absence_type` | ei | |
| `category` | enum `absence_category` | ei | |
| `child_id` | `uuid` | ei | `person.id` |
| `date` | `date` | ei | Poissaolopäivä |
| `id` | `uuid` | ei | |
| `modified_at` | `timestamptz` | ei | |
| `modified_by` | `uuid` | ei | |
| `questionnaire_id` | `uuid` | kyllä | Loma-ajan kysely, jonka vastauksesta poissaolo tuli. Kyselyitä ei viedä. |

### 4.2 `assistance_factor.csv`

Lapsen tuen kerroin.

| Sarake | Tyyppi | Tyhjä | Huomio |
|---|---|---|---|
| `capacity_factor` | `numeric` | ei | |
| `child_id` | `uuid` | ei | `person.id` |
| `created` | `timestamptz` | ei | |
| `id` | `uuid` | ei | |
| `modified` | `timestamptz` | ei | |
| `modified_by` | `uuid` | ei | |
| `updated` | `timestamptz` | ei | |
| `valid_during` | `daterange` | ei | Voimassaoloaika |

### 4.3 `backup_care.csv`

Varasijoitukset.

| Sarake | Tyyppi | Tyhjä | Huomio |
|---|---|---|---|
| `child_id` | `uuid` | ei | `person.id` |
| `created` | `timestamptz` | ei | |
| `end_date` | `date` | ei | Viimeinen päivä (mukana) |
| `group_id` | `uuid` | kyllä | `daycare_group.id` |
| `id` | `uuid` | ei | |
| `start_date` | `date` | ei | |
| `unit_id` | `uuid` | ei | `daycare.id` |
| `updated` | `timestamptz` | ei | |

### 4.4 `care_area.csv`

Palvelualueet.

| Sarake | Tyyppi | Tyhjä | Huomio |
|---|---|---|---|
| `area_code` | `integer` | kyllä | |
| `created` | `timestamptz` | ei | |
| `id` | `uuid` | ei | |
| `name` | `text` | ei | |
| `short_name` | `text` | ei | |
| `sub_cost_center` | `text` | kyllä | |
| `updated` | `timestamptz` | ei | |

### 4.5 `daycare.csv`

Yksiköt.

| Sarake | Tyyppi | Tyhjä | Huomio |
|---|---|---|---|
| `additional_info` | `text` | kyllä | |
| `backup_location` | `text` | kyllä | |
| `business_id` | `text` | ei | |
| `capacity` | `integer` | ei | |
| `care_area_id` | `uuid` | ei | `care_area.id` |
| `closing_date` | `date` | kyllä | |
| `club_apply_period` | `daterange` | kyllä | |
| `cost_center` | `text` | kyllä | |
| `created` | `timestamptz` | ei | |
| `daily_preparatory_time` | `timerange` | kyllä | Aina tyhjä |
| `daily_preschool_time` | `timerange` | kyllä | Aina tyhjä |
| `daycare_apply_period` | `daterange` | kyllä | |
| `decision_daycare_name` | `text` | ei | |
| `decision_handler` | `text` | ei | |
| `decision_handler_address` | `text` | ei | |
| `decision_preschool_name` | `text` | ei | |
| `dw_cost_center` | `text` | kyllä | |
| `email` | `text` | kyllä | |
| `enabled_pilot_features` | enum `pilot_feature[]` | ei | |
| `finance_decision_handler` | `uuid` | kyllä | Työntekijän tunniste. Työntekijöitä ei viedä. |
| `ghost_unit` | `boolean` | kyllä | |
| `iban` | `text` | ei | |
| `id` | `uuid` | ei | |
| `invoiced_by_municipality` | `boolean` | ei | |
| `language` | enum `unit_language` | ei | |
| `location` | `point` | kyllä | |
| `mailing_po_box` | `text` | kyllä | |
| `mailing_post_office` | `text` | kyllä | |
| `mailing_postal_code` | `text` | kyllä | |
| `mailing_street_address` | `text` | kyllä | |
| `mealtime_breakfast` | `timerange` | kyllä | |
| `mealtime_evening_snack` | `timerange` | kyllä | |
| `mealtime_lunch` | `timerange` | kyllä | |
| `mealtime_snack` | `timerange` | kyllä | |
| `mealtime_supper` | `timerange` | kyllä | |
| `name` | `text` | ei | |
| `opening_date` | `date` | kyllä | |
| `operation_times` | `timerange[]` | ei | Aina 7 alkiota, maanantaista sunnuntaihin. Alkio on `NULL`, jos yksikkö ei ole auki sinä viikonpäivänä. Esim. `{"(07:00:00,17:00:00)","(07:00:00,17:00:00)","(07:00:00,17:00:00)","(07:00:00,17:00:00)","(07:00:00,17:00:00)",NULL,NULL}` |
| `oph_organizer_oid` | `text` | kyllä | |
| `oph_unit_oid` | `text` | kyllä | |
| `phone` | `text` | kyllä | |
| `post_office` | `text` | ei | |
| `postal_code` | `text` | ei | |
| `preschool_apply_period` | `daterange` | kyllä | |
| `provider_id` | `text` | ei | |
| `provider_type` | enum `unit_provider_type` | ei | |
| `round_the_clock` | `boolean` | ei | `t`, jos yksikkö tarjoaa vuorohoitoa |
| `schedule` | `text` | kyllä | |
| `street_address` | `text` | ei | |
| `type` | enum `care_types[]` | ei | Perhepäivähoito tunnistetaan arvoista `FAMILY` ja `GROUP_FAMILY` |
| `unit_manager_email` | `text` | ei | |
| `unit_manager_name` | `text` | ei | |
| `unit_manager_phone` | `text` | ei | |
| `updated` | `timestamptz` | ei | |
| `upload_children_to_varda` | `boolean` | ei | |
| `upload_to_koski` | `boolean` | ei | |
| `upload_to_varda` | `boolean` | ei | |
| `url` | `text` | kyllä | |

### 4.6 `daycare_caretaker.csv`

Ryhmän henkilökunnan tarve (suunniteltu henkilökunnan määrä).

| Sarake | Tyyppi | Tyhjä | Huomio |
|---|---|---|---|
| `amount` | `numeric` | ei | Henkilökunnan määrä |
| `created` | `timestamptz` | ei | |
| `end_date` | `date` | kyllä | Viimeinen päivä (mukana). Tyhjä = voimassa toistaiseksi. |
| `group_id` | `uuid` | ei | `daycare_group.id` |
| `id` | `uuid` | ei | |
| `start_date` | `date` | ei | |
| `updated` | `timestamptz` | ei | |

### 4.7 `daycare_group.csv`

Yksiköiden ryhmät.

| Sarake | Tyyppi | Tyhjä | Huomio |
|---|---|---|---|
| `daycare_id` | `uuid` | ei | `daycare.id` |
| `end_date` | `date` | kyllä | Viimeinen päivä (mukana). Tyhjä = voimassa toistaiseksi. |
| `id` | `uuid` | ei | |
| `jamix_customer_number` | `integer` | kyllä | |
| `name` | `text` | ei | |
| `start_date` | `date` | ei | |

### 4.8 `daycare_group_placement.csv`

Sijoitusten kohdistukset ryhmiin.

| Sarake | Tyyppi | Tyhjä | Huomio |
|---|---|---|---|
| `created` | `timestamptz` | ei | |
| `daycare_group_id` | `uuid` | ei | `daycare_group.id` |
| `daycare_placement_id` | `uuid` | ei | `placement.id` |
| `end_date` | `date` | ei | Viimeinen päivä (mukana) |
| `id` | `uuid` | ei | |
| `start_date` | `date` | ei | |
| `updated` | `timestamptz` | ei | |

### 4.9 `person.csv`

Kaikki eVakan henkilöt, myös aikuiset. Nimet, henkilötunnukset, sähköpostiosoitteet,
puhelinnumerot ja laskutusosoitteet eivät ole mukana.

| Sarake | Tyyppi | Tyhjä | Huomio |
|---|---|---|---|
| `created` | `timestamptz` | ei | |
| `date_of_birth` | `date` | ei | |
| `date_of_death` | `date` | kyllä | |
| `duplicate_of` | `uuid` | kyllä | `person.id` |
| `force_manual_fee_decisions` | `boolean` | ei | |
| `id` | `uuid` | ei | |
| `language` | `text` | kyllä | |
| `last_login` | `timestamptz` | kyllä | |
| `nationalities` | `text[]` | ei | Maakoodit, esim. `{FIN}` |
| `oph_person_oid` | `text` | kyllä | |
| `post_office` | `text` | ei | |
| `postal_code` | `text` | ei | |
| `residence_code` | `text` | ei | |
| `restricted_details_enabled` | `boolean` | ei | Turvakielto |
| `restricted_details_end_date` | `date` | kyllä | |
| `ssn_adding_disabled` | `boolean` | ei | |
| `street_address` | `text` | ei | |
| `updated` | `timestamptz` | ei | |
| `updated_from_vtj` | `timestamptz` | kyllä | |
| `vtj_dependants_queried` | `timestamptz` | kyllä | |
| `vtj_guardians_queried` | `timestamptz` | kyllä | |

### 4.10 `placement.csv`

Sijoitukset.

| Sarake | Tyyppi | Tyhjä | Huomio |
|---|---|---|---|
| `child_id` | `uuid` | ei | `person.id` |
| `created` | `timestamptz` | ei | |
| `end_date` | `date` | ei | Viimeinen päivä (mukana) |
| `id` | `uuid` | ei | |
| `place_guarantee` | `boolean` | ei | |
| `start_date` | `date` | ei | |
| `terminated_by` | `uuid` | kyllä | |
| `termination_requested_date` | `date` | kyllä | |
| `type` | enum `placement_type` | ei | |
| `unit_id` | `uuid` | ei | `daycare.id` |
| `updated` | `timestamptz` | ei | |

### 4.11 `service_need.csv`

Sijoitusten palveluntarpeet.

| Sarake | Tyyppi | Tyhjä | Huomio |
|---|---|---|---|
| `confirmed_at` | `timestamptz` | kyllä | |
| `confirmed_by` | `uuid` | kyllä | |
| `created` | `timestamptz` | ei | |
| `end_date` | `date` | ei | Viimeinen päivä (mukana) |
| `id` | `uuid` | ei | |
| `option_id` | `uuid` | ei | `service_need_option.id` |
| `placement_id` | `uuid` | ei | `placement.id` |
| `shift_care` | enum `shift_care_type` | ei | |
| `start_date` | `date` | ei | |
| `updated` | `timestamptz` | ei | |

### 4.12 `service_need_option.csv`

Palveluntarvevaihtoehdot ja niiden kertoimet.

| Sarake | Tyyppi | Tyhjä | Huomio |
|---|---|---|---|
| `contract_days_per_month` | `integer` | kyllä | |
| `created` | `timestamptz` | ei | |
| `daycare_hours_per_month` | `integer` | kyllä | |
| `daycare_hours_per_week` | `integer` | ei | |
| `default_option` | `boolean` | ei | Sijoitustyypin oletusvaihtoehto |
| `display_order` | `integer` | kyllä | |
| `fee_coefficient` | `numeric` | ei | |
| `fee_description_fi` | `text` | ei | |
| `fee_description_sv` | `text` | ei | |
| `id` | `uuid` | ei | |
| `name_en` | `text` | ei | |
| `name_fi` | `text` | ei | |
| `name_sv` | `text` | ei | |
| `occupancy_coefficient` | `numeric` | ei | Täyttöasteen kerroin, vähintään 3-vuotias |
| `occupancy_coefficient_under_3y` | `numeric` | ei | Täyttöasteen kerroin, alle 3-vuotias |
| `part_day` | `boolean` | ei | |
| `part_week` | `boolean` | kyllä | Tyhjä = osaviikkoisuus valitaan palveluntarpeelle |
| `realized_occupancy_coefficient` | `numeric` | ei | Käyttöasteen kerroin, vähintään 3-vuotias |
| `realized_occupancy_coefficient_under_3y` | `numeric` | ei | Käyttöasteen kerroin, alle 3-vuotias |
| `show_for_citizen` | `boolean` | ei | |
| `updated` | `timestamptz` | ei | |
| `valid_from` | `date` | ei | |
| `valid_placement_type` | enum `placement_type` | ei | |
| `valid_to` | `date` | kyllä | Viimeinen päivä (mukana). Tyhjä = voimassa toistaiseksi. |
| `voucher_value_description_fi` | `text` | ei | |
| `voucher_value_description_sv` | `text` | ei | |

### 4.13 `staff_attendance_external.csv`

Ulkopuolisen henkilökunnan (esim. sijaisten) läsnäolot ryhmissä.

| Sarake | Tyyppi | Tyhjä | Huomio |
|---|---|---|---|
| `arrived` | `timestamptz` | ei | |
| `created` | `timestamptz` | ei | |
| `departed` | `timestamptz` | kyllä | Tyhjä, jos läsnäolo on kesken |
| `departed_automatically` | `boolean` | ei | |
| `group_id` | `uuid` | ei | `daycare_group.id` |
| `id` | `uuid` | ei | |
| `name` | `text` | ei | Henkilön nimi vapaamuotoisena tekstinä |
| `occupancy_coefficient` | `numeric` | ei | `7.00` kasvatusvastuullinen, `0.00` muu |
| `updated` | `timestamptz` | ei | |

### 4.14 `staff_attendance_realtime_SNAPSHOT.csv`

Työntekijöiden kirjatut läsnäolot ja poissaolot.

| Sarake | Tyyppi | Tyhjä | Huomio |
|---|---|---|---|
| `arrived` | `timestamptz` | ei | |
| `arrived_added_at` | `timestamptz` | kyllä | |
| `arrived_added_by` | `uuid` | kyllä | |
| `arrived_modified_at` | `timestamptz` | kyllä | |
| `arrived_modified_by` | `uuid` | kyllä | |
| `created` | `timestamptz` | ei | |
| `departed` | `timestamptz` | kyllä | Tyhjä, jos läsnäolo on kesken |
| `departed_added_at` | `timestamptz` | kyllä | |
| `departed_added_by` | `uuid` | kyllä | |
| `departed_automatically` | `boolean` | ei | |
| `departed_modified_at` | `timestamptz` | kyllä | |
| `departed_modified_by` | `uuid` | kyllä | |
| `employee_id` | `uuid` | ei | Työntekijän tunniste. Työntekijöitä ei viedä. |
| `group_id` | `uuid` | kyllä | `daycare_group.id` |
| `id` | `uuid` | ei | |
| `occupancy_coefficient` | `numeric` | ei | `7.00` kasvatusvastuullinen, `0.00` muu |
| `type` | enum `staff_attendance_type` | ei | |
| `updated` | `timestamptz` | ei | |

## 5. Enum-arvot

| Enum | Arvot |
|---|---|
| `absence_category` | `BILLABLE`, `NONBILLABLE` |
| `absence_type` | `OTHER_ABSENCE`, `SICKLEAVE`, `UNKNOWN_ABSENCE`, `PLANNED_ABSENCE`, `PARENTLEAVE`, `FORCE_MAJEURE`, `FREE_ABSENCE`, `UNAUTHORIZED_ABSENCE` |
| `care_types` | `CENTRE`, `FAMILY`, `GROUP_FAMILY`, `CLUB`, `PRESCHOOL`, `PREPARATORY_EDUCATION` |
| `pilot_feature` | `MESSAGING`, `MOBILE`, `RESERVATIONS`, `VASU_AND_PEDADOC`, `MOBILE_MESSAGING`, `PLACEMENT_TERMINATION`, `REALTIME_STAFF_ATTENDANCE`, `PUSH_NOTIFICATIONS`, `SERVICE_APPLICATIONS`, `STAFF_ATTENDANCE_INTEGRATION`, `OTHER_DECISION`, `CITIZEN_BASIC_DOCUMENT` |
| `placement_type` | `DAYCARE`, `PRESCHOOL`, `PRESCHOOL_DAYCARE`, `PRESCHOOL_DAYCARE_ONLY`, `PRESCHOOL_CLUB`, `DAYCARE_PART_TIME`, `PREPARATORY`, `PREPARATORY_DAYCARE`, `PREPARATORY_DAYCARE_ONLY`, `CLUB`, `TEMPORARY_DAYCARE`, `TEMPORARY_DAYCARE_PART_DAY`, `DAYCARE_FIVE_YEAR_OLDS`, `DAYCARE_PART_TIME_FIVE_YEAR_OLDS`, `SCHOOL_SHIFT_CARE` |
| `shift_care_type` | `NONE`, `FULL`, `INTERMITTENT` |
| `staff_attendance_type` | `PRESENT`, `OTHER_WORK`, `TRAINING`, `OVERTIME`, `JUSTIFIED_CHANGE`, `SICKNESS`, `CHILD_SICKNESS` |
| `unit_language` | `fi`, `sv`, `en` |
| `unit_provider_type` | `MUNICIPAL`, `PURCHASED`, `PRIVATE`, `MUNICIPAL_SCHOOL`, `PRIVATE_SERVICE_VOUCHER`, `EXTERNAL_PURCHASED` |

eVakaan voidaan myöhemmin lisätä uusia enum-arvoja.
