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
    remove: 'Ta bort',
    yes: 'Ja'
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
      withText: 'Med extra info'
    },
    noSelection: 'inget val',
    documentStates: {
      DRAFT: 'Utkast',
      PREPARED: 'Behandlad',
      COMPLETED: 'Färdig'
    }
  },
  loginErrorModal: {
    header: 'Inloggningen misslyckades',
    message:
      'Autentiseringen för tjänsten misslyckades eller avbröts. För att logga in gå tillbaka och försök på nytt.',
    returnMessage: 'Gå tillbaka till inloggningen'
  },
  notifications: {
    close: 'Stäng'
  },
  offlineNotification: 'Ingen internetanslutning',
  reloadNotification: {
    title: 'En ny version av eVaka är tillgänglig',
    buttonText: 'Ladda om sidan'
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
    unitNotSelected: 'Välj minst en enhet',
    emailsDoNotMatch: 'E-postadresserna är olika',
    httpUrl: 'Ange i formen https://example.com',
    unselectableDate: 'Ogiltigt datum',
    openAttendance: 'Öppen närvaro'
  }
}

export default components
