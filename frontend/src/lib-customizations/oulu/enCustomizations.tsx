{
  /*
SPDX-FileCopyrightText: 2021 City of Oulu

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import React from 'react'

import ExternalLink from 'lib-components/atoms/ExternalLink'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import { H1, H2, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import type { Translations } from 'lib-customizations/citizen'
import type { DeepPartial } from 'lib-customizations/types'

const customerContactText = function () {
  return (
    <>
      {' '}
      p. <a href="tel:+358855845300">08 558 45300 </a> OR{' '}
      <a href="mailto:varhaiskasvatus@ouka.fi">varhaiskasvatus@ouka.fi</a>.
    </>
  )
}

const fi: DeepPartial<Translations> = {
  children: {
    serviceApplication: {
      createInfo:
        "This allows you to propose a change in your child's need for service. The director of the early childhood education unit will accept or reject your proposal. For more information about changing the service need, ask directly at your child's unit. The contract is concluded for at least five months and it can be changed in the middle of the contract period only for a justified reason.",
      openApplicationInfo:
        'A new service need has been proposed for your child. Your proposal has been submitted to the director of the unit for approval.'
    }
  },
  applications: {
    creation: {
      daycareInfo:
        'An application for early childhood education is used to apply for a place in a municipal daycare centre or family daycare. With the same application, you can also apply for an early childhood education service voucher for private early childhood education by selecting the private unit for which you want to apply for the service voucher in the "Application requests" section.',
      preschoolLabel:
        'Enrolment in preschool education and/or applying for early childhood education related to preschool education',
      preschoolInfo:
        'Free of charge preschool education is provided for four (4) hours a day. The preschool year mainly follows the annual holiday schedule used in basic education.',
      preschoolDaycareInfo:
        'You can also apply for early childhood education related to preschool while enrolling in preschool. Early childhood education is provided in the same place as the preschool before and after the preschool education.',
      clubLabel:
        'Application for open early childhood education club activities',
      clubInfo:
        'With the application for open early childhood education and care, you can apply to groups that meet two to three times a week, as well as family groups.',
      applicationInfo: (
        <>
          <P>
            If your child already has a place in early childhood education or
            preschool education in Oulu, submitting a new application will be
            recorded as a transfer application. There is no need to terminate
            the current place.
          </P>
          <P>
            You can make changes to the application until the service
            coordination unit has begun to process it. After this, you can only
            cancel the application or make changes to it by contacting the early
            childhood education service coordination unit.
            {customerContactText()}
          </P>
        </>
      ),
      duplicateWarning:
        'You have already saved a similar, unfinished application for your child. In order to edit the existing application, return to the Applications view or contact the early childhood service coordination unit.',
      transferApplicationInfo: {
        DAYCARE:
          'The child has already been granted a place in early childhood education in Oulu. With this application, you can request a transfer to another early childhood education unit within Oulu.'
      }
    },
    editor: {
      sentInfo: {
        title: 'Your application has been submitted.',
        text: 'You can make changes to your application until it has entered processing at the service coordination unit.',
        ok: 'Okay!'
      },
      unitPreference: {
        siblingBasis: {
          title: 'Application based on a sibling relationship',
          info: {
            DAYCARE: (
              <>
                <P>
                  The child has a sibling basis for the same early childhood
                  education place where their sibling will be when early
                  childhood education begins. The city strives to place all
                  siblings in the same unit, if this is what the parents wish.
                  If you are filling out applications for siblings of who are
                  not yet included within the scope of early childhood education
                  and care, enter the information about the sibling relationship
                  in the Additional information field of the application.
                </P>
                <P>
                  Fill out this section only if you are applying for a place at
                  an early childhood education and care unit on the basis of a
                  sibling relationship. Make sure that you choose the sibling’s
                  current early childhood education and care unit as the
                  preferred option.
                </P>
              </>
            ),
            PRESCHOOL: (
              <>
                <P>
                  A preschool pupil has sibling priority for a local daycare
                  center where the preschool pupil&apos;s sibling has a place at
                  the start of the preschool year.
                </P>
                <P>
                  Fill in this information only if you are using the sibling
                  criterion, and in the search requests below, select the same
                  unit as the childs sibling as the primary wish.
                </P>
              </>
            ),
            CLUB: (
              <>
                <P>
                  The city always strives to place siblings in the same group if
                  the family so wishes.
                </P>
                <P>
                  Fill out this section only if you are applying for a place at
                  an early childhood education and care unit based on a sibling
                  relationship. Make sure that you choose the sibling’s current
                  open early childhood education and care unit as the preferred
                  option.
                </P>
              </>
            )
          }
        },
        units: {
          info: {
            DAYCARE: (
              <>
                <P>
                  You can apply to 1–3 units in the order of preference of your
                  choosing. Preferred choices do not guarantee a place in the
                  desired unit, but the chances of getting the desired place
                  increase by providing multiple options.
                </P>
                <P>
                  You can view the locations of the early childhood education
                  and care units by choosing the option ‘Unit map view’.
                </P>
                <P>
                  You can apply for a service voucher by selecting the private
                  early childhood education and care unit you wish to apply for
                  as the preferred option. If you apply for a service voucher
                  for a private early childhood education and care unit, the
                  head of the unit will also be informed of this.
                </P>
              </>
            ),
            PRESCHOOL: (
              <>
                <P>
                  You can apply to 1–3 units in the order of your choosing.
                  Select the units shown in the preferred units menu, where
                  preschool education is organized in the school year 2025-2026
                </P>
                <P>
                  You can view the unit locations by selecting the option ‘Unit
                  map view’.
                </P>
                <P>
                  The child is assigned a preschool place, taking into account
                  the school path from their own neighborhood. If you wish for
                  your child to be assigned a preschool place in a place other
                  than your own immediate area, justify your wish in the
                  additional information section. A place from another area
                  cannot be promised until the areas own preschoolers have been
                  placed.
                </P>
              </>
            ),
            CLUB: (
              <>
                <P>
                  You can apply to 1–3 units in the order of preference of your
                  choosing. The order of preference does not guarantee a place
                  in the group or club of your choice, but the more options you
                  name, the greater chances you have to receive a place at one
                  of the desired units.
                </P>
                <P>
                  You can view the locations of the open early childhood
                  education and care units by selecting the option ‘Unit map
                  view’.
                </P>
              </>
            )
          },
          serviceVoucherLink:
            'https://www.ouka.fi/oulu/palveluseteli/yksityisen-paivahoidon-palveluseteli'
        }
      },
      heading: {
        title: {
          PRESCHOOL:
            'Enrollment in preschool education and/or applying for early childhood education related to preschool education',
          CLUB: 'Apply for open early childhood education and care'
        },
        info: {
          DAYCARE: (
            <>
              <P>
                Please apply for open early childhood education and care at
                least four months before you require a place for your child. If
                you urgently need early childhood education due to starting work
                or studies, the processing time for the application is two weeks
                upon receipt.
              </P>
              <P>
                The guardian who has made the enrollment will receive a written
                decision about the place via the Suomi.fi Messages service or by
                mail if the Suomi.fi service is not in use. The decision can be
                viewed in eVaka under Menu – Decisions and must be accepted or
                rejected within two weeks.
              </P>
              <P>
                You can find more information about the Suomi.fi Messages
                service and how to activate it at{' '}
                <ExternalLink
                  text="https://www.suomi.fi/viestit"
                  href="https://www.suomi.fi/viestit"
                  newTab
                />
                .
              </P>
              <P $fitted={true}>
                *Fields marked with an asterisk are mandatory.
              </P>
            </>
          ),
          PRESCHOOL: (
            <>
              <P>
                Children attend pre-primary education one year before compulsory
                schooling begins. schooling begins. Pre-primary education is
                free of charge. For the school year 2026–2027, pre-primary
                placements will be assigned to guardians starting January 8,
                2026, based on the home address, without a separate registration
                or application process. If they wish, guardians can submit a
                secondary application between January 19 and January 31, 2026.
                Pre-primary education begins on August 17, 2026.
              </P>
              <P>
                The guardian who has made the enrollment will receive a written
                decision about the place via the Suomi.fi Messages service or by
                mail if the Suomi.fi service is not in use. The decision can be
                viewed in eVaka under Menu – Decisions and must be accepted or
                rejected within two weeks.
              </P>
              <P>
                You can find more information about the Suomi.fi Messages
                service and how to activate it at{' '}
                <ExternalLink
                  text="Suomi.fi messages"
                  href="https://www.suomi.fi/viestit"
                  newTab
                />
                .
              </P>
              <P $fitted={true}>
                *Fields marked with an asterisk are mandatory.
              </P>
            </>
          ),
          CLUB: (
            <>
              <P>
                A place in open early childhood education club activities is
                granted until the place is terminated or the child transitions
                to early childhood education or preschool education.
              </P>
              <P>
                The guardian who has made the enrollment will receive a written
                decision about the place via the Suomi.fi Messages service or by
                mail if the Suomi.fi service is not in use. The decision can be
                viewed in eVaka under Menu – Decisions and must be accepted or
                rejected within two weeks.
              </P>
              <P>
                Open early childhood education activities are provided free of
                charge, and participating in them does not affect the home care
                allowance granted by Kela.
              </P>
              <P>
                You can find more information about open early childhood
                education on the website of the City of Oulu:{' '}
                <ExternalLink
                  text="Open early childhood education – Clubs, groups and play schools"
                  href="https://www.ouka.fi/oulu/paivahoito-ja-esiopetus/avoin-varhaiskasvatus"
                  newTab
                />
              </P>
              <P $fitted={true}>
                *Fields marked with an asterisk are mandatory.
              </P>
            </>
          )
        }
      },
      serviceNeed: {
        startDate: {
          header: {
            CLUB: 'When does open early childhood education begin?'
          },
          info: {
            PRESCHOOL: [
              'The start date for preschool term varies slightly each year. Below is information on the dates for the current and upcoming school years. If you need early childhood education and care related to preschool education, apply for it under "Early childhood education and care related to preschool education".'
            ],
            CLUB: [
              'Clubs and groups organized as a part of open early childhood education and care generally follow the hours and annual holiday schedule used in preschool education. The child may attend one group that meets two or three times a week and a family group at the same time.'
            ]
          },
          label: {
            PRESCHOOL: 'Desired start date'
          },
          instructions: {
            DAYCARE: (
              <>
                It is possible to postpone the desired start date until the
                early childhood education guidance team has taken the
                application into processing. To advance the desired start date
                or make changes to an application that is being processed,
                please contact the early childhood education guidance team
                {customerContactText()}
              </>
            ),
            PRESCHOOL: (
              <>
                It is possible to postpone the desired start date until the
                early childhood education guidance team has taken the
                application into processing. To advance the desired start date
                or make changes to an application that is being processed,
                please contact the early childhood education guidance team
                {customerContactText()}
              </>
            ),
            CLUB: null
          }
        },
        clubDetails: {
          wasOnDaycare:
            'The child already has a place at an early childhood education and care unit, which they will give up upon receiving the right to attend a club or group.',
          wasOnDaycareInfo:
            'If a child has been attending a municipal day care center or family day care center and will give up their place once they start attending the club or group, the child is more likely to receive a place in a club or group.',
          wasOnClubCareInfo:
            'If a child has already been attending the club or group during the previous term, they will have a better chance of being granted a place in the club or group in the coming term as well.'
        },
        urgent: {
          attachmentsMessage: {
            text: (
              <P $fitted={true}>
                If your child requires early childhood education and care due to
                a recent change in your student or employment status, you must
                send the application no later than two weeks before your child
                should start at the ECEC unit. A statement on the employment or
                student status of both guardians living in the same household
                must be attached to the application. The processing time of two
                weeks starts upon receipt of the application and the mandatory
                attachments.
              </P>
            ),
            subtitle:
              'Add a certificate of employment or a student certificate from both guardians.'
          }
        },
        shiftCare: {
          label: 'Evening and shift care',
          instructions:
            'Evening and shift care mainly refers to early childhood education and care provided at times other than from 6 a.m. to 6 p.m., during the weekends, or around the clock. If you need evening or shift care, please specify your care needs in the Additional information field of the application.',
          attachmentsMessage: {
            DAYCARE: (
              <P>
                Evening and shift care is intended for children whose parents
                are both employed in shift work or receive education mainly in
                the evenings and/or at weekends. A statement on the nature of
                the employment relationship or educational activities taking
                place during the evenings and/or weekends for both guardians
                living in the same household must be attached to the
                application.
              </P>
            ),
            PRESCHOOL: (
              <>
                <P>
                  Evening and shift care is intended for children whose parents
                  are both employed in shift work or receive education mainly in
                  the evenings and/or at weekends. A statement on the nature of
                  the employment relationship or educational activities taking
                  place during the evenings and/or weekends for both guardians
                  living in the same household must be attached to the
                  application.
                </P>
              </>
            )
          },
          attachmentsSubtitle:
            'Add a statement regarding the nature of the employment relationship and/or educational activities taking place during the evenings and/or weekends for both guardians here.'
        },
        assistanceNeedInstructions: {
          DAYCARE:
            "Select this option on the application if your child needs support for their development and/or learning in early childhood education. Support measures will be implemented in the child's daily life as part of other early childhood education activities. If necessary, a special education teacher in early childhood education will contact the applicant to ensure the child's support needs are considered when granting an early childhood education place. Please also indicate the need for support if this is a transfer application.",
          CLUB: "Select this option on the application if your child needs support for their development and/or learning in open early childhood education. Support measures will be implemented in the child's daily life as part of other open early childhood education activities. If necessary, a special education teacher in early childhood education will contact the applicant to ensure the child's support needs are considered when granting an open early childhood education place.",
          PRESCHOOL:
            "Select this section of the application if your child needs support for his/hers development and/or learning during their year in preschool education. Support is implemented in the child's daily life as part of other preschool education and early childhood education and care activities. The special education teacher in early childhood education will contact the applicant if necessary to ensure the child's support needs are considered when assigning a preschool education place. Please also indicate the need for support if this is a transfer application."
        },
        partTime: {
          true: 'Part-time',
          false: 'Full-time'
        },
        dailyTime: {
          label: {
            DAYCARE: 'Service need options',
            PRESCHOOL:
              'Early childhood education need related to preschool education'
          },
          connectedDaycare:
            'I also apply for early childhood education and care related to preschool education.',
          connectedDaycareInfo: (
            <>
              <P>
                You can apply for charged early childhood education related to
                preschool education if needed. If your child needs early
                childhood education in August before the start of preschool
                education, please fill out a separate early childhood education
                application for this period.
              </P>
              <P>
                You will receive a written decision regarding your child’s
                access to early childhood education and care{' '}
                <a
                  href="https://www.suomi.fi/viestit"
                  target="_blank"
                  rel="noreferrer"
                >
                  either as a message in the Suomi.fi
                </a>{' '}
                service, or, if you have not yet started using the service, by
                mail. The decision can also be found in the Applications –
                Decisions section of the eVaka service.
              </P>
            </>
          ),
          instructions: {
            DAYCARE:
              'Indicate the most common times your child needs early childhood education. If your child requires evening and shift care, please specify the earliest and latest times your child needs early childhood education.',
            PRESCHOOL:
              'Daycare centers and schools provide preschool education for four hours a day. Please indicate the hours during which your child needs early childhood education and care, including the time spent in preschool education (e.g. 7 a.m. to 5 p.m.). You will be asked to specify the times in greater detail once your child enters early childhood education and care. If the care needs vary by day or week (e.g. in cases where shift care is required), please indicate the hours in greater detail in the Additional information field of this application.'
          },
          usualArrivalAndDeparture: {
            DAYCARE: 'Daily time in early childhood education and care'
          }
        },
        preparatory: 'The child needs support to learn Finnish.',
        preparatoryInfo:
          "A language assessment is conducted for each child whose mother tongue is not Finnish, Swedish, or Sami. This assessment will serve as a basis for constructing a Finnish as a Second Language (S2) curriculum. S2 education is integrated into daily activities according to the child's needs."
      },
      contactInfo: {
        familyInfo: undefined,
        info: (
          <P data-qa="contact-info-text">
            The personal information has been retrieved from the population
            information system and therefore cannot be changed using this form.
            If there are errors in your personal information, please update your
            information{' '}
            <ExternalLink
              text="on the website of the Digital and Population Data Services Agency"
              href="https://dvv.fi/henkiloasiakkaat"
              newTab
            />
            . If your address is going to change in the near future, you can add
            your future address in a separate field of this application. Enter
            the future address for both the child and the guardian. The address
            is only considered official once it has been updated in the
            population information system. The guardian’s information can be
            found in the population information system; decisions regarding
            preschool education and early childhood education and care will be
            automatically submitted to any guardians living at a different
            address, as well.
          </P>
        ),
        otherChildrenInfo:
          'Other children under 18 years of age living in the same household according to the population register affect the cost of daycare or the deductible for the service voucher.',
        otherChildrenChoiceInfo:
          'Select the children who live in the same household according to the population register.',
        secondGuardianInfoPreschoolSeparated:
          'The address information for the other guardian is retrieved from the population information system automatically. According to our information, the child has another guardian living at a different address. Registration for preschool education must be agreed upon jointly with the other guardian.',
        secondGuardianAgreementStatus: {
          label:
            'Have you and the other guardian agreed to make this application?*',
          AGREED:
            'We have discussed making this application and agreed on it jointly.',
          NOT_AGREED:
            'We have not been able to agree on making the application.',
          RIGHT_TO_GET_NOTIFIED:
            'The other guardian only has the right of information.'
        },
        futureAddressInfo:
          'Oulu’s early childhood education, preschool education and care services consider the address obtained from the population information system to be the official address. The address entered in the population information system changes when you submit a notification of change of address to the Digital and Population Data Services Agency.'
      },
      fee: {
        info: {
          DAYCARE: (
            <P>
              The client fees and the share of the service voucher to be paid by
              the family are determined as a percentage of the family’s combined
              gross income. Family size and income and the duration of early
              childhood education all affect the amount to be paid. If the cost
              of early childhood education and care at a private unit is higher
              than the value of the service voucher, the family is responsible
              for paying the difference. The family must submit an income
              declaration detailing their gross income using the income
              declaration form as soon as possible after the child has started
              attending early childhood education and care.
            </P>
          ),
          PRESCHOOL: (
            <P>
              Preschool education is free of charge, but the related early
              childhood education and care is subject to a fee. If the child
              attends early childhood education and care in related to preschool
              preschool education, the family must submit an income declaration
              detailing their gross income using the income declaration form as
              soon as possible after the child has started attending early
              childhood education and care.
            </P>
          )
        },
        links: (
          <>
            <P>
              You can find the income declaration form in the Income information
              section of the User menu in the eVaka service.
            </P>
            <P>
              For more information on client fees, visit the website of the City
              of Oulu:
              <ExternalLink
                href="https://www.ouka.fi/oulu/paivahoito-ja-esiopetus/paivahoitomaksut"
                text="Client fees in early childhood education"
                newTab
              />
            </P>
          </>
        )
      },
      additionalDetails: {
        dietPlaceholder: 'Indicate your child’s special diet here',
        dietInfo: <> Indicate your child&apos;s special diet here. </>,
        allergiesPlaceholder: 'Indicate your child’s allergies here'
      },
      actions: {
        allowOtherGuardianAccess: (
          <span>
            I understand that the application will also be visible to the other
            guardian. If the other guardian should not be able to see this
            application, please contact the Early Childhood Education Service
            Guidance.
          </span>
        )
      }
    }
  },
  applicationsList: {
    title:
      'Applying for early childhood education and enrolling in preschool education',
    type: {
      DAYCARE: 'Application for early childhood education',
      PRESCHOOL: 'Application for preschool education',
      CLUB: 'Club application'
    },
    summary: (
      <>
        <P $width="800px">
          The child&apos;s guardian can make an application for the child to be
          granted a place in early childhood education and care and in clubs and
          groups organized as a part of open early childhood education, or
          enroll the child in preschool education. The same application can be
          used for applying for a service voucher for early childhood education
          and care, by applying for a place a private early childhood education
          and care unit. The guardian’s and the children’s address information
          is retrieved from the population information system automatically.
        </P>
        <P $width="800px">
          If the child has already been granted a place in an early childhood
          education and care unit in Oulu, and the family wishes to apply for a
          transfer to another unit, they must submit a new application.
        </P>
      </>
    )
  },
  footer: {
    cityLabel: '© City of Oulu',
    privacyPolicyLink: (
      <ExternalLink
        href="https://www.ouka.fi/oulu/verkkoasiointi/tietosuoja-ja-rekisteriselosteet-kasvatus-ja-koulutus"
        text="Privacy statements"
        newTab={true}
        data-qa="footer-policy-link"
      />
    ),
    sendFeedbackLink: (
      <ExternalLink
        href="https://e-kartta.ouka.fi/efeedback"
        text="Give feedback"
        newTab={true}
        data-qa="footer-feedback-link"
      />
    )
  },
  loginPage: {
    title: 'Early childhood education and care in the City of Oulu',
    login: {
      title: 'Sign in with username',
      paragraph:
        'Take care of your child’s daily early childhood education affairs in eVaka.',
      link: 'Log in',
      infoBoxText: (
        <>
          <ExternalLink
            href="https://www.ouka.fi/oulu/english/evaka"
            text="eVaka - Electronic services - Early childhood education - City of Oulu"
            newTab={true}
            data-qa="footer-policy-link"
          />
        </>
      )
    }
  },
  map: {
    mainInfo: `You can use this view to search Oulu’s early childhood education care units, preschool education units, and open early childhood education and care units by their geographic location. You can find information about private kindergartens on the website of the City of Oulu.`,
    privateUnitInfo: <></>,
    serviceVoucherLink:
      'https://www.ouka.fi/oulu/palveluseteli/yksityisen-paivahoidon-palveluseteli',
    searchPlaceholder: 'E.g. Ainolan päiväkoti',
    careTypes: {
      CLUB: 'Open early childhood education and care',
      DAYCARE: 'Early childhood education and care',
      PRESCHOOL: 'Preschool education'
    }
  },
  decisions: {
    summary: 'This page displays the received decisions regarding your child.'
  },
  income: {
    description: (
      <>
        <p data-qa="income-description-p1">
          On this page, you can submit declarations regarding the part of your
          income affecting your early childhood education fees. You can also
          view the income declarations you have submitted and edit or delete
          them until they have been processed by the assigned authority. After
          your form has been processed, you can only update your income
          information by submitting a new form.
        </p>
        <p data-qa="income-description-p2">
          <strong>
            Both adults living in the same household must submit separate income
            declarations.
          </strong>
        </p>
        <p data-qa="income-description-p3">
          The client fees for municipal early childhood education and care are
          determined as a percentage of the family’s combined gross income.
          Family size and income and the duration of early childhood education
          all affect the amount to be paid.
        </p>
        <p data-qa="income-description-p4">
          <a href="https://www.ouka.fi/oulu/paivahoito-ja-esiopetus/paivahoitomaksut">
            Additional information about the client fees
          </a>
        </p>
      </>
    ),
    incomeType: {
      startDate: 'Valid from',
      endDate: 'Expires on',
      title: 'Grounds for determining the client fee',
      agreeToHighestFee:
        'I acknowledge and accept that I will be charged the highest possible client fee for early childhood education and care',
      highestFeeInfo:
        'The highest possible client fee according to my family’s service needs is valid until further notice or until my child’s early childhood education and care ends. (Income information not required)',
      grossIncome: 'Determining the client fee based on gross income.'
    },
    grossIncome: {
      title: 'Filling out the information on your gross income',
      description: (
        <>
          <P />
        </>
      ),
      incomeSource: 'Submitting the income information',
      incomesRegisterConsent:
        'My information is being verified from the income register and Kela.',
      provideAttachments:
        'I will provide my income details as an attachment. If necessary, the accuracy of the information will be verified from the income register or Kela.',
      estimate: 'My estimated gross income',
      estimatedMonthlyIncome:
        'Average income including holiday compensation, €/month',
      otherIncome: 'Other income',
      otherIncomeDescription:
        'If you have any other sources of income, you must provide proof or receipts as attachments. You can find a list of all required attachments at the bottom of the form under the section Attachments relating to income and early childhood education fees.',
      choosePlaceholder: 'Select',
      otherIncomeTypes: {
        PENSION: 'Pension',
        ADULT_EDUCATION_ALLOWANCE: 'Adult education allowance',
        SICKNESS_ALLOWANCE: 'Sickness allowance',
        PARENTAL_ALLOWANCE: 'Maternity and parental allowance',
        HOME_CARE_ALLOWANCE: 'Child home care allowance',
        FLEXIBLE_AND_PARTIAL_HOME_CARE_ALLOWANCE:
          'Flexible or partial child home care allowance',
        ALIMONY: 'Maintenance allowance or support',
        INTEREST_AND_INVESTMENT_INCOME: 'Interest and dividend income',
        RENTAL_INCOME: 'Rental income',
        UNEMPLOYMENT_ALLOWANCE: 'Unemployment allowance',
        LABOUR_MARKET_SUBSIDY: 'Labor market subsidy',
        ADJUSTED_DAILY_ALLOWANCE: 'Adjusted unemployment allowance',
        JOB_ALTERNATION_COMPENSATION: 'Job alternation compensation',
        REWARD_OR_BONUS: 'Compensation or bonus',
        RELATIVE_CARE_SUPPORT: 'Informal care allowance',
        BASIC_INCOME: 'Basic income',
        FOREST_INCOME: 'Forest income',
        FAMILY_CARE_COMPENSATION: 'Family care compensation',
        REHABILITATION:
          'Rehabilitation allowance or partial rehabilitation allowance',
        EDUCATION_ALLOWANCE: 'Education allowance',
        GRANT: 'Grant or scholarship',
        APPRENTICESHIP_SALARY: 'Income from apprenticeship training',
        ACCIDENT_INSURANCE_COMPENSATION: 'Accident insurance payment',
        OTHER_INCOME: 'Other sources of income'
      },
      otherIncomeInfoLabel: 'Estimate of other income',
      otherIncomeInfoDescription:
        'Enter an estimate of your income received from other sources income (EUR/month) here, e.g. "Rental income 150, child home care allowance 300"'
    },
    entrepreneurIncome: {
      title: 'Filling out the income information for entrepreneurs',
      description:
        'If necessary, you can fill in the information for more than one company by ticking the boxes that apply to all of your companies.',
      startOfEntrepreneurship: 'Entrepreneurship started on',
      spouseWorksInCompany: 'Does your spouse work for the company?',
      yes: 'Yes',
      no: 'No',
      startupGrantLabel: 'Has your business been granted a start-up grant?',
      startupGrant:
        'My business has been granted a start-up grant. I will include the decision regarding the start-up grant in the attachments.',
      checkupLabel: 'Review your information',
      checkupConsent:
        'I accept that information related to my income may be retrieved from the income register and Kela if need be.',
      companyInfo: 'Company information',
      companyForm: 'Type of business',
      selfEmployed: 'Trade name',
      limitedCompany: 'Limited liability company',
      partnership: 'Partnership or limited partnership company',
      lightEntrepreneur: 'Light entrepreneurship',
      lightEntrepreneurInfo:
        'Provide receipts for paid salaries and wages in the attachments.',
      partnershipInfo: ''
    },
    moreInfo: {
      title: 'Additional information related to the payment',
      studentLabel: 'Are you currently a student?',
      student: 'I am a student.',
      studentInfo:
        'If you are a student, you must submit a study certificate and a decision regarding social benefits for students.',
      deductions: 'Deductions',
      alimony:
        'I pay child support. I will provide a copy of a receipt for payment in the attachments.',
      otherInfoLabel: 'Additional information about the income information'
    },
    attachments: {
      title:
        'Attachments relating to income and early childhood education fees',
      description:
        'You can submit attachments relating to income and early childhood education fees here. If your family has already agreed to pay the highest possible client fee, the attachments are not required.',
      required: {
        title: 'Required attachments'
      },
      attachmentNames: {
        PENSION: 'Decision on pension',
        ADULT_EDUCATION_ALLOWANCE: 'Decision on adult education allowance',
        SICKNESS_ALLOWANCE: 'Decision on sickness allowance',
        PARENTAL_ALLOWANCE: 'Decision on maternity or parental allowance',
        HOME_CARE_ALLOWANCE: 'Decision on home care allowance',
        FLEXIBLE_AND_PARTIAL_HOME_CARE_ALLOWANCE:
          'Decision on flexible or partial home care allowance',
        ALIMONY: 'Maintenance agreement or decision on maintenance',
        UNEMPLOYMENT_ALLOWANCE: 'Decision on unemployment allowance',
        LABOUR_MARKET_SUBSIDY: 'Decision on labor market subsidy',
        ADJUSTED_DAILY_ALLOWANCE: 'Decision on adjusted unemployment allowance',
        JOB_ALTERNATION_COMPENSATION:
          'Receipt of job alternation compensation ',
        REWARD_OR_BONUS: 'Receipt of paid bonus and/or compensation',
        RELATIVE_CARE_SUPPORT: 'Decision on informal care allowance',
        BASIC_INCOME: 'Decision on basic income',
        FOREST_INCOME: 'Receipt for received forest income',
        FAMILY_CARE_COMPENSATION: 'Receipt for paid family care compensation',
        REHABILITATION:
          'Decision on rehabilitation allowance or partial rehabilitation allowance',
        EDUCATION_ALLOWANCE: 'Decision on education allowance',
        GRANT: 'Receipt for received grant or scholarship',
        APPRENTICESHIP_SALARY:
          'Receipt for salary paid for apprenticeship training',
        ACCIDENT_INSURANCE_COMPENSATION:
          'Receipt for accident insurance payment',
        OTHER_INCOME: 'Attachments detailing other income',
        ALIMONY_PAYOUT: 'Proof of payment of maintenance payment',
        INTEREST_AND_INVESTMENT_INCOME:
          'Receipts for received interest and dividend income',
        RENTAL_INCOME: 'Receipts for rental income and condominium payment',
        PAYSLIP_GROSS: 'Latest payslip',
        PAYSLIP_LLC: 'Latest payslip',
        STARTUP_GRANT: 'Start-up grant',
        ACCOUNTANT_REPORT_PARTNERSHIP:
          'Accountant’s account salary and fringe benefits',
        ACCOUNTANT_REPORT_LLC:
          "Accountant's statement of benefits in kind and dividends",
        PROFIT_AND_LOSS_STATEMENT_SELF_EMPLOYED:
          'Profit and loss account or taxation decision',
        PROFIT_AND_LOSS_STATEMENT_PARTNERSHIP:
          'Profit and loss account and balance sheet',
        SALARY: 'Receipts for paid salaries and wages',
        PROOF_OF_STUDIES:
          'Study certificate or decision on adult education allowance from an unemployment fund / education allowance from an employment fund',
        CHILD_INCOME: "Receipts for the child's income"
      }
    },
    selfEmployed: {
      info: '',
      attachments:
        'I will include the company’s latest income statement and balance sheet or tax decision in the attachments.',
      estimatedIncome:
        'I am a new entrepreneur. I will provide an estimate of my average monthly income. I will make the income statement and balance sheet available as soon as possible.',
      estimatedMonthlyIncome: 'Average income EUR/month',
      timeRange: 'During the following period of time'
    },
    formDescription: (
      <>
        <P data-qa="income-formDescription-p1">
          You must provide the income declaration as soon as possible after the
          early childhood education begins. If the income information is not
          provided or the provided information is incomplete, the client fee
          will be determined as the highest possible fee.
        </P>
        <P>
          The client fee is charged from the early childhood education and care
          start date determined in the corresponding decision.
        </P>
        <P>
          The client must notify the early childhood education and care client
          fee team of any changes in income or family size. If necessary, the
          authorities also have the right to adjust early childhood education
          and care fees retroactively.
        </P>
        <P>
          <strong>Please note:</strong>
        </P>
        <Gap $size="xs" />
        <UnorderedList data-qa="income-formDescription-ul">
          <li>
            If your income exceeds the income limit determined for your family
            size, accept the highest possible early childhood education and care
            client fee. If you do this, you will not have to provide any
            declarations, receipts or other proof of income.
          </li>
          <li>
            If your family includes another adult, they also have to submit an
            income declaration by logging onto eVaka with their own personal
            information and filling out this form.
          </li>
          <li>
            View current income limits
            <a
              target="_blank"
              rel="noreferrer"
              href="https://www.ouka.fi/oulu/paivahoito-ja-esiopetus/paivahoitomaksut"
            >
              here
            </a>
            .
          </li>
        </UnorderedList>
        <P>*Fields marked with an asterisk are mandatory.</P>
      </>
    )
  },
  accessibilityStatement: (
    <>
      <H1>Accessibility statement</H1>
      <P>
        This accessibility statement is given on the eVaka web service provided
        by the early childhood education and care services of the City of Oulu.
        <a href="https://varhaiskasvatus.ouka.fi">varhaiskasvatus.ouka.fi</a>.
        The City of Oulu seeks to guarantee the accessibility of this website,
        to continuously improve the user experience it offers, and to comply
        with the appropriate accessibility standards.
      </P>
      <P>
        The accessibility of this service was assessed by the service
        development team. The accessibility statement was published on April 12,
        2022.
      </P>
      <H2>Service compliance</H2>
      <P>
        This web service meets the critical accessibility requirements set forth
        in the relevant legislation in accordance with level AA of WCAG 2.1. The
        service does not yet completely comply with the requirements.
      </P>
      <H2>Accessibility support measures</H2>
      <P>
        We utilize the following measures to ensure the accessibility of this
        service:
      </P>
      <ul>
        <li>
          Accessibility is taken into account from the start of the process when
          selecting colors and font sizes in the design phase, for example.
        </li>
        <li>
          The elements of the service are defined in a semantically consistent
          manner.
        </li>
        <li>
          The service is tested with a screen reader at regular intervals.
        </li>
        <li>
          The service is tested by different users who provide feedback on
          accessibility.
        </li>
        <li>
          The accessibility of the site is reviewed every time the technology
          used on the site or its contents change.
        </li>
      </ul>
      <P>
        This brochure is updated with site changes and accessibility reviews.
      </P>
      <H2>Known accessibility issues</H2>
      <P>
        Users may still experience some issues when using the site. The
        following is a description of known accessibility issues. If you notice
        an issue that is not on this list, please contact us.
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
        We cannot guarantee the accessibility of third-party services. This web
        service utilizes the following third-party services:
      </P>
      <ul>
        <li>Suomi.fi authorization</li>
        <li>The map service Leaflet</li>
      </ul>
      <H2>Alternative ways of using the service</H2>
      <P>
        <ExternalLink
          href="https://www.ouka.fi/oulu/asiointi-ja-neuvonta"
          text="The service points of the City of Oulu"
          newTab
        />{' '}
        offer guidance to using electronic services. The service point staff
        also provide help to users for whom digital services are not accessible.
      </P>
      <H2>Give feedback</H2>
      <P>
        If you notice an accessibility-related issue on the service, please let
        us know! You can send us feedback using this{' '}
        <ExternalLink
          href="https://e-kartta.ouka.fi/efeedback"
          text="online form"
          newTab
        />{' '}
        or by mail{' '}
        <a href="mailto:varhaiskasvatus@ouka.fi">varhaiskasvatus@ouka.fi</a>.
      </P>
      <H2>Supervisory authority</H2>
      <P>
        If you detect any accessibility problems on the website, please first
        give feedback to the site administrators. It may take 14 days until you
        get a reply. If you are not happy with the response or do not receive a
        reply in two weeks, you can send your feedback to the Finnish Transport
        and Communications Agency Traficom. Finnish Transport and Communications
        Agency Traficom will tell you in detail how to file a complaint and how
        the matter will be dealt with.
      </P>
      <P>
        <strong>Control authority contact details</strong>
        <br />
        Finnish Transport and Communications Agency Traficom <br />
        Digital Accessibility Supervision Unit
        <br />
        <ExternalLink
          href="https://www.saavutettavuusvaatimukset.fi/en"
          text="Web accessibility"
          newTab
        />
        <br />
        <a href="mailto:saavutettavuus@traficom.fi">
          saavutettavuus@traficom.fi
        </a>
        <br />
        telephone switchboard 029 534 5000
      </P>
    </>
  )
}

export default fi
