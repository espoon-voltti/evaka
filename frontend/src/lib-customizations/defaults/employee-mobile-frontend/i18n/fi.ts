// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import components from '../../components/i18n/fi'

export const fi = {
  common: {
    yesIDo: 'Kyllä',
    noIDoNot: 'En',
    loadingFailed: 'Tietojen haku epäonnistui',
    add: 'Lisää',
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
      PRESCHOOL_DAYCARE_ONLY: 'Pelkkä liittyvä',
      PRESCHOOL_CLUB: 'Esiopetuksen kerho',
      PREPARATORY: 'Valmistava',
      PREPARATORY_DAYCARE: 'Valmistava',
      PREPARATORY_DAYCARE_ONLY: 'Pelkkä liittyvä',
      TEMPORARY_DAYCARE: 'Väliaikainen',
      TEMPORARY_DAYCARE_PART_DAY: 'Väliaikainen osa-aikainen',
      SCHOOL_SHIFT_CARE: 'Koululaisten vuorohoito'
    },
    code: 'Koodi',
    children: 'Lapset',
    name: 'Nimi',
    staff: 'Henkilökunta',
    messages: 'Viestit',
    settings: 'Asetukset',
    back: 'Takaisin',
    return: 'Palaa',
    close: 'Sulje',
    open: 'Avaa',
    hours: 'Tuntia',
    remove: 'Poista',
    doNotRemove: 'Älä poista',
    clear: 'Tyhjennä',
    edit: 'Muokkaa',
    save: 'Tallenna',
    saveChanges: 'Tallenna muutokset',
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
    week: 'Viikko',
    yearsShort: 'v',
    errors: {
      minutes: 'Korkeintaan 59 minuuttia'
    },
    child: 'Lapsi',
    group: 'Ryhmä',
    yesterday: 'eilen',
    validation: {
      dateLte: (date: string) => `Oltava ${date} tai aikaisemmin`,
      dateBetween: (start: string, end: string) =>
        `Oltava välillä ${start}-${end}`
    },
    nb: 'Huom',
    validity: 'Voimassaolo',
    validTo: (date: string) => `Voimassa ${date} saakka`,
    lastName: 'Sukunimi',
    firstName: 'Etunimi',
    openExpandingInfo: 'Avaa lisätietokenttä'
  },
  systemNotification: {
    title: 'Ilmoitus'
  },
  errorPage: {
    reload: 'Lataa sivu uudelleen',
    text: 'Kohtasimme odottamattoman ongelman. Virheen tiedot on välitetty eteenpäin.',
    title: 'Jotain meni pieleen'
  },
  absences: {
    title: 'Poissaolomerkintä',
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
    careTypes: {
      SCHOOL_SHIFT_CARE: 'Koululaisten vuorohoito',
      PRESCHOOL: 'Esiopetus',
      PRESCHOOL_DAYCARE: 'Liittyvä varhaiskasvatus',
      PRESCHOOL_CLUB: 'Esiopetuksen kerho',
      DAYCARE_5YO_FREE: '5-vuotiaiden varhaiskasvatus',
      DAYCARE: 'Varhaiskasvatus',
      CLUB: 'Kerho'
    },
    chooseStartDate: 'Valitse tuleva päivä',
    startBeforeEnd: 'Aloitus oltava ennen päättymispäivää.',
    reason: 'Poissaolon syy',
    fullDayHint: 'Poissaolomerkintä tehdään koko päivälle',
    confirmDelete: 'Haluatko poistaa tämän poissaolon?',
    futureAbsence: 'Tulevat poissaolot',
    laterAbsence: 'Myöhemmät poissaolot'
  },
  attendances: {
    views: {
      TODAY: 'Tänään',
      NEXT_DAYS: 'Seuraavat päivät'
    },
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
    staffTypes: {
      PRESENT: 'Paikalla',
      OTHER_WORK: 'Työasia',
      TRAINING: 'Koulutus',
      OVERTIME: 'Ylityö',
      JUSTIFIED_CHANGE: 'Perusteltu muutos'
    },
    groupSelectError: 'Valitun ryhmän nimeä ei löytynyt',
    actions: {
      markAbsent: 'Merkitse poissaolevaksi',
      cancelAbsence: 'Peruuta poissaolo',
      markPresent: 'Merkitse saapuneeksi',
      markDeparted: 'Merkitse lähteneeksi',
      returnToComing: 'Palauta tulossa oleviin',
      returnToPresent: 'Palauta läsnäolevaksi',
      or: 'tai',
      returnToPresentNoTimeNeeded:
        'Palauta läsnäolevaksi ilman uutta saapumisaikaa',
      markAbsentBeforehand: 'Tulevat poissaolot',
      markReservations: 'Tulevat varaukset ja poissaolot',
      confirmedRangeReservations: {
        markReservations: 'Muokkaa varauksia',
        markAbsentBeforehand: 'Merkitse poissaolo'
      }
    },
    validationErrors: {
      required: 'Pakollinen',
      timeFormat: 'Tarkista',
      overlap: 'Päällekkäisiä merkintöjä'
    },
    timeLabel: 'Merkintä',
    termBreak: 'Ei toimintaa tänään',
    departureTime: 'Lähtöaika',
    arrivalTime: 'Saapumisaika',
    chooseGroup: 'Valitse ryhmä',
    chooseGroupInfo: 'Lapsia: Läsnä nyt/Ryhmässä yhteensä',
    searchPlaceholder: 'Etsi lapsen nimellä',
    noAbsences: 'Ei poissaoloja',
    removeAbsence: 'Peru poissaolo',
    timeError: 'Virheellinen aika',
    arrived: 'Saapui',
    departed: 'Lähti',
    noGroup: 'Ei ryhmää',
    serviceTime: {
      reservation: 'Varaus',
      reservations: 'Varaukset',
      serviceToday: (start: string, end: string) =>
        `Varhaiskasvatusaika tänään ${start}-${end}`,
      serviceTodayShort: (start: string, end: string) =>
        `Sop.aika ${start}-${end}`,
      noServiceToday: 'Ei varattua varhaiskasvatusaikaa tänään',
      noServiceTodayShort: 'Ei sop.aikaa tänään',
      notSet: 'Läsnäoloilmoitus puuttuu',
      notSetShort: 'Ilmoitus puuttuu',
      variableTimes: 'Vaihteleva varhaiskasvatusaika',
      variableTimesShort: 'Sop.aika vaihtelee',
      present: 'Läsnä',
      yesterday: 'eilen',
      tomorrow: 'huomenna'
    },
    confirmedDays: {
      noChildren: 'Ei lapsia',
      tomorrow: 'Huomenna',
      inOtherUnit: 'Muussa yksikössä',
      status: {
        ABSENT: 'Poissa',
        ON_TERM_BREAK: 'Ei toimintaa'
      },
      noHolidayReservation: 'Lomavaraus puuttuu'
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
        groupNotesHeader: 'Koko ryhmän muistiinpanot'
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
    staff: {
      errors: {
        employeeNotFound: 'Työntekijää ei löytynyt',
        wrongPin: 'Väärä PIN-koodi'
      },
      previousDays: 'Aiemmat kirjaukset',
      continuationAttendance: '* edellisenä päivänä alkanut kirjaus',
      editContinuationAttendance: 'Siirry muokkaamaan',
      absent: 'Poissa',
      externalPerson: 'Muu henkilö',
      markExternalPerson: 'Kirjaa muu henkilö',
      markExternalPersonTitle: 'Kirjaa muu työntekijä sisään',
      markArrived: 'Kirjaudu läsnäolevaksi',
      markDeparted: 'Kirjaudu poissaolevaksi',
      loginWithPin: 'Kirjaudu PIN-koodilla',
      pinNotSet: 'Aseta itsellesi PIN-koodi',
      pinLocked: 'Vaihda lukkiutunut PIN-koodi',
      plannedAttendance: 'Työvuoro tänään',
      differenceReason: 'Kirjaa syy tarvittaessa',
      differenceInfo: 'Aika poikkeaa työvuorostasi',
      hasFutureAttendance:
        'Sinulla on tulevaisuuteen merkittu läsnäolo, joten et voi kirjautua läsnäolevaksi.',
      summary: 'Yhteenveto',
      plan: 'Työvuoro',
      realization: 'Toteuma',
      rows: 'Päivän kirjaukset',
      noRows: 'Ei kirjauksia',
      open: 'Avoin',
      validationErrors: {
        required: 'Pakollinen',
        timeFormat: 'Tarkista',
        future: 'Tulevaisuudessa',
        overlap: 'Päällekkäisiä merkintöjä',
        dateTooEarly: 'Tarkista',
        dateTooLate: 'Tarkista'
      },
      add: '+ Lisää uusi kirjaus'
    },
    timeDiffTooBigNotification:
      'Voit tehdä sisäänkirjauksen +/- 30 min päähän nykyhetkestä. Kirjauksia voi tarvittaessa muokata työpöytäselaimen kautta.',
    departureCannotBeDoneInFuture:
      'Työvuoron uloskirjausta ei voi merkitä ennakkoon.',
    arrivalIsBeforeDeparture: (departure: string) =>
      `Annettu aika on ennen edellistä lähtöaikaa ${departure}`,
    departureIsBeforeArrival: (arrival: string) =>
      `Annettu aika on ennen viimeisintä saapumisaikaa ${arrival}`,
    confirmAttendanceChangeCancel:
      'Haluatko varmasti peruuttaa viimeisimmän lähtö- tai saapumismerkinnän?',
    notOperationalDate:
      'Yksikköön ei voi kirjautua läsnäolevaksi, koska yksikkö on suljettu.'
  },
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
    additionalInfo: 'Lisätiedot',
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
        },
        disclaimer:
          'Kuvan tallennuksessa on lyhyt viive, jonka aikana kuva ei näy. Kuva alkaa näkyä viimeistään noin minuutti tallennuksen jälkeen.'
      }
    }
  },
  staff: {
    title: 'Henkilökunnan määrä tänään',
    daycareResponsible: 'Kasvatusvastuullisia',
    staffOccupancyEffect: 'Olen kasvatusvastuullinen',
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
    tabs: {
      received: 'Saapuneet',
      sent: 'Lähetetyt',
      drafts: 'Luonnokset'
    },
    inputPlaceholder: 'Kirjoita...',
    newMessage: 'Uusi viesti',
    thread: {
      reply: 'Vastaa viestiin'
    },
    draft: 'Luonnos',
    draftReply: '- Luonnos -',
    messageEditor: {
      newMessage: 'Uusi viesti',
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
      urgent: {
        heading: 'Merkitse kiireelliseksi',
        info: 'Lähetä viesti kiireellisenä vain, jos haluat että huoltaja lukee sen työpäivän aikana.',
        label: 'Kiireellinen'
      },
      sender: 'Lähettäjä',
      receivers: 'Vastaanottajat',
      recipientsPlaceholder: 'Valitse...',
      subject: {
        heading: 'Otsikko',
        placeholder: 'Kirjoita...'
      },
      message: {
        heading: 'Viesti',
        placeholder: 'Viestin sisältö...'
      },
      deleteDraft: 'Hylkää luonnos',
      send: 'Lähetä',
      sending: 'Lähetetään',
      recipientCount: 'Vastaanottajia',
      manyRecipientsWarning: {
        title: 'Viestillä on suuri määrä vastaanottajia.',
        text: (count: number) =>
          `Tämä viesti on lähdössä ${count} vastaanottajalle. Oletko varma, että haluat lähettää viestin?`
      }
    },
    emptyInbox: 'Viestilaatikkosi on tyhjä',
    noSentMessages: 'Ei lähetettyjä viestejä',
    noDrafts: 'Ei luonnoksia',
    unreadMessages: 'Uudet viestit',
    openPinLock: 'Avaa lukitus',
    pinLockInfo:
      'Lukeaksesi viestit sinun tulee avata lukitus PIN-koodilla. Voit lukea vain oman ryhmäsi viestit.',
    noAccountAccess:
      'Viestejä ei voi näyttää, koska sinua ei ole luvitettu ryhmään. Pyydä lupa esimieheltäsi.',
    noReceivers: 'Vastaanottajalle ei voi lähettää viestiä'
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
      text1: 'Syötä eVakasta saatava 12-merkkinen koodi kenttään alla.',
      text2: 'Syötä alla oleva vahvistuskoodi eVakaan.',
      title1: 'eVaka-mobiilin käyttöönotto, vaihe 1/3',
      title2: 'eVaka-mobiilin käyttöönotto, vaihe 2/3',
      title3: 'Tervetuloa käyttämään eVaka-mobiilia!',
      text3: 'eVaka-mobiili on nyt käytössä tässä laitteessa.',
      text4:
        'Turvataksesi lasten tiedot muistathan asettaa laitteeseen pääsykoodin, jos et ole sitä vielä tehnyt.'
    },
    emptyList: (status: 'COMING' | 'ABSENT' | 'PRESENT' | 'DEPARTED') => {
      const statusText = (() => {
        switch (status) {
          case 'COMING':
            return 'tulossa olevia'
          case 'ABSENT':
            return 'poissaolevia'
          case 'PRESENT':
            return 'läsnäolevia'
          case 'DEPARTED':
            return 'lähteneitä'
        }
      })()
      return `Ei ${statusText} lapsia`
    }
  },
  settings: {
    notifications: {
      title: 'Ilmoitusasetukset',
      permission: {
        label: 'Ilmoitukset',
        enable: 'Ota käyttöön',
        state: {
          unsupported: 'Puhelin tai selain ei tue ilmoituksia',
          granted: 'Käytössä',
          prompt: 'Ei käytössä',
          denied: 'Estetty'
        },
        info: {
          unsupported:
            'Ilmoitukset eivät toimi tällä puhelimella, tai käytössä olevalla selaimen versiolla. Ongelma saattaa korjaantua päivittämällä selain.',
          denied:
            'Ilmoitukset on estetty puhelimen asetuksissa. Asia voi korjaantua muuttamalla puhelimen tai selaimen ilmoitusasetuksia.'
        }
      },
      categories: {
        label: 'Aiheet, joista lähetetään ilmoitus tähän puhelimeen',
        values: {
          RECEIVED_MESSAGE: 'Saapuneet viestit'
        }
      },
      groups: {
        label: 'Ryhmät, joita koskevista asioista lähetetään ilmoitus'
      }
    }
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
        FILE_TOO_LARGE: 'Liian suuri tiedosto (max. 25 MB)',
        SERVER_ERROR: 'Lataus ei onnistunut'
      },
      input: {
        title: 'Lisää liite',
        text: [
          'Paina tästä tai raahaa liite laatikkoon yksi kerrallaan.',
          'Tiedoston maksimikoko: 25 MB.',
          'Sallitut tiedostomuodot:',
          'PDF, JPEG/JPG, PNG ja DOC/DOCX'
        ]
      },
      deleteFile: 'Poista tiedosto'
    },
    download: {
      modalHeader: 'Tiedoston käsittely on kesken',
      modalMessage:
        'Tiedosto ei ole juuri nyt avattavissa. Kokeile hetken kuluttua uudelleen.',
      modalClose: 'Sulje'
    }
  },
  units: {
    title: 'Yksiköt',
    children: 'Lapset',
    staff: 'Henkilökunta',
    utilization: 'Käyttöaste',
    description:
      'Yksiköidesi henkilökunnan ja lasten määrä sekä käyttöaste tällä hetkellä.'
  },
  components
}
