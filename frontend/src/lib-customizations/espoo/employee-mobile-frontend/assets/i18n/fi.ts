// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export const fi = {
  common: {
    loadingFailed: 'Tietojen haku epäonnistui',
    cancel: 'Peruuta',
    confirm: 'Vahvista',
    all: 'Kaikki',
    statuses: {
      active: 'Aktiivinen',
      coming: 'Tulossa',
      completed: 'Päättynyt',
      conflict: 'Konflikti'
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
    placement: {
      CLUB: 'Kerho',
      DAYCARE: 'Varhaiskasvatus',
      DAYCARE_PART_TIME: 'Osapäiväinen varhaiskasvatus',
      DAYCARE_FIVE_YEAR_OLDS: '5-vuotiaiden varhaiskasvatus',
      DAYCARE_PART_TIME_FIVE_YEAR_OLDS:
        '5-vuotiaiden osapäiväinen varhaiskasvatus',
      PRESCHOOL: 'Esiopetus',
      PRESCHOOL_DAYCARE: 'Liittyvä varhaiskasvatus',
      PREPARATORY: 'Valmistava',
      PREPARATORY_DAYCARE: 'Valmistava',
      TEMPORARY_DAYCARE: 'Väliaikainen',
      TEMPORARY_DAYCARE_PART_DAY: 'Väliaikainen osa-aikainen',
      SCHOOL_SHIFT_CARE: 'Koululaisten vuorohoito'
    },
    code: 'Koodi',
    children: 'Lapset',
    name: 'Nimi',
    staff: 'Henkilökunta',
    messages: 'Viestit',
    back: 'Takaisin',
    close: 'Sulje',
    hours: 'Tuntia',
    remove: 'Poista',
    doNotRemove: 'Älä poista',
    clear: 'Tyhjennä',
    edit: 'Muokkaa',
    save: 'Tallenna',
    saving: 'Tallennetaan',
    saved: 'Tallennettu',
    search: 'Hae',
    noResults: 'Ei hakutuloksia',
    doNotSave: 'Älä tallenna',
    starts: 'Alkaa',
    ends: 'Päättyy',
    information: 'Tiedot',
    dailyNotes: 'Muistiinpanot',
    saveBeforeClosing: 'Tallennetaanko ennen sulkemista',
    hourShort: 't',
    minuteShort: 'min',
    yearsShort: 'v',
    errors: {
      minutes: 'Korkeintaan 59 minuuttia'
    },
    child: 'Lapsi',
    group: 'Ryhmä',
    yesterday: 'eilen',
    validation: {
      dateLte: (date: string) => `Oltava ${date} tai aikaisemmin`
    },
    openExpandingInfo: 'Avaa lisätietokenttä',
    nb: 'Huom',
    validity: 'Voimassaolo',
    validTo: (date: string) => `Voimassa ${date} saakka`
  },
  errorPage: {
    reload: 'Lataa sivu uudelleen',
    text: 'Kohtasimme odottamattoman ongelman. Virheen tiedot on välitetty eteenpäin.',
    title: 'Jotain meni pieleen'
  },
  absences: {
    title: 'Poissaolomerkintä',
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
      SCHOOL_SHIFT_CARE: 'Koululaisten vuorohoito',
      PRESCHOOL: 'Esiopetus',
      PRESCHOOL_DAYCARE: 'Liittyvä varhaiskasvatus',
      DAYCARE_5YO_FREE: '5-vuotiaiden varhaiskasvatus',
      DAYCARE: 'Varhaiskasvatus',
      CLUB: 'Kerho'
    },
    chooseStartDate: 'Valitse tuleva päivä',
    startBeforeEnd: 'Aloitus oltava ennen päättymispäivää.',
    reason: 'Poissaolon syy',
    fullDayHint: 'Poissaolomerkintä tehdään koko päivälle',
    confirmDelete: 'Haluatko poistaa tämän poissaolon?',
    futureAbsence: 'Tulevat poissaolot'
  },
  attendances: {
    types: {
      COMING: 'Tulossa',
      PRESENT: 'Läsnä',
      DEPARTED: 'Lähtenyt',
      ABSENT: 'Poissa'
    },
    status: {
      COMING: 'Tulossa',
      PRESENT: 'Saapunut',
      DEPARTED: 'Lähtenyt',
      ABSENT: 'Poissa'
    },
    groupSelectError: 'Valitun ryhmän nimeä ei löytynyt',
    actions: {
      markAbsent: 'Merkitse poissaolevaksi',
      markPresent: 'Merkitse saapuneeksi',
      markDeparted: 'Merkitse lähteneeksi',
      returnToComing: 'Palauta tulossa oleviin',
      returnToPresent: 'Palauta läsnäoleviin',
      markAbsentBeforehand: 'Tulevat poissaolot'
    },
    timeLabel: 'Merkintä',
    departureTime: 'Lähtöaika',
    arrivalTime: 'Saapumisaika',
    chooseGroup: 'Valitse ryhmä',
    chooseGroupInfo: 'Lapsia: Läsnä nyt/Ryhmässä yhteensä',
    searchPlaceholder: 'Etsi lapsen nimellä',
    noAbsences: 'Ei poissaoloja',
    missingFrom: 'Poissa seuraavasta toimintamuodosta',
    missingFromPlural: 'Poissa seuraavista toimintamuodoista',
    timeError: 'Virheellinen aika',
    arrived: 'Saapui',
    departed: 'Lähti',
    serviceTime: {
      reservation: 'Varaus tänään',
      reservations: 'Varaukset tänään',
      reservationShort: 'Varaus',
      reservationsShort: 'Varaukset',
      serviceToday: (start: string, end: string) =>
        `Varhaiskasvatusaika tänään ${start}-${end}`,
      serviceTodayShort: (start: string, end: string) =>
        `Sop.aika ${start}-${end}`,
      noServiceToday: 'Ei varattua varhaiskasvatusaikaa tänään',
      noServiceTodayShort: 'Ei sop.aikaa tänään',
      notSet: 'Varhaiskasvatusaikaa ei asetettuna',
      notSetShort: 'Sop.aika puuttuu',
      variableTimes: 'Vaihteleva varhaiskasvatusaika',
      variableTimesShort: 'Sop.aika vaihtelee'
    },
    notes: {
      day: 'Päivä',
      dailyNotes: 'Muistiinpanot',
      addNew: 'Lisää uusi',
      labels: {
        note: 'Päivän tapahtumia',
        feedingNote: 'Lapsi söi tänään',
        sleepingNote: 'Lapsi nukkui tänään',
        reminderNote: 'Muistettavia asioita',
        groupNotesHeader: 'Koko ryhmän muistiinpano'
      },
      sleepingValues: {
        GOOD: 'Hyvin',
        MEDIUM: 'Vähän',
        NONE: 'Ei ollenkaan'
      },
      feedingValues: {
        GOOD: 'Hyvin',
        MEDIUM: 'Vähän',
        NONE: 'Ei ollenkaan/maistoi'
      },
      reminders: {
        DIAPERS: 'Lisää vaippoja',
        CLOTHES: 'Lisää varavaatteita',
        LAUNDRY: 'Repussa pyykkiä'
      },
      placeholders: {
        note: 'Leikkejä, onnistumisia, ilonaiheita ja opittuja asioita tänään (ei terveystietoja tai salassapidettäviä tietoja).',
        childStickyNote:
          'Muistiinpano henkilökunnalle (ei terveystietoja tai salassapidettäviä tietoja).',
        groupNote: 'Koko ryhmää koskeva muistiinpano',
        reminderNote: 'Muuta muistettavaa (esim. aurinkovoide)',
        hours: 'tunnit',
        minutes: 'minuutit'
      },
      noNotes: 'Ei merkintöjä tälle päivälle',
      clearTitle: 'Haluatko tyhjentää merkinnät tältä päivältä?',
      confirmTitle: 'Tallennetaanko tehdyt merkinnät ennen sulkemista?',
      closeWithoutSaving: 'Sulje tallentamatta',
      groupNote: 'Ryhmän muistiinpanot',
      note: 'Tänään koettua ja opittua',
      otherThings: 'Muut asiat',
      childStickyNotes: 'Huomioitavaa lähipäivinä'
    },
    absenceTitle: 'Poissaolomerkintä',
    childInfo: {
      header: 'Lapsen tiedot',
      personalInfoHeader: 'Lapsen henkilötiedot',
      childName: 'Lapsen nimi',
      preferredName: 'Kutsumanimi',
      dateOfBirth: 'Syntymäaika',
      address: 'Lapsen kotiosoite',
      type: 'Sijoitusmuoto',
      allergiesHeader: 'Allergiat, ruokavalio, lääkitys',
      allergies: 'Allergiat',
      diet: 'Ruokavalio',
      medication: 'Lääkitys',
      contactInfoHeader: 'Yhteystiedot',
      contact: 'Yhteyshenkilö',
      name: 'Nimi',
      phone: 'Puhelinnumero',
      backupPhone: 'Varapuhelinnumero',
      email: 'Sähköpostiosoite',
      backupPickup: 'Varahakija',
      backupPickupName: 'Varahakijan nimi',
      image: {
        modalMenu: {
          title: 'Lapsen profiilikuva',
          takeImageButton: 'Valitse kuva',
          deleteImageButton: 'Poista kuva',
          deleteConfirm: {
            title: 'Haluatko varmasti poistaa lapsen kuvan?',
            resolve: 'Poista kuva',
            reject: 'Älä poista'
          }
        }
      }
    },
    staff: {
      errors: {
        employeeNotFound: 'Työntekijää ei löytynyt',
        wrongPin: 'Väärä PIN-koodi'
      },
      externalPerson: 'Muu henkilö',
      markExternalPerson: 'Kirjaa muu henkilö',
      markExternalPersonTitle: 'Kirjaa muu vastuullinen henkilö sisään',
      markArrived: 'Kirjaudu läsnäolevaksi',
      markDeparted: 'Kirjaudu poissaolevaksi',
      loginWithPin: 'Kirjaudu PIN-koodilla',
      pinNotSet: 'Aseta itsellesi PIN-koodi',
      pinLocked: 'Vaihda lukkiutunut PIN-koodi'
    }
  },
  staff: {
    title: 'Henkilökunnan määrä tänään',
    daycareResponsible: 'Kasvatusvastuullisia',
    other: 'Muita (esim. avustajat, opiskelijat, veot)',
    cancel: 'Peru muokkaus',
    realizedGroupOccupancy: 'Ryhmän käyttöaste tänään',
    realizedUnitOccupancy: 'Yksikön käyttöaste tänään',
    notUpdated: 'Tietoja ei ole päivitetty',
    updatedToday: 'Tiedot päivitetty tänään',
    updated: 'Tiedot päivitetty'
  },
  pin: {
    header: 'Lukituksen avaaminen',
    info: 'Anna PIN-koodi avataksesi lapsen tiedot',
    selectStaff: 'Valitse käyttäjä',
    staff: 'Käyttäjä',
    noOptions: 'Ei vaihtoehtoja',
    pinCode: 'PIN-koodi',
    status: {
      SUCCESS: 'Oikea PIN-koodi',
      WRONG_PIN: 'Väärä PIN-koodi',
      PIN_LOCKED: 'PIN-koodi on lukittu',
      NOT_FOUND: 'Tuntematon käyttäjä'
    },
    unknownError: 'Tuntematon virhe',
    logOut: 'Kirjaudu ulos',
    login: 'Kirjaudu',
    loggedIn: 'Kirjautunut sisään'
  },
  messages: {
    title: 'Saapuneet viestit',
    inputPlaceholder: 'Kirjoita...',
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
      deleteDraft: 'Hylkää luonnos',
      send: 'Lähetä',
      sending: 'Lähetetään'
    },
    emptyInbox: 'Viestilaatikkosi on tyhjä'
  },
  mobile: {
    landerText1:
      'Tervetuloa käyttämään Espoon varhaiskasvatuksen mobiilisovellusta!',
    landerText2:
      'Ottaaksesi sovelluksen käyttöön valitse alta ‘Lisää laite’ ja rekisteröi mobiililaite eVakassa oman yksikkösi sivulla.',
    actions: {
      ADD_DEVICE: 'Lisää laite',
      START: 'Aloitetaan'
    },
    wizard: {
      text1: 'Syötä eVakasta saatava 10-merkkinen koodi kenttään alla.',
      text2: 'Syötä alla oleva vahvistuskoodi eVakaan.',
      title1: 'eVaka-mobiilin käyttöönotto, vaihe 1/3',
      title2: 'eVaka-mobiilin käyttöönotto, vaihe 2/3',
      title3: 'Tervetuloa käyttämään eVaka-mobiilia!',
      text3: 'eVaka-mobiili on nyt käytössä tässä laitteessa.',
      text4:
        'Turvataksesi lasten tiedot muistathan asettaa laitteeseen pääsykoodin, jos et ole sitä vielä tehnyt.'
    },
    emptyList: {
      no: 'Ei',
      status: {
        COMING: 'tulossa olevia',
        ABSENT: 'poissaolevia',
        PRESENT: 'läsnäolevia',
        DEPARTED: 'lähteneitä'
      },
      children: 'lapsia'
    }
  },
  reloadNotification: {
    title: 'Uusi versio eVakasta saatavilla',
    buttonText: 'Lataa sivu uudelleen'
  },
  childButtons: {
    newMessage: 'Uusi viesti'
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
  }
}
