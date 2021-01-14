// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Translations } from '.'

const sv: Translations = {
  common: {
    return: 'Tillbaka'
  },
  header: {
    nav: {
      map: 'Karta',
      applications: 'Ansökningar',
      decisions: 'Beslut',
      newDecisions: 'Ny Beslut',
      newApplications: 'Ny Ansökningar'
    },
    lang: {
      fi: 'Suomeksi',
      sv: 'På svenska',
      en: 'In English'
    },
    logout: 'Logga ut'
  },
  footer: {
    espooLabel: '© Esbo stad',
    privacyPolicy: 'Dataskyddsbeskrivningar',
    privacyPolicyLink:
      'https://www.esbo.fi/sv-FI/Etjanster/Dataskydd/Dataskyddsbeskrivningar',
    sendFeedback: 'Ge feedback',
    sendFeedbackLink:
      'https://easiointi.espoo.fi/eFeedback/sv/Feedback/20-S%C3%A4hk%C3%B6iset%20asiointipalvelut'
  },
  applications: {
    title: 'Hakemukset',
    editor: {
      heading: {
        title: {
          daycare: 'Ansökan till småbarnspedagogik'
        },
        info: {
          daycare: [
            'Du kan ansöka om plats i småbarnspedagogisk verksamhet året om. Ansökningen bör lämnas in senast fyra månader före behovet av verksamheten börjar. Om behovet börjar med kortare varsel bör du ansöka om plats senast två veckor före.',
            'Du får ett skriftligt beslut om platsen. Beslutet delges i tjänsten <a href="https://www.suomi.fi/meddelanden" target="_blank" rel="noreferrer">Suomi.fi</a>-meddelanden, eller per post om du inte tagit i bruk meddelandetjänsten i Suomi.fi.',
            '* Informationen markerad med en stjärna krävs'
          ]
        }
      }
    }
  },
  decisions: {
    title: 'Beslut',
    summary:
      'Denna sida visar de beslutar om barns ansökan till småbarnspedagogik, förskola och klubbverksamhet. Du ska omedelbart eller senast två veckor från mottagandet av ett beslut ta emot eller annullera platsen / platserna.',
    unconfirmedDecisions: (n: number) => `${n} beslut inväntar bekräftelse`,
    pageLoadError: 'Hämtar information misslyckades',
    applicationDecisions: {
      decision: 'Beslut om',
      type: {
        CLUB: 'klubbverksamhet',
        DAYCARE: 'småbarnspedagogik',
        DAYCARE_PART_TIME: 'deldag småbarnspedagogik',
        PRESCHOOL: 'förskola',
        PRESCHOOL_DAYCARE:
          'småbarnspedagogik i samband med förskoleundervisningen',
        PREPARATORY_EDUCATION: 'förberedande undervisning'
      },
      childName: 'Barnets namn',
      unit: 'Enhet',
      period: 'Period',
      sentDate: 'Beslutsdatum',
      resolved: 'Bekräftat',
      statusLabel: 'Status',
      summary:
        'Du ska omedelbart eller senast två veckor från mottagandet av ett beslut ta emot eller annullera platsen / platserna.',
      status: {
        PENDING: 'Bekräftas av vårdnadshavaren',
        ACCEPTED: 'Bekräftad',
        REJECTED: 'Avvisade'
      },
      openPdf: 'Visa beslut',
      confirmationInfo: {
        preschool:
          'Du ska omedelbart eller senast två veckor från mottagandet av detta beslut, ta emot eller annullera platsen. Du kan ta emot eller annullera platsen elektroniskt på adressen espoonvarhaiskasvatus.fi (kräver identifiering) eller per post.',
        default:
          'Du ska omedelbart eller senast två veckor från mottagandet av ett beslut ta emot eller annullera platsen.'
      },
      goToConfirmation:
        'Gå till beslutet för att läsa det och svara om du tar emot eller annullerar platsen.',
      confirmationLink: 'Granska och bekräfta beslutet',
      response: {
        title: 'Bekräftelse',
        accept1: 'Vi tar emot platsen från',
        accept2: '',
        reject: 'Vi tar inte emot platsen',
        cancel: 'Gå tillbacka utan att besluta',
        submit: 'Skicka svar på beslutet',
        disabledInfo:
          'OBS! Du kommer att kunna svara på den relaterade beslutet, om du först accepterar den beslutet om förskola / förberedande undervisning.'
      },
      warnings: {
        decisionWithNoResponseWarning: {
          title: 'Ett annat beslut väntar på ditt godkännande',
          text:
            'Ett annat beslut väntar på ditt godkännande. Vill du gå tillbaka till listan utan att svara?',
          resolveLabel: 'Gå tillbaka utan att svara',
          rejectLabel: 'Förtsätt att svara'
        },
        doubleRejectWarning: {
          title: 'Vill du annulera platsen?',
          text:
            'Du ska annullera platsen. Den relaterade småbarnspedagogik plats ska också markeras annulerat.',
          resolveLabel: 'Annulera båda',
          rejectLabel: 'Gå tillbaka'
        }
      },
      errors: {
        pageLoadError: 'Misslyckades att hämta information',
        submitFailure: 'Misslyckades att skicka svar'
      },
      returnToPreviousPage: 'Tillbacka'
    }
  },
  applicationsList: {
    title:
      'Anmälan till förskolan eller ansökan till småbarnspedagogisk verksamhet',
    summary:
      'Barnets vårdnadshavare kan anmäla barnet till förskolan eller ansöka om plats i småbarnspedagogisk verksamhet. Uppgifter om vårdnadshavarens barn kommer automatiskt från befolkningsdatabasen till denna sida.',
    pageLoadError: 'Tietojen hakeminen ei onnistunut',
    type: {
      daycare: 'Varhaiskasvatushakemus',
      preschool: 'Esiopetushakemus',
      club: 'Kerhohakemus'
    },
    unit: 'Yksikkö',
    period: 'Ajalle',
    created: 'Luotu',
    modified: 'Muokattu',
    status: {
      title: 'Tila',
      CREATED: 'Luonnos',
      SENT: 'Lähetetty',
      WAITING_PLACEMENT: 'Käsiteltävänä',
      WAITING_DECISION: 'Käsiteltävänä',
      WAITING_UNIT_CONFIRMATION: 'Käsiteltävänä',
      WAITING_MAILING: 'Käsiteltävänä',
      WAITING_CONFIRMATION: 'Vahvistettavana huoltajalla',
      REJECTED: 'Paikka hylätty',
      ACTIVE: 'Paikka vastaanotettu',
      CANCELLED: 'Poistettu käsittelystä'
    },
    openApplicationLink: 'Näytä hakemus'
  }
}

export default sv
