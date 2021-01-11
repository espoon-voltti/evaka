// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Translations } from '.'

const en: Translations = {
  header: {
    nav: {
      map: 'Map',
      applications: 'Applications',
      decisions: 'Decisions',
      newDecisions: 'New Decisions'
    },
    lang: {
      fi: 'Suomeksi',
      sv: 'På svenska',
      en: 'In English'
    },
    logout: 'Log out'
  },
  footer: {
    espooLabel: '© City of Espoo',
    privacyPolicy: 'Privacy Notices',
    privacyPolicyLink:
      'https://www.espoo.fi/en-US/Eservices/Data_protection/Privacy_Notices',
    sendFeedback: 'Give feedback',
    sendFeedbackLink:
      'https://easiointi.espoo.fi/eFeedback/en/Feedback/20-S%C3%A4hk%C3%B6iset%20asiointipalvelut'
  },
  decisions: {
    title: 'Decisions',
    summary:
      'Tälle sivulle saapuvat lapsen varhaiskasvatus-, esiopetus- ja kerhohakemuksiin liittyvät päätökset. Uuden päätöksen saapuessa sinun tulee kahden viikon sisällä vastata, hyväksytkö vai hylkäätkö lapselle tarjotun paikan.',
    unconfimedDecisions: (n: number) =>
      `${n} ${
        n === 1 ? 'decision is' : 'decisions are'
      } waiting for confirmation`,
    applicationDecisions: {
      decision: 'Decision of',
      type: {
        CLUB: 'club',
        DAYCARE: 'early childhood education',
        DAYCARE_PART_TIME: 'part-day early childhood education',
        PRESCHOOL: 'pre-primary education',
        PRESCHOOL_DAYCARE:
          'early childhood education related to pre-primary education',
        PREPARATORY_EDUCATION: 'preparatory education'
      },
      childName: "Child's name",
      unit: 'Unit',
      period: 'Ajalle',
      sentDate: 'Decision sent',
      resolved: 'Decision confirmed',
      statusLabel: 'Status',
      summary:
        'Päätöksessä ilmoitettu paikka / ilmoitetut paikat tulee joko hyväksyä tai hylätä välittömästi, viimeistään kahden viikon kuluessa päätöksen saapumisesta.',
      status: {
        PENDING: 'Waiting for confirmation from the guardian',
        ACCEPTED: 'Confirmed',
        REJECTED: 'Rejected'
      },
      openPdf: 'Show the decision',
      confirmationInfo: {
        preschool:
          'You must either accept or reject the place proposed in the decision of pre-primary education, preparatory education and/or early childhood education related to pre-primary education within two weeks of receiving this notification. If you have applied for several services, you will receive separate decisions for each of them that require your action.',
        default:
          'You must either accept or reject the place proposed in the decision within two weeks of receiving this notification.'
      },
      goToConfirmation:
        'Please open the decision and respond whether you will accept or reject the place.',
      confirmationLink: 'Review and confirm the decision',
      response: {
        title: 'Paikan hyväksyminen tai hylkääminen',
        accept1: 'Otamme paikan vastaan',
        accept2: 'alkaen',
        reject: 'Emme ota paikkaa vastaan',
        cancel: 'Palaa takaisin vastaamatta',
        submit: 'Lähetä vastaus päätökseen',
        disabledInfo:
          'HUOM! Pääset hyväksymään/hylkäämään liittyvää varhaiskasvatusta koskevan päätöksen mikäli hyväksyt ensin esiopetusta / valmistavaa opetusta koskevan päätöksen.'
      },
      warnings: {
        decisionWithNoResponseWarning: {
          title: 'Toinen päätös odottaa vastaustasi',
          text:
            'Toinen päätös odottaa edelleen vastaustasi. Haluatko  palata listalle vastaamatta?',
          resolveLabel: 'Palaa vastaamatta',
          rejectLabel: 'Jatka vastaamista'
        },
        doubleRejectWarning: {
          title: 'Haluatko hylätä paikan?',
          text:
            'Olet hylkäämässä tarjotun esiopetus / valmistavan paikan. Liittyvän varhaiskasvatuksen paikka merkitään samalla hylätyksi.',
          resolveLabel: 'Hylkää molemmat',
          rejectLabel: 'Palaa takaisin'
        }
      },
      errors: {
        pageLoadError: 'Tietojen hakeminen ei onnistunut',
        submitFailure: 'Päätökseen vastaaminen ei onnistunut'
      },
      returnToPreviousPage: 'Palaa'
    }
  }
}

export default en
