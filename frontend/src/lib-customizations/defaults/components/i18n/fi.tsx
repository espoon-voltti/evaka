// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const components = {
  asyncButton: {
    inProgress: 'Ladataan',
    failure: 'Lataus epäonnistui',
    success: 'Valmis'
  },
  datePicker: {
    placeholder: 'pp.kk.vvvv',
    description:
      'Kirjoita päivämäärä kenttään muodossa pp.kk.vvvv. Tab-näppäimellä pääset kuukausivalitsimeen.',
    validationErrors: {
      validDate: 'Anna muodossa pp.kk.vvvv',
      dateTooEarly: 'Valitse myöhäisempi päivä',
      dateTooLate: 'Valitse aikaisempi päivä'
    }
  },
  loginErrorModal: {
    header: 'Kirjautuminen epäonnistui',
    message:
      'Palveluun tunnistautuminen epäonnistui tai se keskeytettiin. Kirjautuaksesi sisään palaa takaisin ja yritä uudelleen.',
    returnMessage: 'Palaa takaisin'
  },
  notifications: {
    close: 'Sulje'
  },
  offlineNotification: 'Ei verkkoyhteyttä',
  reloadNotification: {
    title: 'Uusi versio eVakasta saatavilla',
    buttonText: 'Lataa sivu uudelleen'
  }
}

export default components
