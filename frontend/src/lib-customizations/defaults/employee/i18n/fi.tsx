// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { ReactNode } from 'react'

import FiniteDateRange from 'lib-common/finite-date-range'
import { InvoiceStatus } from 'lib-common/generated/api-types/invoicing'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import { H3, P } from 'lib-components/typography'

import components from '../../components/i18n/fi'

export const fi = {
  titles: {
    defaultTitle: 'Varhaiskasvatus',
    login: 'Kirjaudu sisään',
    ai: 'AI test',
    applications: 'Hakemukset',
    childInformation: 'Lapsen tiedot',
    employees: 'Käyttäjät',
    financeBasics: 'Talouden maksuasetukset',
    units: 'Yksiköt',
    customers: 'Asiakastiedot',
    placementDraft: 'Sijoitushahmotelma',
    decision: 'Päätöksen teko ja lähetys',
    documentTemplates: 'Lomakepohjat',
    feeDecisions: 'Maksupäätökset',
    feeDecision: 'Maksupäätös',
    feeDecisionDraft: 'Maksupäätösluonnos',
    holidayPeriod: 'Loma-aika',
    holidayPeriods: 'Loma-ajat',
    holidayAndTermPeriods: 'Loma-ajat ja toimintakaudet',
    holidayQuestionnaire: 'Loma-aikakysely',
    groupCaretakers: 'Henkilökunnan tarve ryhmässä',
    incomeStatements: 'Tuloselvitykset',
    valueDecisions: 'Arvopäätökset',
    valueDecision: 'Arvopäätös',
    valueDecisionDraft: 'Arvopäätösluonnos',
    incomeStatement: 'Tuloselvityslomake',
    invoices: 'Laskut',
    payments: 'Maksatus',
    invoice: 'Lasku',
    invoiceDraft: 'Laskuluonnos',
    reports: 'Raportit',
    messages: 'Viestit',
    caretakers: 'Henkilökunta',
    createUnit: 'Luo uusi yksikkö',
    personProfile: 'Aikuisen tiedot',
    personTimeline: 'Asiakkaan aikajana',
    personalMobileDevices: 'Henkilökohtainen eVaka-mobiili',
    preschoolTerm: 'Esiopetuksen lukukausi',
    preschoolTerms: 'Esiopetuksen lukukaudet',
    employeePinCode: 'PIN-koodin hallinta',
    preferredFirstName: 'Kutsumanimen hallinta',
    settings: 'Asetukset',
    systemNotifications: 'Tilapäinen ilmoitus',
    unitFeatures: 'Toimintojen avaukset',
    welcomePage: 'Tervetuloa eVakaan',
    assistanceNeedDecision: 'Päätös tuesta varhaiskasvatuksessa',
    assistanceNeedPreschoolDecision: 'Päätös tuesta esiopetuksessa',
    clubTerm: 'Kerhon lukukausi',
    clubTerms: 'Kerhojen lukukaudet',
    placementTool: 'Optimointityökalu'
  },
  common: {
    yes: 'Kyllä',
    no: 'Ei',
    and: 'Ja',
    loadingFailed: 'Tietojen haku epäonnistui',
    edit: 'Muokkaa',
    add: 'Lisää',
    addNew: 'Lisää uusi',
    clear: 'Tyhjennä',
    create: 'Luo',
    remove: 'Poista',
    doNotRemove: 'Älä poista',
    archive: 'Arkistoi',
    cancel: 'Peruuta',
    goBack: 'Palaa',
    confirm: 'Vahvista',
    period: 'Ajalle',
    search: 'Hae',
    select: 'Valitse',
    send: 'Lähetä',
    save: 'Tallenna',
    saving: 'Tallennetaan',
    saved: 'Tallennettu',
    all: 'Kaikki',
    continue: 'Jatka',
    statuses: {
      active: 'Aktiivinen',
      coming: 'Tulossa',
      completed: 'Päättynyt',
      conflict: 'Konflikti',
      guarantee: 'Takuupaikka'
    },
    careTypeLabels: {
      club: 'Kerho',
      preschool: 'Esiopetus',
      daycare: 'Varhaiskasvatus',
      daycare5yo: 'Varhaiskasvatus',
      preparatory: 'Valmistava',
      'backup-care': 'Varasijoitus',
      temporary: 'Tilapäinen',
      'school-shift-care': 'Koululaisten vuorohoito',
      'connected-daycare': 'Liittyvä'
    },
    providerType: {
      MUNICIPAL: 'Kunnallinen',
      PURCHASED: 'Ostopalvelu',
      PRIVATE: 'Yksityinen',
      MUNICIPAL_SCHOOL: 'Suomenkielinen opetustoimi (SUKO)',
      PRIVATE_SERVICE_VOUCHER: 'Yksityinen (palveluseteli)',
      EXTERNAL_PURCHASED: 'Ostopalvelu (muu)'
    },
    types: {
      CLUB: 'Kerho',
      FAMILY: 'Perhepäivähoito',
      GROUP_FAMILY: 'Ryhmäperhepäivähoito',
      CENTRE: 'Päiväkoti',
      PRESCHOOL: 'Esiopetus',
      DAYCARE: 'Varhaiskasvatus',
      PRESCHOOL_DAYCARE: 'Liittyvä varhaiskasvatus',
      PREPARATORY_EDUCATION: 'Valmistava esiopetus',
      PREPARATORY_DAYCARE: 'Liittyvä varhaiskasvatus',
      DAYCARE_5YO_FREE: '5v maksuton varhaiskasvatus',
      DAYCARE_5YO_PAID: 'Varhaiskasvatus (maksullinen)'
    },
    form: {
      name: 'Nimi',
      firstName: 'Etunimi',
      firstNames: 'Etunimet',
      lastName: 'Sukunimi',
      socialSecurityNumber: 'Hetu',
      birthday: 'Syntymäaika',
      dateOfDeath: 'Kuollut',
      age: 'Ikä',
      startDate: 'Alkaen',
      endDate: ' Päättyen',
      range: 'Ajalle',
      email: 'Sähköposti',
      phone: 'Puhelinnumero',
      backupPhone: 'Varapuhelinnumero',
      updatedFromVtj: 'Tiedot päivitetty VTJ:stä',
      address: 'Osoite',
      streetAddress: 'Katuosoite',
      postalCode: 'Postinumero',
      postOffice: 'Postitoimipaikka',
      invoiceRecipient: 'Laskun vastaanottaja',
      invoicingAddress: 'Laskutusosoite',
      addressRestricted: 'Osoite ei ole saatavilla turvakiellon vuoksi',
      ophPersonOid: 'OPH henkilö-OID'
    },
    expandableList: {
      others: 'muuta'
    },
    resultCount: (count: number) =>
      count > 0 ? `Hakutuloksia: ${count}` : 'Ei hakutuloksia',
    ok: 'Selvä!',
    tryAgain: 'Yritä uudestaan',
    checkDates: 'Tarkista päivämäärät',
    multipleChildren: 'Useita lapsia',
    today: 'Tänään',
    error: {
      unknown: 'Hups, jotain meni pieleen!',
      forbidden: 'Oikeudet tähän toimintoon puuttuvat',
      saveFailed: 'Muutosten tallentaminen ei onnistunut, yritä uudelleen.',
      minutes: 'Korkeintaan 59 minuuttia'
    },
    days: 'päivää',
    day: 'päivä',
    loading: 'Ladataan...',
    noResults: 'Ei hakutuloksia',
    noFirstName: 'Etunimi puuttuu',
    noLastName: 'Sukunimi puuttuu',
    noName: 'Nimi puuttuu',
    date: 'Päivämäärä',
    month: 'Kuukausi',
    year: 'Vuosi',
    code: 'Koodi',
    ready: 'Valmis',
    page: 'Sivu',
    group: 'Ryhmä',
    openExpandingInfo: 'Avaa lisätietokenttä',
    datetime: {
      weekdaysShort: ['Ma', 'Ti', 'Ke', 'To', 'Pe', 'La', 'Su'],
      week: 'Viikko',
      weekShort: 'Vk',
      monthShort: 'Kk',
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
    nb: 'Huom',
    validTo: (date: string) => `Voimassa ${date} saakka`,
    closeModal: 'Sulje ponnahdusikkuna',
    datePicker: {
      previousMonthLabel: 'Edellinen kuukausi',
      nextMonthLabel: 'Seuraava kuukausi',
      calendarLabel: 'Kalenteri'
    },
    close: 'Sulje',
    open: 'Avaa',
    copy: 'Kopioi',
    startDate: 'Aloituspäivä',
    endDate: 'Lopetuspäivä',
    retroactiveConfirmation: {
      title:
        'Olet tekemässä muutosta, joka voi aiheuttaa takautuvasti muutoksia asiakasmaksuihin.',
      checkboxLabel: 'Ymmärrän, olen asiasta yhteydessä laskutustiimiin.*'
    },
    userTypes: {
      SYSTEM: 'järjestelmä',
      CITIZEN: 'kuntalainen',
      EMPLOYEE: 'työntekijä',
      MOBILE_DEVICE: 'mobiililaite',
      UNKNOWN: 'tuntematon'
    }
  },
  header: {
    applications: 'Hakemukset',
    units: 'Yksiköt',
    search: 'Asiakastiedot',
    finance: 'Talous',
    invoices: 'Laskut',
    payments: 'Maksatus',
    incomeStatements: 'Tuloselvitykset',
    feeDecisions: 'Maksupäätökset',
    valueDecisions: 'Arvopäätökset',
    reports: 'Raportit',
    messages: 'Viestit',
    logout: 'Kirjaudu ulos'
  },
  footer: {
    cityLabel: 'Espoon kaupunki',
    linkLabel: 'Espoon varhaiskasvatus',
    linkHref: 'https://www.espoo.fi/fi/kasvatus-ja-opetus/varhaiskasvatus'
  },
  language: {
    fi: 'Suomi',
    sv: 'Ruotsi',
    en: 'Englanti'
  },
  errorPage: {
    reload: 'Lataa sivu uudelleen',
    text: 'Kohtasimme odottamattoman ongelman. Virheen tiedot on välitetty eteenpäin.',
    title: 'Jotain meni pieleen'
  },
  validationError: {
    mandatoryField: 'Pakollinen tieto',
    endDateIsMandatoryField: 'Päättymispäivä on pakollinen tieto',
    dateRange: 'Päivämäärä on virheellinen',
    invertedDateRange:
      'Aloituspäivämäärä ei saa olla lopetuspäivämäärän jälkeen',
    existingDateRangeError:
      'Päivämäärät eivät saa mennä päällekkäin jo luotujen ajanjaksojen kanssa',
    coveringDateRangeError:
      'Päivämääräväli ei saa peittää jo olemassaolevaa kokonaan',
    email: 'Sähköposti on väärässä muodossa',
    phone: 'Puhelinnumero on väärässä muodossa',
    ssn: 'Henkilötunnus on väärässä muodossa',
    time: 'Aika on väärässä muodossa',
    cents: 'Euromäärä on väärässä muodossa',
    decimal: 'Desimaaliluku on väärässä muodossa',
    startDateNotOnTerm: 'Aloituspäivän pitää kohdistua jollekin kaudelle'
  },
  login: {
    title: 'Varhaiskasvatus',
    subtitle: 'Asiakastiedot ja yksiköt',
    login: 'Kirjaudu sisään',
    loginAD: 'Espoo AD',
    loginEvaka: 'Palveluntuottaja',
    error: {
      noRole: 'Sinulla ei ole tarvittavaa roolia',
      default: 'Jokin meni vikaan'
    }
  },
  applications: {
    list: {
      title: 'Hakemukset',
      resultCount: 'Hakutuloksia',
      noResults: 'Ei hakutuloksia',
      type: 'Hakutyyppi',
      subtype: 'Osa / Koko',
      areaPlaceholder: 'Valitse alue',
      transferFilter: {
        title: 'Siirtohakemukset',
        transferOnly: 'Näytä vain siirtohakemukset',
        hideTransfer: 'Piilota siirtohakemukset',
        all: 'Ei rajausta'
      },
      voucherFilter: {
        title: 'Palvelusetelihakemukset',
        firstChoice: 'Näytä jos 1. hakutoiveena',
        allVoucher: 'Näytä kaikki palvelusetelihakemukset',
        hideVoucher: 'Piilota palvelusetelihakemukset',
        noFilter: 'Ei rajausta'
      },
      transfer: 'Siirtohakemus',
      paper: 'Paperihakemus',
      name: 'Lapsen nimi/hetu',
      dueDate: 'Käsiteltävä',
      startDate: 'Aloitus',
      unit: 'Yksikkö',
      status: 'Tila',
      note: 'Huom',
      basis: 'Perusteet',
      currentUnit: 'Nyk.',
      addNote: 'Lisää muistiinpano',
      serviceWorkerNote: 'Palveluohjauksen huomio'
    },
    actions: {
      moveToWaitingPlacement: 'Siirrä sijoitettaviin',
      returnToSent: 'Palauta saapuneisiin',
      cancelApplication: 'Poista käsittelystä',
      cancelApplicationConfirm:
        'Haluatko varmasti poistaa hakemuksen käsittelystä?',
      check: 'Tarkista',
      setVerified: 'Merkitse tarkistetuksi',
      setUnverified: 'Merkitse tarkistamattomaksi',
      createPlacementPlan: 'Sijoita',
      cancelPlacementPlan: 'Palauta sijoitettaviin',
      editDecisions: 'Päätökset',
      confirmPlacementWithoutDecision: 'Vahvista ilman päätöstä',
      sendDecisionsWithoutProposal: 'Lähetä päätökset',
      sendPlacementProposal: 'Lähetä sijoitusehdotus',
      withdrawPlacementProposal: 'Peru sijoitusehdotus',
      confirmDecisionMailed: 'Merkitse postitetuksi',
      checked: (count: number) =>
        count === 1 ? `${count} hakemus valittu` : `${count} hakemusta valittu`
    },
    distinctiveDetails: {
      SECONDARY: 'Näytä myös, jos yksikköön on haettu 2. tai 3. toiveena'
    },
    basisTooltip: {
      ADDITIONAL_INFO: 'Lisätietokentässä tekstiä',
      SIBLING_BASIS: 'Käyttää sisarusperustetta',
      ASSISTANCE_NEED: 'Tuen tarve',
      CLUB_CARE: 'Edellisenä toimintakautena ollut kerhopaikka',
      DAYCARE: 'On ilmoittanut luopuvansa varhaiskasvatuspaikasta',
      EXTENDED_CARE: 'Vuorotyö',
      DUPLICATE_APPLICATION: 'Tuplahakemus',
      URGENT: 'Kiireellinen hakemus',
      HAS_ATTACHMENTS: 'Hakemuksessa liite'
    },
    types: {
      PRESCHOOL: 'Esiopetushakemus',
      DAYCARE: 'Varhaiskasvatushakemus',
      CLUB: 'Kerhohakemus',
      PRESCHOOL_ONLY: 'Esiopetus',
      PRESCHOOL_DAYCARE: 'Esiopetus & liittyvä',
      PRESCHOOL_CLUB: 'Esiopetuksen kerho',
      PREPARATORY_ONLY: 'Valmistava',
      PREPARATORY_DAYCARE: 'Valmistava & liittyvä',
      DAYCARE_ONLY: 'Myöhemmin haettu liittyvä',
      ALL: 'Kaikki'
    },
    searchPlaceholder: 'Haku nimellä, hetulla tai osoitteella',
    basis: 'Huomiot',
    distinctions: 'Tarkennettu haku',
    secondaryTooltip: 'Valitse ensin toimipaikka'
  },
  application: {
    tabTitle: 'Hakemus',
    messageSubject: (date: string, name: string) => `Hakemus ${date}: ${name}`,
    types: {
      PRESCHOOL: 'Esiopetushakemus',
      DAYCARE: 'Varhaiskasvatushakemus',
      CLUB: 'Kerhohakemus',
      PRESCHOOL_DAYCARE: 'Liittyvä varhaiskasvatus',
      PREPARATORY_EDUCATION: 'Valmistava opetus',
      ALL: 'Kaikki'
    },
    statuses: {
      CREATED: 'Luonnos',
      SENT: 'Saapunut',
      WAITING_PLACEMENT: 'Odottaa sijoitusta',
      WAITING_DECISION: 'Päätöksen valmistelu',
      WAITING_UNIT_CONFIRMATION: 'Johtajan tarkistettavana',
      WAITING_MAILING: 'Odottaa postitusta',
      WAITING_CONFIRMATION: 'Vahvistettavana huoltajalla',
      ACTIVE: 'Paikka vastaanotettu',
      REJECTED: 'Paikka hylätty',
      CANCELLED: 'Poistettu käsittelystä',
      ALL: 'Kaikki'
    },
    selectAll: 'Valitse kaikki',
    unselectAll: 'Poista valinnat',
    transfer: 'Siirtohakemus',
    origins: {
      ELECTRONIC: 'Sähköinen hakemus',
      PAPER: 'Paperihakemus'
    },
    person: {
      name: 'Nimi',
      ssn: 'Henkilötunnus',
      dob: 'Syntymäaika',
      address: 'Osoite',
      restricted: 'Turvakielto voimassa',
      hasFutureAddress:
        'Väestorekisterissä oleva osoite on muuttunut / muuttumassa',
      futureAddress: 'Tuleva osoite',
      movingDate: 'Muuttopäivä',
      nationality: 'Kansalaisuus',
      language: 'Kieli',
      phone: 'Puhelinnumero',
      email: 'Sähköposti',
      agreementStatus: 'Sovittu yhdessä',
      otherGuardianAgreementStatuses: {
        AGREED: 'Sovittu yhdessä',
        NOT_AGREED: 'Ei ole sovittu yhdessä',
        RIGHT_TO_GET_NOTIFIED: 'Vain tiedonsaantioikeus',
        NOT_SET: 'Huoltajat asuvat samassa osoitteessa'
      },
      noOtherChildren: 'Ei muita lapsia',
      applicantDead: 'Hakija kuollut'
    },
    serviceNeed: {
      title: 'Palvelun tarve',
      startDate: 'Toivottu aloituspäivä',
      connectedLabel: 'Liittyvä varhaiskasvatus',
      connectedValue: 'Haen myös liittyvää varhaiskasvatusta',
      connectedDaycarePreferredStartDateLabel:
        'Liittyvän varhaiskasvatuksen toivottu aloituspäivä',
      connectedDaycareServiceNeedOptionLabel: 'Täydentävän palveluntarve',
      dailyTime: 'Päivittäinen läsnäoloaika',
      startTimePlaceholder: '08:00',
      endTimePlaceholder: '16:00',
      shiftCareLabel: 'Ilta- ja vuorohoito',
      shiftCareNeeded: 'Tarvitaan ilta- ja vuorohoitoa',
      shiftCareWithAttachments: 'Tarvitaan ilta- ja vuorohoitoa, liitteet:',
      urgentLabel: 'Kiireellinen hakemus',
      notUrgent: 'Ei',
      isUrgent: 'On kiireellinen',
      isUrgentWithAttachments: 'On kiireellinen, liitteet:',
      missingAttachment: 'Liite puuttuu',
      preparatoryLabel: 'Valmistava opetus',
      preparatoryValue: 'Haen myös valmistavaan opetukseen',
      assistanceLabel: 'Tuen tarve',
      assistanceValue: 'Lapsella on tuen tarve',
      assistanceDesc: 'Tuen tarpeen kuvaus',
      partTime: 'Osapäiväinen',
      fullTime: 'Kokopäiväinen',
      partTimeLabel: 'Osa- tai kokopäiväinen',
      error: {
        getServiceNeedOptions: 'Palveluntarpeiden haku epäonnistui!'
      }
    },
    clubDetails: {
      wasOnClubCareLabel: 'Kerhossa edellisenä toimintakautena',
      wasOnClubCareValue:
        'Lapsi on ollut kerhossa edellisen toimintakauden aikana',
      wasOnDaycareLabel: 'Varhaiskasvatuksessa ennen kerhoa',
      wasOnDaycareValue:
        'Lapsi on varhaiskasvatuksessa ennen kerhon toivottua alkamispäivää'
    },
    preferences: {
      title: 'Hakutoive',
      preferredUnits: 'Hakutoiveet',
      missingPreferredUnits: 'Valitse vähintään yksi hakutoive',
      unitMismatch: 'Hakutoiveet eivät vastaa haettavia yksiköitä',
      unitsOnMap: 'Yksiköt kartalla',
      siblingBasisLabel: 'Sisarusperuste',
      siblingBasisValue: 'Haen paikkaa sisarusperusteella',
      siblingName: 'Sisaruksen nimi',
      siblingSsn: 'Sisaruksen henkilötunnus'
    },
    child: {
      title: 'Lapsen tiedot'
    },
    guardians: {
      title: 'Hakijan tiedot',
      appliedGuardian: 'Hakijan tiedot',
      secondGuardian: {
        title: 'Ilmoitetun toisen aikuisen tiedot',
        checkboxLabel: 'Hakija on ilmoittanut toisen huoltajan tiedot',
        exists: 'Lapsella on toinen huoltaja',
        sameAddress: 'Toinen huoltaja asuu samassa osoitteessa',
        separated: 'Toinen huoltaja asuu eri osoitteessa',
        agreed: 'Hakemuksesta on sovittu yhdessä',
        noVtjGuardian: 'Vtj:n mukaan lapsella ei ole toista huoltajaa'
      },
      vtjGuardian: 'VTJ:n mukaisen toisen huoltajan tiedot'
    },
    otherPeople: {
      title: 'Muut henkilöt',
      adult: 'Muu aikuinen',
      spouse: 'Hakija asuu yhdessä muun avio- tai avopuolison kanssa',
      children: 'Muut samassa taloudessa asuvat lapset',
      addChild: 'Lisää lapsi'
    },
    additionalInfo: {
      title: 'Lisätiedot',
      applicationInfo: 'Hakemuksen lisätiedot',
      allergies: 'Allergiat',
      diet: 'Erityisruokavalio',
      maxFeeAccepted: 'Suostumus korkeimpaan maksuun'
    },
    decisions: {
      title: 'Päätökset',
      noDecisions: 'Hakemukseen ei vielä liity päätöksiä.',
      type: 'Päätöksen tyyppi',
      types: {
        CLUB: 'Kerhopäätös',
        DAYCARE: 'Varhaiskasvatuspäätös',
        DAYCARE_PART_TIME: 'Varhaiskasvatuspäätös (osapäiväinen)',
        PRESCHOOL: 'Esiopetuspäätös',
        PRESCHOOL_DAYCARE: 'Liittyvä varhaiskasvatuspäätös',
        PRESCHOOL_CLUB: 'Esiopetuksen kerho',
        PREPARATORY_EDUCATION: 'Valmistavan opetuksen päätös'
      },
      num: 'Päätösnumero',
      status: 'Päätöksen tila',
      statuses: {
        PENDING: 'Vahvistettavana huoltajalla',
        ACCEPTED: 'Vastaanotettu',
        REJECTED: 'Hylätty'
      },
      unit: 'Päätösyksikkö',
      download: 'Lataa päätös PDF-tiedostona',
      downloadPending:
        'Päätöksen PDF-tiedosto ei ole vielä ladattavissa. Yritä myöhemmin uudelleen.',
      response: {
        label: 'Vahvistus kuntalaisen puolesta',
        accept: 'Huoltaja on vastaanottanut paikan',
        reject: 'Huoltaja on hylännyt paikan',
        submit: 'Vahvista kuntalaisen puolesta',
        acceptError:
          'Paikan vastaanottaminen ei onnistunut. Päivämäärä saattaa olla virheellinen.',
        rejectError:
          'Paikan hylkääminen ei onnistunut. Päivitä sivu ja yritä uudelleen.'
      },
      blocked:
        'Tämän päätöksen voi hyväksyä vasta esiopetuspäätöksen hyväksymisen jälkeen'
    },
    attachments: {
      title: 'Liitteet',
      none: 'Hakemukseen ei liity liitteitä',
      name: 'Tiedostonimi',
      updated: 'Muutettu',
      contentType: 'Tyyppi',
      receivedByPaperAt: 'Toimitettu paperisena',
      receivedAt: 'Toimitettu sähköisesti'
    },
    state: {
      title: 'Hakemuksen tila',
      status: 'Hakemuksen tila',
      origin: 'Hakemuksen lähetysmuoto',
      sent: 'Saapunut',
      modified: 'Muokattu viimeksi',
      due: 'Käsiteltävä viimeistään'
    },
    date: {
      DUE: 'Hakemus käsiteltävä viimeistään',
      START: 'Aloitustarve',
      ARRIVAL: 'Hakemus saapunut'
    },
    notes: {
      add: 'Lisää muistiinpano',
      newNote: 'Uusi muistiinpano',
      created: 'Luotu',
      editing: 'Muokattavana',
      lastEdited: 'Muokattu viimeksi',
      placeholder: 'Kirjoita muistiinpano',
      confirmDelete: 'Haluatko varmasti poistaa muistiinpanon',
      sent: 'Lähetetty',
      message: 'viesti',
      error: {
        save: 'Muistiinpanon tallentaminen epäonnnistui',
        remove: 'Muistiinpanon poistaminen epäonnnistui'
      }
    },
    messaging: {
      sendMessage: 'Lähetä viesti'
    }
  },
  childInformation: {
    restrictedDetails: 'Turvakielto',
    personDetails: {
      title: 'Henkilö-, yhteys- ja terveystiedot',
      attendanceReport: 'Läsnä- ja poissaolotiedot',
      name: 'Lapsen nimi',
      email: 'Sähköposti',
      socialSecurityNumber: 'Henkilötunnus',
      birthday: 'Syntymäaika',
      language: 'Kieli (VTJ)',
      address: 'Osoite',
      familyLink: 'Perheen tiedot',
      languageAtHome: 'Kotikieli, jos muu kuin VTJ:ssä mainittu',
      specialDiet: 'Ruokatilausintegraatiossa käytettävä erityisruokavalio',
      mealTexture: 'Ruokatilausintegraatiossa käytettävä ruoan rakenne',
      placeholder: {
        languageAtHome: 'Valitse kieli',
        languageAtHomeDetails: 'Lisätiedot kotikielestä',
        specialDiet: 'Valitse erityisruokavalio',
        mealTexture: 'Valitse ruoan rakenne'
      }
    },
    familyContacts: {
      title: 'Perheen yhteystiedot ja varahakijat',
      contacts: 'Yhteystiedot',
      name: 'Nimi',
      role: 'Rooli',
      roles: {
        LOCAL_GUARDIAN: 'Huoltaja',
        LOCAL_FOSTER_PARENT: 'Sijaisvanhempi',
        LOCAL_ADULT: 'Aikuinen samassa taloudessa',
        LOCAL_SIBLING: 'Lapsi',
        REMOTE_GUARDIAN: 'Huoltaja',
        REMOTE_FOSTER_PARENT: 'Sijaisvanhempi'
      },
      contact: 'S-posti ja puhelin',
      contactPerson: 'Yhteyshlö',
      address: 'Osoite',
      backupPhone: 'Varanro'
    },
    timeBasedStatuses: {
      ACTIVE: 'Aktiivinen',
      ENDED: 'Päättynyt',
      UPCOMING: 'Tuleva'
    },
    serviceNeed: {
      title: 'Palveluntarve',
      dateRange: 'Palveluntarve ajalle',
      hoursPerWeek: 'Viikottainen palveluntarve',
      hoursPerWeekInfo:
        'Kirjoita tähän viikoittainen palveluntarve, joka sisältää kokonaisuudessaan perheen ilmoittaman läsnäoloajan, mukaan lukien mahdollisen esiopetusajan, 5-vuotiaan maksuttoman varhaiskasvatuksen ja valmistavan opetuksen.',
      hoursInWeek: 'h / viikko',
      serviceNeedDetails: 'Tarkennus palveluntarpeeseen',
      createdByName: 'Yksikön johtajan vahvistus',
      create: 'Luo uusi palveluntarve',
      removeServiceNeed: 'Haluatko poistaa palveluntarpeen?',
      previousServiceNeeds: 'Aiemmin luodut palveluntarpeet',
      errors: {
        conflict:
          'Palveluntarve menee päällekkäin toisen palveluntarpeen kanssa.',
        hardConflict:
          'Palveluntarve menee päällekkäin toisen palveluntarpeen alkupäivämäärän kanssa.',
        checkHours: 'Tarkista',
        placementMismatchWarning:
          'Viikottainen palveluntarve ei vastaa sijoituksen toimintamuotoa.',
        autoCutWarning:
          'Aiemmat päällekkäiset palveluntarpeet katkaistaan automaattisesti.'
      }
    },
    dailyServiceTimes: {
      title: 'Päivittäinen varhaiskasvatusaika',
      info: 'Kirjoita tähän varhaiskasvatussopimuksella ilmoitettu päivittäinen varhaiskasvatusaika, sisältäen esiopetuksen / valmistavan opetuksen / 5-vuotiaiden maksuttoman varhaiskasvatuksen.',
      info2:
        'Älä päivitä varhaiskasvatusaikaa, jos uudessa sopimuksessa ilmoitettu varhaiskasvatusaika ei ole muuttunut aiemmasta.',
      info3:
        'Epäsäännölliset ja säännölliset poissaolot merkitään päiväkirjalle.',
      create: 'Luo uusi varhaiskasvatusaika',
      types: {
        REGULAR: 'Säännöllinen varhaiskasvatusaika',
        IRREGULAR: 'Epäsäännöllinen varhaiskasvatusaika',
        VARIABLE_TIME: 'Vaihteleva varhaiskasvatusaika'
      },
      weekdays: {
        monday: 'maanantai',
        tuesday: 'tiistai',
        wednesday: 'keskiviikko',
        thursday: 'torstai',
        friday: 'perjantai',
        saturday: 'lauantai',
        sunday: 'sunnuntai'
      },
      errors: {
        required: 'Pakollinen tieto'
      },
      dailyServiceTime: 'Päivittäinen varhaiskasvatusaika',
      validityPeriod: 'Päivittäinen varhaiskasvatusaika voimassa',
      validFrom: 'Päivittäinen varhaiskasvatusaika voimassa alkaen',
      validUntil: 'Päivittäisen varhaiskasvatusajan voimassaolo päättyy',
      createNewTimes: 'Luo uusi päivittäinen varhaiskasvatusaika',
      deleteModal: {
        title: 'Poistetaanko varhaiskasvatusaika?',
        description:
          'Haluatko varmasti poistaa päivittäisen varhaiskasvatusajan? Aikaa ei saa palautettua, vaan se tulee poiston jälkeen tarvittaessa lisätä uudelleen.',
        deleteBtn: 'Poista aika'
      },
      retroactiveModificationWarning:
        'Huom! Olet muokkaamassa päivittäistä varhaiskasvatusaikaa takautuvasti. Lapsen läsnäolokalenterin merkinnät saattavat muuttua tällä aikavälillä.'
    },
    assistance: {
      title: 'Tuen tarve ja tukitoimet',
      fields: {
        validDuring: 'Voimassaoloaika',
        status: 'Tila',
        capacityFactor: 'Kerroin',
        level: 'Taso',
        otherAssistanceMeasureType: 'Toimi'
      },
      validationErrors: {
        overlap:
          'Tälle ajanjaksolle on jo päällekkäinen merkintä. Muokkaa tarvittaessa edellistä ajanjaksoa'
      },
      types: {
        daycareAssistanceLevel: {
          GENERAL_SUPPORT: 'Yleinen tuki, ei päätöstä',
          GENERAL_SUPPORT_WITH_DECISION: 'Yleinen tuki, päätös tukipalveluista',
          INTENSIFIED_SUPPORT: 'Tehostettu tuki',
          SPECIAL_SUPPORT: 'Erityinen tuki'
        },
        preschoolAssistanceLevel: {
          INTENSIFIED_SUPPORT: 'Tehostettu tuki',
          SPECIAL_SUPPORT:
            'Erityinen tuki ilman pidennettyä oppivelvollisuutta',
          SPECIAL_SUPPORT_WITH_DECISION_LEVEL_1:
            'Erityinen tuki ja pidennetty oppivelvollisuus - muu (Koskeen)',
          SPECIAL_SUPPORT_WITH_DECISION_LEVEL_2:
            'Erityinen tuki ja pidennetty oppivelvollisuus - kehitysvamma 2 (Koskeen)'
        },
        otherAssistanceMeasureType: {
          TRANSPORT_BENEFIT: 'Kuljetusetu (esioppilailla Koski-tieto)',
          ACCULTURATION_SUPPORT: 'Lapsen kotoutumisen tuki (ELY)',
          ANOMALOUS_EDUCATION_START: 'Opetuksen poikkeava aloittamisajankohta',
          CHILD_DISCUSSION_OFFERED: 'Lapset puheeksi tarjottu',
          CHILD_DISCUSSION_HELD: 'Lapset puheeksi pidetty',
          CHILD_DISCUSSION_COUNSELING: 'Lapset puheeksi neuvonpito'
        }
      },
      assistanceFactor: {
        title: 'Tuen kerroin',
        create: 'Luo uusi tuen kertoimen ajanjakso',
        removeConfirmation: 'Haluatko poistaa tuen kertoimen ajanjakson?',
        info: (): React.ReactNode => undefined
      },
      daycareAssistance: {
        title: 'Tuen taso varhaiskasvatuksessa',
        create: 'Luo uusi tuen tason ajanjakso (varhaiskasvatus)',
        removeConfirmation: 'Haluatko poistaa tuen tason ajanjakson?'
      },
      preschoolAssistance: {
        title: 'Tuen taso esiopetuksessa',
        create: 'Luo uusi tuen tason ajanjakso (esiopetus)',
        removeConfirmation: 'Haluatko poistaa tuen tason ajanjakson?'
      },
      otherAssistanceMeasure: {
        title: 'Muut toimet',
        create: 'Lisää muu toimi',
        removeConfirmation: 'Haluatko poistaa muun toimen?',
        infoList: 'Lisätietoja muista toimista:',
        info: {
          TRANSPORT_BENEFIT: (): React.ReactNode => undefined,
          ACCULTURATION_SUPPORT: (): React.ReactNode => undefined,
          ANOMALOUS_EDUCATION_START: (): React.ReactNode => undefined,
          CHILD_DISCUSSION_OFFERED: (): React.ReactNode => undefined,
          CHILD_DISCUSSION_HELD: (): React.ReactNode => undefined,
          CHILD_DISCUSSION_COUNSELING: (): React.ReactNode => undefined
        }
      }
    },
    assistanceNeed: {
      title: 'Tuen tarve',
      fields: {
        dateRange: 'Tuen tarve ajalle',
        capacityFactor: 'Lapsen tuen kerroin',
        capacityFactorInfo:
          'Kapasiteetti määräytyy yleensä lapsen iän ja palveluntarpeen mukaan. Mikäli lapsella on sellainen tuki, joka käyttää kapasiteettia enemmän, lisää tuen kerroin tähän kohtaan. Esim. erityisryhmässä tukea tarvitsevan lapsen kerroin on 2,33' as ReactNode,
        bases: 'Perusteet'
      },
      create: 'Luo uusi tuen tarpeen ajanjakso',
      removeConfirmation: 'Haluatko poistaa tuen tarpeen?',
      errors: {
        invalidCoefficient: 'Virheellinen kerroin.',
        conflict: 'Tuen tarve menee päällekkäin toisen tuen tarpeen kanssa.',
        hardConflict:
          'Tuen tarve menee päällekkäin toisen tuen tarpeen alkupäivämäärän kanssa.',
        autoCutWarning:
          'Aiemmat päällekkäiset tuentarpeet katkaistaan automaattisesti.'
      }
    },
    assistanceAction: {
      title: 'Tukitoimet ja toimenpiteet',
      fields: {
        dateRange: 'Tukitoimien voimassaoloaika',
        actions: 'Tukitoimet',
        actionTypes: {
          OTHER: 'Muu tukitoimi'
        },
        otherActionPlaceholder:
          'Voit kirjoittaa tähän lisätietoa muista tukitoimista.'
      },
      create: 'Luo uusi tukitoimien ajanjakso',
      removeConfirmation: 'Haluatko poistaa tukitoimien ajanjakson?',
      errors: {
        conflict: 'Tukitoimet menevät päällekkäin toisen ajanjakson kanssa.',
        hardConflict:
          'Tukitoimet menevät päällekkäin toisen ajanjakson alkupäivämäärän kanssa.',
        autoCutWarning:
          'Aiemmat päällekkäiset tukitoimet katkaistaan automaattisesti.'
      }
    },
    childDocuments: {
      title: 'Lapsen pedagogiset lomakkeet',
      table: {
        document: 'Lomake',
        status: 'Tila',
        open: 'Avaa lomake',
        modified: 'Muokattu',
        published: 'Julkaistu'
      },
      addNew: 'Luo uusi lomake',
      select: 'Valitse lomake',
      removeConfirmation: 'Haluatko varmasti poistaa dokumentin?',
      statuses: {
        DRAFT: 'Luonnos',
        PREPARED: 'Laadittu',
        COMPLETED: 'Valmis'
      },
      editor: {
        lockedErrorTitle: 'Asiakirja on tilapäisesti lukittu',
        lockedError:
          'Toinen käyttäjä muokkaa asiakirjaa. Yritä myöhemmin uudelleen.',
        lockedErrorDetailed: (modifiedByName: string, opensAt: string) =>
          `Käyttäjä ${modifiedByName} on muokkaamassa asiakirjaa. Asiakirjan lukitus vapautuu ${opensAt} mikäli muokkaamista ei jatketa. Yritä myöhemmin uudelleen.`,
        preview: 'Esikatsele',
        publish: 'Julkaise huoltajalle',
        publishConfirmTitle: 'Haluatko varmasti julkaista huoltajalle?',
        publishConfirmText:
          'Huoltaja saa nähdäkseen tämänhetkisen version. Tämän jälkeen tekemäsi muutokset eivät näy huoltajalle ennen kuin julkaiset uudelleen.',
        goToNextStatus: {
          DRAFT: 'Julkaise luonnos-tilassa',
          PREPARED: 'Julkaise laadittu-tilassa',
          COMPLETED: 'Julkaise valmis-tilassa'
        },
        goToNextStatusConfirmTitle: {
          DRAFT: 'Haluatko varmasti julkaista asiakirjan luonnos-tilassa?',
          PREPARED: 'Haluatko varmasti julkaista asiakirjan laadittu-tilassa?',
          COMPLETED: 'Haluatko varmasti julkaista asiakirjan valmis-tilassa?'
        },
        goToCompletedConfirmText:
          'Huoltaja saa nähdäkseen tämänhetkisen version. Valmis-tilassa olevaa asiakirjaa ei voi enää muokata.',
        goToPrevStatus: {
          DRAFT: 'Palauta luonnokseksi',
          PREPARED: 'Palauta laadituksi',
          COMPLETED: 'Palauta valmiiksi'
        },
        goToPrevStatusConfirmTitle: {
          DRAFT: 'Haluatko varmasti palauttaa asiakirjan luonnokseksi?',
          PREPARED: 'Haluatko varmasti palauttaa asiakirjan laadituksi?',
          COMPLETED: 'Haluatko varmasti palauttaa asiakirjan valmiiksi?'
        },
        deleteDraft: 'Poista luonnos',
        deleteDraftConfirmTitle: 'Haluatko varmasti poistaa luonnoksen?',
        fullyPublished: 'Asiakirjan viimeisin versio on julkaistu',
        notFullyPublished: (publishedAt: HelsinkiDateTime | null) =>
          `Asiakirjassa on julkaisemattomia muutoksia ${
            publishedAt ? ` (julkaistu ${publishedAt.format()})` : ''
          }`
      }
    },
    assistanceNeedPreschoolDecision: {
      sectionTitle: 'Päätökset tuesta esiopetuksessa',
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
      confidential: 'Salassa pidettävä',
      lawReference: 'JulkL 24.1 §',
      types: {
        NEW: 'Erityinen tuki alkaa',
        CONTINUING: 'Erityinen tuki jatkuu',
        TERMINATED: 'Erityinen tuki päättyy'
      },
      decidedAssistance: 'Päätettävä tuki',
      type: 'Erityisen tuen tila',
      validFrom: 'Voimassa alkaen',
      validFromInfo: (): React.ReactNode => (
        <ul>
          <li>
            Erityinen tuki alkaa merkitään huoltajien kuulemispäivämäärästä tai
            esiopetuksen alkamispäivästä (jos päätös tehdään ennen esiopetuksen
            alkua)
          </li>
          <li>
            Erityinen tuki jatkuu merkitään, kun lapsi vaihtaa
            esiopetusyksikköä/tukimuotoihin (esim. lapsi siirtyy erityisryhmään)
            tulee muutoksia/saa päätöksen koululykkäyksestä
          </li>
          <li>
            Erityinen tuki päättyy merkitään, kun erityinen tuki esiopetuksessa
            puretaan
          </li>
        </ul>
      ),
      validTo: 'Voimassa päättyen',
      extendedCompulsoryEducationSection: 'Pidennetty oppivelvollisuus',
      extendedCompulsoryEducation:
        'Kyllä, lapsella on pidennetty oppivelvollisuus',
      no: 'Ei',
      extendedCompulsoryEducationInfo:
        'Lisätiedot pidennetystä oppivelvollisuudesta',
      extendedCompulsoryEducationInfoInfo: (): React.ReactNode => 'infoa',
      grantedAssistanceSection:
        'Myönnettävät tulkitsemis- ja avustajapalvelut tai erityiset apuvälineet',
      grantedAssistanceSectionInfo: (): React.ReactNode =>
        'Merkitään jos lapselle myönnetään avustamis-/tulkitsemispalveluita tai apuvälineitä. Kirjataan perusteluihin ”Lapselle myönnetään perusopetuslain 31§ mukaisena tukipalveluna avustamispalvelua/tarvittavat erityiset apuvälineet/tulkitsemispalvelua/opetuksen poikkeava järjestäminen” sekä lyhyt perustelu.',
      grantedAssistanceService: 'Lapselle myönnetään avustajapalveluita',
      grantedInterpretationService: 'Lapselle myönnetään tulkitsemispalveluita',
      grantedAssistiveDevices: 'Lapselle myönnetään erityisiä apuvälineitä',
      grantedNothing: 'Ei valintaa',
      grantedServicesBasis:
        'Perustelut myönnettäville tulkitsemis- ja avustajapalveluille ja apuvälineille',
      selectedUnit: 'Esiopetuksen järjestämispaikka',
      primaryGroup: 'Pääsääntöinen opetusryhmä',
      primaryGroupInfo: (): React.ReactNode =>
        'Kirjaa tähän ryhmän muoto erityisryhmä/pedagogisesti vahvistettu ryhmä/esiopetusryhmä/3-5-vuotiaiden ryhmä.',
      decisionBasis: 'Perustelut päätökselle',
      decisionBasisInfo: (): React.ReactNode =>
        'Kirjaa mihin selvityksiin päätös perustuu (pedagoginen selvitys ja/tai psykologinen tai lääketieteellinen lausunto sekä päivämäärät). Jos lapselle on myönnetty pidennetty oppivelvollisuus, kirjataan ”lapselle on tehty pidennetyn oppivelvollisuuden päätös pvm."',
      documentBasis: 'Asiakirjat, joihin päätös perustuu',
      documentBasisInfo: (): React.ReactNode =>
        'Liitteenä voi olla myös huoltajan yksilöity valtakirja, huoltajan nimi ja päivämäärä.',
      basisDocumentPedagogicalReport: 'Pedagoginen selvitys',
      basisDocumentPsychologistStatement: 'Psykologin lausunto',
      basisDocumentDoctorStatement: 'Lääkärin lausunto',
      basisDocumentSocialReport: 'Sosiaalinen selvitys',
      basisDocumentOtherOrMissing: 'Liite puuttuu, tai muu liite, mikä?',
      basisDocumentsInfo: 'Lisätiedot liitteistä',
      guardianCollaborationSection: 'Huoltajien kanssa tehty yhteistyö',
      guardiansHeardOn: 'Huoltajien kuulemisen päivämäärä',
      heardGuardians: 'Huoltajat, joita on kuultu, ja kuulemistapa',
      heardGuardiansInfo: (): React.ReactNode =>
        'Kirjaa tähän millä keinoin huoltajaa on kuultu (esim. palaveri, etäyhteys, huoltajien kirjallinen vastine, valtakirja). Jos huoltajaa ei ole kuultu, kirjaa tähän selvitys siitä, miten ja milloin hänet on kutsuttu kuultavaksi.',
      otherRepresentative:
        'Muu laillinen edustaja (nimi, puhelinnumero ja kuulemistapa)',
      viewOfGuardians: 'Huoltajien näkemys esitetystä tuesta',
      viewOfGuardiansInfo: (): React.ReactNode => (
        <div>
          <p>
            Kirjaa selkeästi huoltajien mielipide. Mikäli huoltajat ovat
            haettavista opetusjärjestelyistä eri mieltä, niin perustelut tulee
            kirjata tarkasti.
          </p>
          <p>
            Kirjaa tähän myös lapsen mielipide asiaan tai kirjaa ”lapsi ei
            ikänsä ja/tai kehitystasonsa puolesta pysty ilmaisemaan
            mielipidettään”.
          </p>
        </div>
      ),
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
    assistanceNeedDecision: {
      pageTitle: 'Päätös tuesta varhaiskasvatuksessa',
      annulmentReason: 'Päätöksen mitätöinnin perustelu',
      sectionTitle: 'Päätökset tuesta varhaiskasvatuksessa',
      description:
        'Hyväksytyt ja hylätyt päätökset tuesta näkyvät huoltajalle eVakassa.',
      table: {
        form: 'Lomake',
        inEffect: 'Voimassa',
        unit: 'Yksikkö',
        sentToDecisionMaker: 'Lähetetty päätöksen tekijälle',
        decisionMadeOn: 'Päätös tehty',
        status: 'Tila'
      },
      create: 'Luo uusi päätös',
      modal: {
        delete: 'Poista päätös',
        title: 'Poistetaanko päätös?',
        description:
          'Haluatko varmasti poistaa päätöspohjan? Kaikki päätöspohjaan täydennetyt tiedot menetetään.'
      },
      validation: {
        title: 'Päätösesityksen tarkistus',
        description:
          'Ole hyvä ja tarkista seuraavat tiedot päätösesityksestä ennen esikatselua:'
      },
      genericPlaceholder: 'Kirjoita',
      formLanguage: 'Lomakkeen kieli',
      neededTypesOfAssistance: 'Lapsen tarvitsemat tuen muodot',
      pedagogicalMotivation: 'Pedagogiset tuen muodot ja perustelut',
      pedagogicalMotivationInfo:
        'Kirjaa tähän esitys lapsen tarvitsemista pedagogisen tuen muodoista, esim. päivän rakenteeseen, päivärytmiin ja oppimisympäristöihin liityvät ratkaisut sekä pedagogiset ja erityispedagogiset ratkaisut. Perustele lyhyesti, miksi lapsi saa näitä tuen muotoja.',
      structuralMotivation: 'Rakenteelliset tuen muodot ja perustelut',
      structuralMotivationInfo:
        'Valitse lapsen tarvitsemat rakenteellisen tuen muodot. Perustele, miksi lapsi saa näitä tuen muotoja.',
      structuralMotivationOptions: {
        smallerGroup: 'Ryhmäkoon pienennys',
        specialGroup: 'Erityisryhmä',
        smallGroup: 'Pienryhmä',
        groupAssistant: 'Ryhmäkohtainen avustaja',
        childAssistant: 'Lapsikohtainen avustaja',
        additionalStaff: 'Henkilöresurssin lisäys'
      },
      structuralMotivationPlaceholder:
        'Valittujen rakenteellisten tuen muotojen kuvaus ja perustelut',
      careMotivation: 'Hoidolliset tuen muodot ja perustelut',
      careMotivationInfo:
        'Kirjaa tähän lapsen tarvitsemat hoidollisen tuen muodot, esim. menetelmät lapsen hoitoon, hoivaan ja avustamiseen huomioiden pitkäaikaissairauksien hoito, lääkitys, ruokavalio, liikkuminen ja näihin liittyvät apuvälineet. Perustele, miksi lapsi saa näitä tuen muotoja.',
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
      servicesInfo:
        'Valitse tästä lapselle esitettävät tukipalvelut. Perustele, miksi lapsi saa näitä tukipalveluja',
      servicesPlaceholder: 'Perustelut valituille tukipalveluille',
      collaborationWithGuardians: 'Huoltajien kanssa tehty yhteistyö',
      guardiansHeardOn: 'Huoltajien kuulemisen päivämäärä',
      guardiansHeard: 'Huoltajat, joita on kuultu, ja kuulemistapa',
      guardiansHeardInfo:
        'Kirjaa tähän millä keinoin huoltajaa on kuultu (esim. palaveri, etäyhteys, huoltajan kirjallinen vastine). Jos huoltajaa ei ole kuultu, kirjaa tähän selvitys siitä, miten ja milloin hänet on kutsuttu kuultavaksi, ja miten ja milloin lapsen varhaiskasvatussuunnitelma on annettu huoltajalle tiedoksi.\nKaikilla lapsen huoltajilla tulee olla mahdollisuus tulla kuulluksi. Huoltaja voi tarvittaessa valtuuttaa toisen huoltajan edustamaan itseään valtakirjalla.',
      guardiansHeardValidation: 'Kaikkia huoltajia tulee olla kuultu.',
      oneAssistanceLevel: 'Valitse vain yksi tuen taso',
      viewOfTheGuardians: 'Huoltajien näkemys esitetystä tuesta',
      viewOfTheGuardiansInfo:
        'Kirjaa tähän huoltajien näkemys lapselle esitetystä tuesta.',
      otherLegalRepresentation:
        'Muu laillinen edustaja (nimi, puhelinnumero ja kuulemistapa)',
      decisionAndValidity: 'Päätettävä tuen taso ja voimassaolo',
      futureLevelOfAssistance: 'Lapsen tuen taso jatkossa',
      assistanceLevel: {
        assistanceEnds: 'Erityinen/tehostettu tuki päättyy',
        assistanceServicesForTime: 'Tukipalvelut päätöksen voimassaolon aikana',
        enhancedAssistance: 'Tehostettu tuki',
        specialAssistance: 'Erityinen tuki'
      },
      startDate: 'Voimassa alkaen',
      startDateIndefiniteInfo:
        'Tuki on voimassa toistaiseksi alkamispäivästä alkaen.',
      startDateInfo:
        'Lapsen tuki tarkistetaan aina tuen tarpeen muuttuessa ja vähintään kerran vuodessa.',
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
      title: 'Titteli',
      tel: 'Puhelinnumero',
      disclaimer:
        'Varhaiskasvatuslain 15 e §:n mukaan tämä päätös voidaan panna täytäntöön muutoksenhausta huolimatta.',
      decisionNumber: 'Päätösnumero',
      endDateNotKnown: 'Tukipalvelun päättymisajankohta ei tiedossa',
      statuses: {
        DRAFT: 'Luonnos',
        NEEDS_WORK: 'Korjattava',
        ACCEPTED: 'Hyväksytty',
        REJECTED: 'Hylätty',
        ANNULLED: 'Mitätöity'
      },
      confidential: 'Salassa pidettävä',
      lawReference: 'Varhaiskasvatuslaki 40 §',
      noRecord: 'Ei merkintää',
      leavePage: 'Poistu',
      preview: 'Esikatsele',
      modifyDecision: 'Muokkaa',
      sendToDecisionMaker: 'Lähetä päätöksen tekijälle',
      revertToUnsent: 'Palauta takaisin lähettämättömäksi',
      sentToDecisionMaker: 'Lähetetty päätöksen tekijälle',
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
            saantitodistukseen tai tiedoksiantotodistukseen merkittynä päivänä.
            Käytettäessä tavallista sähköistä tiedoksiantoa asianosaisen
            katsotaan saaneen päätöksestä tiedon, jollei muuta näytetä
            kolmantena päivänä viestin lähettämisestä. Tiedoksisaantipäivää ei
            lueta määräaikaan. Jos määräajan viimeinen päivä on pyhäpäivä,
            itsenäisyyspäivä, vapunpäivä, joulu- tai juhannusaatto tai
            arkilauantai, saa tehtävän toimittaa ensimmäisenä arkipäivänä sen
            jälkeen.
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
              puhelinnumero ja muut asian hoitamiseksi tarvittavat yhteystiedot
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
          <H3>Oikaisuvaatimuksen toimittaminen</H3>
          <P>
            Oikaisuvaatimuskirjelmä on toimitettava oikaisuvaatimusajan kuluessa
            oikaisuvaatimusviranomaiselle. Oikaisuvaatimuskirjelmän tulee olla
            perillä oikaisuvaatimusajan viimeisenä päivänä ennen viraston
            aukiolon päättymistä. Oikaisuvaatimuksen lähettäminen postitse tai
            sähköisesti tapahtuu lähettäjän omalla vastuulla.
          </P>
        </>
      )
    },
    assistanceNeedVoucherCoefficient: {
      sectionTitle: 'Palvelusetelikerroin',
      voucherCoefficient: 'Palvelusetelikerroin',
      create: 'Aseta uusi palvelusetelikerroin',
      form: {
        title: 'Aseta uusi palvelusetelikerroin',
        editTitle: 'Muokkaa palvelusetelikerrointa',
        titleInfo:
          'Valitse palvelusetelikertoimen voimassaolopäivämäärät tuen tarpeen päätöksen mukaisesti.',
        coefficient: 'Palvelusetelikerroin (luku)',
        validityPeriod: 'Palvelusetelikerroin voimassa',
        errors: {
          previousOverlap:
            'Aiempi päällekkäinen palvelusetelikerroin katkaistaan automaattisesti.',
          upcomingOverlap:
            'Tuleva päällekkäinen palvelusetelikerroin siirretään alkamaan myöhemmin automaattisesti.',
          fullOverlap:
            'Edellinen päällekkäinen palvelusetelikerroin poistetaan automaattisesti.',
          coefficientRange: 'Kerroin tulee olla välillä 1-10'
        }
      },
      deleteModal: {
        title: 'Poistetaanko palvelusetelikerroin?',
        description:
          'Haluatko varmasti poistaa palvelusetelikertoimen? Asiakkaalle ei luoda uutta arvopäätöstä, vaikka kertoimen poistaisi, vaan sinun tulee tehdä uusi takautuva arvopäätös.',
        delete: 'Poista kerroin'
      }
    },
    application: {
      title: 'Hakemukset',
      guardian: 'Hakemuksen tekijä',
      preferredUnit: 'Haettu yksikkö',
      startDate: 'Haettu aloituspvm',
      sentDate: 'Hakemuksen saapumispvm',
      type: 'Palvelumuoto',
      types: {
        PRESCHOOL: 'Esiopetus',
        PRESCHOOL_DAYCARE: 'Liittyvä varhaiskasvatus',
        PREPARATORY_EDUCATION: 'Valmistava opetus',
        DAYCARE: 'Varhaiskasvatus',
        DAYCARE_PART_TIME: 'Varhaiskasvatus',
        CLUB: 'Kerho'
      },
      status: 'Tila',
      statuses: {
        CREATED: 'Luonnos',
        SENT: 'Saapunut',
        WAITING_PLACEMENT: 'Odottaa sijoitusta',
        WAITING_DECISION: 'Päätöksen valmistelu',
        WAITING_UNIT_CONFIRMATION: 'Odottaa johtajan hyväksyntää',
        WAITING_MAILING: 'Odottaa postitusta',
        WAITING_CONFIRMATION: 'Vahvistettavana huoltajalla',
        REJECTED: 'Paikka hylätty',
        ACTIVE: 'Paikka vastaanotettu',
        CANCELLED: 'Poistettu käsittelystä'
      },
      open: 'Avaa hakemus',
      create: {
        createButton: 'Luo uusi hakemus',
        modalTitle: 'Uusi hakemus',
        applier: 'Hakemuksen tekijä',
        personTypes: {
          GUARDIAN: 'Valitse huoltajista',
          DB_SEARCH: 'Hae asiakastiedoista',
          VTJ: 'Hae VTJ:stä',
          NEW_NO_SSN: 'Luo uusi hetuton'
        },
        applicationType: 'Hakemustyyppi',
        applicationTypes: {
          DAYCARE: 'Varhaiskasvatushakemus',
          PRESCHOOL: 'Esiopetushakemus',
          CLUB: 'Kerhohakemus'
        },
        sentDate: 'Hakemus saapunut',
        hideFromGuardian: 'Piilota hakemus huoltajalta',
        transferApplication: 'Siirtohakemus'
      }
    },
    additionalInformation: {
      title: 'Lisätietoja',
      allergies: 'Allergiat',
      diet: 'Erityisruokavalio',
      additionalInfo: 'Lisätiedot',
      preferredName: 'Kutsumanimi',
      medication: 'Lääkitys'
    },
    income: {
      title: 'Tulotiedot'
    },
    feeAlteration: {
      title: 'Alennukset, vapautukset ja korotukset',
      error: 'Maksumuutosten lataus epäonnistui',
      create: 'Luo uusi maksumuutos',
      updateError: 'Maksumuutoksen tallennus epäonnistui',
      deleteError: 'Maksumuutoksen poisto epäonnistui',
      confirmDelete: 'Haluatko poistaa maksumuutoksen?',
      editor: {
        titleNew: 'Lisää uusi alennus tai korotus',
        titleEdit: 'Muokkaa alennusta tai korotusta',
        alterationType: 'Muutostyyppi',
        alterationTypePlaceholder: 'Muutostyyppi',
        validDuring: 'Myönnetään ajalle',
        notes: 'Lisätietoja',
        cancel: 'Peruuta',
        save: 'Tallenna'
      },
      types: {
        DISCOUNT: 'Alennus',
        INCREASE: 'Korotus',
        RELIEF: 'Huojennus'
      },
      attachmentsTitle: 'Liitteet',
      employeeAttachments: {
        title: 'Lisää liitteitä',
        description:
          'Tässä voit lisätä asiakkaan toimittamia liitteitä maksujen alennuksiin, vapautuksiin tai korotuksiin.'
      }
    },
    placements: {
      title: 'Sijoitukset ja palveluntarpeet',
      placements: 'Sijoitukset',
      rowTitle: 'Sijoituspäätös voimassa',
      startDate: 'Aloituspäivämäärä',
      endDate: 'Päättymispäivämäärä',
      modifiedAt: 'Muokattu',
      modifiedBy: 'Muutoksentekijä',
      terminatedByGuardian: 'Huoltaja irtisanonut',
      terminated: 'Irtisanottu',
      area: 'Alue',
      daycareUnit: 'Toimipaikka',
      daycareGroups: 'Ryhmä',
      daycareGroupMissing: 'Ei ryhmitetty',
      type: 'Toimintamuoto',
      providerType: 'Järjestämismuoto',
      updatedAt: 'Päivitetty viimeksi',
      serviceNeedMissing1: 'Sijoitukselta puuttuu palveluntarve',
      serviceNeedMissing2:
        'päivältä. Merkitse palveluntarve koko sijoituksen ajalle.',
      serviceNeedMissingTooltip1: 'Palveluntarve puuttuu',
      serviceNeedMissingTooltip2: 'päivältä.',
      deletePlacement: {
        btn: 'Poista sijoitus',
        confirmTitle: 'Haluatko varmasti perua tämän sijoituksen?',
        hasDependingBackupCares:
          'Lapsen varasijoitus on riippuvainen tästä sijoituksesta, joten tämän sijoituksen poistaminen voi muuttaa tai poistaa varasijoituksen.'
      },
      createPlacement: {
        btn: 'Luo uusi sijoitus',
        title: 'Uusi sijoitus',
        text: 'Tästä sijoituksesta ei voi lähettää päätöstä. Jos sijoitus menee päällekkäin lapsen aiemmin luotujen sijoituksien kanssa, näitä sijoituksia lyhennetään tai ne poistetaan automaattisesti.',
        temporaryDaycareWarning: 'HUOM! Älä käytä varasijoitusta tehdessäsi!',
        unitMissing: 'Yksikkö puuttuu',
        placeGuarantee: {
          title: 'Varhaiskasvatuspaikkatakuu',
          info: 'Tulevaisuuden sijoitus liittyy varhaiskasvatuspaikkatakuuseen'
        }
      },
      error: {
        conflict: {
          title: 'Päivämäärää ei voitu muokata',
          text:
            'Lapsella on sijoitus, joka menee päällekkäin' +
            ' nyt ilmoittamiesi päivämäärien kanssa. Voit palata muokkaamaan' +
            ' ilmoittamiasi päivämääriä tai ottaa yhteyttä pääkäyttäjään.'
        }
      },
      warning: {
        overlap: 'Ajalle on jo sijoitus',
        ghostUnit: 'Yksikkö on merkitty haamuyksiköksi',
        backupCareDepends:
          'Varasijoitus on riippuvainen tästä sijoituksesta, ja muutettu aikaväli voi poistaa tai muttaa varasijoitusta.'
      },
      serviceNeeds: {
        title: 'Sijoituksen palveluntarpeet',
        period: 'Aikaväli',
        description: 'Kuvaus',
        shiftCare: 'Ilta/Vuoro',
        shiftCareTypes: {
          NONE: 'Ei',
          INTERMITTENT: 'Satunnainen',
          FULL: 'Kyllä'
        },
        partWeek: 'Osaviikkoinen',
        confirmed: 'Vahvistettu',
        createNewBtn: 'Luo uusi palveluntarve',
        addNewBtn: 'Lisää palveluntarve',
        optionPlaceholder: 'Valitse...',
        missing: 'Puuttuva palveluntarve',
        deleteServiceNeed: {
          btn: 'Poista palveluntarve',
          confirmTitle: 'Haluatko varmasti poistaa tämän palveluntarpeen?'
        },
        overlapWarning: {
          title: 'Palveluntarpeet menevät päällekkäin',
          message:
            'Merkitsemäsi palveluntarve menee päällekkäin aiemmin ilmoitetun kanssa. Mikäli vahvistat nyt merkitsemäsi palveluntarpeen, aiemmin merkitty palveluntarve katkaistaan automaattisesti päällekkäin menevältä ajalta.'
        },
        optionStartNotValidWarningTitle: (validFrom: LocalDate) =>
          `Valittu palveluntarvetyyppi on käytettävissä vasta ${validFrom.format()} alkaen`,
        optionEndNotValidWarningTitle: (validTo: LocalDate) =>
          `Valittu palveluntarvetyyppi on käytettävissä vain ${validTo.format()} asti`,
        optionStartEndNotValidWarningTitle: (validity: FiniteDateRange) =>
          `Valittu palveluntarvetyyppi on käytettävissä ajalla ${validity.format()}`,
        notFullyValidOptionWarning:
          'Valitun palveluntarvetyypin täytyy olla käytettävissä koko ajalla. Luo palveluntarve tarvittaessa kahdessa osassa.'
      }
    },
    serviceApplications: {
      title: 'Palveluntarpeen muutoshakemukset',
      applicationTitle: 'Palveluntarpeen muutoshakemus',
      sentAt: 'Lähetetty',
      sentBy: 'Hakija',
      startDate: 'Ehdotettu aloituspäivä',
      serviceNeed: 'Ehdotettu palveluntarve',
      additionalInfo: 'Lisätiedot',
      status: 'Tila',
      decision: {
        statuses: {
          ACCEPTED: 'Hyväksytty',
          REJECTED: 'Hylätty'
        },
        rejectedReason: 'Hylkäysperuste',
        accept: 'Hyväksy',
        reject: 'Hylkää',
        confirmAcceptTitle: 'Hyväksytäänkö hakemus uudesta palveluntarpeesta?',
        confirmAcceptText: (range: FiniteDateRange, placementChange: boolean) =>
          `Uusi ${placementChange ? 'sijoitus ja ' : ''}palveluntarve luodaan ajalle ${range.format()}.`,
        shiftCareLabel: 'Ilta/vuorohoito',
        shiftCareCheckbox: 'Lapsella on oikeus ilta/vuorohoitoon',
        partWeekLabel: 'Osaviikkoisuus',
        partWeekCheckbox: 'Palveluntarve on osaviikkoinen',
        confirmAcceptBtn: 'Vahvista',
        confirmRejectTitle: 'Hakemuksen hylkääminen'
      },
      decidedApplications: 'Käsitellyt hakemukset',
      noApplications: 'Ei hakemuksia'
    },
    fridgeParents: {
      title: 'Päämiehet',
      name: 'Nimi',
      ssn: 'Hetu',
      startDate: 'Alkaen',
      endDate: 'Päättyen',
      status: 'Tila'
    },
    fosterParents: {
      title: 'Sijaisvanhemmat',
      name: 'Nimi',
      ssn: 'Hetu',
      startDate: 'Alkaen',
      endDate: 'Päättyen',
      status: 'Tila'
    },
    backupCares: {
      title: 'Varasijoitukset',
      remove: 'Haluatko poistaa varasijoituksen?',
      editing: 'muokkauksessa',
      create: 'Luo uusi varasijoitus',
      dateRange: 'Varasijoitus ajalle',
      unit: 'Yksikkö',
      validationNoMatchingPlacement:
        'Varasijoitus ei ole minkään lapsen sijoituksen aikana.'
    },
    backupPickups: {
      title: 'Varahakijat',
      name: 'Varahakijan nimi',
      phone: 'Puhelinnumero',
      add: 'Lisää varahakija',
      edit: 'Muokkaa varahakijan tietoja',
      removeConfirmation: 'Haluatko varmasti poistaa varahakijan?'
    },
    childDocumentsSectionTitle: 'Pedagogiset asiakirjat',
    pedagogicalDocument: {
      title: 'Pedagoginen dokumentointi',
      explanation: '',
      explanationInfo: '',
      date: 'Päivämäärä',
      document: 'Dokumentti',
      documentInfo: '',
      description: 'Pedagoginen kuvaus',
      descriptionInfo: '',
      create: 'Lisää uusi',
      removeConfirmation: 'Haluatko poistaa dokumentin?',
      removeConfirmationText:
        'Haluatko varmasti poistaa pedagogisen dokumentin ja sen kuvaustekstin? Poistoa ei saa peruutettua, ja dokumentti poistetaan näkyvistä myös huoltajalta.'
    }
  },
  personSearch: {
    search: 'Etsi henkilötunnuksella',
    searchByName: 'Etsi henkilötunnuksella tai nimellä',
    notFound: 'Henkilöä ei löydy',
    inputPlaceholder: 'Etsi nimellä, osoitteella tai henkilötunnuksella',
    age: 'Ikä',
    address: 'Osoite',
    maxResultsFound: 'Rajaa hakua nähdäksesi muut tulokset',
    socialSecurityNumber: 'Henkilötunnus',
    newAdult: 'Luo hetuton aikuinen',
    newChild: 'Luo hetuton lapsi',
    addPersonFromVTJ: {
      title: 'Tuo henkilö VTJ:stä',
      modalConfirmLabel: 'Tuo henkilö',
      ssnLabel: 'Henkilötunnus',
      restrictedDetails: 'Henkilöllä on turvakielto',
      badRequest: 'Epäkelpo henkilötunnus',
      notFound: 'Ei tuloksia',
      unexpectedError: 'Henkilötietojen haku epäonnistui'
    },
    createNewPerson: {
      title: 'Luo hetuton henkilö',
      modalConfirmLabel: 'Luo henkilö',
      form: {
        firstName: 'Etunimi',
        lastName: 'Sukunimi',
        dateOfBirth: 'Syntymäaika',
        address: 'Osoite',
        streetAddress: 'Katuosoite',
        postalCode: 'Postinro',
        postOffice: 'Toimipaikka',
        phone: 'Puhelin',
        email: 'Sähköposti'
      }
    }
  },
  personProfile: {
    restrictedDetails: 'Turvakielto',
    timeline: 'Aikajana',
    personDetails: 'Henkilö- ja yhteystiedot',
    addSsn: 'Aseta hetu',
    noSsn: 'Hetuton',
    ssnAddingDisabledCheckbox:
      'Vain pääkäyttäjillä on oikeus asettaa lapselle henkilötunnus',
    ssnAddingDisabledInfo:
      'Palveluohjauksen ja talouden käyttäjät eivät saa asetettua lapselle henkilötunnusta. Kun henkilötunnus puuttuu, lapsella ei ole huoltajasuhdetta. Jos henkilötunnus halutaan myöhemmin asettaa, lapsen aiemmat dokumentit on poistettava järjestelmästä.',
    ssnInvalid: 'Epäkelpo henkilötunnus',
    ssnConflict: 'Tämä käyttäjä löytyy jo järjestelmästä.',
    duplicate: 'Monista hetuton',
    updateFromVtj: 'Päivitä VTJ:stä',
    partner: 'Puolisot',
    partnerInfo:
      'Samassa osoitteessa avio/avoliiton omaisessa suhteessa asuva toinen henkilö',
    partnerAdd: 'Lisää puoliso',
    forceManualFeeDecisionsLabel: 'Maksupäätösten lähettäminen',
    forceManualFeeDecisionsChecked: 'Lähetetään aina manuaalisesti',
    forceManualFeeDecisionsUnchecked: 'Automaattisesti, jos mahdollista',
    fridgeChildOfHead: 'Päämiehen alaiset alle 18v lapset',
    fridgeChildAdd: 'Lisää lapsi',
    fosterChildren: {
      sectionTitle: 'Sijaislapset',
      addFosterChildTitle: 'Lisää uusi sijaislapsi',
      addFosterChildParagraph:
        'Sijaisvanhempi näkee lapsesta samat tiedot eVakassa kuin huoltaja. Sijaislapsen saa lisätä vain sosiaalityöntekijän luvalla.',
      updateFosterChildTitle: 'Päivitä suhteen voimassaoloaikaa',
      childLabel: 'Hetu tai nimi',
      validDuringLabel: 'Voimassa',
      createError: 'Sijaislapsen lisäys epäonnistui',
      deleteFosterChildTitle: 'Sijaislapsen poisto',
      deleteFosterChildParagraph:
        'Haluatko varmasti poistaa sijaislapsen? Sijaisvanhemmuuden päättyessä merkitse suhteelle loppumisaika.'
    },
    fosterParents: 'Sijaisvanhemmat',
    applications: 'Hakemukset',
    feeDecisions: {
      title: 'Päämiehen maksupäätökset',
      createRetroactive: 'Luo takautuvia maksupäätösluonnoksia'
    },
    invoices: 'Päämiehen laskut',
    invoiceCorrections: {
      title: 'Hyvitykset ja korotukset',
      noteModalTitle: 'Talouden oma muistiinpano',
      noteModalInfo: 'Muistiinpano ei tule näkyviin laskulle.',
      invoiceStatusHeader: 'Tila',
      invoiceStatus: (status: InvoiceStatus | null) =>
        status === 'DRAFT'
          ? 'Seuraavalla luonnoksella'
          : status
            ? 'Laskulla osittain'
            : 'Ei vielä laskulla'
    },
    voucherValueDecisions: {
      title: 'Päämiehen arvopäätökset',
      createRetroactive: 'Luo takautuvia arvopäätösluonnoksia'
    },
    dependants: 'Päämiehen huollettavat',
    guardiansAndParents: 'Huoltajat ja päämiehet',
    guardians: 'Huoltajat',
    name: 'Nimi',
    ssn: 'Hetu',
    streetAddress: 'Katuosoite',
    age: 'Ikä',
    evakaRights: {
      tableHeader: 'eVaka-oikeudet',
      statusAllowed: 'Sallittu',
      statusDenied: 'Kielletty',
      editModalTitle: 'Huoltajan eVaka-oikeudet',
      modalInfoParagraph: (
        <>
          EVaka-oikeuksilla määritetään, näkeekö huoltaja huostaanotettuun
          lapseensa liittyvät tiedot eVakassa. Oikeudet voi kieltää vain{' '}
          <strong>
            perustelluissa lastensuojelutilanteissa sosiaalityöntekijän
            kirjallisella ilmoituksella
          </strong>
          . Oikeudet tulee palauttaa, mikäli huostaanotto päättyy.
        </>
      ),
      modalUpdateSubtitle:
        'Huoltajan eVaka-oikeuksien kieltäminen, kun lapsi on huostaanotettu',
      confirmedLabel:
        'Vahvistan, että huoltajan tiedonsaannin rajoittamiseen on sosiaalityöntekijän kirjallinen lupa',
      deniedLabel: 'Kiellän huostaanotetun lapsen huoltajalta eVaka-oikeudet'
    },
    familyOverview: {
      title: 'Perheen tietojen kooste',
      colName: 'Nimi',
      colRole: 'Rooli perheessä',
      colAge: 'Ikä',
      colIncome: 'Tulot',
      colAddress: 'Osoite',
      role: {
        HEAD: 'Päämies',
        PARTNER: 'Puoliso',
        CHILD: 'Lapsi'
      },
      familySizeLabel: 'Perhekoko',
      familySizeValue: (adults: number, children: number) => {
        const adultText = adults === 1 ? 'aikuinen' : 'aikuista'
        const childrenText = children === 1 ? 'lapsi' : 'lasta'
        return `${adults} ${adultText}, ${children} ${childrenText}`
      },
      incomeTotalLabel: 'Aikuisten tulot yhteensä',
      incomeValue: (val: string) => `${val} €`,
      incomeMissingCompletely: 'Tulotiedot puuttuvat'
    },
    fridgeHead: {
      error: {
        edit: {
          title: 'Päämiehen muokkaus epäonnistui!'
        }
      }
    },
    fridgePartner: {
      newPartner: 'Uusi puoliso',
      editPartner: 'Puolison muokkaus',
      removePartner: 'Puolison poisto',
      confirmText:
        'Haluatko varmasti poistaa puolison? Puolison vaihtuessa merkitse edelliselle suhteelle loppumisaika ja lisää sen jälkeen uusi puoliso',
      error: {
        remove: {
          title: 'Puolison poisto epäonnistui!'
        },
        add: {
          title: 'Puolison lisäys epäonnistui!'
        },
        edit: {
          title: 'Puolison muokkaus epäonnistui!'
        },
        conflict:
          'Osapuolilta löytyy aktiivinen suhde annetulta aikaväliltä. Nykyinen aktiivinen suhde tulee päättää ennen uuden luomista'
      },
      validation: {
        deadPerson:
          'Suhteen päättymispäivä ei voi olla henkilön kuolinpäivän jälkeen',
        deadPartner:
          'Suhteen päättymispäivä ei voi olla puolison kuolinpäivän jälkeen'
      },
      searchTitle: 'Hetu tai nimi'
    },
    fridgeChild: {
      newChild: 'Uusi lapsi',
      editChild: 'Lapsen muokkaus',
      removeChild: 'Lapsen poisto',
      confirmText:
        'Haluatko varmasti poistaa lapsen? Päämiehen vaihtuessa merkitse edelliselle suhteelle loppumisaika ja lisää sen jälkeen uusi',
      error: {
        add: {
          title: 'Lapsen lisäys epäonnistui!'
        },
        edit: {
          title: 'Lapsen muokkaus epäonnistui!'
        },
        remove: {
          title: 'Lapsen poisto epäonnistui!'
        },
        conflict:
          'Kyseisellä lapselta löytyy jo tällä aikavälillä päämies. Olemassa oleva päämiessuhde täytyy päättää ensin'
      },
      validation: {
        deadAdult:
          'Suhteen päättymispäivä ei voi olla aikuisen kuolinpäivän jälkeen',
        deadChild:
          'Suhteen päättymispäivä ei voi olla lapsen kuolinpäivän jälkeen'
      },
      searchTitle: 'Hetu tai nimi'
    },
    application: {
      child: 'Lapsi',
      preferredUnit: 'Haettu yksikkö',
      startDate: 'Haettu aloituspvm',
      sentDate: 'Hakemuksen saapumispvm',
      type: 'Palvelumuoto',
      types: {
        PRESCHOOL: 'Esiopetus',
        PRESCHOOL_WITH_DAYCARE: 'Esiopetus + liittyvä',
        PRESCHOOL_DAYCARE: 'Liittyvä varhaiskasvatus',
        PRESCHOOL_CLUB: 'Esiopetuksen kerho',
        PREPARATORY_EDUCATION: 'Valmistava opetus',
        PREPARATORY_WITH_DAYCARE: 'Valmistava opetus + liittyvä',
        DAYCARE: 'Varhaiskasvatus',
        DAYCARE_PART_TIME: 'Varhaiskasvatus',
        CLUB: 'Kerho'
      },
      status: 'Tila',
      open: 'Avaa hakemus',
      statuses: {
        CREATED: 'Luonnos',
        SENT: 'Saapunut',
        WAITING_PLACEMENT: 'Odottaa sijoitusta',
        WAITING_DECISION: 'Päätöksen valmistelu',
        WAITING_UNIT_CONFIRMATION: 'Odottaa johtajan hyväksyntää',
        WAITING_MAILING: 'Odottaa postitusta',
        WAITING_CONFIRMATION: 'Vahvistettavana huoltajalla',
        REJECTED: 'Paikka hylätty',
        ACTIVE: 'Paikka vastaanotettu',
        CANCELLED: 'Poistettu käsittelystä'
      }
    },
    decision: {
      decisions: 'Päätökset',
      decisionUnit: 'Sijoitusyksikkö',
      status: 'Tila',
      statuses: {
        PENDING: 'Odottaa vastausta',
        ACCEPTED: 'Hyväksytty',
        REJECTED: 'Hylätty'
      },
      startDate: 'Aloituspvm päätöksellä',
      sentDate: 'Päätös lähetetty'
    },
    income: {
      title: 'Tulotiedot',
      itemHeader: 'Tulotiedot ajalle',
      itemHeaderNew: 'Uusi tulotieto',
      details: {
        attachments: 'Liitteet',
        name: 'Nimi',
        updated: 'Tulotiedot päivitetty',
        handler: 'Käsittelijä',
        originApplication:
          'Huoltaja on hakemuksella suostunut korkeimpaan maksuluokkaan',
        dateRange: 'Ajalle',
        notes: 'Lisätiedot',
        effect: 'Maksun peruste',
        effectOptions: {
          MAX_FEE_ACCEPTED: 'Huoltaja on suostunut korkeimpaan maksuluokkaan',
          INCOMPLETE: 'Puutteelliset tulotiedot',
          INCOME: 'Huoltajan toimittamat tulotiedot',
          NOT_AVAILABLE: 'Puutteelliset tulotiedot'
        },
        miscTitle: 'Lisätiedot',
        incomeTitle: 'Tulot',
        income: 'Tulot',
        expensesTitle: 'Menot',
        expenses: 'Menot',
        amount: '€',
        coefficient: 'Kerroin',
        monthlyAmount: '€ / KK',
        time: 'Ajalle',
        sum: 'Yhteensä',
        entrepreneur: 'Yrittäjä',
        echa: 'Euroopan kemikaalivirasto',
        source: 'Lähde',
        createdFromApplication: 'Luotu automaattisesti hakemukselta',
        application: 'Hakemus',
        incomeCoefficients: {
          MONTHLY_WITH_HOLIDAY_BONUS: 'Kuukausi',
          MONTHLY_NO_HOLIDAY_BONUS: 'Kuukausi ilman lomarahaa',
          BI_WEEKLY_WITH_HOLIDAY_BONUS: '2 viikkoa',
          BI_WEEKLY_NO_HOLIDAY_BONUS: '2 viikkoa ilman lomarahaa',
          DAILY_ALLOWANCE_21_5: 'Päiväraha x 21,5',
          DAILY_ALLOWANCE_25: 'Päiväraha x 25',
          YEARLY: 'Vuosi'
        },
        updateError: 'Tulotietojen tallennus epäonnistui',
        missingIncomeDaysWarningTitle: 'Tulotiedot puuttuvat joiltain päiviltä',
        missingIncomeDaysWarningText: (missingIncomePeriodsString: string) =>
          `Tulotiedot puuttuvat seuraavilta päiviltä: ${missingIncomePeriodsString}. Jos tulotietoja ei lisätä, tulot määräytyvät näille päiville korkeimman maksuluokan mukaan. Tarkista päivämäärät ja lisää tarvittaessa tulotiedot puuttuville päiville.`,
        conflictErrorText:
          'Ajanjaksolle on jo tallennettu tulotietoja! Tarkista tulotietojen voimassaoloajat.',
        closeWarning: 'Muista tallentaa!',
        closeWarningText:
          'Tallenna tai peruuta muutokset ennen lomakkeen sulkemista.'
      },
      add: 'Luo uusi tulotieto',
      deleteModal: {
        title: 'Tulotiedon poisto',
        confirmText: 'Haluatko varmasti poistaa tulotiedon ajalta',
        cancelButton: 'Peruuta',
        deleteButton: 'Poista'
      },
      incomeNotifications: {
        title: 'Muistutus tulotietojen päivittämisestä lähetetty'
      }
    },
    incomeStatement: {
      title: 'Tuloselvitykset',
      custodianTitle: 'Huollettavien tuloselvitykset',
      noIncomeStatements: 'Ei tuloselvityksiä',
      incomeStatementHeading: 'Asiakkaan tuloselvityslomake',
      createdHeading: 'Saapumispäivä',
      handledHeading: 'Käsitelty',
      open: 'Avaa lomake',
      handled: 'Tuloselvitys käsitelty'
    },
    invoice: {
      validity: 'Aikaväli',
      price: 'Summa',
      status: 'Status'
    },
    downloadAddressPage: 'Lataa osoitesivu'
  },
  timeline: {
    title: 'Perheen aikajana',
    feeDecision: 'Maksupäätös',
    valueDecision: 'Arvopäätös',
    partner: 'Puoliso',
    child: 'Lapsi',
    createdAtTitle: 'Luotu',
    unknownSource: 'Luontilähde ei tiedossa',
    modifiedAtTitle: 'Muokattu',
    unknownModification: 'Muokkauksen tekijä ei tiedossa',
    notModified: 'Ei muokattu',
    user: 'Käyttäjä',
    application: 'Hakemus',
    dvvSync: 'Väestötietojärjestelmä',
    notAvailable: 'Aika ei tiedossa',
    DVV: 'Väestötietojärjestelmä synkronointi'
  },
  incomeStatement: {
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
    otherIncomeInfo: 'Arviot muista tuloista',

    entrepreneurTitle: 'Yrittäjän tulotiedot',
    fullTimeLabel: 'Onko yritystoiminta päätoimista vai sivutoimista',
    fullTime: 'Päätoimista',
    partTime: 'Sivutoimista',
    startOfEntrepreneurship: 'Yrittäjyys alkanut',
    spouseWorksInCompany: 'Työskenteleekö puoliso yrityksessä',
    startupGrant: 'Starttiraha',
    companyInfoTitle: 'Yrityksen tiedot',
    checkupConsentLabel: 'Tietojen tarkastus',
    checkupConsent:
      'Hyväksyn, että tuloihini liittyviä tietoja tarkastellaan tarvittaessa tulorekisteristä sekä Kelasta.',
    companyType: 'Toimintamuoto',
    selfEmployed: 'Toiminimi',
    selfEmployedAttachments:
      'Toimitan liitteinä yrityksen viimeisimmän tulos- ja taselaskelman tai veropäätöksen.',
    selfEmployedEstimation: 'Arvio keskimääräisistä kuukausituloista',
    limitedCompany: 'Osakeyhtiö',
    limitedCompanyIncomesRegister:
      'Tuloni voi tarkastaa suoraan tulorekisteristä sekä tarvittaessa Kelasta.',
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
      CHILD_INCOME: 'Lapsen tulotiedot'
    },
    table: {
      title: 'Käsittelyä odottavat tuloselvitykset',
      customer: 'Asiakas',
      area: 'Alue',
      created: 'Luotu',
      startDate: 'Voimassa',
      incomeEndDate: 'Tulotieto päättyy',
      type: 'Tyyppi',
      link: 'Selvitys',
      note: 'Muistiinpano'
    },
    noNote: 'Tuloselvityksellä ei muistiinpanoa',
    handlerNotesForm: {
      title: 'Käsittelijän muistiinpanot',
      handled: 'Käsitelty',
      handlerNote: 'Muistiinpano (sisäinen)'
    }
  },
  units: {
    name: 'Nimi',
    area: 'Alue',
    address: 'Osoite',
    type: 'Tyyppi',
    findByName: 'Etsi yksikön nimellä',
    selectProviderTypes: 'Valitse järjestämismuoto',
    selectCareTypes: 'Valitse toimintamuoto',
    includeClosed: 'Näytä lopetetut yksiköt',
    noResults: 'Ei tuloksia'
  },
  unit: {
    tabs: {
      unitInfo: 'Yksikön tiedot',
      groups: 'Ryhmät',
      calendar: 'Kalenteri',
      applicationProcess: 'Hakuprosessi'
    },
    create: 'Luo uusi yksikkö',
    openDetails: 'Näytä yksikön kaikki tiedot',
    occupancies: 'Käyttö- ja täyttöaste',
    info: {
      title: 'Yksikön tiedot',
      area: 'Alue',
      visitingAddress: 'Käyntiosoite',
      mailingAddress: 'Postiosoite',
      phone: 'Puhelinnumero',
      caretakers: {
        titleLabel: 'Henkilökuntaa',
        unitOfValue: 'henkilöä'
      }
    },
    manager: {
      title: 'Yksikön johtaja',
      name: 'Nimi',
      email: 'Sähköpostiosoite',
      phone: 'Puhelinnumero'
    },
    accessControl: {
      unitSupervisors: 'Yksikön johtajat',
      specialEducationTeachers: 'Varhaiskasvatuksen erityisopettajat',
      earlyChildhoodEducationSecretary: 'Varhaiskasvatussihteerit',
      staff: 'Yksikön henkilökunta',
      email: 'Sähköpostiosoite',
      removeConfirmation:
        'Haluatko poistaa pääsyoikeuden valitulta henkilöltä?',
      removeTemporaryEmployeeConfirmation:
        'Haluatko poistaa listalta valitun henkilön?',
      addDaycareAclModal: {
        title: 'Lisää henkilö',
        type: {
          PERMANENT: 'Henkilökunta',
          TEMPORARY: 'Tilapäinen sijainen'
        },
        groups: 'Valitse ryhmä',
        employees: 'Valitse henkilö',
        firstName: 'Etunimi',
        firstNamePlaceholder: 'Kirjoita etunimi',
        lastName: 'Sukunimi',
        lastNamePlaceholder: 'Kirjoita sukunimi',
        pinCode: 'PIN-koodi',
        pinCodePlaceholder: 'koodi'
      },
      editEmployeeRowModal: {
        title: 'Muokkaa henkilön tietoja',
        groups: 'Valitse ryhmä'
      },
      choosePerson: 'Valitse henkilö',
      mobileDevices: {
        mobileDevices: 'Yksikön mobiililaitteet',
        addMobileDevice: 'Lisää mobiililaite',
        editName: 'Muokkaa laitteen nimeä',
        removeConfirmation: 'Haluatko poistaa mobiililaitteen?',
        editPlaceholder: 'esim. Hippiäisten kännykkä'
      },
      groups: 'Luvitukset ryhmiin',
      noGroups: 'Ei luvituksia',
      hasOccupancyCoefficient: 'Kasvatusvastuullinen',
      previousTemporaryEmployees: 'Aiemmat tilapäiset työntekijät'
    },
    filters: {
      title: 'Näytä tiedot',
      periods: {
        day: 'Päivä',
        threeMonths: '3 kk',
        sixMonths: '6 kk',
        year: 'Vuosi'
      }
    },
    occupancy: {
      display: 'Näytä',
      fullUnit: 'Koko yksikkö',
      title: 'Yksikön täyttöaste',
      subtitles: {
        confirmed: 'Vahvistettu täyttöaste',
        planned: 'Suunniteltu täyttöaste',
        realized: 'Käyttöaste'
      },
      fail: 'Täyttöasteen lataaminen epäonnistui',
      failRealized: 'Käyttöasteen lataaminen epäonnistui',
      maximum: 'Maksimi',
      minimum: 'Minimi',
      noValidValues: 'Aikavälille ei voitu laskea täyttöastetta',
      noValidValuesRealized: 'Aikavälille ei voitu laskea käyttöastetta',
      realtime: {
        modes: {
          REALIZED: 'Toteuma',
          PLANNED: 'Suunnitelma'
        },
        noData: 'Ei tietoja valitulle päivälle',
        legendTitle: 'Merkintöjen selitykset',
        chartYAxisTitle: 'Lapsia kertoimilla',
        chartY1AxisTitle: 'Henkilökuntaa',
        staffPresent: 'Työntekijöiden lukumäärä',
        staffRequired: 'Tarvittavat työntekijät',
        childrenMax: 'Lasten maksimimäärä (kertoimella)',
        childrenPresent: 'Lasten lukumäärä',
        children: 'Lasten määrä (kertoimella)',
        utilization: 'Käyttöaste'
      }
    },
    staffOccupancies: {
      title: 'Kasvatusvastuullisuus',
      occupancyCoefficientEnabled: 'Lasketaan käyttöasteesen'
    },
    applicationProcess: {
      title: 'Hakuprosessi'
    },
    placementPlans: {
      title: 'Vahvistettavana huoltajalla',
      name: 'Nimi',
      birthday: 'Syntymäaika',
      placementDuration: 'Sijoitettu yksikköön',
      type: 'Toimintamuoto',
      subtype: 'Osa/Koko',
      application: 'Hakemus'
    },
    placementProposals: {
      title: 'Sijoitusehdotukset',
      name: 'Nimi',
      birthday: 'Syntymäaika',
      placementDuration: 'Sijoitettu yksikköön',
      type: 'Toimintamuoto',
      subtype: 'Osa/Koko',
      application: 'Hakemus',
      confirmation: 'Hyväksyntä',
      acceptAllButton: 'Vahvista valinnat',
      rejectTitle: 'Valitse palautuksen syy',
      rejectReasons: {
        REASON_1:
          'TILARAJOITE, sovittu varhaiskasvatuksen aluepäällikön kanssa.',
        REASON_2:
          'YKSIKÖN KOKONAISTILANNE, sovittu varhaiskasvatuksen aluepäällikön kanssa.',
        REASON_3: '',
        OTHER: 'Muu syy'
      },
      infoTitle: 'Hyväksytyksi / hylätyksi merkitseminen',
      infoText:
        'Merkitse lapset, jotka pystyt ottamaan vastaan. Kun olet hyväksynyt kaikki lapset voit painaa Vahvista hyväksytyt -nappia. Mikäli et pysty hyväksymään kaikkia lapsia, merkitse rasti ja lisää perustelu. Palveluohjaus tekee tällöin uuden sijoitusehdotuksen tai ottaa yhteyttä.',
      describeOtherReason: 'Kirjoita perustelu',
      citizenHasRejectedPlacement: 'Paikka hylätty'
    },
    applications: {
      title: 'Hakemukset',
      child: 'Lapsen nimi/synt.aika',
      guardian: 'Hakenut huoltaja',
      type: 'Toimintamuoto',
      types: {
        CLUB: 'Kerho',
        DAYCARE: 'Varhaiskasvatus',
        DAYCARE_PART_TIME: 'Varhaiskasvatus',
        PRESCHOOL: 'Esiopetus',
        PRESCHOOL_DAYCARE: 'Esiopetus',
        PREPARATORY: 'Valmistava',
        PREPARATORY_DAYCARE: 'Valmistava'
      },
      placement: 'Osa/Koko',
      preferenceOrder: 'Toive',
      startDate: 'Aloitus',
      status: 'Tila',
      extendedCare: 'Vuorohoito'
    },
    serviceApplications: {
      title: 'Käsittelyä odottavat palveluntarpeen muutoshakemukset',
      child: 'Lapsi',
      range: 'Ajalle',
      newNeed: 'Uusi tarve',
      currentNeed: 'Nykyinen tarve',
      sentDate: 'Lähetetty'
    },
    placements: {
      title: 'Ryhmää odottavat lapset',
      name: 'Nimi',
      birthday: 'Syntymäaika',
      under3: 'Alle 3-vuotias sijoituksen alkaessa',
      over3: 'Yli 3-vuotias sijoituksen alkaessa',
      placementDuration: 'Sijoitettu yksikköön',
      missingGroup: 'Ryhmä puuttuu',
      type: 'Toimintamuoto',
      subtype: 'Osa/Koko',
      addToGroup: 'Ryhmitä',
      modal: {
        createTitle: 'Lapsen sijoitus ryhmään',
        transferTitle: 'Lapsen siirto toiseen ryhmään',
        child: 'Sijoitettava lapsi',
        group: 'Ryhmä',
        errors: {
          noGroup: 'Et ole valinnut ryhmää tai aktiivisia ryhmiä ei ole',
          groupNotStarted: 'Ryhmä ei ole vielä alkanut',
          groupEnded: 'Ryhmä on jo lakkautettu'
        }
      }
    },
    termination: {
      title: 'Irtisanotut paikat',
      terminationRequestedDate: 'Irtisanomispäivä',
      endDate: 'Päättymispäivämäärä',
      groupName: 'Ryhmä'
    },
    calendar: {
      title: 'Kalenteri',
      noGroup: 'Ei ryhmää',
      staff: 'Henkilökunta',
      allChildren: 'Kaikki lapset',
      modes: {
        week: 'Viikko',
        month: 'Kuukausi'
      },
      attendances: {
        title: 'Varaukset ja läsnäolot'
      },
      nextWeek: 'Seuraava viikko',
      previousWeek: 'Edellinen viikko',
      events: {
        title: 'Tapahtumat',
        createEvent: 'Luo muu tapahtuma',
        edit: {
          title: 'Tapahtuma',
          saveChanges: 'Tallenna muutokset',
          delete: 'Poista tapahtuma'
        },
        create: {
          title: 'Lisää uusi tapahtuma',
          text: 'Lisää tässä tapahtumat, jotka huoltajan on tärkeä muistaa: tapahtuma tulee näkyviin huoltajan eVaka-kalenteriin. Muista tapahtumista kannattaa tiedottaa huoltajaa viestitse.',
          add: 'Lisää tapahtuma',
          period: 'Ajankohta',
          attendees: 'Tapahtuman osallistujat',
          attendeesPlaceholder: 'Valitse...',
          eventTitle: 'Tapahtuman otsikko',
          eventTitlePlaceholder: 'Max. 30 merkkiä',
          description: 'Tapahtuman kuvaus',
          descriptionPlaceholder:
            'Lyhyet ohjeet huoltajalle, esim. kellonaika, mitä pakata mukaan',
          missingPlacementsWarning:
            'Osalla valituista lapsista ei ole sijoitusta nykyisessä yksikössä tai ei ole sijoitettuna valittuun ryhmään tapahtuman aikana. Näinä päivinä lasta ei listata osallistujana eikä huoltajalle näytetä tapahtumaa kalenterissa.'
        },
        discussionReservation: {
          calendar: {
            addTimeButton: 'Näytä lisää',
            eventTooltipTitle: 'Muita tapahtumia:',
            otherEventSingular: 'muu tapahtuma',
            otherEventPlural: 'muuta tapahtumaa'
          },
          discussionPageTitle: 'Keskusteluaikojen hallinta',
          discussionPageDescription:
            'Tällä sivulla voit luoda ja seurata kyselyjä, joilla kysytään huoltajille sopivia keskusteluaikoja.',
          surveyCreate: 'Uusi keskustelukysely',
          surveyBasicsTitle: 'Perustiedot',
          surveyPeriod: 'Kyselyn kesto',
          surveySubject: 'Keskustelun aihe',
          surveyInvitees: 'Keskustelujen osallistujat',
          surveySummary: 'Lisätietoja huoltajalle',
          surveySummaryCalendarLabel: 'Lisätietoja',
          surveySummaryInfo:
            'Tämä teksti näytetään huoltajalle kyselyn yhteydessä. Voit kertoa siinä lisätietoja keskusteluista, esimerkiksi saapumisohjeet tai keskusteluun varattavan ajan.',
          surveySubjectPlaceholder: 'Enintään 30 merkkiä',
          surveySummaryPlaceholder: 'Kirjoita lisätiedot',
          surveyDiscussionTimesTitle: 'Keskusteluajat',
          surveyInviteeTitle: 'Osallistujat',
          editSurveyButton: 'Muokkaa',
          createSurveyButton: 'Lähetä keskusteluajat',
          saveSurveyButton: 'Tallenna muutokset',
          deleteSurveyButton: 'Poista',
          cancelButton: 'Peruuta',
          cancelConfirmation: {
            title: 'Haluatko perua muutokset?',
            text: 'Tekemiäsi muutoksia ei tallenneta',
            cancelButton: 'Jatka muokkaamista',
            continueButton: 'Peru muutokset'
          },
          surveyModifiedAt: 'Muokattu',
          surveyStatus: {
            SENT: 'Lähetetty',
            ENDED: 'Päättynyt'
          },
          reservedTitle: 'Varanneet',
          reserveButton: 'Varaa',
          unreservedTitle: 'Varaamatta',
          calendarSurveySummary: (
            link: (text: string) => React.ReactNode
          ): React.ReactNode => (
            <>
              Tarkempia tietoja varten{' '}
              {link('siirry keskustelukyselyn tarkastelunäkymään')}
            </>
          ),
          reservationModal: {
            reservationStatus: 'Varaustilanne',
            reserved: 'Varattu',
            unreserved: 'Vapaa',
            selectPlaceholder: 'Valitse',
            inviteeLabel: 'Osallistuja',
            reserveError: 'Keskusteluajan varaaminen epäonnistui',
            deleteError: 'Keskusteluajan poistaminen epäonnistui',
            deleteConfirmation: {
              title: 'Poistettava aika on jo varattu',
              text: 'Haluatko poistaa ajan ja varauksen?',
              cancelButton: 'Peru poisto',
              continueButton: 'Poista'
            }
          },
          deleteConfirmation: {
            title: 'Haluatko varmasti poistaa lähetetyn kyselyn?',
            text: 'Kaikki vapaat ja varatut ajat poistetaan. Tätä toimintoa ei voi peruuttaa.',
            error: 'Keskustelukyselyn poistaminen epäonnistui'
          },
          eventTime: {
            addError: 'Keskusteluajan lisääminen epäonnistui',
            deleteError: 'Keskusteluajan poistaminen epäonnistui'
          }
        },
        reservedTimesLabel: 'varattua',
        freeTimesLabel: 'vapaata'
      }
    },
    groups: {
      title: 'Toimipisteen ryhmät',
      familyContacts: 'Näytä yhteystietokooste',
      attendanceReservations: 'Läsnäolovaraukset',
      create: 'Luo uusi ryhmä',
      createModal: {
        title: 'Uusi ryhmä',
        confirmButton: 'Tallenna',
        cancelButton: 'Peruuta',
        name: 'Ryhmän nimi',
        type: 'Tyyppi',
        initialCaretakers: 'Henkilökunnan määrä ryhmän alkaessa',
        errors: {
          nameRequired: 'Ryhmällä täytyy olla nimi',
          initialCaretakersPositive:
            'Henkilökunnan määrä ei voi olla negatiivinen'
        }
      },
      updateModal: {
        title: 'Muokkaa ryhmän tietoja',
        name: 'Nimi',
        startDate: 'Perustettu',
        endDate: 'Lakkautettu',
        info: 'Ryhmän aikaisempia tietoja ei säilytetä',
        jamixPlaceholder: 'Jamix customerNumber',
        jamixTitle: 'Ruokatilausten asiakasnumero'
      },
      startDate: 'Perustettu',
      endDate: 'Lakkautettu',
      caretakers: 'Henkilökuntaa',
      childrenLabel: 'Lapsia',
      childrenValue: {
        single: 'lapsi',
        plural: 'lasta'
      },
      childServiceNeedFactor: 'Lapsen kerroin',
      childAssistanceNeedFactor: 'Tuen tarve',
      factor: 'Kerroin',
      maxOccupancy: 'Suurin täyttöaste',
      maxRealizedOccupancy: 'Suurin käyttöaste',
      name: 'Nimi',
      birthday: 'Syntymäaika',
      placementDuration: 'Sijoitettu ryhmään',
      serviceNeed: 'Palv.tarve',
      serviceNeedChecked: 'Palveluntarve merkitty',
      serviceNeedMissing1: 'Palveluntarve puuttuu (',
      serviceNeedMissing2: 'päivää)',
      placementType: 'Toimintamuoto',
      placementSubtype: 'Osa/Koko',
      noChildren: 'Ryhmään ei ole sijoitettu lapsia.',
      returnBtn: 'Palauta',
      transferBtn: 'Siirrä',
      diaryButton: 'Avaa päiväkirja',
      deleteGroup: 'Poista ryhmä',
      update: 'Muokkaa tietoja',
      daycareDailyNote: {
        dailyNote: 'Päivän muistiinpanot',
        header: 'Tänään koettua ja opittua',
        groupNotesHeader: 'Ryhmän muistiinpanot',
        stickyNotesHeader: 'Huomioitavaa lähipäivinä',
        notesHint:
          'Leikkejä, onnistumisia, ilonaiheita ja opittuja asioita tänään (ei terveystietoja tai salassapidettäviä tietoja).',
        childStickyNoteHint:
          'Muistiinpano henkilökunnalle (ei terveystietoja tai salassapidettäviä tietoja).',
        otherThings: 'Muut asiat',
        feedingHeader: 'Lapsi söi tänään',
        sleepingHeader: 'Lapsi nukkui tänään',
        sleepingHoursHint: 'tunnit',
        sleepingMinutesHint: 'minuutit',
        sleepingHours: 't',
        sleepingMinutes: 'min',
        reminderHeader: 'Muistettavia asioita',
        otherThingsToRememberHeader: 'Muuta muistettavaa (esim aurinkovoide)',
        groupNoteModalLink: 'Ryhmän muistiinpano',
        groupNoteHint: 'Koko ryhmää koskeva muistiinpano',
        edit: 'Lisää päivän muistiinpano',
        level: {
          GOOD: 'Hyvin',
          MEDIUM: 'Kohtalaisesti',
          NONE: 'Ei yhtään'
        },
        reminderType: {
          DIAPERS: 'Lisää vaippoja',
          CLOTHES: 'Lisää vaatteita',
          LAUNDRY: 'Pyykit'
        }
      }
    },
    backupCares: {
      title: 'Varasijoituslapset',
      childName: 'Nimi',
      duration: 'Sijoitettu yksikköön',
      birthDate: 'Syntymäaika'
    },
    attendanceReservations: {
      ungrouped: 'Lapset ilman ryhmää',
      childName: 'Lapsen nimi',
      startTime: 'Saapuu',
      endTime: 'Lähtee',
      requiresBackupCare: 'Tee varasijoitus',
      openReservationModal: 'Tee toistuva varaus',
      childCount: 'Lapsia läsnä',
      reservationModal: {
        title: 'Tee varaus',
        selectedChildren: 'Lapset, joille varaus tehdään',
        dateRange: 'Varauksen voimassaolo',
        dateRangeLabel: 'Tee varaus päiville',
        missingDateRange: 'Valitse varattavat päivät',
        repetition: 'Tyyppi tai toistuvuus',
        times: 'Kellonaika',
        businessDays: 'Ma-Pe',
        repeats: 'Toistuu',
        repetitions: {
          DAILY: 'Päivittäin',
          WEEKLY: 'Viikoittain',
          IRREGULAR: 'Epäsäännöllinen'
        }
      },
      childDateModal: {
        reservations: {
          title: 'Läsnäolovaraus',
          add: 'Lisää varaus',
          noTimes: 'Läsnä, kellonaika ei vielä tiedossa'
        },
        attendances: {
          title: 'Läsnäolototeuma',
          add: 'Lisää uusi rivi'
        },
        absences: {
          title: 'Poissaolo',
          add: {
            BILLABLE: 'Merkitse varhaiskasvatuksen poissaolo',
            NONBILLABLE: 'Merkitse maksuttoman toiminnan poissaolo'
          },
          label: {
            BILLABLE: 'Poissa varhais-kasvatuksesta, syy:',
            NONBILLABLE: 'Poissa maksuttomasta toiminnasta, syy:'
          }
        },
        overlapWarning: 'Tarkista päällekkäisyys',
        absenceWarning: 'Tarkista poissaolo',
        extraNonbillableAbsence:
          'Läsnäoloaikojen mukaan lapsi oli läsnä maksuttomassa toiminnassa.',
        missingNonbillableAbsence:
          'Läsnäoloaikojen mukaan lapsi ei ollut läsnä maksuttomassa toiminnassa.',
        extraBillableAbsence:
          'Läsnäoloaikojen mukaan lapsi oli läsnä maksullisessa varhaiskasvatuksessa.',
        missingBillableAbsence:
          'Läsnäoloaikojen mukaan lapsi ei ollut läsnä maksullisessa varhaiskasvatuksessa.',
        errorCodes: {
          attendanceInFuture: 'Läsnäolo ei voi olla tulevaisuudessa'
        }
      },
      reservationNoTimes: 'Läsnä',
      missingHolidayReservation: 'Lomavaraus puuttuu',
      missingHolidayReservationShort: 'Lomavar. puuttuu',
      fixedSchedule: 'Läsnä',
      termBreak: 'Ei toimintaa',
      missingReservation: 'Ilmoitus puuttuu',
      serviceTimeIndicator: '(s)',
      legend: {
        reservation: 'Varaus',
        serviceTime: 'Sopimusaika',
        attendanceTime: 'Saapumis-/lähtöaika',
        hhmm: 'tt:mm'
      },
      affectsOccupancy: 'Lasketaan käyttöasteeseen',
      doesNotAffectOccupancy: 'Ei lasketa käyttöasteeseen',
      inOtherUnit: 'Muussa yksikössä',
      inOtherGroup: 'Muussa ryhmässä',
      createdByEmployee: '*Henkilökunnan tekemä merkintä'
    },
    staffAttendance: {
      startTime: 'tulo',
      endTime: 'lähtö',
      summary: 'Yhteenveto',
      plan: 'Suunnitelma',
      realized: 'Toteutuma',
      hours: 'Tunnit',
      dailyAttendances: 'Päivän kirjaukset',
      continuationAttendance: '* edellisenä päivänä alkanut kirjaus',
      addNewAttendance: 'Lisää uusi kirjaus',
      saveChanges: 'Tallenna muutokset',
      noGroup: 'Ei ryhmää',
      staffName: 'Työntekijän nimi',
      addPerson: 'Lisää henkilö',
      types: {
        PRESENT: 'Läsnä',
        OTHER_WORK: 'Työasia',
        TRAINING: 'Koulutus',
        OVERTIME: 'Ylityö',
        JUSTIFIED_CHANGE: 'Perusteltu muutos'
      },
      incalculableSum:
        'Tunteja ei voi laskea, koska päivän kirjauksista puuttuu viimeinen lähtöaika.',
      gapWarning: (gapRange: string) => `Kirjaus puuttuu välillä ${gapRange}`,
      openAttendanceWarning: (arrival: string) => `Avoin kirjaus ${arrival}`,
      openAttendanceInAnotherUnitWarning: 'Avoin kirjaus ',
      openAttendanceInAnotherUnitWarningCont:
        '. Kirjaus on päätettävä ennen uuden lisäystä.',
      personCount: 'Läsnäolleiden yhteismäärä',
      personCountAbbr: 'hlö',
      unlinkOvernight: 'Erota yön yli menevä läsnäolo',
      previousDay: 'Edellinen päivä',
      nextDay: 'Seuraava päivä',
      addPersonModal: {
        description:
          'Lisää väliaikaisesti läsnäoleva henkilö ja valitse lasketaanko hänet mukaan käyttöasteeseen.',
        arrival: 'Saapumisaika',
        name: 'Nimi',
        namePlaceholder: 'Sukunimi Etunimi',
        group: 'Ryhmä'
      },
      departedAutomatically: 'Automaattikatkaistu',
      hasStaffOccupancyEffect: 'Kasvatusvastuullinen'
    },
    error: {
      placement: {
        create: 'Sijoitus ryhmään epäonnistui',
        transfer: 'Sijoitus toiseen ryhmään epäonnistui'
      }
    }
  },
  groupCaretakers: {
    info: 'Luo aina uusi henkilökunnan tarve, kun henkilökunnan lukumäärä muuttuu. Ilmoitettu lukumäärä on voimassa valitulla ajanjaksolla ja vaikuttaa yksikön ja ryhmän täyttöasteisiin.',
    create: 'Luo uusi henkilökunnan tarve',
    edit: 'Muokkaa tietoja',
    editActiveWarning:
      'Olet muokkaamassa käynnissäolevan ajanjakson tietoja. Jos henkilökunnan määrän muutos osuu muulle aikavälille, luo uusi henkilökunnan tarve, jotta historiatieto säilyy.',
    editHistoryWarning:
      'Olet muokkaamassa päättyneen ajanjakson tietoja. Jos henkilökunnan määrän muutos osuu muulle aikavälille, luo uusi henkilökunnan tarve, jotta historiatieto säilyy.',
    confirmDelete: 'Haluatko varmasti poistaa henkilökunnan tarpeen?',
    startDate: 'Alkaen',
    endDate: 'Päättyen',
    amount: 'Henkilökunnan tarve',
    amountUnit: 'Henkilöä',
    status: 'Tila',
    conflict:
      'Valitussa ajanjaksossa on päällekkäisyys aiemmin luodun ajanjakson kanssa. Poista päällekkäisyys muokkaamalla toista ajanjaksoa.'
  },
  personalMobileDevices: {
    title: 'Henkilökohtainen eVaka-mobiili',
    infoParagraph1:
      'Tällä sivulla voit määrittää itsellesi omaan henkilökohtaiseen käyttöösi mobiililaitteen, jolla tarkastelet kaikkien yksiköidesi tietoja  eVakassa. Voit myös tarvittaessa poistaa tai lisätä useamman laitteen.',
    infoParagraph2:
      'Huolehdithan, että kaikissa mobiililaitteissasi on pääsykoodi käytössä.',
    name: 'Laitteen nimi',
    addDevice: 'Lisää mobiililaite',
    editName: 'Muokkaa laitteen nimeä',
    deleteDevice: 'Haluatko poistaa mobiililaitteen?'
  },
  mobilePairingModal: {
    sharedDeviceModalTitle: 'Lisää yksikköön uusi mobiililaite',
    personalDeviceModalTitle: 'Lisää uusi henkilökohtainen mobiililaite',
    modalText1: 'Mene mobiililaitteella osoitteeseen',
    modalText2: 'ja syötä laitteeseen alla oleva koodi.',
    modalText3:
      'Syötä mobiililaitteessa näkyvä vahvistuskoodi alla olevaan kenttään.',
    modalText4:
      'Anna mobiililaitteelle vielä nimi, jolla erotat sen muista mobiililaiteista.',
    namePlaceholder: 'Nimi'
  },
  invoices: {
    table: {
      title: 'Laskut',
      toggleAll: 'Valitse kaikki alueen laskut',
      head: 'Päämies',
      children: 'Lapset',
      period: 'Laskutuskausi',
      createdAt: 'Luonnos luotu',
      nb: 'Huom',
      totalPrice: 'Summa',
      status: 'Tila'
    },
    buttons: {
      checked: (count: number) =>
        count === 1 ? `${count} lasku valittu` : `${count} laskua valittu`,
      sendInvoice: (count: number) =>
        count === 1 ? 'Siirrä valittu lasku' : 'Siirrä valitut laskut',
      createInvoices: 'Luo laskuluonnokset',
      deleteInvoice: (count: number) =>
        count === 1 ? 'Poista valittu lasku' : 'Poista valitut laskut',
      checkAreaInvoices: (customRange: boolean) =>
        customRange
          ? 'Valitse laskut valitulta aikaväliltä ja alueilta'
          : 'Valitse tämän kuun laskut valituilta alueilta',
      individualSendAlertText:
        'Muista nostaa aiemmin siirretyt laskut laskutusjärjestelmään ennen uusien siirtämistä.'
    },
    sendModal: {
      title: 'Siirrä valitut laskut',
      invoiceDate: 'Laskun päivä',
      dueDate: 'Laskun eräpäivä'
    }
  },
  invoice: {
    status: {
      DRAFT: 'Luonnos',
      WAITING_FOR_SENDING: 'Siirretään manuaalisesti',
      SENT: 'Siirretty',
      CANCELED: 'Peruutettu'
    },
    title: {
      DRAFT: 'Laskuluonnos',
      WAITING_FOR_SENDING: 'Siirtoa odottava lasku',
      SENT: 'Siirretty lasku',
      CANCELED: 'Peruutettu lasku'
    },
    form: {
      nav: {
        return: 'Palaa'
      },
      child: {
        ssn: 'Lapsen hetu'
      },
      headOfFamily: {
        title: 'Päämies',
        fullName: 'Päämies',
        ssn: 'Päämiehen hetu',
        codebtorName: 'Kanssavelallinen',
        codebtorSsn: 'Kanssavelallisen hetu'
      },
      details: {
        title: 'Laskun tiedot',
        status: 'Tila',
        range: 'Laskutuskausi',
        number: 'Laskun numero',
        dueDate: 'Laskun eräpäivä',
        account: 'Tili',
        accountType: 'Tililaji',
        agreementType: 'Laskulaji',
        relatedFeeDecisions: 'Liittyvät maksupäätökset'
      },
      rows: {
        title: 'Laskurivit',
        product: 'Tuote',
        description: 'Selite',
        unitId: 'Yksikkö',
        daterange: 'Ajanjakso',
        amount: 'Kpl',
        unitPrice: 'A-hinta',
        price: 'Summa',
        subtotal: 'Laskun summa'
      },
      sum: {
        rowSubTotal: 'Lapsen rivien summa',
        familyTotal: 'Perhe yhteensä'
      },
      buttons: {
        markSent: 'Merkitse siirretyksi'
      }
    },
    distinctiveDetails: {
      MISSING_ADDRESS: 'Osoite puuttuu'
    },
    openAbsenceSummary: 'Avaa poissaolokooste'
  },
  invoiceCorrections: {
    noChildren: 'Henkilö ei ole yhdenkään lapsen päämies',
    targetMonth: 'Laskutuskausi',
    addRow: 'Lisää korjausrivi'
  },
  financeDecisions: {
    handlerSelectModal: {
      title: 'Tarkista tiedot',
      label: 'Päätöksentekijä',
      error: 'Päätöksentekijöiden lataus epäonnistui, yritä uudelleen',
      default: 'Yksikön tiedoissa asetettu päätöksentekijä',
      decisionCount: (count: number) =>
        count === 1 ? '1 päätös valittu' : `${count} päätöstä valittu`,
      resolve: (count: number) =>
        count === 1 ? 'Vahvista ja luo päätös' : 'Vahvista ja luo päätökset'
    }
  },
  feeDecisions: {
    table: {
      title: 'Maksupäätökset',
      head: 'Päämies',
      children: 'Lapset',
      validity: 'Maksupäätös voimassa',
      price: 'Summa',
      number: 'Numero',
      status: 'Tila',
      createdAt: 'Luotu',
      sentAt: 'Lähetetty',
      difference: {
        title: 'Muutos',
        value: {
          GUARDIANS: 'Huoltajat',
          CHILDREN: 'Lapset',
          INCOME: 'Tulot',
          PLACEMENT: 'Sijoitus',
          SERVICE_NEED: 'Palveluntarve',
          SIBLING_DISCOUNT: 'Sisaralennus',
          FEE_ALTERATIONS: 'Maksumuutos',
          FAMILY_SIZE: 'Perhekoko',
          FEE_THRESHOLDS: 'Maksuasetukset'
        },
        valueShort: {
          GUARDIANS: 'H',
          CHILDREN: 'L',
          INCOME: 'T',
          PLACEMENT: 'S',
          SERVICE_NEED: 'PT',
          SIBLING_DISCOUNT: 'SA',
          FEE_ALTERATIONS: 'M',
          FAMILY_SIZE: 'P',
          FEE_THRESHOLDS: 'MA'
        }
      },
      annullingDecision: 'Mitätöi tai päättää päätökset ajalta'
    },
    buttons: {
      checked: (count: number) =>
        count === 1 ? `${count} päätös valittu` : `${count} päätöstä valittu`,
      createDecision: (count: number) =>
        count === 1 ? 'Luo päätös' : 'Luo päätökset',
      ignoreDraft: 'Ohita luonnos',
      unignoreDrafts: (count: number) =>
        count === 1 ? 'Kumoa ohitus' : 'Kumoa ohitukset',
      markSent: 'Merkitse postitetuksi',
      close: 'Sulje tallentamatta',
      save: 'Tallenna muutokset',
      errors: {
        WAITING_FOR_MANUAL_SENDING:
          'Osalla päämiehistä on päätöksiä, jotka odottavat manuaalista lähetystä'
      }
    }
  },
  ignoreDraftModal: {
    title: 'Haluatko varmasti ohittaa luonnoksen?',
    content: (
      <div>
        <H3>Luonnoksen saa ohittaa vain jos seuraavat asiat pätevät:</H3>
        <ul>
          <li>Luonnos koskee menneisyyttä, ja</li>
          <li>
            Luonnos on väärin, koska menneisyydessä olevat asiakastiedot ovat
            väärin, ja
          </li>
          <li>Samalle ajalle oleva alkuperäinen lähetetty päätös on oikein</li>
        </ul>
        <p>
          Mikäli luonnos on väärin koska tiedot ovat väärin (esim. perhesuhteita
          on takautuvasti poistettu virheellisesti), on tärkeää ensisijaisesti
          pyrkiä korjaamaan tiedot ennalleen, koska ne vaikuttavat myös muihin
          järjestelmiin.
        </p>
        <p>
          Mikäli luonnos on väärin tai tarpeeton, vaikka tiedot ovat oikein, älä
          ohita luonnosta, vaan ole yhteydessä kehittäjätiimiin, jotta vika
          voidaan tutkia ja korjata.
        </p>
      </div>
    ),
    confirm: 'Ymmärrän ja vahvistan tämän'
  },
  valueDecisions: {
    table: {
      title: 'Arvopäätökset',
      head: 'Päämies',
      child: 'Lapsi',
      validity: 'Arvopäätös voimassa',
      totalValue: 'PS-Arvo',
      totalCoPayment: 'Omavastuu',
      number: 'Numero',
      status: 'Tila',
      createdAt: 'Luotu',
      sentAt: 'Lähetetty',
      difference: {
        title: 'Muutos',
        value: {
          GUARDIANS: 'Huoltajat',
          INCOME: 'Tulot',
          FAMILY_SIZE: 'Perhekoko',
          PLACEMENT: 'Sijoitus',
          SERVICE_NEED: 'Palveluntarve',
          SIBLING_DISCOUNT: 'Sisaralennus',
          CO_PAYMENT: 'Omavastuuosuus ennen maksumuutoksia',
          FEE_ALTERATIONS: 'Maksumuutokset',
          FINAL_CO_PAYMENT: 'Omavastuuosuus',
          BASE_VALUE: 'Perusarvo',
          VOUCHER_VALUE: 'Palvelusetelin arvo',
          FEE_THRESHOLDS: 'Maksuasetukset'
        },
        valueShort: {
          GUARDIANS: 'H',
          INCOME: 'T',
          FAMILY_SIZE: 'P',
          PLACEMENT: 'S',
          SERVICE_NEED: 'PT',
          SIBLING_DISCOUNT: 'SA',
          CO_PAYMENT: 'OM',
          FEE_ALTERATIONS: 'M',
          FINAL_CO_PAYMENT: 'O',
          BASE_VALUE: 'PA',
          VOUCHER_VALUE: 'PS',
          FEE_THRESHOLDS: 'MA'
        }
      },
      annullingDecision: 'Mitätöi tai päättää päätökset ajalta'
    },
    buttons: {
      checked: (count: number) =>
        count === 1 ? `${count} päätös valittu` : `${count} päätöstä valittu`,
      createDecision: (count: number) =>
        count === 1 ? 'Luo päätös' : 'Luo päätökset',
      ignoreDraft: 'Ohita luonnos',
      unignoreDrafts: (count: number) =>
        count === 1 ? 'Kumoa ohitus' : 'Kumoa ohitukset',
      markSent: 'Merkitse postitetuksi',
      close: 'Sulje tallentamatta',
      save: 'Tallenna muutokset',
      errors: {
        WAITING_FOR_MANUAL_SENDING:
          'Osalla lapsista on päätöksiä, jotka odottavat manuaalista lähetystä'
      }
    }
  },
  payments: {
    table: {
      title: 'Maksut',
      toggleAll: 'Valitse kaikki hakua vastaavat rivit',
      unit: 'Yksikkö',
      period: 'Maksatuskausi',
      createdAt: 'Luonnos luotu',
      number: 'Laskunro',
      amount: 'Summa',
      status: 'Tila',
      nb: 'Huom',
      missingPaymentDetails: 'Tietoja puuttuu'
    },
    buttons: {
      createPaymentDrafts: 'Luo maksatusaineisto',
      checked: (count: number) =>
        count === 1 ? `${count} rivi valittu` : `${count} riviä valittu`,
      confirmPayments: (count: number) =>
        count === 1
          ? `Merkitse ${count} maksu tarkastetuksi`
          : `Merkitse ${count} maksua tarkastetuksi`,
      revertPayments: (count: number) =>
        count === 1
          ? `Palauta ${count} maksu luonnokseksi`
          : `Palauta ${count} maksua luonnoksiksi`,
      sendPayments: (count: number) =>
        count === 1 ? `Siirrä ${count} maksu` : `Siirrä ${count} maksua`,
      deletePayment: (count: number) =>
        count === 1 ? `Poista ${count} maksu` : `Poista ${count} maksua`
    },
    status: {
      DRAFT: 'Luonnos',
      CONFIRMED: 'Tarkastettu',
      SENT: 'Siirretty'
    },
    sendModal: {
      title: 'Siirrä valitut maksut',
      paymentDate: 'Maksupäivä',
      dueDate: 'Eräpäivä'
    },
    distinctiveDetails: {
      MISSING_PAYMENT_DETAILS: 'Maksutietoja puuttuu'
    }
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
  feeAlteration: {
    DISCOUNT: 'Alennus',
    INCREASE: 'Korotus',
    RELIEF: 'Huojennus'
  },
  feeDecision: {
    title: {
      DRAFT: 'Maksupäätösluonnos',
      IGNORED: 'Ohitettu maksupäätösluonnos',
      WAITING_FOR_SENDING: 'Maksupäätös (lähdössä)',
      WAITING_FOR_MANUAL_SENDING: 'Maksupäätös (lähetetään manuaalisesti)',
      SENT: 'Maksupäätös',
      ANNULLED: 'Mitätöity maksupäätös'
    },
    distinctiveDetails: {
      UNCONFIRMED_HOURS: 'Puuttuva palveluntarve',
      EXTERNAL_CHILD: 'Ulkopaikkakuntalainen',
      RETROACTIVE: 'Takautuva päätös',
      NO_STARTING_PLACEMENTS: 'Piilota uudet aloittavat lapset',
      MAX_FEE_ACCEPTED: 'Suostumus korkeimpaan maksuun',
      PRESCHOOL_CLUB: 'Vain esiopetuksen kerho'
    },
    status: {
      DRAFT: 'Luonnos',
      IGNORED: 'Ohitettu luonnos',
      WAITING_FOR_SENDING: 'Lähdössä',
      WAITING_FOR_MANUAL_SENDING: 'Lähetetään manuaalisesti',
      SENT: 'Lähetetty',
      ANNULLED: 'Mitätöity'
    },
    type: {
      NORMAL: 'Tavallinen maksupäätös, ei huojennusta',
      RELIEF_ACCEPTED: 'Huojennus hyväksytty (Lähetetään manuaalisesti)',
      RELIEF_PARTLY_ACCEPTED:
        'Osittainen' + ' huojennus hyväksytty (Lähetetään manuaalisesti)',
      RELIEF_REJECTED: 'Huojennus hylätty (Lähetetään manuaalisesti)'
    },
    headOfFamily: 'Päämies',
    partner: 'Toinen huoltaja / maksuvelvollinen',
    decisionNumber: 'Päätöksen numero',
    validPeriod: 'Maksupäätös voimassa',
    sentAt: 'Maksupäätös lähetetty',
    decisionHandler: 'Päätöksen käsittelijä',
    relief: 'Maksupäätöksen huojennus',
    waitingManualSending: 'Lähetetään manuaalisesti',
    pdfLabel: 'Maksupäätös PDF',
    downloadPdf: 'Lataa PDF',
    pdfInProgress:
      '(PDF:ää muodostetaan. Lataa sivu hetken kuluttua' +
      ' uudelleen niin voit ladata sen oheisesta linkistä.)',
    form: {
      nav: {
        return: 'Palaa'
      },
      income: {
        title: 'Perheen tulotiedot',
        maxFeeAccepted: 'Huoltajan suostumus korkeimpaan maksuluokkaan.'
      },
      child: {
        ssn: 'Henkilötunnus',
        placementType: 'Toimintamuoto',
        careArea: 'Palvelualue',
        daycare: 'Toimipaikka',
        placementDate: 'Sijoitus voimassa',
        serviceNeed: 'Palveluntarve',
        name: 'Nimi',
        city: 'Kotikunta'
      },
      summary: {
        title: 'Kooste maksupäätöksen perusteista',
        income: {
          title: 'Kooste perheen tuloista',
          effect: {
            label: 'Maksun peruste',
            MAX_FEE_ACCEPTED:
              'Huoltajan suostumus korkeimpaan varhaiskasvatusmaksuun',
            INCOMPLETE: 'Perheen tulotiedot ovat puutteelliset.',
            INCOME: 'Maksun perusteena huoltajien tulotiedot',
            NOT_AVAILABLE:
              'Maksun perusteena korkein tuloluokka (automaattinen)'
          },
          details: {
            MAX_FEE_ACCEPTED: 'Suostumus korkeimpaan varhaiskasvatusmaksuun',
            INCOMPLETE: 'Puutteelliset tulotiedot',
            NOT_AVAILABLE: 'Tulotietoja ei ole toimitettu'
          },
          income: 'Tulot',
          expenses: 'Menot',
          total: 'Perheen tulot yhteensä',
          familyComposition: 'Perheen kokoonpano ja maksun perusteet',
          familySize: 'Perhekoko',
          persons: ' henkilöä',
          feePercent: 'Maksuprosentti',
          minThreshold: 'Vähimmäisbruttoraja'
        },
        parts: {
          title: 'Kooste perheen lasten maksuista',
          siblingDiscount: 'sisaralennus',
          sum: 'Summa'
        },
        totalPrice: 'Perheen varhaiskasvatusmaksu yhteensä'
      },
      buttons: {
        saveChanges: 'Tallenna muutokset'
      }
    },
    modal: {
      title: 'Haluatko palata tallentamatta muutoksia?',
      cancel: 'Palaa tallentamatta',
      confirm: 'Jatka muokkausta'
    }
  },
  filters: {
    searchTerms: 'Hakuehdot',
    freeTextPlaceholder:
      'Haku nimellä, hetulla, osoitteella tai maksupäätöksen numerolla',
    area: 'Alue',
    unit: 'Toimipaikka',
    financeDecisionHandler: 'Talouspäätösten käsittelijä',
    unitPlaceholder: 'Valitse toimipaikka',
    financeDecisionHandlerPlaceholder: 'Valitse työntekijä',
    distinctiveDetails: 'Muuta huomioitavaa',
    difference: 'Muutos',
    providerType: 'Järjestämismuoto',
    status: 'Tila',
    clear: 'Tyhjennä valinnat',
    validityPeriod: 'Voimassaoloaika',
    searchByStartDate: 'Alkupäivä sijoittuu valitulle aikavälille',
    invoiceDate: 'Laskun päiväys',
    invoiceSearchByStartDate: 'Lähetä laskut valitulta kaudelta',
    paymentDate: 'Maksupäivä',
    paymentFreeTextPlaceholder: 'Haku maksun numerolla',
    incomeStatementSent: 'Tuloselvitys lähetetty',
    incomeStatementPlacementValidDate: 'Sijoitus voimassa'
  },
  valueDecision: {
    title: {
      DRAFT: 'Arvopäätösluonnos',
      IGNORED: 'Ohitettu arvopäätösluonnos',
      WAITING_FOR_SENDING: 'Arvopäätös (lähdössä)',
      WAITING_FOR_MANUAL_SENDING: 'Arvopäätös (lähetetään manuaalisesti)',
      SENT: 'Arvopäätös',
      ANNULLED: 'Mitätöity arvopäätös'
    },
    headOfFamily: 'Päämies',
    partner: 'Toinen huoltaja / maksuvelvollinen',
    decisionNUmber: 'Päätöksen numero',
    validPeriod: 'Arvopäätös voimassa',
    sentAt: 'Arvopäätös lähetetty',
    pdfLabel: 'Arvopäätös PDF',
    decisionHandlerName: 'Päätöksen käsittelijä',
    relief: 'Arvopäätöksen huojennus',
    downloadPdf: 'Lataa PDF',
    pdfInProgress:
      '(PDF:ää muodostetaan. Lataa sivu hetken kuluttua uudelleen niin voit ladata sen oheisesta linkistä.)',
    status: {
      DRAFT: 'Luonnos',
      IGNORED: 'Ohitettu luonnos',
      WAITING_FOR_SENDING: 'Lähdössä',
      WAITING_FOR_MANUAL_SENDING: 'Lähetetään manuaalisesti',
      SENT: 'Lähetetty',
      ANNULLED: 'Mitätöity'
    },
    type: {
      NORMAL: 'Tavallinen arvopäätös, ei huojennusta',
      RELIEF_ACCEPTED: 'Huojennus hyväksytty (Lähetetään manuaalisesti)',
      RELIEF_PARTLY_ACCEPTED:
        'Osittainen' + ' huojennus hyväksytty (Lähetetään manuaalisesti)',
      RELIEF_REJECTED: 'Huojennus hylätty (Lähetetään manuaalisesti)'
    },
    child: {
      name: 'Nimi',
      ssn: 'Henkilötunnus',
      city: 'Kotikunta',
      placementType: 'Toimintamuoto',
      careArea: 'Palvelualue',
      unit: 'Toimipaikka',
      serviceNeed: 'Palveluntarve'
    },
    summary: {
      title: 'Kooste arvopäätöksen perusteista',
      coPayment: 'Omavastuuosuus',
      sum: 'Summa',
      siblingDiscount: 'Sisarusalennus',
      totalValue: 'Palvelusetelin arvo omavastuun jälkeen',
      income: {
        title: 'Kooste perheen tuloista',
        effect: {
          label: 'Maksun peruste',
          MAX_FEE_ACCEPTED:
            'Huoltajan suostumus korkeimpaan varhaiskasvatusmaksuun',
          INCOMPLETE: 'Perheen tulotiedot ovat puutteelliset.',
          INCOME: 'Maksun perusteena huoltajien tulotiedot',
          NOT_AVAILABLE: 'Maksun perusteena korkein tuloluokka (automaattinen)'
        },
        details: {
          MAX_FEE_ACCEPTED: 'Suostumus korkeimpaan varhaiskasvatusmaksuun',
          INCOMPLETE: 'Puutteelliset tulotiedot',
          NOT_AVAILABLE: 'Tulotietoja ei ole toimitettu'
        },
        income: 'Tulot',
        expenses: 'Menot',
        total: 'Perheen tulot yhteensä',
        familyComposition: 'Perheen kokoonpano ja maksun perusteet',
        familySize: 'Perhekoko',
        persons: ' henkilöä',
        feePercent: 'Maksuprosentti',
        minThreshold: 'Vähimmäisbruttoraja'
      },
      value: 'Palvelusetelin arvo',
      age: {
        LESS_THAN_3: 'Alle 3-vuotias',
        OVER_3: 'Vähintään 3-vuotias'
      },
      assistanceNeedCoefficient: 'tuen tarpeen kerroin',
      hoursPerWeek: 'tuntia viikossa'
    }
  },
  // these are directly used by date picker so order and naming matters!
  datePicker: {
    months: [
      'tammikuu',
      'helmikuu',
      'maaliskuu',
      'huhtikuu',
      'toukokuu',
      'kesäkuu',
      'heinäkuu',
      'elokuu',
      'syyskuu',
      'lokakuu',
      'marraskuu',
      'joulukuu'
    ],
    weekdaysLong: [
      'maanantai',
      'tiistai',
      'keskiviikko',
      'torstai',
      'perjantai',
      'lauantai',
      'sunnuntai'
    ],
    weekdaysShort: ['ma', 'ti', 'ke', 'to', 'pe', 'la', 'su']
  },
  absences: {
    title: 'Poissaolot',
    absenceTypes: {
      OTHER_ABSENCE: 'Poissaolo',
      SICKLEAVE: 'Sairaus',
      UNKNOWN_ABSENCE: 'Ilmoittamaton poissaolo',
      PLANNED_ABSENCE: 'Vuorotyöpoissaolo',
      TEMPORARY_RELOCATION: 'Lapsi varasijoitettuna muualla',
      PARENTLEAVE: 'Vanhempainvapaa',
      FORCE_MAJEURE: 'Maksuton päivä',
      FREE_ABSENCE: 'Maksuton poissaolo',
      UNAUTHORIZED_ABSENCE: 'Ilmoittamaton päivystyksen poissaolo',
      NO_ABSENCE: 'Ei poissaoloa'
    },
    missingHolidayReservation: 'Huoltaja ei ole vahvistanut loma-ajan varausta',
    shiftCare: 'Ilta-/vuorohoito',
    requiresBackupCare: 'Odottaa varasijoitusta',
    additionalLegendItems: {
      CONTRACT_DAYS: 'Sopimuspäivällinen palveluntarve'
    },
    absenceTypesShort: {
      OTHER_ABSENCE: 'Poissaolo',
      SICKLEAVE: 'Sairaus',
      UNKNOWN_ABSENCE: 'Ilmoittamaton',
      PLANNED_ABSENCE: 'Vuorotyö',
      TEMPORARY_RELOCATION: 'Varasijoitus',
      PARENTLEAVE: 'Vanh.vap.',
      FORCE_MAJEURE: 'Maksuton',
      FREE_ABSENCE: 'Maksuton',
      UNAUTHORIZED_ABSENCE: 'Sakko',
      NO_ABSENCE: 'Ei poissa'
    },
    absenceTypeInfo: {
      OTHER_ABSENCE:
        'Käytetään aina, kun huoltaja on ilmoittanut poissaolosta mukaan lukien säännölliset vapaat ja loma-aika. Käytetään myös vuoroyksiköissä lasten lomamerkinnöissä tai muissa poissaoloissa, jotka ovat suunniteltujen läsnäolovarausten ulkopuolella.',
      SICKLEAVE:
        '11 päivää ylittävä yhtäjaksoinen sairauspoissaolo vaikuttaa alentavasti maksuun.',
      UNKNOWN_ABSENCE:
        'Käytetään silloin, kun huoltaja ei ole ilmoittanut poissaolosta, vaikuttaa heinäkuussa myös laskutukseen. Koodi muutetaan vain, jos kyseessä on sairauspoissaolo, jonka jatkumisesta huoltaja ilmoittaa seuraavana päivänä.',
      PLANNED_ABSENCE:
        'Käytetään vain vuoroyksiköissä, kun kyse on vuorotyöstä johtuvasta vapaasta, loma-ajat merkitään Poissa- koodilla. Ei oikeuta maksualennukseen laskulla.',
      TEMPORARY_RELOCATION:
        'Lapselle on tehty varasijoitus toiseen yksikköön. Poissaolon voi merkitä, mikäli sellainen on tiedossa. Tutustu kuitenkin loma-ajan ohjeeseen, mikäli poissaolo koskee loma-aikaa.',
      PARENTLEAVE:
        'Vanhempainvapaa, merkitään vain sille lapselle, jonka vuoksi huoltaja on vapaalla, ei sisaruksille. Vaikuttaa maksuun siten, että ko. aika on maksuton.',
      FORCE_MAJEURE:
        'Käytetään vain erikoistilanteissa hallinnon ohjeiden mukaan.',
      FREE_ABSENCE: 'Kesäajan maksuton poissaolo',
      UNAUTHORIZED_ABSENCE: 'Ilmoittamaton päivystyksen poissaolo',
      NO_ABSENCE: 'Jos lapsi on paikalla, älä merkitse mitään.'
    },
    additionalLegendItemInfos: {
      CONTRACT_DAYS: 'Lapsi, jolla palveluntarpeena sopimuspäivä'
    },
    careTypes: {
      SCHOOL_SHIFT_CARE: 'Koululaisten vuorohoito',
      PRESCHOOL: 'Esiopetus',
      PRESCHOOL_DAYCARE: 'Liittyvä varhaiskasvatus',
      DAYCARE_5YO_FREE: '5-vuotiaiden varhaiskasvatus',
      DAYCARE: 'Varhaiskasvatus',
      CLUB: 'Kerho'
    },
    absenceCategories: {
      NONBILLABLE:
        'Esiopetus, valmistava, 5-vuotiaiden varhaiskasvatus tai kerhotoiminta',
      BILLABLE: 'Varhaiskasvatus (maksullinen)'
    },
    modifiedByStaff: 'Henkilökunta',
    modifiedByCitizen: 'Huoltaja',
    modal: {
      absenceSectionLabel: 'Poissaolon syy',
      placementSectionLabel: 'Toimintamuoto, jota poissaolo koskee',
      saveButton: 'Tallenna',
      cancelButton: 'Peruuta',
      absenceTypes: {
        OTHER_ABSENCE: 'Poissaolo',
        SICKLEAVE: 'Sairaus',
        UNKNOWN_ABSENCE: 'Ilmoittamaton poissaolo',
        PLANNED_ABSENCE: 'Vuorotyöpoissaolo',
        TEMPORARY_RELOCATION: 'Varasijoitettuna muualla',
        PARENTLEAVE: 'Vanhempainvapaa',
        FORCE_MAJEURE: 'Maksuton päivä (rajoitettu käyttö)',
        FREE_ABSENCE: 'Maksuton poissaolo',
        UNAUTHORIZED_ABSENCE: 'Ilmoittamaton päivystyksen poissaolo',
        NO_ABSENCE: 'Ei poissaoloa',
        MISSING_HOLIDAY_RESERVATION: 'Loma-ajan ilmoitus puuttuu'
      },
      free: 'Maksuton',
      paid: 'Maksullinen',
      absenceSummaryTitle: 'Lapsen poissaolokooste'
    },
    table: {
      selectAll: 'Valitse kaikki',
      staffRow: 'Henkilökuntaa paikalla',
      disabledStaffCellTooltip: 'Ryhmä ei ole olemassa valittuna päivänä',
      reservationsTotal: 'Varaus/kk',
      attendancesTotal: 'Toteuma/kk'
    },
    legendTitle: 'Merkintöjen selitykset',
    addAbsencesButton(numOfSelected: number) {
      return numOfSelected === 1
        ? 'Lisää merkintä valitulle...'
        : 'Lisää merkinnät valituille...'
    },
    notOperationDay: 'Ei toimintapäivä',
    absence: 'Poissaolo',
    reservation: 'Varaus',
    present: 'Läsnä',
    guardian: 'Huoltaja',
    staff: 'Henkilökunta',
    dailyServiceTime: 'Sopimusaika'
  },
  placementDraft: {
    preschoolDaycare: 'Liittyvä varhaiskasvatus',
    card: {
      title: 'Korkein täyttöaste alkaen sijoituspäivästä',
      titleSpeculated: 'Täyttöaste mikäli lapsi sijoitetaan'
    },
    upcoming: 'Tulossa',
    active: 'Aktiivinen',
    currentPlacements: 'Olemassa olevat sijoitukset',
    noCurrentPlacements: 'Ei olemassaolevia sijoituksia',
    addOtherUnit: 'Lisää muu yksikkö',
    placementOverlapError:
      'Aiemmat päällekkäiset sijoitukset katkaistaan automaattisesti mikäli kuntalainen ottaa tarjottavan paikan vastaan.',
    createPlacementDraft: 'Luo sijoitushahmotelma',
    datesTitle: 'Nyt luotava sijoitushahmotelma',
    type: 'Toimintamuoto',
    date: 'Sijoituspäivämäärä',
    dateError: 'Päällekkäinen sijoitus ajanjaksolle.',
    preparatoryPeriod: 'Valmistava opetus',
    dateOfBirth: 'Syntymäaika',
    selectUnit: 'Valitse yksikkö',
    selectedUnit: 'Valittu yksikkö',
    restrictedDetails: 'Huoltajalla on turvakielto',
    restrictedDetailsTooltip:
      'Päätös pitää lähettää käsin toiselle huoltajalle, kun hakijalla on turvakielto.'
  },
  decisionDraft: {
    title: 'Päätöksen teko ja lähetys',
    info1:
      'Lähettämällä päätöksen hyväksyt sijoitushahmotelman. Kuntalaiselle lähetetään ne päätökset, jotka olet alla valinnut.',
    info2:
      'Huomaathan, että valinnat ja päivämäärät vaikuttavat ainoastaan päätösdokumentteihin. Jos haluat muokata varsinaista sijoitusta, palauta hakemus takaisin sijoitettaviin ja sijoita se uudelleen.',
    ssnInfo1:
      'Huoltajuutta ei voida tarkistaa ilman huoltajan ja lapsen henkilöturvatunnusta.',
    ssnInfo2: 'Lähetä tulostettu päätös postitse ja merkitse se postitetuksi.',
    unitInfo1: 'Yksikön tiedot ovat puutteelliset.',
    unitInfo2:
      'Puutteelliset tiedot on päivitettävä ennen päätösten luontia. Ota yhteyttä kehittäjiin.',
    notGuardianInfo1: 'Hakemuksen huoltaja ei ole lapsen huoltaja.',
    notGuardianInfo2:
      'Henkilö joka on merkitty hakemuksella huoltajaksi ei ole VTJn mukaan lapsen huoltaja. Päätös pitää lähettää paperisena.',
    unit: 'Toimipaikka',
    contact: 'Kontaktihenkilö',
    decisionLabelHeading: 'Toimintamuoto',
    decisionValueHeading: 'Päätöspäivämäärä',
    types: {
      CLUB: 'Kerho',
      DAYCARE: 'Varhaiskasvatus',
      DAYCARE_PART_TIME: 'Osapäiväinen varhaiskasvatus',
      PRESCHOOL_DAYCARE: 'Esiopetukseen liittyvä varhaiskasvatus',
      PRESCHOOL_CLUB: 'Esiopetuksen kerho',
      PRESCHOOL: 'Esiopetus',
      PREPARATORY: 'Valmistava opetus',
      PREPARATORY_EDUCATION: 'Valmistava opetus',
      PREPARATORY_DAYCARE: 'Valmistavaan opetukseen liittyvä varhaiskasvatus'
    },
    placementUnit: 'Sijoittaessa valittu yksikkö',
    selectedUnit: 'Päätökselle valittava yksikkö',
    unitDetailsHeading: 'Päätöksellä näytettävät tiedot',
    preschoolDecisionName: 'Yksikön nimi esiopetuspäätöksellä',
    daycareDecisionName: 'Yksikön nimi varhaiskasvatuspäätöksellä',
    unitManager: 'Yksikön johtaja',
    unitAddress: 'Yksikön osoite',
    handlerName: 'Käsittelijän nimi',
    handlerAddress: 'Käsittelijän osoite',
    receiver: 'Vastaanottaja',
    otherReceiver: 'Vastaanottaja (toinen huoltaja)',
    missingValue: 'Tieto puuttuu.',
    noOtherGuardian: 'Toista huoltajaa ei ole',
    differentUnit:
      'Päätöksellä näkyvä yksikkö on eri kuin alkuperäisessä sijoituksessa.'
  },
  reports: {
    title: 'Raportit',
    downloadButton: 'Lataa raportti',
    common: {
      orderBy: 'Järjestys',
      total: 'Yhteensä',
      totalShort: 'Yht',
      careAreaName: 'Palvelualue',
      unitName: 'Yksikkö',
      groupName: 'Ryhmä',
      unitType: 'Toimintamuoto',
      unitTypes: {
        DAYCARE: 'Päiväkoti',
        FAMILY: 'Perhepäiväkoti',
        GROUP_FAMILY: 'Ryhmäperhepäiväkoti',
        CLUB: 'Kerho'
      },
      unitProviderType: 'Järjestämismuoto',
      unitProviderTypes: {
        MUNICIPAL: 'Kunnallinen',
        PURCHASED: 'Ostopalvelu',
        PRIVATE: 'Yksityinen',
        MUNICIPAL_SCHOOL: 'Suko',
        PRIVATE_SERVICE_VOUCHER: 'Palveluseteli',
        EXTERNAL_PURCHASED: 'Ostopalvelu (muu)'
      },
      filters: {
        showIntentionalDuplicates: 'Näytä myös tarkoituksellisesti monistetut'
      },
      period: 'Ajanjakso',
      date: 'Päivämäärä',
      clock: 'Klo',
      startDate: 'Alkaen',
      endDate: 'Päättyen',
      firstName: 'Etunimi',
      lastName: 'Sukunimi',
      childName: 'Lapsen nimi',
      child: 'Lapsi',
      under3y: '<3v',
      over3y: '3+',
      age: 'Ikä'
    },
    applications: {
      title: 'Saapuneet hakemukset',
      description:
        'Raportti listaa saapuneita ja käsittelyssä olevia hakemuksia yksiköittäin.',
      ageInfo: 'Lapsen ikä lasketaan valitun aikavälin loppupäivänä',
      preferredStartingDate: 'Aloituspäivä',
      under3Years: 'Vakahakemuksia (alle 3v)',
      over3Years: 'Vakahakemuksia (yli 3v)',
      preschool: 'Esiopetushakemuksia',
      club: 'Kerhohakemuksia',
      totalChildren: 'Lapsia hakenut yhteensä'
    },
    decisions: {
      title: 'Päätökset',
      description: 'Raportti listaa tehtyjä päätöksiä yksiköittäin.',
      ageInfo: 'Lapsen ikä lasketaan päätöksen lähetyspäivänä',
      sentDate: 'Päätöksen lähetyspäivä',
      daycareUnder3: 'Vakapäätöksiä (alle 3v)',
      daycareOver3: 'Vakapäätöksiä (yli 3v)',
      preschool: 'Esiopetuspäätöksiä',
      preschoolDaycare: 'Esiopetus+liittyväpäätöksiä',
      connectedDaycareOnly: 'Myöhemmin haetun liittyvän päätöksiä',
      preparatory: 'Valmistavan päätöksiä',
      preparatoryDaycare: 'Valmistavan+liittyvän päätöksiä',
      club: 'Kerhopäätöksiä',
      preference1: '1. toive',
      preference2: '2. toive',
      preference3: '3. toive',
      preferenceNone: 'Ei toiveena',
      total: 'Päätöksiä yhteensä'
    },
    raw: {
      title: 'Raakaraportti',
      description:
        'Vähemmän pitkälle jalostettu laaja tietoaineisto, josta voi itse muodostaa erilaisia raportteja.'
    },
    assistanceNeedDecisions: {
      title: 'Tuen päätökset',
      description: 'Päätöksen tekijälle lähetetyt tuen päätökset.',
      decisionNumber: 'Päätösnumero',
      childhoodEducationPrefix: 'VK ',
      preschoolPrefix: 'EO ',
      sentToDecisionMaker: 'Lähetetty päätöksen tekijälle',
      decisionMade: 'Päätös tehty',
      status: 'Tila',
      returnForEditModal: {
        title: 'Palautetaanko päätös korjattavaksi?',
        okBtn: 'Palauta korjattavaksi',
        text: 'Päätösesitystä ei lähetetä kuntalaiselle.'
      },
      rejectModal: {
        title: 'Hylätäänkö päätös?',
        okBtn: 'Hylkää päätös',
        text: 'Haluatko varmasti tehdä hylätyn päätöksen? Hylätty päätös lähetetään kuntalaisen nähtäväksi eVakaan.'
      },
      approveModal: {
        title: 'Hyväksytäänkö päätös?',
        okBtn: 'Hyväksy päätös',
        text: 'Haluatko varmasti hyväksyä päätöksen? Hyväksytty päätös lähetetään kuntalaisen nähtäväksi eVakaan.'
      },
      approveFailedModal: {
        title: 'Päätöksen hyväksyminen epäonnistui',
        okBtn: 'Sulje'
      },
      annulModal: {
        title: 'Mitätöidäänkö päätös?',
        okBtn: 'Mitätöi päätös',
        text: 'Päätöstä ei saa mitätöidä keskustelematta ensin huoltajan kanssa. Uudella päätöksellä ei hallintolain mukaan saa heikentää huoltajan asemaa. Haluatko varmasti mitätöidä päätöksen? Päätöksen voimassaolo lakkaa välittömästi. Tieto mitätöinnistä ja sen perustelu lähetetään kuntalaisen nähtäväksi eVakaan.',
        inputPlaceholder: 'Kuvaile miksi päätös on mitätöity'
      },
      mismatchDecisionMakerWarning: {
        text: 'Et ole tämän päätöksen päättäjä, joten et voi tehdä päätöstä.',
        link: 'Vaihda itsesi päättäjäksi.'
      },
      mismatchDecisionMakerModal: {
        title: 'Vaihda itsesi päättäjäksi',
        text: 'Päättäjää muuttamalla voit palauttaa päätöksen korjattavaksi tai hylätä tai hyväksyä päätöksen. Nimesi ja tittelisi muutetaan päätökseen.',
        titlePlaceholder: 'Titteli',
        okBtn: 'Vaihda päättäjä'
      },
      rejectDecision: 'Hylkää päätös',
      returnDecisionForEditing: 'Palauta korjattavaksi',
      approveDecision: 'Hyväksy päätös',
      annulDecision: 'Mitätöi päätös'
    },
    attendanceReservation: {
      title: 'Päiväkohtaiset lapsen tulo- ja lähtöajat',
      description:
        'Raportti lasten varauksista ja henkilökunnan määrän tarpeesta',
      ungrouped: 'Ryhmää odottavat lapset',
      capacityFactor: 'Lask',
      staffCountRequired: 'Hlökunta',
      tooLongRange: 'Voit hakea raportin korkeintaan kahden kuukauden ajalta.'
    },
    attendanceReservationByChild: {
      title: 'Lapsikohtaiset läsnäoloajat',
      description:
        'Raportti listaa lapsikohtaisesti huoltajien ilmoittamat lähtö- ja tuloajat. Raportti on saatavilla ryhmä- ja yksikkökohtaisesti.',
      ungrouped: 'Ryhmää odottavat lapset',
      orderByOptions: {
        start: 'Tuloaika',
        end: 'Lähtöaika'
      },
      absence: 'Poissaolo',
      noReservation: 'Varaus puuttuu',
      filterByTime: 'Suodata ajan perusteella',
      reservationStartTime: 'Tulo',
      reservationEndTime: 'Lähtö',
      timeFilterError: 'Virhe'
    },
    childAttendance: {
      title: 'Lapsen läsnä- ja poissaolotiedot',
      range: 'Aikaväli',
      date: 'Päivä',
      reservations: 'Varaus',
      attendances: 'Läsnäolo',
      absenceBillable: 'Poissaolo (maksullisesta)',
      absenceNonbillable: 'Poissaolo (maksuttomasta)'
    },
    customerFees: {
      title: 'Asiakasmaksut',
      description: 'Raportti lapsikohtaisten asiakasmaksujen summista.',
      date: 'Päivämäärä',
      area: 'Palvelualue',
      unit: 'Yksikkö',
      type: 'Päätöstyyppi',
      types: {
        FEE_DECISION: 'Maksupäätökset',
        VOUCHER_VALUE_DECISION: 'Arvopäätökset'
      },
      fee: 'Lapsikohtainen maksu',
      count: 'Lukumäärä'
    },
    duplicatePeople: {
      title: 'Monistuneet kuntalaiset',
      description:
        'Raportti listaa ja antaa yhdistää ihmisiä, jotka vaikuttavat olevan järjestelmässä moneen kertaan.',
      moveFrom: 'Siirrä tiedot',
      moveTo: 'Siirrä tähän',
      confirmMoveTitle:
        'Haluatko varmasti siirtää kaikki tiedot toiselle henkilölle?',
      confirmDeleteTitle: 'Haluatko varmasti poistaa tämän henkilön?',
      errorTitle: 'Tietojen siirtäminen epäonnistui',
      errorText:
        'Tarkista ettei henkilöillä ole esimerkiksi päällekkäisiä sijoituksia, palveluntarpeita tai muita päällekkäisyyksiä, jotka voisivat estää yhdistämisen.',
      columns: {
        'absence.child_id': 'Poissa- oloja',
        'absence.modified_by_guardian_id': 'Itse merkittyjä poissa -oloja',
        'application.child_id': 'Hakemuksia (lapsena)',
        'application.guardian_id': 'Hakemuksia (huoltajana)',
        'application.other_guardian_id': 'Hakemuksia (toisena huoltajana)',
        'assistance_action.child_id': 'Tuki- toimia',
        'assistance_need.child_id': 'Tuen tarpeita',
        'assistance_need_decision.child_id': 'Tuen tarpeen päätöksiä',
        'assistance_need_decision_guardian.person_id':
          'Tuen päätöksen huoltajana',
        'assistance_need_voucher_coefficient.child_id':
          'Tuen palvelusetelikertoimia',
        'attachment.uploaded_by_person': 'Liitteitä',
        'attendance_reservation.child_id': 'Läsnäolo -varauksia',
        'attendance_reservation.created_by_guardian_id':
          'Itse merkittyjä läsnäolo -varauksia',
        'backup_care.child_id': 'Vara- sijoituksia',
        'backup_pickup.child_id': 'Vara- hakijoita',
        'calendar_event_attendee.child_id': 'Kalenteri- osallis- tujana',
        'child_attendance.child_id': 'Läsnäoloja',
        'child_images.child_id': 'Kuvia',
        'backup_curriculum_document.child_id': 'Vanhoja opetussuunnitelemia',
        'daily_service_time.child_id': 'Varhais- kasvatus- aikoja',
        'daily_service_time_notification.guardian_id':
          'Varhais- kasvatus- aikojen ilmoituksia',
        'daycare_daily_note.child_id': 'Muistiin- panoja',
        'family_contact.child_id': 'Yhteys- henkilöitä (lapsi)',
        'family_contact.contact_person_id': 'Yhteys- henkilöitä (aikuinen)',
        'fee_alteration.person_id': 'Maksu- muutoksia',
        'fee_decision.head_of_family_id': 'Maksu- päätöksiä (päämies)',
        'fee_decision.partner_id': 'Maksu- päätöksiä (puoliso)',
        'fee_decision_child.child_id': 'Maksu- päätös- rivejä',
        'fridge_child.child_id': 'Päämiehiä',
        'fridge_child.head_of_child': 'Jääkaappi- lapsia',
        'fridge_partner.person_id': 'Jääkaappi- puolisoja',
        'foster_parent.child_id': 'Sijais- lapsia',
        'foster_parent.parent_id': 'Sijais- vanhempia',
        'holiday_questionnaire_answer.child_id': 'Kyselyvastauksia',
        'income.person_id': 'Tulo- tietoja',
        'income_statement.person_id': 'Tulo -ilmoituksia',
        'invoice.codebtor': 'Laskuja (kanssa -velallinen)',
        'invoice.head_of_family': 'Laskuja',
        'invoice_correction_row.child_id': 'Laskun korjausrivejä (lapsi)',
        'invoice_correction_row.head_of_family':
          'Laskun korjausrivejä (päämies)',
        'invoice_row.child': 'Lasku- rivejä',
        'koski_study_right.child_id': 'Koski opinto- oikeuksia',
        'pedagogical_document.child_id': 'Pedagogisia dokumentteja',
        'placement.child_id': 'Sijoituksia',
        'service_application.child_id': 'Palv.tarve hakemuksia (lapsena)',
        'service_application.person_id': 'Palv.tarve hakemuksia (huoltajana)',
        'varda_child.person_id': 'Varda lapsi',
        'varda_service_need.evaka_child_id': 'Varda palvelun -tarpeita',
        'backup_vasu_document.child_id': 'Vanhoja vasuja',
        'voucher_value_decision.child_id': 'Arvo- päätös- rivejä',
        'voucher_value_decision.head_of_family_id': 'Arvo- päätöksiä (päämies)',
        'voucher_value_decision.partner_id': 'Arvo- päätöksiä (puoliso)',
        'message.sender_id': 'Lähetettyjä viestejä',
        'message_content.author_id': 'Kirjoitettuja viesti- sisältöjä',
        'message_recipients.recipient_id': 'Saatuja viestejä',
        'message_draft.account_id': 'Viesti- luonnoksia'
      }
    },
    familyConflicts: {
      title: 'Perhekonfliktit',
      description:
        'Raportti listaa päämiehet, joiden perhesuhteissa on konflikteja. Konflikti voi muodostua jos hakemuksella ilmoitetut perhesuhteet ovat ristiriidassa aiempien tietojen kanssa.',
      name: 'Päämiehen nimi',
      ssn: 'Hetu',
      partnerConflictCount: 'Konflikteja puolisoissa',
      childConflictCount: 'Konflikteja lapsissa'
    },
    familyContacts: {
      date: 'Päivämäärä',
      name: 'Lapsen nimi',
      ssn: 'Hetu',
      group: 'Ryhmä',
      address: 'Osoite',
      headOfChild: 'Päämies',
      guardian1: 'Huoltaja',
      guardian2: 'Toinen huoltaja',
      phone: 'Puhelinnumero',
      email: 'Sähköpostiosoite'
    },
    familyDaycareMealCount: {
      title: 'Perhepäivähoidossa olevien lasten ateriaraportti',
      description:
        'Raportti laskee perhepäivähoidossa olevien lasten läsnäolomerkinnät ateria-aikoina ja ryhmittelee tulokset yksiköittäin ja alueittain.',
      childName: 'Lapsen nimi',
      firstName: 'Etunimi',
      lastName: 'Sukunimi',
      daycareName: 'Toimintayksikkö',
      timePeriod: 'Ajanjakso',
      timePeriodTooLong: 'Ajanjakso enintään 6kk',
      careArea: 'Palvelualue',
      total: 'Yhteensä',
      breakfastCountHeader: 'Aamiainen',
      lunchCountHeader: 'Lounas',
      snackCountHeader: 'Välipala',
      totalHeader: 'Aterioita yhteensä',
      noCareAreasFound: 'Ei tuloksia sisältäviä palvelualueita',
      noDaycaresFound: 'Ei tuloksia sisältäviä yksiköitä'
    },
    endedPlacements: {
      title: 'Varhaiskasvatuksessa lopettavat lapset',
      description:
        'Kelaan toimitettava raportti varhaiskasvatuksessa lopettavista ja mahdollisesti myöhemmin jatkavista lapsista.',
      ssn: 'Hetu',
      placementEnd: 'Lopettaa varhaiskasvatuksessa',
      unit: 'Yksikkö',
      area: 'Alue',
      nextPlacementStart: 'Jatkaa varhaiskasvatuksessa'
    },
    missingHeadOfFamily: {
      title: 'Puuttuvat päämiehet',
      description:
        'Raportti listaa lapset, joiden nykyisen sijoituksen ajalta puuttuu tieto päämiehestä.',
      childLastName: 'Lapsen sukunimi',
      childFirstName: 'Lapsen etunimi',
      showFosterChildren: 'Näytä myös sijaislapset',
      daysWithoutHeadOfFamily: 'Puutteelliset päivät'
    },
    missingServiceNeed: {
      title: 'Puuttuvat palveluntarpeet',
      description:
        'Raportti listaa lapset, joiden sijoituksen ajalta puuttuu palveluntarve.',
      daysWithoutServiceNeed: 'Puutteellisia päiviä'
    },
    invalidServiceNeed: {
      title: 'Virheelliset palveluntarpeet',
      description:
        'Raportti listaa palveluntarpeet, joissa vaikuttaisi olevan virhe.',
      unit: 'Nykyinen yksikkö',
      noCurrentUnit: 'Lopettanut'
    },
    partnersInDifferentAddress: {
      title: 'Puoliso eri osoitteessa',
      description:
        'Raportti listaa henkilöt, joiden jääkaappipuolisoksi merkitty henkilö asuu VTJ:n mukaan eri osoitteessa. Tarkista ovatko nämä henkilöt enää oikeasti avopuolisoja.',
      person1: 'Henkilö',
      address1: 'Osoite',
      person2: 'Puoliso',
      address2: 'Puolison osoite'
    },
    presence: {
      title: 'Läsnäolot',
      date: 'pvm',
      SSN: 'hetu',
      daycareId: 'varhaiskasvatuslaitos id',
      daycareGroupName: 'ryhmä',
      present: 'läsnä',
      description: 'Talouden tilannehuone -raportti tutkimuskäyttöön',
      info: 'Ajanjakson maksimipituus on kaksi viikkoa.'
    },
    serviceNeeds: {
      title: 'Lasten palvelutarpeet ja iät yksiköissä',
      description:
        'Raportti listaa lasten määriä yksiköissä palveluntarpeen ja iän mukaan.',
      age: 'Ikä',
      fullDay: 'kokopäiväinen',
      partDay: 'osapäiväinen',
      fullWeek: 'kokoviikkoinen',
      partWeek: 'osaviikkoinen',
      shiftCare: 'vuorohoito',
      missingServiceNeed: 'palveluntarve puuttuu',
      total: 'lapsia yhteensä'
    },
    exceededServiceNeed: {
      title: 'Palveluntarpeen ylitykset',
      description:
        'Raportti listaa lapset, joiden palveluntarpeen tunnit on ylitetty.',
      serviceNeedHours: 'Palvelun tarve (h)',
      usedServiceHours: 'Käytetty (h)',
      groupLinkHeading: 'Yksikön viikkokalenteri',
      excessHours: 'Ylitys (h)'
    },
    units: {
      title: 'Yksiköt',
      description: 'Yhteenveto yksiköiden tiedoista.',
      name: 'Nimi',
      careAreaName: 'Palvelualue',
      careTypeCentre: 'Päiväkoti',
      careTypeFamily: <span>Perhe&shy;päivä&shy;hoito</span>,
      careTypeFamilyStr: 'Perhepäivähoito',
      careTypeGroupFamily: <span>Ryhmä&shy;perhe&shy;päivä&shy;hoito</span>,
      careTypeGroupFamilyStr: 'Ryhmäperhepäivähoito',
      careTypeClub: 'Kerho',
      careTypePreschool: 'Esiopetus',
      careTypePreparatoryEducation: 'Valmistava',
      clubApply: <span>Kerho&shy;haku</span>,
      clubApplyStr: 'Kerhohaku',
      daycareApply: <span>Päiväkoti&shy;haku</span>,
      daycareApplyStr: 'Päiväkotihaku',
      preschoolApply: <span>Esiopetus&shy;haku</span>,
      preschoolApplyStr: 'Esiopetushaku',
      providerType: 'Järjestämismuoto',
      uploadToVarda: 'Varda',
      uploadChildrenToVarda: 'Varda (lapset)',
      uploadToKoski: 'Koski',
      ophUnitOid: 'Toimipaikan OID',
      ophOrganizerOid: 'Järjestäjän OID',
      invoicedByMunicipality: 'Laskutetaan eVakasta',
      costCenter: 'Kustannuspaikka',
      address: 'Käyntiosoite',
      unitManagerName: 'Yksikön johtaja',
      unitManagerPhone: 'Johtajan puh.'
    },
    childrenInDifferentAddress: {
      title: 'Lapsi eri osoitteessa',
      description:
        'Raportti listaa päämiehet, joiden jääkaappilapsi asuu VTJ:n mukaan eri osoitteessa. Osa näistä voi olla virheitä, jotka tulisi korjata.',
      person1: 'Päämies',
      address1: 'Päämiehen osoite',
      person2: 'Lapsi',
      address2: 'Lapsen osoite'
    },
    childAgeLanguage: {
      title: 'Lasten kielet ja iät yksiköissä',
      description:
        'Raportti listaa lasten määriä yksiköissä kielen ja iän mukaan. Vain vastaanotetut paikat otetaan huomioon.'
    },
    assistanceNeedsAndActions: {
      title: 'Lasten tuen tarpeet ja tukitoimet',
      description:
        'Raportti listaa lasten määriä yksiköissä ja ryhmissä tuen tarpeen perusteiden ja tukitoimien mukaan. Vain vastaanotetut paikat otetaan huomioon.',
      type: 'Toimintamuoto',
      types: {
        DAYCARE: 'Varhaiskasvatus',
        PRESCHOOL: 'Esiopetus'
      },
      level: 'Tuen taso ja muut toimet',
      showZeroRows: 'Näytä nollarivit',
      groupingTypes: {
        NO_GROUPING: 'Lapset',
        AREA: 'Toimintayksiköt alueittain',
        UNIT: 'Toimintayksiköt'
      },
      basisMissing: 'Peruste puuttuu',
      action: 'Tukitoimi',
      actionMissing: 'Tukitoimi puuttuu',
      assistanceNeedVoucherCoefficient: 'Korotettu PS-kerroin'
    },
    occupancies: {
      title: 'Täyttö- ja käyttöasteet',
      description:
        'Raportti tarjoaa tiedot yhden palvelualueen ja yhden kuukauden käyttö- tai täyttöasteista.',
      filters: {
        areaPlaceholder: 'Valitse palvelualue',
        type: 'Tyyppi',
        types: {
          UNITS: {
            CONFIRMED: 'Vahvistettu täyttöaste yksikössä',
            PLANNED: 'Suunniteltu täyttöaste yksikössä',
            REALIZED: 'Käyttöaste yksikössä'
          },
          GROUPS: {
            CONFIRMED: 'Vahvistettu täyttöaste ryhmissä',
            PLANNED: 'Suunniteltu täyttöaste ryhmissä',
            REALIZED: 'Käyttöaste ryhmissä'
          }
        },
        valueOnReport: 'Näytä tiedot',
        valuesOnReport: {
          percentage: 'Prosentteina',
          headcount: 'Lukumääränä',
          raw: 'Summa ja kasvattajamäärä erikseen'
        }
      },
      unitsGroupedByArea: 'Toimintayksiköt alueittain',
      average: 'Keskiarvo',
      sumUnder3y: 'Alle 3v',
      sumOver3y: 'Yli 3v',
      sum: 'Summa',
      caretakers: 'Kasvattajia',
      missingCaretakersLegend: 'kasvattajien lukumäärä puuttuu'
    },
    invoices: {
      title: 'Laskujen täsmäytys',
      description:
        'Laskujen täsmäytysraportti laskutusjärjestelmään vertailua varten',
      areaCode: 'Alue',
      amountOfInvoices: 'Laskuja',
      totalSumCents: 'Summa',
      amountWithoutSSN: 'Hetuttomia',
      amountWithoutAddress: 'Osoitteettomia',
      amountWithZeroPrice: 'Nollalaskuja'
    },
    startingPlacements: {
      title: 'Varhaiskasvatuksessa aloittavat lapset',
      description:
        'Kelaan toimitettava raportti varhaiskasvatuksessa aloittavista lapsista.',
      ssn: 'Hetu',
      placementStart: 'Aloittaa varhaiskasvatuksessa',
      reportFileName: 'alkavat_sijoitukset'
    },
    voucherServiceProviders: {
      title: 'Palveluseteliyksiköt',
      description:
        'Palveluseteliyksiköihin kohdistuvat palvelusetelisummat sekä lapsikohtaiset maksut.',
      filters: {
        areaPlaceholder: 'Valitse palvelualue',
        allAreas: 'Kaikki alueet',
        unitPlaceholder: 'Hae yksikön nimellä'
      },
      locked: 'Raportti lukittu',
      childCount: 'PS-lasten lkm',
      unitVoucherSum: 'PS summa / kk',
      average: 'Keskiarvo',
      breakdown: 'Erittely'
    },
    voucherServiceProviderUnit: {
      title: 'Palvelusetelilapset yksikössä',
      unitPageLink: 'Yksikön sivu',
      month: 'Kuukausi',
      total: 'Palvelusetelien summa valittuna kuukautena',
      child: 'Lapsen nimi / synt. aika',
      childFirstName: 'Etunimi',
      childLastName: 'Sukunimi',
      note: 'Huomio',
      numberOfDays: 'Päivät',
      start: 'Alkaen',
      end: 'Päättyen',
      serviceVoucherValue: 'Ps korkein arvo',
      serviceVoucherRealizedValue: 'Ps arvo / kk',
      serviceVoucherFinalCoPayment: 'Omavastuu',
      serviceNeed: 'Palveluntarve',
      assistanceNeed: 'Tuen tarve',
      partTime: 'Osa/Koko',
      type: {
        NEW: 'Uusi päätös',
        REFUND: 'Hyvitys',
        CORRECTION: 'Korjaus'
      }
    },
    manualDuplication: {
      title: 'Manuaalisen toisinnuksen raportti',
      description:
        'Raportti listaa esiopetusta täydentävän varhaiskasvatuksen tapaukset, jotka mahdollisesti vaativat lapsen manuaalista toisinnusta',
      childName: 'Lapsen nimi',
      dateOfBirth: 'Syntymäaika',
      connectedDaycare: 'Täydentävän yksikkö',
      connectedSno: 'Täydentävän palveluntarve',
      connectedDuration: 'Täydentävän kesto',
      preschoolDaycare: 'Esiopetuksen yksikkö',
      preschooldDuration: 'Esiopetuksen kesto',
      filters: {
        viewOption: {
          label: 'Valitse näkymä:',
          items: {
            DUPLICATED: 'Näytä vain jo monistetut tapaukset',
            NONDUPLICATED: 'Näytä vain käsittelemättömät tapaukset'
          }
        }
      }
    },
    nonSsnChildren: {
      title: 'Hetuttomat lapset',
      description:
        'Raportti hetuttomista sijoitetuista lapsista OID-tietojen tarkistamiseen',
      childName: 'Lapsen nimi',
      dateOfBirth: 'Syntymäpäivä',
      personOid: 'Lapsen tietojen OID',
      lastSentToVarda: 'Viety Vardaan viimeksi',
      total: 'Yhteensä'
    },
    placementCount: {
      title: 'Sijoitusten määrä',
      description:
        'Raportti sijoitusten määrästä hakuehtojen mukaisissa yksiköissä annettuna päivämääränä',
      noCareAreasFound: 'Ei sijoituksia sisältäviä palvelualueita',
      examinationDate: 'Tarkastelupäivä',
      careArea: 'Palvelualue',
      daycaresByArea: 'Toimintayksiköt alueittain',
      placementCount: 'Lapsia yhteensä',
      calculatedPlacements: 'Laskennallinen määrä',
      providerType: 'Järjestämismuoto',
      placementType: 'Sijoitusmuoto',
      placementsOver3: 'Vähintään 3v',
      placementsUnder3: 'Alle 3v',
      total: 'Yhteensä'
    },
    placementGuarantee: {
      title: 'Varhaiskasvatuspaikkatakuu',
      description:
        'Raportti näyttää varhaiskasvatuspaikkatakuulla olevat lapset'
    },
    placementSketching: {
      title: 'Sijoitusten hahmotteluraportti',
      description:
        'Raportti saapuneista esiopetushakemuksista sijoittamisen avuksi',
      placementStartDate: 'Nykyisen sijoituksen tarkistuspäivä',
      earliestPreferredStartDate: 'Aikaisin haettu aloituspäivä',
      preferredUnit: 'Hakutoive',
      currentUnit: 'Nykyinen yksikkö',
      streetAddress: 'Osoite',
      postalCode: 'Postinumero',
      tel: 'Puhelu',
      email: 'email',
      dob: 'Syntymäaika',
      serviceNeedOption: 'Palveluntarve',
      assistanceNeed: 'Tuen tarve',
      preparatory: 'Valmistava',
      siblingBasis: 'Sisarusperuste',
      connected: 'Liittyvä',
      applicationStatus: 'Hakemuksen tila',
      preferredStartDate: 'Toivottu aloituspäivä',
      sentDate: 'Lähetyspäivä',
      otherPreferredUnits: 'Muut hakutoiveet',
      additionalInfo: 'Lisätiedot',
      childMovingDate: 'Lapsen muuttopäivä',
      childCorrectedStreetAddress: 'Lapsen uusi katuosoite',
      childCorrectedPostalCode: 'Lapsen uusi postinumero',
      childCorrectedCity: 'Lapsen uusi postitoimipaikka',
      applicationSentDateRange: 'Hakemus lähetetty välillä'
    },
    vardaChildErrors: {
      title: 'Varda-lapsivirheet',
      vardaInfo:
        'Aloita päivitys-nappi käynnistää VardaUpdate-eräajon, joka vie kaikkien Vardaan vietäväksi ' +
        'merkittyjen yksiköiden ja lapsien muuttuneet tiedot Vardaan. Aloita uudelleenvienti-nappi käynnistää ' +
        'VardaReset-eräajon, joka poistaa ja vie uudelleen kaikkien uudelleenvietäviksi merkittyjen lasten ' +
        'kaikki vardatiedot. Raportin Uudelleenvie-nappi käynnistää vain kyseiselle lapselle VardaReset-eräajon.',
      vardaUpdateButton: 'Aloita päivitys',
      vardaResetButton: 'Aloita uudelleenvienti',
      description: 'Varda-lasten päivityksissä tapahtuneet virheet',
      updated: 'Päivitetty viimeksi',
      age: 'Ikä (päivää)',
      child: 'Lapsi',
      serviceNeed: 'Palveluntarve',
      error: 'Virhe',
      childLastReset: 'Uudelleenviety viimeksi',
      childMarkedForRest: 'Lapsen tiedot nollataan seuraavalla ajolla',
      resetChild: 'Uudelleenvie',
      updating: 'Päivittää'
    },
    vardaUnitErrors: {
      title: 'Varda-yksikkövirheet',
      description: 'Varda-yksiköiden päivityksissä tapahtuneet virheet',
      age: 'Virheen ikä (päivää)',
      unit: 'Yksikkö',
      error: 'Virhe'
    },
    sextet: {
      title: 'Kuusikkovertailu',
      description:
        'Raportti vuoden toteutuneista läsnäolopäivistä yksiköittäin ja sijoitustyypeittäin',
      placementType: 'Sijoitustyyppi',
      year: 'Vuosi',
      unitName: 'Yksikkö',
      attendanceDays: 'Todelliset läsnäolopäivät'
    },
    invoiceGeneratorDiff: {
      title: 'Laskugeneraattorien eroavaisuudet',
      description:
        'Työkalu uuden laskugeneraattorin analysointiin vs vanha laskugeneraattori',
      report: 'Raportti laskugeneraattorien eroavaisuuksista'
    },
    futurePreschoolers: {
      title: 'Tulevat esikoululaiset',
      description:
        'Raportti tulevan vuoden esiopetuksen lapsista ja yksiköistä automaattisijoitustyökalua varten',
      futurePreschoolersCount: (count: number) =>
        count === 1
          ? `${count} tuleva esikoululainen`
          : `${count} tulevaa esikoululaista`,
      preschoolUnitCount: (count: number) =>
        count === 1
          ? `${count} esiopetusta antava yksikkö`
          : `${count} esiopetusta antavaa yksikköä`,
      sourceUnitCount: (count: number) =>
        count === 1
          ? `${count} tulevien esikoululaisten nykyinen yksikkö`
          : `${count} tulevien esikoululaisten nykyistä yksikköä`
    },
    meals: {
      title: 'Ruokailijamäärät',
      description:
        'Laskee varauksiin perustuvat ruokailijamäärät yksikkökohtaisesti.',
      wholeWeekLabel: 'Koko viikko',
      jamixSend: {
        button: 'Lähetä uudelleen Jamixiin'
      },
      mealName: {
        BREAKFAST: 'Aamupala',
        LUNCH: 'Lounas',
        LUNCH_PRESCHOOL: 'Lounas (esiopetus)',
        SNACK: 'Välipala',
        SUPPER: 'Päivällinen',
        EVENING_SNACK: 'Iltapala'
      },
      headings: {
        mealName: 'Ateria',
        mealId: 'Aterian tunniste',
        mealCount: 'kpl-määrä',
        dietId: 'Erityisruokavalion tunniste',
        dietAbbreviation: 'Erv. lyhenne',
        mealTextureId: 'Ruoan rakenteen tunniste',
        mealTextureName: 'Ruoan rakenne',
        additionalInfo: 'Lisätieto'
      }
    },
    preschoolAbsences: {
      title: 'Esiopetuksen poissaoloraportti',
      description:
        'Raportti listaa esiopetuskauden lapsikohtaiset poissaolomäärät valitulle yksikölle ja ryhmälle',
      firstName: 'Etunimi',
      lastName: 'Sukunimi',
      hours: '(tuntia)',
      total: 'Yhteensä',
      filters: {
        daycareSelection: {
          label: 'Esiopetusyksikkö:',
          placeholder: 'Valitse yksikkö'
        },
        groupSelection: {
          label: 'Ryhmä:',
          placeholder: 'Valitse ryhmä'
        },
        preschoolTerm: {
          label: 'Esiopetuskausi:',
          placeholder: 'Valitse esiopetuskausi'
        }
      }
    },
    holidayPeriodAttendance: {
      title: 'Lomakyselyraportti',
      description: 'Yksikön läsnäolojen päivätason seuranta lomakyselyn aikana',
      periodFilter: 'Lomakysely',
      periodFilterPlaceholder: 'Valitse lomakysely',
      unitFilter: 'Yksikkö',
      fetchButton: 'Hae',
      dateColumn: 'Päivä',
      presentColumn: 'Paikalla',
      assistanceColumn: 'Paikallaolevista tukitoimelliset',
      occupancyColumn: 'Paikalla yhteensä (kerroin)',
      occupancyColumnInfo:
        'Kertoimeen lasketaan kaikkien paikallaolevien lasten kerroin yhteensä. Kertoimeen vaikuttaa esimerkiksi lapsen ikä ja tuen tarve.',
      staffColumn: 'Hlö. kunnan tarve',
      absentColumn: 'Poissa',
      noResponseColumn: 'Ei vastannut',
      moreText: 'lisää',
      showMoreButton: 'Näytä lisää'
    }
  },
  unitEditor: {
    submitNew: 'Luo yksikkö',
    title: {
      contact: 'Yksikön yhteystiedot',
      unitManager: 'Varhaiskasvatusyksikön johtajan yhteystiedot',
      decisionCustomization:
        'Yksikön nimi päätöksellä ja ilmoitus paikan vastaanottamisesta',
      mealOrderIntegration: 'Ruokatilausintegraatio',
      mealtime: 'Yksikön ruokailuajat'
    },
    label: {
      name: 'Yksikön nimi',
      openingDate: 'Yksikön alkamispäivä',
      closingDate: 'päättymispäivä',
      area: 'Alue',
      careTypes: 'Toimintamuodot',
      canApply: 'Näytä yksikkö',
      providerType: 'Järjestämismuoto',
      operationDays: 'Toimintapäivät',
      shiftCareOperationDays: 'Vuorohoidon toimintapäivät',
      operationDay: {
        0: 'SU',
        1: 'MA',
        2: 'TI',
        3: 'KE',
        4: 'TO',
        5: 'PE',
        6: 'LA',
        7: 'SU'
      },
      shiftCare: 'Ilta- ja vuorohoito',
      capacity: 'Yksikön laskennallinen lapsimäärä',
      language: 'Yksikön kieli',
      withSchool: 'Koulun yhteydessä',
      ghostUnit: 'Haamuyksikkö',
      integrations: 'Integraatiot',
      invoicedByMunicipality: 'Laskutetaan eVakasta',
      ophUnitOid: 'Toimipaikan OID',
      ophOrganizerOid: 'Järjestäjän OID',
      costCenter: 'Kustannuspaikka',
      dwCostCenter: 'DW Kustannuspaikka',
      financeDecisionHandler: 'Talouspäätösten käsittelijä',
      additionalInfo: 'Lisätietoja yksiköstä',
      phone: 'Yksikön puhelinnumero',
      email: 'Yksikön sähköpostiosoite',
      url: 'Yksikön URL-osoite',
      visitingAddress: 'Käyntiosoite',
      location: 'Karttakoordinaatit',
      mailingAddress: 'Postiosoite',
      unitManager: {
        name: 'Johtajan nimi',
        phone: 'Johtajan puhelinnumero',
        email: 'Johtajan sähköpostiosoite'
      },
      decisionCustomization: {
        daycareName: 'Yksikön nimi varhaiskasvatuspäätöksellä',
        preschoolName: 'Yksikön nimi esiopetuspäätöksellä',
        handler: 'Huoltajan ilmoituksen vastaanottaja',
        handlerAddress: 'Ilmoituksen vastaanottajan osoite'
      },
      businessId: 'Y-tunnus',
      iban: 'Tilinumero',
      providerId: 'Toimittajanumero',
      mealTime: {
        breakfast: 'Aamupala',
        lunch: 'Lounas',
        snack: 'Välipala',
        supper: 'Päivällinen',
        eveningSnack: 'Iltapala'
      }
    },
    field: {
      applyPeriod: 'Kun toivottu alkamispäivä aikavälillä',
      canApplyDaycare: 'Varhaiskasvatushaussa',
      canApplyPreschool: 'Esiopetushaussa',
      canApplyClub: 'Kerhohaussa',
      providesShiftCare: 'Yksikkö tarjoaa ilta- ja vuorohoitoa',
      shiftCareOpenOnHolidays: 'Vuorohoito on auki myös pyhäpäivinä',
      capacity: 'henkilöä',
      withSchool: 'Yksikkö sijaitsee koulun yhteydessä',
      ghostUnit: 'Yksikkö on haamuyksikkö',
      uploadToVarda: 'Yksikön tiedot lähetetään Vardaan',
      uploadChildrenToVarda: 'Yksikön lasten tiedot lähetetään Vardaan',
      uploadToKoski: 'Lähetetään Koski-palveluun',
      invoicedByMunicipality: 'Laskutetaan eVakasta',
      invoicingByEvaka: 'Yksikön laskutus tapahtuu eVakasta',
      decisionCustomization: {
        handler: [
          'Palveluohjaus',
          'Varhaiskasvatusyksikön johtaja',
          'Ledare inom småbarnspedagogik',
          'Svenska bildningstjänster / Småbarnspedagogik'
        ]
      }
    },
    placeholder: {
      name: 'Anna yksikölle nimi',
      openingDate: 'Alkaen pp.kk.vvvv',
      closingDate: 'Päättyen pp.kk.vvvv',
      area: 'Valitse alue',
      financeDecisionHandler: 'Valitse työntekijä',
      daycareType: 'Valitse tyyppi',
      costCenter: '(eVakasta laskutettaessa pakollinen tieto)',
      dwCostCenter: 'DW:tä varten kustannuspaikan tieto',
      additionalInfo:
        'Voit kirjoittaa lisätietoja yksiköstä (ei näy kuntalaiselle)',
      phone: 'esim. +358 40 555 5555',
      email: 'etunimi.sukunimi@espoo.fi',
      url: 'esim. https://www.espoo.fi/fi/toimipisteet/15585',
      streetAddress: 'Kadunnimi esim. Koivu-Mankkaan tie 22 B 24',
      postalCode: 'Postinumero',
      postOffice: 'Toimipaikka',
      location: 'esim. 60.223038, 24.692637',
      unitManager: {
        name: 'Etunimi Sukunimi'
      },
      decisionCustomization: {
        name: 'esim. Aamunkoiton päiväkoti'
      }
    },
    error: {
      name: 'Nimi puuttuu',
      area: 'Alue puuttuu',
      careType: 'Toimintamuoto puuttuu',
      dailyPreschoolTime: 'Esiopetusaika puuttuu tai on virheellinen',
      dailyPreparatoryTime:
        'Valmistavan opetuksen aika puuttuu tai on virheellinen',
      daycareType: 'Varhaiskasvatuksen tyyppi puuttuu',
      capacity: 'Kapasiteetti on virheellinen',
      costCenter: 'Kustannuspaikka puuttuu',
      url: 'URL-osoitteessa pitää olla https://- tai http://-etuliite',
      visitingAddress: {
        streetAddress: 'Käyntiosoitteen katuosoite puuttuu',
        postalCode: 'Käyntiosoitteen postinumero puuttuu',
        postOffice: 'Käyntiosoitteen postitoimipaikka puuttuu'
      },
      location: 'Karttakoordinaatit ovat virheellisiä',
      unitManager: {
        name: 'Johtajan nimi puuttuu',
        phone: 'Johtajan puhelinnumero puuttuu',
        email: 'Johtajan sähköposti puuttuu'
      },
      cannotApplyToDifferentType: 'Hakutyyppi ja palvelumuoto eivät vastaa',
      financeDecisionHandler: 'Talouspäätösten käsittelijä puuttuu',
      ophUnitOid: 'Yksikön OID puuttuu',
      ophOrganizerOid: 'Järjestäjän OID puuttuu',
      openingDateIsAfterClosingDate: 'Aloituspäivä on päättymispäivän jälkeen',
      businessId: 'Y-tunnus puuttuu',
      iban: 'Tilinumero puuttuu',
      providerId: 'Toimittajanumero puuttuu',
      operationTimes: 'Virheellinen merkintä yksikön toiminta-ajoissa',
      shiftCareOperationTimes:
        'Virheellinen merkintä yksikön vuorohoidon toiminta-ajoissa',
      mealTimes: 'Virheellinen merkintä yksikön ruokailuajoissa',
      closingDateBeforeLastPlacementDate: (lastPlacementDate: LocalDate) =>
        `Yksikössä on sijoituksia ${lastPlacementDate.format()} asti. Kaikki sijoitukset ja varasijoitukset tulee päättää yksikön päättymispäivään mennessä, mukaan lukien myös mahdolliset tulevaisuuden sijoitukset.`
    },
    warning: {
      onlyMunicipalUnitsShouldBeSentToVarda:
        'Älä lähetä Vardaan muiden kuin kunnallisten ja kunnallisten ostopalveluyksiköiden tietoja.',
      handlerAddressIsMandatory:
        'Ilmoituksen vastaanottajan osoite on pakollinen, jos yksikön järjestämismuodoksi on valittu kunnallinen, ostopalvelu tai palveluseteli.'
    }
  },
  fileUpload: {
    download: {
      modalHeader: 'Tiedoston käsittely on kesken',
      modalMessage:
        'Tiedosto ei ole juuri nyt avattavissa. Kokeile hetken kuluttua uudelleen.',
      modalClose: 'Sulje'
    }
  },
  messages: {
    inboxTitle: 'Viestit',
    emptyInbox: 'Tämä kansio on tyhjä',
    replyToThread: 'Vastaa viestiin',
    archiveThread: 'Arkistoi viestiketju',
    unitList: {
      title: 'Yksiköt'
    },
    sidePanel: {
      municipalMessages: 'Kunnan tiedotteet',
      serviceWorkerMessages: 'Palveluohjauksen viestit',
      ownMessages: 'Omat viestit',
      groupsMessages: 'Ryhmien viestit',
      noAccountAccess:
        'Viestejä ei voi näyttää, koska sinua ei ole luvitettu ryhmään. Pyydä lupa esimieheltäsi.'
    },
    messageBoxes: {
      names: {
        received: 'Saapuneet',
        sent: 'Lähetetyt',
        drafts: 'Luonnokset',
        copies: 'Johtajan/kunnan tiedotteet',
        archive: 'Arkisto',
        thread: 'Viestiketju'
      },
      receivers: 'Vastaanottajat',
      newMessage: 'Uusi viesti'
    },
    messageList: {
      titles: {
        received: 'Saapuneet viestit',
        sent: 'Lähetetyt viestit',
        drafts: 'Luonnokset',
        copies: 'Johtajan/kunnan tiedotteet',
        archive: 'Arkisto',
        thread: 'Viestiketju'
      }
    },
    types: {
      MESSAGE: 'Viesti',
      BULLETIN: 'Tiedote'
    },
    receiverSelection: {
      title: 'Vastaanottajat',
      childName: 'Nimi',
      childDob: 'Syntymäaika',
      receivers: 'Vastaanottajat',
      confirmText: 'Lähetä viesti valituille'
    },
    noTitle: 'Ei otsikkoa',
    notSent: 'Ei lähetetty',
    editDraft: 'Muokkaa luonnosta',
    undo: {
      info: 'Viesti lähetetty',
      secondsLeft: (s: number) =>
        s === 1 ? '1 sekunti aikaa' : `${s} sekuntia aikaa`
    },
    sensitive: 'arkaluontoinen',
    messageEditor: {
      message: 'Viesti',
      newMessage: 'Uusi viesti',
      to: {
        label: 'Vastaanottaja',
        placeholder: 'Valitse ryhmä',
        noOptions: 'Ei ryhmiä'
      },
      recipients: 'Vastaanottajat',
      recipientCount: 'Vastaanottajia',
      manyRecipientsWarning: {
        title: 'Viestillä on suuri määrä vastaanottajia.',
        text: (count: number) =>
          `Tämä viesti on lähdössä ${count} vastaanottajalle. Oletko varma, että haluat lähettää viestin?`
      },
      type: {
        label: 'Viestin tyyppi',
        message: 'Viesti',
        bulletin: 'Tiedote (ei voi vastata)'
      },
      flags: {
        heading: 'Viestin lisämerkinnät',
        urgent: {
          info: 'Lähetä viesti kiireellisenä vain, jos haluat että huoltaja lukee sen työpäivän aikana.',
          label: 'Kiireellinen'
        },
        sensitive: {
          info: 'Arkaluontoisen viestin avaaminen vaatii kuntalaiselta vahvan tunnistautumisen.',
          label: 'Arkaluontoinen',
          whyDisabled:
            'Arkaluontoisen viestin voi lähettää vain henkilökohtaisesta käyttäjätilistä yksittäisen lapsen huoltajille.'
        }
      },
      sender: 'Lähettäjä',
      selectPlaceholder: 'Valitse...',
      filters: {
        showFilters: 'Näytä lisävalinnat',
        hideFilters: 'Piilota lisävalinnat',
        yearOfBirth: 'Syntymävuosi',
        shiftCare: {
          heading: 'Vuorohoito',
          label: 'Vuorohoito',
          intermittent: 'Satunnainen vuorohoito'
        },
        familyDaycare: {
          heading: 'Perhepäivähoito',
          label: 'Perhepäivähoito'
        }
      },
      title: 'Otsikko',
      deleteDraft: 'Hylkää luonnos',
      send: 'Lähetä',
      sending: 'Lähetetään'
    }
  },
  pinCode: {
    title: 'eVaka-mobiilin PIN-koodi',
    title2: 'Aseta PIN-koodi',
    text1:
      'Tällä sivulla voit asettaa oman henkilökohtaisen PIN-koodisi eVaka-mobiilia varten. PIN-koodia käytetään eVaka-mobiilissa lukon',
    text2: 'takana olevien tietojen tarkasteluun.',
    text3: 'Huom!',
    text4:
      'Ethän luovuta PIN-koodiasi kenenkään toisen henkilön tietoon. Tarvittaessa voit vaihtaa PIN-koodin milloin vain.',
    text5:
      'PIN-koodin tulee sisältää neljä (4) numeroa. Yleisimmät numeroyhdistelmät (esim. 1234) eivät kelpaa.',
    pinCode: 'PIN-koodi',
    button: 'Tallenna PIN-koodi',
    placeholder: '4 numeroa',
    error: 'Liian helppo PIN-koodi tai PIN-koodi sisältää kirjaimia',
    locked: 'PIN-koodi on lukittu, vaihda se uuteen',
    lockedLong:
      'PIN-koodi on syötetty eVaka-mobiilissa 5 kertaa väärin, ja koodi on lukittu. Ole hyvä ja vaihda tilalle uusi PIN-koodi.',
    link: 'eVaka-mobiilin PIN-koodi',
    unsavedDataWarning: 'Et ole tallentanut PIN-koodia'
  },
  employees: {
    name: 'Nimi',
    rights: 'Oikeudet',
    lastLogin: 'Kirjautunut viimeksi',
    employeeNumber: 'Henkilönumero',
    temporary: 'Tilapäinen sijainen',
    findByName: 'Etsi nimellä',
    activate: 'Aktivoi',
    activateConfirm: 'Haluatko palauttaa käyttäjän aktiiviseksi?',
    deactivate: 'Deaktivoi',
    deactivateConfirm: 'Haluatko deaktivoida käyttäjän?',
    hideDeactivated: 'Näytä vain aktiiviset',
    editor: {
      globalRoles: 'Järjestelmäroolit',
      unitRoles: {
        title: 'Luvitukset',
        unit: 'Yksikkö',
        role: 'Rooli yksikössä',
        deleteConfirm: 'Haluatko poistaa käyttäjän luvituksen?',
        deleteAll: 'Poista kaikki luvitukset',
        deleteAllConfirm: 'Haluatko poistaa käyttäjän kaikki luvitukset?',
        addRoles: 'Lisää luvituksia',
        addRolesModalTitle: 'Uusi luvitus',
        units: 'Yksiköt'
      },
      mobile: {
        title: 'Henkilökohtaiset mobiililaitteet',
        name: 'Laitteen nimi',
        nameless: 'Nimeämätön laite',
        deleteConfirm: 'Haluatko poistaa käyttäjän mobiililaitteen parituksen?'
      }
    }
  },
  financeBasics: {
    fees: {
      title: 'Asiakasmaksut',
      add: 'Luo uudet asiakasmaksut',
      thresholds: 'Tulorajat',
      validDuring: 'Asiakasmaksut ajalle',
      familySize: 'Perheen koko',
      minThreshold: 'Vähimmäisbruttotulo €/kk',
      maxThreshold: 'Korkeimman maksun bruttotuloraja €/kk',
      maxFeeError: 'Enimmäismaksu ei täsmää',
      thresholdIncrease: 'Tulorajan korotussumma, kun perhekoko > 6',
      thresholdIncreaseInfo:
        'Jos perheen koko on suurempi kuin 6, korotetaan maksun määräämisen perusteena olevaa tulorajaa korotussumman verran kustakin seuraavasta perheen alaikäisestä lapsesta.',
      multiplier: 'Maksu %',
      maxFee: 'Enimmäismaksu',
      minFee: 'Pienin perittävä lapsikohtainen maksu',
      siblingDiscounts: 'Sisaralennukset',
      siblingDiscount2: 'Alennus% 1. sisarus',
      siblingDiscount2Plus: 'Alennus% muut sisarukset',
      temporaryFees: 'Tilapäisen varhaiskasvatuksen maksut',
      temporaryFee: 'Perushinta',
      temporaryFeePartDay: 'Osapäiväinen',
      temporaryFeeSibling: 'Perushinta, toinen lapsi',
      temporaryFeeSiblingPartDay: 'Osapäiväinen, toinen lapsi',
      errors: {
        'date-overlap':
          'Maksuasetukset menevät päällekkäin jonkin muun voimassaolevan asetuksen kanssa. Päivitä muiden maksuasetusten voimassaoloaika ensin.'
      },
      modals: {
        editRetroactive: {
          title: 'Haluatko varmasti muokata tietoja?',
          text: 'Haluatko varmasti muokata jo käytössä olevia maksutietoja? Mikäli muokkaat tietoja, kaikille asiakkaille, joita muutos koskee, luodaan takautuva maksu- tai arvopäätös.',
          resolve: 'Muokkaa',
          reject: 'Älä muokkaa'
        },
        saveRetroactive: {
          title: 'Haluatko tallentaa maksuasetukset takautuvasti?',
          text: 'Olet tallentamassa maksuasetuksia, jotka vaikuttavat takautuvasti. Mikäli tallennat tiedot, kaikille asiakkaille, joihin muutos vaikuttaa, luodaan uusi takautuva maksu- tai arvopäätös.',
          resolve: 'Tallenna',
          reject: 'Peruuta'
        }
      }
    },
    serviceNeeds: {
      title: 'Palveluntarpeet',
      add: 'Lisää uusi palveluseteliarvo',
      voucherValues: 'Palvelusetelien arvot',
      validity: 'Voimassaoloaika',
      baseValue: (
        <>
          Perusarvo,
          <br />
          3v tai yli (€)
        </>
      ),
      coefficient: (
        <>
          Kerroin,
          <br />
          3v tai yli
        </>
      ),
      value: (
        <>
          Enimmäisarvo,
          <br />
          3v tai yli (€)
        </>
      ),
      baseValueUnder3y: (
        <>
          Perusarvo,
          <br />
          alle 3v
        </>
      ),
      coefficientUnder3y: (
        <>
          Kerroin,
          <br />
          alle 3v
        </>
      ),
      valueUnder3y: (
        <>
          Enimmäisarvo,
          <br />
          alle 3v (€)
        </>
      ),
      errors: {
        'date-overlap':
          'Voimassaolo ei voi alkaa ennen toisen palveluseteliarvon alkamispäivää',
        'end-date-overlap':
          'Voimassaolo ei voi alkaa ennen edellisen palvelusetelin päättymispäivää seuraavaa päivää',
        'date-gap': 'Voimassaolojen välissä ei voi olla aukkoja',
        shouldNotHappen: 'Odottamaton virhe'
      },
      modals: {
        deleteVoucherValue: {
          title: 'Haluatko varmasti poistaa palveluseteliarvon?'
        }
      }
    }
  },
  documentTemplates: {
    title: 'Lomakepohjat',
    documentTypes: {
      PEDAGOGICAL_REPORT: 'Pedagoginen selvitys',
      PEDAGOGICAL_ASSESSMENT: 'Pedagoginen arvio',
      HOJKS: 'HOJKS',
      MIGRATED_VASU: 'Varhaiskasvatussuunnitelma',
      MIGRATED_LEOPS: 'Esiopetuksen oppimissuunnitelma',
      VASU: 'Varhaiskasvatussuunnitelma',
      LEOPS: 'Esiopetuksen oppimissuunnitelma',
      OTHER: 'Muu lomake'
    },
    languages: {
      FI: 'Suomenkielinen',
      SV: 'Ruotsinkielinen'
    },
    templatesPage: {
      add: 'Luo uusi',
      name: 'Nimi',
      type: 'Tyyppi',
      language: 'Kieli',
      validity: 'Voimassa',
      documentCount: 'Dokumentteja',
      status: 'Tila',
      published: 'Julkaistu',
      draft: 'Luonnos',
      export: 'Vie tiedostoon',
      import: 'Tuo tiedostosta'
    },
    templateModal: {
      title: 'Uusi lomakepohja',
      name: 'Nimi',
      type: 'Lomakkeen tyyppi',
      placementTypes: 'Käytössä sijoituksilla',
      language: 'Lomakkeen kieli',
      confidential: 'Lomake on salassapidettävä',
      legalBasis: 'Lait joihin lomake perustuu (vapaaehtoinen)',
      validity: 'Voimassa ajalla',
      processDefinitionNumber: 'Tehtäväluokka',
      processDefinitionNumberInfo:
        'Tiedonohjaussuunnitelmassa määritelty tehtäväluokan numero. Jätä tyhjäksi jos lomaketta ei arkistoida.',
      archiveDurationMonths: 'Arkistointiaika (kuukautta)'
    },
    templateEditor: {
      confidential: 'Salassapidettävä',
      addSection: 'Uusi osio',
      titleNewSection: 'Uusi osio',
      titleEditSection: 'Muokkaa osiota',
      sectionName: 'Otsikko',
      infoText: 'Ohjeteksti',
      addQuestion: 'Uusi osio',
      titleNewQuestion: 'Uusi kysymys',
      titleEditQuestion: 'Muokkaa kysymystä',
      moveUp: 'Siirrä ylös',
      moveDown: 'Siirrä alas',
      readyToPublish: 'Valmis julkaistavaksi',
      forceUnpublish: {
        button: 'Peruuta julkaisu',
        confirmationTitle: 'Haluatko varmasti perua julkaisun?',
        confirmationText:
          'Kaikki tätä lomakepohjaa käyttävät asiakirjat poistetaan.'
      }
    },
    questionTypes: {
      TEXT: 'Tekstikenttä',
      CHECKBOX: 'Rasti',
      CHECKBOX_GROUP: 'Monivalinta',
      RADIO_BUTTON_GROUP: 'Monivalinta (valitse yksi)',
      STATIC_TEXT_DISPLAY: 'Tekstikappale ilman kysymystä',
      DATE: 'Päivämäärä',
      GROUPED_TEXT_FIELDS: 'Nimettyjä tekstikenttiä'
    },
    ...components.documentTemplates
  },
  settings: {
    key: 'Asetus',
    value: 'Arvo',
    options: {
      DECISION_MAKER_NAME: {
        title: 'Päätöksentekijän nimi',
        description: 'Varhaiskasvatus- ja palvelusetelipäätökselle tuleva nimi'
      },
      DECISION_MAKER_TITLE: {
        title: 'Päätöksentekijän titteli',
        description:
          'Varhaiskasvatus- ja palvelusetelipäätökselle tuleva titteli'
      }
    }
  },
  unitFeatures: {
    page: {
      title: 'Yksiköille avatut toiminnot',
      unit: 'Yksikkö',
      selectAll: 'Valitse kaikki',
      unselectAll: 'Poista kaikki',
      providerType: 'Yksikön toimintamuoto',
      careType: 'Yksikön hoitomuoto',
      undo: 'Kumoa edellinen muutos'
    },
    pilotFeatures: {
      MESSAGING: 'Viestintä',
      MOBILE: 'Mobiili',
      RESERVATIONS: 'Varaukset',
      VASU_AND_PEDADOC: 'Vasu ja pedagoginen dokumentointi',
      MOBILE_MESSAGING: 'Mobiili-viestintä',
      PLACEMENT_TERMINATION: 'Paikan irtisanominen',
      REALTIME_STAFF_ATTENDANCE: 'Henkilökunnan reaaliaikainen läsnäolo',
      PUSH_NOTIFICATIONS: 'Mobiilinotifikaatiot',
      SERVICE_APPLICATIONS: 'Palveluntarpeen muutoshakemukset'
    }
  },
  roles: {
    adRoles: {
      ADMIN: 'Pääkäyttäjä',
      DIRECTOR: 'Hallinto',
      MESSAGING: 'Viestintä',
      REPORT_VIEWER: 'Raportointi',
      FINANCE_ADMIN: 'Talous',
      FINANCE_STAFF: 'Talouden työntekijä (ulkoinen)',
      SERVICE_WORKER: 'Palveluohjaus',
      SPECIAL_EDUCATION_TEACHER: 'Erityisopettaja',
      EARLY_CHILDHOOD_EDUCATION_SECRETARY: 'Varhaiskasvatussihteeri',
      STAFF: 'Henkilökunta',
      GROUP_STAFF: 'Ryhmän henkilökunta',
      UNIT_SUPERVISOR: 'Johtaja',
      MOBILE: 'Mobiili',
      END_USER: 'Kuntalainen',
      CITIZEN_WEAK: 'Kuntalainen (heikko)'
    }
  },
  welcomePage: {
    text: 'Olet kirjautunut sisään Espoon kaupungin eVaka-palveluun. Käyttäjätunnuksellesi ei ole vielä annettu oikeuksia, jotka mahdollistavat palvelun käytön. Tarvittavat käyttöoikeudet saat omalta esimieheltäsi.'
  },
  validationErrors: {
    ...components.validationErrors,
    ...components.datePicker.validationErrors,
    dateRangeNotLinear:
      'Aikavälin aloituspäivä tulee olla ennen lopetuspäivää.',
    timeRangeNotLinear: 'Tarkista järjestys',
    guardianMustBeHeard: 'Huoltajaa on kuultava',
    futureTime: 'Aika tulevaisuudessa'
  },
  holidayPeriods: {
    confirmDelete: 'Haluatko varmasti poistaa loma-ajan?',
    createTitle: 'Luo uusi loma-aika',
    editTitle: 'Muokkaa loma-aikaa',
    period: 'Aikaväli',
    reservationsOpenOn: 'Kysely avataan',
    reservationDeadline: 'Varausten takaraja',
    clearingAlert:
      'Kuntalaisten jo tekemät varaukset pyyhitään valitulta aikaväliltä',
    confirmLabel:
      'Ymmärrän, että tehdyt varaukset poistetaan välittömästi, eikä tätä voi enää perua.',
    validationErrors: {
      tooSoon: 'Loma-ajan voi luoda aikaisintaan 4 viikon päähän',
      tooLong: 'Loma-aika voi olla enintään 15 viikkoa pitkä',
      afterStart: 'Ei voi olla alkamisen jälkeen',
      afterReservationsOpen: 'Ei voi olla avaamispäivän jälkeen'
    }
  },
  holidayQuestionnaires: {
    confirmDelete: 'Haluatko varmasti poistaa kyselyn?',
    types: {
      FIXED_PERIOD: 'Kiinteä kausi'
    },
    questionnaires: 'Poissaolokyselyt',
    absenceType: 'Poissaolon tyyppi',
    title: 'Otsikko',
    description: 'Kyselyn selite kuntalaiselle',
    descriptionLink: 'Lisätietolinkki',
    active: 'Voimassa',
    fixedPeriodOptionLabel: 'Kauden valinnan kysymys',
    fixedPeriodOptionLabelPlaceholder:
      'Esim. Lapset ovat 8 viikkoa poissa aikavälillä',
    fixedPeriodOptions: 'Kausien vaihtoehdot',
    fixedPeriodOptionsPlaceholder:
      '30.5.2022-24.8.2022, 6.6.2022-31.8.2022, pilkuilla tai rivinvaihdoilla erotettuna',
    requiresStrongAuth: 'Vahva tunnistautuminen',
    conditionContinuousPlacement:
      'Kyselyyn voi vastata jos lapsella yhtäjaksoinen sijoitus'
  },
  terms: {
    term: 'Lukukausi',
    finnishPreschool: 'Suomenkielinen esiopetus',
    extendedTermStart: 'Pidennetty lukukausi alkaa',
    applicationPeriodStart: 'Haku lukukaudelle alkaa',
    termBreaks: 'Opetustauot',
    addTerm: 'Lisää lukukausi',
    confirmDelete: 'Haluatko varmasti poistaa lukukauden?',
    extendedTermStartInfo:
      'Aika, jolloin varhaiskasvatusmaksu määräytyy liittyvän varhaiskasvatuksen mukaan.',
    termBreaksInfo:
      'Lisää tähän sellaiset ajat lukukauden aikana, jolloin opetusta ei tarjota, esim. joululomat.',
    addTermBreak: 'Lisää taukojakso',
    validationErrors: {
      overlap:
        'Tälle ajanjaksolle on jo päällekkäinen lukukausi. Yritä kirjata merkintä eri ajanjaksolle.',
      extendedTermOverlap:
        'Tälle ajanjaksolle on jo päällekkäinen pidennetty lukukausi. Yritä kirjata merkintä eri aloituspäivälle',
      extendedTermStartAfter:
        'Pidennetyn lukukauden aloituspäivämäärä ei voi olla lukukauden aloituspäivämäärän jälkeen.',
      termBreaksOverlap: 'Päällekkäiset opetustauot eivät ole sallittua.'
    },
    modals: {
      editTerm: {
        title: 'Haluatko varmasti muokata tietoja?',
        text: 'Haluatko varmasti muokata jo alkanutta lukukautta?',
        resolve: 'Muokkaa',
        reject: 'Älä muokkaa'
      },
      deleteTerm: {
        title: 'Haluatko varmasti poistaa lukukauden?'
      }
    }
  },
  preferredFirstName: {
    popupLink: 'Kutsumanimi',
    title: 'Kutsumanimi',
    description:
      'Voit määritellä eVakassa käytössä olevan kutsumanimesi. Kutsumanimen tulee olla jokin etunimistäsi. Jos nimesi on vaihtunut ja sinulla on tarve päivittää eVakaan uusi nimesi, ole yhteydessä Espoon HelpDeskiin.',
    select: 'Valitse kutsumanimi',
    confirm: 'Vahvista'
  },
  metadata: {
    title: 'Arkistoitava metadata',
    notFound: 'Asiakirjalle ei ole arkistoitavaa metadataa',
    processNumber: 'Asianumero',
    organization: 'Organisaatio',
    archiveDurationMonths: 'Arkistointiaika',
    primaryDocument: 'Ensisijainen asiakirja',
    secondaryDocuments: 'Muut asiakirjat',
    name: 'Asiakirjan nimi',
    createdAt: 'Laatimisajankohta',
    createdBy: 'Laatija',
    monthsUnit: 'kuukautta',
    confidentiality: 'Julkisuus',
    confidential: 'Salassapidettävä',
    public: 'Julkinen',
    history: 'Prosessin historia',
    downloadPdf: 'Lataa PDF',
    states: {
      INITIAL: 'Asian vireillepano / -tulo',
      PREPARATION: 'Asian valmistelu',
      DECIDING: 'Päätöksenteko',
      COMPLETED: 'Toimeenpano / Päättäminen / Sulkeminen'
    }
  },
  systemNotifications: {
    title: {
      CITIZENS: 'Kuntalaisille näkyvä ilmoitus',
      EMPLOYEES: 'Henkilökunnalle näkyvä ilmoitus'
    },
    noNotification: 'Ei ilmoitusta tällä hetkellä',
    setNotification: 'Aseta ilmoitus',
    text: 'Teksti',
    validTo: 'Poistuu näkyvistä'
  },
  placementTool: {
    title: 'Optimointityökalu',
    description:
      'Voit luoda optimointityökalulla tuotetuista sijoitusehdotuksista hakemukset eVakaan. Hakemukset luodaan suoraan odottamaan päätöstä.'
  },
  components
}
