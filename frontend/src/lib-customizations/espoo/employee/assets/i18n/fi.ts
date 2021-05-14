// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export const fi = {
  titles: {
    defaultTitle: 'Varhaiskasvatus',
    login: 'Kirjaudu sisään',
    applications: 'Hakemukset',
    units: 'Yksiköt',
    customers: 'Asiakastiedot',
    placementDraft: 'Sijoitushahmotelma',
    decision: 'Päätöksen teko ja lähetys',
    feeDecisions: 'Maksupäätökset',
    feeDecision: 'Maksupäätös',
    feeDecisionDraft: 'Maksupäätösluonnos',
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
    employeePinCode: 'PIN-koodin hallinta'
  },
  common: {
    yes: 'Kyllä',
    no: 'Ei',
    and: 'Ja',
    loadingFailed: 'Tietojen haku epäonnistui',
    edit: 'Muokkaa',
    add: 'Lisää',
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
      temporary: 'Tilapäinen'
    },
    providerType: {
      MUNICIPAL: 'Kunnallinen',
      PURCHASED: 'Ostopalvelu',
      PRIVATE: 'Yksityinen',
      MUNICIPAL_SCHOOL: 'Suomenkielinen opetustoimi (SUKO)',
      PRIVATE_SERVICE_VOUCHER: 'Yksityinen (palveluseteli)'
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
      addressRestricted: 'Osoite ei ole saatavilla turvakiellon vuoksi'
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
      saveFailed: 'Muutosten tallentaminen ei onnistunut, yritä uudelleen.'
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
    fileDownloadError: {
      modalHeader: 'Tiedoston käsittely on kesken',
      modalMessage:
        'Tiedosto ei ole juuri nyt avattavissa. Kokeile hetken kuluttua uudelleen.'
    }
  },
  header: {
    title: 'Varhaiskasvatus',
    applications: 'Hakemukset',
    clubApplications: 'Kerho',
    units: 'Yksiköt',
    search: 'Asiakastiedot',
    finance: 'Talous',
    invoices: 'Laskut',
    feeDecisions: 'Maksupäätökset',
    valueDecisions: 'Arvopäätökset',
    reports: 'Raportit',
    messages: 'Viestit',
    logout: 'Kirjaudu ulos'
  },
  language: {
    fi: 'Suomi',
    sv: 'Ruotsi',
    en: 'Englanti'
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
    time: 'Aika on väärässä muodossa'
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
      basis: 'Perusteet',
      lessthan3: 'Alle 3-vuotias hoidontarpeen alkaessa',
      morethan3: 'Yli 3-vuotias hoidontarpeen alkaessa',
      currentUnit: 'Nyk.'
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
      noOtherChildren: 'Ei muita lapsia'
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
      partTimeLabel: 'Osa- tai kokopäiväinen'
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
      title: 'Henkilö- ja yhteystiedot',
      name: 'Lapsen nimi',
      email: 'Sähköposti',
      socialSecurityNumber: 'Henkilötunnus',
      birthday: 'Syntymäaika',
      language: 'Kieli',
      address: 'Osoite',
      familyLink: 'Perheen tiedot'
    },
    familyContacts: {
      title: 'Perheen yhteystiedot',
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
      info:
        'Kirjoita tähän varhaiskasvatussopimuksella ilmoitettu päivittäinen varhaiskasvatusaika, sisältäen esiopetuksen / valmistavan opetuksen / 5-vuotiaiden maksuttoman varhaiskasvatuksen.',
      info2:
        'Epäsäännölliset ja säännölliset poissaolot merkitään päiväkirjalle.',
      types: {
        notSet: 'Ei asetettu',
        regular: 'Säännöllinen varhaiskasvatusaika',
        irregular: 'Epäsäännöllinen varhaiskasvatusaika'
      },
      weekdays: {
        monday: 'Maanantai',
        tuesday: 'Tiistai',
        wednesday: 'Keskiviikko',
        thursday: 'Torstai',
        friday: 'Perjantai'
      },
      errors: {
        required: 'Arvo puuttuu'
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
        description: 'Kuvaus',
        descriptionPlaceholder:
          'Voit kirjoittaa tähän lisätietoa tuen tarpeesta.',
        bases: 'Perusteet',
        basisTypes: {
          AUTISM: 'Autismin kirjo',
          DEVELOPMENTAL_DISABILITY_1: 'Kehitysvamma 1',
          DEVELOPMENTAL_DISABILITY_2: 'Kehitysvamma 2',
          DEVELOPMENTAL_DISABILITY_2_INFO:
            'Käytetään silloin, kun esiopetuksessa oleva lapsi on vaikeasti kehitysvammainen.',
          FOCUS_CHALLENGE: 'Keskittymisen / tarkkaavaisuuden vaikeus',
          LINGUISTIC_CHALLENGE: 'Kielellinen vaikeus',
          DEVELOPMENT_MONITORING: 'Lapsen kehityksen seuranta',
          DEVELOPMENT_MONITORING_PENDING:
            'Lapsen kehityksen seuranta, tutkimukset kesken',
          DEVELOPMENT_MONITORING_PENDING_INFO:
            'Lapsi on terveydenhuollon tutkimuksissa, diagnoosi ei ole vielä varmistunut.',
          MULTI_DISABILITY: 'Monivammaisuus',
          LONG_TERM_CONDITION: 'Pitkäaikaissairaus',
          REGULATION_SKILL_CHALLENGE: 'Säätelytaitojen vaikeus',
          DISABILITY: 'Vamma (näkö, kuulo, liikunta, muu)',
          OTHER: 'Muu peruste'
        },
        otherBasisPlaceholder: 'Kirjoita muu peruste.'
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
          ASSISTANCE_SERVICE_CHILD: 'Avustamispalvelut yhdelle lapselle',
          ASSISTANCE_SERVICE_UNIT: 'Avustamispalvelut yksikköön',
          SMALLER_GROUP: 'Pienennetty ryhmä',
          SPECIAL_GROUP: 'Erityisryhmä',
          PERVASIVE_VEO_SUPPORT: 'Laaja-alaisen veon tuki',
          RESOURCE_PERSON: 'Resurssihenkilö',
          RATIO_DECREASE: 'Suhdeluvun väljennys',
          PERIODICAL_VEO_SUPPORT: 'Jaksottainen veon tuki (2–6 kk)',
          OTHER: 'Muu tukitoimi'
        },
        measures: 'Toimenpiteet',
        measureTypes: {
          SPECIAL_ASSISTANCE_DECISION: 'Erityisen tuen päätös, esiopetus\n',
          SPECIAL_ASSISTANCE_DECISION_INFO:
            'Lapsella on pidennetty oppivelvollisuus.',
          INTENSIFIED_ASSISTANCE: 'Tehostettu tuki, esiopetus',
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
      title: 'Sijoitukset',
      restrictedName: '(Yksikön nimi piilotettu)',
      rowTitle: 'Sijoituspäätös voimassa',
      startDate: 'Aloituspäivämäärä',
      endDate: 'Päättymispäivämäärä',
      area: 'Alue',
      daycareUnit: 'Toimipaikka',
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
        text:
          'Tästä sijoituksesta ei voi lähettää päätöstä. Jos sijoitus menee päällekäin lapsen aiemmin luotujen sijoituksien kanssa, näitä sijoituksia lyhennetään tai ne poistetaan automaattisesti.',
        temporaryDaycareWarning: 'HUOM! Älä käytä varasijoitusta tehdessäsi!'
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
        ghostUnit: 'Yksikkö on merkitty haamuyksiköksi.'
      },
      serviceNeeds: {
        title: 'Sijoituksen palveluntarpeet (alpha-testaus)',
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
          title: 'Palveluntarpeet meneevät päällekkäin',
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
      info:
        'Lapseen liittyvät viestit lähetetään merkityille vastaanottajille. Esimies tai palveluohjaus voi perustelluista syistä estää viestin lähettämisen valitulle päämiehelle tai huoltajalle, poistamalla ruksin kyseisen henkilön kohdalta.',
      name: 'Vastaanottajan nimi',
      role: 'Rooli',
      notBlocklisted: 'Saa Vastaanottaa',
      guardian: 'Huoltaja',
      headOfChild: 'Päämies'
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
    title: 'Aikuisen tiedot',
    restrictedDetails: 'Turvakielto',
    personDetails: 'Henkilö- ja yhteystiedot',
    addSsn: 'Aseta hetu',
    noSsn: 'Hetuton',
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
      button: 'Luo takautuva maksupäätösluonnos',
      modalTitle: 'Luo takautuvia maksupäätösluonnoksia'
    },
    invoices: 'Päämiehen laskut',
    dependants: 'Päämiehen huollettavat',
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
      error: 'Tulotietojen lataus epäonnistui',
      itemHeader: 'Tulotiedot ajalle',
      itemHeaderNew: 'Uusi tulotieto',
      details: {
        name: 'Nimi',
        updated: 'Tulotiedot päivitetty',
        handler: 'Käsittelijä',
        originApplication:
          'Huoltaja on hakemuksella suostunut korkeimpaan maksuluokkaan',
        dateRange: 'Ajalle',
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
        incomeTypes: {
          MAIN_INCOME: 'Päätulot',
          SHIFT_WORK_ADD_ON: 'Vuorotyölisät',
          PERKS: 'Luontaisedut',
          SECONDARY_INCOME: 'Sivutulo',
          PENSION: 'Eläkkeet',
          UNEMPLOYMENT_BENEFITS: 'Työttömyyskorvaus/työmarkkinatuki',
          SICKNESS_ALLOWANCE: 'Sairauspäiväraha',
          PARENTAL_ALLOWANCE: 'Äitiys- ja vanhempainraha',
          HOME_CARE_ALLOWANCE: 'Kotihoidontuki, joustava/osittainen hoitoraha',
          ALIMONY: 'Elatusapu/-tuki',
          OTHER_INCOME: 'Muu tulo (korko, vuokra, osinko jne.)',
          ALL_EXPENSES: 'Menot (esim. maksetut elatusmaksut tai syytinki)'
        },
        incomeCoefficients: {
          MONTHLY_WITH_HOLIDAY_BONUS: 'Kuukausi',
          MONTHLY_NO_HOLIDAY_BONUS: 'Kuukausi ilman lomarahaa',
          BI_WEEKLY_WITH_HOLIDAY_BONUS: '2 viikkoa',
          BI_WEEKLY_NO_HOLIDAY_BONUS: '2 viikkoa ilman lomarahaa',
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
    invoice: {
      validity: 'Aikaväli',
      price: 'Summa',
      status: 'Status'
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
      waitingConfirmation: 'Vahvistettavana huoltajalla',
      placementProposals: 'Sijoitusehdotukset',
      applications: 'Hakemukset'
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
        modalText1: 'Mene mobiililaitteella osoitteeseen',
        modalText2: 'ja syötä laitteeseen alla oleva koodi.',
        modalText3:
          'Syötä mobiililaitteessa näkyvä vahvistuskoodi alla olevaan kenttään.',
        modalText4:
          'Anna mobiililaitteelle vielä nimi, jolla erotat sen yksikön muista mobiililaiteista.',
        mobileDevices: 'Yksikön mobiililaitteet',
        addMobileDevice: 'Lisää mobiililaite',
        modalTitle: 'Lisää yksikköön uusi mobiililaite',
        editName: 'Muokkaa laitteen nimeä',
        removeConfirmation: 'Haluatko poistaa mobiililaitteen?',
        editPlaceholder: 'esim. Hippiäisten kännykkä'
      }
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
      acceptAllButton: 'Vahvista hyväksytyt',
      rejectTitle: 'Perustele sijoitusehdotuksen palautus',
      rejectReasons: {
        REASON_1:
          'TILARAJOITE, sovittu varhaiskasvatuksen aluepäällikön kanssa.',
        REASON_2:
          'YKSIKÖN KOKONAISTILANNE, sovittu varhaiskasvatuksen aluepäällikön kanssa.',
        OTHER: 'Muu syy'
      },
      infoBoxTitle: 'Ohje yksikön johtajalle',
      infoBoxText:
        'Merkitse lapset, jotka pystyt ottamaan vastaan. Kun olet hyväksynyt kaikki lapset voit painaa Vahvista hyväksytyt -nappia. Mikäli et pysty hyväksymään kaikkia lapsia, merkitse rasti ja lisää perustelu. Palveluohjaus tekee tällöin uuden sijoitusehdotuksen tai ottaa yhteyttä.'
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
    groups: {
      title: 'Toimipisteen ryhmät',
      familyContacts: 'Näytä yhteystietokooste',
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
        header: 'Päivän muistiinpanot',
        groupNoteHeader: 'Päivän muistiinpano koko ryhmälle',
        notesHeader: 'Päivän tapahtumia (ei terveystietoja)',
        groupNotesHeader: 'Koko ryhmää koskeva muistiinpano',
        notesHint: 'Mitä tänään opin, leikin, oivalsin.',
        feedingHeader: 'Lapsi söi tänään',
        sleepingHeader: 'Lapsi nukkui tänään',
        sleepingHint: 'esim 1,5h',
        sleepingHours: 'tuntia',
        reminderHeader: 'Muistettavia asioita',
        otherThingsToRememberHeader: 'Muuta muistettavaa (esim aurinkovoide)',
        groupNoteModalAddLink: 'Päivän muistiinpano koko ryhmälle',
        groupNoteHint: 'Kirjoita muistiinpano ja ajankohta',
        groupNoteModalModifyLink:
          'Muokkaa tai poista muistiinpano koko ryhmälle',
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
    error: {
      placement: {
        create: 'Sijoitus ryhmään epäonnistui',
        transfer: 'Sijoitus toiseen ryhmään epäonnistui'
      }
    }
  },
  groupCaretakers: {
    title: 'Henkilökunnan tarve ryhmässä',
    info:
      'Luo aina uusi henkilökunnan tarve, kun henkilökunnan lukumäärä muuttuu. Ilmoitettu lukumäärä on voimassa valitulla ajanjaksolla ja vaikuttaa yksikön ja ryhmän täyttöasteisiin.',
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
  invoices: {
    table: {
      title: 'Laskut',
      toggleAll: 'Valitse kaikki alueen laskut',
      head: 'Päämies',
      children: 'Lapset',
      period: 'Laskutuskausi',
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
        ssn: 'Päämiehen hetu'
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
      save: 'Tallenna muutokset'
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
      save: 'Tallenna muutokset'
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
      TEMPORARY_DAYCARE_PART_DAY: 'Tilapäinen osapäiväinen varhaiskasvatus'
    },
    serviceNeed: {
      MISSING: 'Vahvistamaton palveluntarve',
      GTE_35: 'Maksullista varhaiskasvatusta vähintään 35h',
      GTE_25: 'Maksullista varhaiskasvatusta vähintään 25h',
      GT_25_LT_35: 'Maksullista varhaiskasvatusta yli 25h ja alle 35h',
      GT_15_LT_25: 'Maksullista varhaiskasvatusta yli 15h ja alle 25h',
      LTE_25: 'Maksullista varhaiskasvatusta enintään 25h',
      LTE_15: 'Maksullista varhaiskasvatusta enintään 15h',
      LTE_0: 'Ei maksullista varhaiskasvatusta'
    }
  },
  product: {
    DAYCARE: 'Varhaiskasvatus',
    DAYCARE_DISCOUNT: 'Alennus (maksup.)',
    DAYCARE_INCREASE: 'Korotus (maksup.)',
    PRESCHOOL_WITH_DAYCARE: 'Varhaiskasvatus + Esiopetus',
    PRESCHOOL_WITH_DAYCARE_DISCOUNT: 'Alennus (maksup.)',
    PRESCHOOL_WITH_DAYCARE_INCREASE: 'Korotus (maksup.)',
    TEMPORARY_CARE: 'Tilapäinen varhaiskasvatus',
    SICK_LEAVE_100: 'Sairaspoissaolo 100%',
    SICK_LEAVE_50: 'Sairaspoissaolo 50%',
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
    decisionNUmber: 'Päätöksen numero',
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
          types: {
            MAIN_INCOME: 'Päätulot',
            SHIFT_WORK_ADD_ON: 'Vuorotyölisät',
            PERKS: 'Luontaisedut',
            SECONDARY_INCOME: 'Sivutulo',
            PENSION: 'Eläkkeet',
            UNEMPLOYMENT_BENEFITS: 'Työttömyyskorvaus/työmarkkinatuki',
            SICKNESS_ALLOWANCE: 'Sairauspäiväraha',
            PARENTAL_ALLOWANCE: 'Äitiys- ja vanhempainraha',
            HOME_CARE_ALLOWANCE:
              'Kotihoidontuki, joustava/osittainen hoitoraha',
            ALIMONY: 'Elatusapu/-tuki',
            OTHER_INCOME: 'Muu tulo',
            ALL_EXPENSES: 'Menot (esim. maksetut elatusmaksut tai syytinki)'
          },
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
    decisionNUmber: 'Päätöksen numero',
    validPeriod: 'Arvopäätös voimassa',
    sentAt: 'Arvopäätös lähetetty',
    pdfLabel: 'Arvopäätös PDF',
    decisionHandlerName: 'Päätöksen käsittelijä',
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
        types: {
          MAIN_INCOME: 'Päätulot',
          SHIFT_WORK_ADD_ON: 'Vuorotyölisät',
          PERKS: 'Luontaisedut',
          SECONDARY_INCOME: 'Sivutulo',
          PENSION: 'Eläkkeet',
          UNEMPLOYMENT_BENEFITS: 'Työttömyyskorvaus/työmarkkinatuki',
          SICKNESS_ALLOWANCE: 'Sairauspäiväraha',
          PARENTAL_ALLOWANCE: 'Äitiys- ja vanhempainraha',
          HOME_CARE_ALLOWANCE: 'Kotihoidontuki, joustava/osittainen hoitoraha',
          ALIMONY: 'Elatusapu/-tuki',
          OTHER_INCOME: 'Muu tulo',
          ALL_EXPENSES: 'Menot (esim. maksetut elatusmaksut tai syytinki)'
        },
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
      'sunnuntai',
      'maanantai',
      'tiistai',
      'keskiviikko',
      'torstai',
      'perjantai',
      'lauantai'
    ],
    weekdaysShort: ['su', 'ma', 'ti', 'ke', 'to', 'pe', 'la']
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
      PRESENCE: 'Ei poissaoloa'
    },
    careTypes: {
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
        PRESENCE: 'Ei poissaoloa'
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
      title: 'Korkein täyttöaste ennen sijoitusta',
      unitLink: 'Yksikön tiedot',
      remove: 'Poista'
    },
    upcoming: 'Tulossa',
    active: 'Aktiivinen',
    currentPlacements: 'Olemassa olevat sijoitukset',
    placementOverlapError:
      'Aiemmat päällekäiset sijoitukset katkaistaan automaattisesti mikäli kuntalainen ottaa tarjottavan paikan vastaan.',
    createPlacementDraft: 'Luo sijoitushahmotelma',
    datesTitle: 'Nyt luotava sijoitushahmotelma',
    type: 'Toimintamuoto',
    date: 'Sijoituspäivämäärä',
    dateError: 'Päällekkäinen sijoitus ajanjaksolle.',
    preparatoryPeriod: 'Valmistava opetus',
    dateOfBirth: 'Syntymäaika',
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
        PRIVATE_SERVICE_VOUCHER: 'Palveluseteli'
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
      preferredStartingDate: 'Aloituspäivä',
      under3Years: 'Vakahakemuksia (alle 3v)',
      over3Years: 'Vakahakemuksia (yli 3v)',
      preschool: 'Esiopetushakemuksia',
      club: 'Kerhohakemuksia',
      totalChildren: 'Lapsia hakenut yhteensä'
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
        'Tarkista ettei henkilöillä ole esimerkiksi päällekäisiä sijoituksia, palveluntarpeita tai muita päällekkäisyyksiä, jotka voisivat estää yhdistämisen.'
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
      average: 'Keskiarvo'
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
      title: 'Palveluseteli',
      description:
        'Palveluseteliyksiköihin kohdistuvat palvelusetelisummat sekä lapsikohtaiset maksut.',
      filters: {
        areaPlaceholder: 'Valitse palvelualue',
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
      coefficient: 'Kerroin',
      serviceNeed: 'Palveluntarve',
      serviceNeedType: 'h / vk',
      partTime: 'Osa/Koko'
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
      ophUnitOid: 'Toimipaikan OID',
      ophOrganizerOid: 'Järjestäjän OID',
      ophOrganizationOid: 'Organisaation OID',
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
      uploadToVarda: 'Lähetetään Vardaan',
      uploadToKoski: 'Lähetetään Koski-palveluun',
      invoicedByMunicipality: 'Laskutetaan eVakasta',
      decisionCustomization: {
        handler: {
          0: 'Palveluohjaus',
          1: 'Varhaiskasvatusyksikön johtaja',
          2: 'Ledare inom småbarnspedagogik',
          3: 'Svenska bildningstjänster / Småbarnspedagogik'
        }
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
      url:
        'esim. https://www.espoo.fi/fi-FI/Kasvatus_ja_opetus/.../Suvelan_paivakoti',
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
      ophOrganizationOid: 'Organisaation OID puuttuu'
    }
  },
  fileUpload: {
    upload: {
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
    download: {
      modalHeader: 'Tiedoston käsittely on kesken',
      modalMessage:
        'Tiedosto ei ole juuri nyt avattavissa. Kokeile hetken kuluttua uudelleen.'
    }
  },
  messages: {
    inboxTitle: 'Viestit',
    unitList: {
      title: 'Yksiköt'
    },
    sidePanel: {
      ownMessages: 'Omat viestit',
      groupsMessages: 'Ryhmien viestit'
    },
    messageBoxes: {
      names: {
        RECEIVED: 'Saapuneet',
        SENT: 'Lähetetyt',
        DRAFT: 'Luonnokset'
      },
      receivers: 'Vastaanottajat',
      newBulletin: 'Uusi viesti'
    },
    messageList: {
      titles: {
        RECEIVED: 'Saapuneet viestit',
        SENT: 'Lähetetyt viestit',
        DRAFT: 'Luonnokset'
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
      newBulletin: 'Uusi viesti',
      to: {
        label: 'Vastaanottaja',
        placeholder: 'Valitse ryhmä',
        noOptions: 'Ei ryhmiä'
      },
      sender: 'Lähettäjä',
      receivers: 'Vastaanottajat',
      title: 'Otsikko',
      message: 'Viesti',
      deleteDraft: 'Hylkää luonnos',
      send: 'Lähetä'
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
    findByName: 'Etsi nimellä'
  },
  roles: {
    adRoles: {
      ADMIN: 'Pääkäyttäjä',
      DIRECTOR: 'Raportointi',
      FINANCE_ADMIN: 'Talous',
      SERVICE_WORKER: 'Palveluohjaus',
      SPECIAL_EDUCATION_TEACHER: 'Erityisopettaja',
      STAFF: 'Henkilökunta',
      UNIT_SUPERVISOR: 'Johtaja',
      MOBILE: 'Mobiili'
    }
  }
}
