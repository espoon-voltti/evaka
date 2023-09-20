// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Translations } from 'lib-components/i18n'

const components: Translations = {
  asyncButton: {
    inProgress: 'Loading',
    failure: 'Failed to load',
    success: 'Success'
  },
  common: {
    add: 'Add',
    cancel: 'Cancel',
    close: 'Close',
    confirm: 'Confirm',
    no: 'No',
    remove: 'Remove',
    yes: 'Yes'
  },
  datePicker: {
    placeholder: 'dd.mm.yyyy',
    description:
      'Type the date in dd.mm.yyyy format. You can get to month picker with the tab key.',
    validationErrors: {
      validDate: 'Valid date format is dd.mm.yyyy',
      dateTooEarly: 'Pick a later date',
      dateTooLate: 'Pick an earlier date'
    }
  },
  documentTemplates: {
    // only on employee frontend
    templateQuestions: {
      label: 'Label',
      options: 'Options',
      infoText: 'Info text',
      multiline: 'Allow multiline answer',
      withText: 'With extra info'
    },
    noSelection: 'No selection',
    documentStates: {
      DRAFT: 'Draft',
      PREPARED: 'Ready',
      COMPLETED: 'Completed'
    }
  },
  loginErrorModal: {
    header: 'Login failed',
    message:
      'The identification process failed or was stopped. To log in, go back and try again.',
    returnMessage: 'Go back'
  },
  notifications: {
    close: 'Close'
  },
  offlineNotification: 'No internet connection',
  reloadNotification: {
    title: 'New version of eVaka is available',
    buttonText: 'Reload page'
  },
  validationErrors: {
    required: 'Value missing',
    requiredSelection: 'Please select one of the options',
    format: 'Give value in correct format',
    integerFormat: 'Give an integer value',
    ssn: 'Invalid person identification number',
    phone: 'Invalid telephone number',
    email: 'Invalid email',
    preferredStartDate: 'Invalid preferred start date',
    timeFormat: 'Check',
    timeRequired: 'Required',
    unitNotSelected: 'Pick at least one choice',
    emailsDoNotMatch: 'The emails do not match',
    httpUrl: 'Valid url format is https://example.com',
    unselectableDate: 'Invalid date',
    openAttendance: 'Open attendance'
  }
}

export default components
