// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import components from '../../components/i18n/sv'

import type { fi } from './fi'

export const sv: typeof fi = {
  common: {
    yesIDo: 'Ja',
    noIDoNot: 'Nej',
    loadingFailed: 'Det gick inte att hämta data',
    noAccess: 'Rättigheter saknas',
    endpointDisabled:
      'eVaka genomgår för närvarande ett partiellt underhållsavbrott. Vissa funktioner är inte tillgängliga just nu. Försök igen om en stund.',
    add: 'Lägg till',
    cancel: 'Ångra',
    confirm: 'Bekräfta',
    sort: 'Ordna',
    all: 'Alla',
    statuses: {
      active: 'Aktiv',
      coming: 'På väg',
      completed: 'Avslutad',
      conflict: 'Konflikt'
    },
    types: {
      CLUB: 'Klubb',
      FAMILY: 'Familjedagvård',
      GROUP_FAMILY: 'Gruppfamiljedagvård',
      CENTRE: 'Daghem',
      PRESCHOOL: 'Förskoleundervisning',
      DAYCARE: 'Småbarnspedagogik',
      PRESCHOOL_DAYCARE:
        'Småbarnspedagogik i anslutning till förskoleundervisning',
      PREPARATORY_EDUCATION: 'Förberedande förskoleundervisning',
      PREPARATORY_DAYCARE:
        'Småbarnspedagogik i anslutning till förskoleundervisning',
      DAYCARE_5YO_FREE: 'Avgiftsfri småbarnspedagogik för 5-åringar',
      DAYCARE_5YO_PAID: 'Småbarnspedagogik (avgiftsbelagd)'
    },
    placement: {
      CLUB: 'Klubb',
      DAYCARE: 'Småbarnspedagogik',
      DAYCARE_PART_TIME: 'Småbarnspedagogik på deltid',
      DAYCARE_FIVE_YEAR_OLDS: 'Småbarnspedagogik för 5-åringar',
      DAYCARE_PART_TIME_FIVE_YEAR_OLDS:
        'Småbarnspedagogik på deltid för 5-åringar',
      PRESCHOOL: 'Förskoleundervisning',
      PRESCHOOL_DAYCARE:
        'Småbarnspedagogik i anslutning till förskoleundervisning',
      PRESCHOOL_DAYCARE_ONLY: 'Enbart anslutande',
      PRESCHOOL_CLUB: 'Klubb i anslutning till förskoleundervisning',
      PREPARATORY: 'Förberedande',
      PREPARATORY_DAYCARE: 'Förberedande',
      PREPARATORY_DAYCARE_ONLY: 'Endast anslutande',
      TEMPORARY_DAYCARE: 'Tillfällig',
      TEMPORARY_DAYCARE_PART_DAY: 'Tillfällig deltid',
      SCHOOL_SHIFT_CARE: 'Skiftvård för skolelever'
    },
    code: 'Kod',
    children: 'Barn',
    name: 'Namn',
    staff: 'Personal',
    messages: 'Meddelanden',
    settings: 'Inställningar',
    back: 'Tillbaka',
    return: 'Tillbaka',
    close: 'Stäng',
    open: 'Öppna',
    hours: 'Timmar',
    remove: 'Radera',
    doNotRemove: 'Radera inte',
    clear: 'Töm',
    edit: 'Redigera',
    save: 'Spara',
    saveChanges: 'Spara ändringar',
    saving: 'Sparas',
    saved: 'Sparad',
    search: 'Sök',
    noResults: 'Inga sökresultat',
    doNotSave: 'Spara inte',
    starts: 'Börjar',
    ends: 'Slutar',
    information: 'Uppgifter',
    dailyNotes: 'Anteckningar',
    saveBeforeClosing: 'Vill du spara före stängning?',
    hourShort: 'timmar',
    minuteShort: 'minuter',
    week: 'Vecka',
    yearsShort: 'veckor',
    errors: {
      minutes: 'Högst 59 minuter'
    },
    child: 'Barn',
    group: 'Grupp',
    yesterday: 'igår',
    validation: {
      dateLte: (date: string) => `Måste vara ${date} eller tidigare`,
      dateBetween: (start: string, end: string) =>
        `Måste vara mellan ${start}-${end}`
    },
    nb: 'Obs!',
    validity: 'Giltighet',
    lastModified: (dateTime: string) => `Senast redigerad: ${dateTime}`,
    validTo: (date: string) => `Gäller till ${date}`,
    lastName: 'Efternamn',
    firstName: 'Förnamn',
    openExpandingInfo: 'Öppna fält för mer information'
  },
  systemNotification: {
    title: 'Avisering'
  },
  errorPage: {
    reload: 'Ladda ner sidan igen',
    text: 'Det uppstod ett oväntat problem. Felinformationen har vidarebefordrats.',
    title: 'Något gick snett'
  },
  absences: {
    title: 'Frånvaroanteckning',
    absenceTypes: {
      OTHER_ABSENCE: 'Frånvaro',
      SICKLEAVE: 'Sjukfrånvaro',
      UNKNOWN_ABSENCE: 'Oanmäld frånvaro',
      PLANNED_ABSENCE: 'Frånvaro från skiftarbete',
      TEMPORARY_RELOCATION: 'Barnet placerat tillfälligt / reservård',
      PARENTLEAVE: 'Föräldraledighet',
      FORCE_MAJEURE: 'Avgiftsfri dag',
      FREE_ABSENCE: 'Avgiftsfri frånvaro',
      UNAUTHORIZED_ABSENCE: 'Oanmälda frånvaro från jourvård',
      NO_ABSENCE: 'Ingen frånvaro'
    },
    careTypes: {
      SCHOOL_SHIFT_CARE: 'Skiftvård för skolelever',
      PRESCHOOL: 'Förskoleundervisning',
      PRESCHOOL_DAYCARE: 'Ansluten småbarnspedagogik',
      PRESCHOOL_CLUB: 'Klubb i anslutning till förskoleundervisning',
      DAYCARE_5YO_FREE: 'Småbarnspedagogik för 5-åringar',
      DAYCARE: 'Småbarnspedagogik',
      CLUB: 'Klubb'
    },
    chooseStartDate: 'Välj ett kommande datum',
    startBeforeEnd: 'Början måste infalla före slutdatumet.',
    reason: 'Orsak till frånvaro',
    fullDayHint: 'Frånvaron ska antecknas för hela dagen',
    confirmDelete: 'Vill du radera den här frånvaron?',
    futureAbsence: 'Kommande frånvaro',
    laterAbsence: {
      closed: 'Visa kommande  frånvaro',
      open: 'Dölj kommande frånvaro'
    }
  },
  attendances: {
    views: {
      TODAY: 'Idag',
      NEXT_DAYS: 'Följande dagar'
    },
    types: {
      COMING: 'På väg',
      PRESENT: 'På plats',
      DEPARTED: 'Gått',
      ABSENT: 'Borta'
    },
    status: {
      COMING: 'På väg',
      PRESENT: 'Anlänt',
      DEPARTED: 'Gått',
      ABSENT: 'Borta'
    },
    staffTypes: {
      PRESENT: 'På plats',
      OTHER_WORK: 'Arbetsrelaterat ärende',
      TRAINING: 'Utbildning',
      OVERTIME: 'Övertidsarbete',
      JUSTIFIED_CHANGE: 'Motiverad ändring',
      SICKNESS: 'Annan orsak (egen)',
      CHILD_SICKNESS: 'Annan orsak (barn)'
    },
    groupSelectError: 'Namnet på gruppen hittades inte',
    actions: {
      multiselect: {
        toggle: 'Logga in flera barn',
        confirmArrival: (count: number) =>
          count > 1
            ? `Anteckna ${count} barn som anlända`
            : count === 1
              ? 'Anteckna 1 barn som anlänt'
              : 'Anteckna som anlänt',
        confirmDeparture: (count: number) =>
          count > 1
            ? `Anteckna ${count} barn som gått`
            : count === 1
              ? 'Anteckna 1 barn som gått'
              : 'Anteckna som gått',
        select: 'Välj',
        selected: 'Vald'
      },
      sortType: {
        CHILD_FIRST_NAME: 'Alfabetisk ordning',
        RESERVATION_START_TIME: 'Ankomstordning',
        RESERVATION_END_TIME: 'Ut, ordning'
      },
      markAbsent: 'Markera som frånvarande',
      cancelAbsence: 'Ångra frånvaro',
      markPresent: (count: number) =>
        count > 1
          ? `Anteckna ${count} barn som anlända`
          : 'Anteckna som anlänt',
      markDeparted: 'Anteckna som gått',
      returnToComing: 'Återställ till kommande',
      returnToPresent: 'Återställ till närvarande',
      or: 'eller',
      returnToPresentNoTimeNeeded:
        'Återställ till närvarande utan ny ankomsttid',
      markAbsentBeforehand: 'Kommande frånvaro',
      markReservations: 'Kommande reserveringar och frånvaro',
      confirmedRangeReservations: {
        markReservations: 'Redigera reserveringar',
        markAbsentBeforehand: 'Anteckna frånvaro'
      }
    },
    validationErrors: {
      required: 'Obligatorisk',
      timeFormat: 'Kontrollera',
      overlap: 'Överlappande anteckningar'
    },
    timeLabel: 'Anteckning',
    termBreak: 'Ingen aktivitet idag',
    departureTime: 'Utstämplingsstid',
    arrivalTime: 'Instämplingstid',
    chooseGroup: 'Välj grupp',
    chooseGroupInfo: 'Barn: Närvarande nu/I gruppen totalt',
    searchPlaceholder: 'Sök på barnets namn',
    noAbsences: 'Ingen frånvaro',
    removeAbsence: 'Ångra frånvaro',
    timeError: 'Felaktig tid',
    arrived: 'Ankomsttid',
    departed: 'Avgångstid',
    noGroup: 'Ingen grupp',
    serviceTime: {
      reservation: 'Reservering',
      reservations: 'Reserveringar',
      serviceToday: (start: string, end: string) =>
        `Tid i småbarnspedagogik i dag ${start}–${end}`,
      serviceTodayShort: (start: string, end: string) =>
        `Avtalad tid ${start}–${end}`,
      noServiceToday: 'Ingen reserverad barntid i dag',
      noServiceTodayShort: 'Ingen lämplig tid idag',
      notSet: 'Närvaroanmälan saknas',
      notSetShort: 'Anmälan saknas',
      variableTimes: 'Varierande tid i småbarnspedagogik',
      variableTimesShort: 'Lämplig tid varierar',
      present: 'På plats',
      yesterday: 'igår',
      tomorrow: 'i morgon'
    },
    confirmedDays: {
      noChildren: 'Inga barn',
      tomorrow: 'I morgon',
      inOtherUnit: 'I annan enhet',
      status: {
        ABSENT: 'Borta',
        ON_TERM_BREAK: 'Ingen aktivitet'
      },
      noHolidayReservation:
        'Reservering för behov av vård under lovperioden saknas'
    },
    notes: {
      day: 'Dag',
      dailyNotes: 'Anteckningar',
      addNew: 'Lägg till ny',
      labels: {
        stickyNote: 'Att beaktas de närmaste dagarna',
        note: 'Idag upplevt och lärt',
        feedingNote: 'Barnet åt i dag',
        sleepingNote: 'Barnet sov i dag',
        reminderNote: 'Att komma ihåg',
        groupNotesHeader: 'Gruppens anteckningar'
      },
      sleepingValues: {
        GOOD: 'Bra',
        MEDIUM: 'Lite',
        NONE: 'Inte alls'
      },
      feedingValues: {
        GOOD: 'Bra',
        MEDIUM: 'Lite',
        NONE: 'Inte alls/smakade'
      },
      reminders: {
        DIAPERS: 'Mera blöjor',
        CLOTHES: 'Mera reservkläder',
        LAUNDRY: 'Smutsiga kläder i ryggsäcken'
      },
      placeholders: {
        note: 'Lekar, framgång, glädjeämnen och lärdomar i dag (inga hälsouppgifter eller andra sekretessbelagda uppgifter).',
        childStickyNote:
          'Anteckning för personalen (inga hälsouppgifter eller andra sekretessbelagda uppgifter).',
        groupNote: 'Anteckning om hela gruppen',
        reminderNote: 'Annat att komma ihåg (t.ex. solkräm)',
        hours: 'timmar',
        minutes: 'minuter'
      },
      noNotes: 'Inga anteckningar för idag',
      clearTitle: 'Vill du rensa anteckningarna för idag?',
      confirmTitle: 'Vill du spara anteckningarna innan du stänger?',
      closeWithoutSaving: 'Stäng utan att spara',
      groupNote: 'Gruppens anteckningar',
      note: 'Idag upplevt och lärt',
      otherThings: 'Övriga ärenden',
      childStickyNotes: 'Att beaktas de närmaste dagarna'
    },
    absenceTitle: 'Frånvaroanteckningar',
    staff: {
      errors: {
        employeeNotFound: 'Arbetstagaren hittades inte',
        wrongPin: 'Felaktig PIN-kod'
      },
      previousDays: 'Tidigare anteckningar',
      nextDays: 'Kommande arbetsskift och frånvaro',
      continuationAttendance: '*registrering som började föregående dag',
      editContinuationAttendance: 'Gå till redigering',
      absent: 'Frånvaro',
      externalPerson: 'Annan person',
      markExternalPerson: 'Registrera annan person',
      markExternalPersonTitle: 'Registrera annan arbetstagare in',
      markArrived: 'Anteckna dig som närvarande',
      markDeparted: 'Anteckna dig som frånvarande',
      loginWithPin: 'Logga in med PIN-kod',
      pinNotSet: 'Ge dig själv en PIN-kod',
      pinLocked: 'Byt en PIN-kod som låst sig',
      plannedAttendance: 'Arbetsskift i dag',
      differenceReason: 'Anteckna orsak vid behov',
      differenceInfo: 'Tiden avviker från ditt arbetsskift',
      hasFutureAttendance:
        'Du har redan en kommande närvaro, så du kan inte anteckna dig som närvarande.',
      summary: 'Sammandrag',
      plan: 'Arbetsskift',
      realization: 'Förverkligande',
      rows: 'Dagens anteckningar',
      noRows: 'Inga anteckningar',
      open: 'Öppen',
      validationErrors: {
        required: 'Obligatorisk',
        timeFormat: 'Kontrollera',
        future: 'Kommande',
        overlap: 'Överlappande markeringar',
        dateTooEarly: 'Kontrollera',
        dateTooLate: 'Kontrollera'
      },
      add: '+ Lägg till ny anteckning',
      openAttendanceInAnotherUnitWarning: 'Öppen anteckning',
      openAttendanceInAnotherUnitWarningCont:
        '. Avsluta anteckning innan du lägger till en ny.',
      noPlan: 'Ingen planerad arbetstur',
      planWarnings: {
        maybeInOtherUnit: 'Arbetsturen kan vara i en annan enhet',
        maybeInOtherGroup: 'Arbetsturen kan vara i en annan grupp'
      },
      plansInfo:
        'Endast frånvaro som antecknats i arbetsturssystemet syns på listan.',
      staffMemberPlanInfo:
        'Innehåller endast frånvaro som planerats i arbetstursystemet.',
      staffMemberMultipleUnits:
        'Arbetsturer som syns för den här personen kan vara i en annan enhet'
    },
    timeDiffTooBigNotification:
      'Du kan logga in +/- 30 minuter från nu. Vid behov kan anteckningar ändras via webbläsaren.',
    departureCannotBeDoneInFuture: 'Utloggning kan inte göras på förhand.',
    arrivalIsBeforeDeparture: (departure: string) =>
      `Den angivna tiden infaller före föregående avfärdstid ${departure}`,
    departureIsBeforeArrival: (arrival: string) =>
      `Den angivna tiden infaller före den senaste ankomsttiden ${arrival}`,
    confirmAttendanceChangeCancel:
      'Är du säker att du vill ångra den senaste ut- eller inloggningen?',
    notOperationalDate:
      'Du kan inte logga in på enheten, eftersom den är stängd.'
  },
  childInfo: {
    header: 'Uppgifter om barnet',
    personalInfoHeader: 'Personuppgifter om barnet',
    childName: 'Barnets namn',
    preferredName: 'Tilltalsnamn',
    dateOfBirth: 'Födelsedatum',
    address: 'Barnets hemadress',
    type: 'Typ av placering',
    otherInfoHeader: 'Övriga uppgifter',
    allergies: 'Allergier',
    diet: 'Kost',
    medication: 'Medicinering',
    additionalInfo: 'Mer information',
    contactInfoHeader: 'Kontaktuppgifter',
    contact: 'Kontaktperson',
    name: 'Namn',
    phone: 'Telefonnummer',
    backupPhone: 'Reservtelefonnummer',
    email: 'E-postadress',
    backupPickup: 'Reservhämtare',
    backupPickupName: 'Reservhämtarens namn',
    image: {
      modalMenu: {
        title: 'Barnets profilbild',
        takeImageButton: 'Välj bild',
        deleteImageButton: 'Radera bild',
        deleteConfirm: {
          title: 'Är du säker att du vill radera bilden av barnet?',
          resolve: 'Ta bort bild',
          reject: 'Radera inte'
        },
        disclaimer:
          'Den nerladdade bilden visas med liten fördröjning. Bilden visas senast cirka en minut efter att den sparats.'
      }
    },
    showSensitiveInfo: 'Visa känslig information',
    noGuardians: 'Vårdnadshavarna kan inte nås via eVaka'
  },
  staff: {
    title: 'Personalstyrka i dag',
    daycareResponsible: 'Vårdansvarig personal',
    staffOccupancyEffect: 'Jag har vårdansvar',
    other: 'Andra (t.ex. assistenter, studerande, speciallärare)',
    cancel: 'Återta redigering',
    realizedGroupOccupancy: 'Gruppens belastningsgrad idag',
    realizedUnitOccupancy: 'Enhetens belastningsgrad i dag',
    notUpdated: 'Uppgifterna har inte uppdaterats',
    updatedToday: 'Uppgifterna har uppdaterats i dag',
    updated: 'Uppgifterna har uppdaterats'
  },
  pin: {
    header: 'Upplåsning',
    info: 'Skriv PIN-kod för att öppna uppgifterna om barnet',
    selectStaff: 'Välj användare',
    staff: 'Användare',
    noOptions: 'Inga alternativ',
    pinCode: 'PIN-kod',
    status: {
      SUCCESS: 'Rätt PIN-kod',
      WRONG_PIN: 'Felaktig PIN-kod',
      PIN_LOCKED: 'PIN-koden är låst',
      NOT_FOUND: 'Okänd användare'
    },
    unknownError: 'Okänt fel',
    logOut: 'Logga ut',
    login: 'Logga in',
    loggedIn: 'Inloggad'
  },
  messages: {
    tabs: {
      received: 'Inkomna',
      sent: 'Skickade',
      drafts: 'Utkast'
    },
    inputPlaceholder: 'Skriv...',
    newMessage: 'Nytt meddelande',
    thread: {
      reply: 'Svara på meddelandet'
    },
    draft: 'Utkast',
    draftReply: '- Utkast -',
    messageEditor: {
      newMessage: 'Nytt meddelande',
      to: {
        label: 'Mottagare',
        placeholder: 'Välj grupp',
        noOptions: 'Inga grupper'
      },
      type: {
        label: 'Typ av meddelande',
        message: 'Meddelande',
        bulletin: 'Nyhetsbrev (mottagaren kan inte svara)'
      },
      urgent: {
        heading: 'Markera som brådskande',
        info: 'Skicka som brådskande bara om vårdnadshavaren ska läsa det under arbetsdagen.',
        label: 'Brådskande'
      },
      sender: 'Avsändare',
      receivers: 'Mottagare',
      recipientsPlaceholder: 'Välj...',
      starters: 'barn som börjar',
      subject: {
        heading: 'Rubrik',
        placeholder: 'Skriv...'
      },
      message: {
        heading: 'Meddelande',
        placeholder: 'Meddelandets innehåll...'
      },
      deleteDraft: 'Förkasta utkast',
      send: 'Skicka',
      sending: 'Skickas',
      recipientCount: 'Mottagare',
      manyRecipientsWarning: {
        title: (count: number) => `Meddelandet har ${count} mottagare.`,
        text: (count: number) =>
          `Det här meddelandet är på väg att skickas till ${count} mottagare. Är du säker att du vill sända meddelandet?`
      }
    },
    emptyInbox: 'Din postlåda är tom',
    noSentMessages: 'Inga skickade meddelanden',
    noDrafts: 'Inga utkast',
    unreadMessages: 'Nya meddelanden',
    openPinLock: 'Lås upp',
    pinLockInfo:
      'För att läsa meddelanden måste du öppna låset med PIN-kod. Du kan bara läsa meddelanden inom din egen grupp.',
    noAccountAccess:
      'Det går inte att visa meddelanden eftersom du inte har tillstånd till den här gruppen. Be din chef om tillstånd.',
    noRecipients: 'Det går inte att skicka ett meddelande till mottagaren'
  },
  mobile: {
    landerText1:
      'Välkommen att använda Esbo stads mobilapp för småbarnspedagogik!',
    landerText2:
      'För att aktivera appen, välj ”Lägg till apparat” nedan och registrera mobiltelefonen i eVaka på din enhets sida.',
    actions: {
      ADD_DEVICE: 'Lägg till apparat',
      START: 'Då börjar vi'
    },
    wizard: {
      text1: 'Skriv en 6-siffrig kod som du får från eVaka i fältet nedan.',
      text2: 'Skriv verifieringskoden nedan i eVaka.',
      title1: 'Ibruktagande av eVaka-mobilen, steg 1/3',
      title2: 'Ibruktagande av eVaka-mobilen, steg 2/3',
      title3: 'Välkommen att använda eVaka-mobilen!',
      text3: 'eVaka-mobilen är nu i bruk på den här apparaten.',
      text4:
        'För att skydda barnens uppgifter ska du ange en åtkomstkod i din apparat, om du inte redan har gjort det.'
    },
    emptyList: (status: 'COMING' | 'ABSENT' | 'PRESENT' | 'DEPARTED') => {
      const statusText = (() => {
        switch (status) {
          case 'COMING':
            return 'på kommande'
          case 'ABSENT':
            return 'frånvarande'
          case 'PRESENT':
            return 'närvarande'
          case 'DEPARTED':
            return 'som gått'
        }
      })()
      return `Inga barn ${statusText}`
    }
  },
  settings: {
    language: {
      title: 'Språk',
      fi: 'Suomi',
      sv: 'Svenska'
    },
    notifications: {
      title: 'Inställningar för meddelanden',
      permission: {
        label: 'Aviseringar',
        enable: 'Aktivera',
        state: {
          unsupported: 'Aviseringar stöds inte av telefonen eller webbläsare',
          granted: 'I bruk',
          prompt: 'Ur bruk',
          denied: 'Blockerad'
        },
        info: {
          unsupported:
            'Aviseringar fungerar inte med den här telefonen eller webbläsarversionen. Uppgradera webbläsaren så löses kanske problemet.',
          denied:
            'Aviseringar har blockerats i telefonens inställningar. Ändra aviseringsinställningar för telefonen eller webbläsaren, så löses kanske problemet.'
        }
      },
      categories: {
        label: 'Ämnen som ska aviseras till den här telefonen',
        values: {
          RECEIVED_MESSAGE: 'Inkomna meddelanden',
          NEW_ABSENCE: 'Frånvaroanteckningar för barn i dag',
          CALENDAR_EVENT_RESERVATION: 'Bokade och avbokade samtalstider'
        }
      },
      groups: {
        label: 'Grupper, vars ärenden aviseras'
      }
    }
  },
  childButtons: {
    messages: 'Meddelanden'
  },
  fileUpload: {
    upload: {
      loading: 'Laddar...',
      loaded: 'Laddad',
      error: {
        EXTENSION_MISSING: 'Filändelse saknas',
        EXTENSION_INVALID: 'Ogiltig filändelse',
        INVALID_CONTENT_TYPE: 'Ogiltigt filformat',
        FILE_TOO_LARGE: 'För stor fil (max. 25 MB)',
        SERVER_ERROR: 'Det gick inte att ladda ner'
      },
      input: {
        title: 'Lägg till en bilaga',
        text: [
          'Tryck här eller dra en bilaga åt gången till lådan.',
          'Filens maximala storlek: 25 MB.',
          'Tillåtna filformat:',
          'PDF, JPEG/JPG, PNG och DOC/DOCX'
        ]
      },
      deleteFile: 'Ta bort fil'
    },
    download: {
      modalHeader: 'Behandlingen av filen pågår fortfarande',
      modalMessage:
        'Filen är inte tillgänglig just nu. Försök på nytt om en stund.',
      modalClose: 'Stäng'
    }
  },
  units: {
    title: 'Enheter',
    children: 'Barn',
    staff: 'Personal',
    utilization: 'Närvarograd',
    description:
      'Personalstyrka och antal barn i enheterna samt närvarograden för närvarande.'
  },
  components
}
