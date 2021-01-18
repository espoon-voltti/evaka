// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Translations } from '.'

const sv: Translations = {
  common: {
    return: 'Tillbaka'
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
    editor: {
      heading: {
        title: {
          daycare: 'Ansökan till småbarnspedagogik'
        },
        info: {
          daycare: [
            'Du kan ansöka om plats i småbarnspedagogisk verksamhet året om. Ansökningen bör lämnas in senast fyra månader före behovet av verksamheten börjar. Om behovet börjar med kortare varsel bör du ansöka om plats senast två veckor före.',
            'Du får ett skriftligt beslut om platsen. Beslutet delges i tjänsten <a href="https://www.suomi.fi/meddelanden" target="_blank" rel="noreferrer">Suomi.fi</a>-meddelanden, eller per post om du inte tagit i bruk meddelandetjänsten i Suomi.fi.',
            '* Informationen markerad med en stjärna krävs'
          ]
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
      fee: {
        title: 'Avgiften för småbarnspedagogik',
        info:
          'Klientavgiften inom den kommunala småbarnspedagogiken är en procentandel av familjens bruttoinkomster. Avgiften beror på familjens storlek och inkomster samt småbarnspedagogikens dagliga längd, från ingen avgift till en månadsavgift på högst 288 euro per barn. Familjen ska lämna in en utredning över sina bruttoinkomster på en särskild blankett, senast inom två veckor från det att barnet har inlett småbarnspedagogiken.',
        emphasis:
          '<strong>Om familjen samtycker till den högsta avgiften behövs ingen inkomstutredning.</strong>',
        checkbox:
          'Jag ger mitt samtycke till att betala den högsta avgiften. Samtycket gäller tills vidare, tills jag meddelar något annat.',
        links:
          'Mer information om småbarnspedagogikens avgifter och blanketten för inkomstutredning finns här: esbo.fi > Utbildning och fostran > Småbarnspedagogik > <a href="https://www.esbo.fi/sv-FI/Utbildning_och_fostran/Smabarnspedagogik/Avgifter_for_smabarnspedagogik" target="_blank" rel="noopener noreferrer">Avgifter för småbarnspedagogik</a>'
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
      daycare: 'Ansökan till småbarnspedagogik',
      preschool: 'Anmälan till förskolan',
      club: 'Ansökan till klubbverksamhet'
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
    confirmationLinkInstructions:
      'Under Beslut-fliken kan du läsa besluten till dina ansökningar och ta emot/annullera platsen',
    confirmationLink: 'Granska och bekräfta beslutet',
    newApplicationLink: 'Ny ansökan'
  }
}

export default sv
