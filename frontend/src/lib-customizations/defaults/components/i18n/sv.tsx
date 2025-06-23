// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type FiniteDateRange from 'lib-common/finite-date-range'
import type { Translations } from 'lib-components/i18n'

const components: Translations = {
  asyncButton: {
    inProgress: 'Laddar',
    failure: 'Något gick fel',
    success: 'Klar'
  },
  common: {
    add: 'Lägg till',
    cancel: 'Gå tillbaka',
    close: 'Stäng',
    confirm: 'Bekräfta',
    no: 'Nej',
    noResults: 'Inga sökresultat',
    noFirstName: 'Inget förnamn',
    noLastName: 'Inget efternamn',
    open: 'Öppna',
    remove: 'Ta bort',
    saving: 'Sparar',
    saved: 'Sparad',
    search: 'Sök',
    yes: 'Ja',
    openExpandingInfo: 'Öppna detaljer',
    showPassword: 'Visa lösenord',
    hidePassword: 'Dölj lösenord',
    userTypes: {
      // TODO
      SYSTEM: 'järjestelmä',
      CITIZEN: 'kuntalainen',
      EMPLOYEE: 'työntekijä',
      MOBILE_DEVICE: 'mobiililaite',
      UNKNOWN: 'tuntematon'
    }
  },
  datePicker: {
    placeholder: 'dd.mm.åååå',
    description:
      'Skriv in datumet i formatet dd.mm.åååå. Du kan komma till månadsväljaren med pil ned-tangenten.',
    validationErrors: {
      validDate: 'Ange i format dd.mm.åååå',
      dateTooEarly: 'Välj ett senare datum',
      dateTooLate: 'Välj ett tidigare datum'
    },
    open: 'Öppna datumväljaren',
    close: 'Stäng datumväljaren'
  },
  documentTemplates: {
    // only on employee frontend
    templateQuestions: {
      label: 'titel',
      options: 'alternativ',
      infoText: 'info text',
      multiline: 'Tillåt flerradssvar',
      withText: 'Med extra info',
      text: 'Textinnehåll',
      textFields: 'Textfält',
      addTextField: 'Lägg till ett textfält',
      allowMultipleRows: 'Tillåt flera svar',
      addRow: 'Lägg till ny rad'
    },
    noSelection: 'inget val',
    documentStates: {
      DRAFT: 'Utkast',
      PREPARED: 'Behandlad',
      CITIZEN_DRAFT: 'Täytettävänä huoltajalla (sv)',
      DECISION_PROPOSAL: 'Beslutsförslag',
      COMPLETED: 'Färdig',
      ACCEPTED: 'Godkänt',
      REJECTED: 'Avvisat',
      ANNULLED: 'Annullerat'
    }
  },
  fileUpload: {
    loading: 'Uppladdas...',
    loaded: 'Uppladdad',
    error: {
      EXTENSION_MISSING: 'Filtillägget saknas',
      EXTENSION_INVALID: 'Ogiltigt filtillägg',
      INVALID_CONTENT_TYPE: 'Ogiltig filtyp',
      FILE_TOO_LARGE: 'För stor fil (max. 25 MB)',
      SERVER_ERROR: 'Uppladdningen misslyckades'
    },
    input: {
      title: 'Lägg till bilaga',
      text: (fileTypes) =>
        'Tryck här eller dra en bilaga åt gången till lådan. Maximal storlek för filen: 25 MB. Tillåtna format: ' +
        fileTypes
          .map((type) => {
            switch (type) {
              case 'image':
                return 'bilder (JPEG, JPG, PNG)'
              case 'document':
                return 'dokument (PDF, DOC, DOCX, ODT)'
              case 'audio':
                return 'ljud'
              case 'video':
                return 'video'
              case 'csv':
                return 'CSV filer'
            }
          })
          .join(', ')
    },
    uploadFile: 'Ladda upp filen',
    deleteFile: 'Radera fil'
  },
  loginErrorModal: {
    header: 'Inloggningen misslyckades',
    message:
      'Autentiseringen för tjänsten misslyckades eller avbröts. För att logga in gå tillbaka och försök på nytt.',
    returnMessage: 'Gå tillbaka till inloggningen'
  },
  messages: {
    staffAnnotation: 'Personal',
    message: 'Meddelande',
    recipients: 'Mottagare',
    send: 'Skicka',
    sending: 'Skickar',
    types: {
      MESSAGE: 'Meddelande',
      BULLETIN: 'Anslag'
    },
    thread: {
      type: 'Meddelandetyp',
      urgent: 'Akut'
    },
    outOfOffice: {
      singleRecipient: (name: string, period: FiniteDateRange) =>
        `${name} är inte tillgänglig ${period.toString()}.`,
      multipleRecipientsHeader:
        'Dessa mottagare kan inte svara på meddelanden under följande tider.'
    }
  },
  messageReplyEditor: {
    // Currently only used for Finnish frontends
    discard: '',
    messagePlaceholder: undefined,
    messagePlaceholderSensitiveThread: undefined
  },
  sessionTimeout: {
    sessionExpiredTitle: 'Din session har gått ut',
    sessionExpiredMessage: 'Vänligen logga in igen.',
    goToLoginPage: 'Gå till inloggningssidan',
    cancel: 'Avbryt'
  },
  notifications: {
    close: 'Stäng'
  },
  offlineNotification: 'Ingen internetanslutning',
  reloadNotification: {
    title: 'En ny version av eVaka är tillgänglig',
    buttonText: 'Ladda om sidan'
  },
  treeDropdown: {
    // Currently only used for Finnish frontends
    expand: () => '',
    collapse: () => '',
    expandDropdown: ''
  },
  validationErrors: {
    required: 'Värde saknas',
    requiredSelection: 'Val saknas',
    format: 'Ange rätt format',
    integerFormat: 'Ange ett heltal',
    ssn: 'Ogiltigt personbeteckning',
    phone: 'Ogiltigt telefonnummer',
    email: 'Ogiltig e-postadress',
    preferredStartDate: 'Ogiltigt datum',
    connectedPreferredStartDate:
      'Du kan ansöka om kompletterande förskoleutbildning från början av ditt barns förskoleutbildning',
    timeFormat: 'Kolla',
    timeRequired: 'Nödvändig',
    dateTooEarly: 'Välj ett senare datum',
    dateTooLate: 'Välj ett tidigare datum',
    unitNotSelected: 'Välj minst en enhet',
    emailsDoNotMatch: 'E-postadresserna är olika',
    httpUrl: 'Ange i formen https://example.com',
    unselectableDate: 'Ogiltigt datum',
    openAttendance: 'Öppen närvaro',
    generic: 'Kolla'
  },
  metadata: {
    // TODO
    title: 'Arkistoitava metadata',
    notFound: 'Asiakirjalle ei ole arkistoitavaa metadataa',
    processNumber: 'Asianumero',
    processName: 'Asiaprosessi',
    organization: 'Organisaatio',
    archiveDurationMonths: 'Arkistointiaika',
    primaryDocument: 'Ensisijainen asiakirja',
    secondaryDocuments: 'Muut asiakirjat',
    documentId: 'Asiakirjan tunniste',
    name: 'Asiakirjan nimi',
    createdAt: 'Laatimisajankohta',
    createdBy: 'Laatija',
    monthsUnit: 'kuukautta',
    confidentiality: 'Julkisuus',
    confidential: 'Salassapidettävä',
    public: 'Julkinen',
    notSet: 'Asettamatta',
    confidentialityDuration: 'Salassapitoaika',
    confidentialityBasis: 'Salassapitoperuste',
    years: 'vuotta',
    receivedBy: {
      label: 'Saapumistapa',
      PAPER: 'Paperilla',
      ELECTRONIC: 'Sähköisesti'
    },
    sfiDelivery: {
      label: 'Suomi.fi -toimitukset',
      method: {
        ELECTRONIC: 'Sähköisesti',
        PAPER_MAIL: 'Postitse',
        PENDING: 'Odottaa toimitusta'
      }
    },
    history: 'Prosessin historia',
    downloadPdf: 'Lataa PDF',
    states: {
      INITIAL: 'Asian vireillepano / -tulo',
      PREPARATION: 'Asian valmistelu',
      DECIDING: 'Päätöksenteko',
      COMPLETED: 'Toimeenpano / Päättäminen / Sulkeminen'
    },
    section: {
      show: 'Näytä lisätiedot',
      hide: 'Piilota lisätiedot'
    }
  }
}

export default components
