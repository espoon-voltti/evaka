// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import ExternalLink from 'lib-components/atoms/ExternalLink'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import { H1, H2, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import type { Translations } from 'lib-customizations/citizen'
import type { DeepPartial } from 'lib-customizations/types'

import {
  preschoolEnabled,
  serviceApplicationsEnabled
} from './fiCustomizations'

const customerContactText = function () {
  return (
    <>
      <a href="mailto:varhaiskasvatus.palveluohjaus@nokiankaupunki.fi">
        varhaiskasvatus.palveluohjaus@nokiankaupunki.fi
      </a>
      , tel 040 483 7145 on Mondays, Wednesdays and Thursdays 10-12
    </>
  )
}

const en: DeepPartial<Translations> = {
  applications: {
    creation: {
      daycareInfo:
        'An applicant for early childhood education applies for a place in a municipal day care centre or family day care, an outsourced service day care centre or a day care centre supported by a service voucher.',
      clubInfo:
        'With a club application one may apply for a place at municipal clubs or clubs supported by a service voucher.',
      preschoolLabel: 'Enrolment in pre-primary education',
      preschoolInfo:
        'There are four hours of free pre-school education per day. In addition, supplementary activities, for which a fee is charged, can be applied for the child. An application for supplementary activities can be made when enrolling for pre-school education or as a separate application once education has started. The unit for supplementary activities cannot be selected in the application as it is always determined by the preschool unit. If a place in supplementary activities is applied for later, the need for pre-primary education will also be entered in the application.',
      preschoolDaycareInfo:
        'Applying for supplementary activities for children who are enrolled / have been enrolled in preschool',
      applicationInfo: (
        <P>
          The custodian can make amendments to the application on the web up
          until the moment that the application is accepted for processing by
          the customer service. After this, amendments or cancellation of the
          application are possible by getting in contact with the{' '}
          {customerContactText()}
        </P>
      ),
      duplicateWarning:
        'The child already has a similar unfinished application. Please return to the Applications view and complete the existing application or contact the customer service of the Early childhood education.',
      transferApplicationInfo: {
        DAYCARE:
          'The child already has a place in early childhood education in Nokia. With this application you can apply for a transfer to another unit offering early childhood education in Nokia.',
        PRESCHOOL: (
          <span>
            The child already has a pre-school place. With this application, you
            can apply for activities that supplement preschool education or
            transfer to another unit that offers preschool education.
          </span>
        )
      }
    },
    editor: {
      actions: {
        allowOtherGuardianAccess: (
          <span>
            I understand that the application will also be visible to the other
            guardian. If the other guardian should not be able to see this
            application, please contact {customerContactText()}
          </span>
        )
      },
      verification: {
        serviceNeed: {
          connectedDaycare: {
            label: 'Activities supplementing pre-primary education',
            withConnectedDaycare:
              'I am applying for a supplementary activity to pre-school education.'
          },
          startDate: {
            title: {
              PRESCHOOL: 'Start of pre-primary education'
            }
          }
        },
        unitPreference: {
          siblingBasis: {
            unit: "Sibling's school"
          }
        }
      },
      unitPreference: {
        siblingBasis: {
          checkbox: {
            PRESCHOOL:
              "It is our greatest wish that the child is allocated in pre-primary education to the same school as the child's older sibling."
          },
          info: {
            PRESCHOOL: null
          },
          unit: "Sibling's school",
          unitPlaceholder: 'The name of the school'
        },
        units: {
          info: {
            PRESCHOOL: (
              <>
                <P>
                  The pre-school place is determined by the child&apos;s home
                  address, in accordance with the future school path. At the
                  time of enrolment, the child is enrolled in a pre-school or
                  weighted pre-school according to the school path. If the child
                  needs regular evening or shift care, he or she is enrolled in
                  evening or shift care in early childhood education.
                </P>
                <P>
                  Information about the future pre-school place is communicated
                  to the custodians electronically via the Suomi.fi message
                  service. If the custodian has not registered for the service,
                  the information will be sent to them by letter.
                </P>
              </>
            )
          },
          serviceVoucherLink:
            'https://www.nokiankaupunki.fi/varhaiskasvatus-ja-koulutus/varhaiskasvatuspalvelut/varhaiskasvatuksen-maksut-tuet-palveluseteli/palveluseteli/'
        }
      },
      heading: {
        info: {
          DAYCARE: (
            <>
              <P>
                An early child education place may be applied for all year
                round. The application for early childhood education must be
                submitted no later than four months prior to the desired start
                date. If the need for early childhood education is due to
                employment, studies or training, and it has not been possible to
                anticipate the need for care, an early childhood education place
                must be sought as soon as possible – however, no later than two
                weeks before the child needs the place.
              </P>
              <P>
                A written decision on the early childhood education place will
                be sent to the Suomi.fi Messages service. If you wish to be
                notified of the decision electronically, you will need to
                activate the Suomi.fi Messages service. Further information on
                the service and its activation is available at{' '}
                <ExternalLink
                  text="https://www.suomi.fi/messages"
                  href="https://www.suomi.fi/messages"
                  newTab
                />
                . If you do not activate the Suomi.fi Messages service, the
                decision will be sent to you by post.
              </P>
              <P $fitted={true}>
                * The information denoted with an asterisk is mandatory.
              </P>
            </>
          ),
          PRESCHOOL: (
            <>
              <P>
                In accordance with the Basic Education Act (Section 26 a §),
                children must participate in pre-primary education or other
                activities that achieve the goals of pre-primary education in
                the year preceding the commencement of compulsory education.
                Pre-primary education is free of charge.
              </P>
              <P>
                The decisions will be sent to the{' '}
                <a
                  href="https://www.suomi.fi/viestit"
                  target="_blank"
                  rel="noreferrer"
                >
                  Suomi.fi Messages
                </a>{' '}
                service or by post, if the applicant does not use the Suomi.fi
                Messages service.
              </P>
              <P $fitted={true}>
                * The information denoted with an asterisk is mandatory.
              </P>
            </>
          ),
          CLUB: (
            <>
              <P>
                A place at a club can be applied for all year round. A municipal
                place in a club or one supported with a service voucher can be
                applied for with a club application. A written confirmation of a
                place in a club will be sent to the Suomi.fi Messages service.
                If you wish to have the notice in electronic form, you must
                activate the Suomi.fi Messages service. Further information on
                the service and its activation is available at{' '}
                <ExternalLink
                  text="https://www.suomi.fi/messages"
                  href="https://www.suomi.fi/messages"
                  newTab
                />
                . If you do not activate the suomi.fi/messages service, the
                notice of the place at the club will be sent to you by post. A
                place is granted for one administrative period at a time.
              </P>
              <P>
                The club application is for one such period. Once the period in
                question ends, the application is removed from the system.
              </P>
            </>
          )
        }
      },
      serviceNeed: {
        startDate: {
          instructions: {
            DAYCARE: (
              <>
                It is possible to postpone the preferred starting day as long as
                the application has not been processed by the customer service.
                After this, any desired amendments can be made by contacting the{' '}
                {customerContactText()}
              </>
            ),
            PRESCHOOL: null
          },
          info: {
            PRESCHOOL: [
              'The school year 2025-2026 starts on Wednesday 6.8.2025.'
            ]
          },
          label: {
            PRESCHOOL: 'The need starting from'
          }
        },
        clubDetails: {
          wasOnDaycareInfo:
            'If a child has been in municipal day care or family care or they give up their place when the club starts, they have a greater chance to obtain the place in the club.',
          wasOnClubCareInfo:
            'If the child has been in the club already during the previous period, they have a greater chance also to obtain a place from the club during the forthcoming period.'
        },
        urgent: {
          attachmentsMessage: {
            text: (
              <P $fitted={true}>
                If the need for an early child education place is due to sudden
                employment or obtaining a study place, the early childhood
                education place must be sought no later than two weeks before
                the need for care starts. Furthermore, the custodian must make
                contact, without delay, with the {customerContactText()}
              </P>
            )
          }
        },
        shiftCare: {
          instructions:
            "If a child needs evening or day care in addition to pre-school education, he or she must be enrolled in evening or day care pre-school education. In addition, the child must be enrolled in supplementary early childhood education and care, for more than 5 hours per day. The day care centres are open normally on weekdays, from 6 am to 6 pm. Evening care is for children who, due to their parents' work or studies leading to a degree, require regular care after 6 pm. Day care centres offering evening care open at 5.30 am if necessary and close at 10.30 pm at the latest. Some day care centres providing evening care are also open at weekends. Shift care is for children whose parents work shifts and whose care also includes nights.",
          attachmentsMessage: {
            DAYCARE: (
              <P>
                Evening and shift care is intended for those children who, due
                to the parents’ work or studies that lead to a qualification,
                require evening and shift care. In the case of the parents, an
                employer’s certificate of a need for evening or shift care due
                to shift work or study must be attached to the application.
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
                  Evening and shift care is intended for those children who, due
                  to the parents’ work or studies that lead to a qualification,
                  require evening and shift care. In the case of the parents, an
                  employer’s certificate of a need for evening or shift care due
                  to shift work or study must be attached to the application.
                </P>
              </>
            )
          }
        },
        assistanceNeedInstructions: {
          DAYCARE:
            'Intensified or special care is given to a child as soon as the need arises. If a child has received an expert opinion backing the need for support, this must be notified in the early childhood education application. The support measures are carried out in the child’s daily life as part of the early childhood educational activities. Nokia’s early childhood education will separately be in contact after the application has been submitted, if the child has a need for support. The additional information given here will be visible to early childhood education service employees and special early childhood education teachers for the purposes of arranging an early childhood education placement.',
          CLUB: 'If the child has a need for support, the staff of Nokia’s early childhood education will get in contact the application has been submitted.',
          PRESCHOOL:
            "Select this box on the application form, if your child needs support for his/her development and/or learning during the pre-school year. This support is provided in the child's everyday life as a part of pre-school activities. Some places in pre-school are reserved for children who need support. If your child has developmental and/or learning support needs, you will be contacted by the special needs teacher for early childhood education, so that your child's needs can be taken into account when allocating a pre-school place. The additional information given here will be visible to early childhood education service employees and special early childhood education teachers for the purposes of arranging an early childhood education placement."
        },
        partTime: {
          true: 'Part-time'
        },
        dailyTime: {
          label: {
            DAYCARE: 'Service options',
            PRESCHOOL: 'Activities supplementing pre-primary education'
          },
          connectedDaycare:
            'I am applying for a supplementary activity to pre-school education.',
          connectedDaycareInfo: (
            <P>
              Pre-school education lasts for four hours a day, generally from 9
              am to 1 pm. In addition, the child can participate in paid
              supplementary activities in the mornings and in the afternoons.
              Options for supplementary activities are supplementary early
              childhood education in day care centres and pre-school clubs in
              schools.
            </P>
          )
        }
      },
      contactInfo: {
        info: (
          <P data-qa="contact-info-text">
            The personal data have been retrieved from the population data and
            cannot be changed with this application. If there are any errors in
            the personal data, please update the information on the website of
            the{' '}
            <ExternalLink
              text="Digital and Population Data Services Agency"
              href="https://dvv.fi/en/individuals"
              newTab
            />
            . If your address is about to change, you can add the future address
            in a separate box on the application form; add the future address
            both for the child and the custodian. The address information is
            only considered official once it has been updated in the population
            data system. Decisions on early childhood education and care,
            service vouchers{preschoolEnabled ? ', pre-school education' : ''}{' '}
            and information on open early childhood education places are also
            automatically sent to the custodian at a different address found in
            the population data.
          </P>
        ),
        futureAddressInfo: `For early childhood education and pre-school education in Nokia, the official address is the address obtained from population data. The address in the population data register changes when the applicant submits a notification of change of address to the post office or the local register office.`
      },
      fee: {
        info: {
          DAYCARE: (
            <P>
              The client fees for municipal early childhood education and the
              own deductible part of the service voucher are based on the Act on
              Client Fees in Early Childhood Education and Care (1503/2016). The
              client fee is determined by the size of the family, the need for
              service as well the gross income. New clients must fill in the
              client fee form and submit the required appendices to the Client
              fees of Early childhood education within a month from when the
              care started at the latest.
            </P>
          ),
          PRESCHOOL: (
            <P>
              TODO: Esiopetus on maksutonta, mutta sitä täydentävä toiminta on
              maksullista. Jos lapsi osallistuu esiopetusta täydentävään
              toimintaan, perhe toimittaa tuloselvityksen bruttotuloistaan
              tuloselvityslomakkeella viimeistään kahden viikon kuluessa siitä,
              kun lapsi on aloittanut esiopetuksen.
            </P>
          )
        },
        links: (
          <P>
            You will find further information on client fees for early childhood
            education on{' '}
            <ExternalLink
              href="https://www.nokiankaupunki.fi/varhaiskasvatus-ja-koulutus/varhaiskasvatuspalvelut/varhaiskasvatuksen-maksut-tuet-palveluseteli/varhaiskasvatuksen-asiakasmaksut-2/"
              text="the website of the City of Nokia"
              newTab
            />
          </P>
        )
      },
      additionalDetails: {
        dietInfo: (
          <>
            In the case of a special diet, the custodian submits to the early
            childhood education
            {preschoolEnabled ? ' or pre-school education centre' : ''} a{' '}
            <ExternalLink
              href="https://www.nokiankaupunki.fi/kaupunki-ja-hallinto/organisaatio/palvelualueet/ruoka-ja-siivouspalvelut/erityisruokavaliot/"
              text="Notification of Special Diet form"
              newTab
            />
            , completed and signed by a doctor or nutritionist, which is valid
            for a limited period of time.
          </>
        )
      }
    }
  },
  applicationsList: {
    title: `Applying for early childhood education${
      preschoolEnabled ? ' and pre-primary education' : ''
    }`,
    summary: (
      <P $width="800px">
        The child&apos;s custodian can apply for early childhood education
        {preschoolEnabled ? ', pre-primary education' : ''} and a club for the
        child. Information about the custodian&apos;s children is automatically
        retrieved from the Population data register for this view.
      </P>
    )
  },
  children: {
    pageDescription:
      "General information related to your children's early childhood education is displayed on this page."
  },
  footer: {
    cityLabel: '© City of Nokia',
    privacyPolicyLink: (
      <ExternalLink
        href="https://www.nokiankaupunki.fi/varhaiskasvatus-ja-koulutus/varhaiskasvatuspalvelut/varhaiskasvatukseen-hakeminen-ja-irtisanominen/evaka/#5e6b46cb"
        text="Privacy Notices"
        newTab={true}
        data-qa="footer-policy-link"
      />
    ),
    sendFeedbackLink: (
      <ExternalLink
        href="https://www.nokiankaupunki.fi/kaupunki-ja-hallinto/osallistu-ja-vaikuta/palaute/#5e6b46cb"
        text="Give feedback"
        newTab={true}
        data-qa="footer-feedback-link"
      />
    )
  },
  loginPage: {
    applying: {
      infoBullets: [
        `apply for an early childhood${
          preschoolEnabled ? ', pre-primary' : ''
        } or club place for your child or view a previous application`,
        `view pictures and other documents related to your child’s early childhood${
          preschoolEnabled ? ' and pre-primary' : ''
        }`,
        'report your or your child’s income information',
        `accept your child’s early childhood${
          preschoolEnabled ? ', pre-primary' : ''
        } or club place`,
        serviceApplicationsEnabled
          ? 'change your child’s need for services'
          : '',
        'terminate your child’s early childhood or club place.'
      ].filter((s) => s.length > 0)
    },
    login: {
      infoBoxText: (
        <>
          <P>
            If you are not able to log in here, see the instructions{' '}
            <a
              href="https://www.nokiankaupunki.fi/varhaiskasvatus-ja-koulutus/varhaiskasvatuspalvelut/varhaiskasvatukseen-hakeminen-ja-irtisanominen/evaka/"
              target="_blank"
              rel="noreferrer"
            >
              eVaka | City of Nokia
            </a>
            . You can also log in with strong authentication.
          </P>
        </>
      ),
      paragraph: `Guardians whose child is already in early childhood education${
        preschoolEnabled ? ' or preschool' : ''
      }: take care of your child's daily affairs, such as reading messages and reporting the child's attendance and absence times.`
    },
    title: `City of Nokia early childhood${
      preschoolEnabled ? ' and pre-primary' : ''
    } education`
  },
  map: {
    mainInfo: `In this view you can locate on the map all of Nokia’s early childhood education units and clubs. Regional service voucher units and clubs can also be found on the map.${
      preschoolEnabled
        ? ' Pre-primary education is mainly organized in schools.'
        : ''
    }`,
    privateUnitInfo: <></>,
    serviceVoucherLink:
      'https://www.nokiankaupunki.fi/varhaiskasvatus-ja-koulutus/varhaiskasvatuspalvelut/varhaiskasvatuksen-maksut-tuet-palveluseteli/palveluseteli/',
    searchPlaceholder: 'E.g. Amurin päiväkoti'
  },
  decisions: {
    summary: 'This page displays the received decisions regarding your child.',
    applicationDecisions: {
      type: {
        PRESCHOOL_DAYCARE: 'Supplementary early childhood education',
        PRESCHOOL_CLUB: 'Pre-school club'
      },
      summary:
        'The place indicated in the decision must be received or rejected without delay, no later than two weeks after the decision has been notified.',
      warnings: {
        doubleRejectWarning: {
          text: 'You are rejecting an offer on pre-primary placement. The activities supplementing pre-primary education placement will also be rejected.'
        }
      },
      response: {
        disabledInfo:
          'N.B! You will be able to receive / reject the place of activities that complement preschool education only after you have received a place in preschool education.'
      }
    }
  },
  placement: {
    type: {
      PRESCHOOL_DAYCARE:
        'Supplementary early childhood education in day care centre',
      PRESCHOOL_CLUB: 'Pre-school clubs in school from 7 am to 5 pm',
      PRESCHOOL_WITH_DAYCARE:
        'Supplementary early childhood education in day care centre'
    }
  },
  income: {
    description: (
      <>
        <p data-qa="income-description-p1">
          On this page, you can submit statements on your earnings that affect
          the early childhood education fee. You can also view, edit, or delete
          income statements that you have submitted until the authority has
          processed the information. After the form has been processed, you can
          update your income information by submitting a new form.
        </p>
        <p>
          <strong>
            Both adults living in the same household must submit their own
            separate income statements.
          </strong>
        </p>
        <p data-qa="income-description-p2">
          The client fees for municipal early childhood education are determined
          as a percentage of the family’s gross income. The fees vary according
          to family size, income and time in early childhood education.
        </p>
        <p>
          <a href="https://www.nokiankaupunki.fi/varhaiskasvatus-ja-koulutus/varhaiskasvatuspalvelut/varhaiskasvatuksen-maksut-tuet-palveluseteli/varhaiskasvatuksen-asiakasmaksut-2/">
            Further information on client fees.
          </a>
        </p>
      </>
    ),
    formDescription: (
      <>
        <P data-qa="income-formDescription-p1">
          The income statement including attachments must be submitted by the
          end of the calendar month in which the child started early childhood
          education. In case of incomplete income information, the highest fee
          may be charged.
        </P>
        <P>
          The client fee is charged from the first day of early education in
          accordance with the decision.
        </P>
        <P>
          The client must immediately inform the client fees for Early childhood
          education of changes in income and family size.{' '}
        </P>
        <P>
          <strong>To be noted:</strong>
        </P>
        <Gap $size="xs" />
        <UnorderedList>
          <li>
            If your income exceeds the highest payment income threshold
            according to family size, accept the highest early childhood
            education fee. In this case, you do not need to submit an income
            statement.
          </li>
          <li>
            If there&apos;s another adult in your family, they must also submit
            an income statement by personally logging into eVaka and filling out
            this form.
          </li>
          <li>
            See current income thresholds{' '}
            <a
              target="_blank"
              rel="noreferrer"
              href="https://www.nokiankaupunki.fi/varhaiskasvatus-ja-koulutus/varhaiskasvatuspalvelut/varhaiskasvatuksen-maksut-tuet-palveluseteli/varhaiskasvatuksen-asiakasmaksut-2/"
            >
              hare
            </a>
            .
          </li>
        </UnorderedList>
        <P>* The information denoted with an asterisk is mandatory.</P>
      </>
    )
  },
  calendar: {
    absenceModal: {
      absenceTypes: {
        PLANNED_ABSENCE: 'Planned absence'
      },
      selectChildrenInfo:
        'Only report full day absences. Part-day absences can be reported when making a reservation.'
    },
    absences: {
      PLANNED_ABSENCE: 'Planned absence'
    },
    absentPlanned: 'Planned absence'
  },
  accessibilityStatement: (
    <>
      <H1>Accessibility statement</H1>
      <P>
        This accessibility statement applies to the City of Nokia’s early
        childhood education online service eVaka at{' '}
        <a href="https://evaka.nokiankaupunki.fi">evaka.nokiankaupunki.fi</a>.
        The City of Nokia endeavours to ensure the accessibility of the online
        service, continuously improve the user experience and apply appropriate
        accessibility standards.
      </P>
      <P>
        The accessibility of the service was assessed by the development team of
        the service, and this statement was drawn up on 12 April 2024.
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
          Navigating on the Messages page using the keyboard or screen reader
          still requires revision for part of moving and targeted elements.
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
        <strong>
          Early childhood and pre-primary education customer service
        </strong>
      </P>
      <P>
        Email: varhaiskasvatus.palveluohjaus@nokiankaupunki.fi
        <br />
        Phone: 040 483 7145
        <br />
        Phone time: Mon, Wed and Thu between 9.00–12.00
      </P>
      <H2>Give feedback</H2>
      <P>
        If you notice an accessibility gap in our online service, please let us
        know! You can give us feedback using the{' '}
        <ExternalLink
          href="https://www-nokiankaupunki-fi.suomiviestit.fi/suomi.fi/lomake/5f69acac475a6c5531b74150"
          text="Give accessibility feedback with this web form"
        />
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
        Finnish Transport and Communications Agency Traficom
        <br />
        Digital Accessibility Supervision Unit
        <br />
        <ExternalLink
          href="https://www.webaccessibility.fi/"
          text="https://www.webaccessibility.fi/"
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
