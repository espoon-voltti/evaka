// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Translations } from '.'

const sv: Translations = {
  common: {
    cancel: 'Gå tillbaka',
    return: 'Tillbaka',
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
      map: 'Karta',
      applications: 'Ansökningar',
      decisions: 'Beslut',
      newDecisions: 'Ny Beslut',
      newApplications: 'Ny Ansökningar'
    },
    lang: {
      fi: 'Suomeksi',
      sv: 'På svenska',
      en: 'In English'
    },
    logout: 'Logga ut'
  },
  footer: {
    espooLabel: '© Esbo stad',
    privacyPolicy: 'Dataskyddsbeskrivningar',
    privacyPolicyLink:
      'https://www.esbo.fi/sv-FI/Etjanster/Dataskydd/Dataskyddsbeskrivningar',
    sendFeedback: 'Ge feedback',
    sendFeedbackLink:
      'https://easiointi.espoo.fi/eFeedback/sv/Feedback/20-S%C3%A4hk%C3%B6iset%20asiointipalvelut'
  },
  applications: {
    title: 'Hakemukset',
    creation: {
      title: 'Val av ansökningsblankett',
      daycareLabel: 'Ansökan till småbarnspedagogik',
      daycareInfo:
        'Med en ansökan till småbarnspedagogisk verksamhet ansöker du om en plats i småbarnspedagogisk verksamhet i ett daghem, hos en familjedagvårdare eller i ett gruppfamiljedaghem.',
      preschoolLabel:
        'Anmälan till förskoleundervisning och/eller förberedande undervisning',
      preschoolInfo:
        'Förskoleundervisningen är gratis och ordnas fyra timmar per dag. Därtill kan du ansöka om anslutande småbarnspedagogik på samma plats, på morgonen före och på eftermiddagen efter förskoleundervisningen. Du kan ansöka om småbarnspedagogik i anslutning till förskoleundervisningen när du anmäler barnet till förskoleundervisningen eller separat efter att förskoleundervisningen har inletts. Du kan också ansöka på en och samma ansökan om både förberedande undervisning, som är gratis, och anslutande småbarnspedagogik.',
      preschoolDaycareInfo:
        'Ansökan om anslutande småbarnspedagogik för ett barn som anmäls/har anmälts till förskoleundervisning eller förberedande undervisning',
      clubLabel: 'Ansökan till en klubb',
      clubInfo:
        'Med ansökan till klubbverksamhet kan du ansöka till kommunala klubbar.',
      duplicateWarning:
        'Ditt barn har en motsvarande oavslutad ansökan. Gå tillbaka till vyn Ansökningar och bearbeta den befintliga ansökan eller ta kontakt med barninvalskoordinatorn.',
      applicationInfo:
        'Du kan ändra i ansökan så länge den inte har tagits till behandling. Därefter kan du göra ändringar i ansökan genom att kontakta småbarnspedagogikens servicehänvisning (tfn 09 8163 27600). Du kan återta en ansökan som du redan lämnat in genom att meddela detta per e-post till småbarnspedagogikens servicehänvisning <a href="mailto:dagis@esbo.fi">dagis@esbo.fi</a>.',
      create: 'Ny ansökan'
    },
    editor: {
      heading: {
        title: {
          DAYCARE: 'Ansökan till småbarnspedagogik'
        },
        info: {
          DAYCARE: [
            'Du kan ansöka om plats i småbarnspedagogisk verksamhet året om. Ansökningen bör lämnas in senast fyra månader före behovet av verksamheten börjar. Om behovet börjar med kortare varsel bör du ansöka om plats senast två veckor före.',
            'Du får ett skriftligt beslut om platsen. Beslutet delges i tjänsten <a href="https://www.suomi.fi/meddelanden" target="_blank" rel="noreferrer">Suomi.fi</a>-meddelanden, eller per post om du inte tagit i bruk meddelandetjänsten i Suomi.fi.',
            '* Informationen markerad med en stjärna krävs'
          ]
        }
      },
      actions: {
        verify: 'Tarkista hakemus',
        hasVerified: 'Olen tarkistanut hakemuksen tiedot oikeiksi',
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
        title: 'Avgiften för småbarnspedagogik',
        info:
          'Klientavgiften inom den kommunala småbarnspedagogiken är en procentandel av familjens bruttoinkomster. Avgiften beror på familjens storlek och inkomster samt småbarnspedagogikens dagliga längd, från ingen avgift till en månadsavgift på högst 288 euro per barn. Familjen ska lämna in en utredning över sina bruttoinkomster på en särskild blankett, senast inom två veckor från det att barnet har inlett småbarnspedagogiken.',
        emphasis:
          '<strong>Om familjen samtycker till den högsta avgiften behövs ingen inkomstutredning.</strong>',
        checkbox:
          'Jag ger mitt samtycke till att betala den högsta avgiften. Samtycket gäller tills vidare, tills jag meddelar något annat.',
        links:
          'Mer information om småbarnspedagogikens avgifter och blanketten för inkomstutredning finns här:<br/><a href="https://www.esbo.fi/sv-FI/Utbildning_och_fostran/Smabarnspedagogik/Avgifter_for_smabarnspedagogik" target="_blank" rel="noopener noreferrer">Avgifter för småbarnspedagogik</a>'
      },
      additionalDetails: {
        title: 'Övriga tilläggsuppgifter',
        otherInfoLabel: 'Övriga tilläggsuppgifter',
        otherInfoPlaceholder:
          'Du kan ge noggrannare uppgifter för din ansökan i det här fältet',
        dietLabel: 'Specialdiet',
        dietPlaceholder: 'Du kan meddela barnets specialdiet i det här fältet',
        dietInfo:
          'För en del specialdieter behövs även ett skilt läkarintyg som lämnas in till enheten. Undantag är laktosfri eller laktosfattig diet, diet som grundar sig på religiösa orsaker och vegetarisk kost (lakto-ovo).',
        allergiesLabel: 'Allergier',
        allergiesPlaceholder:
          'Du kan meddela barnets allergier i det här fältet',
        allergiesInfo:
          'Information om allergier behövs när du ansöker till familjedagvård.'
      },
      contactInfo: {
        title: 'Henkilötiedot',
        info:
          'Henkilötiedot on haettu väestötiedoista, eikä niitä voi muuttaa tällä hakemuksella. Jos henkilötiedoissa on virheitä, päivitäthän tiedot <a href="https://dvv.fi/henkiloasiakkaat" target="_blank" rel="noreferrer">Digi- ja Väestötietoviraston sivuilla</a>. Mikäli osoitteenne on muuttumassa, voit lisätä tulevan osoitteen erilliseen kohtaan hakemuksella; lisää tuleva osoite sekä lapselle että huoltajalle. Virallisena osoitetietoa pidetään vasta, kun se on päivittynyt väestötietojärjestelmään. Päätökset esiopetus- ja varhaiskasvatuspaikoista toimitetaan automaattisesti myös eri osoitteessa asuvalle väestötiedoista löytyvälle huoltajalle.',
        childInfoTitle: 'Lapsen tiedot',
        childFirstName: 'Lapsen etunimet',
        childLastName: 'Lapsen sukunimi',
        childSSN: 'Lapsen henkilötunnus',
        homeAddress: 'Kotiosoite',
        moveDate: 'Muuttopäivämäärä',
        street: 'Katuosoite',
        postalCode: 'Postinumero',
        postOffice: 'Postitoimipaikka',
        guardianInfoTitle: 'Huoltajan tiedot',
        guardianFirstName: 'Huoltajan etunimet',
        guardianLastName: 'Huoltajan sukunimi',
        guardianSSN: 'Huoltajan henkilötunnus',
        phone: 'Puhelinnumero',
        emailAddress: 'Sähköpostiosoite',
        email: 'Sähköposti',
        secondGuardianInfoTitle: 'Toisen huoltajan tiedot',
        secondGuardianInfo:
          'Toisen huoltajan tiedot haetaan automaattisesti väestötietojärjestelmästä.',
        otherPartnerTitle:
          'Samassa taloudessa asuva avio- tai avopuoliso (ei huoltaja)',
        otherPartnerCheckboxLabel:
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
        guardianFutureAddressEqualsChildFutureAddress:
          'Muutan samaan osoitteeseen kuin lapsi',
        firstNamePlaceholder: 'Etunimet',
        lastNamePlaceholder: 'Sukunimi',
        ssnPlaceholder: 'Henkilötunnus',
        streetPlaceholder: 'Osoite',
        postalCodePlaceholder: 'Postinumero',
        municipalityPlaceholder: 'Postitoimipaikka',
        addChild: 'Lisää lapsi',
        remove: 'Poista',
        areExtraChildren:
          'Samassa taloudessa asuu muita alle 18-vuotiaita lapsia (esim. avopuolison lapset)',
        choosePlaceholder: 'Valitse'
      },
      draftPolicyInfo: {
        title: 'Utkastet till ansökan har sparats',
        text:
          'Ansökan har sparats som halvfärdig. Obs! En halvfärdig ansökan förvaras i tjänsten i en månad efter att den senast sparats',
        ok: 'Klart'
      }
    }
  },
  decisions: {
    title: 'Beslut',
    summary:
      'Denna sida visar de beslutar om barns ansökan till småbarnspedagogik, förskola och klubbverksamhet. Du ska omedelbart eller senast två veckor från mottagandet av ett beslut ta emot eller annullera platsen / platserna.',
    unconfirmedDecisions: (n: number) => `${n} beslut inväntar bekräftelse`,
    pageLoadError: 'Hämtar information misslyckades',
    applicationDecisions: {
      decision: 'Beslut om',
      type: {
        CLUB: 'klubbverksamhet',
        DAYCARE: 'småbarnspedagogik',
        DAYCARE_PART_TIME: 'deldag småbarnspedagogik',
        PRESCHOOL: 'förskola',
        PRESCHOOL_DAYCARE:
          'småbarnspedagogik i samband med förskoleundervisningen',
        PREPARATORY_EDUCATION: 'förberedande undervisning'
      },
      childName: 'Barnets namn',
      unit: 'Enhet',
      period: 'Period',
      sentDate: 'Beslutsdatum',
      resolved: 'Bekräftat',
      statusLabel: 'Status',
      summary:
        'Du ska omedelbart eller senast två veckor från mottagandet av ett beslut ta emot eller annullera platsen / platserna.',
      status: {
        PENDING: 'Bekräftas av vårdnadshavaren',
        ACCEPTED: 'Bekräftad',
        REJECTED: 'Avvisade'
      },
      openPdf: 'Visa beslut',
      confirmationInfo: {
        preschool:
          'Du ska omedelbart eller senast två veckor från mottagandet av detta beslut, ta emot eller annullera platsen. Du kan ta emot eller annullera platsen elektroniskt på adressen espoonvarhaiskasvatus.fi (kräver identifiering) eller per post.',
        default:
          'Du ska omedelbart eller senast två veckor från mottagandet av ett beslut ta emot eller annullera platsen.'
      },
      goToConfirmation:
        'Gå till beslutet för att läsa det och svara om du tar emot eller annullerar platsen.',
      confirmationLink: 'Granska och bekräfta beslutet',
      response: {
        title: 'Bekräftelse',
        accept1: 'Vi tar emot platsen från',
        accept2: '',
        reject: 'Vi tar inte emot platsen',
        cancel: 'Gå tillbacka utan att besluta',
        submit: 'Skicka svar på beslutet',
        disabledInfo:
          'OBS! Du kommer att kunna svara på den relaterade beslutet, om du först accepterar den beslutet om förskola / förberedande undervisning.'
      },
      warnings: {
        decisionWithNoResponseWarning: {
          title: 'Ett annat beslut väntar på ditt godkännande',
          text:
            'Ett annat beslut väntar på ditt godkännande. Vill du gå tillbaka till listan utan att svara?',
          resolveLabel: 'Gå tillbaka utan att svara',
          rejectLabel: 'Förtsätt att svara'
        },
        doubleRejectWarning: {
          title: 'Vill du annulera platsen?',
          text:
            'Du ska annullera platsen. Den relaterade småbarnspedagogik plats ska också markeras annulerat.',
          resolveLabel: 'Annulera båda',
          rejectLabel: 'Gå tillbaka'
        }
      },
      errors: {
        pageLoadError: 'Misslyckades att hämta information',
        submitFailure: 'Misslyckades att skicka svar'
      },
      returnToPreviousPage: 'Tillbacka'
    }
  },
  applicationsList: {
    title:
      'Anmälan till förskolan eller ansökan till småbarnspedagogisk verksamhet',
    summary:
      'Barnets vårdnadshavare kan anmäla barnet till förskolan eller ansöka om plats i småbarnspedagogisk verksamhet. Uppgifter om vårdnadshavarens barn kommer automatiskt från befolkningsdatabasen till denna sida.',
    pageLoadError: 'Tietojen hakeminen ei onnistunut',
    noApplications: 'Inga ansökningar',
    type: {
      DAYCARE: 'Ansökan till småbarnspedagogik',
      PRESCHOOL: 'Anmälan till förskolan',
      CLUB: 'Ansökan till klubbverksamhet'
    },
    unit: 'Enhet',
    period: 'Period',
    created: 'Skapad',
    modified: 'Ändrad',
    status: {
      title: 'Status',
      CREATED: 'Förslag',
      SENT: 'Skickas',
      WAITING_PLACEMENT: 'Bearbetas',
      WAITING_DECISION: 'Bearbetas',
      WAITING_UNIT_CONFIRMATION: 'Bearbetas',
      WAITING_MAILING: 'Bearbetas',
      WAITING_CONFIRMATION: 'Bekräftas av vårdnadshavaren',
      REJECTED: 'Platsen annullerad',
      ACTIVE: 'Godkänd',
      CANCELLED: 'Platsen annullerad'
    },
    openApplicationLink: 'Visa ansökan',
    editApplicationLink: 'Muokkaa hakemusta',
    removeApplicationBtn: 'Poista hakemus',
    confirmationLinkInstructions:
      'Under Beslut-fliken kan du läsa besluten till dina ansökningar och ta emot/annullera platsen',
    confirmationLink: 'Granska och bekräfta beslutet',
    newApplicationLink: 'Ny ansökan'
  }
}

export default sv
