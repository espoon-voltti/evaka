// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { P } from 'lib-components/typography'
import React from 'react'
import { Translations } from 'lib-customizations/citizen'
import ExternalLink from 'lib-components/atoms/ExternalLink'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import { Gap } from 'lib-components/white-space'

const yes = 'Ja'
const no = 'Nej'

const sv: Translations = {
  common: {
    title: 'Småbarnspedagogik',
    cancel: 'Gå tillbaka',
    return: 'Tillbaka',
    ok: 'Ok',
    save: 'Spar',
    discard: 'Spar inte',
    saveConfirmation: 'Vill du spara ändringar?',
    confirm: 'Bekräfta',
    delete: 'Ta bort',
    edit: 'Redigera',
    add: 'Lägg till',
    yes,
    no,
    yesno: (value: boolean): string => (value ? yes : no),
    select: 'Utvalda',
    page: 'Sida',
    unit: {
      providerTypes: {
        MUNICIPAL: 'Kommunal',
        PURCHASED: 'Köptjänst',
        PRIVATE: 'Privat',
        MUNICIPAL_SCHOOL: 'Kommunal',
        PRIVATE_SERVICE_VOUCHER: 'Servicesedel',
        EXTERNAL_PURCHASED: 'Köptjänst (annat)'
      },
      careTypes: {
        CLUB: 'Klubbverksamhet',
        FAMILY: 'Familjedagvård',
        CENTRE: 'Daghem',
        GROUP_FAMILY: 'Gruppfamiljedaghem',
        PRESCHOOL: 'Förskola',
        PREPARATORY_EDUCATION: 'Förberedande undervisning'
      },
      languages: {
        fi: 'finskspråkig',
        sv: 'svenskaspråkig'
      },
      languagesShort: {
        fi: 'suomi',
        sv: 'svenska'
      }
    },
    openExpandingInfo: 'Öppna detaljer',
    errors: {
      genericGetError: 'Hämtning av information misslyckades'
    },
    datetime: {
      weekdaysShort: ['Mån', 'Tis', 'Ons', 'Tor', 'Fre', 'Lör', 'Sön'],
      week: 'Veckan',
      weekShort: 'V',
      weekdays: [
        'Måndag',
        'Tisdag',
        'Onsdag',
        'Torsdag',
        'Fredag',
        'Lördag',
        'Söndag'
      ],
      months: [
        'Januari',
        'Februari',
        'Mars',
        'April',
        'Maj',
        'Juni',
        'Juli',
        'Augusti',
        'September',
        'Oktober',
        'November',
        'December'
      ]
    }
  },
  header: {
    nav: {
      map: 'Karta',
      applications: 'Ansökningar',
      decisions: 'Beslut',
      personalDetails: 'Personuppgifter',
      income: 'Inkomst',
      messages: 'Meddelanden',
      calendar: 'Kalender',
      applying: 'Ansökningar',
      pedagogicalDocuments: 'Tillväxt och inlärning',
      children: 'Barn'
    },
    lang: {
      fi: 'Suomeksi',
      sv: 'På svenska',
      en: 'In English'
    },
    login: 'Logga in',
    logout: 'Logga ut'
  },
  footer: {
    cityLabel: '© Esbo stad',
    privacyPolicy: 'Dataskyddsbeskrivningar',
    privacyPolicyLink: 'https://www.esbo.fi/esbo-stad/dataskydd',
    sendFeedback: 'Skicka feedback',
    sendFeedbackLink:
      'https://easiointi.espoo.fi/eFeedback/sv/Feedback/20-S%C3%A4hk%C3%B6iset%20asiointipalvelut'
  },
  errorPage: {
    reload: 'Ladda om sidan',
    text: 'Vi stötte på ett oväntat fel. Utvecklarna har meddelats.',
    title: 'Något gick fel'
  },
  map: {
    title: 'Enheter på kartan',
    mainInfo:
      'I den här vyn kan du på kartan söka enheter med småbarnspedagogik och förskola.',
    privateUnitInfo: (
      <span>
        För information om privata daghem,{' '}
        <ExternalLink
          text="klicka här."
          href="https://www.esbo.fi/tjanster/privat-smabarnspedagogik"
          newTab
        />
      </span>
    ),
    searchLabel: 'Sök med adress eller enhetens namn',
    searchPlaceholder: 'T.ex. Purola daghem',
    address: 'Adress',
    noResults: 'Inga sökresultat',
    keywordRequired: 'Ange ett sökord',
    distanceWalking: 'Avstånd från vald enhet gående',
    careType: 'Verksamhetsform',
    careTypePlural: 'Verksamhetsformer',
    careTypes: {
      CLUB: 'Klubbverksamhet',
      DAYCARE: 'Småbarnspedagogik',
      PRESCHOOL: 'Förundervisning'
    },
    language: 'Enhetens språk',
    providerType: 'Tjänstleverantör',
    providerTypes: {
      MUNICIPAL: 'kommunala',
      PURCHASED: 'köpavtal',
      PRIVATE: 'privata',
      PRIVATE_SERVICE_VOUCHER: 'servicesedel',
      EXTERNAL_PURCHASED: 'köpavtal (annat)'
    },
    homepage: 'Hemsida',
    unitHomepage: 'Enhetens hemsida',
    route: 'Se rutten till enheten',
    routePlanner: 'Reseplanerare',
    newTab: '(Öppnas till nytt mellanblad)',
    shiftCareTitle: 'Kvälls- och skiftvård',
    shiftCareLabel: 'Visa endast kvälls- och skiftvårds enheter',
    shiftCareYes: 'Enheten erbjuder kvälls- och / eller skifttjänster',
    shiftCareNo: 'Ingen kvälls- och/eller skiftvård',
    showMoreFilters: 'Visa fler filter',
    showLessFilters: 'Visa färre filter',
    nearestUnits: 'Närmaste enheter',
    moreUnits: 'Fler enheter',
    showMore: 'Visa fler sökresultat',
    mobileTabs: {
      map: 'Karta',
      list: 'Enheter'
    },
    serviceVoucherLink:
      'https://www.esbo.fi/fostran-och-utbildning/smabarnspedagogik/servicesedel-smabarnspedagogik#section-6228',
    noApplying: 'Ingen sökning via eVaka, kontakta tjänsten',
    backToSearch: 'Tillbaka till sökning'
  },
  calendar: {
    title: 'Kalender',
    holiday: 'Helgdag',
    absent: 'Frånvarande',
    absences: {
      SICKLEAVE: 'Sjukskriven',
      PLANNED_ABSENCE: 'Ledig dag'
    },
    newReservationOrAbsence: 'Reservation / Frånvaro',
    newAbsence: 'Anmäl frånvaro',
    newReservationBtn: 'Reservera',
    noReservation: 'Ingen reservation',
    reservation: 'Reservation',
    realized: 'Förverkligad',
    reservationsAndRealized: 'Reservationer och förverkligade',
    reservationModal: {
      title: 'Reservera',
      selectChildren: 'Barn som reserveras för',
      selectChildrenLabel: 'Utvalda barn',
      dateRange: 'Reservationens giltighet',
      dateRangeLabel: 'Reservera dagarna',
      missingDateRange: 'Välj barn att reservera',
      repetition: 'Upprepning',
      postError: 'Misslyckades med att reservera',
      repetitions: {
        DAILY: 'Dagligen',
        WEEKLY: 'Veckovis',
        IRREGULAR: 'Irreguljär'
      },
      start: 'Börjar',
      end: 'Slutar'
    },
    absenceModal: {
      title: 'Anmäl frånvaro',
      selectedChildren: 'Utvalda barn',
      selectChildrenInfo:
        'Anmäl endast heldagsfrånvaro. Kortare frånvaro kan anmälas genom reservationer.',
      dateRange: 'Frånvaromeddelande för ',
      absenceType: 'Anledning till frånvaro',
      absenceTypes: {
        SICKLEAVE: 'Sjukdom',
        OTHER_ABSENCE: 'Annan frånvaro',
        PLANNED_ABSENCE: 'Vanlig ledig dag/skiftvård'
      }
    }
  },
  messages: {
    inboxTitle: 'Inkorg',
    noMessagesInfo: 'Skickade och mottagna meddelanden visas här.',
    noSelectedMessage: 'Inget valt meddelande',
    emptyInbox: 'Din inkorg är tom.',
    recipients: 'Mottagare',
    send: 'Skicka',
    sender: 'Avsändare',
    sending: 'Skickas',
    messagePlaceholder:
      'Meddelandeinnehåll... Obs! Ange inte känslig information här.',
    types: {
      MESSAGE: 'Meddelande',
      BULLETIN: 'Bulletin'
    },
    staffAnnotation: 'Personal',
    replyToThread: 'Svar',
    messageEditor: {
      newMessage: 'Nytt Meddelande',
      recipients: 'Mottagare',
      subject: 'Ämne',
      message: 'Meddelande',
      deleteDraft: 'Ta bort utkast',
      send: 'Skicka',
      discard: 'Kassera',
      search: 'Sök',
      noResults: 'Inga resultat',
      messageSendError: 'Misslyckades med att skicka meddelande'
    }
  },
  applications: {
    title: 'Ansökningar',
    deleteDraftTitle: 'Vill du ta bort din ansökan?',
    deleteDraftText:
      'Vill du återta din ansökan? Om du återtar ansökan raderas alla uppgifter.',
    deleteDraftOk: 'Ta bort ansökan',
    deleteDraftCancel: 'Tillbaka',
    deleteSentTitle: 'Vill du återta din ansökan?',
    deleteSentText:
      'Alla uppgifter på din ansökan raderas, också ansökan som du redan skickat raderas.',
    deleteSentOk: 'Ta bort ansökan',
    deleteSentCancel: 'Tillbaka',
    deleteUnprocessedApplicationError: 'Att radera ansökan misslyckades',
    creation: {
      title: 'Val av ansökningsblankett',
      daycareLabel:
        'Ansökan till småbarnspedagogik och ansökan om servicesedel',
      daycareInfo:
        'Med en ansökan till småbarnspedagogisk verksamhet ansöker du om en plats i småbarnspedagogisk verksamhet i ett daghem, hos en familjedagvårdare eller i ett gruppfamiljedaghem. Med samma ansökan kan du också ansöka om servicesedel inom småbarnspedagogiken, genom att vid alternativet Ansökningsönskemål välja den servicesedelenhet som du vill söka till.',
      preschoolLabel:
        'Anmälan till förskoleundervisning och/eller förberedande undervisning',
      preschoolInfo:
        'Förskoleundervisningen är gratis och ordnas fyra timmar per dag. Därtill kan du ansöka om anslutande småbarnspedagogik på samma plats, på morgonen före och på eftermiddagen efter förskoleundervisningen. Du kan också ansöka om servicesedel för småbarnspedagogik i samband med förskoleundervisningen, genom att vid alternativet Ansökningsönskemål välja den servicesedelenhet som du vill söka till. Du kan ansöka om småbarnspedagogik i anslutning till förskoleundervisningen när du anmäler barnet till förskoleundervisningen eller separat efter att förskoleundervisningen har inletts. Du kan också ansöka på en och samma ansökan om både förberedande undervisning, som är gratis, och anslutande småbarnspedagogik.',
      preschoolDaycareInfo:
        'Ansökan om anslutande småbarnspedagogik för ett barn som anmäls/har anmälts till förskoleundervisning eller förberedande undervisning',
      clubLabel: 'Ansökan till en klubb',
      clubInfo:
        'Med ansökan till klubbverksamhet kan du ansöka till kommunala klubbar.',
      duplicateWarning:
        'Ditt barn har en motsvarande oavslutad ansökan. Gå tillbaka till vyn Ansökningar och bearbeta den befintliga ansökan eller ta kontakt med servicehandledningen.',
      transferApplicationInfo: {
        DAYCARE:
          'Barnet har redan en plats i Esbo stads småbarnspedagogik. Med denna ansökan kan du ansöka om byte till en annan enhet som erbjuder småbarnspedagogik.',
        PRESCHOOL:
          'Barnet har redan en förskoleplats. Med denna ansökan kan du ansöka om <strong>småbarnspedagogik i anslutning till förskoleundervisningen</strong> eller om byte till en annan enhet som erbjuder förskoleundervisning.'
      },
      create: 'Ny ansökan',
      daycare4monthWarning: 'Behandlingstiden för ansökningen är 4 månader.',
      applicationInfo: (
        <P>
          Du kan ändra i ansökan så länge den inte har tagits till behandling.
          Därefter kan du göra ändringar i ansökan genom att kontakta
          servicehandledningen inom småbarnspedagogik tfn 09 81627600. Du kan
          återta ansökan som du redan lämnat in genom att meddela detta per
          e-post till servicehandledningen inom småbarnspedagogik{' '}
          <a href="mailto:dagis@esbo.fi">dagis@esbo.fi</a>.
        </P>
      )
    },
    editor: {
      heading: {
        title: {
          DAYCARE: 'Ansökan till småbarnspedagogik och ansökan om servicesedel',
          PRESCHOOL: 'Anmälan till förskolan',
          CLUB: 'Ansökan till klubbverksamhet'
        },
        info: {
          DAYCARE: (
            <>
              <P>
                Du kan ansöka om plats i småbarnspedagogisk verksamhet året om.
                Ansökningen bör lämnas in senast fyra månader före behovet av
                verksamheten börjar. Om behovet börjar med kortare varsel pga.
                sysselsättning eller studier bör du ansöka om plats senast två
                veckor före.
              </P>
              <P>
                Du får ett skriftligt beslut om platsen. Beslutet delges i
                tjänsten{' '}
                <a
                  href="https://www.suomi.fi/meddelanden"
                  target="_blank"
                  rel="noreferrer"
                >
                  Suomi.fi
                </a>
                -meddelanden, eller per post om du inte tagit i bruk
                meddelandetjänsten i Suomi.fi. * Informationen markerad med en
                stjärna krävs.
              </P>
              <P fitted={true}>* Informationen markerad med en stjärna krävs</P>
            </>
          ),
          PRESCHOOL: (
            <>
              <P>
                Barn deltar i förskoleundervisning året innan läroplikten
                börjar. Förskoleundervisningen är avgiftsfri. Anmälningstiden
                till förskoleundervisningen 2022–2023 är 10.–21.1.2022.
                Förskolan börjar i <strong>11.8.2022</strong>. Beslutet delges
                inom mars i{' '}
                <a
                  href="https://www.suomi.fi/meddelanden"
                  target="_blank"
                  rel="noreferrer"
                >
                  Suomi.fi-meddelandetjänsten
                </a>{' '}
                eller per post om du inte tagit i bruk{' '}
                <a
                  href="https://www.suomi.fi/meddelanden"
                  target="_blank"
                  rel="noreferrer"
                >
                  meddelandetjänsten i Suomi.fi
                </a>
                .
              </P>
              <P>
                Du får ett skriftligt beslut om platsen. Beslutet delges i
                tjänsten{' '}
                <a
                  href="https://www.suomi.fi/meddelanden"
                  target="_blank"
                  rel="noreferrer"
                >
                  Suomi.fi
                </a>
                -meddelanden, eller per post om du inte tagit i bruk
                meddelandetjänsten i Suomi.fi.
              </P>
              <P fitted={true}>* Informationen markerad med en stjärna krävs</P>
            </>
          ),
          CLUB: (
            <>
              <P>
                Ansökningsperioden för klubbar som börjar på hösten är i mars.
                Om ditt barn får en klubbplats, du får beslutet om det under
                april-maj. Beslutet fattas för en verksamhetsperiod för perioden
                (augusti till slutet av maj). Beslut om klubbplats kommer till
                Suomi.fi-tjänsten eller per post om du inte har tagit det
                service.
              </P>
              <P>
                En klubbansökan kan också lämnas in utanför ansökningsperioden
                och vidare efter att klubbens säsong redan har börjat. Under
                ansökningsperioden De mottagna ansökningarna behandlas dock
                först och ansökningsperioden ansökningar som tas emot utifrån
                kommer att behandlas i ankomstordning. Klubbansökan är för en
                för klubbsäsongen. I slutet av perioden kommer ansökan att
                raderas systemet.
              </P>
              <P>
                Klubbaktiviteter är gratis, inte deltagande påverkar
                hemvårdsbidraget som betalas av FPA. Om ett barn det istället
                för att beviljas förskoleundervisning eller privat vårdbidrag
                kan han inte beviljas klubbplats.
              </P>
              <P fitted={true}>
                * Informationen markerad med en stjärna krävs.
              </P>
            </>
          )
        },
        errors: (count: number) => (count === 1 ? '1 fel' : `${count} fel`),
        hasErrors:
          'Var så god och kontrollera följande information för din ansökan'
      },
      actions: {
        verify: 'Granska ansökan',
        hasVerified: 'Jag har granskat att uppgifterna är rätt',
        returnToEdit: 'Gå tillbaka och bearbeta ansökan',
        returnToEditBtn: 'Gå tillbaka till vyn Ansökningar',
        cancel: 'Tillbaka',
        send: 'Skicka ansökan',
        update: 'Spara ändringarna',
        sendError: 'Att skicka lyckades inte',
        saveDraft: 'Spara som oavslutad',
        updateError: 'Att spara lyckades inte'
      },
      verification: {
        title: {
          DAYCARE: 'Granska ansökan till småbarnspedagogisk verksamhet',
          PRESCHOOL: 'Granska ansökan till förskola',
          CLUB: 'Var god och granska följande obligatoriska fält i blanketten'
        },
        notYetSent: (
          <P>
            <strong>Du har inte ännu skickat ansökan.</strong>
            Granska uppgifterna och skicka ansökan med <i>Skicka ansökan</i>
            -knappen i slutet av sidan.
          </P>
        ),
        notYetSaved: (
          <P>
            <strong>Ändringarna har inte sparats än.</strong> Granska
            uppgifterna du gett, och skicka ansökan med <i>Spara ändringarna</i>
            -knappen i slutet av sidan.
          </P>
        ),
        no: 'Nej',
        basics: {
          created: 'Skapad',
          modified: 'Uppdaterad'
        },
        attachmentBox: {
          nb: 'Obs!',
          headline:
            'Om du lägger till bilagor elektroniskt i följande punkter, behandlas din ansökan snabbare. Behandlingstiden börjar från bilagornas ankomst.',
          urgency: 'Ansökningen är brådskande',
          shiftCare: 'Kvälls- och skiftvård',
          goBackLinkText: 'Återvänd tillbaka till ansökan vyn',
          goBackRestText: 'för att lägga till bilagor till ansökan.'
        },
        serviceNeed: {
          title: 'Behov av småbarnspedagogisk verksamhet',
          wasOnDaycare: 'I tidig barndomsutbildning innan klubben',
          wasOnDaycareYes:
            'Ett barn för vilket en klubbplats ansöks är i förskoleundervisningen innan klubbens önskade startdatum.',
          wasOnClubCare: 'I klubben under föregående säsong',
          wasOnClubCareYes:
            'Barnet har varit i klubben under föregående operationsperiod\n.',
          connectedDaycare: {
            title:
              'Behov av småbarnspedagogik i samband med förskoleundervisningen.',
            label: 'Småbarnspedagogik i samband med förskoleundervisning',
            withConnectedDaycare:
              'Jag ansöker också om kompletterande småbarnspedagogik utöver förskolan.',
            withoutConnectedDaycare: 'Nej'
          },
          attachments: {
            label: 'Nödvändiga bilagor',
            withoutAttachments: 'Inte bifogats – skickas per post'
          },
          startDate: {
            title: {
              DAYCARE: 'Inledningsdatum',
              PRESCHOOL: 'Inledningsdatum',
              CLUB: 'Inledningsdatum'
            },
            preferredStartDate: 'Önskat inledningsdatum',
            urgency: 'Ansökningen är brådskande',
            withUrgency: 'Ja',
            withoutUrgency: 'Nej'
          },
          dailyTime: {
            title: 'Daglig vårdtid inom småbarnspedagogik börjar och slutar',
            partTime: 'Hel- eller deldag',
            withPartTime: 'Deldag',
            withoutPartTime: 'Heldag',
            dailyTime:
              'Daglig vårdtid inom småbarnspedagogik börjar och slutar',
            shiftCare: 'Kvälls- och skiftvård',
            withShiftCare: 'Behov av kvälls- eller skiftvård',
            withoutShiftCare: 'Behöver inte kvälls- eller skiftvård'
          },
          assistanceNeed: {
            title: 'Behov av stöd',
            assistanceNeed: 'Barnet har stödbehov',
            withAssistanceNeed: 'Barnet har stödbehov',
            withoutAssistanceNeed: 'Barnet har inte stödbehov',
            description: 'Beskrivning av stödbehov'
          },
          preparatoryEducation: {
            label: 'Perusopetukseen valmistava opetus',
            withPreparatory:
              'Barnet behöver stöd för att lära sig finska. Barnet söker också till undervisning som förbereder för den grundläggande utbildningen.',
            withoutPreparatory: 'Nej'
          }
        },
        unitPreference: {
          title: 'Ansökningsönskemål',
          siblingBasis: {
            title: 'Ansökan på basis av syskonrelationer',
            siblingBasisLabel: 'Syskonrelation',
            siblingBasisYes:
              'Jag ansöker i första hand om plats i den enheten där barnets syskon redan har en plats inom småbarnspedagogik',
            name: 'Syskonets för- och efternamn',
            ssn: 'Syskonets personbeteckning'
          },
          units: {
            title: 'Ansökningsönskemål',
            label: 'Utvalda enheter'
          }
        },
        contactInfo: {
          title: 'Personuppgifter',
          child: {
            title: 'Barnets uppgifter',
            name: 'Barnets namn',
            ssn: 'Barnets personbeteckning',
            streetAddress: 'Hemadress',
            isAddressChanging: 'Adressen har ändrats/kommer att ändras',
            hasFutureAddress:
              'Adressen som finns i befolkningsdatabasen har ändrats/kommer att ändras',
            addressChangesAt: 'Flyttdatum',
            newAddress: 'Ny adress'
          },
          guardian: {
            title: 'Vårdnadshavarens information',
            name: 'Namn',
            ssn: 'Personbeteckning',
            streetAddress: 'Hemadress',
            tel: 'Telefonnummer',
            email: 'E-postadress',
            isAddressChanging: 'Adressen har ändrats/kommer att ändras',
            hasFutureAddress: 'Adressen har ändrats/kommer att ändras',
            addressChangesAt: 'Flyttdatum',
            newAddress: 'Ny adress'
          },
          secondGuardian: {
            title: 'Uppgifter om den andra vårdnadshavaren',
            email: 'E-postadress',
            tel: 'Telefonnummer',
            info: 'Den andra vårdnadshavarens information hämtas automatiskt från befolkningsinformationssystemet.',
            agreed: 'Vi har tillsammans kommit överens att fylla i ansökan.',
            notAgreed: 'Vi har inte kunnat komma överens om ansökan.',
            rightToGetNotified:
              'Den andra vårdnadshavaren har endast rätt att få uppgifter om barnet.',
            noAgreementStatus: 'Okänd'
          },
          fridgePartner: {
            title: 'Maka/make bosatt i samma hushåll (icke vårdnadshavare)',
            fridgePartner:
              'Maka/make bosatt i samma hushåll (icke vårdnadshavare)',
            name: 'Namn',
            ssn: 'Personbeteckning'
          },
          fridgeChildren: {
            title: 'Barn som bor i samma hushåll',
            name: 'Barnets namn',
            ssn: 'Personbeteckning',
            noOtherChildren: 'Inga andra barn'
          }
        },
        additionalDetails: {
          title: 'Övriga tilläggsuppgifter',
          otherInfoLabel: 'Tilläggsuppgifter till ansökan',
          dietLabel: 'Specialdiet',
          allergiesLabel: 'Allergier'
        }
      },
      serviceNeed: {
        title: 'Vårdbehov',
        startDate: {
          header: {
            DAYCARE: 'Inledningsdatum för småbarnspedagogik',
            PRESCHOOL: 'Inledningsdatum för förskoleundervisning',
            CLUB: 'Inledningsdatum'
          },
          info: {
            DAYCARE: [],
            PRESCHOOL: [
              'Förskolan börjar i 11.8.2022. Om ditt barn behöver småbarnspedagogisk verksamhet fr.o.m. 1.8.2022 före förskolan börjar kan du ansöka om sådan med denna blankett. Välj ”Jag ansöker om småbarnspedagogik i samband med förskoleundervisning”.'
            ],
            CLUB: [
              'Klubbar följer förskolans arbetstid och semester. Klubbens verksamhetsperiod är från augusti till slutet av maj och ansökas om separat för varje operationsperiod. Olika klubbar möts olika veckodagar.'
            ]
          },
          clubTerm: 'Klubbens verksamhetsperiod',
          clubTerms: 'Klubbens verksamhetsperioder',
          label: {
            DAYCARE: 'Önskat inledningsdatum',
            PRESCHOOL: 'Inledningsdatum i augusti',
            CLUB: 'Önskat inledningsdatum'
          },
          noteOnDelay: 'Behandlingstiden för ansökningen är 4 månader.',
          instructions: (
            <>
              Det är möjligt att senarelägga det önskade startdatumet så länge
              ansökan inte har tagits upp till behandling. Därefter kan du ändra
              det önskade startdatumet genom att kontakta servicehandledningen
              inom småbarnspedagogik (tfn 09 816 27600).
            </>
          ),
          placeholder: 'Välj inledningsdatum',
          validationText: 'Önskat inledningsdatum: '
        },
        clubDetails: {
          wasOnDaycare:
            'Barnet har en tidig småbarnspedagogikplats, som han ger upp när han får en klubbplats',
          wasOnDaycareInfo:
            'Om ett barn har gått i förskoleundervisningen (dagis, familjedaghem eller gruppfamiljedaghem) och lämnar sin plats i början av klubben, har han eller hon större chans att få en klubbplats.',
          wasOnClubCare:
            'Barnet har varit i klubben under föregående operationsperiod.',
          wasOnClubCareInfo:
            'Om barnet redan har varit på klubben under föregående säsong har han eller hon större chans att få en plats i klubben.'
        },
        urgent: {
          label: 'Ansökningen är brådskande',
          attachmentsMessage: {
            text: (
              <P fitted={true}>
                Om behovet av en plats inom småbarnspedagogiken beror på att du
                plötsligt fått sysselsättning eller börjat studera, ska platsen
                sökas senast <strong>två veckor innan</strong> behovet börjar.
                Bifoga till ansökan ett arbets- eller studieintyg av båda
                vårdnadshavarna som bor i samma hushåll. Om du inte kan lägga
                till bilagor till ansökan elektroniskt, skicka dem per post till
                adressen Servicehandledningen inom småbarnspedagogik PB 32,
                02070 Esbo stad. Behandlingstiden på två veckor börjar när vi
                har tagit emot ansökan och bilagorna som behövs.
              </P>
            ),
            subtitle:
              'Lägg här till ett arbets- eller studieintyg av båda föräldrarna.'
          }
        },
        partTime: {
          true: 'Deldag (max 5h/dag, 25h/vecka)',
          false: 'Heldag'
        },
        dailyTime: {
          label: {
            DAYCARE: 'Tiden för småbarnspedagogik per dag',
            PRESCHOOL:
              'Tiden för småbarnspedagogik i samband med förskoleundervisning'
          },
          connectedDaycareInfo: (
            <>
              <P>
                Du kan vid behov ansöka om avgiftsbelagd småbarnspedagogik i
                samband med förskoleundervisningen. Småbarnspedagogik erbjuds
                utöver förskoleundervisningen (fyra timmar per dag) på morgnar
                och eftermiddagar på samma ställe som förskoleundervisningen. Om
                barnet börjar senare, meddela detta under{' '}
                <i>Tilläggsuppgifter</i>.
              </P>
              <P>
                Plats i anslutning till förskoleundervisningen i en privat enhet
                för småbarnspedagogik ska sökas direkt hos enheten (med undantag
                för servicesedelenheter), enheten informerar om hur man ansöker.
                I dessa fall omvandlar servicehänvisningen ansökan till en
                ansökan om endast förskoleundervisning.
              </P>
              <P>
                Du ansöker om en servicesedel genom att som ansökningsönskemål
                välja den servicesedelenhet du vill söka till.
              </P>
              <P>
                Du får ett separat skriftligt beslut om platsen inom
                småbarnspedagogik. Beslutet delges i tjänsten
                Suomi.fi-meddelanden eller per post, om du inte har tagit
                tjänsten i bruk.
              </P>
            </>
          ),
          connectedDaycare:
            'Jag ansöker också om småbarnspedagogik i samband med förskoleundervisning.',
          instructions: {
            DAYCARE:
              'Meddela tiden då ditt barn behöver småbarnspedagogisk verksamhet. Du kan meddela den mera exakta tiden när verksamheten börjar. Om ditt behov varierar dagligen eller per vecka (t.ex i skiftvård) kan du meddela behovet mer exakt i tilläggsuppgifterna.',
            PRESCHOOL:
              'Förskoleundervisning ordnas fyra timmar per dag, huvudsakligen kl. 9.00-13.00, men tiden kan variera per enhet. Meddela tiden för småbarnspedagogik så att den innefattar tiden för förskoleundervisning som är fyra timmar (t.ex. kl. 7.00–17.00). Vårdnadshavare meddelar de mera exakta tiderna när småbarnspedagogiken börjar.  Om behovet av småbarnspedagogik varierar dagligen eller per vecka (t.ex. i skiftvård), meddela behovet mer exakt i tilläggsuppgifterna.'
          },
          usualArrivalAndDeparture: {
            DAYCARE: 'Den dagliga start-och sluttiden för småbarnspedagogik',
            PRESCHOOL:
              'Tiden för småbarnspedagogik per dag (inkluderar förskoleundervisning)'
          },
          starts: 'Börjar',
          ends: 'Sluttiden'
        },
        shiftCare: {
          label: 'Kvälls- och skiftvård',
          instructions:
            'Med skiftvård avses verksamhet under veckosluten eller dygnet runt. Kvälls- och skiftvård är vård som huvudsakligen sker under annan tid än vardagar klockan 6.30-18.00. Om ditt barn behöver skiftvård, beskriv behovet i fältet för tilläggsuppgifter.',
          message: {
            title: 'Kvälls- och skiftvård',
            text: 'Kvälls- och skiftvård är till för barn vars båda föräldrar jobbar i skiften eller studerar huvudsakligen kvällstid och under veckoslut. Som bilaga till ansökan ska ett intyg om skiftesarbete eller studier lämnas in av båda vårdnadshavarna.'
          },
          attachmentsMessage: {
            text: 'Kvälls- och skiftvård är avsedd för barn vars båda föräldrar har skiftarbete eller studerar huvudsakligen på kvällar och/eller veckoslut. Som bilaga till ansökan ska av båda föräldrarna lämnas ett intyg av arbetsgivaren över skiftarbete eller studier som orsakar behovet av kvälls- eller skiftomsorg. Vi rekommenderar att bilagan skickas elektroniskt här. Om du inte kan lägga till bilagor till ansökan elektroniskt, skicka dem per post till adressen Servicehandledning inom småbarnspedagogik PB 32, 02070 Esbo stad.',
            subtitle:
              'Lägg här till för båda föräldrarna antingen arbetsgivarens intyg över skiftarbete eller ett intyg över studier på kvällar/veckoslut.'
          }
        },
        assistanceNeed: 'Behov av stöd för utveckling och lärande',
        assistanceNeeded: 'Barnet har behov av stöd för utveckling och lärande',
        assistanceNeedLabel: 'Beskrivning av stödbehov',
        assistanceNeedPlaceholder:
          'Berätta om barnets behov av stöd för utveckling och lärande',
        assistanceNeedInstructions: {
          DAYCARE:
            'Med behov av stöd för utveckling och lärande avses behov av sådana stödåtgärder som har konstaterats i ett sakkunnigutlåtande. Om ditt barn behöver stöd för utveckling eller lärande, kontaktar specialläraren inom småbarnspedagogik dig, så att vi ska kunna beakta barnets behov vid beviljandet av plats inom småbarnspedagogik.',
          CLUB: 'Med behov av stöd för utveckling och lärande avses behov av sådana stödåtgärder som har konstaterats i ett sakkunnigutlåtande. Om ditt barn behöver stöd för utveckling eller lärande, kontaktar specialläraren inom småbarnspedagogik dig, så att vi ska kunna beakta barnets behov vid beviljandet av plats inom småbarnspedagogik.',
          PRESCHOOL:
            'Om ditt barn behöver stöd för utveckling eller lärande, kontaktar specialläraren inom småbarnspedagogik dig, så att vi ska kunna beakta barnets behov vid beviljandet av plats inom  förskoleundervisning.'
        },
        preparatory:
          'Barnet behöver stöd för att lära sig finska. Barnet söker också till undervisning som förbereder för den grundläggande utbildningen.',
        preparatoryInfo:
          'Den förberedande undervisningen för förskoleelever är avsedd för barn med invandrarbakgrund, återinvandrare, barn i tvåspråkiga familjer (med undantag av finska-svenska) och adoptivbarn som behöver stöd i det finska språket innan de inleder den grundläggande utbildningen. Barnet har rätt att delta i undervisning som förbereder för den grundläggande utbildningen under ett läsårs tid. Förberedande undervisning ordnas 5 timmar per dag. Undervisningen är avgiftsfri.'
      },
      unitPreference: {
        title: 'Ansökningsönskemål',
        siblingBasis: {
          title: 'Ansökan på basis av syskonrelationer',
          info: {
            DAYCARE: (
              <>
                <P>
                  Barnet har syskonprincip till samma småbarnspedagogisk enhet
                  som hens syskon är när beslutet för småbarnspedagogik fattas.
                  Som syskon betraktas barn som är folkbokförda på samma adress.
                  Målet är att placera syskon i samma enhet om inte familjen
                  önskar annat. Om du ansöker om en plats för syskon, som inte
                  ännu har plats inom småbarnspedagogik, skriv uppgiften i
                  tilläggsuppgifter. Fyll i dessa uppgifter endast om du vill
                  hänvisa till barnets syskonrelationer.
                </P>
              </>
            ),
            PRESCHOOL: (
              <>
                <P>
                  Fyll i dessa uppgifter endast om du yrkar på
                  prioriteringsordningen till sekundär förskola
                </P>
                <P>
                  Vårdnadshavarna kan ansöka om plats för sitt barn i en annan
                  förskola än närförskolan. Sökande från andra
                  elevupptagningsområden kan antas endast om det finns lediga
                  platser efter att barnen i förskolans eget
                  elevupptagningsområde har fått en plats. Om det finns flera
                  sökande än det finns platser tillämpas följande
                  prioriteringsordning. Om du hänvisar till
                  kontinuitetsprincipen skriv barnets nuvarande enhet i fältet
                  för tilläggsuppgifter.
                </P>
                <ol type="a">
                  <li>
                    Kontinuitet vid övergång från småbarnspedagogik till
                    förskola. Vid antagning till förskola prioriteras ett barn
                    som haft sin dagvårdsplats i det daghem där förskolan är.
                  </li>
                  <li>
                    Barn med syskon i skolan som finns i samma
                    elevupptagningsområde. Syskonprincipen innebär att elever
                    placeras i samma enhet som äldre syskon. För barn i
                    förskolan tillämpas principen om eleven har syskon i årskurs
                    1–6 i skolan som finns i samma elevupptagningsområde som
                    förskolan. Som syskon betraktas barn som är folkbokförda på
                    samma adress.
                  </li>
                </ol>
                <P>
                  Fyll i dessa uppgifter endast om du vill hänvisa till barnets
                  syskonrelationer.
                </P>
              </>
            ),
            CLUB: (
              <>
                <P>
                  Barn som bor på samma adress betraktas som syskon. Ett försök
                  görs för att placera syskonen i samma klubbgrupp när familjen
                  önskar det.
                </P>
                <P>
                  Fyll i dessa uppgifter endast om du vill hänvisa till barnets
                  syskonrelationer och välj samma klubb som syskonet deltar i
                  nedan.
                </P>
              </>
            )
          },
          checkbox: {
            DAYCARE:
              'Jag ansöker i första hand om plats i den enheten där barnets syskon redan har en plats.',
            PRESCHOOL:
              'Jag ansöker om plats i en annan förskola än närförskolan med syskonprincipen',
            CLUB: 'Jag ansöker främst om en plats i samma klubb där barnets syskon deltar.'
          },
          radioLabel: {
            DAYCARE: 'Välj syskonet',
            PRESCHOOL: 'Välj syskonet',
            CLUB: 'Välj syskonet'
          },
          otherSibling: 'Annat syskon',
          names: 'Syskonets för- och efternamn',
          namesPlaceholder: 'För- och efternamn',
          ssn: 'Syskonets personbeteckning',
          ssnPlaceholder: 'Personbeteckning'
        },
        units: {
          title: 'Ansökningsönskemål',
          startDateMissing:
            'För att kunna välja önskade enheter, välj först det önskade startdatumet i avsnittet om "Behov av småbarnspedagogisk verksamhet"',
          info: {
            DAYCARE: (
              <>
                <P>
                  Du kan ange 1-3 platser i önskad ordning. Önskemålen
                  garanterar inte en plats i den önskade enheten, men
                  möjligheterna att få en önskad plats ökar om du anger flera
                  alternativ. Du kan se var enheterna är belägna, genom att
                  välja <i>Enheter på kartan</i>.
                </P>
                <P>
                  Du ansöker om en servicesedel genom att som ansökningsönskemål
                  välja den servicesedelenhet du vill söka till. När familjen
                  ansöker till en servicesedel enhet får även ledaren inom
                  småbarnspedagogik informationen.
                </P>
              </>
            ),
            PRESCHOOL: (
              <>
                <P>
                  Du kan ange 1-3 platser i önskad ordning. Önskemålen
                  garanterar inte en plats i den önskade enheten, men
                  möjligheterna att få en önskad plats ökar om du anger flera
                  alternativ. Du kan se var enheterna är belägna, genom att
                  välja <i>Enheter på kartan</i>.
                </P>
                <P>
                  Du ansöker om en servicesedel genom att som ansökningsönskemål
                  välja den servicesedelenhet du vill söka till. När familjen
                  ansöker till en servicesedel enhet får även ledaren inom
                  småbarnspedagogik informationen.
                </P>
              </>
            ),
            CLUB: (
              <>
                <P>
                  Du kan ange 1-3 platser i önskad ordning. Önskemålen
                  garanterar inte en plats i den önskade enheten, men
                  möjligheterna att få en önskad plats ökar om du anger flera
                  alternativ. Du kan se var enheterna är belägna, genom att
                  välja <i>Enheter på kartan</i>.
                </P>
              </>
            )
          },
          mapLink: 'Enheter på kartan',
          serviceVoucherLink:
            'https://www.esbo.fi/fostran-och-utbildning/smabarnspedagogik/servicesedel-smabarnspedagogik#section-6228',
          languageFilter: {
            label: 'Enhetens språk:',
            fi: 'finska',
            sv: 'svenska'
          },
          select: {
            label: 'Välj önskade enheter',
            placeholder: 'Sök enheter',
            maxSelected: 'Max antal valda enheter.',
            noOptions: 'Inga ansökningar som motsvarar sökkriterier.'
          },
          preferences: {
            label: 'Enheter som du har valt',
            noSelections: 'Inga val',
            info: 'Välj minst 1 och högst 3 enheter och ange dem i önskad ordning. Du kan ändra på ordningsföljden med hjälp av pilarna.',
            fi: 'Finskspråkig',
            sv: 'Svenskspråkig',
            en: 'Engelskspråkig',
            moveUp: 'Flytta uppåt',
            moveDown: 'Flytta neråt',
            remove: 'Ta bort önskad enhet'
          }
        }
      },
      fee: {
        title: 'Avgiften för småbarnspedagogik',
        info: {
          DAYCARE: (
            <P>
              Klientavgiften inom den kommunala småbarnspedagogiken och för
              servicesedelns självriskandel är en procentandel av familjens
              bruttoinkomster. Avgiften varierar från avgiftsfri
              småbarnspedagogik till högst 288 euro i månaden, beroende på
              familjens storlek, familjens inkomster och barnets vårdtid för
              familjens första barn. Servicesedel enheterna kan dock ta ut
              tilläggspris mellan 0-50€/mån/barn. Familjen ska lämna in en
              utredning över sina bruttoinkomster på en särskild blankett,
              senast inom två veckor från det att barnet har inlett
            </P>
          ),
          PRESCHOOL: (
            <P>
              Förskoleundervisning är avgiftsfri, men för småbarnspedagogik i
              samband med förskoleundervisning uppbärs en avgift. Om barnet
              deltar i småbarnspedagogisk verksamhet i samband med
              förskoleundervisning ska familjen lämna in en utredning över sina
              bruttoinkomster på en särskild blankett, senast inom två veckor
              från det att barnet har inlett.
            </P>
          ),
          CLUB: <P></P>
        },
        emphasis: (
          <strong>
            Om familjen samtycker till den högsta avgiften behövs ingen
            inkomstutredning.
          </strong>
        ),
        checkbox:
          'Jag ger mitt samtycke till att betala den högsta avgiften. Samtycket gäller tills vidare, tills jag meddelar något annat.',
        links: (
          <P>
            Mer information om småbarnspedagogikens avgifter, servicesedelns
            tilläggspris och blanketten för inkomstutredning finns här:
            <br />
            <a
              href="https://www.esbo.fi/fostran-och-utbildning/smabarnspedagogik/klientavgifter-i-smabarnspedagogik"
              target="_blank"
              rel="noopener noreferrer"
            >
              Avgifter för småbarnspedagogik
            </a>
          </P>
        )
      },
      additionalDetails: {
        title: 'Övriga tilläggsuppgifter',
        otherInfoLabel: 'Övriga tilläggsuppgifter',
        otherInfoPlaceholder:
          'Du kan ge noggrannare uppgifter till din ansökan i det här fältet',
        dietLabel: 'Specialdiet',
        dietPlaceholder: 'Du kan meddela barnets specialdiet i det här fältet',
        dietInfo: (
          <>
            För en del specialdieter behövs även ett skilt läkarintyg som lämnas
            in till enheten. Undantag är laktosfri eller laktosfattig diet, diet
            som grundar sig på religiösa orsaker eller vegetarisk kost
            (lakto-ovo).
          </>
        ),
        allergiesLabel: 'Allergier',
        allergiesPlaceholder:
          'Du kan meddela barnets allergier i det här fältet',
        allergiesInfo:
          'Information om allergier behövs när du ansöker till familjedagvård.'
      },
      contactInfo: {
        title: 'Personuppgifter',
        familyInfo: (
          <P>
            Meddela på ansökningen alla de vuxna och barn som bor i samma
            hushåll.
          </P>
        ),
        info: (
          <P>
            Personuppgifterna hämtas från befolkningsdatabasen och du kan inte
            ändra dem med den här ansökan. Om det finns fel i personuppgifterna,
            vänligen uppdatera uppgifterna på webbplatsen{' '}
            <a
              href="https://dvv.fi/sv/kontroll-av-egna-uppgifter-service"
              target="_blank"
              rel="noopener noreferrer"
            >
              dvv.fi
            </a>{' '}
            (Myndigheten för digitalisering och befolkningsdata). Ifall adressen
            kommer att ändras, kan du lägga till den nya adressen på ett separat
            ställe i ansökan. Fyll i den nya adressen både för vårdnadshavare
            och barnet. Adressuppgifterna är officiella först när de har
            uppdaterats av myndigheten för digitalisering och befolkningsdata.
            Beslutet om barnets plats inom småbarnspedagogiken eller
            förskoleundervisningen skickas automatiskt också till en
            vårdnadshavare som bor på en annan adress enligt
            befolkningsregistret.
          </P>
        ),
        emailInfoText:
          'E-postadressen används för att meddela om nya eVaka-meddelanden. Den aktuella e-postadressen har hämtats från eVakas kundregister. Om du ändrar det kommer det gamla e-postmeddelandet att uppdateras när ansökan har skickats.',
        childInfoTitle: 'Barnets information',
        childFirstName: 'Alla förnamn',
        childLastName: 'Efternamn',
        childSSN: 'Personbeteckning',
        homeAddress: 'Hemadress',
        moveDate: 'Flyttdatum',
        street: 'Gatuadress',
        postalCode: 'Postnummer',
        postOffice: 'Postanstalt',
        guardianInfoTitle: 'Vårdnadshavarens uppgifter',
        guardianFirstName: 'Vårdnadshavarens alla förnamn',
        guardianLastName: 'Efternamn',
        guardianSSN: 'Personbeteckning',
        phone: 'Telefonnummer',
        verifyEmail: 'Verifiera e-postadress',
        email: 'E-postadress',
        noEmail: 'Jag har inte en e-postadress',
        secondGuardianInfoTitle: 'Uppgifter om den andra vårdnadshavaren',
        secondGuardianInfo:
          'Den andra vårdnadshavarens information hämtas automatiskt från befolkningsinformationssystemet.',
        secondGuardianNotFound:
          'Baserat på information från Befolkningsdatasystemet har barnet ingen andra vårdnadshavare',
        secondGuardianInfoPreschoolSeparated:
          'Enligt våra uppgifter bor barnets andra vårdnadshavare på en annan adress. Du ska avtala om ansökan om småbarnspedagogik med en annan vårdnadshavare.',
        secondGuardianAgreementStatus: {
          label:
            'Har du kommit överens om ansökan med den andra vårdnadshavaren?',
          AGREED: 'Vi har tillsammans kommit överens om att fylla i ansökan.',
          NOT_AGREED: 'Vi har inte kunnat komma överens om ansökan.',
          RIGHT_TO_GET_NOTIFIED:
            'Den andra vårdnadshavaren har endast rätt att få uppgifter om barnet.'
        },
        secondGuardianPhone: 'Den andra vårdnadshavarens telefonnummer',
        secondGuardianEmail: 'Den andra vårdnadshavarens e-postadress',
        otherPartnerTitle:
          'Maka/make bosatt i samma hushåll (icke vårdnadshavare)',
        otherPartnerCheckboxLabel:
          'Maka/make som bor i samma hushåll men är inte barnets vårdnadshavare',
        personFirstName: 'Alla förnamn',
        personLastName: 'Efternamn',
        personSSN: 'Personbeteckning',
        otherChildrenTitle: 'Familjens övriga barn under 18 år i samma hushåll',
        otherChildrenInfo:
          'Barn som bor i samma hushåll påverkar avgifterna för småbarnspedagogik.',
        otherChildrenChoiceInfo: 'Lägg till ett barn i samma hushåll',
        hasFutureAddress:
          'Adressen som finns i befolkningsdatabasen har ändrats/ska ändras',
        futureAddressInfo:
          'Esbos småbarnspedagogik betraktar adressen i befolkningsdatabasen som den officiella adressen. Adressen ändras i befolkningsregistret när du gör en flyttanmälan till posten eller magistraten.',
        guardianFutureAddressEqualsChildFutureAddress:
          'Jag flyttar till samma adress som barnet\n',
        firstNamePlaceholder: 'Alla förnamn',
        lastNamePlaceholder: 'Efternamn',
        ssnPlaceholder: 'Personbeteckning',
        streetPlaceholder: 'Gatuadress',
        postalCodePlaceholder: 'Postnummer',
        municipalityPlaceholder: 'Postanstalt',
        addChild: 'Lägg till ett barn',
        remove: 'Ta bort',
        areExtraChildren:
          'Övriga barn under 18 år som bor i samma hushåll (t.ex. makas/makes)',
        choosePlaceholder: 'Välj'
      },
      draftPolicyInfo: {
        title: 'Utkastet till ansökan har sparats',
        text: 'Ansökan har sparats som halvfärdig. Obs! En halvfärdig ansökan förvaras i tjänsten i en månad efter att den senast sparats',
        ok: 'Klart'
      },
      sentInfo: {
        title: 'Ansökan har lämnats in',
        text: 'Om du vill kan du göra ytterligare ändringar i ansökan så länge ansökan inte tagits till behandling.',
        ok: 'Klart!'
      },
      updateInfo: {
        title: 'Ändringar i ansökan har sparats.',
        text: 'Om du vill kan du göra ändringar i ansökan så länge ansökan inte har behandlats.',
        ok: 'Klart!'
      }
    }
  },
  decisions: {
    title: 'Beslut',
    summary: (
      <P width="800px">
        Till denna sida kommer beslut gällande barnets ansökan till
        småbarnspedagogik och förskola. Du ska omedelbart eller senast två
        veckor från mottagandet av ett beslut ta emot eller annullera platsen /
        platserna.
      </P>
    ),
    unconfirmedDecisions: (n: number) => `${n} beslut inväntar bekräftelse`,
    pageLoadError: 'Hämtning av information misslyckades',
    applicationDecisions: {
      decision: 'Beslut',
      type: {
        CLUB: 'klubbverksamhet',
        DAYCARE: 'småbarnspedagogik',
        DAYCARE_PART_TIME: 'deldag småbarnspedagogik',
        PRESCHOOL: 'förskola',
        PRESCHOOL_DAYCARE: 'kompletterande småbarnspedagogik',
        PREPARATORY_EDUCATION: 'förberedande undervisning'
      },
      childName: 'Barnets namn',
      unit: 'Enhet',
      period: 'För tiden',
      sentDate: 'Beslutsdatum',
      resolved: 'Bekräftat',
      statusLabel: 'Status',
      summary:
        'Du ska omedelbart eller senast två veckor från mottagandet av ett beslut ta emot eller avvisa platsen / platserna.',
      status: {
        PENDING: 'Bekräftas av vårdnadshavaren',
        ACCEPTED: 'Bekräftad',
        REJECTED: 'Avvisad'
      },
      confirmationInfo: {
        preschool:
          'Du ska omedelbart eller senast två veckor från mottagandet av detta beslut, ta emot eller annullera platsen. Du kan ta emot eller annullera platsen elektroniskt på adressen espoonvarhaiskasvatus.fi (kräver identifiering) eller per post.',
        default:
          'Du ska omedelbart eller senast två veckor från mottagandet av ett beslut ta emot eller annullera platsen.'
      },
      goToConfirmation:
        'Gå till beslutet för att läsa det och svara om du tar emot eller avvisar platsen.',
      confirmationLink: 'Gå vidare för att bekräfta',
      response: {
        title: 'Mottagande eller avvisande av plats',
        accept1: 'Vi tar emot platsen från och med',
        accept2: '',
        reject: 'Vi tar inte emot platsen',
        cancel: 'Gå tillbaka utan att svara',
        submit: 'Skicka svar på beslutet',
        disabledInfo:
          'OBS! Du kan bekräfta/avvisa beslutet gällande kompletterande småbarnspedagogik, om du först bekräftar beslutet för förskola..'
      },
      openPdf: 'Visa beslut',
      warnings: {
        decisionWithNoResponseWarning: {
          title: 'Ett annat beslut väntar på ditt godkännande',
          text: 'Ett annat beslut väntar på ditt godkännande. Vill du gå tillbaka till listan utan att svara?',
          resolveLabel: 'Gå tillbaka utan att svara',
          rejectLabel: 'Fortsätt att svara'
        },
        doubleRejectWarning: {
          title: 'Vill du avvisa platsen?',
          text: 'Du tänker avvisa erbjuden förskoleplats. Den kompletterande småbarnspedagogiken markeras samtidigt annulerad.',
          resolveLabel: 'Avvisa båda',
          rejectLabel: 'Gå tillbaka'
        }
      },
      errors: {
        pageLoadError: 'Informations sök misslyckades',
        submitFailure: 'Misslyckades att skicka svar'
      },
      returnToPreviousPage: 'Tillbaka'
    }
  },
  applicationsList: {
    title: 'Ansökan till småbarnspedagogik eller anmälan till förskolan',
    summary: (
      <P width="800px">
        Barnets vårdnadshavare kan anmäla barnet till förskolan eller ansöka om
        plats i småbarnspedagogisk verksamhet. Med samma ansökan kan du ansöka
        om servicesedel inom småbarnspedagogik när du ansöker om en plats i en
        servicesedelenhet. Uppgifter om vårdnadshavarens barn kommer automatiskt
        från befolkningsdatabasen till denna sida.
      </P>
    ),
    pageLoadError: 'Tietojen hakeminen ei onnistunut',
    noApplications: 'Inga ansökningar',
    type: {
      DAYCARE: 'Ansökan till småbarnspedagogik',
      PRESCHOOL: 'Ansökan till förskolan',
      CLUB: 'Ansökan till klubbverksamhet'
    },
    transferApplication: 'Ansökan om överföring',
    unit: 'Enhet',
    period: 'För tiden',
    created: 'Ansökan skapad',
    modified: 'Redigerad/uppdaterad',
    status: {
      title: 'Status',
      CREATED: 'Utkast',
      SENT: 'Skickat',
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
    editApplicationLink: 'Uppdatera',
    removeApplicationBtn: 'Ta bort ansökan',
    cancelApplicationBtn: 'Ta bort ansökan',
    confirmationLinkInstructions:
      'Under Beslut-fliken kan du läsa besluten till dina ansökningar och ta emot/annullera platsen',
    confirmationLink: 'Gå vidare för att svara',
    newApplicationLink: 'Ny ansökan'
  },
  fileUpload: {
    loading: 'Uppladdas...',
    loaded: 'Uppladdad',
    error: {
      EXTENSION_MISSING: 'Filtillägget saknas',
      EXTENSION_INVALID: 'Ogiltigt filtillägg',
      INVALID_CONTENT_TYPE: 'Ogiltig filtyp',
      FILE_TOO_LARGE: 'För stor fil (max. 10MB)',
      SERVER_ERROR: 'Uppladdningen misslyckades'
    },
    input: {
      title: 'Lägg till bilaga',
      text: [
        'Tryck här eller dra en bilaga åt gången till lådan.',
        'Maximal storlek för filen: 10 MB.',
        'Tillåtna format:',
        'PDF, JPEG/JPG, PNG och DOC/DOCX'
      ]
    },
    deleteFile: 'Radera fil'
  },
  fileDownload: {
    modalHeader: 'Behandling av filen pågår',
    modalMessage: 'Filen kan inte öppnas just nu. Försök igen om en stund.',
    download: 'Ladda ner'
  },
  personalDetails: {
    title: 'Personuppgifter',
    description: (
      <P>
        Här kan du kontrollera och komplettera dina egna person- och
        kontaktuppgifter. Vi hämtar ditt namn och din adress i
        befolkningsdatasystemet och om de ändras ska du göra en anmälan till
        myndigheten för digitalisering och befolkningsdata (DVV).
      </P>
    ),
    noEmailAlert:
      'Din epostadress saknas. Var god och fyll i den nedan, så att du kan ta emot notiser från eVaka.',
    personalInfo: 'Personuppgifter',
    name: 'Namn',
    preferredName: 'Tilltalsnamn',
    contactInfo: 'Kontakt information',
    address: 'Adress',
    phone: 'Telefonnummer',
    backupPhone: 'Reservtelefonnummer',
    backupPhonePlaceholder: 'T.ex. arbetstelefon',
    email: 'E-postadress',
    emailMissing: 'E-postadress saknas',
    noEmail: 'Jag har ingen e-postadress',
    emailInfo:
      'En epostadress behövs så att vi kan skicka notiser om nya meddelanden, bokningar av närvarotider samt andra angelägenheter angående ditt barns småbarnspedagogik.'
  },
  income: {
    title: 'Inkomstuppgifter',
    description: (
      <>
        <p>
          På denna sida kan du skicka utredningar om dina inkomster som påverkar
          avgiften för småbarnspedagogik. Du kan också granska, redigera eller
          radera inkomstutredningar som du har lämnat in tills myndigheten har
          behandlat uppgifterna. Efter att blanketten har behandlats kan du
          uppdatera dina inkomstuppgifter genom att lämna in en ny blankett.
        </p>
        <p>
          Avgifterna för kommunal småbarnspedagogik beräknas i procentandel av
          familjens bruttoinkomster. Avgifterna varierar beroende på familjens
          storlek och inkomster samt barnets vårdtid inom småbarnspedagogik.
          Kontrollera{' '}
          <a
            target="_blank"
            rel="noreferrer"
            href="https://www.espoo.fi/sv/fostran-och-utbildning/smabarnspedagogik/avgifter-inom-smabarnspedagogik"
          >
            här
          </a>{' '}
          om du behöver lämna in en inkomstutredning, eller om er familj
          automatiskt omfattas av den högsta avgiftsklassen för
          småbarnspedagogik.
        </p>
        <p>
          Mer information om avgifterna:{' '}
          <a href="https://www.espoo.fi/sv/fostran-och-utbildning/smabarnspedagogik/avgifter-inom-smabarnspedagogik">
            Avgifter inom småbarnspedagogik
          </a>
        </p>
      </>
    ),
    formTitle: 'Anmälan av inkomstuppgifter',
    formDescription: (
      <>
        <P>
          Inkomstutredningen jämte dess bilagor lämnas in inom två veckor efter
          att småbarnspedagogiken startats. En bristfällig inkomstutredning kan
          leda till den högsta avgiften.
        </P>
        <P>
          Klientavgiften tas ut från och med den dag då småbarnspedagogiken
          startar.
        </P>
        <P>
          Klienten ska omedelbart informera om förändringar i inkomst och
          familjestorlek till enheten för klientavgifter. Myndigheten har vid
          behov rätt att bära upp avgifterna för småbarnspedagogiken även
          retroaktivt.
        </P>
        <P>
          <strong>Observera:</strong>
        </P>
        <Gap size="xs" />
        <UnorderedList>
          <li>
            Om dina inkomster överskrider inkomstgränsen enligt
            familjestorleken, godkänn den högsta avgiften för småbarnspedagogik.
            I detta fall behöver du inte alls reda ut dina inkomster.
          </li>
          <li>
            Om din familj inkluderar en annan vuxen måste de också skicka in en
            inkomstutredning genom att personligen logga in på eVaka och fylla i
            detta formulär.
          </li>
        </UnorderedList>
        <P>
          Se nuvarande inkomstgränserna{' '}
          <a
            target="_blank"
            rel="noreferrer"
            href="https://static.espoo.fi/cdn/ff/uCz08Q1RDj-eVJJvhztJC6oTCmF_4OGQOtOiDUwT4II/1621487195/public/2021-05/Asiakastiedote%20varhaiskasvatusmaksuista%201.8.2021%20%283%29.pdf"
          >
            här
          </a>
          .
        </P>
        <P>* Uppgifter markerade med en asterisk är obligatoriska</P>
      </>
    ),
    confidential: (
      <P>
        <strong>Sekretessbelagt</strong>
        <br />
        (24.1 §, punkt 23 i offentlighetslagen)
      </P>
    ),
    addNew: 'Ny inkomstutredning',
    incomeInfo: 'Inkomstuppgifter',
    incomesRegisterConsent:
      'Jag samtycker till att uppgifterna som rör mina inkomster kontrolleras i inkomstregistret samt hos FPA vid behov',
    incomeType: {
      description: (
        <>
          Om du är företagare men har också andra inkomster, välj både{' '}
          <strong>Företagarens inkomstuppgifter</strong>, och{' '}
          <strong>
            Fastställande av klientavgiften enligt bruttoinkomster
          </strong>
          .
        </>
      ),
      startDate: 'Gäller från och med',
      endDate: 'Upphör att gälla',
      title: 'Grunder för klientavgiften',
      agreeToHighestFee:
        'Jag samtycker till den högsta avgiften för småbarnspedagogik',
      highestFeeInfo:
        'Jag samtycker till att betala den högsta avgiften för småbarnspedagogik som gäller till vidare enligt vid den aktuella tidpunkten gällande lagen om klientavgifter och stadsstyrelsens beslut, tills jag meddelar något annat eller tills mitt barns småbarnspedagogik upphör. (Inkomstuppgifterna behöver inte lämnas in)',
      grossIncome: 'Fastställande av avgiften enligt bruttoinkomster',
      entrepreneurIncome: 'Uppgifter om företagarens inkomster'
    },
    grossIncome: {
      title: 'Att fylla i uppgifterna om bruttoinkomster',
      description: (
        <>
          <P noMargin>
            Välj nedan om du vill skicka in dina inkomstuppgifter som bilaga,
            eller om myndigheten ska se dina uppgifter direkt i inkomstregistret
            samt hos FPA vid behov.
          </P>
          <P>
            Om du har börjat eller är på väg att börja ett nytt jobb, skicka
            alltid med ett bifogat anställningsavtal som visar din lön, eftersom
            informationen i Inkomstregistret uppdateras med en fördröjning.
          </P>
        </>
      ),
      incomeSource: 'Inlämning av inkomstuppgifterna',
      provideAttachments:
        'Jag lämnar in uppgifterna som bilaga, och mina uppgifter får kontrolleras hos FPA vid behov',
      estimate: 'Uppskattning av mina bruttoinkomster',
      estimatedMonthlyIncome:
        'Genomsnittliga inkomster inklusive semesterpenning, €/månad',
      otherIncome: 'Övriga inkomster',
      otherIncomeDescription:
        'Om du har några andra inkomster, ska du lämna in uppgifterna som bilaga. En lista över nödvändiga bilagor finns längst ner på blanketten under: Bilagor som rör inkomsterna och avgifterna för småbarnspedagogik.',
      choosePlaceholder: 'Välj',
      otherIncomeTypes: {
        PENSION: 'Pension',
        ADULT_EDUCATION_ALLOWANCE: 'Vuxenutbildningsstöd',
        SICKNESS_ALLOWANCE: 'Sjukdagpenning',
        PARENTAL_ALLOWANCE: 'Moderskaps- och föräldrapenning',
        HOME_CARE_ALLOWANCE: 'Stöd för hemvård av barn',
        FLEXIBLE_AND_PARTIAL_HOME_CARE_ALLOWANCE:
          'Flexibel eller partiell vårdpenning',
        ALIMONY: 'Underhållsbidrag eller -stöd',
        INTEREST_AND_INVESTMENT_INCOME: 'Ränte- och dividendinkomster',
        RENTAL_INCOME: 'Hyresinkomster',
        UNEMPLOYMENT_ALLOWANCE: 'Arbetslöshetsdagpenning',
        LABOUR_MARKET_SUBSIDY: 'Arbetsmarknadsstöd',
        ADJUSTED_DAILY_ALLOWANCE: 'Jämkad dagpenning',
        JOB_ALTERNATION_COMPENSATION: 'Alterneringsersättning',
        REWARD_OR_BONUS: 'Belöning eller bonus',
        RELATIVE_CARE_SUPPORT: 'Stöd för närståendevård',
        BASIC_INCOME: 'Basinkomst',
        FOREST_INCOME: 'Skogsinkomst',
        FAMILY_CARE_COMPENSATION: 'Arvoden för familjevård',
        REHABILITATION: 'Rehabiliteringsstöd eller rehabiliteringspenning',
        EDUCATION_ALLOWANCE: 'Utbildningsdagpenning',
        GRANT: 'Stipendium',
        APPRENTICESHIP_SALARY: 'Inkomster från läroavtalsutbildning',
        ACCIDENT_INSURANCE_COMPENSATION:
          'Ersättning från olycksfallsförsäkring',
        OTHER_INCOME: 'Övriga inkomster'
      },
      otherIncomeInfoLabel: 'Uppskattning av övriga inkomster',
      otherIncomeInfoDescription:
        'Skriv här uppskattningar av andra inkomster per månad, t.ex. "Hyresinkomster 150, vårdpenning 300"'
    },
    entrepreneurIncome: {
      title: 'Att fylla i företagarens inkomstuppgifter',
      description: (
        <>
          Med denna blankett kan du vid behov fylla i uppgifterna för flera
          företag genom att välja de punkter som gäller alla dina företag.
          Skicka mer detaljerade företagsspecifika uppgifter som bilaga.
          <br />
          En lista över obligatoriska bilagor finns längst ner på blanketten
          under ”Bilagor som rör inkomsterna och avgifterna för
          småbarnspedagogik”.
        </>
      ),
      fullTimeLabel: 'Är företagsverksamheten en huvudsyssla eller bisyssla?',
      fullTime: 'Huvudsyssla',
      partTime: 'Bisyssla',
      startOfEntrepreneurship: 'Entreprenörskap har börjat',
      spouseWorksInCompany: 'Arbetar din maka/make i företaget?',
      yes: 'Ja',
      no: 'Nej',
      startupGrantLabel: 'Har företaget fått startpeng?',
      startupGrant:
        'Mitt företag har fått startpeng. Jag skickar beslutet om startpeng som bilaga.',
      checkupLabel: 'Kontroll av uppgifter',
      checkupConsent:
        'Jag samtycker till att uppgifter som rör mina inkomster kontrolleras i inkomstregistret samt hos FPA vid behov.',
      companyInfo: 'Företagets uppgifter',
      companyForm: 'Företagets verksamhetsform',
      selfEmployed: 'Firmanamn',
      limitedCompany: 'Aktiebolag',
      partnership: 'Öppet bolag eller kommanditbolag',
      lightEntrepreneur: 'Lättföretagande',
      lightEntrepreneurInfo:
        'Betalningsverifikaten över löner och arbetsersättningar ska skickas som bilaga.',
      partnershipInfo:
        'Resultaträkningen och balansräkningen samt bokförarens utredning av lön och naturaförmåner ska skickas som bilaga.'
    },
    selfEmployed: {
      info: 'Om företagsverksamheten har varat över tre månader, ska företagets senaste resultat- och balansräkning eller skattebeslut skickas in.',
      attachments:
        'Jag bifogar företagets senaste resultat- och balansräkning eller skattebeslut.',
      estimatedIncome:
        'Jag fyller i en uppskattning av min genomsnittliga månadsinkomst.',
      estimatedMonthlyIncome: 'Genomsnittliga inkomster euro/månad',
      timeRange: 'Under perioden'
    },
    limitedCompany: {
      info: (
        <>
          <strong>
            Verifikaten över dividendinkomster ska skickas som bilaga.
          </strong>{' '}
          Välj ett lämpligt sätt att överföra övriga uppgifter nedan.
        </>
      ),
      incomesRegister:
        'Mina inkomster kan kontrolleras direkt hos FPA och i inkomstregistret.',
      attachments:
        'Jag bifogar verifikaten över mina inkomster och samtycker till att uppgifter som rör mina inkomster kontrolleras hos FPA.'
    },
    accounting: {
      title: 'Bokförarens kontaktuppgifter',
      description:
        'Bokförarens kontaktuppgifter krävs om du är verksam i ett aktiebolag, kommanditbolag eller öppet bolag.',
      accountant: 'Bokförare',
      accountantPlaceholder: 'Bokförarens namn / företagets namn',
      email: 'E-postadress',
      emailPlaceholder: 'E-post',
      address: 'Postadress',
      addressPlaceholder: 'Gatuadress, postnummer, postort',
      phone: 'Telefonnummer',
      phonePlaceholder: 'Telefonnummer'
    },
    moreInfo: {
      title: 'Övriga uppgifter som rör betalningen',
      studentLabel: 'Är du studerande?',
      student: 'Jag är studerande.',
      studentInfo:
        'Studerandena lämnar in ett studieintyg från läroanstalten eller beslut om arbetslöshetskassans studieförmån / sysselsättningsfondens utbildningsstöd.',
      deductions: 'Avdrag',
      alimony:
        'Jag betalar underhållsbidrag. Jag bifogar en kopia av betalningsverifikatet.',
      otherInfoLabel: 'Mer information om inkomstuppgifter'
    },
    attachments: {
      title: 'Bilagor som rör inkomsterna och avgifterna för småbarnspedagogik',
      description:
        'Här kan du elektroniskt skicka de begärda bilagor som rör dina inkomster eller avgifter för småbarnspedagogik, såsom lönekvittona eller FPA:s intyg över stöd för privat vård. Obs! Bilagor som rör inkomsterna behövs i regel inte, om er familj har samtyckt till den högsta avgiften.',
      required: {
        title: 'Obligatoriska bilagor'
      },
      attachmentNames: {
        PENSION: 'Beslut om pension',
        ADULT_EDUCATION_ALLOWANCE: 'Beslut om vuxenutbildningsstöd',
        SICKNESS_ALLOWANCE: 'Beslut om sjukdagpenning',
        PARENTAL_ALLOWANCE: 'Beslut om moderskaps- eller föräldrapenning',
        HOME_CARE_ALLOWANCE: 'Beslut om hemvårdsstöd',
        FLEXIBLE_AND_PARTIAL_HOME_CARE_ALLOWANCE: 'Beslut om vårdpenning',
        ALIMONY: 'Underhållsavtal eller beslut om underhållsstöd',
        UNEMPLOYMENT_ALLOWANCE: 'Beslut om arbetslöshetsdagpenning',
        LABOUR_MARKET_SUBSIDY: 'Beslut om arbetsmarknadsstöd',
        ADJUSTED_DAILY_ALLOWANCE: 'Beslut om dagpenning',
        JOB_ALTERNATION_COMPENSATION: 'Verifikat över alterneringsersättning',
        REWARD_OR_BONUS: 'Nytt löneintyg eller lönekvitto med bonus',
        RELATIVE_CARE_SUPPORT: 'Beslut om stöd för närståendevård',
        BASIC_INCOME: 'Beslut om basinkomst',
        FOREST_INCOME: 'Verifikat över skogsinkomst',
        FAMILY_CARE_COMPENSATION: 'Verifikat över arvoden för familjevård',
        REHABILITATION:
          'Beslut om rehabiliteringsstöd eller rehabiliteringspenning',
        EDUCATION_ALLOWANCE: 'Beslut om utbildningsdagpenning',
        GRANT: 'Verifikat över stipendium',
        APPRENTICESHIP_SALARY:
          'Verifikat över inkomster från läroavtalsutbildning',
        ACCIDENT_INSURANCE_COMPENSATION:
          'Verifikat över ersättning från olycksfallsförsäkring',
        OTHER_INCOME: 'Bilagor om övriga inkomster',
        ALIMONY_PAYOUT: 'Betalningsverifikat för underhållsbidrag',
        INTEREST_AND_INVESTMENT_INCOME:
          'Verifikat över ränte- och dividendinkomster',
        RENTAL_INCOME: 'Verifikat över hyresinkomster',
        PAYSLIP: 'Senaste lönekvitto',
        STARTUP_GRANT: 'Beslut om startpeng',
        ACCOUNTANT_REPORT: 'Bokförarens utredning av lön och naturaförmåner',
        ACCOUNTANT_REPORT_LLC:
          'Bokförarens utredning av naturaförmåner och dividender',
        PROFIT_AND_LOSS_STATEMENT: 'Resultaträkning och balansräkning',
        SALARY: 'Utbetalningsspecifikationer av lön och annan arbetsersättning',
        PROOF_OF_STUDIES:
          'Studieintyg eller beslut om arbetslöshetskassans studieförmån / sysselsättningsfondens utbildningsstöd'
      }
    },
    assure: 'Jag försäkrar att de uppgifter jag lämnat in är riktiga.',
    errors: {
      invalidForm:
        'Blanketten saknar vissa nödvändiga uppgifter eller uppgifterna är felaktiga. Vänligen kontrollera den information som du har fyllt i.',
      choose: 'Välj ett alternativ',
      chooseAtLeastOne: 'Välj minst ett alternativ',
      deleteFailed: 'Inkomstutredningen kunde inte raderas'
    },
    table: {
      title: 'Inkomstutredningar',
      incomeStatementForm: 'Blankett för inkomstutredning',
      startDate: 'Gäller från och med',
      endDate: 'Gäller till och med',
      handled: 'Handläggare',
      openIncomeStatement: 'Öppna blanketten',
      deleteConfirm: 'Vill du radera inkomstutredningen?',
      deleteDescription:
        'Är du säker på att du vill radera den inkomstutredning som du har lämnat in? All information på den blankett som ska raderas förloras.'
    },
    view: {
      title: 'Blankett för inkomstutredning',
      startDate: 'Gäller från och med',
      feeBasis: 'Grund för klientavgiften',

      grossTitle: 'Bruttoinkomster',
      incomeSource: 'Inlämning av uppgifter',
      incomesRegister:
        'Jag samtycker till att uppgifter som rör mina inkomster kontrolleras hos FPA samt i inkomstregistret.',
      attachmentsAndKela:
        'Jag lämnar in uppgifterna som bilaga, och mina uppgifter får kontrolleras hos FPA',
      grossEstimatedIncome: 'Uppskattning av bruttoinkomsterna',
      otherIncome: 'Övriga inkomster',
      otherIncomeInfo: 'Uppskattning av övriga inkomster',

      entrepreneurTitle: 'Uppgifter om företagarens inkomster',
      fullTimeLabel: 'Är företagsverksamheten en huvudsyssla eller bisyssla',
      fullTime: 'Huvudsyssla',
      partTime: 'Bisyssla',
      startOfEntrepreneurship: 'Entreprenörskap har börjat',
      spouseWorksInCompany: 'Arbetar din maka/make i företaget',
      startupGrant: 'Startpeng',
      checkupConsentLabel: 'Kontroll av uppgifter',
      checkupConsent:
        'Mina inkomster kan kontrolleras direkt hos FPA och i inkomstregistret.',
      companyInfoTitle: 'Företagets uppgifter',
      companyType: 'Verksamhetsform',
      selfEmployed: 'Firmanamn',
      selfEmployedAttachments:
        'Jag bifogar företagets senaste resultat- och balansräkning eller skattebeslut.',
      selfEmployedEstimation: 'Uppskattning av genomsnittlig månadsinkomst',
      limitedCompany: 'Aktiebolag',
      limitedCompanyIncomesRegister:
        'Mina inkomster kan kontrolleras direkt hos FPA och i inkomstregistret.',
      limitedCompanyAttachments:
        'Jag bifogar verifikaten över mina inkomster och samtycker till att uppgifter som rör mina inkomster kontrolleras hos FPA.',
      partnership: 'Öppet bolag eller kommanditbolag',
      lightEntrepreneur: 'Lättföretagande',
      attachments: 'Bilagor',

      estimatedMonthlyIncome: 'Genomsnittliga inkomster euro/månad',
      timeRange: 'Under perioden',

      accountantTitle: 'Uppgifter om bokföraren',
      accountant: 'Bokförare',
      email: 'E-postadress',
      phone: 'Telefonnummer',
      address: 'Postadress',

      otherInfoTitle: 'Övriga uppgifter som rör inkomsterna',
      student: 'Studerande',
      alimonyPayer: 'Betalar underhållsbidrag',
      otherInfo: 'Mer information om inkomstuppgifterna',

      citizenAttachments: {
        title:
          'Bilagor som rör inkomsterna och avgifterna för småbarnspedagogik',
        noAttachments: 'Inga bilagor'
      },

      employeeAttachments: {
        title: 'Lägg till bilagor',
        description:
          'Här kan du lägga till de bilagor som klienten har skickat in i pappersform till inkomstutredningen som lämnats in via eVaka.'
      },

      statementTypes: {
        HIGHEST_FEE: 'Samtycke till den högsta avgiftsklassen',
        INCOME: 'Inkomstuppgifter som vårdnadshavaren har skickat'
      }
    }
  },
  validationErrors: {
    required: 'Värde saknas',
    requiredSelection: 'Val saknas',
    format: 'Ange rätt format',
    ssn: 'Ogiltigt personbeteckning',
    phone: 'Ogiltigt telefonnummer',
    email: 'Ogiltig e-postadress',
    validDate: 'Ange i format dd.mm.åååå',
    dateTooEarly: 'Välj ett senare datum',
    dateTooLate: 'Välj ett tidigare datum',
    preferredStartDate: 'Ogiltigt datum',
    timeFormat: 'Kolla',
    timeRequired: 'Nödvändig',
    unitNotSelected: 'Välj minst en enhet',
    emailsDoNotMatch: 'E-postadresserna är olika'
  },
  login: {
    failedModal: {
      header: 'Inloggningen misslyckades',
      message:
        'Autentiseringen för tjänsten misslyckades eller avbröts. För att logga in gå tillbaka och försök på nytt.',
      returnMessage: 'Gå tillbaka till inloggningen'
    }
  },
  pedagogicalDocuments: {
    title: 'Tillväxt och inlärning',
    description:
      'Hit samlas dokument och bilder angående barnets/barnens tillväxt och inlärning.',
    table: {
      date: 'Datum',
      child: 'Barn',
      document: 'Dokument',
      description: 'Beskrivning'
    },
    toggleExpandText: 'Visa eller göm texten'
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
      PRESCHOOL_DAYCARE: 'Esiopetus ja liittyvä varhaiskasvatus',
      TEMPORARY_DAYCARE: 'Tilapäinen kokopäiväinen varhaiskasvatus',
      TEMPORARY_DAYCARE_PART_DAY: 'Tilapäinen osapäiväinen varhaiskasvatus',
      SCHOOL_SHIFT_CARE: 'Koululaisten vuorohoito'
    }
  },
  reloadNotification: {
    title: 'En ny version av eVaka är tillgänglig',
    buttonText: 'Ladda om sidan'
  },
  children: {
    // TODO copy
    title: 'Barn',
    pageDescription:
      'Tällä sivulla näet lastesi varhaiskasvatukseen tai esiopetukseen liittyvät yleiset tiedot.',
    noChildren: 'Inga barn',
    placementTermination: {
      title: 'Paikan irtisanominen',
      description:
        'Irtisanoessasi paikkaa huomaathan, että mahdollinen siirtohakemus poistuu viimeisen läsnäolopäivän jälkeen. Jos tarvitset lapsellesi myöhemmin paikan, sinun tulee hakea sitä uudella hakemuksella.',
      until: (date: string) => `voimassa ${date}`,
      choosePlacement: 'Valitse paikka, jonka haluat irtisanoa',
      terminatedPlacements: 'Olet irtisanonut paikan',
      lastDayInfo:
        'Viimeinen päivä, jolloin lapsesi tarvitsee paikkaa. Paikka irtisanotaan päättymään tähän päivään.',
      lastDayOfPresence: 'Viimeinen läsnäolopäivä',
      terminate: 'Irtisano paikka'
    }
  }
}

export default sv
