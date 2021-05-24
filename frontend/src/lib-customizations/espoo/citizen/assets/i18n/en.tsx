// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { P } from 'lib-components/typography'
import React from 'react'
import { Translations } from 'lib-customizations/citizen'
import ExternalLink from 'lib-components/atoms/ExternalLink'

const en: Translations = {
  common: {
    title: 'Early childhood education',
    cancel: 'Cancel',
    return: 'Return',
    ok: 'Ok',
    unit: {
      providerTypes: {
        MUNICIPAL: 'Municipal',
        PURCHASED: 'Purchased service',
        PRIVATE: 'Private service',
        MUNICIPAL_SCHOOL: 'Municipal',
        PRIVATE_SERVICE_VOUCHER: 'Private service (service voucher)'
      },
      careTypes: {
        CLUB: 'Club',
        FAMILY: 'Family daycare',
        CENTRE: 'Daycare',
        GROUP_FAMILY: 'Group family daycare',
        PRESCHOOL: 'Pre-primary education',
        PREPARATORY_EDUCATION: 'Preparatory education'
      },
      languages: {
        fi: 'finnish',
        sv: 'swedish'
      },
      languagesShort: {
        fi: 'finnish',
        sv: 'swedish'
      }
    },
    openExpandingInfo: 'Open the details',
    errors: {
      genericGetError: 'Error in fetching the requested information'
    }
  },
  header: {
    nav: {
      map: 'Map',
      applications: 'Applications',
      decisions: 'Decisions',
      messages: 'Messages'
    },
    lang: {
      fi: 'Suomeksi',
      sv: 'På svenska',
      en: 'In English'
    },
    login: 'Log in',
    logout: 'Log out'
  },
  footer: {
    cityLabel: '© City of Espoo',
    privacyPolicy: 'Privacy Notices',
    privacyPolicyLink:
      'https://www.espoo.fi/en-US/Eservices/Data_protection/Privacy_Notices',
    sendFeedback: 'Give feedback',
    sendFeedbackLink:
      'https://easiointi.espoo.fi/eFeedback/en/Feedback/20-S%C3%A4hk%C3%B6iset%20asiointipalvelut'
  },
  map: {
    title: 'Units on the map',
    mainInfo:
      "In this view, you can search the map for Espoo's early childhood education, pre-primary education and club places.",
    privateUnitInfo: function PrivateUnitInfo() {
      return (
        <span>
          For information on private units,{' '}
          <ExternalLink
            text="click here."
            href="https://www.espoo.fi/en-US/Childcare_and_education/Early_childhood_education/Private_early_childhood_education"
            newTab
          />
        </span>
      )
    },
    searchLabel: 'Search by address or by a unit name',
    searchPlaceholder: 'For example Purolan päiväkoti',
    address: 'Address',
    noResults: 'No results',
    distanceWalking: 'Walking distance from selected address',
    careType: 'Service need',
    careTypePlural: 'Service needs',
    careTypes: {
      CLUB: 'Club',
      DAYCARE: 'Early childhood education',
      PRESCHOOL: 'Pre-primary education'
    },
    language: 'Unit language',
    providerType: 'Service provider',
    providerTypes: {
      MUNICIPAL: 'municipal',
      PURCHASED: 'purchased service',
      PRIVATE: 'private service',
      PRIVATE_SERVICE_VOUCHER: 'private service (service voucher)'
    },
    homepage: 'Homepage',
    unitHomepage: "Unit's homepage",
    route: 'See route to unit',
    routePlanner: 'Journey Planner',
    newTab: '(Opens in a new tab)',
    shiftCareTitle: 'Shift care (evenings and weekends)',
    shiftCareLabel: 'Only show Shift care (evenings and weekends) units',
    shiftCareYes: 'Unit provides evening or round-the-clock care',
    shiftCareNo: 'Unit does not provide evening or round-the-clock care',
    showMoreFilters: 'Show more filters',
    showLessFilters: 'Show less filters',
    nearestUnits: 'Nearest units',
    moreUnits: 'More units',
    showMore: 'Show more results',
    mobileTabs: {
      map: 'Map',
      list: 'List of units'
    },
    serviceVoucherLink:
      'https://www.espoo.fi/en-US/Childcare_and_education/Early_childhood_education/Applying_for_early_childhood_education/Service_voucher/Information_for_families',
    noApplying: 'No applying via eVaka, contact the unit',
    backToSearch: 'Back to search'
  },
  messages: {
    inboxTitle: 'Inbox',
    noMessages: 'No messages',
    types: {
      MESSAGE: 'Message',
      BULLETIN: 'Bulletin'
    }
  },
  applications: {
    title: 'Applications',
    deleteDraftTitle: 'Do you want to remove the application?',
    deleteDraftText: 'All the information of the application will be lost.',
    deleteDraftOk: 'Delete application',
    deleteDraftCancel: 'Go back',
    deleteSentTitle: 'Do you want to cancel the application?',
    deleteSentText:
      'All the information of the application will be lost. Also, as the application has already been sent, it will be cancelled.',
    deleteSentOk: 'Cancel application',
    deleteSentCancel: 'Go back',
    deleteUnprocessedApplicationError: 'Failed to remove the application',
    creation: {
      title: 'Selection of application type',
      daycareLabel: 'Early childhood education and service voucher application',
      daycareInfo:
        'The application for early childhood education is used to apply for a municipal early childhood education place at a day-care centre, family day-care provider and group family day-care provider. You can also use the same application to apply for a service voucher for early childhood education. Under Preferred units, select the service voucher unit to which you want to apply.',
      preschoolLabel:
        'Enrolment to pre-primary education and/or preparatory education',
      preschoolInfo:
        'Free-of-charge pre-primary education is available for four hours per day. In addition, you can apply for connected early childhood education (subject to a fee) for your child. You can also apply for a service voucher for connected early childhood education. Under Preferred units, select the service voucher unit to which you want to apply. It is available in pre-primary education units in the morning before pre-primary education and in the afternoon after pre-primary education. You can submit an application for connected early childhood education at the same time as you enrol your child in pre-primary education or through a separate application after pre-primary education has started. You can also use the same application to apply for free-of-charge preparatory education and early childhood education connected to preparatory education.',
      preschoolDaycareInfo:
        'Application for connected early childhood education for a child who will be / has been enrolled in pre-primary education or preparatory education',
      clubLabel: 'Club application',
      clubInfo:
        'The club application is used for applying for municipal clubs.',
      duplicateWarning:
        'The child already has a similar unfinished application. Please return to the Applications view and complete the existing application or contact the Early Childhood Education Service Guidance.',
      transferApplicationInfo: {
        DAYCARE:
          'Your child already has a place in the City of Espoo’s early childhood education. Use this application to apply for a transfer to another early childhood education unit.',
        PRESCHOOL:
          'Your child already has a place in pre-primary education. Use this application to apply for early childhood education <strong>connected to pre-primary education</strong> or a transfer to another pre-primary education unit.'
      },
      create: 'Apply',
      daycare4monthWarning: 'The application processing time is 4 months.',
      applicationInfo: function ApplicationInfoText() {
        return (
          <P>
            You can make changes to your application until its processing
            starts. After this, you can make changes to your application by
            contacting early childhood education service counselling (tel. 09
            816 31000). If you wish to cancel an application you have submitted,
            please send an email to early childhood education service
            counselling (
            <a href="mailto:varhaiskasvatuksen.palveluohjaus@espoo.fi">
              varhaiskasvatuksen.palveluohjaus@espoo.fi
            </a>
            ).
          </P>
        )
      }
    },
    editor: {
      heading: {
        title: {
          DAYCARE:
            'Application for early childhood education and service voucher',
          PRESCHOOL: 'Registration for pre-school education',
          CLUB: 'Application for club'
        },
        info: {
          DAYCARE: function EditorHeadingInfoDaycareText() {
            return (
              <>
                <P>
                  You can apply for early childhood education all year round.
                  The application must be submitted at the latest four months
                  before the need for early childhood education begins. If you
                  urgently need early childhood education, you must apply for a
                  place at the latest two weeks before the need begins.
                </P>
                <P>
                  The applicant will receive a written decision on the early
                  childhood education place via the{' '}
                  <a
                    href="https://www.suomi.fi/messages"
                    target="_blank"
                    rel="noreferrer"
                  >
                    Messages service of Suomi.fi
                  </a>{' '}
                  or by post if the applicant has not taken in use the Messages
                  service of Suomi.fi.
                </P>
                <P fitted={true}>
                  * Information marked with a star is required
                </P>
              </>
            )
          },
          PRESCHOOL: function EditorHeadingInfoPreschoolText() {
            return (
              <>
                <P>
                  Pre-primary education is attended one year before the start of
                  compulsory education. Pre-primary education is free of charge.
                  Enrolment for pre-primary education in the 2021–2022 school
                  year takes place on 8–20 January 2021. Finnish and swedish
                  pre-primary education begins on{' '}
                  <strong>11 August 2021</strong>. The decisions will be sent to
                  the{' '}
                  <a
                    href="https://www.suomi.fi/messages"
                    target="_blank"
                    rel="noreferrer"
                  >
                    Suomi.fi Messages
                  </a>{' '}
                  service or by post, if the applicant does not use the{' '}
                  <a
                    href="https://www.suomi.fi/messages"
                    target="_blank"
                    rel="noreferrer"
                  >
                    Suomi.fi Messages
                  </a>{' '}
                  service.
                </P>
                <P>
                  The decisions will be sent to the{' '}
                  <a
                    href="https://www.suomi.fi/messages"
                    target="_blank"
                    rel="noreferrer"
                  >
                    Suomi.fi Messages
                  </a>{' '}
                  service or by post, if the applicant does not use the{' '}
                  <a
                    href="https://www.suomi.fi/messages"
                    target="_blank"
                    rel="noreferrer"
                  >
                    Suomi.fi Messages
                  </a>{' '}
                  service.
                </P>
                <P fitted={true}>
                  * Information marked with a star is required
                </P>
              </>
            )
          },
          CLUB: function EditorHeadingInfoClubText() {
            return (
              <>
                <P>
                  The application period for clubs that start in the autumn is
                  in March. If your child is given a place in a club, you will
                  receive a decision in April or May. The decision will cover
                  one operating period (from August until the end of May). The
                  decision will be sent to the Suomi.fi service or by post if
                  you have not activated the service.
                </P>
                <P>
                  You can also submit a club applicationoutside of the
                  application period and after the clubs’ operating period has
                  started. We will, however, first process all applications that
                  are submitted during the application period. The applications
                  that are submitted outside of the application period will be
                  processed in the order in which they are received. The club
                  application is for one operating period. At the end of the
                  period, the application will be removed from the system.
                </P>
                <P>
                  Club activities are free of charge and participation in club
                  activities will not affect the child home care allowance paid
                  by Kela. However, if the child has been granted a place in
                  early childhood education or your family has been granted
                  private day care allowance, a place in a club cannot be
                  granted to the child
                </P>
                <P fitted={true}>
                  * Information marked with an asterisk is required
                </P>
              </>
            )
          }
        },
        errors: (count: number) =>
          count === 1 ? '1 error' : `${count} errors`,
        hasErrors: 'Please correct the following information:'
      },
      actions: {
        hasVerified:
          'I have checked that the information in the application is correct',
        verify: 'Verify application',
        returnToEdit: 'Return to editing',
        returnToEditBtn: 'Return to editing',
        cancel: 'Go back',
        send: 'Send application',
        update: 'Save changes',
        sendError: 'Failed to send the application',
        saveDraft: 'Save as draft',
        updateError: 'Saving the changes failed'
      },
      verification: {
        title: {
          DAYCARE: 'Verifying the application',
          PRESCHOOL: 'Verifying the application',
          CLUB: 'Verifying the application'
        },
        notYetSent: function NotYetSentText() {
          return (
            <P>
              <strong>The application has not yet been sent.</strong> Please
              check the information you provided and send the application by
              clicking {'"'}Send the application{'"'} button in the end of the
              form.
            </P>
          )
        },
        notYetSaved: function NotYetSavedText() {
          return (
            <P>
              <strong>The changes have not been saved yet.</strong> Please check
              the information you provided and send the application by clicking
              {'"'}Save changes{'"'} button at the end of the form.
            </P>
          )
        },
        no: 'No',
        basics: {
          created: 'Created',
          modified: 'Last modified'
        },
        attachmentBox: {
          nb: 'Note!',
          headline:
            'If you add attachments to the following sections online, your application will be processed more quickly as the processing period will begin once we have received the required attachments.',
          urgency: 'Application is urgent',
          shiftCare: 'Evening and round-the-clock care',
          goBackLinkText: 'Return back to application',
          goBackRestText: 'to add the attachments.'
        },
        serviceNeed: {
          title: 'Service need',
          wasOnDaycare: 'Was on early childhood education before club',
          wasOnDaycareYes:
            'Child is in early childhood education before the desired start date of the club.',
          wasOnClubCare: 'In a club in the previous club term',
          wasOnClubCareYes:
            'Child has been in a club during the previous club term',
          connectedDaycare: {
            title: 'Apply for early childhood education',
            label: 'Early childhood education',
            withConnectedDaycare:
              'I also apply for early childhood education related to pre-primary education.',
            withoutConnectedDaycare: 'No'
          },
          attachments: {
            label: 'Required attachments',
            withoutAttachments: 'Not attached – will be sent by post'
          },
          startDate: {
            title: {
              DAYCARE: 'Start of early childhood education',
              PRESCHOOL: 'Start of early childhood education',
              CLUB: 'Club start date'
            },
            preferredStartDate: 'Desired start date',
            urgency: 'Application is urgent',
            withUrgency: 'Yes',
            withoutUrgency: 'No'
          },
          dailyTime: {
            title: 'Daily early childhood education time',
            partTime: 'Part- or full-day',
            withPartTime: 'Part-day',
            withoutPartTime: 'Full-day',
            dailyTime: 'Start and end time of daily early childhood education',
            shiftCare: 'Evening and round-the-clock care',
            withShiftCare: 'Need for evening or round-the-clock care',
            withoutShiftCare: 'No need for evening or round-the-clock care'
          },
          assistanceNeed: {
            title: 'Support need',
            assistanceNeed: 'Support need',
            withAssistanceNeed: 'Child needs special support',
            withoutAssistanceNeed: 'No need for special support',
            description: 'Description'
          },
          preparatoryEducation: {
            label: 'Pre-primary education and preparatory education',
            withPreparatory:
              'The child needs support with learning Finnish. I am also applying for preparatory education.',
            withoutPreparatory: 'No'
          }
        },
        unitPreference: {
          title: 'Preferred units',
          siblingBasis: {
            title: 'Application on a sibling basis',
            siblingBasisLabel: 'Sibling basis',
            siblingBasisYes:
              'I primarily apply for a place at the same early childhood education location attended by the child’s sibling.',
            name: 'First and last name of the sibling',
            ssn: 'Personal identity code of the sibling'
          },
          units: {
            title: 'Preferred units',
            label: 'Selected units'
          }
        },
        contactInfo: {
          title: 'Personal information',
          child: {
            title: "Child's information",
            name: "Child's name",
            ssn: "Child's personal identity code",
            streetAddress: 'Home address',
            isAddressChanging: 'Address is about to change',
            hasFutureAddress:
              'The address in the database of the Digital and Population Data Services Agency has changed/is about to change',
            addressChangesAt: 'Moving date',
            newAddress: 'New address'
          },
          guardian: {
            title: 'Guardian information',
            name: 'Guardian name',
            ssn: 'Guardian personal identity code',
            streetAddress: 'Street address',
            tel: 'Phone number',
            email: 'Email',
            isAddressChanging: 'Address is about to change',
            hasFutureAddress:
              'The address in the database of the Digital and Population Data Services Agency has changed/is about to change',
            addressChangesAt: 'Moving date',
            newAddress: 'New address'
          },
          secondGuardian: {
            title: 'Second guardian information',
            email: 'Email',
            tel: 'Phone number',
            info:
              'Second guardian information is automatically fetched from VTJ',
            agreed:
              'We have agreed with the other guardian about this application.',
            notAgreed:
              'We have not agreed with the other guardian about this application.',
            rightToGetNotified:
              'The other guardian has only the right to get notified about the placement.',
            noAgreementStatus: 'Unknown'
          },
          fridgePartner: {
            title:
              'Spouse or cohabiting partner (not a guardian) living in the same household',
            fridgePartner:
              'Spouse or cohabiting partner (not a guardian) living in the same household',
            name: 'Name',
            ssn: 'Person identification code'
          },
          fridgeChildren: {
            title:
              'Other children under 18 years of age living in the same household.',
            name: "Child's name",
            ssn: 'Person identification code',
            noOtherChildren: 'No other children'
          }
        },
        additionalDetails: {
          title: 'Other additional information',
          otherInfoLabel: 'Additional information',
          dietLabel: 'Special diet',
          allergiesLabel: 'Allergies'
        }
      },
      serviceNeed: {
        title: 'Service need',
        startDate: {
          header: {
            DAYCARE: 'Start date',
            PRESCHOOL: 'Start date',
            CLUB: 'Start date'
          },
          info: {
            DAYCARE: [],
            PRESCHOOL: [
              'Finnish and Swedish pre-primary education begins on 11 August 2021. If you need early childhood education starting from 1 August 2021 before the start of pre-primary education, you can apply for it with this application by selecting “I also apply for early childhood education related to pre-primary education”.'
            ],
            CLUB: ['Clubs follow preschool work and vacation times.']
          },
          clubTerm: 'Club term',
          clubTerms: 'Club terms',
          label: {
            DAYCARE: 'Desired start date',
            PRESCHOOL: 'Start date in August',
            CLUB: 'Desired start date'
          },
          noteOnDelay: 'The application processing time is 4 months.',
          instructions: function ServiceNeedInstructionsText() {
            return (
              <>
                You can postpone your desired start date until the processing of
                your application starts. After this, you can make changes to
                your desired start date by contacting early childhood education
                service counselling (tel. 09 816 31000).
              </>
            )
          },
          placeholder: 'Select the start date',
          validationText: 'Desired start date: '
        },
        clubDetails: {
          wasOnDaycare:
            'Child has an early childhood education placement, which he or she will give up when he or she gets a club placement',
          wasOnDaycareInfo:
            'If a child has been in early childhood education (daycare, family daycare or group family daycare) and gives up his or her place at the start of the club, he or she has a better chance of getting a club placement.',
          wasOnClubCare: 'Child has been in a club during last club term',
          wasOnClubCareInfo:
            'If the child has already been to the club during the previous club term, he or she has a better chance of getting a placement in the club.'
        },
        urgent: {
          label: 'Application is urgent',
          attachmentsMessage: {
            text: function UrgentApplicationAttachmentMessageText() {
              return (
                <P fitted={true}>
                  If the need for a place in early childhood education arises
                  from sudden employment or a sudden start of studies, you will
                  need to apply for a place no later than{' '}
                  <strong>two weeks before</strong> your child needs it. In
                  addition to the application, you will need to{' '}
                  <strong>
                    provide documents as proof of the employment or student
                    status
                  </strong>{' '}
                  of both guardians living in the same household. If you are
                  unable to add attachments to your online application, please
                  send the documents by post to Early childhood education
                  service counselling, P.O. Box 3125, 02070 City of Espoo. The
                  processing period of two weeks will not begin until we have
                  received the application and the required attachments.
                </P>
              )
            },
            subtitle:
              'Add here documents as proof of the employment or student status of both guardians.'
          }
        },
        partTime: {
          true: 'Part-day (max 5h/day, 25h/week)',
          false: 'Full-day'
        },
        dailyTime: {
          label: {
            DAYCARE: 'Daily early childhood education time',
            PRESCHOOL:
              'Early childhood education need related to pre-primary education'
          },
          connectedDaycareInfo: function ConnectedDaycareInfoText() {
            return (
              <>
                <P>
                  If necessary, you can apply for early childhood education
                  related to pre-primary education for the child, which is
                  subject to a charge and provided in addition to pre-primary
                  education (4 h/day) in the mornings and/or afternoons in the
                  same place as pre-primary education. If you wish early
                  childhood education to start later than pre-primary education,
                  enter the desired start date in the additional information
                  field.
                </P>
                <P>
                  When you apply for a place at a private pre-primary education
                  unit, you must apply for connected early childhood education
                  directly from the unit (with the exception of service voucher
                  units). The units will provide you with application
                  instructions. In these cases, the service guidance team will
                  convert the application into an application for pre-primary
                  education only.
                </P>
                <P>
                  Apply for a service voucher by selecting the service voucher
                  unit to which you want to apply as your preferred unit.
                </P>
                <P>
                  A separate written decision will be issued on the early
                  childhood education place. The decision will be sent to the{' '}
                  <a
                    href="https://www.suomi.fi/messages"
                    target="_blank"
                    rel="noreferrer"
                  >
                    Suomi.fi Messages
                  </a>{' '}
                  service or by post if you do not use the{' '}
                  <a
                    href="https://www.suomi.fi/messages"
                    target="_blank"
                    rel="noreferrer"
                  >
                    Suomi.fi messages
                  </a>{' '}
                  service.
                </P>
              </>
            )
          },
          connectedDaycare:
            'I also apply for early childhood education related to pre-primary education.',
          instructions: {
            DAYCARE:
              'Indicate the most commonly needed early childhood education time, the time will be specified when early childhood education begins.',
            PRESCHOOL:
              'Pre-primary education is provided in both day-care centres and schools for 4 hours a day, as a rule, between 09:00 and 13:00, but the hours may vary by unit. Indicate the early childhood education hours needed by the child so that it includes the 4 hours of pre-primary education (e.g. 7:00–17:00). The hours will be specified when early childhood education begins. If the hours of early childhood education need to vary daily or weekly (e.g. in round-the-clock care), please specify the need in the additional information field.'
          },
          usualArrivalAndDeparture: {
            DAYCARE: 'Daily early childhood education time',
            PRESCHOOL: 'Daily early childhood education time'
          },
          starts: 'Start time',
          ends: 'End time'
        },
        shiftCare: {
          label: 'Evening and round-the-clock care',
          instructions:
            'Round-the-clock care refers to early childhood education needed on weekends or around the clock. Evening and round-the-clock care is mainly early childhood education taking place outside the weekday hours of 6:30 and 18:00.',
          message: {
            title: 'Evening and round-the-clock care',
            text:
              'Evening and round-the-clock care is intended for children whose both parents do shift work or study mainly in the evenings and/or on weekends. The application must be appended with both parents’ proof of shift work from the employer or studies that cause the need for evening and round-the-clock care.'
          },
          attachmentsMessage: {
            text:
              'Evening and round-the-clock care is intended for children whose both parents do shift work or mainly study in the evening and/or during the weekends. In addition to the application, both parents will need to provide a document issued by their employer concerning shift work or a document concerning their studies as proof of the child’s need for evening or round-the-clock care. We recommend that you send the additional documents through this online system. If you are unable to add attachments to your online application, please send the documents by post to Early childhood education service counselling, P.O. Box 3125, 02070 City of Espoo.',
            subtitle:
              'Add here, for both parents, either a document issued by their employer concerning shift work or a document concerning their evening/weekend studies.'
          }
        },
        assistanceNeed: 'Support need',
        assistanceNeeded: 'The child needs support',
        assistanceNeedPlaceholder:
          'Describe the need for support for the child.',
        assistanceNeedInstructions: {
          DAYCARE:
            'The support need refers to such a need for support measures that has been indicated by an expert opinion. If the child has not previously attended the Espoo early childhood education services and their support need has been established, the Special Early Education Coordinator will contact you, if necessary, once you have indicated the support need on this application.',
          CLUB:
            'The support need refers to such a need for support measures that has been indicated by an expert opinion. If the child has not previously attended the Espoo early childhood education services and their support need has been established, the Special Early Education Coordinator will contact you, if necessary, once you have indicated the support need on this application.',
          PRESCHOOL:
            'The support need refers to such a need for support measures that has been indicated by an expert opinion. If the child has not previously attended the Espoo early childhood education services and their support need has been established, the Special Early Education Coordinator will contact you, if necessary, once you have indicated the support need on this application.'
        },
        preparatory:
          'The child needs support with learning Finnish. I am also applying for preparatory education. Not applicable for Swedish pre-primary education.',
        preparatoryInfo:
          'The preparatory education for children in the pre-primary education age is intended for children with an immigrant background, Finnish returnee children, children of bilingual families (excl. Finnish/Swedish) and adopted children who need support with the Finnish language before their transition to basic education. Preparatory education is provided, in addition to pre-primary education, on average for 1 hour per day. Education is free of charge.'
      },
      unitPreference: {
        title: 'Preferred units',
        siblingBasis: {
          title: 'Application on a sibling basis',
          info: {
            DAYCARE: function SiblingBasisSummaryTextDaycare() {
              return (
                <>
                  <P>
                    The child has a sibling basis to the same early childhood
                    education place attended by their sibling at the time of
                    making the decision. All children living at the same address
                    are considered to be siblings. The aim is to place siblings
                    at the same early childhood education place, if the family
                    so wishes. If you are applying for places for siblings who
                    do not yet attend early childhood education, enter this
                    piece of information in the additional information field.
                  </P>
                  <P>
                    Fill in this information only if you want to exercise the
                    sibling-basis right.
                  </P>
                </>
              )
            },
            PRESCHOOL: function SiblingBasisSummaryTextPreschool() {
              return (
                <>
                  <P>
                    A child enrolling for pre-primary education has a
                    sibling-basis right to a
                  </P>
                  <ol type="a">
                    <li>
                      early childhood education in their own service area, where
                      they have a sibling who has, at the moment of making the
                      decision and in the coming pre-primary education year, a
                      place at the pre-primary education day-care centre
                    </li>
                    <li>
                      local school determined by the city, which will be
                      attended by their sibling in the coming school year.
                    </li>
                  </ol>
                  <P>
                    If the child enrolling for pre-primary education has a
                    sibling-basis right to both options, the guardian may decide
                    which of them will be exercised. The selection is indicated
                    by entering the name of the sibling in the field below.
                  </P>
                  <P>
                    Fill in this information only if you want to exercise the
                    sibling-basis right.
                  </P>
                </>
              )
            },
            CLUB: function SiblingBasisSummaryTextClub() {
              return (
                <>
                  <P>
                    Children living in the same address are considered siblings.
                    An effort is made to put the siblings into a same club group
                    when the family wishes that.
                  </P>
                  <P>
                    Fill in this information only if you want to exercise the
                    sibling-basis right and choose the same club the sibling
                    attends below.
                  </P>
                </>
              )
            }
          },
          checkbox: {
            DAYCARE:
              'I primarily apply for a place at the same early childhood education location attended by the child’s sibling.',
            PRESCHOOL:
              'I primarily apply for a place in the same location attended by the child’s sibling.',
            CLUB:
              'I primarily apply for a place in the same club attended by the child’s sibling.'
          },
          radioLabel: {
            DAYCARE: 'Choose the sibling',
            PRESCHOOL: 'Choose the sibling',
            CLUB: 'Choose the sibling'
          },
          otherSibling: 'Other sibling',
          names: 'First and last name of the sibling',
          namesPlaceholder: 'First and last name',
          ssn: 'Personal identity code of the sibling',
          ssnPlaceholder: 'e.g. 010110A000P'
        },
        units: {
          title: 'Preferred units',
          startDateMissing:
            'To select preferred units first set the preferred start date on "Service need" section',
          info: {
            DAYCARE: function UnitPreferenceInfoTextDaycare() {
              return (
                <>
                  <P>
                    You can apply for 1 to 3 locations in your order of
                    preference. Application preferences do not guarantee a place
                    at the desired location, but the possibility of obtaining a
                    desired location increases by giving more than one option.
                  </P>
                  <P>
                    You can display the unit locations by choosing {"'"}Unit map
                    view{"'"}.
                  </P>
                  <P>
                    Apply for a service voucher by selecting the service voucher
                    unit to which you want to apply as your preferred unit. When
                    applying to a service voucher unit, the unit’s supervisor
                    will also be informed of the application.
                  </P>
                </>
              )
            },
            PRESCHOOL: function UnitPreferenceInfoTextPreschool() {
              return (
                <>
                  <P>
                    You can apply for 1 to 3 locations in your order of
                    preference. Application preferences do not guarantee a place
                    at the desired location, but the possibility of obtaining a
                    desired location increases by giving more than one option.
                  </P>
                  <P>
                    You can display the unit locations by choosing {"'"}Unit map
                    view{"'"}.
                  </P>
                  <P>
                    Apply for a service voucher by selecting the service voucher
                    unit to which you want to apply as your preferred unit. When
                    applying to a service voucher unit, the unit’s supervisor
                    will also be informed of the application.
                  </P>
                </>
              )
            },
            CLUB: function UnitPreferenceInfoTextClub() {
              return (
                <>
                  <P>
                    You can apply for 1 to 3 locations in your order of
                    preference. Application preferences do not guarantee a place
                    at the desired location, but the possibility of obtaining a
                    desired location increases by giving more than one option.
                  </P>
                  <P>
                    You can display the unit locations by choosing {"'"}Unit map
                    view{"'"}.
                  </P>
                </>
              )
            }
          },
          mapLink: 'Unit map view',
          serviceVoucherLink:
            'https://www.espoo.fi/en-US/Childcare_and_education/Early_childhood_education/Applying_for_early_childhood_education/Service_voucher/Information_for_families',
          languageFilter: {
            label: 'Language of the location:',
            fi: 'finnish',
            sv: 'swedish'
          },
          select: {
            label: 'Select preferences',
            placeholder: 'Select units',
            maxSelected:
              'Maximum number of preferred units reached. Remove an unit so you can add a new one',
            noOptions: 'No matches'
          },
          preferences: {
            label: 'Application preferences you selected',
            noSelections: 'No selections',
            info:
              'Select 1 - 3 preferred units, and sort them in preferred order with the arrows',
            fi: 'finnish',
            sv: 'swedish',
            moveUp: 'Move up',
            moveDown: 'Move down',
            remove: 'Remove preference'
          }
        }
      },
      fee: {
        title: 'Early childhood education fee',
        info: {
          DAYCARE: function FeeInfoTextDaycare() {
            return (
              <P>
                The client fees for municipal early childhood education and the
                client charges for service voucher users are calculated as a
                percentage of the family’s gross income. Depending on the
                family’s size and income and the hours of early childhood
                education, the fees vary from free early childhood education to
                a maximum monthly fee of EUR 288 per child. Service voucher
                units may, however, charge an additional monthly fee of EUR 0–50
                per child. Each family must provide information about their
                gross income using the income statement form, no later than two
                weeks after their child’s early childhood education has started.
              </P>
            )
          },
          PRESCHOOL: function FeeInfoTextPreschool() {
            return (
              <P>
                The client fees for municipal early childhood education are
                calculated as a percentage of the family’s gross income. Each
                family must provide information about their gross income using
                the income statement form, no later than two weeks after their
                child’s early childhood education has started.
              </P>
            )
          },
          CLUB: function FeeInfoTextClub() {
            return <P></P>
          }
        },
        emphasis: function FeeEmphasisText() {
          return (
            <strong>
              The income statement is not needed if the family agrees to pay the
              highest fee.
            </strong>
          )
        },
        checkbox:
          'I give consent to the highest fee. This consent will remain valid until I state otherwise.',
        links: function FeeLinksText() {
          return (
            <P>
              You can find further information about early childhood education
              fees, additional fees for service voucher users and the income
              statement form here:
              <br />
              <a
                href="https://www.espoo.fi/en-US/Childcare_and_education/Early_childhood_education/Early_childhood_education_fees"
                target="_blank"
                rel="noopener noreferrer"
              >
                Early childhood education fees
              </a>
            </P>
          )
        }
      },
      additionalDetails: {
        title: 'Other additional information',
        otherInfoLabel: 'Additional information',
        otherInfoPlaceholder:
          'If you wish, you can provide more detailed information related to the application in this field',
        dietLabel: 'Special diet',
        dietPlaceholder:
          'If you wish, you can indicate your child’s special diet in this field',
        dietInfo: function DietInfoText() {
          return 'Some special diets require a separate medical certificate to be delivered to the early childhood education location. Exceptions are a low-lactose or lactose-free diet, a diet based on religious beliefs and vegetarian diet (lacto-ovo).'
        },
        allergiesLabel: 'Allergies',
        allergiesPlaceholder:
          'If you wish, you can indicate your child’s allergies in this field',
        allergiesInfo:
          'Information on allergies is mainly needed when applying for family day care.'
      },
      contactInfo: {
        title: 'Personal information',
        info: function ContactInfoInfoText() {
          return (
            <P>
              The personal information has been retrieved from the population
              data services and cannot be changed with this application. If the
              personal information is incorrect, please update the information
              on
              <a
                href="https://dvv.fi/en/certificates-from-the-population-information-system."
                target="_blank"
                rel="noopener noreferrer"
              >
                dvv.fi
              </a>{' '}
              If your address is about to change, you can add the new address in
              a separate field in the application. Add a future address for both
              the child and guardian. The address information will be official
              only when it has been updated to the database of the Digital and
              Population Data Services Agency. Decisions on the child{"'"}s
              early childhood or pre-primary education place will be
              automatically sent to another guardian who lives at a different
              address based on the Population Information System.
            </P>
          )
        },
        emailInfoText:
          'The email address is used for notifying about new eVaka messages. The current email address has been fetched from the eVaka customer records. In case you modify it, the old email will be updated once the application is sent.',
        childInfoTitle: "Child's information",
        childFirstName: "Child's first name(s)",
        childLastName: "Child's last name",
        childSSN: "Child's personal identity code",
        homeAddress: 'Home address',
        moveDate: 'Date of move',
        street: 'Street address',
        postalCode: 'Postal code',
        postOffice: 'Post office',
        guardianInfoTitle: 'Guardian information',
        guardianFirstName: 'Guardian first name(s)',
        guardianLastName: 'Guardian last name',
        guardianSSN: 'Guardian personal identity code',
        phone: 'Phone number',
        verifyEmail: 'Verify email',
        email: 'Email',
        noEmail: "I don't have an email address",
        secondGuardianInfoTitle: 'Second guardian information',
        secondGuardianInfo:
          'Second guardian information is automatically fetched from VTJ',
        secondGuardianNotFound:
          'Based on information from the national Population Information System (VTJ) the child does not have a second guardian.',
        secondGuardianInfoPreschoolSeparated:
          'Information about other guardian is automatically fetched from the national Population Information System. According to it, the other guardian lives in a different address. Guardians need to agree on applying to early education.',
        secondGuardianAgreementStatus: {
          label:
            'Have you agreed about the application together with the second guardian?',
          AGREED:
            'We have agreed with the other guardian about this application.',
          NOT_AGREED:
            'We have not agreed with the other guardian about this application.',
          RIGHT_TO_GET_NOTIFIED:
            'The other guardian has only the right to get notified about the placement.'
        },
        secondGuardianPhone: "Other guardian's phone number",
        secondGuardianEmail: "Other guardian's email",
        otherPartnerTitle:
          'Spouse or cohabiting partner (not a guardian) living in the same household',
        otherPartnerCheckboxLabel:
          'The applicant has a spouse or a cohabiting partner who lives in the same household with the applicant and who is not the child’s guardian.',
        personFirstName: 'First name(s)',
        personLastName: 'Last name',
        personSSN: 'Person identification code',
        otherChildrenTitle:
          'Other children under 18 years of age living in the same household.',
        otherChildrenInfo:
          'Other children under 18 years of age living in the same household affect the cost of daycare',
        otherChildrenChoiceInfo: 'Select children living in the same household',
        hasFutureAddress: 'Address in VTJ is changing',
        futureAddressInfo:
          'Espoo’s early childhood education considers the address retrieved from the Population Register Centre as the official address. The address in the Population Register Centre changes when the applicant makes a notification of move to Posti or the Local Register Office.',
        guardianFutureAddressEqualsChildFutureAddress:
          'Moving in with the child',
        firstNamePlaceholder: 'First names',
        lastNamePlaceholder: 'Last name',
        ssnPlaceholder: 'Person identification code',
        streetPlaceholder: 'Street address',
        postalCodePlaceholder: 'Postal code',
        municipalityPlaceholder: 'Post office',
        addChild: 'Add child',
        remove: 'Remove',
        areExtraChildren: 'Children under 18 living in the same address',
        choosePlaceholder: 'Choose'
      },
      draftPolicyInfo: {
        title: 'Your draft application was saved',
        text:
          'Your unfinished application was saved. Please note! An unfinished application is stored in the system for one month after it was last saved.',
        ok: 'OK'
      },
      sentInfo: {
        title: 'Your application has been sent',
        text:
          'You can continue editing the application until processing starts',
        ok: 'Ok!'
      },
      updateInfo: {
        title: 'Modifications saved',
        text:
          'If you wish, you can make further changes as long as the application has not been processed.',
        ok: 'Ok!'
      }
    }
  },
  decisions: {
    title: 'Decisions',
    summary: function DecisionsSummaryText() {
      return (
        <P width="800px">
          This page displays the received decisions regarding child{"'"}s early
          childhood education, preschool and club applications. Upon receiving a
          new decision, you are required to respond in two weeks, whether you
          accept or reject it.
        </P>
      )
    },
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
        accept1: 'We accept the placement from',
        accept2: '',
        reject: 'We reject the placement',
        cancel: 'Cancel',
        submit: 'Submit response to the decision',
        disabledInfo:
          'NOTE! You are able to accept/reject the related early childhood education decision if you accept the preschool / preparatory education decision first.'
      },
      openPdf: 'Show the decision',
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
    summary: function ApplicationListSummaryText() {
      return (
        <P width="800px">
          A child’s guardian can submit an application for early childhood
          education or a club and enroll the child to pre-primary education. You
          can also use the same application to apply for a service voucher for
          early childhood education. To do this, you have to apply for a place
          in early childhood education at a service voucher unit. Information on
          the guardian’s children is automatically retrieved from the Digital
          and Population Data Services Agency and displayed in this view.
        </P>
      )
    },
    pageLoadError: 'Failed to load guardian applications',
    noApplications: 'No applications',
    type: {
      DAYCARE: 'Daycare application',
      PRESCHOOL: 'Early education application',
      CLUB: 'Club application'
    },
    transferApplication: 'Transfer application',
    unit: 'Unit',
    period: 'Period',
    created: 'Created',
    modified: 'Modified',
    status: {
      title: 'Status',
      CREATED: 'Draft',
      SENT: 'Sent',
      WAITING_PLACEMENT: 'Being processed',
      WAITING_DECISION: 'Being processed',
      WAITING_UNIT_CONFIRMATION: 'Being processed',
      WAITING_MAILING: 'Being processed',
      WAITING_CONFIRMATION: 'Waiting for confirmation from the guardian',
      REJECTED: 'Place rejected',
      ACTIVE: 'Accepted',
      CANCELLED: 'Place rejected'
    },
    openApplicationLink: 'Open application',
    editApplicationLink: 'Edit application',
    removeApplicationBtn: 'Remove application',
    cancelApplicationBtn: 'Cancel application',
    confirmationLinkInstructions:
      'In the Decisions page you can read the decision and either accept or reject the proposed place',
    confirmationLink: 'Review and confirm the decision',
    newApplicationLink: 'New application'
  },
  fileUpload: {
    loading: 'Loading',
    loaded: 'Loaded',
    error: {
      FILE_TOO_LARGE: 'File is too big (max. 10MB)',
      SERVER_ERROR: 'Upload failed'
    },
    input: {
      title: 'Add an attachment',
      text: [
        'Press here or drag and drop a new file',
        'Max file size: 10MB.',
        'Allowed formats:',
        'PDF, JPEG/JPG, PNG and DOC/DOCX'
      ]
    },
    deleteFile: 'Delete file'
  },
  fileDownload: {
    modalHeader: 'Processing file',
    modalMessage: 'File is being processed. Try again later'
  },
  validationErrors: {
    required: 'Value missing',
    requiredSelection: 'Please select one of the options',
    format: 'Give value in correct format',
    ssn: 'Invalid person identification number',
    phone: 'Invalid telephone number',
    email: 'Invalid email',
    validDate: 'Valid date format is pp.kk.vvvv',
    preferredStartDate: 'Invalid preferred start date',
    timeFormat: 'Valid time format is hh:mm',
    unitNotSelected: 'Pick at least one choice',
    emailsDoNotMatch: 'The emails do not match'
  }
}

export default en
