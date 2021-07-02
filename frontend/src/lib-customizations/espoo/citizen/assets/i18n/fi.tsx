// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { P } from 'lib-components/typography'
import ExternalLink from 'lib-components/atoms/ExternalLink'

export default {
  common: {
    title: 'Varhaiskasvatus',
    cancel: 'Peruuta',
    return: 'Palaa',
    ok: 'Ok',
    add: 'Lisää',
    unit: {
      providerTypes: {
        MUNICIPAL: 'Kunnallinen',
        PURCHASED: 'Ostopalvelu',
        PRIVATE: 'Yksityinen',
        MUNICIPAL_SCHOOL: 'Kunnallinen',
        PRIVATE_SERVICE_VOUCHER: 'Palveluseteli'
      },
      careTypes: {
        CLUB: 'Kerho',
        FAMILY: 'Perhepäivähoito',
        CENTRE: 'Päiväkoti',
        GROUP_FAMILY: 'Ryhmäperhepäivähoito',
        PRESCHOOL: 'Esiopetus',
        PREPARATORY_EDUCATION: 'Valmistava opetus'
      },
      languages: {
        fi: 'suomenkielinen',
        sv: 'ruotsinkielinen'
      },
      languagesShort: {
        fi: 'suomi',
        sv: 'ruotsi'
      }
    },
    openExpandingInfo: 'Avaa lisätietokenttä',
    errors: {
      genericGetError: 'Tietojen hakeminen ei onnistunut'
    }
  },
  header: {
    nav: {
      map: 'Kartta',
      applications: 'Hakemukset',
      decisions: 'Päätökset',
      messages: 'Viestit'
    },
    lang: {
      fi: 'Suomeksi',
      sv: 'På svenska',
      en: 'In English'
    },
    login: 'Tunnistaudu palveluun',
    logout: 'Kirjaudu ulos'
  },
  footer: {
    cityLabel: '© Espoon kaupunki',
    privacyPolicy: 'Tietosuojaselosteet',
    privacyPolicyLink:
      'https://www.espoo.fi/fi-FI/Asioi_verkossa/Tietosuoja/Tietosuojaselosteet',
    sendFeedback: 'Lähetä palautetta',
    sendFeedbackLink:
      'https://easiointi.espoo.fi/eFeedback/fi/Feedback/20-S%C3%A4hk%C3%B6iset%20asiointipalvelut'
  },
  map: {
    title: 'Yksiköt kartalla',
    mainInfo:
      'Tässä näkymässä voit hakea kartalta Espoon varhaiskasvatus-, esiopetus- ja kerhopaikkoja.',
    privateUnitInfo: function PrivateUnitInfo() {
      return (
        <span>
          Tietoa yksityisistä päiväkodeista löydät{' '}
          <ExternalLink
            text="täältä."
            href="https://www.espoo.fi/fi-FI/Kasvatus_ja_opetus/Varhaiskasvatus/Yksityinen_varhaiskasvatus"
            newTab
          />
        </span>
      )
    },
    searchLabel: 'Hae osoitteella tai yksikön nimellä',
    searchPlaceholder: 'Esim. Purolan päiväkoti',
    address: 'Osoite',
    noResults: 'Ei hakutuloksia',
    distanceWalking: 'Etäisyys valitusta osoitteesta kävellen',
    careType: 'Toimintamuoto',
    careTypePlural: 'Toimintamuodot',
    careTypes: {
      CLUB: 'Kerho',
      DAYCARE: 'Varhaiskasvatus',
      PRESCHOOL: 'Esiopetus'
    },
    language: 'Yksikön kieli',
    providerType: 'Palveluntarjoaja',
    providerTypes: {
      MUNICIPAL: 'kunnalliset',
      PURCHASED: 'ostopalvelu',
      PRIVATE: 'yksityiset',
      PRIVATE_SERVICE_VOUCHER: 'palveluseteli'
    },
    homepage: 'Kotisivu',
    unitHomepage: 'Yksikön kotisivu',
    route: 'Katso reitti yksiköön',
    routePlanner: 'Reittiopas',
    newTab: '(Avautuu uuteen välilehteen)',
    shiftCareTitle: 'Ilta- ja vuorohoito',
    shiftCareLabel: 'Näytä vain ilta- ja vuorohoitoyksiköt',
    shiftCareYes: 'Yksikkö tarjoaa ilta- ja/tai vuorohoitoa',
    shiftCareNo: 'Yksikkö ei tarjoa ilta- ja/tai vuorohoitoa',
    showMoreFilters: 'Näytä lisää suodattimia',
    showLessFilters: 'Näytä vähemmän suodattimia',
    nearestUnits: 'Lähimmät yksiköt',
    moreUnits: 'Lisää yksiköitä',
    showMore: 'Näytä lisää hakutuloksia',
    mobileTabs: {
      map: 'Kartta',
      list: 'Lista yksiköistä'
    },
    serviceVoucherLink:
      'https://www.espoo.fi/fi-FI/Kasvatus_ja_opetus/Varhaiskasvatus/Hakeminen_varhaiskasvatukseen/Palveluseteli/Tietoa_perheille',
    noApplying: 'Ei hakua eVakan kautta, ota yhteys yksikköön',
    backToSearch: 'Takaisin hakuun'
  },
  messages: {
    inboxTitle: 'Saapuneet viestit',
    noMessages: 'Ei saapuneita viestejä',
    recipients: 'Vastaanottajat',
    send: 'Lähetä',
    sending: 'Lähetetään',
    messagePlaceholder:
      'Viestin sisältö... Huom! Älä kirjoita tähän arkaluontoisia asioita.',
    types: {
      MESSAGE: 'Viesti',
      BULLETIN: 'Tiedote'
    },
    messageEditor: {
      newMessage: 'Uusi viesti',
      receivers: 'Vastaanottajat',
      title: 'Otsikko',
      message: 'Viesti',
      deleteDraft: 'Hylkää luonnos',
      send: 'Lähetä',
      search: 'Haku',
      noResults: 'Ei tuloksia',
      messageSendError: 'Viestin lähetys epäonnistui'
    }
  },
  applications: {
    title: 'Hakemukset',
    deleteDraftTitle: 'Haluatko poistaa hakemuksen?',
    deleteDraftText:
      'Haluatko varmasti poistaa hakemusluonnoksen? Kaikki poistettavan hakemuksen tiedot menetetään.',
    deleteDraftOk: 'Poista hakemus',
    deleteDraftCancel: 'Palaa takaisin',
    deleteSentTitle: 'Haluatko peruuttaa hakemuksen?',
    deleteSentText:
      'Haluatko varmasti peruuttaa hakemuksen? Jos peruutat hakemuksen, kaikki tiedot menetetään.',
    deleteSentOk: 'Peruuta hakemus',
    deleteSentCancel: 'Palaa takaisin',
    deleteUnprocessedApplicationError: 'Hakemuksen poisto epäonnistui',
    creation: {
      title: 'Valitse hakemustyyppi',
      daycareLabel: 'Varhaiskasvatus- ja palvelusetelihakemus',
      daycareInfo:
        'Varhaiskasvatushakemuksella haetaan kunnallista varhaiskasvatuspaikkaa päiväkotiin, perhepäivähoitoon tai ryhmäperhepäiväkotiin. Samalla hakemuksella voi hakea myös varhaiskasvatuksen palveluseteliä, valitsemalla Hakutoiveet-kohtaan palveluseteliyksikkö, johon halutaan hakea.',
      preschoolLabel:
        'Ilmoittautuminen esiopetukseen ja / tai valmistavaan opetukseen',
      preschoolInfo:
        'Maksutonta esiopetusta on neljä tuntia päivässä. Tämän lisäksi lapselle voidaan hakea maksullista liittyvää varhaiskasvatusta, jota tarjotaan esiopetuspaikoissa aamulla ennen esiopetuksen alkua ja iltapäivisin esiopetuksen jälkeen. Liittyvään varhaiskasvatukseen voi hakea myös palveluseteliä, valitsemalla Hakutoiveet -kohtaan palveluseteliyksikön, johon halutaan hakea. Hakemuksen liittyvään varhaiskasvatukseen voi tehdä esiopetukseen ilmoittautumisen yhteydessä tai erillisenä hakemuksena opetuksen jo alettua. Samalla hakemuksella voit hakea myös maksuttomaan valmistavaan opetukseen sekä valmistavaan opetukseen liittyvään varhaiskasvatukseen.',
      preschoolDaycareInfo:
        'Liittyvän varhaiskasvatuksen hakeminen lapsille, jotka ilmoitetaan / on ilmoitettu esiopetukseen tai valmistavaan opetukseen',
      clubLabel: 'Kerhohakemus',
      clubInfo: 'Kerhohakemuksella haetaan kunnallisiin kerhoihin.',
      duplicateWarning:
        'Lapsella on jo samantyyppinen, keskeneräinen hakemus. Palaa Hakemukset-näkymään ja muokkaa olemassa olevaa hakemusta tai ota yhteyttä palveluohjaukseen.',
      transferApplicationInfo: {
        DAYCARE:
          'Lapsella on jo paikka Espoon varhaiskasvatuksessa. Tällä hakemuksella voit hakea siirtoa toiseen varhaiskasvatusta tarjoavaan yksikköön Espoossa.',
        PRESCHOOL:
          'Lapsella on jo esiopetuspaikka. Tällä hakemuksella voit hakea esiopetukseen liittyvää varhaiskasvatusta tai siirtoa toiseen esiopetusta tarjoavaan yksikköön.'
      },
      create: 'Tee hakemus',
      daycare4monthWarning: 'Hakemuksen käsittelyaika on 4 kuukautta.',
      applicationInfo: function ApplicationInfoText() {
        return (
          <P>
            Hakemukseen voi tehdä muutoksia niin kauan kuin hakemusta ei ole
            otettu käsittelyyn. Tämän jälkeen muutokset hakemukseen tehdään
            ottamalla yhteyttä varhaiskasvatuksen palveluohjaukseen (puh. 09 816
            31000). Voit perua jo tehdyn hakemuksen ilmoittamalla siitä
            sähköpostilla varhaiskasvatuksen palveluohjaukseen{' '}
            <a href="mailto:varhaiskasvatuksen.palveluohjaus@espoo.fi">
              varhaiskasvatuksen.palveluohjaus@espoo.fi
            </a>
            .
          </P>
        )
      }
    },
    editor: {
      heading: {
        title: {
          DAYCARE: 'Varhaiskasvatus- ja palvelusetelihakemus',
          PRESCHOOL: 'Ilmoittautuminen esiopetukseen',
          CLUB: 'Kerhohakemus'
        },
        info: {
          DAYCARE: function EditorHeadingInfoDaycareText() {
            return (
              <>
                <P>
                  Varhaiskasvatusta voi hakea ympäri vuoden. Hakemus on
                  jätettävä viimeistään neljä kuukautta ennen kuin tarvitsette
                  paikan. Mikäli tarvitsette varhaiskasvatusta kiireellisesti
                  työn tai opiskelujen vuoksi, tulee paikkaa hakea viimeistään
                  kaksi viikkoa ennen.
                </P>
                <P>
                  Saatte kirjallisen päätöksen varhaiskasvatuspaikasta{' '}
                  <a
                    href="https://www.suomi.fi/viestit"
                    target="_blank"
                    rel="noreferrer"
                  >
                    Suomi.fi-viestit
                  </a>{' '}
                  -palveluun tai postitse, mikäli et ole ottanut Suomi.fi
                  -palvelua käyttöön.
                </P>
                <P fitted={true}>* Tähdellä merkityt tiedot ovat pakollisia</P>
              </>
            )
          },
          PRESCHOOL: function EditorHeadingInfoPreschoolText() {
            return (
              <>
                <P>
                  Esiopetukseen osallistutaan vuosi ennen oppivelvollisuuden
                  alkamista. Esiopetus on maksutonta. Lukuvuoden 2021–2022
                  esiopetukseen ilmoittaudutaan 8.–20.1.2021. Suomen ja ruotsin
                  kielinen esiopetus alkaa 11.8.2021.
                </P>
                <P>
                  Päätökset tulevat{' '}
                  <a
                    href="https://www.suomi.fi/viestit"
                    target="_blank"
                    rel="noreferrer"
                  >
                    Suomi.fi-viestit
                  </a>{' '}
                  -palveluun tai postitse, mikäli et ole ottanut Suomi.fi
                  -palvelua käyttöön.
                </P>
                <P fitted={true}>* Tähdellä merkityt tiedot ovat pakollisia</P>
              </>
            )
          },
          CLUB: function EditorHeadingInfoClubText() {
            return (
              <>
                <P>
                  Hakuaika syksyllä käynnistyviin kerhoihin on maaliskuussa. Jos
                  lapsenne saa kerhopaikan, saatte päätöksen siitä
                  huhti-toukokuun aikana. Päätös tehdään yhden toimintakauden
                  ajaksi (elokuusta toukokuun loppuun). Päätös kerhopaikasta
                  tulee Suomi.fi-palveluun tai postitse, mikäli ette ole ottanut
                  palvelua käyttöön.
                </P>
                <P>
                  Kerhohakemuksen voi jättää myös hakuajan ulkopuolella ja sen
                  jälkeen, kun kerhojen toimintakausi on jo alkanut. Hakuaikana
                  saapuneet hakemukset käsitellään kuitenkin ensin, ja hakuajan
                  ulkopuolella tulleet hakemukset käsitellään
                  saapumisjärjestyksessä. Kerhohakemus kohdistuu yhdelle
                  kerhokaudelle. Kauden päättyessä hakemus poistetaan
                  järjestelmästä.
                </P>
                <P>
                  Kerhotoiminta on maksutonta, eikä siihen osallistuminen
                  vaikuta Kelan maksamaan kotihoidontukeen. Jos lapselle sen
                  sijaan on myönnetty varhaiskasvatuspaikka tai yksityisen
                  hoidon tuki, ei hänelle voida myöntää kerhopaikkaa.
                </P>
                <P fitted={true}>* Tähdellä merkityt tiedot ovat pakollisia</P>
              </>
            )
          }
        },
        errors: (count: number) =>
          count === 1 ? '1 virhe' : `${count} virhettä`,
        hasErrors: 'Ole hyvä ja tarkista seuraavat tiedot hakemuksestasi:'
      },
      actions: {
        verify: 'Tarkista hakemus',
        hasVerified: 'Olen tarkistanut hakemuksen tiedot oikeiksi',
        returnToEdit: 'Palaa muokkaamaan hakemusta',
        returnToEditBtn: 'Takaisin hakemusnäkymään',
        cancel: 'Peruuta',
        send: 'Lähetä hakemus',
        update: 'Tallenna muutokset',
        sendError: 'Hakemuksen lähettäminen epäonnistui',
        saveDraft: 'Tallenna keskeneräisenä',
        updateError: 'Muutosten tallentaminen epäonnistui'
      },
      verification: {
        title: {
          DAYCARE: 'Varhaiskasvatushakemuksen tarkistaminen',
          PRESCHOOL: 'Esiopetushakemuksen tarkistaminen',
          CLUB: 'Kerhohakemuksen tarkistaminen'
        },
        notYetSent: function NotYetSentText() {
          return (
            <P>
              <strong>Hakemusta ei ole vielä lähetetty.</strong> Tarkista
              antamasi tiedot ja lähetä sivun lopussa olevalla Lähetä
              hakemus-painikkeella.
            </P>
          )
        },
        notYetSaved: function NotYetSavedText() {
          return (
            <P>
              <strong>Muutoksia ei ole vielä tallennettu.</strong> Tarkista
              antamasi tiedot ja tallenna sivun lopussa olevalla Tallenna
              muutokset -painikkeella.
            </P>
          )
        },
        no: 'Ei',
        basics: {
          created: 'Hakemus luotu',
          modified: 'Hakemusta muokattu viimeksi'
        },
        attachmentBox: {
          nb: 'Huom!',
          headline:
            'Jos lisäät liitteet seuraaviin kohtiin sähköisesti, hakemuksesi käsitellään nopeammin, sillä käsittelyaika alkaa liitteiden saapumisesta.',
          urgency: 'Hakemus on kiireellinen',
          shiftCare: 'Ilta- ja vuorohoito',
          goBackLinkText: 'Palaa takaisin hakemusnäkymään',
          goBackRestText: 'lisätäksesi liitteet hakemukseen.'
        },
        serviceNeed: {
          title: 'Palveluntarve',
          wasOnDaycare: 'Varhaiskasvatuksessa ennen kerhoa',
          wasOnDaycareYes:
            'Lapsi, jolle haetaan kerhopaikkaa, on varhaiskasvatuksessa ennen kerhon toivottua aloituspäivää.',
          wasOnClubCare: 'Kerhossa edellisenä toimintakautena',
          wasOnClubCareYes:
            'Lapsi on ollut kerhossa edellisen toimintakauden aikana.',
          connectedDaycare: {
            title: 'Esiopetukseen liittyvän varhaiskasvatuksen tarve',
            label: 'Liittyvä varhaiskasvatus',
            withConnectedDaycare:
              'Haen myös esiopetukseen liittyvää varhaiskasvatusta.',
            withoutConnectedDaycare: 'Ei'
          },
          attachments: {
            label: 'Tarvittavat liitteet',
            withoutAttachments: 'Ei liitetty, lähetetään postilla'
          },
          startDate: {
            title: {
              DAYCARE: 'Varhaiskasvatuksen aloitus',
              PRESCHOOL: 'Varhaiskasvatuksen aloitus',
              CLUB: 'Kerhon aloitus'
            },
            preferredStartDate: 'Toivottu aloituspäivä',
            urgency: 'Hakemus on kiireellinen',
            withUrgency: 'Kyllä',
            withoutUrgency: 'Ei'
          },
          dailyTime: {
            title: 'Päivittäinen varhaiskasvatusaika',
            partTime: 'Osa- tai kokopäiväinen',
            withPartTime: 'Osapäiväinen',
            withoutPartTime: 'Kokopäiväinen',
            dailyTime: 'Varhaiskasvatuksen alkamis- ja päättymisaika',
            shiftCare: 'Ilta- ja vuorohoito',
            withShiftCare: 'Tarvitaan ilta- tai vuorohoitoa',
            withoutShiftCare: 'Ei tarvita ilta- tai vuorohoitoa'
          },
          assistanceNeed: {
            title: 'Tuen tarve',
            assistanceNeed: 'Lapsella on tuen tarve',
            withAssistanceNeed: 'Lapsella on tuen tarve',
            withoutAssistanceNeed: 'Lapsella ei ole tuen tarvetta',
            description: 'Tuen tarpeen kuvaus'
          },
          preparatoryEducation: {
            label: 'Perusopetukseen valmistava opetus',
            withPreparatory:
              'Lapsi tarvitsee tukea suomen kielen oppimisessa. Haen myös perusopetukseen valmistavaan opetukseen.',
            withoutPreparatory: 'Ei'
          }
        },
        unitPreference: {
          title: 'Hakutoive',
          siblingBasis: {
            title: 'Haku sisarperusteella',
            siblingBasisLabel: 'Sisarusperuste',
            siblingBasisYes:
              'Haen ensisijaisesti samaan paikkaan, jossa lapsen sisarus on jo varhaiskasvatuksessa',
            name: 'Sisaruksen nimi',
            ssn: 'Sisaruksen henkilötunnus'
          },
          units: {
            title: 'Hakutoiveet',
            label: 'Valitsemasi hakutoiveet'
          }
        },
        contactInfo: {
          title: 'Henkilötiedot',
          child: {
            title: 'Lapsen tiedot',
            name: 'Lapsen nimi',
            ssn: 'Lapsen henkilötunnus',
            streetAddress: 'Kotiosoite',
            isAddressChanging: 'Osoite muuttunut / muuttumassa',
            hasFutureAddress:
              'Väestörekisterissä oleva osoite on muuttunut/muuttumassa ',
            addressChangesAt: 'Muuttopäivämäärä',
            newAddress: 'Uusi osoite'
          },
          guardian: {
            title: 'Huoltajan tiedot',
            name: 'Huoltajan nimi',
            ssn: 'Huoltajan henkilötunnus',
            streetAddress: 'Kotiosoite',
            tel: 'Puhelinnumero',
            email: 'Sähköpostiosoite',
            isAddressChanging: 'Osoite muuttunut / muuttumassa',
            hasFutureAddress: 'Osoite muuttunut / muuttumassa',
            addressChangesAt: 'Muuttopäivämäärä',
            newAddress: 'Uusi osoite'
          },
          secondGuardian: {
            title: 'Toisen huoltajan tiedot',
            email: 'Sähköposti',
            tel: 'Puhelin',
            info:
              'Toisen huoltajan tiedot haetaan automaattisesti väestötietojärjestelmästä.',
            agreed:
              'Olemme yhdessä sopineet lapsen esiopetuksen hakemisesta lomakkeen tietojen mukaisesti.',
            notAgreed: 'Emme ole voineet sopia hakemuksen tekemisestä yhdessä',
            rightToGetNotified:
              'Toisella huoltajalla on vain tiedonsaantioikeus.',
            noAgreementStatus: 'Ei tiedossa'
          },
          fridgePartner: {
            title:
              'Samassa taloudessa asuva avio- tai avopuoliso (ei huoltaja)',
            fridgePartner:
              'Samassa taloudessa asuu avio- tai avopuoliso (ei huoltaja)',
            name: 'Henkilön nimi',
            ssn: 'Henkilön henkilötunnus'
          },
          fridgeChildren: {
            title: 'Samassa taloudessa asuvat alle 18-vuotiaat lapset',
            name: 'Lapsen nimi',
            ssn: 'Henkilön henkilötunnus',
            noOtherChildren: 'Ei muita lapsia'
          }
        },
        additionalDetails: {
          title: 'Muut lisätiedot',
          otherInfoLabel: 'Hakuun liittyvät lisätiedot',
          dietLabel: 'Erityisruokavalio',
          allergiesLabel: 'Allergiat'
        }
      },
      serviceNeed: {
        title: 'Palveluntarve',
        startDate: {
          header: {
            DAYCARE: 'Varhaiskasvatuksen aloitus',
            PRESCHOOL: 'Esiopetuksen alkaminen',
            CLUB: 'Kerhon alkaminen'
          },
          info: {
            DAYCARE: [],
            PRESCHOOL: [
              'Suomen- ja ruotsinkielinen esiopetus alkaa 11.8.2021. Jos tarvitsette varhaiskasvatusta 1.8.2021 lähtien ennen esiopetuksen alkua, voitte hakea sitä tällä hakemuksella valitsemalla ”Haen myös esiopetukseen liittyvää varhaiskasvatusta”.'
            ],
            CLUB: [
              'Kerhot noudattavat esiopetuksen työ- ja loma-aikoja. Kerhon toimintakausi on elokuusta toukokuun loppuun, ja kullekin toimintakaudelle haetaan erikseen. Eri kerhot kokoontuvat eri viikonpäivinä.'
            ]
          },
          clubTerm: 'Kerhon toimintakausi',
          clubTerms: 'Kerhon toimintakaudet',
          label: {
            DAYCARE: 'Toivottu aloituspäivä',
            PRESCHOOL: 'Toivottu aloituspäivä',
            CLUB: 'Kerhon toivottu aloituspäivä'
          },
          noteOnDelay: 'Hakemuksen käsittelyaika on 4 kuukautta.',
          instructions: function ServiceNeedInstructionsText() {
            return (
              <>
                Toivottua aloituspäivää on mahdollista muuttaa myöhemmäksi niin
                kauan kuin hakemusta ei ole otettu käsittelyyn. Tämän jälkeen
                toivotun aloituspäivän muutokset tehdään ottamalla yhteyttä
                varhaiskasvatuksen palveluohjaukseen (puh. 09 816 31000).
              </>
            )
          },
          placeholder: 'Valitse aloituspäivä',
          validationText: 'Toivottu aloituspäivä: '
        },
        clubDetails: {
          wasOnDaycare:
            'Lapsella on varhaiskasvatuspaikka, josta hän luopuu kerhopaikan saadessaan.',
          wasOnDaycareInfo:
            'Jos lapsi on ollut varhaiskasvatuksessa (päiväkodissa, perhepäivähoidossa tai ryhmäperhepäivähoidossa) ja luopuu paikastaan kerhon alkaessa, hänellä on suurempi mahdollisuus saada kerhopaikka.',
          wasOnClubCare:
            'Lapsi on ollut kerhossa edellisen toimintakauden aikana.',
          wasOnClubCareInfo:
            'Jos lapsi on ollut kerhossa jo edellisen toimintakauden aikana, on hänellä suurempi mahdollisuus saada paikka kerhosta.'
        },
        urgent: {
          label: 'Hakemus on kiireellinen',
          attachmentsMessage: {
            text: function UrgentApplicationAttachmentMessageText() {
              return (
                <P fitted={true}>
                  Mikäli varhaiskasvatuspaikan tarve johtuu äkillisestä
                  työllistymisestä tai opiskelusta, tulee paikkaa hakea
                  viimeistään <strong>kaksi viikkoa ennen</strong> kuin tarve
                  alkaa. Hakemuksen{' '}
                  <strong>
                    liitteenä tulee olla työ- tai opiskelutodistus
                  </strong>{' '}
                  molemmilta samassa taloudessa asuvilta huoltajilta.
                  Suosittelemme toimittamaan liitteen sähköisesti tässä, sillä
                  kahden viikon käsittelyaika alkaa siitä, kun olemme
                  vastaanottaneet hakemuksen tarvittavine liitteineen. Jos et
                  voi lisätä liitteitä hakemukselle sähköisesti, lähetä ne
                  postilla osoitteeseen Varhaiskasvatuksen palveluohjaus, PL
                  3125, 02070 Espoon kaupunki.
                </P>
              )
            },
            subtitle:
              'Lisää tähän työ- tai opiskelutodistus molemmilta vanhemmilta.'
          }
        },
        partTime: {
          true: 'Osapäiväinen (max 5h / pv, 25h / vko)',
          false: 'Kokopäiväinen'
        },
        dailyTime: {
          label: {
            DAYCARE: 'Päivittäinen varhaiskasvatusaika',
            PRESCHOOL: 'Esiopetukseen liittyvän varhaiskasvatuksen tarve'
          },
          connectedDaycareInfo: function ConnectedDaycareInfoText() {
            return (
              <>
                <P>
                  Voit hakea lapselle tarvittaessa{' '}
                  <strong>
                    esiopetukseen liittyvää varhaiskasvatusta, joka on
                    maksullista, ja jota annetaan esiopetuksen (4 tuntia/Päivä)
                    lisäksi
                  </strong>{' '}
                  aamuisin ja/tai iltapäivisin samassa paikassa kuin esiopetus.
                  Jos haluat aloittaa varhaiskasvatuksen myöhemmin kuin
                  esiopetus alkaa, kirjoita haluttu aloituspäivämäärä hakemuksen
                  “Muut lisätiedot” -kohtaan.
                </P>
                <P>
                  Yksityisiin esiopetusyksiköihin haettassa, liittyvä
                  varhaiskasvatus haetaan suoraan yksiköstä (pois lukien
                  palveluseteliyksiköt), yksiköt informoivat asiakkaita
                  hakutavasta. Näissä tapauksissa palveluohjaus muuttaa
                  hakemuksen pelkäksi esiopetushakemukseksi.
                </P>
                <P>
                  Palveluseteliä haetaan valitsemalla hakutoiveeksi se
                  palveluseteliyksikkö, johon halutaan hakea.
                </P>
                <P>
                  Saat varhaiskasvatuspaikasta erillisen kirjallisen päätöksen,
                  ja päätös tulee{' '}
                  <a
                    href="https://www.suomi.fi/viestit"
                    target="_blank"
                    rel="noreferrer"
                  >
                    Suomi.fi-viestit
                  </a>{' '}
                  -palveluun tai postitse, mikäli et ole ottanut
                  Suomi.fi-viestit -palvelua käyttöön.
                </P>
              </>
            )
          },
          connectedDaycare:
            'Haen myös esiopetukseen liittyvää varhaiskasvatusta.',
          instructions: {
            DAYCARE:
              'Ilmoita lapsen yleisimmin tarvitseva varhaiskasvatusaika, aika tarkennetaan varhaiskasvatuksen alkaessa.',
            PRESCHOOL:
              'Esiopetusta tarjotaan sekä päiväkodeissa että kouluissa 4 tuntia päivässä pääsääntöisesti 09:00 – 13:00, mutta aika saattaa vaihdella yksiköittäin. Ilmoita lapsen tarvitsema varhaiskasvatusaika siten, että se sisältää esiopetusajan 4 h (esim. 7.00 – 17.00). Aika tarkennetaan varhaiskasvatuksen alkaessa. Varhaiskasvatustarpeen ajan vaihdellessa päivittäin tai viikoittain (esim. vuorohoidossa), ilmoita tarve tarkemmin lisätiedoissa lomakkeen lopussa.'
          },
          usualArrivalAndDeparture: {
            DAYCARE: 'Varhaiskasvatuksen alkamis- ja päättymisaika',
            PRESCHOOL:
              'Päivittäinen varhaiskasvatusaika (sisältäen esiopetuksen)'
          },
          starts: 'Alkaa',
          ends: 'Päättyy'
        },
        shiftCare: {
          label: 'Ilta- ja vuorohoito',
          instructions:
            'Vuorohoidolla tarkoitetaan viikonloppuna tai ympärivuorokautisesti tarvittavaa varhaiskasvatusta. Iltahoito on pääasiassa klo 6.30-18.00 ulkopuolella ja viikonloppuisin tapahtuvaa varhaiskasvatusta. Mikäli tarvitset ilta- tai vuorohoitoa, täsmennä tarvetta lisätietokentässä.',
          message: {
            title: 'Ilta- ja vuorohoito',
            text:
              'Ilta- ja vuorohoito on tarkoitettu lapsille, joiden molemmat vanhemmat ovat vuorotyössä tai opiskelevat pääsääntöisesti iltaisin ja/ viikonloppuisin. Hakemuksen liitteeksi toimitetaan molempien vanhempien osalta työnantajan todistus vuorotyöstä tai opiskelusta johtuvasta ilta- tai vuorohoidon tarpeesta.'
          },
          attachmentsMessage: {
            text:
              'Ilta- ja vuorohoito on tarkoitettu lapsille, joiden molemmat vanhemmat ovat vuorotyössä tai opiskelevat pääsääntöisesti iltaisin ja/tai viikonloppuisin. Hakemuksen liitteeksi toimitetaan molempien vanhempien osalta työnantajan todistus vuorotyöstä tai opiskelusta johtuvasta ilta- tai vuorohoidon tarpeesta. Jos et voi lisätä liitteitä hakemukselle sähköisesti, lähetä ne postilla osoitteeseen Varhaiskasvatuksen palveluohjaus, PL 3125, 02070 Espoon kaupunki.',
            subtitle:
              'Lisää tähän molemmilta vanhemmilta joko työnantajan todistus vuorotyöstä tai todistus opiskelusta iltaisin/viikonloppuisin.'
          }
        },
        assistanceNeed: 'Tuen tarve',
        assistanceNeeded: 'Lapsella on tuen tarve',
        assistanceNeedLabel: 'Tuen tarpeen kuvaus',
        assistanceNeedPlaceholder: 'Kerro lapsen tuen tarpeesta.',
        assistanceNeedInstructions: {
          DAYCARE:
            'Lapsen tuen tarpeella tarkoitetaan sellaisten tukitoimien tarvetta, jotka on osoitettu asiantuntijalausunnoin. Tuen tarpeissa Espoon varhaiskasvatuksesta otetaan erikseen yhteyttä hakemuksen jättämisen jälkeen. Tukitoimet toteutuvat lapsen arjessa osana varhaiskasvatuksen muuta toimintaa. Osa varhaiskasvatuspaikoista on varattu tukea tarvitseville lapsille.',
          CLUB:
            'Lapsen tuen tarpeella tarkoitetaan sellaisten tukitoimien tarvetta, jotka on osoitettu asiantuntijalausunnoin. Tuen tarpeissa Espoon varhaiskasvatuksesta otetaan erikseen yhteyttä hakemuksen jättämisen jälkeen. Tukitoimet toteutuvat lapsen arjessa osana varhaiskasvatuksen muuta toimintaa. Osa varhaiskasvatuspaikoista on varattu tukea tarvitseville lapsille.',
          PRESCHOOL:
            'Lapsen tuen tarpeella tarkoitetaan sellaisten tukitoimien tarvetta, jotka on osoitettu asiantuntijalausunnoin. Tuen tarpeissa Espoon varhaiskasvatuksesta otetaan erikseen yhteyttä hakemuksen jättämisen jälkeen. Tukitoimet toteutuvat lapsen arjessa osana varhaiskasvatuksen muuta toimintaa. Osa varhaiskasvatuspaikoista on varattu tukea tarvitseville lapsille.'
        },
        preparatory:
          'Lapsi tarvitsee tukea suomen kielen oppimisessa. Haen myös perusopetukseen valmistavaan opetukseen. Ei koske ruotsinkielistä esiopetusta.',
        preparatoryInfo:
          'Esiopetusikäisten valmistavaan opetukseen voivat hakeutua maahanmuuttajataustaiset lapset, paluumuuttajalapset, kaksikielisten perheiden lapset (paitsi suomi-ruotsi) ja adoptiolapset, jotka tarvitsevat tukea suomen kielessä ennen perusopetukseen siirtymistä. Valmistavaa opetusta annetaan esiopetuksen lisäksi keskimäärin 1 h/Päivä. Opetus on maksutonta.'
      },
      unitPreference: {
        title: 'Hakutoive',
        siblingBasis: {
          title: 'Haku sisarperusteella',
          info: {
            DAYCARE: function SiblingBasisSummaryTextDaycare() {
              return (
                <>
                  <P>
                    Lapsella on sisarusperuste samaan varhaiskasvatuspaikkaan,
                    jossa hänen sisaruksensa on päätöksentekohetkellä.
                    Sisarukseksi katsotaan kaikki samassa osoitteessa asuvat
                    lapset. Tavoitteena on sijoittaa sisarukset samaan
                    varhaiskasvatuspaikkaan perheen niin toivoessa. Jos haet
                    paikkaa sisaruksille, jotka eivät vielä ole
                    varhaiskasvatuksessa, kirjoita tieto lisätietokenttään.
                  </P>
                  <P>
                    Täytä nämä tiedot vain, jos käytät sisarusperustetta, sekä
                    valitse alla olevissa hakutoiveissa ensisijaiseksi toiveeksi
                    sama varhaiskasvatusyksikkö, jossa lapsen sisarus on.
                  </P>
                </>
              )
            },
            PRESCHOOL: function SiblingBasisSummaryTextPreschool() {
              return (
                <>
                  <P>Esioppilaalla on sisarusperuste:</P>
                  <ol type="a">
                    <li>
                      Oman palvelualueen päiväkotiin, jossa esioppilaalla on
                      sisarus, jolla on päätöksentekohetkellä ja tulevana
                      esiopetusvuonna paikka esiopetuspäiväkodissa.
                    </li>
                    <li>
                      Kunnan osoittamaan lähikouluun, jota esioppilaan sisarus
                      käy tulevana lukuvuonna.
                    </li>
                  </ol>
                  <P>
                    Huoltaja voi valita, käyttääkö hän sisarusperustetta kohdan
                    a vai b mukaisesti, jos esioppilaalla on sisarusperuste
                    molempien kohtien mukaan. Valinta ilmoitetaan alla.
                    Sisarukseksi katsotaan kaikki samassa osoitteessa asuvat
                    lapset.
                  </P>
                  <P>
                    Täytä nämä tiedot vain, jos käytät sisarusperustetta, sekä
                    valitse alla olevissa hakutoiveissa ensisijaiseksi toiveeksi
                    sama yksikkö, jossa lapsen sisarus on.
                  </P>
                </>
              )
            },
            CLUB: function SiblingBasisSummaryTextClub() {
              return (
                <>
                  <P>
                    Sisarukseksi katsotaan kaikki samassa osoitteessa asuvat
                    lapset. Tavoitteena on sijoittaa sisarukset samaan
                    kerhoryhmään perheen niin toivoessa.
                  </P>
                  <P>
                    Täytä nämä tiedot vain, jos käytät sisarusperustetta, sekä
                    valitse alla olevissa hakutoiveissa ensisijaiseksi toiveeksi
                    sama kerho, jossa lapsen sisarus on.
                  </P>
                </>
              )
            }
          },
          checkbox: {
            DAYCARE:
              'Haen ensisijaisesti samaan paikkaan, jossa lapsen sisarus on jo varhaiskasvatuksessa.',
            PRESCHOOL:
              'Haen ensisijaisesti samaan paikkaan, jossa lapsen sisarus on.',
            CLUB:
              'Haen ensisijaisesti paikkaa samasta kerhoryhmästä, jossa lapsen sisarus on.'
          },
          radioLabel: {
            DAYCARE:
              'Valitse sisarus, jonka kanssa haet samaan varhaiskasvatuspaikkaan',
            PRESCHOOL: 'Valitse sisarus, jonka kanssa haet samaan paikkaan',
            CLUB: 'Valitse sisarus, jonka kanssa haet samaan kerhoryhmään'
          },
          otherSibling: 'Muu sisarus',
          names: 'Sisaruksen etunimet ja sukunimi',
          namesPlaceholder: 'Etunimet ja sukunimi',
          ssn: 'Sisaruksen henkilötunnus',
          ssnPlaceholder: 'Henkilötunnus'
        },
        units: {
          title: 'Hakutoiveet',
          startDateMissing:
            'Päästäksesi valitsemaan hakutoiveet valitse ensin toivottu aloituspäivä "Palvelun tarve" -osiosta',
          info: {
            DAYCARE: function UnitPreferenceInfoTextDaycare() {
              return (
                <>
                  <P>
                    Voit hakea 1-3 paikkaa toivomassasi järjestyksessä.
                    Hakutoiveet eivät takaa paikkaa toivotussa yksikössä, mutta
                    mahdollisuus toivotun paikan saamiseen kasvaa antamalla
                    useamman vaihtoehdon.
                  </P>
                  <P>
                    Näet eri varhaiskasvatusyksiköiden sijainnin valitsemalla
                    ‘Yksiköt kartalla’.
                  </P>
                  <P>
                    Palveluseteliä haetaan valitsemalla hakutoiveeksi se
                    palveluseteliyksikkö, johon halutaan hakea.
                    Palveluseteliyksikköön haettaessa myös yksikön esimies saa
                    tiedon hakemuksesta.
                  </P>
                </>
              )
            },
            PRESCHOOL: function UnitPreferenceInfoTextPreschool() {
              return (
                <>
                  <P>
                    Voit hakea 1-3 paikka paikkaa toivomassasi järjestyksessä.
                    Hakutoiveet eivät takaa paikkaa toivotussa yksikössä, mutta
                    mahdollisuus toivotun paikan saamiseen kasvaa antamalla
                    useamman vaihtoehdon.
                  </P>
                  <P>
                    Näet eri yksiköiden sijainnin valitsemalla ‘Yksiköt
                    kartalla’.
                  </P>
                  <P>
                    Palveluseteliä haetaan valitsemalla hakutoiveeksi se
                    palveluseteliyksikkö, johon halutaan hakea.
                    Palveluseteliyksikköön haettaessa myös yksikön esimies saa
                    tiedon hakemuksesta.
                  </P>
                </>
              )
            },
            CLUB: function UnitPreferenceInfoTextClub() {
              return (
                <>
                  <P>
                    Voit hakea 1-3 paikkaa toivomassasi järjestyksessä.
                    Hakutoiveet eivät takaa paikkaa toivotussa kerhossa, mutta
                    mahdollisuus toivotun paikan saamiseen kasvaa antamalla
                    useamman vaihtoehdon.
                  </P>
                  <P>
                    Näet eri kerhojen sijainnin valitsemalla ‘Yksiköt kartalla’.
                  </P>
                </>
              )
            }
          },
          mapLink: 'Yksiköt kartalla',
          serviceVoucherLink:
            'https://www.espoo.fi/fi-FI/Kasvatus_ja_opetus/Varhaiskasvatus/Hakeminen_varhaiskasvatukseen/Palveluseteli/Tietoa_perheille',
          languageFilter: {
            label: 'Yksikön kieli',
            fi: 'suomi',
            sv: 'ruotsi'
          },
          select: {
            label: 'Valitse hakutoiveet',
            placeholder: 'Hae yksiköitä',
            maxSelected: 'Maksimimäärä yksiköitä valittu',
            noOptions: 'Ei hakuehtoja vastaavia yksiköitä'
          },
          preferences: {
            label: 'Valitsemasi hakutoiveet',
            noSelections: 'Ei valintoja',
            info:
              'Valitse 1-3 varhaiskasvatusyksikköä ja järjestä ne toivomaasi järjestykseen. Voit muuttaa järjestystä nuolien avulla.',
            fi: 'suomenkielinen',
            sv: 'ruotsinkielinen',
            moveUp: 'Siirrä ylöspäin',
            moveDown: 'Siirrä alaspäin',
            remove: 'Poista hakutoive'
          }
        }
      },
      fee: {
        title: 'Varhaiskasvatusmaksu',
        info: {
          DAYCARE: function FeeInfoTextDaycare() {
            return (
              <P>
                Kunnallisen varhaiskasvatuksen asiakasmaksut ja palvelusetelin
                omavastuuosuus määräytyvät prosenttiosuutena perheen
                bruttotuloista. Maksut vaihtelevat perheen koon ja tulojen sekä
                varhaiskasvatusajan mukaan maksuttomasta varhaiskasvatuksesta
                enintään 288 euron kuukausimaksuun lasta kohden.
                Palveluseteliyksiköissä voidaan kuitenkin periä 0-50€/kk/lapsi
                lisämaksu. Perhe toimittaa tuloselvityksen bruttotuloistaan
                tuloselvityslomakkeella, viimeistään kahden viikon kuluessa
                siitä, kun lapsi on aloittanut varhaiskasvatuksessa.
              </P>
            )
          },
          PRESCHOOL: function FeeInfoTextPreschool() {
            return (
              <P>
                Esiopetus on maksutonta, mutta siihen liittyvä varhaiskasvatus
                on maksullista. Jos lapsi osallistuu liittyvään
                varhaiskasvatukseen, perhe toimittaa tuloselvityksen
                bruttotuloistaan tuloselvityslomakkeella viimeistään kahden
                viikon kuluessa siitä, kun lapsi on aloittanut
                varhaiskasvatuksessa.
              </P>
            )
          },
          CLUB: function FeeInfoTextClub() {
            return <P></P>
          }
        },
        emphasis: function FeeEmphasisText() {
          return (
            <strong>
              Tuloselvitystä ei tarvita, jos perhe suostuu korkeimpaan maksuun.
            </strong>
          )
        },
        checkbox:
          'Annan suostumuksen korkeimpaan maksuun. Suostumus on voimassa toistaiseksi, kunnes toisin ilmoitan.',
        links: function FeeLinksText() {
          return (
            <P>
              Lisätietoa varhaiskasvatuksen maksuista, palvelusetelin
              lisämaksusta ja tuloselvityslomakkeen löydät täältä:
              <br />
              <a
                href="https://www.espoo.fi/fi-FI/Kasvatus_ja_opetus/Varhaiskasvatus/Maksut_varhaiskasvatuksessa"
                target="_blank"
                rel="noopener noreferrer"
              >
                Maksut varhaiskasvatuksessa
              </a>
            </P>
          )
        }
      },
      additionalDetails: {
        title: 'Muut lisätiedot',
        otherInfoLabel: 'Hakuun liittyvät lisätiedot',
        otherInfoPlaceholder:
          'Voit halutessasi antaa hakuun liittyvää tarkempaa lisätietoa',
        dietLabel: 'Erityisruokavalio',
        dietPlaceholder: 'Voit halutessasi ilmoittaa lapsen erityisruokavalion',
        dietInfo: function DietInfoText() {
          return 'Osaan erityisruokavalioista tarvitaan erikseen lääkärintodistus, joka toimitetaan varhaiskasvatuspaikkaan. Poikkeuksena vähälaktoosinen tai laktoositon ruokavalio, uskonnollisiin syihin perustuva ruokavalio tai kasvisruokavalio (lakto-ovo).'
        },
        allergiesLabel: 'Allergiat',
        allergiesPlaceholder: 'Voit halutessasi ilmoittaa lapsen allergiat',
        allergiesInfo:
          'Allergiatieto tarvitaan lähinnä perhepäivähoitoa haettaessa.'
      },
      contactInfo: {
        title: 'Henkilötiedot',
        info: function ContactInfoInfoText() {
          return (
            <P>
              Henkilötiedot on haettu väestötiedoista, eikä niitä voi muuttaa
              tällä hakemuksella. Jos henkilötiedoissa on virheitä, päivitäthän
              tiedot{' '}
              <a
                href="https://dvv.fi/henkiloasiakkaat"
                target="_blank"
                rel="noreferrer"
              >
                Digi- ja Väestötietoviraston sivuilla
              </a>
              . Mikäli osoitteenne on muuttumassa, voit lisätä tulevan osoitteen
              erilliseen kohtaan hakemuksella; lisää tuleva osoite sekä lapselle
              että huoltajalle. Virallisena osoitetietoa pidetään vasta, kun se
              on päivittynyt väestötietojärjestelmään. Päätökset esiopetus- ja
              varhaiskasvatuspaikoista toimitetaan automaattisesti myös eri
              osoitteessa asuvalle väestötiedoista löytyvälle huoltajalle.
            </P>
          )
        },
        emailInfoText:
          'Sähköpostiosoitteen avulla saat tiedon uusista viesteistä eVakassa. Esitäytetty sähköpostiosoite on haettu eVakan asiakastiedoista. Mikäli muokkaat sitä, päivitetään vanha sähköpostiosoite, kun hakemus lähetetään.',
        childInfoTitle: 'Lapsen tiedot',
        childFirstName: 'Lapsen etunimet',
        childLastName: 'Lapsen sukunimi',
        childSSN: 'Lapsen henkilötunnus',
        homeAddress: 'Kotiosoite',
        moveDate: 'Muuttopäivämäärä',
        street: 'Katuosoite',
        postalCode: 'Postinumero',
        postOffice: 'Postitoimipaikka',
        guardianInfoTitle: 'Huoltajan tiedot',
        guardianFirstName: 'Huoltajan etunimet',
        guardianLastName: 'Huoltajan sukunimi',
        guardianSSN: 'Huoltajan henkilötunnus',
        phone: 'Puhelinnumero',
        verifyEmail: 'Vahvista sähköpostiosoite',
        email: 'Sähköpostiosoite',
        noEmail: 'Minulla ei ole sähköpostiosoitetta',
        secondGuardianInfoTitle: 'Toisen huoltajan tiedot',
        secondGuardianInfo:
          'Toisen huoltajan tiedot haetaan automaattisesti väestötietojärjestelmästä.',
        secondGuardianNotFound:
          'Väestötietojärjestelmästä saatujen tietojen mukaan lapsella ei ole toista huoltajaa',
        secondGuardianInfoPreschoolSeparated:
          'Toisen huoltajan tiedot haetaan automaattisesti väestötietojärjestelmästä. Tietojemme mukaan lapsen toinen huoltaja asuu eri osoitteessa. Esiopetukseen ilmoittautumisesta tulee sopia yhdessä toisen huoltajan kanssa.',
        secondGuardianAgreementStatus: {
          label:
            'Oletteko sopineet hakemuksen tekemisestä yhdessä toisen huoltajan kanssa?',
          AGREED: 'Olemme yhdessä sopineet hakemuksen tekemisestä.',
          NOT_AGREED: 'Emme ole voineet sopia hakemuksen tekemisestä yhdessä.',
          RIGHT_TO_GET_NOTIFIED:
            'Toisella huoltajalla on vain tiedonsaantioikeus.'
        },
        secondGuardianPhone: 'Toisen huoltajan puhelinnumero',
        secondGuardianEmail: 'Toisen huoltajan sähköpostiosoite',
        otherPartnerTitle:
          'Samassa taloudessa asuva avio- tai avopuoliso (ei huoltaja)',
        otherPartnerCheckboxLabel:
          'Samassa taloudessa asuu hakijan kanssa avio- tai avoliitossa oleva henkilö, joka ei ole lapsen huoltaja.',
        personFirstName: 'Henkilön etunimet',
        personLastName: 'Henkilön sukunimi',
        personSSN: 'Henkilön henkilötunnus',
        otherChildrenTitle: 'Samassa taloudessa asuvat alle 18-vuotiaat lapset',
        otherChildrenInfo:
          'Samassa taloudessa asuvat alle 18-vuotiaat lapset vaikuttavat varhaiskasvatusmaksuihin.',
        otherChildrenChoiceInfo:
          'Valitse lapset, jotka asuvat samassa taloudessa.',
        hasFutureAddress:
          'Väestörekisterissä oleva osoite on muuttunut tai muuttumassa',
        futureAddressInfo:
          'Espoon varhaiskasvatuksessa virallisena osoitteena pidetään väestötiedoista saatavaa osoitetta. Osoite väestötiedoissa muuttuu hakijan tehdessä muuttoilmoituksen postiin tai maistraattiin.',
        guardianFutureAddressEqualsChildFutureAddress:
          'Muutan samaan osoitteeseen kuin lapsi',
        firstNamePlaceholder: 'Etunimet',
        lastNamePlaceholder: 'Sukunimi',
        ssnPlaceholder: 'Henkilötunnus',
        streetPlaceholder: 'Osoite',
        postalCodePlaceholder: 'Postinumero',
        municipalityPlaceholder: 'Postitoimipaikka',
        addChild: 'Lisää lapsi',
        remove: 'Poista',
        areExtraChildren:
          'Samassa taloudessa asuu muita alle 18-vuotiaita lapsia (esim. puolison / avopuolison lapset)',
        choosePlaceholder: 'Valitse'
      },
      draftPolicyInfo: {
        title: 'Hakemusluonnos on tallennettu',
        text:
          'Hakemus on tallennettu keskeneräisenä. Huom! Keskeneräistä hakemusta säilytetään palvelussa yhden kuukauden ajan viimeisimmästä tallennuksesta.',
        ok: 'Selvä!'
      },
      sentInfo: {
        title: 'Hakemus on lähetetty',
        text:
          'Halutessasi voit tehdä hakemukseen muutoksia niin kauan kuin hakemusta ei olla otettu käsittelyyn.',
        ok: 'Selvä!'
      },
      updateInfo: {
        title: 'Muutokset hakemukseen on tallennettu',
        text:
          'Halutessasi voit tehdä lisää muutoksia niin kauan kuin hakemusta ei olla otettu käsittelyyn.',
        ok: 'Selvä!'
      }
    }
  },
  decisions: {
    title: 'Päätökset',
    summary: function DecisionsSummaryText() {
      return (
        <P width="800px">
          Tälle sivulle saapuvat lapsen varhaiskasvatus-, esiopetus- ja
          kerhohakemuksiin liittyvät päätökset. Uuden päätöksen saapuessa{' '}
          <strong>sinun tulee kahden viikon sisällä vastata</strong>, hyväksytkö
          vai hylkäätkö lapselle tarjotun paikan.
        </P>
      )
    },
    unconfirmedDecisions: (n: number) =>
      `${n} ${n === 1 ? 'päätös' : 'päätöstä'} odottaa vahvistustasi`,
    pageLoadError: 'Tietojen hakeminen ei onnistunut',
    applicationDecisions: {
      decision: 'Päätös',
      type: {
        CLUB: 'kerhosta',
        DAYCARE: 'varhaiskasvatuksesta',
        DAYCARE_PART_TIME: 'osa-aikaisesta varhaiskasvatuksesta',
        PRESCHOOL: 'esiopetuksesta',
        PRESCHOOL_DAYCARE: 'liittyvästä varhaiskasvatuksesta',
        PREPARATORY_EDUCATION: 'valmistavasta opetuksesta'
      },
      childName: 'Lapsen nimi',
      unit: 'Toimipaikka',
      period: 'Ajalle',
      sentDate: 'Päätös saapunut',
      resolved: 'Vahvistettu',
      statusLabel: 'Tila',
      summary:
        'Päätöksessä ilmoitettu paikka / ilmoitetut paikat tulee joko hyväksyä tai hylätä välittömästi, viimeistään kahden viikon kuluessa päätöksen saapumisesta.',
      status: {
        PENDING: 'Vahvistettavana huoltajalla',
        ACCEPTED: 'Hyväksytty',
        REJECTED: 'Hylätty'
      },
      confirmationInfo: {
        preschool:
          'Esiopetuksen, valmistavan opetuksen ja/tai liittyvän varhaiskasvatuksen hyväksymis- tai hylkäämisilmoitus on toimitettava välittömästi, viimeistään kahden viikon kuluessa tämän ilmoituksen saamisesta. Jos olet hakenut useampaa palvelua, saat jokaisesta oman päätöksen erikseen vahvistettavaksi',
        default:
          'Päätöksessä ilmoitetun paikan hyväksymis- tai hylkäämisilmoitus on toimitettava välittömästi, viimeistään kahden viikon kuluessa tämän ilmoituksen saamisesta.'
      },
      goToConfirmation:
        'Siirry lukemaan päätös ja vastaamaan hyväksytkö vai hylkäätkö paikan.',
      confirmationLink: 'Siirry vastaamaan',
      response: {
        title: 'Paikan hyväksyminen tai hylkääminen',
        accept1: 'Otamme paikan vastaan',
        accept2: 'alkaen',
        reject: 'Emme ota paikkaa vastaan',
        cancel: 'Palaa takaisin vastaamatta',
        submit: 'Lähetä vastaus päätökseen',
        disabledInfo:
          'HUOM! Pääset hyväksymään/hylkäämään liittyvää varhaiskasvatusta koskevan päätöksen mikäli hyväksyt ensin esiopetusta / valmistavaa opetusta koskevan päätöksen.'
      },
      openPdf: 'Näytä päätös',
      warnings: {
        decisionWithNoResponseWarning: {
          title: 'Toinen päätös odottaa vastaustasi',
          text:
            'Toinen päätös odottaa edelleen vastaustasi. Haluatko  palata listalle vastaamatta?',
          resolveLabel: 'Palaa vastaamatta',
          rejectLabel: 'Jatka vastaamista'
        },
        doubleRejectWarning: {
          title: 'Haluatko hylätä paikan?',
          text:
            'Olet hylkäämässä tarjotun esiopetus / valmistavan paikan. Liittyvän varhaiskasvatuksen paikka merkitään samalla hylätyksi.',
          resolveLabel: 'Hylkää molemmat',
          rejectLabel: 'Palaa takaisin'
        }
      },
      errors: {
        pageLoadError: 'Tietojen hakeminen ei onnistunut',
        submitFailure: 'Päätökseen vastaaminen ei onnistunut'
      },
      returnToPreviousPage: 'Palaa'
    }
  },
  applicationsList: {
    title: 'Hakeminen varhaiskasvatukseen ja ilmoittautuminen esiopetukseen',
    summary: function ApplicationListSummaryText() {
      return (
        <P width="800px">
          Lapsen huoltaja voi tehdä lapselle hakemuksen varhaiskasvatukseen ja
          kerhoon tai ilmoittaa lapsen esiopetukseen. Samalla hakemuksella voi
          hakea myös varhaiskasvatuksen palveluseteliä, hakemalla
          varhaiskasvatuspaikkaa palveluseteliyksiköstä. Huoltajan lasten tiedot
          haetaan tähän näkymään automaattisesti Väestötietojärjestelmästä.
        </P>
      )
    },
    pageLoadError: 'Tietojen hakeminen ei onnistunut',
    noApplications: 'Ei hakemuksia',
    type: {
      DAYCARE: 'Varhaiskasvatushakemus',
      PRESCHOOL: 'Esiopetushakemus',
      CLUB: 'Kerhohakemus'
    },
    transferApplication: 'Siirtohakemus',
    unit: 'Yksikkö',
    period: 'Ajalle',
    created: 'Luotu',
    modified: 'Muokattu',
    status: {
      title: 'Tila',
      CREATED: 'Luonnos',
      SENT: 'Lähetetty',
      WAITING_PLACEMENT: 'Käsiteltävänä',
      WAITING_DECISION: 'Käsiteltävänä',
      WAITING_UNIT_CONFIRMATION: 'Käsiteltävänä',
      WAITING_MAILING: 'Käsiteltävänä',
      WAITING_CONFIRMATION: 'Vahvistettavana huoltajalla',
      REJECTED: 'Paikka hylätty',
      ACTIVE: 'Paikka vastaanotettu',
      CANCELLED: 'Poistettu käsittelystä'
    },
    openApplicationLink: 'Näytä hakemus',
    editApplicationLink: 'Muokkaa hakemusta',
    removeApplicationBtn: 'Poista hakemus',
    cancelApplicationBtn: 'Peruuta hakemus',
    confirmationLinkInstructions:
      'Päätökset-välilehdellä voit lukea päätöksen ja hyväksyä/hylätä tarjotun paikan',
    confirmationLink: 'Siirry vahvistamaan',
    newApplicationLink: 'Uusi hakemus'
  },
  fileUpload: {
    loading: 'Ladataan...',
    loaded: 'Ladattu',
    error: {
      FILE_TOO_LARGE: 'Liian suuri tiedosto (max. 10MB)',
      SERVER_ERROR: 'Lataus ei onnistunut'
    },
    input: {
      title: 'Lisää liite',
      text: [
        'Paina tästä tai raahaa liite laatikkoon yksi kerrallaan.',
        'Tiedoston maksimikoko: 10MB.',
        'Sallitut tiedostomuodot:',
        'PDF, JPEG/JPG, PNG ja DOC/DOCX'
      ]
    },
    deleteFile: 'Poista tiedosto'
  },
  fileDownload: {
    modalHeader: 'Tiedoston käsittely on kesken',
    modalMessage:
      'Tiedosto ei ole juuri nyt avattavissa. Kokeile hetken kuluttua uudelleen.'
  },
  validationErrors: {
    required: 'Pakollinen tieto',
    requiredSelection: 'Valinta puuttuu',
    format: 'Anna oikeassa muodossa',
    ssn: 'Virheellinen henkilötunnus',
    phone: 'Virheellinen numero',
    email: 'Virheellinen sähköpostiosoite',
    validDate: 'Anna muodossa pp.kk.vvvv',
    preferredStartDate: 'Aloituspäivä ei ole sallittu',
    timeFormat: 'Anna muodossa hh:mm',
    unitNotSelected: 'Valitse vähintään yksi hakutoive',
    emailsDoNotMatch: 'Sähköpostiosoitteet eivät täsmää'
  },
  login: {
    failedModal: {
      header: 'Kirjautuminen epäonnistui',
      message:
        'Palveluun tunnistautuminen epäonnistui tai se keskeytettiin. Kirjautuaksesi sisään palaa takaisin ja yritä uudelleen.',
      returnMessage: 'Palaa takaisin'
    }
  }
}
