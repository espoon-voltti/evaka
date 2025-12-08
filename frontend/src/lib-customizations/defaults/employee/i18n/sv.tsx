// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { type ReactNode } from 'react'

import type DateRange from 'lib-common/date-range'
import type FiniteDateRange from 'lib-common/finite-date-range'
import type { InvoiceStatus } from 'lib-common/generated/api-types/invoicing'
import type HelsinkiDateTime from 'lib-common/helsinki-date-time'
import type LocalDate from 'lib-common/local-date'
import { H3, P } from 'lib-components/typography'

import components from '../../components/i18n/sv'

export const sv = {
  titles: {
    defaultTitle: 'Småbarnspedagogik',
    login: 'Logga in',
    ai: 'AI test',
    applications: 'Ansökningar',
    childInformation: 'Barnets uppgifter',
    employees: 'Användare',
    financeBasics: 'Ekonomins betalningsinställningar',
    units: 'Enheter',
    customers: 'Kunduppgifter',
    placementPlan: 'Placeringsplan',
    decision: 'Beslut och utskick',
    documentTemplates: 'Dokumentmallar',
    feeDecisions: 'Avgiftsbeslut',
    feeDecision: 'Avgiftsbeslut',
    feeDecisionDraft: 'Utkast till avgiftsbeslut',
    holidayPeriod: 'Semestertid',
    holidayPeriods: 'Semestertider',
    holidayAndTermPeriods: 'Semestertider och verksamhetsperioder',
    holidayQuestionnaire: 'Semestertidsenkät',
    groupCaretakers: 'Personalbehov i gruppen',
    incomeStatements: 'Inkomstutredningar',
    valueDecisions: 'Värdebeslut',
    valueDecision: 'Värdebeslut',
    valueDecisionDraft: 'Utkast till värdebeslut',
    incomeStatement: 'Inkomstutredningsblankett',
    invoices: 'Fakturor',
    payments: 'Betalning',
    invoice: 'Faktura',
    invoiceDraft: 'Utkast till faktura',
    reports: 'Rapporter',
    messages: 'Meddelanden',
    caretakers: 'Personal',
    createUnit: 'Skapa ny enhet',
    personProfile: 'Vuxenuppgifter',
    personTimeline: 'Kundens tidslinje',
    personalMobileDevices: 'Personlig eVaka-mobil',
    preschoolTerm: 'Förskolans läsår',
    preschoolTerms: 'Förskolans läsår',
    employeePinCode: 'Hantering av PIN-kod',
    preferredFirstName: 'Hantering av tilltalsnamn',
    settings: 'Inställningar',
    systemNotifications: 'Tillfälligt meddelande',
    unitFeatures: 'Öppning av funktioner',
    welcomePage: 'Välkommen till eVaka',
    assistanceNeedDecision: 'Beslut om stöd inom småbarnspedagogik',
    assistanceNeedPreschoolDecision: 'Beslut om stöd i förskolan',
    clubTerm: 'Klubbens läsår',
    clubTerms: 'Klubbarnas läsår',
    placementTool: 'Optimeringsverktyg',
    outOfOffice: 'Frånvaromeddelande'
  },
  common: {
    yes: 'Ja',
    no: 'Nej',
    and: 'Och',
    loadingFailed: 'Hämtningen av uppgifter misslyckades',
    noAccess: 'Saknar behörighet',
    edit: 'Redigera',
    add: 'Lägg till',
    addNew: 'Lägg till ny',
    clear: 'Töm',
    create: 'Skapa',
    remove: 'Ta bort',
    doNotRemove: 'Ta inte bort',
    archive: 'Arkivera',
    download: 'Ladda ner',
    cancel: 'Avbryt',
    goBack: 'Återgå',
    leavePage: 'Lämna',
    confirm: 'Bekräfta',
    period: 'För tiden',
    search: 'Sök',
    select: 'Välj',
    send: 'Skicka',
    save: 'Spara',
    saving: 'Sparar',
    saved: 'Sparad',
    unknown: 'Okänd',
    all: 'Alla',
    continue: 'Fortsätt',
    statuses: {
      active: 'Aktiv',
      coming: 'Kommande',
      completed: 'Avslutad',
      conflict: 'Konflikt',
      guarantee: 'Garantiplats'
    },
    careTypeLabels: {
      club: 'Klubb',
      preschool: 'Förskola',
      daycare: 'Småbarnspedagogik',
      daycare5yo: 'Småbarnspedagogik',
      preparatory: 'Förberedande',
      'backup-care': 'Reservplacering',
      temporary: 'Tillfällig',
      'school-shift-care': 'Skiftomsorg för skolbarn',
      'connected-daycare': 'Ansluten'
    },
    providerType: {
      MUNICIPAL: 'Kommunal',
      PURCHASED: 'Köptjänst',
      PRIVATE: 'Privat',
      MUNICIPAL_SCHOOL: 'Svenskspråkig undervisningssektor (SUKO)',
      PRIVATE_SERVICE_VOUCHER: 'Privat (servicesedel)',
      EXTERNAL_PURCHASED: 'Köptjänst (annan)'
    },
    types: {
      CLUB: 'Klubb',
      FAMILY: 'Familjedagvård',
      GROUP_FAMILY: 'Gruppfamiljedagvård',
      CENTRE: 'Daghem',
      PRESCHOOL: 'Förskola',
      DAYCARE: 'Småbarnspedagogik',
      PRESCHOOL_DAYCARE: 'Ansluten småbarnspedagogik',
      PREPARATORY_EDUCATION: 'Förberedande förskola',
      PREPARATORY_DAYCARE: 'Ansluten småbarnspedagogik',
      DAYCARE_5YO_FREE: '5-årig avgiftsfri småbarnspedagogik',
      DAYCARE_5YO_PAID: 'Småbarnspedagogik (avgiftsbelagd)'
    },
    form: {
      address: 'Adress',
      addressRestricted:
        'Adressen är inte tillgänglig på grund av skyddsförbud',
      age: 'Ålder',
      backupPhone: 'Reservtelefonnummer',
      birthday: 'Födelsedatum',
      dateOfDeath: 'Död',
      email: 'E-post',
      endDate: ' Slutar',
      firstName: 'Förnamn',
      firstNames: 'Förnamn',
      invoiceRecipient: 'Fakturamottagare',
      invoicingAddress: 'Faktureringsadress',
      lastModified: 'Senast redigerad',
      lastModifiedBy: (name: string) => `Redigerad av: ${name}`,
      lastName: 'Efternamn',
      name: 'Namn',
      ophPersonOid: 'UBS person-OID',
      phone: 'Telefonnummer',
      postOffice: 'Postanstalt',
      postalCode: 'Postnummer',
      municipalityOfResidence: 'Hemkommun',
      range: 'För tiden',
      socialSecurityNumber: 'Personbeteckning',
      startDate: 'Från och med',
      streetAddress: 'Gatuadress',
      updatedFromVtj: 'Uppgifter uppdaterade från BIS'
    },
    expandableList: {
      others: 'andra'
    },
    resultCount: (count: number) =>
      count > 0 ? `Sökresultat: ${count}` : 'Inga sökresultat',
    ok: 'Okej!',
    tryAgain: 'Försök igen',
    checkDates: 'Kontrollera datum',
    multipleChildren: 'Flera barn',
    today: 'Idag',
    error: {
      unknown: 'Hoppsan, något gick fel!',
      forbidden: 'Saknar behörighet för denna åtgärd',
      saveFailed: 'Att spara ändringarna misslyckades, försök igen.',
      minutes: 'Högst 59 minuter'
    },
    days: 'dagar',
    day: 'dag',
    loading: 'Laddar...',
    noResults: 'Inga sökresultat',
    noFirstName: 'Förnamn saknas',
    noLastName: 'Efternamn saknas',
    noName: 'Namn saknas',
    date: 'Datum',
    month: 'Månad',
    year: 'År',
    code: 'Kod',
    ready: 'Klar',
    page: 'Sida',
    group: 'Grupp',
    openExpandingInfo: 'Öppna tilläggsinformationsfält',
    datetime: {
      weekdaysShort: ['Må', 'Ti', 'On', 'To', 'Fr', 'Lö', 'Sö'],
      week: 'Vecka',
      weekShort: 'V',
      monthShort: 'Mån',
      weekdays: [
        'Måndag',
        'Tisdag',
        'Onsdag',
        'Torsdag',
        'Fredag',
        'Lördag',
        'Söndag'
      ],
      months: [
        'Januari',
        'Februari',
        'Mars',
        'April',
        'Maj',
        'Juni',
        'Juli',
        'Augusti',
        'September',
        'Oktober',
        'November',
        'December'
      ]
    },
    nb: 'Obs',
    lastModified: (dateTime: string) => `Senast redigerad: ${dateTime}`,
    validTo: (date: string) => `Gäller till och med ${date}`,
    closeModal: 'Stäng popup-fönster',
    datePicker: {
      previousMonthLabel: 'Föregående månad',
      nextMonthLabel: 'Nästa månad',
      calendarLabel: 'Kalender'
    },
    close: 'Stäng',
    open: 'Öppna',
    copy: 'Kopiera',
    startDate: 'Startdatum',
    endDate: 'Slutdatum',
    retroactiveConfirmation: {
      title:
        'Du håller på att göra en ändring som retroaktivt kan orsaka ändringar i kundavgifterna.',
      checkboxLabel: 'Jag förstår, jag kontaktar faktureringsteamet om saken.*'
    },
    userTypes: {
      SYSTEM: 'system',
      CITIZEN: 'kommuninvånare',
      EMPLOYEE: 'anställd',
      MOBILE_DEVICE: 'mobilenhet',
      UNKNOWN: 'okänd'
    },
    showMore: 'Visa mer',
    showLess: 'Dölj'
  },
  header: {
    applications: 'Ansökningar',
    units: 'Enheter',
    search: 'Kunduppgifter',
    finance: 'Ekonomi',
    invoices: 'Fakturor',
    payments: 'Betalning',
    incomeStatements: 'Inkomstutredningar',
    feeDecisions: 'Avgiftsbeslut',
    valueDecisions: 'Värdebeslut',
    reports: 'Rapporter',
    messages: 'Meddelanden',
    logout: 'Logga ut'
  },
  footer: {
    cityLabel: 'Esbo stad',
    linkLabel: 'Småbarnspedagogik i Esbo',
    linkHref:
      'https://www.espoo.fi/sv/fostran-och-utbildning/smabarnspeadagogik'
  },
  language: {
    fi: 'Finska',
    sv: 'Svenska',
    en: 'Engelska'
  },
  errorPage: {
    reload: 'Ladda om sidan',
    text: 'Vi stötte på ett oväntat problem. Uppgifterna om felet har vidarebefordrats.',
    title: 'Något gick fel'
  },
  validationError: {
    mandatoryField: 'Obligatorisk uppgift',
    endDateIsMandatoryField: 'Slutdatum är obligatorisk uppgift',
    dateRange: 'Datumet är felaktigt',
    invertedDateRange: 'Startdatum får inte vara efter slutdatum',
    existingDateRangeError:
      'Datum får inte överlappa redan skapade tidsperioder',
    coveringDateRangeError:
      'Datumintervallet får inte helt täcka ett redan existerande',
    email: 'E-postadressen är i fel format',
    phone: 'Telefonnumret är i fel format',
    ssn: 'Personbeteckningen är i fel format',
    time: 'Tiden är i fel format',
    cents: 'Eurobeloppet är i fel format',
    decimal: 'Decimaltal är i fel format',
    startDateNotOnTerm: 'Startdatum måste infalla under någon termin'
  },
  login: {
    title: 'Småbarnspedagogik',
    subtitle: 'Kunduppgifter och enheter',
    systemNotification: 'Viktigt meddelande',
    login: 'Logga in',
    loginAD: 'Esbo AD',
    loginEvaka: 'Tjänsteleverantör',
    error: {
      noRole: 'Du har inte den nödvändiga rollen',
      default: 'Något gick fel'
    }
  },
  applications: {
    show: 'Visa',
    asList: 'Som lista',
    asDesktop: 'Som skrivbord',
    list: {
      addNote: 'Lägg till anteckning',
      areaPlaceholder: 'Välj område',
      basis: 'Grunder',
      currentUnit: 'Nuv.',
      dueDate: 'Att behandla',
      name: 'Barnets namn/ålder',
      noResults: 'Inga sökresultat',
      note: 'Obs',
      paper: 'Pappersansökan',
      resultCount: 'Sökresultat',
      serviceWorkerNote: 'Servicehandledningens anmärkning',
      startDate: 'Start',
      status: 'Status',
      statusLastModified: 'Status senast redigerad',
      subtype: 'Del / Hel',
      title: 'Ansökningar',
      transfer: 'Överflyttningsansökan',
      transferFilter: {
        title: 'Överflyttningsansökningar',
        transferOnly: 'Visa endast överflyttningsansökningar',
        hideTransfer: 'Dölj överflyttningsansökningar',
        all: 'Ingen avgränsning'
      },
      type: 'Ansökningstyp',
      unit: 'Enhet',
      voucherFilter: {
        title: 'Servicesedelansökningar',
        firstChoice: 'Visa om 1. önskat placeringsställe',
        allVoucher: 'Visa alla servicesedelansökningar',
        hideVoucher: 'Dölj servicesedelansökningar',
        noFilter: 'Ingen avgränsning'
      }
    },
    placementDesktop: {
      warnings: {
        tooManyApplicationsTitle: (count: number) =>
          `För många ansökningar (${count})`,
        tooManyApplicationsMessage:
          'Precisera sökvillkoren så att det finns högst 50 ansökningar.'
      },
      occupancyPeriod: 'Visa beläggningsgradens maximum för tidsintervallet',
      shownUnitsCount: 'Enheter att visa',
      addShownUnit: 'Lägg till enhet att visa...',
      applicationsCount: 'Ansökningar',
      preferences: 'Placeringsönskemål',
      createPlacementDraft: 'Skissa',
      createPlacementDraftToOtherUnit: 'Skissa till annan enhet...',
      cancelPlacementDraft: 'Avbryt skiss',
      cancelPlacementDraftConfirmationTitle:
        'Vill du verkligen avbryta placeringsskissen?',
      cancelPlacementDraftConfirmationMessage:
        'Den relaterade ansökan ingår inte i de sökresultat som visas nu.',
      show: 'Visa',
      showUnit: 'Visa enhet',
      hideUnit: 'Dölj enhet',
      other: 'Annan',
      birthDate: 'Födelsedatum',
      dueDate: 'Lagstadgat',
      preferredStartDate: 'Önskad start',
      transfer: 'Överflyttning',
      toPlacementPlan: 'Placera',
      checkApplication: 'Kontrollera ansökan',
      occupancies: 'Beläggningsgrader',
      occupancyTypes: {
        confirmed: 'Bekräftad',
        planned: 'Planerad',
        draft: 'Skissad'
      },
      openGraph: 'Öppna beläggningsgradsgraf',
      placementDrafts: 'Placeringsskisser',
      notInSearchResults: 'Ansökan ingår inte i sökresultaten',
      draftedBy: 'Skissad av'
    },
    actions: {
      moveToWaitingPlacement: 'Flytta till att placera',
      returnToSent: 'Återför till inkomna',
      cancelApplication: 'Ta bort från behandling',
      cancelApplicationConfirm:
        'Vill du verkligen ta bort ansökan från behandlingen?',
      cancelApplicationConfidentiality: 'Är ansökan sekretessbelagd?',
      check: 'Kontrollera',
      setVerified: 'Markera som kontrollerad',
      createPlacementPlan: 'Placera',
      cancelPlacementPlan: 'Återför till att placera',
      editDecisions: 'Beslut',
      confirmPlacementWithoutDecision: 'Bekräfta utan beslut',
      sendDecisionsWithoutProposal: 'Skicka beslut',
      sendPlacementProposal: 'Skicka placeringsförslag',
      withdrawPlacementProposal: 'Återkalla placeringsförslag',
      confirmDecisionMailed: 'Markera som postad',
      checked: (count: number) =>
        count === 1 ? `${count} ansökan vald` : `${count} ansökningar valda`
    },
    distinctiveDetails: {
      SECONDARY: 'Visa även om enheten sökts som 2. eller 3. önskan'
    },
    basisTooltip: {
      ADDITIONAL_INFO: 'Text i tilläggsinformationsfältet',
      SIBLING_BASIS: 'Använder syskongrund',
      ASSISTANCE_NEED: 'Stödbehov',
      CLUB_CARE: 'Haft klubbplats föregående verksamhetsperiod',
      CONTINUATION: 'Fortsättande barn',
      DAYCARE: 'Har meddelat att ger upp småbarnspedagogikplats',
      EXTENDED_CARE: 'Skiftarbete',
      DUPLICATE_APPLICATION: 'Dubblettansökan',
      URGENT: 'Brådskande ansökan',
      HAS_ATTACHMENTS: 'Ansökan har bilaga'
    },
    types: {
      PRESCHOOL: 'Förskoleansökan',
      DAYCARE: 'Småbarnspedagogikansökan',
      CLUB: 'Klubbansökan',
      PRESCHOOL_ONLY: 'Förskola',
      PRESCHOOL_DAYCARE: 'Förskola & ansluten',
      PRESCHOOL_CLUB: 'Förskolans klubb',
      PREPARATORY_ONLY: 'Förberedande',
      PREPARATORY_DAYCARE: 'Förberedande & ansluten',
      DAYCARE_ONLY: 'Senare sökt ansluten',
      ALL: 'Alla'
    },
    searchPlaceholder: 'Sök med namn, personbeteckning eller adress',
    basis: 'Anmärkningar',
    distinctions: 'Preciserad sökning',
    secondaryTooltip: 'Välj först verksamhetsställe'
  },
  application: {
    tabTitle: 'Ansökan',
    messageSubject: (date: string, name: string) => `Ansökan ${date}: ${name}`,
    types: {
      PRESCHOOL: 'Förskoleansökan',
      DAYCARE: 'Småbarnspedagogikansökan',
      CLUB: 'Klubbansökan',
      PRESCHOOL_DAYCARE: 'Ansluten småbarnspedagogik',
      PREPARATORY_EDUCATION: 'Förberedande undervisning',
      ALL: 'Alla'
    },
    statuses: {
      CREATED: 'Utkast',
      SENT: 'Inkommen',
      WAITING_PLACEMENT: 'Väntar på placering',
      WAITING_DECISION: 'Beslutsberedning',
      WAITING_UNIT_CONFIRMATION: 'För rektors granskning',
      WAITING_MAILING: 'Väntar på postning',
      WAITING_CONFIRMATION: 'För vårdnadshavares bekräftelse',
      ACTIVE: 'Plats mottagen',
      REJECTED: 'Plats avslagen',
      CANCELLED: 'Borttagen från behandling',
      ALL: 'Alla'
    },
    selectConfidentialityLabel: 'Är ansökan sekretessbelagd?',
    selectAll: 'Välj alla',
    unselectAll: 'Ta bort val',
    transfer: 'Överflyttningsansökan',
    origins: {
      ELECTRONIC: 'Elektronisk ansökan',
      PAPER: 'Pappersansökan'
    },
    person: {
      name: 'Namn',
      ssn: 'Personbeteckning',
      dob: 'Födelsedatum',
      address: 'Adress',
      restricted: 'Skyddsförbud i kraft',
      hasFutureAddress:
        'Adressen i befolkningsregistret har ändrats / kommer att ändras',
      futureAddress: 'Kommande adress',
      movingDate: 'Flyttningsdatum',
      nationality: 'Nationalitet',
      language: 'Språk',
      phone: 'Telefonnummer',
      email: 'E-post',
      agreementStatus: 'Överenskommet tillsammans',
      otherGuardianAgreementStatuses: {
        AGREED: 'Överenskommet tillsammans',
        NOT_AGREED: 'Inte överenskommet tillsammans',
        RIGHT_TO_GET_NOTIFIED: 'Endast rätt till information',
        AUTOMATED: 'Automatiskt beslut',
        NOT_SET: 'Vårdnadshavarna bor på samma adress'
      },
      noOtherChildren: 'Inga andra barn',
      applicantDead: 'Sökande avliden'
    },
    serviceNeed: {
      title: 'Servicebehov',
      startDate: 'Önskat startdatum',
      connectedLabel: 'Ansluten småbarnspedagogik',
      connectedValue: 'Jag söker även ansluten småbarnspedagogik',
      connectedDaycarePreferredStartDateLabel:
        'Önskat startdatum för ansluten småbarnspedagogik',
      connectedDaycareServiceNeedOptionLabel: 'Kompletterande servicebehov',
      dailyTime: 'Daglig närvarotid',
      startTimePlaceholder: '08:00',
      endTimePlaceholder: '16:00',
      shiftCareLabel: 'Kväll- och skiftomsorg',
      shiftCareNeeded: 'Kväll- och skiftomsorg behövs',
      shiftCareWithAttachments: 'Kväll- och skiftomsorg behövs, bilagor:',
      urgentLabel: 'Brådskande ansökan',
      notUrgent: 'Nej',
      isUrgent: 'Är brådskande',
      isUrgentWithAttachments: 'Är brådskande, bilagor:',
      missingAttachment: 'Bilaga saknas',
      preparatoryLabel: 'Förberedande undervisning',
      preparatoryValue: 'Jag söker även till förberedande undervisning',
      assistanceLabel: 'Stödbehov',
      assistanceValue: 'Barnet har stödbehov',
      assistanceDesc: 'Beskrivning av stödbehov',
      partTime: 'Deltid',
      fullTime: 'Heltid',
      partTimeLabel: 'Deltid eller heltid',
      error: {
        getServiceNeedOptions: 'Hämtning av servicebehov misslyckades!'
      }
    },
    clubDetails: {
      wasOnClubCareLabel: 'I klubb föregående verksamhetsperiod',
      wasOnClubCareValue:
        'Barnet har varit i klubb under föregående verksamhetsperiod',
      wasOnDaycareLabel: 'I småbarnspedagogik innan klubben',
      wasOnDaycareValue:
        'Barnet är i småbarnspedagogik innan klubbens önskade startdatum'
    },
    preferences: {
      title: 'Placeringsönskan',
      preferredUnits: 'Placeringsönskemål',
      moveUp: 'Flytta uppåt',
      moveDown: 'Flytta nedåt',
      missingPreferredUnits: 'Välj minst ett placeringsönskemål',
      unitMismatch: 'Placeringsönskemål motsvarar inte sökta enheter',
      unitsOnMap: 'Enheter på karta',
      siblingBasisLabel: 'Syskongrund',
      siblingBasisValue: 'Jag söker plats på syskongrund',
      siblingName: 'Syskonets namn',
      siblingSsn: 'Syskonets personbeteckning',
      siblingUnit: 'Syskonets enhet'
    },
    child: {
      title: 'Barnets uppgifter'
    },
    guardians: {
      title: 'Sökandens uppgifter',
      appliedGuardian: 'Sökandens uppgifter',
      secondGuardian: {
        title: 'Uppgifter om anmäld andra vuxen',
        checkboxLabel:
          'Sökanden har anmält den andra vårdnadshavarens uppgifter',
        exists: 'Barnet har en andra vårdnadshavare',
        sameAddress: 'Den andra vårdnadshavaren bor på samma adress',
        separated: 'Den andra vårdnadshavaren bor på annan adress',
        agreed: 'Ansökan har överenskommits tillsammans',
        noVtjGuardian: 'Enligt BIS har barnet inte en andra vårdnadshavare'
      },
      vtjGuardian: 'Uppgifter om andra vårdnadshavaren enligt BIS'
    },
    otherPeople: {
      title: 'Andra personer',
      adult: 'Annan vuxen',
      spouse: 'Sökanden bor tillsammans med annan make/maka eller sambo',
      children: 'Andra barn som bor i samma hushåll',
      addChild: 'Lägg till barn'
    },
    additionalInfo: {
      title: 'Tilläggsinformation',
      applicationInfo: 'Ansökans tilläggsinformation',
      allergies: 'Allergier',
      diet: 'Specialdiet',
      maxFeeAccepted: 'Samtycke till högsta avgift',
      serviceWorkerAttachmentsTitle: 'Servicehandledningens bilagor',
      noAttachments: 'Inga bilagor'
    },
    decisions: {
      title: 'Beslut',
      noDecisions: 'Ansökan har ännu inga beslut.',
      type: 'Beslutstyp',
      types: {
        CLUB: 'Klubbeslut',
        DAYCARE: 'Beslut om småbarnspedagogik',
        DAYCARE_PART_TIME: 'Beslut om småbarnspedagogik (deltid)',
        PRESCHOOL: 'Förskolebeslut',
        PRESCHOOL_DAYCARE: 'Beslut om ansluten småbarnspedagogik',
        PRESCHOOL_CLUB: 'Förskolans klubb',
        PREPARATORY_EDUCATION: 'Beslut om förberedande undervisning'
      },
      num: 'Beslutsnummer',
      status: 'Beslutets status',
      statuses: {
        draft: 'Utkast',
        waitingMailing: 'Väntar på postning',
        PENDING: 'För vårdnadshavares bekräftelse',
        ACCEPTED: 'Mottagen',
        REJECTED: 'Avslagen'
      },
      unit: 'Beslutsenhet',
      download: 'Ladda ner beslut som PDF-fil',
      downloadPending:
        'Beslutets PDF-fil är ännu inte nedladdningsbar. Försök igen senare.',
      response: {
        label: 'Bekräftelse på kommuninvånarens vägnar',
        accept: 'Vårdnadshavaren har mottagit platsen',
        reject: 'Vårdnadshavaren har avslagit platsen',
        submit: 'Bekräfta på kommuninvånarens vägnar',
        acceptError:
          'Mottagande av platsen misslyckades. Datumet kan vara felaktigt.',
        rejectError:
          'Avslag av platsen misslyckades. Uppdatera sidan och försök igen.'
      },
      blocked:
        'Detta beslut kan godkännas först efter att förskolebeslutet godkänts'
    },
    attachments: {
      title: 'Bilagor',
      none: 'Ansökan har inga bilagor',
      name: 'Filnamn',
      updated: 'Ändrad',
      contentType: 'Typ',
      receivedByPaperAt: 'Levererad som papper',
      receivedAt: 'Levererad elektroniskt'
    },
    state: {
      title: 'Ansökans status',
      status: 'Ansökans status',
      origin: 'Ansökans sändningsform',
      sent: 'Inkommen',
      modified: 'Senast redigerad',
      modifiedBy: 'Redigerare',
      due: 'Att behandla senast'
    },
    date: {
      DUE: 'Ansökan att behandla senast',
      START: 'Startbehov',
      ARRIVAL: 'Ansökan inkommen'
    },
    notes: {
      add: 'Lägg till anteckning',
      newNote: 'Ny anteckning',
      created: 'Skapad',
      editing: 'Under redigering',
      lastEdited: 'Senast redigerad',
      placeholder: 'Skriv anteckning',
      confirmDelete: 'Vill du verkligen ta bort anteckningen',
      sent: 'Skickad',
      message: 'meddelande',
      error: {
        save: 'Att spara anteckningen misslyckades',
        remove: 'Att ta bort anteckningen misslyckades'
      }
    },
    messaging: {
      sendMessage: 'Skicka meddelande'
    }
  },
  childInformation: {
    restrictedDetails: 'Skyddsförbud',
    asAdult: 'Granska som vuxen',
    personDetails: {
      title: 'Person-, kontakt- och hälsouppgifter',
      attendanceReport: 'Närvaro- och frånvarouppgifter',
      name: 'Barnets namn',
      email: 'E-post',
      socialSecurityNumber: 'Personbeteckning',
      birthday: 'Födelsedatum',
      language: 'Språk (BIS)',
      address: 'Adress',
      familyLink: 'Familjens uppgifter',
      languageAtHome: 'Hemspråk, om annat än angivet i BIS',
      specialDiet: 'Specialdiet för matbeställningsintegration',
      mealTexture: 'Matens konsistens för matbeställningsintegration',
      participatesInBreakfast: 'Äter frukost',
      participatesInBreakfastYes: 'Ja',
      participatesInBreakfastNo: 'Nej',
      nekkuDiet: 'Nekku-matbeställningens diet',
      nekkuSpecialDiet: 'Nekku-specialdiet',
      nekkuSpecialDietInfo:
        'I fältet Nekku-specialdiet antecknas endast sådana allergier som inte kan väljas med kryssrutorna i punkten Nekku-specialdiet. Här antecknas inte barnets måltidsdrycker, utan de antecknas i punkten Tilläggsinformation ovan.',
      noGuardian:
        'Vårdnadshavaruppgifter saknas. Barnets vårdnadshavare kan inte använda eVaka',
      placeholder: {
        languageAtHome: 'Välj språk',
        languageAtHomeDetails: 'Tilläggsinformation om hemspråk',
        specialDiet: 'Välj specialdiet',
        mealTexture: 'Välj matens konsistens'
      }
    },
    familyContacts: {
      title: 'Familjens kontaktuppgifter och reservsökande',
      contacts: 'Kontaktuppgifter',
      name: 'Namn',
      role: 'Roll',
      roles: {
        LOCAL_GUARDIAN: 'Vårdnadshavare',
        LOCAL_FOSTER_PARENT: 'Fosterförälder',
        LOCAL_ADULT: 'Vuxen i samma hushåll',
        LOCAL_SIBLING: 'Barn',
        REMOTE_GUARDIAN: 'Vårdnadshavare',
        REMOTE_FOSTER_PARENT: 'Fosterförälder'
      },
      contact: 'E-post och telefon',
      contactPerson: 'Kontaktperson',
      address: 'Adress',
      backupPhone: 'Reservnr'
    },
    timeBasedStatuses: {
      ACTIVE: 'Aktiv',
      ENDED: 'Avslutad',
      UPCOMING: 'Kommande'
    },
    serviceNeed: {
      title: 'Servicebehov',
      dateRange: 'Servicebehov för tiden',
      hoursPerWeek: 'Veckovis servicebehov',
      hoursPerWeekInfo:
        'Skriv här det veckobehov som omfattar hela den närvarotid som familjen angett, inklusive eventuell förskoletid, 5-årig avgiftsfri småbarnspedagogik och förberedande undervisning.',
      hoursInWeek: 'h / vecka',
      serviceNeedDetails: 'Precisering av servicebehov',
      createdByName: 'Enhetschefens bekräftelse',
      create: 'Skapa nytt servicebehov',
      removeServiceNeed: 'Vill du ta bort servicebehovet?',
      previousServiceNeeds: 'Tidigare skapade servicebehov',
      errors: {
        conflict: 'Servicebehovet överlappar med ett annat servicebehov.',
        hardConflict:
          'Servicebehovet överlappar med ett annat servicebehovs startdatum.',
        checkHours: 'Kontrollera',
        placementMismatchWarning:
          'Veckovis servicebehov motsvarar inte placeringens verksamhetsform.',
        autoCutWarning:
          'Tidigare överlappande servicebehov avbryts automatiskt.'
      }
    },
    dailyServiceTimes: {
      title: 'Daglig småbarnspedagogiktid',
      info: 'Skriv här den dagliga småbarnspedagogiktid som angetts i småbarnspedagogikavtalet, inklusive förskola / förberedande undervisning / 5-årig avgiftsfri småbarnspedagogik.',
      info2:
        'Uppdatera inte småbarnspedagogiktiden om den småbarnspedagogiktid som angetts i det nya avtalet inte har ändrats från tidigare.',
      info3: 'Oregelbundna och regelbundna frånvaron antecknas i dagboken.',
      create: 'Skapa ny småbarnspedagogiktid',
      types: {
        REGULAR: 'Regelbunden småbarnspedagogiktid',
        IRREGULAR: 'Oregelbunden småbarnspedagogiktid',
        VARIABLE_TIME: 'Varierande småbarnspedagogiktid'
      },
      weekdays: {
        monday: 'måndag',
        tuesday: 'tisdag',
        wednesday: 'onsdag',
        thursday: 'torsdag',
        friday: 'fredag',
        saturday: 'lördag',
        sunday: 'söndag'
      },
      errors: {
        required: 'Obligatorisk uppgift'
      },
      dailyServiceTime: 'Daglig småbarnspedagogiktid',
      validityPeriod: 'Daglig småbarnspedagogiktid i kraft',
      validFrom: 'Daglig småbarnspedagogiktid i kraft från och med',
      validUntil: 'Daglig småbarnspedagogiktids giltighet upphör',
      createNewTimes: 'Skapa ny daglig småbarnspedagogiktid',
      deleteModal: {
        title: 'Tas småbarnspedagogiktiden bort?',
        description:
          'Vill du verkligen ta bort den dagliga småbarnspedagogiktiden? Tiden kan inte återställas, utan den måste läggas till på nytt vid behov efter borttagning.',
        deleteBtn: 'Ta bort tid'
      },
      retroactiveModificationWarning:
        'Obs! Du håller på att redigera den dagliga småbarnspedagogiktiden retroaktivt. Barnets närvarokalenderanteckningar kan ändras under denna tidsperiod.'
    },
    assistance: {
      title: 'Stödbehov och stödåtgärder',
      unknown: 'Okänd',
      fields: {
        capacityFactor: 'Koefficient',
        lastModified: 'Senast redigerad',
        lastModifiedBy: (name: string) => `Redigerare ${name}.`,
        level: 'Nivå',
        otherAssistanceMeasureType: 'Åtgärd',
        status: 'Status',
        validDuring: 'Giltighetstid'
      },
      validationErrors: {
        overlap:
          'För denna tidsperiod finns redan en överlappande anteckning. Redigera vid behov föregående tidsperiod',
        startBeforeMinDate: (date: LocalDate) =>
          `Detta stöd kan börja tidigast ${date.format()}`,
        endAfterMaxDate: (date: LocalDate) =>
          `Detta stöd kan beviljas högst till och med ${date.format()}`
      },
      types: {
        daycareAssistanceLevel: {
          GENERAL_SUPPORT: 'Allmänt stöd, inget beslut',
          GENERAL_SUPPORT_WITH_DECISION: 'Allmänt stöd, beslut om stödtjänster',
          INTENSIFIED_SUPPORT: 'Intensifierat stöd',
          SPECIAL_SUPPORT: 'Särskilt stöd'
        },
        preschoolAssistanceLevel: {
          INTENSIFIED_SUPPORT: 'Intensifierat stöd',
          SPECIAL_SUPPORT: 'Särskilt stöd utan förlängd läroplikt',
          SPECIAL_SUPPORT_WITH_DECISION_LEVEL_1:
            'Särskilt stöd och förlängd läroplikt - annat (till Koski)',
          SPECIAL_SUPPORT_WITH_DECISION_LEVEL_2:
            'Särskilt stöd och förlängd läroplikt - utvecklingsstörning 2 (till Koski)',
          CHILD_SUPPORT:
            'Barnspecifikt stöd utan tidigarelagd läroplikt (till Koski)',
          CHILD_SUPPORT_AND_EXTENDED_COMPULSORY_EDUCATION:
            'Barnspecifikt stöd och tidigarelagd läroplikt (till Koski)',
          CHILD_SUPPORT_AND_OLD_EXTENDED_COMPULSORY_EDUCATION:
            'Barnspecifikt stöd och gammalt format förlängd lp - annat (till Koski, i bruk under övergångsperiod 1.8.2025 - 31.7.2026)',
          CHILD_SUPPORT_2_AND_OLD_EXTENDED_COMPULSORY_EDUCATION:
            'Barnspecifikt stöd och gammalt format förlängd lp - utvecklingsstörning 2 (till Koski, i bruk under övergångsperiod 1.8.2025 - 31.7.2026)',
          GROUP_SUPPORT: 'Gruppspecifika stödformer'
        },
        otherAssistanceMeasureType: {
          TRANSPORT_BENEFIT: 'Transportförmån (för förskolebarn Koski-uppgift)',
          ACCULTURATION_SUPPORT: 'Barnets integrationsstöd (ELY)',
          ANOMALOUS_EDUCATION_START:
            'Avvikande tidpunkt för undervisningsstart',
          CHILD_DISCUSSION_OFFERED: 'Barnet på tal erbjuden',
          CHILD_DISCUSSION_HELD: 'Barnet på tal hållen',
          CHILD_DISCUSSION_COUNSELING: 'Barnet på tal rådgivning'
        }
      },
      assistanceFactor: {
        title: 'Stödkoefficient',
        create: 'Skapa ny stödkoefficientsperiod',
        removeConfirmation: 'Vill du ta bort stödkoefficientsperioden?',
        info: (): React.ReactNode => undefined
      },
      daycareAssistance: {
        title: 'Stödnivå inom småbarnspedagogik',
        create: 'Skapa ny stödnivåperiod (småbarnspedagogik)',
        removeConfirmation: 'Vill du ta bort stödnivåperioden?'
      },
      preschoolAssistance: {
        title: 'Stöd i förskolan',
        create: 'Skapa ny stödperiod (förskola)',
        removeConfirmation: 'Vill du ta bort stödperioden?'
      },
      otherAssistanceMeasure: {
        title: 'Andra åtgärder',
        create: 'Lägg till annan åtgärd',
        removeConfirmation: 'Vill du ta bort den andra åtgärden?',
        infoList: 'Tilläggsinformation om andra åtgärder:',
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
      title: 'Stödbehov',
      fields: {
        dateRange: 'Stödbehov för tiden',
        capacityFactor: 'Barnets stödkoefficient',
        capacityFactorInfo:
          'Kapaciteten bestäms vanligtvis enligt barnets ålder och servicebehov. Om barnet har sådant stöd som använder mer kapacitet, lägg till stödkoefficienten här. T.ex. för barn i behov av stöd i specialgrupp är koefficienten 2,33' as ReactNode,
        bases: 'Grunder'
      },
      create: 'Skapa ny stödbehovsperiod',
      removeConfirmation: 'Vill du ta bort stödbehovet?',
      errors: {
        invalidCoefficient: 'Felaktig koefficient.',
        conflict: 'Stödbehovet överlappar med ett annat stödbehov.',
        hardConflict:
          'Stödbehovet överlappar med ett annat stödbehovs startdatum.',
        autoCutWarning: 'Tidigare överlappande stödbehov avbryts automatiskt.'
      }
    },
    assistanceAction: {
      title: 'Stödåtgärder och åtgärder',
      modified: 'Senast redigerad',
      fields: {
        dateRange: 'Stödåtgärdernas giltighetstid',
        actions: 'Stödåtgärder',
        actionsByCategory: {
          DAYCARE: 'Småbarnspedagogikens stödåtgärder',
          PRESCHOOL: 'Förskolans stödåtgärder',
          OTHER: 'Andra stödåtgärder'
        },
        actionTypes: {
          OTHER: 'Annan stödåtgärd'
        },
        otherActionPlaceholder:
          'Du kan här skriva tilläggsinformation om andra stödåtgärder.',
        lastModifiedBy: (name: string) => `Redigerare ${name}.`
      },
      create: 'Skapa ny stödåtgärdsperiod',
      removeConfirmation: 'Vill du ta bort stödåtgärdsperioden?',
      errors: {
        conflict: 'Stödåtgärder överlappar med en annan period.',
        hardConflict:
          'Stödåtgärder överlappar med en annan periods startdatum.',
        autoCutWarning:
          'Tidigare överlappande stödåtgärder avbryts automatiskt.',
        startBeforeMinDate: (date: LocalDate) =>
          `Denna stödåtgärd kan börja tidigast ${date.format()}`,
        endAfterMaxDate: (date: LocalDate) =>
          `Denna stödåtgärd kan beviljas högst till och med ${date.format()}`
      }
    },
    childDocuments: {
      title: {
        internal: 'Pedagogiska dokument',
        decision: 'Andra beslut',
        external: 'Dokument för vårdnadshavare att fylla i'
      },
      table: {
        document: 'Dokument',
        status: 'Status',
        open: 'Öppna dokument',
        modified: 'Redigerad',
        modifiedBy: (name: string) => `Redigerare ${name}.`,
        unit: 'Enhet',
        valid: 'I kraft',
        published: 'Publicerad',
        publishedBy: (name: string) => `Publicerare ${name}.`,
        sent: 'Skickad',
        notSent: 'Inte skickad',
        answered: 'Besvarad',
        unanswered: 'Inte besvarad'
      },
      addNew: {
        internal: 'Skapa nytt pedagogiskt dokument',
        decision: 'Skapa nytt beslut',
        external: 'Skapa dokument för vårdnadshavare att fylla i'
      },
      select: 'Välj dokument',
      removeConfirmation: 'Vill du verkligen ta bort dokumentet?',
      confirmation:
        'Är du säker på att du vill öppna detta dokument för barnet? Alla dokument publiceras till vårdnadshavare och arkiveras automatiskt när verksamhetsperioden upphör',
      statuses: {
        DRAFT: 'Utkast',
        PREPARED: 'Upprättad',
        DECISION_PROPOSAL: 'Beslutsförslag',
        COMPLETED: 'Färdig'
      },
      decisions: {
        accept: 'Gör ett positivt beslut',
        acceptConfirmTitle: 'Vill du verkligen göra ett positivt beslut?',
        validityPeriod: 'Beviljas för tiden',
        reject: 'Gör ett negativt beslut',
        rejectConfirmTitle: 'Vill du verkligen göra ett negativt beslut?',
        annul: 'Annullera beslut',
        annulConfirmTitle: 'Vill du verkligen annullera beslutet?',
        annulInstructions:
          'Annullera beslutet endast av grundad anledning, t.ex. om beslutet har fattats av misstag till fel enhet. Meddela alltid vårdnadshavarna om annulleringen.',
        annulReasonLabel: 'Motivering för annullering av beslut',
        decisionNumber: 'Beslutsnummer',
        updateValidity: 'Korrigera beslutets giltighetstid',
        otherValidDecisions: {
          title: 'Andra giltiga beslut',
          description: (validity: DateRange) => (
            <P>
              Du håller på att göra ett positivt beslut.
              <br />
              Barnet har andra beslut som gäller när det nu fattade beslutet
              träder i kraft {validity.start.format().toString()}
            </P>
          ),
          label: 'Välj lämplig åtgärd för följande beslut*',
          options: {
            end: 'Avbryts',
            keep: 'Avbryts inte'
          }
        }
      },
      editor: {
        lockedErrorTitle: 'Dokumentet är tillfälligt låst',
        lockedError:
          'En annan användare redigerar dokumentet. Försök igen senare.',
        lockedErrorDetailed: (modifiedByName: string, opensAt: string) =>
          `Användaren ${modifiedByName} redigerar dokumentet. Dokumentets låsning frigörs ${opensAt} om redigeringen inte fortsätter. Försök igen senare.`,
        saveError: 'Att spara dokumentet misslyckades.',
        preview: 'Förhandsgranska',
        publish: 'Publicera till vårdnadshavare',
        publishConfirmTitle: 'Vill du verkligen publicera till vårdnadshavare?',
        publishConfirmText:
          'Vårdnadshavaren får se den nuvarande versionen. Ändringar du gör efter detta syns inte för vårdnadshavaren förrän du publicerar igen.',
        downloadPdf: 'Ladda ner som PDF-fil',
        archive: 'Arkivera',
        alreadyArchived: (archivedAt: HelsinkiDateTime) =>
          `Dokumentet är arkiverat ${archivedAt.toLocalDate().format()}`,
        archiveDisabledNotExternallyArchived:
          'Dokumentet har inte definierats för överföring till externt arkiv',
        archiveDisabledNotCompleted: 'Dokumentet är inte i färdig-status',
        goToNextStatus: {
          DRAFT: 'Publicera i utkast-status',
          PREPARED: 'Publicera i upprättad-status',
          CITIZEN_DRAFT: 'Skicka för kommuninvånare att fylla i',
          DECISION_PROPOSAL: 'Skicka till beslutsfattare',
          COMPLETED: 'Publicera i färdig-status'
        },
        goToNextStatusConfirmTitle: {
          DRAFT: 'Vill du verkligen publicera dokumentet i utkast-status?',
          PREPARED:
            'Vill du verkligen publicera dokumentet i upprättad-status?',
          CITIZEN_DRAFT:
            'Vill du verkligen publicera dokumentet i för kommuninvånare att fylla i-status?',
          DECISION_PROPOSAL:
            'Vill du verkligen skicka beslutsförslaget till beslutsfattare?',
          COMPLETED: 'Vill du verkligen publicera dokumentet i färdig-status?'
        },
        goToCompletedConfirmText:
          'Vårdnadshavaren får se den nuvarande versionen. Ett dokument i färdig-status kan inte längre redigeras. Endast huvudanvändare kan ångra detta.',
        extraConfirmCompletion:
          'Jag förstår att dokumentet inte kan redigeras efter detta',
        goToPrevStatus: {
          DRAFT: 'Återställ till utkast',
          PREPARED: 'Återställ till upprättad',
          CITIZEN_DRAFT: 'Återställ till för kommuninvånare att fylla i',
          DECISION_PROPOSAL: 'Återställ till beslutsförslag', // not applicable,
          COMPLETED: 'Återställ till färdig' // not applicable
        },
        goToPrevStatusConfirmTitle: {
          DRAFT: 'Vill du verkligen återställa dokumentet till utkast?',
          PREPARED: 'Vill du verkligen återställa dokumentet till upprättad?',
          CITIZEN_DRAFT:
            'Vill du verkligen återställa dokumentet till för kommuninvånare att fylla i?',
          DECISION_PROPOSAL:
            'Vill du verkligen återställa beslutet till beslutsförslag?', // not applicable,
          COMPLETED: 'Vill du verkligen återställa dokumentet till färdig?' // not applicable,
        },
        goBackToDraftConfirmText:
          'I utkastfasen kan du redigera dokumentets uppgifter.',
        deleteDraft: 'Ta bort utkast',
        deleteDraftConfirmTitle: 'Vill du verkligen ta bort utkastet?',
        fullyPublished: 'Dokumentets senaste version är publicerad',
        notFullyPublished: (publishedAt: HelsinkiDateTime | null) =>
          `Dokumentet har opublicerade ändringar${
            publishedAt ? ` (publicerad ${publishedAt.format()})` : ''
          }`,
        decisionMaker: 'Beslutsfattare',
        notSet: 'Inte angiven'
      }
    },
    assistanceNeedPreschoolDecision: {
      sectionTitle: 'Beslut om stöd i förskolan',
      statuses: {
        DRAFT: 'Utkast',
        NEEDS_WORK: 'Att korrigera',
        ACCEPTED: 'Godkänd',
        REJECTED: 'Avslagen',
        ANNULLED: 'Annullerad'
      },
      annulmentReason: 'Motivering för annullering av beslut',
      pageTitle: 'Beslut om stöd i förskolan',
      decisionNumber: 'Beslutsnummer',
      confidential: 'Sekretessbelagd',
      lawReference: 'OffL 24.1 §',
      types: {
        NEW: 'Särskilt stöd börjar',
        CONTINUING: 'Särskilt stöd fortsätter',
        TERMINATED: 'Särskilt stöd upphör'
      },
      decidedAssistance: 'Stöd som beslutas',
      type: 'Det särskilda stödets status',
      validFrom: 'I kraft från och med',
      validFromInfo: (): React.ReactNode => (
        <ul>
          <li>
            Särskilt stöd börjar noteras från och med datumet för hörandet av
            vårdnadshavarna eller från och med förskolans startdatum (om
            beslutet fattas innan förskolan börjar)
          </li>
          <li>
            Särskilt stöd fortsätter noteras när barnet byter
            förskoleenhet/stödformer (t.ex. barnet flyttas till en specialgrupp)
            blir föremål för ändringar/får beslut om uppskjuten skolstart
          </li>
          <li>
            Särskilt stöd upphör noteras när det särskilda stödet i förskolan
            avbryts
          </li>
        </ul>
      ),
      validTo: 'I kraft t.o.m.',
      extendedCompulsoryEducationSection: 'Förlängd läroplikt',
      extendedCompulsoryEducation: 'Ja, barnet har förlängd läroplikt',
      no: 'Nej',
      extendedCompulsoryEducationInfo: 'Mer information om förlängd läroplikt',
      extendedCompulsoryEducationInfoInfo: (): React.ReactNode => 'info',
      grantedAssistanceSection:
        'Tolknings- och assistenttjänster eller särskilda hjälpmedel som beviljas',
      grantedAssistanceSectionInfo: (): React.ReactNode =>
        'Antecknas om barnet beviljas assistans-/tolkningstjänster eller hjälpmedel. I motiveringarna antecknas "Barnet beviljas enligt grundskolelagen 31§ assistanstjänster/nödvändiga särskilda hjälpmedel/tolkningstjänster/avvikande ordnande av undervisning" samt en kort motivering.',
      grantedAssistanceService: 'Barnet beviljas assistenttjänster',
      grantedInterpretationService: 'Barnet beviljas tolkningstjänster',
      grantedAssistiveDevices: 'Barnet beviljas särskilda hjälpmedel',
      grantedNothing: 'Inget val',
      grantedServicesBasis:
        'Motiveringar till de tolknings- och assistenttjänster och hjälpmedel som beviljas',
      selectedUnit: 'Plats för förskoleundervisning',
      primaryGroup: 'Huvudsaklig undervisningsgrupp',
      primaryGroupInfo: (): React.ReactNode =>
        'Anteckna här gruppens form specialgrupp/pedagogiskt förstärkt grupp/förskolegrupp/grupp för 3-5-åringar.',
      decisionBasis: 'Motiveringar till beslutet',
      decisionBasisInfo: (): React.ReactNode =>
        'Anteckna vilka utredningar beslutet grundar sig på (pedagogisk utredning och/eller psykologiskt eller medicinskt utlåtande samt datum). Om barnet har beviljats förlängd läroplikt, antecknas "barnet har ett beslut om förlängd läroplikt datum."',
      documentBasis: 'Handlingar som beslutet grundar sig på',
      documentBasisInfo: (): React.ReactNode =>
        'Som bilaga kan också finnas vårdnadshavarens specificerade fullmakt, vårdnadshavarens namn och datum.',
      basisDocumentPedagogicalReport: 'Pedagogisk utredning',
      basisDocumentPsychologistStatement: 'Psykologutlåtande',
      basisDocumentDoctorStatement: 'Läkarutlåtande',
      basisDocumentSocialReport: 'Social utredning',
      basisDocumentOtherOrMissing: 'Bilaga saknas, eller annan bilaga, vilken?',
      basisDocumentsInfo: 'Ytterligare information om bilagor',
      guardianCollaborationSection: 'Samarbete med vårdnadshavare',
      guardiansHeardOn: 'Datum för hörande av vårdnadshavare',
      heardGuardians: 'Vårdnadshavare som har hörts och hörandesätt',
      heardGuardiansInfo: (): React.ReactNode =>
        'Anteckna här med vilka medel vårdnadshavaren har hörts (t.ex. möte, distansförbindelse, vårdnadshavarnas skriftliga svar, fullmakt). Om vårdnadshavaren inte har hörts, anteckna här en utredning om hur och när hen har kallats till hörande.',
      otherRepresentative:
        'Annan laglig företrädare (namn, telefonnummer och hörandesätt)',
      viewOfGuardians: 'Vårdnadshavarnas syn på det föreslagna stödet',
      viewOfGuardiansInfo: (): React.ReactNode => (
        <div>
          <p>
            Anteckna tydligt vårdnadshavarnas åsikt. Om vårdnadshavarna har
            olika åsikter om de sökta undervisningsarrangemangen, ska
            motiveringarna antecknas noggrant.
          </p>
          <p>
            Anteckna här också barnets åsikt i ärendet eller anteckna
            &quot;barnet kan inte på grund av sin ålder och/eller
            utvecklingsnivå uttrycka sin åsikt&quot;.
          </p>
        </div>
      ),
      responsiblePeople: 'Ansvariga personer',
      preparer: 'Beslutsberedare',
      decisionMaker: 'Beslutsfattare',
      employeeTitle: 'Titel',
      phone: 'Telefonnummer',
      legalInstructions: 'Tillämpade rättsregler',
      legalInstructionsText: 'Grundskolelagen 17 §',
      legalInstructionsTextExtendedCompulsoryEducation: 'Läropliktslag 2 §',
      jurisdiction: 'Behörighet',
      jurisdictionText:
        'Delegationsbeslut om den finskspråkiga småbarnspedagogikens samt sektorns för tillväxt och lärande stabs beslutsrätt A del 3 § 1 punkt' as
          | string
          | React.ReactNode,
      disclaimer: null as string | null,
      appealInstructionsTitle: 'Anvisning för begäran om omprövning',
      appealInstructions: (
        <>
          <P>
            Den som är missnöjd med detta beslut kan göra en skriftlig begäran
            om omprövning. Beslutet kan inte överklagas genom besvär till
            domstol.
          </P>

          <H3>Rätt att begära omprövning</H3>
          <P>
            Omprövning får begäras av den som beslutet riktar sig till eller
            vars rätt, skyldighet eller fördel direkt påverkas av beslutet
            (part).
          </P>

          <H3>Tid för begäran om omprövning</H3>
          <P>
            Begäran om omprövning ska göras inom 14 dagar från delfåendet av
            beslutet.
          </P>
          <P>
            Begäran om omprövning ska ha kommit fram till
            Regionförvaltningsverket i Södra Finland senast den sista dagen av
            fristen före Regionförvaltningsverket i Södra Finlands öppettid
            upphör.
          </P>
          <P>
            En part anses ha fått del av beslutet, om inte annat visas, sju
            dagar efter att brevet avsändes eller den dag som antecknats i
            mottagningsbeviset eller delgivningsbeviset.
          </P>
          <P>
            Vid användning av vanlig elektronisk delgivning anses parten ha fått
            del av beslutet, om inte annat visas, den tredje dagen efter att
            meddelandet sändes.
          </P>
          <P>
            Dagen för delfående räknas inte in i tiden för begäran om
            omprövning. Om den sista dagen för begäran om omprövning är en
            helgdag, självständighetsdagen, första maj, jul- eller
            midsommarafton eller en helgfri lördag, får begäran om omprövning
            göras den första vardagen därefter.
          </P>

          <H3>Omprövningsmyndighet</H3>
          <P>
            Den myndighet till vilken begäran om omprövning riktas är
            Regionförvaltningsverket i Södra Finland
          </P>
          <P>
            Postadress: PB 1, 13035 RFV
            <br />
            Besöksadress för enheten i Helsingfors: Bangårdsvägen 9, 00520
            Helsingfors
            <br />
            E-postadress: registratur.sodra@rfv.fi
            <br />
            Telefonväxel: 0295 016 000
            <br />
            Faxnummer: 0295 016 661
            <br />
            Ämbetsverkets öppettider: må-fr 8.00–16.15
          </P>
          <H3>Begärans form och innehåll</H3>
          <P>
            Begäran om omprövning ska göras skriftligt. Även elektroniska
            dokument uppfyller kravet på skriftlig form.
          </P>
          <P noMargin>I begäran om omprövning ska anges</P>
          <ul>
            <li>det beslut som begäran om omprövning gäller,</li>
            <li>på vilket sätt beslutet ska omprövas,</li>
            <li>på vilka grunder omprövning begärs</li>
          </ul>
          <P>
            I begäran om omprövning ska dessutom anges uppgiftsgivarens namn,
            hemkommun, postadress, telefonnummer och övriga kontaktuppgifter som
            behövs för skötseln av ärendet.
          </P>
          <P>
            Om omprövningsbeslutet kan delges elektroniskt som ett meddelande,
            ber vi om att även e-postadress anges som kontaktuppgift.
          </P>
          <P>
            Om den som gjort begäran om omprövning inte själv sköter sin talan,
            utan genom sin lagliga företrädare eller ombud, eller om någon annan
            person har upprättat begäran, ska även denna persons namn och
            hemkommun anges i begäran om omprövning.
          </P>
          <P noMargin>Till begäran om omprövning ska fogas</P>
          <ul>
            <li>det beslut som överklagas i original eller som kopia</li>
            <li>
              intyg över vilken dag beslutet har delgetts, eller annan utredning
              om när tiden för begäran om omprövning började
            </li>
            <li>
              de handlingar som den som begär omprövning åberopar till stöd för
              sin begäran, om de inte redan tidigare har lämnats till
              myndigheten.
            </li>
          </ul>
        </>
      )
    },
    assistanceNeedDecision: {
      pageTitle: 'Beslut om stöd i småbarnspedagogiken',
      annulmentReason: 'Motivering för annullering av beslut',
      sectionTitle: 'Beslut om stöd i småbarnspedagogiken',
      deprecated:
        'Här hittar du tills vidare beslut om stöd enligt den gamla modellen. Nya beslut görs i avsnittet Barnets dokument > Andra beslut.',
      description:
        'Godkända och avslagna beslut om stöd syns för vårdnadshavaren i eVaka.',
      table: {
        form: 'Dokument',
        inEffect: 'I kraft',
        unit: 'Enhet',
        sentToDecisionMaker: 'Skickad till beslutsfattare',
        decisionMadeOn: 'Beslut fattat',
        status: 'Status'
      },
      create: 'Skapa nytt beslut',
      modal: {
        delete: 'Ta bort beslut',
        title: 'Ska beslutet tas bort?',
        description:
          'Vill du verkligen ta bort beslutsmallen? Alla uppgifter som fyllts i i beslutsmallen går förlorade.'
      },
      validation: {
        title: 'Granskning av beslutsförslag',
        description:
          'Var god och granska följande uppgifter från beslutsförslaget före förhandsgranskning:'
      },
      genericPlaceholder: 'Skriv',
      formLanguage: 'Blankett språk',
      neededTypesOfAssistance: 'De former av stöd barnet behöver',
      pedagogicalMotivation: 'Pedagogiska stödformer och motiveringar',
      pedagogicalMotivationInfo:
        'Anteckna här förslaget på de pedagogiska stödformer barnet behöver, t.ex. lösningar relaterade till dagens struktur, dagsrytm och lärmiljöer samt pedagogiska och specialpedagogiska lösningar. Motivera kort varför barnet får dessa stödformer.',
      structuralMotivation: 'Strukturella stödformer och motiveringar',
      structuralMotivationInfo:
        'Välj de strukturella stödformer barnet behöver. Motivera varför barnet får dessa stödformer.',
      structuralMotivationOptions: {
        smallerGroup: 'Minskning av gruppstorlek',
        specialGroup: 'Specialgrupp',
        smallGroup: 'Liten grupp',
        groupAssistant: 'Gruppspecifik assistent',
        childAssistant: 'Barnspecifik assistent',
        additionalStaff: 'Ökning av personalresurs'
      },
      structuralMotivationPlaceholder:
        'Beskrivning och motiveringar för valda strukturella stödformer',
      careMotivation: 'Vårdrelaterade stödformer och motiveringar',
      careMotivationInfo:
        'Anteckna här de vårdrelaterade stödformer barnet behöver, t.ex. metoder för barnets vård, omvårdnad och assistans med beaktande av vård av långvariga sjukdomar, medicinering, kost, förflyttning och relaterade hjälpmedel. Motivera varför barnet får dessa stödformer.',
      serviceOptions: {
        consultationSpecialEd:
          'Konsultation av speciallärare i småbarnspedagogik',
        partTimeSpecialEd:
          'Deltidsundervisning av speciallärare i småbarnspedagogik',
        fullTimeSpecialEd:
          'Heltidsundervisning av speciallärare i småbarnspedagogik',
        interpretationAndAssistanceServices: 'Tolknings- och assistanstjänster',
        specialAides: 'Hjälpmedel'
      },
      services: 'Stödtjänster och motiveringar',
      servicesInfo:
        'Välj här de stödtjänster som föreslås för barnet. Motivera varför barnet får dessa stödtjänster',
      servicesPlaceholder: 'Motiveringar för valda stödtjänster',
      collaborationWithGuardians: 'Samarbete med vårdnadshavare',
      guardiansHeardOn: 'Datum för hörande av vårdnadshavare',
      guardiansHeard: 'Vårdnadshavare som hörts och hörandesätt',
      guardiansHeardInfo:
        'Anteckna här med vilka medel vårdnadshavaren har hörts (t.ex. möte, distansförbindelse, vårdnadshavarens skriftliga svar). Om vårdnadshavaren inte har hörts, anteckna här en utredning om hur och när hen har kallats för att höras, och hur och när barnets plan för småbarnspedagogik har delgivits vårdnadshavaren.\nAlla barnets vårdnadshavare ska ha möjlighet att höras. Vårdnadshavaren kan vid behov ge fullmakt åt en annan vårdnadshavare att representera sig.',
      guardiansHeardValidation: 'Alla vårdnadshavare ska ha hörts.',
      oneAssistanceLevel: 'Välj endast en stödnivå',
      viewOfTheGuardians: 'Vårdnadshavarnas syn på det föreslagna stödet',
      viewOfTheGuardiansInfo:
        'Anteckna här vårdnadshavarnas syn på det stöd som föreslås för barnet.',
      otherLegalRepresentation:
        'Annan laglig företrädare (namn, telefonnummer och hörandesätt)',
      decisionAndValidity: 'Stödnivå som ska beslutas och giltighetstid',
      futureLevelOfAssistance: 'Barnets stödnivå framöver',
      assistanceLevel: {
        assistanceEnds: 'Särskilt/intensifierat stöd upphör',
        assistanceServicesForTime: 'Stödtjänster under beslutets giltighetstid',
        enhancedAssistance: 'Intensifierat stöd',
        specialAssistance: 'Särskilt stöd'
      },
      startDate: 'I kraft från och med',
      startDateIndefiniteInfo:
        'Stödet gäller tills vidare från och med startdatumet.',
      startDateInfo:
        'Barnets stöd granskas alltid när stödbehovet förändras och minst en gång per år.',
      endDate: 'Beslut i kraft t.o.m.',
      endDateServices: 'Beslut i kraft för stödtjänster t.o.m.',
      selectedUnit: 'Småbarnspedagogikenhet vald för beslutet',
      unitMayChange:
        'Under semesterperioder kan platsen och sättet för ordnandet av stöd förändras.',
      motivationForDecision: 'Motiveringar för barnets stödnivå',
      legalInstructions: 'Tillämpade rättsregler',
      legalInstructionsText: 'Lagen om småbarnspedagogik, kapitel 3 a',
      jurisdiction: 'Behörighet',
      jurisdictionText: (): React.ReactNode =>
        'Delegationsbeslut om den finskspråkiga småbarnspedagogikens samt sektorns för tillväxt och lärande stabs beslutsrätt A del 3 § 3 punkt',
      personsResponsible: 'Ansvariga personer',
      preparator: 'Beslutsberedare',
      decisionMaker: 'Beslutsfattare',
      title: 'Titel',
      tel: 'Telefonnummer',
      disclaimer:
        'Enligt 15 e § i lagen om småbarnspedagogik kan detta beslut verkställas trots ändringssökande.',
      decisionNumber: 'Beslutsnummer',
      endDateNotKnown: 'Slutdatum för stödtjänst är okänt',
      statuses: {
        DRAFT: 'Utkast',
        NEEDS_WORK: 'Att korrigera',
        ACCEPTED: 'Godkänd',
        REJECTED: 'Avslagen',
        ANNULLED: 'Annullerad'
      },
      confidential: 'Sekretessbelagd',
      lawReference: 'Lagen om småbarnspedagogik 40 §',
      noRecord: 'Ingen anteckning',
      leavePage: 'Lämna',
      preview: 'Förhandsgranska',
      modifyDecision: 'Redigera',
      sendToDecisionMaker: 'Skicka till beslutsfattare',
      revertToUnsent: 'Återställ till oskickad',
      sentToDecisionMaker: 'Skickad till beslutsfattare',
      appealInstructionsTitle: 'Anvisning för begäran om omprövning',
      appealInstructions: (
        <>
          <H3>Rätt att begära omprövning</H3>
          <P>
            Omprövning får begäras av den som beslutet riktar sig till eller
            vars rätt, skyldighet eller fördel direkt påverkas av beslutet
            (part).
          </P>
          <H3>Tid för begäran om omprövning</H3>
          <P>
            Begäran om omprövning ska göras inom 30 dagar från delfåendet av
            beslutet.
          </P>
          <H3>Delfående</H3>
          <P>
            En part anses ha fått del av beslutet, om inte annat visas, sju
            dagar efter att brevet avsändes eller den dag som antecknats i
            mottagningsbeviset eller delgivningsbeviset. Vid användning av
            vanlig elektronisk delgivning anses parten ha fått del av beslutet,
            om inte annat visas, den tredje dagen efter att meddelandet sändes.
            Dagen för delfående räknas inte in i fristen. Om fristens sista dag
            är en helgdag, självständighetsdagen, första maj, jul- eller
            midsommarafton eller en helgfri lördag, får uppgiften lämnas in den
            första vardagen därefter.
          </P>
          <H3>Omprövningsmyndighet</H3>
          <P>Omprövning begärs hos Regionförvaltningsverket i Södra Finland.</P>
          <P>
            Regionförvaltningsverket i Södra Finland
            <br />
            Besöksadress: Bangårdsvägen 9, 00520 Helsingfors
            <br />
            Ämbetsverkets öppettider: må-fr 8.00–16.15
            <br />
            Postadress: PB 1, 13035 RFV
            <br />
            E-post: registratur.sodra@rfv.fi
            <br />
            Fax: 0295 016 661
            <br />
            Telefon: 0295 016 000
          </P>
          <H3>Begärans form och innehåll</H3>
          <P>
            Begäran om omprövning ska göras skriftligt. Även elektroniska
            dokument uppfyller kravet på skriftlig form.
          </P>
          <P noMargin>I begäran om omprövning ska anges</P>
          <ul>
            <li>
              Den som begär omprövnings namn, hemkommun, postadress,
              telefonnummer och övriga kontaktuppgifter som behövs för skötseln
              av ärendet
            </li>
            <li>det beslut som överklagas</li>
            <li>
              till vilka delar omprövning begärs och vilken omprövning som krävs
            </li>
            <li>grunderna för kravet</li>
          </ul>
          <P>
            Om omprövningsbeslutet kan delges elektroniskt som ett meddelande,
            ber vi om att även e-postadress anges som kontaktuppgift.
          </P>
          <P>
            Om den som gjort begäran om omprövning inte själv sköter sin talan,
            utan genom sin lagliga företrädare eller ombud, eller om någon annan
            person har upprättat begäran om omprövning, ska även denna persons
            namn och hemkommun anges i begäran.
          </P>
          <P noMargin>Till begäran om omprövning ska fogas</P>
          <ul>
            <li>det beslut som överklagas i original eller som kopia</li>
            <li>
              intyg över vilken dag beslutet har delgetts, eller annan utredning
              om när tiden för begäran om omprövning började
            </li>
            <li>
              de handlingar som den som begär omprövning åberopar till stöd för
              sin begäran, om de inte redan tidigare har lämnats till
              myndigheten.
            </li>
          </ul>
          <H3>Lämnande av begäran om omprövning</H3>
          <P>
            Skrivelsen med begäran om omprövning ska lämnas in till
            omprövningsmyndigheten inom tiden för begäran. Skrivelsen ska ha
            kommit fram den sista dagen av fristen före ämbetsverkets öppettid
            upphör. Att skicka begäran om omprövning per post eller elektroniskt
            sker på avsändarens eget ansvar.
          </P>
        </>
      )
    },
    assistanceNeedVoucherCoefficient: {
      actions: 'Åtgärder',
      create: 'Sätt ny servicesedelkoefficient',
      deleteModal: {
        title: 'Ska servicesedelkoefficienten tas bort?',
        description:
          'Vill du verkligen ta bort servicesedelkoefficienten? Ett nytt värdebeslut skapas inte för kunden även om koefficienten tas bort, utan du måste göra ett nytt retroaktivt värdebeslut.',
        delete: 'Ta bort koefficient'
      },
      factor: 'Koefficient',
      form: {
        coefficient: 'Servicesedelkoefficient (tal)',
        editTitle: 'Redigera servicesedelkoefficient',
        errors: {
          previousOverlap:
            'Tidigare överlappande servicesedelkoefficient avbryts automatiskt.',
          upcomingOverlap:
            'Kommande överlappande servicesedelkoefficient flyttas fram automatiskt.',
          fullOverlap:
            'Tidigare överlappande servicesedelkoefficient tas bort automatiskt.',
          coefficientRange: 'Koefficienten ska vara mellan 1-10'
        },
        title: 'Sätt ny servicesedelkoefficient',
        titleInfo:
          'Välj giltighetsdatum för servicesedelkoefficienten enligt beslutet om stödbehov.',
        validityPeriod: 'Servicesedelkoefficient i kraft'
      },
      lastModified: 'Senast redigerad',
      lastModifiedBy: (name: string) => `Redigerare ${name}.`,
      sectionTitle: 'Servicesedelkoefficient',
      status: 'Status',
      unknown: 'Okänd',
      validityPeriod: 'Giltighetstid',
      voucherCoefficient: 'Servicesedelkoefficient'
    },
    application: {
      title: 'Ansökningar',
      guardian: 'Ansökan gjord av',
      preferredUnit: 'Ansökt enhet',
      startDate: 'Ansökt startdatum',
      sentDate: 'Ansökan anländ',
      type: 'Serviceform',
      types: {
        PRESCHOOL: 'Förskola',
        PRESCHOOL_DAYCARE: 'Anknuten småbarnspedagogik',
        PREPARATORY_EDUCATION: 'Förberedande undervisning',
        DAYCARE: 'Småbarnspedagogik',
        DAYCARE_PART_TIME: 'Småbarnspedagogik',
        CLUB: 'Klubb'
      },
      status: 'Status',
      statuses: {
        CREATED: 'Utkast',
        SENT: 'Mottagen',
        WAITING_PLACEMENT: 'Väntar på placering',
        WAITING_DECISION: 'Beslut under beredning',
        WAITING_UNIT_CONFIRMATION: 'Väntar på chefens godkännande',
        WAITING_MAILING: 'Väntar på utskick',
        WAITING_CONFIRMATION: 'Bekräftas av vårdnadshavare',
        REJECTED: 'Plats nekad',
        ACTIVE: 'Plats mottagen',
        CANCELLED: 'Avförd från behandling'
      },
      open: 'Öppna ansökan',
      create: {
        createButton: 'Skapa ny ansökan',
        modalTitle: 'Ny ansökan',
        applier: 'Ansökningslämnare',
        personTypes: {
          GUARDIAN: 'Välj från vårdnadshavare',
          DB_SEARCH: 'Sök i kunduppgifter',
          VTJ: 'Sök i BIS',
          NEW_NO_SSN: 'Skapa ny utan personbeteckning'
        },
        applicationType: 'Ansökningstyp',
        applicationTypes: {
          DAYCARE: 'Ansökan om småbarnspedagogik',
          PRESCHOOL: 'Ansökan om förskola',
          CLUB: 'Klubbansökan'
        },
        sentDate: 'Ansökan mottagen',
        hideFromGuardian: 'Dölj ansökan för vårdnadshavare',
        transferApplication: 'Överflyttningsansökan'
      }
    },
    additionalInformation: {
      title: 'Tilläggsinformation',
      allergies: 'Allergier',
      diet: 'Specialdiet',
      additionalInfo: 'Tilläggsinformation',
      preferredName: 'Tilltalsnamn',
      medication: 'Medicinering'
    },
    income: {
      title: 'Inkomstuppgifter'
    },
    feeAlteration: {
      title: 'Nedsättningar, befrielser och höjningar',
      error: 'Laddning av avgiftsändringar misslyckades',
      create: 'Skapa ny avgiftsändring',
      updateError: 'Sparande av avgiftsändring misslyckades',
      deleteError: 'Borttagning av avgiftsändring misslyckades',
      confirmDelete: 'Vill du ta bort avgiftsändringen?',
      lastModifiedAt: (date: string) => `Senast redigerad ${date}`,
      lastModifiedBy: (name: string) => `Redigerare: ${name}`,
      editor: {
        titleNew: 'Lägg till ny nedsättning eller höjning',
        titleEdit: 'Redigera nedsättning eller höjning',
        alterationType: 'Ändringstyp',
        alterationTypePlaceholder: 'Ändringstyp',
        validDuring: 'Beviljas för tiden',
        notes: 'Tilläggsinformation',
        cancel: 'Avbryt',
        save: 'Spara'
      },
      types: {
        DISCOUNT: 'Nedsättning',
        INCREASE: 'Höjning',
        RELIEF: 'Lättnad'
      },
      attachmentsTitle: 'Bilagor',
      employeeAttachments: {
        title: 'Lägg till bilagor',
        description:
          'Här kan du lägga till bilagor som kunden har lämnat för avgiftsnedsättningar, befrielser eller höjningar.'
      }
    },
    placements: {
      title: 'Placeringar och servicebehov',
      placements: 'Placeringar',
      rowTitle: 'Placeringsbeslut i kraft',
      startDate: 'Startdatum',
      endDate: 'Slutdatum',
      createdBy: 'Skapare',
      source: 'Skapandekälla',
      sourceOptions: {
        CITIZEN: 'Ansökan från kommuninvånare',
        EMPLOYEE_MANUAL: 'Anställd manuellt',
        EMPLOYEE_PAPER: 'Anställd från pappersansökan',
        SYSTEM: 'System',
        UNKNOWN: 'Information inte tillgänglig'
      },
      modifiedAt: 'Redigerad',
      modifiedBy: 'Redigerare',
      terminatedByGuardian: 'Uppsagd av vårdnadshavare',
      terminated: 'Uppsagd',
      area: 'Område',
      daycareUnit: 'Enhet',
      daycareGroups: 'Grupp',
      daycareGroupMissing: 'Inte grupperad',
      type: 'Placeringstyp',
      providerType: 'Ordnandeform',
      updatedAt: 'Senast uppdaterad',
      serviceNeedMissing1: 'Placeringen saknar servicebehov för',
      serviceNeedMissing2:
        'dag(ar). Anteckna servicebehov för hela placeringstiden.',
      serviceNeedMissingTooltip1: 'Servicebehov saknas för',
      serviceNeedMissingTooltip2: 'dag(ar).',
      deletePlacement: {
        btn: 'Ta bort placering',
        confirmTitle: 'Vill du verkligen avbryta denna placering?',
        hasDependingBackupCares:
          'Barnets reservplacering är beroende av denna placering, så borttagning av denna placering kan ändra eller ta bort reservplaceringen.'
      },
      createPlacement: {
        btn: 'Skapa ny placering',
        title: 'Ny placering',
        text: 'Ett beslut kan inte skickas för denna placering. Om placeringen överlappar med barnets tidigare skapade placeringar, förkortas eller tas dessa placeringar bort automatiskt.',
        temporaryDaycareWarning:
          'OBS! Använd inte vid skapande av reservplacering!',
        startDateMissing: 'Startdatum är obligatorisk uppgift',
        unitMissing: 'Enhet saknas',
        preschoolTermNotOpen: 'Placeringen måste vara under förskoleperioden',
        preschoolExtendedTermNotOpen:
          'Placeringen måste vara under förskoleperioden',
        placeGuarantee: {
          title: 'Småbarnspedagogikplatsgaranti',
          info: 'Framtida placering är relaterad till småbarnspedagogikplatsgarantin'
        }
      },
      error: {
        conflict: {
          title: 'Datumet kunde inte redigeras',
          text:
            'Barnet har en placering som överlappar med' +
            ' de datum du nu angett. Du kan gå tillbaka för att redigera' +
            ' de datum du angett eller kontakta huvudanvändaren.'
        }
      },
      warning: {
        overlap: 'Det finns redan en placering för denna tid',
        ghostUnit: 'Enheten är markerad som spökenhet',
        backupCareDepends:
          'Reservplaceringen är beroende av denna placering, och den ändrade perioden kan ta bort eller ändra reservplaceringen.'
      },
      serviceNeeds: {
        title: 'Placeringens servicebehov',
        period: 'Period',
        description: 'Beskrivning',
        shiftCare: 'Kväll/Skift',
        shiftCareTypes: {
          NONE: 'Nej',
          INTERMITTENT: 'Sporadisk',
          FULL: 'Ja'
        },
        partWeek: 'Delvecka',
        confirmed: 'Bekräftad',
        createNewBtn: 'Skapa nytt servicebehov',
        addNewBtn: 'Lägg till servicebehov',
        optionPlaceholder: 'Välj...',
        missing: 'Saknat servicebehov',
        deleteServiceNeed: {
          btn: 'Ta bort servicebehov',
          confirmTitle: 'Vill du verkligen ta bort detta servicebehov?'
        },
        overlapWarning: {
          title: 'Servicebehoven överlappar',
          message:
            'Det servicebehov du angett överlappar med ett tidigare angivet. Om du bekräftar det nu angivna servicebehovet kommer det tidigare angivna servicebehovet att avbrytas automatiskt för den överlappande tiden.'
        },
        optionStartNotValidWarningTitle: (validFrom: LocalDate) =>
          `Den valda servicebehovstypen är tillgänglig först från och med ${validFrom.format()}`,
        optionEndNotValidWarningTitle: (validTo: LocalDate) =>
          `Den valda servicebehovstypen är endast tillgänglig till och med ${validTo.format()}`,
        optionStartEndNotValidWarningTitle: (validity: FiniteDateRange) =>
          `Den valda servicebehovstypen är tillgänglig under perioden ${validity.format()}`,
        notFullyValidOptionWarning:
          'Den valda servicebehovstypen måste vara tillgänglig för hela perioden. Skapa servicebehovet vid behov i två delar.'
      }
    },
    absenceApplications: {
      title: 'Ansökningar om frånvaro från förskolan',
      absenceApplication: 'Frånvaroansökan',
      range: 'Frånvaroperiod',
      createdBy: 'Ansökan gjord av',
      description: 'Orsak till frånvaro',
      acceptInfo:
        'Om du godkänner förslaget kommer barnet automatiskt att markeras som frånvarande för den tid vårdnadshavaren ansökt om.',
      reject: 'Neka ansökan',
      accept: 'Godkänn ansökan',
      list: 'Tidigare ansökningar',
      status: 'Status',
      statusText: {
        WAITING_DECISION: 'Väntar på beslut',
        ACCEPTED: 'Godkänd',
        REJECTED: 'Nekad'
      },
      rejectedReason: 'Orsak',
      rejectModal: {
        title: 'Nekande av ansökan om frånvaro från förskolan',
        reason: 'Orsak till nekande'
      },
      userType: {
        SYSTEM: 'system',
        CITIZEN: 'vårdnadshavare',
        EMPLOYEE: 'anställd',
        MOBILE_DEVICE: 'mobil',
        UNKNOWN: 'okänd'
      }
    },
    serviceApplications: {
      title: 'Ansökningar om ändring av servicebehov',
      applicationTitle: 'Ansökan om ändring av servicebehov',
      sentAt: 'Skickad',
      sentBy: 'Sökande',
      startDate: 'Föreslagen startdag',
      serviceNeed: 'Föreslagt servicebehov',
      additionalInfo: 'Tilläggsinformation',
      status: 'Status',
      decision: {
        statuses: {
          ACCEPTED: 'Godkänd',
          REJECTED: 'Nekad'
        },
        rejectedReason: 'Grund för nekande',
        accept: 'Godkänn',
        reject: 'Neka',
        confirmAcceptTitle: 'Ska ansökan om nytt servicebehov godkännas?',
        confirmAcceptText: (range: FiniteDateRange, placementChange: boolean) =>
          `Ny ${placementChange ? 'placering och ' : ''}servicebehov skapas för perioden ${range.format()}.`,
        shiftCareLabel: 'Kväll/skiftvård',
        shiftCareCheckbox: 'Barnet har rätt till kväll/skiftvård',
        partWeekLabel: 'Delvecka',
        partWeekCheckbox: 'Servicebehovet är delvecka',
        confirmAcceptBtn: 'Bekräfta',
        confirmRejectTitle: 'Nekande av ansökan'
      },
      decidedApplications: 'Behandlade ansökningar',
      noApplications: 'Inga ansökningar'
    },
    fridgeParents: {
      title: 'Huvudmän',
      name: 'Namn',
      ssn: 'Personbeteckning',
      startDate: 'Från och med',
      endDate: 'Till och med',
      status: 'Status'
    },
    fosterParents: {
      title: 'Fosterföräldrar',
      name: 'Namn',
      ssn: 'Personbeteckning',
      startDate: 'Från och med',
      endDate: 'Till och med',
      status: 'Status'
    },
    backupCares: {
      title: 'Reservplaceringar',
      remove: 'Vill du ta bort reservplaceringen?',
      editing: 'under redigering',
      create: 'Skapa ny reservplacering',
      dateRange: 'Reservplacering för perioden',
      unit: 'Enhet',
      validationNoMatchingPlacement:
        'Reservplaceringen är inte under någon av barnets placeringar.',
      validationChildAlreadyInOtherUnit:
        'Barnet är redan inskrivet i en annan enhet.',
      validationBackupCareNotOpen:
        'Enheten är inte öppen under hela reservplaceringsperioden.'
    },
    backupPickups: {
      title: 'Reservhämtare',
      name: 'Reservhämtarens namn',
      phone: 'Telefonnummer',
      add: 'Lägg till reservhämtare',
      edit: 'Redigera reservhämtarens uppgifter',
      removeConfirmation: 'Vill du verkligen ta bort reservhämtaren?'
    },
    childDocumentsSectionTitle: 'Barnets dokument',
    pedagogicalDocument: {
      create: 'Lägg till ny',
      created: 'Tillagd',
      createdBy: (name: string) => `Tillagd av: ${name}`,
      date: 'Datum',
      descriptionInfo: '',
      description: 'Pedagogisk beskrivning',
      document: 'Dokument',
      documentInfo: '',
      explanation: '',
      explanationInfo: '',
      lastModified: 'Senast redigerad',
      lastModifiedBy: (name: string) => `Redigerare: ${name}`,
      removeConfirmation: 'Vill du ta bort dokumentet?',
      removeConfirmationText:
        'Vill du verkligen ta bort det pedagogiska dokumentet och dess beskrivningstext? Borttagningen kan inte ångras, och dokumentet tas bort från vyn även för vårdnadshavaren.',
      title: 'Pedagogisk dokumentation'
    }
  },
  personSearch: {
    search: 'Sök med personbeteckning',
    searchByName: 'Sök med personbeteckning eller namn',
    notFound: 'Person hittades inte',
    inputPlaceholder: 'Sök med namn, adress eller personbeteckning',
    age: 'Ålder',
    address: 'Adress',
    maxResultsFound: 'Begränsa sökningen för att se andra resultat',
    socialSecurityNumber: 'Personbeteckning',
    newAdult: 'Skapa vuxen utan personbeteckning',
    newChild: 'Skapa barn utan personbeteckning',
    addPersonFromVTJ: {
      title: 'Importera person från BRC',
      modalConfirmLabel: 'Importera person',
      ssnLabel: 'Personbeteckning',
      restrictedDetails: 'Personen har spärrmarkering',
      badRequest: 'Ogiltig personbeteckning',
      notFound: 'Inga resultat',
      unexpectedError: 'Hämtning av personuppgifter misslyckades'
    },
    createNewPerson: {
      title: 'Skapa person utan personbeteckning',
      modalConfirmLabel: 'Skapa person',
      form: {
        firstName: 'Förnamn',
        lastName: 'Efternamn',
        dateOfBirth: 'Födelsedatum',
        address: 'Adress',
        streetAddress: 'Gatuadress',
        postalCode: 'Postnummer',
        postOffice: 'Postort',
        phone: 'Telefon',
        email: 'E-post'
      }
    }
  },
  personProfile: {
    restrictedDetails: 'Spärrmarkering',
    asChild: 'Visa som barn',
    timeline: 'Tidslinje',
    personDetails: 'Person- och kontaktuppgifter',
    addSsn: 'Ange personbeteckning',
    noSsn: 'Utan personbeteckning',
    ssnAddingDisabledCheckbox:
      'Endast huvudanvändare har rätt att ange personbeteckning för barn',
    ssnAddingDisabledInfo:
      'Användare för servicerådgivning och ekonomi får inte ange personbeteckning för barn. När personbeteckning saknas har barnet ingen vårdnadsrelation. Om personbeteckning ska anges senare måste barnets tidigare dokument tas bort från systemet.',
    ssnInvalid: 'Ogiltig personbeteckning',
    ssnConflict: 'Denna användare finns redan i systemet.',
    updateFromVtj: 'Uppdatera från BRC',
    partner: 'Makar',
    partnerInfo:
      'Annan person som bor på samma adress i ett förhållande som liknar äktenskap/samboförhållande',
    partnerAdd: 'Lägg till maka/make',
    financeNotesAndMessages: {
      title: 'Ekonomins anteckningar och meddelanden',
      addNote: 'Lägg till anteckning',
      sendMessage: 'Skicka eVaka-meddelande',
      noMessaging:
        'eVaka-meddelande kan endast skickas till person med personbeteckning.',
      link: 'Länk till originalmeddelandet',
      showMessages: 'Visa alla meddelanden',
      hideMessages: 'Dölj alla meddelanden',
      confirmDeleteNote: 'Vill du verkligen ta bort anteckningen',
      confirmArchiveThread:
        'Vill du verkligen flytta meddelandetråden till arkivet',
      note: 'Anteckning',
      created: 'Skapad',
      inEdit: 'Under redigering'
    },
    forceManualFeeDecisionsLabel: 'Sändning av avgiftsbeslut',
    forceManualFeeDecisionsChecked: 'Skickas alltid manuellt',
    forceManualFeeDecisionsUnchecked: 'Automatiskt, om möjligt',
    fridgeChildOfHead: 'Huvudmannens underställda barn under 18 år',
    fridgeChildAdd: 'Lägg till barn',
    fosterChildren: {
      sectionTitle: 'Fosterbarn',
      addFosterChildTitle: 'Lägg till nytt fosterbarn',
      addFosterChildParagraph:
        'Fosterförälder ser samma uppgifter om barnet i eVaka som vårdnadshavare. Fosterbarn får endast läggas till med socialarbetarens tillstånd.',
      updateFosterChildTitle: 'Uppdatera relationens giltighetstid',
      childLabel: 'Personbeteckning eller namn',
      validDuringLabel: 'Giltig',
      createError: 'Tillägg av fosterbarn misslyckades',
      deleteFosterChildTitle: 'Borttagning av fosterbarn',
      deleteFosterChildParagraph:
        'Vill du verkligen ta bort fosterbarnet? När fosterföräldraskapet upphör, markera ett slutdatum för relationen.'
    },
    fosterParents: 'Fosterföräldrar',
    applications: 'Ansökningar',
    feeDecisions: {
      title: 'Huvudmannens avgiftsbeslut',
      createRetroactive: 'Skapa retroaktiva utkast till avgiftsbeslut'
    },
    invoices: 'Huvudmannens fakturor',
    invoiceCorrections: {
      title: 'Krediteringar och höjningar',
      noteModalTitle: 'Ekonomins egen anteckning',
      noteModalInfo: 'Anteckningen syns inte på fakturan.',
      invoiceStatusHeader: 'Status',
      invoiceStatus: (status: InvoiceStatus | null) =>
        status === 'DRAFT'
          ? 'På fakturautkast'
          : status
            ? 'På faktura'
            : 'Inte på faktura'
    },
    voucherValueDecisions: {
      title: 'Huvudmannens värdebeslut',
      createRetroactive: 'Skapa retroaktiva utkast till värdebeslut'
    },
    dependants: 'Huvudmannens vårdnadshavare',
    guardiansAndParents: 'Vårdnadshavare och huvudmän',
    guardians: 'Vårdnadshavare',
    name: 'Namn',
    ssn: 'Personbeteckning',
    streetAddress: 'Gatuadress',
    age: 'Ålder',
    evakaRights: {
      tableHeader: 'eVaka-rättigheter',
      statusAllowed: 'Tillåten',
      statusDenied: 'Förbjuden',
      editModalTitle: 'Vårdnadshavarens eVaka-rättigheter',
      modalInfoParagraph: (
        <>
          Med eVaka-rättigheter bestäms om vårdnadshavaren ser uppgifter
          relaterade till sitt omhändertagna barn i eVaka. Rättigheterna kan
          endast förbjudas{' '}
          <strong>
            i motiverade barnskyddssituationer med socialarbetarens skriftliga
            meddelande
          </strong>
          . Rättigheterna ska återställas om omhändertagandet upphör.
        </>
      ),
      modalUpdateSubtitle:
        'Förbud av vårdnadshavarens eVaka-rättigheter när barnet är omhändertaget',
      confirmedLabel:
        'Jag bekräftar att det finns socialarbetarens skriftliga tillstånd för att begränsa vårdnadshavarens rätt till information',
      deniedLabel:
        'Jag förbjuder vårdnadshavaren för omhändertaget barn eVaka-rättigheter'
    },
    familyOverview: {
      title: 'Sammanfattning av familjens uppgifter',
      colName: 'Namn',
      colRole: 'Roll i familjen',
      colAge: 'Ålder',
      colIncome: 'Inkomster',
      colAddress: 'Adress',
      role: {
        HEAD: 'Huvudman',
        PARTNER: 'Maka/make',
        CHILD: 'Barn'
      },
      familySizeLabel: 'Familjestorlek',
      familySizeValue: (adults: number, children: number) => {
        const adultText = adults === 1 ? 'vuxen' : 'vuxna'
        const childrenText = children === 1 ? 'barn' : 'barn'
        return `${adults} ${adultText}, ${children} ${childrenText}`
      },
      incomeTotalLabel: 'Vuxnas inkomster totalt',
      incomeValue: (val: string) => `${val} €`,
      incomeMissingCompletely: 'Inkomstuppgifter saknas'
    },
    fridgeHead: {
      error: {
        edit: {
          title: 'Redigering av huvudman misslyckades!'
        }
      }
    },
    fridgePartner: {
      newPartner: 'Ny maka/make',
      editPartner: 'Redigering av maka/make',
      removePartner: 'Borttagning av maka/make',
      confirmText:
        'Vill du verkligen ta bort makan/maken? Vid byte av maka/make, markera ett slutdatum för den tidigare relationen och lägg sedan till en ny maka/make',
      error: {
        remove: {
          title: 'Borttagning av maka/make misslyckades!'
        },
        add: {
          title: 'Tillägg av maka/make misslyckades!'
        },
        edit: {
          title: 'Redigering av maka/make misslyckades!'
        },
        conflict:
          'Det finns en aktiv relation för parterna under angiven tidsperiod. Den nuvarande aktiva relationen måste avslutas innan en ny skapas'
      },
      validation: {
        deadPerson:
          'Relationens slutdatum kan inte vara efter personens dödsdag',
        deadPartner:
          'Relationens slutdatum kan inte vara efter makens/makans dödsdag'
      },
      searchTitle: 'Personbeteckning eller namn'
    },
    fridgeChild: {
      newChild: 'Nytt barn',
      editChild: 'Redigering av barn',
      removeChild: 'Borttagning av barn',
      confirmText:
        'Vill du verkligen ta bort barnet? Vid byte av huvudman, markera ett slutdatum för den tidigare relationen och lägg sedan till en ny',
      error: {
        add: {
          title: 'Tillägg av barn misslyckades!'
        },
        edit: {
          title: 'Redigering av barn misslyckades!'
        },
        remove: {
          title: 'Borttagning av barn misslyckades!'
        },
        conflict:
          'Det finns redan en huvudman för detta barn under denna tidsperiod. Den befintliga huvudmanrelationen måste avslutas först'
      },
      validation: {
        deadAdult:
          'Relationens slutdatum kan inte vara efter den vuxnas dödsdag',
        deadChild: 'Relationens slutdatum kan inte vara efter barnets dödsdag'
      },
      searchTitle: 'Personbeteckning eller namn'
    },
    application: {
      child: 'Barn',
      preferredUnit: 'Sökt enhet',
      startDate: 'Sökt startdatum',
      sentDate: 'Ansökningens ankomstdatum',
      type: 'Serviceform',
      types: {
        PRESCHOOL: 'Förskoleundervisning',
        PRESCHOOL_WITH_DAYCARE: 'Förskoleundervisning + tillhörande',
        PRESCHOOL_DAYCARE: 'Tillhörande småbarnspedagogik',
        PRESCHOOL_CLUB: 'Förskoleundervisningens klubb',
        PREPARATORY_EDUCATION: 'Förberedande undervisning',
        PREPARATORY_WITH_DAYCARE: 'Förberedande undervisning + tillhörande',
        DAYCARE: 'Småbarnspedagogik',
        DAYCARE_PART_TIME: 'Småbarnspedagogik',
        CLUB: 'Klubb'
      },
      status: 'Status',
      open: 'Öppna ansökan',
      statuses: {
        CREATED: 'Utkast',
        SENT: 'Anländ',
        WAITING_PLACEMENT: 'Väntar på placering',
        WAITING_DECISION: 'Beslutsförberedelse',
        WAITING_UNIT_CONFIRMATION: 'Väntar på chefsens godkännande',
        WAITING_MAILING: 'Väntar på utskick',
        WAITING_CONFIRMATION: 'Bekräftas av vårdnadshavare',
        REJECTED: 'Plats avvisad',
        ACTIVE: 'Plats mottagen',
        CANCELLED: 'Bortagen från behandlingen'
      }
    },
    decision: {
      decisions: 'Beslut',
      decisionUnit: 'Placeringsenhet',
      status: 'Status',
      archived: 'Arkiverad',
      statuses: {
        PENDING: 'Väntar på svar',
        ACCEPTED: 'Godkänd',
        REJECTED: 'Avvisad'
      },
      archive: 'Arkivera',
      startDate: 'Startdatum enligt beslut',
      sentDate: 'Beslut skickat'
    },
    income: {
      title: 'Inkomstuppgifter',
      itemHeader: 'Inkomstuppgifter för perioden',
      itemHeaderNew: 'Ny inkomstuppgift',
      lastModifiedAt: (date: string) => `Senast redigerad ${date}`,
      lastModifiedBy: (name: string) => `Redigerare: ${name}`,
      details: {
        attachments: 'Bilagor',
        name: 'Namn',
        created: 'Inkomstuppgifter skapade',
        handler: 'Handläggare',
        originApplication:
          'Vårdnadshavare har i ansökan godkänt högsta avgiftsklass',
        dateRange: 'För perioden',
        notes: 'Tilläggsuppgifter',
        effect: 'Avgiftsgrund',
        effectOptions: {
          MAX_FEE_ACCEPTED: 'Vårdnadshavare har godkänt högsta avgiftsklass',
          INCOMPLETE: 'Bristfälliga inkomstuppgifter',
          INCOME: 'Vårdnadshavarens levererade inkomstuppgifter',
          NOT_AVAILABLE: 'Bristfälliga inkomstuppgifter'
        },
        miscTitle: 'Tilläggsuppgifter',
        incomeTitle: 'Inkomster',
        income: 'Inkomster',
        expensesTitle: 'Utgifter',
        expenses: 'Utgifter',
        amount: '€',
        coefficient: 'Koefficient',
        monthlyAmount: '€ / MÅN',
        time: 'För perioden',
        sum: 'Totalt',
        entrepreneur: 'Företagare',
        echa: 'Europeiska kemikaliemyndigheten',
        source: 'Källa',
        createdFromApplication: 'Skapad automatiskt från ansökan',
        application: 'Ansökan',
        incomeCoefficients: {
          MONTHLY_WITH_HOLIDAY_BONUS: 'Månad',
          MONTHLY_NO_HOLIDAY_BONUS: 'Månad utan semesterpenning',
          BI_WEEKLY_WITH_HOLIDAY_BONUS: '2 veckor',
          BI_WEEKLY_NO_HOLIDAY_BONUS: '2 veckor utan semesterpenning',
          DAILY_ALLOWANCE_21_5: 'Dagpenning x 21,5',
          DAILY_ALLOWANCE_25: 'Dagpenning x 25',
          YEARLY: 'År'
        },
        updateError: 'Sparande av inkomstuppgifter misslyckades',
        missingIncomeDaysWarningTitle:
          'Inkomstuppgifter saknas för vissa dagar',
        missingIncomeDaysWarningText: (missingIncomePeriodsString: string) =>
          `Inkomstuppgifter saknas för följande dagar: ${missingIncomePeriodsString}. Om inkomstuppgifter inte läggs till, fastställs inkomsterna för dessa dagar enligt högsta avgiftsklass. Kontrollera datumen och lägg vid behov till inkomstuppgifter för de saknade dagarna.`,
        conflictErrorText:
          'Inkomstuppgifter har redan sparats för tidsperioden! Kontrollera inkomstuppgifternas giltighetstider.',
        closeWarning: 'Kom ihåg att spara!',
        closeWarningText:
          'Spara eller avbryt ändringar innan du stänger formuläret.'
      },
      add: 'Skapa ny inkomstuppgift',
      deleteModal: {
        title: 'Borttagning av inkomstuppgift',
        confirmText: 'Vill du verkligen ta bort inkomstuppgiften för perioden',
        cancelButton: 'Avbryt',
        deleteButton: 'Ta bort'
      }
    },
    incomeStatement: {
      title: 'Inkomstutredningar',
      notificationsTitle: 'Påminnelser om att göra inkomstutredning',
      custodianTitle: 'Vårdnadshavarnas inkomstutredningar',
      noIncomeStatements: 'Inga inkomstutredningar',
      incomeStatementHeading: 'Kundens inkomstutredningsformulär',
      sentAtHeading: 'Ankomstdag',
      handledHeading: 'Behandlad',
      open: 'Öppna formulär',
      handled: 'Inkomstutredning behandlad',
      notificationSent: 'Skickad',
      noNotifications: 'Inga skickade påminnelser',
      notificationTypes: {
        INITIAL_EMAIL: 'Första påminnelsen',
        REMINDER_EMAIL: 'Andra påminnelsen',
        EXPIRED_EMAIL: 'Inkomster upphört',
        NEW_CUSTOMER: 'Inledande kund'
      },
      noCustodians: 'Inga vårdnadshavare'
    },
    invoice: {
      createReplacementDrafts: 'Bilda korrigeringsfakturor',
      validity: 'Period',
      price: 'Summa',
      status: 'Status'
    },
    downloadAddressPage: 'Ladda ner adresssida'
  },
  timeline: {
    title: 'Familjens tidslinje',
    feeDecision: 'Avgiftsbeslut',
    valueDecision: 'Värdebeslut',
    partner: 'Maka/make',
    child: 'Barn',
    createdAtTitle: 'Skapad',
    unknownSource: 'Skapandekälla okänd',
    modifiedAtTitle: 'Redigerad',
    unknownModification: 'Redigerare okänd',
    notModified: 'Inte redigerad',
    user: 'Användare',
    application: 'Ansökan',
    dvvSync: 'Befolkningsdatasystemet',
    notAvailable: 'Tid okänd',
    DVV: 'Befolkningsdatasystemets synkronisering'
  },
  incomeStatement: {
    startDate: 'Giltig från och med',
    feeBasis: 'Grund för kundavgift',

    grossTitle: 'Bruttoinkomster',
    noIncomeTitle:
      'Inga inkomster eller stöd, uppgifterna får kontrolleras från inkomstregistret och FPA',
    noIncomeDescription: 'Beskriv din situation noggrannare',
    incomeSource: 'Leverans av uppgifter',
    incomesRegister:
      'Jag godkänner att uppgifter relaterade till mina inkomster granskas från FPA samt inkomstregistret.',
    attachmentsAndKela:
      'Jag levererar uppgifterna som bilagor och mina uppgifter får kontrolleras från FPA',
    grossEstimatedIncome: 'Uppskattning av bruttoinkomster',
    otherIncome: 'Andra inkomster',
    otherIncomeTypes: {
      PENSION: 'Pension',
      ADULT_EDUCATION_ALLOWANCE: 'Vuxenutbildningsstöd',
      SICKNESS_ALLOWANCE: 'Sjukdagpenning',
      PARENTAL_ALLOWANCE: 'Moderskaps- och föräldrapenning',
      HOME_CARE_ALLOWANCE: 'Stöd för hemvård av barn',
      FLEXIBLE_AND_PARTIAL_HOME_CARE_ALLOWANCE:
        'Flexibel eller partiell vårdpenning',
      ALIMONY: 'Underhållsbidrag eller -stöd',
      INTEREST_AND_INVESTMENT_INCOME: 'Ränte- och dividendinkomster',
      RENTAL_INCOME: 'Hyresinkomster',
      UNEMPLOYMENT_ALLOWANCE: 'Arbetslöshetsdagpenning',
      LABOUR_MARKET_SUBSIDY: 'Arbetsmarknadsstöd',
      ADJUSTED_DAILY_ALLOWANCE: 'Jämkad dagpenning',
      JOB_ALTERNATION_COMPENSATION: 'Alterneringsledighetsersättning',
      REWARD_OR_BONUS: 'Arvode eller bonus',
      RELATIVE_CARE_SUPPORT: 'Närståendevårdsstöd',
      BASIC_INCOME: 'Grundinkomst',
      FOREST_INCOME: 'Skogsinkomst',
      FAMILY_CARE_COMPENSATION: 'Arvoden för familjevård',
      REHABILITATION: 'Rehabiliteringsstöd eller rehabiliteringspenning',
      EDUCATION_ALLOWANCE: 'Utbildningsdagpenning',
      GRANT: 'Stipendium',
      APPRENTICESHIP_SALARY: 'Löneinkomst från läroavtalsutbildning',
      ACCIDENT_INSURANCE_COMPENSATION: 'Ersättning från olycksfallsförsäkring',
      OTHER_INCOME: 'Andra inkomster'
    },
    otherIncomeInfo: 'Uppskattningar av andra inkomster',

    entrepreneurTitle: 'Företagarens inkomstuppgifter',
    startOfEntrepreneurship: 'Företagsverksamhet påbörjad',
    companyName: 'Företagets / företagens namn',
    businessId: 'FO-nummer / FO-nummer',
    spouseWorksInCompany: 'Arbetar makan/maken i företaget',
    startupGrant: 'Startpeng',
    companyInfoTitle: 'Företagets uppgifter',
    checkupConsentLabel: 'Kontroll av uppgifter',
    checkupConsent:
      'Jag godkänner att uppgifter relaterade till mina inkomster vid behov granskas från inkomstregistret samt FPA.',
    companyType: 'Verksamhetsform',
    selfEmployed: 'Firma',
    selfEmployedAttachments:
      'Jag levererar som bilagor företagets senaste resultat- och balansräkning eller skattebeslut.',
    selfEmployedEstimation: 'Uppskattning av genomsnittliga månadsinkomster',
    limitedCompany: 'Aktiebolag',
    limitedCompanyIncomesRegister:
      'Mina inkomster kan kontrolleras direkt från inkomstregistret samt vid behov från FPA.',
    limitedCompanyAttachments:
      'Jag levererar verifikat för mina inkomster som bilaga och godkänner att uppgifter relaterade till mina inkomster granskas från FPA.',
    partnership: 'Öppet bolag eller kommanditbolag',
    lightEntrepreneur: 'Lättföretagande',
    attachments: 'Bilagor',

    estimatedMonthlyIncome: 'Genomsnittliga inkomster €/mån',
    timeRange: 'Under tidsperioden',

    accountantTitle: 'Bokförarens uppgifter',
    accountant: 'Bokförare',
    email: 'E-postadress',
    phone: 'Telefonnummer',
    address: 'Postadress',

    otherInfoTitle: 'Andra uppgifter relaterade till inkomster',
    student: 'Studerande',
    alimonyPayer: 'Betalar underhållsbidrag',
    otherInfo: 'Tilläggsuppgifter relaterade till inkomstuppgifter',

    citizenAttachments: {
      title: 'Bilagor relaterade till inkomster och småbarnspedagogikavgifter',
      noAttachments: 'Inga bilagor',
      attachmentMissing: 'Bilaga saknas'
    },

    employeeAttachments: {
      title: 'Lägg till bilagor',
      description:
        'Här kan du lägga till bilagor som kunden levererat i pappersform till inkomstutredningen som returnerats via eVaka.'
    },

    statementTypes: {
      HIGHEST_FEE: 'Samtycke till högsta avgiftsklass',
      INCOME: 'Vårdnadshavarens levererade inkomstuppgifter',
      CHILD_INCOME: 'Barnets inkomstuppgifter'
    },
    table: {
      title: 'Inkomstutredningar som väntar på behandling',
      customer: 'Kund',
      area: 'Område',
      sentAt: 'Skickad',
      startDate: 'Giltig',
      incomeEndDate: 'Inkomstuppgift upphör',
      type: 'Typ',
      link: 'Utredning',
      note: 'Anteckning'
    },
    noNote: 'Inkomstutredningen har ingen anteckning',
    handlerNotesForm: {
      title: 'Handläggarens anteckningar',
      handled: 'Behandlad',
      handlerNote: 'Anteckning (intern)'
    },
    attachmentNames: {
      OTHER: 'Annan bilaga',
      PENSION: 'Beslut om pension',
      ADULT_EDUCATION_ALLOWANCE: 'Beslut om vuxenutbildningsstöd',
      SICKNESS_ALLOWANCE: 'Beslut om sjukdagpenning',
      PARENTAL_ALLOWANCE: 'Beslut om moderskaps- eller föräldrapenning',
      HOME_CARE_ALLOWANCE: 'Beslut om stöd för hemvård',
      FLEXIBLE_AND_PARTIAL_HOME_CARE_ALLOWANCE: 'Beslut om vårdpenning',
      ALIMONY: 'Underhållsavtal eller beslut om underhållsstöd',
      UNEMPLOYMENT_ALLOWANCE: 'Beslut om arbetslöshetsdagpenning',
      LABOUR_MARKET_SUBSIDY: 'Beslut om arbetsmarknadsstöd',
      ADJUSTED_DAILY_ALLOWANCE: 'Beslut om dagpenning',
      JOB_ALTERNATION_COMPENSATION: 'Verifikat alterneringsledighetsersättning',
      REWARD_OR_BONUS: 'Löneverifikat för bonus och/eller arvode',
      RELATIVE_CARE_SUPPORT: 'Beslut om närståendevårdsstöd',
      BASIC_INCOME: 'Beslut om grundinkomst',
      FOREST_INCOME: 'Verifikat för skogsinkomst',
      FAMILY_CARE_COMPENSATION: 'Verifikat för arvoden för familjevård',
      REHABILITATION:
        'Beslut om rehabiliteringsstöd eller rehabiliteringspenning',
      EDUCATION_ALLOWANCE: 'Beslut om utbildningsdagpenning',
      GRANT: 'Verifikat för stipendium',
      APPRENTICESHIP_SALARY:
        'Verifikat för löneinkomst från läroavtalsutbildning',
      ACCIDENT_INSURANCE_COMPENSATION:
        'Verifikat för ersättning från olycksfallsförsäkring',
      OTHER_INCOME: 'Bilagor för andra inkomster',
      ALIMONY_PAYOUT: 'Betalningsverifikat för underhållsbidrag',
      INTEREST_AND_INVESTMENT_INCOME:
        'Verifikat för ränte- och dividendinkomster',
      RENTAL_INCOME: 'Verifikat för hyresinkomster och vederlag',
      PAYSLIP_GROSS: 'Senaste lönespecifikationen',
      STARTUP_GRANT: 'Beslut om startpeng',
      ACCOUNTANT_REPORT_PARTNERSHIP:
        'Bokförarens utredning om lön och naturaförmåner',
      PAYSLIP_LLC: 'Senaste lönespecifikationen',
      ACCOUNTANT_REPORT_LLC:
        'Bokförarens utredning om naturaförmåner och dividender',
      PROFIT_AND_LOSS_STATEMENT_SELF_EMPLOYED:
        'Resultat- och balansräkning eller skattebeslut',
      PROFIT_AND_LOSS_STATEMENT_PARTNERSHIP: 'Resultat- och balansräkning',
      SALARY: 'Betalningsverifikat för löner och arbetsersättningar',
      PROOF_OF_STUDIES:
        'Studieintyg eller beslut om studieförmån från arbetslöshetskassan / utbildningsstöd från sysselsättningsfonden',
      CHILD_INCOME: 'Verifikat för barnets inkomster'
    }
  },
  units: {
    name: 'Namn',
    area: 'Område',
    address: 'Adress',
    city: 'Kommun',
    type: 'Typ',
    findByName: 'Sök med enhetens namn',
    selectProviderTypes: 'Välj anordningsform',
    selectCareTypes: 'Välj verksamhetsform',
    includeClosed: 'Visa avslutade enheter',
    noResults: 'Inga resultat'
  },
  unit: {
    serviceWorkerNote: {
      title: 'Anteckningar från servicerådgivning',
      add: 'Lägg till anteckning'
    },
    tabs: {
      unitInfo: 'Enhetsinformation',
      groups: 'Grupper',
      calendar: 'Kalender',
      applicationProcess: 'Ansökningsprocess'
    },
    create: 'Skapa ny enhet',
    openDetails: 'Visa alla enhetens uppgifter',
    occupancies: 'Utnyttjande- och beläggningsgrad',
    info: {
      title: 'Enhetsinformation',
      area: 'Område',
      visitingAddress: 'Besöksadress',
      mailingAddress: 'Postadress',
      phone: 'Telefonnummer',
      caretakers: {
        titleLabel: 'Personal',
        unitOfValue: 'personer'
      }
    },
    manager: {
      title: 'Enhetschef',
      name: 'Namn',
      email: 'E-postadress',
      phone: 'Telefonnummer'
    },
    accessControl: {
      aclRoles: 'Behörigheter',
      activeAclRoles: 'Aktiva behörigheter',
      roleChange: 'Rollbyte',
      scheduledAclRoles: 'Kommande behörigheter',
      role: 'Roll',
      name: 'Namn',
      email: 'E-postadress',
      aclStartDate: 'Behörighet börjar',
      aclEndDate: 'Behörighet upphör',
      removeConfirmation:
        'Vill du ta bort åtkomsträttigheten från den valda personen?',
      removeScheduledConfirmation: 'Vill du ta bort den kommande behörigheten?',
      addDaycareAclModal: {
        title: 'Lägg till behörighet',
        role: 'Välj roll',
        employees: 'Välj person',
        scheduledAclWarning:
          'Personen har en kommande behörighet i denna enhet. Den kommande behörigheten kommer att tas bort.'
      },
      editDaycareAclModal: {
        title: 'Redigera behörighet'
      },
      chooseRole: 'Välj roll',
      choosePerson: 'Välj person',
      chooseGroup: 'Välj grupp',
      temporaryEmployees: {
        title: 'Tillfälliga vikarier',
        previousEmployeesTitle: 'Tidigare tillfälliga vikarier',
        firstName: 'Förnamn',
        firstNamePlaceholder: 'Skriv förnamn',
        lastName: 'Efternamn',
        lastNamePlaceholder: 'Skriv efternamn',
        pinCode: 'PIN-kod',
        pinCodePlaceholder: 'kod'
      },
      addTemporaryEmployeeModal: {
        title: 'Lägg till tillfällig vikarie'
      },
      editTemporaryEmployeeModal: {
        title: 'Redigera tillfällig vikarie'
      },
      reactivateTemporaryEmployee: 'Bevilja behörighet på nytt',
      removeTemporaryEmployeeConfirmation:
        'Vill du ta bort den valda personen från listan?',
      mobileDevices: {
        mobileDevices: 'Enhetens mobilenheter',
        addMobileDevice: 'Lägg till mobilenhet',
        editName: 'Redigera enhetens namn',
        removeConfirmation: 'Vill du ta bort mobilenheten?',
        editPlaceholder: 't.ex. Hippogruppensmobil'
      },
      groups: 'Gruppbehörigheter',
      noGroups: 'Inga behörigheter',
      hasOccupancyCoefficient: 'Ansvarig för fostran'
    },
    filters: {
      title: 'Visa uppgifter',
      periods: {
        day: 'Dag',
        threeMonths: '3 mån',
        sixMonths: '6 mån',
        year: 'År'
      }
    },
    occupancy: {
      display: 'Visa',
      fullUnit: 'Hela enheten',
      title: 'Enhetens beläggningsgrad',
      subtitles: {
        confirmed: 'Bekräftad beläggningsgrad',
        planned: 'Planerad beläggningsgrad',
        draft: 'Uppskattad beläggningsgrad',
        realized: 'Utnyttjandegrad'
      },
      fail: 'Laddning av beläggningsgrad misslyckades',
      failRealized: 'Laddning av utnyttjandegrad misslyckades',
      maximum: 'Maximum',
      minimum: 'Minimum',
      noValidValues: 'Beläggningsgrad kunde inte beräknas för tidsperioden',
      noValidValuesRealized:
        'Utnyttjandegrad kunde inte beräknas för tidsperioden',
      realtime: {
        modes: {
          REALIZED: 'Utfall',
          PLANNED: 'Plan'
        },
        noData: 'Inga uppgifter för vald dag',
        legendTitle: 'Förklaring av markeringar',
        chartYAxisTitle: 'Barn med koefficienter',
        chartY1AxisTitle: 'Personal',
        staffPresent: 'Antal arbetare',
        staffRequired: 'Erforderliga arbetare',
        childrenMax: 'Maximalt antal barn (med koefficient)',
        childrenPresent: 'Antal barn',
        children: 'Antal barn (med koefficient)',
        unknownChildren: '+ barn utan reservation',
        utilization: 'Utnyttjandegrad'
      }
    },
    staffOccupancies: {
      title: 'Ansvar för fostran',
      occupancyCoefficientEnabled: 'Räknas in i utnyttjandegraden'
    },
    applicationProcess: {
      title: 'Ansökningsprocess'
    },
    placementPlans: {
      title: 'Väntar på bekräftelse från vårdnadshavare',
      name: 'Namn',
      birthday: 'Födelsedatum',
      placementDuration: 'Placerad i enheten',
      type: 'Placeringstyp',
      subtype: 'Del/Hel',
      application: 'Ansökan'
    },
    placementProposals: {
      acceptAllTitle: 'Valda placeringsförslag',
      acceptAllSummary: ({
        accepted,
        rejected
      }: {
        accepted: number
        rejected: number
      }) => `${accepted} godkänns, ${rejected} avvisas`,
      acceptAllButton: 'Bekräfta val',
      application: 'Ansökan',
      birthday: 'Födelsedatum',
      citizenHasRejectedPlacement: 'Plats avvisad',
      confirmation: 'Godkännande',
      describeOtherReason: 'Skriv motivering',
      infoText:
        'Markera de barn som du kan ta emot. När du har godkänt alla barn kan du trycka på knappen Bekräfta godkända. Om du inte kan godkänna alla barn, markera krysset och lägg till en motivering. Servicerådgivningen gör då ett nytt placeringsförslag eller tar kontakt.',
      infoTitle: 'Markering som godkänd / avvisad',
      name: 'Namn',
      placementDuration: 'Placerad i enheten',
      rejectTitle: 'Välj orsak till avvisning',
      rejectReasons: {
        REASON_1:
          'UTRYMMESBEGRÄNSNING, överenskommen med områdeschefen för småbarnspedagogik.',
        REASON_2:
          'ENHETENS TOTALA SITUATION, överenskommen med områdeschefen för småbarnspedagogik.',
        REASON_3: '',
        OTHER: 'Annan orsak'
      },
      statusLastModified: (name: string, date: string) =>
        `Senast ändrad ${date}. Redaktör: ${name}`,
      subtype: 'Del/Hel',
      title: 'Placeringsförslag',
      type: 'Placeringstyp',
      unknown: 'Okänd'
    },
    applications: {
      title: 'Ansökningar',
      child: 'Barnets namn/födelsedatum',
      guardian: 'Vårdnadshavare som ansökt',
      type: 'Placeringstyp',
      types: {
        CLUB: 'Klubb',
        DAYCARE: 'Småbarnspedagogik',
        DAYCARE_PART_TIME: 'Småbarnspedagogik',
        PRESCHOOL: 'Förskola',
        PRESCHOOL_DAYCARE: 'Förskola',
        PREPARATORY: 'Förberedande',
        PREPARATORY_DAYCARE: 'Förberedande'
      },
      placement: 'Del/Hel',
      preferenceOrder: 'Önskemål',
      startDate: 'Start',
      status: 'Status',
      extendedCare: 'Skiftvård'
    },
    transferApplications: {
      title: 'Ansökt om överföring till annan plats',
      child: 'Barnets namn/födelsedatum',
      startDate: 'Önskemål om startdatum, ännu ingen placering'
    },
    serviceApplications: {
      title: 'Ändringsansökningar för servicebehov som väntar på behandling',
      child: 'Barn',
      range: 'För perioden',
      newNeed: 'Nytt behov',
      currentNeed: 'Aktuellt behov',
      sentDate: 'Skickat'
    },
    placements: {
      title: 'Barn som väntar på grupp',
      name: 'Namn',
      birthday: 'Födelsedatum',
      under3: 'Under 3 år vid placeringens början',
      over3: 'Över 3 år vid placeringens början',
      placementDuration: 'Placerad i enheten',
      missingGroup: 'Grupp saknas',
      type: 'Placeringstyp',
      subtype: 'Del/Hel',
      addToGroup: 'Gruppera',
      modal: {
        createTitle: 'Barnets placering i grupp',
        transferTitle: 'Överföring av barn till annan grupp',
        child: 'Barn som ska placeras',
        group: 'Grupp',
        errors: {
          noGroup: 'Du har inte valt grupp eller det finns inga aktiva grupper',
          noStartDate: 'Du har inte valt startdatum',
          noEndDate: 'Du har inte valt slutdatum',
          groupNotStarted: 'Gruppen har inte börjat ännu',
          groupEnded: 'Gruppen är redan nedlagd'
        }
      }
    },
    termination: {
      title: 'Placeringar som upphör',
      info: 'På listan visas de barn vars vårdnadshavare har gjort en uppsägningsanmälan under de senaste två veckorna, eller som har en av vårdnadshavaren godkänd överföringsansökan till en annan enhet. Barn som har en placering som upphör av annan orsak visas inte på listan.',
      terminationRequestedDate: 'Uppsägningsdatum',
      endDate: 'Slutdatum',
      groupName: 'Grupp'
    },
    calendar: {
      title: 'Kalender',
      noGroup: 'Ingen grupp',
      shiftCare: 'Skiftvård',
      staff: 'Personal',
      allChildren: 'Alla barn',
      modes: {
        week: 'Vecka',
        month: 'Månad'
      },
      attendances: {
        title: 'Reservationer och närvaro'
      },
      nextWeek: 'Nästa vecka',
      previousWeek: 'Föregående vecka',
      events: {
        title: 'Händelser',
        createEvent: 'Skapa annan händelse',
        lastModified: (date: string, name: string) =>
          `Senast ändrad ${date}; redaktör: ${name}`,
        lastModifiedAt: 'Senast ändrad',
        lastModifiedBy: 'Redaktör',
        edit: {
          title: 'Händelse',
          saveChanges: 'Spara ändringar',
          delete: 'Ta bort händelse'
        },
        create: {
          title: 'Lägg till ny händelse',
          text: 'Lägg här till händelser som är viktiga för vårdnadshavaren att komma ihåg: händelsen visas i vårdnadshavarens eVaka-kalender. Om andra händelser är det bra att informera vårdnadshavaren via meddelande.',
          add: 'Lägg till händelse',
          period: 'Tidpunkt',
          attendees: 'Deltagare i händelsen',
          attendeesPlaceholder: 'Välj...',
          eventTitle: 'Händelserubrik',
          eventTitlePlaceholder: 'Max. 30 tecken',
          description: 'Händelsebeskrivning',
          descriptionPlaceholder:
            'Korta instruktioner till vårdnadshavaren, t.ex. klockslag, vad som ska packas',
          missingPlacementsWarning:
            'En del av de valda barnen har ingen placering i den aktuella enheten eller är inte placerade i den valda gruppen under händelsen. Under dessa dagar listas inte barnet som deltagare och händelsen visas inte för vårdnadshavaren i kalendern.',
          unorderedMeals: 'Måltider som inte ska beställas',
          meals: {
            BREAKFAST: 'Frukost',
            LUNCH: 'Lunch',
            SNACK: 'Mellanmål',
            DINNER: 'Middag',
            SUPPER: 'Kvällsmål'
          }
        },
        discussionReservation: {
          calendar: {
            eventTooltipTitle: 'Andra händelser:',
            otherEventSingular: 'annan händelse',
            otherEventPlural: 'andra händelser'
          },
          discussionPageTitle: 'Hantering av samtalstider',
          discussionPageDescription:
            'På denna sida kan du skapa och följa upp förfrågningar där du frågar vårdnadshavare om passande samtalstider.',
          surveyCreate: 'Ny samtalförfrågan',
          surveyBasicsTitle: 'Grunduppgifter',
          surveyPeriod: 'Förfrågningens varaktighet',
          surveySubject: 'Samtalsämne',
          surveyInvitees: 'Deltagare i samtalen',
          surveySummary: 'Ytterligare information till vårdnadshavaren',
          surveySummaryCalendarLabel: 'Ytterligare information',
          surveySummaryInfo:
            'Denna text visas för vårdnadshavaren i samband med förfrågan. Du kan berätta mer om samtalen, till exempel ankomstanvisningar eller hur lång tid samtalet tar.',
          surveySubjectPlaceholder: 'Högst 30 tecken',
          surveySummaryPlaceholder: 'Skriv ytterligare information',
          surveyDiscussionTimesTitle: 'Samtalstider',
          surveyInviteeTitle: 'Deltagare',
          editSurveyButton: 'Redigera',
          createSurveyButton: 'Skicka samtalstider',
          saveSurveyButton: 'Spara ändringar',
          deleteSurveyButton: 'Ta bort',
          cancelButton: 'Avbryt',
          cancelConfirmation: {
            title: 'Vill du avbryta ändringarna?',
            text: 'Dina ändringar kommer inte att sparas',
            cancelButton: 'Fortsätt redigera',
            continueButton: 'Avbryt ändringar'
          },
          surveyModifiedAt: 'Ändrad',
          surveyStatus: {
            SENT: 'Skickad',
            ENDED: 'Avslutad'
          },
          reservedTitle: 'Har reserverat',
          reserveButton: 'Reservera',
          unreservedTitle: 'Ej reserverat',
          calendarSurveySummary: (
            link: (text: string) => React.ReactNode
          ): React.ReactNode => (
            <>
              För mer detaljerad information{' '}
              {link('gå till granskningsvyn för samtalförfrågan')}
            </>
          ),
          reservationModal: {
            reservationStatus: 'Reservationsstatus',
            removeReservation: 'Ta bort reservation',
            removeDiscussionTime: 'Ta bort samtalstid',
            reserved: 'Reserverad',
            unreserved: 'Ledig',
            selectPlaceholder: 'Välj',
            inviteeLabel: 'Deltagare',
            reserveError: 'Reservation av samtalstid misslyckades',
            deleteError: 'Borttagning av samtalstid misslyckades',
            deleteConfirmation: {
              title: 'Tiden som ska tas bort är redan reserverad',
              text: 'Vill du ta bort tiden och reservationen?',
              cancelButton: 'Avbryt borttagning',
              continueButton: 'Ta bort'
            }
          },
          deleteConfirmation: {
            title: 'Vill du verkligen ta bort den skickade förfrågan?',
            text: 'Alla lediga och reserverade tider kommer att tas bort. Denna åtgärd kan inte ångras.',
            error: 'Borttagning av samtalförfrågan misslyckades'
          },
          eventTime: {
            addError: 'Tillägg av samtalstid misslyckades',
            deleteError: 'Borttagning av samtalstid misslyckades'
          },
          reservationClearConfirmationTitle: 'Ta bort följande reservationer?',
          clearReservationButtonLabel: 'Ta bort reservationer'
        },
        reservedTimesLabel: 'reserverade',
        freeTimesLabel: 'lediga'
      }
    },
    groups: {
      title: 'Enhetens grupper',
      familyContacts: 'Visa kontaktuppgiftssammanställning',
      attendanceReservations: 'Närvaroreservationer',
      create: 'Skapa ny grupp',
      createModal: {
        title: 'Ny grupp',
        confirmButton: 'Spara',
        cancelButton: 'Avbryt',
        name: 'Gruppens namn',
        type: 'Typ',
        initialCaretakers: 'Antal personal vid gruppens start',
        aromiCustomerId: 'Aromi ansvarsenhetskod',
        errors: {
          nameRequired: 'Gruppen måste ha ett namn',
          aromiWarning:
            'Om Aromi ansvarsenhetskod saknas ingår inte gruppmedlemmarna i matbeställningen',
          initialCaretakersPositive: 'Antalet personal kan inte vara negativt'
        }
      },
      updateModal: {
        title: 'Redigera gruppinformation',
        name: 'Namn',
        startDate: 'Grundad',
        endDate: 'Sista verksamhetsdag',
        info: 'Tidigare information om gruppen bevaras inte',
        jamixPlaceholder: 'Jamix customerNumber',
        jamixTitle: 'Matbeställningarnas kundnummer',
        aromiPlaceholder: 'Aromi ansvarsenhetskod',
        aromiTitle: 'Aromi-matbeställningarnas ansvarsenhetskod',
        nekkuUnitTitle: 'Nekku-matbeställningarnas enhet',
        nekkuCustomerNumberTitle: 'Nekku-matbeställningarnas kundnummer'
      },
      nekkuOrderModal: {
        title: 'Nekku-matbeställning'
      },
      startDate: 'Grundad',
      endDate: 'Sista verksamhetsdag',
      caretakers: 'Personal',
      childrenLabel: 'Barn',
      childrenValue: {
        single: 'barn',
        plural: 'barn'
      },
      childServiceNeedFactor: 'Barnets koefficient',
      childAssistanceNeedFactor: 'Stödbehov',
      factor: 'Koefficient',
      maxOccupancy: 'Högsta beläggningsgrad',
      maxRealizedOccupancy: 'Högsta utnyttjandegrad',
      name: 'Namn',
      birthday: 'Födelsedatum',
      placementDuration: 'Placerad i gruppen',
      serviceNeed: 'Servicebehov',
      serviceNeedChecked: 'Servicebehov markerat',
      serviceNeedMissing1: 'Servicebehov saknas (',
      serviceNeedMissing2: 'dagar)',
      placementType: 'Placeringstyp',
      placementSubtype: 'Del/Hel',
      noChildren: 'Inga barn har placerats i gruppen.',
      returnBtn: 'Återställ',
      transferBtn: 'Överför',
      diaryButton: 'Öppna dagbok',
      deleteGroup: 'Ta bort grupp',
      update: 'Redigera uppgifter',
      nekkuOrder: 'Nekku-beställning',
      daycareDailyNote: {
        dailyNote: 'Dagens anteckningar',
        header: 'Dagens upplevelser och lärdomar',
        groupNotesHeader: 'Gruppanteckningar',
        stickyNotesHeader: 'Att observera de närmaste dagarna',
        notesHint:
          'Lekar, framgångar, glädjeämnen och lärdomar idag (ej hälsoinformation eller sekretessbelagd information).',
        childStickyNoteHint:
          'Anteckning för personalen (ej hälsoinformation eller sekretessbelagd information).',
        otherThings: 'Annat',
        feedingHeader: 'Barnet åt idag',
        sleepingHeader: 'Barnet sov idag',
        sleepingHoursHint: 'timmar',
        sleepingMinutesHint: 'minuter',
        sleepingHours: 't',
        sleepingMinutes: 'min',
        reminderHeader: 'Saker att komma ihåg',
        otherThingsToRememberHeader: 'Annat att komma ihåg (t.ex. solkräm)',
        groupNoteModalLink: 'Gruppanteckning',
        groupNoteHint: 'Anteckning som gäller hela gruppen',
        edit: 'Lägg till dagens anteckning',
        level: {
          GOOD: 'Bra',
          MEDIUM: 'Måttligt',
          NONE: 'Inte alls'
        },
        reminderType: {
          DIAPERS: 'Lägg till blöjor',
          CLOTHES: 'Lägg till kläder',
          LAUNDRY: 'Tvätt'
        }
      },
      childDocuments: {
        createModalLink: 'Skicka dokument',
        createModal: {
          title: 'Skicka dokument till flera mottagare',
          template: 'Dokument',
          placements: 'Mottagare'
        }
      }
    },
    backupCares: {
      title: 'Reservplaceringsbarn',
      childName: 'Namn',
      duration: 'Placerad i enheten',
      birthDate: 'Födelsedatum'
    },
    attendanceReservations: {
      ungrouped: 'Barn utan grupp',
      childName: 'Barnets namn',
      startTime: 'Anländer',
      endTime: 'Går',
      requiresBackupCare: 'Gör reservplacering',
      openReservationModal: 'Gör återkommande reservation',
      childCount: 'Barn närvarande',
      lastModifiedStaff: (date: string, name: string) => (
        <div>
          <p>*Markering gjord av personal</p>
          <p>
            Senast ändrad {date}; redaktör: {name}
          </p>
        </div>
      ),
      lastModifiedOther: (date: string, name: string) =>
        `Senast ändrad ${date}; redaktör: ${name}`,
      reservationModal: {
        title: 'Gör reservation',
        selectedChildren: 'Barn för vilka reservation görs',
        dateRange: 'Reservationens giltighetstid',
        dateRangeLabel: 'Gör reservation för dagarna',
        missingDateRange: 'Välj dagar att reservera',
        repetition: 'Typ eller återkommande',
        times: 'Klockslag',
        businessDays: 'Må-Fre',
        repeats: 'Återkommer',
        repetitions: {
          DAILY: 'Dagligen',
          WEEKLY: 'Veckovis',
          IRREGULAR: 'Oregelbunden'
        }
      },
      childDateModal: {
        reservations: {
          title: 'Närvaroreservation',
          add: 'Lägg till reservation',
          noTimes: 'Närvarande, klockslag ännu inte känt'
        },
        attendances: {
          title: 'Närvaroutfall',
          add: 'Lägg till ny rad'
        },
        absences: {
          title: 'Frånvaro',
          add: {
            BILLABLE: 'Markera frånvaro från småbarnspedagogik',
            NONBILLABLE: 'Markera frånvaro från kostnadsfri verksamhet'
          },
          label: {
            BILLABLE: 'Frånvarande från småbarnspedagogik, orsak:',
            NONBILLABLE: 'Frånvarande från kostnadsfri verksamhet, orsak:'
          }
        },
        overlapWarning: 'Kontrollera överlappning',
        absenceWarning: 'Kontrollera frånvaro',
        extraNonbillableAbsence:
          'Enligt närvarotiderna var barnet närvarande i kostnadsfri verksamhet.',
        missingNonbillableAbsence:
          'Enligt närvarotiderna var barnet inte närvarande i kostnadsfri verksamhet.',
        extraBillableAbsence:
          'Enligt närvarotiderna var barnet närvarande i avgiftsbelagd småbarnspedagogik.',
        missingBillableAbsence:
          'Enligt närvarotiderna var barnet inte närvarande i avgiftsbelagd småbarnspedagogik.',
        errorCodes: {
          attendanceInFuture: 'Närvaro kan inte vara i framtiden'
        }
      },
      reservationNoTimes: 'Närvarande',
      missingHolidayReservation: 'Semesterreservation saknas',
      missingHolidayReservationShort: 'Semesterres. saknas',
      fixedSchedule: 'Närvarande',
      termBreak: 'Ingen verksamhet',
      missingReservation: 'Anmälan saknas',
      serviceTimeIndicator: '(s)',
      legend: {
        reservation: 'Reservation',
        serviceTime: 'Avtalstid',
        attendanceTime: 'Ankomst-/avgångstid',
        hhmm: 'tt:mm'
      },
      affectsOccupancy: 'Räknas in i utnyttjandegraden',
      doesNotAffectOccupancy: 'Räknas inte in i utnyttjandegraden',
      inOtherUnit: 'I annan enhet',
      inOtherGroup: 'I annan grupp',
      createdByEmployee: '*Markering gjord av personal'
    },
    staffAttendance: {
      startTime: 'ankomst',
      endTime: 'avgång',
      summary: 'Sammanfattning',
      plan: 'Plan',
      realized: 'Utfall',
      hours: 'Timmar',
      dailyAttendances: 'Dagens registreringar',
      continuationAttendance: '* registrering som började föregående dag',
      addNewAttendance: 'Lägg till ny registrering',
      saveChanges: 'Spara ändringar',
      noGroup: 'Ingen grupp',
      staffName: 'Arbetarens namn',
      addPerson: 'Lägg till person',
      types: {
        PRESENT: 'Närvarande',
        OTHER_WORK: 'Arbetsärende',
        TRAINING: 'Utbildning',
        OVERTIME: 'Övertid',
        JUSTIFIED_CHANGE: 'Motiverad ändring',
        SICKNESS: 'Annan orsak (egen)',
        CHILD_SICKNESS: 'Annan orsak (barn)'
      },
      incalculableSum:
        'Timmarna kan inte beräknas eftersom den sista avgångstiden saknas från dagens registreringar.',
      gapWarning: (gapRange: string) => `Registrering saknas för ${gapRange}`,
      openAttendanceWarning: (arrival: string) =>
        `Öppen registrering ${arrival}`,
      openAttendanceInAnotherUnitWarning: 'Öppen registrering ',
      openAttendanceInAnotherUnitWarningCont:
        '. Registreringen måste avslutas innan en ny läggs till.',
      personCount: 'Totalt antal närvarande',
      personCountAbbr: 'pers',
      unlinkOvernight: 'Separera närvaro över natten',
      previousDay: 'Föregående dag',
      nextDay: 'Nästa dag',
      addPersonModal: {
        description:
          'Lägg till en tillfälligt närvarande person och välj om hen ska räknas med i utnyttjandegraden.',
        arrival: 'Ankomsttid',
        name: 'Namn',
        namePlaceholder: 'Efternamn Förnamn',
        group: 'Grupp'
      },
      addedAt: 'Registrering skapad',
      modifiedAt: 'Ändrad',
      departedAutomatically: 'Automatiskt avslutad',
      hasStaffOccupancyEffect: 'Ansvarig för fostran'
    },
    error: {
      placement: {
        create: 'Placering i grupp misslyckades',
        transfer: 'Placering i annan grupp misslyckades'
      }
    }
  },
  groupCaretakers: {
    info: 'Skapa alltid ett nytt personalbehov när antalet personal ändras. Det angivna antalet gäller för den valda tidsperioden och påverkar enhetens och gruppens beläggningsgrader.',
    create: 'Skapa nytt personalbehov',
    edit: 'Redigera uppgifter',
    editActiveWarning:
      'Du redigerar uppgifter för en pågående tidsperiod. Om ändringen i personalantalet gäller en annan tidsperiod, skapa ett nytt personalbehov så att historikuppgifterna bevaras.',
    editHistoryWarning:
      'Du redigerar uppgifter för en avslutad tidsperiod. Om ändringen i personalantalet gäller en annan tidsperiod, skapa ett nytt personalbehov så att historikuppgifterna bevaras.',
    confirmDelete: 'Vill du verkligen ta bort personalbehovet?',
    startDate: 'Från och med',
    endDate: 'Till och med',
    amount: 'Personalbehov',
    amountUnit: 'Personer',
    status: 'Status',
    conflict:
      'Det finns en överlappning med en tidigare skapad tidsperiod i den valda tidsperioden. Ta bort överlappningen genom att redigera den andra tidsperioden.'
  },
  personalMobileDevices: {
    title: 'Personlig eVaka-mobil',
    infoParagraph1:
      'På denna sida kan du definiera en mobilenhet för ditt eget personliga bruk, med vilken du granskar alla dina enheters uppgifter i eVaka. Du kan också vid behov ta bort eller lägga till fler enheter.',
    infoParagraph2:
      'Se till att alla dina mobilenheter har en åtkomstkod aktiverad.',
    name: 'Enhetens namn',
    addDevice: 'Lägg till mobilenhet',
    editName: 'Redigera enhetens namn',
    deleteDevice: 'Vill du ta bort mobilenheten?'
  },
  mobilePairingModal: {
    sharedDeviceModalTitle: 'Lägg till ny mobilenhet till enheten',
    personalDeviceModalTitle: 'Lägg till ny personlig mobilenhet',
    modalText1: 'Gå med mobilenheten till adressen',
    modalText2: 'och skriv in koden nedan i enheten.',
    modalText3:
      'Skriv in bekräftelsekoden som visas på mobilenheten i fältet nedan.',
    modalText4:
      'Ge mobilenheten ett namn som du kan skilja den från andra mobilenheter med.',
    namePlaceholder: 'Namn'
  },
  invoices: {
    table: {
      title: 'Fakturor',
      toggleAll: 'Välj alla områdets fakturor',
      head: 'Huvudman',
      children: 'Barn',
      period: 'Faktureringsperiod',
      createdAt: 'Utkast skapat',
      nb: 'Obs',
      totalPrice: 'Summa',
      status: 'Status',
      replacementInvoice: 'Korrigeringsfaktura'
    },
    buttons: {
      checked: (count: number) =>
        count === 1 ? `${count} faktura vald` : `${count} fakturor valda`,
      sendInvoice: (count: number) =>
        count === 1 ? 'Överför vald faktura' : 'Överför valda fakturor',
      resendInvoice: (count: number) =>
        count === 1
          ? 'Skicka vald faktura på nytt'
          : 'Skicka valda fakturor på nytt',
      createInvoices: 'Skapa fakturautkast',
      deleteInvoice: (count: number) =>
        count === 1 ? 'Ta bort vald faktura' : 'Ta bort valda fakturor',
      checkAreaInvoices: (customRange: boolean) =>
        customRange
          ? 'Välj fakturor från vald tidsperiod och områden'
          : 'Välj denna månads fakturor från valda områden',
      individualSendAlertText:
        'Kom ihåg att hämta tidigare överförda fakturor till faktureringssystemet innan nya överförs.'
    },
    sendModal: {
      title: 'Överför valda fakturor',
      invoiceDate: 'Fakturans datum',
      dueDate: 'Fakturans förfallodag'
    },
    resendModal: {
      title: 'Vill du verkligen skicka fakturorna på nytt?',
      text: 'Kontrollera först noggrant att fakturorna inte har gått till faktureringssystemet.',
      confirm: 'Ja, jag förstår vad jag gör'
    },
    sendSuccess: 'Sändning lyckades',
    sendFailure: 'Sändning misslyckades'
  },
  invoice: {
    status: {
      DRAFT: 'Utkast',
      WAITING_FOR_SENDING: 'Överförs manuellt',
      SENT: 'Överförd',
      REPLACEMENT_DRAFT: 'Korrigeringsutkast',
      REPLACED: 'Korrigerad'
    },
    title: {
      DRAFT: 'Fakturautkast',
      WAITING_FOR_SENDING: 'Faktura som väntar på överföring',
      SENT: 'Överförd faktura',
      REPLACEMENT_DRAFT: 'Utkast till korrigeringsfaktura',
      REPLACED: 'Korrigerad faktura'
    },
    form: {
      nav: {
        return: 'Återvänd'
      },
      child: {
        ssn: 'Barnets personbeteckning'
      },
      headOfFamily: {
        title: 'Huvudman',
        fullName: 'Huvudman',
        ssn: 'Huvudmannens personbeteckning',
        codebtorName: 'Medskyldige',
        codebtorSsn: 'Medskyldiges personbeteckning'
      },
      details: {
        title: 'Fakturans uppgifter',
        status: 'Status',
        range: 'Faktureringsperiod',
        number: 'Fakturanummer',
        dueDate: 'Fakturans förfallodag',
        account: 'Konto',
        accountType: 'Kontotyp',
        agreementType: 'Faktureringstyp',
        relatedFeeDecisions: 'Relaterade avgiftsbeslut',
        replacedInvoice: 'Ersätter fakturan',
        invoice: 'Faktura',
        revision: (revisionNumber: number) =>
          `Korrigeringsfaktura ${revisionNumber}`,
        replacedBy: (link: React.ReactNode) => (
          <>Denna faktura är korrigerad. Ersättande faktura: {link}</>
        ),
        replacedByDraft: (link: React.ReactNode) => (
          <>För denna faktura finns ett ersättande korrigeringsutkast: {link}</>
        )
      },
      replacement: {
        title: 'Uppgifter relaterade till korrigering av fakturan',
        info: 'Du kan lägga till uppgifter relaterade till korrigeringen här.',
        reason: 'Orsak till korrigering',
        reasons: {
          SERVICE_NEED: 'Fel servicebehov',
          ABSENCE: 'Dagboksanteckning',
          INCOME: 'Saknade/felaktiga inkomstuppgifter',
          FAMILY_SIZE: 'Felaktig familjestorlek',
          RELIEF_RETROACTIVE: 'Avgiftsfrihet, retroaktiv',
          OTHER: 'Annan'
        },
        notes: 'Ytterligare information',
        attachments: 'Bilagor',
        sendInfo:
          'När du markerar denna faktura som överförd, markeras den ersatta fakturan som korrigerad!',
        send: 'Markera som överförd',
        markedAsSent: 'Markerad som överförd'
      },
      rows: {
        title: 'Fakturarader',
        product: 'Produkt',
        description: 'Förklaring',
        unitId: 'Enhet',
        daterange: 'Tidsperiod',
        amount: 'St',
        unitPrice: 'Á-pris',
        price: 'Summa',
        subtotal: 'Fakturans summa'
      },
      sum: {
        rowSubTotal: 'Summa för barnets rader',
        familyTotal: 'Familj totalt'
      },
      buttons: {
        markSent: 'Markera som överförd'
      }
    },
    distinctiveDetails: {
      MISSING_ADDRESS: 'Adress saknas'
    },
    openAbsenceSummary: 'Öppna frånvarosammanställning'
  },
  invoiceCorrections: {
    noChildren: 'Personen är inte huvudman för något barn',
    targetMonth: 'Korrigeras under faktureringsperioden',
    nextTargetMonth: 'Nästa faktureringsperiod',
    range: 'Orsakens tidsperiod',
    addRow: 'Lägg till korrigeringsrad',
    addTitle: 'Ny korrigeringsrad',
    editTitle: 'Redigera korrigeringsrad',
    deleteConfirmTitle: 'Ta bort korrigeringsrad?'
  },
  financeDecisions: {
    handlerSelectModal: {
      title: 'Kontrollera uppgifterna',
      label: 'Beslutsfattare',
      error: 'Laddning av beslutsfattare misslyckades, försök igen',
      default: 'Beslutsfattare som angetts i enhetens uppgifter',
      decisionCount: (count: number) =>
        count === 1 ? '1 beslut valt' : `${count} beslut valda`,
      resolve: (count: number) =>
        count === 1 ? 'Bekräfta och skapa beslut' : 'Bekräfta och skapa beslut'
    }
  },
  feeDecisions: {
    table: {
      title: 'Avgiftsbeslut',
      head: 'Huvudman',
      children: 'Barn',
      validity: 'Avgiftsbeslut giltigt',
      price: 'Summa',
      number: 'Nummer',
      status: 'Status',
      createdAt: 'Skapad',
      sentAt: 'Skickad',
      difference: {
        title: 'Ändring',
        value: {
          GUARDIANS: 'Vårdnadshavare',
          CHILDREN: 'Barn',
          INCOME: 'Inkomst',
          PLACEMENT: 'Placering',
          SERVICE_NEED: 'Servicebehov',
          SIBLING_DISCOUNT: 'Syskonrabatt',
          FEE_ALTERATIONS: 'Avgiftsändring',
          FAMILY_SIZE: 'Familjestorlek',
          FEE_THRESHOLDS: 'Avgiftsinställningar'
        },
        valueShort: {
          GUARDIANS: 'V',
          CHILDREN: 'B',
          INCOME: 'I',
          PLACEMENT: 'P',
          SERVICE_NEED: 'SB',
          SIBLING_DISCOUNT: 'SR',
          FEE_ALTERATIONS: 'Ä',
          FAMILY_SIZE: 'F',
          FEE_THRESHOLDS: 'AI'
        }
      },
      annullingDecision: 'Annullera eller avsluta beslut från perioden'
    },
    buttons: {
      checked: (count: number) =>
        count === 1 ? `${count} beslut valt` : `${count} beslut valda`,
      createDecision: (count: number) =>
        count === 1 ? 'Skapa beslut' : 'Skapa beslut',
      ignoreDraft: 'Hoppa över utkast',
      unignoreDrafts: (count: number) =>
        count === 1 ? 'Ångra överhoppning' : 'Ångra överhoppningar',
      markSent: 'Markera som postad',
      close: 'Stäng utan att spara',
      save: 'Spara ändringar',
      errors: {
        WAITING_FOR_MANUAL_SENDING:
          'En del huvudmän har beslut som väntar på manuell sändning'
      }
    }
  },
  ignoreDraftModal: {
    title: 'Vill du verkligen hoppa över utkastet?',
    content: (
      <div>
        <H3>Utkastet får endast hoppas över om följande saker stämmer:</H3>
        <ul>
          <li>Utkastet gäller det förflutna, och</li>
          <li>
            Utkastet är felaktigt eftersom kunduppgifterna i det förflutna är
            felaktiga, och
          </li>
          <li>
            Det ursprungliga skickade beslutet för samma period är korrekt
          </li>
        </ul>
        <p>
          Om utkastet är felaktigt eftersom uppgifterna är felaktiga (t.ex.
          familjerelationer har felaktigt tagits bort retroaktivt), är det
          viktigt att i första hand försöka korrigera uppgifterna till
          ursprungligt skick, eftersom de också påverkar andra system.
        </p>
        <p>
          Om utkastet är felaktigt eller onödigt, även om uppgifterna är
          korrekta, hoppa inte över utkastet, utan kontakta utvecklingsteamet så
          att felet kan undersökas och korrigeras.
        </p>
      </div>
    ),
    confirm: 'Jag förstår och bekräftar detta'
  },
  valueDecisions: {
    table: {
      title: 'Värdebeslut',
      head: 'Huvudman',
      child: 'Barn',
      validity: 'Värdebeslut giltigt',
      totalValue: 'SV-Värde',
      totalCoPayment: 'Självrisk',
      number: 'Nummer',
      status: 'Status',
      createdAt: 'Skapad',
      sentAt: 'Skickad',
      difference: {
        title: 'Ändring',
        value: {
          GUARDIANS: 'Vårdnadshavare',
          INCOME: 'Inkomst',
          FAMILY_SIZE: 'Familjestorlek',
          PLACEMENT: 'Placering',
          SERVICE_NEED: 'Servicebehov',
          SIBLING_DISCOUNT: 'Syskonrabatt',
          CO_PAYMENT: 'Självriskanandel före avgiftsändringar',
          FEE_ALTERATIONS: 'Avgiftsändringar',
          FINAL_CO_PAYMENT: 'Självriskanandel',
          BASE_VALUE: 'Grundvärde',
          VOUCHER_VALUE: 'Servicesedelns värde',
          FEE_THRESHOLDS: 'Avgiftsinställningar'
        },
        valueShort: {
          GUARDIANS: 'V',
          INCOME: 'I',
          FAMILY_SIZE: 'F',
          PLACEMENT: 'P',
          SERVICE_NEED: 'SB',
          SIBLING_DISCOUNT: 'SR',
          CO_PAYMENT: 'SJ',
          FEE_ALTERATIONS: 'Ä',
          FINAL_CO_PAYMENT: 'S',
          BASE_VALUE: 'GV',
          VOUCHER_VALUE: 'SV',
          FEE_THRESHOLDS: 'AI'
        }
      },
      annullingDecision: 'Annullera eller avsluta beslut från perioden'
    },
    buttons: {
      checked: (count: number) =>
        count === 1 ? `${count} beslut valt` : `${count} beslut valda`,
      createDecision: (count: number) =>
        count === 1 ? 'Skapa beslut' : 'Skapa beslut',
      ignoreDraft: 'Hoppa över utkast',
      unignoreDrafts: (count: number) =>
        count === 1 ? 'Ångra överhoppning' : 'Ångra överhoppningar',
      markSent: 'Markera som postad',
      close: 'Stäng utan att spara',
      save: 'Tallenna muutokset',
      errors: {
        WAITING_FOR_MANUAL_SENDING:
          'Osalla lapsista on päätöksiä, jotka odottavat manuaalista lähetystä'
      }
    }
  },
  payments: {
    table: {
      title: 'Betalningar',
      toggleAll: 'Välj alla rader som matchar sökningen',
      unit: 'Enhet',
      period: 'Utbetalningsperiod',
      createdAt: 'Utkast skapat',
      number: 'Fakturanr',
      amount: 'Summa',
      status: 'Status',
      nb: 'Obs',
      missingPaymentDetails: 'Uppgifter saknas'
    },
    buttons: {
      createPaymentDrafts: 'Skapa utbetalningsunderlag',
      checked: (count: number) =>
        count === 1 ? `${count} rad vald` : `${count} rader valda`,
      confirmPayments: (count: number) =>
        count === 1
          ? `Markera ${count} betalning som granskad`
          : `Markera ${count} betalningar som granskade`,
      revertPayments: (count: number) =>
        count === 1
          ? `Återställ ${count} betalning till utkast`
          : `Återställ ${count} betalningar till utkast`,
      sendPayments: (count: number) =>
        count === 1
          ? `Överför ${count} betalning`
          : `Överför ${count} betalningar`,
      deletePayment: (count: number) =>
        count === 1
          ? `Ta bort ${count} betalning`
          : `Ta bort ${count} betalningar`
    },
    status: {
      DRAFT: 'Utkast',
      CONFIRMED: 'Granskad',
      SENT: 'Överförd'
    },
    sendModal: {
      title: 'Överför valda betalningar',
      paymentDate: 'Betalningsdag',
      dueDate: 'Förfallodag'
    },
    distinctiveDetails: {
      MISSING_PAYMENT_DETAILS: 'Betalningsuppgifter saknas'
    }
  },
  placement: {
    type: {
      CLUB: 'Klubb',
      DAYCARE: 'Småbarnspedagogik',
      FIVE_YEARS_OLD_DAYCARE: 'Småbarnspedagogik för 5-åringar',
      PRESCHOOL_WITH_DAYCARE: 'Förskola och tillhörande småbarnspedagogik',
      PREPARATORY_WITH_DAYCARE:
        'Förberedande undervisning och tillhörande småbarnspedagogik',
      DAYCARE_PART_TIME: 'Deltids småbarnspedagogik',
      DAYCARE_FIVE_YEAR_OLDS: 'Småbarnspedagogik för 5-åringar',
      DAYCARE_PART_TIME_FIVE_YEAR_OLDS:
        'Deltids småbarnspedagogik för 5-åringar',
      PRESCHOOL: 'Förskola',
      PREPARATORY: 'Förberedande undervisning',
      PREPARATORY_DAYCARE:
        'Förberedande undervisning och tillhörande småbarnspedagogik',
      PREPARATORY_DAYCARE_ONLY:
        'Småbarnspedagogik tillhörande förberedande undervisning',
      PRESCHOOL_DAYCARE: 'Förskola och tillhörande småbarnspedagogik',
      PRESCHOOL_DAYCARE_ONLY: 'Småbarnspedagogik tillhörande förskola',
      PRESCHOOL_CLUB: 'Förskoleklubb',
      TEMPORARY_DAYCARE: 'Tillfällig heldags småbarnspedagogik',
      TEMPORARY_DAYCARE_PART_DAY: 'Tillfällig deltids småbarnspedagogik',
      SCHOOL_SHIFT_CARE: 'Skiftvård för skolbarn'
    },
    messagingCategory: {
      MESSAGING_CLUB: 'Klubb',
      MESSAGING_DAYCARE: 'Småbarnspedagogik',
      MESSAGING_PRESCHOOL: 'Förskola'
    },
    defaultOptionText: '(Standard)',
    defaultOptionMissingText: 'Standard servicebehov inte tillgängligt'
  },
  feeAlteration: {
    DISCOUNT: 'Rabatt',
    INCREASE: 'Höjning',
    RELIEF: 'Lättnad'
  },
  feeDecision: {
    title: {
      DRAFT: 'Utkast till avgiftsbeslut',
      IGNORED: 'Överhoppat utkast till avgiftsbeslut',
      WAITING_FOR_SENDING: 'Avgiftsbeslut (skickas)',
      WAITING_FOR_MANUAL_SENDING: 'Avgiftsbeslut (skickas manuellt)',
      SENT: 'Avgiftsbeslut',
      ANNULLED: 'Annullerat avgiftsbeslut'
    },
    distinctiveDetails: {
      UNCONFIRMED_HOURS: 'Saknat servicebehov',
      EXTERNAL_CHILD: 'Barn från annan kommun',
      RETROACTIVE: 'Retroaktivt beslut',
      NO_STARTING_PLACEMENTS: 'Dölj nya barn som börjar',
      MAX_FEE_ACCEPTED: 'Samtycke till högsta avgift',
      PRESCHOOL_CLUB: 'Endast förskoleklubb',
      NO_OPEN_INCOME_STATEMENTS: 'Inga öppna inkomstutredningar'
    },
    status: {
      DRAFT: 'Utkast',
      IGNORED: 'Överhoppat utkast',
      WAITING_FOR_SENDING: 'Skickas',
      WAITING_FOR_MANUAL_SENDING: 'Skickas manuellt',
      SENT: 'Skickad',
      ANNULLED: 'Annullerad'
    },
    type: {
      NORMAL: 'Vanligt avgiftsbeslut, ingen lättnad',
      RELIEF_ACCEPTED: 'Lättnad godkänd (Skickas manuellt)',
      RELIEF_PARTLY_ACCEPTED:
        'Partiell' + ' lättnad godkänd (Skickas manuellt)',
      RELIEF_REJECTED: 'Lättnad avvisad (Skickas manuellt)'
    },
    headOfFamily: 'Huvudman',
    partner: 'Annan vårdnadshavare / betalskyldig',
    decisionNumber: 'Beslutsnummer',
    validPeriod: 'Avgiftsbeslut giltigt',
    sentAt: 'Avgiftsbeslut skickat',
    decisionHandler: 'Beslutshanterare',
    relief: 'Avgiftsbeslutets lättnad',
    waitingManualSending: 'Skickas manuellt',
    pdfLabel: 'Avgiftsbeslut PDF',
    downloadPdf: 'Ladda ner PDF',
    pdfInProgress:
      '(PDF:en skapas. Ladda om sidan om ett ögonblick' +
      ' så kan du ladda ner den från länken bredvid.)',
    form: {
      nav: {
        return: 'Återvänd'
      },
      income: {
        title: 'Familjens inkomstuppgifter',
        maxFeeAccepted: 'Vårdnadshavarens samtycke till högsta avgiftsklass.'
      },
      child: {
        ssn: 'Personbeteckning',
        placementType: 'Placeringstyp',
        careArea: 'Serviceområde',
        daycare: 'Verksamhetsställe',
        placementDate: 'Placering giltig',
        serviceNeed: 'Servicebehov',
        name: 'Namn',
        postOffice: 'Postanstalt'
      },
      summary: {
        title: 'Sammanställning av avgiftsbeslutets grunder',
        income: {
          title: 'Sammanställning av familjens inkomster',
          effect: {
            label: 'Avgiftsgrund',
            MAX_FEE_ACCEPTED:
              'Vårdnadshavarens samtycke till högsta småbarnspedagogikavgift',
            INCOMPLETE: 'Familjens inkomstuppgifter är bristfälliga.',
            INCOME: 'Avgiften baseras på vårdnadshavarnas inkomstuppgifter',
            NOT_AVAILABLE:
              'Avgiften baseras på högsta inkomstklass (automatisk)'
          },
          details: {
            MAX_FEE_ACCEPTED: 'Samtycke till högsta småbarnspedagogikavgift',
            INCOMPLETE: 'Bristfälliga inkomstuppgifter',
            NOT_AVAILABLE: 'Inkomstuppgifter har inte lämnats'
          },
          income: 'Inkomster',
          expenses: 'Utgifter',
          total: 'Familjens inkomster totalt',
          familyComposition: 'Familjens sammansättning och avgiftsgrunder',
          familySize: 'Familjestorlek',
          persons: ' personer',
          feePercent: 'Avgiftsprocent',
          minThreshold: 'Minimibruttosgräns'
        },
        parts: {
          title: 'Sammanställning av familjens barns avgifter',
          siblingDiscount: 'syskonrabatt',
          sum: 'Summa'
        },
        totalPrice: 'Familjens småbarnspedagogikavgift totalt'
      },
      buttons: {
        saveChanges: 'Spara ändringar'
      }
    },
    modal: {
      title: 'Vill du återvända utan att spara ändringarna?',
      cancel: 'Återvänd utan att spara',
      confirm: 'Fortsätt redigera'
    }
  },
  filters: {
    searchTerms: 'Sökvillkor',
    freeTextPlaceholder:
      'Sök med namn, personbeteckning, adress eller avgiftsbeslutsnummer',
    area: 'Område',
    unit: 'Verksamhetsställe',
    financeDecisionHandler: 'Beslutshanterare för ekonomibeslut',
    unitPlaceholder: 'Välj verksamhetsställe',
    financeDecisionHandlerPlaceholder: 'Välj arbetare',
    distinctiveDetails: 'Annat att observera',
    difference: 'Ändring',
    providerType: 'Arrangemangsform',
    status: 'Status',
    clear: 'Rensa val',
    validityPeriod: 'Giltighetstid',
    searchByStartDate: 'Startdatum infaller inom vald tidsperiod',
    invoiceDate: 'Fakturans datum',
    invoiceSearchByStartDate: 'Skicka fakturor från vald period',
    paymentDate: 'Betalningsdag',
    paymentFreeTextPlaceholder: 'Sök med betalningsnummer',
    incomeStatementSent: 'Inkomstutredning skickad',
    incomeStatementPlacementValidDate: 'Placering giltig'
  },
  valueDecision: {
    title: {
      DRAFT: 'Utkast till värdebeslut',
      IGNORED: 'Överhoppat utkast till värdebeslut',
      WAITING_FOR_SENDING: 'Värdebeslut (skickas)',
      WAITING_FOR_MANUAL_SENDING: 'Värdebeslut (skickas manuellt)',
      SENT: 'Värdebeslut',
      ANNULLED: 'Annullerat värdebeslut'
    },
    headOfFamily: 'Huvudman',
    partner: 'Annan vårdnadshavare / betalskyldig',
    decisionNUmber: 'Beslutsnummer',
    validPeriod: 'Värdebeslut giltigt',
    sentAt: 'Värdebeslut skickat',
    pdfLabel: 'Värdebeslut PDF',
    decisionHandlerName: 'Beslutshanterare',
    relief: 'Värdebeslutets lättnad',
    downloadPdf: 'Ladda ner PDF',
    pdfInProgress:
      '(PDF:en skapas. Ladda om sidan om ett ögonblick så kan du ladda ner den från länken bredvid.)',
    status: {
      DRAFT: 'Utkast',
      IGNORED: 'Överhoppat utkast',
      WAITING_FOR_SENDING: 'Skickas',
      WAITING_FOR_MANUAL_SENDING: 'Skickas manuellt',
      SENT: 'Skickad',
      ANNULLED: 'Annullerad'
    },
    type: {
      NORMAL: 'Vanligt värdebeslut, ingen lättnad',
      RELIEF_ACCEPTED: 'Lättnad godkänd (Skickas manuellt)',
      RELIEF_PARTLY_ACCEPTED:
        'Partiell' + ' lättnad godkänd (Skickas manuellt)',
      RELIEF_REJECTED: 'Lättnad avvisad (Skickas manuellt)'
    },
    child: {
      name: 'Namn',
      ssn: 'Personbeteckning',
      postOffice: 'Postanstalt',
      placementType: 'Placeringstyp',
      careArea: 'Serviceområde',
      unit: 'Verksamhetsställe',
      serviceNeed: 'Servicebehov'
    },
    summary: {
      title: 'Sammanställning av värdebeslutets grunder',
      coPayment: 'Självriskanandel',
      sum: 'Summa',
      siblingDiscount: 'Syskonrabatt',
      totalValue: 'Servicesedelns värde efter självrisk',
      income: {
        title: 'Sammanställning av familjens inkomster',
        effect: {
          label: 'Avgiftsgrund',
          MAX_FEE_ACCEPTED:
            'Vårdnadshavarens samtycke till högsta småbarnspedagogikavgift',
          INCOMPLETE: 'Familjens inkomstuppgifter är bristfälliga.',
          INCOME: 'Avgiften baseras på vårdnadshavarnas inkomstuppgifter',
          NOT_AVAILABLE: 'Avgiften baseras på högsta inkomstklass (automatisk)'
        },
        details: {
          MAX_FEE_ACCEPTED: 'Samtycke till högsta småbarnspedagogikavgift',
          INCOMPLETE: 'Bristfälliga inkomstuppgifter',
          NOT_AVAILABLE: 'Inkomstuppgifter har inte lämnats'
        },
        income: 'Inkomster',
        expenses: 'Utgifter',
        total: 'Familjens inkomster totalt',
        familyComposition: 'Familjens sammansättning och avgiftsgrunder',
        familySize: 'Familjestorlek',
        persons: ' personer',
        feePercent: 'Avgiftsprocent',
        minThreshold: 'Minimibruttosgräns'
      },
      value: 'Servicesedelns värde',
      age: {
        LESS_THAN_3: 'Under 3 år',
        OVER_3: 'Minst 3 år'
      },
      assistanceNeedCoefficient: 'stödbehovets koefficient',
      hoursPerWeek: 'timmar per vecka'
    }
  },
  // these are directly used by date picker so order and naming matters!
  datePicker: {
    months: [
      'januari',
      'februari',
      'mars',
      'april',
      'maj',
      'juni',
      'juli',
      'augusti',
      'september',
      'oktober',
      'november',
      'december'
    ],
    weekdaysLong: [
      'måndag',
      'tisdag',
      'onsdag',
      'torsdag',
      'fredag',
      'lördag',
      'söndag'
    ],
    weekdaysShort: ['må', 'ti', 'on', 'to', 'fr', 'lö', 'sö']
  },
  absences: {
    title: 'Frånvaro',
    absenceTypes: {
      OTHER_ABSENCE: 'Frånvaro',
      SICKLEAVE: 'Sjukdom',
      UNKNOWN_ABSENCE: 'Oanmäld frånvaro',
      PLANNED_ABSENCE: 'Skiftarbetsfrånvaro',
      TEMPORARY_RELOCATION: 'Barn reservplacerat på annan plats',
      PARENTLEAVE: 'Föräldraledighet',
      FORCE_MAJEURE: 'Avgiftsfri dag',
      FREE_ABSENCE: 'Avgiftsfri frånvaro',
      UNAUTHORIZED_ABSENCE: 'Oanmäld frånvaro från jour',
      NO_ABSENCE: 'Ingen frånvaro'
    },
    missingHolidayReservation:
      'Vårdnadshavare har inte bekräftat semesterreservation',
    missingHolidayQuestionnaireAnswer:
      'Vårdnadshavare har inte svarat på frånvaroförfrågan',
    shiftCare: 'Kväll-/skiftvård',
    requiresBackupCare: 'Väntar på reservplacering',
    additionalLegendItems: {
      CONTRACT_DAYS: 'Servicebehov med avtalsdagar'
    },
    absenceTypesShort: {
      OTHER_ABSENCE: 'Frånvaro',
      SICKLEAVE: 'Sjukdom',
      UNKNOWN_ABSENCE: 'Oanmäld',
      PLANNED_ABSENCE: 'Skiftarbete',
      TEMPORARY_RELOCATION: 'Reservplacering',
      PARENTLEAVE: 'Föräldral.',
      FORCE_MAJEURE: 'Avgiftsfri',
      FREE_ABSENCE: 'Avgiftsfri',
      UNAUTHORIZED_ABSENCE: 'Avgift',
      NO_ABSENCE: 'Ej frånvaro'
    },
    absenceTypeInfo: {
      OTHER_ABSENCE:
        'Används alltid när vårdnadshavaren har anmält frånvaro, inklusive regelbundna ledigheter och semestertid. Används också i skiftenheter för barns semesteranteckningar eller andra frånvaror som är utanför planerade närvaroreservationer.',
      SICKLEAVE:
        'Sammanhängande sjukfrånvaro över 11 dagar påverkar avgiften nedsättande.',
      UNKNOWN_ABSENCE:
        'Används när vårdnadshavaren inte har anmält frånvaro, påverkar även faktureringen i juli. Koden ändras endast om det gäller sjukfrånvaro vars fortsättning vårdnadshavaren anmäler följande dag.',
      PLANNED_ABSENCE:
        'Används endast i skiftenheter när det gäller ledighet på grund av skiftarbete, semestertider markeras med Frånvaro-kod. Ger inte rätt till avgiftsreducering på fakturan.',
      TEMPORARY_RELOCATION:
        'Barnet har reservplacerats i en annan enhet. Frånvaron kan markeras om sådan är känd. Bekanta dig dock med semesteranvisningen om frånvaron gäller semestertid.',
      PARENTLEAVE:
        'Föräldraledighet, markeras endast för det barn vars skull vårdnadshavaren är ledig, inte för syskon. Påverkar avgiften så att tiden är avgiftsfri.',
      FORCE_MAJEURE:
        'Används endast i specialsituationer enligt administrationens anvisningar.',
      FREE_ABSENCE: 'Avgiftsfri sommarfrånvaro',
      UNAUTHORIZED_ABSENCE: 'Oanmäld frånvaro från jour',
      NO_ABSENCE: 'Om barnet är på plats, markera ingenting.'
    },
    additionalLegendItemInfos: {
      CONTRACT_DAYS: 'Barn med avtalsdag som servicebehov'
    },
    careTypes: {
      SCHOOL_SHIFT_CARE: 'Skiftvård för skolbarn',
      PRESCHOOL: 'Förskola',
      PRESCHOOL_DAYCARE: 'Tillhörande småbarnspedagogik',
      DAYCARE_5YO_FREE: 'Småbarnspedagogik för 5-åringar',
      DAYCARE: 'Småbarnspedagogik',
      CLUB: 'Klubb'
    },
    absenceCategories: {
      NONBILLABLE:
        'Förskola, förberedande, småbarnspedagogik för 5-åringar eller klubbverksamhet',
      BILLABLE: 'Småbarnspedagogik (avgiftsbelagd)'
    },
    modifiedByStaff: 'Personal',
    modifiedByCitizen: 'Vårdnadshavare',
    modal: {
      absenceSectionLabel: 'Orsak till frånvaro',
      placementSectionLabel: 'Verksamhetsform som frånvaron gäller',
      saveButton: 'Spara',
      cancelButton: 'Avbryt',
      absenceTypes: {
        OTHER_ABSENCE: 'Frånvaro',
        SICKLEAVE: 'Sjukdom',
        UNKNOWN_ABSENCE: 'Oanmäld frånvaro',
        PLANNED_ABSENCE: 'Skiftarbetsfrånvaro',
        TEMPORARY_RELOCATION: 'Reservplacerad på annan plats',
        PARENTLEAVE: 'Föräldraledighet',
        FORCE_MAJEURE: 'Avgiftsfri dag (begränsad användning)',
        FREE_ABSENCE: 'Avgiftsfri frånvaro',
        UNAUTHORIZED_ABSENCE: 'Oanmäld frånvaro från jour',
        NO_ABSENCE: 'Ingen frånvaro',
        MISSING_HOLIDAY_RESERVATION: 'Semesteranmälan saknas'
      },
      free: 'Avgiftsfri',
      paid: 'Avgiftsbelagd',
      absenceSummaryTitle: 'Barnets frånvarosammanställning'
    },
    table: {
      selectAll: 'Välj alla',
      staffRow: 'Personal närvarande',
      disabledStaffCellTooltip: 'Gruppen existerar inte den valda dagen',
      reservationsTotal: 'Reservation/mån',
      attendancesTotal: 'Utfall/mån'
    },
    legendTitle: 'Förklaring av markeringar',
    addAbsencesButton(numOfSelected: number) {
      return numOfSelected === 1
        ? 'Lägg till markering för vald...'
        : 'Lägg till markeringar för valda...'
    },
    notOperationDay: 'Ingen verksamhetsdag',
    absence: 'Frånvaro',
    reservation: 'Reservation',
    present: 'Närvarande',
    guardian: 'Vårdnadshavare',
    staff: 'Personal',
    dailyServiceTime: 'Avtalstid'
  },
  placementDraft: {
    preschoolDaycare: 'Tillhörande småbarnspedagogik',
    card: {
      title: 'Högsta beläggningsgrad från placeringsdagen',
      titleSpeculated: 'Beläggningsgrad om barnet placeras'
    },
    upcoming: 'Kommande',
    active: 'Aktiv',
    currentPlacements: 'Befintliga placeringar',
    noCurrentPlacements: 'Inga befintliga placeringar',
    addOtherUnit: 'Lägg till annan enhet',
    placementOverlapError:
      'Tidigare överlappande placeringar avbryts automatiskt om kommuninvånaren tar emot den erbjudna platsen.',
    createPlacementDraft: 'Skapa placeringsplan',
    datesTitle: 'Nu skapad placeringsplan',
    type: 'Placeringstyp',
    date: 'Placeringsdatum',
    dateError: 'Överlappande placering för perioden.',
    preparatoryPeriod: 'Förberedande undervisning',
    dateOfBirth: 'Födelsedatum',
    selectUnit: 'Välj enhet',
    selectedUnit: 'Vald enhet',
    restrictedDetails: 'Vårdnadshavaren har spärrmarkering',
    restrictedDetailsTooltip:
      'Beslutet måste skickas manuellt till den andra vårdnadshavaren när sökanden har spärrmarkering.'
  },
  decisionDraft: {
    title: 'Beslutfattande och sändning',
    info1:
      'Genom att skicka beslutet godkänner du placeringsplanen. Kommuninvånaren skickas de beslut som du har valt nedan.',
    info2:
      'Observera att valen och datumen endast påverkar beslutsdokumenten. Om du vill redigera den faktiska placeringen, returnera ansökan tillbaka till placeringskö och placera den på nytt.',
    ssnInfo1:
      'Vårdnaden kan inte verifieras utan vårdnadshavarens och barnets personbeteckning.',
    ssnInfo2:
      'Skicka det utskrivna beslutet per post och markera det som postat.',
    unitInfo1: 'Enhetens uppgifter är bristfälliga.',
    unitInfo2:
      'Bristfälliga uppgifter måste uppdateras innan beslut skapas. Kontakta utvecklarna.',
    notGuardianInfo1: 'Ansökans vårdnadshavare är inte barnets vårdnadshavare.',
    notGuardianInfo2:
      'Personen som är markerad som vårdnadshavare på ansökan är inte enligt BIS barnets vårdnadshavare. Beslutet måste skickas på papper.',
    unit: 'Verksamhetsställe',
    contact: 'Kontaktperson',
    decisionLabelHeading: 'Placeringstyp',
    decisionValueHeading: 'Beslutsdatum',
    types: {
      CLUB: 'Klubb',
      DAYCARE: 'Småbarnspedagogik',
      DAYCARE_PART_TIME: 'Deltids småbarnspedagogik',
      PRESCHOOL_DAYCARE: 'Småbarnspedagogik tillhörande förskola',
      PRESCHOOL_CLUB: 'Förskoleklubb',
      PRESCHOOL: 'Förskola',
      PREPARATORY: 'Förberedande undervisning',
      PREPARATORY_EDUCATION: 'Förberedande undervisning',
      PREPARATORY_DAYCARE:
        'Småbarnspedagogik tillhörande förberedande undervisning'
    },
    placementUnit: 'Vid placering vald enhet',
    selectedUnit: 'Enhet att välja för beslutet',
    unitDetailsHeading: 'Uppgifter som visas på beslutet',
    preschoolDecisionName: 'Enhetens namn på förskolebeslutet',
    daycareDecisionName: 'Enhetens namn på småbarnspedagogikbeslutet',
    unitManager: 'Enhetschef',
    unitAddress: 'Enhetens adress',
    handlerName: 'Handläggarens namn',
    handlerAddress: 'Handläggarens adress',
    receiver: 'Mottagare',
    otherReceiver: 'Mottagare (annan vårdnadshavare)',
    missingValue: 'Uppgift saknas.',
    noOtherGuardian: 'Ingen annan vårdnadshavare',
    differentUnit:
      'Enheten som visas på beslutet är annan än i den ursprungliga placeringen.'
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
      placementType: 'Sijoitustyyppi',
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
      age: 'Ikä',
      dateOfBirth: 'Syntymäaika',
      attendanceType: 'Läsnäolo',
      attendanceTypes: {
        RESERVATION: 'Varaus',
        REALIZATION: 'Toteuma'
      }
    },
    applications: {
      title: 'Inkomna ansökningar',
      description:
        'Rapporten listar inkomna och handlagda ansökningar enhetsvis.',
      ageInfo: 'Barnets ålder räknas på slutdagen för vald tidsperiod',
      preferredStartingDate: 'Startdag',
      under3Years: 'Ansökningar om småbarnspedagogik (under 3 år)',
      over3Years: 'Ansökningar om småbarnspedagogik (över 3 år)',
      preschool: 'Ansökningar om förskoleundervisning',
      club: 'Ansökningar om klubb',
      totalChildren: 'Barn totalt som ansökt'
    },
    childDocumentDecisions: {
      title: 'Beslut om stöd',
      description: 'Beslut om stöd som skickats till beslutsfattaren.',
      statusFilter: 'Visa statusar',
      otherFilters: 'Andra val',
      includeEnded: 'Visa avslutade beslut',
      templateName: 'Beslut',
      childName: 'Barn',
      modifiedAt: 'Ändrat',
      decisionMaker: 'Beslutsfattare',
      decisionMade: 'Beslut fattat',
      status: 'Status'
    },
    decisions: {
      title: 'Beslut',
      description: 'Rapporten listar fattade beslut enhetsvis.',
      ageInfo: 'Barnets ålder räknas på beslutsöversändningsdagen',
      sentDate: 'Beslutsöversändningsdag',
      daycareUnder3: 'Beslut om småbarnspedagogik (under 3 år)',
      daycareOver3: 'Beslut om småbarnspedagogik (över 3 år)',
      preschool: 'Beslut om förskoleundervisning',
      preschoolDaycare: 'Beslut om förskoleundervisning+kompletterande',
      connectedDaycareOnly: 'Beslut om senare ansökt kompletterande',
      preparatory: 'Beslut om förberedande',
      preparatoryDaycare: 'Beslut om förberedande+kompletterande',
      club: 'Beslut om klubb',
      preference1: '1. önskemål',
      preference2: '2. önskemål',
      preference3: '3. önskemål',
      preferenceNone: 'Inte som önskemål',
      total: 'Beslut totalt'
    },
    raw: {
      title: 'Rådata-rapport',
      description:
        'Mindre förädlad omfattande datamängd, som kan användas för att skapa olika rapporter.'
    },
    assistanceNeedDecisions: {
      title: 'Beslut om stöd enligt gammal modell',
      description:
        'Beslut om stöd enligt gammal modell som skickats till beslutsfattaren.',
      decisionNumber: 'Beslutsnummer',
      childhoodEducationPrefix: 'SB ',
      preschoolPrefix: 'FU ',
      sentToDecisionMaker: 'Skickad till beslutsfattaren',
      decisionMade: 'Beslut fattat',
      status: 'Status',
      statusFilter: 'Visa statusar',
      otherFilters: 'Andra val',
      showExpired: 'Visa avslutade beslut om stöd',
      returnForEditModal: {
        title: 'Returnera beslut för redigering?',
        okBtn: 'Returnera för redigering',
        text: 'Beslutsförslag skickas inte till medborgaren.'
      },
      rejectModal: {
        title: 'Avslå beslut?',
        okBtn: 'Avslå beslut',
        text: 'Vill du verkligen fatta ett avslagsbeslut? Det avslagna beslutet skickas till medborgaren för visning i eVaka.'
      },
      approveModal: {
        title: 'Godkänn beslut?',
        okBtn: 'Godkänn beslut',
        text: 'Vill du verkligen godkänna beslutet? Det godkända beslutet skickas till medborgaren för visning i eVaka.'
      },
      approveFailedModal: {
        title: 'Beslutsgodkännandet misslyckades',
        okBtn: 'Stäng'
      },
      annulModal: {
        title: 'Annullera beslut?',
        okBtn: 'Annullera beslut',
        text: 'Beslutet får inte annulleras utan att först diskutera det med vårdnadshavaren. Med ett nytt beslut får vårdnadshavarens ställning inte försämras enligt förvaltningslagen. Vill du verkligen annullera beslutet? Beslutets giltighet upphör omedelbart. Information om annulleringen och dess motivering skickas till medborgaren för visning i eVaka.',
        inputPlaceholder: 'Beskriv varför beslutet annullerades'
      },
      mismatchDecisionMakerWarning: {
        text: 'Du är inte beslutsfattare för detta beslut, så du kan inte fatta beslut.',
        link: 'Byt dig själv till beslutsfattare.'
      },
      mismatchDecisionMakerModal: {
        title: 'Byt dig själv till beslutsfattare',
        text: 'Genom att ändra beslutsfattare kan du returnera beslutet för redigering eller avslå eller godkänna beslutet. Ditt namn och titel ändras i beslutet.',
        titlePlaceholder: 'Titel',
        okBtn: 'Byt beslutsfattare'
      },
      rejectDecision: 'Avslå beslut',
      returnDecisionForEditing: 'Returnera för redigering',
      approveDecision: 'Godkänn beslut',
      annulDecision: 'Annullera beslut'
    },
    attendanceReservation: {
      title: 'Dagsvisa barnets ankomst- och avresetider',
      description: 'Rapport om barns reservationer och behov av personalantal',
      ungrouped: 'Barn som väntar på grupp',
      capacityFactor: 'Koeff',
      staffCount: 'Personal',
      tooLongRange: 'Du kan hämta rapporten för högst två månaders period.'
    },
    attendanceReservationByChild: {
      title: 'Närvaro- och frånvarotider barnvis',
      description:
        'Rapporten listar barnvis de av vårdnadshavare meddelade avgångs- och ankomsttider. Rapporten är tillgänglig grupp- och enhetsvis.',
      ungrouped: 'Barn som väntar på grupp',
      orderByOptions: {
        start: 'Ankomsttid',
        end: 'Avgångstid'
      },
      absence: 'Frånvaro',
      noReservation: 'Reservation saknas',
      filterByTime: 'Filtrera enligt tid',
      showOnlyShiftCare: 'Visa endast skiftvård',
      includeClosed: 'Visa avslutade enheter och grupper',
      reservationStartTime: 'Ankomst',
      reservationEndTime: 'Avgång',
      timeFilterError: 'Fel'
    },
    childAttendance: {
      title: 'Barnets närvaro- och frånvarouppgifter',
      range: 'Tidsperiod',
      date: 'Dag',
      reservations: 'Reservation',
      attendances: 'Närvaro',
      absenceBillable: 'Frånvaro (avgiftsbelagd)',
      absenceNonbillable: 'Frånvaro (avgiftsfri)'
    },
    customerFees: {
      title: 'Kundavgifter',
      description: 'Rapport om summor för kundavgifter barnvis.',
      date: 'Datum',
      area: 'Serviceområde',
      unit: 'Enhet',
      providerType: 'Arrangemangsform',
      placementType: 'Placeringstyp',
      type: 'Beslutstyp',
      types: {
        FEE_DECISION: 'Avgiftsbeslut',
        VOUCHER_VALUE_DECISION: 'Värdebeslut'
      },
      fee: 'Avgift per barn',
      count: 'Antal'
    },
    duplicatePeople: {
      title: 'Duplicerade medborgare',
      description:
        'Rapporten listar och låter sammanslå personer som verkar finnas flera gånger i systemet.',
      moveFrom: 'Flytta uppgifter',
      moveTo: 'Flytta hit',
      confirmMoveTitle:
        'Vill du verkligen flytta alla uppgifter till en annan person?',
      confirmDeleteTitle: 'Vill du verkligen ta bort denna person?',
      errorTitle: 'Överföringen av uppgifter misslyckades',
      errorText:
        'Kontrollera att personerna inte har exempelvis överlappande placeringar, tjänstebehov eller andra överlappningar som kan förhindra sammanslagningen.',
      columns: {
        'absence.child_id': 'Frånvaror',
        'absence.modified_by_guardian_id': 'Självantecknade frånvaror',
        'absence_application.child_id': 'Frånvaroansökningar för förskola',
        'application.child_id': 'Ansökningar (som barn)',
        'application.guardian_id': 'Ansökningar (som vårdnadshavare)',
        'application.other_guardian_id':
          'Ansökningar (som andra vårdnadshavare)',
        'assistance_action.child_id': 'Stödåtgärder',
        'assistance_need.child_id': 'Stödbehov',
        'assistance_need_decision.child_id': 'Beslut om stödbehov',
        'assistance_need_decision_guardian.person_id':
          'Beslut om stöd som vårdnadshavare',
        'assistance_need_voucher_coefficient.child_id':
          'Servicesedelkoefficienter för stöd',
        'attachment.uploaded_by_person': 'Bilagor',
        'attendance_reservation.child_id': 'Närvaroreservationer',
        'attendance_reservation.created_by_guardian_id':
          'Självantecknade närvaroreservationer',
        'backup_care.child_id': 'Reservplaceringar',
        'backup_pickup.child_id': 'Reservhämtare',
        'calendar_event_attendee.child_id': 'Kalenderdeltagare',
        'child_attendance.child_id': 'Närvaroanteckningar',
        'child_images.child_id': 'Bilder',
        'backup_curriculum_document.child_id': 'Gamla läroplaner',
        'daily_service_time.child_id': 'Småbarnspedagogiska tider',
        'daily_service_time_notification.guardian_id':
          'Meddelanden om småbarnspedagogiska tider',
        'daycare_daily_note.child_id': 'Anteckningar',
        'family_contact.child_id': 'Kontaktpersoner (barn)',
        'family_contact.contact_person_id': 'Kontaktpersoner (vuxen)',
        'fee_alteration.person_id': 'Avgiftsändringar',
        'fee_decision.head_of_family_id': 'Avgiftsbeslut (huvudman)',
        'fee_decision.partner_id': 'Avgiftsbeslut (partner)',
        'fee_decision_child.child_id': 'Avgiftsbeslut rader',
        'fridge_child.child_id': 'Huvudmän',
        'fridge_child.head_of_child': 'Kylskåps barn',
        'fridge_partner.person_id': 'Kylskåps partner',
        'foster_parent.child_id': 'Fosterbarn',
        'foster_parent.parent_id': 'Fosterföräldrar',
        'holiday_questionnaire_answer.child_id': 'Enkätsvar',
        'income.person_id': 'Inkomstuppgifter',
        'income_statement.person_id': 'Inkomstutredningar',
        'invoice.codebtor': 'Fakturor (medgäldenär)',
        'invoice.head_of_family': 'Fakturor',
        'invoice_correction_row.child_id': 'Faktura korrigeringsrader (barn)',
        'invoice_correction_row.head_of_family':
          'Faktura korrigeringsrader (huvudman)',
        'invoice_row.child': 'Fakturarader',
        'koski_study_right.child_id': 'Koski studierättigheter',
        'nekku_special_diet_choices.child_id': 'Nekku specialkost',
        'pedagogical_document.child_id': 'Pedagogiska dokument',
        'placement.child_id': 'Placeringar',
        'service_application.child_id': 'Tjänstebeh. ansökningar (som barn)',
        'service_application.person_id':
          'Tjänstebeh. ansökningar (som vårdnadshavare)',
        'varda_child.person_id': 'Varda barn',
        'varda_service_need.evaka_child_id': 'Varda tjänstebehov',
        'backup_vasu_document.child_id': 'Gamla planer för småbarnspedagogik',
        'voucher_value_decision.child_id': 'Värdebeslut rader',
        'voucher_value_decision.head_of_family_id': 'Värdebeslut (huvudman)',
        'voucher_value_decision.partner_id': 'Värdebeslut (partner)',
        'message.sender_id': 'Skickade meddelanden',
        'message_content.author_id': 'Skrivna meddelandeinnehåll',
        'message_recipients.recipient_id': 'Mottagna meddelanden',
        'message_draft.account_id': 'Meddelandeutkast'
      }
    },
    familyConflicts: {
      title: 'Familjekonflikter',
      description:
        'Rapporten listar huvudmän som har konflikter i sina familjerelationer. Konflikt kan uppstå om familjerelationer angivna i ansökan står i strid med tidigare uppgifter.',
      name: 'Huvudmannens namn',
      ssn: 'Personbeteckning',
      partnerConflictCount: 'Konflikter i partner',
      childConflictCount: 'Konflikter i barn'
    },
    familyContacts: {
      date: 'Datum',
      name: 'Barnets namn',
      ssn: 'Personbeteckning',
      group: 'Grupp',
      address: 'Adress',
      headOfChild: 'Huvudman',
      guardian1: 'Vårdnadshavare',
      guardian2: 'Andra vårdnadshavare',
      phone: 'Telefonnummer',
      email: 'E-postadress'
    },
    familyDaycareMealCount: {
      title: 'Måltidsrapport för barn i familjedagvård',
      description:
        'Rapporten räknar närvaroanteckningar för barn i familjedagvård under måltidstider och grupperar resultaten enhets- och områdesvis.',
      childName: 'Barnets namn',
      firstName: 'Förnamn',
      lastName: 'Efternamn',
      daycareName: 'Verksamhetsenhet',
      timePeriod: 'Tidsperiod',
      timePeriodTooLong: 'Tidsperiod högst 6 mån',
      careArea: 'Serviceområde',
      total: 'Totalt',
      breakfastCountHeader: 'Frukost',
      lunchCountHeader: 'Lunch',
      snackCountHeader: 'Mellanmål',
      totalHeader: 'Måltider totalt',
      noCareAreasFound: 'Inga serviceområden med resultat',
      noDaycaresFound: 'Inga enheter med resultat'
    },
    endedPlacements: {
      title: 'Barn som avslutar småbarnspedagogik',
      description:
        'Rapport till FPA om barn som avslutar småbarnspedagogik och eventuellt fortsätter senare.',
      ssn: 'Personbeteckning',
      placementEnd: 'Avslutar småbarnspedagogik',
      unit: 'Enhet',
      area: 'Område',
      nextPlacementStart: 'Fortsätter småbarnspedagogik',
      nextPlacementUnitName: 'Fortsätter i enhet'
    },
    missingHeadOfFamily: {
      title: 'Huvudmän saknas',
      description:
        'Rapporten listar barn som saknar huvudman under den nuvarande placeringsperioden.',
      childLastName: 'Barnets efternamn',
      childFirstName: 'Barnets förnamn',
      showFosterChildren: 'Visa även fosterbarn',
      daysWithoutHeadOfFamily: 'Ofullständiga dagar'
    },
    missingServiceNeed: {
      title: 'Tjänstebehov saknas',
      description:
        'Rapporten listar barn som saknar tjänstebehov under placeringsperioden.',
      daysWithoutServiceNeed: 'Ofullständiga dagar',
      defaultOption: 'Använt standardtjänstebehov'
    },
    invalidServiceNeed: {
      title: 'Felaktiga tjänstebehov',
      description:
        'Rapporten listar tjänstebehov som verkar innehålla ett fel.',
      unit: 'Nuvarande enhet',
      noCurrentUnit: 'Avslutad'
    },
    partnersInDifferentAddress: {
      title: 'Partner på annan adress',
      description:
        'Rapporten listar personer vars kylskåpspartner enligt BIS bor på en annan adress. Kontrollera om dessa personer fortfarande är samboende.',
      person1: 'Person',
      address1: 'Adress',
      person2: 'Partner',
      address2: 'Partners adress'
    },
    presence: {
      title: 'Närvaroanteckningar',
      date: 'datum',
      SSN: 'personbeteckning',
      daycareId: 'småbarnspedagogisk anstalt id',
      daycareGroupName: 'grupp',
      present: 'närvarande',
      description: 'Rapport för forskningsbruk om ekonomins läge',
      info: 'Tidsperiodens maximala längd är två veckor.'
    },
    serviceNeeds: {
      title: 'Barns tjänstebehov och ålder i enheter',
      description:
        'Rapporten listar antal barn i enheter enligt tjänstebehov och ålder.',
      age: 'Ålder',
      fullDay: 'heldags',
      partDay: 'halvdags',
      fullWeek: 'helvecka',
      partWeek: 'halvvecka',
      shiftCare: 'skiftvård',
      missingServiceNeed: 'tjänstebehov saknas',
      total: 'barn totalt'
    },
    exceededServiceNeed: {
      title: 'Överskriden tjänstebehov',
      description:
        'Rapporten listar barn vars tjänstebehov i timmar överskridits.',
      serviceNeedHours: 'Tjänstebehov (h)',
      usedServiceHours: 'Använt (h)',
      groupLinkHeading: 'Enhetens veckokalender',
      excessHours: 'Överskridning (h)'
    },
    units: {
      title: 'Enheter',
      description: 'Sammanfattning av enheters uppgifter.',
      name: 'Namn',
      careAreaName: 'Serviceområde',
      careTypeCentre: 'Daghem',
      careTypeFamily: <span>Familje&shy;dagvård</span>,
      careTypeFamilyStr: 'Familjedagvård',
      careTypeGroupFamily: <span>Grupp&shy;familje&shy;dagvård</span>,
      careTypeGroupFamilyStr: 'Gruppfamiljedagvård',
      careTypeClub: 'Klubb',
      careTypePreschool: 'Förskola',
      careTypePreparatoryEducation: 'Förberedande',
      clubApply: <span>Klubb&shy;ansökan</span>,
      clubApplyStr: 'Klubbansökan',
      daycareApply: <span>Daghem&shy;ansökan</span>,
      daycareApplyStr: 'Daghemansökan',
      preschoolApply: <span>Förskole&shy;ansökan</span>,
      preschoolApplyStr: 'Förskoleansökan',
      providerType: 'Arrangemangsform',
      uploadToVarda: 'Varda',
      uploadChildrenToVarda: 'Varda (barn)',
      uploadToKoski: 'Koski',
      ophUnitOid: 'Verksamhetsställets OID',
      ophOrganizerOid: 'Anordnarens OID',
      invoicedByMunicipality: 'Faktureras från eVaka',
      costCenter: 'Kostnadsställe',
      address: 'Besöksadress',
      unitManagerName: 'Enhetens ledare',
      unitManagerPhone: 'Ledarens tel.',
      capacity: 'Beräknad kapacitet'
    },
    childrenInDifferentAddress: {
      title: 'Barn på annan adress',
      description:
        'Rapporten listar huvudmän vars kylskåpsbarn enligt BIS bor på en annan adress. En del av dessa kan vara fel som borde korrigeras.',
      person1: 'Huvudman',
      address1: 'Huvudmannens adress',
      person2: 'Barn',
      address2: 'Barnets adress'
    },
    childAgeLanguage: {
      title: 'Barns språk och ålder i enheter',
      description:
        'Rapporten listar antal barn i enheter enligt språk och ålder. Endast mottagna platser beaktas.'
    },
    childDocuments: {
      title: 'Rapport om pedagogiska dokument',
      description:
        'Rapporten visar det nuvarande läget för pedagogiska dokument i valda enheter.',
      info: 'Siffrorna visar hur många barn som har något av de valda dokumenten i det givna tillståndet.',
      info2:
        'I kolumnerna "Inga dokument" och "Barn totalt" räknas endast de barn för vilka det är möjligt att skapa något av de valda dokumenten.',
      filters: {
        units: 'Enheter',
        templates: 'Dokument'
      },
      table: {
        unitOrGroup: 'Enhet/Grupp',
        draft: 'Utkast',
        prepared: 'Uppgjord',
        completed: 'Färdig',
        none: 'Inga dokument',
        total: 'Barn totalt',
        expand: 'Visa grupper',
        collapse: 'Dölj grupper'
      },
      categories: {
        VASU: 'Plan för småbarnspedagogik',
        LEOPS_HOJKS: 'Leops/Individuellt program',
        OTHER: 'Andra dokument'
      }
    },
    assistanceNeedsAndActions: {
      title: 'Barns stödbehov och stödåtgärder',
      description:
        'Rapporten listar antal barn i enheter och grupper enligt stödbehovs grunder och stödåtgärder. Endast mottagna platser beaktas.',
      type: 'Stödnivå',
      types: {
        DAYCARE: 'i småbarnspedagogik',
        PRESCHOOL: 'i förskoleundervisning'
      },
      placementType: 'Placeringstyp',
      level: 'Stödnivå och andra åtgärder',
      showZeroRows: 'Visa nollrader',
      groupingTypes: {
        NO_GROUPING: 'Barn',
        AREA: 'Verksamhetsenheter områdesvis',
        UNIT: 'Verksamhetsenheter'
      },
      basisMissing: 'Grund saknas',
      action: 'Stödåtgärd',
      actionMissing: 'Stödåtgärd saknas',
      assistanceNeedVoucherCoefficient: 'Förhöjd SS-koefficient',
      daycareAssistanceNeedDecisions:
        'Aktiva beslut om stöd i småbarnspedagogik',
      preschoolAssistanceNeedDecisions:
        'Aktiva beslut om stöd i förskoleundervisning'
    },
    occupancies: {
      title: 'Beläggnings- och utnyttjandegrader',
      description:
        'Rapporten erbjuder uppgifter om ett serviceområdes och en månads utnyttjande- eller beläggningsgrader.',
      filters: {
        areaPlaceholder: 'Välj serviceområde',
        unitPlaceholder: 'Välj enhet',
        type: 'Typ',
        types: {
          UNITS: {
            CONFIRMED: 'Bekräftad beläggningsgrad i enhet',
            PLANNED: 'Planerad beläggningsgrad i enhet',
            DRAFT: 'Skisserad beläggningsgrad i enhet',
            REALIZED: 'Utnyttjandegrad i enhet'
          },
          GROUPS: {
            CONFIRMED: 'Bekräftad beläggningsgrad i grupper',
            PLANNED: 'Planerad beläggningsgrad i grupper',
            DRAFT: 'Skisserad beläggningsgrad i grupper',
            REALIZED: 'Utnyttjandegrad i grupper'
          }
        },
        valueOnReport: 'Visa uppgifter',
        valuesOnReport: {
          percentage: 'I procent',
          headcount: 'Som antal',
          raw: 'Summa och antal uppfostrare separat'
        }
      },
      unitsGroupedByArea: 'Verksamhetsenheter områdesvis',
      average: 'Medelvärde',
      sumUnder3y: 'Under 3 år',
      sumOver3y: 'Över 3 år',
      sum: 'Summa',
      caretakers: 'Uppfostrare',
      missingCaretakersLegend: 'antal uppfostrare saknas'
    },
    incompleteIncomes: {
      title: 'Inkomstuppgifter saknas',
      description:
        'Rapport om föräldrar vars inkomstuppgifter har föråldrats, men barnet har ännu en aktiv placering.',
      validFrom: 'Startdatum',
      fullName: 'Namn',
      daycareName: 'Daghem',
      careareaName: 'Serviceområde'
    },
    invoices: {
      title: 'Fakturaavstämning',
      description:
        'Fakturaavstämningsrapport för jämförelse med faktureringssystemet',
      period: 'Faktureringsperiod',
      areaCode: 'Område',
      amountOfInvoices: 'Fakturor',
      totalSumCents: 'Summa',
      amountWithoutSSN: 'Utan personbeteckning',
      amountWithoutAddress: 'Utan adress',
      amountWithZeroPrice: 'Nollfakturor'
    },
    nekkuOrders: {
      title: 'Nekku beställningar',
      description: 'Rapport om realiserade Nekku beställningar',
      tooLongRange: 'Du kan hämta rapporten för högst en månads period.',
      sku: 'Produktnummer',
      quantity: 'Antal',
      mealTime: 'Måltid',
      mealType: 'Kost',
      mealTimeValues: {
        BREAKFAST: 'Frukost',
        LUNCH: 'Lunch',
        SNACK: 'Mellanmål',
        DINNER: 'Middag',
        SUPPER: 'Kvällsmål'
      },
      mealTypeValues: {
        DEFAULT: 'Blandat',
        VEGAN: 'Vegan',
        VEGETABLE: 'Vegetarisk'
      },
      specialDiets: 'Specialkoster',
      nekkuOrderInfo: 'Beställningsinfo',
      nekkuOrderTime: 'Beställningstidpunkt'
    },
    startingPlacements: {
      title: 'Barn som inleder småbarnspedagogik',
      description: 'Rapport till FPA om barn som inleder småbarnspedagogik.',
      ssn: 'Personbeteckning',
      childLastName: 'Barnets efternamn',
      childFirstName: 'Barnets förnamn',
      placementStart: 'Inleder småbarnspedagogik',
      reportFileName: 'inledande_placeringar'
    },
    voucherServiceProviders: {
      title: 'Servicesedelenheter',
      description:
        'Servicesedelsummor för servicesedelenheter samt avgifter per barn.',
      filters: {
        areaPlaceholder: 'Välj serviceområde',
        allAreas: 'Alla områden',
        unitPlaceholder: 'Sök med enhetens namn',
        separate: 'Grunddelar och förhöjningsdelar separat'
      },
      locked: 'Rapporten låst',
      childCount: 'Antal SS-barn',
      sumBeforeAssistanceNeed: 'Grunddelens summa / mån',
      assistanceNeedSum: 'Förhöjningsdelens summa / mån',
      unitVoucherSum: 'SS summa / mån',
      average: 'Medelvärde',
      breakdown: 'Specifikation'
    },
    voucherServiceProviderUnit: {
      title: 'Servicesedelbarn i enhet',
      unitPageLink: 'Enhetssida',
      month: 'Månad',
      total: 'Servicesedlarnas summa vald månad',
      child: 'Barnets namn / födelsetid',
      childFirstName: 'Förnamn',
      childLastName: 'Efternamn',
      note: 'Notering',
      numberOfDays: 'Dagar',
      start: 'Från och med',
      end: 'Till och med',
      serviceVoucherValue: 'SS högsta värde',
      serviceVoucherRealizedValueBeforeAssistanceNeed: 'Grunddel / mån',
      serviceVoucherRealizedAssistanceNeedValue: 'Förhöjningsdel / mån',
      serviceVoucherRealizedValue: 'SS värde / mån',
      serviceVoucherFinalCoPayment: 'Självrisk',
      serviceNeed: 'Tjänstebehov',
      assistanceNeed: 'Stödbehov',
      partTime: 'Halv/Hel',
      type: {
        NEW: 'Nytt beslut',
        REFUND: 'Återbetalning',
        CORRECTION: 'Korrigering'
      }
    },
    nonSsnChildren: {
      title: 'Barn utan personbeteckning',
      description:
        'Rapport om barn utan personbeteckning med placering för kontroll av OID-uppgifter',
      childName: 'Barnets namn',
      dateOfBirth: 'Födelsedag',
      personOid: 'OID för barnets uppgifter',
      lastSentToVarda: 'Senast exporterad till Varda',
      lastSentToKoski: 'Senast exporterad till Koski',
      total: 'Totalt'
    },
    placementCount: {
      title: 'Antal placeringar',
      description:
        'Rapport om antal placeringar i enheter enligt sökkriterier på angivet datum',
      noCareAreasFound: 'Inga serviceområden med placeringar',
      examinationDate: 'Granskningsdag',
      careArea: 'Serviceområde',
      daycaresByArea: 'Verksamhetsenheter områdesvis',
      placementCount: 'Barn totalt',
      calculatedPlacements: 'Beräknat antal',
      providerType: 'Arrangemangsform',
      placementType: 'Placeringstyp',
      placementsOver3: 'Minst 3 år',
      placementsUnder3: 'Under 3 år',
      total: 'Totalt'
    },
    placementGuarantee: {
      title: 'Placeringsgaranti för småbarnspedagogik',
      description:
        'Rapporten visar barn med placeringsgaranti för småbarnspedagogik'
    },
    placementSketching: {
      title: 'Skissrapport för förskoleplaceringar',
      description:
        'Rapport om inkomna förskoleansökningar för att underlätta placering',
      placementStartDate: 'Granskningsdag för nuvarande placering',
      earliestPreferredStartDate: 'Tidigast ansökt startdag',
      preferredUnit: 'Ansökningsönskemål',
      currentUnit: 'Nuvarande enhet',
      streetAddress: 'Adress',
      postalCode: 'Postnummer',
      tel: 'Telefon',
      email: 'e-post',
      dob: 'Födelsetid',
      serviceNeedOption: 'Tjänstebehov',
      assistanceNeed: 'Stödbehov',
      preparatory: 'Förberedande',
      siblingBasis: 'Syskongrund',
      connected: 'Kompletterande',
      applicationStatus: 'Ansökningsstatus',
      preferredStartDate: 'Önskad startdag',
      sentDate: 'Sändningsdag',
      otherPreferredUnits: 'Andra ansökningsönskemål',
      additionalInfo: 'Tilläggsuppgifter',
      childMovingDate: 'Barnets flyttdag',
      childCorrectedStreetAddress: 'Barnets nya gatuadress',
      childCorrectedPostalCode: 'Barnets nya postnummer',
      childCorrectedCity: 'Barnets nya postanstalt',
      applicationSentDateRange: 'Ansökan skickad mellan'
    },
    vardaChildErrors: {
      title: 'Varda-barnfel',
      ma003: {
        include: 'Inkludera MA003-fel',
        exclude: 'Dölj MA003-fel',
        only: 'Visa endast MA003-fel'
      },
      description: 'Fel som uppstått vid uppdateringar av Varda-barn',
      updated: 'Senast uppdaterad',
      age: 'Ålder (dagar)',
      child: 'Barn',
      error: 'Fel',
      updateChild: 'Återexportera'
    },
    vardaUnitErrors: {
      title: 'Varda-enhetsfel',
      description: 'Fel som uppstått vid uppdateringar av Varda-enheter',
      age: 'Felets ålder (dagar)',
      unit: 'Enhet',
      error: 'Fel'
    },
    titaniaErrors: {
      title: 'Titania-fel',
      description: 'Fel som hittats i scheman som importerats från Titania',
      header: 'Titania-export',
      date: 'Datum',
      shift1: 'Första skiftet',
      shift2: 'Överlappande skift'
    },
    sextet: {
      title: 'Sextetjämförelse',
      description:
        'Rapport om årets realiserade närvarodagar enhetsvis och placer ingstypsvis',
      placementType: 'Placeringstyp',
      year: 'År',
      unitName: 'Enhet',
      attendanceDays: 'Verkliga närvarodagar'
    },
    invoiceGeneratorDiff: {
      title: 'Fakturageneratorers skillnader',
      description:
        'Verktyg för att analysera ny fakturagenerator kontra gammal fakturagenerator',
      report: 'Rapport om fakturageneratorers skillnader'
    },
    futurePreschoolers: {
      title: 'Framtida förskolebarn',
      description:
        'Rapport om nästa års förskolebarn och enheter för automatisk placeringsverktyg',
      futurePreschoolersCount: (count: number) =>
        count === 1
          ? `${count} framtida förskolebarn`
          : `${count} framtida förskolebarn`,
      preschoolUnitCount: (count: number) =>
        count === 1
          ? `${count} enhet som erbjuder förskoleundervisning`
          : `${count} enheter som erbjuder förskoleundervisning`,
      sourceUnitCount: (count: number) =>
        count === 1
          ? `${count} nuvarande enhet för framtida förskolebarn`
          : `${count} nuvarande enheter för framtida förskolebarn`
    },
    meals: {
      title: 'Matgästantal',
      description: 'Räknar matgästantal baserat på reservationer enhetsvis.',
      wholeWeekLabel: 'Hela veckan',
      jamixSend: {
        button: 'Skicka om till Jamix',
        confirmationTitle: 'Skicka matbeställningar om till Jamix?'
      },
      mealName: {
        BREAKFAST: 'Frukost',
        LUNCH: 'Lunch',
        LUNCH_PRESCHOOL: 'Lunch (förskola)',
        SNACK: 'Mellanmål',
        SUPPER: 'Middag',
        EVENING_SNACK: 'Kvällsmål'
      },
      headings: {
        mealName: 'Måltid',
        mealId: 'Måltidsidentifierare',
        mealCount: 'antal st',
        dietId: 'Specialkostidentifierare',
        dietAbbreviation: 'Specialkostförkortning',
        mealTextureId: 'Matstrukturidentifierare',
        mealTextureName: 'Matstruktur',
        additionalInfo: 'Tilläggsuppgift'
      }
    },
    preschoolAbsences: {
      title: 'Frånvarorapport för förskoleundervisning',
      description:
        'Rapporten listar barnvisa frånvaroantal för förskoleperioden för vald enhet och grupp',
      firstName: 'Förnamn',
      lastName: 'Efternamn',
      daycareName: 'Enhet',
      groupName: 'Grupp',
      hours: '(timmar)',
      total: 'Totalt',
      filters: {
        areaSelection: {
          label: 'Område:',
          placeHolder: 'Välj område'
        },
        daycareSelection: {
          label: 'Förskoleanstalt:',
          placeholder: 'Välj enhet'
        },
        groupSelection: {
          label: 'Grupp:',
          placeholder: 'Välj grupp'
        },
        preschoolTerm: {
          label: 'Förskoleperiod:',
          placeholder: 'Välj förskoleperiod'
        },
        includeClosed: 'Visa avslutade enheter och grupper'
      }
    },
    preschoolApplications: {
      title: 'Rådgivande FU-rapport',
      description:
        'Rapporten visar ansökningar som hör till rådgivande förskoleplatsbeslutprocess',
      columns: {
        applicationUnitName: 'Enhet',
        childLastName: 'Efternamn',
        childFirstName: 'Förnamn',
        childDateOfBirth: 'Födelsetid',
        childStreetAddress: 'Postadress',
        childPostalCode: 'Post\u00ADnr',
        childPostalCodeFull: 'Postnummer',
        currentUnitName: 'Nuvarande enhet',
        isDaycareAssistanceNeed: 'Stödbehov'
      }
    },
    holidayPeriodAttendance: {
      title: 'Semesterenkätrapport',
      description:
        'Enhetens dagsvis uppföljning av närvaro under semesterenkät',
      periodFilter: 'Semesterenkät',
      periodFilterPlaceholder: 'Välj semesterenkät',
      unitFilter: 'Enhet',
      groupFilter: 'Gruppval',
      groupFilterPlaceholder: 'Hela enheten',
      fetchButton: 'Hämta',
      dateColumn: 'Dag',
      presentColumn: 'Närvarande',
      assistanceColumn: 'Närvarande med stödåtgärder',
      occupancyColumn: 'Närvarande totalt (koefficient)',
      occupancyColumnInfo:
        'Koefficienten räknar alla närvarande barns koefficient totalt. Koefficienten påverkas till exempel av barnets ålder och stödbehov.',
      staffColumn: 'Personalbehov',
      absentColumn: 'Frånvarande',
      noResponseColumn: 'Svarade inte',
      moreText: 'mer'
    },
    holidayQuestionnaire: {
      title: 'Frånvaroenkätrapport',
      description:
        'Enhetens dagsvis uppföljning av närvaro under frånvaroenkät',
      questionnaireFilter: 'Frånvaroenkät',
      questionnaireFilterPlaceholder: 'Välj frånvaroenkät',
      unitFilter: 'Enhet',
      groupFilter: 'Gruppval',
      groupFilterPlaceholder: 'Hela enheten',
      fetchButton: 'Hämta',
      dateColumn: 'Dag',
      presentColumn: 'Närvarande',
      assistanceColumn: 'Närvarande med stödåtgärder',
      occupancyColumn: 'Närvarande totalt (koefficient)',
      occupancyColumnInfo:
        'Koefficienten räknar alla närvarande barns koefficient totalt. Koefficienten påverkas till exempel av barnets ålder och stödbehov.',
      staffColumn: 'Personalbehov',
      absentColumn: 'Frånvarande',
      noResponseColumn: 'Svarade inte',
      moreText: 'mer'
    },
    tampereRegionalSurvey: {
      title: 'Tammerfors regionundersökning',
      description:
        'Rapporten samlar kommunens årliga regionundersökning uppgifter nedladdningsbara som CSV-filer',
      monthlyReport: 'Regionundersökningens månatliga antal',
      ageStatisticsReport: 'Regionundersökningens åldersfördelningar',
      yearlyStatisticsReport: 'Regionundersökningens årliga antal',
      municipalVoucherReport:
        'Regionundersökningens servicesedlarnas antal kommunvis',
      reportLabel: 'Regionundersökning',
      monthlyColumns: {
        month: 'Månad',
        municipalOver3FullTimeCount:
          'Antal barn 3 år och över i heldags småbarnspedagogik',
        municipalOver3PartTimeCount:
          'Antal barn 3 år och över i halvdags småbarnspedagogik',
        municipalUnder3FullTimeCount:
          'Antal barn under 3 år i heldags småbarnspedagogik',
        municipalUnder3PartTimeCount:
          'Antal barn under 3 år i halvdags småbarnspedagogik',
        familyUnder3Count: 'Antal barn under 3 år i familjedagvård',
        familyOver3Count: 'Antal barn 3 år och över i familjedagvård',
        municipalShiftCareCount: 'Antal i skiftvård',
        assistanceCount:
          'Barn med intensifierat och särskilt stöd / Barn som behöver särskilt eller tillväxt- och lärandestöd',
        statDay: '(läget sista dagen i månaden)'
      },
      ageStatisticColumns: {
        voucherUnder3Count: 'Antal servicesedelplatser under 3 år',
        voucherOver3Count: 'Antal servicesedelplatser 3 år och över',
        purchasedUnder3Count: 'Antal köpta tjänsteplatser under 3 år',
        purchasedOver3Count: 'Antal köpta tjänsteplatser 3 år och över',
        clubUnder3Count: 'Antal klubbplatser under 3 år',
        clubOver3Count: 'Antal klubbplatser 3 år och över',
        nonNativeLanguageUnder3Count: 'Antal med främmande språk under 3 år',
        nonNativeLanguageOver3Count: 'Antal med främmande språk 3 år och över',
        effectiveCareDaysUnder3Count:
          'Småbarnspedagogikens vårddagar under 3 år',
        effectiveCareDaysOver3Count:
          'Småbarnspedagogikens vårddagar 3 år och över',
        effectiveFamilyDaycareDaysUnder3Count:
          'Familjedagvårdens vårddagar under 3 år',
        effectiveFamilyDaycareDaysOver3Count:
          'Familjedagvårdens vårddagar 3 år och över',
        languageStatDay: '(läget 30.11.)'
      },
      yearlyStatisticsColumns: {
        voucherTotalCount: 'Antal servicesedlar',
        voucherAssistanceCount: 'Antal stödbarn i servicesedelenheter',
        voucher5YearOldCount: '5-åringar i servicesedelenheter',
        purchased5YearlOldCount: '5-åringar i köpta tjänsteenheter',
        municipal5YearOldCount: '5-åringar i kommunala enheter',
        familyCare5YearOldCount: '5-åringar i familjedagvård',
        club5YearOldCount: '5-åringar i klubb',
        preschoolDaycareUnitCareCount:
          'Barn i kompletterande småbarnspedagogik i småbarnspedagogikenheter',
        preschoolDaycareSchoolCareCount:
          'Barn i kompletterande småbarnspedagogik i skolor',
        preschoolDaycareFamilyCareCount:
          'Barn i kompletterande småbarnspedagogik i familjedagvård',
        preschoolDaycareUnitShiftCareCount:
          'Barn i kompletterande småbarnspedagogik i skiftvård i småbarnspedagogikenheter',
        preschoolDaycareSchoolShiftCareCount:
          'Barn i kompletterande småbarnspedagogik i skiftvård i skolor',
        voucherGeneralAssistanceCount: 'Antal allmänt stöd (servicesedel)',
        voucherSpecialAssistanceCount: 'Antal särskilt stöd (servicesedel)',
        voucherEnhancedAssistanceCount:
          'Antal intensifierat stöd (servicesedel)',
        municipalGeneralAssistanceCount: 'Antal allmänt stöd (kommunal)',
        municipalSpecialAssistanceCount: 'Antal särskilt stöd (kommunal)',
        municipalEnhancedAssistanceCount: 'Antal intensifierat stöd (kommunal)',
        statDay: '(läget 15.12.)'
      },
      municipalVoucherColumns: {
        statDay: '(läget 15.12.)',
        municipality: 'Belägenhetskommunen',
        under3VoucherCount: 'Servicesedlar under 3 år',
        over3VoucherCount: 'Servicesedlar 3 år och över'
      }
    },
    citizenDocumentResponseReport: {
      title: 'Medborgarens dokument',
      description:
        'Rapporten listar gruppvis medborgarnas dokuments senaste svar på ja/nej- eller flervalsfrågor',
      filters: {
        unit: 'Enhet',
        group: 'Grupp',
        template: 'Dokument',
        showBackupChildren: 'Visa även reservplacerade'
      },
      headers: {
        name: 'Namn',
        answeredAt: 'Besvarad'
      },
      noSentDocument: 'Inget skickat dokument',
      noAnswer: 'Inte besvarad'
    }
  },
  unitEditor: {
    submitNew: 'Skapa enhet',
    title: {
      contact: 'Enhetens kontaktuppgifter',
      unitManager: 'Småbarnspedagogiska enhetens ledares kontaktuppgifter',
      preschoolManager: 'Förskoleundervisningens ledares kontaktuppgifter',
      decisionCustomization:
        'Enhetens namn i beslut och meddelande om mottagande av plats',
      mealOrderIntegration: 'Matbeställningsintegration',
      mealtime: 'Enhetens måltidstider'
    },
    label: {
      name: 'Enhetens namn',
      openingDate: 'Enhetens startdag',
      closingDate: 'slutdag',
      area: 'Område',
      careTypes: 'Verksamhetsformer',
      dailyPreschoolTime: 'Undervisningstid',
      dailyPreparatoryTime: 'Undervisningstid',
      canApply: 'Visa enhet',
      providerType: 'Arrangemangsform',
      operationDays: 'Verksamhetsdagar',
      shiftCareOperationDays: 'Skiftvårdens verksamhetsdagar',
      operationDay: {
        0: 'SÖ',
        1: 'MÅ',
        2: 'TI',
        3: 'ON',
        4: 'TO',
        5: 'FR',
        6: 'LÖ',
        7: 'SÖ'
      },
      shiftCare: 'Kvälls- och skiftvård',
      capacity: 'Enhetens beräknade barnantal',
      language: 'Enhetens språk',
      withSchool: 'I anslutning till skola',
      ghostUnit: 'Spökenhet',
      integrations: 'Integrationer',
      invoicedByMunicipality: 'Faktureras från eVaka',
      ophUnitOid: 'Verksamhetsställets OID',
      ophOrganizerOid: 'Anordnarens OID',
      costCenter: 'Kostnadsställe',
      dwCostCenter: 'DW Kostnadsställe',
      financeDecisionHandler: 'Handläggare av ekonomibeslut',
      additionalInfo: 'Tilläggsuppgifter om enheten',
      phone: 'Enhetens telefonnummer',
      email: 'Enhetens e-postadress',
      url: 'Enhetens URL-adress',
      visitingAddress: 'Besöksadress',
      location: 'Kartkoordinater',
      mailingAddress: 'Postadress',
      unitManager: {
        name: 'Ledarens namn',
        phone: 'Ledarens telefonnummer',
        email: 'Ledarens e-postadress'
      },
      preschoolManager: {
        name: 'Förskoleundervisningens ledares namn',
        phone: 'Förskoleundervisningens ledares telefonnummer',
        email: 'Förskoleundervisningens ledares e-postadress'
      },
      decisionCustomization: {
        daycareName: 'Enhetens namn i småbarnspedagogikbeslut',
        preschoolName: 'Enhetens namn i förskoleundervisningsbeslut',
        handler: 'Mottagare av vårdnadshavarens meddelande',
        handlerAddress: 'Meddelandemottagarens adress'
      },
      businessId: 'FO-nummer',
      iban: 'Kontonummer',
      providerId: 'Leverantörsnummer',
      partnerCode: 'Partnerkod',
      mealTime: {
        breakfast: 'Frukost',
        lunch: 'Lunch',
        snack: 'Mellanmål',
        supper: 'Middag',
        eveningSnack: 'Kvällsmål'
      },
      nekkuMealReduction: 'Nekku-reduktionsprocent',
      nekkuNoWeekendMealOrders: 'Inga Nekku-beställningar på helger'
    },
    info: {
      varda: 'Används i Varda-integration',
      koski: 'Används i Koski-integration'
    },
    field: {
      applyPeriod: 'När önskad startdag inom tidsperiod',
      canApplyDaycare: 'I småbarnspedagogikansökan',
      canApplyPreschool: 'I förskoleundervisningsansökan',
      canApplyClub: 'I klubbansökan',
      providesShiftCare: 'Enheten erbjuder kvälls- och skiftvård',
      shiftCareOpenOnHolidays: 'Skiftvård är öppen även på helgdagar',
      capacity: 'personer',
      withSchool: 'Enheten är belägen i anslutning till skola',
      ghostUnit: 'Enheten är en spökenhet',
      uploadToVarda: 'Enhetens uppgifter skickas till Varda',
      uploadChildrenToVarda: 'Enhetens barns uppgifter skickas till Varda',
      uploadToKoski: 'Skickas till Koski-tjänsten',
      invoicedByMunicipality: 'Faktureras från eVaka',
      invoicingByEvaka: 'Enhetens fakturering sker från eVaka',
      decisionCustomization: {
        handler: [
          'Servicehandledning',
          'Småbarnspedagogiska enhetens ledare',
          'Ledare inom småbarnspedagogik',
          'Svenska bildningstjänster / Småbarnspedagogik'
        ]
      },
      nekkuNoWeekendMealOrders: 'Nekku-beställningar görs inte för helger'
    },
    placeholder: {
      name: 'Ge enheten ett namn',
      area: 'Välj område',
      financeDecisionHandler: 'Välj anställd',
      daycareType: 'Välj typ',
      costCenter: '(obligatorisk uppgift vid fakturering från eVaka)',
      dwCostCenter: 'Kostnadsställesuppgift för DW',
      additionalInfo:
        'Du kan skriva tilläggsuppgifter om enheten (syns inte för medborgaren)',
      phone: 't.ex. +358 40 555 5555',
      email: 'fornamn.efternamn@esbo.fi',
      url: 't.ex. https://www.esbo.fi/sv/verksamhetsstallen/15585',
      streetAddress: 'Gatunamn t.ex. Björk-Mankans väg 22 B 24',
      postalCode: 'Postnummer',
      postOffice: 'Postanstalt',
      location: 't.ex. 60.223038, 24.692637',
      manager: {
        name: 'Förnamn Efternamn'
      },
      decisionCustomization: {
        name: 't.ex. Morgonrodnans daghem'
      }
    },
    error: {
      name: 'Namn saknas',
      area: 'Område saknas',
      careType: 'Verksamhetsform saknas',
      dailyPreschoolTime: 'Förskoletid saknas eller är felaktig',
      dailyPreparatoryTime:
        'Förberedande undervisningstid saknas eller är felaktig',
      daycareType: 'Småbarnspedagogiktyp saknas',
      capacity: 'Kapaciteten är felaktig',
      costCenter: 'Kostnadsställe saknas',
      reservedOphUnitOid: 'Enhetens OPH OID används redan i en annan enhet',
      url: 'URL-adressen måste ha https://- eller http://-prefix',
      visitingAddress: {
        streetAddress: 'Besöksadressens gatuadress saknas',
        postalCode: 'Besöksadressens postnummer saknas',
        postOffice: 'Besöksadressens postanstalt saknas'
      },
      location: 'Kartkoordinaterna är felaktiga',
      unitManager: {
        name: 'Ledarens namn saknas',
        phone: 'Ledarens telefonnummer saknas',
        email: 'Ledarens e-post saknas'
      },
      cannotApplyToDifferentType: 'Ansökningstyp och serviceform stämmer inte',
      financeDecisionHandler: 'Handläggare av ekonomibeslut saknas',
      ophUnitOid: 'Enhetens OID saknas',
      ophOrganizerOid: 'Anordnarens OID saknas',
      openingDateIsAfterClosingDate: 'Startdagen är efter slutdagen',
      businessId: 'FO-nummer saknas',
      iban: 'Kontonummer saknas',
      providerId: 'Leverantörsnummer saknas',
      operationTimes: 'Felaktig anteckning i enhetens verksamhetstider',
      shiftCareOperationTimes:
        'Felaktig anteckning i enhetens skiftvårds verksamhetstider',
      mealTimes: 'Felaktig anteckning i enhetens måltidstider',
      closingDateBeforeLastPlacementDate: (lastPlacementDate: LocalDate) =>
        `Enheten har placeringar fram till ${lastPlacementDate.format()}. Alla placeringar och reservplaceringar ska avslutas senast enhetens slutdag, inklusive eventuella framtida placeringar.`
    },
    warning: {
      onlyMunicipalUnitsShouldBeSentToVarda:
        'Skicka inte till Varda andra än kommunala och kommunala köpta tjänsteenheters uppgifter.',
      handlerAddressIsMandatory:
        'Meddelandemottagarens adress är obligatorisk om enhetens arrangemangsform har valts som kommunal, köpt tjänst eller servicesedel.'
    },
    closingDateModal: 'Ange slutdag'
  },
  fileUpload: {
    download: {
      modalHeader: 'Filbehandling pågår',
      modalMessage: 'Filen kan inte öppnas just nu. Försök igen om en stund.',
      modalClose: 'Stäng'
    }
  },
  messages: {
    inboxTitle: 'Meddelanden',
    emptyInbox: 'Den här mappen är tom',
    replyToThread: 'Svara på meddelande',
    archiveThread: 'Arkivera meddelandetråd',
    markUnread: 'Markera som oläst',
    changeFolder: {
      button: 'Byt mapp',
      modalTitle: 'Välj mapp',
      modalOk: 'Flytta till mapp'
    },
    unitList: {
      title: 'Enheter'
    },
    sidePanel: {
      municipalMessages: 'Kommunens meddelanden',
      serviceWorkerMessages: 'Servicehandledningens meddelanden',
      serviceWorkerFolders: 'Servicehandledningens mappar',
      financeMessages: 'Ekonomiförvaltningens meddelanden',
      financeFolders: 'Ekonomiförvaltningens mappar',
      ownMessages: 'Egna meddelanden',
      groupsMessages: 'Gruppernas meddelanden',
      noAccountAccess:
        'Meddelanden kan inte visas eftersom du inte har behörighet till gruppen. Be om tillstånd från din chef.'
    },
    messageBoxes: {
      names: {
        received: 'Mottagna',
        sent: 'Skickade',
        drafts: 'Utkast',
        copies: 'Ledarens/kommunens meddelanden',
        archive: 'Arkiv',
        thread: 'Meddelandetråd'
      },
      receivers: 'Mottagare',
      newMessage: 'Nytt meddelande'
    },
    messageList: {
      titles: {
        received: 'Mottagna meddelanden',
        sent: 'Skickade meddelanden',
        drafts: 'Utkast',
        copies: 'Ledarens/kommunens meddelanden',
        archive: 'Arkiv',
        thread: 'Meddelandetråd'
      }
    },
    types: {
      MESSAGE: 'Meddelande',
      BULLETIN: 'Bulletin'
    },
    recipientSelection: {
      title: 'Mottagare',
      childName: 'Namn',
      childDob: 'Födelsetid',
      receivers: 'Mottagare',
      confirmText: 'Skicka meddelande till valda',
      starters: 'barn som börjar'
    },
    noTitle: 'Ingen rubrik',
    notSent: 'Inte skickat',
    editDraft: 'Redigera utkast',
    undo: {
      info: 'Meddelande skickat',
      secondsLeft: (s: number) =>
        s === 1 ? '1 sekund tid' : `${s} sekunder tid`
    },
    sensitive: 'känsligt',
    customer: 'Kund',
    applicationTypes: {
      PRESCHOOL: 'Förskoleundervisningsansökan',
      DAYCARE: 'Småbarnspedagogikansökan',
      CLUB: 'Klubbansökan'
    },
    application: 'Ansökan',
    showApplication: 'Visa ansökan',
    messageEditor: {
      message: 'Meddelande',
      newMessage: 'Nytt meddelande',
      to: {
        label: 'Mottagare',
        placeholder: 'Välj grupp',
        noOptions: 'Inga grupper'
      },
      recipients: 'Mottagare',
      recipientCount: 'Mottagare',
      manyRecipientsWarning: {
        title: 'Meddelandet har ett stort antal mottagare.',
        text: (count: number) =>
          `Detta meddelande är på väg till ${count} mottagare. Är du säker på att du vill skicka meddelandet?`
      },
      type: {
        label: 'Meddelandetyp',
        message: 'Meddelande',
        bulletin: 'Bulletin (kan inte besvaras)'
      },
      flags: {
        heading: 'Meddelandets tilläggsmarkeringar',
        urgent: {
          info: 'Skicka meddelande som brådskande endast om du vill att vårdnadshavaren läser det under arbetsdagen.',
          label: 'Brådskande'
        },
        sensitive: {
          info: 'Öppning av känsligt meddelande kräver stark autentisering från medborgaren.',
          label: 'Känsligt',
          whyDisabled:
            'Känsligt meddelande kan skickas endast från personligt användarkonto till en enskild barnets vårdnadshavare.'
        }
      },
      sender: 'Avsändare',
      selectPlaceholder: 'Välj...',
      filters: {
        showFilters: 'Visa tilläggsval',
        hideFilters: 'Dölj tilläggsval',
        yearOfBirth: 'Födelseår',
        placementType: 'Placeringstyp',
        shiftCare: {
          heading: 'Skiftvård',
          label: 'Skiftvård',
          intermittent: 'Tillfällig skiftvård'
        },
        familyDaycare: {
          heading: 'Familjedagvård',
          label: 'Familjedagvård'
        }
      },
      title: 'Rubrik',
      setFolder: 'Flytta till mapp',
      deleteDraft: 'Kassera utkast',
      send: 'Skicka',
      sending: 'Skickar'
    }
  },
  pinCode: {
    title: 'eVaka-mobilens PIN-kod',
    title2: 'Ange PIN-kod',
    text1:
      'På den här sidan kan du ange din egen personliga PIN-kod för eVaka-mobilen. PIN-koden används i eVaka-mobilen för att visa uppgifter som finns bakom låset',
    text2: '.',
    text3: 'Obs!',
    text4:
      'Ge inte din PIN-kod till någon annan person. Vid behov kan du byta PIN-kod när som helst.',
    text5:
      'PIN-koden ska innehålla fyra (4) siffror. De vanligaste sifferkombinationerna (t.ex. 1234) godkänns inte.',
    pinCode: 'PIN-kod',
    button: 'Spara PIN-kod',
    placeholder: '4 siffror',
    error: 'För enkel PIN-kod eller PIN-koden innehåller bokstäver',
    locked: 'PIN-koden är låst, byt den till en ny',
    lockedLong:
      'PIN-koden har angetts fel 5 gånger i eVaka-mobilen och koden är låst. Vänligen byt till en ny PIN-kod.',
    link: 'eVaka-mobilens PIN-kod',
    unsavedDataWarning: 'Du har inte sparat PIN-koden'
  },
  employees: {
    name: 'Namn',
    email: 'E-post',
    rights: 'Rättigheter',
    lastLogin: 'Senast inloggad',
    employeeNumber: 'Personalnummer',
    temporary: 'Tillfällig vikarie',
    findByName: 'Sök med namn',
    activate: 'Aktivera',
    activateConfirm: 'Vill du återställa användaren till aktiv?',
    deactivate: 'Deaktivera',
    deactivateConfirm: 'Vill du deaktivera användaren?',
    deleteConfirm: 'Vill du ta bort användaren?',
    hideDeactivated: 'Visa endast aktiva användare',
    editor: {
      globalRoles: 'Systemroller',
      unitRoles: {
        name: 'Enhetsroller',
        title: 'Behörigheter',
        scheduledRolesTitle: 'Kommande behörigheter',
        unit: 'Enhet',
        role: 'Roll i enhet',
        startDate: 'Behörighet börjar',
        endDate: 'Behörighet slutar',
        deleteConfirm: 'Vill du ta bort användarens behörighet?',
        deleteAll: 'Ta bort alla behörigheter',
        deleteAllConfirm: 'Vill du ta bort användarens alla behörigheter?',
        addRoles: 'Lägg till behörigheter',
        addRolesModalTitle: 'Ny behörighet',
        units: 'Enheter',
        warnings: {
          hasCurrent: 'Personen har redan behörigheter i följande enheter',
          hasScheduled:
            'Personen har redan kommande behörigheter i följande enheter',
          currentEnding: (date: LocalDate) =>
            `Överlappande behörigheter ersätts från och med ${date.format()}.`,
          currentRemoved: 'Dessa behörigheter tas bort.',
          scheduledRemoved: 'Dessa kommande behörigheter tas bort.'
        }
      },
      mobile: {
        title: 'Personliga mobilenheter',
        name: 'Enhetens namn',
        nameless: 'Namnlös enhet',
        deleteConfirm: 'Vill du ta bort användarens mobilenhetsparning?'
      }
    },
    createNewSsnEmployee: 'Skapa ny användare med personbeteckning',
    newSsnEmployeeModal: {
      title: 'Lägg till ny användare med personbeteckning',
      createButton: 'Skapa konto',
      ssnConflict: 'Personbeteckning används redan'
    },
    hasSsn: 'Användare med personbeteckning'
  },
  financeBasics: {
    fees: {
      title: 'Kundavgifter',
      add: 'Skapa nya kundavgifter',
      thresholds: 'Inkomstgränser',
      validDuring: 'Kundavgifter för period',
      familySize: 'Familjens storlek',
      minThreshold: 'Minimibruttoinkomst €/mån',
      maxThreshold: 'Bruttoinkomstgräns för högsta avgift €/mån',
      maxFeeError: 'Maximiavgiften stämmer inte',
      thresholdIncrease: 'Inkomstgräns höjningssumma, när familjestorlek > 6',
      thresholdIncreaseInfo:
        'Om familjens storlek är större än 6, höjs inkomstgränsen som ligger till grund för avgiftsbestämningen med höjningssumman för varje följande minderårigt barn i familjen.',
      multiplier: 'Avgift %',
      maxFee: 'Maximiavgift',
      minFee: 'Lägsta avgift per barn',
      siblingDiscounts: 'Syskonrabatter',
      siblingDiscount2: 'Rabatt% 1:a syskon',
      siblingDiscount2Plus: 'Rabatt% andra syskon',
      temporaryFees: 'Avgifter för tillfällig småbarnspedagogik',
      temporaryFee: 'Grundpris',
      temporaryFeePartDay: 'Halvdags',
      temporaryFeeSibling: 'Grundpris, andra barn',
      temporaryFeeSiblingPartDay: 'Halvdags, andra barn',
      errors: {
        'date-overlap':
          'Avgiftsinställningar överlappar med en annan giltig inställning. Uppdatera giltighetstiden för andra avgiftsinställningar först.'
      },
      modals: {
        editRetroactive: {
          title: 'Vill du verkligen redigera uppgifter?',
          text: 'Vill du verkligen redigera redan använda avgiftsuppgifter? Om du redigerar uppgifter, skapas ett retroaktivt avgifts- eller värdebeslut för alla kunder som ändringen gäller.',
          resolve: 'Redigera',
          reject: 'Redigera inte'
        },
        saveRetroactive: {
          title: 'Vill du spara avgiftsinställningar retroaktivt?',
          text: 'Du håller på att spara avgiftsinställningar som gäller retroaktivt. Om du sparar uppgifter, skapas ett nytt retroaktivt avgifts- eller värdebeslut för alla kunder som ändringen påverkar.',
          resolve: 'Spara',
          reject: 'Avbryt'
        }
      }
    },
    serviceNeeds: {
      title: 'Tjänstebehov',
      add: 'Lägg till nytt servicesedelvärde',
      voucherValues: 'Servicesedlarnas värden',
      validity: 'Giltighetstid',
      baseValue: (
        <>
          Grundvärde,
          <br />3 år eller över (€)
        </>
      ),
      coefficient: (
        <>
          Koefficient,
          <br />3 år eller över
        </>
      ),
      value: (
        <>
          Maxvärde,
          <br />3 år eller över (€)
        </>
      ),
      baseValueUnder3y: (
        <>
          Grundvärde,
          <br />
          under 3 år
        </>
      ),
      coefficientUnder3y: (
        <>
          Koefficient,
          <br />
          under 3 år
        </>
      ),
      valueUnder3y: (
        <>
          Maxvärde,
          <br />
          under 3 år (€)
        </>
      ),
      errors: {
        'date-overlap':
          'Giltighetstid kan inte börja före en annan servicesedels startdag',
        'end-date-overlap':
          'Giltighetstid kan inte börja före dagen efter föregående servicesedels slutdag',
        'date-gap': 'Det kan inte finnas luckor mellan giltighetstider',
        shouldNotHappen: 'Oväntat fel'
      },
      modals: {
        deleteVoucherValue: {
          title: 'Vill du verkligen ta bort servicesedelvärdet?'
        }
      }
    }
  },
  documentTemplates: {
    title: 'Dokumentmallar',
    documentTypes: {
      PEDAGOGICAL_REPORT: 'Pedagogisk redogörelse',
      PEDAGOGICAL_ASSESSMENT: 'Pedagogisk bedömning',
      HOJKS: 'Individuellt program',
      MIGRATED_VASU: 'Plan för småbarnspedagogik (överförd)',
      MIGRATED_LEOPS: 'Plan för förskoleundervisning (överförd)',
      MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION:
        'Beslut om stöd i småbarnspedagogik (överförd)',
      MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION:
        'Beslut om stöd i förskoleundervisning (överförd)',
      VASU: 'Plan för småbarnspedagogik',
      LEOPS: 'Plan för förskoleundervisning',
      CITIZEN_BASIC: 'Dokument att fylla i tillsammans med vårdnadshavare',
      OTHER_DECISION: 'Beslutsdokument',
      OTHER: 'Annat barndokument'
    },
    documentTypeInfos: {
      CITIZEN_BASIC:
        'Detta är ett dokument som både medborgaren och personalen kan fylla i. Om personalen vill kan de svara på frågorna först, varefter dokumentet kan skickas till medborgaren för ifyllning i eVaka.',
      OTHER_DECISION:
        'Med detta skapas alla beslutsmallar förutom beslut relaterade till ansökningar',
      OTHER: 'Personalens ifyllda barnets pedagogiska dokument eller plan'
    },
    languages: {
      FI: 'Finskspråkig',
      SV: 'Svenskspråkig',
      EN: 'Engelskspråkig'
    },
    templatesPage: {
      add: 'Skapa ny',
      name: 'Namn',
      type: 'Typ',
      language: 'Språk',
      validity: 'Giltig',
      documentCount: 'Dokument',
      status: 'Status',
      published: 'Publicerad',
      draft: 'Utkast',
      export: 'Exportera till fil',
      import: 'Importera från fil',
      filters: {
        validity: 'Giltighet',
        active: 'I bruk',
        draft: 'Utkast',
        future: 'Kommande i bruk',
        past: 'Avslutade',
        type: 'Dokumenttyp',
        all: 'Alla',
        language: 'Språk'
      }
    },
    templateModal: {
      title: 'Ny dokumentmall',
      name: 'Namn',
      type: 'Dokumenttyp',
      placementTypes: 'I bruk med placeringar',
      language: 'Dokumentets språk',
      confidential: 'Dokumentet är sekretessbelagt',
      confidentialityDuration: 'Sekretessperiod (år)',
      confidentialityBasis: 'Sekretessgrund (metadata och arkivering)',
      legalBasis: 'Sekretessgrund / lagrefere ns (syns på formuläret)',
      validity: 'Giltig under period',
      processDefinitionNumber: 'Uppgiftsklass',
      processDefinitionNumberInfo:
        'Uppgiftsklassnummer definierat i informationsstyrningsplan. Lämna tomt om dokumentet inte arkiveras.',
      archiveDurationMonths: 'Arkiveringstid (månader)',
      archiveExternally: 'Ska överföras till externt arkiv före radering',
      endDecisionWhenUnitChanges: 'Beslut bryts om barnet byter enhet'
    },
    templateEditor: {
      confidential: 'Sekretessbelagt',
      addSection: 'Nytt avsnitt',
      titleNewSection: 'Nytt avsnitt',
      titleEditSection: 'Redigera avsnitt',
      sectionName: 'Rubrik',
      infoText: 'Instruktionstext',
      addQuestion: 'Ny fråga',
      titleNewQuestion: 'Ny fråga',
      titleEditQuestion: 'Redigera fråga',
      moveUp: 'Flytta upp',
      moveDown: 'Flytta ner',
      readyToPublish: 'Redo att publiceras',
      forceUnpublish: {
        button: 'Avbryt publicering',
        confirmationTitle: 'Vill du verkligen avbryta publiceringen?',
        confirmationText:
          'Alla dokument som använder denna dokumentmall kommer att tas bort.'
      }
    },
    questionTypes: {
      TEXT: 'Textfält',
      CHECKBOX: 'Kryssruta',
      CHECKBOX_GROUP: 'Flerval',
      RADIO_BUTTON_GROUP: 'Flerval (välj ett)',
      STATIC_TEXT_DISPLAY: 'Textstycke utan fråga',
      DATE: 'Datum',
      GROUPED_TEXT_FIELDS: 'Namngivna textfält'
    },
    ...components.documentTemplates
  },
  settings: {
    key: 'Inställning',
    value: 'Värde',
    options: {
      DECISION_MAKER_NAME: {
        title: 'Beslutsfattarens namn',
        description:
          'Namnet som kommer på småbarnspedagogik- och servicesedelbeslut'
      },
      DECISION_MAKER_TITLE: {
        title: 'Beslutsfattarens titel',
        description:
          'Titeln som kommer på småbarnspedagogik- och servicesedelbeslut'
      }
    }
  },
  unitFeatures: {
    page: {
      title: 'Funktioner öppnade för enheter',
      unit: 'Enhet',
      selectAll: 'Välj alla',
      unselectAll: 'Ta bort alla',
      providerType: 'Enhetens verksamhetsform',
      careType: 'Enhetens vårdform',
      undo: 'Ångra föregående ändring'
    },
    pilotFeatures: {
      MESSAGING: 'Meddelanden',
      MOBILE: 'Mobil',
      RESERVATIONS: 'Medborgarens kalender',
      VASU_AND_PEDADOC: 'Pedagogiska dokument och pedagogisk dokumentation',
      MOBILE_MESSAGING: 'Mobilmeddelanden',
      PLACEMENT_TERMINATION: 'Uppsägning av plats',
      REALTIME_STAFF_ATTENDANCE: 'Personalens realtidsnärvaro',
      PUSH_NOTIFICATIONS: 'Mobilnotifieringar',
      SERVICE_APPLICATIONS: 'Ansökningar om ändring av tjänstebehov',
      STAFF_ATTENDANCE_INTEGRATION: 'Integration för arbetsturplanering',
      OTHER_DECISION: 'Andra beslut',
      CITIZEN_BASIC_DOCUMENT: 'Dokument att fyllas i av vårdnadshavare'
    }
  },
  roles: {
    adRoles: {
      ADMIN: 'Huvudanvändare',
      DIRECTOR: 'Förvaltning',
      MESSAGING: 'Meddelanden',
      REPORT_VIEWER: 'Rapportering',
      FINANCE_ADMIN: 'Ekonomi',
      FINANCE_STAFF: 'Ekonomianställd (extern)',
      SERVICE_WORKER: 'Servicehandledning',
      SPECIAL_EDUCATION_TEACHER: 'Speciallärare',
      EARLY_CHILDHOOD_EDUCATION_SECRETARY: 'Småbarnspedagogiksekreterare',
      STAFF: 'Personal',
      UNIT_SUPERVISOR: 'Ledare'
    }
  },
  welcomePage: {
    text: 'Du har loggat in på Esbo stads eVaka-tjänst. Ditt användarkonto har ännu inte tilldelats rättigheter som möjliggör användning av tjänsten. Nödvändiga användarrättigheter får du av din egen chef.'
  },
  validationErrors: {
    ...components.validationErrors,
    ...components.datePicker.validationErrors,
    dateRangeNotLinear: 'Tidsperiodens startdag ska vara före slutdagen.',
    timeRangeNotLinear: 'Kontrollera ordningen',
    guardianMustBeHeard: 'Vårdnadshavaren måste höras',
    futureTime: 'Tid i framtiden'
  },
  holidayPeriods: {
    confirmDelete: 'Vill du verkligen ta bort semesterperioden?',
    createTitle: 'Skapa ny semesterperiod',
    editTitle: 'Redigera semesterperiod',
    period: 'Tidsperiod',
    reservationsOpenOn: 'Enkät öppnas',
    reservationDeadline: 'Reservationernas sista dag',
    clearingAlert:
      'Medborgarnas redan gjorda reservationer raderas för vald tidsperiod',
    confirmLabel:
      'Jag förstår att gjorda reservationer tas bort omedelbart och att detta inte längre kan ångras.',
    validationErrors: {
      tooSoon: 'Semesterperiod kan skapas tidigast 4 veckor framåt',
      tooLong: 'Semesterperiod kan vara högst 15 veckor lång',
      afterStart: 'Kan inte vara efter start',
      afterReservationsOpen: 'Kan inte vara efter öppningsdagen'
    }
  },
  holidayQuestionnaire: {
    confirmDelete: 'Vill du verkligen ta bort enkäten?',
    types: {
      FIXED_PERIOD: 'Fast period',
      OPEN_RANGES: 'Öppen period'
    },
    questionnaires: 'Frånvaroenkäter',
    absenceType: 'Frånvarotyp',
    title: 'Rubrik',
    description: 'Enkätens förklaring för medborgaren',
    descriptionLink: 'Tilläggsinfo-länk',
    active: 'Giltig',
    fixedPeriodOptionLabel: 'Periodens valfråga',
    fixedPeriodOptionLabelPlaceholder:
      'T.ex. Barn är borta 8 veckor under tidsperioden',
    fixedPeriodOptions: 'Periodens alternativ',
    fixedPeriodOptionsPlaceholder:
      '30.5.2022-24.8.2022, 6.6.2022-31.8.2022, separerade med kommatecken eller radbrytningar',
    requiresStrongAuth: 'Stark autentisering',
    conditionContinuousPlacement:
      'Kan svara på enkät om barnet har kontinuerlig placering',
    period: 'Frånvaroperiod',
    absenceTypeThreshold: 'Minimilängd för sammanhängande frånvaro',
    days: 'dagar'
  },
  holidayQuestionnaires: {
    confirmDelete: 'Vill du verkligen ta bort enkäten?',
    types: {
      FIXED_PERIOD: 'Fast period',
      OPEN_RANGES: 'Öppen period'
    },
    questionnaires: 'Frånvaroenkäter',
    absenceType: 'Frånvarotyp',
    title: 'Rubrik',
    description: 'Enkätens förklaring för medborgaren',
    descriptionLink: 'Tilläggsinfo-länk',
    active: 'Giltig',
    fixedPeriodOptionLabel: 'Periodens valfråga',
    fixedPeriodOptionLabelPlaceholder:
      'T.ex. Barn är borta 8 veckor under tidsperioden',
    fixedPeriodOptions: 'Periodens alternativ',
    fixedPeriodOptionsPlaceholder:
      '30.5.2022-24.8.2022, 6.6.2022-31.8.2022, separerade med kommatecken eller radbrytningar',
    requiresStrongAuth: 'Stark autentisering',
    conditionContinuousPlacement:
      'Kan svara på enkät om barnet har kontinuerlig placering',
    period: 'Frånvaroperiod',
    absenceTypeThreshold: 'Minimilängd för sammanhängande frånvaro',
    days: 'dagar'
  },
  terms: {
    term: 'Läsår',
    finnishPreschool: 'Finskspråkig förskoleundervisning',
    extendedTermStart: 'Förlängt läsår börjar',
    applicationPeriodStart: 'Ansökan till läsåret börjar',
    termBreaks: 'Undervisningsuppehåll',
    addTerm: 'Lägg till läsår',
    confirmDelete: 'Vill du verkligen ta bort läsåret?',
    extendedTermStartInfo:
      'Tidpunkt då småbarnspedagogikavgiften bestäms enligt kompletterande småbarnspedagogik.',
    termBreaksInfo:
      'Lägg till här sådana perioder under läsåret då undervisning inte erbjuds, t.ex. jullov.',
    addTermBreak: 'Lägg till uppehållsperiod',
    validationErrors: {
      overlap:
        'För denna tidsperiod finns redan ett överlappande läsår. Försök anteckna notering för en annan tidsperiod.',
      extendedTermOverlap:
        'För denna tidsperiod finns redan ett överlappande förlängt läsår. Försök anteckna notering för ett annat startdatum',
      extendedTermStartAfter:
        'Det förlängda läsårets startdatum kan inte vara efter läsårets startdatum.',
      termBreaksOverlap: 'Överlappande undervisningsuppehåll är inte tillåtet.'
    },
    modals: {
      editTerm: {
        title: 'Vill du verkligen redigera uppgifter?',
        text: 'Vill du verkligen redigera ett redan påbörjat läsår?',
        resolve: 'Redigera',
        reject: 'Redigera inte'
      },
      deleteTerm: {
        title: 'Vill du verkligen ta bort läsåret?'
      }
    }
  },
  preferredFirstName: {
    popupLink: 'Tilltalsnamn',
    title: 'Tilltalsnamn',
    description:
      'Du kan definiera ditt tilltalsnamn som används i eVaka. Tilltalsnamnet ska vara ett av dina förnamn. Om ditt namn har ändrats och du behöver uppdatera ditt nya namn i eVaka, kontakta Esbo HelpDesk.',
    select: 'Välj tilltalsnamn',
    confirm: 'Bekräfta'
  },
  metadata: {
    title: 'Arkiverbar metadata',
    notFound: 'Dokumentet har ingen arkiverbar metadata',
    caseIdentifier: 'Ärendebeteckning',
    processName: 'Ärendeprocess',
    organization: 'Organisation',
    archiveDurationMonths: 'Arkiveringstid',
    primaryDocument: 'Primärt dokument',
    secondaryDocuments: 'Andra dokument',
    documentId: 'Dokumentidentifierare',
    name: 'Dokumentets namn',
    createdAt: 'Upprättningstidpunkt',
    createdBy: 'Upprättare',
    monthsUnit: 'månader',
    confidentiality: 'Offentlighet',
    confidential: 'Sekretessbelagt',
    public: 'Offentlig',
    notSet: 'Ej angiven',
    confidentialityDuration: 'Sekretessperiod',
    confidentialityBasis: 'Sekretessgrund',
    years: 'år',
    receivedBy: {
      label: 'Ankomsstsätt',
      PAPER: 'På papper',
      ELECTRONIC: 'Elektroniskt'
    },
    sfiDelivery: {
      label: 'Suomi.fi -leveranser',
      method: {
        ELECTRONIC: 'Elektroniskt',
        PAPER_MAIL: 'Per post',
        PENDING: 'Väntar på leverans'
      }
    },
    history: 'Processhistorik',
    downloadPdf: 'Ladda ner PDF',
    states: {
      INITIAL: 'Ärendets anhängiggörande / -inkomst',
      PREPARATION: 'Ärendets beredning',
      DECIDING: 'Beslutsfattande',
      COMPLETED: 'Verkställande / Avslutande / Stängning'
    }
  },
  systemNotifications: {
    title: {
      CITIZENS: 'Meddelande synligt för medborgare',
      EMPLOYEES: 'Meddelande synligt för personal'
    },
    noNotification: 'Inget meddelande för tillfället',
    setNotification: 'Ange meddelande',
    text: 'Text',
    textFi: 'Text på finska',
    textSv: 'Text på svenska',
    textEn: 'Text på engelska',
    validTo: 'Försvinner från vyn'
  },
  placementTool: {
    title: 'Optimeringsverktyg',
    description:
      'Du kan skapa ansökningar i eVaka från placeringsförslag som producerats med optimeringsverktyget. Ansökningar skapas direkt i väntan på beslut.',
    preschoolTermNotification: 'Ansökningar skapas för nästa förskoleperiod:',
    preschoolTermWarning:
      'eVaka saknar definition för nästa förskoleperiod. Förskoleperiod behövs för att skapa ansökningar.',
    validation: (count: number, existing: number) =>
      `Du importerar ${count} placeringar${existing > 0 ? ` (varav ${existing} redan finns i systemet)` : ''}, fortsätt?`
  },
  outOfOffice: {
    menu: 'Ledarens frånvaroperiod',
    title: 'Frånvaroperiod',
    description:
      'Du kan lägga till information här om till exempel din semester. Barns vårdnadshavare ser ett meddelande under din frånvaro att du inte är på plats.',
    header: 'Frånvaroperiod',
    noFutureOutOfOffice: 'Inga kommande frånvaror',
    addOutOfOffice: 'Lägg till frånvaroperiod',
    validationErrors: {
      endBeforeToday: 'Kan inte sluta i det förflutna'
    }
  },
  components
}
