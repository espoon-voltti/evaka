// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Translations } from '.'

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
        CLUB: 'Kerho',
        FAMILY: 'Perhepäivähoito',
        CENTRE: 'Päiväkoti',
        GROUP_FAMILY: 'Ryhmäperhepäivähoito',
        PRESCHOOL: 'Esiopetus',
        PREPARATORY_EDUCATION: 'Valmistava opetus'
      },
      languages: {
        fi: 'suomenkielinen',
        sv: 'ruotsinkielinen'
      },
      languagesShort: {
        fi: 'suomi',
        sv: 'ruotsi'
      }
    },
    errors: {
      genericGetError: 'Error in fetching the requested information'
    }
  },
  header: {
    nav: {
      map: 'Map',
      applications: 'Applications',
      decisions: 'Decisions',
      newMap: 'New Map',
      newDecisions: 'New Decisions',
      newApplications: 'New Applications'
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
    espooLabel: '© City of Espoo',
    privacyPolicy: 'Privacy Notices',
    privacyPolicyLink:
      'https://www.espoo.fi/en-US/Eservices/Data_protection/Privacy_Notices',
    sendFeedback: 'Give feedback',
    sendFeedbackLink:
      'https://easiointi.espoo.fi/eFeedback/en/Feedback/20-S%C3%A4hk%C3%B6iset%20asiointipalvelut'
  },
  map: {
    title: 'Yksiköt kartalla',
    mainInfo:
      'Tässä näkymässä voit hakea kartalta Espoon varhaiskasvatus-, esiopetus- ja kerhopaikkoja.',
    searchLabel: 'Hae osoitteella tai yksikön nimellä',
    searchPlaceholder: 'Esim. Kilontie 3 tai Purolan päiväkoti',
    address: 'Osoite',
    noResults: 'Ei hakutuloksia',
    distanceWalking: 'Etäisyys valitusta osoitteesta kävellen',
    careType: 'Toimintamuoto',
    careTypePlural: 'Toimintamuodot',
    careTypes: {
      CLUB: 'Kerho',
      DAYCARE: 'Varhaiskasvatus',
      PRESCHOOL: 'Esiopetus'
    },
    language: 'Yksikön kieli',
    providerType: 'Palveluntarjoaja',
    providerTypes: {
      MUNICIPAL: 'kunnalliset',
      PURCHASED: 'ostopalvelu',
      PRIVATE: 'yksityiset',
      PRIVATE_SERVICE_VOUCHER: 'palveluseteli'
    },
    homepage: 'Kotisivu',
    unitHomepage: 'Yksikön kotisivu',
    route: 'Katso reitti yksiköön',
    routePlanner: 'Reittiopas',
    newTab: '(Avautuu uuteen välilehteen)',
    shiftCareTitle: 'Ilta- ja vuorohoito',
    shiftCareLabel: 'Näytä vain ilta- ja vuorohoitoyksiköt',
    shiftCareYes: 'Yksikkö tarjoaa ilta- ja/tai vuorohoitoa',
    shiftCareNo: 'Yksikkö ei tarjoa ilta- ja/tai vuorohoitoa',
    showMoreFilters: 'Näytä lisää suodattimia',
    showLessFilters: 'Näytä vähemmän suodattimia',
    nearestUnits: 'Lähimmät yksiköt',
    moreUnits: 'Lisää yksiköitä',
    showMore: 'Näytä lisää hakutuloksia',
    mobileTabs: {
      map: 'Kartta',
      list: 'Lista yksiköistä'
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
      daycareLabel: 'Application for early childhood education',
      daycareInfo:
        'The application for early childhood education is used to apply for a municipal early childhood education place at a day-care centre, family day-care provider and group family day-care provider.',
      preschoolLabel:
        'Enrolment to pre-primary education and/or preparatory education',
      preschoolInfo:
        'Free-of-charge pre-primary education is available for four hours per day. In addition, you can apply for connected early childhood education (subject to a fee) for your child. It is available in pre-primary education units in the morning before pre-primary education and in the afternoon after pre-primary education. You can submit an application for connected early childhood education at the same time as you enrol your child in pre-primary education or through a separate application after pre-primary education has started. You can also use the same application to apply for free-of-charge preparatory education and early childhood education connected to preparatory education.',
      preschoolDaycareInfo:
        'Application for connected early childhood education for a child who will be / has been enrolled in pre-primary education or preparatory education',
      clubLabel: 'Club application',
      clubInfo:
        'The club application is used for applying for municipal clubs.',
      duplicateWarning:
        'The child already has a similar unfinished application. Please return to the Applications view and complete the existing application or contact the Early Childhood Education Service Guidance.',
      applicationInfo:
        'You can make changes to your application until its processing starts. After this, you can make changes to your application by contacting early childhood education service counselling (tel. 09 816 31000). If you wish to cancel an application you have submitted, please send an email to early childhood education service counselling (<a href="varhaiskasvatuksen.palveluohjaus@espoo.fi">varhaiskasvatuksen.palveluohjaus@espoo.fi</a>).',
      transferApplicationInfo: {
        DAYCARE:
          'Your child already has a place in the City of Espoo’s early childhood education. Use this application to apply for a transfer to another early childhood education unit.',
        PRESCHOOL:
          'Your child already has a place in pre-primary education. Use this application to apply for early childhood education <strong>connected to pre-primary education</strong> or a transfer to another pre-primary education unit.'
      },
      create: 'Apply',
      daycare4monthWarning: 'The application processing time is 4 months.'
    },
    editor: {
      heading: {
        title: {
          DAYCARE: 'Application for early childhood education',
          PRESCHOOL: 'Registration for pre-school education',
          CLUB: 'Application for club'
        },
        info: {
          DAYCARE: [
            'You can apply for early childhood education all year round. The application must be submitted at the latest four months before the need for early childhood education begins. If you urgently need early childhood education, you must apply for a place at the latest two weeks before the need begins.',
            'The applicant will receive a written decision on the early childhood education place via the <a href="https://www.suomi.fi/messages" target="_blank" rel="noreferrer">Messages service of Suomi.fi</a> or by post if the applicant has not taken in use the Messages service of Suomi.fi.',
            '* Information marked with a star is required'
          ],
          PRESCHOOL: [
            'Pre-primary education is attended one year before the start of compulsory education. Pre-primary education is free of charge. Enrolment for pre-primary education in the 2021–2022 school year takes place on 8–20 January 2021. Finnish and swedish pre-primary education begins on <strong>11 August 2021</strong>. The decisions will be sent to the <a href="https://www.suomi.fi/messages" target="_blank" rel="noreferrer">Suomi.fi Messages</a> service or by post, if the applicant does not use the <a href="https://www.suomi.fi/messages" target="_blank" rel="noreferrer">Suomi.fi Messages</a> service.',
            'The decisions will be sent to the <a href="https://www.suomi.fi/messages" target="_blank" rel="noreferrer">Suomi.fi Messages</a> service or by post, if the applicant does not use the <a href="https://www.suomi.fi/messages" target="_blank" rel="noreferrer">Suomi.fi Messages</a> service.',
            '* Information marked with a star is required'
          ],
          CLUB: [
            'Hakuaika syksyllä käynnistyviin kerhoihin on maaliskuussa. Jos lapsenne saa kerhopaikan, saatte päätöksen siitä huhti-toukokuun aikana. Päätös tehdään yhden toimintakauden ajaksi (elokuusta toukokuun loppuun). Päätös kerhopaikasta tulee Suomi.fi-palveluun tai postitse, mikäli ette ole ottanut palvelua käyttöön.',
            'Kerhohakemuksen voi jättää myös hakuajan ulkopuolella ja sen jälkeen, kun kerhojen toimintakausi on jo alkanut. Hakuaikana saapuneet hakemukset käsitellään kuitenkin ensin, ja hakuajan ulkopuolella tulleet hakemukset käsitellään saapumisjärjestyksessä. Kerhohakemus kohdistuu yhdelle kerhokaudelle. Kauden päättyessä hakemus poistetaan järjestelmästä.',
            'Kerhotoiminta on maksutonta, eikä siihen osallistuminen vaikuta Kelan maksamaan kotihoidontukeen. Jos lapselle sen sijaan on myönnetty varhaiskasvatuspaikka tai yksityisen hoidon tuki, ei hänelle voida myöntää kerhopaikkaa.',
            '* Tähdellä merkityt tiedot ovat pakollisia'
          ]
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
        notYetSent:
          '<strong>The application has not yet been sent.</strong> Please check the information you provided and send the application by clicking "Send the application" button in the end of the form.',
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
          wasOnDaycare: 'Varhaiskasvatuksessa ennen kerhoa',
          wasOnDaycareYes:
            'Lapsi, jolle haetaan kerhopaikkaa, on varhaiskasvatuksessa ennen kerhon toivottua aloituspäivää.',
          wasOnClubCare: 'Kerhossa edellisenä toimintakautena',
          wasOnClubCareYes:
            'Lapsi on ollut kerhossa edellisen toimintakauden aikana.',
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
              CLUB: 'Kerhon aloitus'
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
            CLUB: [
              'Kerhot noudattavat esiopetuksen työ- ja loma-aikoja. Kerhon toimintakausi on elokuusta toukokuun loppuun, ja kullekin toimintakaudelle haetaan erikseen. Eri kerhot kokoontuvat eri viikonpäivinä.'
            ]
          },
          clubTerm: 'Club term',
          label: {
            DAYCARE: 'Desired start date',
            PRESCHOOL: 'Start date in August',
            CLUB: 'Desired start date'
          },
          noteOnDelay: 'The application processing time is 4 months.',
          instructions:
            'You can postpone your desired start date until the processing of your application starts. After this, you can make changes to your desired start date by contacting early childhood education service counselling (tel. 09 816 31000).',
          placeholder: 'Select the start date',
          validationText: 'Desired start date: '
        },
        clubDetails: {
          wasOnDaycare:
            'Lapsella on varhaiskasvatuspaikka, josta hän luopuu kerhopaikan saadessaan.',
          wasOnDaycareInfo:
            'Jos lapsi on ollut varhaiskasvatuksessa (päiväkodissa, perhepäivähoidossa tai ryhmäperhepäivähoidossa) ja luopuu paikastaan kerhon alkaessa, hänellä on suurempi mahdollisuus saada kerhopaikka.',
          wasOnClubCare:
            'Lapsi on ollut kerhossa edellisen toimintakauden aikana.',
          wasOnClubCareInfo:
            'Jos lapsi on ollut kerhossa jo edellisen toimintakauden aikana, on hänellä suurempi mahdollisuus saada paikka kerhosta.'
        },
        urgent: {
          label: 'Application is urgent',
          attachmentsMessage: {
            text:
              'If the need for a place in early childhood education arises from sudden employment or a sudden start of studies, you will need to apply for a place no later than <strong>two weeks before</strong> your child needs it. In addition to the application, you will need to <strong>provide documents as proof of the employment or student status</strong> of both guardians living in the same household. If you are unable to add attachments to your online application, please send the documents by post to Early childhood education service counselling, P.O. Box 3125, 02070 City of Espoo. The processing period of two weeks will not begin until we have received the application and the required attachments.',
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
          connectedDaycareInfo: [
            'If necessary, you can apply for early childhood education related to pre-primary education for the child, which is subject to a charge and provided in addition to pre-primary education (4 h/day) in the mornings and/or afternoons in the same place as pre-primary education. If you wish early childhood education to start later than pre-primary education, enter the desired start date in the additional information field.',
            "Private kindergartens and some of the private kindergartens need a separate application for related early childhood education. Espoo's early childhood education is in contact with those applicants who are affected.",
            'A separate written decision will be issued on the early childhood education place. The decision will be sent to the <a href="https://www.suomi.fi/messages" target="_blank" rel="noreferrer">Suomi.fi Messages</a> service or by post if you do not use the <a href="https://www.suomi.fi/messages" target="_blank" rel="noreferrer">Suomi.fi messages</a> service.'
          ],
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
              'Evening and round-the-clock care is intended for children whose both parents do shift work or mainly study in the evening and/or during the weekends. In addition to the application, both parents will need to provide a document issued by their employer concerning shift work or a document concerning their studies as proof of the child’s need for evening or round-the-clock care. We recommend that you send the additional documents through this online system, as the processing period of two weeks will not begin until we have received the application and the required attachments. If you are unable to add attachments to your online application, please send the documents by post to Early childhood education service counselling, P.O. Box 3125, 02070 City of Espoo.',
            subtitle:
              'Add here, for both parents, either a document issued by their employer concerning shift work or a document concerning their evening/weekend studies.'
          }
        },
        assistanceNeed: 'Support need',
        assistanceNeeded: 'The child needs support',
        assistanceNeedPlaceholder:
          'Describe the need for support for the child.',
        assistanceNeedInstructions:
          'The support need refers to such a need for support measures that has been indicated by an expert opinion. If the child has not previously attended the Espoo early childhood education services and their support need has been established, the Special Early Education Coordinator will contact you, if necessary, once you have indicated the support need on this application.',
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
            DAYCARE: `
            <p>The child has a sibling basis to the same early childhood education place attended by their sibling at the time of making the decision. All children living at the same address are considered to be siblings. The aim is to place siblings at the same early childhood education place, if the family so wishes. If you are applying for places for siblings who do not yet attend early childhood education, enter this piece of information in the additional information field.</p>
            <p>Fill in this information only if you want to exercise the sibling-basis right.</p>`,
            PRESCHOOL: `
            <p>A child enrolling for pre-primary education has a sibling-basis right to a</p>
            <ol type="a">
              <li>early childhood education in their own service area, where they have a sibling who has, at the moment of making the decision and in the coming pre-primary education year, a place at the pre-primary education day-care centre</li>
              <li>local school determined by the city, which will be attended by their sibling in the coming school year.</li>
            </ol>
            <p>If the child enrolling for pre-primary education has a sibling-basis right to both options, the guardian may decide which of them will be exercised. The selection is indicated by entering the name of the sibling in the field below.</p>
            <p>Fill in this information only if you want to exercise the sibling-basis right.</p>
            `,
            CLUB: `
            <p>Children living in the same address are considered siblings. An effort is made to put the siblings into a same club group when the family wishes that.</p>
            <p>Fill in this information only if you want to exercise the sibling-basis right and choose the same club the sibling attends below.</p>
            `
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
            DAYCARE: `
            <p>You can apply for 1 to 3 locations in your order of preference. Application preferences do not guarantee a place at the desired location, but the possibility of obtaining a desired location increases by giving more than one option.</p>
            <p>You can display the unit locations by choosing 'Unit map view'.</p>`,
            PRESCHOOL: `
            <p>You can apply for 1 to 3 locations in your order of preference. Application preferences do not guarantee a place at the desired location, but the possibility of obtaining a desired location increases by giving more than one option.</p>
            <p>You can display the unit locations by choosing 'Unit map view'.</p>`,
            CLUB: `
            <p>You can apply for 1 to 3 locations in your order of preference. Application preferences do not guarantee a place at the desired location, but the possibility of obtaining a desired location increases by giving more than one option.</p>
            <p>You can display the unit locations by choosing 'Unit map view'.</p>`
          },
          mapLink: 'Unit map view',
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
          DAYCARE:
            'The client fees for municipal early childhood education are calculated as a percentage of the family’s gross income. Each family must provide information about their gross income using the income statement form, no later than two weeks after their child’s early childhood education has started.',
          PRESCHOOL:
            'The client fees for municipal early childhood education are calculated as a percentage of the family’s gross income. Each family must provide information about their gross income using the income statement form, no later than two weeks after their child’s early childhood education has started.',
          CLUB: ''
        },
        emphasis:
          '<strong>The income statement is not needed if the family agrees to pay the highest fee.</strong>',
        checkbox:
          'I give consent to the highest fee. This consent will remain valid until I state otherwise.',
        links:
          'You can find further information about early childhood education fees and the income statement form here:<br/><a href="https://www.espoo.fi/en-US/Childcare_and_education/Early_childhood_education/Early_childhood_education_fees" target="_blank" rel="noopener noreferrer">Early childhood education fees</a>'
      },
      additionalDetails: {
        title: 'Other additional information',
        otherInfoLabel: 'Additional information',
        otherInfoPlaceholder:
          'If you wish, you can provide more detailed information related to the application in this field',
        dietLabel: 'Special diet',
        dietPlaceholder:
          'If you wish, you can indicate your child’s special diet in this field',
        dietInfo:
          'Some special diets require a separate medical certificate to be delivered to the early childhood education location. Exceptions are a low-lactose or lactose-free diet, a diet based on religious beliefs and vegetarian diet (lacto-ovo).',
        allergiesLabel: 'Allergies',
        allergiesPlaceholder:
          'If you wish, you can indicate your child’s allergies in this field',
        allergiesInfo:
          'Information on allergies is mainly needed when applying for family day care.'
      },
      contactInfo: {
        title: 'Personal information',
        info:
          "The personal information has been retrieved from the population data services and cannot be changed with this application. If the personal information is incorrect, please update the information on https://dvv.fi/en/certificates-from-the-population-information-system. If your address is about to change, you can add the new address in a separate field in the application. Add a future address for both the child and guardian. The address information will be official only when it has been updated to the database of the Digital and Population Data Services Agency. Decisions on the child's early childhood or pre-primary education place will be automatically sent to another guardian who lives at a different address based on the Population Information System.",
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
        emailAddress: 'Email',
        email: 'Email',
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
    modalHeader: 'Processing file',
    modalMessage: 'File is being processed. Try again later',
    modalConfirm: 'OK',
    deleteFile: 'Delete file'
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
    unitNotSelected: 'Pick at least one choice'
  }
}

export default en
