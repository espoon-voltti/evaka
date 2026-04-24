// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { type ReactNode } from 'react'

import type DateRange from 'lib-common/date-range'
import type FiniteDateRange from 'lib-common/finite-date-range'
import type { InvoiceStatus } from 'lib-common/generated/api-types/invoicing'
import type HelsinkiDateTime from 'lib-common/helsinki-date-time'
import type LocalDate from 'lib-common/local-date'
import { H3 } from 'lib-components/typography'

import components from '../../components/i18n/sv'

import type { fi } from './fi'

export const sv: typeof fi = {
  titles: {
    defaultTitle: 'Småbarnspedagogik',
    login: 'Logga in',
    ai: 'AI test',
    applications: 'Ansökningar',
    childInformation: 'Barnets uppgifter',
    employees: 'Användare',
    financeBasics: 'Ekonomins betalningsinställningar',
    units: 'Enheter',
    customers: 'Klientuppgifter',
    placementPlan: 'Placeringsplan',
    decision: 'Beslutsberedning och utskick',
    documentTemplates: 'Dokumentmallar',
    feeDecisions: 'Avgiftsbeslut',
    feeDecision: 'Avgiftsbeslut',
    feeDecisionDraft: 'Utkast för avgiftsbeslut',
    holidayPeriod: 'Semestertid',
    holidayPeriods: 'Semestertider',
    holidayAndTermPeriods: 'Semestertider och verksamhetsperioder',
    holidayQuestionnaire: 'Enkät för semestertider',
    groupCaretakers: 'Personalbehov i gruppen',
    incomeStatements: 'Inkomstutredningar',
    valueDecisions: 'Värdebeslut',
    valueDecision: 'Värdebeslut',
    valueDecisionDraft: 'Utkast för värdebeslut',
    incomeStatement: 'Blankett för inkomstutredning',
    invoices: 'Fakturor',
    payments: 'Fakturering',
    invoice: 'Faktura',
    invoiceDraft: 'Utkast för faktura',
    reports: 'Rapporter',
    messages: 'Meddelanden',
    caretakers: 'Personal',
    createUnit: 'Skapa ny enhet',
    personProfile: 'Den vuxnas uppgifter',
    personTimeline: 'Klientens tidigare registrerade uppgifter',
    personalMobileDevices: 'Personlig eVaka-mobil',
    preschoolTerm: 'Förskoletermin',
    preschoolTerms: 'Förskoleterminer',
    employeePinCode: 'PIN-kodhantering',
    preferredFirstName: 'Hantering av tilltalsnamn',
    settings: 'Inställningar',
    systemNotifications: 'Tillfälligt meddelande',
    unitFeatures: 'Öppna funktioner',
    welcomePage: 'Välkommen till eVaka',
    clubTerm: 'Klubbtermin',
    clubTerms: 'Klubbterminer',
    placementTool: 'Optimeringsverktyg',
    outOfOffice: 'Frånvaromeddelande',
    decisionReasonings: 'Beslutsmotiveringar'
  },
  common: {
    yes: 'Ja',
    no: 'Nej',
    and: 'Ja',
    loadingFailed: 'Hämtning av information misslyckades',
    noAccess: 'Rättigheter saknas',
    endpointDisabled:
      'eVaka genomgår för närvarande ett partiellt underhållsavbrott. Vissa funktioner är inte tillgängliga just nu. Försök igen om en stund.',
    edit: 'Redigera',
    add: 'Lägg till',
    addNew: 'Lägg till ny',
    clear: 'Töm',
    create: 'Skapa',
    remove: 'Ta bort',
    doNotRemove: 'Ta inte bort',
    archive: 'Arkivera',
    download: 'Ladda ner',
    cancel: 'Gå tillbaka',
    goBack: 'Tillbaka',
    leavePage: 'Avsluta',
    confirm: 'Bekräfta',
    period: 'För perioden',
    search: 'Sök',
    select: 'Välj',
    send: 'Skicka',
    save: 'Spara',
    saving: 'Sparas',
    saved: 'Sparad',
    unknown: 'Inte känd',
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
      club: 'Klubbverksamhet',
      preschool: 'Förskola',
      daycare: 'Småbarnspedagogik',
      daycare5yo: 'Småbarnspedagogik',
      preparatory: 'Förberedande',
      'backup-care': 'Reservplacering',
      temporary: 'Tillfällig',
      'school-shift-care': 'Skiftvård för skolbarn',
      'connected-daycare': 'Ansluten'
    },
    careTypeLabelsShort: {
      club: 'Klubbverksamhet',
      preschool: 'Förskola',
      daycare: 'Småbarnspedagogik (Sbp)',
      daycare5yo: 'Småbarnspedagogik (Sbp)',
      preparatory: 'Förberedande',
      'backup-care': 'Reservpl.',
      temporary: 'Tillfällig',
      'school-shift-care': 'Skolb. skiftv.',
      'connected-daycare': 'Ansluten'
    },
    providerType: {
      MUNICIPAL: 'Kommunal',
      PURCHASED: 'Köptjänst',
      PRIVATE: 'Privat',
      MUNICIPAL_SCHOOL: 'Finskspråkig grundläggande utbildning (SUPE)',
      PRIVATE_SERVICE_VOUCHER: 'Privat (servicesedel)',
      EXTERNAL_PURCHASED: 'Köptjänst (annat)'
    },
    types: {
      CLUB: 'Klubbverksamhet',
      FAMILY: 'Familjedagvård',
      GROUP_FAMILY: 'Gruppfamiljedagvård',
      CENTRE: 'Daghem',
      PRESCHOOL: 'Förskola',
      DAYCARE: 'Småbarnspedagogik',
      PRESCHOOL_DAYCARE:
        'Småbarnspedagogik i anslutning till förskoleundervisning',
      PREPARATORY_EDUCATION: 'Förberedande förskola',
      PREPARATORY_DAYCARE:
        'Småbarnspedagogik i anslutning till förskoleundervisning',
      DAYCARE_5YO_FREE: 'Avgiftsfri småbarnspedagogik för 5-åringar',
      DAYCARE_5YO_PAID: 'Småbarnspedagogik (avgiftsbelagd)'
    },
    form: {
      address: 'Adress',
      addressRestricted:
        'Adressen är inte tillgänglig på grund av sekretessmarkering',
      age: 'Ålder',
      backupPhone: 'Reserv telefonnummer',
      birthday: 'Födelsedatum',
      dateOfDeath: 'Avliden',
      email: 'E-post',
      endDate: 'Fram till',
      firstName: 'Förnamn',
      firstNames: 'Förnamn',
      invoiceRecipient: 'Fakturamottagare',
      invoicingAddress: 'Faktureringsadress',
      lastModified: 'Senast redigerad',
      lastModifiedBy: (name: string) => `Redigerare: ${name}`,
      lastName: 'Efternamn',
      name: 'Namn',
      ophPersonOid: 'UBS person-OID',
      phone: 'Telefonnummer',
      postOffice: 'Postort',
      postalCode: 'Postnummer',
      municipalityOfResidence: 'Hemkommun',
      range: 'För perioden',
      socialSecurityNumber: 'Personbeteckning',
      startDate: 'Från och med',
      streetAddress: 'Gatuadress',
      updatedFromVtj: 'Uppgifter uppdaterade från befolkningsregistret'
    },
    expandableList: {
      others: 'annat'
    },
    resultCount: (count: number) =>
      count > 0 ? `Sökresultat: ${count}` : 'Inga sökresultat',
    ok: 'Ok!',
    tryAgain: 'Försök igen',
    checkDates: 'Kontrollera datum',
    multipleChildren: 'Flera barn',
    today: 'Idag',
    error: {
      unknown: 'Hoppsan, något gick fel!',
      forbidden: 'Rättigheter saknas',
      saveFailed: 'Sparande av ändringar misslyckades, försök igen.',
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
    openExpandingInfo: 'Öppna fältet för tilläggsuppgifter',
    datetime: {
      weekdaysShort: ['Mån', 'Tis', 'Ons', 'Tor', 'Fre', 'Lör', 'Sön'],
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
    validTo: (date: string) => `Giltig till och med ${date}`,
    closeModal: 'Stäng popup',
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
        'Du håller på att göra en ändring som kan orsaka retroaktiva ändringar i klientfakturor.',
      checkboxLabel: 'Jag förstår, jag kontaktar faktureringen om detta.*'
    },
    userTypes: {
      SYSTEM: 'system',
      CITIZEN: 'kommuninvånare',
      EMPLOYEE: 'arbetstagare',
      MOBILE_DEVICE: 'mobilenhet',
      UNKNOWN: 'okänd'
    },
    showMore: 'Visa mer',
    showLess: 'Dölj'
  },
  header: {
    applications: 'Ansökningar',
    units: 'Enheter',
    search: 'Personuppgifter',
    finance: 'Ekonomi',
    invoices: 'Fakturor',
    payments: 'Fakturering',
    incomeStatements: 'Inkomstutredningar',
    feeDecisions: 'Avgiftsbeslut',
    valueDecisions: 'Värdebeslut',
    reports: 'Rapporter',
    messages: 'Meddelanden',
    logout: 'Logga ut'
  },
  footer: {
    cityLabel: 'Esbo stad',
    linkLabel: 'Esbo småbarnspedagogik',
    linkHref: 'https://www.espoo.fi/sv/fostran-och-utbildning/smabarnspedagogik'
  },
  language: {
    title: 'Språk',
    fi: 'Finska',
    sv: 'Svenska',
    en: 'Engelska'
  },
  errorPage: {
    reload: 'Ladda om sidan',
    text: 'Det uppstod ett oväntat fel. Felet har rapporterats vidare.',
    title: 'Något gick fel'
  },
  validationError: {
    mandatoryField: 'Obligatorisk uppgift',
    endDateIsMandatoryField: 'Slutdatum är en obligatorisk uppgift',
    dateRange: 'Datumet är felaktigt',
    invertedDateRange: 'Startdatumet får inte vara efter slutdatumet',
    existingDateRangeError:
      'Datumen får inte överlappa med redan skapade perioder',
    coveringDateRangeError:
      'Datumintervallet får inte täcka en existerande period helt',
    email: 'E-postadressen är i fel format',
    phone: 'Telefonnumret är i fel format',
    ssn: 'Personbeteckningen är i fel format',
    time: 'Tiden är i fel format',
    cents: 'Eurobeloppet är i fel format',
    decimal: 'Decimaltal är i fel format',
    startDateNotOnTerm: 'Startdatum måste infalla under en period'
  },
  login: {
    title: 'Småbarnspedagogik',
    subtitle: 'Personuppgifter och enheter',
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
    asDesktop: 'Som arbetsbord',
    list: {
      addNote: 'Lägg till anteckning',
      areaPlaceholder: 'Välj område',
      basis: 'Grunder',
      currentUnit: 'Nuvarande',
      dueDate: 'Bör behandlas',
      name: 'Barnets namn/ålder',
      noResults: 'Inga sökresultat',
      note: 'Obs',
      paper: 'Pappersansökan',
      resultCount: 'Sökresultat',
      serviceWorkerNote: 'Servicehandledningens anteckningar',
      siblingBasis: 'Syskonprincip',
      siblingNotFound: 'Syskonuppgifter hittades inte',
      noValidPlacement: 'Ingen gällande placering',
      startDate: 'Start',
      status: 'Status',
      statusLastModified: 'Status senast redigerad',
      subtype: 'Del / Hel',
      title: 'Ansökningar',
      transfer: 'Ansökan om byte',
      transferFilter: {
        title: 'Bytesansökningar',
        transferOnly: 'Visa endast bytesansökningar',
        hideTransfer: 'Dölj bytesansökningar',
        all: 'Ingen begränsning'
      },
      type: 'Ansökningstyp',
      unit: 'Enhet',
      voucherFilter: {
        title: 'Servicesedelansökningar',
        firstChoice: 'Visa om 1. önskemål',
        allVoucher: 'Visa alla servicesedelansökningar',
        hideVoucher: 'Dölj servicesedelansökningar',
        noFilter: 'Ingen begränsning'
      }
    },
    placementDesktop: {
      warnings: {
        tooManyApplicationsTitle: (count: number) =>
          `För många ansökningar (${count})`,
        tooManyApplicationsMessage:
          'Förfina sökvillkoren så att det finns högst 50 ansökningar.'
      },
      occupancyPeriod: 'Visa beläggningsgradens maximum för tidsperioden',
      shownUnitsCount: 'Enheter att visa',
      addShownUnit: 'Lägg till enhet att visa...',
      applicationsCount: 'Ansökningar',
      preferences: 'Ansökningsönskemål',
      createPlacementDraft: 'Gör ett utkast',
      createPlacementDraftToOtherUnit: 'Utkast till annan enhet...',
      cancelPlacementDraft: 'Avbryt utkast',
      cancelPlacementDraftConfirmationTitle:
        'Vill du verkligen avbryta placeringsutkastet?',
      cancelPlacementDraftConfirmationMessage:
        'Ansökan kopplad till detta finns inte med i sökresultaten som visas nu.',
      show: 'Visa',
      showUnit: 'Visa enhet',
      hideUnit: 'Dölj enhet',
      other: 'Annan',
      addToOtherUnit: 'Utkast till annan enhet',
      birthDate: 'Födelsedatum',
      dueDate: 'Lagstadgat',
      preferredStartDate: 'Önskat startdatum',
      transfer: 'Byte',
      toPlacementPlan: 'Placera',
      checkApplication: 'Kontrollera',
      occupancies: 'Belastningsgrad',
      occupancyTypes: {
        confirmed: 'Bekräftad',
        planned: 'Planerad',
        draft: 'Utkast'
      },
      openGraph: 'Öppna diagram för beläggningsgrad',
      placementDrafts: 'Utkast för placering',
      notInSearchResults: 'Ansökan finns inte med i sökresultaten',
      draftedBy: 'Utkast'
    },
    actions: {
      moveToWaitingPlacement: 'Flytta vidare för placering',
      returnToSent: 'Återställ till inkomna',
      cancelApplication: 'Ta bort från handläggning',
      cancelApplicationConfirm:
        'Vill du verkligen ta bort ansökan från handläggning?',
      cancelApplicationConfidentiality: 'Ska ansökan vara sekretessbelagd?',
      check: 'Kontrollera',
      setVerified: 'Markera som kontrollerad',
      createPlacementPlan: 'Placera',
      cancelPlacementPlan: 'Återställ till att placera',
      editDecisions: 'Beslut',
      confirmPlacementWithoutDecision: 'Bekräfta utan beslut',
      sendDecisionsWithoutProposal: 'Skicka beslut',
      sendPlacementProposal: 'Skicka placeringsförslag',
      withdrawPlacementProposal: 'Avbryt placeringsförslag',
      confirmDecisionMailed: 'Markera som posterad',
      checked: (count: number) =>
        count === 1 ? `${count} ansökan vald` : `${count} ansökningar valda`
    },
    distinctiveDetails: {
      SECONDARY: 'Visa även om enhet sökts som 2. eller 3. önskemål'
    },
    basisTooltip: {
      ADDITIONAL_INFO: 'Text i detaljfältet',
      SIBLING_BASIS: 'Använder syskongrund',
      ASSISTANCE_NEED: 'Stödbehov',
      CLUB_CARE: 'Klubbplats under föregående verksamhetsperiod',
      CONTINUATION: 'Fortsättningsbarn',
      DAYCARE: 'Har meddelat att hen avstår från plats i småbarnspedagogiken',
      EXTENDED_CARE: 'Skiftarbete',
      DUPLICATE_APPLICATION: 'Dubbelansökan',
      URGENT: 'Brådskande ansökan',
      HAS_ATTACHMENTS: 'Bilaga i ansökan'
    },
    types: {
      PRESCHOOL: 'Förskoleansökan',
      DAYCARE: 'Småbarnspedagogikansökan',
      CLUB: 'Klubbansökan',
      PRESCHOOL_ONLY: 'Förskola',
      PRESCHOOL_DAYCARE: 'Förskola & kompletterande småbarnspedagogik',
      PRESCHOOL_CLUB: 'Förskolans klubbverksamhet',
      PREPARATORY_ONLY: 'Förberedande',
      PREPARATORY_DAYCARE: 'Förberedande & kompletterande småbarnspedagogik',
      DAYCARE_ONLY: 'Senare ansökt om kompletterande småbarnspedagogik',
      ALL: 'Alla'
    },
    searchPlaceholder: 'Sök med namn, personbeteckning eller adress',
    basis: 'Observationer',
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
      PRESCHOOL_DAYCARE: 'Kompletterande småbarnspedagogik',
      PREPARATORY_EDUCATION: 'Förberedande undervisning',
      ALL: 'Alla'
    },
    statuses: {
      CREATED: 'Utkast',
      SENT: 'Inkommit',
      WAITING_PLACEMENT: 'Väntar på placering',
      WAITING_DECISION: 'Beslutsberedning',
      WAITING_UNIT_CONFIRMATION: 'För ledarens granskning',
      WAITING_MAILING: 'Väntar på att postas',
      WAITING_CONFIRMATION: 'Väntar på bekräftelse från vårdnadshavare',
      ACTIVE: 'Plats mottagen',
      REJECTED: 'Plats avvisad',
      CANCELLED: 'Avlägsnades från behandlingen',
      ALL: 'Alla'
    },
    selectConfidentialityLabel: 'Ska ansökan sekretessbeläggas?',
    selectAll: 'Välj alla',
    unselectAll: 'Avmarkera alla',
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
      restricted: 'Spärr markering i kraft',
      hasFutureAddress:
        'Adressen i befolkningsregistret har ändrats / kommer att ändras',
      futureAddress: 'Kommande adress',
      movingDate: 'Flyttdatum',
      nationality: 'Medborgarskap',
      language: 'Språk',
      phone: 'Telefonnummer',
      email: 'E-post',
      agreementStatus: 'Överenskommits tillsammans',
      otherGuardianAgreementStatuses: {
        AGREED: 'Överenskommits tillsammans',
        NOT_AGREED: 'Har inte överenskommits tillsammans',
        RIGHT_TO_GET_NOTIFIED: 'Endast informationsrätt',
        AUTOMATED: 'Automatiskt beslut',
        NOT_SET: 'Vårdnadshavare bor på samma adress'
      },
      noOtherChildren: 'Inga andra barn',
      applicantDead: 'Sökande avliden'
    },
    serviceNeed: {
      title: 'Servicebehov',
      startDate: 'Önskat startdatum',
      connectedLabel: 'Anslutande småbarnspedagogik',
      connectedValue: 'Jag ansöker också om anslutande småbarnspedagogik',
      connectedDaycarePreferredStartDateLabel:
        'Önskat startdatum för anslutande småbarnspedagogik',
      connectedDaycareServiceNeedOptionLabel: 'Behov av kompletterande service',
      dailyTime: 'Daglig närvarotid',
      startTimePlaceholder: '08:00',
      endTimePlaceholder: '16:00',
      shiftCareLabel: 'Kvälls- och skiftvård',
      shiftCareNeeded: 'Behöver kvälls- och skiftvård',
      shiftCareWithAttachments: 'Behöver kvälls- och skiftvård, bilagor:',
      urgentLabel: 'Brådskande ansökan',
      notUrgent: 'Nej',
      isUrgent: 'Är brådskande',
      isUrgentWithAttachments: 'Är brådskande, bilagor:',
      missingAttachment: 'Bilaga saknas',
      preparatoryLabel: 'Förberedande undervisning',
      preparatoryValue: 'Jag ansöker också om förberedande undervisning',
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
      wasOnClubCareLabel: 'I klubben under föregående verksamhetsperiod',
      wasOnClubCareValue:
        'Barnet har varit i klubben under föregående verksamhetsperiod',
      wasOnDaycareLabel: 'I småbarnspedagogik före klubben',
      wasOnDaycareValue:
        'Barnet är i småbarnspedagogik före klubbens önskade startdatum'
    },
    preferences: {
      title: 'Ansökningsönskemål',
      preferredUnits: 'Ansökningsönskemål',
      moveUp: 'Flytta uppåt',
      moveDown: 'Flytta nedåt',
      missingPreferredUnits: 'Välj minst ett ansökningsönskemål',
      unitMismatch: 'Ansökningsönskemål motsvarar inte sökta enheter',
      unitsOnMap: 'Enheter på kartan',
      siblingBasisLabel: 'Syskongrund',
      siblingBasisValue: 'Jag ansöker om plats på syskongrund',
      siblingName: 'Syskons namn',
      siblingSsn: 'Syskons personbeteckning',
      siblingUnit: 'Syskons enhet'
    },
    child: {
      title: 'Barnets uppgifter'
    },
    guardians: {
      title: 'Sökandens uppgifter',
      appliedGuardian: 'Sökandens uppgifter',
      secondGuardian: {
        title: 'Uppgifter om meddelad andra vuxen',
        checkboxLabel:
          'Sökande har meddelat den andra vårdnadshavarens uppgifter',
        exists: 'Barnet har en annan vårdnadshavare',
        sameAddress: 'Andra vårdnadshavaren bor på samma adress',
        separated: 'Andra vårdnadshavaren bor på annan adress',
        agreed: 'Ansökan är överenskommen tillsammans',
        noVtjGuardian:
          'Enligt befolkningsregistret har barnet ingen annan vårdnadshavare'
      },
      vtjGuardian:
        'Uppgifter om andra vårdnadshavaren enligt befolkningsregistret'
    },
    otherPeople: {
      title: 'Andra personer',
      adult: 'Annan vuxen',
      spouse: 'Sökande bor tillsammans med annan make eller sambo',
      children: 'Andra barn i samma hushåll',
      addChild: 'Lägg till barn'
    },
    additionalInfo: {
      title: 'Tilläggsuppgifter',
      applicationInfo: 'Ansökans tilläggsuppgifter',
      allergies: 'Allergier',
      diet: 'Specialdiet',
      maxFeeAccepted: 'Samtycke till högsta avgift',
      serviceWorkerAttachmentsTitle: 'Bilagor från servicerådgivningen',
      noAttachments: 'Inga bilagor'
    },
    decisions: {
      title: 'Beslut',
      noDecisions: 'Inga beslut har ännu kopplats till ansökan.',
      type: 'Typ av beslut',
      types: {
        CLUB: 'Klubbeslut',
        DAYCARE: 'Beslut om småbarnspedagogik',
        DAYCARE_PART_TIME: 'Beslut om småbarnspedagogik (deltid)',
        PRESCHOOL: 'Beslut om förskola',
        PRESCHOOL_DAYCARE: 'Beslut om anslutande småbarnspedagogik',
        PRESCHOOL_CLUB: 'Förskolegrupp',
        PREPARATORY_EDUCATION: 'Beslut om förberedande undervisning'
      },
      num: 'Beslutsnummer',
      status: 'Beslutets status',
      statuses: {
        draft: 'Utkast',
        waitingMailing: 'Väntar på postning',
        PENDING: 'Ska bekräftas av vårdnadshavare',
        ACCEPTED: 'Mottagen',
        REJECTED: 'Avvisad'
      },
      unit: 'Beslutsenhet',
      download: 'Ladda ner beslutet som PDF-fil',
      downloadPending:
        'Beslutets PDF-fil är inte ännu tillgänglig för nedladdning. Försök igen senare.',
      response: {
        label: 'Bekräftelse på kommuninvånarens vägnar',
        accept: 'Vårdnadshavare har mottagit platsen',
        reject: 'Vårdnadshavare har avvisat platsen',
        submit: 'Bekräfta på kommuninvånarens vägnar',
        acceptError:
          'Mottagning av plats misslyckades. Datumet kan vara felaktigt.',
        rejectError:
          'Avvisning av plats misslyckades. Uppdatera sidan och försök igen.'
      },
      blocked:
        'Detta beslut kan godkännas först efter att beslutet om förskola har godkänts'
    },
    attachments: {
      title: 'Bilagor',
      none: 'Inga bilagor kopplade till ansökan',
      name: 'Filnamn',
      updated: 'Ändrad',
      contentType: 'Typ',
      receivedByPaperAt: 'Levererad i pappersform',
      receivedAt: 'Levererad elektroniskt'
    },
    state: {
      title: 'Ansökans status',
      status: 'Ansökans status',
      origin: 'Ansökans avsändningssätt',
      sent: 'Anlänt',
      modified: 'Senast ändrad',
      modifiedBy: 'Redigerare',
      due: 'Ska behandlas senast'
    },
    date: {
      DUE: 'Ansökan ska behandlas senast',
      START: 'Startbehov',
      ARRIVAL: 'Ansökan anlände'
    },
    notes: {
      add: 'Lägg till anteckning',
      newNote: 'Ny anteckning',
      created: 'Skapad',
      editing: 'Under redigering',
      lastEdited: 'Senast redigerad',
      placeholder: 'Skriv en anteckning',
      confirmDelete: 'Vill du verkligen radera anteckningen',
      sent: 'Skickad',
      message: 'meddelande',
      error: {
        save: 'Spara anteckning misslyckades',
        remove: 'Radera anteckning misslyckades'
      }
    },
    messaging: {
      sendMessage: 'Skicka meddelande'
    }
  },
  childInformation: {
    restrictedDetails: 'Spärr för utlämnande av sekretessbelagda uppgifter',
    asAdult: 'Granska som vuxen',
    personDetails: {
      title: 'Person-, kontakt- och hälsouppgifter',
      attendanceReport: 'Närvaro- och frånvarouppgifter',
      name: 'Barnets namn',
      email: 'E-post',
      socialSecurityNumber: 'Personbeteckning',
      birthday: 'Födelsedatum',
      language: 'Språk (Befolkningsregistret)',
      address: 'Adress',
      familyLink: 'Familjens uppgifter',
      languageAtHome: 'Hemspråk, om annat än angivet i befolkningsregistret',
      specialDiet: 'Specialdiet som används i matbeställningsintegrationen',
      mealTexture: 'Matens struktur som används i matbeställningsintegrationen',
      participatesInBreakfast: 'Äter frukost',
      participatesInBreakfastYes: 'Ja',
      participatesInBreakfastNo: 'Nej',
      nekkuDiet: 'Nekku-matbeställningens diet',
      nekkuSpecialDiet: 'Nekku-specialdiet',
      nekkuSpecialDietInfo:
        'I fältet Nekku-specialdiet antecknas endast sådana allergier som inte kan väljas med markeringar i punkt Nekku-specialdiet. Här antecknas inte barnets drycker till mat, utan de antecknas i punkten Tilläggsuppgifter ovan.',
      noGuardian:
        'Vårdnadshavaruppgifter saknas. Barnets vårdnadshavare kan inte uträtta ärenden i eVaka',
      placeholder: {
        languageAtHome: 'Välj språk',
        languageAtHomeDetails: 'Tilläggsuppgifter om hemspråk',
        specialDiet: 'Välj specialdiet',
        mealTexture: 'Välj matens struktur'
      }
    },
    familyContacts: {
      title: 'Familjens kontaktuppgifter och reservhämtare',
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
      backupPhone: 'Reserv telefonnummer'
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
        'Skriv här veckovis servicebehov som omfattar i sin helhet den närvarotid som familjen angett, inklusive eventuell förskola, gratis småbarnspedagogik för 5-åringar och förberedande undervisning.',
      hoursInWeek: 'h / vecka',
      serviceNeedDetails: 'Precisering av servicebehov',
      createdByName: 'Bekräftelse av enhetens ledare',
      create: 'Skapa nytt servicebehov',
      removeServiceNeed: 'Vill du radera servicebehovet?',
      previousServiceNeeds: 'Tidigare skapade servicebehov',
      errors: {
        conflict: 'Servicebehov överlappar med ett annat servicebehov.',
        hardConflict:
          'Servicebehov överlappar med ett annat servicebehovs startdatum.',
        checkHours: 'Kontrollera',
        placementMismatchWarning:
          'Veckovis servicebehov motsvarar inte placeringens verksamhetsform.',
        autoCutWarning:
          'Tidigare överlappande servicebehov avbryts automatiskt.'
      }
    },
    dailyServiceTimes: {
      title: 'Daglig småbarnspedagogiktid',
      info: 'Skriv här den dagliga småbarnspedagogiktid som angetts i småbarnspedagogikavtalet, inklusive förskola / förberedande undervisning / gratis småbarnspedagogik för 5-åringar.',
      info2:
        'Uppdatera inte småbarnspedagogiktiden om den småbarnspedagogiktid som angetts i det nya avtalet inte har ändrats från den tidigare.',
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
      validUntil: 'Daglig småbarnspedagogiktids giltighetstid upphör',
      createNewTimes: 'Skapa ny daglig småbarnspedagogiktid',
      deleteModal: {
        title: 'Tas småbarnspedagogiktiden bort?',
        description:
          'Vill du verkligen radera den dagliga småbarnspedagogiktiden? Tiden kan inte återställas, utan den måste vid behov läggas till på nytt efter radering.',
        deleteBtn: 'Ta bort tid'
      },
      retroactiveModificationWarning:
        'Obs! Du redigerar den dagliga småbarnspedagogiktiden retroaktivt. Barnets närvarokalenderns anteckningar kan ändras under denna tidsperiod.'
    },
    assistance: {
      title: 'Stödbehov och stödåtgärder',
      unknown: 'Inte känt',
      fields: {
        capacityFactor: 'Koefficient',
        lastModified: 'Senast ändrad',
        lastModifiedBy: (name: string) => `Redigerare ${name}.`,
        level: 'Nivå',
        otherAssistanceMeasureType: 'Åtgärd',
        status: 'Status',
        validDuring: 'Giltighetstid'
      },
      validationErrors: {
        overlap:
          'För denna tidsperiod finns det redan en överlappande anteckning. Redigera vid behov föregående tidsperiod',
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
          SPECIAL_SUPPORT: 'Särskilt stöd utan tidigarelagd läroplikt',
          SPECIAL_SUPPORT_WITH_DECISION_LEVEL_1:
            'Särskilt stöd och tidigarelagd läroplikt - övrigt (till Koski)',
          SPECIAL_SUPPORT_WITH_DECISION_LEVEL_2:
            'Särskilt stöd och tidigarelagd läroplikt - utvecklingsstörning 2 (till Koski)',
          CHILD_SUPPORT:
            'Barnspecifikt stöd utan tidigarelagd läroplikt (till Koski)',
          CHILD_SUPPORT_AND_EXTENDED_COMPULSORY_EDUCATION:
            'Barnspecifikt stöd och tidigarelagd läroplikt (till Koski)',
          CHILD_SUPPORT_AND_OLD_EXTENDED_COMPULSORY_EDUCATION:
            'Barnspecifikt stöd och förlängd läroplikt enligt gammal modell - övrigt (till Koski, i bruk under övergångsperioden 1.8.2025 - 31.7.2026)',
          CHILD_SUPPORT_2_AND_OLD_EXTENDED_COMPULSORY_EDUCATION:
            'Barnspecifikt stöd och förlängd läroplikt enligt gammal modell - utvecklingsstörning 2 (till Koski, i bruk under övergångsperioden 1.8.2025 - 31.7.2026)',
          GROUP_SUPPORT: 'Gruppspecifika stödformer (inte i bruk)'
        },
        otherAssistanceMeasureType: {
          TRANSPORT_BENEFIT: 'Transportförmån (för förskolebarn Koski-uppgift)',
          ACCULTURATION_SUPPORT: 'Stöd för barnets integration (NTM)',
          ANOMALOUS_EDUCATION_START:
            'Avvikande starttidpunkt för undervisningen',
          CHILD_DISCUSSION_OFFERED: 'Barndiskussion erbjuden',
          CHILD_DISCUSSION_HELD: 'Barndiskussion hållen',
          CHILD_DISCUSSION_COUNSELING: 'Barndiskussion konsultation'
        }
      },
      assistanceFactor: {
        title: 'Stödkoefficient',
        create: 'Skapa ny tidsperiod för stödkoefficienten',
        removeConfirmation: 'Vill du radera stödkoefficientsperioden?',
        info: (): React.ReactNode => undefined
      },
      daycareAssistance: {
        title: 'Stödnivå i småbarnspedagogiken',
        create: 'Skapa ny tidsperiod för stödnivån (småbarnspedagogik)',
        removeConfirmation: 'Vill du radera stödnivåns tidsperiod?'
      },
      preschoolAssistance: {
        title: 'Stöd i förskolan',
        create: 'Skapa ny stödperiod (förskola)',
        removeConfirmation: 'Vill du radera stödperioden?'
      },
      otherAssistanceMeasure: {
        title: 'Andra åtgärder',
        create: 'Lägg till annan åtgärd',
        removeConfirmation: 'Vill du radera den andra åtgärden?',
        infoList: 'Tilläggsuppgifter om andra åtgärder:',
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
          'Kapaciteten bestäms vanligtvis utifrån barnets ålder och servicebehov. Om barnet har ett sådant stöd som använder mer kapacitet, lägg till stödets koefficient här. T.ex. koefficienten för ett barn som behöver stöd i en specialgrupp är 2,33' as ReactNode,
        bases: 'Grunder'
      },
      create: 'Skapa ny stödbehovsperiod',
      removeConfirmation: 'Vill du radera stödbehovet?',
      errors: {
        invalidCoefficient: 'Felaktig koefficient.',
        conflict: 'Stödbehov överlappar med ett annat stödbehov.',
        hardConflict:
          'Stödbehov överlappar med ett annat stödbehovs startdatum.',
        autoCutWarning: 'Tidigare överlappande stödbehov avbryts automatiskt.'
      }
    },
    assistanceAction: {
      title: 'Stödåtgärder och verksamhet',
      modified: 'Senast ändrad',
      fields: {
        dateRange: 'Stödåtgärdernas giltighetstid',
        actions: 'Stödåtgärder',
        actionsByCategory: {
          DAYCARE: 'Stödåtgärder i småbarnspedagogiken',
          PRESCHOOL: 'Stödåtgärder i förskolan',
          OTHER: 'Andra stödåtgärder'
        },
        actionTypes: {
          OTHER: 'Annan stödåtgärd'
        },
        otherActionPlaceholder:
          'Du kan skriva här tilläggsuppgifter om andra stödåtgärder.',
        lastModifiedBy: (name: string) => `Redigerare ${name}.`
      },
      create: 'Skapa ny tidsperiod för stödåtgärder',
      removeConfirmation: 'Vill du radera stödåtgärdernas tidsperiod?',
      errors: {
        conflict: 'Stödåtgärder överlappar med en annan tidsperiod.',
        hardConflict:
          'Stödåtgärder överlappar med en annan tidsperiods startdatum.',
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
        external: 'Dokument som ska fyllas i av vårdnadshavare'
      },
      table: {
        document: 'Dokument',
        status: 'Status',
        open: 'Öppna dokument',
        modified: 'Ändrad',
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
        external: 'Skapa dokument som ska fyllas i av vårdnadshavare'
      },
      select: 'Välj dokument',
      removeConfirmation: 'Vill du verkligen radera dokumentet?',
      confirmation:
        'Är du säker på att du vill öppna detta dokument för barnet? Alla dokument publiceras för vårdnadshavare och arkiveras automatiskt vid slutet av verksamhetsperioden',
      statuses: {
        DRAFT: 'Utkast',
        PREPARED: 'Upprättad',
        DECISION_PROPOSAL: 'Beslutsförslag',
        COMPLETED: 'Färdig'
      },
      decisions: {
        accept: 'Gör ett jakande beslut',
        acceptConfirmTitle: 'Vill du verkligen göra ett jakande beslut?',
        retroactiveWarningTitle: 'Obs!',
        retroactiveWarningMessage:
          'Du håller på att göra ett retroaktivt beslut som börjar i det förflutna.',
        validityPeriod: 'Beviljas för tiden',
        reject: 'Gör ett nekande beslut',
        rejectConfirmTitle: 'Vill du verkligen göra ett nekande beslut?',
        annul: 'Annullera beslutet',
        annulConfirmTitle: 'Vill du verkligen annullera beslutet?',
        annulInstructions:
          'Annullera beslutet endast av motiverad anledning, t.ex. om beslutet har gjorts av misstag till fel enhet. Meddela alltid vårdnadshavare om annulleringen.',
        annulReasonLabel: 'Motivering för annullering av beslutet',
        decisionNumber: 'Beslutsnummer',
        updateValidity: 'Korrigera beslutets giltighetstid',
        otherValidDecisions: {
          title: 'Andra gällande beslut',
          description1: 'Du håller på att göra ett jakande beslut',
          description2: (validity: DateRange) =>
            `Barnet har andra beslut som är giltiga när det nya beslutet träder i kraft ${validity.start.format()}`,
          label: 'Välj lämplig åtgärd för följande beslut*',
          options: {
            end: 'Avbryts',
            keep: 'Avbryts inte'
          }
        },
        errors: {
          conflict:
            'Klienten har redan ett beslut från och med samma datum. Annullera det gamla beslutet eller gör ett nytt beslut som börjar från ett senare datum.'
        }
      },
      editor: {
        lockedErrorTitle: 'Dokumentet är tillfälligt låst',
        lockedError:
          'En annan användare redigerar dokumentet. Försök igen senare.',
        lockedErrorDetailed: (modifiedByName: string, opensAt: string) =>
          `Användaren ${modifiedByName} redigerar dokumentet. Dokumentlåset frigörs ${opensAt} om redigeringen inte fortsätts. Försök igen senare.`,
        saveError: 'Det gick inte att spara dokumentet.',
        preview: 'Förhandsgranska',
        publish: 'Publicera för vårdnadshavare',
        publishConfirmTitle: 'Vill du verkligen publicera för vårdnadshavaren?',
        publishConfirmText:
          'Vårdnadshavaren får se den aktuella versionen. Ändringar du gör därefter syns inte för vårdnadshavaren förrän du publicerar på nytt.',
        downloadPdf: 'Ladda ner som PDF',
        archive: 'Arkivera',
        alreadyArchived: (archivedAt: HelsinkiDateTime) =>
          `Dokumentet är arkiverat ${archivedAt.toLocalDate().format()}`,
        archiveDisabledNotExternallyArchived:
          'Dokumentet har inte konfigurerats för att överföras till ett externt arkiv',
        archiveDisabledNotCompleted: 'Dokumentet är inte i klar-status',
        goToNextStatus: {
          DRAFT: 'Publicera i utkast-status',
          PREPARED: 'Publicera i upprättad-status',
          CITIZEN_DRAFT: 'Skicka för kommuninvånaren att fylla i',
          DECISION_PROPOSAL: 'Skicka till beslutsfattare',
          COMPLETED: 'Publicera i klar-status'
        },
        goToNextStatusConfirmTitle: {
          DRAFT: 'Vill du verkligen publicera dokumentet i utkast-status?',
          PREPARED:
            'Vill du verkligen publicera dokumentet i upprättad-status?',
          CITIZEN_DRAFT:
            'Vill du verkligen publicera dokumentet i status för kommuninvånaren att fylla i?',
          DECISION_PROPOSAL:
            'Vill du verkligen skicka beslutsförslaget till beslutsfattaren?',
          COMPLETED: 'Vill du verkligen publicera dokumentet i klar-status?'
        },
        goToCompletedConfirmText:
          'Vårdnadshavaren får se den aktuella versionen. Ett dokument i klar-status kan inte längre redigeras. Endast huvudanvändare kan ångra detta.',
        extraConfirmCompletion:
          'Jag förstår att dokumentet inte kan redigeras efter detta',
        goToPrevStatus: {
          DRAFT: 'Återställ till utkast',
          PREPARED: 'Återställ till upprättad',
          CITIZEN_DRAFT: 'Returnera till vårdnadshavaren',
          DECISION_PROPOSAL: 'Återställ till beslutsförslag', // not applicable,
          COMPLETED: 'Återställ till klar' // not applicable
        },
        goToPrevStatusConfirmTitle: {
          DRAFT: 'Vill du verkligen återställa dokumentet till utkast?',
          PREPARED: 'Vill du verkligen återställa dokumentet till berett?',
          CITIZEN_DRAFT:
            'Vill du verkligen återställa dokumentet för vårdnadshavaren att fylla i?',
          DECISION_PROPOSAL:
            'Vill du verkligen återställa beslutet till beslutsförslag?', // not applicable,
          COMPLETED: 'Vill du verkligen återställa dokumentet till klar?' // not applicable,
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
        notSet: 'Ej angiven'
      }
    },
    assistanceNeedVoucherCoefficient: {
      actions: 'Åtgärder',
      create: 'Ange ny servicesedelkoefficient',
      deleteModal: {
        title: 'Ta bort servicesedelkoefficient?',
        description:
          'Vill du verkligen ta bort servicesedelkoefficienten? Ett nytt värdebeslut skapas inte för klienten även om koefficienten tas bort, utan du måste göra ett nytt retroaktivt värdebeslut.',
        delete: 'Ta bort koefficient'
      },
      factor: 'Koefficient',
      form: {
        coefficient: 'Servicesedelkoefficient (tal)',
        editTitle: 'Redigera servicesedelkoefficient',
        errors: {
          previousOverlap:
            'En tidigare överlappande servicesedelkoefficient avslutas automatiskt.',
          upcomingOverlap:
            'En framtida överlappande servicesedelkoefficient flyttas automatiskt för att börja senare.',
          fullOverlap:
            'Föregående överlappande servicesedelkoefficient tas bort automatiskt.',
          coefficientRange: 'Koefficienten måste vara mellan 1-10'
        },
        title: 'Ange ny servicesedelkoefficient',
        titleInfo:
          'Välj servicesedelkoefficients giltighetsdatum enligt beslutet om stödbehov.',
        validityPeriod: 'Servicesedelkoefficient giltig'
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
      guardian: 'Ansökans avsändare',
      preferredUnit: 'Ansökt enhet',
      startDate: 'Ansökt startdatum',
      sentDate: 'Ansökan inlämnad',
      type: 'Serviceform',
      types: {
        PRESCHOOL: 'Förskola',
        PRESCHOOL_DAYCARE: 'Ansluten småbarnspedagogik',
        PREPARATORY_EDUCATION: 'Förberedande undervisning',
        DAYCARE: 'Småbarnspedagogik',
        DAYCARE_PART_TIME: 'Småbarnspedagogik',
        CLUB: 'Klubbverksamhet'
      },
      status: 'Status',
      statuses: {
        CREATED: 'Utkast',
        SENT: 'Inlämnad',
        WAITING_PLACEMENT: 'Väntar på placering',
        WAITING_DECISION: 'Beslut under beredning',
        WAITING_UNIT_CONFIRMATION: 'Väntar på ledarens godkännande',
        WAITING_MAILING: 'Väntar på postning',
        WAITING_CONFIRMATION: 'Bekräftas av vårdnadshavare',
        REJECTED: 'Plats avvisad',
        ACTIVE: 'Plats mottagen',
        CANCELLED: 'Borttagen från handläggning'
      },
      open: 'Öppna ansökan',
      create: {
        createButton: 'Skapa ny ansökan',
        modalTitle: 'Ny ansökan',
        applier: 'Ansökans avsändare',
        personTypes: {
          GUARDIAN: 'Välj bland vårdnadshavare',
          DB_SEARCH: 'Sök i klientuppgifter',
          VTJ: 'Sök i befolkningsregistret',
          NEW_NO_SSN: 'Skapa ny utan personbeteckning'
        },
        applicationType: 'Ansökningstyp',
        applicationTypes: {
          DAYCARE: 'Ansökan om småbarnspedagogik',
          PRESCHOOL: 'Ansökan om förskola',
          CLUB: 'Ansökan om klubbverksamhet'
        },
        sentDate: 'Ansökan inlämnad',
        hideFromGuardian: 'Dölj ansökan för vårdnadshavare',
        transferApplication: 'Överflyttningsansökan'
      }
    },
    additionalInformation: {
      title: 'Tilläggsuppgifter',
      allergies: 'Allergier',
      diet: 'Specialdiet',
      additionalInfo: 'Tilläggsuppgifter',
      preferredName: 'Tilltalsnamn',
      medication: 'Medicinering'
    },
    income: {
      title: 'Inkomstuppgifter'
    },
    feeAlteration: {
      title: 'Avdrag, befrielser och höjningar',
      error: 'Det gick inte att ladda avgiftsändringar',
      create: 'Skapa ny avgiftsändring',
      updateError: 'Det gick inte att spara avgiftsändring',
      deleteError: 'Det gick inte att ta bort avgiftsändring',
      confirmDelete: 'Vill du ta bort avgiftsändringen?',
      lastModifiedAt: (date: string) => `Senast redigerad ${date}`,
      lastModifiedBy: (name: string) => `Redigerare: ${name}`,
      editor: {
        titleNew: 'Lägg till ny sänkning eller höjning',
        titleEdit: 'Redigera sänkning eller höjning',
        alterationType: 'Ändringstyp',
        alterationTypePlaceholder: 'Ändringstyp',
        validDuring: 'Beviljas för',
        notes: 'Tilläggsuppgifter',
        cancel: 'Avbryt',
        save: 'Spara'
      },
      types: {
        DISCOUNT: 'Sänkning',
        INCREASE: 'Höjning',
        RELIEF: 'Sänkning'
      },
      attachmentsTitle: 'Bilagor',
      employeeAttachments: {
        title: 'Lägg till bilagor',
        description:
          'Här kan du lägga till bilagor som klienten har skickat för avgifts-sänkningar, befrielser eller höjningar.'
      }
    },
    placements: {
      title: 'Placeringar och servicebehov',
      placements: 'Placeringar',
      rowTitle: 'Placeringsbeslut giltigt',
      startDate: 'Startdatum',
      endDate: 'Slutdatum',
      createdBy: 'Skapad av',
      source: 'Informationen ifylld av',
      sourceOptions: {
        CITIZEN: 'Kommuninvånarens ansökan',
        EMPLOYEE_MANUAL: 'Arbetstagare manuellt',
        EMPLOYEE_PAPER: 'Arbetstagare från pappersansökan',
        SYSTEM: 'System',
        UNKNOWN: 'Information ej tillgänglig'
      },
      modifiedAt: 'Redigerad',
      modifiedBy: 'Redigerare',
      terminatedByGuardian: 'Vårdnadshavare har sagt upp',
      terminated: 'Uppsagd',
      area: 'Område',
      daycareUnit: 'Verksamhetsställe',
      daycareGroups: 'Grupp',
      daycareGroupMissing: 'Ej grupperad',
      type: 'Placeringstyp',
      providerType: 'Arrangemangsform',
      updatedAt: 'Senast uppdaterad',
      serviceNeedMissing1: 'Servicebehov saknas för placering',
      serviceNeedMissing2:
        'dagen. Markera servicebehov för hela placeringstiden.',
      serviceNeedMissingTooltip1: 'Servicebehov saknas',
      serviceNeedMissingTooltip2: 'dagen.',
      deletePlacement: {
        btn: 'Ta bort placering',
        confirmTitle: 'Vill du verkligen ta bort denna placering?',
        hasDependingBackupCares:
          'Barnets reservplacering är beroende av denna placering, så att ta bort denna placering kan ändra eller ta bort reservplaceringen.'
      },
      createPlacement: {
        btn: 'Skapa ny placering',
        title: 'Ny placering',
        text: 'Beslut kan inte skickas för denna placering. Om placeringen överlappar med barnets tidigare skapade placeringar, kommer dessa placeringar att förkortas eller tas bort automatiskt.',
        temporaryDaycareWarning: 'OBS! Använd inte vid reservplacering!',
        startDateMissing: 'Startdatum är obligatorisk uppgift',
        unitMissing: 'Enhet saknas',
        preschoolTermNotOpen: 'Placeringen måste vara under förskolesäsongen',
        preschoolExtendedTermNotOpen:
          'Placeringen måste vara under förskolesäsongen',
        placeGuarantee: {
          title: 'Garanti för plats inom småbarnspedagogisk verksamhet',
          info: 'Framtida placering är relaterad till garantin för småbarnspedagogikplats'
        }
      },
      error: {
        conflict: {
          title: 'Datumet kunde inte redigeras',
          text:
            'Barnet har en placering som överlappar' +
            ' med de datum du angett. Du kan gå tillbaka och redigera' +
            ' datumen eller kontakta en huvudanvändare.'
        }
      },
      warning: {
        overlap: 'Det finns redan en placering för perioden',
        ghostUnit: 'Inte preciserad enhet',
        backupCareDepends:
          'Reservplacering är beroende av denna placering, och det ändrade tidsintervallet kan ta bort eller ändra reservplaceringen.'
      },
      serviceNeeds: {
        title: 'Placeringens servicebehov',
        period: 'Tidsintervall',
        description: 'Beskrivning',
        shiftCare: 'Kväll/Skift',
        shiftCareTypes: {
          NONE: 'Nej',
          INTERMITTENT: 'Slumpmässig',
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
            'Det servicebehov du markerat överlappar med ett tidigare angivet. Om du bekräftar det nu markerade servicebehovet, kommer det tidigare markerade servicebehovet att avslutas automatiskt för den överlappande perioden.'
        },
        optionStartNotValidWarningTitle: (validFrom: LocalDate) =>
          `Den valda servicebehovstypen är tillgänglig först från och med ${validFrom.format()}`,
        optionEndNotValidWarningTitle: (validTo: LocalDate) =>
          `Den valda servicebehovstypen är tillgänglig endast till och med ${validTo.format()}`,
        optionStartEndNotValidWarningTitle: (validity: FiniteDateRange) =>
          `Den valda servicebehovstypen är tillgänglig under perioden ${validity.format()}`,
        notFullyValidOptionWarning:
          'Den valda servicebehovstypen måste vara tillgänglig under hela perioden. Skapa servicebehovet vid behov i två delar.'
      }
    },
    absenceApplications: {
      title: 'Ansökningar om frånvaro från förskola',
      absenceApplication: 'Ansökan om frånvaro',
      range: 'Frånvaroperiod',
      createdBy: 'Ansökans avsändare',
      description: 'Orsak till frånvaro',
      acceptInfo:
        'Om du godkänner förslaget, markeras barnet automatiskt som frånvarande för den period vårdnadshavaren ansökt om.',
      reject: 'Avslå ansökan',
      accept: 'Godkänn ansökan',
      list: 'Tidigare ansökningar',
      status: 'Status',
      statusText: {
        WAITING_DECISION: 'Väntar på beslut',
        ACCEPTED: 'Godkänd',
        REJECTED: 'Avslagen'
      },
      rejectedReason: 'Orsak',
      rejectModal: {
        title: 'Avslag på ansökan om frånvaro från förskola',
        reason: 'Avslagsorsak'
      },
      userType: {
        SYSTEM: 'system',
        CITIZEN: 'vårdnadshavare',
        EMPLOYEE: 'arbetstagare',
        MOBILE_DEVICE: 'mobil',
        UNKNOWN: 'okänd'
      }
    },
    serviceApplications: {
      title: 'Ansökningar om ändring av servicebehov',
      applicationTitle: 'Ansökan om ändring av servicebehov',
      sentAt: 'Inskickad',
      sentBy: 'Sökande',
      startDate: 'Föreslagen startdag',
      serviceNeed: 'Föreslaget servicebehov',
      additionalInfo: 'Tilläggsuppgifter',
      status: 'Status',
      decision: {
        statuses: {
          ACCEPTED: 'Godkänd',
          REJECTED: 'Avslagen'
        },
        rejectedReason: 'Avslagsgrund',
        accept: 'Godkänn',
        reject: 'Avslå',
        confirmAcceptTitle: 'Godkänns ansökan om nytt servicebehov?',
        confirmAcceptText: (range: FiniteDateRange, placementChange: boolean) =>
          `En ny ${placementChange ? 'placering och ' : ''}servicebehov skapas för perioden ${range.format()}.`,
        shiftCareLabel: 'Kväll/Skiftvård',
        shiftCareCheckbox: 'Barnet har rätt till kväll/skiftvård',
        partWeekLabel: 'Delvecka',
        partWeekCheckbox: 'Servicebehovet är delvecka',
        confirmAcceptBtn: 'Bekräfta',
        confirmRejectTitle: 'Avslag på ansökan'
      },
      decidedApplications: 'Handlagda ansökningar',
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
      dateRange: 'Reservplacering för tiden',
      unit: 'Enhet',
      validationNoMatchingPlacement:
        'Reservplaceringen är inte under något barns placeringstid.',
      validationChildAlreadyInOtherUnit:
        'Barnet är redan inskrivet på en annan enhet.',
      validationBackupCareNotOpen:
        'Enheten är inte öppen under hela reservplaceringstiden.'
    },
    backupPickups: {
      title: 'Reservsökande',
      name: 'Reservsökandes namn',
      phone: 'Telefonnummer',
      add: 'Lägg till reservsökande',
      edit: 'Redigera reservsökandes uppgifter',
      removeConfirmation: 'Vill du verkligen ta bort reservsökanden?'
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
        'Vill du verkligen ta bort det pedagogiska dokumentet och dess beskrivningstext? Borttagningen kan inte ångras, och dokumentet tas bort även från vårdnadshavarens vy.',
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
    maxResultsFound: 'Begränsa sökningen för att se övriga resultat',
    socialSecurityNumber: 'Personbeteckning',
    newAdult: 'Skapa vuxen utan personbeteckning',
    newChild: 'Skapa barn utan personbeteckning',
    addPersonFromVTJ: {
      title: 'Importera person från befolkningsregistret',
      modalConfirmLabel: 'Importera person',
      ssnLabel: 'Personbeteckning',
      restrictedDetails: 'Personen har sekretessmarkering',
      badRequest: 'Ogiltig personbeteckning',
      notFound: 'Inga resultat',
      unexpectedError: 'Det gick inte att söka personuppgifter'
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
        postOffice: 'Verksamhetsställe',
        phone: 'Telefon',
        email: 'E-post'
      }
    }
  },
  personProfile: {
    restrictedDetails: 'Sekretessmarkering',
    asChild: 'Granska som barn',
    timeline: 'Tidslinje',
    personDetails: 'Person- och kontaktuppgifter',
    addSsn: 'Ange personbeteckning',
    noSsn: 'Utan personbeteckning',
    ssnAddingDisabledCheckbox:
      'Endast huvudanvändare har rätt att ange personbeteckning för barn',
    ssnAddingDisabledInfo:
      'Användare inom servicehandledning och ekonomi får inte ange personbeteckning för barn. När personbeteckning saknas har barnet ingen vårdnadsrelation. Om personbeteckning ska anges senare måste barnets tidigare dokument tas bort från systemet.',
    ssnInvalid: 'Ogiltig personbeteckning',
    ssnConflict: 'Denna användare finns redan i systemet.',
    showDetails: 'Visa alla',
    hideDetails: 'Visa mindre',
    updateFromVtj: 'Uppdatera från befolkningsregistret',
    partner: 'Makar/Sambor',
    partnerInfo:
      'Den andra personen som bor på samma adress i ett äktenskaps/samboförhållande',
    partnerAdd: 'Lägg till maka/sambo',
    financeNotesAndMessages: {
      title: 'Hushållets anteckningar och meddelanden',
      addNote: 'Lägg till anteckning',
      sendMessage: 'Skicka eVaka-meddelande',
      noMessaging:
        'eVaka-meddelande kan endast skickas till person med personbeteckning.',
      link: 'Länk till ursprungligt meddelande',
      showMessages: 'Visa alla meddelanden',
      hideMessages: 'Dölj alla meddelanden',
      confirmDeleteNote: 'Vill du verkligen ta bort anteckningen',
      confirmArchiveThread:
        'Vill du verkligen flytta meddelandetråden till arkivet',
      note: 'Anteckning',
      created: 'Skapad',
      inEdit: 'Under redigering'
    },
    forceManualFeeDecisionsLabel: 'Skickning av avgiftsbeslut',
    forceManualFeeDecisionsChecked: 'Skickas alltid manuellt',
    forceManualFeeDecisionsUnchecked: 'Automatiskt, om möjligt',
    fridgeChildOfHead: 'Huvudmannens underordnade barn under 18 år',
    fridgeChildAdd: 'Lägg till barn',
    fosterChildren: {
      sectionTitle: 'Fosterbarn',
      addFosterChildTitle: 'Lägg till nytt fosterbarn',
      addFosterChildParagraph:
        'Fosterförälder ser samma uppgifter om barnet i eVaka som vårdnadshavaren. Fosterbarn får endast läggas till med socialarbetarens tillstånd.',
      updateFosterChildTitle: 'Uppdatera relationens giltighetstid',
      childLabel: 'Personbeteckning eller namn',
      validDuringLabel: 'Giltigt',
      createError: 'Det gick inte att lägga till fosterbarn',
      deleteFosterChildTitle: 'Borttagning av fosterbarn',
      deleteFosterChildParagraph:
        'Vill du verkligen ta bort fosterbarnet? När fosterföräldraskapet upphör, markera en sluttid för relationen.'
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
      noteModalInfo: 'Anteckningen kommer inte att synas på fakturan.',
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
    dependants: 'Huvudmannens barn under vårdnad',
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
          Med eVaka-rättigheter bestäms om vårdnadshavaren kan se uppgifter som
          gäller barnet i eVaka. Rättigheterna kan förbjudas{' '}
          <strong>
            till exempel på grund av en anmälan från en socialarbetare eller
            annan behörig myndighet
          </strong>
          . Rättigheterna ska återställas om det inte längre finns grund för
          förbudet.
        </>
      ),
      modalUpdateSubtitle:
        'Förbud av vårdnadshavarens eVaka-rättigheter när barnet är omhändertaget',
      confirmedLabel:
        'Jag bekräftar att begränsningen av vårdnadshavarens rätt till information har socialarbetarens skriftliga tillstånd',
      deniedLabel:
        'Jag förbjuder eVaka rättigheter för vårdnadshavaren för det omhändertagna barnet'
    },
    familyOverview: {
      title: 'Sammandrag av familjens uppgifter',
      colName: 'Namn',
      colRole: 'Roll i familjen',
      colAge: 'Ålder',
      colIncome: 'Inkomster',
      colAddress: 'Adress',
      role: {
        HEAD: 'Huvudman',
        PARTNER: 'Make/maka',
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
      newPartner: 'Ny make/maka',
      editPartner: 'Redigering av make/maka',
      removePartner: 'Borttagning av make/maka',
      confirmText:
        'Vill du verkligen ta bort make/maka? När make/maka byts ska föregående förhållande markeras med ett slutdatum och därefter läggs en ny make/maka till',
      error: {
        remove: {
          title: 'Borttagning av make/maka misslyckades!'
        },
        add: {
          title: 'Tillägg av make/maka misslyckades!'
        },
        edit: {
          title: 'Redigering av make/maka misslyckades!'
        },
        conflict:
          'Parterna har ett aktivt förhållande under den angivna tidsperioden. Nuvarande aktiva förhållande måste avslutas innan ett nytt skapas'
      },
      validation: {
        deadPerson:
          'Förhållandets slutdatum kan inte vara efter personens dödsdag',
        deadPartner:
          'Förhållandets slutdatum kan inte vara efter makens/makans dödsdag'
      },
      searchTitle: 'Personbeteckning eller namn'
    },
    fridgeChild: {
      newChild: 'Nytt barn',
      editChild: 'Redigering av barn',
      removeChild: 'Borttagning av barn',
      confirmText:
        'Vill du verkligen ta bort barnet? När huvudman byts ska föregående förhållande markeras med ett slutdatum och därefter läggs ett nytt till',
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
          'För detta barn finns redan en huvudman under denna tidsperiod. Befintligt huvudmannaförhållande måste avslutas först'
      },
      validation: {
        deadAdult:
          'Förhållandets slutdatum kan inte vara efter den vuxnas dödsdag',
        deadChild: 'Förhållandets slutdatum kan inte vara efter barnets dödsdag'
      },
      searchTitle: 'Personbeteckning eller namn'
    },
    application: {
      child: 'Barn',
      preferredUnit: 'Ansökt enhet',
      startDate: 'Ansökt startdatum',
      sentDate: 'Ansökans ankomstdatum',
      type: 'Serviceform',
      types: {
        PRESCHOOL: 'Förskola',
        PRESCHOOL_WITH_DAYCARE: 'Förskola + ansluten',
        PRESCHOOL_DAYCARE: 'Ansluten småbarnspedagogik',
        PRESCHOOL_CLUB: 'Förskoleklubb',
        PREPARATORY_EDUCATION: 'Förberedande undervisning',
        PREPARATORY_WITH_DAYCARE: 'Förberedande undervisning + ansluten',
        DAYCARE: 'Småbarnspedagogik',
        DAYCARE_PART_TIME: 'Småbarnspedagogik',
        CLUB: 'Klubbverksamhet'
      },
      status: 'Status',
      open: 'Öppna ansökan',
      statuses: {
        CREATED: 'Utkast',
        SENT: 'Anländ',
        WAITING_PLACEMENT: 'Väntar på placering',
        WAITING_DECISION: 'Beslutsförberedelse',
        WAITING_UNIT_CONFIRMATION: 'Väntar på ledarens godkännande',
        WAITING_MAILING: 'Väntar på postning',
        WAITING_CONFIRMATION: 'Bekräftas av vårdnadshavare',
        REJECTED: 'Plats avslagen',
        ACTIVE: 'Plats mottagen',
        CANCELLED: 'Borttagen från handläggning'
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
        REJECTED: 'Avslagen'
      },
      archive: 'Arkivera',
      startDate: 'Startdatum enligt beslut',
      sentDate: 'Beslut skickat'
    },
    income: {
      title: 'Inkomstuppgifter',
      itemHeader: 'Inkomstuppgifter för tiden',
      itemHeaderNew: 'Ny inkomstuppgift',
      lastModifiedAt: (date: string) => `Senast redigerad ${date}`,
      lastModifiedBy: (name: string) => `Redigerare: ${name}`,
      details: {
        attachments: 'Bilagor',
        name: 'Namn',
        created: 'Inkomstuppgifter skapade',
        handler: 'Handläggare',
        originApplication:
          'Vårdnadshavare har i ansökan samtyckt till högsta avgiftsklass',
        dateRange: 'För tiden',
        notes: 'Tilläggsuppgifter',
        effect: 'Grund för avgift',
        effectOptions: {
          MAX_FEE_ACCEPTED:
            'Vårdnadshavare har samtyckt till högsta avgiftsklass',
          INCOMPLETE: 'Bristfälliga inkomstuppgifter',
          INCOME: 'Vårdnadshavarens inlämnade inkomstuppgifter',
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
        time: 'För tiden',
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
          DAILY_ALLOWANCE_21_5: 'Dagtraktamente x 21,5',
          DAILY_ALLOWANCE_25: 'Dagtraktamente x 25',
          YEARLY: 'År'
        },
        updateError: 'Sparande av inkomstuppgifter misslyckades',
        missingIncomeDaysWarningTitle:
          'Inkomstuppgifter saknas för vissa dagar',
        missingIncomeDaysWarningText: (missingIncomePeriodsString: string) =>
          `Inkomstuppgifter saknas för följande dagar: ${missingIncomePeriodsString}. Om inkomstuppgifter inte läggs till, bestäms inkomsterna för dessa dagar enligt den högsta avgiftsklassen. Kontrollera datum och lägg till inkomstuppgifter för de saknade dagarna vid behov.`,
        conflictErrorText:
          'För tidsperioden har redan inkomstuppgifter sparats! Kontrollera inkomstuppgifternas giltighetstider.',
        closeWarning: 'Kom ihåg att spara!',
        closeWarningText:
          'Spara eller avbryt ändringar innan formuläret stängs.'
      },
      add: 'Skapa ny inkomstuppgift',
      deleteModal: {
        title: 'Borttagning av inkomstuppgift',
        confirmText: 'Vill du verkligen ta bort inkomstuppgiften för tiden',
        cancelButton: 'Avbryt',
        deleteButton: 'Ta bort'
      }
    },
    incomeStatement: {
      title: 'Inkomstutredningar',
      notificationsTitle: 'Påminnelser om att göra inkomstutredning',
      custodianTitle:
        'Inkomstutredningar för barn som står under klientens vårdnad',
      noIncomeStatements: 'Inga inkomstutredningar',
      incomeStatementHeading: 'Klientens inkomstutredningsblankett',
      sentAtHeading: 'Ankomstdatum',
      handledHeading: 'Behandlad',
      open: 'Öppna blankett',
      handled: 'Inkomstutredning behandlad',
      notificationSent: 'Skickad',
      noNotifications: 'Inga skickade påminnelser',
      notificationTypes: {
        INITIAL_EMAIL: 'Första påminnelsen',
        REMINDER_EMAIL: 'Andra påminnelsen',
        EXPIRED_EMAIL: 'Inkomster avslutade',
        NEW_CUSTOMER: 'Startande klient'
      },
      noCustodians: 'Inga barn som står under vårdnad'
    },
    invoice: {
      createReplacementDrafts: 'Skapa korrigeringsfakturor',
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
    partner: 'Make/maka',
    child: 'Barn',
    createdAtTitle: 'Skapad',
    unknownSource: 'Okänt vem som skapat informationen',
    modifiedAtTitle: 'Redigerad',
    unknownModification: 'Redigerare okänd',
    notModified: 'Inte redigerad',
    user: 'Användare',
    application: 'Ansökan',
    dvvSync: 'Befolkningsdatasystemet',
    notAvailable: 'Tid okänd',
    DVV: 'Synkronisering av befolkningsdatasystemet'
  },
  incomeStatement: {
    startDate: 'Gäller från och med',
    feeBasis: 'Grund för klientavgift',

    grossTitle: 'Bruttoinkomster',
    noIncomeTitle:
      'Inga inkomster eller stöd, uppgifter kan kontrolleras från inkomstregistret och FPA',
    noIncomeDescription: 'Beskriv din situation noggrannare',
    incomeSource: 'Leverans av uppgifter',
    incomesRegister:
      'Jag  samtycker till att uppgifterna som rör mina inkomster kontrolleras i inkomstregistret samt hos FPA vid behov',
    attachmentsAndKela:
      'Jag lämnar in uppgifterna som bilagor och mina uppgifter kan kontrolleras från FPA',
    grossEstimatedIncome: 'Uppskattning av min bruttolön',
    otherIncome: 'Övriga inkomster',
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
      JOB_ALTERNATION_COMPENSATION: 'Alterneringsersättning',
      REWARD_OR_BONUS: 'Arvode eller bonus',
      RELATIVE_CARE_SUPPORT: 'Stöd för närståendevård',
      BASIC_INCOME: 'Basinkomst',
      FOREST_INCOME: 'Skogsinkomst',
      FAMILY_CARE_COMPENSATION: 'Arvoden för familjevård',
      REHABILITATION: 'Rehabiliteringsstöd eller rehabiliteringspenning',
      EDUCATION_ALLOWANCE: 'Utbildningsdagpenning',
      GRANT: 'Stipendium',
      APPRENTICESHIP_SALARY: 'Löneinkomst från läroavtalsutbildning',
      ACCIDENT_INSURANCE_COMPENSATION: 'Ersättning från olycksfallsförsäkring',
      OTHER_INCOME: 'Övriga inkomster'
    },
    otherIncomeInfo: 'Uppskattning av övriga inkomster',

    entrepreneurTitle: 'Företagarens inkomstuppgifter',
    startOfEntrepreneurship: 'Entrepenörskap har börjat',
    companyName: 'Företagets / företagens namn',
    businessId: 'FO-numret/ FO-numren',
    spouseWorksInCompany: 'Arbetar make/maka i företaget',
    startupGrant: 'Startpeng',
    companyInfoTitle: 'Företagets uppgifter',
    checkupConsentLabel: 'Kontroll av uppgifter',
    checkupConsent:
      'Jag samtycker till att uppgifter som rör mina inkomster kontrolleras i inkomstregistret samt FPA vid behov',
    companyType: 'Företagets verksamhetsform',
    selfEmployed: 'Firma namn',
    selfEmployedAttachments:
      'Jag bifogar företagets senaste resultat -och balansräkning eller skattebeslut.',
    selfEmployedEstimation:
      'Uppskattning av genomsnittliga inkomster euro/månad',
    limitedCompany: 'Aktiebolag',
    limitedCompanyIncomesRegister:
      'Mina inkomster kan kontrolleras direkt hos FPA och i inkomstregistret.',
    limitedCompanyAttachments:
      'Jag  bifogar verifikaten över mina inkomster och samtycker till att uppgifter som rör mina inkomster kontrolleras hos från FPA.',
    partnership: 'Öppet bolag eller kommanditbolag',
    lightEntrepreneur: 'Lätt företagande',
    attachments: 'Bilagor',

    estimatedMonthlyIncome: 'Genomsnittliga inkomster €/mån',
    timeRange: 'Under tidsperioden',

    accountantTitle: 'Bokförarens kontaktuppgifter',
    accountant: 'Bokförare',
    email: 'E-postadress',
    phone: 'Telefonnummer',
    address: 'Postadress',

    otherInfoTitle: 'Mer information om inkomstuppgifter',
    student: 'Jag är studerande',
    alimonyPayer: 'Jag betalar underhållsbidrag',
    otherInfo: 'Tilläggsuppgifter relaterade till inkomstuppgifter',

    citizenAttachments: {
      title: 'Bilagor relaterade till inkomster och småbarnspedagogikavgifter',
      noAttachments: 'Inga bilagor',
      attachmentMissing: 'Bilaga saknas'
    },

    employeeAttachments: {
      title: 'Lägg till bilagor',
      description:
        'Här kan du lägga till bilagor som klienten har lämnat i pappersform till inkomstutredningen som returneras via eVaka.'
    },

    statementTypes: {
      HIGHEST_FEE:
        'Jag samtycker till den högsta avgiften för småbarnspedagogik',
      INCOME: 'Vårdnadshavarens inlämnade inkomstuppgifter',
      CHILD_INCOME: 'Barnets inkomstuppgifter'
    },
    table: {
      title: 'Inkomstutredningar som väntar på behandling',
      customer: 'Klient',
      area: 'Område',
      sentAt: 'Skickad',
      citizenModifiedAt: 'Redigerad',
      startDate: 'Gäller fr.o.m.',
      incomeEndDate: 'Inkomstuppgift upphör',
      type: 'Typ',
      link: 'Utredning',
      note: 'Anteckning'
    },
    noNote: 'Ingen anteckning på inkomstutredningen',
    handlerNotesForm: {
      title: 'Behandling',
      statusLabel: 'Status',
      status: {
        SENT: 'Väntar på behandling',
        HANDLING: 'Under behandling',
        HANDLED: 'Behandlad'
      },
      statusInfo:
        'När inkomstutredningen är under behandling kan kommuninvånaren inte återkalla ansökan, men kan lägga till saknade bilagor.',
      startHandlingBtn: 'Ta under behandling',
      markHandledBtn: 'Markera som behandlad',
      returnToHandlingBtn: 'Återför till behandling',
      handlerNote: 'Anteckning (intern)'
    },
    attachmentNames: {
      OTHER: 'Annan bilaga',
      PENSION: 'Beslut om pension',
      ADULT_EDUCATION_ALLOWANCE: 'Beslut om vuxenutbildningsstöd',
      SICKNESS_ALLOWANCE: 'Beslut om sjukdagpenning',
      PARENTAL_ALLOWANCE: 'Beslut om moderskaps- eller föräldrapenning',
      HOME_CARE_ALLOWANCE: 'Beslut om hemvårdsstöd',
      FLEXIBLE_AND_PARTIAL_HOME_CARE_ALLOWANCE: 'Beslut om vårdpenning',
      ALIMONY: 'Underhållsavtal eller beslut om underhållsstöd',
      UNEMPLOYMENT_ALLOWANCE: 'Beslut om arbetslöshetsdagpenning',
      LABOUR_MARKET_SUBSIDY: 'Beslut om arbetsmarknadsstöd',
      ADJUSTED_DAILY_ALLOWANCE: 'Beslut om dagpenning',
      JOB_ALTERNATION_COMPENSATION:
        'Alterneringsledighetsersättnings verifikat',
      REWARD_OR_BONUS: 'Löneverifikat för bonus eller/och arvode',
      RELATIVE_CARE_SUPPORT: 'Beslut om stöd för närståendevård',
      BASIC_INCOME: 'Beslut om basinkomst',
      FOREST_INCOME: 'Verifikat för skogsinkomst',
      FAMILY_CARE_COMPENSATION: 'Verifikat om arvode för familjevård',
      REHABILITATION:
        'Beslut om rehabiliteringsstöd eller rehabiliteringspenning',
      EDUCATION_ALLOWANCE: 'Beslut om utbildningsdagpenning',
      GRANT: 'Verifikat för stipendium',
      APPRENTICESHIP_SALARY:
        'Verifikat för löneinkomster från läroavtalsutbildning',
      ACCIDENT_INSURANCE_COMPENSATION:
        'Verifikat för ersättning från olycksfallsförsäkring',
      OTHER_INCOME: 'Bilagor för övriga  inkomster',
      ALIMONY_PAYOUT: 'Betalningsverifikat för underhållsbidrag',
      INTEREST_AND_INVESTMENT_INCOME:
        'Verifikat för ränte- och dividendinkomster',
      RENTAL_INCOME: 'Verifikat för hyresinkomster och vederlag',
      PAYSLIP_GROSS: 'Senaste lönespecifikation',
      STARTUP_GRANT: 'Beslut för startpeng',
      ACCOUNTANT_REPORT_PARTNERSHIP:
        'Bokförarens utredning om lön och naturaförmåner',
      PAYSLIP_LLC: 'Senaste lönespecifikation',
      ACCOUNTANT_REPORT_LLC:
        'Bokförarens utredning om naturaförmåner och dividender',
      PROFIT_AND_LOSS_STATEMENT_SELF_EMPLOYED:
        'Resultat- och balansräkning eller skattebeslut',
      PROFIT_AND_LOSS_STATEMENT_PARTNERSHIP: 'Resultat- och balansräkning',
      SALARY: 'Betalningsverifikat för löner och arbetsersättningar',
      PROOF_OF_STUDIES:
        'Studieintyg eller beslut om studiestöd från arbetslöshetskassa / utbildningsstöd från sysselsättningsfonden',
      CHILD_INCOME: 'Verifikat för barnets inkomster'
    }
  },
  units: {
    name: 'Namn',
    area: 'Område',
    address: 'Adress',
    city: 'Kommun',
    type: 'Typ',
    findByName: 'Sök efter enhetens namn',
    selectProviderTypes: 'Välj ordningsform',
    selectCareTypes: 'Välj verksamhetsform',
    includeClosed: 'Visa nedlagda enheter',
    noResults: 'Inga resultat'
  },
  unit: {
    serviceWorkerNote: {
      title: 'Servicestyrets anteckningar',
      add: 'Sätt anteckning'
    },
    tabs: {
      unitInfo: 'Enhetens uppgifter',
      groups: 'Grupper',
      calendar: 'Kalender',
      applicationProcess: 'Ansökningsprocess'
    },
    create: 'Skapa ny enhet',
    openDetails: 'Visa enhetens alla uppgifter',
    occupancies: 'Belastnings- och beläggningsgrad',
    info: {
      title: 'Enhetens uppgifter',
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
      title: 'Enhetsledare',
      name: 'Namn',
      email: 'E-postadress',
      phone: 'Telefonnummer'
    },
    accessControl: {
      aclRoles: 'Tillstånd',
      activeAclRoles: 'Aktiva tillstånd',
      roleChange: 'Rollbyte',
      scheduledAclRoles: 'Kommande tillstånd',
      role: 'Roll',
      name: 'Namn',
      email: 'E-postadress',
      aclStartDate: 'Tillstånd börjar',
      aclEndDate: 'Tillstånd upphör',
      removeConfirmation:
        'Vill du ta bort åtkomsträttigheterna från den valda personen?',
      removeScheduledConfirmation: 'Vill du ta bort det kommande tillståndet?',
      addDaycareAclModal: {
        title: 'Lägg till tillstånd',
        role: 'Välj roll',
        employees: 'Välj person',
        scheduledAclWarning:
          'Personen har en kommande rättighet i denna enhet. Den kommande rättigheten tas bort.'
      },
      editDaycareAclModal: {
        title: 'Redigera tillstånd'
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
      reactivateTemporaryEmployee: 'Ge tillstånd på nytt',
      removeTemporaryEmployeeConfirmation:
        'Vill du ta bort den valda personen från listan?',
      mobileDevices: {
        mobileDevices: 'Enhetens mobilenheter',
        addMobileDevice: 'Lägg till mobilenhet',
        editName: 'Redigera enhetens namn',
        removeConfirmation: 'Vill du ta bort mobilenheten?',
        editPlaceholder: 't.ex. Jordgubbarnas mobil'
      },
      groups: 'Tillstånd till grupper',
      noGroups: 'Inga tillstånd',
      hasOccupancyCoefficient: 'Vårdansvarig'
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
        draft: 'Utkast för beläggningsgrad',
        realized: 'Belastningsgrad'
      },
      fail: 'Inläsning av beläggningsgrad misslyckades',
      failRealized: 'Inläsning av användningsgrad misslyckades',
      maximum: 'Maximum',
      minimum: 'Minimum',
      noValidValues: 'Beläggningsgrad kunde inte beräknas för tidsperioden',
      noValidValuesRealized:
        'Belastningsgrad kunde inte beräknas för tidsperioden',
      realtime: {
        modes: {
          REALIZED: 'Utfall',
          PLANNED: 'Plan'
        },
        noData: 'Inga uppgifter för vald dag',
        legendTitle: 'Förklaringar till anteckningar',
        chartYAxisTitle: 'Barn med koefficienter',
        chartY1AxisTitle: 'Personal',
        staffPresent: 'Antal anställda',
        staffRequired: 'Behovet av anställda',
        childrenMax: 'Maximalt antal barn (med koefficient)',
        childrenPresent: 'Antal barn',
        children: 'Antal barn (med koefficient)',
        unknownChildren: '+ barn utan reservation',
        utilization: 'Belastningsgrad'
      }
    },
    staffOccupancies: {
      title: 'Vårdansvar',
      occupancyCoefficientEnabled: 'Inräknad i belastningsgraden'
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
      }) => `${accepted} godkänns, ${rejected} avslås`,
      acceptAllButton: 'Bekräfta val',
      application: 'Ansökan',
      birthday: 'Födelsedatum',
      citizenHasRejectedPlacement: 'Plats avslagen',
      confirmation: 'Godkännande',
      describeOtherReason: 'Skriv motivering',
      infoText:
        'Markera de barn som du kan ta emot. När du har godkänt alla barn kan du trycka på knappen Bekräfta godkända. Om du inte kan godkänna alla barn, markera kryssrutan och lägg till en motivering. Servicehandleningen gör då ett nytt placeringsförslag eller kontaktar dig.',
      infoTitle: 'Markering som godkänd / avvisad',
      name: 'Namn',
      placementDuration: 'Placerad i enheten',
      rejectTitle: 'Välj anledning för återlämning',
      rejectReasons: {
        REASON_1:
          'UTRYMMESBEGRÄNSNING, avtalat med ledande ledare för småbarnspedagogik.',
        REASON_2:
          'ENHETENS HELHETSSITUATION, avtalat med den ledande ledaren för småbarnspedagogik.',
        REASON_3: '',
        OTHER: 'Annan orsak'
      },
      statusLastModified: (name: string, date: string) =>
        `Senast redigerad ${date}. Redigerare: ${name}`,
      subtype: 'Del/Hel',
      title: 'Placeringsförslag',
      type: 'Placeringstyp',
      unknown: 'Ej känd'
    },
    applications: {
      title: 'Ansökningar',
      child: 'Barnets namn/födelsedatum',
      guardian: 'Vårdnadshavare som ansökt',
      type: 'Placeringstyp',
      types: {
        CLUB: 'Klubbverksamhet',
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
      title: 'Sökande till förflyttning till annan plats',
      child: 'Barnets namn/födelsedatum',
      startDate: 'Önskemål om startdatum, ännu ingen placering'
    },
    serviceApplications: {
      title: 'Ansökningar om ändring av servicebehov som väntar på behandling',
      child: 'Barn',
      range: 'För tiden',
      newNeed: 'Nytt behov',
      currentNeed: 'Nuvarande behov',
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
        transferTitle: 'Barnets överföring till annan grupp',
        child: 'Barn som ska placeras',
        group: 'Grupp',
        errors: {
          noGroup: 'Du har inte valt grupp eller det finns inga aktiva grupper',
          noStartDate: 'Du har inte valt startdatum',
          noEndDate: 'Du har inte valt slutdatum',
          groupNotStarted: 'Gruppen har inte börjat ännu',
          groupEnded: 'Gruppen har redan upphört'
        }
      }
    },
    termination: {
      title: 'Placeringar som upphör',
      info: 'På listan visas de barn för vilka vårdnadshavaren har gjort en uppsägningsanmälan under de föregående två veckorna, eller som har en av vårdnadshavaren godkänd överföringsansökan till en annan enhet. Barn som har en placering som upphör av annan orsak visas inte på denna lista.',
      terminationRequestedDate: 'Uppsägningsdag',
      endDate: 'Slutdatum',
      groupName: 'Grupp'
    },
    calendar: {
      title: 'Kalender',
      noGroup: 'Ingen grupp',
      shiftCare: 'Skiftomsorg',
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
          `Senast redigerad ${date}; redigerare: ${name}`,
        lastModifiedAt: 'Senast redigerad',
        lastModifiedBy: 'Redigerare',
        edit: {
          title: 'Händelse',
          saveChanges: 'Spara ändringar',
          delete: 'Ta bort händelse'
        },
        create: {
          title: 'Lägg till ny händelse',
          text: 'Lägg här till de händelser som är viktiga för vårdnadshavaren att komma ihåg: händelsen kommer att synas i vårdnadshavarens eVaka-kalender. För andra typer av händelser är det bra att informera vårdnadshavaren via ett meddelande.',
          add: 'Lägg till händelse',
          period: 'Tidpunkt',
          attendees: 'Händelsens deltagare',
          attendeesPlaceholder: 'Välj...',
          eventTitle: 'Händelsens rubrik',
          eventTitlePlaceholder: 'Max. 30 tecken',
          description: 'Händelsens beskrivning',
          descriptionPlaceholder:
            'Korta instruktioner till vårdnadshavaren, t.ex. klockslag, vad som ska packas med',
          missingPlacementsWarning:
            'En del av de valda barnen har ingen placering i nuvarande enhet eller är inte placerade i den valda gruppen under händelsen. På dessa dagar listas inte barnet som deltagare och vårdnadshavaren visas inte händelsen i kalendern.',
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
            'På denna sida kan du skapa och följa upp förfrågningar där du frågar vårdnadshavare om lämpliga samtalstider.',
          surveyCreate: 'Ny samtalsförfrågan',
          surveyBasicsTitle: 'Grunduppgifter',
          surveyPeriod: 'Förfrågningens varaktighet',
          surveySubject: 'Samtalets ämne',
          surveyInvitees: 'Samtalets deltagare',
          surveySummary: 'Ytterligare information till vårdnadshavaren',
          surveySummaryCalendarLabel: 'Ytterligare information',
          surveySummaryInfo:
            'Denna text visas för vårdnadshavaren i samband med förfrågan. Du kan berätta ytterligare information om samtalen, till exempel ankomstinstruktioner eller den tid som reserverats för samtalet.',
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
            text: 'Dina ändringar sparas inte',
            cancelButton: 'Fortsätt redigera',
            continueButton: 'Avbryt ändringar'
          },
          surveyModifiedAt: 'Redigerad',
          surveyStatus: {
            SENT: 'Skickad',
            ENDED: 'Avslutad'
          },
          reservedTitle: 'Reserverade',
          reserveButton: 'Reservera',
          unreservedTitle: 'Oreserverade',
          calendarSurveySummary: (
            link: (text: string) => React.ReactNode
          ): React.ReactNode => (
            <>
              För mer detaljerad information,{' '}
              {link('gå till granskningsvyn för diskussionsenkäten')}
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
            reserveError: 'Reservering av samtalstid misslyckades',
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
            text: 'Alla lediga och reserverade tider tas bort. Denna åtgärd kan inte ångras.',
            error: 'Borttagning av samtalsförfrågan misslyckades'
          },
          eventTime: {
            addError: 'Tillägg av samtalstid misslyckades',
            deleteError: 'Borttagning av samtalstid misslyckades'
          },
          reservationClearConfirmationTitle:
            'Ska följande reservationer tas bort?',
          clearReservationButtonLabel: 'Ta bort reservationer'
        },
        reservedTimesLabel: 'reserverade',
        freeTimesLabel: 'lediga'
      }
    },
    groups: {
      title: 'Verksamhetsställets grupper',
      familyContacts: 'Visa sammanställning av kontaktuppgifter',
      attendanceReservations: 'Närvaroreservationer',
      create: 'Skapa ny grupp',
      createModal: {
        title: 'Ny grupp',
        confirmButton: 'Spara',
        cancelButton: 'Avbryt',
        name: 'Gruppens namn',
        type: 'Typ',
        initialCaretakers: 'Antal personal när gruppen börjar',
        aromiCustomerId: 'Aromis ansvarsenhets kod',
        errors: {
          nameRequired: 'Gruppen måste ha ett namn',
          aromiWarning:
            'Om Aromis ansvarsenhetskod saknas, ingår inte gruppmedlemmarna i matbeställningen',
          initialCaretakersPositive: 'Antal personal kan inte vara negativt'
        }
      },
      updateModal: {
        title: 'Redigera gruppens uppgifter',
        name: 'Namn',
        startDate: 'Grundad',
        endDate: 'Sista verksamhetsdag',
        info: 'Gruppens tidigare uppgifter sparas inte',
        jamixPlaceholder: 'Jamix customerNumber',
        jamixTitle: 'Matbeställningarnas kundnummer',
        aromiPlaceholder: 'Aromis ansvarsenhetskod',
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
      maxRealizedOccupancy: 'Högsta användningsgrad',
      name: 'Namn',
      birthday: 'Födelsedatum',
      placementDuration: 'Placerad i gruppen',
      serviceNeed: 'Servicebehov',
      serviceNeedChecked: 'Servicebehov markerat',
      serviceNeedMissing1: 'Servicebehov saknas (',
      serviceNeedMissing2: 'dagar)',
      placementType: 'Placeringstyp',
      placementSubtype: 'Del/Hel',
      noChildren: 'Inga barn är placerade i gruppen.',
      returnBtn: 'Returnera',
      transferBtn: 'Flytta',
      diaryButton: 'Öppna dagbok',
      deleteGroup: 'Ta bort grupp',
      update: 'Redigera uppgifter',
      nekkuOrder: 'Nekku-beställning',
      daycareDailyNote: {
        dailyNote: 'Dagens anteckningar',
        header: 'Idag upplevt och lärt',
        groupNotesHeader: 'Gruppens anteckningar',
        stickyNotesHeader: 'Att uppmärksamma under närmaste dagarna',
        notesHint:
          'Lekar, lyckanden, glädjeämnen och inlärda saker idag (inga hälsouppgifter eller sekretessbelagda uppgifter).',
        childStickyNoteHint:
          'Anteckning för personalen (inga hälsouppgifter eller sekretessbelagda uppgifter).',
        otherThings: 'Övriga ärenden',
        feedingHeader: 'Barnet åt idag',
        sleepingHeader: 'Barnet sov idag',
        sleepingHoursHint: 'timmar',
        sleepingMinutesHint: 'minuter',
        sleepingHours: 'tim',
        sleepingMinutes: 'min',
        reminderHeader: 'Saker att komma ihåg',
        otherThingsToRememberHeader: 'Annat att komma ihåg (t.ex. solkräm)',
        groupNoteModalLink: 'Gruppens anteckning',
        groupNoteHint: 'Anteckning som gäller hela gruppen',
        edit: 'Lägg till dagens anteckning',
        level: {
          GOOD: 'Väl',
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
        createModalLink: 'Skicka handling',
        createModal: {
          title: 'Skicka handling till flera mottagare',
          template: 'Handling',
          placements: 'Mottagare'
        }
      }
    },
    backupCares: {
      title: 'Barn med reservplacering',
      childName: 'Namn',
      duration: 'Placerad i enheten',
      birthDate: 'Födelsedatum'
    },
    attendanceReservations: {
      ungrouped: 'Barn utan grupp',
      childName: 'Barnets namn',
      startTime: 'Anländer',
      endTime: 'Avgår',
      requiresBackupCare: 'Gör reservplacering',
      openReservationModal: 'Gör återkommande reservation',
      childCount: 'Barn närvarande',
      lastModifiedStaff: (date: string, name: string) => (
        <div>
          <p>*Anteckning gjord av personal</p>
          <p>
            Senast redigerad {date}; redigerare: {name}
          </p>
        </div>
      ),
      lastModifiedOther: (date: string, name: string) =>
        `Senast redigerad ${date}; redigerare: ${name}`,
      reservationModal: {
        title: 'Gör reservation',
        selectedChildren: 'Barn för vilka reservation görs',
        dateRange: 'Reservationens giltighet',
        dateRangeLabel: 'Gör reservation för dagar',
        missingDateRange: 'Välj dagar att reservera',
        repetition: 'Typ eller återkommande',
        times: 'Klockslag',
        businessDays: 'Må-Fr',
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
          noTimes: 'Närvarande, klockslag ännu inte känd'
        },
        attendances: {
          title: 'Närvaroutfall',
          add: 'Lägg till ny rad'
        },
        absences: {
          title: 'Frånvaro',
          add: {
            BILLABLE: 'Markera frånvaro från småbarnspedagogik',
            NONBILLABLE: 'Markera frånvaro från avgiftsfri verksamhet'
          },
          label: {
            BILLABLE: 'Frånvarande från småbarnspedagogik, orsak:',
            NONBILLABLE: 'Frånvarande från avgiftsfri verksamhet, orsak:'
          }
        },
        overlapWarning: 'Kontrollera överlappning',
        absenceWarning: 'Kontrollera frånvaro',
        extraNonbillableAbsence:
          'Enligt närvarotider var barnet närvarande i avgiftsfri verksamhet.',
        missingNonbillableAbsence:
          'Enligt närvarotider var barnet inte närvarande i avgiftsfri verksamhet.',
        extraBillableAbsence:
          'Enligt närvarotider var barnet närvarande i avgiftsbelagd småbarnspedagogik.',
        missingBillableAbsence:
          'Enligt närvarotider var barnet inte närvarande i avgiftsbelagd småbarnspedagogik.',
        errorCodes: {
          attendanceInFuture: 'Närvaro kan inte vara i framtiden'
        }
      },
      reservationNoTimes: 'Närvarande',
      missingHolidayReservation: 'Semesterbokning saknas',
      missingHolidayReservationShort: 'Semesterbokning saknas',
      fixedSchedule: 'Närvarande',
      termBreak: 'Ingen verksamhet',
      missingReservation: 'Anmälan saknas',
      serviceTimeIndicator: '(s)',
      legend: {
        reservation: 'Bokning',
        serviceTime: 'Avtalsid',
        attendanceTime: 'Ankomst-/avgångstid',
        hhmm: 'tt:mm'
      },
      affectsOccupancy: 'Räknas i belastningsgraden',
      doesNotAffectOccupancy: 'Räknas inte i belastningsgraden',
      inOtherUnit: 'I annan enhet',
      inOtherGroup: 'I annan grupp',
      createdByEmployee: '*Anteckning gjord av personal'
    },
    staffAttendance: {
      startTime: 'ankomst',
      endTime: 'avgång',
      summary: 'Sammandrag',
      plan: 'Plan',
      realized: 'Utfall',
      hours: 'Timmar',
      dailyAttendances: 'Dagens anteckningar',
      continuationAttendance: '* anteckning som påbörjats föregående dag',
      addNewAttendance: 'Lägg till ny anteckning',
      saveChanges: 'Spara ändringar',
      noGroup: 'Ingen grupp',
      staffName: 'Arbetstagarens namn',
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
        'Timmar kan inte räknas eftersom sista avgångstiden saknas i dagens anteckningar.',
      gapWarning: (gapRange: string) =>
        `Registrering saknas mellan ${gapRange}`,
      openAttendanceWarning: (arrival: string) =>
        `Öppen registrering ${arrival}`,
      openAttendanceInAnotherUnitWarning: 'Öppen anteckning',
      openAttendanceInAnotherUnitWarningCont:
        '. Anteckningen måste avslutas innan en ny kan läggas till.',
      personCount: 'Närvarandes totala antal',
      personCountAbbr: 'pers.',
      unlinkOvernight: 'Separera närvaro som går över natten',
      previousDay: 'Föregående dag',
      nextDay: 'Följande dag',
      addPersonModal: {
        description:
          'Lägg till en tillfälligt närvarande person och välj om hen ska räknas med i belastningsgraden.',
        arrival: 'Ankomsttid',
        name: 'Namn',
        namePlaceholder: 'Efternamn Förnamn',
        group: 'Grupp'
      },
      addedAt: 'Anteckning skapad',
      modifiedAt: 'Redigerad',
      departedAutomatically: 'Automatiskt avbruten',
      departedAutomaticallyBanner: (count: number) =>
        `${count} automatiska avbrott av närvaro denna vecka.`,
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
    info: 'Skapa alltid ett nytt personalbehov när antalet personal ändras. Angivet antal är i kraft under vald period och påverkar enhetens och gruppens fyllnadsgrader.',
    create: 'Skapa nytt personalbehov',
    edit: 'Redigera uppgifter',
    editActiveWarning:
      'Du redigerar uppgifter för en pågående period. Om ändringen i personalantalet gäller en annan tidsperiod, skapa ett nytt personalbehov så att historikuppgifterna bevaras.',
    editHistoryWarning:
      'Du redigerar uppgifter för en avslutad period. Om ändringen i personalantalet gäller en annan tidsperiod, skapa ett nytt personalbehov så att historikuppgifterna bevaras.',
    confirmDelete: 'Vill du verkligen ta bort personalbehovet?',
    startDate: 'Fr.o.m.',
    endDate: 'T.o.m.',
    amount: 'Personalbehov',
    amountUnit: 'Personer',
    status: 'Status',
    conflict:
      'Under vald period finns överlappning med en tidigare skapad period. Ta bort överlappningen genom att redigera den andra perioden.'
  },
  personalMobileDevices: {
    title: 'Personlig eVaka-mobil',
    infoParagraph1:
      'På den här sidan kan du ställa in en mobilenhet för ditt eget personliga bruk, med vilken du granskar alla dina enheters uppgifter i eVaka. Du kan också vid behov ta bort eller lägga till flera enheter.',
    infoParagraph2: 'Se till att alla dina mobilenheter har åtkomstkod i bruk.',
    name: 'Enhetens namn',
    addDevice: 'Lägg till mobilenhet',
    editName: 'Redigera enhetens namn',
    deleteDevice: 'Vill du ta bort mobilenheten?'
  },
  mobilePairingModal: {
    sharedDeviceModalTitle: 'Lägg till ny mobilenhet till enheten',
    personalDeviceModalTitle: 'Lägg till ny personlig mobilenhet',
    modalText1: 'Gå med mobilenheten till adressen',
    modalText2: 'och mata in koden nedan i enheten.',
    modalText3:
      'Mata in bekräftelsekoden som visas på mobilenheten i fältet nedan.',
    modalText4:
      'Ge mobilenheten ett namn som skiljer den från andra mobilenheter.',
    namePlaceholder: 'Namn'
  },
  invoices: {
    table: {
      title: 'Fakturor',
      toggleAll: 'Välj alla fakturor i området',
      head: 'Huvudman',
      children: 'Barn',
      period: 'Faktureringsperiod',
      createdAt: 'Utkast skapat',
      nb: 'Obs',
      totalPrice: 'Summa',
      status: 'Status',
      replacementInvoice: 'Kreditfaktura'
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
          ? 'Välj fakturor för vald tidsperiod och områden'
          : 'Välj denna månads fakturor för valda områden',
      individualSendAlertText:
        'Kom ihåg att ta upp tidigare överförda fakturor i faktureringssystemet innan nya överförs.'
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
      REPLACEMENT_DRAFT: 'Krediteringsutkast',
      REPLACED: 'Krediterad'
    },
    title: {
      DRAFT: 'Fakturautkast',
      WAITING_FOR_SENDING: 'Faktura som väntar på överföring',
      SENT: 'Överförd faktura',
      REPLACEMENT_DRAFT: 'Kreditfakturautkast',
      REPLACED: 'Krediterad faktura'
    },
    form: {
      nav: {
        return: 'Tillbaka'
      },
      child: {
        ssn: 'Barnets personbeteckning'
      },
      headOfFamily: {
        title: 'Huvudman',
        fullName: 'Huvudman',
        ssn: 'Huvudmannens personbeteckning',
        codebtorName: 'Medgäldenär',
        codebtorSsn: 'Medgäldenärens personbeteckning'
      },
      details: {
        title: 'Fakturans uppgifter',
        status: 'Status',
        range: 'Faktureringsperiod',
        number: 'Fakturans nummer',
        dueDate: 'Fakturans förfallodag',
        account: 'Konto',
        accountType: 'Kontoslag',
        agreementType: 'Fakturatyp',
        relatedFeeDecisions: 'Relaterade avgiftsbeslut',
        replacedInvoice: 'Ersätt fakturan',
        invoice: 'Faktura',
        revision: (revisionNumber: number) =>
          `Korrektionsfaktura ${revisionNumber}`,
        replacedBy: (link: React.ReactNode) => (
          <>Denna faktura är korrigerad. Ersättande faktura: {link}</>
        ),
        replacedByDraft: (link: React.ReactNode) => (
          <>För denna faktura finns ett ersättande korrektionsutkast: {link}</>
        )
      },
      replacement: {
        title: 'Uppgifter relaterade till fakturans kreditering',
        info: 'Du kan lägga till relaterade uppgifter om kreditering här.',
        reason: 'Orsak till kreditering',
        reasons: {
          SERVICE_NEED: 'Felaktigt servicebehov',
          ABSENCE: 'Dagboksanteckning',
          INCOME: 'Saknade/felaktiga inkomstuppgifter',
          FAMILY_SIZE: 'Felaktig familjestorlek',
          RELIEF_RETROACTIVE: 'Avgiftsbefrielse, retroaktiv',
          OTHER: 'Annat'
        },
        notes: 'Tilläggsuppgifter',
        attachments: 'Bilagor',
        sendInfo:
          'När du markerar denna faktura som överförd, markeras fakturan som ska ersättas som krediterad!',
        send: 'Markera som överförd',
        markedAsSent: 'Markerad som överförd'
      },
      rows: {
        title: 'Fakturarader',
        product: 'Produkt',
        description: 'Förklaring',
        unitId: 'Enhet',
        daterange: 'Period',
        amount: 'Antal',
        unitPrice: 'A-pris',
        price: 'Summa',
        subtotal: 'Fakturans summa'
      },
      sum: {
        rowSubTotal: 'Barnets raders summa',
        familyTotal: 'Familjen totalt'
      },
      buttons: {
        markSent: 'Markera som överförd'
      }
    },
    distinctiveDetails: {
      MISSING_ADDRESS: 'Adress saknas'
    },
    openAbsenceSummary: 'Öppna frånvarosammandrag'
  },
  invoiceCorrections: {
    noChildren: 'Personen är inte huvudman för något barn',
    targetMonth: 'Korrigeras under faktureringsperioden',
    nextTargetMonth: 'Nästa faktureringsperiod',
    range: 'Orsakens period',
    addRow: 'Lägg till korrigeringsrad',
    addTitle: 'Ny korrigeringsrad',
    editTitle: 'Redigera korrigeringsrad',
    deleteConfirmTitle: 'Tas korrigeringsraden bort?'
  },
  financeDecisions: {
    handlerSelectModal: {
      title: 'Kontrollera uppgifter',
      label: 'Beslutsfattare',
      error: 'Laddning av beslutsfattare misslyckades, försök igen',
      default: 'Beslutsfattare angiven i enhetens uppgifter',
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
      validity: 'Avgiftsbeslut i kraft',
      price: 'Summa',
      number: 'Nummer',
      status: 'Status',
      createdAt: 'Skapat',
      sentAt: 'Skickad',
      difference: {
        title: 'Ändring',
        value: {
          GUARDIANS: 'Vårdnadshavare',
          CHILDREN: 'Barn',
          INCOME: 'Inkomster',
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
          FEE_ALTERATIONS: 'A',
          FAMILY_SIZE: 'F',
          FEE_THRESHOLDS: 'AI'
        }
      },
      annullingDecision: 'Annullera eller avsluta beslut fr.o.m.'
    },
    buttons: {
      checked: (count: number) =>
        count === 1 ? `${count} beslut valt` : `${count} beslut valda`,
      createDecision: (count: number) =>
        count === 1 ? 'Skapa beslut' : 'Skapa beslut',
      ignoreDraft: 'Hoppa över utkast',
      unignoreDrafts: (count: number) =>
        count === 1 ? 'Ångra överhoppning' : 'Ångra överhoppningar',
      markSent: 'Markera som postat',
      close: 'Stäng utan att spara',
      save: 'Spara ändringar',
      errors: {
        WAITING_FOR_MANUAL_SENDING:
          'En del av huvudmännen har beslut som väntar på manuell sändning'
      }
    }
  },
  ignoreDraftModal: {
    title: 'Vill du verkligen hoppa över utkastet?',
    content: (
      <div>
        <H3>Utkastet får hoppas över endast om följande villkor uppfylls:</H3>
        <ul>
          <li>Utkastet gäller en period i det förflutna, och</li>
          <li>
            Utkastet är felaktigt eftersom klientuppgifterna för den förflutna
            perioden är felaktiga, och
          </li>
          <li>
            Det ursprungliga skickade beslutet för samma period är korrekt
          </li>
        </ul>
        <p>
          Om utkastet är felaktigt eftersom uppgifterna är felaktiga (t.ex.
          familjerelationer har retroaktivt tagits bort felaktigt), är det
          viktigt att i första hand försöka korrigera uppgifterna till sitt
          ursprungliga skick, eftersom de påverkar även andra system.
        </p>
        <p>
          Om utkastet är felaktigt eller onödigt trots att uppgifterna är
          korrekta, hoppa inte över utkastet utan kontakta utvecklingsteamet så
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
      validity: 'Värdebeslut i kraft',
      totalValue: 'SS-Värde',
      totalCoPayment: 'Självrisk',
      number: 'Nummer',
      status: 'Status',
      createdAt: 'Skapat',
      sentAt: 'Skickat',
      difference: {
        title: 'Ändring',
        value: {
          GUARDIANS: 'Vårdnadshavare',
          INCOME: 'Inkomster',
          FAMILY_SIZE: 'Familjestorlek',
          PLACEMENT: 'Placering',
          SERVICE_NEED: 'Servicebehov',
          SIBLING_DISCOUNT: 'Syskonrabatt',
          CO_PAYMENT: 'Självrisken innan avgiftsändringar',
          FEE_ALTERATIONS: 'Avgiftsändringar',
          FINAL_CO_PAYMENT: 'Självrisken',
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
          CO_PAYMENT: 'SA',
          FEE_ALTERATIONS: 'A',
          FINAL_CO_PAYMENT: 'S',
          BASE_VALUE: 'GV',
          VOUCHER_VALUE: 'SV',
          FEE_THRESHOLDS: 'AI'
        }
      },
      annullingDecision: 'Annullera eller avsluta beslut fr.o.m.'
    },
    buttons: {
      checked: (count: number) =>
        count === 1 ? `${count} beslut valt` : `${count} beslut valda`,
      createDecision: (count: number) =>
        count === 1 ? 'Skapa beslut' : 'Skapa beslut',
      ignoreDraft: 'Hoppa över utkast',
      unignoreDrafts: (count: number) =>
        count === 1 ? 'Ångra överhoppning' : 'Ångra överhoppningar',
      markSent: 'Markera som postat',
      close: 'Stäng utan att spara',
      save: 'Spara ändringar',
      errors: {
        WAITING_FOR_MANUAL_SENDING:
          'En del av barnen har beslut som väntar på manuell sändning'
      }
    }
  },
  payments: {
    table: {
      title: 'Avgifter',
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
      SENT: 'Överfört'
    },
    sendModal: {
      title: 'Överför valda utbetalningar',
      paymentDate: 'Betalningsdag',
      dueDate: 'Förfallodag'
    },
    distinctiveDetails: {
      MISSING_PAYMENT_DETAILS: 'Betalningsuppgifter saknas'
    }
  },
  placement: {
    type: {
      CLUB: 'Klubbverksamhet',
      DAYCARE: 'Småbarnspedagogik',
      FIVE_YEARS_OLD_DAYCARE: 'Småbarnspedagogik för 5-åringar',
      PRESCHOOL_WITH_DAYCARE: 'Förskola och kompletterande småbarnspedagogik',
      PREPARATORY_WITH_DAYCARE:
        'Förberedande undervisning och kompletterande småbarnspedagogik',
      DAYCARE_PART_TIME: 'Deltids småbarnspedagogik',
      DAYCARE_FIVE_YEAR_OLDS: 'Småbarnspedagogik för 5-åringar',
      DAYCARE_PART_TIME_FIVE_YEAR_OLDS:
        'Deltids småbarnspedagogik för 5-åringar',
      PRESCHOOL: 'Förskola',
      PREPARATORY: 'Förberedande undervisning',
      PREPARATORY_DAYCARE:
        'Förberedande undervisning och kompletterande småbarnspedagogik',
      PREPARATORY_DAYCARE_ONLY:
        'Småbarnspedagogik tillhörande förberedande undervisning',
      PRESCHOOL_DAYCARE: 'Förskola och kompletterande småbarnspedagogik',
      PRESCHOOL_DAYCARE_ONLY: 'Småbarnspedagogik med kompletterande förskola',
      PRESCHOOL_CLUB: 'Förskoleklubb',
      TEMPORARY_DAYCARE: 'Tillfällig heldags småbarnspedagogik',
      TEMPORARY_DAYCARE_PART_DAY: 'Tillfällig deltids småbarnspedagogik',
      SCHOOL_SHIFT_CARE: 'Skiftvård för skolbarn'
    },
    messagingCategory: {
      MESSAGING_CLUB: 'Klubbverksamhet',
      MESSAGING_DAYCARE: 'Småbarnspedagogik',
      MESSAGING_PRESCHOOL: 'Förskola'
    },
    defaultOptionText: '(Standard)',
    defaultOptionMissingText: 'Inte tillgänglig standardservicebehov'
  },
  feeAlteration: {
    DISCOUNT: 'Sänkning',
    INCREASE: 'Höjning',
    RELIEF: 'Lindring'
  },
  feeDecision: {
    title: {
      DRAFT: 'Avgiftsbeslututkast',
      IGNORED: 'Överhoppat avgiftsbeslututkast',
      WAITING_FOR_SENDING: 'Avgiftsbeslut (utgående)',
      WAITING_FOR_MANUAL_SENDING: 'Avgiftsbeslut (skickas manuellt)',
      SENT: 'Avgiftsbeslut',
      ANNULLED: 'Annullerat avgiftsbeslut'
    },
    distinctiveDetails: {
      UNCONFIRMED_HOURS: 'Saknad servicebehov',
      EXTERNAL_CHILD: 'Utomkommunal',
      RETROACTIVE: 'Retroaktivt beslut',
      NO_STARTING_PLACEMENTS: 'Dölj nya barn som börjar',
      MAX_FEE_ACCEPTED: 'Samtycke till högsta avgift',
      PRESCHOOL_CLUB: 'Endast förskoleklubb',
      NO_OPEN_INCOME_STATEMENTS: 'Inga öppna inkomstutredningar'
    },
    status: {
      DRAFT: 'Utkast',
      IGNORED: 'Överhoppat utkast',
      WAITING_FOR_SENDING: 'Utgående',
      WAITING_FOR_MANUAL_SENDING: 'Skickas manuellt',
      SENT: 'Skickat',
      ANNULLED: 'Annullerat'
    },
    type: {
      NORMAL: 'Vanligt avgiftsbeslut, ingen lindring',
      RELIEF_ACCEPTED: 'Lindring godkänd (Skickas manuellt)',
      RELIEF_PARTLY_ACCEPTED:
        'Partiell' + ' lättnad godkänd (Skickas manuellt)',
      RELIEF_REJECTED: 'Lindring avvisad (Skickas manuellt)'
    },
    headOfFamily: 'Huvudman',
    partner: 'Andra vårdnadshavaren / betalningsskyldige',
    decisionNumber: 'Beslutets nummer',
    validPeriod: 'Avgiftsbeslut i kraft',
    sentAt: 'Avgiftsbeslut skickat',
    decisionHandler: 'Beslutets handläggare',
    relief: 'Avgiftsbeslutets lindring',
    waitingManualSending: 'Skickas manuellt',
    pdfLabel: 'Avgiftsbeslut PDF',
    downloadPdf: 'Ladda ner PDF',
    pdfInProgress:
      '(PDF genereras. Ladda om sidan om en stund' +
      ' så kan du ladda ner den från länken bredvid.)',
    form: {
      nav: {
        return: 'Tillbaka'
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
        placementDate: 'Placering i kraft',
        serviceNeed: 'Servicebehov',
        name: 'Namn',
        postOffice: 'Postort'
      },
      summary: {
        title: 'Sammandrag av grunden för avgiftsbeslutet',
        income: {
          title: 'Sammandrag av familjens inkomster',
          effect: {
            label: 'Avgiftsgrund',
            MAX_FEE_ACCEPTED:
              'Vårdnadshavarens samtycke till högsta avgiften för småbarnspedagogik',
            INCOMPLETE: 'Familjens inkomstuppgifter är bristfälliga.',
            INCOME: 'Som avgiftsgrund vårdnadshavarnas inkomstuppgifter',
            NOT_AVAILABLE: 'Som avgiftsgrund högsta inkomstklass (automatisk)'
          },
          details: {
            MAX_FEE_ACCEPTED:
              'Samtycke till högsta avgiften för småbarnspedagogik',
            INCOMPLETE: 'Bristfälliga inkomstuppgifter',
            NOT_AVAILABLE: 'Inkomstuppgifter har inte lämnats'
          },
          income: 'Inkomster',
          expenses: 'Utgifter',
          total: 'Familjens totala inkomster',
          familyComposition: 'Familjens sammansättning och avgiftsgrunder',
          familySize: 'Familjestorlek',
          persons: 'personer',
          feePercent: 'Avgiftsprocent',
          minThreshold: 'Lägsta bruttogräns'
        },
        parts: {
          title: 'Sammandrag av familjens barns avgifter',
          siblingDiscount: 'syskonrabatt',
          sum: 'Summa'
        },
        totalPrice: 'Familjens avgift för småbarnspedagogik totalt'
      },
      buttons: {
        saveChanges: 'Spara ändringar'
      }
    },
    modal: {
      title: 'Vill du återgå utan att spara ändringarna?',
      cancel: 'Återgå utan att spara',
      confirm: 'Fortsätt redigera'
    }
  },
  filters: {
    searchTerms: 'Sökvillkor',
    freeTextPlaceholder:
      'Sök med namn, personbeteckning, adress eller avgiftsbeslutets nummer',
    area: 'Område',
    unit: 'Enhet',
    financeDecisionHandler: 'Handläggare av ekonomibeslut',
    unitPlaceholder: 'Välj enhet',
    financeDecisionHandlerPlaceholder: 'Välj anställd',
    distinctiveDetails: 'Annat att beakta',
    difference: 'Ändring',
    providerType: 'Arrangemangsform',
    status: 'Status',
    clear: 'Rensa val',
    validityPeriod: 'Giltighetstid',
    searchByStartDate: 'Startdatum infaller på valt tidsintervall',
    invoiceDate: 'Fakturans datum',
    invoiceSearchByStartDate: 'Skicka fakturor för vald period',
    paymentDate: 'Betalningsdag',
    paymentFreeTextPlaceholder: 'Sök med avgiftens nummer',
    incomeStatementSent: 'Inkomstutredning skickad',
    incomeStatementPlacementValidDate: 'Placering i kraft',
    incomeStatementStatusTitle: 'Visa status',
    incomeStatementStatus: {
      SENT: 'Väntar på handläggning',
      HANDLING: 'Under behandling'
    },
    showClosedUnits: 'Visa stängda enheter',
    hideClosedUnits: 'Dölj stängda enheter'
  },
  valueDecision: {
    title: {
      DRAFT: 'Utkast till värdebeslut',
      IGNORED: 'Hoppat över utkast till värdebeslut',
      WAITING_FOR_SENDING: 'Värdebeslut (på väg)',
      WAITING_FOR_MANUAL_SENDING: 'Värdebeslut (skickas manuellt)',
      SENT: 'Värdebeslut',
      ANNULLED: 'Annullerat värdebeslut'
    },
    headOfFamily: 'Huvudman',
    partner: 'Andra vårdnadshavaren / betalningsskyldig',
    decisionNUmber: 'Beslutets nummer',
    validPeriod: 'Värdebeslut i kraft',
    sentAt: 'Värdebeslut skickat',
    pdfLabel: 'Värdebeslut PDF',
    decisionHandlerName: 'Beslutets handläggare',
    relief: 'Lättnad i värdebeslut',
    downloadPdf: 'Ladda ner PDF',
    pdfInProgress:
      '(PDF genereras. Ladda om sidan om en stund så kan du ladda ner den från länken bredvid.)',
    status: {
      DRAFT: 'Utkast',
      IGNORED: 'Hoppat över utkast',
      WAITING_FOR_SENDING: 'På väg',
      WAITING_FOR_MANUAL_SENDING: 'Skickas manuellt',
      SENT: 'Skickat',
      ANNULLED: 'Annullerat'
    },
    type: {
      NORMAL: 'Vanligt värdebeslut, ingen lättnad',
      RELIEF_ACCEPTED: 'Lättnad godkänd (Skickas manuellt)',
      RELIEF_PARTLY_ACCEPTED:
        'Partiell' + ' lättnad godkänd (Skickas manuellt)',
      RELIEF_REJECTED: 'Lättnad avslagen (Skickas manuellt)'
    },
    child: {
      name: 'Namn',
      ssn: 'Personbeteckning',
      postOffice: 'Postort',
      placementType: 'Placeringstyp',
      careArea: 'Serviceområde',
      unit: 'Enhet',
      serviceNeed: 'Servicebehov'
    },
    summary: {
      title: 'Sammandrag av grunden för värdebeslutet',
      coPayment: 'Självrisk',
      sum: 'Summa',
      siblingDiscount: 'Syskonrabatt',
      totalValue: 'Servicesedelns värde efter självrisk',
      income: {
        title: 'Sammandrag av familjens inkomster',
        effect: {
          label: 'Avgiftsgrund',
          MAX_FEE_ACCEPTED:
            'Vårdnadshavarens samtycke till högsta avgiften för småbarnspedagogik',
          INCOMPLETE: 'Familjens inkomstuppgifter är bristfälliga.',
          INCOME: 'Avgiften baseras på vårdnadshavarnas inkomstuppgifter',
          NOT_AVAILABLE:
            'Avgiften baserar sig på högsta inkomstklass (automatisk)'
        },
        details: {
          MAX_FEE_ACCEPTED:
            'Samtycke till högsta avgiften för småbarnspedagogik',
          INCOMPLETE: 'Bristfälliga inkomstuppgifter',
          NOT_AVAILABLE: 'Inkomstuppgifter har inte lämnats'
        },
        income: 'Inkomster',
        expenses: 'Utgifter',
        total: 'Familjens totala inkomster',
        familyComposition: 'Familjens sammansättning och avgiftsgrunder',
        familySize: 'Familjestorlek',
        persons: 'personer',
        feePercent: 'Avgiftsprocent',
        minThreshold: 'Lägsta bruttogräns'
      },
      value: 'Servicesedelns värde',
      age: {
        LESS_THAN_3: 'Under 3-åring',
        OVER_3: 'Minst 3-åring'
      },
      assistanceNeedCoefficient: 'koefficient för stödbehov',
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
    weekdaysShort: ['mån', 'tis', 'ons', 'tor', 'fre', 'lör', 'sön']
  },
  absences: {
    title: 'Frånvaro',
    absenceTypes: {
      OTHER_ABSENCE: 'Frånvaro',
      SICKLEAVE: 'Sjukdom',
      UNKNOWN_ABSENCE: 'Oanmäld frånvaro',
      PLANNED_ABSENCE: 'Skiftarbetsfrånvaro',
      TEMPORARY_RELOCATION: 'Barnet reservplacerat på annan plats',
      PARENTLEAVE: 'Föräldraledighet',
      FORCE_MAJEURE: 'Avgiftsfri dag',
      FREE_ABSENCE: 'Avgiftsfri frånvaro',
      UNAUTHORIZED_ABSENCE: 'Oanmäld jourbarnvårdsfrånvaro',
      NO_ABSENCE: 'Inte frånvarande'
    },
    missingHolidayReservation:
      'Vårdnadshavaren har inte bekräftat reservering för semestertid',
    missingHolidayQuestionnaireAnswer:
      'Vårdnadshavaren har inte svarat på frånvaroförfrågan',
    shiftCare: 'Kväll-/skiftvård',
    requiresBackupCare: 'Väntar på reservplacering',
    additionalLegendItems: {
      CONTRACT_DAYS: 'Avtalsdaglig servicebehov'
    },
    absenceTypesShort: {
      OTHER_ABSENCE: 'Frånvaro',
      SICKLEAVE: 'Sjukdom',
      UNKNOWN_ABSENCE: 'Oanmäld',
      PLANNED_ABSENCE: 'Skiftarbete',
      TEMPORARY_RELOCATION: 'Reservplacering',
      PARENTLEAVE: 'Föräldraledighet',
      FORCE_MAJEURE: 'Avgiftsfri',
      FREE_ABSENCE: 'Avgiftsfri',
      UNAUTHORIZED_ABSENCE: 'Böter',
      NO_ABSENCE: 'Inte borta'
    },
    absenceTypeInfo: {
      OTHER_ABSENCE:
        'Används alltid när vårdnadshavaren har anmält frånvaro inklusive regelbundna ledigheter och semestertid. Används även i skiftenheter för barns semesteranteckningar eller andra frånvaror som är utanför planerade närvaroreserveringar.',
      SICKLEAVE:
        'Sammanhängande sjukfrånvaro som överskrider 11 dagar påverkar avgiften nedåt.',
      UNKNOWN_ABSENCE:
        'Används då vårdnadshavaren inte har anmält frånvaron, påverkar även debiteringen i juli. Koden ändras endast om det gäller sjukfrånvaro, vars fortsättning vårdnadshavaren meddelar följande dag.',
      PLANNED_ABSENCE:
        'Används endast i skiftenheter, när det gäller ledighet på grund av skiftarbete, semestertider antecknas med kod Frånvarande. Berättigar inte till avgiftsnedsättning på fakturan.',
      TEMPORARY_RELOCATION:
        'Barnet har reservplacerats i en annan enhet. Frånvaron kan antecknas om en sådan är känd. Bekanta dig dock med anvisningen för semestertid om frånvaron gäller semestertid.',
      PARENTLEAVE:
        'Föräldraledighet, antecknas endast för det barn för vilket vårdnadshavaren är ledig, inte för syskon. Påverkar avgiften så att tiden i fråga är avgiftsfri.',
      FORCE_MAJEURE:
        'Används endast i specialsituationer enligt förvaltningens anvisningar.',
      FREE_ABSENCE: 'Avgiftsfri frånvaro för sommartid',
      UNAUTHORIZED_ABSENCE: 'Oanmäld jourbarnvårdsfrånvaro',
      NO_ABSENCE: 'Om barnet är närvarande, anteckna ingenting.'
    },
    additionalLegendItemInfos: {
      CONTRACT_DAYS: 'Barn med avtalsdaglig servicebehov'
    },
    careTypes: {
      SCHOOL_SHIFT_CARE: 'Skiftarbete för skolbarn',
      PRESCHOOL: 'Förskola',
      PRESCHOOL_DAYCARE: 'Anknytande småbarnspedagogik',
      DAYCARE_5YO_FREE: 'Småbarnspedagogik för 5-åringar',
      DAYCARE: 'Småbarnspedagogik',
      CLUB: 'Klubbverksamhet'
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
        UNAUTHORIZED_ABSENCE: 'Oanmäld jourbarnvårdsfrånvaro',
        NO_ABSENCE: 'Inte frånvarande',
        MISSING_HOLIDAY_RESERVATION: 'Anmälan om semestertid saknas'
      },
      free: 'Avgiftsfri',
      paid: 'Avgiftsbelagd',
      absenceSummaryTitle: 'Barnets frånvarosammandrag'
    },
    table: {
      selectAll: 'Välj alla',
      staffRow: 'Personal närvarande',
      disabledStaffCellTooltip: 'Gruppen existerar inte på vald dag',
      reservationsTotal: 'Reservering/mån',
      attendancesTotal: 'Utfall/mån'
    },
    legendTitle: 'Förklaring av anteckningar',
    addAbsencesButton(numOfSelected: number) {
      return numOfSelected === 1
        ? 'Lägg till anteckning för vald...'
        : 'Lägg till anteckningar för valda...'
    },
    notOperationDay: 'Inte verksamhetsdag',
    absence: 'Frånvaro',
    reservation: 'Reservering',
    present: 'Närvarande',
    guardian: 'Vårdnadshavare',
    staff: 'Personal',
    dailyServiceTime: 'Avtalstid'
  },
  placementDraft: {
    preschoolDaycare: 'Kompletterande småbarnspedagogik',
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
      'Tidigare överlappande placeringar avbryts automatiskt om kommunmedlemmen tar emot den erbjudna platsen.',
    createPlacementDraft: 'Skapa placeringsplan',
    applicationDatesTitle: 'Utkast till placering',
    drafted: 'Utkast',
    dueDate: 'Laglighet',
    preferred: 'Önskad',
    datesTitle: 'Placeringsplan som skapas nu',
    type: 'Placeringstyp',
    date: 'Placeringsdatum',
    dateError: 'Överlappande placering för tidsperioden.',
    preparatoryPeriod: 'Förberedande undervisning',
    dateOfBirth: 'Födelsedatum',
    selectUnit: 'Välj enhet',
    selectedUnit: 'Vald enhet',
    restrictedDetails: 'Vårdnadshavaren har skyddat adress',
    restrictedDetailsTooltip:
      'Beslutet måste skickas för hand till den andra vårdnadshavaren när sökanden har skyddat adress.'
  },
  decisionDraft: {
    title: 'Beslutets utarbetande och skickande',
    info1:
      'Genom att skicka beslutet godkänner du placeringsplanen. Kommunmedlemmen skickas de beslut som du har valt nedan.',
    info2:
      'Observera att valen och datumen endast påverkar beslutsdokumenten. Om du vill redigera den faktiska placeringen, returnera ansökan tillbaka till placerbara och placera den på nytt.',
    ssnInfo1:
      'Vårdnaden kan inte kontrolleras utan vårdnadshavarens och barnets personbeteckning.',
    ssnInfo2:
      'Skicka det utskrivna beslutet per post och markera det som postlagt.',
    unitInfo1: 'Enhetens uppgifter är bristfälliga.',
    unitInfo2:
      'Bristfälliga uppgifter måste uppdateras innan besluten skapas. Kontakta huvudanvändaren',
    notGuardianInfo1: 'Ansökans vårdnadshavare är inte barnets vårdnadshavare.',
    notGuardianInfo2:
      'Personen som är antecknad som vårdnadshavare i ansökan är inte barnets vårdnadshavare enligt befolkningsregistret. Beslutet måste skickas i pappersform.',
    unit: 'Enhet',
    contact: 'Kontaktperson',
    decisionLabelHeading: 'Placeringstyp',
    decisionValueHeading: 'Beslutsdatum',
    types: {
      CLUB: 'Klubbverksamhet',
      DAYCARE: 'Småbarnspedagogik',
      DAYCARE_PART_TIME: 'Deltids småbarnspedagogik',
      PRESCHOOL_DAYCARE: 'Småbarnspedagogik anknytande till förskola',
      PRESCHOOL_CLUB: 'Förskolans klubbverksamhet',
      PRESCHOOL: 'Förskola',
      PREPARATORY: 'Förberedande undervisning',
      PREPARATORY_EDUCATION: 'Förberedande undervisning',
      PREPARATORY_DAYCARE:
        'Småbarnspedagogik anknytande till förberedande undervisning'
    },
    placementUnit: 'Vid placering vald enhet',
    selectedUnit: 'För beslut valbar enhet',
    unitDetailsHeading: 'Uppgifter som visas på beslut',
    preschoolDecisionName: 'Enhetens namn på förskolebeslut',
    daycareDecisionName: 'Enhetens namn på beslut om småbarnspedagogik',
    unitManager: 'Enhetens ledare',
    unitAddress: 'Enhetens adress',
    handlerName: 'Handläggarens namn',
    handlerAddress: 'Handläggarens adress',
    receiver: 'Mottagare',
    otherReceiver: 'Mottagare (andra vårdnadshavaren)',
    missingValue: 'Uppgift saknas.',
    noOtherGuardian: 'Det finns ingen andra vårdnadshavare',
    differentUnit:
      'Enheten som visas på beslutet är en annan än i den ursprungliga placeringen.'
  },
  reports: {
    title: 'Rapporter',
    downloadButton: 'Ladda ner rapport',
    common: {
      orderBy: 'Ordning',
      total: 'Totalt',
      totalShort: 'Tot',
      careAreaName: 'Serviceområde',
      unitName: 'Enhet',
      groupName: 'Grupp',
      unitType: 'Verksamhetsform',
      unitTypes: {
        DAYCARE: 'Daghem',
        FAMILY: 'Familjedagvård',
        GROUP_FAMILY: 'Gruppfamiljedagvård',
        CLUB: 'Klubbverksamhet'
      },
      unitProviderType: 'Verksamhetsform',
      unitProviderTypes: {
        MUNICIPAL: 'Kommunal',
        PURCHASED: 'Köptjänst',
        PRIVATE: 'Privat',
        MUNICIPAL_SCHOOL: 'Supe',
        PRIVATE_SERVICE_VOUCHER: 'Servicesedel',
        EXTERNAL_PURCHASED: 'Köptjänst (annan)'
      },
      placementType: 'Placeringstyp',
      period: 'Tidsperiod',
      date: 'Datum',
      clock: 'Kl',
      startDate: 'Från',
      endDate: 'Till',
      firstName: 'Förnamn',
      lastName: 'Efternamn',
      childName: 'Barnets namn',
      child: 'Barn',
      under3y: '<3å',
      over3y: '3+',
      age: 'Ålder',
      dateOfBirth: 'Födelsedatum',
      attendanceType: 'Närvaro',
      attendanceTypes: {
        RESERVATION: 'Reservering',
        REALIZATION: 'Förverkligande'
      }
    },
    applications: {
      title: 'Inkomna ansökningar',
      description:
        'Rapporten listar inkomna och pågående ansökningar per enhet.',
      ageInfo:
        'Barnets ålder beräknas på slutdagen av det valda tidsintervallet',
      preferredStartingDate: 'Startdag',
      under3Years: 'Småbarnspedagogik ansökningar (under 3 år)',
      over3Years: 'Småbarnspedagogik ansökningar (över 3år)',
      preschool: 'Förskoleansökningar',
      club: 'Klubbansökningar',
      totalChildren: 'Barn som ansökt totalt'
    },
    childDocumentDecisions: {
      title: 'Stödbeslut',
      description: 'Till beslutsfattaren skickade stödbeslut.',
      statusFilter: 'Status till påseende',
      otherFilters: 'Andra val',
      includeEnded: 'Visa avslutade beslut',
      templateName: 'Beslut',
      childName: 'Barn',
      modifiedAt: 'Redigerad',
      decisionMaker: 'Beslutsfattare',
      decisionMade: 'Beslut fattat',
      status: 'Status'
    },
    decisions: {
      title: 'Beslut',
      description: 'Rapporten listar fattade beslut per enhet.',
      ageInfo: 'Barnets ålder beräknas på beslutets skickningsdatum',
      sentDate: 'Beslutets skickningsdatum',
      daycareUnder3: 'Småbarnspedagogik-beslut (under 3 år)',
      daycareOver3: 'Småbarnspedagogik-beslut (över 3 år)',
      preschool: 'Förskolebeslut',
      preschoolDaycare: 'Förskola+anknytande beslut',
      connectedDaycareOnly: 'Senare ansökta anknytande beslut',
      preparatory: 'Förberedande beslut',
      preparatoryDaycare: 'Förberedande+anknytande beslut',
      club: 'Klubbbeslut',
      preference1: '1. önskemål',
      preference2: '2. önskemål',
      preference3: '3. önskemål',
      preferenceNone: 'Inte som önskemål',
      total: 'Beslut totalt'
    },
    raw: {
      title: 'Rårapport',
      description:
        'Mindre bearbetat omfattande datamaterial, utifrån vilket man själv kan skapa olika rapporter.'
    },
    attendanceReservation: {
      title: 'Dagliga barnets ankomst- och avgångstider',
      description: 'Rapport om barnens reserveringar och behovet av personal',
      ungrouped: 'Barn som väntar på grupp',
      capacityFactor: 'Deb',
      staffCount: 'Personal',
      tooLongRange: 'Du kan söka rapport för högst två månaders tid.'
    },
    attendanceReservationByChild: {
      title: 'Barnspecifika närvarotider',
      description:
        'Rapporten listar barnspecifikt de ankomst- och avgångstider som vårdnadshavarna har meddelat. Rapporten finns tillgänglig grupp- och enhetsvis.',
      ungrouped: 'Barn som väntar på grupp',
      orderByOptions: {
        start: 'Ankomsttid',
        end: 'Avgångstid'
      },
      absence: 'Frånvaro',
      noReservation: 'Reservering saknas',
      filterByTime: 'Filtrera baserat på tid',
      showOnlyShiftCare: 'Visa endast skiftvård',
      includeClosed: 'Visa avslutade enheter och grupper',
      reservationStartTime: 'Ankomst',
      reservationEndTime: 'Avresa',
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
      title: 'Klientavgifter',
      description: 'Rapport över summorna för barnspecifika klientavgifter.',
      date: 'Datum',
      area: 'Serviceområde',
      unit: 'Enhet',
      providerType: 'Organisationsform',
      placementType: 'Placeringstyp',
      type: 'Beslutstyp',
      types: {
        FEE_DECISION: 'Avgiftsbeslut',
        VOUCHER_VALUE_DECISION: 'Värdebeslut'
      },
      fee: 'Barnspecifik avgift',
      count: 'Antal'
    },
    duplicatePeople: {
      title: 'Duplicerade kommuninvånare',
      description:
        'Rapporten listar och gör det möjligt att förena personer som verkar finnas flera gånger i systemet.',
      moveFrom: 'Flytta uppgifter',
      moveTo: 'Flytta hit',
      confirmMoveTitle:
        'Vill du verkligen flytta alla uppgifter till en annan person?',
      confirmDeleteTitle: 'Vill du verkligen ta bort denna person?',
      errorTitle: 'Flyttning av uppgifter misslyckades',
      errorText:
        'Kontrollera att personerna inte har till exempel överlappande placeringar, servicebehov eller andra överlappningar som skulle kunna hindra föreningen.',
      columns: {
        'absence.child_id': 'Frånvaron',
        'absence.modified_by_guardian_id': 'Själv- markerad frånvaro',
        'absence_application.child_id': 'Frånvaro- ansökningar (förskola)',
        'application.child_id': 'Ansökningar (som barn)',
        'application.guardian_id': 'Ansökningar (som vårdnads- havare)',
        'application.other_guardian_id':
          'Ansökningar (som andra vårdnads- havare)',
        'assistance_action.child_id': 'Stöd- åtgärder',
        'assistance_need.child_id': 'Stödbehov',
        'assistance_need_decision.child_id': 'Stödbehov- beslut',
        'assistance_need_decision_guardian.person_id':
          'Vårdnads- havare i stödbeslut',
        'assistance_need_voucher_coefficient.child_id':
          'Stödets servicesedel- koefficienter',
        'attachment.uploaded_by_person': 'Bilagor',
        'attendance_reservation.child_id': 'Närvaro- reservationer',
        'attendance_reservation.created_by_guardian_id':
          'Själv- markerade närvaro- reservationer',
        'backup_care.child_id': 'Reserv- placeringar',
        'backup_pickup.child_id': 'Reserv- hämtare',
        'calendar_event_attendee.child_id': 'Kalender- deltagare',
        'child_attendance.child_id': 'Närvaron',
        'child_images.child_id': 'Bilder',
        'backup_curriculum_document.child_id': 'Gamla läroplaner',
        'daily_service_time.child_id': 'Dagliga service- tider',
        'daily_service_time_notification.guardian_id':
          'Meddelanden om dagliga servicetider',
        'daycare_daily_note.child_id': 'Dagliga anteckningar',
        'family_contact.child_id': 'Kontakt- personer (barn)',
        'family_contact.contact_person_id': 'Kontakt- personer (vuxen)',
        'fee_alteration.person_id': 'Avgifts- ändringar',
        'fee_decision.head_of_family_id': 'Avgifts- beslut (huvudman)',
        'fee_decision.partner_id': 'Avgifts- beslut (partner)',
        'fee_decision_child.child_id': 'Avgifts- besluts- rader',
        'fridge_child.child_id': 'Huvudmän',
        'fridge_child.head_of_child': 'Familje- barn',
        'fridge_partner.person_id': 'Familje- partner',
        'foster_parent.child_id': 'Foster- barn',
        'foster_parent.parent_id': 'Foster- föräldrar',
        'holiday_questionnaire_answer.child_id': 'Enkätsvar',
        'income.person_id': 'Inkomst- uppgifter',
        'income_statement.person_id': 'Inkomst- utredningar',
        'invoice.codebtor': 'Fakturor (med- gäldenär)',
        'invoice.head_of_family': 'Fakturor',
        'invoice_correction_row.child_id': 'Faktura- korrigeringsrader (barn)',
        'invoice_correction_row.head_of_family':
          'Faktura- korrigeringsrader (huvudman)',
        'invoice_row.child': 'Faktura- rader',
        'koski_study_right.child_id': 'Koski studie- rättigheter',
        'nekku_special_diet_choices.child_id': 'Nekku-special- kost',
        'pedagogical_document.child_id': 'Pedagogiska dokument',
        'placement.child_id': 'Placeringar',
        'service_application.child_id': 'Servicebehov- ansökningar (som barn)',
        'service_application.person_id':
          'Servicebehov- ansökningar (som vårdnads- havare)',
        'varda_child.person_id': 'Varda barn',
        'varda_service_need.evaka_child_id': 'Varda service- behov',
        'backup_vasu_document.child_id': 'Gamla vasu-dokument',
        'voucher_value_decision.child_id': 'Värde- besluts- rader',
        'voucher_value_decision.head_of_family_id': 'Värde- beslut (huvudman)',
        'voucher_value_decision.partner_id': 'Värde- beslut (partner)',
        'message.sender_id': 'Skickade meddelanden',
        'message_content.author_id': 'Skrivna meddelande- innehåll',
        'message_recipients.recipient_id': 'Mottagna meddelanden',
        'message_draft.account_id': 'Meddelande- utkast'
      }
    },
    familyConflicts: {
      title: 'Familjekonflikter',
      description:
        'Rapporten listar huvudmän vars familjerelationer i systemet har konflikter. En konflikt kan uppstå om familjerelationerna som angivits i ansökan är i strid med tidigare uppgifter.',
      name: 'Huvudmannens namn',
      ssn: 'Personbeteckning',
      partnerConflictCount: 'Konflikter i makar',
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
      guardian2: 'Andra vårdnadshavaren',
      phone: 'Telefonnummer',
      email: 'E-postadress'
    },
    familyDaycareMealCount: {
      title: 'Måltidsrapport för barn i familjedagvård',
      description:
        'Rapporten räknar närvaroanteckningar för barn i familjedagvård under måltidstider och grupperar resultaten per enhet och område.',
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
      title: 'Barn som slutar i småbarnspedagogiken',
      description:
        'Rapport till FPA över barn som slutar i småbarnspedagogiken och eventuellt fortsätter senare.',
      ssn: 'Personbeteckning',
      placementEnd: 'Slutar i småbarnspedagogiken',
      unit: 'Enhet',
      area: 'Område',
      nextPlacementStart: 'Fortsätter i småbarnspedagogiken',
      nextPlacementUnitName: 'Fortsätter i enheten'
    },
    missingHeadOfFamily: {
      title: 'Saknade huvudmän',
      description:
        'Rapporten listar barn vars nuvarande placeringstid saknar uppgift om huvudman.',
      childLastName: 'Barnets efternamn',
      childFirstName: 'Barnets förnamn',
      showFosterChildren: 'Visa även fosterbarn',
      daysWithoutHeadOfFamily: 'Bristfälliga dagar'
    },
    missingServiceNeed: {
      title: 'Saknade servicebehov',
      description:
        'Rapporten listar barn vars placeringstid saknar servicebehov.',
      daysWithoutServiceNeed: 'Bristfälliga dagar',
      defaultOption: 'Använt standardservicebehov'
    },
    invalidServiceNeed: {
      title: 'Felaktiga servicebehov',
      description: 'Rapporten listar servicebehov som verkar innehålla fel.',
      unit: 'Nuvarande enhet',
      noCurrentUnit: 'Slutat'
    },
    partnersInDifferentAddress: {
      title: 'Make/maka på annan adress',
      description:
        'Rapporten listar personer vars kylskåpsmake/maka enligt befolkningsregistret bor på en annan adress. Kontrollera om dessa personer fortfarande verkligen är sambor.',
      person1: 'Person',
      address1: 'Adress',
      person2: 'Make/maka',
      address2: 'Makens/makans adress'
    },
    serviceNeeds: {
      title: 'Barns servicebehov och åldrar i enheter',
      description:
        'Rapporten listar antal barn i enheter enligt servicebehov och ålder.',
      age: 'Ålder',
      fullDay: 'heltidsvård',
      partDay: 'deltidsvård',
      fullWeek: 'heltidsvård',
      partWeek: 'detidsvård',
      shiftCare: 'skiftvård',
      missingServiceNeed: 'servicebehov saknas',
      total: 'barn totalt'
    },
    exceededServiceNeed: {
      title: 'Överträdelser av servicebehov',
      description:
        'Rapporten listar barn vars servicebehovstimmar har överskridits.',
      serviceNeedHours: 'Servicebehovet (h)',
      usedServiceHours: 'Använt (h)',
      groupLinkHeading: 'Enhetens veckokalender',
      excessHours: 'Överskridning (h)'
    },
    units: {
      title: 'Enheter',
      description: 'Sammanfattning av enheternas uppgifter.',
      name: 'Namn',
      careAreaName: 'Serviceområde',
      careTypeCentre: 'Daghem',
      careTypeFamily: <span>Familje&shy;dag&shy;vård</span>,
      careTypeFamilyStr: 'Familjedagvård',
      careTypeGroupFamily: <span>Grupp&shy;familje&shy;dag&shy;vård</span>,
      careTypeGroupFamilyStr: 'Gruppfamiljedagvård',
      careTypeClub: 'Klubbverksamhet',
      careTypePreschool: 'Förskola',
      careTypePreparatoryEducation: 'Förberedande',
      clubApply: <span>Klubb&shy;ansökan</span>,
      clubApplyStr: 'Klubbansökan',
      daycareApply: <span>Dag&shy;hems&shy;ansökan</span>,
      daycareApplyStr: 'Daghemansökan',
      preschoolApply: <span>Förskole&shy;ansökan</span>,
      preschoolApplyStr: 'Förskoleansökan',
      providerType: 'Organisationsform',
      uploadToVarda: 'Varda',
      uploadChildrenToVarda: 'Varda (barn)',
      uploadToKoski: 'Koski',
      ophUnitOid: 'Verksamhetsställets OID',
      ophOrganizerOid: 'Anordnarens OID',
      invoicedByMunicipality: 'Faktureras från eVaka',
      costCenter: 'Kostnadsställe',
      address: 'Besöksadress',
      postOffice: 'Postanstalt',
      unitManagerName: 'Enhetens ledare',
      unitManagerPhone: 'Ledarens tfn',
      unitManagerEmail: 'Ledarens e-post',
      preschoolManagerName: 'Förskolans enhetsledare',
      preschoolManagerPhone: 'Förskolans enhetsledarens tfn.',
      preschoolManagerEmail: 'Förskolans enhetsledarens e-post',
      providesShiftCare: 'Erbjuder skiftvård',
      capacity: 'Kalkylerad kapacitet'
    },
    childrenInDifferentAddress: {
      title: 'Barn på annan adress',
      description:
        'Rapporten listar huvudmän vars kylskåpsbarn enligt befolkningsregistret bor på en annan adress. En del av dessa kan vara fel som borde korrigeras.',
      person1: 'Huvudman',
      address1: 'Huvudmannens adress',
      person2: 'Barn',
      address2: 'Barnets adress'
    },
    childAgeLanguage: {
      title: 'Barns språk och åldrar i enheter',
      description:
        'Rapporten listar antal barn i enheter enligt språk och ålder. Endast mottagna platser beaktas.'
    },
    childDocuments: {
      title: 'Pedagogiska dokument -rapport',
      description:
        'Rapporten visar det nuvarande läget för pedagogiska dokument i de enheter du valt.',
      info: 'Siffrorna anger för hur många barn det finns något av de valda dokumenten i det aktuella tillståndet.',
      info2:
        'Inga dokument och Barn totalt kolumnerna räknar endast de barn för vilka det är möjligt att skapa något av de valda dokumenten.',
      filters: {
        units: 'Enheter',
        templates: 'Dokument'
      },
      table: {
        unitOrGroup: 'Enhet/Grupp',
        draft: 'Utkast',
        prepared: 'Upprättad',
        completed: 'Färdig',
        none: 'Inga dokument',
        total: 'Barn totalt',
        expand: 'Visa grupper',
        collapse: 'Dölj grupper'
      },
      categories: {
        VASU: 'Plan för småbarnspedagogik',
        LEOPS_HOJKS:
          'Plan för genomförande av barnspecifikt stöd inom förskoleundervisningen /IP',
        OTHER: 'Andra dokument'
      }
    },
    assistanceNeedsAndActions: {
      title: 'Barns stödbehov och stödåtgärder',
      description:
        'Rapporten listar antal barn i enheter och grupper enligt stödbehovets grunder och stödåtgärder. Endast mottagna platser beaktas.',
      type: 'Stödnivå',
      types: {
        DAYCARE: 'i småbarnspedagogiken',
        PRESCHOOL: 'i förskolan'
      },
      placementType: 'Placeringstyp',
      level: 'Stödnivå och andra åtgärder',
      showZeroRows: 'Visa nollrader',
      decisionDocuments: 'Visa beslutsdokument',
      groupingTypes: {
        NO_GROUPING: 'Barn',
        AREA: 'Verksamhetsenheter per område',
        UNIT: 'Verksamhetsenheter'
      },
      basisMissing: 'Grund saknas',
      action: 'Stödåtgärd',
      actionMissing: 'Stödåtgärd saknas',
      assistanceNeedVoucherCoefficient:
        'Förhöjd koefficient för småbarnspedagogik'
    },
    occupancies: {
      title: 'Fyllnads- och utnyttjandegrader',
      description:
        'Rapporten erbjuder uppgifter om en serviceområdes och en månads utnyttjande- eller fyllnadsgrader.',
      filters: {
        areaPlaceholder: 'Välj serviceområde',
        unitPlaceholder: 'Välj enhet',
        type: 'Typ',
        types: {
          UNITS: {
            CONFIRMED: 'Bekräftad beläggningsgrad i enheten',
            PLANNED: 'Planerad beläggningsgrad i enheten',
            DRAFT: 'Utkast till beläggningsgrad i enheten',
            REALIZED: 'Beläggningsgrad i enheten'
          },
          GROUPS: {
            CONFIRMED: 'Bekräftad beläggningsgrad i grupper',
            PLANNED: 'Planerad beläggningsgrad i grupper',
            DRAFT: 'Utkast till beläggningsgrad i grupper',
            REALIZED: 'Gruppernas belstningsgrad'
          }
        },
        valueOnReport: 'Visa uppgifter',
        valuesOnReport: {
          percentage: 'I procent',
          headcount: 'I antal',
          raw: 'Summa och antal vårdansvariga separat'
        }
      },
      unitsGroupedByArea: 'Verksamhetsenheter per område',
      average: 'Medelvärde',
      sumUnder3y: 'Under 3 år',
      sumOver3y: 'Över 3 år',
      sum: 'Summa',
      caretakers: 'Vårdansvariga',
      missingCaretakersLegend: 'antal vårdansvariga saknas'
    },
    incompleteIncomes: {
      title: 'Saknade inkomstuppgifter',
      description:
        'Rapport över föräldrar vars inkomstuppgifter har föråldrats, men barnet har fortfarande en aktiv placering.',
      validFrom: 'Startdatum',
      fullName: 'Namn',
      daycareName: 'Daghem',
      careareaName: 'Serviceområde'
    },
    invoices: {
      title: 'Fakturornas avstämning',
      description:
        'Fakturornas avstämningsrapport för jämförelse med faktureringssystemet',
      period: 'Faktureringsperiod',
      areaCode: 'Område',
      amountOfInvoices: 'Fakturor',
      totalSumCents: 'Summa',
      amountWithoutSSN: 'Utan personbeteckning',
      amountWithoutAddress: 'Utan adress',
      amountWithZeroPrice: 'Nollafakturor'
    },
    nekkuOrders: {
      title: 'Nekku beställningar',
      description: 'Rapport över genomförda Nekku beställningar',
      tooLongRange: 'Du kan hämta rapporten för högst en månads tid.',
      sku: 'Produktnummer',
      quantity: 'Mängd',
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
        VEGAN: 'Veganskt',
        VEGETABLE: 'Vegetariskt'
      },
      specialDiets: 'Specialkost',
      nekkuOrderInfo: 'Beställningsinfo',
      nekkuOrderTime: 'Beställningstidpunkt'
    },
    startingPlacements: {
      title: 'Barn som börjar i småbarnspedagogiken',
      description:
        'Rapport till FPA över barn som börjar i småbarnspedagogiken.',
      ssn: 'Personbeteckning',
      childLastName: 'Barnets efternamn',
      childFirstName: 'Barnets förnamn',
      placementStart: 'Börjar i småbarnspedagogiken',
      reportFileName: 'nybörjarplaceringar'
    },
    voucherServiceProviders: {
      title: 'Servicesedelenheter',
      description:
        'Servicesedelsummor riktade till servicesedelenheter samt barnspecifika avgifter.',
      filters: {
        areaPlaceholder: 'Välj serviceområde',
        allAreas: 'Alla områden',
        unitPlaceholder: 'Sök med enhetens namn',
        separate: 'Grunddelar och förhöjningsdelar separat'
      },
      locked: 'Rapport låst',
      childCount: 'Antal barn med servicesedel',
      sumBeforeAssistanceNeed: 'Grunddelens summa / mån',
      assistanceNeedSum: 'Förhöjningsdelens summa / mån',
      unitVoucherSum: 'Servicesedelns summa / mån',
      average: 'Medelvärde',
      breakdown: 'Specifikation'
    },
    voucherServiceProviderUnit: {
      title: 'Servicesedelbarn i enheten',
      unitPageLink: 'Enhetens sida',
      month: 'Månad',
      total: 'Servicesedlarnas summa under vald månad',
      child: 'Barnets namn / födelsedatum',
      childFirstName: 'Förnamn',
      childLastName: 'Efternamn',
      note: 'Observationer',
      numberOfDays: 'Dagar',
      start: 'Från och med',
      end: 'Till och med',
      serviceVoucherValue: 'Servicesedelns högsta värde',
      serviceVoucherRealizedValueBeforeAssistanceNeed: 'Grunddel / mån',
      serviceVoucherRealizedAssistanceNeedValue: 'Förhöjningsdel / mån',
      serviceVoucherRealizedValue: 'Servicesedelvärde / mån',
      serviceVoucherFinalCoPayment: 'Självrisk',
      serviceNeed: 'Servicebehov',
      assistanceNeed: 'Stödbehov',
      partTime: 'Del/Hel',
      type: {
        NEW: 'Nytt beslut',
        REFUND: 'Gottgörelse',
        CORRECTION: 'Korrigering'
      }
    },
    nonSsnChildren: {
      title: 'Barn utan personbeteckning',
      description:
        'Rapport över placerade barn utan personbeteckning för kontroll av OID-uppgifter',
      childName: 'Barnets namn',
      dateOfBirth: 'Födelsedatum',
      personOid: 'Barnets uppgifters OID',
      lastSentToVarda: 'Exporterat till Varda senast',
      lastSentToKoski: 'Exporterat till Koski senast',
      total: 'Totalt'
    },
    placementCount: {
      title: 'Antal placeringar',
      description:
        'Rapport över antal placeringar i enheter enligt sökkriterier på angivet datum',
      noCareAreasFound: 'Inga serviceområden med placeringar',
      examinationDate: 'Granskningsdag',
      careArea: 'Serviceområde',
      daycaresByArea: 'Verksamhetsenheter per område',
      placementCount: 'Barn totalt',
      calculatedPlacements: 'Kalkylerat antal',
      providerType: 'Organisationsform',
      placementType: 'Placeringstyp',
      placementsOver3: 'Minst 3 år',
      placementsUnder3: 'Under 3 år',
      total: 'Totalt'
    },
    placementGuarantee: {
      title: 'Platsgaranti för småbarnspedagogik',
      description: 'Rapporten visar barn med platsgaranti för småbarnspedagogik'
    },
    placementSketching: {
      title: 'Utkastrapport för förskoleplaceringar',
      description:
        'Rapport över mottagna förskoleansökningar som hjälp vid placering',
      placementStartDate: 'Kontrolldag för nuvarande placering',
      earliestPreferredStartDate: 'Tidigaste sökta startdatum',
      preferredUnit: 'Önskemål',
      currentUnit: 'Nuvarande enhet',
      streetAddress: 'Adress',
      postalCode: 'Postnummer',
      tel: 'Telefonsamtal',
      email: 'e-post',
      dob: 'Födelsedatum',
      serviceNeedOption: 'Servicebehov',
      assistanceNeed: 'Stödbehov',
      preparatory: 'Förberedande',
      siblingBasis: 'Syskongrund',
      connected: 'Relaterad',
      applicationStatus: 'Ansökningens status',
      preferredStartDate: 'Önskat startdatum',
      sentDate: 'Avsändningsdatum',
      otherPreferredUnits: 'Andra önskemål',
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
      description: 'Fel som inträffat vid uppdatering av Varda-barn',
      updated: 'Senast uppdaterad',
      age: 'Ålder (dagar)',
      child: 'Barn',
      error: 'Fel',
      updateChild: 'Skicka på nytt'
    },
    vardaUnitErrors: {
      title: 'Varda-enhetsfel',
      description: 'Fel som inträffat vid uppdatering av Varda-enheter',
      age: 'Felets ålder (dagar)',
      unit: 'Enhet',
      error: 'Fel'
    },
    titaniaErrors: {
      title: 'Titania-fel',
      description: 'Fel funna i skiftlistor importerade från Titania',
      header: 'Titania-export',
      date: 'Datum',
      shift1: 'Första skiftet',
      shift2: 'Överlappande skift'
    },
    sextet: {
      title: 'Kuusikko kommunernas jämförelse',
      description:
        'Rapport över årets verkliga närvarotdagar per enhet och placeringstyp',
      placementType: 'Placeringstyp',
      year: 'År',
      unitName: 'Enhet',
      attendanceDays: 'Verkliga närvarotdagar'
    },
    invoiceGeneratorDiff: {
      title: 'Faktureringsgeneratorernas skillnader',
      description:
        'Verktyg för att analysera ny faktureringsgenerator vs gammal faktureringsgenerator',
      report: 'Rapport över faktureringsgeneratorernas skillnader'
    },
    futurePreschoolers: {
      title: 'Blivande förskolebarn',
      description:
        'Rapport över nästa års förskoleundervisningens barn och enheter för automatisk placeringsverktyg',
      futurePreschoolersCount: (count: number) =>
        count === 1
          ? `${count} blivande förskolebarn`
          : `${count} blivande förskolebarn`,
      preschoolUnitCount: (count: number) =>
        count === 1
          ? `${count} förskoleundervisningsenhet`
          : `${count} förskoleundervisningsenheter`,
      sourceUnitCount: (count: number) =>
        count === 1
          ? `${count} nuvarande enhet för blivande förskolebarn`
          : `${count} nuvarande enheter för blivande förskolebarn`
    },
    meals: {
      title: 'Antal ätande',
      description: 'Beräknar bokningsbaserade antal ätande per enhet.',
      wholeWeekLabel: 'Hela veckan',
      jamixSend: {
        button: 'Skicka igen till Jamix',
        confirmationTitle: 'Skickas matbeställningarna igen till Jamix?'
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
        mealCount: 'antal st.',
        dietId: 'Specialdietens identifierare',
        dietAbbreviation: 'Specialdiet förkortning',
        mealTextureId: 'Konsistensidentifierare',
        mealTextureName: 'Konsistens',
        additionalInfo: 'Tilläggsinfo'
      }
    },
    preschoolAbsences: {
      title: 'Frånvarorapport för förskola',
      description:
        'Rapporten listar barnspecifika frånvaroantal för vald enhet och grupp under förskoleperioden',
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
          label: 'Förskoleenhet:',
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
      title: 'Föreslagen FO-rapport kommande förskolebarn',
      description:
        'Rapporten visar ansökningar som ingår i den föreslagande förskolebesluts processen',
      columns: {
        applicationUnitName: 'Enhet',
        childLastName: 'Efternamn',
        childFirstName: 'Förnamn',
        childDateOfBirth: 'Födelsedatum',
        childStreetAddress: 'Postadress',
        childPostalCode: 'Postnu00ADmmer',
        childPostalCodeFull: 'Postnummer',
        currentUnitName: 'Nuvarande enhet',
        isDaycareAssistanceNeed: 'Stödbehov'
      }
    },
    holidayPeriodAttendance: {
      title: 'Semesterenkätsrapport',
      description:
        'Enhetens uppföljning av närvaro på dagsnivå under tiden för semesterenkäten',
      periodFilter: 'Semesterenkät',
      periodFilterPlaceholder: 'Välj semesterenkät',
      unitFilter: 'Enhet',
      groupFilter: 'Gruppval',
      groupFilterPlaceholder: 'Hela enheten',
      fetchButton: 'Sök',
      dateColumn: 'Dag',
      presentColumn: 'Närvarande',
      assistanceColumn: 'Av närvarande de med stödåtgärder',
      occupancyColumn: 'Närvarande totalt (koefficient)',
      occupancyColumnInfo:
        'I koefficienten räknas alla närvarande barns koefficient totalt. Koefficienten påverkas till exempel av barnets ålder och stödbehov.',
      staffColumn: 'Personal kommunens behov',
      absentColumn: 'Frånvarande',
      noResponseColumn: 'Svarade inte',
      moreText: 'mer'
    },
    holidayQuestionnaire: {
      title: 'Frånvaroenkätsrapport',
      description:
        'Enhetens uppföljning av närvaro på dagsnivå under frånvaroenkäten',
      questionnaireFilter: 'Frånvaroenkät',
      questionnaireFilterPlaceholder: 'Välj frånvaroenkät',
      unitFilter: 'Enhet',
      groupFilter: 'Gruppval',
      groupFilterPlaceholder: 'Hela enheten',
      fetchButton: 'Sök',
      dateColumn: 'Dag',
      presentColumn: 'Närvarande',
      assistanceColumn: 'Av närvarande de med stödåtgärder',
      occupancyColumn: 'Närvarande totalt (koefficient)',
      occupancyColumnInfo:
        'I koefficienten räknas alla närvarande barns koefficient totalt. Koefficienten påverkas till exempel av barnets ålder och stödbehov.',
      staffColumn: 'Personal kommunens behov',
      absentColumn: 'Frånvarande',
      noResponseColumn: 'Svarade inte',
      moreText: 'mer'
    },
    tampereRegionalSurvey: {
      title: 'Tammerfors områdes regionutredning',
      description:
        'Rapporten samlar kommunens årliga regionutredningens uppgifter som nedladdningsbara CSV-filer',
      monthlyReport: 'Regionutredningens månatliga antal',
      ageStatisticsReport: 'Regionutredningens åldersfördelningar',
      yearlyStatisticsReport: 'Regionutredningens årliga antal',
      municipalVoucherReport:
        'Regionutredningens servicesedelns antal per lokationskommun',
      reportLabel: 'Regionutredning',
      monthlyColumns: {
        month: 'Månad',
        municipalOver3FullTimeCount:
          'Antal barn 3 år och över i heltids småbarnspedagogik',
        municipalOver3PartTimeCount:
          'Antal barn 3 år och över i deltids småbarnspedagogik',
        municipalUnder3FullTimeCount:
          'Antal barn under 3 år i heltids småbarnspedagogik',
        municipalUnder3PartTimeCount:
          'Antal barn under 3 år i deltids småbarnspedagogik',
        familyUnder3Count: 'Antal barn under 3 år i familjedagvård',
        familyOver3Count: 'Antal barn 3 år och över i familjedagvård',
        municipalShiftCareCount: 'Antal i skiftvård',
        assistanceCount:
          'Barn med intensifierat och särskilt stöd / Barn som behöver särskilt eller stöd för tillväxt och lärande',
        statDay: '(läge sista dagen i månaden)'
      },
      ageStatisticColumns: {
        voucherUnder3Count: 'Antal servicesedelplatser under 3 år',
        voucherOver3Count: 'Antal servicesedelplatser 3 år och över',
        purchasedUnder3Count: 'Antal köpta tjänsteplatser under 3 år',
        purchasedOver3Count: 'Antal köpta tjänsteplatser 3 år och över',
        clubUnder3Count: 'Antal klubbplatser under 3 år',
        clubOver3Count: 'Antal klubbplatser 3 år och över',
        nonNativeLanguageUnder3Count: 'Antal främmandespråkiga under 3 år',
        nonNativeLanguageOver3Count: 'Antal främmandespråkiga 3 år och över',
        effectiveCareDaysUnder3Count:
          'Småbarnspedagogikens vårddagar under 3 år',
        effectiveCareDaysOver3Count:
          'Småbarnspedagogikens vårddagar 3 år och över',
        effectiveFamilyDaycareDaysUnder3Count:
          'Familjedagvårdens vårddagar under 3 år',
        effectiveFamilyDaycareDaysOver3Count:
          'Familjedagvårdens vårddagar 3 år och över',
        languageStatDay: '(läge 30.11.)'
      },
      yearlyStatisticsColumns: {
        voucherTotalCount: 'Antal servicesedlar',
        voucherAssistanceCount: 'Antal barn med stöd i servicesedelenheter',
        voucher5YearOldCount: '5-åringar i servicesedelenheter',
        purchased5YearlOldCount: '5-åringar i köpta tjänsteenheter',
        municipal5YearOldCount: '5-åringar i kommunala enheter',
        familyCare5YearOldCount: '5-åringar i familjedagvård',
        club5YearOldCount: '5-åringar i klubbverksamhet',
        preschoolDaycareUnitCareCount:
          'Kompletterande småbarnspedagogikens barn i småbarnspedagogikenheter',
        preschoolDaycareSchoolCareCount:
          'Kompletterande småbarnspedagogikens barn i skolor',
        preschoolDaycareFamilyCareCount:
          'Kompletterande småbarnspedagogikens barn i familjedagvård',
        preschoolDaycareUnitShiftCareCount:
          'Kompletterande småbarnspedagogikens skiftvårds barn i småbarnspedagogikenheter',
        preschoolDaycareSchoolShiftCareCount:
          'Kompletterande småbarnspedagogikens skiftvårds barn i skolor',
        voucherGeneralAssistanceCount:
          'Antal barn med allmänt stöd (servicesedel)',
        voucherSpecialAssistanceCount:
          'Antal barn med särskilt stöd (servicesedel)',
        voucherEnhancedAssistanceCount:
          'Antal barn med intensifierat stöd (servicesedel)',
        municipalGeneralAssistanceCount:
          'Antal barn med allmänt stöd (kommunal)',
        municipalSpecialAssistanceCount:
          'Antal barn med särskilt stöd (kommunal)',
        municipalEnhancedAssistanceCount:
          'Antal barn med intensifierat stöd (kommunal)',
        statDay: '(läge 15.12.)'
      },
      municipalVoucherColumns: {
        statDay: '(läge 15.12.)',
        municipality: 'Lokationskommun',
        under3VoucherCount: 'Servicesedlar under 3 år',
        over3VoucherCount: 'Servicesedlar 3 år och över'
      }
    },
    citizenDocumentResponseReport: {
      title: 'Kommuninnvånarens dokument',
      description:
        'Rapporten listar gruppvis kommuninvånarens senaste dokumentsvar på ja/nej- eller flervalsfrågorna',
      filters: {
        unit: 'Enhet',
        group: 'Grupp',
        template: 'Dokument',
        showBackupChildren: 'Visa även de med reservplacering'
      },
      headers: {
        name: 'Namn',
        answeredAt: 'Besvarat'
      },
      noSentDocument: 'Inget skickat dokument',
      noAnswer: 'Ej besvarat'
    }
  },
  unitEditor: {
    submitNew: 'Skapa enhet',
    title: {
      contact: 'Enhetens kontaktuppgifter',
      unitManager: 'Småbarnspedagogikenhetens ledares kontaktuppgifter',
      preschoolManager: 'Förskolans ledares kontaktuppgifter',
      decisionCustomization:
        'Enhetens namn på beslutet och meddelande om mottagande av plats',
      mealOrderIntegration: 'Matbeställningsintegration',
      mealtime: 'Enhetens måltidstider'
    },
    label: {
      name: 'Enhetens namn',
      openingDate: 'Enhetens startdatum',
      closingDate: 'slutdatum',
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
      shiftCare: 'Kväll- och skiftvård',
      capacity: 'Enhetens kalkylerade barnantal',
      language: 'Enhetens språk',
      withSchool: 'I anslutning till skola',
      ghostUnit: 'Ospecifierad enhet',
      integrations: 'Integrationer',
      invoicedByMunicipality: 'Faktureras från eVaka',
      ophUnitOid: 'Verksamhetsställets OID',
      ophOrganizerOid: 'Anordnarens OID',
      costCenter: 'Kostnadsställe',
      dwCostCenter: 'DW Kostnadsställe',
      financeDecisionHandler: 'Budgetbeslutens handläggare',
      additionalInfo: 'Tilläggsinfo om enheten',
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
        name: 'Förskolans ledares namn',
        phone: 'Förskolans ledares telefonnummer',
        email: 'Förskolans ledares e-postadress'
      },
      decisionCustomization: {
        daycareName: 'Enhetens namn på småbarnspedagogikbeslutet',
        preschoolName: 'Enhetens namn på förskolebeslutet',
        handler: 'Mottagare av vårdnadshavarens meddelande',
        handlerAddress: 'Mottagarens adress för meddelande'
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
      nekkuMealReduction: 'Nekku-reduceringsprocent',
      nekkuNoWeekendMealOrders: 'Inga Nekku-beställningar på helger'
    },
    info: {
      varda: 'Används i Varda-integration',
      koski: 'Används i Koski-integration'
    },
    field: {
      applyPeriod: 'När önskat startdatum i tidsintervallet',
      canApplyDaycare: 'I småbarnspedagogiksökning',
      canApplyPreschool: 'I förskoleundervisningssökning',
      canApplyClub: 'I klubbsökning',
      providesShiftCare: 'Enheten erbjuder kväll- och skiftvård',
      shiftCareOpenOnHolidays: 'Skiftvården är öppen även på helgdagar',
      capacity: 'personer',
      withSchool: 'Enheten är belägen i anslutning till skola',
      ghostUnit: 'Enheten är en fantomenhet',
      uploadToVarda: 'Enhetens uppgifter skickas till Varda',
      uploadChildrenToVarda: 'Enhetens barns uppgifter skickas till Varda',
      uploadToKoski: 'Skickas till Koski-tjänsten',
      invoicedByMunicipality: 'Faktureras från eVaka',
      invoicingByEvaka: 'Enhetens fakturering sker från eVaka',
      decisionCustomization: {
        handler: [
          'Palveluohjaus',
          'Varhaiskasvatusyksikön johtaja',
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
      costCenter: '(vid fakturering från eVaka obligatorisk uppgift)',
      dwCostCenter: 'För DW kostnadsställets uppgift',
      additionalInfo:
        'Du kan skriva tilläggsinfo om enheten (syns inte för kommunmedlem)',
      phone: 't.ex. +358 40 555 5555',
      email: 'fornamn.efternamn@esbo.fi',
      url: 't.ex. https://www.esbo.fi/sv/verksamhetsställen/15585',
      streetAddress: 'Gatunamn t.ex. Björk-Mankans väg 22 B 24',
      postalCode: 'Postnummer',
      postOffice: 'Verksamhetsställe',
      location: 't.ex. 60.223038, 24.692637',
      manager: {
        name: 'Förnamn Efternamn'
      },
      decisionCustomization: {
        name: 't.ex. Morgonrodnans daghem'
      }
    },
    error: {
      name: 'Namnet saknas',
      area: 'Området saknas',
      careType: 'Verksamhetsform saknas',
      dailyPreschoolTime: 'Förskoleundervisningstiden saknas eller är felaktig',
      dailyPreparatoryTime:
        'Förberedande undervisningens tid saknas eller är felaktig',
      daycareType: 'Småbarnspedagogikens typ saknas',
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
      cannotApplyToDifferentType:
        'Ansökningstyp och serviceform motsvarar inte',
      financeDecisionHandler: 'Budgetbeslutens handläggare saknas',
      ophUnitOid: 'Enhetens OID saknas',
      ophOrganizerOid: 'Anordnarens OID saknas',
      openingDateIsAfterClosingDate: 'Startdatum är efter slutdatum',
      businessId: 'FO-nummer saknas',
      iban: 'Kontonummer saknas',
      providerId: 'Leverantörsnummer saknas',
      operationTimes: 'Felaktig anteckning i enhetens verksamhetstider',
      shiftCareOperationTimes:
        'Felaktig anteckning i enhetens skiftvårdens verksamhetstider',
      mealTimes: 'Felaktig anteckning i enhetens måltidstider',
      closingDateBeforeLastPlacementDate: (lastPlacementDate: LocalDate) =>
        `Enheten har placeringar till och med ${lastPlacementDate.format()}. Alla placeringar och reservplaceringar måste avslutas senast enhetens slutdatum, inklusive eventuella framtida placeringar.`
    },
    warning: {
      onlyMunicipalUnitsShouldBeSentToVarda:
        'Skicka inte till Varda andra än kommunala och kommunala köpta tjänsteenheters uppgifter.',
      handlerAddressIsMandatory:
        'Mottagarens adress för meddelande är obligatorisk, om enhetens arrangemangsform har valts som kommunal, köpt tjänst eller servicesedel.'
    },
    closingDateModal: 'Sätt slutdatum'
  },
  fileUpload: {
    download: {
      modalHeader: 'Filbehandlingen pågår',
      modalMessage: 'Filen kan inte öppnas just nu. Försök igen om en stund.',
      modalClose: 'Stäng'
    }
  },
  messages: {
    inboxTitle: 'Meddelanden',
    emptyInbox: 'Denna mapp är tom',
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
        'Du har inte rättigheter till gruppen. Begär rättigheter av din ledare.'
    },
    messageBoxes: {
      names: {
        received: 'Mottagna',
        sent: 'Skickade',
        drafts: 'Utkast',
        copies: 'Ledares/kommunens meddelanden',
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
        copies: 'Ledares/kommunens meddelanden',
        archive: 'Arkiv',
        thread: 'Meddelandetråd'
      }
    },
    types: {
      MESSAGE: 'Meddelande',
      BULLETIN: 'Meddelande'
    },
    recipientSelection: {
      title: 'Mottagare',
      childName: 'Namn',
      childDob: 'Födelsedatum',
      receivers: 'Mottagare',
      confirmText: 'Skicka meddelande till valda',
      starters: 'barn som börjar'
    },
    noTitle: 'Ingen rubrik',
    notSent: 'Ej skickat',
    editDraft: 'Redigera utkast',
    undo: {
      info: 'Meddelande skickat',
      secondsLeft: (s: number) =>
        s === 1 ? '1 sekunti aikaa' : `${s} sekuntia aikaa`
    },
    sensitive: 'känsligt',
    customer: 'Klient',
    applicationTypes: {
      PRESCHOOL: 'Förskoleansökan',
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
        title: (count: number) => `Meddelandet har ${count} mottagare.`,
        text: (count: number) =>
          `Detta meddelande kommer att skickas till ${count} mottagare. Är du säker på att du vill skicka meddelandet?`
      },
      type: {
        label: 'Meddelandetyp',
        message: 'Meddelande',
        bulletin: 'Nyhetsbrev (kan inte besvaras)'
      },
      flags: {
        heading: 'Meddelandets tilläggsmarkeringar',
        urgent: {
          info: 'Skicka meddelandet som brådskande endast om du vill att vårdnadshavaren läser det under arbetsdagen.',
          label: 'Brådskande'
        },
        sensitive: {
          info: 'Att öppna ett känsligt meddelande kräver stark autentisering från kommuninvånaren.',
          label: 'Känsligt',
          whyDisabled:
            'Ett känsligt meddelande kan endast skickas från ett personligt användarkonto till vårdnadshavare för ett enskilt barn.'
        }
      },
      sender: 'Avsändare',
      selectPlaceholder: 'Välj...',
      filters: {
        showFilters: 'Visa tilläggsinställningar',
        hideFilters: 'Dölj tilläggsinställningar',
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
      'På den här sidan kan du ange din egen personliga PIN-kod för eVaka-mobil. PIN-koden används i eVaka-mobil för att granska',
    text2: 'information som finns bakom låset.',
    text3: 'Obs!',
    text4:
      'Lämna inte ut din PIN-kod till någon annan person. Vid behov kan du byta PIN-kod när som helst.',
    text5:
      'PIN-koden ska innehålla fyra (4) siffror. De vanligaste sifferkombinationerna (t.ex. 1234) godkänns inte.',
    pinCode: 'PIN-kod',
    button: 'Spara PIN-kod',
    placeholder: '4 siffror',
    error: 'För enkel PIN-kod eller PIN-kod innehåller bokstäver',
    locked: 'PIN-koden är låst, byt den till en ny',
    lockedLong:
      'PIN-koden har matats in fel 5 gånger i eVaka-mobil, och koden är låst. Vänligen byt till en ny PIN-kod.',
    link: 'eVaka-mobilens PIN-kod',
    unsavedDataWarning: 'Du har inte sparat en PIN-kod'
  },
  employees: {
    name: 'Namn',
    email: 'E-post',
    rights: 'Rättigheter',
    lastLogin: 'Senast inloggad',
    employeeNumber: 'Personnummer',
    temporary: 'Tillfällig vikarie',
    findByName: 'Sök efter namn',
    activate: 'Aktivera',
    activateConfirm: 'Vill du återställa användaren till aktiv?',
    deactivate: 'Inaktivera',
    deactivateConfirm: 'Vill du inaktivera användaren?',
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
        endDate: 'Behörighet upphör',
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
            `Överlappande rättigheter ersätts från och med ${date.format()}.`,
          currentRemoved: 'Dessa behörigheter tas bort.',
          scheduledRemoved: 'Dessa kommande behörigheter tas bort.'
        }
      },
      mobile: {
        title: 'Personliga mobilenheter',
        name: 'Enhetens namn',
        nameless: 'Namnlös enhet',
        deleteConfirm: 'Vill du ta bort användarens mobilenhetsparkoppling?'
      }
    },
    createNewSsnEmployee: 'Skapa ny användare med personnummer',
    newSsnEmployeeModal: {
      title: 'Lägg till ny användare med personnummer',
      createButton: 'Skapa konto',
      ssnConflict: 'Personnummer är redan i bruk'
    },
    hasSsn: 'Användare med personnummer'
  },
  financeBasics: {
    fees: {
      title: 'Klientavgifter',
      add: 'Skapa nya klientavgifter',
      thresholds: 'Inkomstgränser',
      validDuring: 'Klientavgifter för perioden',
      familySize: 'Familjestorlek',
      minThreshold: 'Minsta bruttoinkomst €/mån',
      maxThreshold: 'Bruttoinkomstgräns för högsta avgift €/mån',
      maxFeeError: 'Högsta avgift stämmer inte',
      thresholdIncrease: 'Höjning av inkomstgräns när familjestorlek > 6',
      thresholdIncreaseInfo:
        'Om familjens storlek är större än 6, höjs inkomstgränsen som ligger till grund för avgiften med höjningsbeloppet för varje följande minderårigt barn i familjen.',
      multiplier: 'Avgift %',
      maxFee: 'Högsta avgift',
      minFee: 'Minsta barnspecifika avgift som tas ut',
      siblingDiscounts: 'Syskonrabatter',
      siblingDiscount2: 'Rabatt% 1:a syskon',
      siblingDiscount2Plus: 'Rabatt% övriga syskon',
      temporaryFees: 'Avgifter för tillfällig småbarnspedagogik',
      temporaryFee: 'Grundpris',
      temporaryFeePartDay: 'Halvdagsplats',
      temporaryFeeSibling: 'Grundpris, andra barn',
      temporaryFeeSiblingPartDay: 'Halvdagsplats, andra barn',
      errors: {
        'date-overlap':
          'Avgiftsinställningarna överlappar med en annan giltig inställning. Uppdatera giltighetstiden för de andra avgiftsinställningarna först.'
      },
      modals: {
        editRetroactive: {
          title: 'Vill du verkligen redigera uppgifterna?',
          text: 'Vill du verkligen redigera avgiftsuppgifter som redan är i bruk? Om du redigerar uppgifterna skapas ett retroaktivt avgifts- eller värdebeslut för alla klienter som ändringen gäller.',
          resolve: 'Redigera',
          reject: 'Redigera inte'
        },
        saveRetroactive: {
          title: 'Vill du spara avgiftsinställningarna retroaktivt?',
          text: 'Du håller på att spara avgiftsinställningar som påverkar retroaktivt. Om du sparar uppgifterna skapas ett nytt retroaktivt avgifts- eller värdebeslut för alla klienter som påverkas av ändringen.',
          resolve: 'Spara',
          reject: 'Avbryt'
        }
      }
    },
    serviceNeeds: {
      title: 'Servicebehov',
      add: 'Lägg till nytt servicesedelvärde',
      voucherValues: 'Servicesedlarnas värden',
      validity: 'Giltighetstid',
      baseValue: (
        <>
          Grundvärde,
          <br />3 år eller äldre (€)
        </>
      ),
      coefficient: (
        <>
          Koefficient,
          <br />3 år eller äldre
        </>
      ),
      value: (
        <>
          Maxvärde,
          <br />3 år eller äldre (€)
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
          'Giltighetstiden kan inte börja före startdatumet för ett annat servicesedelvärde',
        'end-date-overlap':
          'Giltighetstiden kan inte börja före dagen efter föregående servicesedels slutdatum',
        'date-gap': 'Det får inte finnas luckor mellan giltighetstiderna',
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
      PEDAGOGICAL_REPORT: 'Pedagogisk utredning',
      PEDAGOGICAL_ASSESSMENT: 'Pedagogisk bedömning',
      HOJKS: 'IP',
      MIGRATED_VASU: 'Småbarnspedagogisk plan (migrerad)',
      MIGRATED_LEOPS: 'Förskoleplan (migrerad)',
      MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION:
        'Beslut om stöd i småbarnspedagogik (migrerat)',
      MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION:
        'Beslut om stöd i förskola (migrerat)',
      VASU: 'Småbarnspedagogisk plan',
      LEOPS: 'Förskoleplan',
      CITIZEN_BASIC: 'Dokument som fylls i tillsammans med vårdnadshavare',
      OTHER_DECISION: 'Beslutsdokument',
      OTHER: 'Annat barndokument'
    },
    documentTypeInfos: {
      CITIZEN_BASIC:
        'Detta är ett dokument som både kommuninvånare och personal kan fylla i. Vid behov kan personalen svara på frågorna först, varefter dokumentet kan skickas till kommuninvånaren för ifyllnad i eVaka.',
      OTHER_DECISION:
        'Detta används för att göra alla beslutsmallar utom beslut relaterade till ansökningar',
      OTHER: 'Dokument om barnets pedagogik eller plan som fylls i av anställd'
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
        future: 'Kommer i bruk',
        past: 'Upphörda',
        type: 'Dokumenttyp',
        all: 'Alla',
        language: 'Språk'
      }
    },
    templateModal: {
      title: 'Ny dokumentmall',
      name: 'Namn',
      type: 'Dokumenttyp',
      placementTypes: 'I bruk vid placeringar',
      language: 'Dokumentspråk',
      confidential: 'Dokumentet är sekretessbelagt',
      confidentialityDuration: 'Sekretessperiod (år)',
      confidentialityBasis: 'Sekretessgrund (metadata och arkivering)',
      legalBasis: 'Sekretessgrund / lagrum (syns på blankett)',
      validity: 'Giltigt under perioden',
      processDefinitionNumber: 'Uppgiftsklass',
      processDefinitionNumberInfo:
        'Uppgiftsklassens nummer som definierats i informationsstyrningsplanen. Lämna tomt om dokumentet inte arkiveras.',
      archiveDurationMonths: 'Arkiveringstid (månader)',
      archiveExternally: 'Ska överföras till externt arkiv före radering',
      endDecisionWhenUnitChanges: 'Beslutet avbryts om barnet byter enhet'
    },
    templateEditor: {
      confidential: 'Sekretessbelagt',
      addSection: 'Nytt avsnitt',
      titleNewSection: 'Nytt avsnitt',
      titleEditSection: 'Redigera avsnitt',
      sectionName: 'Rubrik',
      infoText: 'Instruktionstext',
      addQuestion: 'Nytt avsnitt',
      titleNewQuestion: 'Ny fråga',
      titleEditQuestion: 'Redigera fråga',
      moveUp: 'Flytta upp',
      moveDown: 'Flytta ned',
      readyToPublish: 'Klar för publicering',
      forceUnpublish: {
        button: 'Avbryt publicering',
        confirmationTitle: 'Vill du verkligen avbryta publiceringen?',
        confirmationText:
          'Alla dokument som använder denna dokumentmall tas bort.'
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
        description: 'Namn på småbarnspedagogiks- och servicesedelbeslut'
      },
      DECISION_MAKER_TITLE: {
        title: 'Beslutsfattarens titel',
        description: 'Titel på småbarnspedagogiks- och servicesedelbeslut'
      }
    }
  },
  unitFeatures: {
    page: {
      title: 'Öppnade funktioner för enheter',
      unit: 'Enhet',
      selectAll: 'Välj alla',
      unselectAll: 'Ta bort alla',
      providerType: 'Enhetens verksamhetsform',
      careType: 'Enhetens vårdform',
      undo: 'Ångra föregående ändring'
    },
    pilotFeatures: {
      MESSAGING: 'Kommunikation',
      MOBILE: 'Mobil',
      RESERVATIONS: 'Kommuninvånarens kalender',
      VASU_AND_PEDADOC: 'Pedagogiska dokument och pedagogisk dokumentation',
      MOBILE_MESSAGING: 'Mobil­kommunikation',
      PLACEMENT_TERMINATION: 'Uppsägning av plats',
      REALTIME_STAFF_ATTENDANCE: 'Personalens realtidsnärvaro',
      PUSH_NOTIFICATIONS: 'Mobilnotifieringar',
      SERVICE_APPLICATIONS: 'Ansökningar om förändring av servicebehov',
      STAFF_ATTENDANCE_INTEGRATION: 'Arbetsturplanserings­integration',
      OTHER_DECISION: 'Övriga beslut',
      CITIZEN_BASIC_DOCUMENT: 'Dokument som vårdnadshavare fyller i'
    }
  },
  roles: {
    adRoles: {
      ADMIN: 'Huvudanvändare',
      DIRECTOR: 'Förvaltning',
      MESSAGING: 'Kommunikation',
      REPORT_VIEWER: 'Rapportering',
      FINANCE_ADMIN: 'Ekonomi',
      FINANCE_STAFF: 'Ekonomianställd (extern)',
      SERVICE_WORKER: 'Servicehandledning',
      SPECIAL_EDUCATION_TEACHER: 'Speciallärare',
      EARLY_CHILDHOOD_EDUCATION_SECRETARY: 'Sekretarare',
      STAFF: 'Personal',
      UNIT_SUPERVISOR: 'Ledare'
    }
  },
  welcomePage: {
    text: 'Du är inloggad på Esbo stads eVaka-tjänst. Ditt användarkonto har ännu inte beviljats rättigheter som möjliggör användning av tjänsten. Du får nödvändiga behörigheter av din egen ledare.'
  },
  validationErrors: {
    ...components.validationErrors,
    ...components.datePicker.validationErrors,
    dateRangeNotLinear: 'Periodstartdatumet ska vara före slutdatumet.',
    timeRangeNotLinear: 'Kontrollera ordning',
    guardianMustBeHeard: 'Vårdnadshavare ska höras',
    futureTime: 'Tid i framtiden'
  },
  holidayPeriods: {
    confirmDelete: 'Vill du verkligen ta bort semesterperioden?',
    createTitle: 'Skapa ny semesterperiod',
    editTitle: 'Redigera semesterperiod',
    period: 'Period',
    reservationsOpenOn: 'Enkäten öppnas',
    reservationDeadline: 'Bokningarnas tidsfrist',
    clearingAlert:
      'Kommuninvånarnas redan gjorda bokningar rensas från den valda perioden',
    confirmLabel:
      'Jag förstår att gjorda bokningar tas bort omedelbart och detta kan inte ångras.',
    validationErrors: {
      tooSoon: 'Semesterperioden kan skapas tidigast 4 veckor framåt',
      tooLong: 'Semesterperioden kan vara högst 15 veckor lång',
      afterStart: 'Kan inte vara efter början',
      afterReservationsOpen: 'Kan inte vara efter öppningsdagen'
    }
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
    description: 'Enkätens förklaring till kommuninvånare',
    descriptionLink: 'Tilläggsinfo-länk',
    active: 'Giltig',
    fixedPeriodOptionLabel: 'Periodens valfråga',
    fixedPeriodOptionLabelPlaceholder:
      'T.ex. Barn är borta 8 veckor under perioden',
    fixedPeriodOptions: 'Periodalternativ',
    fixedPeriodOptionsPlaceholder:
      '30.5.2022-24.8.2022, 6.6.2022-31.8.2022, separerade med kommatecken eller radbrytningar',
    requiresStrongAuth: 'Stark autentisering',
    conditionContinuousPlacement:
      'Man kan svara på enkäten om barnet har oavbruten placering',
    period: 'Frånvaroperiod',
    absenceTypeThreshold: 'Minimilängd för kontinuerlig frånvaro',
    days: 'dagar'
  },
  terms: {
    term: 'Läsår',
    finnishPreschool: 'Finskspråkig förskola',
    extendedTermStart: 'Förlängd läsår börjar',
    applicationPeriodStart: 'Ansökan till läsår börjar',
    termBreaks: 'Undervisningsuppehåll',
    addTerm: 'Lägg till läsår',
    confirmDelete: 'Vill du verkligen ta bort läsåret?',
    extendedTermStartInfo:
      'Tid då klientavgiften för småbarnspedagogik bestäms enligt tillhörande småbarnspedagogik.',
    termBreaksInfo:
      'Lägg till sådana tider under läsåret då undervisning inte erbjuds, t.ex. jullov.',
    addTermBreak: 'Lägg till uppehållsperiod',
    validationErrors: {
      overlap:
        'För denna tidsperiod finns redan överlappande läsår. Försök registrera uppgiften för en annan tidsperiod.',
      extendedTermOverlap:
        'För denna tidsperiod finns redan överlappande förlängd läsår. Försök registrera uppgiften för ett annat startdatum',
      extendedTermStartAfter:
        'Startdatum för förlängd läsår kan inte vara efter läsårets startdatum.',
      termBreaksOverlap: 'Överlappande undervisningsuppehåll är inte tillåtet.'
    },
    modals: {
      editTerm: {
        title: 'Vill du verkligen redigera uppgifterna?',
        text: 'Vill du verkligen redigera ett läsår som redan börjat?',
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
    caseIdentifier: 'Ärendetunnus',
    processName: 'Ärendeprocess',
    organization: 'Organisation',
    archiveDurationMonths: 'Arkiveringstid',
    primaryDocument: 'Primärt dokument',
    secondaryDocuments: 'Övriga dokument',
    documentId: 'Dokumentidentifierare',
    name: 'Dokumentnamn',
    createdAt: 'Upprättandetidpunkt',
    createdBy: 'Upprättare',
    monthsUnit: 'månader',
    confidentiality: 'Offentlighet',
    confidential: 'Sekretessbelagt',
    public: 'Offentligt',
    notSet: 'Ej angiven',
    confidentialityDuration: 'Sekretessperiod',
    confidentialityBasis: 'Sekretessgrund',
    years: 'år',
    receivedBy: {
      label: 'Ankomstsätt',
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
    history: 'Processhistoria',
    downloadPdf: 'Ladda ner PDF',
    states: {
      INITIAL: 'Ärendets initiering / -ankomst',
      PREPARATION: 'Ärendeberedning',
      DECIDING: 'Beslutfattande',
      COMPLETED: 'Verkställighet / Avslutande / Stängning'
    }
  },
  systemNotifications: {
    title: {
      CITIZENS: 'Meddelande som syns för kommuninvånare',
      EMPLOYEES: 'Meddelande som syns för personal'
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
      'Du kan skapa ansökningar i eVaka från placeringsförslag som producerats med optimeringsverktyget. Ansökningarna skapas direkt för att vänta på beslut.',
    preschoolTermNotification:
      'Ansökningarna skapas till följande förskoleperiod:',
    preschoolTermWarning:
      'eVaka saknar definition av följande förskoleperiod. Förskoleperiod behövs för att skapa ansökningar.',
    validation: (count: number, existing: number) =>
      `Du importerar ${count} placeringar${existing > 0 ? ` (varav ${existing} redan finns i systemet)` : ''}, fortsätta?`
  },
  outOfOffice: {
    menu: 'Ledarens frånvaroperiod',
    title: 'Frånvaroperiod',
    description:
      'Du kan lägga till information här om t.ex. din semester. Barnens vårdnadshavare ser ett meddelande när du är frånvarande att du inte är på plats.',
    header: 'Frånvaroperiod',
    noFutureOutOfOffice: 'Inga kommande frånvaror',
    addOutOfOffice: 'Lägg till frånvaroperiod',
    validationErrors: {
      endBeforeToday: 'Kan inte upphöra i det förflutna'
    }
  },
  decisionReasonings: {
    tabs: {
      DAYCARE: 'Småbarnspedagogik',
      PRESCHOOL: 'Förskoleundervisning'
    },
    collectionInfo: {
      DAYCARE:
        'Småbarnspedagogikmotiveringar används i beslut som gäller placeringstyper:',
      PRESCHOOL:
        'Förskolemotiveringar används i beslut som gäller placeringstyper:'
    },
    placementTypes: {
      DAYCARE: [
        'Småbarnspedagogikbeslut',
        'Deltid småbarnspedagogik',
        'Ansluten småbarnspedagogik',
        'Klubb'
      ],
      PRESCHOOL: ['Förskoleundervisning', 'Förberedande undervisning']
    },
    generic: {
      title: 'Allmänna motiveringar',
      addNew: 'Lägg till allmän motivering',
      dateSuffix: 'begynnande placeringar',
      dateLabel: 'Gäller placeringar som börjar',
      textFi: 'Text för beslutet',
      textSv: 'Text för beslutet',
      statusReady: 'I bruk',
      statusNotReady: 'Inte i bruk',
      statusOutdated: 'Föråldrad',
      notReadyWarning:
        'Beslut kan inte skickas innan motiveringen har tagits i bruk',
      outdated: 'Föråldrade',
      cancel: 'Avbryt',
      saveAsNotReady: 'Spara utan att aktivera',
      saveAndActivate: 'Ta i bruk',
      saveAndActivateConfirmTitle: 'Ta motivering i bruk',
      saveAndActivateConfirmText:
        'Observera att en allmän motivering som tagits i bruk inte kan tas bort. En befintlig allmän motivering kan dock ersättas med en ny allmän motivering som tas i bruk för samma period. Vill du ta den allmänna motiveringen i bruk?',
      edit: 'Redigera',
      delete: 'Ta bort',
      deleteConfirmTitle: 'Ta bort motivering',
      deleteConfirmText: 'Vill du ta bort den allmänna motiveringen?'
    },
    individual: {
      title: 'Individuella motiveringar',
      addNew: 'Lägg till individuell motivering',
      statusActive: 'Tillgänglig',
      statusRemoved: 'Borttagen från bruk',
      titleFi: 'Internt namn',
      titleSv: 'Internt namn',
      textFi: 'Text för beslutet',
      textSv: 'Text för beslutet',
      removed: 'Borttagen från bruk',
      cancel: 'Avbryt',
      saveAndActivate: 'Ta i bruk',
      saveAndActivateConfirmTitle: 'Ta motivering i bruk',
      saveAndActivateConfirmText:
        'Observera att om en individuell motivering tas bort från bruk senare, försvinner den inte från beslut där den redan valts. Vill du ta den individuella motiveringen i bruk?',
      removeConfirmTitle: 'Ta bort motivering från bruk',
      removeConfirmText:
        'Vill du ta bort den individuella motiveringen från bruk?'
    },
    fi: 'FI',
    sv: 'SV'
  },
  components
}
