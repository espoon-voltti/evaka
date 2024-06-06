// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import ExternalLink from 'lib-components/atoms/ExternalLink'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { Translations as ComponentTranslations } from 'lib-components/i18n'
import { H1, H2, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { Translations } from 'lib-customizations/citizen'
import colors from 'lib-customizations/common'

import components from '../../components/i18n/en'

import fi from './fi'

const yes = 'Yes'
const no = 'No'

const componentTranslations: ComponentTranslations = {
  ...components,
  messageReplyEditor: {
    ...components.messageReplyEditor,
    messagePlaceholder:
      'Content... Note! Do not enter sensitive information here.',
    messagePlaceholderSensitiveThread: 'Content...'
  }
}

const en: Translations = {
  common: {
    title: 'Early childhood education',
    cancel: 'Cancel',
    return: 'Return',
    download: 'Download',
    print: 'Print',
    ok: 'Ok',
    save: 'Save',
    discard: 'Discard',
    saveConfirmation: 'Do you want to save changes?',
    confirm: 'Confirm',
    delete: 'Remove',
    edit: 'Edit',
    add: 'Add',
    show: 'Show',
    hide: 'Hide',
    yes,
    no,
    yesno: (value: boolean): string => (value ? yes : no),
    select: 'Select',
    page: 'Page',
    unit: {
      providerTypes: {
        MUNICIPAL: 'Municipal',
        PURCHASED: 'Purchased service',
        PRIVATE: 'Private service',
        MUNICIPAL_SCHOOL: 'Municipal',
        PRIVATE_SERVICE_VOUCHER: 'Private service (service voucher)',
        EXTERNAL_PURCHASED: 'Purchased service (other)'
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
        fi: 'Finnish',
        sv: 'Swedish',
        en: 'English'
      },
      languagesShort: {
        fi: 'Finnish',
        sv: 'Swedish',
        en: 'English'
      }
    },
    openExpandingInfo: 'Open the details',
    errors: {
      genericGetError: 'Error in fetching the requested information'
    },
    datetime: {
      dayShort: 'd',
      weekdaysShort: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
      week: 'Week',
      weekShort: 'Wk',
      weekdays: [
        'Monday',
        'Tuesday',
        'Wednesday',
        'Thursday',
        'Friday',
        'Saturday',
        'Sunday'
      ],
      months: [
        'January',
        'February',
        'March',
        'April',
        'May',
        'June',
        'July',
        'August',
        'September',
        'October',
        'November',
        'December'
      ]
    },
    closeModal: 'Close popup',
    close: 'Close',
    duplicatedChild: {
      identifier: {
        DAYCARE: {
          short: 'ECE',
          long: 'Early childhood education'
        },
        PRESCHOOL: {
          short: 'PPE',
          long: 'Pre-primary education'
        }
      }
    },
    tense: {
      past: 'Past',
      present: 'Active',
      future: 'Future'
    }
  },
  header: {
    nav: {
      map: 'Map',
      applications: 'Applications',
      decisions: 'Decisions',
      personalDetails: 'Personal information',
      income: 'Income',
      messages: 'Messages',
      calendar: 'Calendar',
      children: 'Children',
      subNavigationMenu: 'Menu',
      messageCount: (n: number) =>
        n > 1 ? `${n} new messages` : `${n} new message`
    },
    lang: {
      fi: 'Suomeksi',
      sv: 'På svenska',
      en: 'In English'
    },
    langMobile: {
      fi: 'Suomeksi',
      sv: 'Svenska',
      en: 'English'
    },
    login: 'Log in',
    logout: 'Log out',
    openMenu: 'Open menu',
    closeMenu: 'Close menu',
    goToHomepage: 'Go to homepage',
    notifications: 'notifications',
    attention: 'Attention',
    requiresStrongAuth: 'requires strong authentication'
  },
  footer: {
    cityLabel: '© City of Espoo',
    privacyPolicyLink: (
      <a
        href="https://www.espoo.fi/en/city-espoo/data-protection"
        data-qa="footer-policy-link"
        style={{ color: colors.main.m2 }}
      >
        Privacy Notices
      </a>
    ),
    accessibilityStatement: 'Accessibility statement',
    sendFeedbackLink: (
      <a
        href="https://easiointi.espoo.fi/eFeedback/en/Feedback/20-S%C3%A4hk%C3%B6iset%20asiointipalvelut"
        data-qa="footer-feedback-link"
        style={{ color: colors.main.m2 }}
      >
        Give feedback
      </a>
    )
  },
  loginPage: {
    title: 'City of Espoo early childhood education',
    login: {
      title: 'Sign in with username',
      paragraph:
        'Take care of your child’s daily early childhood education affairs in eVaka.',
      link: 'Log in',
      infoBoxText: (
        <>
          <P>
            If you are not able to log in here, see the instructions{' '}
            <a
              href="https://www.espoo.fi/fi/palvelut/evaka"
              target="_blank"
              rel="noreferrer"
            >
              eVaka | Espoon kaupunki
            </a>
            . You can also log in with strong authentication.
          </P>
        </>
      )
    },
    applying: {
      title: 'Sign in using Suomi.fi',
      paragraph: 'By using strong suomi.fi-authentication in eVaka, you can',
      infoBoxText:
        'When using strong authentication the personal data and address information of the registrant, the second guardian and the dependants are retrieved from the population data service.',
      infoBullets: [
        'apply for an early childhood, pre-primary or club place for your child or view a previous application',
        'view pictures and other documents related to your child’s early childhood or pre-primary education',
        "report your or your child's income information",
        'accept or reject a decision, if you made the application'
      ],
      link: 'Authenticate',
      mapText: 'Look up the units that you can apply to in eVaka on the map.',
      mapLink: 'Units on the map'
    }
  },
  ctaToast: {
    holidayPeriodCta: (period: FiniteDateRange, deadline: LocalDate) => (
      <>
        <InlineButton text="Register" onClick={() => undefined} /> attendances
        and absences between {period.start.format('dd.MM.')}-
        {period.end.format()} no later than {deadline.format()}. The exact times
        of attendances will be marked after the questionnaire has closed.
      </>
    ),
    fixedPeriodCta: (deadline: LocalDate) =>
      `Answer the absence questionnaire before ${deadline.format()}.`,
    incomeExpirationCta: (expirationDate: string) =>
      `Please remember to update your income statement by ${expirationDate}`
  },
  errorPage: {
    reload: 'Reload page',
    text: 'We encountered an unexpected error. The developers have been notified with the error details.',
    title: 'Something went wrong'
  },
  map: {
    title: 'Units on the map',
    mainInfo:
      "In this view, you can search the map for Espoo's early childhood education, pre-primary education and club places.",
    privateUnitInfo: (
      <span>
        For information on private units,{' '}
        <ExternalLink
          text="click here."
          href="https://www.espoo.fi/en/childcare-and-education/early-childhood-education/private-early-childhood-education-and-day-care-centers"
          newTab
        />
      </span>
    ),
    searchLabel: 'Search by address or by a unit name',
    searchPlaceholder: 'For example Purolan päiväkoti',
    address: 'Address',
    noResults: 'No results',
    keywordRequired: 'Type a keyword',
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
      PRIVATE_SERVICE_VOUCHER: 'private service (service voucher)',
      EXTERNAL_PURCHASED: 'purchased service (other)',
      MUNICIPAL_SCHOOL: 'school'
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
      list: 'Units'
    },
    serviceVoucherLink:
      'https://www.espoo.fi/en/childcare-and-education/early-childhood-education/service-voucher#section-6228',
    noApplying: 'No applying via eVaka, contact the unit',
    backToSearch: 'Back to search'
  },
  calendar: {
    title: 'Calendar',
    holiday: 'Holiday',
    absent: 'Absent',
    absentFree: 'Free absence',
    absentPlanned: 'Shift work absence',
    absences: {
      SICKLEAVE: 'Absence due to illness',
      PLANNED_ABSENCE: 'Shift work absence'
    },
    absenceMarkedByEmployee: 'Absence marked by staff',
    contactStaffToEditAbsence:
      'Contact staff if you want to change the absence',
    intermittentShiftCareNotification: 'Shift care',
    newReservationOrAbsence: 'Attendance / Absence',
    newHoliday: 'Holiday questionnaire',
    newAbsence: 'Register absence',
    newReservationBtn: 'Register attendance',
    missingReservation: 'No attendance',
    reservationNotRequired: 'Reservation not required',
    termBreak: 'No teaching today',
    reservation: 'Registered attendance',
    reservationNoTimes: 'Present',
    attendance: 'Actual attendance',
    exceedStart: 'Arrived earlier than reported.',
    exceedEnd: 'Departed later than reported.',
    exceedGeneric: 'Actual attendance time exceeds reported time.',
    calculatedUsedServiceTime:
      'Used service need is determined by the monthly service need.',
    usedService: 'Used service need',
    reservationsAndRealized: 'Period of attendance',
    events: 'Events of the day',
    noActivePlacements:
      'Your children are not in early childhood or pre-primary education on this day',
    attendanceWarning:
      'The realized attendance time exceeds the reported attendance time',
    eventsCount: 'events',
    shiftCareInfoLabel: 'Early childhood education overnight?',
    shiftCareInfo: (): React.ReactNode => (
      <>
        <P>
          If the child needs early childhood education overnight, mark the part
          before midnight to end at 23:59, and the part after midnight to start
          the next day at 00:00.
        </P>
        <P>
          For example, if the child is in care overnight from Monday evening at
          20:00 to Tuesday morning at 06:00, mark for Monday 20:00 - 23:59 and
          for Tuesday 00:00 - 06:00
        </P>
      </>
    ),
    reservationModal: {
      title: 'Register attendance',
      selectChildren: 'Select children',
      holidayPeriod: (period: FiniteDateRange) =>
        `Holiday period: ${period.start.format('dd.MM.')}-${period.end.format(
          'dd.MM.'
        )}. Mark holiday attendances and absences by the deadline. The exact times of attendances will be marked after the questionnaire has closed.`,
      dateRange: 'Validity',
      dateRangeLabel: 'Enter period of attendance',
      dateRangeInfo: (date: LocalDate) =>
        `You can register attendances until ${date.format()}.`,
      missingDateRange: 'Select period of attendance',
      noReservableDays: 'No reservable days',
      selectRecurrence: 'Select recurrence of attendance',
      postError: 'The reservation failed',
      repetitions: {
        DAILY: 'Same time every day',
        WEEKLY: 'Different depending on the day of the week',
        IRREGULAR: 'Changes frequently'
      },
      start: 'Start',
      end: 'End',
      present: 'Present',
      absent: 'Absent',
      reservationClosed: 'Reservation closed',
      reservationClosedInfo:
        'Please contact staff if you want to add an attendance for this day',
      saveErrors: {
        failure: 'Could not save',
        NON_RESERVABLE_DAYS: 'Some of the selected days cannot be reserved'
      }
    },
    absenceModal: {
      title: 'Report absence',
      selectedChildren: 'Selected children',
      selectChildrenInfo:
        'Only report full day absences. Part-day absences can be reported when making a reservation.',
      lockedAbsencesWarningTitle: 'Absence for multiple days',
      lockedAbsencesWarningText:
        'You are marking an absence for multiple days for which the registration deadline has passed. This can be only cancelled by contacting the daycare staff.',
      dateRange: 'Absent during',
      absenceType: 'Reason for absence',
      absenceTypes: {
        SICKLEAVE: 'Sickness',
        OTHER_ABSENCE: 'Absence',
        PLANNED_ABSENCE: 'Shift work absence'
      },
      contractDayAbsenceTypeWarning:
        'Only some of the children have contract days in use, so the absence of contract days cannot be recorded for all children at the same time',
      attendanceAlreadyExistsErrorTitle:
        'There is already attendance record for the selected days',
      attendanceAlreadyExistsErrorDescription:
        'It is not possible to add an absence for a child on a day that already has an attendance record.'
    },
    holidayModal: {
      additionalInformation: 'Read more',
      holidayFor: 'Holiday for:',
      childOnHoliday: 'Child is on holiday',
      addTimePeriod: 'Add holiday',
      emptySelection: 'No free absences',
      notEligible: (period: FiniteDateRange) =>
        `The questionnaire doesn't concern the child, because he or she hasn't been in early childhood education continuously during ${period.format()}.`
    },
    previousDay: 'Previous day',
    nextDay: 'Next day',
    previousMonth: 'Previous month',
    nextMonth: 'Next month',
    dailyServiceTimeModifiedNotification: (dateFrom: string) =>
      `A new daily service time has been updated to your early childhood education contract starting from ${dateFrom}.`,
    dailyServiceTimeModifiedDestructivelyModal: {
      text: (dateFrom: string) =>
        `A new daily service time has been updated to your early childhood education contract. Please make new reservations starting from ${dateFrom}.`,
      title: 'Make new reservations',
      ok: 'OK'
    },
    absentEnable: 'Mark as absent',
    absentDisable: 'Mark as present',
    validationErrors: {
      range: 'Outside opening hours'
    },
    monthSummary: {
      title: 'Attendance summary',
      reserved: 'Reservations',
      usedService: 'Realized',
      minutes: 'min',
      hours: 'h',
      warningUsedServiceExceeded: 'Attendance in excess of the contract:',
      warningPlannedServiceExceeded:
        'Attendance planned in excess of the contract:'
    }
  },
  messages: {
    inboxTitle: 'Inbox',
    noMessagesInfo: 'Sent and received messages are displayed here.',
    noSelectedMessage: 'A message is not selected',
    emptyInbox: 'Your inbox is empty',
    openMessage: 'Open message',
    recipients: 'Recipients',
    send: 'Send',
    sender: 'Sender',
    sending: 'Sending',
    types: {
      MESSAGE: 'Message',
      BULLETIN: 'Bulletin'
    },
    deleteThread: 'Delete thread',
    staffAnnotation: 'Staff',
    threadList: {
      participants: 'Participants',
      title: 'Title',
      sentAt: 'Sent',
      message: 'Message'
    },
    thread: {
      children: 'Regarding children',
      title: 'Subject',
      reply: 'Reply',
      jumpToLastMessage: 'Jump to last message',
      jumpToBeginning: 'Jump to beginning',
      close: 'Back to list',
      sender: 'Sender',
      sentAt: 'Sent at',
      recipients: 'Recipients'
    },
    messageEditor: {
      newMessage: 'New message',
      recipients: 'Recipients',
      secondaryRecipients: 'Other recipients',
      singleUnitRequired:
        'Messages can only be sent to a single unit at a time',
      children: 'The message is regarding',
      subject: 'Subject',
      message: 'Message',
      deleteDraft: 'Delete draft',
      send: 'Send',
      discard: 'Discard',
      search: 'Search',
      noResults: 'No results, select a child or children first',
      messageSendError: 'Failed to send the message'
    },
    confirmDelete: {
      title: 'Do you really want to delete the message thread?',
      text: 'The messages cannot be recovered after deleting.',
      cancel: 'Do not delete',
      confirm: 'Delete thread'
    },
    sensitive: 'Sensitive message thread',
    strongAuthRequired: 'Reading requires strong authentication',
    strongAuthRequiredThread: 'Reading requires strong authentication.',
    strongAuthLink: 'Authenticate'
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
        'The application for early childhood education is used to apply for a municipal early childhood education place at an early childhood education unit, family daycare provider and group family daycare provider. You can also use the same application to apply for a service voucher for early childhood education. Under Preferred units, select the service voucher unit to which you want to apply.',
      preschoolLabel:
        'Enrolment to pre-primary education and/or preparatory education',
      preschoolInfo:
        'Free-of-charge pre-primary education is available for four hours per day. In addition, you can apply for connected early childhood education (subject to a fee) for your child. You can also apply for a service voucher for connected early childhood education. Under Preferred units, select the service voucher unit to which you want to apply. It is available in pre-primary education units in the morning before pre-primary education and in the afternoon after pre-primary education. You can submit an application for connected early childhood education at the same time as you enrol your child in pre-primary education or through a separate application after pre-primary education has started. You can also use the same application to apply for free-of-charge preparatory education and early childhood education connected to preparatory education.',
      preschoolDaycareInfo:
        'With this form, you can also apply for connected early childhood education for a child who will be / has been enrolled in pre-primary education or preparatory education',
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
      applicationInfo: (
        <P>
          You can make changes to your application until its processing starts.
          After this, you can make changes to your application by contacting
          early childhood education service counselling (tel. 09 816 31000). If
          you wish to cancel an application you have submitted, please send an
          email to early childhood education service counselling (
          <a href="mailto:vaka.palveluohjaus@espoo.fi">
            vaka.palveluohjaus@espoo.fi
          </a>
          ).
        </P>
      )
    },
    editor: {
      heading: {
        title: {
          DAYCARE:
            'Application for early childhood education and service voucher',
          PRESCHOOL: 'Registration for pre-primary education',
          CLUB: 'Application for club'
        },
        info: {
          DAYCARE: (
            <>
              <P>
                You can apply for early childhood education all year round. The
                application must be submitted at the latest four months before
                the need for early childhood education begins. If you urgently
                need early childhood education, you must apply for a place at
                the latest two weeks before the need begins.
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
              <P>
                The client fees for municipal early childhood education and the
                client charges for service voucher users are calculated as a
                percentage of the family’s gross income. The fees vary depending
                on the family’s size and the hours of early childhood education.
                Service voucher units may charge an additional monthly fee,
                information of the possible additional fee can be found from
                units own website. Each family must provide information about
                their gross income, no later than two weeks after their child’s
                early childhood education has started.
              </P>
              <P>
                You can find further information about early childhood education
                fees, additional fees for service voucher users and how to
                deliver the income statement form here:{' '}
                <a href="https://www.espoo.fi/en/childcare-and-education/early-childhood-education/early-childhood-education-fees">
                  Early childhood education fees
                </a>
                .
              </P>
              <P fitted={true}>* Information marked with a star is required</P>
            </>
          ),
          PRESCHOOL: (
            <>
              <P>
                Children attend pre-primary education in the year preceding the
                start of their compulsory education. Pre-primary education is
                free of charge. Enrolment for pre-primary education in the
                2024–2025 school year takes place on 9–19 January 2024. Finnish
                and swedish pre-primary education begins on 8 August 2024. The
                decisions will be sent to the{' '}
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
              <P fitted={true}>* Information marked with a star is required</P>
            </>
          ),
          CLUB: (
            <>
              <P>
                The application period for clubs that start in the autumn is in
                March. If your child is given a place in a club, you will
                receive a decision in April or May. The decision will cover one
                operating period (from August until the end of May). The
                decision will be sent to the Suomi.fi service or by post if you
                have not activated the service.
              </P>
              <P>
                You can also submit a club application outside of the
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
                activities will not affect the child home care allowance paid by
                Kela. However, if the child has been granted a place in early
                childhood education or your family has been granted private
                daycare allowance, a place in a club cannot be granted to the
                child
              </P>
              <P fitted={true}>
                * Information marked with an asterisk is required
              </P>
            </>
          )
        },
        errors: (count: number) =>
          count === 1 ? '1 error' : `${count} errors`,
        hasErrors: 'Please correct the following information:',
        invalidFields: (count: number) =>
          `${count} fields with missing or invalid information`
      },
      actions: {
        hasVerified:
          'I have checked that the information in the application is correct',
        allowOtherGuardianAccess: (
          <span>
            I understand that the application will also be visible to the other
            guardian. If the other guardian should not be able to see this
            application, please contact the Early Childhood Education Service
            Guidance, whose contact information can be found{' '}
            <a
              href="https://www.espoo.fi/childcare-and-education/early-childhood-education/service-guidance-and-applying-early-childhood-education#section-38795"
              target="_blank"
              rel="noreferrer"
            >
              here
            </a>
            .
          </span>
        ),
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
        notYetSent: (
          <P>
            <strong>The application has not yet been sent.</strong> Please check
            the information you provided and send the application by clicking{' '}
            &ldquo;Send the application&ldquo; button in the end of the form.
          </P>
        ),
        notYetSaved: (
          <P>
            <strong>The changes have not been saved yet.</strong> Please check
            the information you provided and send the application by clicking
            &ldquo;Save changes&ldquo; button at the end of the form.
          </P>
        ),
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
            label: 'Early childhood education',
            withConnectedDaycare:
              'I also apply for early childhood education related to pre-primary education.',
            withoutConnectedDaycare: 'No',
            startDate: 'Desired start date',
            serviceNeed: 'Service need'
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
            info: 'Second guardian information is automatically fetched from VTJ',
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
          missing: 'To select service need first set the preferred start date',
          info: {
            DAYCARE: [],
            PRESCHOOL: [
              'Finnish and Swedish-language pre-primary education starts on 8 August 2024.'
            ],
            CLUB: [
              'Clubs follow pre-primary education work and vacation times.'
            ]
          },
          clubTerm: 'Club term',
          clubTerms: 'Club terms',
          label: {
            DAYCARE: 'Desired start date',
            PRESCHOOL: 'Start date in August',
            CLUB: 'Desired start date'
          },
          noteOnDelay: 'The application processing time is 4 months.',
          instructions: {
            DAYCARE: (
              <>
                You can postpone your desired start date until the processing of
                your application starts. After this, you can make changes to
                your desired start date by contacting early childhood education
                service counselling (tel. 09 816 31000).
              </>
            ),
            PRESCHOOL: (
              <>
                You can postpone your desired start date until the processing of
                your application starts. After this, you can make changes to
                your desired start date by contacting early childhood education
                service counselling (tel. 09 816 31000).
              </>
            ),
            CLUB: null
          },
          placeholder: 'Select the start date',
          validationText: 'Desired start date: '
        },
        clubDetails: {
          wasOnDaycare:
            'Child has an early childhood education placement, which he or she will give up when he or she gets a club placement',
          wasOnDaycareInfo:
            'If a child has been in early childhood education, family daycare or group family daycare and gives up his or her place at the start of the club, he or she has a better chance of getting a club placement.',
          wasOnClubCare: 'Child has been in a club during last club term',
          wasOnClubCareInfo:
            'If the child has already been to the club during the previous club term, he or she has a better chance of getting a placement in the club.'
        },
        urgent: {
          label: 'Application is urgent',
          attachmentsMessage: {
            text: (
              <P fitted={true}>
                If the need for a place in early childhood education arises from
                sudden employment or a sudden start of studies, you will need to
                apply for a place no later than{' '}
                <strong>two weeks before</strong> your child needs it. In
                addition to the application, you will need to{' '}
                <strong>
                  provide documents as proof of the employment or student status
                </strong>{' '}
                of both guardians living in the same household. If you are
                unable to add attachments to your online application, please
                send the documents by post to Early childhood education service
                counselling, P.O. Box 3125, 02070 City of Espoo. The processing
                period of two weeks will not begin until we have received the
                application and the required attachments.
              </P>
            ),
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
          connectedDaycareInfo: (
            <>
              <P>
                You can apply for your child early childhood education related
                to pre-primary education, which is subject to a charge. Related
                early childhood education is provided in the mornings before
                pre-primary education and in the afternoons after pre-primary
                education in accordance with the opening hours of the
                pre-primary education unit.
              </P>
              <P>
                If your child needs early childhood education as of 1 August
                2024 before pre-primary education starts, please take this into
                account when selecting the desired starting date.
              </P>
              <P>
                When applying for a service voucher unit, apply for a service
                voucher for related early childhood education by selecting as
                your preferred unit the service voucher unit you want to apply
                for.
              </P>
              <P>
                When applying for private pre-primary education units, apply for
                related early childhood education directly from the unit (with
                the exception of service voucher units); the units inform
                customers about the application procedure. If you have applied
                for related early childhood education from a private unit on the
                pre-primary education application, the service guidance will
                change your application to a pre-primary education application
                only.
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
          ),
          connectedDaycare:
            'I also apply for early childhood education related to pre-primary education.',
          instructions: {
            DAYCARE:
              'Indicate the most commonly needed early childhood education time, the time will be specified when early childhood education begins.',
            PRESCHOOL:
              'Pre-primary education is provided in both early childhood education units and schools for 4 hours a day, as a rule, between 09:00 and 13:00, but the hours may vary by unit. Indicate the early childhood education hours needed by the child so that it includes the 4 hours of pre-primary education (e.g. 7:00–17:00). The hours will be specified when early childhood education begins. If the hours of early childhood education need to vary daily or weekly (e.g. in round-the-clock care), please specify the need in the additional information field.'
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
          instructions2:
            'If a guardian living in the same household does regular shift work or attends evening studies full-time, they must provide proof of this (a document issued by their employer or the educational institution) together with the application for pre-primary education.  The documents must be dated in the year when the application for pre-primary education is submitted.',
          message: {
            title: 'Evening and round-the-clock care',
            text: 'Evening and round-the-clock care is intended for children whose both parents do shift work or study mainly in the evenings and/or on weekends. The application must be appended with both parents’ proof of shift work from the employer or studies that cause the need for evening and round-the-clock care.'
          },
          attachmentsMessage: {
            text: 'Evening and round-the-clock care is intended for children whose both parents do shift work or mainly study in the evening and/or during the weekends. In addition to the application, both parents will need to provide a document issued by their employer concerning shift work or a document concerning their studies as proof of the child’s need for evening or round-the-clock care. We recommend that you send the additional documents through this online system. If you are unable to add attachments to your online application, please send the documents by post to Early childhood education service counselling, P.O. Box 3125, 02070 City of Espoo.',
            subtitle:
              'Add here, for both parents, either a document issued by their employer concerning shift work or a document concerning their evening/weekend studies.'
          }
        },
        assistanceNeed: 'Support need',
        assistanceNeeded: 'The child needs support',
        assistanceNeedLabel: 'Description',
        assistanceNeedPlaceholder:
          'Describe the need for support for the child.',
        assistanceNeedInstructions: {
          DAYCARE:
            'Select this section on the application if your child needs support for their development, learning or wellbeing. Support is provided as part of early childhood education activities. If your child needs support, a special needs teacher will contact you to ensure that your child’s needs are taken into account when granting a place in early childhood education.',
          CLUB: 'Select this section on the application if your child needs support for their development, learning or wellbeing. Support is provided as part of early childhood education activities. If your child needs support, a special needs teacher will contact you to ensure that your child’s needs are taken into account when granting a place in early childhood education.',
          PRESCHOOL:
            'Select this section on the application if your child needs support for their development and/or learning during the pre-primary education year. Support is provided as part of pre-primary education and early childhood education activities. Select this section also if your child has another special reason that directly affects the organisation of pre-primary education or in which unit their pre-primary education should be organised. If your child needs support for development and/or learning, a special needs teacher will contact you to ensure that your child’s needs are taken into account when granting a place in pre-primary education.'
        },
        preparatory:
          'The child needs support with learning Finnish. I am also applying for preparatory education. Not applicable for Swedish pre-primary education.',
        preparatoryInfo:
          'Instruction preparing for basic education is offered in connection with pre-primary education to children who do not yet have Finnish language skills or who know some Finnish. The child’s current day care centre has recommended the child attend preparatory pre-primary education. Instruction preparing for basic education is offered to children of pre-primary education age in Finnish-speaking municipal pre-primary education groups. Instruction preparing for basic education is available for 5 hours per day in connection with pre-primary education. Instruction is free of charge.'
      },
      unitPreference: {
        title: 'Preferred units',
        siblingBasis: {
          title: 'Application on a sibling basis',
          info: {
            DAYCARE: (
              <>
                <P>
                  The sibling principle applies if you apply for a place in an
                  early childhood education unit in which the child’s sibling
                  has a place when early childhood education starts. All
                  children living at the same address are considered to be
                  siblings. The aim is to place siblings at the same early
                  childhood education place, if the family so wishes. If you are
                  applying for places for siblings who do not yet attend early
                  childhood education, enter this piece of information in the
                  additional information field.
                </P>
                <P>
                  Fill in this information only if you want to exercise the
                  sibling-basis right.
                </P>
              </>
            ),
            PRESCHOOL: (
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
                    place at the pre-primary education unit
                  </li>
                  <li>
                    local school determined by the city, which will be attended
                    by their sibling in the coming school year.
                  </li>
                </ol>
                <P>
                  If the child enrolling for pre-primary education has a
                  sibling-basis right to both options, the guardian may decide
                  which of them will be exercised. The selection is indicated by
                  entering the name of the sibling in the field below.
                </P>
                <P>
                  Fill in this information only if you want to exercise the
                  sibling-basis right.
                </P>
              </>
            ),
            CLUB: (
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
          },
          checkbox: {
            DAYCARE:
              'I primarily apply for a place at the same early childhood education location attended by the child’s sibling.',
            PRESCHOOL:
              'I primarily apply for a place in the same location attended by the child’s sibling.',
            CLUB: 'I primarily apply for a place in the same club attended by the child’s sibling.'
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
          title: (maxUnits: number) =>
            maxUnits === 1 ? 'Preferred unit' : 'Preferred units',
          startDateMissing:
            'To select preferred units first set the preferred start date on "Service need" section',
          info: {
            DAYCARE: (
              <>
                <P>
                  You can apply for 1 to 3 locations in your order of
                  preference. Application preferences do not guarantee a place
                  at the desired location, but the possibility of obtaining a
                  desired location increases by giving more than one option.
                </P>
                <P>
                  You can display the unit locations by choosing &lsquo;Unit map
                  view&lsquo;.
                </P>
                <P>
                  Apply for a service voucher by selecting the service voucher
                  unit to which you want to apply as your preferred unit. When
                  applying to a service voucher unit, the unit’s supervisor will
                  also be informed of the application.
                </P>
              </>
            ),
            PRESCHOOL: (
              <>
                <P>
                  You can apply for 1–3 different units in your order of
                  preference. Application preferences do not guarantee a place
                  at the desired unit, but your chances of getting a preferred
                  place increase if you list several options.
                </P>
                <P>
                  You can display the unit locations by choosing &lsquo;Unit map
                  view&lsquo;.
                </P>
                <P>
                  Apply for a service voucher by selecting the service voucher
                  unit to which you want to apply as your preferred unit. When
                  applying to a service voucher unit, the unit’s supervisor will
                  also be informed of the application.
                </P>
              </>
            ),
            CLUB: (
              <>
                <P>
                  You can apply for 1 to 3 locations in your order of
                  preference. Application preferences do not guarantee a place
                  at the desired location, but the possibility of obtaining a
                  desired location increases by giving more than one option.
                </P>
                <P>
                  You can display the unit locations by choosing &lsquo;Unit map
                  view&lsquo;.
                </P>
              </>
            )
          },
          mapLink: 'Unit map view',
          serviceVoucherLink:
            'https://www.espoo.fi/en/childcare-and-education/early-childhood-education/service-voucher#section-6228',
          languageFilter: {
            label: 'Language of the location:',
            fi: 'finnish',
            sv: 'swedish'
          },
          select: {
            label: (maxUnits: number) =>
              maxUnits === 1 ? 'Select preference' : 'Select preferences',
            placeholder: 'Select units',
            maxSelected:
              'Maximum number of preferred units reached. Remove an unit so you can add a new one',
            noOptions: 'No matches'
          },
          preferences: {
            label: (maxUnits: number) =>
              maxUnits === 1
                ? 'Application preference you selected'
                : 'Application preferences you selected',
            noSelections: 'No selections',
            info: (maxUnits: number) =>
              maxUnits === 1
                ? 'Select one preferred unit'
                : `Select 1 - ${maxUnits} preferred units, and sort them in preferred order with the arrows`,
            fi: 'finnish',
            sv: 'swedish',
            en: 'english',
            moveUp: 'Move up',
            moveDown: 'Move down',
            remove: 'Remove preference'
          }
        }
      },
      fee: {
        title: 'Early childhood education fee',
        info: {
          DAYCARE: (
            <P>
              The client fees for municipal early childhood education and the
              client charges for service voucher users are calculated as a
              percentage of the family’s gross income. Depending on the family’s
              size and income and the hours of early childhood education, the
              fees vary from free early childhood education to a maximum monthly
              fee of EUR 288 per child. Service voucher units may, however,
              charge an additional monthly fee of EUR 0–50 per child. Each
              family must provide information about their gross income using the
              income statement form, no later than two weeks after their child’s
              early childhood education has started.
            </P>
          ),
          PRESCHOOL: (
            <P>
              The client fees for municipal early childhood education are
              calculated as a percentage of the family’s gross income. Each
              family must provide information about their gross income using the
              income statement form, no later than two weeks after their child’s
              early childhood education has started.
            </P>
          ),
          CLUB: <P />
        },
        emphasis: (
          <strong>
            The income statement is not needed if the family agrees to pay the
            highest fee.
          </strong>
        ),
        checkbox:
          'I give consent to the highest fee. This consent will remain valid until I state otherwise.',
        links: (
          <P>
            You can find further information about early childhood education
            fees, additional fees for service voucher users and the income
            statement form here:
            <br />
            <a
              href="https://www.espoo.fi/en/childcare-and-education/early-childhood-education/early-childhood-education-fees"
              target="_blank"
              rel="noopener noreferrer"
            >
              Early childhood education fees
            </a>
          </P>
        )
      },
      additionalDetails: {
        title: 'Other additional information',
        otherInfoLabel: 'Additional information',
        otherInfoPlaceholder:
          'If you wish, you can provide more detailed information related to the application in this field',
        dietLabel: 'Special diet',
        dietPlaceholder:
          'If you wish, you can indicate your child’s special diet in this field',
        dietInfo: (
          <>
            Some special diets require a separate medical certificate to be
            delivered to the early childhood education location. Exceptions are
            a low-lactose or lactose-free diet, a diet based on religious
            beliefs and vegetarian diet (lacto-ovo).
          </>
        ),
        allergiesLabel: 'Allergies',
        allergiesPlaceholder:
          'If you wish, you can indicate your child’s allergies in this field'
      },
      contactInfo: {
        title: 'Personal information',
        familyInfo: (
          <P>
            Please report all adults and children living in the same household.
          </P>
        ),
        info: (
          <P>
            The personal information has been retrieved from the population data
            services and cannot be changed with this application. If the
            personal information is incorrect, please update the information on
            <a
              href="https://dvv.fi/en/certificates-from-the-population-information-system."
              target="_blank"
              rel="noopener noreferrer"
            >
              dvv.fi
            </a>{' '}
            If your address is about to change, you can add the new address in a
            separate field in the application. Add a future address for both the
            child and guardian. The address information will be official only
            when it has been updated to the database of the Digital and
            Population Data Services Agency. Decisions on the child&lsquo;s
            early childhood or pre-primary education place will be automatically
            sent to another guardian who lives at a different address based on
            the Population Information System.
          </P>
        ),
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
          'Information about other guardian is automatically fetched from the national Population Information System. According to it, the other guardian lives in a different address.',
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
        text: 'Your unfinished application was saved. Please note! An unfinished application is stored in the system for one month after it was last saved.',
        ok: 'OK'
      },
      sentInfo: {
        title: 'Your application has been sent',
        text: 'You can continue editing the application until processing starts',
        ok: 'Ok!'
      },
      updateInfo: {
        title: 'Modifications saved',
        text: 'If you wish, you can make further changes as long as the application has not been processed.',
        ok: 'Ok!'
      }
    }
  },
  decisions: {
    title: 'Decisions',
    childhoodEducationTitle:
      'Decisions regarding child’s early childhood education, pre-primary education and clubs',
    summary: (
      <P width="800px">
        This page displays the received decisions regarding child&lsquo;s early
        childhood education, pre-primary education, clubs and payments.
        <br aria-hidden="true" />
        <br aria-hidden="true" />
        Upon receiving a new decision concerning a new placement applied for a
        child, you are required to respond in two weeks, whether you accept or
        reject it.
      </P>
    ),
    unconfirmedDecisions: (n: number) =>
      `${n} ${
        n === 1 ? 'decision is' : 'decisions are'
      } waiting for confirmation`,
    noUnconfirmedDecisions: 'all decisions confirmed',
    unreadDecision: 'unread decision',
    pageLoadError: 'Error in fetching the requested information',
    financeDecisions: {
      type: {
        FEE_DECISION: 'Fee decision',
        VOUCHER_VALUE_DECISION: 'Voucher value decision'
      },
      title: 'Decisions regarding payments',
      liableCitizens: 'Persons liable for payment',
      validityPeriod: 'Decision valid from',
      sentAt: 'Decision sent',
      voucherValueChild: 'Affected child',
      loadDecisionPDF: 'Show decision'
    },
    applicationDecisions: {
      decision: 'Decision of',
      type: {
        CLUB: 'club',
        DAYCARE: 'early childhood education',
        DAYCARE_PART_TIME: 'part-day early childhood education',
        PRESCHOOL: 'pre-primary education',
        PRESCHOOL_DAYCARE:
          'early childhood education related to pre-primary education',
        PRESCHOOL_CLUB: 'esiopetuksen kerhosta (en)',
        PREPARATORY_EDUCATION: 'preparatory education'
      },
      childName: "Child's name",
      unit: 'Unit',
      period: 'Time period',
      sentDate: 'Decision made',
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
          'NOTE! You are able to accept/reject the related early childhood education decision if you accept the pre-primary / preparatory education decision first.'
      },
      openPdf: 'Show the decision',
      warnings: {
        decisionWithNoResponseWarning: {
          title: 'There is a decision without a response',
          text: 'There is a decision without a response. Are you sure you want to return to the list without responding?',
          resolveLabel: 'Return without responding',
          rejectLabel: 'Continue responding'
        },
        doubleRejectWarning: {
          title: 'Are you sure you want to reject the placement?',
          text: 'You are rejecting an offer on pre-primary / preparatory education placement. The related early childhood education placement will also be rejected.',
          resolveLabel: 'Reject both',
          rejectLabel: 'Cancel'
        }
      },
      errors: {
        pageLoadError: 'Error in fetching the requested information',
        submitFailure: 'Error in submitting the response'
      },
      returnToPreviousPage: 'Return'
    },
    assistancePreschoolDecisions: {
      // the actual decision cannot be in English
      ...fi.decisions.assistancePreschoolDecisions,
      title: 'Decision of special support in pre-primary education',
      statuses: {
        DRAFT: 'Draft',
        NEEDS_WORK: 'Needs work',
        ACCEPTED: 'Accepted',
        REJECTED: 'Rejected',
        ANNULLED: 'Annulled'
      },
      annulmentReason: 'Reason for the annulment',
      pageTitle: 'Decision of support in pre-primary education',
      type: 'Special support status',
      types: {
        NEW: 'Special support begins',
        CONTINUING: 'Special support continues',
        TERMINATED: 'Special support ends'
      }
    },
    assistanceDecisions: {
      title: 'Support decision',
      assistanceLevel: 'Level of support',
      validityPeriod: 'Valid',
      unit: 'Unit',
      decisionMade: 'Decision made',
      level: {
        ASSISTANCE_ENDS: 'Special/enhanced assistance ends',
        ASSISTANCE_SERVICES_FOR_TIME:
          'Assistance services during validity of decision',
        ENHANCED_ASSISTANCE: 'Enhanced assistance',
        SPECIAL_ASSISTANCE: 'Special assistance'
      },
      statusLabel: 'Status',
      openDecision: 'Show decision',
      // the actual decision cannot be in English
      decision: {
        ...fi.decisions.assistanceDecisions.decision,
        statuses: {
          DRAFT: 'Draft',
          NEEDS_WORK: 'Needs work',
          ACCEPTED: 'Accepted',
          REJECTED: 'Rejected',
          ANNULLED: 'Annulled'
        },
        assistanceLevel: {
          assistanceEnds: 'Special/enhanced assistance ends',
          assistanceServicesForTime:
            'Assistance services during validity of decision',
          enhancedAssistance: 'Enhanced assistance',
          specialAssistance: 'Special assistance'
        }
      }
    }
  },
  applicationsList: {
    title:
      'Applying for early childhood education or a club and enrolling for pre-primary education',
    summary: (
      <P width="800px">
        A child’s guardian can submit an application for early childhood
        education or a club and enroll the child to pre-primary education. You
        can also use the same application to apply for a service voucher for
        early childhood education. To do this, you have to apply for a place in
        early childhood education at a service voucher unit. Information on the
        guardian’s children is automatically retrieved from the Digital and
        Population Data Services Agency and displayed in this view.
      </P>
    ),
    pageLoadError: 'Failed to load guardian applications',
    type: {
      DAYCARE: 'Application for early childhood education',
      PRESCHOOL: 'Pre-primary education application',
      CLUB: 'Club application'
    },
    transferApplication: 'Transfer application',
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
    newApplicationLink: 'New application',
    namelessChild: 'Nameless child'
  },
  fileDownload: {
    download: 'Download'
  },
  personalDetails: {
    title: 'Personal information',
    description: (
      <P>
        Here you can check and update your personal and contact information.
        Your name and address are retrieved from the Population Information
        System. If they change, you will need to inform the Digital and
        Population Data Services Agency (DVV).
      </P>
    ),
    detailsSection: {
      noEmailAlert:
        'Your email address is missing. Please fill it down below to receive notifications sent by eVaka.',
      noPhoneAlert: 'Your phone number is missing. Please fill it down below.',
      title: 'Personal information',
      name: 'Name',
      preferredName: 'Preferred first name',
      contactInfo: 'Contact information',
      address: 'Address',
      phone: 'Phone number',
      backupPhone: 'Additional phone number',
      backupPhonePlaceholder: 'Eg. work phone number',
      email: 'Email',
      emailMissing: 'Email missing',
      phoneMissing: 'Phone number missing',
      noEmail: 'I have no email address',
      emailInfo:
        'Email is required to receive notifications about new messages, attendance reservations and other matters concerning your child’s early childhood education.'
    },
    loginDetailsSection: {
      title: 'Login information',
      keycloakEmail: 'Username'
    },
    notificationsSection: {
      title: 'Email notifications',
      info: 'You can choose to receive email notifications on the following topics. You can edit the settings by clicking on the Edit button',
      subtitle: 'Email notifications',
      message: 'messages sent to eVaka by staff',
      bulletin: 'bulletins sent to eVaka',
      outdatedIncome: 'Reminders about updating your income information',
      outdatedIncomeInfo:
        'If your family pays less than the maximum fee for early childhood education, you must update your income information regularly. If your income information is missing or expires, you will be charged the maximum fee for early childhood education.',
      outdatedIncomeWarning:
        'If your income information is missing or expires, you will be charged the maximum fee for early childhood education.',
      calendarEvent: 'Reminders about new events marked in the calendar',
      decision: 'New decisions',
      document: 'New documents',
      documentInfo:
        'Documents refer to official documents that are not decisions. These include early childhood education plans and pedagogic assessments.',
      informalDocument: "Other documents related to the child's everyday life",
      informalDocumentInfo:
        'These documents may include, for example, images of drawings made by the child.',
      missingAttendanceReservation:
        'Reminders of missing attendance reservations',
      missingAttendanceReservationInfo:
        'A reminder will be sent before the deadline if you have not registered your child’s attendance/absence for the following two weeks.'
    }
  },
  income: {
    title: 'Income information',
    description: (
      <>
        <p>
          On this page, you can submit statements on your and your
          children&apos;s earnings that affect the early childhood education
          fee. You can also view, edit, or delete income statements that you
          have submitted until the authority has processed the information.
          After the form has been processed, you can update your income
          information by submitting a new form.
        </p>
        <p>
          The client fees for municipal early childhood education are determined
          as a percentage of the family’s gross income. The fees vary according
          to family size, income and time in early childhood education. Check
          the table in the customer bulletin (found{' '}
          <a
            target="_blank"
            rel="noreferrer"
            href="https://www.espoo.fi/en/childcare-and-education/early-childhood-education/early-childhood-education-fees"
          >
            here
          </a>
          ) to see if you need to submit an income statement or if your family
          belongs to the category of the highest early childhood education fee.
        </p>
        <p>
          More information on the fees:{' '}
          <a href="https://www.espoo.fi/en/childcare-and-education/early-childhood-education/early-childhood-education-fees">
            Early childhood education fees
          </a>
        </p>
      </>
    ),
    formTitle: 'Reporting income information',
    formDescription: (
      <>
        <P>
          The income statement and its attachments must be submitted within two
          weeks of the beginning of early childhood education. In case of
          incomplete income information, the fee may be set at the highest fee.
        </P>
        <P>
          The client fee is charged from the first day of early education in
          accordance with the decision.
        </P>
        <P>
          The client must immediately inform the client fee unit of changes in
          income and family size. If necessary, the authority is also entitled
          to collect early childhood education fees retrospectively.
        </P>
        <P>
          <strong>To be noted:</strong>
        </P>
        <Gap size="xs" />
        <UnorderedList>
          <li>
            The fee can be reviewed during the operating year if there are
            changes in the family&apos;s situation, income, or the child&apos;s
            service needs. The fee can also be changed in accordance with
            changes in legislation or decisions made by the city council.
          </li>
          <li>
            If there are significant changes (+/-10%) in the family&apos;s
            income, a new income statement must be made immediately.
          </li>
        </UnorderedList>
        <P>
          See current income thresholds{' '}
          <a
            target="_blank"
            rel="noreferrer"
            href="https://www.espoo.fi/en/childcare-and-education/early-childhood-education/early-childhood-education-fees"
          >
            here
          </a>
          .
        </P>
        <P>* The information denoted with an asterisk is mandatory.</P>
      </>
    ),
    childFormTitle: "Child's income information",
    childFormDescription: (
      <>
        <P>
          The income of a child in early childhood education should be
          clarified, as the income affects the early childhood education fee.
          Child income these may include, but are not limited to, maintenance or
          support, interest and dividend income, and a pension.
        </P>

        <P>
          If your child has no income, or your family has agreed to the highest
          early childhood fee, you should not fill this form.
        </P>

        <P>* Information marked with an asterisk is required.</P>
      </>
    ),
    confidential: (
      <P>
        <strong>Confidential</strong>
        <br />
        (Act on the Openness of Government Activities, Section 24(1)(23))
      </P>
    ),
    addNew: 'New income statement',
    incomeInfo: 'Income information',
    incomeInstructions:
      'Please submit an income statement after your child has got the place in early childhood education.',
    childIncomeInfo: "Validity of the child's income information",
    incomeStatementMissing:
      'If your child has income, report it with an income statement.',
    incomesRegisterConsent:
      'I agree that information related to my income will be checked from the Incomes Register and Kela if necessary',
    incomeType: {
      description: (
        <>
          If you are an entrepreneur but also have other income, choose both{' '}
          <strong>Entrepreneur&apos;s income information</strong>, and{' '}
          <strong>Determination of the client fee by gross income</strong>.
        </>
      ),
      startDate: 'Valid as of',
      endDate: 'Valid until',
      title: 'Grounds for the client fee',
      agreeToHighestFee: 'I agree to the highest early education fee',
      highestFeeInfo:
        'I agree to pay the highest early education fee in accordance with the early education time, the valid Act on Client Charges in Healthcare and Social Welfare and the decisions of the City Board for the time being until I declare otherwise or until the early education of my child ends. (No need to provide income information)',
      grossIncome: 'Determination of the client fee by gross income',
      entrepreneurIncome: "Entrepreneur's income information"
    },
    childIncome: {
      childAttachments: 'Child income information attached *',
      additionalInfo: "Learn more about your child's income information",
      write: 'Write'
    },
    grossIncome: {
      title: 'Filling in gross income data',
      description: (
        <>
          <P noMargin>
            Select below whether you want to submit your income information as
            attachments, or whether the authority will check your information
            directly from the Incomes Register and Kela, if necessary.
          </P>
          <P>
            If you have started or will start at a new job, always submit the
            contract of employment as an attachment, because there is a delay
            updating the Incomes Register.
          </P>
        </>
      ),
      incomeSource: 'Reporting income information',
      provideAttachments:
        'I will submit the information as attachments, and my information can be checked from Kela if necessary',
      attachmentsVerificationInfo: 'Income information is verified annually.',
      estimate: 'Estimate of my gross income',
      estimatedMonthlyIncome:
        'Average earnings including holiday bonus, €/month',
      otherIncome: 'Other income',
      otherIncomeDescription:
        'If you have other income, it must be itemised in an attachment. A list of the required attachments can be found at the bottom of the form under: Attachments related to income and early childhood education fees.',
      choosePlaceholder: 'Select',
      otherIncomeTypes: {
        PENSION: 'Pension',
        ADULT_EDUCATION_ALLOWANCE: 'Adult education allowance',
        SICKNESS_ALLOWANCE: 'Sickness benefit',
        PARENTAL_ALLOWANCE: 'Maternity and parental allowance',
        HOME_CARE_ALLOWANCE: 'Child home care allowance',
        FLEXIBLE_AND_PARTIAL_HOME_CARE_ALLOWANCE:
          'Flexible or partial care allowance',
        ALIMONY: 'Maintenance allowance/support',
        INTEREST_AND_INVESTMENT_INCOME: 'Income from interest and dividends',
        RENTAL_INCOME: 'Rental income',
        UNEMPLOYMENT_ALLOWANCE: 'Unemployment benefit',
        LABOUR_MARKET_SUBSIDY: 'Labour market subsidy',
        ADJUSTED_DAILY_ALLOWANCE: 'Adjusted unemployment benefit',
        JOB_ALTERNATION_COMPENSATION: 'Job alternation leave',
        REWARD_OR_BONUS: 'Fee or bonus',
        RELATIVE_CARE_SUPPORT: 'Informal care allowance',
        BASIC_INCOME: 'Guaranteed minimum income',
        FOREST_INCOME: 'Forest income',
        FAMILY_CARE_COMPENSATION: 'Fees received for family care',
        REHABILITATION: 'Rehabilitation allowance or grant',
        EDUCATION_ALLOWANCE: 'Training allowance',
        GRANT: 'Grant/scholarship',
        APPRENTICESHIP_SALARY: 'Earnings from apprenticeship training',
        ACCIDENT_INSURANCE_COMPENSATION: 'Compensation from accident insurance',
        OTHER_INCOME: 'Other income'
      },
      otherIncomeInfoLabel: 'Estimate of other income',
      otherIncomeInfoDescription:
        'Estimate of other income sources per month, eg. "Rent 150, child home care allowance 300"'
    },
    entrepreneurIncome: {
      title: "Filling in the entrepreneur's income information",
      description: (
        <>
          If necessary, you can fill in the information for more than one
          company by ticking the boxes that apply to all of your companies.
          Please provide more detailed company-specific information as
          attachments.
          <br />A list of the required attachments can be found at the bottom of
          the form under &quot;Attachments related to income and early childhood
          education fees&quot;.
        </>
      ),
      fullTimeLabel: 'Are the business activities full-time or part-time?',
      fullTime: 'Full-time',
      partTime: 'Part-time',
      startOfEntrepreneurship: 'Business activities started',
      spouseWorksInCompany: 'Does your spouse work for your company?',
      yes: 'Yes',
      no: 'No',
      startupGrantLabel: 'Has your company received a start-up grant?',
      startupGrant:
        'My company received a start-up grant. I will submit the start-up grant decision as an attachment.',
      checkupLabel: 'Checking of information',
      checkupConsent:
        'I agree that information related to my income will be checked from the Incomes Register and Kela if necessary.',
      companyInfo: 'Company details',
      companyForm: 'Company type',
      selfEmployed: 'Proprietorship',
      limitedCompany: 'Limited liability company',
      partnership: 'General or limited partnership',
      lightEntrepreneur: 'Light entrepreneurship',
      lightEntrepreneurInfo:
        'Proof of payment of salary and remuneration must be submitted as attachments.',
      partnershipInfo:
        'The profit and loss account, balance sheet and the accountant’s report on salary and fringe benefits must be submitted as attachments.'
    },
    selfEmployed: {
      info: 'If the business activities have continued for more than 3 months, the company’s most recent profit and loss account or tax decision must be submitted.',
      attachments:
        'I will submit the company’s most recent profit and loss account and balance sheet statement or tax decision as attachments.',
      estimatedIncome:
        'I will fill out an estimate of my average monthly earnings.',
      estimatedMonthlyIncome: 'Average earnings €/month',
      timeRange: 'Time range'
    },
    limitedCompany: {
      info: (
        <>
          <strong>
            Documents of dividend income must be submitted as an attachment.
          </strong>{' '}
          Select the appropriate method to provide other information below.
        </>
      ),
      incomesRegister:
        'My income can be checked directly from Kela and the Incomes Register.',
      attachments:
        'I will provide documents of my income as an attachment, and I agree that information related to my income will be checked from Kela.'
    },
    accounting: {
      title: 'Contact information of the accountant',
      description:
        'The contact information of the accountant is required if you are involved in a limited company, limited partnership or general partnership.',
      accountant: 'Accountant',
      accountantPlaceholder: 'Name of the accountant/company',
      email: 'E-mail address',
      emailPlaceholder: 'E-mail',
      address: 'Postal address',
      addressPlaceholder: 'Street address, postal code, city/town',
      phone: 'Telephone number',
      phonePlaceholder: 'Telephone'
    },
    moreInfo: {
      title: 'Other information related to the fee',
      studentLabel: 'Are you a student?',
      student: 'I am a student',
      studentInfo:
        'Students must submit a certificate of student status from the educational institute or a decision on a student benefit from an unemployment fund/training allowance from an employment fund.',
      deductions: 'Deductions',
      alimony:
        'I pay child support. I will provide a copy of proof of payment as an attachment.',
      otherInfoLabel: 'More information about income information'
    },
    attachments: {
      title: 'Attachments related to income and early childhood education fees',
      description:
        'Here, you can electronically send the attachments related to your income or early childhood education fees, such as pay slips or Kela’s certificate of support for private care. Note! Income-related attachments are usually not required if your family has agreed to pay the highest fee.',
      required: {
        title: 'Necessary attachments'
      },
      attachmentNames: {
        PENSION: 'Decision on pension',
        ADULT_EDUCATION_ALLOWANCE: 'Decision on adult education allowance',
        SICKNESS_ALLOWANCE: 'Decision on sickness benefit',
        PARENTAL_ALLOWANCE: 'Decision on maternity or parental allowance',
        HOME_CARE_ALLOWANCE: 'Decision on child home care allowance',
        FLEXIBLE_AND_PARTIAL_HOME_CARE_ALLOWANCE: 'Decision on care allowance',
        ALIMONY:
          'Child maintenance agreement or decision on maintenance allowance',
        UNEMPLOYMENT_ALLOWANCE: 'Decision on unemployment benefit',
        LABOUR_MARKET_SUBSIDY: 'Decision on labour market subsidy',
        ADJUSTED_DAILY_ALLOWANCE: 'Decision on the daily allowance',
        JOB_ALTERNATION_COMPENSATION:
          'Document of job alternation leave payment',
        REWARD_OR_BONUS: 'New pay statement or pay slip with bonus',
        RELATIVE_CARE_SUPPORT: 'Decision on informal care allowance',
        BASIC_INCOME: 'Decision on guaranteed minimum income',
        FOREST_INCOME: 'Document of forest income',
        FAMILY_CARE_COMPENSATION: 'Documents of family care remuneration',
        REHABILITATION:
          'Decision on rehabilitation allowance or rehabilitation grant',
        EDUCATION_ALLOWANCE: 'Decision on training allowance',
        GRANT: 'Document of grant/scholarship',
        APPRENTICESHIP_SALARY:
          'Document of earnings from apprenticeship training',
        ACCIDENT_INSURANCE_COMPENSATION:
          'Document of compensation for accident insurance',
        OTHER_INCOME: 'Attachments on other income',
        ALIMONY_PAYOUT: 'Proof of payment of child support',
        INTEREST_AND_INVESTMENT_INCOME:
          'Documents of interest and dividend income',
        RENTAL_INCOME: 'Documents of rental income',
        PAYSLIP: 'Last pay slip',
        STARTUP_GRANT: 'Decision on a start-up grant',
        ACCOUNTANT_REPORT:
          'Accountant’s account of fringe benefits and dividends',
        ACCOUNTANT_REPORT_LLC:
          'Accountant’s account of fringe benefits and dividends',
        PROFIT_AND_LOSS_STATEMENT: 'Profit and loss account and balance sheet',
        SALARY: 'Payslips of salaries and trade incomes',
        PROOF_OF_STUDIES:
          'Certificate of student status or a decision on a student benefit from an unemployment fund / training allowance from an employment fund',
        CHILD_INCOME: 'Documents of child income'
      }
    },
    assure: <>I testify that the information I have provided is correct. *</>,
    errors: {
      invalidForm:
        'The form is missing some required information or the information is incorrect. Please review the information you filled in.',
      choose: 'Please select an option',
      chooseAtLeastOne: 'Select at least one option',
      deleteFailed: 'The income statement could not be deleted'
    },
    table: {
      title: 'Income statements',
      incomeStatementForm: 'Income statement form',
      startDate: 'Valid as of',
      endDate: 'Valid until',
      handled: 'Processed',
      openIncomeStatement: 'Open form',
      deleteConfirm: 'Do you want to delete the income statement?',
      deleteDescription:
        'Are you sure you want to delete the income statement you submitted? All information on the deleted form will be lost.'
    },
    view: {
      title: 'Income statement form',
      startDate: 'Valid as of',
      feeBasis: 'Grounds for the client fee',

      grossTitle: 'Gross income',
      incomeSource: 'Submission of information',
      incomesRegister:
        'I agree that information related to my income will be checked from Kela and the Incomes Register.',
      attachmentsAndKela:
        'I will submit the information as attachments, and my information can be checked from Kela',
      grossEstimatedIncome: 'Estimate of gross income',
      otherIncome: 'Other income',
      otherIncomeInfo: 'Estimate of other income',

      entrepreneurTitle: "Entrepreneur's income information",
      fullTimeLabel: 'Are the business activities full-time or part-time',
      fullTime: 'Full-time',
      partTime: 'Part-time',
      startOfEntrepreneurship: 'Business activities started',
      spouseWorksInCompany: 'Does your spouse work for your company',
      startupGrant: 'Start-up grant',
      checkupConsentLabel: 'Checking of information',
      checkupConsent:
        'I agree that information related to my income will be checked from the Incomes Register and Kela if necessary.',
      companyInfoTitle: 'Company details',
      companyType: 'Company type',
      selfEmployed: 'Proprietorship',
      selfEmployedAttachments:
        "I will submit the company's most recent profit and loss account and balance sheet statement or tax decision as attachments.",
      selfEmployedEstimation: 'Estimated average monthly earnings',
      limitedCompany: 'Limited liability company',
      limitedCompanyIncomesRegister:
        'I agree that information related to my income will be checked from the Incomes Register and Kela if necessary.',
      limitedCompanyAttachments:
        'I will submit the information as attachments, and my information can be checked from Kela if necessary.',
      partnership: 'General or limited partnership',
      lightEntrepreneur: 'Light entrepreneurship',
      attachments: 'Attachments',

      estimatedMonthlyIncome: 'Average earnings €/month',
      timeRange: 'Time range',

      accountantTitle: 'Information of the accountant',
      accountant: 'Accountant',
      email: 'E-mail address',
      phone: 'Telephone number',
      address: 'Postal address',

      otherInfoTitle: 'Other information related to income',
      student: 'Student',
      alimonyPayer: 'Pays child support',
      otherInfo: 'More information related to income information',

      citizenAttachments: {
        title:
          'Attachments related to income and early childhood education fees',
        noAttachments: 'No attachments'
      },

      employeeAttachments: {
        title: 'Add attachments',
        description:
          'Here, you can add attachments provided by the client in paper format to an income statement submitted via eVaka.'
      },

      statementTypes: {
        HIGHEST_FEE: 'Consent for the highest fee category',
        INCOME: 'Income information provided by parent/guardian',
        CHILD_INCOME: 'Child income'
      }
    },
    children: {
      title: 'Children income information',
      description: (
        <>
          The income of children in early childhood education should be
          clarified. The most common income of a child is maintenance or
          support, interest and dividend income and pension.
        </>
      ),
      noChildIncomeStatementsNeeded: (
        <>You currently have no children to report income.</>
      )
    }
  },
  validationErrors: {
    ...components.validationErrors,
    ...components.datePicker.validationErrors,
    outsideUnitOperationTime: 'Outside opening hours'
  },
  placement: {
    // TODO i18n
    type: {
      CLUB: 'Kerho',
      DAYCARE: 'Varhaiskasvatus',
      FIVE_YEARS_OLD_DAYCARE: '5-vuotiaiden varhaiskasvatus',
      PRESCHOOL_WITH_DAYCARE: 'Esiopetus ja liittyvä varhaiskasvatus',
      PREPARATORY_WITH_DAYCARE: 'Valmistava opetus ja liittyvä varhaiskasvatus',
      DAYCARE_PART_TIME: 'Osapäiväinen varhaiskasvatus',
      DAYCARE_FIVE_YEAR_OLDS: '5-vuotiaiden varhaiskasvatus',
      DAYCARE_PART_TIME_FIVE_YEAR_OLDS:
        '5-vuotiaiden osapäiväinen varhaiskasvatus',
      PRESCHOOL: 'Esiopetus',
      PREPARATORY: 'Valmistava opetus',
      PREPARATORY_DAYCARE: 'Valmistava opetus ja liittyvä varhaiskasvatus',
      PREPARATORY_DAYCARE_ONLY:
        'Valmistavan opetuksen liittyvä varhaiskasvatus',
      PRESCHOOL_DAYCARE: 'Esiopetus ja liittyvä varhaiskasvatus',
      PRESCHOOL_DAYCARE_ONLY: 'Esiopetuksen liittyvä varhaiskasvatus',
      PRESCHOOL_CLUB: 'Esiopetuksen kerho (en)',
      TEMPORARY_DAYCARE: 'Tilapäinen kokopäiväinen varhaiskasvatus',
      TEMPORARY_DAYCARE_PART_DAY: 'Tilapäinen osapäiväinen varhaiskasvatus',
      SCHOOL_SHIFT_CARE: 'Koululaisten vuorohoito'
    }
  },
  children: {
    title: 'Children',
    pageDescription:
      "General information related to your children's early childhood education or pre-primary education is displayed on this page.",
    noChildren: 'No children',
    unreadCount: 'unread',
    childPicture: 'Picture of the child',
    placementTermination: {
      title: 'Terminating early childhood education agreement',
      description:
        "When terminating your child's early childhood education agreement, please note that any transfer applications will be automatically withdrawn after your child's last day of attendance. If you later need early childhood education for your child, you must submit a new application.",
      terminatedPlacements:
        "You have terminated your child's early childhood education agreement",
      until: (date: string) => `valid until ${date}`,
      choosePlacement:
        'Select the early childhood education agreement that you want to terminate',
      invoicedDaycare: 'Paid early childhood education',
      nonTerminatablePlacement:
        "The agreement cannot be terminated online. Please contact your unit's supervisor.",
      lastDayInfo:
        "The last day when your child needs early childhood education. Your child's early childhood education agreement will be terminated to end on this day.",
      lastDayOfPresence: 'Last day of attendance',
      confirmQuestion:
        "Do you want to terminate your child's early childhood education agreement?",
      confirmDescription: (date: string) =>
        `Are you sure you want to terminate the agreement so that your child's last day of attendance is ${date}? Once completed, you cannot cancel the termination.`,
      terminate: 'Terminate the agreement'
    },
    pedagogicalDocuments: {
      title: 'Growth and learning',
      noDocuments: 'No documents',
      table: {
        date: 'Date',
        child: 'Child',
        document: 'Document',
        description: 'Description'
      },
      readMore: 'Read more',
      collapseReadMore: 'Show less',
      nextPage: 'Next page',
      previousPage: 'Previous page',
      pageCount: (current: number, total: number) =>
        `Page ${current} of ${total}`
    },
    serviceNeedAndDailyServiceTime: {
      title: 'Service need',
      titleWithDailyServiceTime: 'Service need and daily service time'
    },
    serviceNeed: {
      title: 'Service need',
      validity: 'Validity',
      description: 'Description',
      unit: 'Unit',
      status: 'Status',
      empty: 'Service need is not defined'
    },
    attendanceSummary: {
      title: 'Attendances',
      attendanceDays: 'Contract days',
      warning: 'The number of contract days per month has been exceeded.',
      empty: 'No contract days in the selected period'
    },
    dailyServiceTime: {
      title: 'Daily service time',
      validity: 'Validity',
      description: 'Description',
      status: 'Status',
      variableTime: 'Daily time varies',
      empty: 'Not defined'
    },
    vasu: {
      title: 'Pedagogical documents',
      plansTitle:
        'Early childhood education and care plans and learning plans for pre-primary education',
      noVasus: 'No plans',
      hojksTitle: 'Individual education plans',
      otherDocumentsTitle: 'Other documents',
      noDocuments: 'No documents',
      lastModified: 'Last modified',
      lastPublished: 'Last published',
      leavePage: 'Leave page',
      edited: 'Edited',
      eventTypes: {
        PUBLISHED: 'Published',
        MOVED_TO_READY: 'Move to ready',
        RETURNED_TO_READY: 'Returned to ready',
        MOVED_TO_REVIEWED: 'Moved to reviewed',
        RETURNED_TO_REVIEWED: 'Returned to reviewed',
        MOVED_TO_CLOSED: 'Moved to closed'
      },
      states: {
        DRAFT: 'Draft',
        READY: 'Ready',
        REVIEWED: 'Reviewed',
        CLOSED: 'Closed'
      },
      state: 'State',
      events: {
        DAYCARE: 'Early childhood plan events',
        PRESCHOOL: 'Pre-primary plan events'
      },
      confidential: 'Confidential',
      noRecord: 'No record',
      givePermissionToShareInfoVasu:
        'If necessary, some information from the plan will be transferred to other recipients (Section 41 of the Act on Early Childhood Education and Care, section 41 of the Basic Education Act)',
      givePermissionToShareInfoVasuInfoText:
        "Contact the unit for early childhood education if you want to change the recipients mentioned in the plan. The plan may be handed over to a new provider of early childhood education, pre-primary education or basic education even without the guardian's permission, if it is necessary for arranging early childhood education, pre-primary or primary education (Section 41 of the Act on Early Childhood Education and Care, Section 41 of the Basic Education Act).",
      givePermissionToShareTitleVasu:
        'Confirmation of the transfer of the early childhood education plan to other recipients',
      givePermissionToShareTitleLeops:
        'Confirmation of the transfer of the pre-primary education learning plan to other recipients',
      givePermissionToShareVasu:
        "I confirm that the child's early childhood education plan may be handed over to the parties mentioned in the “Recipients” section.",
      givePermissionToShareLeops:
        "I confirm that the child's pre-primary education learning plan may be handed over to the parties mentioned in the “Information Recipients” section.",
      givePermissionToShareInfoBase:
        "If you want to change the recipients mentioned in the plan, contact the staff of your child's early childhood education unit.",
      givePermissionToShareReminder:
        'Confirm the recipients to whom the plan may be disclosed',
      sharingLeopsDisclaimer:
        "When a child changes to another early childhood education unit in the City of Espoo, the early childhood education plan and other early childhood education documents are transferred to the new unit (also applies to early childhood education organized by Espoo as a purchasing service). The guardian will be informed about the transfer in advance. We ask the guardian for permission to hand over the early childhood education plan to an outsider. However, if a child enters private or other municipality's early childhood education, the early childhood education plan must be submitted to the new early childhood education provider even without permission, if the information is necessary for arranging the child's early childhood education (section 41 (3) of the Early Childhood Education Act). In addition, the early childhood education plan must be submitted to the organizer of pre-primary or primary education if it is necessary for the organization of the child's education (section 41 (4) of the Basic Education Act). The guardian will be informed of the transfer in advance.",
      sharingVasuDisclaimer:
        "The pre-primary learning plan may be handed over to a new provider of early childhood education, pre-primary education or basic education even without the guardian's permission, if it is necessary for arranging early childhood education, pre-primary or primary education (Section 4 of the Act on Early Childhood Education and Care, Section 41 of the Basic Education Act).",
      givePermissionToShareVasuBrief:
        'Necessary information will be shared if required to do so. (Section 41 of the Act on Early Childhood Education and Care, section 41 of the Basic Education Act)',
      givePermissionToShareLeopsBrief:
        'Necessary information will be shared if required to do so. (Section 41 of the Act on Early Childhood Education and Care, section 41 of the Basic Education Act)'
    }
  },
  accessibilityStatement: (
    <>
      <H1>Accessibility statement</H1>
      <P>
        This accessibility statement applies to the City of Espoo’s early
        childhood education online service eVaka at{' '}
        <a href="https://espoonvarhaiskasvatus.fi">espoonvarhaiskasvatus.fi</a>.
        The City of Espoo endeavours to ensure the accessibility of the online
        service, continuously improve the user experience and apply appropriate
        accessibility standards.
      </P>
      <P>
        The accessibility of the service was assessed by the development team of
        the service, and this statement was drawn up on 12 April 2022.
      </P>
      <H2>Compliance of the service</H2>
      <P>
        The online service complies with the statutory critical accessibility
        requirements in accordance with Level AA of the Accessibility Guidelines
        for the WCAG v2.1. The service is not yet fully compliant with the
        requirements.
      </P>
      <H2>Measures to support accessibility</H2>
      <P>
        The accessibility of the online service is ensured, among other things,
        by the following measures:
      </P>
      <ul>
        <li>
          Accessibility has been taken into account from the beginning of the
          design phase, for example, when choosing the colours and font sizes of
          the service.
        </li>
        <li>
          The service elements have been defined in consistently in terms of
          semantics.
        </li>
        <li>The service is continuously tested with a screen reader.</li>
        <li>
          Various users test the service and give feedback on its accessibility.
        </li>
        <li>
          When website technology or content changes, its accessibility is
          ensured through constant monitoring.
        </li>
      </ul>
      <P>
        This statement will be updated in conjunction with website changes and
        accessibility evaluations.
      </P>
      <H2>Known accessibility issues</H2>
      <P>
        Users may still encounter some issues on the website. The following
        contains a description of known accessibility issues. If you notice an
        issue on the site that is not listed, please contact us.
      </P>
      <ul>
        <li>
          Date pickers and multi-select dropdowns in the service are not
          optimised to be used with a screen reader.
        </li>
        <li>
          The service’s unit map cannot be navigated using the keyboard/screen
          reader, but the units can be browsed on the list available in the same
          view. The map used in the service is produced by a third party.
        </li>
      </ul>
      <H2>Third parties</H2>
      <P>
        The online service uses the following third party services, the
        accessibility of which we cannot be responsible for.
      </P>
      <ul>
        <li>Keycloak user identification service</li>
        <li>Suomi.fi identification</li>
        <li>Leaflet map service</li>
      </ul>
      <H2>Alternative ways of accessing the service</H2>
      <P>
        <ExternalLink
          href="https://www.espoo.fi/en/city-espoo/customer-service/service-points-and-espoo-info/service-points"
          text="The City of Espoo’s Service Points"
        />{' '}
        provide assistance with using electronic services. Service advisors at
        the Service Points help users who cannot access digital services.
      </P>
      <H2>Give feedback</H2>
      <P>
        If you notice an accessibility gap in our online service, please let us
        know! You can give us feedback using the{' '}
        <ExternalLink
          href="https://easiointi.espoo.fi/eFeedback/en/Feedback/20-S%C3%A4hk%C3%B6iset%20asiointipalvelut"
          text="online form"
        />{' '}
        or by emailing{' '}
        <a href="mailto:evakatuki@espoo.fi">evakatuki@espoo.fi</a>.
      </P>
      <H2>Supervisory authority</H2>
      <P>
        If you notice any accessibility issues on the website, please send us,
        the site administrator, feedback first. It may take us up to 14 days to
        reply. If you are not satisfied with the reply or you do not receive a
        reply within two weeks, you can give feedback to the Regional State
        Administrative Agency for Southern Finland. The website of the Regional
        State Administrative Agency for Southern Finland explains in detail how
        a complaint can be submitted, and how the matter will be processed.
      </P>

      <P>
        <strong>Contact information of the supervisory authority</strong>
        <br />
        Regional State Administrative Agency of Southern Finland
        <br />
        Accessibility Supervision Unit
        <br />
        <ExternalLink
          href="https://www.saavutettavuusvaatimukset.fi"
          text="www.saavutettavuusvaatimukset.fi"
        />
        <br />
        <a href="mailto:saavutettavuus@avi.fi">saavutettavuus@avi.fi</a>
        <br />
        tel. (exchange) 0295 016 000
        <br />
        Open: Mon-Fri 8.00–16.15
      </P>
    </>
  ),
  skipLinks: {
    mainContent: 'Skip to main content',
    applyingSubNav: 'Skip to applications navigation'
  },
  components: componentTranslations
}

export default en
