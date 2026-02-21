// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import components from '../../components/i18n/sv'

import type { fi } from './fi'

export const sv: typeof fi = {
  common: {
    yesIDo: 'Ja',
    noIDoNot: 'Nej',
    loadingFailed: 'Hämtning av information misslyckades',
    noAccess: 'Behörighet saknas',
    add: 'Lägg till',
    cancel: 'Avbryt',
    confirm: 'Bekräfta',
    sort: 'Sortera',
    all: 'Alla',
    statuses: {
      active: 'Aktiv',
      coming: 'Kommande',
      completed: 'Avslutad',
      conflict: 'Konflikt'
    },
    types: {
      CLUB: 'Klubbverksamhet',
      FAMILY: 'Familjedagvård',
      GROUP_FAMILY: 'Gruppfamiljedagvård',
      CENTRE: 'Daghem',
      PRESCHOOL: 'Förskola',
      DAYCARE: 'Småbarnspedagogik',
      PRESCHOOL_DAYCARE: 'Kompletterande småbarnspedagogik',
      PREPARATORY_EDUCATION: 'Förberedande förskola',
      PREPARATORY_DAYCARE: 'Kompletterande småbarnspedagogik',
      DAYCARE_5YO_FREE: 'Avgiftsfri småbarnspedagogik för 5-åringar',
      DAYCARE_5YO_PAID: 'Småbarnspedagogik (avgiftsbelagd)'
    },
    placement: {
      CLUB: 'Klubbverksamhet',
      DAYCARE: 'Småbarnspedagogik',
      DAYCARE_PART_TIME: 'Halvdags småbarnspedagogik',
      DAYCARE_FIVE_YEAR_OLDS: 'Småbarnspedagogik för 5-åringar',
      DAYCARE_PART_TIME_FIVE_YEAR_OLDS:
        'Halvdags småbarnspedagogik för 5-åringar',
      PRESCHOOL: 'Förskola',
      PRESCHOOL_DAYCARE: 'Kompletterande småbarnspedagogik',
      PRESCHOOL_DAYCARE_ONLY: 'Enbart kompletterande',
      PRESCHOOL_CLUB: 'Förskoleklubb',
      PREPARATORY: 'Förberedande',
      PREPARATORY_DAYCARE: 'Förberedande',
      PREPARATORY_DAYCARE_ONLY: 'Enbart kompletterande',
      TEMPORARY_DAYCARE: 'Tillfällig',
      TEMPORARY_DAYCARE_PART_DAY: 'Tillfällig deltid',
      SCHOOL_SHIFT_CARE: 'Skiftesomvårdnad för skolbarn'
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
    remove: 'Ta bort',
    doNotRemove: 'Ta inte bort',
    clear: 'Rensa',
    edit: 'Redigera',
    save: 'Spara',
    saveChanges: 'Spara ändringar',
    saving: 'Sparar',
    saved: 'Sparat',
    search: 'Sök',
    noResults: 'Inga sökresultat',
    doNotSave: 'Spara inte',
    starts: 'Börjar',
    ends: 'Slutar',
    information: 'Information',
    dailyNotes: 'Anteckningar',
    saveBeforeClosing: 'Vill du spara innan du stänger',
    hourShort: 'h',
    minuteShort: 'min',
    week: 'Vecka',
    yearsShort: 'år',
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
    nb: 'Obs',
    validity: 'Giltighet',
    lastModified: (dateTime: string) => `Senast ändrad: ${dateTime}`,
    validTo: (date: string) => `Giltig till ${date}`,
    lastName: 'Efternamn',
    firstName: 'Förnamn',
    openExpandingInfo: 'Öppna detaljer'
  },
  systemNotification: {
    title: 'Meddelande'
  },
  errorPage: {
    reload: 'Ladda om sidan',
    text: 'Vi stötte på ett oväntat problem. Felinformationen har vidarebefordrats.',
    title: 'Något gick fel'
  },
  absences: {
    title: 'Frånvaromarkering',
    absenceTypes: {
      OTHER_ABSENCE: 'Frånvaro',
      SICKLEAVE: 'Sjukdom',
      UNKNOWN_ABSENCE: 'Oanmäld frånvaro',
      PLANNED_ABSENCE: 'Skiftesarbetsfrånvaro',
      TEMPORARY_RELOCATION: 'Barnet ersättningsplacerat på annat ställe',
      PARENTLEAVE: 'Föräldraledighet',
      FORCE_MAJEURE: 'Avgiftsfri dag',
      FREE_ABSENCE: 'Avgiftsfri frånvaro',
      UNAUTHORIZED_ABSENCE: 'Oanmäld jourfrånvaro',
      NO_ABSENCE: 'Ingen frånvaro'
    },
    careTypes: {
      SCHOOL_SHIFT_CARE: 'Skiftesomvårdnad för skolbarn',
      PRESCHOOL: 'Förskola',
      PRESCHOOL_DAYCARE: 'Kompletterande småbarnspedagogik',
      PRESCHOOL_CLUB: 'Förskoleklubb',
      DAYCARE_5YO_FREE: 'Småbarnspedagogik för 5-åringar',
      DAYCARE: 'Småbarnspedagogik',
      CLUB: 'Klubbverksamhet'
    },
    chooseStartDate: 'Välj ett kommande datum',
    startBeforeEnd: 'Startdatum måste vara före slutdatum.',
    reason: 'Orsak till frånvaro',
    fullDayHint: 'Frånvaromarkeringen gäller hela dagen',
    confirmDelete: 'Vill du ta bort denna frånvaro?',
    futureAbsence: 'Kommande frånvaron',
    laterAbsence: {
      closed: 'Visa senare frånvaron',
      open: 'Dölj senare frånvaron'
    }
  },
  attendances: {
    views: {
      TODAY: 'Idag',
      NEXT_DAYS: 'Följande dagar'
    },
    types: {
      COMING: 'Kommande',
      PRESENT: 'Närvarande',
      DEPARTED: 'Gått',
      ABSENT: 'Frånvarande'
    },
    status: {
      COMING: 'Kommande',
      PRESENT: 'Anlänt',
      DEPARTED: 'Gått',
      ABSENT: 'Frånvarande'
    },
    staffTypes: {
      PRESENT: 'På plats',
      OTHER_WORK: 'Arbetsärende',
      TRAINING: 'Utbildning',
      OVERTIME: 'Övertid',
      JUSTIFIED_CHANGE: 'Motiverad ändring',
      SICKNESS: 'Annan orsak (egen)',
      CHILD_SICKNESS: 'Annan orsak (barn)'
    },
    groupSelectError: 'Den valda gruppens namn hittades inte',
    actions: {
      multiselect: {
        toggle: 'Registrera flera barn',
        confirmArrival: (count: number) =>
          `Markera som anlänt${
            count > 1 ? `: ${count} barn` : count === 1 ? ': 1 barn' : ''
          }`,
        confirmDeparture: (count: number) =>
          `Markera som gått${
            count > 1 ? `: ${count} barn` : count === 1 ? ': 1 barn' : ''
          }`,
        select: 'Välj',
        selected: 'Vald'
      },
      sortType: {
        CHILD_FIRST_NAME: 'Alfabetisk ordning',
        RESERVATION_START_TIME: 'Ankomstordning',
        RESERVATION_END_TIME: 'Avreseordning'
      },
      markAbsent: 'Markera som frånvarande',
      cancelAbsence: 'Ångra frånvaro',
      markPresent: (count: number) =>
        count > 1 ? `Markera ${count} barn som anlända` : 'Markera som anlänt',
      markDeparted: 'Markera som gått',
      returnToComing: 'Flytta tillbaka till kommande',
      returnToPresent: 'Flytta tillbaka till närvarande',
      or: 'eller',
      returnToPresentNoTimeNeeded:
        'Flytta tillbaka till närvarande utan ny ankomsttid',
      markAbsentBeforehand: 'Kommande frånvaron',
      markReservations: 'Kommande reservationer och frånvaron',
      confirmedRangeReservations: {
        markReservations: 'Redigera reservationer',
        markAbsentBeforehand: 'Markera frånvaro'
      }
    },
    validationErrors: {
      required: 'Obligatorisk',
      timeFormat: 'Kontrollera',
      overlap: 'Överlappande markeringar'
    },
    timeLabel: 'Markering',
    termBreak: 'Ingen verksamhet idag',
    departureTime: 'Avgångstid',
    arrivalTime: 'Ankomsttid',
    chooseGroup: 'Välj grupp',
    chooseGroupInfo: 'Barn: Närvarande nu/Totalt i gruppen',
    searchPlaceholder: 'Sök med barnets namn',
    noAbsences: 'Inga frånvaron',
    removeAbsence: 'Ångra frånvaro',
    timeError: 'Felaktig tid',
    arrived: 'Anlände',
    departed: 'Gick',
    noGroup: 'Ingen grupp',
    serviceTime: {
      reservation: 'Reservation',
      reservations: 'Reservationer',
      serviceToday: (start: string, end: string) =>
        `Småbarnspedagogiktid idag ${start}-${end}`,
      serviceTodayShort: (start: string, end: string) =>
        `Avtalstid ${start}-${end}`,
      noServiceToday: 'Ingen reserverad småbarnspedagogiktid idag',
      noServiceTodayShort: 'Ingen avtalstid idag',
      notSet: 'Närvaroanmälan saknas',
      notSetShort: 'Anmälan saknas',
      variableTimes: 'Varierande småbarnspedagogiktid',
      variableTimesShort: 'Avtalstiden varierar',
      present: 'Närvarande',
      yesterday: 'igår',
      tomorrow: 'imorgon'
    },
    confirmedDays: {
      noChildren: 'Inga barn',
      tomorrow: 'Imorgon',
      inOtherUnit: 'I en annan enhet',
      status: {
        ABSENT: 'Frånvarande',
        ON_TERM_BREAK: 'Ingen verksamhet'
      },
      noHolidayReservation: 'Semesterreservation saknas'
    },
    notes: {
      day: 'Dag',
      dailyNotes: 'Anteckningar',
      addNew: 'Lägg till ny',
      labels: {
        stickyNote: 'Att observera de närmaste dagarna',
        note: 'Dagens upplevelser och lärdomar',
        feedingNote: 'Barnet åt idag',
        sleepingNote: 'Barnet sov idag',
        reminderNote: 'Saker att komma ihåg',
        groupNotesHeader: 'Anteckningar för hela gruppen'
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
        DIAPERS: 'Fler blöjor',
        CLOTHES: 'Fler ombyteskläder',
        LAUNDRY: 'Tvätt i ryggsäcken'
      },
      placeholders: {
        note: 'Lekar, framgångar, glädje och lärdomar idag (inte hälsouppgifter eller sekretessbelagda uppgifter).',
        childStickyNote:
          'Anteckning för personalen (inte hälsouppgifter eller sekretessbelagda uppgifter).',
        groupNote: 'Anteckning som gäller hela gruppen',
        reminderNote: 'Annat att komma ihåg (t.ex. solkräm)',
        hours: 'timmar',
        minutes: 'minuter'
      },
      noNotes: 'Inga anteckningar för denna dag',
      clearTitle: 'Vill du rensa anteckningarna för denna dag?',
      confirmTitle: 'Vill du spara anteckningarna innan du stänger?',
      closeWithoutSaving: 'Stäng utan att spara',
      groupNote: 'Gruppanteckningar',
      note: 'Dagens upplevelser och lärdomar',
      otherThings: 'Övrigt',
      childStickyNotes: 'Att observera de närmaste dagarna'
    },
    absenceTitle: 'Frånvaromarkeringar',
    staff: {
      errors: {
        employeeNotFound: 'Arbetstagaren hittades inte',
        wrongPin: 'Fel PIN-kod'
      },
      previousDays: 'Tidigare registreringar',
      nextDays: 'Kommande arbetsskift och frånvaron',
      continuationAttendance: '* registrering som påbörjats föregående dag',
      editContinuationAttendance: 'Gå till redigering',
      absent: 'Frånvarande',
      externalPerson: 'Annan person',
      markExternalPerson: 'Registrera annan person',
      markExternalPersonTitle: 'Registrera annan arbetstagare som närvarande',
      markArrived: 'Registrera dig som närvarande',
      markDeparted: 'Registrera dig som frånvarande',
      loginWithPin: 'Logga in med PIN-kod',
      pinNotSet: 'Ange en PIN-kod för dig själv',
      pinLocked: 'Byt låst PIN-kod',
      plannedAttendance: 'Arbetsskift idag',
      differenceReason: 'Ange orsak vid behov',
      differenceInfo: 'Tiden avviker från ditt arbetsskift',
      hasFutureAttendance:
        'Du har en framtida närvaroregistrering, så du kan inte registrera dig som närvarande.',
      summary: 'Sammandrag',
      plan: 'Arbetsskift',
      realization: 'Utfall',
      rows: 'Dagens registreringar',
      noRows: 'Inga registreringar',
      open: 'Öppen',
      validationErrors: {
        required: 'Obligatorisk',
        timeFormat: 'Kontrollera',
        future: 'I framtiden',
        overlap: 'Överlappande markeringar',
        dateTooEarly: 'Kontrollera',
        dateTooLate: 'Kontrollera'
      },
      add: '+ Lägg till ny registrering',
      openAttendanceInAnotherUnitWarning: 'Öppen registrering ',
      openAttendanceInAnotherUnitWarningCont:
        '. Registreringen måste avslutas innan en ny kan läggas till.',
      noPlan: 'Inget planerat arbetsskift',
      planWarnings: {
        maybeInOtherUnit: 'Arbetsskiftet kan vara i en annan enhet',
        maybeInOtherGroup: 'Arbetsskiftet kan vara i en annan grupp'
      },
      plansInfo:
        'Enbart frånvaron registrerade i skiftplaneringssystemet visas i listan.',
      staffMemberPlanInfo:
        'Innehåller enbart frånvaron planerade i skiftplaneringssystemet.',
      staffMemberMultipleUnits:
        'Arbetsskiften som visas för denna person kan vara i en annan enhet'
    },
    timeDiffTooBigNotification:
      'Du kan göra en registrering +/- 30 min från nuvarande tid. Registreringar kan vid behov redigeras via skrivbordsversionen.',
    departureCannotBeDoneInFuture:
      'Utcheckning från arbetsskiftet kan inte registreras i förväg.',
    arrivalIsBeforeDeparture: (departure: string) =>
      `Angiven tid är före föregående avgångstid ${departure}`,
    departureIsBeforeArrival: (arrival: string) =>
      `Angiven tid är före senaste ankomsttid ${arrival}`,
    confirmAttendanceChangeCancel:
      'Vill du verkligen ångra den senaste avgångs- eller ankomstmarkeringen?',
    notOperationalDate:
      'Du kan inte registrera dig som närvarande i enheten, eftersom enheten är stängd.'
  },
  childInfo: {
    header: 'Barnets uppgifter',
    personalInfoHeader: 'Barnets personuppgifter',
    childName: 'Barnets namn',
    preferredName: 'Tilltalsnamn',
    dateOfBirth: 'Födelsedatum',
    address: 'Barnets hemadress',
    type: 'Placeringstyp',
    otherInfoHeader: 'Övriga uppgifter',
    allergies: 'Allergier',
    diet: 'Kost',
    medication: 'Medicinering',
    additionalInfo: 'Tilläggsinformation',
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
        deleteImageButton: 'Ta bort bild',
        deleteConfirm: {
          title: 'Vill du verkligen ta bort barnets bild?',
          resolve: 'Ta bort bild',
          reject: 'Ta inte bort'
        },
        disclaimer:
          'Det finns en kort fördröjning vid sparande av bilden, under vilken bilden inte visas. Bilden börjar visas senast ungefär en minut efter sparandet.'
      }
    },
    showSensitiveInfo: 'Visa känsliga uppgifter',
    noGuardians: 'Vårdnadshavare kan inte nås via eVaka'
  },
  staff: {
    title: 'Personalantal idag',
    daycareResponsible: 'Fostringsansvariga',
    staffOccupancyEffect: 'Jag är fostringsansvarig',
    other: 'Övriga (t.ex. assistenter, studerande, seo)',
    cancel: 'Ångra redigering',
    realizedGroupOccupancy: 'Gruppens beläggningsgrad idag',
    realizedUnitOccupancy: 'Enhetens beläggningsgrad idag',
    notUpdated: 'Uppgifterna har inte uppdaterats',
    updatedToday: 'Uppgifterna uppdaterade idag',
    updated: 'Uppgifterna uppdaterade'
  },
  pin: {
    header: 'Lås upp',
    info: 'Ange PIN-kod för att öppna barnets uppgifter',
    selectStaff: 'Välj användare',
    staff: 'Användare',
    noOptions: 'Inga alternativ',
    pinCode: 'PIN-kod',
    status: {
      SUCCESS: 'Rätt PIN-kod',
      WRONG_PIN: 'Fel PIN-kod',
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
      received: 'Mottagna',
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
        label: 'Meddelandetyp',
        message: 'Meddelande',
        bulletin: 'Bulletin (mottagaren kan inte svara)'
      },
      urgent: {
        heading: 'Markera som brådskande',
        info: 'Skicka meddelandet som brådskande enbart om du vill att vårdnadshavaren läser det under arbetsdagen.',
        label: 'Brådskande'
      },
      sender: 'Avsändare',
      receivers: 'Mottagare',
      recipientsPlaceholder: 'Välj...',
      starters: 'börjande barn',
      subject: {
        heading: 'Rubrik',
        placeholder: 'Skriv...'
      },
      message: {
        heading: 'Meddelande',
        placeholder: 'Meddelandeinnehåll...'
      },
      deleteDraft: 'Radera utkast',
      send: 'Skicka',
      sending: 'Skickar',
      recipientCount: 'Mottagare',
      manyRecipientsWarning: {
        title: (count: number) => `Meddelandet har ${count} mottagare.`,
        text: (count: number) =>
          `Detta meddelande skickas till ${count} mottagare. Är du säker på att du vill skicka meddelandet?`
      }
    },
    emptyInbox: 'Din inkorg är tom',
    noSentMessages: 'Inga skickade meddelanden',
    noDrafts: 'Inga utkast',
    unreadMessages: 'Nya meddelanden',
    openPinLock: 'Lås upp',
    pinLockInfo:
      'För att läsa meddelanden måste du låsa upp med PIN-kod. Du kan enbart läsa din egen grupps meddelanden.',
    noAccountAccess:
      'Meddelanden kan inte visas eftersom du inte har behörighet till gruppen. Be din chef om behörighet.',
    noRecipients: 'Det går inte att skicka meddelande till mottagaren'
  },
  mobile: {
    landerText1:
      'Välkommen att använda Esbo småbarnspedagogiks mobilapplikation!',
    landerText2:
      'För att ta applikationen i bruk välj \u2019Lägg till enhet\u2019 nedan och registrera mobilenheten i eVaka på din enhets sida.',
    actions: {
      ADD_DEVICE: 'Lägg till enhet',
      START: 'Vi börjar'
    },
    wizard: {
      text1: 'Ange den 6-siffriga koden från eVaka i fältet nedan.',
      text2: 'Ange bekräftelsekoden nedan i eVaka.',
      title1: 'Ibruktagande av eVaka-mobilen, steg 1/3',
      title2: 'Ibruktagande av eVaka-mobilen, steg 2/3',
      title3: 'Välkommen att använda eVaka-mobilen!',
      text3: 'eVaka-mobilen är nu i bruk på denna enhet.',
      text4:
        'För att skydda barnens uppgifter, kom ihåg att ställa in en åtkomstkod på enheten om du inte redan har gjort det.'
    },
    emptyList: (status: 'COMING' | 'ABSENT' | 'PRESENT' | 'DEPARTED') => {
      const statusText = (() => {
        switch (status) {
          case 'COMING':
            return 'kommande'
          case 'ABSENT':
            return 'frånvarande'
          case 'PRESENT':
            return 'närvarande'
          case 'DEPARTED':
            return 'avresta'
        }
      })()
      return `Inga ${statusText} barn`
    }
  },
  settings: {
    language: {
      title: 'Språk',
      fi: 'Suomi',
      sv: 'Svenska'
    },
    notifications: {
      title: 'Notifikationsinställningar',
      permission: {
        label: 'Notifikationer',
        enable: 'Aktivera',
        state: {
          unsupported: 'Telefonen eller webbläsaren stöder inte notifikationer',
          granted: 'Aktiverad',
          prompt: 'Inte aktiverad',
          denied: 'Blockerad'
        },
        info: {
          unsupported:
            'Notifikationer fungerar inte på denna telefon eller med den aktuella webbläsarversionen. Problemet kan åtgärdas genom att uppdatera webbläsaren.',
          denied:
            'Notifikationer har blockerats i telefonens inställningar. Problemet kan åtgärdas genom att ändra telefonens eller webbläsarens notifikationsinställningar.'
        }
      },
      categories: {
        label: 'Ämnen för vilka notifikationer skickas till denna telefon',
        values: {
          RECEIVED_MESSAGE: 'Mottagna meddelanden',
          NEW_ABSENCE: 'Frånvaromarkeringar för barnens aktuella dag',
          CALENDAR_EVENT_RESERVATION: 'Bokade och avbokade samtalstider'
        }
      },
      groups: {
        label: 'Grupper om vilka notifikationer skickas'
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
        EXTENSION_INVALID: 'Felaktig filändelse',
        INVALID_CONTENT_TYPE: 'Felaktigt filformat',
        FILE_TOO_LARGE: 'För stor fil (max. 25 MB)',
        SERVER_ERROR: 'Uppladdning misslyckades'
      },
      input: {
        title: 'Lägg till bilaga',
        text: [
          'Klicka här eller dra bilagan till rutan en åt gången.',
          'Maximal filstorlek: 25 MB.',
          'Tillåtna filformat:',
          'PDF, JPEG/JPG, PNG och DOC/DOCX'
        ]
      },
      deleteFile: 'Ta bort fil'
    },
    download: {
      modalHeader: 'Filen bearbetas',
      modalMessage: 'Filen kan inte öppnas just nu. Försök igen om en stund.',
      modalClose: 'Stäng'
    }
  },
  units: {
    title: 'Enheter',
    children: 'Barn',
    staff: 'Personal',
    utilization: 'Beläggningsgrad',
    description:
      'Antal personal och barn samt beläggningsgrad för dina enheter just nu.'
  },
  components
}
