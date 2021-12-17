// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import ExternalLink from 'lib-components/atoms/ExternalLink'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import { P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import React from 'react'

const yes = 'Kyllä'
const no = 'Ei'

export default {
  common: {
    title: 'Varhaiskasvatus',
    cancel: 'Peruuta',
    return: 'Palaa',
    ok: 'Ok',
    save: 'Tallenna',
    discard: 'Älä tallenna',
    saveConfirmation: 'Haluatko tallentaa muutokset?',
    confirm: 'Vahvista',
    delete: 'Poista',
    edit: 'Muokkaa',
    add: 'Lisää',
    yes,
    no,
    yesno: (value: boolean): string => (value ? yes : no),
    select: 'Valitse',
    page: 'Sivu',
    unit: {
      providerTypes: {
        MUNICIPAL: 'Kunnallinen',
        PURCHASED: 'Ostopalvelu',
        PRIVATE: 'Yksityinen',
        MUNICIPAL_SCHOOL: 'Kunnallinen',
        PRIVATE_SERVICE_VOUCHER: 'Palveluseteli',
        EXTERNAL_PURCHASED: 'Ostopalvelu (muu)'
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
    },
    datetime: {
      weekdaysShort: ['Ma', 'Ti', 'Ke', 'To', 'Pe', 'La', 'Su'],
      week: 'Viikko',
      weekShort: 'Vk',
      weekdays: [
        'Maanantai',
        'Tiistai',
        'Keskiviikko',
        'Torstai',
        'Perjantai',
        'Lauantai',
        'Sunnuntai'
      ],
      months: [
        'Tammikuu',
        'Helmikuu',
        'Maaliskuu',
        'Huhtikuu',
        'Toukokuu',
        'Kesäkuu',
        'Heinäkuu',
        'Elokuu',
        'Syyskuu',
        'Lokakuu',
        'Marraskuu',
        'Joulukuu'
      ]
    }
  },
  header: {
    nav: {
      map: 'Kartta',
      applications: 'Hakemukset',
      decisions: 'Päätökset',
      personalDetails: 'Omat tiedot',
      income: 'Tulotiedot',
      messages: 'Viestit',
      calendar: 'Kalenteri',
      applying: 'Hakeminen',
      pedagogicalDocuments: 'Kasvu ja oppiminen',
      children: 'Lapset'
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
    privacyPolicyLink: 'https://www.espoo.fi/fi/espoon-kaupunki/tietosuoja',
    sendFeedback: 'Lähetä palautetta',
    sendFeedbackLink:
      'https://easiointi.espoo.fi/eFeedback/fi/Feedback/20-S%C3%A4hk%C3%B6iset%20asiointipalvelut'
  },
  errorPage: {
    reload: 'Lataa sivu uudelleen',
    text: 'Kohtasimme odottamattoman ongelman. Virheen tiedot on välitetty eteenpäin.',
    title: 'Jotain meni pieleen'
  },
  map: {
    title: 'Yksiköt kartalla',
    mainInfo:
      'Tässä näkymässä voit hakea kartalta Espoon varhaiskasvatus-, esiopetus- ja kerhopaikkoja.',
    privateUnitInfo: (
      <span>
        Tietoa yksityisistä päiväkodeista löydät{' '}
        <ExternalLink
          text="täältä."
          href="https://www.espoo.fi/fi/kasvatus-ja-opetus/varhaiskasvatus/yksityinen-varhaiskasvatus-ja-paivakodit"
          newTab
        />
      </span>
    ),
    searchLabel: 'Hae osoitteella tai yksikön nimellä',
    searchPlaceholder: 'Esim. Purolan päiväkoti',
    address: 'Osoite',
    noResults: 'Ei hakutuloksia',
    keywordRequired: 'Kirjoita hakusana',
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
      EXTERNAL_PURCHASED: 'ostopalvelu',
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
    showMoreFilters: 'Näytä lisää filttereitä',
    showLessFilters: 'Näytä vähemmän filttereitä',
    nearestUnits: 'Lähimmät yksiköt',
    moreUnits: 'Lisää yksiköitä',
    showMore: 'Näytä lisää hakutuloksia',
    mobileTabs: {
      map: 'Kartta',
      list: 'Yksiköt'
    },
    serviceVoucherLink:
      'https://www.espoo.fi/fi/kasvatus-ja-opetus/varhaiskasvatus/varhaiskasvatuksen-palveluseteli#section-6228',
    noApplying: 'Ei hakua eVakan kautta, ota yhteys yksikköön',
    backToSearch: 'Takaisin hakuun'
  },
  calendar: {
    title: 'Kalenteri',
    holiday: 'Pyhäpäivä',
    absent: 'Poissa',
    absences: {
      SICKLEAVE: 'Sairauspoissaolo',
      PLANNED_ABSENCE: 'Vapaapäivä'
    },
    newReservationOrAbsence: 'Varaus / Poissaolo',
    newAbsence: 'Ilmoita poissaolo',
    newReservationBtn: 'Tee varaus',
    noReservation: 'Ei varausta',
    reservation: 'Varaus',
    realized: 'Toteuma',
    reservationsAndRealized: 'Varaukset ja toteuma',
    reservationModal: {
      title: 'Tee varaus',
      selectChildren: 'Lapset, joille varaus tehdään',
      selectChildrenLabel: 'Valitse lapset',
      dateRange: 'Varauksen voimassaolo',
      dateRangeLabel: 'Tee varaus päiville',
      missingDateRange: 'Valitse varattavat päivät',
      repetition: 'Toistuvuus',
      postError: 'Varauksen luominen ei onnistunut',
      repetitions: {
        DAILY: 'Päivittäin',
        WEEKLY: 'Viikoittain',
        IRREGULAR: 'Epäsäännöllinen'
      },
      start: 'Alkaa',
      end: 'Päättyy'
    },
    absenceModal: {
      title: 'Ilmoita poissaolo',
      selectedChildren: 'Valitse lapset, joille ilmoitat poissaolon',
      selectChildrenInfo:
        'Ilmoita tässä vain koko päivän poissaolot. Osan päivän poissaolon voit ilmoittaa läsnäoloilmoituksella.',
      dateRange: 'Poissaoloilmoitus ajalle',
      absenceType: 'Poissaolon syy',
      absenceTypes: {
        SICKLEAVE: 'Sairaus',
        OTHER_ABSENCE: 'Muu poissaolo',
        PLANNED_ABSENCE: 'Säännöllinen vapaapäivä/vuorohoito'
      }
    }
  },
  messages: {
    inboxTitle: 'Viestit',
    noMessagesInfo: 'Tässä näet saapuneet ja lähetetyt viestisi',
    emptyInbox: 'Viestilaatikkosi on tyhjä',
    noSelectedMessage: 'Ei valittua viestiä',
    recipients: 'Vastaanottajat',
    send: 'Lähetä',
    sender: 'Lähettäjä',
    sending: 'Lähetetään',
    messagePlaceholder:
      'Viestin sisältö... Huom! Älä kirjoita tähän arkaluontoisia asioita.',
    types: {
      MESSAGE: 'Viesti',
      BULLETIN: 'Tiedote'
    },
    replyToThread: 'Vastaa viestiin',
    staffAnnotation: 'Henkilökunta',
    messageEditor: {
      newMessage: 'Uusi viesti',
      recipients: 'Vastaanottajat',
      subject: 'Otsikko',
      message: 'Viesti',
      deleteDraft: 'Hylkää luonnos',
      send: 'Lähetä',
      discard: 'Hylkää',
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
        'Liittyvän varhaiskasvatuksen hakeminen lapsille, jotka ilmoitetaan / on ilmoitettu esiopetukseen tai perusopetukseen valmistavaan opetukseen',
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
      applicationInfo: (
        <P>
          Hakemukseen voi tehdä muutoksia niin kauan kuin hakemusta ei ole
          otettu käsittelyyn. Tämän jälkeen muutokset hakemukseen tehdään
          ottamalla yhteyttä varhaiskasvatuksen palveluohjaukseen (puh. 09 816
          31000). Voit perua jo tehdyn hakemuksen ilmoittamalla siitä
          sähköpostilla varhaiskasvatuksen palveluohjaukseen{' '}
          <a href="mailto:vaka.palveluohjaus@espoo.fi">
            vaka.palveluohjaus@espoo.fi
          </a>
          .
        </P>
      )
    },
    editor: {
      heading: {
        title: {
          DAYCARE: 'Varhaiskasvatus- ja palvelusetelihakemus',
          PRESCHOOL: 'Ilmoittautuminen esiopetukseen',
          CLUB: 'Kerhohakemus'
        },
        info: {
          DAYCARE: (
            <>
              <P>
                Varhaiskasvatusta voi hakea ympäri vuoden. Hakemus on jätettävä
                viimeistään neljä kuukautta ennen kuin tarvitsette paikan.
                Mikäli tarvitsette varhaiskasvatusta kiireellisesti työn tai
                opiskelujen vuoksi, tulee paikkaa hakea viimeistään kaksi
                viikkoa ennen.
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
          ),
          PRESCHOOL: (
            <>
              <P>
                Esiopetukseen osallistutaan vuosi ennen oppivelvollisuuden
                alkamista. Esiopetus on maksutonta. Lukuvuoden 2022–2023
                esiopetukseen ilmoittaudutaan 10.–21.1.2022. Suomen- ja
                ruotsinkielinen esiopetus alkaa 11.8.2022.
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
          ),
          CLUB: (
            <>
              <P>
                Hakuaika syksyllä käynnistyviin kerhoihin on maaliskuussa. Jos
                lapsenne saa kerhopaikan, saatte päätöksen siitä huhti-toukokuun
                aikana. Päätös tehdään yhden toimintakauden ajaksi (elokuusta
                toukokuun loppuun). Päätös kerhopaikasta tulee
                Suomi.fi-palveluun tai postitse, mikäli ette ole ottanut
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
                Kerhotoiminta on maksutonta, eikä siihen osallistuminen vaikuta
                Kelan maksamaan kotihoidontukeen. Jos lapselle sen sijaan on
                myönnetty varhaiskasvatuspaikka tai yksityisen hoidon tuki, ei
                hänelle voida myöntää kerhopaikkaa.
              </P>
              <P fitted={true}>* Tähdellä merkityt tiedot ovat pakollisia</P>
            </>
          )
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
        notYetSent: (
          <P>
            <strong>Hakemusta ei ole vielä lähetetty.</strong> Tarkista antamasi
            tiedot ja lähetä sivun lopussa olevalla Lähetä hakemus-painikkeella.
          </P>
        ),
        notYetSaved: (
          <P>
            <strong>Muutoksia ei ole vielä tallennettu.</strong> Tarkista
            antamasi tiedot ja tallenna sivun lopussa olevalla Tallenna
            muutokset -painikkeella.
          </P>
        ),
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
            info: 'Toisen huoltajan tiedot haetaan automaattisesti väestötietojärjestelmästä.',
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
              'Suomen- ja ruotsinkielinen esiopetus alkaa 11.8.2022. Jos tarvitsette varhaiskasvatusta 1.8.2022 lähtien ennen esiopetuksen alkua, voitte hakea sitä tällä hakemuksella valitsemalla ”Haen myös esiopetukseen liittyvää varhaiskasvatusta”.'
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
          instructions: (
            <>
              Toivottua aloituspäivää on mahdollista muuttaa myöhemmäksi niin
              kauan kuin hakemusta ei ole otettu käsittelyyn. Tämän jälkeen
              toivotun aloituspäivän muutokset tehdään ottamalla yhteyttä
              varhaiskasvatuksen palveluohjaukseen (puh. 09 816 31000).
            </>
          ),
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
            text: (
              <P fitted={true}>
                Mikäli varhaiskasvatuspaikan tarve johtuu äkillisestä
                työllistymisestä tai opiskelusta, tulee paikkaa hakea
                viimeistään <strong>kaksi viikkoa ennen</strong> kuin tarve
                alkaa. Hakemuksen{' '}
                <strong>liitteenä tulee olla työ- tai opiskelutodistus</strong>{' '}
                molemmilta samassa taloudessa asuvilta huoltajilta.
                Suosittelemme toimittamaan liitteen sähköisesti tässä, sillä
                kahden viikon käsittelyaika alkaa siitä, kun olemme
                vastaanottaneet hakemuksen tarvittavine liitteineen. Jos et voi
                lisätä liitteitä hakemukselle sähköisesti, lähetä ne postilla
                osoitteeseen Varhaiskasvatuksen palveluohjaus, PL 3125, 02070
                Espoon kaupunki.
              </P>
            ),
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
          connectedDaycareInfo: (
            <>
              <P>
                Voit hakea lapselle tarvittaessa{' '}
                <strong>
                  esiopetukseen liittyvää varhaiskasvatusta, joka on
                  maksullista, ja jota annetaan esiopetuksen (4 tuntia/päivä)
                  lisäksi
                </strong>{' '}
                aamuisin ja/tai iltapäivisin samassa paikassa kuin esiopetus.
                Jos haluat aloittaa varhaiskasvatuksen myöhemmin kuin esiopetus
                alkaa, kirjoita haluttu aloituspäivämäärä hakemuksen “Muut
                lisätiedot” -kohtaan.
              </P>
              <P>
                Yksityisiin esiopetusyksiköihin haettassa, liittyvä
                varhaiskasvatus haetaan suoraan yksiköstä (pois lukien
                palveluseteliyksiköt), yksiköt informoivat asiakkaita
                hakutavasta. Näissä tapauksissa palveluohjaus muuttaa hakemuksen
                pelkäksi esiopetushakemukseksi.
              </P>
              <P>
                Palveluseteliä haetaan valitsemalla hakutoiveeksi se
                palveluseteliyksikkö, johon halutaan hakea.
              </P>
              <P>
                Saat varhaiskasvatuspaikasta erillisen kirjallisen päätöksen, ja
                päätös tulee{' '}
                <a
                  href="https://www.suomi.fi/viestit"
                  target="_blank"
                  rel="noreferrer"
                >
                  Suomi.fi-viestit
                </a>{' '}
                -palveluun tai postitse, mikäli et ole ottanut Suomi.fi-viestit
                -palvelua käyttöön.
              </P>
            </>
          ),
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
            text: 'Ilta- ja vuorohoito on tarkoitettu lapsille, joiden molemmat vanhemmat ovat vuorotyössä tai opiskelevat pääsääntöisesti iltaisin ja/ viikonloppuisin. Hakemuksen liitteeksi toimitetaan molempien vanhempien osalta työnantajan todistus vuorotyöstä tai opiskelusta johtuvasta ilta- tai vuorohoidon tarpeesta.'
          },
          attachmentsMessage: {
            text: 'Ilta- ja vuorohoito on tarkoitettu lapsille, joiden molemmat vanhemmat ovat vuorotyössä tai opiskelevat pääsääntöisesti iltaisin ja/tai viikonloppuisin. Hakemuksen liitteeksi toimitetaan molempien vanhempien osalta työnantajan todistus vuorotyöstä tai opiskelusta johtuvasta ilta- tai vuorohoidon tarpeesta. Jos et voi lisätä liitteitä hakemukselle sähköisesti, lähetä ne postilla osoitteeseen Varhaiskasvatuksen palveluohjaus, PL 3125, 02070 Espoon kaupunki.',
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
            'Lapsen tuen tarpeella tarkoitetaan sellaisten tukitoimien tarvetta, jotka on osoitettu asiantuntijalausunnoin. Tukitoimet toteutuvat lapsen arjessa osana varhaiskasvatuksen muuta toimintaa. Osa varhaiskasvatuspaikoista on varattu tukea tarvitseville lapsille. Jos lapsellanne on kehityksen tai oppimisen tuen tarvetta, varhaiskasvatuksen erityisopettaja ottaa hakijaan yhteyttä, jotta lapsen tarpeet voidaan ottaa huomioon varhaiskasvatuspaikkaa myönnettäessä.',
          CLUB: 'Lapsen tuen tarpeella tarkoitetaan sellaisten tukitoimien tarvetta, jotka on osoitettu asiantuntijalausunnoin. Tukitoimet toteutuvat lapsen arjessa osana varhaiskasvatuksen muuta toimintaa. Osa varhaiskasvatuspaikoista on varattu tukea tarvitseville lapsille. Jos lapsellanne on kehityksen tai oppimisen tuen tarvetta, varhaiskasvatuksen erityisopettaja ottaa hakijaan yhteyttä, jotta lapsen tarpeet voidaan ottaa huomioon varhaiskasvatuspaikkaa myönnettäessä.',
          PRESCHOOL:
            'Valitse hakemuksesta tämä kohta, jos lapsi tarvitsee kehitykselleen ja/tai oppimiselleen tukea esiopetusvuonna. Tukea toteutetaan lapsen arjessa osana esiopetuksen ja varhaiskasvatuksen muuta toimintaa. Osa esiopetuspaikoista on varattu tukea tarvitseville lapsille. Jos lapsellanne on kehityksen ja/tai oppimisen tuen tarvetta, varhaiskasvatuksen erityisopettaja ottaa hakijaan yhteyttä, jotta lapsen tarpeet voidaan ottaa huomioon esiopetuspaikkaa osoitettaessa.'
        },
        preparatory:
          'Lapsi tarvitsee tukea suomen kielen oppimisessa. Haen myös perusopetukseen valmistavaan opetukseen. Ei koske ruotsinkielistä esiopetusta.',
        preparatoryInfo:
          'Esiopetusikäisten perusopetukseen valmistavaan opetukseen voivat hakeutua maahanmuuttajataustaiset lapset, paluumuuttajalapset, kaksikielisten perheiden lapset (paitsi suomi-ruotsi) ja adoptiolapset, jotka tarvitsevat tukea suomen kielen oppimisessa ennen perusopetukseen siirtymistä. Perusopetukseen valmistavaa opetusta annetaan esiopetuksessa 5h/päivä. Opetus on maksutonta.'
      },
      unitPreference: {
        title: 'Hakutoive',
        siblingBasis: {
          title: 'Haku sisarperusteella',
          info: {
            DAYCARE: (
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
            ),
            PRESCHOOL: (
              <>
                <P>Esioppilaalla on sisarusperuste:</P>
                <ol type="a">
                  <li>
                    Oman palvelualueen päiväkotiin, jossa esioppilaalla on
                    sisarus, jolla on päätöksentekohetkellä ja tulevana
                    esiopetusvuonna paikka esiopetuspäiväkodissa.
                  </li>
                  <li>
                    Kunnan osoittamaan lähikouluun, jota esioppilaan sisarus käy
                    tulevana lukuvuonna.
                  </li>
                </ol>
                <P>
                  Huoltaja voi valita, käyttääkö hän sisarusperustetta kohdan a
                  vai b mukaisesti, jos esioppilaalla on sisarusperuste
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
            ),
            CLUB: (
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
          },
          checkbox: {
            DAYCARE:
              'Haen ensisijaisesti samaan paikkaan, jossa lapsen sisarus on jo varhaiskasvatuksessa.',
            PRESCHOOL:
              'Haen ensisijaisesti samaan paikkaan, jossa lapsen sisarus on.',
            CLUB: 'Haen ensisijaisesti paikkaa samasta kerhoryhmästä, jossa lapsen sisarus on.'
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
            DAYCARE: (
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
            ),
            PRESCHOOL: (
              <>
                <P>
                  Voit hakea 1-3 paikka paikkaa toivomassasi järjestyksessä.
                  Hakutoiveet eivät takaa paikkaa toivotussa yksikössä, mutta
                  mahdollisuus toivotun paikan saamiseen kasvaa antamalla
                  useamman vaihtoehdon.
                </P>
                <P>
                  Näet eri yksiköiden sijainnin valitsemalla ‘Yksiköt kartalla’.
                </P>
                <P>
                  Palveluseteliä haetaan valitsemalla hakutoiveeksi se
                  palveluseteliyksikkö, johon halutaan hakea.
                  Palveluseteliyksikköön haettaessa myös yksikön esimies saa
                  tiedon hakemuksesta.
                </P>
              </>
            ),
            CLUB: (
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
          },
          mapLink: 'Yksiköt kartalla',
          serviceVoucherLink:
            'https://www.espoo.fi/fi/kasvatus-ja-opetus/varhaiskasvatus/varhaiskasvatuksen-palveluseteli#section-6228',
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
            info: 'Valitse 1-3 varhaiskasvatusyksikköä ja järjestä ne toivomaasi järjestykseen. Voit muuttaa järjestystä nuolien avulla.',
            fi: 'suomenkielinen',
            sv: 'ruotsinkielinen',
            en: 'englanninkielinen',
            moveUp: 'Siirrä ylöspäin',
            moveDown: 'Siirrä alaspäin',
            remove: 'Poista hakutoive'
          }
        }
      },
      fee: {
        title: 'Varhaiskasvatusmaksu',
        info: {
          DAYCARE: (
            <P>
              Kunnallisen varhaiskasvatuksen asiakasmaksut ja palvelusetelin
              omavastuuosuus määräytyvät prosenttiosuutena perheen
              bruttotuloista. Maksut vaihtelevat perheen koon ja tulojen sekä
              varhaiskasvatusajan mukaan maksuttomasta varhaiskasvatuksesta
              enintään 288 euron kuukausimaksuun lasta kohden.
              Palveluseteliyksiköissä voidaan kuitenkin periä 0-50€/kk/lapsi
              lisämaksu. Perhe toimittaa tuloselvityksen bruttotuloistaan
              tuloselvityslomakkeella, viimeistään kahden viikon kuluessa siitä,
              kun lapsi on aloittanut varhaiskasvatuksessa.
            </P>
          ),
          PRESCHOOL: (
            <P>
              Esiopetus on maksutonta, mutta siihen liittyvä varhaiskasvatus on
              maksullista. Jos lapsi osallistuu liittyvään varhaiskasvatukseen,
              perhe toimittaa tuloselvityksen bruttotuloistaan
              tuloselvityslomakkeella viimeistään kahden viikon kuluessa siitä,
              kun lapsi on aloittanut varhaiskasvatuksessa.
            </P>
          ),
          CLUB: <P></P>
        },
        emphasis: (
          <strong>
            Tuloselvitystä ei tarvita, jos perhe suostuu korkeimpaan maksuun.
          </strong>
        ),
        checkbox:
          'Annan suostumuksen korkeimpaan maksuun. Suostumus on voimassa toistaiseksi, kunnes toisin ilmoitan.',
        links: (
          <P>
            Lisätietoa varhaiskasvatuksen maksuista, palvelusetelin lisämaksusta
            ja tuloselvityslomakkeen löydät täältä:
            <br />
            <a
              href="https://www.espoo.fi/fi/kasvatus-ja-opetus/varhaiskasvatus/maksut-varhaiskasvatuksessa"
              target="_blank"
              rel="noopener noreferrer"
            >
              Maksut varhaiskasvatuksessa
            </a>
          </P>
        )
      },
      additionalDetails: {
        title: 'Muut lisätiedot',
        otherInfoLabel: 'Hakuun liittyvät lisätiedot',
        otherInfoPlaceholder:
          'Voit halutessasi antaa hakuun liittyvää tarkempaa lisätietoa',
        dietLabel: 'Erityisruokavalio',
        dietPlaceholder: 'Voit halutessasi ilmoittaa lapsen erityisruokavalion',
        dietInfo: (
          <>
            Osaan erityisruokavalioista tarvitaan erikseen lääkärintodistus,
            joka toimitetaan varhaiskasvatuspaikkaan. Poikkeuksena
            vähälaktoosinen tai laktoositon ruokavalio, uskonnollisiin syihin
            perustuva ruokavalio tai kasvisruokavalio (lakto-ovo).
          </>
        ),
        allergiesLabel: 'Allergiat',
        allergiesPlaceholder: 'Voit halutessasi ilmoittaa lapsen allergiat',
        allergiesInfo:
          'Allergiatieto tarvitaan lähinnä perhepäivähoitoa haettaessa.'
      },
      contactInfo: {
        title: 'Henkilötiedot',
        familyInfo: (
          <P>
            Ilmoita hakemuksella kaikki samassa taloudessa asuvat aikuiset ja
            lapset.
          </P>
        ),
        info: (
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
            että huoltajalle. Virallisena osoitetietoa pidetään vasta, kun se on
            päivittynyt väestötietojärjestelmään. Päätökset esiopetus- ja
            varhaiskasvatuspaikoista toimitetaan automaattisesti myös eri
            osoitteessa asuvalle väestötiedoista löytyvälle huoltajalle.
          </P>
        ),
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
        text: 'Hakemus on tallennettu keskeneräisenä. Huom! Keskeneräistä hakemusta säilytetään palvelussa yhden kuukauden ajan viimeisimmästä tallennuksesta.',
        ok: 'Selvä!'
      },
      sentInfo: {
        title: 'Hakemus on lähetetty',
        text: 'Halutessasi voit tehdä hakemukseen muutoksia niin kauan kuin hakemusta ei olla otettu käsittelyyn.',
        ok: 'Selvä!'
      },
      updateInfo: {
        title: 'Muutokset hakemukseen on tallennettu',
        text: 'Halutessasi voit tehdä lisää muutoksia niin kauan kuin hakemusta ei olla otettu käsittelyyn.',
        ok: 'Selvä!'
      }
    }
  },
  decisions: {
    title: 'Päätökset',
    summary: (
      <P width="800px">
        Tälle sivulle saapuvat lapsen varhaiskasvatus-, esiopetus- ja
        kerhohakemuksiin liittyvät päätökset. Uuden päätöksen saapuessa{' '}
        <strong>sinun tulee kahden viikon sisällä vastata</strong>, hyväksytkö
        vai hylkäätkö lapselle tarjotun paikan.
      </P>
    ),
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
          text: 'Toinen päätös odottaa edelleen vastaustasi. Haluatko  palata listalle vastaamatta?',
          resolveLabel: 'Palaa vastaamatta',
          rejectLabel: 'Jatka vastaamista'
        },
        doubleRejectWarning: {
          title: 'Haluatko hylätä paikan?',
          text: 'Olet hylkäämässä tarjotun esiopetus / valmistavan paikan. Liittyvän varhaiskasvatuksen paikka merkitään samalla hylätyksi.',
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
    summary: (
      <P width="800px">
        Lapsen huoltaja voi tehdä lapselle hakemuksen varhaiskasvatukseen ja
        kerhoon tai ilmoittaa lapsen esiopetukseen. Samalla hakemuksella voi
        hakea myös varhaiskasvatuksen palveluseteliä, hakemalla
        varhaiskasvatuspaikkaa palveluseteliyksiköstä. Huoltajan lasten tiedot
        haetaan tähän näkymään automaattisesti Väestötietojärjestelmästä.
      </P>
    ),
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
      EXTENSION_MISSING: 'Tiedostopääte puuttuu',
      EXTENSION_INVALID: 'Virheellinen tiedostopääte',
      INVALID_CONTENT_TYPE: 'Virheellinen tiedostomuoto',
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
      'Tiedosto ei ole juuri nyt avattavissa. Kokeile hetken kuluttua uudelleen.',
    download: 'Lataa'
  },
  personalDetails: {
    title: 'Omat tiedot',
    description: (
      <P>
        Täällä voit tarkistaa ja täydentää omat henkilö- ja yhteystietosi.
        Nimesi ja osoitteesi haetaan väestötietojärjestelmästä, ja mikäli ne
        muuttuvat, sinun tulee tehdä ilmoitus maistraattiin.
      </P>
    ),
    noEmailAlert:
      'Sähköpostiosoitteesi puuttuu. Ole hyvä ja täydennä se alle, jotta pystyt vastaanottamaan eVakasta lähetetyt ilmoitukset.',
    personalInfo: 'Henkilötiedot',
    name: 'Nimi',
    preferredName: 'Kutsumanimi',
    contactInfo: 'Yhteystiedot',
    address: 'Osoite',
    phone: 'Puhelinnumero',
    backupPhone: 'Varapuhelinnumero',
    backupPhonePlaceholder: 'Esim. työpuhelin',
    email: 'Sähköpostiosoite',
    emailMissing: 'Sähköpostiosoite puuttuu',
    noEmail: 'Minulla ei ole sähköpostiosoitetta',
    emailInfo:
      'Sähköpostiosoite tarvitaan, jotta voimme lähettää sinulle ilmoitukset uusista viesteistä, läsnäoloaikojen varaamisesta sekä muista lapsen varhaiskasvatukseen liittyvistä asioista.'
  },
  income: {
    title: 'Tulotiedot',
    description: (
      <>
        <p>
          Tällä sivulla voit lähettää selvitykset varhaiskasvatusmaksuun
          vaikuttavista tuloistasi. Voit myös tarkastella palauttamiasi
          tuloselvityksiä ja muokata tai poistaa niitä, kunnes viranomainen on
          käsitellyt tiedot. Lomakkeen käsittelyn jälkeen voit päivittää
          tulotietojasi toimittamalla uuden lomakkeen.
        </p>
        <p>
          Kunnallisen varhaiskasvatuksen asiakasmaksut määräytyvät
          prosenttiosuutena perheen bruttotuloista. Maksut vaihtelevat perheen
          koon ja tulojen sekä varhaiskasvatusajan mukaan. Tarkista{' '}
          <a
            target="_blank"
            rel="noreferrer"
            href="https://www.espoo.fi/fi/kasvatus-ja-opetus/varhaiskasvatus/maksut-varhaiskasvatuksessa"
          >
            tästä
          </a>{' '}
          tarvitseeko sinun toimittaa tuloselvitystä, vai kuuluuko perheenne
          automaattisesti korkeimman varhaiskasvatusmaksun piiriin.
        </p>
        <p>
          Lisätietoja maksuista:{' '}
          <a href="https://www.espoo.fi/fi/kasvatus-ja-opetus/varhaiskasvatus/maksut-varhaiskasvatuksessa">
            Maksut varhaiskasvatuksessa
          </a>
        </p>
      </>
    ),
    formTitle: 'Tulotietojen ilmoitus',
    formDescription: (
      <>
        <P>
          Tuloselvitys liitteineen palautetaan kahden viikon kuluessa
          varhaiskasvatuksen aloittamisesta. Maksu voidaan määrätä
          puutteellisilla tulotiedoilla korkeimpaan maksuun. Puutteellisia
          tulotietoja ei korjata takautuvasti oikaisuvaatimusajan jälkeen.
        </P>
        <P>
          Asiakasmaksu peritään päätöksen mukaisesta varhaiskasvatuksen
          alkamispäivästä lähtien.
        </P>
        <P>
          Asiakkaan on viipymättä ilmoitettava tulojen ja perhekoon muutoksista
          varhaiskasvatuksen asiakasmaksuyksikköön. Viranomainen on tarvittaessa
          oikeutettu perimään varhaiskasvatusmaksuja myös takautuvasti.
        </P>
        <P>
          <strong>Huomioitavaa:</strong>
        </P>
        <Gap size="xs" />
        <UnorderedList>
          <li>
            Jos tulosi ylittävät perhekoon mukaisen tulorajan, hyväksy korkein
            varhaiskasvatusmaksu. Tällöin sinun ei tarvitse selvittää tulojasi
            lainkaan.
          </li>
          <li>
            Jos perheeseesi kuuluu toinen aikuinen, myös hänen on toimitettava
            tuloselvitys tunnistautumalla eVakaan omilla henkilötiedoillaan ja
            täyttämällä tämä lomake.
          </li>
        </UnorderedList>
        <P>
          Katso voimassaolevat tulorajat{' '}
          <a
            target="_blank"
            rel="noreferrer"
            href="https://static.espoo.fi/cdn/ff/uCz08Q1RDj-eVJJvhztJC6oTCmF_4OGQOtOiDUwT4II/1621487195/public/2021-05/Asiakastiedote%20varhaiskasvatusmaksuista%201.8.2021%20%283%29.pdf"
          >
            tästä
          </a>
          .
        </P>
        <P>* Tähdellä merkityt tiedot ovat pakollisia</P>
      </>
    ),
    confidential: (
      <P>
        <strong>Salassapidettävä</strong>
        <br />
        (JulkL 24.1 §:n 23 kohta)
      </P>
    ),
    addNew: 'Uusi tuloselvitys',
    incomeInfo: 'Tulotiedot',
    incomesRegisterConsent:
      'Hyväksyn, että tuloihini liittyviä tietoja tarkastellaan tulorekisteristä, sekä Kelasta tarvittaessa',
    incomeType: {
      description: (
        <>
          Jos olet yrittäjä, mutta sinulla on myös muita tuloja, valitse sekä{' '}
          <strong>Yrittäjän tulotiedot</strong>, että{' '}
          <strong>Asiakasmaksun määritteleminen bruttotulojen mukaan</strong>.
        </>
      ),
      startDate: 'Voimassa alkaen',
      endDate: 'Voimassaolo päättyy',
      title: 'Asiakasmaksun perusteet',
      agreeToHighestFee: 'Suostun korkeimpaan varhaiskasvatusmaksuun',
      highestFeeInfo:
        'Suostun maksamaan varhaiskasvatusajan ja kulloinkin voimassa olevan asiakasmaksulain ja kaupungin hallituksen päätösten mukaista korkeinta varhaiskasvatusmaksua, joka on voimassa toistaiseksi siihen saakka, kunnes toisin ilmoitan tai kunnes lapseni varhaiskasvatus päättyy. (Tulotietoja ei tarvitse toimittaa)',
      grossIncome: 'Maksun määritteleminen bruttotulojen mukaan',
      entrepreneurIncome: 'Yrittäjän tulotiedot'
    },
    grossIncome: {
      title: 'Bruttotulotietojen täyttäminen',
      description: (
        <>
          <P noMargin>
            Valitse alta haluatko toimittaa tulotietosi liitteinä, vai katsooko
            viranomainen tietosi suoraan tulorekisteristä sekä Kelasta
            tarvittaessa.
          </P>
          <P>
            Jos olet aloittanut tai aloittamassa uudessa työssä, lähetä aina
            liitteenä työsopimus, josta ilmenee palkka, koska tällöin tulosi
            näkyvät tulorekisterissä viiveellä.
          </P>
        </>
      ),
      incomeSource: 'Tulotietojen toimitus',
      provideAttachments:
        'Toimitan tiedot liitteinä, ja tietoni saa tarkastaa Kelasta tarvittaessa',
      estimate: 'Arvio bruttotuloistani',
      estimatedMonthlyIncome: 'Keskimääräiset tulot sisältäen lomarahat, €/kk',
      otherIncome: 'Muut tulot',
      otherIncomeDescription:
        'Jos sinulla on muita tuloja, on niistä toimitettavana tositteet liitteinä. Listan tarvittavista liitteistä löydät lomakkeen alaosasta kohdasta: Tuloihin ja varhaiskasvatusmaksuihin liittyvät liitteet.',
      choosePlaceholder: 'Valitse',
      otherIncomeTypes: {
        PENSION: 'Eläke',
        ADULT_EDUCATION_ALLOWANCE: 'Aikuiskoulutustuki',
        SICKNESS_ALLOWANCE: 'Sairauspäiväraha',
        PARENTAL_ALLOWANCE: 'Äitiys- ja vanhempainraha',
        HOME_CARE_ALLOWANCE: 'Lasten kotihoidontuki',
        FLEXIBLE_AND_PARTIAL_HOME_CARE_ALLOWANCE:
          'Joustava tai osittainen hoitoraha',
        ALIMONY: 'Elatusapu tai -tuki',
        INTEREST_AND_INVESTMENT_INCOME: 'Korko- ja osinkotulot',
        RENTAL_INCOME: 'Vuokratulot',
        UNEMPLOYMENT_ALLOWANCE: 'Työttömyyspäiväraha',
        LABOUR_MARKET_SUBSIDY: 'Työmarkkinatuki',
        ADJUSTED_DAILY_ALLOWANCE: 'Soviteltu päiväraha',
        JOB_ALTERNATION_COMPENSATION: 'Vuorotteluvapaakorvaus',
        REWARD_OR_BONUS: 'Palkkio tai bonus',
        RELATIVE_CARE_SUPPORT: 'Omaishoidontuki',
        BASIC_INCOME: 'Perustulo',
        FOREST_INCOME: 'Metsätulo',
        FAMILY_CARE_COMPENSATION: 'Perhehoidon palkkiot',
        REHABILITATION: 'Kuntoutustuki tai kuntoutusraha',
        EDUCATION_ALLOWANCE: 'Koulutuspäiväraha',
        GRANT: 'Apuraha',
        APPRENTICESHIP_SALARY: 'Palkkatulo oppisopimuskoulutuksesta',
        ACCIDENT_INSURANCE_COMPENSATION: 'Korvaus tapaturmavakuutuksesta',
        OTHER_INCOME: 'Muut tulot'
      },
      otherIncomeInfoLabel: 'Arviot muista tuloista',
      otherIncomeInfoDescription:
        'Kirjoita tähän arviot muiden tulojen määristä €/kk, esim. "Vuokratulot 150, lasten kotihoidontuki 300"'
    },
    entrepreneurIncome: {
      title: 'Yrittäjän tulotietojen täyttäminen',
      description: (
        <>
          Tällä lomakkeella voit tarvittaessa täyttää tiedot myös useammalle
          yritykselle valitsemalla kaikkia yrityksiäsi koskevat kohdat. Toimita
          tarkemmat yrityskohtaiset tiedot liitteinä.
          <br />
          Listan tarvittavista liitteistä löydät lomakkeen alaosasta kohdasta
          “Tuloihin ja varhaiskasvatusmaksuihin liittyvät liitteet”.
        </>
      ),
      fullTimeLabel: 'Onko yritystoiminta päätoimista vai sivutoimista?',
      fullTime: 'Päätoimista',
      partTime: 'Sivutoimista',
      startOfEntrepreneurship: 'Yrittäjyys alkanut',
      spouseWorksInCompany: 'Työskenteleekö puoliso yrityksessä?',
      yes: 'Kyllä',
      no: 'Ei',
      startupGrantLabel: 'Saako yrityksesi starttirahaa?',
      startupGrant:
        'Yritykseni saa starttirahaa. Toimitan starttirahapäätöksen liitteenä.',
      checkupLabel: 'Tietojen tarkastus',
      checkupConsent:
        'Hyväksyn, että tuloihini liittyviä tietoja tarkastellaan tarvittaessa tulorekisteristä sekä Kelasta.',
      companyInfo: 'Yrityksen tiedot',
      companyForm: 'Yrityksen toimintamuoto',
      selfEmployed: 'Toiminimi',
      limitedCompany: 'Osakeyhtiö',
      partnership: 'Avoin yhtiö tai kommandiittiyhtiö',
      lightEntrepreneur: 'Kevytyrittäjyys',
      lightEntrepreneurInfo:
        'Maksutositteet palkoista ja työkorvauksista tulee toimittaa liitteinä.',
      partnershipInfo:
        'Tuloslaskelma ja tase sekä kirjanpitäjän selvitys palkasta ja luontaiseduista tulee toimittaa liitteinä.'
    },
    selfEmployed: {
      info: 'Jos yritystoiminta on jatkunut yli 3 kuukautta, on yrityksen viimeisin tulos- ja taselaskelman tai veropäätös toimitettava. Jos yritystoiminta on kestänyt alle 3 kuukautta etkä toimita liitteitä nyt, ne on toimitettava viimeistään 3 kuukauden kuluttua täyttämällä tämä tuloselvityslomake uudestaan.',
      attachments:
        'Toimitan liitteinä yrityksen viimeisimmän tulos- ja taselaskelman tai veropäätöksen.',
      estimatedIncome: 'Täytän arvion keskimääräisistä kuukausitulostani.',
      estimatedMonthlyIncome: 'Keskimääräiset tulot €/kk',
      timeRange: 'Aikavälillä'
    },
    limitedCompany: {
      info: (
        <>
          <strong>
            Kirjanpitäjän selvitys luontoiseduista ja osingoista tulee toimittaa
            liitteenä.
          </strong>{' '}
          Valitse alta sopiva tapa muiden tietojen toimittamiseen.
        </>
      ),
      incomesRegister:
        'Tuloni voi tarkastaa tulorekisteristä sekä tarvittaessa Kelasta.',
      attachments:
        'Toimitan tositteet tuloistani liitteenä ja hyväksyn, että tuloihini liittyviä tietoja tarkastellaan tarvittaessa Kelasta.'
    },
    accounting: {
      title: 'Kirjanpitäjän yhteystiedot',
      description:
        'Kirjanpitäjän yhteystiedot tarvitaan jos toimit osakeyhtiössä, kommandiittiyhtiössä tai avoimessa yhtiössä.',
      accountant: 'Kirjanpitäjä',
      accountantPlaceholder: 'Kirjanpitäjän nimi / yhtiön nimi',
      email: 'Sähköpostiosoite',
      emailPlaceholder: 'Sähköposti',
      address: 'Postiosoite',
      addressPlaceholder: 'Katuosoite, postinumero, toimipaikka',
      phone: 'Puhelinnumero',
      phonePlaceholder: 'Puhelinnumero'
    },
    moreInfo: {
      title: 'Muita maksuun liittyviä tietoja',
      studentLabel: 'Oletko opiskelija?',
      student: 'Olen opiskelija.',
      studentInfo:
        'Opiskelijat toimittavat oppilaitoksesta opiskelutodistuksen tai päätöksen työttömyyskassan opintoetuudesta / työllisyysrahaston koulutustuesta.',
      deductions: 'Vähennykset',
      alimony:
        'Maksan elatusmaksuja. Toimitan kopion maksutositteesta liitteenä.',
      otherInfoLabel: 'Lisätietoja tulotietoihin liittyen'
    },
    attachments: {
      title: 'Tuloihin ja varhaiskasvatusmaksuihin liittyvät liitteet',
      description:
        'Tässä voit lähettää sähköisesti sinulta pyydetyt tuloihin tai varhaiskasvatusmaksuihin liittyvät liitteet, kuten palkkakuitit tai Kelan todistuksen yksityisen hoidon tuesta. Huom! Tuloihin liittyviä liitteitä ei tarvita, jos perheenne on suostunut korkeimpaan maksuun.',
      required: {
        title: 'Tarvittavat liitteet'
      },
      attachmentNames: {
        PENSION: 'Päätös eläkkeestä',
        ADULT_EDUCATION_ALLOWANCE: 'Päätös aikuiskoulutustuesta',
        SICKNESS_ALLOWANCE: 'Päätös sairauspäivärahasta',
        PARENTAL_ALLOWANCE: 'Päätös äitiys- tai vanhempainrahasta',
        HOME_CARE_ALLOWANCE: 'Päätös kotihoidontuesta',
        FLEXIBLE_AND_PARTIAL_HOME_CARE_ALLOWANCE: 'Päätös hoitorahasta',
        ALIMONY: 'Elatussopimus tai päätös elatustuesta',
        UNEMPLOYMENT_ALLOWANCE: 'Päätös työttömyyspäivärahasta',
        LABOUR_MARKET_SUBSIDY: 'Päätös työmarkkinatuesta',
        ADJUSTED_DAILY_ALLOWANCE: 'Päätös päivärahasta',
        JOB_ALTERNATION_COMPENSATION: 'Tosite vuorotteluvapaakorvaus',
        REWARD_OR_BONUS: 'Palkkatosite bonuksesta tai/ja palkkiosta',
        RELATIVE_CARE_SUPPORT: 'Päätös omaishoidontuesta',
        BASIC_INCOME: 'Päätös perustulosta',
        FOREST_INCOME: 'Tosite metsätulosta',
        FAMILY_CARE_COMPENSATION: 'Tositteet perhehoidon palkkioista',
        REHABILITATION: 'Päätös kuntoutustuesta tai kuntoutusrahasta',
        EDUCATION_ALLOWANCE: 'Päätös koulutuspäivärahasta',
        GRANT: 'Tosite apurahasta',
        APPRENTICESHIP_SALARY: 'Tosite oppisopimuskoulutuksen palkkatuloista',
        ACCIDENT_INSURANCE_COMPENSATION:
          'Tosite tapaturmavakuutuksen korvauksesta',
        OTHER_INCOME: 'Liitteet muista tuloista',
        ALIMONY_PAYOUT: 'Maksutosite elatusmaksuista',
        INTEREST_AND_INVESTMENT_INCOME: 'Tositteet korko- ja osinkotuloista',
        RENTAL_INCOME: 'Tositteet vuokratuloista ja vastikkeesta',
        PAYSLIP: 'Viimeisin palkkakuitti',
        STARTUP_GRANT: 'Starttirahapäätös',
        ACCOUNTANT_REPORT: 'Kirjanpitäjän selvitys palkasta ja luontoiseduista',
        ACCOUNTANT_REPORT_LLC:
          'Kirjanpitäjän selvitys luontoiseduista ja osingoista',
        PROFIT_AND_LOSS_STATEMENT: 'Tuloslaskelma ja tase',
        SALARY: 'Maksutositteet palkoista ja työkorvauksista',
        PROOF_OF_STUDIES:
          'Opiskelutodistus tai päätös työttömyyskassan opintoetuudesta / työllisyysrahaston koulutustuesta'
      }
    },
    assure: 'Vakuutan antamani tiedot oikeiksi.',
    errors: {
      invalidForm:
        'Lomakkeelta puuttuu joitakin tarvittavia tietoja tai tiedot ovat virheellisiä. Ole hyvä ja tarkista täyttämäsi tiedot.',
      choose: 'Valitse vaihtoehto',
      chooseAtLeastOne: 'Valitse vähintään yksi vaihtoehto',
      deleteFailed: 'Tuloselvitystä ei voitu poistaa'
    },
    table: {
      title: 'Tuloselvitykset',
      incomeStatementForm: 'Tuloselvityslomake',
      startDate: 'Voimassa alkaen',
      endDate: 'Voimassa asti',
      handled: 'Käsitelty',
      openIncomeStatement: 'Avaa lomake',
      deleteConfirm: 'Haluatko poistaa tuloselvityksen?',
      deleteDescription:
        'Haluatko varmasti poistaa toimittamasi tuloselvityksen? Kaikki poistettavan lomakkeen tiedot menetetään.'
    },
    view: {
      title: 'Tuloselvityslomake',
      startDate: 'Voimassa alkaen',
      feeBasis: 'Asiakasmaksun peruste',

      grossTitle: 'Bruttotulot',
      incomeSource: 'Tietojen toimitus',
      incomesRegister:
        'Hyväksyn, että tuloihini liittyviä tietoja tarkastellaan Kelasta sekä tulorekisteristä.',
      attachmentsAndKela:
        'Toimitan tiedot liitteinä ja tietoni saa tarkastaa Kelasta',
      grossEstimatedIncome: 'Arvio bruttotuloista',
      otherIncome: 'Muut tulot',
      otherIncomeInfo: 'Arviot muista tuloista',

      entrepreneurTitle: 'Yrittäjän tulotiedot',
      fullTimeLabel: 'Onko yritystoiminta päätoimista vai sivutoimista',
      fullTime: 'Päätoimista',
      partTime: 'Sivutoimista',
      startOfEntrepreneurship: 'Yrittäjyys alkanut',
      spouseWorksInCompany: 'Työskenteleekö puoliso yrityksessä',
      startupGrant: 'Starttiraha',
      checkupConsentLabel: 'Tietojen tarkastus',
      checkupConsent:
        'Hyväksyn, että tuloihini liittyviä tietoja tarkastellaan tarvittaessa tulorekisteristä sekä Kelasta.',
      companyInfoTitle: 'Yrityksen tiedot',
      companyType: 'Toimintamuoto',
      selfEmployed: 'Toiminimi',
      selfEmployedAttachments:
        'Toimitan liitteinä yrityksen viimeisimmän tulos- ja taselaskelman tai veropäätöksen.',
      selfEmployedEstimation: 'Arvio keskimääräisistä kuukausituloista',
      limitedCompany: 'Osakeyhtiö',
      limitedCompanyIncomesRegister:
        'Tuloni voi tarkastaa suoraan tulorekisteristä sekä Kelasta tarvittaessa.',
      limitedCompanyAttachments:
        'Toimitan tositteet tuloistani liitteenä ja hyväksyn, että tuloihini liittyviä tietoja tarkastellaan Kelasta.',
      partnership: 'Avoin yhtiö tai kommandiittiyhtiö',
      lightEntrepreneur: 'Kevytyrittäjyys',
      attachments: 'Liitteet',

      estimatedMonthlyIncome: 'Keskimääräiset tulot €/kk',
      timeRange: 'Aikavälillä',

      accountantTitle: 'Kirjanpitäjän tiedot',
      accountant: 'Kirjanpitäjä',
      email: 'Sähköpostiosoite',
      phone: 'Puhelinnumero',
      address: 'Postiosoite',

      otherInfoTitle: 'Muita tuloihin liittyviä tietoja',
      student: 'Opiskelija',
      alimonyPayer: 'Maksaa elatusmaksuja',
      otherInfo: 'Lisätietoja tulotietoihin liittyen',

      citizenAttachments: {
        title: 'Tuloihin ja varhaiskasvatusmaksuihin liittyvät liitteet',
        noAttachments: 'Ei liitteitä'
      },

      employeeAttachments: {
        title: 'Lisää liitteitä',
        description:
          'Tässä voit lisätä asiakkaan paperisena toimittamia liitteitä eVakan kautta palautettuun tuloselvitykseen.'
      },

      statementTypes: {
        HIGHEST_FEE: 'Suostumus korkeimpaan maksuluokkaan',
        INCOME: 'Huoltajan toimittamat tulotiedot'
      }
    }
  },
  validationErrors: {
    required: 'Pakollinen tieto',
    requiredSelection: 'Valinta puuttuu',
    format: 'Anna oikeassa muodossa',
    ssn: 'Virheellinen henkilötunnus',
    phone: 'Virheellinen numero',
    email: 'Virheellinen sähköpostiosoite',
    validDate: 'Anna muodossa pp.kk.vvvv',
    dateTooEarly: 'Valitse myöhäisempi päivä',
    dateTooLate: 'Valitse aikaisempi päivä',
    preferredStartDate: 'Aloituspäivä ei ole sallittu',
    timeFormat: 'Tarkista',
    timeRequired: 'Pakollinen',
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
  },
  pedagogicalDocuments: {
    title: 'Kasvu ja oppiminen',
    description:
      'Tälle sivulle kootaan lapsen kasvuun, oppimiseen ja päiväkotiarkeen liittyviä kuvia ja muita dokumentteja.',
    table: {
      date: 'Päivämäärä',
      child: 'Lapsi',
      document: 'Dokumentti',
      description: 'Kuvaus'
    },
    toggleExpandText: 'Näytä tai piilota teksti'
  },
  placement: {
    type: {
      CLUB: 'Kerho',
      DAYCARE: 'Varhaiskasvatus',
      FIVE_YEARS_OLD_DAYCARE: '5-vuotiaiden varhaiskasvatus',
      PRESCHOOL_WITH_DAYCARE: 'Esiopetus ja liittyvä varhaiskasvatus',
      PREPARATORY_WITH_DAYCARE: 'Valmistava opetus ja liittyvä varhaiskasvatus',
      DAYCARE_PART_TIME: 'Osapäiväinen varhaiskasvatus',
      DAYCARE_FIVE_YEAR_OLDS: '5-vuotiaiden varhaiskasvatus',
      DAYCARE_PART_TIME_FIVE_YEAR_OLDS:
        '5-vuotiaiden osapäiväinen varhaiskasvatus',
      PRESCHOOL: 'Esiopetus',
      PREPARATORY: 'Valmistava opetus',
      PREPARATORY_DAYCARE: 'Valmistava opetus ja liittyvä varhaiskasvatus',
      PRESCHOOL_DAYCARE: 'Esiopetus ja liittyvä varhaiskasvatus',
      TEMPORARY_DAYCARE: 'Tilapäinen kokopäiväinen varhaiskasvatus',
      TEMPORARY_DAYCARE_PART_DAY: 'Tilapäinen osapäiväinen varhaiskasvatus',
      SCHOOL_SHIFT_CARE: 'Koululaisten vuorohoito'
    }
  },
  reloadNotification: {
    title: 'Uusi versio eVakasta saatavilla',
    buttonText: 'Lataa sivu uudelleen'
  },
  children: {
    title: 'Lapset',
    pageDescription:
      'Tällä sivulla näet lastesi varhaiskasvatukseen tai esiopetukseen liittyvät yleiset tiedot.',
    noChildren: 'Ei lapsia',
    placementTermination: {
      title: 'Paikan irtisanominen',
      description:
        'Irtisanoessasi paikkaa huomaathan, että mahdollinen siirtohakemus poistuu viimeisen läsnäolopäivän jälkeen. Jos tarvitset lapsellesi myöhemmin paikan, sinun tulee hakea sitä uudella hakemuksella.',
      terminatedPlacements: 'Olet irtisanonut paikan',
      invoicedDaycare: 'Maksullinen varhaiskasvatus',
      until: (date: string) => `voimassa ${date}`,
      choosePlacement: 'Valitse paikka, jonka haluat irtisanoa',
      lastDayInfo:
        'Viimeinen päivä, jolloin lapsesi tarvitsee paikkaa. Paikka irtisanotaan päättymään tähän päivään.',
      lastDayOfPresence: 'Viimeinen läsnäolopäivä',
      terminate: 'Irtisano paikka'
    }
  }
}
