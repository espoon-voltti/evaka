// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Translations } from 'lib-components/i18n'

const components: Translations = {
  asyncButton: {
    inProgress: 'Ladataan',
    failure: 'Lataus epäonnistui',
    success: 'Valmis'
  },
  common: {
    add: 'Lisää',
    cancel: 'Peruuta',
    close: 'Sulje',
    confirm: 'Vahvista',
    no: 'Ei',
    remove: 'Poista',
    yes: 'Kyllä'
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
  documentTemplates: {
    templateQuestions: {
      label: 'Otsikko',
      options: 'Vaihtoehdot',
      infoText: 'Ohjeteksti',
      multiline: 'Salli monirivinen vastaus',
      withText: 'Pyydetään lisätietoja'
    },
    noSelection: 'Ei valintaa',
    documentStates: {
      DRAFT: 'Luonnos',
      PREPARED: 'Laadittu',
      COMPLETED: 'Valmis'
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
  },
  validationErrors: {
    required: 'Pakollinen tieto',
    requiredSelection: 'Valinta puuttuu',
    format: 'Anna oikeassa muodossa',
    integerFormat: 'Anna kokonaisluku',
    ssn: 'Virheellinen henkilötunnus',
    phone: 'Virheellinen numero',
    email: 'Virheellinen sähköpostiosoite',
    preferredStartDate: 'Aloituspäivä ei ole sallittu',
    timeFormat: 'Tarkista',
    timeRequired: 'Pakollinen',
    unitNotSelected: 'Valitse vähintään yksi hakutoive',
    emailsDoNotMatch: 'Sähköpostiosoitteet eivät täsmää',
    httpUrl: 'Anna muodossa https://example.com',
    unselectableDate: 'Päivä ei ole sallittu',
    openAttendance: 'Avoin kirjaus'
  }
}

export default components
