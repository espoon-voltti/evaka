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
    noResults: 'Ei hakutuloksia',
    open: 'Avaa',
    remove: 'Poista',
    saving: 'Tallennetaan',
    saved: 'Tallennettu',
    search: 'Hae',
    yes: 'Kyllä',
    openExpandingInfo: 'Avaa lisätietokenttä'
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
      withText: 'Pyydetään lisätietoja',
      text: 'Tekstisisältö',
      textFields: 'Tekstikentät',
      addTextField: 'Lisää tekstikenttä',
      allowMultipleRows: 'Salli useampi vastaus',
      addRow: 'Lisää rivi'
    },
    noSelection: 'Ei valintaa',
    documentStates: {
      DRAFT: 'Luonnos',
      PREPARED: 'Laadittu',
      COMPLETED: 'Valmis'
    }
  },
  fileUpload: {
    loading: 'Ladataan...',
    loaded: 'Ladattu',
    error: {
      EXTENSION_MISSING: 'Tiedostopääte puuttuu',
      EXTENSION_INVALID: 'Virheellinen tiedostopääte',
      INVALID_CONTENT_TYPE: 'Virheellinen tiedostomuoto',
      FILE_TOO_LARGE: 'Liian suuri tiedosto (max. 25 MB)',
      SERVER_ERROR: 'Lataus ei onnistunut'
    },
    input: {
      title: 'Lisää liite',
      text: (fileTypes) =>
        'Paina tästä tai raahaa liite laatikkoon yksi kerrallaan. Tiedoston maksimikoko: 25 MB. Sallitut tiedostomuodot: ' +
        fileTypes
          .map((type) => {
            switch (type) {
              case 'image':
                return 'kuvat (JPEG, JPG, PNG)'
              case 'document':
                return 'dokumentit (PDF, DOC, DOCX, ODT)'
              case 'audio':
                return 'äänitiedostot'
              case 'video':
                return 'videotiedostot'
              case 'csv':
                return 'CSV-tiedostot'
            }
          })
          .join(', ')
    },
    deleteFile: 'Poista tiedosto'
  },
  loginErrorModal: {
    header: 'Kirjautuminen epäonnistui',
    message:
      'Palveluun tunnistautuminen epäonnistui tai se keskeytettiin. Kirjautuaksesi sisään palaa takaisin ja yritä uudelleen.',
    returnMessage: 'Palaa takaisin'
  },
  messages: {
    staffAnnotation: 'Henkilökunta',
    message: 'Viesti',
    recipients: 'Vastaanottajat',
    send: 'Lähetä',
    sending: 'Lähetetään',
    types: {
      MESSAGE: 'Viesti',
      BULLETIN: 'Tiedote'
    },
    thread: {
      type: 'Tyyppi',
      urgent: 'Kiireellinen'
    }
  },
  messageEditor: {
    newMessage: 'Uusi viesti',
    to: {
      label: 'Vastaanottaja',
      placeholder: 'Valitse ryhmä',
      noOptions: 'Ei ryhmiä'
    },
    type: {
      label: 'Viestin tyyppi',
      message: 'Viesti',
      bulletin: 'Tiedote (ei voi vastata)'
    },
    flags: {
      heading: 'Viestin lisämerkinnät',
      urgent: {
        info: 'Lähetä viesti kiireellisenä vain, jos haluat että huoltaja lukee sen työpäivän aikana.',
        label: 'Kiireellinen'
      },
      sensitive: {
        info: 'Arkaluontoisen viestin avaaminen vaatii kuntalaiselta vahvan tunnistautumisen.',
        label: 'Arkaluontoinen',
        whyDisabled:
          'Arkaluontoisen viestin voi lähettää vain henkilökohtaisesta käyttäjätilistä yksittäisen lapsen huoltajille.'
      }
    },
    sender: 'Lähettäjä',
    recipientsPlaceholder: 'Valitse...',
    title: 'Otsikko',
    deleteDraft: 'Hylkää luonnos'
  },
  messageReplyEditor: {
    messagePlaceholder: undefined,
    discard: 'Hylkää',
    messagePlaceholderSensitiveThread: undefined
  },
  notifications: {
    close: 'Sulje'
  },
  offlineNotification: 'Ei verkkoyhteyttä',
  reloadNotification: {
    title: 'Uusi versio eVakasta saatavilla',
    buttonText: 'Lataa sivu uudelleen'
  },
  treeDropdown: {
    expand: (opt: string) => `Avaa vaihtoehdon ${opt} alaiset vaihtoehdot`,
    collapse: (opt: string) => `Sulje vaihtoehdon ${opt} alaiset vaihtoehdot`,
    expandDropdown: 'Avaa'
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
    dateTooEarly: 'Valitse myöhäisempi päivä',
    dateTooLate: 'Valitse aikaisempi päivä',
    unitNotSelected: 'Valitse vähintään yksi hakutoive',
    emailsDoNotMatch: 'Sähköpostiosoitteet eivät täsmää',
    httpUrl: 'Anna muodossa https://example.com',
    unselectableDate: 'Päivä ei ole sallittu',
    openAttendance: 'Avoin kirjaus',
    generic: 'Tarkista'
  }
}

export default components
