// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Translations } from '.'

const sv: Translations = {
  common: {
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
      }
    }
  },
  header: {
    nav: {
      map: 'Karta',
      applications: 'Ansökningar',
      decisions: 'Beslut',
      newMap: 'Ny Karta',
      newDecisions: 'Ny Beslut',
      newApplications: 'Ny Ansökningar'
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
      applicationInfo:
        'Du kan ändra i ansökan så länge den inte har tagits till behandling. Därefter kan du göra ändringar i ansökan genom att kontakta småbarnspedagogikens servicehänvisning (tfn 09 8163 27600). Du kan återta en ansökan som du redan lämnat in genom att meddela detta per e-post till småbarnspedagogikens servicehänvisning <a href="mailto:dagis@esbo.fi">dagis@esbo.fi</a>.',
      create: 'Ny ansökan'
    },
    editor: {
      heading: {
        title: {
          DAYCARE: 'Ansökan till småbarnspedagogik',
          PRESCHOOL: 'Anmälan till förskolan',
          CLUB: 'Ansökan till klubbverksamhet'
        },
        info: {
          DAYCARE: [
            'Du kan ansöka om plats i småbarnspedagogisk verksamhet året om. Ansökningen bör lämnas in senast fyra månader före behovet av verksamheten börjar. Om behovet börjar med kortare varsel bör du ansöka om plats senast två veckor före.',
            'Du får ett skriftligt beslut om platsen. Beslutet delges i tjänsten <a href="https://www.suomi.fi/meddelanden" target="_blank" rel="noreferrer">Suomi.fi</a>-meddelanden, eller per post om du inte tagit i bruk meddelandetjänsten i Suomi.fi.',
            '* Informationen markerad med en stjärna krävs'
          ],
          PRESCHOOL: [
            'Barn deltar i förskoleundervisning året innan läroplikten börjar. Förskoleundervisningen är avgiftsfri. Anmälningstiden till förskoleundervisningen 2021–2022 är 8.–20.1.2021. Förskolan börjar i <strong>11.8.2021</strong>. Beslutet delges inom mars i <a href="https://www.suomi.fi/meddelanden" target="_blank" rel="noreferrer">Suomi.fi-meddelandetjänsten</a> eller per post om du inte tagit i bruk <a href="https://www.suomi.fi/meddelanden" target="_blank" rel="noreferrer">meddelandetjänsten i Suomi.fi</a>.',
            'Du får ett skriftligt beslut om platsen. Beslutet delges i tjänsten <a href="https://www.suomi.fi/meddelanden" target="_blank" rel="noreferrer">Suomi.fi</a>-meddelanden, eller per post om du inte tagit i bruk meddelandetjänsten i Suomi.fi.',
            '* Informationen markerad med en stjärna krävs'
          ],
          CLUB: [
            'Hakuaika syksyllä käynnistyviin kerhoihin on maaliskuussa. Jos lapsenne saa kerhopaikan, saatte päätöksen siitä huhti-toukokuun aikana. Päätös tehdään yhden toimintakauden ajaksi (elokuusta toukokuun loppuun). Päätös kerhopaikasta tulee Suomi.fi-palveluun tai postitse, mikäli ette ole ottanut palvelua käyttöön.',
            'Kerhohakemuksen voi jättää myös hakuajan ulkopuolella ja sen jälkeen, kun kerhojen toimintakausi on jo alkanut. Hakuaikana saapuneet hakemukset käsitellään kuitenkin ensin, ja hakuajan ulkopuolella tulleet hakemukset käsitellään saapumisjärjestyksessä. Kerhohakemus kohdistuu yhdelle kerhokaudelle. Kauden päättyessä hakemus poistetaan järjestelmästä.',
            'Kerhotoiminta on maksutonta, eikä siihen osallistuminen vaikuta Kelan maksamaan kotihoidontukeen. Jos lapselle sen sijaan on myönnetty varhaiskasvatuspaikka tai yksityisen hoidon tuki, ei hänelle voida myöntää kerhopaikkaa.',
            '* Informationen markerad med en stjärna krävs'
          ]
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
            'Var god och granska följande obligatoriska fält i blanketten'
        },
        notYetSent:
          '<strong>Du har inte ännu skickat ansökan.</strong> Granska informationen du gett, och skicka ansökan med Skicka ansökan-knappen när du är färdig',
        no: 'Nej',
        basics: {
          created: 'Skapad',
          modified: 'Uppdaterad'
        },
        serviceNeed: {
          title: 'Palveluntarve',
          attachments: {
            label: 'Tarvittavat liitteet',
            withoutAttachments: 'Ei liitetty, lähetetään postilla'
          },
          startDate: {
            title: 'Varhaiskasvatuksen aloitus',
            preferredStartDate: 'Toivottu aloituspäivä',
            urgency: 'Hakemus on kiireellinen',
            withUrgency: 'Kyllä',
            withoutUrgency: 'Ei'
          },
          dailyTime: {
            title: 'Päivittäinen varhaiskasvatusaika',
            partTime: 'Osa- tai kokopäiväinen',
            withPartTime: 'Osa-aikainen',
            withoutPartTime: 'Kokoaikainen',
            dailyTime: 'Varhaiskasvatuksen alkamis- ja päättymisaika',
            shiftCare: 'Ilta- ja vuorohoito',
            withShiftCare: 'Tarvitaan ilta- tai vuorohoitoa',
            withoutShiftCare: 'Ei tarvita ilta- tai vuorohoitoa'
          },
          assistanceNeed: {
            title: 'Tuen tarve',
            assistanceNeed: 'Lapsella on tuen tarve',
            withAssistanceNeed: 'Lapsella on tuen tarve',
            withoutAssistanceNeed: 'Lapsella ei ole tuen tarvetta',
            description: 'Tuen tarpeen kuvaus'
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
          title: 'Henkilötiedot',
          child: {
            title: 'Lapsen tiedot',
            name: 'Lapsen nimi',
            ssn: 'Lapsen henkilötunnus',
            streetAddress: 'Kotiosoite',
            hasFutureAddress: 'Osoite muuttunut / muuttumassa',
            addressChangesAt: 'Muuttopäivämäärä',
            newAddress: 'Uusi osoite'
          },
          guardian: {
            title: 'Huoltajan tiedot',
            name: 'Huoltajan nimi',
            ssn: 'Huoltajan henkilötunnus',
            streetAddress: 'Kotiosoite',
            tel: 'Puhelinnumero',
            email: 'Sähköpostiosoite',
            hasFutureAddress: 'Osoite muuttunut / muuttumassa',
            addressChangesAt: 'Muuttopäivämäärä',
            newAddress: 'Uusi osoite'
          },
          secondGuardian: {
            title: 'Toisen huoltajan tiedot',
            info:
              'Toisen huoltajan tiedot haetaan automaattisesti väestötietojärjestelmästä.'
          },
          fridgePartner: {
            title:
              'Samassa taloudessa asuva avio- tai avopuoliso (ei huoltaja)',
            fridgePartner:
              'Samassa taloudessa asuu avio- tai avopuoliso (ei huoltaja)',
            name: 'Henkilön nimi',
            ssn: 'Henkilön henkilötunnus'
          },
          fridgeChildren: {
            title: 'Samassa taloudessa asuvat alle 18-vuotiaat lapset',
            name: 'Lapsen nimi',
            ssn: 'Henkilön henkilötunnus'
          }
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
          label: 'Önskat inledningsdatum',
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
          message: {
            title: 'Ansökningen är brådskande',
            text:
              'Om behovet av småbarnspedagogisk verksamhet beror på att du har blivit sysselsatt eller att du har fått en studieplats på kort varsel, bör du ansöka om plats senast två veckor före behovet börjar. Behandlingstiden börjar från den dag då du lämnat in ett intyg från arbets- eller studieplatsen till barninvalskoordinatorerna (dagis@esbo.fi).'
          },
          attachmentsMessage: {
            text:
              'Om behovet av en plats inom småbarnspedagogiken beror på att du plötsligt fått sysselsättning eller börjat studera, ska platsen sökas senast två veckor innan behovet börjar. Bifoga till ansökan ett arbets- eller studieintyg av båda vårdnadshavarna som bor i samma hushåll. Om du inte kan lägga till bilagor till ansökan elektroniskt, skicka dem per post till adressen Småbarnspedagogikens servicehänvisning, PB 3125, 02070 Esbo stad. Behandlingstiden på två veckor börjar när vi har tagit emot ansökan och bilagorna som behövs.',
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
          connectedDaycareInfo: [
            'Du kan vid behov ansöka om avgiftsbelagd småbarnspedagogik i samband med förskoleundervisningen. Småbarnspedagogik erbjuds utöver förskoleundervisningen (fyra timmar per dag) på morgnar och eftermiddagar på samma ställe som förskoleundervisningen. Om barnet börjar senare, meddela detta under ”Tilläggsuppgifter”.',
            'Privata förskolor och några av inköpsserviceförskolorna måste göra en separat ansökan om relaterad förskoleundervisning. Espoos förskoleundervisning är i kontakt med de sökande som berörs.',
            'Du får ett separat skriftligt beslut om platsen inom småbarnspedagogik. Beslutet delges i tjänsten Suomi.fi-meddelanden eller per post, om du inte har tagit tjänsten i bruk.'
          ],
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
              'Kvälls- och skiftomsorg är avsedd för barn vars båda föräldrar har skiftarbete eller studerar huvudsakligen på kvällar och/eller veckoslut. Som bilaga till ansökan ska av båda föräldrarna lämnas ett intyg av arbetsgivaren över skiftarbete eller studier som orsakar behovet av kvälls- eller skiftomsorg. Vi rekommenderar att bilagan skickas elektroniskt här, eftersom behandlingstiden på två veckor börjar när vi har tagit emot ansökan med de bilagor som behövs. Om du inte kan lägga till bilagor till ansökan elektroniskt, skicka dem per post till adressen Småbarnspedagogikens servicehänvisning, PB 3125, 02070 Esbo stad.',
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
          p1:
            'Målet är att placera syskon i samma enhet om inte familjen önskar annat. Som syskon betraktas barn som är folkbokförda på samma adress. Om du ansöker om en plats för syskon, som inte ännu har plats inom småbarnspedagogik, skriv uppgiften i tilläggsuppgifter.',
          p2:
            'Fyll i dessa uppgifter endast om du vill hänvisa till barnets syskonrelationer.',
          checkbox:
            'Jag ansöker i första hand om plats i den enheten där barnets syskon redan har en plats.',
          radioLabel:
            'Välj ett syskon som du söker till samma förskoleutbildningsplats',
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
          p1:
            'Du kan ange 1-3 platser i önskad ordning. Önskemålen garanterar inte en plats i den önskade enheten, men möjligheterna att få en önskad plats ökar om du anger flera alternativ.',
          p2:
            'Om du i det tidigare fältet har hänvisat till barnets syskonrelationer ska du som förstahandsönskemål ange den enhet där syskonet redan har en plats.',
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
        title: 'Personuppgifter',
        info:
          'Personuppgifterna hämtas från befolkningsdatabasen och du kan inte ändra dem med den här ansökan. Om det finns fel i personuppgifterna, vänligen uppdatera uppgifterna på webbplatsen <a href="https://dvv.fi/sv/kontroll-av-egna-uppgifter-service" target="_blank" rel="noopener noreferrer">dvv.fi</a> (Myndigheten för digitalisering och befolkningsdata). Ifall adressen kommer att ändras, kan du lägga till den nya adressen på ett separat ställe i ansökan. Fyll i den nya adressen både för vårdnadshavare och barnet. Adressuppgifterna är officiella först när de har uppdaterats av myndigheten för digitalisering och befolkningsdata. Beslutet om barnets plats inom småbarnspedagogiken eller förskoleundervisningen skickas automatiskt också till en vårdnadshavare som bor på en annan adress enligt befolkningsregistret.',
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
    modalConfirm: 'OK'
  },
  validationErrors: {
    required: 'Värde saknas',
    requiredSelection: 'Ingenting är valt',
    format: 'Ange rätt format',
    ssn: 'Ogiltigt personbeteckning',
    phone: 'Ogiltigt telefonnummer',
    email: 'Ogiltig e-postadress',
    validDate: 'Ange formuläret pp.kk.vvvv',
    timeFormat: 'Ange formuläret hh:mm',
    unitNotSelected: 'Välj minst en sökbegäran'
  }
}

export default sv
