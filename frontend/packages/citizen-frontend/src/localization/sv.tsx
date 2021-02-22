// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { P } from '@evaka/lib-components/src/typography'
import React from 'react'
import { Translations } from '.'

const sv: Translations = {
  common: {
    title: 'Småbarnspedagogik',
    cancel: 'Gå tillbaka',
    return: 'Tillbaka',
    ok: 'Ok',
    unit: {
      providerTypes: {
        MUNICIPAL: 'Kommunal tjänst',
        PURCHASED: 'Köptjänst',
        PRIVATE: 'Privat tjänst',
        MUNICIPAL_SCHOOL: 'Kommunal tjänst',
        PRIVATE_SERVICE_VOUCHER: 'Privat tjänst (servicesedel)'
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
        fi: 'suomenkielinen',
        sv: 'ruotsinkielinen'
      },
      languagesShort: {
        fi: 'suomi',
        sv: 'ruotsi'
      }
    },
    errors: {
      genericGetError: 'Hämtar information misslyckades'
    }
  },
  header: {
    nav: {
      map: 'Karta',
      applications: 'Ansökningar',
      decisions: 'Beslut'
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
    espooLabel: '© Esbo stad',
    privacyPolicy: 'Dataskyddsbeskrivningar',
    privacyPolicyLink:
      'https://www.esbo.fi/sv-FI/Etjanster/Dataskydd/Dataskyddsbeskrivningar',
    sendFeedback: 'Ge feedback',
    sendFeedbackLink:
      'https://easiointi.espoo.fi/eFeedback/sv/Feedback/20-S%C3%A4hk%C3%B6iset%20asiointipalvelut'
  },
  map: {
    title: 'Enheter på kartan',
    mainInfo:
      'I den här vyn kan du söka på kartan för Espoos tidiga barndomsutbildning, förskola och klubbar.',
    searchLabel: 'Sök efter din adress eller enhetsnamn',
    searchPlaceholder: 'Till ex. Kilontie 3 eller Purola daghem',
    address: 'Adress',
    noResults: 'Inga sökresultat',
    distanceWalking: 'Avstånd från den valda adressen till fots',
    careType: 'Handlingssätt',
    careTypePlural: 'Åtgärdssätt',
    careTypes: {
      CLUB: 'Klubbverksamhet',
      DAYCARE: 'Småbarnspedagogik',
      PRESCHOOL: 'Förskola'
    },
    language: 'Enhetens språk',
    providerType: 'Tjänsteleverantör',
    providerTypes: {
      MUNICIPAL: 'kommunal tjänst',
      PURCHASED: 'köptjänst',
      PRIVATE: 'privat',
      PRIVATE_SERVICE_VOUCHER: 'servicesedel'
    },
    homepage: 'Hemsida',
    unitHomepage: 'Enhetens hemsida',
    route: 'Se rutten till enheten',
    routePlanner: 'Reittiopas',
    newTab: '(Öppnas i en ny flik)',
    shiftCareTitle: 'Kvälls- och skiftterapi',
    shiftCareLabel: 'Visa endast kvälls- och skiftenheter',
    shiftCareYes: 'Enheten erbjuder kvälls- och / eller skifttjänster',
    shiftCareNo: 'Enheten tillhandahåller inte kvälls- och / eller skiftvård',
    showMoreFilters: 'Visa fler filter',
    showLessFilters: 'Visa färre filter',
    nearestUnits: 'Närmaste enheter',
    moreUnits: 'Fler enheter',
    showMore: 'Visa fler sökresultat',
    mobileTabs: {
      map: 'Karta',
      list: 'Lista över enheter'
    },
    serviceVoucherLink:
      'https://www.esbo.fi/sv-FI/Utbildning_och_fostran/Smabarnspedagogik/Privat_smabarnspedagogik'
  },
  applications: {
    title: 'Ansökningar',
    deleteDraftTitle: 'Vill du ta bort din ansökan?',
    deleteDraftText:
      'Alla uppgifter på din ansökan raderas, också ansökan som du redan skickat raderas.',
    deleteDraftOk: 'Ta bort ansökan',
    deleteDraftCancel: 'Tillbaka',
    deleteSentTitle: 'Vill du ta bort din ansökan?',
    deleteSentText:
      'Alla uppgifter på din ansökan raderas, också ansökan som du redan skickat raderas.',
    deleteSentOk: 'Ta bort ansökan',
    deleteSentCancel: 'Tillbaka',
    deleteUnprocessedApplicationError: 'Radera ansökan misslyckades',
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
      transferApplicationInfo: {
        DAYCARE:
          'Barnet har redan en plats i Esbo stads småbarnspedagogik. Med denna ansökan kan du ansöka om flyttning till en annan enhet som erbjuder småbarnspedagogik.',
        PRESCHOOL:
          'Barnet har redan en förskoleplats. Med denna ansökan kan du ansöka om <strong>småbarnspedagogik i anslutning till förskoleundervisningen</strong> eller om flyttning till en annan enhet som erbjuder förskoleundervisning.'
      },
      create: 'Ny ansökan',
      daycare4monthWarning: 'Behandlingstiden för ansökningen är 4 månader.',
      applicationInfo: function ApplicationInfoText() {
        return (
          <P>
            Du kan ändra i ansökan så länge den inte har tagits till behandling.
            Därefter kan du göra ändringar i ansökan genom att kontakta
            småbarnspedagogikens servicehänvisning (tfn 09 8163 27600). Du kan
            återta en ansökan som du redan lämnat in genom att meddela detta per
            e-post till småbarnspedagogikens servicehänvisning{' '}
            <a href="mailto:dagis@esbo.fi">dagis@esbo.fi</a>.
          </P>
        )
      }
    },
    editor: {
      heading: {
        title: {
          DAYCARE: 'Ansökan till småbarnspedagogik',
          PRESCHOOL: 'Anmälan till förskolan',
          CLUB: 'Ansökan till klubbverksamhet'
        },
        info: {
          DAYCARE: function EditorHeadingInfoDaycareText() {
            return (
              <>
                <P>
                  Du kan ansöka om plats i småbarnspedagogisk verksamhet året
                  om. Ansökningen bör lämnas in senast fyra månader före behovet
                  av verksamheten börjar. Om behovet börjar med kortare varsel
                  bör du ansöka om plats senast två veckor före.
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
                <P fitted={true}>
                  * Informationen markerad med en stjärna krävs
                </P>
              </>
            )
          },
          PRESCHOOL: function EditorHeadingInfoPreschoolText() {
            return (
              <>
                <P>
                  Barn deltar i förskoleundervisning året innan läroplikten
                  börjar. Förskoleundervisningen är avgiftsfri. Anmälningstiden
                  till förskoleundervisningen 2021–2022 är 8.–20.1.2021.
                  Förskolan börjar i <strong>11.8.2021</strong>. Beslutet delges
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
                <P fitted={true}>
                  * Informationen markerad med en stjärna krävs
                </P>
              </>
            )
          },
          CLUB: function EditorHeadingInfoClubText() {
            return (
              <>
                <P>
                  Ansökningsperioden för klubbar som börjar på hösten är i mars.
                  Om ditt barn får en klubbplats, du får beslutet om det under
                  april-maj. Beslutet fattas för en verksamhetsperiod för
                  perioden (augusti till slutet av maj). Beslut om klubbplats
                  kommer till Suomi.fi-tjänsten eller per post om du inte har
                  tagit det service.
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
                <P fitted={true}>* Tähdellä merkityt tiedot ovat pakollisia</P>
              </>
            )
          }
        },
        errors: (count: number) => (count === 1 ? '1 fel' : `${count} fel`),
        hasErrors:
          'Var så god och kontrollera följande information för din ansökan'
      },
      actions: {
        verify: 'Granska',
        hasVerified: 'Jag har granskat att uppgifterna är rätt',
        returnToEdit: 'Gå tillbaka',
        returnToEditBtn: 'Gå tillbaka',
        cancel: 'Tillbaka',
        send: 'Skicka ansökan',
        update: 'Spara ändringarna',
        sendError: 'Spara lyckades inte',
        saveDraft: 'Spara som oavslutad',
        updateError: 'Spara lyckades inte'
      },
      verification: {
        title: {
          DAYCARE:
            'Var god och granska följande obligatoriska fält i blanketten',
          PRESCHOOL:
            'Var god och granska följande obligatoriska fält i blanketten',
          CLUB: 'Var god och granska följande obligatoriska fält i blanketten'
        },
        notYetSent: function NotYetSentText() {
          return (
            <P>
              <strong>Du har inte ännu skickat ansökan.</strong> Granska
              informationen du gett, och skicka ansökan med Skicka
              ansökan-knappen när du är färdig
            </P>
          )
        },
        notYetSaved: function NotYetSavedText() {
          return (
            <P>
              <strong>Ändringarna har inte sparats än.</strong> Granska
              informationen du gett, och skicka ansökan med {'"'}Spara
              ändringarna{'"'} -knappen i slutet av blankett.
            </P>
          )
        },
        no: 'Nej',
        basics: {
          created: 'Skapad',
          modified: 'Uppdaterad'
        },
        attachmentBox: {
          nb: 'Obs!',
          headline:
            'Om du lägger till bilagor elektroniskt i följande punkter, behandlas din ansökan snabbare. Behandlingstiden börjar när bilagorna kommer in.',
          urgency: 'Ansökningen är brådskande',
          shiftCare: 'Kvälls- och skiftesvård',
          goBackLinkText: 'Återvänd tillbaka till ansökan',
          goBackRestText: 'för att lägga till bilagor.'
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
            title: 'Småbarnspedagogik i samband med förskoleundervisning.',
            label: 'Småbarnspedagogik',
            withConnectedDaycare:
              'Jag ansöker också om småbarnspedagogik i samband med förskoleundervisning.',
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
            title: 'Tiden för småbarnspedagogik per dag',
            partTime: 'Hel- eller deldag',
            withPartTime: 'Deldag',
            withoutPartTime: 'Heldag',
            dailyTime: 'Tiden för småbarnspedagogik per dag',
            shiftCare: 'Kvälls- och skiftesvård',
            withShiftCare: 'Ja',
            withoutShiftCare: 'Nej'
          },
          assistanceNeed: {
            title: 'Behov av stöd för utveckling och lärande',
            assistanceNeed:
              'Barnet har behov av stöd för utveckling och lärande',
            withAssistanceNeed:
              'Barnet har behov av stöd för utveckling och lärande',
            withoutAssistanceNeed:
              'Barnet har inte behov av stöd för utveckling och lärande',
            description: 'Beskrivning'
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
              'Jag ansöker i första hand om plats i den enheten där barnets syskon redan har en plats',
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
            title: 'Barnets information',
            name: 'Namn',
            ssn: 'Personbeteckning',
            streetAddress: 'Hemadress',
            isAddressChanging:
              'Adressen som finns i befolkningsdatabasen har ändrats/ska ändras',
            hasFutureAddress:
              'Väestörekisterissä oleva osoite on muuttunut/muuttumassa ',
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
            isAddressChanging:
              'Adressen som finns i befolkningsdatabasen har ändrats/ska ändras',
            hasFutureAddress:
              'Adressen som finns i befolkningsdatabasen har ändrats/ska ändras',
            addressChangesAt: 'Flyttdatum',
            newAddress: 'Ny adress'
          },
          secondGuardian: {
            title: 'Uppgifter om den andra vårdnadshavaren',
            email: 'E-postadress',
            tel: 'Telefonnummer',
            info:
              'Den andra vårdnadshavarens information hämtas automatiskt från befolkningsinformationssystemet.',
            agreed: 'Vi har tillsammans kommit överens att fylla i ansökan.',
            notAgreed: 'Vi har inte kunnat komma överens om ansökan.',
            rightToGetNotified:
              'Den andra vårdnadshavaren har endast rätt att få uppgifter om barnet.',
            noAgreementStatus: '-'
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
            name: 'Namn',
            ssn: 'Personbeteckning',
            noOtherChildren: 'Ingen'
          }
        },
        additionalDetails: {
          title: 'Övriga tilläggsuppgifter',
          otherInfoLabel: 'Övriga tilläggsuppgifter',
          dietLabel: 'Specialdiet',
          allergiesLabel: 'Allergier'
        }
      },
      serviceNeed: {
        title: 'Behov av småbarnspedagogisk verksamhet',
        startDate: {
          header: {
            DAYCARE: 'Inledningsdatum',
            PRESCHOOL: 'Inledningsdatum',
            CLUB: 'Inledningsdatum'
          },
          info: {
            DAYCARE: [],
            PRESCHOOL: [
              'Förskolan börjar i 11.8.2021. Om ditt barn behöver småbarnspedagogisk verksamhet fr.o.m. 1.8.2021 före förskolan börjar kan du ansöka om sådan med denna blankett. Välj ”Jag ansöker om småbarnspedagogik i samband med förskoleundervisning”.'
            ],
            CLUB: [
              'Kerhot noudattavat esiopetuksen työ- ja loma-aikoja. Kerhon toimintakausi on elokuusta toukokuun loppuun, ja kullekin toimintakaudelle haetaan erikseen. Eri kerhot kokoontuvat eri viikonpäivinä.'
            ]
          },
          clubTerm: 'Kerhon toimintakausi',
          label: {
            DAYCARE: 'Önskat inledningsdatum',
            PRESCHOOL: 'Inledningsdatum i augusti',
            CLUB: 'Önskat inledningsdatum'
          },
          noteOnDelay: 'Behandlingstiden för ansökningen är 4 månader.',
          instructions:
            'Det är möjligt att senarelägga det önskade startdatumet så länge ansökan inte har tagits upp till behandling. Därefter kan du ändra det önskade startdatumet genom att kontakta småbarnspedagogikens servicehänvisning (tfn 09 816 27600).',
          placeholder: 'Välj inledningsdatum',
          validationText: 'Önskat inledningsdatum: '
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
          label: 'Ansökningen är brådskande',
          attachmentsMessage: {
            text: function UrgentApplicatiionAttachmentMessageText() {
              return (
                <P fitted={true}>
                  Om behovet av en plats inom småbarnspedagogiken beror på att
                  du plötsligt fått sysselsättning eller börjat studera, ska
                  platsen sökas <strong>senast två veckor innan</strong> behovet
                  börjar.{' '}
                  <strong>
                    Bifoga till ansökan ett arbets- eller studieintyg
                  </strong>{' '}
                  av båda vårdnadshavarna som bor i samma hushåll. Om du inte
                  kan lägga till bilagor till ansökan elektroniskt, skicka dem
                  per post till adressen Småbarnspedagogikens servicehänvisning,
                  PB 3125, 02070 Esbo stad. Behandlingstiden på två veckor
                  börjar när vi har tagit emot ansökan och bilagorna som behövs.
                </P>
              )
            },
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
            PRESCHOOL: 'Tiden för småbarnspedagogik per dag'
          },
          connectedDaycareInfo: function ConnectedDaycareInfoText() {
            return (
              <>
                <P>
                  Du kan vid behov ansöka om avgiftsbelagd småbarnspedagogik i
                  samband med förskoleundervisningen. Småbarnspedagogik erbjuds
                  utöver förskoleundervisningen (fyra timmar per dag) på morgnar
                  och eftermiddagar på samma ställe som förskoleundervisningen.
                  Om barnet börjar senare, meddela detta under
                  ”Tilläggsuppgifter”.
                </P>
                <P>
                  Privata förskolor och några av inköpsserviceförskolorna måste
                  göra en separat ansökan om relaterad förskoleundervisning.
                  Espoos förskoleundervisning är i kontakt med de sökande som
                  berörs.
                </P>
                <P>
                  Du får ett separat skriftligt beslut om platsen inom
                  småbarnspedagogik. Beslutet delges i tjänsten
                  Suomi.fi-meddelanden eller per post, om du inte har tagit
                  tjänsten i bruk.
                </P>
              </>
            )
          },
          connectedDaycare:
            'Jag ansöker också om småbarnspedagogik i samband med förskoleundervisning.',
          instructions: {
            DAYCARE:
              'Meddela tiden då ditt barn behöver småbarnspedagogisk verksamhet. Du kan meddela den mera exakta tiden när verksamheten börjar. Om ditt behov varierar dagligen eller per vecka (t.ex i skiftesvård) kan du meddela behovet mer exakt i tilläggsuppgifterna.',
            PRESCHOOL:
              'Förskoleundervisning ordnas fyra timmar per dag, huvudsakligen kl. 9.00-13.00, men tiden kan variera per enhet. Meddela tiden för småbarnspedagogik så att den innefattar tiden för förskoleundervisning som är fyra timmar (t.ex. kl. 7.00–17.00). Vårdnadshavare meddelar de mera exakta tiderna när småbarnspedagogiken börjar.  Om behovet av småbarnspedagogik varierar dagligen eller per vecka (t.ex. i skiftvård), meddela behovet mer exakt i tilläggsuppgifterna.'
          },
          usualArrivalAndDeparture: {
            DAYCARE: 'Den dagliga sluttiden för småbarnspedagogisk verksamhet',
            PRESCHOOL:
              'Tiden för småbarnspedagogik per dag (inkluderar förskoleundervisning)'
          },
          starts: 'Starttiden',
          ends: 'Sluttiden'
        },
        shiftCare: {
          label: 'Kvälls- och skiftesvård',
          instructions:
            'Med skiftesvård avses verksamhet under veckosluten eller dygnet runt. Kvälls- och skiftesvård är vård som huvudsakligen sker under annan tid än vardagar klockan 6.30-18.00. Om ditt barn behöver skiftesvård, beskriv behovet i fältet för tilläggsuppgifter.',
          message: {
            title: 'Kvälls- och skiftesvård',
            text:
              'Kvälls- och skiftesvård är till för barn vars båda föräldrar jobbar i skiften eller studerar huvudsakligen kvällstid och under veckoslut. Som bilaga till ansökan ska ett intyg om skiftesarbete eller studier lämnas in av båda vårdnadshavarna.'
          },
          attachmentsMessage: {
            text:
              'Kvälls- och skiftomsorg är avsedd för barn vars båda föräldrar har skiftarbete eller studerar huvudsakligen på kvällar och/eller veckoslut. Som bilaga till ansökan ska av båda föräldrarna lämnas ett intyg av arbetsgivaren över skiftarbete eller studier som orsakar behovet av kvälls- eller skiftomsorg. Vi rekommenderar att bilagan skickas elektroniskt här. Om du inte kan lägga till bilagor till ansökan elektroniskt, skicka dem per post till adressen Småbarnspedagogikens servicehänvisning, PB 3125, 02070 Esbo stad.',
            subtitle:
              'Lägg här till för båda föräldrarna antingen arbetsgivarens intyg över skiftarbete eller ett intyg över studier på kvällar/veckoslut.'
          }
        },
        assistanceNeed: 'Behov av stöd för utveckling och lärande',
        assistanceNeeded: 'Barnet har behov av stöd för utveckling och lärande',
        assistanceNeedPlaceholder:
          'Berätta om barnets behov av stöd för utveckling och lärande',
        assistanceNeedInstructions:
          'Med behov av stöd för utveckling och lärande avses behov av sådana stödåtgärder som har konstaterats i ett sakkunnigutlåtande. Om ditt barn inte tidigare har deltagit i småbarnspedagogisk verksamhet i Esbo och hen har behov av stöd, kontaktar en konsultativ speciallärare inom småbarnspedagogik dig vid behov då du har meddelat om behovet i ansökan.',
        preparatory:
          'Barnet behöver stöd för att lära sig finska. Barnet söker också till undervisning som förbereder för den grundläggande utbildningen.',
        preparatoryInfo:
          'Den förberedande undervisningen för förskoleelever är avsedd för barn med invandrarbakgrund, återinvandrare, barn i tvåspråkiga familjer (med undantag av finska-svenska) och adoptivbarn som behöver stöd i det finska språket innan de inleder den grundläggande utbildningen. Barnet har rätt att delta i undervisning som förbereder för den grundläggande utbildningen under ett läsårs tid.  Förberedande undervisning ordnas utöver förskoleundervisning i genomsnitt ½–1 timme per dag. Undervisningen är avgiftsfri.'
      },
      unitPreference: {
        title: 'Ansökningsönskemål',
        siblingBasis: {
          title: 'Ansökan på basis av syskonrelationer',
          info: {
            DAYCARE: function SiblingBasisSummaryTextDaycare() {
              return (
                <>
                  <P>
                    Målet är att placera syskon i samma enhet om inte familjen
                    önskar annat. Som syskon betraktas barn som är folkbokförda
                    på samma adress. Om du ansöker om en plats för syskon, som
                    inte ännu har plats inom småbarnspedagogik, skriv uppgiften
                    i tilläggsuppgifter.
                  </P>
                  <P>
                    Fyll i dessa uppgifter endast om du vill hänvisa till
                    barnets syskonrelationer.
                  </P>
                </>
              )
            },
            PRESCHOOL: function SiblingBasisSummaryTextPreschool() {
              return (
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
                      förskolan tillämpas principen om eleven har syskon i
                      årskurs 1–6 i skolan som finns i samma
                      elevupptagningsområde som förskolan. Som syskon betraktas
                      barn som är folkbokförda på samma adress.
                    </li>
                  </ol>
                  <P>
                    Fyll i dessa uppgifter endast om du vill hänvisa till
                    barnets syskonrelationer.
                  </P>
                </>
              )
            },
            CLUB: function SiblingBasisSummaryTextClub() {
              return (
                <>
                  <P>
                    Barn som bor på samma adress betraktas som syskon. Ett
                    försök görs för att placera syskonen i samma klubbgrupp när
                    familjen önskar det.
                  </P>
                  <P>
                    Fyll i dessa uppgifter endast om du vill hänvisa till
                    barnets syskonrelationer och välj samma klubb som syskonet
                    deltar i nedan.
                  </P>
                </>
              )
            }
          },
          checkbox: {
            DAYCARE:
              'Jag ansöker i första hand om plats i den enheten där barnets syskon redan har en plats.',
            PRESCHOOL:
              'Jag ansöker om plats i en annan förskola än närförskolan med syskonprincipen',
            CLUB:
              'Jag ansöker främst om en plats i samma klubb där barnets syskon deltar.'
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
            'För att välja önskade enheter, välj det första önskade startdatumet i avsnittet om "Servicebehov"',
          info: {
            DAYCARE: function UnitPreferenceInfoTextDaycare() {
              return (
                <>
                  <P>
                    Du kan ange 1-3 platser i önskad ordning. Önskemålen
                    garanterar inte en plats i den önskade enheten, men
                    möjligheterna att få en önskad plats ökar om du anger flera
                    alternativ.
                  </P>
                  <P>
                    Du kan visa enhetsplatserna genom att välja {"'"}Enheter på
                    kartan{"'"}.
                  </P>
                </>
              )
            },
            PRESCHOOL: function UnitPreferenceInfoTextPreschool() {
              return (
                <>
                  <P>
                    Du kan ange 1-3 platser i önskad ordning. Önskemålen
                    garanterar inte en plats i den önskade enheten, men
                    möjligheterna att få en önskad plats ökar om du anger flera
                    alternativ.
                  </P>
                  <P>
                    Du kan visa enhetsplatserna genom att välja {"'"}Enheter på
                    kartan{"'"}.
                  </P>
                </>
              )
            },
            CLUB: function UnitPreferenceInfoTextClub() {
              return (
                <>
                  <P>
                    Du kan ange 1-3 platser i önskad ordning. Önskemålen
                    garanterar inte en plats i den önskade enheten, men
                    möjligheterna att få en önskad plats ökar om du anger flera
                    alternativ.
                  </P>
                  <P>
                    Du kan visa enhetsplatserna genom att välja {"'"}Enheter på
                    kartan{"'"}.
                  </P>
                </>
              )
            }
          },
          mapLink: 'Enheter på kartan',
          languageFilter: {
            label: 'Enhetens språk:',
            fi: 'finska',
            sv: 'svenska'
          },
          select: {
            label: 'Välj önskade enheter',
            placeholder: 'Välj enhet',
            maxSelected:
              'Max antal valda verksamhetsenheter. Ta bort en så att du kan lägga till en ny',
            noOptions: 'Inga hittade enheter'
          },
          preferences: {
            label: 'Enheter som du har valt',
            noSelections: 'Inga val',
            info:
              'Välj minst 1 och högst 3 enheter och ange dem i önskad ordning. Du kan ändra på ordningsföljden genom att dra alternativen till rätt plats.',
            fi: 'På finska',
            sv: 'På svenska',
            moveUp: 'Flytta upp',
            moveDown: 'Flytta ner',
            remove: 'Ta bort'
          }
        }
      },
      fee: {
        title: 'Avgiften för småbarnspedagogik',
        info: {
          DAYCARE: function FeeInfoTextDaycare() {
            return (
              <P>
                Klientavgiften inom den kommunala småbarnspedagogiken är en
                procentandel av familjens bruttoinkomster. Familjen ska lämna in
                en utredning över sina bruttoinkomster på en särskild blankett,
                senast inom två veckor från det att barnet har inlett
                småbarnspedagogiken.
              </P>
            )
          },
          PRESCHOOL: function FeeInfoTextPreschool() {
            return (
              <P>
                Klientavgiften inom den kommunala småbarnspedagogiken är en
                procentandel av familjens bruttoinkomster. Familjen ska lämna in
                en utredning över sina bruttoinkomster på en särskild blankett,
                senast inom två veckor från det att barnet har inlett
                småbarnspedagogiken.
              </P>
            )
          },
          CLUB: function FeeInfoTextClub() {
            return <P></P>
          }
        },
        emphasis: function FeeEmphasisText() {
          return (
            <strong>
              Om familjen samtycker till den högsta avgiften behövs ingen
              inkomstutredning.
            </strong>
          )
        },
        checkbox:
          'Jag ger mitt samtycke till att betala den högsta avgiften. Samtycket gäller tills vidare, tills jag meddelar något annat.',
        links: function FeeLinksText() {
          return (
            <P>
              Mer information om småbarnspedagogikens avgifter och blanketten
              för inkomstutredning finns här:
              <br />
              <a
                href="https://www.esbo.fi/sv-FI/Utbildning_och_fostran/Smabarnspedagogik/Avgifter_for_smabarnspedagogik"
                target="_blank"
                rel="noopener noreferrer"
              >
                Avgifter för småbarnspedagogik
              </a>
            </P>
          )
        }
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
        title: 'Personuppgifter',
        info: function ContactInfoInfoText() {
          return (
            <P>
              Personuppgifterna hämtas från befolkningsdatabasen och du kan inte
              ändra dem med den här ansökan. Om det finns fel i
              personuppgifterna, vänligen uppdatera uppgifterna på webbplatsen{' '}
              <a
                href="https://dvv.fi/sv/kontroll-av-egna-uppgifter-service"
                target="_blank"
                rel="noopener noreferrer"
              >
                dvv.fi
              </a>{' '}
              (Myndigheten för digitalisering och befolkningsdata). Ifall
              adressen kommer att ändras, kan du lägga till den nya adressen på
              ett separat ställe i ansökan. Fyll i den nya adressen både för
              vårdnadshavare och barnet. Adressuppgifterna är officiella först
              när de har uppdaterats av myndigheten för digitalisering och
              befolkningsdata. Beslutet om barnets plats inom
              småbarnspedagogiken eller förskoleundervisningen skickas
              automatiskt också till en vårdnadshavare som bor på en annan
              adress enligt befolkningsregistret.
            </P>
          )
        },
        childInfoTitle: 'Barnets information',
        childFirstName: 'Alla förnamn',
        childLastName: 'Efternamn',
        childSSN: 'Personbeteckning',
        homeAddress: 'Hemadress',
        moveDate: 'Flyttdatum',
        street: 'Gatuadress',
        postalCode: 'Postnummer',
        postOffice: 'Postanstalt',
        guardianInfoTitle: 'Vårdnadshavarens information',
        guardianFirstName: 'Alla förnamn',
        guardianLastName: 'Efternamn',
        guardianSSN: 'Personbeteckning',
        phone: 'Telefonnummer',
        emailAddress: 'E-postadress',
        email: 'E-postadress',
        secondGuardianInfoTitle: 'Uppgifter om den andra vårdnadshavaren',
        secondGuardianInfo:
          'Den andra vårdnadshavarens information hämtas automatiskt från befolkningsinformationssystemet.',
        secondGuardianNotFound:
          'Baserat på information från VTJ har barnet ingen andra vårdnadshavare',
        secondGuardianInfoPreschoolSeparated:
          'Enligt våra uppgifter bor barnets andra vårdnadshavare på en annan adress. Du ska avtala om ansökan om småbarnspedagogik med en annan vårdnadshavare.',
        secondGuardianAgreementStatus: {
          label:
            'Har du kommit överens om ansökan med den andra vårdnadshavaren?',
          AGREED: 'Vi har tillsammans kommit överens att fylla i ansökan.',
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
        otherChildrenTitle: 'Familjens övriga barn',
        otherChildrenInfo:
          'Barn som bor i samma hushåll påverkar avgifterna för småbarnspedagogik.',
        otherChildrenChoiceInfo: 'Lägg till et barn',
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
          'Andra barn under 18 år bor i samma hushåll (t.ex. sambo barn)',
        choosePlaceholder: 'Välja'
      },
      draftPolicyInfo: {
        title: 'Utkastet till ansökan har sparats',
        text:
          'Ansökan har sparats som halvfärdig. Obs! En halvfärdig ansökan förvaras i tjänsten i en månad efter att den senast sparats',
        ok: 'Klart'
      },
      sentInfo: {
        title: 'Ansökan har lämnats in',
        text:
          'Om du vill kan du göra ändringar i ansökan så länge ansökan inte har behandlats.',
        ok: 'Klart!'
      },
      updateInfo: {
        title: 'Ändringar i applikationen har sparats',
        text:
          'Om du vill kan du göra ändringar i ansökan så länge ansökan inte har behandlats.',
        ok: 'Klart!'
      }
    }
  },
  decisions: {
    title: 'Beslut',
    summary: function DecisionsSummaryText() {
      return (
        <P width="800px">
          Denna sida visar de beslutar om barns ansökan till småbarnspedagogik,
          förskola och klubbverksamhet. Du ska omedelbart eller senast två
          veckor från mottagandet av ett beslut ta emot eller annullera platsen
          / platserna.
        </P>
      )
    },
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
      openPdf: 'Visa beslut',
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
    summary: function ApplicationListSummaryText() {
      return (
        <P width="800px">
          Barnets vårdnadshavare kan anmäla barnet till förskolan eller ansöka
          om plats i småbarnspedagogisk verksamhet. Uppgifter om
          vårdnadshavarens barn kommer automatiskt från befolkningsdatabasen
          till denna sida.
        </P>
      )
    },
    pageLoadError: 'Tietojen hakeminen ei onnistunut',
    noApplications: 'Inga ansökningar',
    type: {
      DAYCARE: 'Ansökan till småbarnspedagogik',
      PRESCHOOL: 'Anmälan till förskolan',
      CLUB: 'Ansökan till klubbverksamhet'
    },
    transferApplication: 'Ansökan om överföring',
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
    editApplicationLink: 'Uppdatera',
    removeApplicationBtn: 'Ta bort ansökan',
    cancelApplicationBtn: 'Ta bort ansökan',
    confirmationLinkInstructions:
      'Under Beslut-fliken kan du läsa besluten till dina ansökningar och ta emot/annullera platsen',
    confirmationLink: 'Granska och bekräfta beslutet',
    newApplicationLink: 'Ny ansökan'
  },
  fileUpload: {
    loading: 'Uppladdas...',
    loaded: 'Uppladdad',
    error: {
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
    modalHeader: 'Behandling av filen pågår',
    modalMessage: 'Filen kan inte öppnas just nu. Försök igen om en stund.',
    modalConfirm: 'OK',
    deleteFile: 'Radera fil'
  },
  validationErrors: {
    required: 'Värde saknas',
    requiredSelection: 'Ingenting är valt',
    format: 'Ange rätt format',
    ssn: 'Ogiltigt personbeteckning',
    phone: 'Ogiltigt telefonnummer',
    email: 'Ogiltig e-postadress',
    validDate: 'Ange formuläret pp.kk.vvvv',
    preferredStartDate: 'Ogiltigt datum',
    timeFormat: 'Ange formuläret hh:mm',
    unitNotSelected: 'Välj minst en sökbegäran'
  }
}

export default sv
