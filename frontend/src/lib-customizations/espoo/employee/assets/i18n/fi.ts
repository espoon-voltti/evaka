// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export const fi = {
  titles: {
    defaultTitle: 'Varhaiskasvatus',
    login: 'Kirjaudu sisään',
    ai: 'AI test',
    applications: 'Hakemukset',
    units: 'Yksiköt',
    customers: 'Asiakastiedot',
    placementDraft: 'Sijoitushahmotelma',
    decision: 'Päätöksen teko ja lähetys',
    feeDecisions: 'Maksupäätökset',
    feeDecision: 'Maksupäätös',
    feeDecisionDraft: 'Maksupäätösluonnos',
    incomeStatements: 'Tuloselvitykset',
    valueDecisions: 'Arvopäätökset',
    valueDecision: 'Arvopäätös',
    valueDecisionDraft: 'Arvopäätösluonnos',
    invoices: 'Laskut',
    invoice: 'Lasku',
    invoiceDraft: 'Laskuluonnos',
    reports: 'Raportit',
    messages: 'Viestit',
    caretakers: 'Henkilökunta',
    createUnit: 'Luo uusi yksikkö',
    personalMobileDevices: 'Johtajan eVaka-mobiili',
    employeePinCode: 'PIN-koodin hallinta',
    welcomePage: 'Tervetuloa eVakaan',
    vasuPage: 'Vasu 2021',
    vasuTemplates: 'Vasu-pohjat'
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
    statuses: {
      active: 'Aktiivinen',
      coming: 'Tulossa',
      completed: 'Päättynyt',
      conflict: 'Konflikti'
    },
    careTypeLabels: {
      club: 'Kerho',
      preschool: 'Esiopetus',
      daycare: 'Varhaiskasvatus',
      daycare5yo: 'Varhaiskasvatus',
      preparatory: 'Valmistava',
      'backup-care': 'Varasijoitus',
      temporary: 'Tilapäinen',
      'school-shift-care': 'Koululaisten vuorohoito'
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
    fileDownloadError: {
      modalHeader: 'Tiedoston käsittely on kesken',
      modalMessage:
        'Tiedosto ei ole juuri nyt avattavissa. Kokeile hetken kuluttua uudelleen.'
    },
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
      ]
    },
    nb: 'Huom',
    validTo: (date: string) => `Voimassa ${date} saakka`
  },
  header: {
    applications: 'Hakemukset',
    units: 'Yksiköt',
    search: 'Asiakastiedot',
    finance: 'Talous',
    invoices: 'Laskut',
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
    },
    failedModal: {
      header: 'Kirjautuminen epäonnistui',
      message:
        'Palveluun tunnistautuminen epäonnistui tai se keskeytettiin. Kirjautuaksesi sisään palaa takaisin ja yritä uudelleen.',
      returnMessage: 'Palaa takaisin'
    }
  },
  applications: {
    list: {
      title: 'Hakemukset',
      resultCount: 'Hakutuloksia',
      noResults: 'Ei hakutuloksia',
      type: 'Hakutyyppi',
      subtype: 'Osa/Koko',
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
      lessthan3: 'Alle 3-vuotias hoidontarpeen alkaessa',
      morethan3: 'Yli 3-vuotias hoidontarpeen alkaessa',
      currentUnit: 'Nyk.',
      addNote: 'Lisää muistiinpano',
      serviceWorkerNote: 'Palveluohjauksen huomio'
    },
    actions: {
      moveToWaitingPlacement: 'Siirrä sijoitettaviin',
      returnToSent: 'Palauta saapuneisiin',
      cancelApplication: 'Poista käsittelystä',
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
      DAYCARE: 'On ilmoittanut luopuvansa päivähoitopaikasta',
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
        RIGHT_TO_GET_NOTIFIED: 'Tiedonsaantioikeus',
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
      dailyTime: 'Päivittäinen hoitoaika',
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
      siblingBasisLabel: 'Sisarusperuste',
      siblingBasisValue: 'Haen paikkaa sisarusperusteella',
      siblingName: 'Sisaruksen nimi',
      siblingSsn: 'Sisaruksen henkilötunnus'
    },
    child: {
      title: 'Lapsen tiedot'
    },
    guardians: {
      title: 'Huoltajien tiedot',
      appliedGuardian: 'Hakeneen huoltajan tiedot',
      secondGuardian: {
        title: 'Ilmoitetun toisen huoltajan tiedot',
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
        DAYCARE: 'Varhaiskasvatuspäätös',
        DAYCARE_PART_TIME: 'Varhaiskasvatuspäätös (osapäiväinen)',
        PRESCHOOL: 'Esiopetuspäätös',
        PRESCHOOL_DAYCARE: 'Liittyvä varhaiskasvastuspäätös',
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
      error: {
        save: 'Muistiinpanon tallentaminen epäonnnistui',
        remove: 'Muistiinpanon poistaminen epäonnnistui'
      }
    }
  },
  childInformation: {
    title: 'Lapsen tiedot',
    restrictedDetails: 'Turvakielto',
    personDetails: {
      title: 'Henkilö-, yhteys- ja terveystiedot',
      name: 'Lapsen nimi',
      email: 'Sähköposti',
      socialSecurityNumber: 'Henkilötunnus',
      birthday: 'Syntymäaika',
      language: 'Kieli',
      address: 'Osoite',
      familyLink: 'Perheen tiedot'
    },
    familyContacts: {
      title: 'Perheen yhteystiedot ja varahakijat',
      contacts: 'Yhteystiedot',
      name: 'Nimi',
      role: 'Rooli',
      roles: {
        LOCAL_GUARDIAN: 'Huoltaja',
        LOCAL_ADULT: 'Aikuinen samassa taloudessa',
        LOCAL_SIBLING: 'Lapsi',
        REMOTE_GUARDIAN: 'Huoltaja'
      },
      contact: 'S-posti ja puhelin',
      contactPerson: 'Yhteyshlö',
      address: 'Osoite',
      backupPhone: 'Varanro'
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
          'Palveluntarve menee päällekäin toisen palveluntarpeen kanssa.',
        hardConflict:
          'Palveluntarve menee päällekäin toisen palveluntarpeen alkupäivämäärän kanssa.',
        checkHours: 'Tarkista',
        placementMismatchWarning:
          'Viikottainen palveluntarve ei vastaa sijoituksen toimintamuotoa.',
        autoCutWarning:
          'Aiemmat päällekkäiset palveluntarpeet katkaistaan automaattisesti.'
      },
      services: {
        partDay: 'Osapäiväinen varhaiskasvatus (max. 5 h / päivä)',
        partDayShort: 'Osapäiväinen varhaiskasvatus',
        partWeek: 'Osaviikkoinen varhaiskasvatus (alle 5 arkipäivää / viikko)',
        partWeekShort: 'Osaviikkoinen varhaiskasvatus',
        shiftCare:
          'Ilta- ja vuorohoito (kello 18-06 arkisin ja / tai viikonloppuisin)',
        shiftCareShort: 'Ilta- ja vuorohoito',
        preparatoryEducation: 'Valmistava opetus'
      }
    },
    dailyServiceTimes: {
      title: 'Päivittäinen varhaiskasvatusaika',
      info: 'Kirjoita tähän varhaiskasvatussopimuksella ilmoitettu päivittäinen varhaiskasvatusaika, sisältäen esiopetuksen / valmistavan opetuksen / 5-vuotiaiden maksuttoman varhaiskasvatuksen.',
      info2:
        'Epäsäännölliset ja säännölliset poissaolot merkitään päiväkirjalle.',
      types: {
        notSet: 'Ei asetettu',
        regular: 'Säännöllinen varhaiskasvatusaika',
        irregular: 'Epäsäännöllinen varhaiskasvatusaika',
        variableTimes: 'Vaihteleva varhaiskasvatusaika'
      },
      weekdays: {
        monday: 'Maanantai',
        tuesday: 'Tiistai',
        wednesday: 'Keskiviikko',
        thursday: 'Torstai',
        friday: 'Perjantai',
        saturday: 'Lauantai',
        sunday: 'Sunnuntai'
      },
      errors: {
        required: 'Pakollinen tieto'
      }
    },
    assistance: {
      title: 'Tuen tarve ja tukitoimet'
    },
    assistanceNeed: {
      title: 'Tuen tarve',
      fields: {
        dateRange: 'Tuen tarve ajalle',
        capacityFactor: 'Lapsen tuen kerroin',
        capacityFactorInfo:
          'Kapasiteetti määräytyy yleensä lapsen iän ja palvelun tarpeen mukaan. Mikäli lapsella on sellainen tuen tarve, joka lisää kapasiteettia, lisätään tuen tarpeen kerroin tähän. Tuen tarpeen ja kertoimen lisää varhaiserityiskasvatuksen koordinaattori.',
        bases: 'Perusteet'
      },
      create: 'Luo uusi tuen tarpeen ajanjakso',
      removeConfirmation: 'Haluatko poistaa tuen tarpeen?',
      errors: {
        invalidCoefficient: 'Virheellinen kerroin.',
        conflict: 'Tuen tarve menee päällekäin toisen tuen tarpeen kanssa.',
        hardConflict:
          'Tuen tarve menee päällekäin toisen tuen tarpeen alkupäivämäärän kanssa.',
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
        measures: 'Toimenpiteet',
        measureTypes: {
          SPECIAL_ASSISTANCE_DECISION: 'Erityisen tuen päätös\n',
          SPECIAL_ASSISTANCE_DECISION_INFO:
            'Lapsella on pidennetty oppivelvollisuus.',
          INTENSIFIED_ASSISTANCE: 'Tehostettu tuki',
          INTENSIFIED_ASSISTANCE_INFO:
            'Lapsella on avustamispalvelu tai lapsi on pedagogisesti vahvistetussa ryhmässä. Koskee myös osaa laaja-alaisen veon tukea saavista lapsista. ',
          EXTENDED_COMPULSORY_EDUCATION: 'Pidennetty oppivelvollisuus',
          EXTENDED_COMPULSORY_EDUCATION_INFO:
            'Päätös tehdään perusopetuksessa.',
          CHILD_SERVICE: 'Lastensuojelu',
          CHILD_SERVICE_INFO:
            'Merkitään lapselle, jolla varhaiskasvatus on lastensuojelun avohuollon tukitoimena perhe- ja sosiaalipalvelujen päätöksellä.',
          CHILD_ACCULTURATION_SUPPORT: 'Lapsen kotoutumisen tuki (ELY)',
          CHILD_ACCULTURATION_SUPPORT_INFO:
            'Lapsen tuen tarpeen perusteella Espoolle myönnetty korvaus (ELY)',
          TRANSPORT_BENEFIT: 'Kuljetusetu',
          TRANSPORT_BENEFIT_INFO: 'Kuljetusetu'
        },
        otherActionPlaceholder:
          'Voit kirjoittaa tähän lisätietoa muista tukitoimista.'
      },
      create: 'Luo uusi tukitoimien ajanjakso',
      removeConfirmation: 'Haluatko poistaa tukitoimien ajanjakson?',
      errors: {
        conflict: 'Tukitoimet menevät päällekäin toisen ajanjakson kanssa.',
        hardConflict:
          'Tukitoimet menevät päällekäin toisen ajanjakson alkupäivämäärän kanssa.',
        autoCutWarning:
          'Aiemmat päällekkäiset tukitoimet katkaistaan automaattisesti.'
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
      }
    },
    placements: {
      title: 'Sijoitukset ja palveluntarpeet',
      placements: 'Sijoitukset',
      restrictedName: '(Yksikön nimi piilotettu)',
      rowTitle: 'Sijoituspäätös voimassa',
      startDate: 'Aloituspäivämäärä',
      endDate: 'Päättymispäivämäärä',
      terminatedByGuardian: 'Huoltaja irtisanonut',
      terminated: 'Irtisanottu',
      area: 'Alue',
      daycareUnit: 'Toimipaikka',
      daycareGroup: 'Nykyinen ryhmä',
      daycareGroupMissing: 'Ei ryhmitetty',
      type: 'Toimintamuoto',
      providerType: 'Järjestämismuoto',
      serviceNeedMissing1: 'Sijoitukselta puuttuu palveluntarve',
      serviceNeedMissing2:
        'päivältä. Merkitse palveluntarve koko sijoituksen ajalle.',
      serviceNeedMissingTooltip1: 'Palveluntarve puuttuu',
      serviceNeedMissingTooltip2: 'päivältä.',
      deletePlacement: {
        btn: 'Poista sijoitus',
        confirmTitle: 'Haluatko varmasti perua tämän sijoituksen?'
      },
      createPlacement: {
        btn: 'Luo uusi sijoitus',
        title: 'Uusi sijoitus',
        text: 'Tästä sijoituksesta ei voi lähettää päätöstä. Jos sijoitus menee päällekäin lapsen aiemmin luotujen sijoituksien kanssa, näitä sijoituksia lyhennetään tai ne poistetaan automaattisesti.',
        temporaryDaycareWarning: 'HUOM! Älä käytä varasijoitusta tehdessäsi!',
        unitMissing: 'Yksikkö puuttuu'
      },
      error: {
        conflict: {
          title: 'Päivämäärää ei voitu muokata',
          text:
            'Lapsella on sijoitus, joka menee päällekäin' +
            ' nyt ilmoittamiesi päivämäärien kanssa. Voit palata muokkaamaan' +
            ' ilmoittamiasi päivämääriä tai ottaa yhteyttä pääkäyttäjään.'
        }
      },
      warning: {
        overlap: 'Ajalle on jo sijoitus',
        ghostUnit: 'Yksikkö on merkitty haamuyksiköksi'
      },
      serviceNeeds: {
        title: 'Sijoituksen palveluntarpeet',
        period: 'Aikaväli',
        description: 'Kuvaus',
        shiftCare: 'Ilta/Vuoro',
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
        }
      }
    },
    fridgeParents: {
      title: 'Päämiehet',
      name: 'Nimi',
      ssn: 'Hetu',
      startDate: 'Alkaen',
      endDate: 'Päättyen',
      status: 'Tila'
    },
    messaging: {
      title: 'Lapseen liittyvä viestintä (vastaanottajat)',
      info: 'Lapseen liittyvät viestit lähetetään merkityille huoltajille. Esimies tai palveluohjaus voi perustelluista syistä estää viestin lähettämisen valitulle huoltajalle, poistamalla ruksin kyseisen henkilön kohdalta. Viestejä ei lähetetä päämiehille.',
      name: 'Vastaanottajan nimi',
      notBlocklisted: 'Saa vastaanottaa'
    },
    backupCares: {
      title: 'Varasijoitukset',
      remove: 'Haluatko poistaa varasijoituksen?',
      editing: 'muokkauksessa',
      create: 'Luo uusi varasijoitus',
      dateRange: 'Varasijoitus ajalle',
      unit: 'Yksikkö'
    },
    backupPickups: {
      title: 'Varahakijat',
      name: 'Varahakijan nimi',
      phone: 'Puhelinnumero',
      add: 'Lisää varahakija',
      edit: 'Muokkaa varahakijan tietoja',
      removeConfirmation: 'Haluatko varmasti poistaa varahakijan?'
    },
    vasu: {
      title: 'Varhaiskasvatussuunnitelma ja esiopetuksen oppimissuunnitelma',
      createNew: 'Luo uusi suunnitelma',
      modified: 'Muokattu',
      published: 'Julkaistu',
      init: {
        chooseTemplate: 'Valitse pohja',
        noTemplates: 'Yhtään pohjaa ei löytynyt',
        error: 'Suunnitelman luonti epäonnistui'
      }
    },
    pedagogicalDocument: {
      title: 'Pedagoginen dokumentointi',
      date: 'Päivämäärä',
      document: 'Dokumentti',
      description: 'Pedagoginen kuvaus',
      create: 'Lisää uusi',
      removeConfirmation: 'Haluatko poistaa dokumentin?',
      removeConfirmationText:
        'Haluatko varmasti poistaa pedagogisen dokumentin ja sen kuvaustekstin? Poistoa ei saa peruutettua, ja dokumentti poistetaan näkyvistä myös huoltajalta.'
    }
  },
  vasu: {
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
    transitions: {
      guardiansWillBeNotified:
        'Huoltajalle/huoltajille lähetetään sähköpostiin viesti, että eVakaan on julkaistu uusi dokumentti.',
      vasuIsPublishedToGuardians: 'Suunnitelma julkaistaan huoltajille',
      PUBLISHED: {
        buttonText: 'Julkaise suunnitelma',
        confirmTitle: 'Haluatko julkaista suunnitelman vaihtamatta tilaa?',
        confirmAction: 'Julkaise',
        successTitle: 'Suunnitelma on julkaistu huoltajalle!',
        successText:
          'Mikäli muokkaat suunnitelmaa myöhemmin, voit julkaista sen uudelleen. Tällöin vanha versio päivittyy automaattisesti ja huoltajalle/huoltajille lähetetään uusi viesti.'
      },
      MOVED_TO_READY: {
        buttonText: 'Julkaise Laadittu-tilassa',
        confirmTitle: 'Haluatko julkaista laaditun suunnitelman?',
        confirmAction: 'Julkaise suunnitelma',
        successTitle: 'Laadittu suunnitelma on julkaistu huoltajalle!',
        successText:
          'Mikäli muokkaat laadittua suunnitelmaa myöhemmin, voit julkaista sen uudelleen. Tällöin vanha versio päivittyy automaattisesti ja huoltajalle/huoltajille lähetetään uusi viesti.'
      },
      MOVED_TO_REVIEWED: {
        buttonText: 'Julkaise Arvioitu-tilassa',
        confirmTitle: 'Haluatko julkaista arvioinnin?',
        confirmAction: 'Julkaise arviointi',
        successTitle: 'Arviointi on julkaistu huoltajalle!',
        successText:
          'Mikäli muokkaat arvioitua suunnitelmaa myöhemmin, voit julkaista sen uudelleen. Tällöin vanha versio päivittyy automaattisesti ja huoltajalle/huoltajille lähetetään uusi viesti.'
      },
      MOVED_TO_CLOSED: {
        buttonText: 'Merkitse päättyneeksi',
        confirmTitle: 'Haluatko siirtää suunnitelman Päättynyt-tilaan?',
        confirmAction: 'Siirrä',
        successTitle: 'Suunnitelma on merkitty päättyneeksi.',
        successText: ''
      },
      RETURNED_TO_READY: {
        buttonText: 'Palauta laadituksi',
        confirmTitle: 'Haluatko palauttaa suunnitelman Laadittu-tilaan?',
        confirmAction: 'Siirrä',
        successTitle: 'Suunnitelma on palautettu Laadittu-tilaan',
        successText: ''
      },
      RETURNED_TO_REVIEWED: {
        buttonText: 'Palauta arvioiduksi',
        confirmTitle: 'Haluatko palauttaa suunnitelman Arvioitu-tilaan?',
        confirmAction: 'Siirrä',
        successTitle: 'Suunnitelma on palautettu Arvioitu-tilaan',
        successText: ''
      }
    },
    state: 'Suunnitelman tila',
    events: {
      DAYCARE: 'Varhaiskasvatussuunnitelman tapahtumat',
      PRESCHOOL: 'Lapsen esiopetuksen oppimissuunnitelman tapahtumat'
    },
    noRecord: 'Ei merkintää',
    checkInPreview: 'Tarkista esikatselussa'
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
    title: 'Aikuisen tiedot',
    restrictedDetails: 'Turvakielto',
    personDetails: 'Henkilö- ja yhteystiedot',
    addSsn: 'Aseta hetu',
    noSsn: 'Hetuton',
    ssnAddingDisabledCheckbox:
      'Vain pääkäyttäjillä on oikeus asettaa lapselle henkilötunnus',
    ssnAddingDisabledInfo:
      'Palveluohjauksen ja talouden käyttäjät eivät saa asetettua lapselle henkilötunnusta. Kun henkilötunnus puuttuu, lapsella ei ole huoltajasuhdetta. Jos henkilötunnus halutaan myöhemmin asettaa, lapsen aiemmat dokumentit on poistettava järjestelmästä.',
    ssnInvalid: 'Epäkelpo henkilötunnus',
    ssnConflict: 'Tämä käyttäjä löytyy jo järjestelmästä.',
    partner: 'Puolisot',
    partnerInfo:
      'Samassa osoitteessa avio/avoliiton omaisessa suhteessa asuva toinen henkilö',
    partnerAdd: 'Lisää puoliso',
    forceManualFeeDecisionsLabel: 'Maksupäätösten lähettäminen',
    forceManualFeeDecisionsChecked: 'Lähetetään aina manuaalisesti',
    forceManualFeeDecisionsUnchecked: 'Automaattisesti, jos mahdollista',
    fridgeChildOfHead: 'Päämiehen alaiset alle 18v lapset',
    fridgeChildAdd: 'Lisää lapsi',
    applications: 'Hakemukset',
    feeDecisions: {
      title: 'Päämiehen maksupäätökset',
      createRetroactive: 'Luo takautuvia maksupäätösluonnoksia'
    },
    invoices: 'Päämiehen laskut',
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
      incomeTotalLabel: 'Perheen tulot yhteensä',
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
        miscTitle: 'Päämiehen tiedot',
        incomeTitle: 'Päämiehen tulot',
        income: 'Tulot',
        expensesTitle: 'Päämiehen menot',
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
      }
    },
    incomeStatement: {
      title: 'Tuloselvitykset',
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
    }
  },
  incomeStatement: {
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
      INCOME: 'Huoltajan toimittamat tulotiedot'
    },
    table: {
      title: 'Käsittelyä odottavat tuloselvitykset',
      customer: 'Asiakas',
      area: 'Alue',
      created: 'Luotu',
      startDate: 'Voimassa',
      type: 'Tyyppi'
    },
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
      staff: 'Yksikön henkilökunta',
      email: 'Sähköpostiosoite',
      removeConfirmation:
        'Haluatko poistaa pääsyoikeuden valitulta henkilöltä?',
      addPerson: 'Lisää henkilö',
      choosePerson: 'Valitse henkilö',
      mobileDevices: {
        mobileDevices: 'Yksikön mobiililaitteet',
        addMobileDevice: 'Lisää mobiililaite',
        editName: 'Muokkaa laitteen nimeä',
        removeConfirmation: 'Haluatko poistaa mobiililaitteen?',
        editPlaceholder: 'esim. Hippiäisten kännykkä'
      },
      groups: 'Luvitukset ryhmiin',
      noGroups: 'Ei luvituksia'
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
      noValidValuesRealized: 'Aikavälille ei voitu laskea käyttöastetta'
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
      status: 'Tila'
    },
    placements: {
      title: 'Ryhmää odottavat lapset',
      name: 'Nimi',
      birthday: 'Syntymäaika',
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
      title: 'Varaukset ja läsnäolot',
      noGroup: 'Ei ryhmää',
      modes: {
        week: 'Viikko',
        month: 'Kuukausi'
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
        info: 'Ryhmän aikaisempia tietoja ei säilytetä'
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
      }
    },
    error: {
      placement: {
        create: 'Sijoitus ryhmään epäonnistui',
        transfer: 'Sijoitus toiseen ryhmään epäonnistui'
      }
    }
  },
  groupCaretakers: {
    title: 'Henkilökunnan tarve ryhmässä',
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
    title: 'Johtajan eVaka-mobiili',
    infoParagraph1:
      'Tällä sivulla voit yksikön johtajana/varajohtajana määrittää itsellesi omaan henkilökohtaiseen käyttöösi mobiililaitteen, jolla tarkastelet kaikkien yksiköidesi tietoja  eVakassa. Voit myös tarvittaessa poistaa tai lisätä useamman laitteen.',
    infoParagraph2:
      'Huolehdithan, että kaikissa mobiililaitteissasi on pääsykoodi käytössä.',
    name: 'Laitteen nimi',
    addDevice: 'Lisää mobiililaite',
    editName: 'Muokkaa laitteen nimeä',
    deleteDevice: 'Haluatko poistaa mobiililaitteen?'
  },
  mobilePairingModal: {
    sharedDeviceModalTitle: 'Lisää yksikköön uusi mobiililaite',
    personalDeviceModalTitle: 'Lisää uusi johtajan mobiililaite',
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
        'Muista nostaa aiemmin siirretyt laskut Communityyn ennen uusien siirtämistä.'
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
        agreementType: 'Laskulaji'
      },
      rows: {
        title: 'Laskurivit',
        product: 'Tuote',
        description: 'Selite',
        costCenter: 'KP',
        subCostCenter: 'ALAKP',
        daterange: 'Ajanjakso',
        amount: 'Kpl',
        unitPrice: 'A-hinta',
        price: 'Summa',
        subtotal: 'Laskun summa',
        addRow: 'Lisää laskurivi'
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
      sentAt: 'Lähetetty'
    },
    buttons: {
      checked: (count: number) =>
        count === 1
          ? `${count} maksupäätös valittu`
          : `${count} maksupäätöstä valittu`,
      createDecision: (count: number) =>
        count === 1 ? 'Luo maksupäätös' : 'Luo maksupäätökset',
      markSent: 'Merkitse postitetuksi',
      close: 'Sulje tallentamatta',
      save: 'Tallenna muutokset',
      errors: {
        WAITING_FOR_MANUAL_SENDING:
          'Osalla päämiehistä on päätöksiä, jotka odottavat manuaalista lähetystä'
      }
    }
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
      sentAt: 'Lähetetty'
    },
    buttons: {
      checked: (count: number) =>
        count === 1
          ? `${count} arvopäätös valittu`
          : `${count} arvopäätöstä valittu`,
      createDecision: (count: number) =>
        count === 1 ? 'Luo arvopäätös' : 'Luo arvopäätökset',
      markSent: 'Merkitse postitetuksi',
      close: 'Sulje tallentamatta',
      save: 'Tallenna muutokset',
      errors: {
        WAITING_FOR_MANUAL_SENDING:
          'Osalla lapsista on päätöksiä, jotka odottavat manuaalista lähetystä'
      }
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
      PRESCHOOL_DAYCARE: 'Esiopetus ja liittyvä varhaiskasvatus',
      TEMPORARY_DAYCARE: 'Tilapäinen kokopäiväinen varhaiskasvatus',
      TEMPORARY_DAYCARE_PART_DAY: 'Tilapäinen osapäiväinen varhaiskasvatus',
      SCHOOL_SHIFT_CARE: 'Koululaisten vuorohoito'
    }
  },
  product: {
    DAYCARE: 'Varhaiskasvatus',
    DAYCARE_DISCOUNT: 'Alennus (maksup.)',
    DAYCARE_INCREASE: 'Korotus (maksup.)',
    PRESCHOOL_WITH_DAYCARE: 'Varhaiskasvatus + Esiopetus',
    SCHOOL_SHIFT_CARE: 'Koululaisten vuorohoito',
    PRESCHOOL_WITH_DAYCARE_DISCOUNT: 'Alennus (maksup.)',
    PRESCHOOL_WITH_DAYCARE_INCREASE: 'Korotus (maksup.)',
    TEMPORARY_CARE: 'Tilapäinen varhaiskasvatus',
    SICK_LEAVE_100: 'Laskuun vaikuttava poissaolo 100%',
    SICK_LEAVE_50: 'Laskuun vaikuttava poissaolo 50%',
    ABSENCE: 'Poissaolovähennys',
    EU_CHEMICAL_AGENCY: 'Kemikaaliviraston varhaiskasvatuksen laskut',
    OTHER_MUNICIPALITY: 'Ulkopaikkakuntalaisten lasten varhaiskasvatus',
    FREE_OF_CHARGE: 'Poissaolovähennys'
  },
  feeAlteration: {
    DISCOUNT: 'Alennus',
    INCREASE: 'Korotus',
    RELIEF: 'Huojennus'
  },
  feeDecision: {
    title: {
      DRAFT: 'Maksupäätösluonnos',
      WAITING_FOR_SENDING: 'Maksupäätös (lähdössä)',
      WAITING_FOR_MANUAL_SENDING: 'Maksupäätös (lähetetään manuaalisesti)',
      SENT: 'Maksupäätös',
      ANNULLED: 'Mitätöity maksupäätös'
    },
    distinctiveDetails: {
      UNCONFIRMED_HOURS: 'Palveluntarve vahvistamatta',
      EXTERNAL_CHILD: 'Ulkopaikkakuntalainen',
      RETROACTIVE: 'Takautuva maksupäätös'
    },
    status: {
      DRAFT: 'Luonnos',
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
    status: 'Tila',
    clear: 'Tyhjennä valinnat',
    validityPeriod: 'Voimassaoloaika',
    searchByStartDate: 'Alkupäivä sijoittuu valitulle aikavälille',
    invoiceDate: 'Laskun päiväys',
    invoiceSearchByStartDate: 'Lähetä laskut valitulta kaudelta'
  },
  valueDecision: {
    title: {
      DRAFT: 'Arvopäätösluonnos',
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
      capacityFactor: 'tuen tarpeen kerroin',
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
      OTHER_ABSENCE: 'Muu poissaolo',
      SICKLEAVE: 'Sairaus',
      UNKNOWN_ABSENCE: 'Ilmoittamaton poissaolo',
      PLANNED_ABSENCE: 'Suunniteltu poissaolo / vuorohoito',
      TEMPORARY_RELOCATION: 'Lapsi varasijoitettuna muualla',
      TEMPORARY_VISITOR: 'Varalapsi läsnä',
      PARENTLEAVE: 'Isyysvapaa',
      FORCE_MAJEURE: 'Maksuton päivä',
      NO_ABSENCE: 'Ei poissaoloa'
    },
    careTypes: {
      SCHOOL_SHIFT_CARE: 'Koululaisten vuorohoito',
      PRESCHOOL: 'Esiopetus',
      PRESCHOOL_DAYCARE: 'Liittyvä varhaiskasvatus',
      DAYCARE_5YO_FREE: '5-vuotiaiden varhaiskasvatus',
      DAYCARE: 'Varhaiskasvatus',
      CLUB: 'Kerho'
    },
    careTypeCategories: {
      NONBILLABLE:
        'Esiopetus, valmistava, 5-vuotiaiden varhaiskasvatus tai kerhotoiminta',
      BILLABLE: 'Varhaiskasvatus (maksullinen)'
    },
    modifiedByType: {
      CITIZEN: 'Huoltaja',
      EMPLOYEE: 'Henkilökunta',
      SYSTEM: '?',
      MOBILE_DEVICE: '?',
      UNKNOWN: '?'
    },
    modal: {
      absenceSectionLabel: 'Poissaolon syy',
      placementSectionLabel: 'Toimintamuoto, jota poissaolo koskee',
      saveButton: 'Tallenna',
      cancelButton: 'Peruuta',
      absenceTypes: {
        OTHER_ABSENCE: 'Muu poissaolo',
        SICKLEAVE: 'Sairaus',
        UNKNOWN_ABSENCE: 'Ilmoittamaton poissaolo',
        PLANNED_ABSENCE: 'Suunniteltu poissaolo / vuorotyö',
        TEMPORARY_RELOCATION: 'Varasijoitettuna muualla',
        TEMPORARY_VISITOR: 'Varalapsi läsnä',
        PARENTLEAVE: 'Isyysvapaa',
        FORCE_MAJEURE: 'Maksuton päivä (rajoitettu käyttö)',
        NO_ABSENCE: 'Ei poissaoloa'
      },
      free: 'Maksuton',
      paid: 'Maksullinen',
      absenceSummaryTitle: 'Lapsen poissaolokooste'
    },
    table: {
      nameCol: 'Nimi',
      dobCol: 'Synt.aika',
      staffRow: 'Henkilökuntaa paikalla',
      disabledStaffCellTooltip: 'Ryhmä ei ole olemassa valittuna päivänä'
    },
    legendTitle: 'Merkintöjen selitykset',
    addAbsencesButton(numOfSelected: number) {
      return numOfSelected === 1
        ? 'Lisää merkintä valitulle...'
        : 'Lisää merkinnät valituille...'
    },
    notOperationDay: 'Ei toimintapäivä',
    absence: 'Poissaolo'
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
      'Aiemmat päällekäiset sijoitukset katkaistaan automaattisesti mikäli kuntalainen ottaa tarjottavan paikan vastaan.',
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
      total: 'Yhteensä',
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
      period: 'Ajanjakso',
      date: 'Päivämäärä',
      startDate: 'Alkaen',
      endDate: 'Päättyen',
      childName: 'Lapsen nimi'
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
        'Tarkista ettei henkilöillä ole esimerkiksi päällekäisiä sijoituksia, palveluntarpeita tai muita päällekkäisyyksiä, jotka voisivat estää yhdistämisen.',
      columns: {
        'absence.child_id': 'Poissa- oloja',
        'absence.modified_by_guardian_id': 'Itse merkittyjä poissa -oloja',
        'application.child_id': 'Hakemuksia (lapsena)',
        'application.guardian_id': 'Hakemuksia (huoltajana)',
        'application.other_guardian_id': 'Hakemuksia (toisena huoltajana)',
        'assistance_action.child_id': 'Tuki- toimia',
        'assistance_need.child_id': 'Tuen tarpeita',
        'attachment.uploaded_by_person': 'Liitteitä',
        'attendance_reservation.child_id': 'Läsnäolo -varauksia',
        'attendance_reservation.created_by_guardian_id':
          'Itse merkittyjä läsnäolo -varauksia',
        'backup_care.child_id': 'Vara- sijoituksia',
        'backup_pickup.child_id': 'Vara- hakijoita',
        'child_attendance.child_id': 'Läsnäoloja',
        'child_images.child_id': 'Kuvia',
        'curriculum_document.child_id': 'Opetussuunnitelemia',
        'daily_service_time.child_id': 'Varhais- kasvatus- aikoja',
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
        'income.person_id': 'Tulo- tietoja',
        'income_statement.person_id': 'Tulo -ilmoituksia',
        'invoice.codebtor': 'Laskuja (kanssa -velallinen)',
        'invoice.head_of_family': 'Laskuja',
        'invoice_row.child': 'Lasku- rivejä',
        'koski_study_right.child_id': 'Koski opinto- oikeuksia',
        'messaging_blocklist.blocked_recipient': 'Estettynä viestin saajana',
        'messaging_blocklist.child_id': 'Estettyjä viestin saajia',
        'pedagogical_document.child_id': 'Pedagogisia dokumentteja',
        'placement.child_id': 'Sijoituksia',
        'varda_child.person_id': 'Varda lapsi',
        'varda_service_need.evaka_child_id': 'Varda palvelun -tarpeita',
        'vasu_document.child_id': 'Vasuja',
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
    endedPlacements: {
      title: 'Varhaiskasvatuksessa lopettavat lapset',
      description:
        'Kelaan toimitettava raportti varhaiskasvatuksessa lopettavista ja mahdollisesti myöhemmin jatkavista lapsista.',
      ssn: 'Hetu',
      placementEnd: 'Lopettaa varhaiskasvatuksessa',
      nextPlacementStart: 'Jatkaa varhaiskasvatuksessa'
    },
    missingHeadOfFamily: {
      title: 'Puuttuvat päämiehet',
      description:
        'Raportti listaa lapset, joiden nykyisen sijoituksen ajalta puuttuu tieto päämiehestä.',
      daysWithoutHeadOfFamily: 'Puutteellisia päiviä'
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
      basisMissing: 'Peruste puuttuu',
      actionMissing: 'Tukitoimi puuttuu'
    },
    occupancies: {
      title: 'Täyttö- ja käyttöasteet',
      description:
        'Raportti tarjoaa tiedot yhden palvelualueen ja yhden kuukauden käyttö- tai täyttöasteista.',
      filters: {
        areaPlaceholder: 'Valitse palvelualue',
        type: 'Tyyppi',
        types: {
          UNIT_CONFIRMED: 'Vahvistettu täyttöaste yksikössä',
          UNIT_PLANNED: 'Suunniteltu täyttöaste yksikössä',
          UNIT_REALIZED: 'Käyttöaste yksikössä',
          GROUP_CONFIRMED: 'Vahvistettu täyttöaste ryhmissä',
          GROUP_REALIZED: 'Käyttöaste ryhmissä'
        },
        valueOnReport: 'Näytä tiedot',
        valuesOnReport: {
          percentage: 'Prosentteina',
          headcount: 'Lukumääränä',
          raw: 'Summa ja kasvattajamäärä erikseen'
        }
      },
      average: 'Keskiarvo',
      missingCaretakersLegend: 'kasvattajien lukumäärä puuttuu'
    },
    invoices: {
      title: 'Laskujen täsmäytys',
      description:
        'Laskujen täsmäytysraportti Community-järjestelmään vertailua varten',
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
      serviceVoucherCoPayment: 'Omavastuu',
      serviceNeed: 'Palveluntarve',
      capacityFactor: 'Tuen tarve',
      partTime: 'Osa/Koko',
      under3: 'Alle 3-vuotias',
      atLeast3: 'Vähintään 3-vuotias',
      type: {
        NEW: 'Uusi päätös',
        REFUND: 'Hyvitys',
        CORRECTION: 'Korjaus'
      }
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
      tel: 'Puhelu',
      email: 'email',
      dob: 'Syntymäaika',
      assistanceNeed: 'Tuen tarve',
      preparatory: 'Valmistava',
      siblingBasis: 'sisarusperuste',
      connected: 'Liittyvä',
      preferredStartDate: 'Toivottu aloituspäivä',
      sentDate: 'Lähetyspäivä',
      otherPreferredUnits: 'Muut hakutoiveet'
    },
    vardaErrors: {
      title: 'Varda-virheet',
      vardaResetButton: 'Käynnistä lasten resetointi',
      description:
        'Varda-päivityksissä tapahtuneet virheet annetusta ajanhetkestä eteenpäin',
      updated: 'Muokattu',
      age: 'Ikä (päivää)',
      child: 'Lapsi',
      serviceNeed: 'Palveluntarve',
      error: 'Virhe',
      childLastReset: 'Resetoitu viimeksi',
      childMarkedForRest: 'Lapsen tiedot nollataan seuraavalla ajolla',
      resetChild: 'Resetoi',
      updating: 'Päivittää'
    }
  },
  unitEditor: {
    submitNew: 'Luo yksikkö',
    title: {
      contact: 'Yksikön yhteystiedot',
      unitManager: 'Varhaiskasvatusyksikön johtajan yhteystiedot',
      decisionCustomization:
        'Yksikön nimi päätöksellä ja ilmoitus paikan vastaanottamisesta'
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
      roundTheClock: 'Ilta- ja vuorohoito',
      capacity: 'Yksikön laskennallinen lapsimäärä',
      language: 'Yksikön kieli',
      ghostUnit: 'Haamuyksikkö',
      integrations: 'Integraatiot',
      invoicedByMunicipality: 'Laskutetaan eVakasta',
      ophUnitOid: 'Toimipaikan OID',
      ophOrganizerOid: 'Järjestäjän OID',
      costCenter: 'Kustannuspaikka',
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
      }
    },
    field: {
      applyPeriod: 'Kun toivottu alkamispäivä aikavälillä',
      canApplyDaycare: 'Varhaiskasvatushaussa',
      canApplyPreschool: 'Esiopetushaussa',
      canApplyClub: 'Kerhohaussa',
      roundTheClock: 'Yksikkö tarjoaa ilta- ja vuorohoitoa',
      capacity: 'henkilöä',
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
      openingDateIsAfterClosingDate: 'Aloituspäivä on päättymispäivän jälkeen'
    },
    warning: {
      placementsShouldBeEndedIfUnitIsClosed:
        'Huomioithan, että kaikki sijoitukset tulee päättää yksikön päättymispäivään mennessä, mukaan lukien myös mahdolliset tulevaisuuden sijoitukset.',
      onlyMunicipalUnitsShouldBeSentToVarda:
        'Älä lähetä Vardaan muiden kuin kunnallisten ja kunnallisten ostopalveluyksiköiden tietoja.',
      handlerAddressIsMandatory:
        'Ilmoituksen vastaanottajan osoite on pakollinen, jos yksikön järjestämismuodoksi on valittu kunnallinen, ostopalvelu tai palveluseteli.'
    }
  },
  fileUpload: {
    upload: {
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
    download: {
      modalHeader: 'Tiedoston käsittely on kesken',
      modalMessage:
        'Tiedosto ei ole juuri nyt avattavissa. Kokeile hetken kuluttua uudelleen.'
    }
  },
  messages: {
    inboxTitle: 'Viestit',
    emptyInbox: 'Tämä kansio on tyhjä',
    replyToThread: 'Vastaa viestiin',
    unitList: {
      title: 'Yksiköt'
    },
    sidePanel: {
      ownMessages: 'Omat viestit',
      groupsMessages: 'Ryhmien viestit',
      noAccountAccess:
        'Viestejä ei voi näyttää, koska sinua ei ole luvitettu ryhmään. Pyydä lupa esimieheltäsi.'
    },
    messageBoxes: {
      names: {
        RECEIVED: 'Saapuneet',
        SENT: 'Lähetetyt',
        DRAFTS: 'Luonnokset'
      },
      receivers: 'Vastaanottajat',
      newMessage: 'Uusi viesti'
    },
    messageList: {
      titles: {
        RECEIVED: 'Saapuneet viestit',
        SENT: 'Lähetetyt viestit',
        DRAFTS: 'Luonnokset'
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
    messageEditor: {
      newMessage: (unitName: string) => `Uusi viesti (${unitName})`,
      to: {
        label: 'Vastaanottaja',
        placeholder: 'Valitse ryhmä',
        noOptions: 'Ei ryhmiä'
      },
      type: {
        label: 'Viestin tyyppi',
        message: 'Viesti',
        bulletin: 'Tiedote (vastaanottaja ei voi vastata)'
      },
      sender: 'Lähettäjä',
      receivers: 'Vastaanottajat',
      title: 'Otsikko',
      message: 'Viesti',
      discard: 'Hylkää',
      deleteDraft: 'Hylkää luonnos',
      send: 'Lähetä',
      sending: 'Lähetetään'
    },
    noTitle: 'Ei otsikkoa',
    notSent: 'Ei lähetetty',
    editDraft: 'Muokkaa luonnosta'
  },
  pinCode: {
    title: 'eVaka-mobiilin PIN-koodi',
    title2: 'Aseta PIN-koodi',
    text1:
      'Tällä sivulla voit asettaa oman henkilökohtaisen PIN-koodisi Espoon varhaiskasvatuksen mobiilisovellusta varten. PIN-koodia käytetään eVaka-mobiilissa lukon',
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
    title: 'Käyttäjät',
    name: 'Nimi',
    rights: 'Oikeudet',
    findByName: 'Etsi nimellä',
    editor: {
      roles: 'Roolit'
    }
  },
  financeBasics: {
    title: 'Talouden maksuasetukset',
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
      errors: {
        'date-overlap':
          'Maksuasetukset menevät päällekkäin jonkin muun voimassaolevan asetuksen kanssa. Päivitä muiden maksuasetusten voimassaoloaika ensin.'
      },
      modals: {
        editRetroactive: {
          title: 'Haluatko varmasti muokata tietoja?',
          text: 'Haluatko varmasti muokata jo käytössä olevia maksutietoja? Mikäli muokkaat tietoja, kaikille asiakkaille luodaan takautuva maksupäätös.',
          resolve: 'Muokkaa',
          reject: 'Älä muokkaa'
        },
        saveRetroactive: {
          title: 'Haluatko tallentaa maksuasetukset takautuvasti?',
          text: 'Olet tallentamassa maksuasetuksia, jotka vaikuttavat takautuvasti. Mikäli tallennat tiedot, kaikille asiakkaille luodaan uusi takautuva maksupäätös. Haluatko varmasti tallentaa?',
          resolve: 'Tallenna',
          reject: 'Peruuta'
        }
      }
    }
  },
  vasuTemplates: {
    title: 'Opetussuunnitelmapohjat',
    name: 'Nimi',
    valid: 'Käytössä aikavälillä',
    type: 'Tyyppi',
    types: {
      DAYCARE: 'Varhaiskasvatussuunnitelma',
      PRESCHOOL: 'Esiopetuksen oppimissuunnitelma'
    },
    language: 'Kieli',
    languages: {
      FI: 'Suomenkielinen',
      SV: 'Ruotsinkielinen'
    },
    documentCount: 'Dokumentteja',
    addNewTemplate: 'Lisää uusi pohja',
    templateModal: {
      createTitle: 'Uusi pohja',
      editTitle: 'Muokkaa pohjaa',
      copyTitle: 'Kopioi pohja',
      validStart: 'Toimintavuosi alkaa',
      validEnd: 'Toimintavuosi päättyy'
    },
    unsavedWarning: 'Haluatko varmasti poistua tallentamatta?',
    addNewSection: 'Lisää uusi osio',
    addNewQuestion: 'Lisää uusi kysymys',
    addNewParagraph: 'Lisää uusi tekstikappale',
    hideSectionBeforeReady: 'Osio näytetään vasta laatimisen jälkeen',
    autoGrowingList: 'Automaattisesti kasvava lista',
    questionModal: {
      title: 'Uusi kysymys',
      type: 'Kysymyksen tyyppi',
      name: 'Kysymysteksti',
      info: 'Ohjeteksti',
      options: 'Vaihtoehdot',
      addNewOption: 'Lisää vaihtoehto',
      multiline: 'Monirivinen',
      minSelections: 'Valittava vähintään',
      keys: 'Tekstikenttien nimet',
      addNewKey: 'Lisää nimetty tekstikenttä',
      dateIsTrackedInEvents:
        'Päivämäärä näytetään suunnitelman tapahtumissa nimellä',
      paragraphTitle: 'Kappaleen otsikko',
      paragraphText: 'Kappaleen leipäteksti'
    },
    questionTypes: {
      TEXT: 'Tekstimuotoinen',
      CHECKBOX: 'Rasti',
      RADIO_GROUP: 'Valitse yksi',
      MULTISELECT: 'Monivalinta',
      MULTI_FIELD: 'Nimettyjä tekstikenttiä',
      MULTI_FIELD_LIST: 'Kasvava lista nimettyjä tekstikenttiä',
      DATE: 'Päivämäärä',
      FOLLOWUP: 'Seuranta'
    },
    errorCodes: {
      EXPIRED_START: 'Päättyneen pohjan alkupäivää ei voi muuttaa',
      EXPIRED_END: 'Päättyneen pohjan loppupäivää ei voi aikaistaa',
      FUTURE_START:
        'Tulevaisuuden pohjan alkupäivää ei voi siirtää menneisyyteen',
      CURRENT_START: 'Voimassa olevan pohjan alkupäivää ei voi vaihtaa',
      CURRENT_END:
        'Voimassa olevan pohjan loppupäivä voi olla aikaisintaan eilen',
      TEMPLATE_NAME: 'Käytössä olevan pohjan nimeä ei voi vaihtaa',
      TEMPLATE_LANGUAGE: 'Käytössä olevan pohjan kieltä ei voi vaihtaa'
    }
  },
  settings: {
    title: 'Asetukset',
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
    title: 'Toimintojen avaukset'
  },
  roles: {
    adRoles: {
      ADMIN: 'Pääkäyttäjä',
      DIRECTOR: 'Hallinto',
      REPORT_VIEWER: 'Raportointi',
      FINANCE_ADMIN: 'Talous',
      SERVICE_WORKER: 'Palveluohjaus',
      SPECIAL_EDUCATION_TEACHER: 'Erityisopettaja',
      STAFF: 'Henkilökunta',
      UNIT_SUPERVISOR: 'Johtaja',
      MOBILE: 'Mobiili'
    }
  },
  welcomePage: {
    text: 'Olet kirjautunut sisään Espoon kaupungin eVaka-palveluun. Käyttäjätunnuksellesi ei ole vielä annettu oikeuksia, jotka mahdollistavat palvelun käytön. Tarvittavat käyttöoikeudet saat omalta esimieheltäsi.'
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
  reloadNotification: {
    title: 'Uusi versio eVakasta saatavilla',
    buttonText: 'Lataa sivu uudelleen'
  }
}
