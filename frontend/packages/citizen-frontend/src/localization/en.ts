// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Translations } from '.'

const en: Translations = {
  common: {
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
      }
    }
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
      create: 'Apply'
    },
    editor: {
      heading: {
        title: {
          DAYCARE: 'Application for early childhood education',
          PRESCHOOL: 'Ilmoittautuminen esiopetukseen',
          CLUB: 'Kerhohakemus'
        },
        info: {
          DAYCARE: [
            'You can apply for early childhood education all year round. The application must be submitted at the latest four months before the need for early childhood education begins. If you urgently need early childhood education, you must apply for a place at the latest two weeks before the need begins.',
            'The applicant will receive a written decision on the early childhood education place via the <a href="https://www.suomi.fi/messages" target="_blank" rel="noreferrer">Messages service of Suomi.fi</a> or by post if the applicant has not taken in use the Messages service of Suomi.fi.',
            '* Information marked with a star is required'
          ],
          PRESCHOOL: [
            'Esiopetukseen osallistutaan vuosi ennen oppivelvollisuuden alkamista. Esiopetus on maksutonta. Lukuvuoden 2021–2022 esiopetukseen ilmoittaudutaan 8.–20.1.2021. Suomen ja ruotsin kielinen esiopetus alkaa 11.8.2021.',
            'Päätökset tulevat <a href="https://www.suomi.fi/viestit" target="_blank" rel="noreferrer">Suomi.fi-viestit</a> -palveluun tai postitse, mikäli et ole ottanut Suomi.fi -palvelua käyttöön.',
            '* Tähdellä merkityt tiedot ovat pakollisia'
          ],
          CLUB: [
            'Hakuaika syksyllä käynnistyviin kerhoihin on maaliskuussa. Jos lapsenne saa kerhopaikan, saatte päätöksen siitä huhti-toukokuun aikana. Päätös tehdään yhden toimintakauden ajaksi (elokuusta toukokuun loppuun). Päätös kerhopaikasta tulee Suomi.fi-palveluun tai postitse, mikäli ette ole ottanut palvelua käyttöön.',
            'Kerhohakemuksen voi jättää myös hakuajan ulkopuolella ja sen jälkeen, kun kerhojen toimintakausi on jo alkanut. Hakuaikana saapuneet hakemukset käsitellään kuitenkin ensin, ja hakuajan ulkopuolella tulleet hakemukset käsitellään saapumisjärjestyksessä. Kerhohakemus kohdistuu yhdelle kerhokaudelle. Kauden päättyessä hakemus poistetaan järjestelmästä.',
            'Kerhotoiminta on maksutonta, eikä siihen osallistuminen vaikuta Kelan maksamaan kotihoidontukeen. Jos lapselle sen sijaan on myönnetty varhaiskasvatuspaikka tai yksityisen hoidon tuki, ei hänelle voida myöntää kerhopaikkaa.',
            '* Tähdellä merkityt tiedot ovat pakollisia'
          ]
        },
        errors: (count: number) =>
          count === 1 ? '1 virhe' : `${count} virhettä`,
        hasErrors: 'Ole hyvä ja tarkista seuraavat tiedot hakemuksestasi:'
      },
      actions: {
        hasVerified:
          'I have checked that the information in the application is correct',
        verify: 'Verify application',
        returnToEdit: 'Return to editing',
        returnToEditBtn: 'Return to editing',
        cancel: 'Go back',
        send: 'Send application',
        update: 'Tallenna muutokset',
        sendError: 'Failed to send the application',
        saveDraft: 'Save as draft',
        updateError: 'Saving the changes failed'
      },
      verification: {
        title: {
          DAYCARE: 'Verifying the application'
        },
        notYetSent:
          '<strong>The application has not yet been sent.</strong> Please check the information you provided and send the application by clicking "Send the application" button in the end of the form.',
        no: 'No',
        basics: {
          created: 'Created',
          modified: 'Last modified'
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
        }
      },
      serviceNeed: {
        title: 'Service need',
        startDate: {
          header: {
            DAYCARE: 'Start date',
            PRESCHOOL: 'Esiopetuksen alkaminen',
            CLUB: 'Kerhon alkaminen'
          },
          info: {
            DAYCARE: [],
            PRESCHOOL: [
              'Suomen- ja ruotsinkielinen esiopetus alkaa 11.8.2021. Jos tarvitsette varhaiskasvatusta 1.8.2021 lähtien ennen esiopetuksen alkua, voitte hakea sitä tällä hakemuksella valitsemalla ”Haen myös esiopetukseen liittyvää varhaiskasvatusta”.'
            ],
            CLUB: [
              'Kerhot noudattavat esiopetuksen työ- ja loma-aikoja. Kerhon toimintakausi on elokuusta toukokuun loppuun, ja kullekin toimintakaudelle haetaan erikseen. Eri kerhot kokoontuvat eri viikonpäivinä.'
            ]
          },
          clubTerm: 'Kerhon toimintakausi',
          label: 'Desired start date',
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
          message: {
            title: 'Application is urgent',
            text:
              'If the need for an early childhood education place is due to sudden start of employment or studies, the place must be applied for at least two weeks before the need begins. The two-week processing time begins when proof of employment or studies has been submitted to Service Guidance (<a href="mailto:varhaiskasvatuksen.palveluohjaus@espoo.fi">varhaiskasvatuksen.palveluohjaus@espoo.fi</a>).'
          },
          attachmentsMessage: {
            text:
              'If the need for a place in early childhood education arises from sudden employment or a sudden start of studies, you will need to apply for a place no later than two weeks before your child needs it. In addition to the application, you will need to provide documents as proof of the employment or student status of both guardians living in the same household. If you are unable to add attachments to your online application, please send the documents by post to Early childhood education service counselling, P.O. Box 3125, 02070 City of Espoo. The processing period of two weeks will not begin until we have received the application and the required attachments.',
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
            PRESCHOOL: 'Esiopetukseen liittyvän varhaiskasvatuksen tarve'
          },
          connectedDaycareInfo: [
            'Voit hakea lapselle tarvittaessa <strong>esiopetukseen liittyvää varhaiskasvatusta, joka on maksullista, ja jota annetaan esiopetuksen (4 tuntia/päivä) lisäksi</strong> aamuisin ja/tai iltapäivisin samassa paikassa kuin esiopetus. Jos haluat aloittaa varhaiskasvatuksen myöhemmin kuin esiopetus alkaa, kirjoita haluttu aloituspäivämäärä hakemuksen “Muut lisätiedot” -kohtaan.',
            'Yksityisiin päiväkoteihin ja osaan ostopalvelupäiväkodeista on tehtävä erillinen hakemus liittyvään varhaiskasvatukseen. Espoon varhaiskasvatus on yhteydessä niihin hakijoihin, joita tämä koskee.',
            'Saat varhaiskasvatuspaikasta erillisen kirjallisen päätöksen, ja päätös tulee <a href="https://www.suomi.fi/viestit" target="_blank" rel="noreferrer">Suomi.fi-viestit</a> -palveluun tai postitse, mikäli et ole ottanut Suomi.fi-viestit -palvelua käyttöön.'
          ],
          connectedDaycare:
            'Haen myös esiopetukseen liittyvää varhaiskasvatusta.',
          instructions: {
            DAYCARE:
              'Indicate the most commonly needed early childhood education time, the time will be specified when early childhood education begins.',
            PRESCHOOL:
              'Esiopetusta tarjotaan sekä päiväkodeissa että kouluissa 4 tuntia päivässä pääsääntöisesti 09:00 – 13:00, mutta aika saattaa vaihdella yksiköittäin. Ilmoita lapsen tarvitsema varhaiskasvatusaika siten, että se sisältää esiopetusajan 4 h (esim. 7.00 – 17.00). Aika tarkennetaan varhaiskasvatuksen alkaessa. Varhaiskasvatustarpeen ajan vaihdellessa päivittäin tai viikoittain (esim. vuorohoidossa), ilmoita tarve tarkemmin lisätiedoissa lomakkeen lopussa.'
          },
          usualArrivalAndDeparture: {
            DAYCARE: 'Varhaiskasvatuksen alkamis- ja päättymisaika',
            PRESCHOOL: 'Daily early childhood education time'
          },
          starts: 'Start time',
          ends: 'End time'
        },
        shiftCare: {
          label: 'Evening and round-the-clock care',
          instructions:
            'Vuorohoidolla tarkoitetaan viikonloppuna tai ympärivuorokautisesti tarvittavaa varhaiskasvatusta. Iltahoito on pääasiassa klo 6.30-18.00 ulkopuolella ja viikonloppuisin tapahtuvaa varhaiskasvatusta. Mikäli tarvitset ilta- tai vuorohoitoa, täsmennä tarvetta lisätietokentässä.',
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
          'Lapsi tarvitsee tukea suomen kielen oppimisessa. Haen myös perusopetukseen valmistavaan opetukseen. Ei koske ruotsinkielistä esiopetusta.',
        preparatoryInfo:
          'Esiopetusikäisten valmistavaan opetukseen voivat hakeutua maahanmuuttajataustaiset lapset, paluumuuttajalapset, kaksikielisten perheiden lapset (paitsi suomi-ruotsi) ja adoptiolapset, jotka tarvitsevat tukea suomen kielessä ennen perusopetukseen siirtymistä. Valmistavaa opetusta annetaan esiopetuksen lisäksi keskimäärin 1 h/päivä. Opetus on maksutonta.'
      },
      unitPreference: {
        title: 'Preferred units',
        siblingBasis: {
          title: 'Application on a sibling basis',
          p1:
            'The child has a sibling basis to the same early childhood education place attended by their sibling at the time of making the decision. All children living at the same address are considered to be siblings. The aim is to place siblings at the same early childhood education place, if the family so wishes. If you are applying for places for siblings who do not yet attend early childhood education, enter this piece of information in the additional information field.',
          p2:
            'Fill in this information only if you are applying the sibling basis.',
          checkbox:
            'I primarily apply for a place at the same early childhood education location attended by the child’s sibling.',
          radioLabel:
            'Valitse sisarus, jonka kanssa haet samaan varhaiskasvatuspaikkaan',
          otherSibling: 'Muu sisarus',
          names: 'First and last name of the sibling',
          namesPlaceholder: 'First and last name',
          ssn: 'Personal identity code of the sibling',
          ssnPlaceholder: 'e.g. 010110A000P'
        },
        units: {
          title: 'Preferred units',
          startDateMissing:
            'To select preferred units first set the preferred start date on "Service need" section',
          p1:
            'You can apply for 1 to 3 locations in your order of preference. Application preferences do not guarantee a place at the desired location, but the possibility of obtaining a desired location increases by giving more than one option.',
          p2: 'To see the units on map select ‘unit map view’',
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
        info:
          'The client fees for municipal early childhood education are calculated as a percentage of the family’s gross income. Depending on the family’s size and income and the hours of early childhood education, the fees vary from free early childhood education to a maximum monthly fee of EUR 288 per child. Each family must provide information about their gross income using the income statement form, no later than two weeks after their child’s early childhood education has started.',
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
        childInfoTitle: 'Child´s information',
        childFirstName: 'Child’s first name(s)',
        childLastName: 'Child’s last name',
        childSSN: 'Child’s personal identity code',
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
          'Espoon varhaiskasvatuksessa virallisena osoitteena pidetään väestötiedoista saatavaa osoitetta. Osoite väestötiedoissa muuttuu hakijan tehdessä muuttoilmoituksen postiin tai maistraattiin.',
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
        title: 'Muutokset hakemukseen on tallennettu',
        text:
          'Halutessasi voit tehdä lisää muutoksia niin kauan kuin hakemusta ei olla otettu käsittelyyn.',
        ok: 'Selvä!'
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
    modalConfirm: 'OK'
  },
  validationErrors: {
    required: 'Arvo puuttuu',
    format: 'Anna oikeassa muodossa',
    ssn: 'Virheellinen henkilötunnus',
    phone: 'Virheellinen puhelinnumero',
    email: 'Virheellinen sähköpostiosoite',
    validDate: 'Anna muodossa pp.kk.vvvv',
    timeFormat: 'Anna muodossa hh:mm',
    unitNotSelected: 'Valitse vähintään yksi hakutoive'
  }
}

export default en
