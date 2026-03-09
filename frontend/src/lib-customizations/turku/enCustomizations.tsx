{
  /*
SPDX-FileCopyrightText: 2021 City of Turku

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import React from 'react'

import ExternalLink from 'lib-components/atoms/ExternalLink'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import { H1, H2, H3, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import type { Translations } from 'lib-customizations/citizen'
import type { DeepPartial } from 'lib-customizations/types'

const customerContactText = function () {
  return (
    <>
      {' '}
      early childhood education service guidance tel.{' '}
      <a href="tel:+35822625610">02 2625610</a>.
    </>
  )
}

const en: DeepPartial<Translations> = {
  calendar: {
    absenceModal: {
      absenceTypes: {
        PLANNED_ABSENCE: 'Planned absence'
      },
      selectChildrenInfo: 'Enter here only full-day absences.'
    }
  },
  children: {
    serviceApplication: {
      startDateInfo:
        'Choose the date when you want the new service need to start. The service need can only be changed from the beginning of the month and must be valid for at least four (4) months.'
    }
  },
  applications: {
    creation: {
      daycareInfo:
        'An early childhood education and care application is submitted for a municipal daycare centre or private family daycare. You can use the same application to apply for a service voucher for private early childhood education and care by selecting the service voucher unit you want to apply for under Wishes.',
      preschoolLabel: 'Enrolment in pre-primary education ',
      preschoolInfo:
        'Free pre-primary education is given four hours a day. The school year follows quite closely the school and holiday periods of primary schools. You can also apply for early childhood education and care that supplements pre-primary education. This is given in pre-primary education places in the mornings before and in the afternoons after pre-primary education.',
      preschoolDaycareInfo: '',
      clubLabel: 'Open early childhood education and care application',
      clubInfo:
        'With an open early childhood education and care application, you apply for open early childhood education and care clubs and playground activities.',
      applicationInfo: (
        <P>
          The parent or guardian can make changes to the application online
          until the application is processed by our customer services. Any
          changes after this point, or cancellation of application, can be done
          by contacting
          {customerContactText()}
        </P>
      ),
      duplicateWarning:
        'The child already has a similar application that is being processed. Go back to the Applications view and make changes to an existing application or contact the Early childhood education and care customer service.',
      transferApplicationInfo: {
        DAYCARE:
          'The child already has a place in early childhood education in Turku. Use this application to apply for a transfer to another unit in Turku that provides early childhood education and care.'
      }
    },
    editor: {
      heading: {
        title: {
          DAYCARE:
            'Early childhood education and care and service voucher application',
          PRESCHOOL: 'Enrolment in pre-primary education',
          CLUB: 'Application for open early childhood education and care'
        },
        info: {
          DAYCARE: (
            <>
              <P>
                Early childhood education and care can be applied for any time
                of the year. The application must be submitted four months
                before you need the place. If you need early childhood education
                and care urgently because of work or study, the application must
                be sent at least two weeks before.
              </P>
              <p>
                You will receive a written decision about a place in early
                childhood education and care{' '}
                <ExternalLink
                  text="in the Suomi.fi Messages service"
                  href="https://https://www.suomi.fi/messages/chains"
                  newTab
                />{' '}
                or by post if you are not using the Suomi.fi service. The
                decision is also available in the eVaka service under
                Applications - Decisions.
              </p>
              <P $fitted={true}>
                * Fields indicated by an asterisk are required
              </P>
            </>
          ),
          PRESCHOOL: (
            <>
              <P>
                Children take part in pre-primary education one year before the
                start of compulsory education. Pre-primary education is free.
                Enrol for pre-primary education for the 2026/27 school year 1–15
                January 2026. Pre-primary education begins on 11th of August
                When registering, please explain your application options in the
                information section.
              </P>
              <P>
                Decisions will appear in the
                <a
                  href="https://https://www.suomi.fi/messages/chains"
                  target="_blank"
                  rel="noreferrer"
                >
                  Suomi.fi Messages service, or by post if you are not using the
                  Suomi.fi service.
                </a>{' '}
              </P>
              <P $fitted={true}>
                * Fields indicated by an asterisk are required
              </P>
            </>
          ),
          CLUB: (
            <>
              <P>
                You can apply for open early childhood education and care all
                year round, and the place is available until you cancel it or
                the child moves on to early childhood education and care and
                pre-primary education. A decision on open early childhood
                education and care will be sent to the Suomi.fi service, or by
                post if you are not using the Suomi.fi service. The decision is
                also available in the eVaka service under Applications –
                Decisions.
              </P>
              <P>
                Open early childhood education and care organised by the City of
                Turku is free (clubs and playground activities).
              </P>
              <P>
                For more information about open early childhood education and
                care, see the City of Turku’s website:{' '}
                <ExternalLink
                  text="Clubs, playgrounds activities and open daycare."
                  href="https://www.turku.fi/varhaiskasvatus-ja-esiopetus/avoin-varhaiskasvatustoiminta"
                  newTab
                />
              </P>
              <P>* Fields indicated by an asterisk are required.</P>
            </>
          )
        }
      },
      serviceNeed: {
        preparatory: 'Child needs assistance in learning Finnish.',
        preparatoryInfo:
          'Each child whose mother tongue is not Finnish, Swedish or Sámi. Turku early childhood education and care will assess the child’s need for instruction in preparation for basic education.',
        startDate: {
          header: {
            DAYCARE: 'Start of early childhood education and care',
            PRESCHOOL: 'Start of pre-primary education',
            CLUB: 'Start of open early childhood education and care'
          },
          clubTerm:
            'Open early childhood education and care period of activity',
          clubTerms:
            'Open early childhood education and care periods of activity',
          label: {
            DAYCARE: 'Desired start date',
            PRESCHOOL: 'Desired start date',
            CLUB: 'Desired start date for open early childhood education and care'
          },
          info: {
            DAYCARE: [],
            PRESCHOOL: [
              'Pre-primary education in Finnish and Swedish begins on 11 August 2026. If you need early childhood education and care to complement pre-primary education, you can apply for it under Early childhood education and care complementing pre-primary education. If a child transfers from private early childhood education and care to municipal, submit an application for supplementary early childhood education and care.'
            ],
            CLUB: [
              'Open early childhood education and care clubs and playground activities follow as a rule the pre-primary education school and holiday periods. A child can participate in one open early childhood education and care service at a time, excluding clubs for families with children.'
            ]
          },
          instructions: {
            DAYCARE: (
              <>
                You can postpone the desired start date until service guidance
                has started processing the application. After this, any changes
                to the desired start date are made by contacting
                {customerContactText()}
              </>
            ),
            PRESCHOOL: (
              <>
                You can postpone the desired start date until service guidance
                has started processing the application. After this, any changes
                to the desired start date are made by contacting
                {customerContactText()}
              </>
            ),
            CLUB: null
          }
        },
        clubDetails: {
          wasOnDaycare:
            'A child has an early childhood education and care place that will be given up once an open early childhood education and care place becomes available.',
          wasOnDaycareInfo: '',
          wasOnClubCare:
            'A child has been on open early childhood education and care during the previous period of activity.',
          wasOnClubCareInfo: ''
        },
        urgent: {
          attachmentsMessage: {
            text: (
              <P $fitted={true}>
                If the need for an early childhood education and care place is
                caused by work or study that will begin on short notice, the
                place must be applied for at least two weeks before the need
                begins. The application must include the relevant documentation
                for work or study from both parents or guardians in the
                household. The two-week processing period begins once we have
                received the application and the necessary attachments. If you
                cannot add attachments electronically, please phone
                {customerContactText()} You can also post attachments to
                Varhaiskasvatuksen palveluohjaus PL 355, 20101, Turun kaupunki
                or take them in person to Kauppatori Monitori,
                Varhaiskasvatuksen palveluohjaus, Aurakatu 8.
              </P>
            )
          }
        },
        shiftCare: {
          instructions:
            'By evening and nonstandard hour childcare we primarily mean early childhood education and care outside the hours of 6 am and 6 pm, or during weekends or round the clock. If you need evening or nonstandard hour childcare, please specify your need under Additional information.',
          attachmentsMessage: {
            DAYCARE: (
              <P>
                Evening and nonstandard hour childcare is for children whose
                both parents do shift work or study as a rule during evenings
                and/or weekends. The application must include documentation for
                shift work by the employer or the need for care in evenings or
                weekends owing to studies. If you cannot add attachments
                electronically, phone Early childhood education and care service
                guidance on +358 2 262 5610. You can also post attachments to
                Varhaiskasvatuksen palveluohjaus PL 355, 20101, Turun kaupunki
                or taking them in person to Kauppatori Monitori,
                Varhaiskasvatuksen palveluohjaus, Aurakatu 8.
              </P>
            ),
            PRESCHOOL: (
              <>
                <P>
                  If a guardian living in the same household does regular shift
                  work or attends evening studies full-time, they must provide
                  proof of this (a document issued by their employer or the
                  educational institution) together with the application for
                  pre-primary education. The documents must be dated in the year
                  when the application for pre-primary education is submitted.
                </P>
                <P>
                  Evening and nonstandard hour childcare is for children whose
                  both parents do shift work or study as a rule during evenings
                  and/or weekends. The application must include documentation
                  for shift work by the employer or the need for care in
                  evenings or weekends owing to studies. If you cannot add
                  attachments electronically, phone Early childhood education
                  and care service guidance on +358 2 262 5610. You can also
                  post attachments to Varhaiskasvatuksen palveluohjaus PL 355,
                  20101, Turun kaupunki or taking them in person to Kauppatori
                  Monitori, Varhaiskasvatuksen palveluohjaus, Aurakatu 8.
                </P>
              </>
            )
          }
        },
        assistanceNeedInstructions: {
          DAYCARE:
            'Need for support for a child refers to a type of need that has been verified by expert statements. The child is supported daily as part of early childhood education and care. A special needs teacher in early childhood education and care will contact the applicant to ensure that the child’s needs are taken into account when selecting a place.',
          CLUB: 'Need for support for a child refers to a type of need that has been verified by expert statements. The child is supported daily as part of early childhood education and care. A special needs teacher in early childhood education and care will contact the applicant to ensure that the child’s needs are taken into account when selecting a place.',
          PRESCHOOL:
            'Select this part in the application if your child need support for their development and/or learning during their pre-primary education year. The child is supported daily as part of pre-primary education and early childhood education and care. A special needs teacher in early childhood education and care will contact the applicant to ensure that the child’s needs are taken into account in terms of pre-primary education.'
        },
        partTime: {
          true: 'Part-time (max 20 h/week, max 84 h/month)',
          false: 'Full-day'
        },
        dailyTime: {
          label: {
            DAYCARE: 'Service need',
            PRESCHOOL:
              'Pre-primary education provided in daycare centres and schools for four hours a day. Indicate the child’s need for early childhood education and care so that it also includes the pre-primary education (for example, 7 am to 5 pm). This time is be specified in more detail once early childhood education and care begins. If the daily early childhood education and care changes daily or weekly (in the case of nonstandard hour childcare), indicate the need in more details under “Additional information”.'
          },
          connectedDaycare:
            'I am also applying for early childhood education and care complementing pre-primary education.',
          connectedDaycareInfo: (
            <>
              <P>
                You can apply for chargeable early childhood education and care
                to complement your child’s pre-primary education. If you want
                your child to begin early childhood education and care later
                than pre-primary education, enter the desired start date under
                “Additional information”. If you are applying for a service
                voucher for a private daycare centre, select the unit you are
                applying for.
              </P>
              <P>
                You will receive a written decision about a place in early
                childhood education and care{' '}
                <a
                  href="https://https://www.suomi.fi/messages/chains"
                  target="_blank"
                  rel="noreferrer"
                >
                  in the Suomi.fi Messages
                </a>{' '}
                service, or by post if you are not using the Suomi.fi service.
                The decision is also available in the eVaka service under
                Applications - Decision.
              </P>
            </>
          )
        }
      },
      verification: {
        serviceNeed: {
          wasOnClubCareYes:
            'A child has been on open early childhood education and care during the previous period of activity.',
          connectedDaycare: {
            label: 'Complementary early childhood education and care',
            withConnectedDaycare:
              'I am also applying for early childhood education and care complementing pre-primary education.',
            withoutConnectedDaycare: 'Ei'
          }
        }
      },
      unitPreference: {
        title: 'Application preference',
        siblingBasis: {
          title: 'Application on a sibling basis',
          info: {
            DAYCARE: (
              <>
                <P>
                  A child will have preference for a unit providing early
                  childhood education and care where their siblings are. The
                  goal is to have siblings in the same early childhood education
                  and care unit if the family so wishes. If you are applying for
                  a place for siblings that <b>are not yet</b> in early
                  childhood education and care, please indicate this in the
                  application under Additional information.
                </P>
                <P>
                  Fill in these details only if you are applying on a sibling
                  basis, and select the same early childhood education and care
                  unit where the child’s sibling is as the primary choice below.
                </P>
              </>
            ),
            PRESCHOOL: (
              <>
                <P>Applying on a sibling basis is not available in Turku.</P>
              </>
            ),
            CLUB: (
              <>
                <P>
                  The goal is to have siblings in the same open early childhood
                  education and care if the family so wishes.
                </P>
                <P>
                  Fill in this information only if applying on a sibling basis
                  and select below under Preferences the same open early
                  childhood education and care unit where the child’s sibling is
                  as the primary choice.
                </P>
              </>
            )
          },
          checkbox: {
            DAYCARE:
              'I am applying primarily for the same open early childhood education and care unit where the child’s sibling already is.',
            PRESCHOOL:
              'I am applying primarily for the same unit where the child’s sibling already is.',
            CLUB: 'I am applying primarily for the same open early childhood education and care where the child’s sibling already is.'
          },
          radioLabel: {
            DAYCARE:
              'Select the sibling with which you want the child to be in the same early childhood education and care unit',
            PRESCHOOL:
              'Select the sibling with whom you want the child to be in the same unit',
            CLUB: 'Select the sibling with whom you want the child to be in the same open early childhood education and care unit'
          },
          otherSibling: 'Other sibling',
          names: 'Sibling’s given names and surname',
          namesPlaceholder: 'Given names and surname',
          ssn: 'Sibling’s personal identity code',
          ssnPlaceholder: 'Personal identity code'
        },
        units: {
          title: (maxUnits: number): string =>
            maxUnits === 1
              ? 'Application preference'
              : 'Application preferences',
          startDateMissing:
            'In order to select your preferences, first indicate the desired start date in the “Service need” section',
          info: {
            DAYCARE: (
              <>
                <P>
                  You can apply for up to three places in order of preference.
                  We cannot guarantee a place in the unit of your preference,
                  but you chances are improved if you enter more than one
                  alternative.
                </P>
                <P>
                  You can see the location of early childhood education and care
                  units by selecting ‘Units on the map’.
                </P>
                <P>
                  If you are applying for a service voucher, please select the
                  unit you are applying for. If your primary preference is a
                  service voucher unit, please contact the unit directly.{' '}
                  <i>
                    When applying for a service voucher unit, the unit’s manager
                    will be informed of your application.
                  </i>
                </P>
              </>
            ),
            PRESCHOOL: (
              <>
                <P>
                  You can apply up to three places in order of preference. We
                  cannot guarantee a place in the unit of your preference, but
                  you chances are improved if you enter more than one
                  alternative.
                </P>
                <P>
                  You can see the location of the units by selecting ‘Units on
                  the map’.
                </P>
                <P>
                  If you are applying for a service voucher, please select the
                  unit you are applying for. When applying for a service voucher
                  unit, the unit’s manager will be informed of your application.
                </P>
              </>
            ),
            CLUB: (
              <>
                <P>
                  You can apply for up to three places in order of preference.
                  We cannot guarantee a place in the open early childhood
                  education and care unit of your preference, but you chances
                  are improved if you enter more than one alternative.
                </P>
                <P>
                  You can see the location of open early childhood education and
                  care units by selecting ‘Units on the map’.
                </P>
                <P>
                  The Units on the map link will show a map of Turku. The unit
                  language is left as it is for the time being.
                </P>
              </>
            )
          },
          mapLink: 'Units on the map',
          serviceVoucherLink:
            'https://www.turku.fi/varhaiskasvatus-ja-esiopetus/maksut-tuet-ja-palveluseteli',
          languageFilter: {
            label: 'Unit language',
            fi: 'Finnish',
            sv: 'Swedish'
          },
          select: {
            label: (maxUnits: number): string =>
              maxUnits === 1 ? 'Select preference' : 'Select preferences',
            placeholder: 'Search units',
            maxSelected: 'Maximum number of units selected',
            noOptions: 'No units matching the criteria'
          },
          preferences: {
            label: (maxUnits: number): string =>
              maxUnits === 1 ? 'Your preference' : 'Your preferences',
            noSelections: 'No choices',
            info: (maxUnits: number) =>
              maxUnits === 1
                ? 'Select one early childhood education and care unit'
                : `Select 1–${maxUnits} early childhood education and care units and place them in your order of preference. You can change the order using the arrows.`,
            fi: 'Finnish',
            sv: 'Swedish',
            en: 'English',
            moveUp: 'Move up',
            moveDown: 'Move down',
            remove: 'Remove selection'
          }
        }
      },
      contactInfo: {
        secondGuardianInfoPreschoolSeparated:
          'Details of the other parent or guardian are retrieved automatically from the Population Information System. According to information we have, the other parent or guardian lives at a different address. Enrolment in pre-primary education must be agreed with the other parent or guardian',
        info: (
          <>
            <P>
              Please list in the application all adults and children in the same
              household.
            </P>
            <P data-qa="contact-info-text">
              The personal data is retrieved from the Population Information
              System and cannot be changed with this application. If there are
              any errors in the personal data, please update the information{' '}
              <ExternalLink
                text="on the Digital and Population Data Services Agency website"
                href="https://dvv.fi/en/individuals"
                newTab
              />
              . If your home address will be changing, you can add your future
              address in another part of the application; please add the future
              address for both the child and the parent or guardian. Address
              information is considered official only once it has been updated
              in the Population Information System. Decisions about pre-primary
              education and early childhood education and care places are
              automatically also sent to parents or guardians living at a
              different address according to the Population Information System.
            </P>
          </>
        ),
        futureAddressInfo:
          'Turku early childhood education and care considers the address obtained from the Population Information System to be the official address. Address information changes once the applicant submits are change notification in the Post Office or the Digital and Population Data Services Agency.'
      },
      fee: {
        info: {
          DAYCARE: (
            <P>
              The customer fees for municipal early childhood education and care
              and the deductible of the service voucher is a percentage of the
              family’s gross income. The fees vary depending on the size and
              income of the family and the period of early childhood education
              and care. If the price of a private early childhood education and
              care place is higher than the value of the service voucher, the
              family pays the difference. The family submits their gross income
              using a form as soon as the child has started in early childhood
              education and care.
            </P>
          ),
          PRESCHOOL: (
            <P>
              Pre-primary education is free, but complementary early childhood
              education and care is charged for. If a child participates in
              early childhood education and care complementing pre-primary
              education, the family submits their gross income using a form as
              soon as the child has started in early childhood education and
              care.
            </P>
          )
        },
        links: (
          <>
            <P>
              The income report form is in eVaka in the User menu, under Income
              details.
            </P>
            <P>
              For more information about customer fees, please see the City of
              Turku website:{' '}
              <ExternalLink
                href="https://www.turku.fi/varhaiskasvatus-ja-esiopetus/maksut-tuet-ja-palveluseteli"
                text="Early education customer fees."
                newTab
              />
            </P>
          </>
        )
      },
      sentInfo: {
        title: 'The application has been sent',
        text: 'The application can be changed until early childhood education and care service guidance has begun processing it.',
        ok: 'OK!'
      },
      actions: {
        allowOtherGuardianAccess: (
          <span>
            I understand that information about the application will also be
            sent to the child’s other parent or guardian. If this information
            should not be sent to the other parent or guardian, please contact
            service guidance.
          </span>
        )
      }
    }
  },
  applicationsList: {
    title: 'Applying for early childhood education and care',
    summary: (
      <>
        <P $width="800px">
          A child’s parent or guardian can submit an application for early
          childhood education care and open early childhood education and care,
          or enrol the child for pre-primary education. The same application can
          be used to apply for early childhood education and care by applying
          for an early childhood education and care place in a service voucher
          unit. Details of the parent or guardian’s children are retrieved
          automatically into this view from the Population Information System.
        </P>
        <P $width="800px">
          If the child already has a place in early childhood education and care
          in Turku and you want the child to be transferred to another unit, you
          must submit a new application.
        </P>
      </>
    )
  },
  footer: {
    cityLabel: '© City of Turku',
    privacyPolicyLink: (
      <ExternalLink
        href="https://www.turku.fi/en/data-protection"
        text="Privacy Policy"
        newTab={true}
        data-qa="footer-policy-link"
      />
    ),
    sendFeedbackLink: (
      <ExternalLink
        href="https://opaskartta.turku.fi/eFeedback/en"
        text="Give feedback"
        newTab={true}
        data-qa="footer-feedback-link"
      />
    )
  },
  loginPage: {
    title: 'City of Turku early childhood education and care',
    login: {
      title: 'Login with username',
      paragraph:
        'Parents or guardians whose child is already in early childhood education and care or pre-primary education: take care of your child’s daily early childhood education and care matters, such as reading messages and informing when your child is and is not present.',
      link: 'Log in',
      infoBoxText: (
        <>
          <P>
            You can create an eVaka username by logging in with strong
            authentication.
          </P>
        </>
      )
    }
  },
  map: {
    mainInfo: `In this view, you can use the map to search for places in early childhood education and care, pre-primary education and open early childhood education and care. Information about private daycare centres can be found on the Day Care and Early Childhood Education web page of the City of Turku.`,
    privateUnitInfo: <></>,
    serviceVoucherLink:
      'https://www.turku.fi/varhaiskasvatus-ja-esiopetus/maksut-tuet-ja-palveluseteli',
    searchPlaceholder: 'For example, the daycare centre on Arkeologinkatu.',
    careTypes: {
      CLUB: 'Open early childhood education and care',
      DAYCARE: 'Early childhood education and care',
      PRESCHOOL: 'Pre-primary education'
    }
  },
  decisions: {
    assistanceDecisions: {
      decision: {
        jurisdiction: '',
        jurisdictionText: '',
        appealInstructionsTitle: 'Appeal instructions',
        appealInstructions: (
          <>
            <H3>Appeal instructions</H3>
            <P>
              You may file an appeal with the Regional State Administrative
              Agency for Southwestern Finland against the above decision within
              30 days of having been notified of it. The decision cannot be
              appealed by taking the matter to a court of law.
            </P>
            <P>An appeal may be filed</P>
            <ul>
              <li>by the party that is affected by it </li>
              <li>
                {' '}
                or whose right, duty or interest the decision directly affects.
              </li>
            </ul>
            <h3>Notification</h3>
            <P>
              If the decision is notified about by letter, the party in question
              (the child’s parent or guardian) is considered to have received
              it, unless can otherwise be shown, seven days after it was sent.
            </P>
            <P>
              When the standard electronic notification is used, the party is
              considered to have received it on the third day after it having
              been sent.
            </P>
            <P>
              If the decision is delivered in person, the party (the child’s
              parent or guardian) is considered to have received it on the day
              when it was handed over to the party of their legal
              representative.
            </P>
            <P>
              An advice of delivery letter is considered to have been received
              at the time indicated in the advice notice.
            </P>
            <P>
              The date of the advice of delivery letter is not included in the
              appeal period. If the last date of the appeal period is a public
              holiday, Finnish Independence Day, May Day, Christmas or Midsummer
              Eve or any Saturday, the appeal may be filed on the first business
              day following it.
            </P>
            <H3>Content of the appeal</H3>
            <P>
              An appeal must be submitted in writing. An electronic document
              fulfils this requirement.
            </P>
            <P>The appeal must include</P>
            <ul>
              <li>the decision that is appealed against</li>
              <li>
                which part of the decision is objected to and how you propose it
                should be rectified
              </li>
              <li>grounds for the appeal</li>
            </ul>
            <P>
              The appeal must include the name and home municipality of the
              person filing the appeal. If the right of action of the person
              filing the appeal is exercised by their legal representative or
              proxy, or if the appeal has been made by some other person, this
              person’s name and home municipality must be included in the
              appeal.
            </P>
            <P>
              The appeal must also include the relevant postal address,
              telephone number and any other necessary contact details. If the
              decision by the authority responding to the appeal may be sent as
              an electronic message, please also include the relevant email
              address.
            </P>
            <P>
              The person filing the appeal or their legal representative or
              proxy must sign the appeal. However, an electronic document does
              not have to be signed if the document includes details of the
              sender and there is no reason to doubt the authenticity or
              integrity of the document.
            </P>
            <P>The following must be appended</P>
            <ul>
              <li>
                the decision that is being appealed against, either the original
                or a copy if itproof of the date when the decision was issued,
                or otherdocumentation to indicate when the appeal period began
              </li>
              <li>
                documents to which the person filing the appeal bases the
                appeal, unless they have already been submitted to the
                authority.
              </li>
            </ul>
            <H3>Sending the appeal</H3>
            <P>
              The appeal must be sent within the appeal period to the Regional
              State Administrative Agency for Southwestern Finland, address:
            </P>
            <P>
              Lounais-Suomen aluehallintovirasto
              <br />
              PL 4, 13035 AVI
              <br />
              Itsenäisyydenaukio 2, 20800 Turku
              <br />
              Email: kirjaamo.lounais@avi.fi
              <br />
              Tel: +358 295 018 000
              <br />
              The registry’s opening hours: 8.00 am to 4.15 pm
              <br />
              Fax +358 2 2511 820
            </P>
            <P>
              You may also send your appeal by post or by courier at your own
              risk. If sending by post, the documents must be sent early enough
              to reach the office during opening hours on the final day of the
              appeal period.
            </P>
            <P>
              You can submit, at your own risk, your appeal before the end of
              the appeal period as a telecopy/telefax or email. Documents must
              be submitted by the deadline so that the document is available on
              the device or information system used by the authority.
            </P>
          </>
        )
      }
    },
    assistancePreschoolDecisions: {
      jurisdiction: '',
      jurisdictionText: '',
      appealInstructions: (
        <>
          <H3>Appeal instructions</H3>
          <P>
            You may file an appeal with the Regional State Administrative Agency
            for Southwestern Finland against the above decision within 14 days
            of having been notified of it. The decision cannot be appealed by
            taking the matter to a court of law.
          </P>
          <P>An appeal may be filed</P>
          <ul>
            <li>by the party that is affected by it </li>
            <li>
              {' '}
              or whose right, duty or interest the decision directly affects.
            </li>
          </ul>
          <h3>Notification</h3>
          <P>
            If the decision is notified about by letter, the party in question
            (the child’s parent or guardian) is considered to have received it,
            unless can otherwise be shown, seven days after it was sent.
          </P>
          <P>
            When the standard electronic notification is used, the party is
            considered to have received it on the third day after it having been
            sent.
          </P>
          <P>
            If the decision is delivered in person, the party (the child’s
            parent or guardian) is considered to have received it on the day
            when it was handed over to the party of their legal representative.
          </P>
          <P>
            An advice of delivery letter is considered to have been received at
            the time indicated in the advice notice.
          </P>
          <P>
            The date of the advice of delivery letter is not included in the
            appeal period. If the last date of the appeal period is a public
            holiday, Finnish Independence Day, May Day, Christmas or Midsummer
            Eve or any Saturday, the appeal may be filed on the first business
            day following it.
          </P>
          <H3>Content of the appeal</H3>
          <P>
            An appeal must be submitted in writing. An electronic document
            fulfils this requirement.
          </P>
          <P>The appeal must include</P>
          <ul>
            <li>the decision that is appealed against</li>
            <li>
              which part of the decision is objected to and how you propose it
              should be rectified
            </li>
            <li>grounds for the appeal</li>
          </ul>
          <P>
            The appeal must include the name and home municipality of the person
            filing the appeal. If the right of action of the person filing the
            appeal is exercised by their legal representative or proxy, or if
            the appeal has been made by some other person, this person’s name
            and home municipality must be included in the appeal.
          </P>
          <P>
            The appeal must also include the relevant postal address, telephone
            number and any other necessary contact details. If the decision by
            the authority responding to the appeal may be sent as an electronic
            message, please also include the relevant email address.
          </P>
          <P>
            The person filing the appeal or their legal representative or proxy
            must sign the appeal. However, an electronic document does not have
            to be signed if the document includes details of the sender and
            there is no reason to doubt the authenticity or integrity of the
            document.
          </P>
          <P>The following must be appended</P>
          <ul>
            <li>
              the decision that is being appealed against, either the original
              or a copy if itproof of the date when the decision was issued, or
              otherdocumentation to indicate when the appeal period began
            </li>
            <li>
              documents to which the person filing the appeal bases the appeal,
              unless they have already been submitted to the authority.
            </li>
          </ul>
          <H3>Sending the appeal</H3>
          <P>
            The appeal must be sent within the appeal period to the Regional
            State Administrative Agency for Southwestern Finland, address:
          </P>
          <P>
            Lounais-Suomen aluehallintovirasto
            <br />
            PL 4, 13035 AVI
            <br />
            Itsenäisyydenaukio 2, 20800 Turku
            <br />
            Email: kirjaamo.lounais@avi.fi
            <br />
            Tel: +358 295 018 000
            <br />
            The registry’s opening hours: 8.00 am to 4.15 pm
            <br />
            Fax +358 2 2511 820
          </P>
          <P>
            You may also send your appeal by post or by courier at your own
            risk. If sending by post, the documents must be sent early enough to
            reach the office during opening hours on the final day of the appeal
            period.
          </P>
          <P>
            You can submit, at your own risk, your appeal before the end of the
            appeal period as a telecopy/telefax or email. Documents must be
            submitted by the deadline so that the document is available on the
            device or information system used by the authority.
          </P>
        </>
      )
    },
    summary: 'This page displays the received decisions regarding your child.'
  },
  income: {
    description: (
      <>
        <p data-qa="income-description-p1">
          Use this page to send details about your income affecting the early
          childhood education and care fee. You can also view the details about
          your income and edit or remove them until the authority has processed
          them. Once the form has been processed, you can update your income
          details by submitting another form.
        </p>
        <p data-qa="income-description-p2">
          <strong>
            Both adults living in the household must submit their own income
            details.
          </strong>
        </p>
        <p data-qa="income-description-p3">
          Customer fees for municipal early childhood education and care is
          determined as a percentage of the family’s gross income. The fees vary
          depending on the size of the family, the family’s income level and the
          period early childhood education and care is required.
        </p>
        <p data-qa="income-description-p4">
          <a href="https://www.turku.fi/en/early-childhood-education-and-preschool-education/fees-financial-support-and-service-vouchers">
            More information about customer fees
          </a>
        </p>
      </>
    ),
    incomeType: {
      startDate: 'Start date',
      endDate: 'End date',
      title: 'Basis for customer ee',
      agreeToHighestFee:
        'I agree to pay the highest early childhood education and care fee',
      highestFeeInfo:
        'The highest fee for the service required is valid until further notice or until my child’s early childhood education and care ends. (No need to submit income details)',
      grossIncome: 'Fee determined by gross income'
    },
    grossIncome: {
      title: 'Entering gross income',
      description: (
        <>
          <P> </P>
        </>
      ),
      incomeSource: 'Income data submitted',
      incomesRegisterConsent:
        'I consent to my income-related information being reviewed from the income register and I will provide any benefit information as attachments.',
      provideAttachments: 'I will submit my income details in an attachment',
      estimate: 'Estimate of my gross income',
      estimatedMonthlyIncome: 'Average monthly income including holiday pay, €',
      otherIncome: 'Other income',
      otherIncomeDescription:
        'If you have other income, you must attach documentation on them. For a list of any necessary attachments, see at the bottom of the form under: Attachments related to income and early childhood education and care fees.',
      choosePlaceholder: 'Select',
      otherIncomeTypes: {
        PENSION: 'Pension',
        ADULT_EDUCATION_ALLOWANCE: 'Adult education allowance',
        SICKNESS_ALLOWANCE: 'Sickness allowance',
        PARENTAL_ALLOWANCE: 'Parental allowance',
        HOME_CARE_ALLOWANCE: 'Home care allowance',
        FLEXIBLE_AND_PARTIAL_HOME_CARE_ALLOWANCE:
          'Flexible and partial home care allowance',
        ALIMONY: 'Alimony',
        INTEREST_AND_INVESTMENT_INCOME: 'Interest and investment income',
        RENTAL_INCOME: 'Rental income',
        UNEMPLOYMENT_ALLOWANCE: 'Unemployment allowance',
        LABOUR_MARKET_SUBSIDY: 'Labour market subsidy',
        ADJUSTED_DAILY_ALLOWANCE: 'Adjusted daily allowance',
        JOB_ALTERNATION_COMPENSATION: 'Job alternation compensation',
        REWARD_OR_BONUS: 'Reward or bonus',
        RELATIVE_CARE_SUPPORT: 'Relative care support',
        BASIC_INCOME: 'Basic income',
        FOREST_INCOME: 'Forest income',
        FAMILY_CARE_COMPENSATION: 'Family care compensation',
        REHABILITATION: 'Rehabilitation',
        EDUCATION_ALLOWANCE: 'Education allowance',
        GRANT: 'Grant',
        APPRENTICESHIP_SALARY: 'Apprenticeship salary',
        ACCIDENT_INSURANCE_COMPENSATION: 'Accident insurance compensation',
        OTHER_INCOME: 'Other income'
      },
      otherIncomeInfoLabel: 'Estimate of other income',
      otherIncomeInfoDescription:
        'Enter here an estimate of other monthly income, €, e.g. "Rental income150, child home care allowance 300"'
    },
    entrepreneurIncome: {
      title: 'Entering entrepreneur’s income details',
      description:
        'If necessary, you can fill in the information for more than one company by ticking the boxes that apply to all of your companies.',
      startOfEntrepreneurship: 'Entrepreneurship began',
      spouseWorksInCompany: 'Does your spouse work in the company?',
      yes: 'Yes',
      no: 'No',
      startupGrantLabel: 'Does your company receive a startup grant?',
      startupGrant:
        'My company receives a startup grant. I will attach the startup grant decision.',
      checkupLabel: 'Checking of information',
      checkupConsent:
        'I accept that my income details will be checked if necessary with the Incomes Register and the Social Insurance Institution of Finland (Kela).',
      companyInfo: 'Company details',
      companyForm: 'Type of business',
      selfEmployed: 'Self-employed',
      limitedCompany: 'Limited company',
      partnership: 'Partnership',
      lightEntrepreneur: 'Light entrepreneurship',
      lightEntrepreneurInfo:
        'Receipts for pay and remuneration must be attached.',
      partnershipInfo: ''
    },
    moreInfo: {
      title: 'Other details related to the fee',
      studentLabel: 'Are you a student?',
      student: 'I am a student?',
      studentInfo:
        'Students must submit a certificate of student status and a decision on study benefit.',
      deductions: 'Deductions',
      alimony: 'I pay child support. I will attach payment receipts.',
      otherInfoLabel: 'Additional information about income details'
    },
    attachments: {
      title:
        'Attachments related to income and early childhood education and care fees',
      description:
        'Please submit here any attachments concerning income or early childhood education and care. No attachments are necessary if your family has agreed to pay the highest fee.',
      required: {
        title: 'Necessary attachments'
      },
      attachmentNames: {
        PENSION: 'Pension decision',
        ADULT_EDUCATION_ALLOWANCE: 'Decision on adult education allowance',
        SICKNESS_ALLOWANCE: 'Decision on sickness allowance',
        PARENTAL_ALLOWANCE: 'Decision on maternity or parental allowance',
        HOME_CARE_ALLOWANCE: 'Decision on home care allowance',
        FLEXIBLE_AND_PARTIAL_HOME_CARE_ALLOWANCE:
          'Decision on home care allowance',
        ALIMONY: 'Child support agreement or decision on child support',
        UNEMPLOYMENT_ALLOWANCE: 'Decision on unemployment allowance',
        LABOUR_MARKET_SUBSIDY: 'Decision on labour market subsidy',
        ADJUSTED_DAILY_ALLOWANCE: 'Decision on daily allowance',
        JOB_ALTERNATION_COMPENSATION:
          'Receipt for job alternation compensation',
        REWARD_OR_BONUS: 'Pay receipt for bonus or reward',
        RELATIVE_CARE_SUPPORT: 'Decision on informal care support',
        BASIC_INCOME: 'Decision on basic income',
        FOREST_INCOME: 'Receipt for forest income',
        FAMILY_CARE_COMPENSATION: 'Receipts for family care compensation',
        REHABILITATION: 'Decision on rehabilitation benefit or allowance',
        EDUCATION_ALLOWANCE: 'Decision on training allowance',
        GRANT: 'Receipt for grant',
        APPRENTICESHIP_SALARY: 'Receipt for apprenticeship salary',
        ACCIDENT_INSURANCE_COMPENSATION:
          'Receipt for accident insurance compensation',
        OTHER_INCOME: 'Attachments for other income',
        ALIMONY_PAYOUT: 'Receipt of child support paid',
        INTEREST_AND_INVESTMENT_INCOME:
          'Receipts for interest and dividend income',
        RENTAL_INCOME: 'Receipts for rental income and maintenance charge',
        PAYSLIP_GROSS: 'Latest payslip',
        PAYSLIP_LLC: 'Latest payslip',
        STARTUP_GRANT: 'Start-up grant decision',
        ACCOUNTANT_REPORT_PARTNERSHIP:
          'Accountant’s account salary and fringe benefits',
        ACCOUNTANT_REPORT_LLC:
          'Accountant’s report on fringe benefits and dividends',
        PROFIT_AND_LOSS_STATEMENT_SELF_EMPLOYED:
          'Profit and loss account or taxation decision',
        PROFIT_AND_LOSS_STATEMENT_PARTNERSHIP:
          'Profit and loss account and balance sheet',
        SALARY: 'Receipts for pay and remuneration',
        PROOF_OF_STUDIES:
          'Certificate of student status or decision by an unemployment fund’s study benefit / an employment fund’s education allowance',
        CHILD_INCOME: 'Receipts for child income'
      }
    },
    selfEmployed: {
      info: '',
      attachments:
        'I am submitting as an attachment the company’s latest profit and loss statement and balance sheet or tax decision.',
      estimatedIncome:
        'I am a new entrepreneur. I will enter an estimate of my average monthly income. I will submit my profit and loss statement and balance sheet as soon as possible.',
      estimatedMonthlyIncome: 'Average monthly income (EUR)',
      timeRange: 'Time range'
    },
    formDescription: (
      <>
        <P data-qa="income-formDescription-p1">
          You must report your income within a month after your child began
          receiving early childhood education and care. If you submit incomplete
          details, the fee can be set at the maximum amount. Incomplete income
          details will not be corrected retroactively after the period for
          rectifying them has elapsed.
        </P>
        <P>
          The customer fee is charged from the start date for the early
          childhood education and care concerning the decision.
        </P>
        <P>
          The customer must inform, without delay, of any changes to their
          income or family size that may affect the fee. The authority has the
          right to demand early childhood education and care fees in arrears.
        </P>
        <P>
          <strong>Note:</strong>
        </P>
        <Gap $size="xs" />
        <UnorderedList data-qa="income-formDescription-ul">
          <li>
            If your income exceeds the income threshold for your family size,
            please accept the highest fee for early childhood education and
            care. This means that you do not have to report on your income at
            all.
          </li>
          <li>
            If your family includes another adult, they must also report their
            income by logging into eVaka with their personal details, and fill
            in this form.
          </li>
          <li>
            See the current income thresholds{' '}
            <a
              target="_blank"
              rel="noreferrer"
              href="https://www.turku.fi/en/early-childhood-education-and-preschool-education/fees-financial-support-and-service-vouchers"
            >
              here
            </a>
            .
          </li>
        </UnorderedList>
        <P>* Fields indicated by an asterisk are required.</P>
      </>
    )
  },
  accessibilityStatement: (
    <>
      <H1>Accessibility statement</H1>
      <P>
        This accessibility statement concerns the City of Turku’s eVaka online
        service for early childhood education and care at{' '}
        <a href="https://evaka.turku.fi">evaka.turku.fi</a>. The City of Turku
        aims to ensure the accessibility of the online service, to continuously
        improve the user experience and to apply the appropriate accessibility
        standards.
      </P>
      <P>
        The accessibility of the service has been assessed by a service
        development team, and the statement was drawn up in 12 April 2022.
      </P>
      <H2>Compliance of service with requirements</H2>
      <P>
        The online service fulfils the critical accessibility requirements
        required by law in accordance with level AA of the Web Content
        Accessibility Guidelines, version 2.1. The service is not yet fully
        compliant with the requirements.
      </P>
      <H2>Actions to support accessibility</H2>
      <P>
        Accessibility of the online service is ensured with measures including
        the following, for example:
      </P>
      <ul>
        <li>
          Accessibility is taken account of from the beginning in the planning
          stage, such as by selecting suitable colours and font sizes.
        </li>
        <li>
          Elements of the service are defined to be semantically consistent.
        </li>
        <li>The service is continuously tested with a screen reader.</li>
        <li>
          Different kind of users test the service and give feedback on its
          accessibility.
        </li>
        <li>
          The accessibility of the website is taken care of through continuous
          monitoring as the technology and content changes.
        </li>
      </ul>
      <P>
        This statement is updated when changes are made to the website and
        accessibility is monitored.
      </P>
      <H2>Known accessibility problems</H2>
      <P>
        Users may still encounter some accessibility problems on the website.
        The following is a description of known accessibility problems. If you
        encounter a problem that is not listed below, please get in touch with
        us.
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
        The online service uses the following third-party services for which we
        cannot guarantee as to their accessibility.
      </P>
      <ul>
        <li>Suomi.fi authorisation</li>
        <li>Leaflet map service</li>
      </ul>
      <H2>Alternative methods to get things done</H2>
      <P>
        <ExternalLink
          href="https://www.turku.fi/varhaiskasvatus-ja-esiopetus"
          text="City of Turku service points"
        />{' '}
        can help you with e-services. Go to the service points if you do not
        have access to digital services.
      </P>
      <H2>Give feedback</H2>
      <P>
        If you detect an accessibility problem in our online service, please
        contact us. You can give us feedback at{' '}
        <ExternalLink
          href="https://opaskartta.turku.fi/eFeedback/en"
          text="using on online form"
        />{' '}
        or my email{' '}
        <a href="mailto:varhaiskasvatus@turku.fi">varhaiskasvatus@turku.fi</a>.
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
        <strong>Contact details of the supervisory authority</strong>
        <br />
        Finnish Transport and Communications Agency Traficom <br />
        Digital Accessibility Supervision Unit
        <br />
        <ExternalLink
          href="https://saavutettavuusvaatimukset.fi/en/"
          text="Web accessibility"
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

export default en
