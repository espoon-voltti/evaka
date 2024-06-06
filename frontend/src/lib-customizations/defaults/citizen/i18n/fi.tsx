// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import ExternalLink from 'lib-components/atoms/ExternalLink'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import type { Translations as ComponentTranslations } from 'lib-components/i18n'
import { H1, H2, H3, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import components from '../../components/i18n/fi'

const componentTranslations: ComponentTranslations = {
  ...components,
  messageReplyEditor: {
    ...components.messageReplyEditor,
    messagePlaceholder:
      'Viestin sisältö... Huom! Älä kirjoita tähän arkaluontoisia asioita.',
    messagePlaceholderSensitiveThread: 'Viestin sisältö...'
  }
}

const yes = 'Kyllä'
const no = 'Ei'

export default {
  common: {
    title: 'Varhaiskasvatus',
    cancel: 'Peruuta',
    return: 'Palaa',
    download: 'Lataa',
    print: 'Tulosta',
    ok: 'Ok',
    save: 'Tallenna',
    discard: 'Älä tallenna',
    saveConfirmation: 'Haluatko tallentaa muutokset?',
    confirm: 'Vahvista',
    delete: 'Poista',
    edit: 'Muokkaa',
    add: 'Lisää',
    show: 'Näytä',
    hide: 'Piilota',
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
        sv: 'ruotsinkielinen',
        en: 'englanninkielinen'
      },
      languagesShort: {
        fi: 'suomi',
        sv: 'ruotsi',
        en: 'englanti'
      }
    },
    openExpandingInfo: 'Avaa lisätietokenttä',
    errors: {
      genericGetError: 'Tietojen hakeminen ei onnistunut'
    },
    datetime: {
      dayShort: 'pv',
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
    },
    closeModal: 'Sulje ponnahdusikkuna',
    close: 'Sulje',
    duplicatedChild: {
      identifier: {
        DAYCARE: {
          short: 'VAKA',
          long: 'Varhaiskasvatus'
        },
        PRESCHOOL: {
          short: 'EO',
          long: 'Esiopetus'
        }
      }
    },
    tense: {
      past: 'Päättynyt',
      present: 'Voimassa',
      future: 'Tuleva'
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
      children: 'Lapset',
      subNavigationMenu: 'Valikko',
      messageCount: (n: number) =>
        n > 1 ? `${n} uutta viestiä` : `${n} uusi viesti`
    },
    lang: {
      fi: 'Suomeksi',
      sv: 'På svenska',
      en: 'In English'
    },
    langMobile: {
      fi: 'Suomeksi',
      sv: 'Svenska',
      en: 'English'
    },
    login: 'Kirjaudu sisään',
    logout: 'Kirjaudu ulos',
    openMenu: 'Avaa valikko',
    closeMenu: 'Sulje valikko',
    goToHomepage: 'Siirry etusivulle',
    notifications: 'ilmoitusta',
    attention: 'Huomio',
    requiresStrongAuth: 'vaatii vahvan tunnistautumisen'
  },
  footer: {
    cityLabel: '© Espoon kaupunki',
    privacyPolicyLink: (
      <a
        href="https://www.espoo.fi/fi/espoon-kaupunki/tietosuoja"
        data-qa="footer-policy-link"
        style={{ color: colors.main.m2 }}
      >
        Tietosuojaselosteet
      </a>
    ),
    accessibilityStatement: 'Saavutettavuusseloste',
    sendFeedbackLink: (
      <a
        href="https://easiointi.espoo.fi/eFeedback/fi/Feedback/20-S%C3%A4hk%C3%B6iset%20asiointipalvelut"
        data-qa="footer-feedback-link"
        style={{ color: colors.main.m2 }}
      >
        Lähetä palautetta
      </a>
    )
  },
  loginPage: {
    title: 'Espoon kaupungin varhaiskasvatus',
    login: {
      title: 'Kirjaudu käyttäjätunnuksella',
      paragraph:
        'Huoltajat, joiden lapsi on jo varhaiskasvatuksessa tai esiopetuksessa: hoida lapsesi päivittäisiä varhaiskasvatusasioita kuten lue viestejä ja ilmoita lapsen läsnäoloajat ja poissaolot.',
      link: 'Kirjaudu sisään',
      infoBoxText: (
        <>
          <P>
            Mikäli kirjautuminen tästä ei onnistu, katso ohjeet{' '}
            <a
              href="https://www.espoo.fi/fi/palvelut/evaka"
              target="_blank"
              rel="noreferrer"
            >
              eVaka | Espoon kaupunki
            </a>
            . Voit kirjautua myös käyttämällä vahvaa tunnistautumista.
          </P>
        </>
      )
    },
    applying: {
      title: 'Kirjaudu Suomi.fi:ssä',
      paragraph: 'Tunnistautumalla eVakaan vahvasti Suomi.fi:ssä voit',
      infoBoxText:
        'Tunnistautumisen yhteydessä väestötietojärjestelmästä haetaan kirjautujan, toisen huoltajan sekä alaikäisten huollettavien henkilö- ja osoitetiedot.  ',
      infoBullets: [
        'hakea lapsellesi varhaiskasvatus-, esiopetus- tai kerhopaikkaa tai tarkastella aiemmin tekemääsi hakemusta',
        'tarkastella lapsesi varhaiskasvatukseen tai esiopetukseen liittyviä kuvia ja muita dokumentteja',
        'ilmoittaa omat tai lapsesi tulotiedot',
        'hyväksyä tai hylätä päätöksen, jos olet hakemuksen tekijä'
      ],
      link: 'Tunnistaudu',
      mapText: 'Katso kartalta yksiköt, joihin voit hakea eVakassa.',
      mapLink: 'Yksiköt kartalla'
    }
  },
  ctaToast: {
    holidayPeriodCta: (period: FiniteDateRange, deadline: LocalDate) => (
      <>
        <InlineButton text="Ilmoita tästä" onClick={() => undefined} /> läsnä-
        ja poissaolot välille {period.start.format('dd.MM.')}-
        {period.end.format()} viimeistään {deadline.format()}. Läsnäolojen
        tarkat kellonajat merkitään, kun kysely on päättynyt.
      </>
    ),
    fixedPeriodCta: (deadline: LocalDate) =>
      `Vastaa poissaolokyselyyn ${deadline.format()} mennessä.`,
    incomeExpirationCta: (expirationDate: string) =>
      `Muista päivittää tulotietosi ${expirationDate} mennessä`
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
      PRIVATE_SERVICE_VOUCHER: 'palveluseteli',
      MUNICIPAL_SCHOOL: 'koulu'
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
    absentFree: 'Maksuton poissaolo',
    absentPlanned: 'Vuorotyöpoissaolo',
    absences: {
      SICKLEAVE: 'Sairauspoissaolo',
      PLANNED_ABSENCE: 'Vuorotyöpoissaolo'
    },
    absenceMarkedByEmployee: 'Henkilökunnan merkitsemä poissaolo',
    contactStaffToEditAbsence:
      'Jos haluat muuttaa poissaoloa, ota yhteyttä henkilökuntaan.',
    intermittentShiftCareNotification: 'Ilta-/vuorohoito',
    newReservationOrAbsence: 'Läsnäolo / Poissaolo',
    newHoliday: 'Vastaa poissaolokyselyyn',
    newAbsence: 'Ilmoita poissaolo',
    newReservationBtn: 'Ilmoita läsnäolo',
    missingReservation: 'Ilmoitus puuttuu',
    reservationNotRequired: 'Ilmoitusta ei tarvita',
    termBreak: 'Ei opetusta tänään',
    reservation: 'Ilmoitettu läsnäolo',
    reservationNoTimes: 'Läsnä',
    attendance: 'Toteutunut läsnäolo',
    exceedStart: 'Saapunut ilmoitettua aikaisemmin.',
    exceedEnd: 'Lähtenyt ilmoitettua myöhemmin.',
    exceedGeneric: 'Toteunut läsnäoloaika ylittää ilmoitetun ajan.',
    calculatedUsedServiceTime:
      'Käytetty palveluntarve määräytyy kuukauden palveluntarpeen mukaan.',
    usedService: 'Käytetty palveluntarve',
    reservationsAndRealized: 'Läsnäoloaika',
    events: 'Päivän tapahtumat',
    noActivePlacements:
      'Lapsesi ei ole varhaiskasvatuksessa tai esiopetuksessa tänä päivänä',
    attendanceWarning: 'Toteutunut läsnäoloaika ylittää ilmoitetun ajan.',
    eventsCount: 'tapahtumaa',
    shiftCareInfoLabel: 'Varhaiskasvatus yön yli?',
    shiftCareInfo: (): React.ReactNode => (
      <>
        <P>
          Jos lapsi tarvitsee varhaiskasvatusta yön yli, merkitse keskiyötä
          edeltävä osa päättymään klo 23:59, ja keskiyön jälkeinen osa alkamaan
          seuraavana päivänä klo 00:00.
        </P>
        <P>
          Esimerkiksi jos lapsi on hoidossa yön yli maanantai-illasta klo 20:00
          tiistai-aamuun klo 06:00, merkitse maanantaille 20:00 - 23:59 ja
          tiistaille 00:00 - 06:00.
        </P>
      </>
    ),
    reservationModal: {
      title: 'Ilmoita läsnäolo',
      selectChildren: 'Valitse lapset',
      holidayPeriod: (period: FiniteDateRange) =>
        `Loma-aika: ${period.start.format('dd.MM.')}-${period.end.format(
          'dd.MM.'
        )}. Merkitse loma-ajan läsnäolot määräaikaan mennessä. Tarkat kellonajat voi merkitä, kun kyselyn määräaika on päättynyt.`,
      dateRange: 'Läsnäoloaika',
      dateRangeLabel: 'Ilmoita läsnäolo päiville',
      dateRangeInfo: (date: LocalDate) =>
        `Voit ilmoittaa läsnäolon enimmillään ${date.format()} asti.`,
      noReservableDays:
        'Tulossa ei ole päiviä, joille voisi tehdä läsnäoloilmoituksen.',
      missingDateRange: 'Valitse aikaväli',
      selectRecurrence: 'Valitse miten kellonaika toistuu',
      postError: 'Läsnäolon ilmoittaminen ei onnistunut',
      repetitions: {
        DAILY: 'Päivittäin sama aika',
        WEEKLY: 'Viikonpäivittäin sama aika',
        IRREGULAR: 'Vaihtelevat ajat'
      },
      start: 'Alkaa',
      end: 'Päättyy',
      present: 'Läsnä',
      absent: 'Poissa',
      reservationClosed: 'Ilmoittautuminen päättynyt',
      reservationClosedInfo:
        'Jos haluat ilmoittaa läsnäoloajan tälle päivälle, ota yhteyttä henkilökuntaan',
      saveErrors: {
        failure: 'Tallennus epäonnistui',
        NON_RESERVABLE_DAYS: 'Joitain valittuja päiviä ei voida varata'
      }
    },
    absenceModal: {
      title: 'Ilmoita poissaolo',
      selectedChildren: 'Valitse lapset, jotka ovat poissa',
      selectChildrenInfo:
        'Ilmoita tässä vain koko päivän kestävät poissaolot. Osapäiväiset poissaolot voit ilmoittaa muokkaamalla lapsen läsnäoloaikaa.',
      lockedAbsencesWarningTitle: 'Poissaolo useammalle päivälle',
      lockedAbsencesWarningText:
        'Olet merkitsemässä poissaoloa useammalle päivälle, jonka ilmoittautumisaika on mennyt umpeen. Tämän voi perua vain olemalla yhteydessä henkilökuntaan.',
      dateRange: 'Poissaoloilmoitus päiville',
      absenceType: 'Poissaolon syy',
      absenceTypes: {
        SICKLEAVE: 'Sairaus',
        OTHER_ABSENCE: 'Poissaolo',
        PLANNED_ABSENCE: 'Vuorotyöpoissaolo'
      },
      contractDayAbsenceTypeWarning:
        'Vain osalla lapsista on sopimuspäivät käytössä, joten sopimuspäivien poissaoloa ei voida tallentaa kaikille lapsille samaan aikaan.',
      attendanceAlreadyExistsErrorTitle:
        'Valituille päiville on jo läsnäolomerkintöjä',
      attendanceAlreadyExistsErrorDescription:
        'Et voi lisätä poissaolomerkintää päivälle jolle lapsella on jo läsnäolomerkintä.'
    },
    holidayModal: {
      additionalInformation: 'Lisätietoja',
      holidayFor: 'Loman ajankohta:',
      childOnHoliday: 'Lapsi on lomalla',
      addTimePeriod: 'Lisää ajankohta',
      emptySelection: 'Ei maksutonta poissaoloa',
      notEligible: (period: FiniteDateRange) =>
        `Kysely ei koske lasta, koska hän ei ole ollut varhaiskasvatuksessa yhtäjaksoisesti ${period.format()}.`
    },
    previousDay: 'Edellinen päivä',
    nextDay: 'Seuraava päivä',
    previousMonth: 'Edellinen kuukausi',
    nextMonth: 'Seuraava kuukausi',
    dailyServiceTimeModifiedNotification: (dateFrom: string) =>
      `Varhaiskasvatussopimukseenne on päivitetty uusi läsnäoloaika ${dateFrom} alkaen.`,
    dailyServiceTimeModifiedDestructivelyModal: {
      text: (dateFrom: string) =>
        `Varhaiskasvatussopimukseenne on päivitetty uusi päivittäinen läsnäoloaika. Teethän uudet läsnäoloilmoitukset ${dateFrom} alkaen.`,
      title: 'Tee uudet läsnäoloilmoitukset',
      ok: 'Selvä!'
    },
    absentEnable: 'Merkitse poissaolevaksi',
    absentDisable: 'Merkitse läsnä olevaksi',
    validationErrors: {
      range: 'Yksikön aukiolo ylittyy'
    },
    monthSummary: {
      title: 'Läsnäolot',
      reserved: 'Suunnitelma',
      usedService: 'Toteuma',
      minutes: 'min',
      hours: 'h',
      warningUsedServiceExceeded: 'Läsnäoloja sopimuksen ylittävä määrä:',
      warningPlannedServiceExceeded:
        'Läsnäoloja suunniteltu sopimuksen ylittävä määrä:'
    }
  },
  messages: {
    inboxTitle: 'Viestit',
    noMessagesInfo: 'Tässä näet saapuneet ja lähetetyt viestisi',
    emptyInbox: 'Viestilaatikkosi on tyhjä',
    openMessage: 'Avaa viesti',
    noSelectedMessage: 'Ei valittua viestiä',
    recipients: 'Vastaanottajat',
    send: 'Lähetä',
    sender: 'Lähettäjä',
    sending: 'Lähetetään',
    types: {
      MESSAGE: 'Viesti',
      BULLETIN: 'Tiedote'
    },
    deleteThread: 'Poista viestiketju',
    staffAnnotation: 'Henkilökunta',
    threadList: {
      participants: 'Osallistujat',
      title: 'Otsikko',
      sentAt: 'Lähetetty',
      message: 'Viesti'
    },
    thread: {
      children: 'Koskee lapsia',
      title: 'Otsikko',
      reply: 'Vastaa viestiin',
      jumpToLastMessage: 'Siirry viimeiseen viestiin',
      jumpToBeginning: 'Takaisin alkuun',
      close: 'Takaisin listaan',
      sender: 'Lähettäjä',
      sentAt: 'Lähetetty',
      recipients: 'Vastaanottajat'
    },
    messageEditor: {
      newMessage: 'Uusi viesti',
      recipients: 'Vastaanottajat',
      secondaryRecipients: 'Muut vastaanottajat',
      singleUnitRequired:
        'Viestejä voi lähettää vain yhteen yksikköön kerrallaan',
      children: 'Viesti koskee',
      subject: 'Otsikko',
      message: 'Viesti',
      deleteDraft: 'Hylkää luonnos',
      send: 'Lähetä',
      discard: 'Hylkää',
      search: 'Haku',
      noResults: 'Ei tuloksia, valitse ensin lapsi tai lapsia',
      messageSendError: 'Viestin lähetys epäonnistui'
    },
    confirmDelete: {
      title: 'Haluatko varmasti poistaa viestiketjun?',
      text: 'Viestejä ei poistamisen jälkeen saa enää palautettua takaisin.',
      cancel: 'Älä poista',
      confirm: 'Poista viestiketju'
    },
    sensitive: 'Arkaluontoinen viestiketju',
    strongAuthRequired: 'Lukeminen vaatii vahvan tunnistautuminen',
    strongAuthRequiredThread:
      'Viestiketjun lukeminen vaatii vahvan tunnistautumisen.',
    strongAuthLink: 'Tunnistaudu'
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
        'Tällä lomakkeella voit myös hakea liittyvää varhaiskasvatusta lapsille, jotka ilmoitetaan / on ilmoitettu esiopetukseen tai valmistavaan opetukseen.',
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
              <P>
                Kunnallisen varhaiskasvatuksen asiakasmaksut ja palvelusetelin
                omavastuuosuus määräytyvät prosenttiosuutena perheen
                bruttotuloista. Tulojen lisäksi maksuihin vaikuttaa perheen koko
                ja sovittu varhaiskasvatusaika. Palveluseteliyksiköissä voidaan
                periä lisämaksu, tiedot mahdollisesta lisämaksusta löytyy
                yksikön nettisivuilta. Perhe toimittaa tuloselvityksen
                bruttotuloistaan viimeistään kahden viikon kuluessa siitä, kun
                lapsi on aloittanut varhaiskasvatuksessa.
              </P>
              <P>
                Lisätietoa varhaiskasvatuksen maksuista, tuloselvityksen
                toimittamisesta ja palvelusetelin lisämaksusta löydät täältä:{' '}
                <a href="https://www.espoo.fi/fi/kasvatus-ja-opetus/varhaiskasvatus/maksut-varhaiskasvatuksessa">
                  Maksut varhaiskasvatuksessa
                </a>
                .
              </P>
              <P fitted={true}>* Tähdellä merkityt tiedot ovat pakollisia</P>
            </>
          ),
          PRESCHOOL: (
            <>
              <P>
                Esiopetukseen osallistutaan oppivelvollisuuden alkamista
                edeltävänä vuonna. Esiopetus on maksutonta. Lukuvuoden 2024–2025
                esiopetukseen ilmoittaudutaan 9.–19.1.2024. Suomen- ja
                ruotsinkielinen esiopetus alkaa 8.8.2024.
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
        hasErrors: 'Ole hyvä ja tarkista seuraavat tiedot hakemuksestasi:',
        invalidFields: (count: number) => `${count} ${
          count === 1 ? 'kenttä' : 'kenttää'
        } jossa puutteellisia tai
        virheellisiä tietoja.`
      },
      actions: {
        verify: 'Tarkista hakemus',
        hasVerified: 'Olen tarkistanut hakemuksen tiedot oikeiksi',
        allowOtherGuardianAccess: (
          <span>
            Ymmärrän, että tieto hakemuksesta menee myös lapsen toiselle
            huoltajalle. Jos tieto ei saa mennä toiselle huoltajalle, ole
            yhteydessä palveluohjaukseen. Palveluohjauksen yhteystiedot löydät{' '}
            <a
              href="https://www.espoo.fi/fi/kasvatus-ja-opetus/varhaiskasvatus/varhaiskasvatukseen-hakeminen-ja-palveluohjaus#section-38795"
              target="_blank"
              rel="noreferrer"
            >
              täältä
            </a>
            .
          </span>
        ),
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
            label: 'Liittyvä varhaiskasvatus',
            withConnectedDaycare:
              'Haen myös esiopetukseen liittyvää varhaiskasvatusta.',
            withoutConnectedDaycare: 'Ei',
            startDate: 'Toivottu aloituspäivä',
            serviceNeed: 'Palveluntarve'
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
              'Olemme yhdessä sopineet hakemisesta lomakkeen tietojen mukaisesti.',
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
          missing:
            'Päästäksesi valitsemaan palveluntarpeen valitse ensin toivottu aloituspäivä',
          info: {
            DAYCARE: [],
            PRESCHOOL: [
              'Suomen- ja ruotsinkielinen esiopetus alkaa 8.8.2024. Jos tarvitsette varhaiskasvatusta 1.8.2024 lähtien ennen esiopetuksen alkua, voitte hakea sitä tällä hakemuksella valitsemalla ”Haen myös esiopetukseen liittyvää varhaiskasvatusta”.'
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
          instructions: {
            DAYCARE: (
              <>
                Toivottua aloituspäivää on mahdollista muuttaa myöhemmäksi niin
                kauan kuin hakemusta ei ole otettu käsittelyyn. Tämän jälkeen
                toivotun aloituspäivän muutokset tehdään ottamalla yhteyttä
                varhaiskasvatuksen palveluohjaukseen (puh. 09 816 31000).
              </>
            ),
            PRESCHOOL: (
              <>
                Toivottua aloituspäivää on mahdollista muuttaa myöhemmäksi niin
                kauan kuin hakemusta ei ole otettu käsittelyyn. Tämän jälkeen
                toivotun aloituspäivän muutokset tehdään ottamalla yhteyttä
                varhaiskasvatuksen palveluohjaukseen (puh. 09 816 31000).
              </>
            ),
            CLUB: null
          } as {
            DAYCARE: React.JSX.Element | null
            PRESCHOOL: React.JSX.Element | null
            CLUB: React.JSX.Element | null
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
                  maksullista.
                </strong>{' '}
                Liittyvää varhaiskasvatusta järjestetään aamuisin ennen
                esiopetusta ja esiopetuksen jälkeen esiopetusyksikön
                aukioloaikojen mukaisesti.
              </P>
              <P>
                Jos tarvitset varhaiskasvatusta 1.8.2024 lähtien ennen
                esiopetuksen alkua huomioi tämä toivotun aloituspäivämäärän
                valinnassa.
              </P>
              <P>
                Palveluseteliyksiköhin haettaessa, liittyvän varhaiskasvatuksen
                palveluseteliä haetaan valitsemalla hakutoiveeksi se
                palveluseteliyksikkö, johon halutaan hakea.
              </P>
              <P>
                Yksityisiin esiopetusyksiköihin haettaessa, liittyvä
                varhaiskasvatus haetaan suoraan yksiköstä (pois lukien
                palveluseteliyksiköt), yksiköt informoivat asiakkaita
                hakutavasta. Jos esiopetushakemuksessa on haettu liityvää
                varhaiskasvatusta yksityisestä yksiköstä, palveluohjaus muuttaa
                hakemuksen vain esiopetushakemukseksi.
              </P>
              <P>
                Saat varhaiskasvatuspaikasta erillisen kirjallisen päätöksen.
                Päätös tulee{' '}
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
            'Vuorohoidolla tarkoitetaan viikonloppuna tai ympärivuorokautisesti tarvittavaa varhaiskasvatusta. Iltahoito on pääasiassa klo 6.30-18.00 ulkopuolella ja viikonloppuisin tapahtuvaa varhaiskasvatusta.',
          instructions2:
            'Esiopetushakemukselle pyydämme liittämään samassa taloudessa asuvien huoltajien osalta todistukset työnantajalta säännöllisestä vuorotyöstä tai oppilaitoksen edustajalta päätoimisesta iltaopiskelusta. Dokumenttien tulee olla kirjattu sinä vuonna, kun hakemus esiopetukseen tehdään',
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
            'Valitse hakemuksesta tämä kohta, jos lapsi tarvitsee tukea kehitykselleen, oppimiselleen tai hyvinvoinnilleen. Tukea toteutetaan lapsen arjessa osana varhaiskasvatuksen toimintaa. Jos lapsellanne on tuen tarvetta, varhaiskasvatuksen erityisopettaja ottaa hakijaan yhteyttä, jotta lapsen tarpeet voidaan ottaa huomioon varhaiskasvatuspaikkaa osoitettaessa.',
          CLUB: 'Valitse hakemuksesta tämä kohta, jos lapsi tarvitsee tukea kehitykselleen, oppimiselleen tai hyvinvoinnilleen. Tukea toteutetaan lapsen arjessa osana varhaiskasvatuksen muuta toimintaa. Jos lapsellanne on tuen tarvetta, varhaiskasvatuksen erityisopettaja ottaa hakijaan yhteyttä, jotta lapsen tarpeet voidaan ottaa huomioon varhaiskasvatuspaikkaa osoitettaessa.',
          PRESCHOOL:
            'Valitse hakemuksesta tämä kohta, jos lapsi tarvitsee kasvulleen ja/tai oppimiselleen tukea esiopetusvuonna. Tukea toteutetaan lapsen arjessa osana esiopetuksen ja varhaiskasvatuksen toimintaa. Valitse tämä kohta myös, jos lapsella on muu erityinen syy, jolla on suoranaista vaikutusta esiopetuksen järjestämiseen ja siihen, missä yksikössä lapsen esiopetus tulee järjestää. Jos lapsella on kasvun ja/tai oppimisen tuen tarvetta, varhaiskasvatuksen erityisopettaja ottaa hakijaan yhteyttä, jotta lapsen tarpeet voidaan ottaa huomioon esiopetuspaikkaa osoitettaessa.'
        },
        preparatory:
          'Lapsi tarvitsee tukea suomen kielen oppimisessa. Haen myös perusopetukseen valmistavaan opetukseen. Ei koske ruotsinkielistä esiopetusta.',
        preparatoryInfo:
          'Esiopetuksessa toteutettavaan perusopetukseen valmistavaan opetukseen voivat hakeutua lapset, joilla ei ole vielä suomen kielen taitoa tai jotka osaavat jo jonkin verran suomea. Lapselle on suositeltu valmistavaa esiopetusta nykyisestä päiväkodista. Esiopetusikäisten perusopetukseen valmistavaa opetusta järjestetään kunnallisissa suomenkielisissä esiopetusryhmissä. '
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
                  jossa hänen sisaruksensa on varhaiskasvatuksen
                  aloitushetkellä. Sisarukseksi katsotaan kaikki samassa
                  osoitteessa asuvat lapset. Tavoitteena on sijoittaa sisarukset
                  samaan varhaiskasvatuspaikkaan perheen niin toivoessa. Jos
                  haet paikkaa sisaruksille, jotka eivät vielä ole
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
          } as {
            DAYCARE: React.JSX.Element | null
            PRESCHOOL: React.JSX.Element | null
            CLUB: React.JSX.Element | null
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
          title: (maxUnits: number): string =>
            maxUnits === 1 ? 'Hakutoive' : 'Hakutoiveet',
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
                  Voit hakea 1-3 eri yksikköön toivomassasi järjestyksessä.
                  Hakutoiveet eivät takaa paikkaa toivotusta yksiköstä, mutta
                  mahdollisuus toivotun paikan saamiseen kasvaa antamalla
                  useamman vaitoehdon.
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
            label: (maxUnits: number): string =>
              maxUnits === 1 ? 'Valitse hakutoive' : 'Valitse hakutoiveet',
            placeholder: 'Hae yksiköitä',
            maxSelected: 'Maksimimäärä yksiköitä valittu',
            noOptions: 'Ei hakuehtoja vastaavia yksiköitä'
          },
          preferences: {
            label: (maxUnits: number): string =>
              maxUnits === 1
                ? 'Valitsemasi hakutoive'
                : 'Valitsemasi hakutoiveet',
            noSelections: 'Ei valintoja',
            info: (maxUnits: number) =>
              maxUnits === 1
                ? 'Valitse yksi varhaiskasvatusyksikkö'
                : `Valitse 1-${maxUnits} varhaiskasvatusyksikköä ja järjestä ne toivomaasi järjestykseen. Voit muuttaa järjestystä nuolien avulla.`,
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
          CLUB: <P />
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
        allergiesPlaceholder: 'Voit halutessasi ilmoittaa lapsen allergiat'
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
          'Toisen huoltajan tiedot haetaan automaattisesti väestötietojärjestelmästä. Tietojemme mukaan lapsen toinen huoltaja asuu eri osoitteessa.',
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
    childhoodEducationTitle:
      'Varhaiskasvatukseen, esiopetukseen ja kerhoon liittyvät päätökset',
    summary: (
      <P width="800px">
        Tälle sivulle saapuvat lapsen varhaiskasvatukseen, esiopetukseen,
        kerhoon ja maksuihin liittyvät päätökset.
        <br aria-hidden="true" />
        <br aria-hidden="true" />
        Jos päätös koskee uutta lapselle haettua paikkaa,{' '}
        <strong>sinun tulee vastata kahden viikon sisällä</strong>, hyväksytkö
        vai hylkäätkö lapselle tarjotun paikan.
      </P>
    ),
    unconfirmedDecisions: (n: number) =>
      `${n} ${n === 1 ? 'päätös' : 'päätöstä'} odottaa vahvistustasi`,
    noUnconfirmedDecisions: 'kaikki päätökset vahvistettu',
    unreadDecision: 'lukematon päätös',
    pageLoadError: 'Tietojen hakeminen ei onnistunut',
    financeDecisions: {
      type: {
        FEE_DECISION: 'Maksupäätös',
        VOUCHER_VALUE_DECISION: 'Arvopäätös'
      },
      title: 'Maksuihin liittyvät päätökset',
      liableCitizens: 'Maksuvelvolliset',
      validityPeriod: 'Päätös voimassa alkaen',
      sentAt: 'Päätös lähetetty',
      voucherValueChild: 'Lapsi jota päätös koskee',
      loadDecisionPDF: 'Näytä päätös'
    },
    applicationDecisions: {
      decision: 'Päätös',
      type: {
        CLUB: 'kerhosta',
        DAYCARE: 'varhaiskasvatuksesta',
        DAYCARE_PART_TIME: 'osa-aikaisesta varhaiskasvatuksesta',
        PRESCHOOL: 'esiopetuksesta',
        PRESCHOOL_DAYCARE: 'liittyvästä varhaiskasvatuksesta',
        PRESCHOOL_CLUB: 'esiopetuksen kerhosta',
        PREPARATORY_EDUCATION: 'valmistavasta opetuksesta'
      },
      childName: 'Lapsen nimi',
      unit: 'Toimipaikka',
      period: 'Ajalle',
      sentDate: 'Päätös tehty',
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
    },
    assistancePreschoolDecisions: {
      title: 'Päätös erityisestä tuesta esiopetuksessa',
      statuses: {
        DRAFT: 'Luonnos',
        NEEDS_WORK: 'Korjattava',
        ACCEPTED: 'Hyväksytty',
        REJECTED: 'Hylätty',
        ANNULLED: 'Mitätöity'
      },
      annulmentReason: 'Päätöksen mitätöinnin perustelu',
      pageTitle: 'Päätös tuesta esiopetuksessa',
      decisionNumber: 'Päätösnumero',
      confidential: 'Salassapidettävä',
      lawReference: 'JulkL 24.1 §',
      types: {
        NEW: 'Erityinen tuki alkaa',
        CONTINUING: 'Erityinen tuki jatkuu',
        TERMINATED: 'Erityinen tuki päättyy'
      },
      decidedAssistance: 'Päätettävä tuki',
      type: 'Erityisen tuen tila',
      validFrom: 'Voimassa alkaen',
      validTo: 'Voimassa päättyen',
      extendedCompulsoryEducationSection: 'Pidennetty oppivelvollisuus',
      extendedCompulsoryEducation:
        'Kyllä, lapsella on pidennetty oppivelvollisuus',
      no: 'Ei',
      extendedCompulsoryEducationInfo:
        'Lisätiedot pidennetystä oppivelvollisuudesta',
      grantedAssistanceSection:
        'Myönnettävät tulkitsemis- ja avustajapalvelut tai erityiset apuvälineet',
      grantedAssistanceService: 'Lapselle myönnetään avustajapalveluita',
      grantedInterpretationService: 'Lapselle myönnetään tulkitsemispalveluita',
      grantedAssistiveDevices: 'Lapselle myönnetään erityisiä apuvälineitä',
      grantedNothing: 'Ei valintaa',
      grantedServicesBasis:
        'Perustelut myönnettäville tulkitsemis- ja avustajapalveluille ja apuvälineille',
      selectedUnit: 'Esiopetuksen järjestämispaikka',
      primaryGroup: 'Pääsääntöinen opetusryhmä',
      decisionBasis: 'Perustelut päätökselle',
      documentBasis: 'Asiakirjat, joihin päätös perustuu',
      basisDocumentPedagogicalReport: 'Pedagoginen selvitys',
      basisDocumentPsychologistStatement: 'Psykologin lausunto',
      basisDocumentDoctorStatement: 'Lääkärin lausunto',
      basisDocumentSocialReport: 'Sosiaalinen selvitys',
      basisDocumentOtherOrMissing: 'Liite puuttuu, tai muu liite, mikä?',
      basisDocumentsInfo: 'Lisätiedot liitteistä',
      guardianCollaborationSection: 'Huoltajien kanssa tehty yhteistyö',
      guardiansHeardOn: 'Huoltajien kuulemisen päivämäärä',
      heardGuardians: 'Huoltajat, joita on kuultu, ja kuulemistapa',
      otherRepresentative:
        'Muu laillinen edustaja (nimi, puhelinnumero ja kuulemistapa)',
      viewOfGuardians: 'Huoltajien näkemys esitetystä tuesta',
      responsiblePeople: 'Vastuuhenkilöt',
      preparer: 'Päätöksen valmistelija',
      decisionMaker: 'Päätöksen tekijä',
      employeeTitle: 'Titteli',
      phone: 'Puhelinnumero',
      legalInstructions: 'Sovelletut oikeusohjeet',
      legalInstructionsText: 'Perusopetuslaki 17 §',
      legalInstructionsTextExtendedCompulsoryEducation:
        'Oppivelvollisuulaki 2 §',
      jurisdiction: 'Toimivalta',
      jurisdictionText:
        'Delegointipäätös suomenkielisen varhaiskasvatuksen sekä kasvun ja oppimisen toimialan esikunnan viranhaltijoiden ratkaisuvallasta A osa 3 § 1 kohta',
      appealInstructionsTitle: 'Oikaisuvaatimusohje',
      appealInstructions: (
        <>
          <P>
            Tähän päätökseen tyytymätön voi tehdä kirjallisen
            oikaisuvaatimuksen. Päätökseen ei saa hakea muutosta valittamalla
            tuomioistuimeen.
          </P>

          <H3>Oikaisuvaatimusoikeus</H3>
          <P>
            Oikaisuvaatimuksen saa tehdä se, johon päätös on kohdistettu tai
            jonka oikeuteen, velvollisuuteen tai etuun päätös välittömästi
            vaikuttaa (asianosainen).
          </P>

          <H3>Oikaisuvaatimusaika</H3>
          <P>
            Oikaisuvaatimus on tehtävä 14 päivän kuluessa päätöksen
            tiedoksisaannista.
          </P>
          <P>
            Oikaisuvaatimus on toimitettava Etelä-Suomen aluehallintovirastolle
            viimeistään määräajan viimeisenä päivänä ennen Etelä-Suomen
            aluehallintoviraston aukioloajan päättymistä.
          </P>
          <P>
            Asianosaisen katsotaan saaneen päätöksestä tiedon, jollei muuta
            näytetä, seitsemän päivän kuluttua kirjeen lähettämisestä tai
            saantitodistukseen tai tiedoksiantotodistukseen merkittynä päivänä.
          </P>
          <P>
            Käytettäessä tavallista sähköistä tiedoksiantoa asianosaisen
            katsotaan saaneen päätöksestä tiedon, jollei muuta näytetä,
            kolmantena päivänä viestin lähettämisestä.
          </P>
          <P>
            Tiedoksisaantipäivää ei lueta oikaisuvaatimusaikaan. Jos
            oikaisuvaatimusajan viimeinen päivä on pyhäpäivä, itsenäisyyspäivä,
            vapunpäivä, joulu- tai juhannusaatto tai arkilauantai, saa
            oikaisuvaatimuksen tehdä ensimmäisenä arkipäivänä sen jälkeen.
          </P>

          <H3>Oikaisuviranomainen</H3>
          <P>
            Viranomainen, jolle oikaisuvaatimus tehdään, on Etelä-Suomen
            aluehallintovirasto
          </P>
          <P>
            Postiosoite: PL 1, 13035 AVI
            <br />
            Helsingin toimipaikan käyntiosoite: Ratapihantie 9, 00521 Helsinki
            <br />
            Sähköpostiosoite: kirjaamo.etela@avi.fi
            <br />
            Puhelinvaihde: 0295 016 000
            <br />
            Faksinumero: 0295 016 661
            <br />
            Virastoaika: ma-pe 8.00–16.15
          </P>
          <H3>Oikaisuvaatimuksen muoto ja sisältö</H3>
          <P>
            Oikaisuvaatimus on tehtävä kirjallisesti. Myös sähköinen asiakirja
            täyttää vaatimuksen kirjallisesta muodosta.
          </P>
          <P noMargin>Oikaisuvaatimuksessa on ilmoitettava</P>
          <ul>
            <li>päätös, johon vaaditaan oikaisua,</li>
            <li>millaista oikaisua päätökseen vaaditaan,</li>
            <li>millä perusteilla oikaisua vaaditaan</li>
          </ul>
          <P>
            Oikaisuvaatimuksessa on lisäksi ilmoitettava tekijän nimi,
            kotikunta, postiosoite, puhelinnumero ja muut asian hoitamiseksi
            tarvittavat yhteystiedot.
          </P>
          <P>
            Jos oikaisuvaatimuspäätös voidaan antaa tiedoksi sähköisenä
            viestinä, yhteystietona pyydetään ilmoittamaan myös
            sähköpostiosoite.
          </P>
          <P>
            Jos oikaisuvaatimuksen tekijän puhevaltaa käyttää hänen laillinen
            edustajansa tai asiamiehensä tai jos oikaisuvaatimuksen laatijana on
            joku muu henkilö, oikaisuvaatimuksessa on ilmoitettava myös tämän
            nimi ja kotikunta.
          </P>
          <P noMargin>Oikaisuvaatimukseen on liitettävä</P>
          <ul>
            <li>
              päätös, johon haetaan oikaisua, alkuperäisenä tai jäljennöksenä
            </li>
            <li>
              todistus siitä, minä päivänä päätös on annettu tiedoksi, tai muu
              selvitys oikaisuvaatimusajan alkamisen ajankohdasta
            </li>
            <li>
              asiakirjat, joihin oikaisuvaatimuksen tekijä vetoaa
              oikaisuvaatimuksensa tueksi, jollei niitä ole jo aikaisemmin
              toimitettu viranomaiselle.
            </li>
          </ul>
        </>
      )
    },
    assistanceDecisions: {
      title: 'Päätös erityisestä tuesta varhaiskasvatuksessa',
      assistanceLevel: 'Tuen taso',
      validityPeriod: 'Voimassa',
      unit: 'Yksikkö',
      decisionMade: 'Päätös tehty',
      level: {
        ASSISTANCE_ENDS: 'Erityinen/tehostettu tuki päättyy',
        ASSISTANCE_SERVICES_FOR_TIME:
          'Tukipalvelut päätöksen voimassaolon aikana',
        ENHANCED_ASSISTANCE: 'Tehostettu tuki',
        SPECIAL_ASSISTANCE: 'Erityinen tuki'
      },
      statusLabel: 'Tila',
      openDecision: 'Näytä päätös',
      decision: {
        pageTitle: 'Päätös tuesta varhaiskasvatuksessa',
        annulmentReason: 'Päätöksen mitätöinnin perustelu',
        neededTypesOfAssistance: 'Lapsen tarvitsemat tuen muodot',
        pedagogicalMotivation: 'Pedagogiset tuen muodot ja perustelut',
        structuralMotivation: 'Rakenteelliset tuen muodot ja perustelut',
        structuralMotivationOptions: {
          smallerGroup: 'Ryhmäkoon pienennys',
          specialGroup: 'Erityisryhmä',
          smallGroup: 'Pienryhmä',
          groupAssistant: 'Ryhmäkohtainen avustaja',
          childAssistant: 'Lapsikohtainen avustaja',
          additionalStaff: 'Henkilöresurssin lisäys'
        },
        careMotivation: 'Hoidolliset tuen muodot ja perustelut',
        serviceOptions: {
          consultationSpecialEd:
            'Varhaiskasvatuksen erityisopettajan antama konsultaatio',
          partTimeSpecialEd:
            'Varhaiskasvatuksen erityisopettajan osa-aikainen opetus',
          fullTimeSpecialEd:
            'Varhaiskasvatuksen erityisopettajan kokoaikainen opetus',
          interpretationAndAssistanceServices:
            'Tulkitsemis- ja avustamispalvelut',
          specialAides: 'Apuvälineet'
        },
        services: 'Tukipalvelut ja perustelut',
        collaborationWithGuardians: 'Huoltajien kanssa tehty yhteistyö',
        guardiansHeardOn: 'Huoltajien kuulemisen päivämäärä',
        guardiansHeard: 'Huoltajat, joita on kuultu, ja kuulemistapa',
        viewOfTheGuardians: 'Huoltajien näkemys esitetystä tuesta',
        decisionAndValidity: 'Päätettävä tuen taso ja voimassaolo',
        futureLevelOfAssistance: 'Lapsen tuen taso jatkossa',
        assistanceLevel: {
          assistanceEnds: 'Erityinen/tehostettu tuki päättyy',
          assistanceServicesForTime:
            'Tukipalvelut päätöksen voimassaolon aikana',
          enhancedAssistance: 'Tehostettu tuki',
          specialAssistance: 'Erityinen tuki'
        },
        startDate: 'Voimassa alkaen',
        endDate: 'Päätös voimassa saakka',
        endDateServices: 'Päätös voimassa tukipalveluiden osalta saakka',
        selectedUnit: 'Päätökselle valittu varhaiskasvatusyksikkö',
        unitMayChange:
          'Loma-aikoina tuen järjestämispaikka ja -tapa saattavat muuttua.',
        motivationForDecision: 'Perustelut lapsen tuen tasolle',
        legalInstructions: 'Sovelletut oikeusohjeet',
        legalInstructionsText: 'Varhaiskasvatuslaki, 3 a luku',
        jurisdiction: 'Toimivalta',
        jurisdictionText:
          'Delegointipäätös suomenkielisen varhaiskasvatuksen sekä kasvun ja oppimisen toimialan esikunnan viranhaltijoiden ratkaisuvallasta A osa 3 § 3 kohta',
        personsResponsible: 'Vastuuhenkilöt',
        preparator: 'Päätöksen valmistelija',
        decisionMaker: 'Päätöksen tekijä',
        disclaimer:
          'Varhaiskasvatuslain 15 e §:n mukaan tämä päätös voidaan panna täytäntöön muutoksenhausta huolimatta.',
        decisionNumber: 'Päätösnumero',
        statuses: {
          DRAFT: 'Luonnos',
          NEEDS_WORK: 'Korjattava',
          ACCEPTED: 'Hyväksytty',
          REJECTED: 'Hylätty',
          ANNULLED: 'Mitätöity'
        },
        confidential: 'Salassapidettävä',
        lawReference: 'Varhaiskasvatuslaki 40 §',
        appealInstructionsTitle: 'Oikaisuvaatimusohje',
        appealInstructions: (
          <>
            <H3>Oikaisuvaatimusoikeus</H3>
            <P>
              Oikaisuvaatimuksen saa tehdä se, johon päätös on kohdistettu tai
              jonka oikeuteen, velvollisuuteen tai etuun päätös välittömästi
              vaikuttaa (asianosainen).
            </P>
            <H3>Oikaisuvaatimusaika</H3>
            <P>
              Oikaisuvaatimus on tehtävä 30 päivän kuluessa päätöksen
              tiedoksisaannista.
            </P>
            <H3>Tiedoksisaanti</H3>
            <P>
              Asianosaisen katsotaan saaneen päätöksestä tiedon, jollei muuta
              näytetä, seitsemän päivän kuluttua kirjeen lähettämisestä tai
              saantitodistukseen tai tiedoksiantotodistukseen merkittynä
              päivänä. Käytettäessä tavallista sähköistä tiedoksiantoa
              asianosaisen katsotaan saaneen päätöksestä tiedon, jollei muuta
              näytetä kolmantena päivänä viestin lähettämisestä.
              Tiedoksisaantipäivää ei lueta määräaikaan. Jos määräajan viimeinen
              päivä on pyhäpäivä, itsenäisyyspäivä, vapunpäivä, joulu- tai
              juhannusaatto tai arkilauantai, saa tehtävän toimittaa
              ensimmäisenä arkipäivänä sen jälkeen.
            </P>
            <H3>Oikaisuviranomainen</H3>
            <P>Oikaisu tehdään Etelä-Suomen aluehallintovirastolle.</P>
            <P>
              Etelä-Suomen aluehallintovirasto
              <br />
              Käyntiosoite: Ratapihantie 9, 00521 Helsinki
              <br />
              Virastoaika: ma-pe 8.00–16.15
              <br />
              Postiosoite: PL 1, 13035 AVI
              <br />
              Sähköposti: kirjaamo.etela@avi.fi
              <br />
              Fax: 0295 016 661
              <br />
              Puhelin: 0295 016 000
            </P>
            <H3>Oikaisuvaatimuksen muoto ja sisältö</H3>
            <P>
              Oikaisuvaatimus on tehtävä kirjallisesti. Myös sähköinen asiakirja
              täyttää vaatimuksen kirjallisesta muodosta.
            </P>
            <P noMargin>Oikaisuvaatimuksessa on ilmoitettava</P>
            <ul>
              <li>
                Oikaisuvaatimuksen tekijän nimi, kotikunta, postiosoite,
                puhelinnumero ja muut asian hoitamiseksi tarvittavat
                yhteystiedot
              </li>
              <li>päätös, johon haetaan oikaisua</li>
              <li>
                miltä osin päätökseen haetaan oikaisua ja mitä oikaisua siihen
                vaaditaan tehtäväksi
              </li>
              <li>vaatimuksen perusteet</li>
            </ul>
            <P>
              Jos oikaisuvaatimuspäätös voidaan antaa tiedoksi sähköisenä
              viestinä, yhteystietona pyydetään ilmoittamaan myös
              sähköpostiosoite.
            </P>
            <P>
              Jos oikaisuvaatimuksen tekijän puhevaltaa käyttää hänen laillinen
              edustajansa tai asiamiehensä tai jos oikaisuvaatimuksen laatijana
              on joku muu henkilö, oikaisuvaatimuksessa on ilmoitettava myös
              tämän nimi ja kotikunta.
            </P>
            <P noMargin>Oikaisuvaatimukseen on liitettävä</P>
            <ul>
              <li>
                päätös, johon haetaan oikaisua, alkuperäisenä tai jäljennöksenä
              </li>
              <li>
                todistus siitä, minä päivänä päätös on annettu tiedoksi, tai muu
                selvitys oikaisuvaatimusajan alkamisen ajankohdasta
              </li>
              <li>
                asiakirjat, joihin oikaisuvaatimuksen tekijä vetoaa
                oikaisuvaatimuksensa tueksi, jollei niitä ole jo aikaisemmin
                toimitettu viranomaiselle.
              </li>
            </ul>
            <H3>Oikaisuvaatimuksen toimittaminen</H3>
            <P>
              Oikaisuvaatimuskirjelmä on toimitettava oikaisuvaatimusajan
              kuluessa oikaisuvaatimusviranomaiselle. Oikaisuvaatimuskirjelmän
              tulee olla perillä oikaisuvaatimusajan viimeisenä päivänä ennen
              viraston aukiolon päättymistä. Oikaisuvaatimuksen lähettäminen
              postitse tai sähköisesti tapahtuu lähettäjän omalla vastuulla.
            </P>
          </>
        )
      }
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
    type: {
      DAYCARE: 'Varhaiskasvatushakemus',
      PRESCHOOL: 'Esiopetushakemus',
      CLUB: 'Kerhohakemus'
    },
    transferApplication: 'Siirtohakemus',
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
    newApplicationLink: 'Uusi hakemus',
    namelessChild: 'Nimetön lapsi'
  },
  fileDownload: {
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
    detailsSection: {
      title: 'Henkilötiedot',
      noEmailAlert:
        'Sähköpostiosoitteesi puuttuu, ole hyvä ja täydennä se alle, jotta pystyt vastaanottamaan eVakasta lähetetyt ilmoitukset',
      noPhoneAlert: 'Puhelinnumerosi puuttuu, ole hyvä ja täydennä se alle',
      name: 'Nimi',
      preferredName: 'Kutsumanimi',
      contactInfo: 'Yhteystiedot',
      address: 'Osoite',
      phone: 'Puhelinnumero*',
      backupPhone: 'Varapuhelinnumero',
      backupPhonePlaceholder: 'Esim. työpuhelin',
      email: 'Sähköpostiosoite',
      emailMissing: 'Sähköpostiosoite puuttuu',
      phoneMissing: 'Puhelinnumero puuttuu',
      noEmail: 'Minulla ei ole sähköpostiosoitetta',
      emailInfo:
        'Sähköpostiosoite tarvitaan, jotta voimme lähettää sinulle ilmoitukset uusista viesteistä, läsnäoloaikojen varaamisesta sekä muista lapsen varhaiskasvatukseen liittyvistä asioista.'
    },
    loginDetailsSection: {
      title: 'Kirjautumistiedot',
      keycloakEmail: 'Käyttäjätunnus'
    },
    notificationsSection: {
      title: 'Sähköposti-ilmoitukset',
      info: 'Voit saada ilmoituksia sähköpostiin seuraavista aiheista. Asetuksia pystyy muokkaamaan muokkaa-nappia painamalla.',
      subtitle: 'Sähköpostiin lähetettävät ilmoitukset',
      message: 'eVakaan saapuneista henkilökunnan lähettämistä viesteistä',
      bulletin: 'eVakaan saapuneista yleisistä tiedotteista',
      outdatedIncome: 'Muistutukset tulotietojen päivittämisestä',
      outdatedIncomeInfo:
        'Mikäli ette maksa korkeinta varhaiskasvatusmaksua, on tulotiedot päivitettävä säännöllisesti. Jos tulotiedot puuttuvat tai vanhenevat, merkitään varhaiskasvatuksesta maksettavaksi korkein maksu.',
      outdatedIncomeWarning:
        'Jos tulotiedot puuttuvat tai vanhenevat, merkitään varhaiskasvatuksesta maksettavaksi korkein maksu.',
      calendarEvent:
        'Muistutukset kalenteriin merkityistä uusista tapahtumista',
      decision: 'Saapuneista päätöksistä',
      document: 'Uusista asiakirjoista',
      documentInfo:
        'Asiakirjoilla tarkoitetaan virallisia asiakirjoja, jotka eivät ole päätöksiä. Tällaisia ovat esimerkiksi varhaiskasvatussuunnitelmat ja pedagogiset arviot.',
      informalDocument: 'Muista lapsen arkeen liittyvistä dokumenteista',
      informalDocumentInfo:
        'Muut lapsen arkeen liittyvät dokumentit voivat olla esimerkiksi kuvia lapsen tekemistä piirustuksista.',
      missingAttendanceReservation:
        'Muistutukset puuttuvista läsnäoloilmoituksista',
      missingAttendanceReservationInfo:
        'Muistutus lähetetään ennen läsnäoloilmoitusten määräaikaa, mikäli jollakin lapsistasi puuttuu läsnäoloilmoitus tai poissaolomerkintä seuraavalta kahdelta viikolta.'
    }
  },
  income: {
    title: 'Tulotiedot',
    description: (
      <>
        <P>
          Tällä sivulla voit lähettää selvitykset varhaiskasvatusmaksuun
          vaikuttavista omista ja lastesi tuloista. Voit myös tarkastella
          palauttamiasi tuloselvityksiä ja muokata tai poistaa niitä, kunnes
          viranomainen on käsitellyt tiedot. Lomakkeen käsittelyn jälkeen voit
          päivittää tulotietojasi toimittamalla uuden lomakkeen.
        </P>

        <P>
          Kunnallisen varhaiskasvatuksen asiakasmaksut määräytyvät
          prosenttiosuutena perheen bruttotuloista. Maksut vaihtelevat perheen
          koon ja tulojen sekä varhaiskasvatusajan mukaan. Tarkista
          asiakastiedotteen taulukosta (jonka löydät{' '}
          <a
            target="_blank"
            rel="noreferrer"
            href="https://www.espoo.fi/fi/kasvatus-ja-opetus/varhaiskasvatus/maksut-varhaiskasvatuksessa"
          >
            täältä
          </a>
          ) tarvitseeko sinun toimittaa tuloselvitystä, vai kuuluuko perheenne
          automaattisesti korkeimman varhaiskasvatusmaksun piiriin.
        </P>
        <P>
          Lisätietoja maksuista:{' '}
          <a href="https://www.espoo.fi/fi/kasvatus-ja-opetus/varhaiskasvatus/maksut-varhaiskasvatuksessa">
            Maksut varhaiskasvatuksessa
          </a>
        </P>
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
            Jos tulosi ylittävät perhekoon mukaisen korkeimman maksun tulorajan,
            hyväksy korkein varhaiskasvatusmaksu. Tällöin sinun ei tarvitse
            selvittää tulojasi lainkaan.
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
            href="https://www.espoo.fi/fi/kasvatus-ja-opetus/varhaiskasvatus/varhaiskasvatuksen-asiakasmaksut#section-59617"
          >
            tästä
          </a>
          .
        </P>
        <P>* Tähdellä merkityt tiedot ovat pakollisia</P>
      </>
    ),
    childFormTitle: 'Lapsen tuloselvitys',
    childFormDescription: (
      <>
        <P>
          Varhaiskasvatuksessa olevan lapsen tuloista tulee tehdä selvitys,
          sillä tulot vaikuttavat varhaiskasvatusmaksuun. Lapsen tuloja voivat
          olla muun muassa elatusapu tai -tuki, korko- ja osinkotulot sekä
          eläke.
        </P>

        <P>
          Jos lapsella ei ole tuloja, tai perheenne on suostunut korkeimpaan
          varhaiskasvatusmaksuun, ei tätä lomaketta tule täyttää.
        </P>

        <P>* Tähdellä merkityt tiedot ovat pakollisia.</P>
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
    incomeInstructions:
      'Toimita tulotiedot vasta, kun lapsesi on saanut varhaiskasvatuspaikkapäätöksen.',
    childIncomeInfo: 'Lapsen tulotietojen voimassaoloaika',
    incomeStatementMissing:
      'Jos lapsellasi on tuloja, ilmoita se tuloselvityksellä.',
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
    childIncome: {
      childAttachments: 'Lapsen tulotiedot liitteinä *',
      additionalInfo: 'Lisätietoja lapsen tulotietoihin liittyen',
      write: 'Kirjoita'
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
      attachmentsVerificationInfo: 'Tulotiedot tarkistetaan vuoden välein.',
      estimate: 'Arvio bruttotuloistani',
      estimatedMonthlyIncome: 'Keskimääräiset tulot sisältäen lomarahat, €/kk',
      otherIncome: 'Muut tulot',
      otherIncomeDescription:
        'Mikäli sinulla on muita tuloja, tulee niistä toimittaa tositteet liitteenä. Listan tarvittavista liitteistä löydät lomakkeen alaosasta kohdasta: Tuloihin ja varhaiskasvatusmaksuihin liittyvät liitteet.',
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
          'Opiskelutodistus tai päätös työttömyyskassan opintoetuudesta / työllisyysrahaston koulutustuesta',
        CHILD_INCOME: 'Tositteet lapsen tuloista'
      }
    },
    assure: <>Vakuutan antamani tiedot oikeiksi. *</>,
    errors: {
      invalidForm:
        'Lomakkeelta puuttuu joitakin tarvittavia tietoja tai tiedot ovat virheellisiä. Ole hyvä ja tarkista täyttämäsi tiedot.',
      choose: 'Valitse vaihtoehto',
      chooseAtLeastOne: 'Valitse vähintään yksi vaihtoehto',
      deleteFailed: 'Tuloselvitystä ei voitu poistaa'
    },
    table: {
      title: 'Omat tuloselvitykseni',
      incomeStatementForm: 'Tuloselvitys',
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
        INCOME: 'Huoltajan toimittamat tulotiedot',
        CHILD_INCOME: 'Lasten tulotiedot'
      }
    },
    children: {
      title: 'Lasten tuloselvitykset',
      description: (
        <>
          Varhaiskasvatuksessa olevien lasten tuloista tulee tehdä selvitys.
          Yleisimpiä lapsen tuloja ovat elatusapu tai -tuki, korko- ja
          osinkotulot sekä eläke.
        </>
      ),
      noChildIncomeStatementsNeeded: (
        <>
          Sinulla ei tällä hetkellä ole lapsia joiden tulotiedot pitää
          ilmoittaa.
        </>
      )
    }
  },
  validationErrors: {
    ...components.validationErrors,
    ...components.datePicker.validationErrors,
    outsideUnitOperationTime: 'Yksikön aukiolo ylittyy'
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
      PREPARATORY_DAYCARE_ONLY:
        'Valmistavan opetuksen liittyvä varhaiskasvatus',
      PRESCHOOL_DAYCARE: 'Esiopetus ja liittyvä varhaiskasvatus',
      PRESCHOOL_DAYCARE_ONLY: 'Esiopetuksen liittyvä varhaiskasvatus',
      PRESCHOOL_CLUB: 'Esiopetuksen kerho',
      TEMPORARY_DAYCARE: 'Tilapäinen kokopäiväinen varhaiskasvatus',
      TEMPORARY_DAYCARE_PART_DAY: 'Tilapäinen osapäiväinen varhaiskasvatus',
      SCHOOL_SHIFT_CARE: 'Koululaisten vuorohoito'
    }
  },
  children: {
    title: 'Lapset',
    pageDescription:
      'Tällä sivulla näet lastesi varhaiskasvatukseen tai esiopetukseen liittyvät yleiset tiedot.',
    noChildren: 'Ei lapsia',
    unreadCount: 'lukematonta',
    childPicture: 'Lapsen kuva',
    serviceNeedAndDailyServiceTime: {
      title: 'Palveluntarve',
      titleWithDailyServiceTime:
        'Palveluntarve ja päivittäinen varhaiskasvatusaika'
    },
    serviceNeed: {
      title: 'Palveluntarve',
      validity: 'Voimassaoloaika',
      description: 'Kuvaus',
      unit: 'Yksikkö',
      status: 'Tila',
      empty: 'Palveluntarvetta ei määritelty'
    },
    attendanceSummary: {
      title: 'Läsnäolot',
      attendanceDays: 'Sopimuspäivät',
      warning: 'Kuukauden sopimuspäivien määrä on ylittynyt.',
      empty: 'Ei sopimuspäiviä valitulla ajanjaksolla'
    },
    dailyServiceTime: {
      title: 'Päivittäinen varhaiskasvatusaika',
      validity: 'Voimassaoloaika',
      description: 'Kuvaus',
      status: 'Tila',
      variableTime: 'Päivittäinen aika vaihtelee',
      empty: 'Ei määritelty'
    },
    placementTermination: {
      title: 'Paikan irtisanominen',
      description:
        'Irtisanoessasi paikkaa huomaathan, että mahdollinen siirtohakemus poistuu viimeisen läsnäolopäivän jälkeen. Jos tarvitset lapsellesi myöhemmin paikan, sinun tulee hakea sitä uudella hakemuksella.',
      terminatedPlacements: 'Olet irtisanonut paikan',
      invoicedDaycare: 'Maksullinen varhaiskasvatus',
      nonTerminatablePlacement:
        'Paikkaa ei voi irtisanoa verkkopalvelussa. Ota yhteyttä yksikön johtajaan.',
      until: (date: string) => `voimassa ${date}`,
      choosePlacement: 'Valitse paikka, jonka haluat irtisanoa',
      lastDayInfo:
        'Viimeinen päivä, jolloin lapsesi tarvitsee paikkaa. Paikka irtisanotaan päättymään tähän päivään.',
      lastDayOfPresence: 'Viimeinen läsnäolopäivä',
      confirmQuestion: 'Irtisanotaanko paikka?',
      confirmDescription: (date: string) =>
        `Haluatko varmasti irtisanoa paikan niin, että lapsen viimeinen läsnäolopäivä on ${date}?\nPaikan irtisanomista ei voi peruuttaa.`,
      terminate: 'Irtisano paikka'
    },
    pedagogicalDocuments: {
      title: 'Lapsen arkeen liittyviä dokumentteja',
      noDocuments: 'Ei dokumentteja',
      table: {
        date: 'Päivämäärä',
        child: 'Lapsi',
        document: 'Dokumentti',
        description: 'Kuvaus'
      },
      readMore: 'Lue lisää',
      collapseReadMore: 'Vähemmän',
      nextPage: 'Seuraava sivu',
      previousPage: 'Edellinen sivu',
      pageCount: (current: number, total: number) => `Sivu ${current}/${total}`
    },
    vasu: {
      title: 'Pedagogiset asiakirjat',
      plansTitle:
        'Varhaiskasvatussuunnitelmat ja esiopetuksen oppimissuunnitelmat',
      noVasus: 'Ei suunnitelmia',
      hojksTitle:
        'Henkilökohtaisen opetuksen järjestämistä koskevat suunnitelmat',
      otherDocumentsTitle: 'Muut asiakirjat',
      noDocuments: 'Ei asiakirjoja',
      lastModified: 'Viimeisin muokkauspäivämäärä',
      lastPublished: 'Viimeksi julkaistu huoltajalle',
      leavePage: 'Poistu',
      edited: 'muokattu',
      eventTypes: {
        PUBLISHED: 'Julkaistu huoltajalle',
        MOVED_TO_READY: 'Julkaistu Laadittu-tilaan',
        RETURNED_TO_READY: 'Palautettu Laadittu-tilaan',
        MOVED_TO_REVIEWED: 'Julkaistu Arvioitu-tilaan',
        RETURNED_TO_REVIEWED: 'Palautettu Arvioitu-tilaan',
        MOVED_TO_CLOSED: 'Päättynyt'
      },
      states: {
        DRAFT: 'Luonnos',
        READY: 'Laadittu',
        REVIEWED: 'Arvioitu',
        CLOSED: 'Päättynyt'
      },
      state: 'Suunnitelman tila',
      events: {
        DAYCARE: 'Varhaiskasvatussuunnitelman tapahtumat',
        PRESCHOOL: 'Lapsen esiopetuksen oppimissuunnitelman tapahtumat'
      },
      confidential: 'Salassapidettävä',
      noRecord: 'Ei merkintää',
      givePermissionToShareInfoVasu:
        'Tarvittaessa suunnitelman välttämättömät tiedot luovutetaan muille tiedonsaajatahoille. (Varhaiskasvatuslaki 41 §, Perusopetuslaki 41 §)',
      givePermissionToShareInfoVasuInfoText:
        'Jos haluat muuttaa suunnitelmassa mainittuja tiedonsaajatahoja, ota yhteyttä lapsesi yksikön henkilökuntaan. Sunnitelma voidaan luovuttaa uudelle varhaiskasvatuksen, esiopetuksen tai perusopetuksen järjestäjälle myös ilman huoltajan lupaa, jos se on välttämätöntä lapsen varhaiskasvatuksen, esi- tai perusopetuksen järjestämiseksi (Varhaiskasvatuslaki 41§, Perusopetuslaki 41§).',
      givePermissionToShareTitleVasu:
        'Vahvistus varhaiskasvatussuunnitelman luovuttamisesta muille tiedonsaajatahoille',
      givePermissionToShareTitleLeops:
        'Vahvistus esiopetuksen oppimissuunnitelman luovuttamisesta muille tiedonsaajatahoille',
      givePermissionToShareVasu:
        'Vahvistan, että lapsen varhaiskasvatussuunitelman saa luovuttaa kohdassa “Tiedonsaajatahot” mainituille tahoille.',
      givePermissionToShareLeops:
        'Vahvistan, että lapsen esiopetuksen oppimissuunnitelman saa luovuttaa kohdassa “Tiedonsaajatahot” mainituille tahoille.',
      givePermissionToShareInfoBase:
        'Jos haluat muuttaa suunnitelmassa mainittuja tiedonsaajatahoja, ota yhteyttä lapsesi varhaiskasvatusyksikön henkilökuntaan.',
      givePermissionToShareReminder:
        'Vahvista tiedonsaajatahot, joille suunnitelman saa luovuttaa',
      sharingVasuDisclaimer:
        'Lapsen vaihtaessa toiseen kaupungin varhaiskasvatusyksikköön, lapsen varhaiskasvatussuunnitelma ja muut varhaiskasvatuksen asiakirjat siirtyvät uuteen yksikköön (koskee myös ostopalveluna järjestettävää varhaiskasvatusta). Varhaiskasvatussuunnitelman luovuttamiseen ulkopuoliselle taholle pyydämme huoltajilta luvan. Lapsen siirtyessä yksityisen palveluntuottajan tai toisen kunnan järjestämään varhaiskasvatukseen, on varhaiskasvatussuunnitelma kuitenkin toimitettava uudelle varhaiskasvatuksen järjestäjälle myös ilman huoltajan lupaa, mikäli tiedot ovat välttämättömiä lapsen varhaiskasvatuksen järjestämiseksi (Varhaiskasvatuslaki 41 §:n 3 mom.). Lisäksi varhaiskasvatussuunnitelma on toimitettava esi- tai perusopetuksen järjestäjälle, jos se on välttämätöntä lapsen opetuksen järjestämiseksi (Varhaiskasvatuslaki 41 §, perusopetuslaki 41 §:n 3 mom.). Luovuttamisesta informoidaan huoltajaa etukäteen.',
      sharingLeopsDisclaimer:
        'Esiopetuksen oppimissuunnitelma voidaan luovuttaa uudelle varhaiskasvatuksen, esiopetuksen tai perusopetuksen järjestäjälle myös ilman huoltajan lupaa, jos se on välttämätöntä lapsen varhaiskasvatuksen, esi- tai perusopetuksen järjestämiseksi (Varhaiskasvatuslaki 41§, Perusopetuslaki 41§).',
      givePermissionToShareVasuBrief:
        'Tarvittaessa välttämättömät tiedot siirretään. (Varhaiskasvatuslaki 41 §, Perusopetuslaki 41 §)',
      givePermissionToShareLeopsBrief:
        'Tarvittaessa välttämättömät tiedot siirretään. (Varhaiskasvatuslaki 41 §, Perusopetuslaki 41 §)'
    }
  },
  accessibilityStatement: (
    <>
      <H1>Saavutettavuusseloste</H1>
      <P>
        Tämä saavutettavuusseloste koskee Espoon kaupungin varhaiskasvatuksen
        eVaka-verkkopalvelua osoitteessa{' '}
        <a href="https://espoonvarhaiskasvatus.fi">espoonvarhaiskasvatus.fi</a>.
        Espoon kaupunki pyrkii takaamaan verkkopalvelun saavutettavuuden,
        parantamaan käyttäjäkokemusta jatkuvasti ja soveltamaan asianmukaisia
        saavutettavuusstandardeja.
      </P>
      <P>
        Palvelun saavutettavuuden on arvioinut palvelun kehitystiimi, ja seloste
        on laadittu 12.4.2022.
      </P>
      <H2>Palvelun vaatimustenmukaisuus</H2>
      <P>
        Verkkopalvelu täyttää lain asettamat kriittiset
        saavutettavuusvaatimukset WCAG v2.1 -tason AA mukaisesti. Palvelu ei ole
        vielä kaikilta osin vaatimusten mukainen.
      </P>
      <H2>Toimet saavutettavuuden tukemiseksi</H2>
      <P>
        Verkkopalvelun saavutettavuus varmistetaan muun muassa seuraavilla
        toimenpiteillä:
      </P>
      <ul>
        <li>
          Saavutettavuus huomioidaan alusta lähtien suunnitteluvaiheessa, mm.
          valitsemalla palvelun värit ja kirjaisinten koot saavutettavasti.
        </li>
        <li>
          Palvelun elementit on määritelty semantiikaltaan johdonmukaisesti.
        </li>
        <li>Palvelua testataan jatkuvasti ruudunlukijalla.</li>
        <li>
          Erilaiset käyttäjät testaavat palvelua ja antavat saavutettavuudesta
          palautetta.
        </li>
        <li>
          Sivuston saavutettavuudesta huolehditaan jatkuvalla valvonnalla
          tekniikan tai sisällön muuttuessa.
        </li>
      </ul>
      <P>
        Tätä selostetta päivitetään sivuston muutosten ja saavutettavuuden
        tarkistusten yhteydessä.
      </P>
      <H2>Tunnetut saavutettavuusongelmat</H2>
      <P>
        Käyttäjät saattavat edelleen kohdata sivustolla joitakin ongelmia.
        Seuraavassa on kuvaus tunnetuista saavutettavuusongelmista. Jos huomaat
        sivustolla ongelman, joka ei ole luettelossa, otathan meihin yhteyttä.
      </P>
      <ul>
        <li>
          Palvelun päivämäärävalitsinta ja monivalintojen alasvetovalikkoa ei
          ole optimoitu käytettäväksi ruudunlukijalla.
        </li>
        <li>
          Palvelun yksikkökartassa ei pysty liikkumaan
          näppäimistöllä/ruudunlukijalla, mutta yksikköjä voi selata samassa
          näkymässä olevalta listalta. Palvelussa käytetty kartta on kolmannen
          osapuolen tuottama.
        </li>
      </ul>
      <H2>Kolmannet osapuolet</H2>
      <P>
        Verkkopalvelussa käytetään seuraavia kolmannen osapuolen palveluita,
        joiden saavutettavuudesta emme voi vastata.
      </P>
      <ul>
        <li>Keycloak käyttäjän tunnistautumispalvelu</li>
        <li>Suomi.fi-tunnistautuminen</li>
        <li>Leaflet-karttapalvelu</li>
      </ul>
      <H2>Vaihtoehtoiset asiointitavat</H2>
      <P>
        <ExternalLink
          href="https://www.espoo.fi/fi/espoon-kaupunki/asiakaspalvelu/asiointipisteet-ja-espoo-info/asiointipisteet"
          text="Espoon kaupungin asiointipisteistä"
        />{' '}
        saa apua sähköiseen asiointiin. Asiointipisteiden palveluneuvojat
        auttavat käyttäjiä, joille digipalvelut eivät ole saavutettavissa.
      </P>
      <H2>Anna palautetta</H2>
      <P>
        Jos huomaat saavutettavuuspuutteen verkkopalvelussamme, kerro siitä
        meille. Voit antaa palautetta{' '}
        <ExternalLink
          href="https://easiointi.espoo.fi/eFeedback/fi/Feedback/20-S%C3%A4hk%C3%B6iset%20asiointipalvelut"
          text="verkkolomakkeella"
        />{' '}
        tai sähköpostitse{' '}
        <a href="mailto:evakatuki@espoo.fi">evakatuki@espoo.fi</a>.
      </P>
      <H2>Valvontaviranomainen</H2>
      <P>
        Jos huomaat sivustolla saavutettavuusongelmia, anna ensin palautetta
        meille sivuston ylläpitäjille. Vastauksessa voi mennä 14 päivää. Jos et
        ole tyytyväinen saamaasi vastaukseen, tai et saa vastausta lainkaan
        kahden viikon aikana, voit antaa palautteen Etelä-Suomen
        aluehallintovirastoon. Etelä-Suomen aluehallintoviraston sivulla
        kerrotaan tarkasti, miten valituksen voi tehdä, ja miten asia
        käsitellään.
      </P>

      <P>
        <strong>Valvontaviranomaisen yhteystiedot </strong>
        <br />
        Etelä-Suomen aluehallintovirasto <br />
        Saavutettavuuden valvonnan yksikkö
        <br />
        <ExternalLink
          href="https://www.saavutettavuusvaatimukset.fi"
          text="www.saavutettavuusvaatimukset.fi"
        />
        <br />
        <a href="mailto:saavutettavuus@avi.fi">saavutettavuus@avi.fi</a>
        <br />
        puhelinnumero vaihde 0295 016 000
        <br />
        Avoinna: ma-pe klo 8.00–16.15
      </P>
    </>
  ),
  skipLinks: {
    mainContent: 'Siirry pääsisältöön',
    applyingSubNav: 'Siirry hakemusnavigaatioon'
  },
  components: componentTranslations
}
