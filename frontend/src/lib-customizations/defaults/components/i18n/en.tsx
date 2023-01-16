// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const components = {
  asyncButton: {
    inProgress: 'Loading',
    failure: 'Failed to load',
    success: 'Success'
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
  }
}

export default components
