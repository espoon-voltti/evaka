// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export default {
  common: {
    cancel: 'Peruuta',
    return: 'Palaa',
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
      map: 'Kartta',
      applications: 'Hakemukset',
      decisions: 'Päätökset',
      newDecisions: 'Uusi Päätökset',
      newApplications: 'Uusi Hakemukset'
    },
    lang: {
      fi: 'Suomeksi',
      sv: 'På svenska',
      en: 'In English'
    },
    logout: 'Kirjaudu ulos'
  },
  footer: {
    espooLabel: '© Espoon kaupunki',
    privacyPolicy: 'Tietosuojaselosteet',
    privacyPolicyLink:
      'https://www.espoo.fi/fi-FI/Asioi_verkossa/Tietosuoja/Tietosuojaselosteet',
    sendFeedback: 'Lähetä palautetta',
    sendFeedbackLink:
      'https://easiointi.espoo.fi/eFeedback/fi/Feedback/20-S%C3%A4hk%C3%B6iset%20asiointipalvelut'
  },
  applications: {
    title: 'Hakemukset',
    creation: {
      title: 'Valitse hakemustyyppi',
      daycareLabel: 'Varhaiskasvatushakemus',
      daycareInfo:
        'Varhaiskasvatushakemuksella haetaan kunnallista varhaiskasvatuspaikkaa päiväkotiin, perhepäivähoitoon tai ryhmäperhepäiväkotiin.',
      preschoolLabel:
        'Ilmoittautuminen esiopetukseen ja / tai valmistavaan opetukseen',
      preschoolInfo:
        'Maksutonta esiopetusta on neljä tuntia päivässä. Tämän lisäksi lapselle voidaan hakea maksullista liittyvää varhaiskasvatusta, jota tarjotaan esiopetuspaikoissa aamulla ennen esiopetuksen alkua ja iltapäivisin esiopetuksen jälkeen. Hakemuksen liittyvään varhaiskasvatukseen voi tehdä esiopetukseen ilmoittautumisen yhteydessä tai erillisenä hakemuksena opetuksen jo alettua. Samalla hakemuksella voit hakea myös maksuttomaan valmistavaan opetukseen sekä valmistavaan opetukseen liittyvään varhaiskasvatukseen.',
      preschoolDaycareInfo:
        'Liittyvän varhaiskasvatuksen hakeminen lapsille, jotka ilmoitetaan / on ilmoitettu esiopetukseen tai valmistavaan opetukseen',
      clubLabel: 'Kerhohakemus',
      clubInfo: 'Kerhohakemuksella haetaan kunnallisiin kerhoihin.',
      duplicateWarning:
        'Lapsella on jo samantyyppinen, keskeneräinen hakemus. Palaa Hakemukset-näkymään ja muokkaa olemassa olevaa hakemusta tai ota yhteyttä palveluohjaukseen.',
      applicationInfo:
        'Hakemukseen voi tehdä muutoksia niin kauan kuin hakemusta ei ole otettu käsittelyyn. Tämän jälkeen muutokset hakemukseen tehdään ottamalla yhteyttä varhaiskasvatuksen palveluohjaukseen (puh. 09 816 31000). Voit perua jo tehdyn hakemuksen ilmoittamalla siitä sähköpostilla varhaiskasvatuksen palveluohjaukseen <a href="mailto:varhaiskasvatuksen.palveluohjaus@espoo.fi">varhaiskasvatuksen.palveluohjaus@espoo.fi</a>.',
      create: 'Tee hakemus'
    },
    editor: {
      heading: {
        title: {
          DAYCARE: 'Varhaiskasvatushakemus'
        },
        info: {
          DAYCARE: [
            'Varhaiskasvatusta voi hakea ympäri vuoden. Hakemus on jätettävä viimeistään neljä kuukautta ennen kuin tarvitsette paikan. Mikäli tarvitsette varhaiskasvatusta kiireellisesti työn tai opiskelujen vuoksi, tulee paikkaa hakea viimeistään kaksi viikkoa ennen.',
            'Saatte kirjallisen päätöksen varhaiskasvatuspaikasta <a href="https://www.suomi.fi/viestit" target="_blank" rel="noreferrer">Suomi.fi-viestit</a> -palveluun tai postitse, mikäli et ole ottanut Suomi.fi -palvelua käyttöön.',
            '* Tähdellä merkityt tiedot ovat pakollisia'
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
        title: 'Varhaiskasvatusmaksu',
        info:
          'Kunnallisen varhaiskasvatuksen asiakasmaksut määräytyvät prosenttiosuutena perheen bruttotuloista. Maksut vaihtelevat perheen koon ja tulojen sekä varhaiskasvatusajan mukaan maksuttomasta varhaiskasvatuksesta enintään 288 euron kuukausimaksuun lasta kohden. Perhe toimittaa tuloselvityksen bruttotuloistaan tuloselvityslomakkeella, viimeistään kahden viikon kuluessa siitä, kun lapsi on aloittanut varhaiskasvatuksessa.',
        emphasis:
          '<strong>Tuloselvitystä ei tarvita, jos perhe suostuu korkeimpaan maksuun.</strong>',
        checkbox:
          'Annan suostumuksen korkeimpaan maksuun. Suostumus on voimassa toistaiseksi, kunnes toisin ilmoitan.',
        links:
          'Lisätietoa varhaiskasvatuksen maksuista ja tuloselvityslomakeen löydät Espoon kaupungin sivuilta:<br/><a href="https://www.espoo.fi/fi-FI/Kasvatus_ja_opetus/Varhaiskasvatus/Maksut_varhaiskasvatuksessa" target="_blank" rel="noopener noreferrer">Maksut varhaiskasvatuksessa</a>'
      },
      additionalDetails: {
        title: 'Muut lisätiedot',
        otherInfoLabel: 'Hakuun liittyvät lisätiedot',
        otherInfoPlaceholder:
          'Voit halutessasi antaa hakuun liittyvää tarkempaa lisätietoa',
        dietLabel: 'Erityisruokavalio',
        dietPlaceholder: 'Voit halutessasi ilmoittaa lapsen erityisruokavalion',
        dietInfo:
          'Osaan erityisruokavalioista tarvitaan erikseen lääkärintodistus, joka toimitetaan varhaiskasvatuspaikkaan. Poikkeuksena vähälaktoosinen tai laktoositon ruokavalio, uskonnollisiin syihin perustuva ruokavalio tai kasvisruokavalio (lakto-ovo).',
        allergiesLabel: 'Allergiat',
        allergiesPlaceholder: 'Voit halutessasi ilmoittaa lapsen allergiat',
        allergiesInfo:
          'Allergiatieto tarvitaan lähinnä perhepäivähoitoa haettaessa.'
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
        title: 'Hakemusluonnos on tallennettu',
        text:
          'Hakemus on tallennettu keskeneräisenä. Huom! Keskeneräistä hakemusta säilytetään palvelussa yhden kuukauden ajan viimeisimmästä tallennuksesta.',
        ok: 'Selvä'
      }
    }
  },
  decisions: {
    title: 'Päätökset',
    summary:
      'Tälle sivulle saapuvat lapsen varhaiskasvatus-, esiopetus- ja kerhohakemuksiin liittyvät päätökset. Uuden päätöksen saapuessa <strong>sinun tulee kahden viikon sisällä vastata</strong>, hyväksytkö vai hylkäätkö lapselle tarjotun paikan.',
    unconfirmedDecisions: (n: number) =>
      `${n} ${n === 1 ? 'päätös' : 'päätöstä'} odottaa vahvistustasi`,
    pageLoadError: 'Tietojen hakeminen ei onnistunut',
    applicationDecisions: {
      decision: 'Päätös',
      type: {
        CLUB: 'kerhosta',
        DAYCARE: 'varhaiskasvatuksesta',
        DAYCARE_PART_TIME: 'osa-aikaisesta varhaiskasvatuksesta',
        PRESCHOOL: 'esiopetuksesta',
        PRESCHOOL_DAYCARE: 'liittyvästä varhaiskasvatuksesta',
        PREPARATORY_EDUCATION: 'valmistavasta opetuksesta'
      },
      childName: 'Lapsen nimi',
      unit: 'Toimipaikka',
      period: 'Ajalle',
      sentDate: 'Päätös saapunut',
      resolved: 'Vahvistettu',
      statusLabel: 'Tila',
      summary:
        'Päätöksessä ilmoitettu paikka / ilmoitetut paikat tulee joko hyväksyä tai hylätä välittömästi, viimeistään kahden viikon kuluessa päätöksen saapumisesta.',
      status: {
        PENDING: 'Vahvistettavana huoltajalla',
        ACCEPTED: 'Hyväksytty',
        REJECTED: 'Hylätty'
      },
      confirmationInfo: {
        preschool:
          'Esiopetuksen, valmistavan opetuksen ja/tai liittyvän varhaiskasvatuksen hyväksymis- tai hylkäämisilmoitus on toimitettava välittömästi, viimeistään kahden viikon kuluessa tämän ilmoituksen saamisesta. Jos olet hakenut useampaa palvelua, saat jokaisesta oman päätöksen erikseen vahvistettavaksi',
        default:
          'Päätöksessä ilmoitetun paikan hyväksymis- tai hylkäämisilmoitus on toimitettava välittömästi, viimeistään kahden viikon kuluessa tämän ilmoituksen saamisesta.'
      },
      goToConfirmation:
        'Siirry lukemaan päätös ja vastaamaan hyväksytkö vai hylkäätkö paikan.',
      confirmationLink: 'Siirry vastaamaan',
      response: {
        title: 'Paikan hyväksyminen tai hylkääminen',
        accept1: 'Otamme paikan vastaan',
        accept2: 'alkaen',
        reject: 'Emme ota paikkaa vastaan',
        cancel: 'Palaa takaisin vastaamatta',
        submit: 'Lähetä vastaus päätökseen',
        disabledInfo:
          'HUOM! Pääset hyväksymään/hylkäämään liittyvää varhaiskasvatusta koskevan päätöksen mikäli hyväksyt ensin esiopetusta / valmistavaa opetusta koskevan päätöksen.'
      },
      openPdf: 'Näytä päätös',
      warnings: {
        decisionWithNoResponseWarning: {
          title: 'Toinen päätös odottaa vastaustasi',
          text:
            'Toinen päätös odottaa edelleen vastaustasi. Haluatko  palata listalle vastaamatta?',
          resolveLabel: 'Palaa vastaamatta',
          rejectLabel: 'Jatka vastaamista'
        },
        doubleRejectWarning: {
          title: 'Haluatko hylätä paikan?',
          text:
            'Olet hylkäämässä tarjotun esiopetus / valmistavan paikan. Liittyvän varhaiskasvatuksen paikka merkitään samalla hylätyksi.',
          resolveLabel: 'Hylkää molemmat',
          rejectLabel: 'Palaa takaisin'
        }
      },
      errors: {
        pageLoadError: 'Tietojen hakeminen ei onnistunut',
        submitFailure: 'Päätökseen vastaaminen ei onnistunut'
      },
      returnToPreviousPage: 'Palaa'
    }
  },
  applicationsList: {
    title: 'Hakeminen varhaiskasvatukseen ja ilmoittautuminen esiopetukseen',
    summary:
      'Lapsen huoltaja voi tehdä lapselle hakemuksen varhaiskasvatukseen ja kerhoon tai ilmoittaa lapsen esiopetukseen. Huoltajan lasten tiedot haetaan tähän näkymään automaattisesti Väestötietojärjestelmästä.',
    pageLoadError: 'Tietojen hakeminen ei onnistunut',
    noApplications: 'Ei hakemuksia',
    type: {
      DAYCARE: 'Varhaiskasvatushakemus',
      PRESCHOOL: 'Esiopetushakemus',
      CLUB: 'Kerhohakemus'
    },
    unit: 'Yksikkö',
    period: 'Ajalle',
    created: 'Luotu',
    modified: 'Muokattu',
    status: {
      title: 'Tila',
      CREATED: 'Luonnos',
      SENT: 'Lähetetty',
      WAITING_PLACEMENT: 'Käsiteltävänä',
      WAITING_DECISION: 'Käsiteltävänä',
      WAITING_UNIT_CONFIRMATION: 'Käsiteltävänä',
      WAITING_MAILING: 'Käsiteltävänä',
      WAITING_CONFIRMATION: 'Vahvistettavana huoltajalla',
      REJECTED: 'Paikka hylätty',
      ACTIVE: 'Paikka vastaanotettu',
      CANCELLED: 'Poistettu käsittelystä'
    },
    openApplicationLink: 'Näytä hakemus',
    editApplicationLink: 'Muokkaa hakemusta',
    removeApplicationBtn: 'Poista hakemus',
    confirmationLinkInstructions:
      'Päätökset-välilehdellä voit lukea päätöksen ja hyväksyä/hylätä tarjotun paikan',
    confirmationLink: 'Siirry vahvistamaan',
    newApplicationLink: 'Uusi hakemus'
  },
  fileUpload: {
    loading: 'Ladataan...',
    loaded: 'Ladattu',
    error: {
      fileTooBig: 'Liian suuri tiedosto (max. 10MB)',
      default: 'Lataus ei onnistunut'
    },
    input: {
      title: 'Lisää liite',
      text: [
        'Paina tästä tai raahaa liite laatikkoon yksi kerrallaan.',
        'Tiedoston maksimikoko: 10MB.',
        'Sallitut tiedostomuodot:',
        'PDF, JPEG/JPG, PNG ja DOC/DOCX'
      ]
    },
    modalHeader: 'Tiedoston käsittely on kesken',
    modalMessage:
      'Tiedosto ei ole juuri nyt avattavissa. Kokeile hetken kuluttua uudelleen.',
    modalConfirm: 'Selvä'
  }
}
