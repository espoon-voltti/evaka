<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Maksupäätökset

Maksupäätös lähetetään kuntalaiselle ja hänen kanssavelalliselle puolisolleen. Siinä kerrotaan paljonko varhaiskasvatuksesta tarvitsee maksaa, ja sen perusteella muodostetaan kuukausittain laskut. 

## Generointialgoritmi

### Termimäärittelyt:

- Atominen aikaväli: Useiden aikasarjojen lomittuessa atominen aikaväli on intersektio, jonka sisällä mikään tiedoista ei muutu.

Uudet luonnokset muodostetaan tietylle päämiehelle seuraavan logiikan mukaisesti.

### Vaihe I, luonnosten muodostaminen

1. Haetaan päämiehen entiset maksupäätökset.
2. Haetaan päämieheen liittyvät perhesuhteet (puolisot, lapset, puolisoiden lapset). Jokaiselle atomiselle aikavälille valitaan tosiasialliseksi perheen päämieheksi joko generoinnin kohdehenkilö tai puoliso. Päämieheksi valitaan se kumman alaisuudessa on enemmän lapsia tai tasatilanteessa kummalla on nuorin lapsi. Mikäli kohdehenkilö on aikavälillä perheen päämies, merkitään lapsiksi molempien lapset, muuten ei lapsia.
3. Haetaan kaikkien kohdan 2 perheiden henkilöiden tulotiedot.
4. Haetaan kaikkien kohdan 2 perheiden lasten sijoitus- ja palveluntarvetiedot. Kerhosijoitukset, koululaisten vuorohoitosijoitukset ja tilapäiset sijoitukset jätetään kokonaan pois (eivät vaikuta myöskään sisarusalennukseen).
5. Haetaan kaikkien kohdan 2 perheiden lasten maksumuutokset/huojennokset/korotukset (FeeAlterations).
6. Haetaan maksurajat (FeeThresholds).
7. Haetaan palveluntarpeiden maksutiedot (ServiceNeedOptionFee).
8. Muodostetaan kohdista 2-7 atomiset aikavälit ja kerätään jokaisesta maksuperustetietue (FeeBasis). Maksuperustetietue sisältää muiden tietojen kuten perhekoon lisäksi listan lapsimaksuperusteita (ChildFeeBasis). Niiden muodostamiseksi järjestetään perheen lapset iän mukaan (tasatilanteessa hetun ja id:n), poislukien ne, joille ei ole kohdan 4 sijoitustietoa, ja merkitään lapsen järjestysnumero lapsimaksuperusteeseen. Lisäksi merkitään lapsen muiden maksumuutosten joukkoon ECHA-korotus mikäli aikavälillä joku perheen aikuisista työskentelee tulotietojen mukaan Euroopan kemikaalivirastossa.
9. Muodostetaan jokaisesta edellisen kohdan maksuperustetietueesta varsinainen maksupäätös. Tässä lapsirivit muodostetaan lapsimaksuperusteista seuraavien kohtien 10-14 mukaisesti.
10. Mikäli sijoituksen päätöstyyppinä ei ole maksupäätös (esim. lapsi on palveluseteliyksikössä), ei muodosteta lapsiriviä. Huom: Sisarusalennusvaikutus säilyy koska lapsien järjestysnumerot tallennettiin aiemmin.
11. Lasketaan sisarusalennus lapsen järjestysnumeron perusteella. Alennusmäärät haetaan kohdan 7 tiedoista (Tampere?), tai jos niitä ei ole, niin kohdan 6 tiedoista.
12. Otetaan perusmaksu (baseFee) kohdan 7 tiedoista (Tampere?), tai lasketaan se kohdan 6 tiedoista perhekoon ja tulotietojen perusteella.
13. Lasketaan maksu ennen maksumuutoksia (feeBeforeAlterations) vähentämällä sisarusalennus perusmaksusta ja kertomalla tulos palveluntarpeen kertoimella (feeCoefficient).
14. Lasketaan lopullinen maksu (finalFee) lisäämällä/vähentämällä maksumuutokset.
15. Maksupäätöksen kokonaismaksu muodostuu lapsirivien summasta.

### Vaihe II, luonnosten yhdistely ja karsiminen

Suoritetaan jokaiselle vaiheessa I muodostetulle luonnokselle seuraavat askeleet:

1. Mikäli lähetetyissä aktiivisissa maksupäätöksissä on jo riittävän samanlainen päätös, voidaan tämä luonnos pudottaa pois. Riittävän samanlainen määritellään niin, että lähetetyn päätöksen aikaväli sisältää luonnoksen koko aikavälin ja päätösten muu sisältö on riittävän sama. Tässä on lisäksi huomioitava, että mikäli joku muu edellisen vaiheen luonnoksista tulisi lähetettäessä mitätöimään kyseisen lähetetyn päätöksen tai lyhentämään sen kestoa niin, ettei se enää sisällä koko luonnoksen aikaväliä, ei luonnosta tällöin kuitenkaan saa pudottaa pois.
2. Jos luonnoksessa ei ole yhtään lapsiriviä (tyhjä tai mitätöivä päätös) eikä sillä aikavälillä ole minään päivänä aktiivista päätöstä, voidaan luonnos pudottaa pois.
3. Yhdistetään peräkkäiset sisällöltään riittävän identtiset luonnokset.
4. Suoritetaan kohta 1 vielä uudelleen.
5. Mikäli ohitetut luonnokset tilassa löytyy täsmälleen samalle aikavälille sisällöltään riittävän identtinen luonnos, pudotetaan tämä luonnos pois.

### Vaihe III, muutosten tallennus

1. Poistetaan kaikki vanhat luonnokset.
2. Tallennetaan tilalle uudet luonnokset. Mikäli poistetuissa luonnoksissa oli identtisiä luonnoksia uusiin verrattuina, kopioidaan niistä luontipäivämäärät uusiin.

## Päätösten lähetys

TODO: Mitä tapahtuu? Mitätöinti- ja lyhentämislogiikka?
