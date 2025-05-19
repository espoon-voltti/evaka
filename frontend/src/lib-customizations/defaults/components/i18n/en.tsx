// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type FiniteDateRange from 'lib-common/finite-date-range'
import type { Translations } from 'lib-components/i18n'

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
    noResults: 'No results',
    open: 'Open',
    remove: 'Remove',
    saving: 'Saving',
    saved: 'Saved',
    search: 'Search',
    yes: 'Yes',
    openExpandingInfo: 'Open the details'
  },
  datePicker: {
    placeholder: 'dd.mm.yyyy',
    description:
      'Type the date in dd.mm.yyyy format. You can get to month picker with the Down Arrow key.',
    validationErrors: {
      validDate: 'Valid date format is dd.mm.yyyy',
      dateTooEarly: 'Pick a later date',
      dateTooLate: 'Pick an earlier date'
    },
    open: 'Open date picker',
    close: 'Close date picker'
  },
  documentTemplates: {
    // only on employee frontend
    templateQuestions: {
      label: 'Label',
      options: 'Options',
      infoText: 'Info text',
      multiline: 'Allow multiline answer',
      withText: 'With extra info',
      text: 'Text content',
      textFields: 'Text fields',
      addTextField: 'Add a text field',
      allowMultipleRows: 'Allow multiple answers',
      addRow: 'Add new row'
    },
    noSelection: 'No selection',
    documentStates: {
      DRAFT: 'Draft',
      PREPARED: 'Ready',
      CITIZEN_DRAFT: 'Täytettävänä huoltajalla (en)',
      DECISION_PROPOSAL: 'Decision proposal',
      COMPLETED: 'Completed',
      ACCEPTED: 'Accepted',
      REJECTED: 'Rejected',
      ANNULLED: 'Annulled'
    }
  },
  fileUpload: {
    loading: 'Loading',
    loaded: 'Loaded',
    error: {
      EXTENSION_MISSING: 'Missing file extension',
      EXTENSION_INVALID: 'Invalid file extension',
      INVALID_CONTENT_TYPE: 'Invalid content type',
      FILE_TOO_LARGE: 'File is too big (max. 25 MB)',
      SERVER_ERROR: 'Upload failed'
    },
    input: {
      title: 'Add an attachment',
      text: (fileTypes) =>
        'Press here or drag and drop a new file. Max file size: 25 MB. Allowed formats: ' +
        fileTypes
          .map((type) => {
            switch (type) {
              case 'image':
                return 'images (JPEG, JPG, PNG)'
              case 'document':
                return 'documents (PDF, DOC, DOCX, ODT)'
              case 'audio':
                return 'audio'
              case 'video':
                return 'video'
              case 'csv':
                return 'CSV files'
            }
          })
          .join(', ')
    },
    uploadFile: 'Upload file',
    deleteFile: 'Delete file'
  },
  loginErrorModal: {
    header: 'Login failed',
    message:
      'The identification process failed or was stopped. To log in, go back and try again.',
    returnMessage: 'Go back'
  },
  messages: {
    staffAnnotation: 'Staff',
    recipients: 'Recipients',
    message: 'Message',
    send: 'Send',
    sending: 'Sending',
    types: {
      MESSAGE: 'Message',
      BULLETIN: 'Bulletin'
    },
    thread: {
      type: 'Type',
      urgent: 'Urgent'
    },
    outOfOffice: {
      singleRecipient: (name: string, period: FiniteDateRange) =>
        `${name} is out of office during ${period.format()}.`,
      multipleRecipientsHeader:
        'These recipients are out of office during the following periods.'
    }
  },
  messageReplyEditor: {
    // Currently only used for Finnish frontends
    discard: '',
    messagePlaceholder: undefined,
    messagePlaceholderSensitiveThread: undefined
  },
  sessionTimeout: {
    sessionExpiredTitle: 'Your session has timed out',
    sessionExpiredMessage: 'Please log in again.',
    goToLoginPage: 'Go to login page',
    cancel: 'Cancel'
  },
  notifications: {
    close: 'Close'
  },
  offlineNotification: 'No internet connection',
  reloadNotification: {
    title: 'New version of eVaka is available',
    buttonText: 'Reload page'
  },
  treeDropdown: {
    // Currently only used for Finnish frontends
    expand: () => '',
    collapse: () => '',
    expandDropdown: ''
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
    connectedPreferredStartDate:
      "You can apply for supplementary early childhood education from the start of your child's preschool education.",
    timeFormat: 'Check',
    timeRequired: 'Required',
    dateTooEarly: 'Pick a later date',
    dateTooLate: 'Pick an earlier date',
    unitNotSelected: 'Pick at least one choice',
    emailsDoNotMatch: 'The emails do not match',
    httpUrl: 'Valid url format is https://example.com',
    unselectableDate: 'Invalid date',
    openAttendance: 'Open attendance',
    generic: 'Check'
  }
}

export default components
