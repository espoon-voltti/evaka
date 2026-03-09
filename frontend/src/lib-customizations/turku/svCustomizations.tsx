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
      till småbarnspedagogikens servicehandledning tfn{' '}
      <a href="tel:+35822625610">02 2625610</a>.
    </>
  )
}

const sv: DeepPartial<Translations> = {
  calendar: {
    absenceModal: {
      absenceTypes: {
        PLANNED_ABSENCE: 'Planerad frånvaro'
      },
      selectChildrenInfo:
        'Anmäl frånvaron här bara om barnet är frånvarande hela dagen.'
    }
  },
  children: {
    serviceApplication: {
      startDateInfo:
        'Välj det datum då du vill att det nya servicebehovet ska börja. Servicebehovet kan endast ändras från början av månaden och måste vara i kraft i minst fyra (4) månader.'
    }
  },
  applications: {
    creation: {
      daycareInfo:
        'Med en ansökan till småbarnspedagogik ansöker man om en plats i den kommunala dagvården eller familjedagvården. Du kan också ansöka om en servicesedel för privat småbarnspedagogik med samma ansökan genom att välja den önskade servicesedelenheten i punkten Önskad enhet.',
      preschoolLabel: 'Anmälan till förskoleundervisning ',
      preschoolInfo:
        'Avgiftsfri förskoleundervisning ordnas fyra (4) timmar om dagen. Verksamhetsåret följer i huvudsak skolornas skol- och lovtider. Därtill kan ni ansöka om kompletterande småbarnspedagogik för barn i förskoleåldern som tillhandahålls på förskoleenheter på morgonen innan förskoleundervisningen börjar och på eftermiddagen efter att förskoleundervisningen slutar.',
      preschoolDaycareInfo: '',
      clubLabel: 'Ansökan till öppen småbarnspedagogik',
      clubInfo:
        'Med en ansökan till öppen småbarnspedagogik ansöker man till klubbar och lekplatsverksamhet.',
      applicationInfo: (
        <P>
          Vårdnadshavaren kan göra ändringar i ansökan via e-tjänsten tills
          kundtjänsten börjar behandla ansökan. Efter detta, om du vill göra
          ändringar i ansökan eller återkalla ansökan, ska du kontakta
          {customerContactText()}
        </P>
      ),
      duplicateWarning:
        'Barnet har redan en liknande, halvfärdig ansökan. Gå tillbaka till vyn Ansökningar och redigera den befintliga ansökan eller kontakta småbarnspedagogikens kundtjänst.',
      transferApplicationInfo: {
        DAYCARE:
          'Barnet har redan fått en plats inom småbarnspedagogiken i Åbo. Med denna blankett kan du ansöka om överföring till en annan enhet i Åbo.'
      }
    },
    editor: {
      heading: {
        title: {
          DAYCARE: 'Ansökan om småbarnspedagogik och servicesedel',
          PRESCHOOL: 'Anmälan till förskoleundervisning',
          CLUB: 'Ansökan till öppen småbarnspedagogik'
        },
        info: {
          DAYCARE: (
            <>
              <P>
                Du kan lämna in en ansökan till småbarnspedagogiken året runt.
                Ansökan måste skickas in fyra månader innan barnet behöver en
                plats. Om du ansöker om en dagvårdsplats på grund av arbete
                eller studier, ska du skicka in ansökan minst två veckor innan.
              </P>
              <p>
                Du får ett skriftligt beslut om platsen{' '}
                <ExternalLink
                  text="till inkorgen i Suomi.fi-tjänsten"
                  href="https://www.suomi.fi/viestit"
                  newTab
                />{' '}
                eller per brevpost, om du inte har aktiverat Suomi.fi-tjänsten.
                Beslutet finns också i eVaka-tjänsten under Ansökning - Beslut.
              </p>
              <P $fitted={true}>
                * Obligatoriska uppgifter är markerade med en stjärna
              </P>
            </>
          ),
          PRESCHOOL: (
            <>
              <P>
                Förskoleundervisningen börjar året innan läroplikten börjar.
                Förskoleundervisningen är avgiftsfri. Anmälningstiden till
                förskoleundervisningen för läsåret 2026–2027 är 1.1–15.1.2026.
                Förskoleundervisningen inleds 11.8.2026. Vid registreringen,
                vänligen förklara dina ansökningsalternativ i avsnittet för
                övriga tillägsuppgifter.
              </P>
              <P>
                Beslutet skickas till{' '}
                <a
                  href="https://www.suomi.fi/viestit"
                  target="_blank"
                  rel="noreferrer"
                >
                  Suomi.fi-tjänsten
                </a>{' '}
                eller, om du inte har tagit Suomi.fi-tjänsten i bruk, per
                brevpost
              </P>
              <P $fitted={true}>
                * Obligatoriska uppgifter är markerade med en stjärna
              </P>
            </>
          ),
          CLUB: (
            <>
              <P>
                Du kan ansöka till öppen småbarnspedagogik året runt, och
                platsen beviljas tills att platsen sägs upp eller tills att
                barnet flyttar till småbarnspedagogiken eller
                förskoleundervisningen. Beslutet om den öppna
                småbarnspedagogiken skickas till Suomi.fi-tjänsten eller per
                post, om tjänsten inte har tagits i bruk. Du hittar beslutet
                även i eVaka-tjänsten under Ansökning - Beslut.
              </P>
              <P>
                Den öppna småbarnspedagogiken (klubbar och lekparksverksamheten)
                som ordnas av Åbo stad är avgiftsfri.
              </P>
              <P>
                För mer information om den öppna småbarnspedagogiken, besök Åbo
                stads webbplats:{' '}
                <ExternalLink
                  text="Klubbar, lekparksverksamhet och öppen småbarnspedagogik."
                  href="https://www.turku.fi/varhaiskasvatus-ja-esiopetus/avoin-varhaiskasvatustoiminta"
                  newTab
                />
              </P>
              <P>* Obligatoriska uppgifter är markerade med en stjärna.</P>
            </>
          )
        }
      },
      serviceNeed: {
        preparatory: 'Barnet behöver stöd i att lära sig finska.',
        preparatoryInfo:
          'Avsett för barn vars modersmål inte är finska, svenska eller samiska. Barnets behov av förberedande undervisning bedöms i småbarnspedagogiken.',
        startDate: {
          header: {
            DAYCARE: 'Småbarnspedagogiken inleds',
            PRESCHOOL: 'Förskoleundervisningen inleds',
            CLUB: 'Den öppna småbarnspedagogiken inleds'
          },
          clubTerm: 'Verksamhetsperioden för den öppna småbarnspedagogiken',
          clubTerms: 'Verksamhetsperioderna för den öppna småbarnspedagogiken',
          label: {
            DAYCARE: 'Önskat startdatum',
            PRESCHOOL: 'Önskat startdatum',
            CLUB: 'Önskat startdatum för den öppna småbarnspedagogiken'
          },
          info: {
            DAYCARE: [],
            PRESCHOOL: [
              'Den finsk- och svenskspråkiga förskoleundervisningen inleds den 11 augusti 2026. Ansök om kompletterande småbarnspedagogik för barn i förskoleåldern under punkten Kompletterande småbarnspedagogik för barn i förskoleåldern. Om barnet övergår från privat till kommunal småbarnspedagogik, ska du också göra en ansökan till kompletterande småbarnspedagogik'
            ],
            CLUB: [
              'Klubbarna och lekparksverksamheten som ingår i den öppna småbarnspedagogiken följer förskoleundervisningens arbets- och semestertider. Barnet kan delta i en verksamhet inom den öppna småbarnspedagogiken åt gången, med undantag för familjeklubbar.'
            ]
          },
          instructions: {
            DAYCARE: (
              <>
                Du kan skjuta upp det önskade startdatumet fram till dess att
                servicehandledningen har börjat behandla ansökan. Efter detta
                kan startdatumet ändras endast genom att kontakta
                servicehandledningen
                {customerContactText()}
              </>
            ),
            PRESCHOOL: (
              <>
                Du kan skjuta upp det önskade startdatumet fram till dess att
                servicehandledningen har börjat behandla ansökan. Efter detta
                kan startdatumet ändras endast genom att kontakta
                servicehandledningen
                {customerContactText()}
              </>
            ),
            CLUB: null
          }
        },
        clubDetails: {
          wasOnDaycare:
            'Barnet har en plats inom småbarnspedagogiken som hen kommer att avstå från när hen får en plats inom den öppna småbarnspedagogiken.',
          wasOnDaycareInfo: '',
          wasOnClubCare:
            'Barnet har deltagit i den öppna småbarnspedagogiken under den föregående verksamhetsperioden.',
          wasOnClubCareInfo: ''
        },
        urgent: {
          attachmentsMessage: {
            text: (
              <P $fitted={true}>
                Om barnet behöver en plats inom småbarnspedagogiken på grund av
                ny arbets- eller studieplats ska du ansöka om platsen minst två
                veckor innan behovet blir aktuellt. Båda vårdnadshavarna som bor
                på samma adress ska bifoga ett arbets- eller studieintyg till
                ansökan. Handläggningstiden på två veckor börjar från det datum
                då vi mottar din ansökan och de nödvändiga bilagorna. Kontakta
                oss per telefon om du inte kan bifoga bilagorna elektroniskt.
                {customerContactText()} Du kan också skicka bilagorna per post
                till adressen Småbarnspedagogikens servicehandledning PB 355,
                20101 Åbo stad eller genom att leverera dem till Monitori vid
                Åbo salutorg, Småbarnspedagogikens servicehandledning, Auragatan
                8.
              </P>
            )
          }
        },
        shiftCare: {
          instructions:
            'Med kvälls- och skiftesvård avses småbarnspedagogik som ordnas som dygnetruntvård utanför tiden 6.00–18.00 och under veckoslut. Du kan ange eventuellt behov av kvälls- eller skiftesvård i fältet Ytterligare information.',
          attachmentsMessage: {
            DAYCARE: (
              <P>
                Kvälls- och skiftesvård är avsett för barn vars båda föräldrar
                arbetar i skift eller studerar huvudsakligen på kvällar
                och/eller under veckoslut. Båda vårdnadshavare bifogar ett intyg
                om sitt skiftesarbete eller ett intyg om behovet av kvälls-
                eller skiftesvård. Om du inte kan lägga till bilagorna
                elektroniskt, vänligen kontakta Småbarnspedagogikens
                servicehandledning per telefon på numret 02 2625610. Du kan
                också skicka bilagorna per post till adressen
                Småbarnspedagogikens servicehandledning PB 355, 20101 Åbo eller
                genom att lämna in dem till Salutorgets Monitori,
                Småbarnspedagogikens servicehandledning, Auragatan 8.
              </P>
            ),
            PRESCHOOL: (
              <>
                <P>
                  Kvälls- och skiftesvård är avsett för barn vars båda föräldrar
                  arbetar i skift eller studerar huvudsakligen på kvällar
                  och/eller under veckoslut. Båda vårdnadshavare bifogar ett
                  intyg om sitt skiftesarbete eller ett intyg om behovet av
                  kvälls- eller skiftesvård. Om du inte kan lägga till bilagorna
                  elektroniskt, vänligen kontakta Småbarnspedagogikens
                  servicehandledning per telefon på numret 02 2625610. Du kan
                  också skicka bilagorna per post till adressen
                  Småbarnspedagogikens servicehandledning PB 355, 20101 Åbo
                  eller genom att lämna in dem till Salutorgets Monitori,
                  Småbarnspedagogikens servicehandledning, Auragatan 8
                </P>
                <P>
                  Om en vårdnadshavare som bor i samma hushåll regelbundet utför
                  skiftarbete eller avlägger kvällsstudier som huvudsyssla, ska
                  du bifoga ett intyg över detta (av arbetsgivaren eller en
                  representant för läroanstalten) till ansökan om
                  förskoleundervisning. Dokumenten ska ha registrerats det år då
                  ansökan om förskoleundervisning görs.
                </P>
              </>
            )
          }
        },
        assistanceNeedInstructions: {
          DAYCARE:
            'Med barnets behov av stöd avses stödåtgärder som fastställts i expertutlåtanden. Dessa stödåtgärder genomförs i barnets vardag som en del av den övriga småbarnspedagogiska verksamheten. En speciallärare inom småbarnspedagogik kontaktar den sökande för att säkerställa att barnets behov kan beaktas när platsen till småbarnspedagogiken tilldelas.',
          CLUB: 'Med barnets behov av stöd avses stödåtgärder som fastställts i expertutlåtanden. Dessa stödåtgärder genomförs i barnets vardag som en del av den övriga småbarnspedagogiska verksamheten. En speciallärare inom småbarnspedagogik kontaktar den sökande för att säkerställa att barnets behov kan beaktas när platsen inom småbarnspedagogiken tilldelas.',
          PRESCHOOL:
            'Kryssa för detta ställe i ansökan om barnet behöver stöd för sin utveckling/inlärning under förskoleåret. Detta stöd ges som en del av förskoleundervisningen och småbarnspedagogiken. Specialläraren kontaktar den sökande för att säkerställa att barnets stödbehov kan beaktas vid tilldelningen av förskoleplats.'
        },
        partTime: {
          true: 'Deltid (max 20 h/v, max 84 h/mån)',
          false: 'Heltid'
        },
        dailyTime: {
          label: {
            DAYCARE: 'Servicebehov',
            PRESCHOOL:
              'Förskoleundervisning ges i daghem och skolor fyra timmar om dagen. Ange barnets behov av småbarnspedagogik så att den inkluderar också förskoletiden (t.ex. 7.00–17.00). Tiderna fastställs när småbarnspedagogiken börjar. Om behovet för småbarnspedagogik varierar från dag till dag eller vecka till vecka (t.ex. skiftvård), ge mer information om detta under punkten Ytterligare information.'
          },
          connectedDaycare:
            'Jag ansöker också om kompletterande småbarnspedagogik för ett barn i förskoleåldern.',
          connectedDaycareInfo: (
            <>
              <P>
                Vid behov kan du ansöka om avgiftsbelagd, kompletterande
                småbarnspedagogik för ett barn i förskoleåldern. Om du vill
                börja småbarnspedagogiken senare än när förskoleundervisningen
                börjar, ange önskat startdatum under Ytterligare information i
                ansökan. Om du vill ansöka om en servicesedel för ett privat
                daghem ska du ange den önskade servicesedelenheten som ditt
                förstahandsval.
              </P>
              <P>
                Du får ett skriftligt beslut om platsen inom småbarnspedagogiken{' '}
                <a
                  href="https://www.suomi.fi/viestit"
                  target="_blank"
                  rel="noreferrer"
                >
                  i Suomi.fi-tjänsten
                </a>{' '}
                eller, om du inte har tagit Suomi.fi-tjänsten i bruk, per
                brevpost. Du kan se beslutet även i eVaka-tjänsten under
                Ansökning - Beslut.
              </P>
            </>
          )
        }
      },
      verification: {
        serviceNeed: {
          wasOnClubCareYes:
            'Barnet har deltagit i den öppna småbarnspedagogiken under den föregående verksamhetsperioden.',
          connectedDaycare: {
            label: 'Kompletterande småbarnspedagogik',
            withConnectedDaycare:
              'Jag ansöker också om kompletterande småbarnspedagogik för ett barn i förskoleåldern.',
            withoutConnectedDaycare: 'Ei'
          }
        }
      },
      unitPreference: {
        title: 'Önskad enhet',
        siblingBasis: {
          title: 'Ansökan enligt syskonprincipen',
          info: {
            DAYCARE: (
              <>
                <P>
                  Barnet har förtur till samma daghem där hens syskon redan går.
                  Målet är att se till att barnen i samma familj går på samma
                  daghem om familjen så önskar. Om du ansöker om plats för ett
                  syskonpar som <b>inte ännu har börjat</b> på
                  småbarnspedagogiken, ange detta i punkten Ytterligare
                  information i ansökan.
                </P>
                <P>
                  Fyll i dessa uppgifter endast om du vill tillämpa
                  syskonprincipen. Kom också ihåg att välja enheten där barnets
                  syskon redan går på som ert förstahandsval.
                </P>
              </>
            ),
            PRESCHOOL: (
              <>
                <P>Syskonprincipen tillämpas inte i Åbo.</P>
              </>
            ),
            CLUB: (
              <>
                <P>
                  Målet är att placera syskonen i samma öppna småbarnspedagogik
                  om familjen så önskar.
                </P>
                <P>
                  Fyll i dessa uppgifter endast om du vill tillämpa
                  syskonprincipen. Kom också ihåg att välja den enhet för öppen
                  småbarnspedagogik där barnets syskon redan går på som ert
                  förstahandsval.
                </P>
              </>
            )
          },
          checkbox: {
            DAYCARE:
              'Mitt förstahandsval är samma enhet för småbarnspedagogik som barnets syskon redan går på.',
            PRESCHOOL:
              'Mitt förstahandsval är samma enhet som barnets syskon redan går på.',
            CLUB: 'Vårt förstahandsval är samma enhet för öppen småbarnspedagogik som barnets syskon redan går på.'
          },
          radioLabel: {
            DAYCARE:
              'Välj det syskon vars enhet för småbarnspedagogik du ansöker till',
            PRESCHOOL: 'Välj det syskon vars enhet du ansöker till',
            CLUB: 'Välj det syskon vars enhet för öppen småbarnspedagogik du ansöker till'
          },
          otherSibling: 'Andra syskon',
          names: 'Syskonens för- och efternamn',
          namesPlaceholder: 'För- och efternamn',
          ssn: 'Syskonens personbeteckning',
          ssnPlaceholder: 'Personbeteckning'
        },
        units: {
          title: (maxUnits: number): string =>
            maxUnits === 1 ? 'Önskad enhet' : 'Önskade enheter',
          startDateMissing:
            'För att kunna ange de önskade enheterna, börja med att välja önskat startdatum i avsnittet "Servicebehov"',
          info: {
            DAYCARE: (
              <>
                <P>
                  Du kan ansöka om 1–3 platser i önskad ordning. Vi kan inte
                  garantera att barnet får en plats vid den önskade enheten, men
                  chansen att få en plats i den önskade enheten växer om ni ger
                  flera alternativ.
                </P>
                <P>
                  För att se var de olika enheterna ligger, klicka på ‘Enheterna
                  på kartan’.
                </P>
                <P>
                  Ansök om en servicesedel genom att välja den önskade
                  servicesedelenheten som önskad enhet. Om ditt förstahandsval
                  är en servicesedelenhet, ska du kontakta ifrågavarande enhet.{' '}
                  <i>
                    När du ansöker om plats till en servicesedelenhet,
                    informeras enhetens föreståndare om saken.
                  </i>
                </P>
              </>
            ),
            PRESCHOOL: (
              <>
                <P>
                  Du kan ansöka om 1–3 platser i önskad ordning. Vi kan inte
                  garantera att barnet får en plats vid den önskade enheten, men
                  chansen att få en plats i den önskade enheten växer om ni ger
                  flera alternativ.
                </P>
                <P>
                  För att se var de olika enheterna ligger, klicka på ‘Enheterna
                  på kartan’.
                </P>
                <P>
                  Ansök om en servicesedel genom att välja den önskade
                  servicesedelenheten som önskad enhet. När du ansöker om plats
                  till en servicesedelenhet, informeras enhetens föreståndare om
                  saken.
                </P>
              </>
            ),
            CLUB: (
              <>
                <P>
                  Du kan ansöka om 1–3 platser i önskad ordning. Vi kan inte
                  garantera att barnet får en plats vid den önskade enheten för
                  småbarnspedagogik, men chansen att få en plats vid den önskade
                  enheten växer om ni ger flera alternativ.
                </P>
                <P>
                  Du kan se var enheterna för öppen småbarnspedagogik ligger
                  genom att klicka på ’Visa enheterna på kartan’.
                </P>
                <P>
                  Visa enheterna på kartan-länken öppnar en karta över Åbo.
                  Lämna valet Enhetens språk oförändrat.
                </P>
              </>
            )
          },
          mapLink: 'Visa enheterna på kartan',
          serviceVoucherLink:
            'https://www.turku.fi/varhaiskasvatus-ja-esiopetus/maksut-tuet-ja-palveluseteli',
          languageFilter: {
            label: 'Enhetens språk',
            fi: 'finska',
            sv: 'svenska'
          },
          select: {
            label: (maxUnits: number): string =>
              maxUnits === 1 ? 'Välj önskad enhet' : 'Välj önskade enheter',
            placeholder: 'Sök enheter',
            maxSelected: 'Du har valt det maximala antalet enheter',
            noOptions: 'Hittade inga enheter som motsvarar sökkriterierna'
          },
          preferences: {
            label: (maxUnits: number): string =>
              maxUnits === 1 ? 'Din önskad enhet' : 'Dina önskade enheter',
            noSelections: 'Inga val',
            info: (maxUnits: number) =>
              maxUnits === 1
                ? 'Välj en enhet för småbarnspedagogik'
                : `Välj 1-${maxUnits} enheter för småbarnspedagogik och ordna dem i önskad prioritetsordning. Använd pilarna för att ändra ordningen.`,
            fi: 'finskspråkig',
            sv: 'svenskspråkig',
            en: 'engelskspråkig',
            moveUp: 'Flytta upp',
            moveDown: 'Flytta ned',
            remove: 'Ta bort den önskade enheten'
          }
        }
      },
      contactInfo: {
        secondGuardianInfoPreschoolSeparated:
          'Den andra vårdnadshavarens uppgifter hämtas automatiskt från befolkningsdatasystemet. Enligt våra uppgifter bor barnets andra vårdnadshavare på en annan adress. Vårdnadshavarna bör komma överens om anmälan till förskoleundervisning innan den lämnas in.',
        info: (
          <>
            <P>Ange alla vuxna och barn som bor i samma hushåll.</P>
            <P data-qa="contact-info-text">
              Personuppgifterna hämtas från befolkningsdatasystemet och kan inte
              ändras med denna ansökan. Om personuppgifterna är felaktiga,
              vänligen uppdatera uppgifterna på webbplatsen för
              <ExternalLink
                text="Myndigheten för digitalisering och befolkningsdata"
                href="https://dvv.fi/henkiloasiakkaat"
                newTab
              />
              . Om din adress kommer att ändras kan du ange den framtida
              adressen i ett separat fält: ange framtida adress för barnet och
              dennes vårdnadshavare. Adressuppgifterna anses vara officiella
              först när de har uppdaterats i befolkningsdatasystemet. Beslut om
              platser inom förskoleundervisningen och småbarnspedagogiken
              skickas också till vårdnadshavare som bor på en annan adress och
              vars uppgifter finns i befolkningsdatasystemet.
            </P>
          </>
        ),
        futureAddressInfo:
          'Åbo småbarnspedagogik anser att den officiella adressen är den som finns i befolkningsdatasystemet. Den sökande kan ändra adressen i befolkningsdatasystemet genom att göra en flyttanmälan i till Posti eller Myndigheten för digitalisering och befolkningsdata.'
      },
      fee: {
        info: {
          DAYCARE: (
            <P>
              Kundavgifterna inom den kommunala småbarnspedagogiken och
              servicesedelns självriskandel beräknas i procentandel av familjens
              bruttoinkomster. Avgiften varierar beroende på familjens storlek
              och inkomster samt på det hur mycket barnet deltar i
              småbarnspedagogiken. Om avgiften för platsen för småbarnspedagogik
              överstiger servicesedelns värde är det familjen som betalar
              skillnaden. Familjen ska lämna in en inkomstutredning om sina
              bruttoinkomster på inkomstutredningsblanketten så snart som
              möjligt efter att barnet har börjat inom småbarnspedagogiken.
            </P>
          ),
          PRESCHOOL: (
            <P>
              Förskoleundervisningen är avgiftsfri, men den kompletterande
              småbarnspedagogiken är avgiftsbelagd. Om barnet deltar i
              kompletterande småbarnspedagogik ska familjen lämna in en
              utredning om sina bruttoinkomster via inkomstutredningsblanketten
              så snart som möjligt efter att barnet har börjat på
              småbarnspedagogiken.
            </P>
          )
        },
        links: (
          <>
            <P>
              Ni hittar inkomstutredningsblanketten i eVaka under
              Inkomstuppgifter i Användare-menyn.
            </P>
            <P>
              Mer information om kundavgifterna finns på Åbo stads webbplats:{' '}
              <ExternalLink
                href="https://www.turku.fi/varhaiskasvatus-ja-esiopetus/maksut-tuet-ja-palveluseteli"
                text="Avgifter inom småbarnspedagogiken."
                newTab
              />
            </P>
          </>
        )
      },
      sentInfo: {
        title: 'Ansökan har skickats in',
        text: 'Vårdnadshavaren kan göra ändringar i ansökan via e-tjänsten tills småbarnspedagogikens servicehandledning börjar behandla ansökan.',
        ok: 'Okej!'
      },
      actions: {
        allowOtherGuardianAccess: (
          <span>
            Jag förstår att information om ansökan skickas också till den andra
            vårdnadshavaren. Kontakta servicehandledningen om du inte vill att
            den andra vårdnadshavaren informeras om ansökan.
          </span>
        )
      }
    }
  },
  applicationsList: {
    title: 'Ansökan till småbarnspedagogiken',
    summary: (
      <>
        <P $width="800px">
          Barnets vårdnadshavare kan göra en ansökan till småbarnspedagogik och
          öppen småbarnspedagogik eller anmäla barnet till förskoleundervisning.
          Du kan ansöka om servicesedel för småbarnspedagogik med samma ansökan
          genom att ansöka om en plats på en servicesedelenhet. Uppgifterna om
          vårdnadshavarens barn hämtas automatiskt från befolkningsdatasystemet.
        </P>
        <P $width="800px">
          Om barnet redan har en plats på en enhet för småbarnspedagogik i Åbo
          och du vill flytta barnet till en annan enhet måste du göra en ny
          ansökan.
        </P>
      </>
    )
  },
  footer: {
    cityLabel: '© Åbo stad',
    privacyPolicyLink: (
      <ExternalLink
        href="https://www.turku.fi/sv/datasekretess"
        text="Datasekretess"
        newTab={true}
        data-qa="footer-policy-link"
      />
    ),
    sendFeedbackLink: (
      <ExternalLink
        href="https://opaskartta.turku.fi/efeedback"
        text="Skicka feedback"
        newTab={true}
        data-qa="footer-feedback-link"
      />
    )
  },
  loginPage: {
    title: 'Åbo stads småbarnspedagogik',
    login: {
      title: 'Logga in med ditt användarnamn',
      paragraph:
        'Vårdnadshavare vars barn redan har en plats i småbarnspedagogiken eller förskoleundervisningen: sköt vardagliga ärenden, t.ex. läs meddelanden eller rapportera barnets när- och frånvarotider.',
      link: 'Logga in',
      infoBoxText: (
        <>
          <P>
            Du kan skapa ett eVaka-användarnamn genom att logga in med stark
            autentisering.
          </P>
        </>
      )
    }
  },
  map: {
    mainInfo: `På denna karta kan du söka efter Åbos enheter för småbarnspedagogik, förskoleundervisning och öppen småbarnspedagogik. För mer information om privata daghem, besök webbplatsen för Åbo stads småbarnspedagogik.`,
    privateUnitInfo: <></>,
    serviceVoucherLink:
      'https://www.turku.fi/varhaiskasvatus-ja-esiopetus/maksut-tuet-ja-palveluseteli',
    searchPlaceholder: 'T.ex. Arkeologinkadun päiväkoti.',
    careTypes: {
      CLUB: 'Öppen småbarnspedagogik',
      DAYCARE: 'Småbarnspedagogik',
      PRESCHOOL: 'Förskoleundervisning'
    }
  },
  decisions: {
    assistanceDecisions: {
      title: 'Beslut om behov av stöd inom småbarnspedagogik',
      decision: {
        pageTitle: 'Beslut om behov av stöd inom småbarnspedagogik',
        jurisdiction: 'Befogenhet',
        jurisdictionText: (): React.ReactNode =>
          'Beslut om organisation för nämnden för fostran och undervisning, 3 kap. 11 §',
        unitMayChange: 'Enheten kan ändras under semestertider',
        appealInstructionsTitle: 'Anvisning om hur man begär omprövning',
        appealInstructions: (
          <>
            <H3>Anvisning om hur man begär omprövning</H3>
            <P>
              Du kan begära om omprövning av ovanstående beslut hos
              Regionförvaltningsverket i Sydvästra Finland inom 30 dagar från
              dagen för delfående av beslutet. Beslutet kan inte överklagas
              genom besvär hos förvaltningsdomstolen.
            </P>
            <P>En begäran om omprövning</P>
            <ul>
              <li>får göras av den person som beslutet gäller </li>
              <li>
                {' '}
                eller den person vars rätt, skyldighet eller fördel direkt
                påverkas av beslutet.
              </li>
            </ul>
            <h3>Delfående</h3>
            <P>
              Om beslutet delgivits per brev, anses den berörda parten (barnets
              vårdnadshavare) ha tagit del av beslutet, om inte annat bevisas, 7
              dagar efter det att brevet skickades.
            </P>
            <P>
              Om beslutet skickas elektroniskt anses den berörda parten ha tagit
              del av beslutet tre dagar efter att meddelandet skickades.
            </P>
            <P>
              Om beslutet delges personligen, anses den berörda parten (barnets
              vårdnadshavare) ha tagit del av beslutet den dag då beslutet
              delgivits till den berörda parten eller deras lagliga företrädare.
            </P>
            <P>
              Om dokumentet skickas med post mot ett mottagningsbevis , anses
              den berörda parten ha tagit del av beslutet vid den tidpunkt som
              anges i mottagningsbeviset.
            </P>
            <P>
              Själva delfåendedagen räknas inte med i besvärstiden. Om
              besvärstidens sista dag infaller på en helgdag,
              självständighetsdagen, första maj, jul- eller midsommarafton eller
              en helgfri lördag är den första dagen då man får göra en begäran
              om omprövning den följande vardagen.
            </P>
            <H3>Innehållet i begäran om omprövning</H3>
            <P>
              Begäran ska vara skriftlig. Ett elektroniskt dokument fyller också
              kravet på en skriftlig begäran.
            </P>
            <P>I begäran om omprövning ska det framgå</P>
            <ul>
              <li>vilket beslut begäran handlar om</li>
              <li>
                vilka punkter i beslutet begäran berör och hurdan omprövning som
                begärs
              </li>
              <li>på vilka grunder omprövning begärs</li>
            </ul>
            <P>
              Begäran om omprövning måste innehålla namnet och hemorten till den
              som begär omprövning. Om begäran bereds av en laglig företrädare
              till den som begär omprövning eller hens representant, eller om
              begäran bereds av en annan person, ska också dennes namn och
              hemort anges i begäran.
            </P>
            <P>
              En begäran om omprövning ska även innehålla dennes postadress,
              telefonnummer och andra relevanta kontaktuppgifter. Om beslutet
              kan meddelas elektroniskt, bör även e-postadress anges.
            </P>
            <P>
              Den som framställt begäran eller dennes lagliga företrädare eller
              ombud ska underteckna begäran. Ett elektroniskt dokument behöver
              inte undertecknas om dokumentet innehåller avsändarens uppgifter
              och det finns ingen anledning att tvivla på dess äkthet eller
              integritet.
            </P>
            <P>Till begäran om omprövning bifogas</P>
            <ul>
              <li>beslutet som begäran gäller (originalet eller en kopia)</li>
              <li>
                ett intyg från vilket det framgår när beslutet delgavs eller en
                annan utredning där det framgår när besvärstiden började.
              </li>
              <li>
                dokumenten som den som skickat in begäran åberopar i fall dessa
                inte tidigare skickats till myndigheten.
              </li>
            </ul>
            <H3>Inlämnande av en begäran om omprövning</H3>
            <P>
              Begäran om omprövning skickas in inom besvärstiden till
              Regionförvaltiningsverket i Västra och Inre Finland på adressen:
            </P>
            <P>
              Regionförvaltiningsverket i Västra och Inre Finland
              <br />
              PB 5, 13035 AVI
              <br />
              Wolffskavägen 35
              <br />
              E-post: registratur.vastra@rfv.fi
              <br />
              Tfn: 0295 018 450
              <br />
              Öppettider för registratorskontoret: 8.00–16.15
            </P>
            <P>
              Om begäran om omprövning skickas via post eller med bud sker detta
              på avsändarens eget ansvar. Dokumenten i begäran om omprövning ska
              skickas per post i så god tid att de hinner fram senast under
              besvärstidens sista dag innan registratorskontoret stänger.
            </P>
            <P>
              Begäran om omprövning kan också skickas in via fax eller e-post
              innan besvärstiden upphör, men detta sker på avsändarens eget
              ansvar. Avsändaren ska se till att dokumentet levereras så att det
              befinner sig i myndighetens mottagningsenhet eller
              informationssystem innan tidsfristen.
            </P>
          </>
        )
      }
    },
    assistancePreschoolDecisions: {
      jurisdiction: 'Befogenhet',
      jurisdictionText:
        'Beslut om organisation för nämnden för fostran och undervisning, 3 kap. 11 §',
      appealInstructions: (
        <>
          <H3>Anvisning om hur man begär omprövning</H3>
          <P>
            Du kan begära om omprövning av ovanstående beslut hos
            Regionförvaltningsverket i Sydvästra Finland inom 14 dagar från
            dagen för delfående av beslutet. Beslutet kan inte överklagas genom
            besvär hos förvaltningsdomstolen.
          </P>
          <P>En begäran om omprövning</P>
          <ul>
            <li>får göras av den person som beslutet gäller </li>
            <li>
              {' '}
              eller den person vars rätt, skyldighet eller fördel direkt
              påverkas av beslutet.
            </li>
          </ul>
          <h3>Delfående</h3>
          <P>
            Om beslutet delgivits per brev, anses den berörda parten (barnets
            vårdnadshavare) ha tagit del av beslutet, om inte annat bevisas, 7
            dagar efter det att brevet skickades.
          </P>
          <P>
            Om beslutet skickas elektroniskt anses den berörda parten ha tagit
            del av beslutet tre dagar efter att meddelandet skickades.
          </P>
          <P>
            Om beslutet delges personligen, anses den berörda parten (barnets
            vårdnadshavare) ha tagit del av beslutet den dag då beslutet
            delgivits till den berörda parten eller deras lagliga företrädare.
          </P>
          <P>
            Om dokumentet skickas med post mot ett mottagningsbevis , anses den
            berörda parten ha tagit del av beslutet vid den tidpunkt som anges i
            mottagningsbeviset.
          </P>
          <P>
            Själva delfåendedagen räknas inte med i besvärstiden. Om
            besvärstidens sista dag infaller på en helgdag,
            självständighetsdagen, första maj, jul- eller midsommarafton eller
            en helgfri lördag är den första dagen då man får göra en begäran om
            omprövning den följande vardagen.
          </P>
          <H3>Innehållet i begäran om omprövning</H3>
          <P>
            Begäran ska vara skriftlig. Ett elektroniskt dokument fyller också
            kravet på en skriftlig begäran.
          </P>
          <P>I begäran om omprövning ska det framgå</P>
          <ul>
            <li>vilket beslut begäran handlar om</li>
            <li>
              vilka punkter i beslutet begäran berör och hurdan omprövning som
              begärs
            </li>
            <li>på vilka grunder omprövning begärs</li>
          </ul>
          <P>
            Begäran om omprövning måste innehålla namnet och hemorten till den
            som begär omprövning. Om begäran bereds av en laglig företrädare
            till den som begär omprövning eller hens representant, eller om
            begäran bereds av en annan person, ska också dennes namn och hemort
            anges i begäran.
          </P>
          <P>
            En begäran om omprövning ska även innehålla dennes postadress,
            telefonnummer och andra relevanta kontaktuppgifter. Om beslutet kan
            meddelas elektroniskt, bör även e-postadress anges.
          </P>
          <P>
            Den som framställt begäran eller dennes lagliga företrädare eller
            ombud ska underteckna begäran. Ett elektroniskt dokument behöver
            inte undertecknas om dokumentet innehåller avsändarens uppgifter och
            det finns ingen anledning att tvivla på dess äkthet eller
            integritet.
          </P>
          <P>Till begäran om omprövning bifogas</P>
          <ul>
            <li>beslutet som begäran gäller (originalet eller en kopia)</li>
            <li>
              ett intyg från vilket det framgår när beslutet delgavs eller en
              annan utredning där det framgår när besvärstiden började.
            </li>
            <li>
              dokumenten som den som skickat in begäran åberopar i fall dessa
              inte tidigare skickats till myndigheten.
            </li>
          </ul>
          <H3>Inlämnande av en begäran om omprövning</H3>
          <P>
            Begäran om omprövning skickas in inom besvärstiden till
            Regionförvaltiningsverket i Västra och Inre Finland på adressen:
          </P>
          <P>
            Regionförvaltiningsverket i Västra och Inre Finland
            <br />
            PB 5, 13035 AVI
            <br />
            Wolffskavägen 35
            <br />
            E-post: registratur.vastra@rfv.fi
            <br />
            Tfn: 0295 018 450
            <br />
            Öppettider för registratorskontoret: 8.00–16.15
          </P>
          <P>
            Om begäran om omprövning skickas via post eller med bud sker detta
            på avsändarens eget ansvar. Dokumenten i begäran om omprövning ska
            skickas per post i så god tid att de hinner fram senast under
            besvärstidens sista dag innan registratorskontoret stänger.
          </P>
          <P>
            Begäran om omprövning kan också skickas in via fax eller e-post
            innan besvärstiden upphör, men detta sker på avsändarens eget
            ansvar. Avsändaren ska se till att dokumentet levereras så att det
            befinner sig i myndighetens mottagningsenhet eller
            informationssystem innan tidsfristen.
          </P>
        </>
      ),
      disclaimer:
        'Ett beslut som fattats i enlighet med lagen om grundläggande utbildning 17§ kan förverkligas även om någon sökt ändring av beslutet.'
    },
    summary: 'Alla barnets beslut kommer på denna sidan.'
  },
  income: {
    description: (
      <>
        <p data-qa="income-description-p1">
          På den här sidan kan du skicka in utredningar om dina inkomster som
          påverkar avgiften för småbarnspedagogik. Du kan även kontrollera
          tidigare inkomstutredningar och redigera eller ta bort dem fram till
          att myndigheten har behandlat uppgifterna. Efter handläggningen kan du
          uppdatera dina inkomstuppgifter genom att fylla i och skicka in ett
          nytt formulär.
        </p>
        <p data-qa="income-description-p2">
          <strong>
            Vuxna som bor i samma hushåll ska skicka in sina egna, separata
            inkomstutredningar.
          </strong>
        </p>
        <p data-qa="income-description-p3">
          Kundavgiften för kommunal småbarnspedagogik beräknas i procentandel av
          familjens bruttoinkomster. Avgiften varierar beroende på familjens
          storlek och inkomster samt på hur mycket barnet deltar i
          småbarnspedagogiken.
        </p>
        <p data-qa="income-description-p4">
          <a href="https://www.turku.fi/sv/smabarnspedagogik-och-forskoleundervisning/avgifter-understod-och-servicesedlar">
            Ytterligare information om kundavgifter
          </a>
        </p>
      </>
    ),
    incomeType: {
      startDate: 'Gäller från',
      endDate: 'Gäller till',
      title: 'Kundavgifter',
      agreeToHighestFee: 'Jag samtycker till den högsta avgiften',
      highestFeeInfo:
        'Den högsta avgiften, relaterat till servicebehovet, gäller tills vidare eller tills jag anmäl annat eller mitt barn inte längre deltar i småbarnspedagogiken. (Jag behöver inte skicka in mina inkomstuppgifter)',
      grossIncome: 'Fastställande av avgift baserat på bruttoinkomst'
    },
    grossIncome: {
      title: 'Fylla i information om bruttoinkomst',
      description: (
        <>
          <P> </P>
        </>
      ),
      incomeSource: 'Inlämnande av inkomstuppgifter',
      incomesRegisterConsent:
        'Jag godkänner att uppgifter om mina inkomster granskas från inkomstregistret och jag kommer att bifoga eventuella förmånsuppgifter som bilagor.',
      provideAttachments: 'Jag lämnar in mina inkomstuppgifter som bilaga',
      estimate: 'Uppskattad bruttoinkomst',
      estimatedMonthlyIncome:
        'Genomsnittliga inkomster, inklusive semesterlön, €/mån',
      otherIncome: 'Övriga inkomster',
      otherIncomeDescription:
        'Om du har övriga inkomster ska du skicka in intyg om dem som bilaga. En lista på nödvändiga bilagor hittar du i nedre delen av blanketten under: Bilagor relaterade till inkomsterna och avgiften för småbarnspedagogik.',
      choosePlaceholder: 'Välj',
      otherIncomeTypes: {
        PENSION: 'Pension',
        ADULT_EDUCATION_ALLOWANCE: 'Vuxenutbildningsstöd',
        SICKNESS_ALLOWANCE: 'Sjukpenning',
        PARENTAL_ALLOWANCE: 'Moderskaps- och föräldrapenning',
        HOME_CARE_ALLOWANCE: 'Stöd för hemvård av barn',
        FLEXIBLE_AND_PARTIAL_HOME_CARE_ALLOWANCE:
          'Flexibel eller partiell vårdpenning',
        ALIMONY: 'Underhållsbidrag eller -stöd',
        INTEREST_AND_INVESTMENT_INCOME: 'Ränte- och utdelningsinkomster',
        RENTAL_INCOME: 'Hyresinkomst',
        UNEMPLOYMENT_ALLOWANCE: 'Arbetslöshetsdagpenning',
        LABOUR_MARKET_SUBSIDY: 'Arbetsmarknadsstöd',
        ADJUSTED_DAILY_ALLOWANCE: 'Justerad dagpenning',
        JOB_ALTERNATION_COMPENSATION: 'Alterneringsersättning',
        REWARD_OR_BONUS: 'Arvode eller bonus',
        RELATIVE_CARE_SUPPORT: 'Stöd för närståendevård',
        BASIC_INCOME: 'Basinkomst',
        FOREST_INCOME: 'Skogsinkomster',
        FAMILY_CARE_COMPENSATION: 'Arvoden för familjevård',
        REHABILITATION: 'Rehabiliteringsbidrag eller -penning',
        EDUCATION_ALLOWANCE: 'Utbildningsdagpenning',
        GRANT: 'Stipendium',
        APPRENTICESHIP_SALARY: 'Löneinkomster från läroavtalsutbildning',
        ACCIDENT_INSURANCE_COMPENSATION:
          'Ersättning från olycksfallsförsäkringen',
        OTHER_INCOME: 'Övriga inkomster'
      },
      otherIncomeInfoLabel: 'Uppskattad övrig inkomst',
      otherIncomeInfoDescription:
        'Ange här en uppskattning om övriga inkomster (€/månad), t.ex. "Hyresinkomst 150, stöd för hemvård av barn 300"'
    },
    entrepreneurIncome: {
      title: 'Fyll i företagarens inkomstuppgifter',
      description:
        'Med denna blankett kan du vid behov fylla i uppgifterna för flera företag genom att välja de punkter som gäller alla dina företag.',
      startOfEntrepreneurship: 'Företaget grundades',
      spouseWorksInCompany: 'Arbetar din make/maka på företaget?',
      yes: 'Ja',
      no: 'Nej',
      startupGrantLabel: 'Får ditt företag startpeng?',
      startupGrant:
        'Mitt företag får startpeng. Jag bifogar beslutet om startpeng.',
      checkupLabel: 'Kontroll av uppgifter',
      checkupConsent:
        'Jag samtycker till att uppgifter relaterade till mina inkomster kontrolleras i Inkomstregistret och FPA vid behov.',
      companyInfo: 'Information om företaget',
      companyForm: 'Företagsform',
      selfEmployed: 'Firma',
      limitedCompany: 'Aktiebolag',
      partnership: 'Öppet bolag eller kommanditbolag',
      lightEntrepreneur: 'Lättföretagare',
      lightEntrepreneurInfo:
        'Kvitton på betalning av löner och arbetsersättningar bifogas som bilagor.',
      partnershipInfo: ''
    },
    moreInfo: {
      title: 'Övrig information relaterad till betalningen',
      studentLabel: 'Är du studerande?',
      student: 'Jag är studerande.',
      studentInfo:
        'Studerande ska skicka in ett studieintyg och ett beslut om studiestöd.',
      deductions: 'Avdrag',
      alimony: 'Jag betalar underhåll. Jag bifogar en kopia av kvittot.',
      otherInfoLabel: 'Ytterligare information om inkomster'
    },
    attachments: {
      title:
        'Bilagor relaterade till inkomsterna och avgiften för småbarnspedagogik',
      description:
        'Här kan du skicka in eventuella bilagor relaterade till inkomsterna och avgiften för småbarnspedagogik. Inga bilagor krävs om familjen har gett sitt samtycke till den högsta avgiften.',
      required: {
        title: 'Obligatoriska bilagor'
      },
      attachmentNames: {
        PENSION: 'Beslut om pension',
        ADULT_EDUCATION_ALLOWANCE: 'Beslut om vuxenutbildningsstöd',
        SICKNESS_ALLOWANCE: 'Beslut om sjukpenning',
        PARENTAL_ALLOWANCE: 'Beslut om moderskaps- och föräldrapenning',
        HOME_CARE_ALLOWANCE: 'Beslut om stöd för hemvård av barn',
        FLEXIBLE_AND_PARTIAL_HOME_CARE_ALLOWANCE: 'Beslut om vårdpenning',
        ALIMONY: 'Underhållsavtal eller beslut om underhållsstöd',
        UNEMPLOYMENT_ALLOWANCE: 'Beslut om arbetslöshetsdagpenning',
        LABOUR_MARKET_SUBSIDY: 'Beslut om arbetsmarknadsstöd',
        ADJUSTED_DAILY_ALLOWANCE: 'Beslut om dagpenning',
        JOB_ALTERNATION_COMPENSATION: 'Kvitto på alterneringsersättning',
        REWARD_OR_BONUS: 'Lönebesked om bonus och/eller arvode',
        RELATIVE_CARE_SUPPORT: 'Beslut om stöd för närståendevård',
        BASIC_INCOME: 'Beslut om basinkomst',
        FOREST_INCOME: 'Kvitto på skogsinkomst',
        FAMILY_CARE_COMPENSATION: 'Kvitton på arvoden för familjevård',
        REHABILITATION: 'Beslut om rehabiliteringsbidrag eller -penning',
        EDUCATION_ALLOWANCE: 'Beslut om utbildningsdagpenning',
        GRANT: 'Kvitto på betalning av stipendium',
        APPRENTICESHIP_SALARY:
          'Kvitto på betalning av löneinkomster från läroavtalsutbildning',
        ACCIDENT_INSURANCE_COMPENSATION:
          'Kvitto på ersättning från olycksfallsförsäkringen',
        OTHER_INCOME: 'Bilagor om övriga inkomster',
        ALIMONY_PAYOUT: 'Kvitto på betalning av underhåll',
        INTEREST_AND_INVESTMENT_INCOME:
          'Intyg över ränte- och utdelningsinkomster',
        RENTAL_INCOME: 'Intyg över hyresinkomst och bolagsvederlag',
        PAYSLIP_GROSS: 'Nyaste lönebesked',
        PAYSLIP_LLC: 'Nyaste lönebesked',
        STARTUP_GRANT: 'Beslut om startpeng',
        ACCOUNTANT_REPORT_PARTNERSHIP:
          'Bokförarens utredning av lön och naturaförmåner',
        ACCOUNTANT_REPORT_LLC:
          'Bokförarens utredning över naturaförmåner och utdelningar',
        PROFIT_AND_LOSS_STATEMENT_SELF_EMPLOYED:
          'Resultaträkning och balansräkning eller beskattningsbeslut',
        PROFIT_AND_LOSS_STATEMENT_PARTNERSHIP:
          'Resultaträkning och balansräkning',
        SALARY: 'Kvitton på betalning av löner och arbetsersättningar',
        PROOF_OF_STUDIES:
          'Studieintyg eller beslut om Arbetslöshetskassans studiestöd/Sysselsättningsfondens utbildningsstöd',
        CHILD_INCOME: 'Intyg om barnets inkomster'
      }
    },
    selfEmployed: {
      info: '',
      attachments:
        'Jag bifogar företagets senaste balansräkning eller skattedeklaration.',
      estimatedIncome:
        'Jag är en ny företagare. Jag fyller i en uppskattning av min genomsnittliga månadsinkomst. Jag skickar in resultaträkningen och balansräkningen så snart som möjligt.',
      estimatedMonthlyIncome: 'Genomsnittlig inkomst €/mån',
      timeRange: 'Under perioden'
    },
    formDescription: (
      <>
        <P data-qa="income-formDescription-p1">
          Inkomstutredning inklusive bilagor returneras under den månad då
          småbarnspedagogiken inleds. Ifall uppgifterna är ofullständiga kan
          avgiften fastställas så att den är den högsta möjliga. Ofullständiga
          inkomstuppgifter korrigeras inte retroaktivt efter att besvärstiden
          har gått ut.
        </P>
        <P>
          Avgiften tas ut från och med den dag då småbarnspedagogiken började.
        </P>
        <P>
          Kunden ska omedelbart anmäla on alla ändringar i sina inkomster och
          familjestorlek till småbarnspedagogikens kundavgiftsteam. Myndigheten
          har rätt att efter behov att ta ut avgifter inom småbarnspedagogiken
          även retroaktivt.
        </P>
        <P>
          <strong>Observera:</strong>
        </P>
        <Gap $size="xs" />
        <UnorderedList data-qa="income-formDescription-ul">
          <li>
            Om dina inkomster överstiger inkomstgränsen som fastställts för din
            familjestorlek ska du ge ditt samtycke till den högsta kundavgiften.
            Då slipper du göra en inkomstutredning.
          </li>
          <li>
            Om det finns en annan vuxen i ditt hushåll ska även hen lämna in en
            inkomstutredning genom att logga in på eVaka med sina egna
            personuppgifter och fylla i detta formulär.
          </li>
          <li>
            Kontrollera de aktuella inkomstgränserna{' '}
            <a
              target="_blank"
              rel="noreferrer"
              href="https://www.turku.fi/sv/smabarnspedagogik-och-forskoleundervisning/avgifter-understod-och-servicesedlar"
            >
              här
            </a>
            .
          </li>
        </UnorderedList>
        <P>* Obligatoriska uppgifter är markerade med en stjärna.</P>
      </>
    )
  },
  accessibilityStatement: (
    <>
      <H1>Tillgänglighetsutlåtande</H1>
      <P>
        Denna tillgänglighetsbeskrivning gäller Åbo stads eVaka-tjänst för
        småbarnspedagogiken på adressen{' '}
        <a href="https://evaka.turku.fi">evaka.turku.fi</a>. Åbo stad strävar
        efter att säkerställa tjänstens tillgänglighet, kontinuerligt förbättra
        användarupplevelsen samt tillämpa lämpliga tillgänglighetsstandarder.
      </P>
      <P>
        Tillgängligheten av tjänsten har utvärderats av tjänstens
        utvecklingsteam och detta utlåtande har upprättats den 12 april 2022.
      </P>
      <H2>Tjänstens tillgänglighet</H2>
      <P>
        Tjänsten uppfyller tillgänglighetskriterierna på nivåerna AA som krävs
        enligt lagen (WCAG v2.1). Alla delar av tjänsten uppfyller inte kraven.
      </P>
      <H2>Åtgärder för att förbättra tillgängligheten</H2>
      <P>
        Webbtjänstens tillgänglighet säkerställs genom bland annat följande
        åtgärder:
      </P>
      <ul>
        <li>
          Tillgänglighet beaktas redan i planeringsfasen, bl.a. genom att välja
          tjänstens färger och teckenstorlekar från ett
          tillgänglighetsperspektiv.
        </li>
        <li>
          Tjänstens beståndsdelar har definierats på ett semantiskt
          sammanhängande sätt.
        </li>
        <li>Tjänsten testas kontinuerligt med hjälp av en skärmläsare.</li>
        <li>
          Olika användare testar tjänsten och ger feedback om tjänstens
          tillgänglighet.
        </li>
        <li>
          Tjänstens tillgänglighet övervakas ständigt vid tekniska eller
          innehållsmässiga förändringar.
        </li>
      </ul>
      <P>
        Detta utlåtande uppdateras i samband med att webbplatsen förändras och
        tillgängligheten kontrolleras.
      </P>
      <H2>Kända problem med tillgängligheten</H2>
      <P>
        Användarna kan fortfarande stöta på vissa problem när de använder
        webbplatsen. Dessa kända problem med tillgängligheten beskrivs nedan. Om
        du stöter på ett problem som inte finns med på listan, vänligen kontakta
        oss.
      </P>
      <ul>
        <li>
          Tjänstens datum- och flervalsfält är inte optimerade för att användas
          med skärmläsare
        </li>
        <li>
          Det går inte att navigera på serviceenhetskartan med
          tangentbordet/skärmläsaren, men man kan bläddra bland enheterna på
          listan i samma vy. Kartan som används i tjänsten är framtagen av en
          tredje part.
        </li>
      </ul>
      <H2>Tredje parter</H2>
      <P>
        Webbtjänsten utnyttjar följande tjänster från en tredje part vars
        tillgänglighet vi inte ansvarar för.
      </P>
      <ul>
        <li>Suomi.fi-identifikation</li>
        <li>Leaflet-karttjänsten</li>
      </ul>
      <H2>Alternativa användningssätt</H2>
      <P>
        <ExternalLink
          href="https://www.turku.fi/varhaiskasvatus-ja-esiopetus"
          text="Åbo stads servicepunkter"
        />{' '}
        hjälper till med att använda e-tjänsterna. Kundrådgivarna vid
        servicepunkterna hjälper användare för vilka de digitala tjänsterna inte
        är tillgängliga.
      </P>
      <H2>Ge feedback</H2>
      <P>
        Om du märker brister i webbtjänstens tillgänglighet, vänligen berätta om
        det för oss. Du kan lämna feedback via{' '}
        <ExternalLink
          href="https://opaskartta.turku.fi/efeedback"
          text="webbformuläret"
        />{' '}
        eller per e-post{' '}
        <a href="mailto:varhaiskasvatus@turku.fi">varhaiskasvatus@turku.fi</a>.
      </P>
      <H2>Tillsynsmyndighet</H2>
      <P>
        Om du märker brister med webbplatsens tillgänglighet, vänligen kontakta
        administratörerna först. Det kan ta upp till 14 dagar innan du får ett
        svar. Om du inte är nöjd med svaret eller om du inte får ett svar inom
        två veckor kan du skicka feedback till Transport- och
        kommunikationsverket Traficom. På webbplatsen för Transport- och
        kommunikationsverket Traficom förklaras hur man kan lämna in ett
        klagomål och hur ärendet handläggs.
      </P>

      <P>
        <strong>Kontaktuppgifter till tillsynsmyndigheten </strong>
        <br />
        Transport- och kommunikationsverket Traficom <br />
        Enheten för tillsyn över digital tillgänglighet
        <br />
        <ExternalLink
          href="https://www.saavutettavuusvaatimukset.fi/sv"
          text="Tillgänglighetskrav"
        />
        <br />
        <a href="mailto:tillganglighet@traficom.fi">
          tillganglighet@traficom.fi
        </a>
        <br />
        telefonnummer växeln 029 534 5000
      </P>
    </>
  )
}

export default sv
