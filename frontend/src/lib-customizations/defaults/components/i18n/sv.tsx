// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Translations } from 'lib-components/i18n'

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
    open: 'Öppna',
    remove: 'Ta bort',
    saving: 'Sparar',
    saved: 'Sparad',
    search: 'Sök',
    yes: 'Ja',
    openExpandingInfo: 'Öppna detaljer'
  },
  datePicker: {
    placeholder: 'dd.mm.åååå',
    description:
      'Skriv in datumet i formatet dd.mm.åååå. Du kan komma till månadsväljaren med tabbtangenten.',
    validationErrors: {
      validDate: 'Ange i format dd.mm.åååå',
      dateTooEarly: 'Välj ett senare datum',
      dateTooLate: 'Välj ett tidigare datum'
    }
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
      COMPLETED: 'Färdig'
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
    deleteFile: 'Radera fil'
  },
  loginErrorModal: {
    header: 'Inloggningen misslyckades',
    message:
      'Autentiseringen för tjänsten misslyckades eller avbröts. För att logga in gå tillbaka och försök på nytt.',
    inactiveUserMessage:
      'Användaren har avaktiverats. Vänligen kontakta systemadministratören.',
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
    }
  },
  messageEditor: {
    // Currently only used for Finnish frontends
    newMessage: '',
    to: {
      label: '',
      placeholder: '',
      noOptions: ''
    },
    type: {
      label: '',
      message: '',
      bulletin: ''
    },
    flags: {
      heading: '',
      urgent: {
        info: '',
        label: ''
      },
      sensitive: {
        info: '',
        label: '',
        whyDisabled: ''
      }
    },
    sender: '',
    recipientsPlaceholder: '',
    title: '',
    deleteDraft: ''
  },
  messageReplyEditor: {
    // Currently only used for Finnish frontends
    discard: '',
    messagePlaceholder: undefined,
    messagePlaceholderSensitiveThread: undefined
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
    timeFormat: 'Kolla',
    timeRequired: 'Nödvändig',
    dateTooEarly: 'Välj ett senare datum',
    dateTooLate: 'Välj ett tidigare datum',
    unitNotSelected: 'Välj minst en enhet',
    emailsDoNotMatch: 'E-postadresserna är olika',
    httpUrl: 'Ange i formen https://example.com',
    unselectableDate: 'Ogiltigt datum',
    openAttendance: 'Öppen närvaro'
  }
}

export default components
