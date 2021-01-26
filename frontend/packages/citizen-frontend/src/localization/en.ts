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
        MUNICIPAL: 'Kunnallinen',
        PURCHASED: 'Ostopalvelu',
        PRIVATE: 'Yksityinen',
        MUNICIPAL_SCHOOL: 'Kunnallinen',
        PRIVATE_SERVICE_VOUCHER: 'Palveluseteli'
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
          DAYCARE: 'Varhaiskasvatushakemus'
        },
        info: {
          DAYCARE: [
            'You can apply for early childhood education all year round. The application must be submitted at the latest four months before the need for early childhood education begins. If you urgently need early childhood education, you must apply for a place at the latest two weeks before the need begins.',
            'The applicant will receive a written decision on the early childhood education place via the <a href="https://www.suomi.fi/messages" target="_blank" rel="noreferrer">Messages service of Suomi.fi</a> or by post if the applicant has not taken in use the Messages service of Suomi.fi.',
            '* Information marked with a star is required'
          ]
        }
      },
      actions: {
        hasVerified: 'Olen tarkistanut hakemuksen tiedot oikeiksi',
        verify: 'Tarkista hakemus',
        returnToEdit: 'Palaa muokkaamaan hakemusta',
        returnToEditBtn: 'Takaisin hakemusnäkymään',
        cancel: 'Palaa',
        send: 'Lähetä hakemus',
        sendError: 'Hakemuksen lähettäminen epäonnistui',
        saveDraft: 'Tallenna keskeneräisenä',
        saveDraftError: 'Muutosten tallentaminen epäonnistui'
      },
      verification: {
        title: {
          DAYCARE: 'Varhaiskasvatushakemuksen tarkistaminen'
        },
        notYetSent:
          '<strong>Hakemusta ei ole vielä lähetetty.</strong> Tarkista antamasi tiedot ja lähetä sivun lopussa olevalla Lähetä hakemus-painikkeella.',
        no: 'ei',
        basics: {
          created: 'Hakemus luotu',
          modified: 'Hakemusta muokattu viimeksi'
        },
        unitPreference: {
          title: 'Hakutoive',
          siblingBasis: {
            title: 'Haku sisarperusteella',
            siblingBasisLabel: 'Sisarusperuste',
            siblingBasisYes:
              'Haen ensisijaisesti samaan paikkaan, jossa lapsen sisarus on jo varhaiskasvatuksessa',
            name: 'Sisaruksen nimi',
            ssn: 'Sisaruksen henkilötunnus'
          },
          units: {
            title: 'Hakutoiveet',
            label: 'Valitsemasi hakutoiveet'
          }
        }
      },
      serviceNeed: {
        serviceNeed: 'Palveluntarve',
        startDate: {
          label: 'Toivottu aloituspäivä',
          noteOnDelay: 'Hakemuksen käsittelyaika on 4 kuukautta.',
          instructions:
            'Toivottua aloituspäivää on mahdollista muuttaa myöhemmäksi niin kauan kuin hakemusta ei ole otettu käsittelyyn. Tämän jälkeen toivotun aloituspäivän muutokset tehdään ottamalla yhteyttä varhaiskasvatuksen palveluohjaukseen (puh. 09 816 31000).',
          placeholder: 'Valitse aloituspäivä',
          validationText: 'Toivottu aloituspäivä: '
        },
        urgent: {
          label: 'Hakemus on kiireellinen',
          message: {
            title: 'Hakemus on kiireellinen',
            text:
              'Mikäli varhaiskasvatuspaikan tarve johtuu äkillisestä työllistymisestä tai opiskelusta, tulee paikkaa hakea viimeistään kaksi viikkoa ennen kuin tarve alkaa. Kahden viikon käsittelyaika alkaa siitä, kun työ- tai opiskelutodistukset on toimitettu palveluohjaukseen (varhaiskasvatuksen.palveluohjaus@espoo.fi).'
          },
          attachmentsMessage: {
            text:
              'Mikäli varhaiskasvatuspaikan tarve johtuu äkillisestä työllistymisestä tai opiskelusta, tulee paikkaa hakea viimeistään kaksi viikkoa ennen kuin tarve alkaa. Hakemuksen liitteenä tulee olla työ- tai opiskelutodistus molemmilta samassa taloudessa asuvilta huoltajilta. Suosittelemme toimittamaan liitteen sähköisesti tässä, sillä kahden viikon käsittelyaika alkaa siitä, kun olemme vastaanottaneet hakemuksen tarvittavine liitteineen. Jos et voi lisätä liitteitä hakemukselle sähköisesti, lähetä ne postilla osoitteeseen Varhaiskasvatuksen palveluohjaus, PL 3125, 02070 Espoon kaupunki.',
            subtitle:
              'Lisää tähän työ- tai opiskelutodistus molemmilta vanhemmilta.'
          }
        },
        partTime: {
          true: 'Osapäiväinen (max 5h/pv, 25h/vko)',
          false: 'Kokopäiväinen'
        },
        dailyTime: {
          label: 'Päivittäinen varhaiskasvatusaika',
          instructions:
            'Ilmoita lapsen yleisimmin tarvitseva varhaiskasvatusaika, aika tarkennetaan varhaiskasvatuksen alkaessa.',
          usualArrivalAndDeparture:
            'Varhaiskasvatuksen alkamis- ja päättymisaika'
        },
        shiftCare: {
          label: 'Ilta- ja vuorohoito',
          instructions:
            'Vuorohoidolla tarkoitetaan viikonloppuna tai ympärivuorokautisesti tarvittavaa varhaiskasvatusta. Iltahoito on pääasiassa klo 6.30-18.00 ulkopuolella ja viikonloppuisin tapahtuvaa varhaiskasvatusta. Mikäli tarvitset ilta- tai vuorohoitoa, täsmennä tarvetta lisätietokentässä.',
          message: {
            title: 'Ilta- ja vuorohoito',
            text:
              'Ilta- ja vuorohoito on tarkoitettu lapsille, joiden molemmat vanhemmat ovat vuorotyössä tai opiskelevat pääsääntöisesti iltaisin ja/ viikonloppuisin. Hakemuksen liitteeksi toimitetaan molempien vanhempien osalta työnantajan todistus vuorotyöstä tai opiskelusta johtuvasta ilta- tai vuorohoidon tarpeesta.'
          },
          attachmentsMessage: {
            text:
              'Ilta-  ja vuorohoito on tarkoitettu lapsille, joiden molemmat vanhemmat ovat vuorotyössä tai opiskelevat pääsääntöisesti iltaisin ja/tai viikonloppuisin. Hakemuksen liitteeksi toimitetaan molempien vanhempien osalta työnantajan todistus vuorotyöstä tai opiskelusta johtuvasta ilta- tai vuorohoidon tarpeesta. Suosittelemme toimittamaan liitteen sähköisesti tässä, sillä kahden viikon käsittelyaika alkaa siitä, kun olemme vastaanottaneet hakemuksen tarvittavine liitteineen. Jos et voi lisätä liitteitä hakemukselle sähköisesti, lähetä ne postilla osoitteeseen Varhaiskasvatuksen palveluohjaus, PL 3125, 02070 Espoon kaupunki.',
            subtitle:
              'Lisää tähän molemmilta vanhemmilta joko työnantajan todistus vuorotyöstä tai todistus opiskelusta iltaisin/viikonloppuisin.'
          }
        },
        assistanceNeed: 'Tuen tarve',
        assistanceNeeded: 'Lapsella on tuen tarve',
        assistanceNeedPlaceholder: 'Kerro lapsen tuen tarpeesta.',
        assistanceNeedInstructions:
          'Tehostetun ja tuen tarpeella tarkoitetaan sellaisten tukitoimien tarvetta, jotka on osoitettu asiantuntijalausunnoin. Tuen tarpeissa Espoon varhaiskasvatuksesta otetaan erikseen yhteyttä hakemuksen jättämisen jälkeen. Kehityksen ja oppimisen tuki varhaiskasvatuksessa toteutuu pääsääntöisesti lapsen kotia lähellä olevassa päiväkodissa tai perhepäivähoidossa. Tukitoimet toteutuvat lapsen arjessa osana varhaiskasvatuksen muuta toimintaa. Osa hoitopaikoista on varattu tukea tarvitseville lapsille.'
      },
      unitPreference: {
        title: 'Hakutoive',
        siblingBasis: {
          title: 'Haku sisarperusteella',
          p1:
            'Lapsella on sisarusperuste samaan varhaiskasvatuspaikkaan, jossa hänen sisaruksensa on päätöksentekohetkellä. Sisarukseksi katsotaan kaikki samassa osoitteessa asuvat lapset. Tavoitteena on sijoittaa sisarukset samaan varhaiskasvatuspaikkaan perheen niin toivoessa. Jos haet paikkaa sisaruksille, jotka eivät vielä ole varhaiskasvatuksessa, kirjoita tieto lisätietokenttään.',
          p2:
            'Täytä nämä tiedot vain, jos käytät sisarusperustetta, sekä valitse alla olevissa hakutoiveissa ensisijaiseksi toiveeksi sama varhaiskasvatusyksikkö, jossa lapsen sisarus on.',
          checkbox:
            'Haen ensisijaisesti samaan paikkaan, jossa lapsen sisarus on jo varhaiskasvatuksessa.',
          names: 'Sisaruksen etunimet ja sukunimi *',
          namesPlaceholder: 'Etunimet ja sukunimi',
          ssn: 'Sisaruksen henkilötunnus *',
          ssnPlaceholder: 'Henkilötunnus'
        },
        units: {
          title: 'Hakutoiveet',
          startDateMissing:
            'Päästäksesi valitsemaan hakutoiveet valitse ensin toivottu aloituspäivä "Palvelun tarve" -osiosta',
          p1:
            'Voit hakea paikkaa 1-3 varhaiskasvatusyksiköstä toivomassasi järjestyksessä. Hakutoiveet eivät takaa paikkaa toivotussa yksikössä, mutta mahdollisuus toivotun paikan saamiseen kasvaa antamalla useamman vaihtoehdon.',
          p2:
            'Näet eri varhaiskasvatusyksiköiden sijainnin valitsemalla ‘Yksiköt kartalla’.',
          mapLink: 'Yksiköt kartalla',
          languageFilter: {
            label: 'Yksikön kieli',
            fi: 'suomi',
            sv: 'ruotsi'
          },
          select: {
            label: 'Valitse hakutoiveet *',
            placeholder: 'Hae yksiköitä',
            maxSelected: 'Maksimimäärä yksiköitä valittu',
            noOptions: 'Ei hakuehtoja vastaavia yksiköitä'
          },
          preferences: {
            label: 'Valitsemasi hakutoiveet',
            info:
              'Valitse 1-3 varhaiskasvatusyksikköä ja järjestä ne toivomaasi järjestykseen. Voit muuttaa järjestystä nuolien avulla.',
            fi: 'suomenkielinen',
            sv: 'ruotsinkielinen',
            moveUp: 'Siirrä ylöspäin',
            moveDown: 'Siirrä alaspäin',
            remove: 'Poista hakutoive'
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
        title: 'Henkilötiedot',
        info:
          'Henkilötiedot on haettu väestötiedoista, eikä niitä voi muuttaa tällä hakemuksella. Jos henkilötiedoissa on virheitä, päivitäthän tiedot Digi- ja Väestötietoviraston sivuilla. Mikäli osoitteenne on muuttumassa, voit lisätä tulevan osoitteen erilliseen kohtaan hakemuksella; lisää tuleva osoite sekä lapselle että huoltajalle. Virallisena osoitetietoa pidetään vasta, kun se on päivittynyt väestötietojärjestelmään. Päätökset esiopetus- ja varhaiskasvatuspaikoista toimitetaan automaattisesti myös eri osoitteessa asuvalle väestötiedoista löytyvälle huoltajalle.',
        childInfoTitle: 'Lapsen tiedot',
        childFirstName: 'Lapsen etunimet',
        childLastName: 'Lapsen sukunimi',
        childSSN: 'Lapsen henkilötunnus',
        homeAddress: 'Kotiosoite',
        moveDate: 'Muuttopäivämäärä',
        street: 'Katuosoite',
        postalCode: 'Postinumero',
        municipality: 'Postitoimipaikka',
        guardianInfoTitle: 'Huoltajan tiedot',
        guardianFirstName: 'Huoltajan etunimet',
        guardianLastName: 'Huoltajan sukunimi',
        guardianSSN: 'Huoltajan henkilötunnus',
        phone: 'Puhelinnumero',
        email: 'Sähköposti',
        secondGuardianInfoTitle: 'Toisen huoltajan tiedot',
        secondGuardianInfo:
          'Toisen huoltajan tiedot haetaan automaattisesti väestötietojärjestelmästä.',
        nonCaretakerPartnerTitle:
          'Samassa taloudessa asuva avio- tai avopuoliso (ei huoltaja)',
        nonCaretakerPartnerCheckboxLabel:
          'Samassa taloudessa asuu hakijan kanssa avio- tai avoliitossa oleva henkilö, joka ei ole lapsen huoltaja.',
        personFirstName: 'Henkilön etunimet',
        personLastName: 'Henkilön sukunimi',
        personSSN: 'Henkilön henkilötunnus',
        otherChildrenTitle: 'Samassa taloudessa asuvat alle 18-vuotiaat lapset',
        otherChildrenInfo:
          'Samassa taloudessa asuvat alle 18-vuotiaat lapset vaikuttavat varhaiskasvatusmaksuihin.',
        otherChildrenChoiceInfo:
          'Valitse lapset, jotka asuvat samassa taloudessa.',
        hasFutureAddress:
          'Väestörekisterissä oleva osoite on muuttunut tai muuttumassa',
        firstNamePlaceholder: 'Etunimet',
        lastNamePlaceholder: 'Sukunimi',
        ssnPlaceholder: 'Henkilötunnus',
        streetPlaceholder: 'Osoite',
        postalCodePlaceholder: 'Postinumero',
        municipalityPlaceholder: 'Postitoimipaikka',
        choosePlaceholder: 'Valitse'
      },
      draftPolicyInfo: {
        title: 'Your draft application was saved',
        text:
          'Your unfinished application was saved. Please note! An unfinished application is stored in the system for one month after it was last saved.',
        ok: 'OK'
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
    editApplicationLink: 'Muokkaa hakemusta',
    removeApplicationBtn: 'Poista hakemus',
    confirmationLinkInstructions:
      'In the Decisions page you can read the decision and either accept or reject the proposed place',
    confirmationLink: 'Review and confirm the decision',
    newApplicationLink: 'New application'
  }
}

export default en
