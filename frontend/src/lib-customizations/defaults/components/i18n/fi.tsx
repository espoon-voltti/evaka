// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type FiniteDateRange from 'lib-common/finite-date-range'
import type { Translations } from 'lib-components/i18n'

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
    noFirstName: 'Etunimi puuttuu',
    noLastName: 'Sukunimi puuttuu',
    open: 'Avaa',
    remove: 'Poista',
    saving: 'Tallennetaan',
    saved: 'Tallennettu',
    search: 'Hae',
    yes: 'Kyllä',
    openExpandingInfo: 'Avaa lisätietokenttä',
    showPassword: 'Näytä salasana',
    hidePassword: 'Piilota salasana',
    userTypes: {
      SYSTEM: 'järjestelmä',
      CITIZEN: 'kuntalainen',
      EMPLOYEE: 'työntekijä',
      MOBILE_DEVICE: 'mobiililaite',
      UNKNOWN: 'tuntematon'
    }
  },
  datePicker: {
    placeholder: 'pp.kk.vvvv',
    description:
      'Kirjoita päivämäärä kenttään muodossa pp.kk.vvvv. Nuoli alas -näppäimellä pääset kuukausivalitsimeen.',
    validationErrors: {
      validDate: 'Anna muodossa pp.kk.vvvv',
      dateTooEarly: 'Valitse myöhäisempi päivä',
      dateTooLate: 'Valitse aikaisempi päivä'
    },
    open: 'Avaa päivämäärävalitsin',
    close: 'Sulje päivämäärävalitsin'
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
      CITIZEN_DRAFT: 'Täytettävänä huoltajalla',
      DECISION_PROPOSAL: 'Päätösesitys',
      COMPLETED: 'Valmis',
      ACCEPTED: 'Hyväksytty',
      REJECTED: 'Hylätty',
      ANNULLED: 'Mitätöity'
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
    uploadFile: 'Lähetä tiedosto',
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
    },
    outOfOffice: {
      singleRecipient: (name: string, period: FiniteDateRange) =>
        `${name} ei ole tavoitettavissa aikavälillä ${period.format()}.`,
      multipleRecipientsHeader:
        'Nämä vastaanottajat eivät ole tavoitettavissa seuraavina ajankohtina.'
    }
  },
  messageReplyEditor: {
    messagePlaceholder: undefined,
    discard: 'Hylkää',
    messagePlaceholderSensitiveThread: undefined
  },
  sessionTimeout: {
    sessionExpiredTitle: 'Istuntosi on katkaistu',
    sessionExpiredMessage: 'Ole hyvä ja kirjaudu sisään uudelleen.',
    goToLoginPage: 'Siirry kirjautumissivulle',
    cancel: 'Peruuta'
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
    connectedPreferredStartDate:
      'Voit hakea täydentävää varhaiskasvatusta lapsen esiopetuksen alkamisesta alkaen.',
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
  },
  metadata: {
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
