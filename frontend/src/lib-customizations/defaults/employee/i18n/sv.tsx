// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { H3, P } from 'lib-components/typography'

import { fi } from './fi'

export const sv = {
  ...fi,
  childInformation: {
    ...fi.childInformation,
    assistanceNeedPreschoolDecision: {
      ...fi.childInformation.assistanceNeedPreschoolDecision,
      pageTitle: 'Beslut om stöd i förskoleundervisningen',
      decisionNumber: 'Beslutsnummer',
      confidential: 'Konfidentiellt',
      lawReference: '24.1 § i offentlighetslagen',
      types: {
        NEW: 'Särskilt stöd börjar',
        CONTINUING: 'Särskilt stöd fortsätter',
        TERMINATED: 'Särskilt stöd upphör'
      },
      decidedAssistance: 'Stöd som avgörs',
      type: 'Status för särskilt stöd',
      validFrom: 'Gäller från och med',
      extendedCompulsoryEducationSection: 'Förlängd läroplikt',
      extendedCompulsoryEducation: 'Ja, barnet har förlängd läroplikt',
      no: 'Nej',
      extendedCompulsoryEducationInfo: 'Mer information om förlängd läroplikt',
      grantedAssistanceSection:
        'Tolknings- och assistenttjänster eller särskilda hjälpmedel som beviljas',
      grantedAssistanceService: 'Barnet beviljas assistenttjänster',
      grantedInterpretationService: 'Barnet beviljas tolkningstjänster',
      grantedAssistiveDevices: 'Barnet beviljas särskilda hjälpmedel',
      grantedNothing: 'Inget val',
      grantedServicesBasis:
        'Motiveringar till de tolknings- och assistenstjänster och hjälpmedel som beviljas',
      selectedUnit: 'Plats för förskoleundervisning',
      primaryGroup: 'Huvudsaklig undervisningsgrupp',
      decisionBasis: 'Motiveringar till beslutet',
      documentBasis: 'Handlingar som beslutet grundar sig på',
      basisDocumentPedagogicalReport: 'Pedagogisk utredning',
      basisDocumentPsychologistStatement: 'Psykologiskt utlåtande',
      basisDocumentDoctorStatement: 'Läkarutlåtande',
      basisDocumentSocialReport: 'Social utredning',
      basisDocumentOtherOrMissing: 'Bilaga saknas, eller annan bilaga, vilken?',
      basisDocumentsInfo: 'Mer information om bilagorna',
      guardianCollaborationSection: 'Samarbete med vårdnadshavarna',
      guardiansHeardOn: 'Datum för hörande av vårdnadshavarna',
      heardGuardians: 'Vårdnadshavare som har hörts och hörandesätt',
      otherRepresentative:
        'Annan laglig företrädare (namn, telefonnummer och hörandesätt)',
      viewOfGuardians: 'Vårdnadshavarnas syn på det föreslagna stödet',
      responsiblePeople: 'Ansvariga personer',
      preparer: 'Beslutets beredare',
      decisionMaker: 'Beslutsfattare',
      employeeTitle: 'Titel',
      phone: 'Telefonnummer',
      legalInstructions: 'Tillämpade bestämmelser',
      legalInstructionsText: 'Lag om grundläggande utbildning 17 §',
      legalInstructionsTextExtendedCompulsoryEducation: 'Läropliktslag 2 §',
      jurisdiction: 'Befogenhet',
      jurisdictionText:
        'Delegointipäätös suomenkielisen varhaiskasvatuksen sekä kasvun ja oppimisen toimialan esikunnan viranhaltijoiden ratkaisuvallasta A osa 3 § 1 kohta',
      appealInstructionsTitle: 'Anvisning om begäran om omprövning',
      appealInstructions: (
        <>
          <P>
            Den som är missnöjd med detta beslut kan begära omprövning
            skriftligt. Ändring i beslutet får inte sökas genom besvär hos
            domstol.
          </P>

          <H3>Rätt att begära omprövning</H3>
          <P>
            Omprövning får begäras av den som beslutet avser eller vars rätt,
            skyldighet eller fördel direkt påverkas av beslutet (part).
          </P>

          <H3>Tidsfrist för omprövningsbegäran</H3>
          <P>
            En begäran om omprövning ska framställas inom 14 dagar från
            delfåendet av beslutet.
          </P>
          <P>
            Begäran om omprövning ska lämnas in till Regionförvaltningsverket i
            Västra och Inre Finland senast under tidsfristens sista dag innan
            regionförvaltningsverket stänger.
          </P>
          <P>
            En part anses ha fått del av beslutet sju dagar efter att brevet
            sändes eller på den mottagningsdag som anges i mottagningsbeviset
            eller delgivningsbeviset, om inte något annat visas.
          </P>
          <P>
            Vid vanlig elektronisk delgivning anses parten ha fått del av
            beslutet den tredje dagen efter att meddelandet sändes.
          </P>
          <P>
            Dagen för delfåendet räknas inte med i tidsfristen för
            omprövningsbegäran. Om den sista dagen för omprövningsbegäran
            infaller på en helgdag, självständighetsdagen, första maj, julafton,
            midsommarafton eller en helgfri lördag, får omprövning begäras den
            första vardagen därefter.
          </P>

          <H3>Omprövningsmyndighet</H3>
          <P>
            Omprövning begärs hos Regionförvaltningsverket i Västra och Inre
            Finland
          </P>
          <P>
            Postadress: PB 5, 13035 AVI
            <br />
            Besöksadress: Verksamhetsstället i Helsingfors, Bangårdsvägen 9,
            00520 Helsingfors
            <br />
            E-post: registratur.vastra@rfv.fi
            <br />
            Telefonväxel: 0295 016 000
            <br />
            Fax: 06 317 4817
            <br />
            Tjänstetid: 8.00–16.15
          </P>

          <H3>Omprövningsbegärans form och innehåll</H3>
          <P>
            Omprövning ska begäras skriftligt. Också elektroniska dokument
            uppfyller kravet på skriftlig form.
          </P>
          <P noMargin>I omprövningsbegäran ska uppges:</P>
          <ul>
            <li>det beslut i vilket omprövning begärs</li>
            <li>hurdan omprövning som yrkas</li>
            <li>på vilka grunder omprövning begärs.</li>
          </ul>
          <P>
            I omprövningsbegäran ska dessutom uppges namn på den som begär
            omprövning, personens hemkommun, postadress och telefonnummer samt
            övrig kontaktinformation som behövs för att ärendet ska kunna
            skötas.
          </P>
          <P>
            Om omprövningsbeslutet får delges som ett elektroniskt meddelande
            ska också e-postadress uppges.
          </P>
          <P>
            Om talan för den som begär omprövning förs av personens lagliga
            företrädare eller ombud eller om någon annan person har upprättat
            omprövningsbegäran, ska även denna persons namn och hemkommun uppges
            i omprövningsbegäran.
          </P>
          <P noMargin>Till omprövningsbegäran ska fogas:</P>
          <ul>
            <li>det beslut som avses, i original eller kopia</li>
            <li>
              ett intyg över vilken dag beslutet har delgetts eller någon annan
              utredning över när tidsfristen för omprövningsbegäran har börjat
            </li>
            <li>
              de handlingar som den som begär omprövning åberopar, om de inte
              redan tidigare har lämnats till myndigheten.
            </li>
          </ul>
        </>
      )
    },
    assistanceNeedDecision: {
      ...fi.childInformation.assistanceNeedDecision,
      pageTitle: 'Beslut om stöd',
      genericPlaceholder: 'Skriv',
      formLanguage: 'Formulärets språk',
      neededTypesOfAssistance: 'Stödformer utgående från barnets behov',
      pedagogicalMotivation: 'Pedagogiska stödformer och motivering',
      pedagogicalMotivationInfo:
        'Beskriv de former av stöd barnet behöver, såsom lösningar relaterade till dagens struktur, dagsrytm och lärmiljö samt pedagogiska och specialpedagogiska lösningar. Förklara kort varför barnet får dessa former av stödåtgärder.',
      structuralMotivation: 'Strukturella stödformer och motivering',
      structuralMotivationInfo:
        'Välj de former av strukturellt stöd barnet behöver. Förklara varför barnet får dessa former av stöd.',
      structuralMotivationOptions: {
        smallerGroup: 'Minskad gruppstorlek',
        specialGroup: 'Specialgrupp',
        smallGroup: 'Smågrupp',
        groupAssistant: 'Assistent för gruppen',
        childAssistant: 'Assistent för barnet',
        additionalStaff: 'Ökad personalresurs i gruppen'
      },
      structuralMotivationPlaceholder:
        'Beskrivning och motivering av de valda strukturella stödformerna',
      careMotivation: 'Vårdinriktade stödformer och motivering',
      careMotivationInfo:
        'Fyll i de stödet som barnet behöver, såsom metoder för att vårda, ta hand om och hjälpa barnet vid behandling av långvariga sjukdomar, medicinering, kost, rörelse och hjälpmedel som relaterar till dessa. Förklara varför barnet får dessa vårdinriktade stödformer.',
      serviceOptions: {
        consultationSpecialEd:
          'Konsultation med speciallärare inom småbarnspedagogik',
        partTimeSpecialEd:
          'Undervisning på deltid av speciallärare inom småbarnspedagogik',
        fullTimeSpecialEd:
          'Undervisning på heltid av speciallärare inom småbarnspedagogik',
        interpretationAndAssistanceServices: 'Tolknings-och assistenttjänster',
        specialAides: 'Hjälpmedel'
      },
      services: 'Stödtjänster och motivering',
      servicesInfo:
        'Välj stödtjänster för barnet här. Förklara varför barnet får dessa stödtjänster',
      servicesPlaceholder: 'Motivering av de valda vårdinriktade stödformer',
      collaborationWithGuardians: 'Samarbete med vårdnadshavare',
      guardiansHeardOn: 'Datum för hörande av vårdnadshavare',
      guardiansHeard: 'Vårdnadshavare som hörts och förfaringssätt vid hörande',
      guardiansHeardInfo:
        'Anteckna hur vårdnadshavaren har konsulterats (t.ex. möte, distanskontakt, skriftligt svar från vårdnadshavaren). Om vårdnadshavaren inte har konsulterats, anteckna här, hur och när hen har kallats för att höras och hur och när barnets plan för småbarnspedagogiken getts till kännedom (till vårdnadshavaren). Alla barnets vårdnadshavare bör ha möjlighet att bli hörda. Vårdnadshavaren kan vid behov ge fullmakt åt en annan förmyndare att företräda sig själv.',
      viewOfTheGuardians: 'Vårdnadshavarnas syn på det rekommenderade stödet',
      viewOfTheGuardiansInfo:
        'Anteckna vårdnadshavarnas syn på det stöd som erbjuds barnet.',
      otherLegalRepresentation:
        'Annan laglig företrädare (namn, telefonnummer och förfaringssätt vid hörande)',
      decisionAndValidity:
        'Beslut om stödnivån och när beslutet träder i kraft',
      futureLevelOfAssistance: 'Barnets stödnivå framöver',
      assistanceLevel: {
        assistanceEnds: 'Särskilda/intensifierade stödet avslutas',
        assistanceServicesForTime: 'Stödtjänster under beslutets giltighetstid',
        enhancedAssistance: 'Intensifierat stöd',
        specialAssistance: 'Särskilt stöd'
      },
      startDate: 'Stödet är i kraft fr.o.m.',
      startDateIndefiniteInfo:
        'Beslutet träder i kraft från angivet startdatum.',
      startDateInfo:
        'Barnets stöd ses över närhelst behovet ändras och minst en gång per år.',
      endDate: 'Beslutet i kraft till',
      endDateServices: 'Beslutet angående stödtjänster i kraft till',
      selectedUnit: 'Enheten där stödet ges',
      unitMayChange: 'Enheten och stödformer kan ändras under semestertider',
      motivationForDecision: 'Motivering av beslut',
      legalInstructions: 'Tillämpade bestämmelser',
      legalInstructionsText: 'Lag om småbarnspedagogik, 3 a kap 15 §',
      jurisdiction: 'Befogenhet',
      jurisdictionText: (): React.ReactNode =>
        'Beslutanderätt i enlighet med lagstiftningen som gäller småbarnspedagogik och utbildning för tjänstemän inom Esbo stads resultatenhet svenska bildningstjänster och staben för sektorn Del A 7 § punkt 10 för beslut om särskilt stöd gäller Del A 3 § punkt 20 och Del A 3 § punkt 21',
      personsResponsible: 'Ansvarspersoner',
      preparator: 'Beredare av beslutet',
      decisionMaker: 'Beslutsfattare',
      title: 'Titel',
      tel: 'Telefonnummer',
      disclaimer:
        'Ett beslut som fattats i enlighet med lagen om småbarnspedagogik 15 § kan förverkligas även om någon sökt ändring av beslutet.',
      decisionNumber: 'Beslutsnummer',
      statuses: {
        DRAFT: 'Utkast',
        NEEDS_WORK: 'Bör korrigeras',
        ACCEPTED: 'Godkänt',
        REJECTED: 'Avvisat',
        ANNULLED: 'Annullerat'
      },
      confidential: 'Konfidentiellt',
      lawReference: 'Lagen om småbarnspedagogik 40 §',
      noRecord: 'Ingen anmärkning',
      leavePage: 'Stäng',
      modifyDecision: 'Redigera',
      sendToDecisionMaker: 'Skicka till beslutsfattaren',
      sentToDecisionMaker: 'Skickat till beslutsfattaren',
      appealInstructionsTitle: 'Anvisningar för begäran om omprövning',
      appealInstructions: (
        <>
          <P>
            En part som är missnöjd med beslutet kan göra en skriftlig begäran
            om omprövning.
          </P>
          <H3>Rätt att begära omprövning</H3>
          <P>
            En begäran om omprövning får göras av den som beslutet avser, eller
            vars rätt, skyldigheter eller fördel direkt påverkas av beslutet.
          </P>
          <H3>Myndighet hos vilken omprövningen begärs</H3>
          <P>
            Begäran om omprövning görs hos Regionförvaltningsverket i Västra och
            Inre Finland (huvudkontoret i Vasa).
          </P>
          <P>
            Regionförvaltningsverket i Västra och Inre Finlands huvudkontor
            <br />
            Besöksadress: Bangårdsvägen 9, 00520 Helsingfors
            <br />
            Öppet: mån–fre kl. 8.00–16.15
            <br />
            Postadress: PB 5, 13035 AVI
            <br />
            E-post: registratur.vastra@rfv.fi
            <br />
            Fax 06-317 4817
            <br />
            Telefonväxel 0295 018 450
          </P>
          <H3>Tidsfrist för begäran om omprövning</H3>
          <P>
            En begäran om omprövning ska lämnas in inom 30 dagar efter
            delgivningen av beslutet.
          </P>
          <H3>Delgivning av beslut</H3>
          <P>
            Om inte något annat visas, anses en part ha fått del av beslutet sju
            dagar från det att det postades, tre dagar efter att det skickades
            elektroniskt, enligt tiden som anges i mottagningsbeviset eller
            enligt tidpunkten som anges i delgivningsbeviset. Delgivningsdagen
            räknas inte med i beräkningen av tidsfristen. Om den utsatta dagen
            för begäran om omprövning är en helgdag, självständighetsdag, första
            maj, julafton, midsommarafton eller lördag, är det möjligt att göra
            begäran om omprövning ännu under följande vardag.
          </P>
          <H3>Begäran om omprövning</H3>
          <P noMargin>
            Begäran om omprövning ska innehålla följande uppgifter:
          </P>
          <ul>
            <li>
              Namnet på den som begär omprövning och personens hemkommun,
              postadress och telefonnummer
            </li>
            <li>Vilket beslut som omprövas</li>
            <li>
              Vilka delar av beslutet som ska omprövas och vilken ändring som
              söks
            </li>
            <li>På vilka grunder omprövningen begärs</li>
          </ul>
          <P noMargin>
            Till begäran om omprövning bifogas följande handlingar:
          </P>
          <ul>
            <li>
              beslutet som begäran om omprövning gäller, som original eller
              kopia
            </li>
            <li>
              en redogörelse för när den som begär omprövning har tagit del av
              beslutet, eller annan redogörelse för när tidsfristen för begäran
              om omprövning har börjat
            </li>
            <li>
              handlingar som begäran om omprövning stöder sig på, ifall dessa
              inte tidigare skickats till myndigheten.
            </li>
          </ul>
          <P>
            Ett ombud ska bifoga en skriftlig fullmakt till begäran om
            omprövning, så som det föreskrivs i § 32 i lagen om rättegång i
            förvaltningsärenden (808/2019).
          </P>
          <H3>Att sända begäran om omprövning</H3>
          <P>
            En skriftlig begäran om omprövning ska inom tidsfristen sändas till
            myndigheten hos vilken omprövningen begärs. En begäran om omprövning
            måste finnas hos myndigheten senast den sista dagen för sökande av
            ändring, före öppethållningstidens slut. Omprövningsbegäran sänds
            per post eller elektroniskt på avsändarens ansvar.
          </P>
        </>
      )
    }
  }
}
