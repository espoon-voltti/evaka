<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Arkistointi-integraatiot: tekniset kuvaukset ja esimerkit

Tässä hakemistossa kuvataan eVakan nykyiset arkistointiliitännät: mitä eVaka lähettää, missä muodossa ja milloin. Kuvaukset ja esimerkit on tarkoitettu kunnille, integraatioalustan toimittajille ja arkistojärjestelmien toimittajille, kun ne suunnittelevat uutta liitäntää tai arvioivat, voiko olemassa olevaa käyttää sellaisenaan.

Yleiskuvaus arkistoinnista ja asianhallinnan metatiedoista on eVakan wikissä: [Arkistointi](https://github.com/espoon-voltti/evaka/wiki/Arkistointi) ja [Asianhallintaprosessit ja metatiedot](https://github.com/espoon-voltti/evaka/wiki/Asianhallintaprosessit-ja-metatiedot).

## Toteutukset

| Kunta | Kuvaus | Siirtotapa | Asiakirjat | Kuittaus vastaanotosta |
|---|---|---|---|---|
| Espoo | [espoo-sarma](espoo-sarma/) | HTTP POST (multipart) Särmän X-Archive-rajapintaan | lapsen asiakirjat | kyllä, `status_code` ja arkiston tunniste vastauksessa |
| Tampere | [tampere-frends-rest](tampere-frends-rest/) | REST (multipart) Frends-integraatioalustan kautta | päätökset, maksu- ja arvopäätökset, lapsen asiakirjat | kyllä, arkistotunnus vastauksessa |
| Nokia, Pirkkala, Kangasala | [tweb-sftp](tweb-sftp/) | SFTP (PDF ja metatieto-XML erillisinä tiedostoina) | päätökset, maksu- ja arvopäätökset, lapsen asiakirjat | ei, vain tieto onnistuneesta tiedostokirjoituksesta |

Muissa kunnissa arkistointiliitäntää ei ole käytössä (`ArchivalIntegrationClient.FailingClient`).

Esimerkit on tuotettu ajamalla eVakaa paikallisesti kunnan asetuksilla ja ohjaamalla liitäntä paikalliseen vastaanottimeen, joka tallensi pyynnöt sellaisenaan. Asiakirjat käsiteltiin tavallisten työntekijätoimintojen kautta. Henkilöt ovat kuvitteellisia, ja henkilötunnukset ovat keksittyjä. Esimerkkejä ei päivitetä automaattisesti, joten ne kuvaavat tilannetta kirjoitushetkellä (lokakuu 2026). Ajantasainen totuus on lähdekoodissa, johon kunkin kuvauksen lopussa viitataan.

`request.http`-tiedostoissa rivinvaihdot ovat LF-muodossa. Varsinaisissa HTTP-pyynnöissä otsakkeiden ja multipart-osien rivinvaihdot ovat CRLF.

## Yhteinen malli

Kaikki liitännät toteuttavat saman rajapinnan, `ArchivalIntegrationClient` (`service/src/main/kotlin/evaka/core/document/archival/`). Jokaiselle asiakirjaryhmälle on oma metodinsa:

| Metodi | Asiakirjat | Metatietojen lähde |
|---|---|---|
| `uploadDecisionToArchive` | varhaiskasvatus-, esiopetus- ja kerhopäätökset | hakemuksen asianhallintaprosessi (`case_process`) ja kunnan konfiguraatio |
| `uploadFeeDecisionToArchive` | maksupäätökset | maksupäätöksen asianhallintaprosessi ja kunnan konfiguraatio |
| `uploadVoucherValueDecisionToArchive` | arvopäätökset | arvopäätöksen asianhallintaprosessi ja kunnan konfiguraatio |
| `uploadChildDocumentToArchive` | lomakepohjista tehdyt lapsen asiakirjat | asiakirjapohjan arkistointitiedot (tehtäväluokka, säilytysaika, salassapito) ja asianhallintaprosessi |

eVaka antaa liitännälle asiakirjan tiedot, PDF:n, asiakirjaan liittyvän henkilön, asianhallintaprosessin käsittelyhistorioineen sekä viennin käynnistäjän. Liitäntä muodostaa niistä arkistojärjestelmän vaatiman metatietoviestin ja lähettää sen. Kun metodi palaa ilman virhettä, eVaka merkitsee asiakirjan arkistoiduksi (`archived_at`). Jos metodi heittää virheen, asiakirja jää arkistoimattomaksi.

Viennin voi käynnistää kahdella tavalla:

- **Käsin**: työntekijä painaa asiakirjan kohdalla "Arkistoi".
- **Ajastetusti**: kunnan ajastettu ajo valitsee ehdot täyttävät asiakirjat ja luo niistä taustatyöt. Valintaehdot ja viiveet ovat kuntakohtaisia.

Taustatyöt ajetaan omassa `archival`-altaassaan. Sen rinnakkaisuutta ja tahtia säädetään asetuksilla `evaka.archival_pool_concurrency` (oletus 4) ja `evaka.archival_pool_throttle_interval_seconds` (oletus 0).

## Uuden liitännän suunnittelu

Liitäntä on helpointa toteuttaa, kun arkistojärjestelmä tai integraatioalusta

- ottaa vastaan yhden asiakirjan kerrallaan synkronisessa kutsussa (esim. REST), jossa ovat mukana metatiedot ja PDF
- palauttaa kutsun vastauksessa koneluettavan tiedon siitä, onnistuiko vastaanotto (esim. arkistotunnus tai virhekoodi), jotta eVaka voi merkitä asiakirjan arkistoiduksi ja aineisto voidaan myöhemmin poistaa eVakasta ilman käsityötä (myös esimerkiksi arkiston validaatiossa hylätty asiakirja pitää näkyä virheenä)
- tarjoaa testiympäristön, johon voi lähettää aineistoa sekä kehittäjän koneelta että eVakan testiympäristöstä, ja jossa vastaanotetut asiakirjat ja niiden metatiedot voi tarkastaa käyttöliittymän tai rajapinnan kautta.

Jos arkistojärjestelmä hyväksyy jo olemassa olevan viestimuodon (esimerkiksi Tampereen Profium-muotoisen viestin Frendsin kautta), uusi kunta voi käyttää valmista liitäntää pelkillä asetuksilla ja kuntakohtaisilla tehtäväluokilla.
