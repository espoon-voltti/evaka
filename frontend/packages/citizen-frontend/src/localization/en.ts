// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Translations } from '.'

const en: Translations = {
  common: {
    return: 'Return'
  },
  header: {
    nav: {
      map: 'Map',
      applications: 'Applications',
      decisions: 'Decisions',
      newDecisions: 'New Decisions',
      newApplications: 'New Applications'
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
  applications: {
    title: 'Applications',
    editor: {
      heading: {
        title: {
          daycare: 'Varhaiskasvatushakemus'
        },
        info: {
          daycare: [
            'You can apply for early childhood education all year round. The application must be submitted at the latest four months before the need for early childhood education begins. If you urgently need early childhood education, you must apply for a place at the latest two weeks before the need begins.',
            'The applicant will receive a written decision on the early childhood education place via the <a href="https://www.suomi.fi/messages" target="_blank" rel="noreferrer">Messages service of Suomi.fi</a> or by post if the applicant has not taken in use the Messages service of Suomi.fi.',
            '* Information marked with a star is required'
          ]
        }
      }
    }
  },
  decisions: {
    title: 'Decisions',
    summary:
      "This page displays the received decisions regarding child's early childhood education, preschool and club applications. Upon receiving a new decision, you are required to respond in two weeks, whether you accept or reject it.",
    unconfirmedDecisions: (n: number) =>
      `${n} ${
        n === 1 ? 'decision is' : 'decisions are'
      } waiting for confirmation`,
    pageLoadError: 'Error in fetching the requested information',
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
      period: 'Time period',
      sentDate: 'Decision sent',
      resolved: 'Decision confirmed',
      statusLabel: 'Status',
      summary:
        'The placement / placements indicated in the decision should be either accepted or rejected immediately and no later than two weeks after receiving the decision.',
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
        title: 'Accepting or rejecting the placement',
        accept1: 'We accept the placement',
        accept2: 'from',
        reject: 'We reject the placement',
        cancel: 'Cancel',
        submit: 'Submit response to the decision',
        disabledInfo:
          'NOTE! You are able to accept/reject the related early childhood education decision if you accept the preschool / preparatory education decision first.'
      },
      warnings: {
        decisionWithNoResponseWarning: {
          title: 'There is a decision without a response',
          text:
            'There is a decision without a response. Are you sure you want to return to the list without responding?',
          resolveLabel: 'Return without responding',
          rejectLabel: 'Continue responding'
        },
        doubleRejectWarning: {
          title: 'Are you sure you want to reject the placement?',
          text:
            'You are rejecting an offer on preschool / preparatory education placement. The related early childhood education placement will also be rejected.',
          resolveLabel: 'Reject both',
          rejectLabel: 'Cancel'
        }
      },
      errors: {
        pageLoadError: 'Error in fetching the requested information',
        submitFailure: 'Error in submitting the response'
      },
      returnToPreviousPage: 'Return'
    }
  },
  applicationsList: {
    title:
      'Applying for early childhood education or a club and enrolling for pre-primary education',
    summary:
      'A child’s guardian can submit an application for early childhood education or a club and enrol the child to pre-primary education. Information on the guardian’s children is automatically retrieved from the Digital and Population Data Services Agency and displayed in this view.',
    pageLoadError: 'Failed to load guardian applications',
    type: {
      daycare: 'Daycare application',
      preschool: 'Early education application',
      club: 'Club application'
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
    openApplicationLink: 'Näytä hakemus',
    confirmationLinkInstructions:
      'Päätökset-välilehdellä voit lukea päätöksen ja hyväksyä/hylätä tarjotun paikan',
    confirmationLink: 'Siirry vahvistamaan',
    newApplicationLink: 'Uusi hakemus'
  }
}

export default en
