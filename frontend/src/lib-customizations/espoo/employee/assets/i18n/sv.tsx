// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { H2, P } from 'lib-components/typography'

import { fi } from './fi'

export const sv = {
  ...fi,
  childInformation: {
    ...fi.childInformation,
    assistanceNeedDecision: {
      ...fi.childInformation.assistanceNeedDecision,
      pageTitle: 'Beslut om stödbehov',
      genericPlaceholder: 'Skriv',
      formLanguage: 'Formulärets språk',
      neededTypesOfAssistance: 'Former av stöd enligt barnets behov',
      pedagogicalMotivation: 'Former och motiveringar för pedagogiskt stöd',
      pedagogicalMotivationInfo:
        'Anteckna en presentation av de former av pedagogiskt stöd barnet behöver, såsom lösningar relaterade till dagens struktur, dagsrytm och lärmiljö samt pedagogiska och specialpedagogiska lösningar. Förklara kort varför barnet får dessa former av stöd.',
      structuralMotivation: 'Strukturella former av stöd och motivering',
      structuralMotivationInfo:
        'Välj de former av strukturellt stöd barnet behöver. Förklara varför barnet får dessa former av stöd.',
      structuralMotivationOptions: {
        smallerGroup: 'Minskad gruppstorlek',
        specialGroup: 'Specialgrupp',
        smallGroup: 'Liten grupp',
        groupAssistant: 'Gruppassistent',
        childAssistant: 'Personlig assistent för barnet',
        additionalStaff: 'Ökning av mänskliga resurser'
      },
      structuralMotivationPlaceholder:
        'Beskrivning av och motivering för de valda formerna av strukturellt stöd',
      careMotivation: 'Former och motivering för vårdnadsstöd',
      careMotivationInfo:
        'Anteckna de vårdformer som barnet behöver, såsom metoder för att vårda, ta hand om och hjälpa barnet vid behandling av långvariga sjukdomar, medicinering, kost, träning och relaterade hjälpmedel. Förklara varför barnet får dessa former av stöd.',
      serviceOptions: {
        consultationSpecialEd:
          'Konsultation av speciallärare i smågbarnspedagogik',
        partTimeSpecialEd:
          'Deltidsundervisning av speciallärare i småbarnspedagogik',
        fullTimeSpecialEd:
          'Heltidsundervisning av speciallärare i småbarnspedagogik',
        interpretationAndAssistanceServices: 'Tolk- och assistanstjänster',
        specialAides: 'Hjälpmedel'
      },
      services: 'Stödtjänster och motivering',
      servicesInfo:
        'Välj stödtjänster för barnet här. Förklara varför barnet får dessa stödtjänster',
      servicesPlaceholder: 'Motivering för valda stödtjänster',
      collaborationWithGuardians: 'Samarbete med vårdnadshavarna',
      guardiansHeardAt: 'Datum för samråd med vårdnadshavarna',
      guardiansHeard:
        'Vårdnadshavare vilka blivit konsulterade, samt metoden som använts',
      guardiansHeardInfo:
        'Anteckna hur vårdnadshavaren har konsulterats (t.ex. möte, distanskontakt, skriftligt svar från vårdnadshavaren). Om vårdnadshavaren inte har konsulterats, anteckna här hur och när han eller hon har kallats för att höras och hur och när barnets plan för småbarnspedagogiken har kommunicerats till vårdnadshavaren. Alla barnets vårdnadshavare bör ha möjlighet att bli hörda. Vårdnadshavaren kan vid behov ge fullmakt att företräda sig själv åt en annan förmyndare.',
      viewOfTheGuardians: 'Vårdnadshavarnas syn på stödet som erbjuds',
      viewOfTheGuardiansInfo:
        'Anteckna vårdnadshavarnas syn på det stöd som erbjuds barnet.',
      otherLegalRepresentation:
        'Annat juridiskt ombud (namn, telefonnummer och metod för konsultation)',
      decisionAndValidity: 'Beslut om stödets nivå samt giltighet',
      futureLevelOfAssistance: 'Nivån för barnets stöd i fortsättningen',
      assistanceLevel: {
        assistanceEnds: 'Stödet upphör',
        assistanceServicesForTime: 'Stödtjänster för tiden',
        enhancedAssistance: 'Intensifierat stöd',
        specialAssistance: 'Specialstöd'
      },
      startDate: 'Beslutet i kraft fr.o.m.',
      startDateInfo:
        'Beslutet gäller från angivet startdatum. Barnets stöd ses över närhelst behovet ändras och minst en gång per år.',
      selectedUnit: 'Enhet för småbarnspedagogik utvald för beslutet',
      unitMayChange: 'Under semestern kan platsen eller metoden ändras',
      motivationForDecision: 'Motivering för beslutet',
      personsResponsible: 'Ansvariga',
      preparator: 'Beslutets förberedare',
      decisionMaker: 'Beslutsfattare',
      title: 'Titel',
      disclaimer:
        'Ett beslut enligt 15 e § förskolelagen kan verkställas även om det överklagas.',
      decisionNumber: 'Beslutsnummer',
      statuses: {
        DRAFT: 'Utkast',
        NEEDS_WORK: 'Bör korrigeras',
        ACCEPTED: 'Godkänt',
        REJECTED: 'Avvisat'
      },
      confidential: 'Konfidentiellt',
      lawReference: 'Lagen om småbarnspedagogik 15 §',
      noRecord: 'Ingen anmärkning',
      leavePage: 'Stäng',
      modifyDecision: 'Redigera',
      sendToDecisionMaker: 'Skicka till beslutsfattaren',
      sentToDecisionMaker: 'Skickat till beslutsfattaren',
      appealInstructionsTitle: 'Anvisningar för begäran om omprövning',
      appealInstructions: (
        <>
          <P>
            En part som missnöjd med beslutet kan göra en skriftlig begäran om
            omprövning.
          </P>
          <H2>Rätt att begära omprövning</H2>
          <P>
            En begäran om omprövning får göras av den som beslutet avser, eller
            vars rätt, skyldigheter eller fördel direkt påverkas av beslutet.
          </P>
          <H2>Myndighet hos vilken omprövningen begärs</H2>
          <P>
            Begäran om omprövning görs hos Regionförvaltningsverket i Västra och
            Inre Finland (huvudkontoret i Vasa).
          </P>
          <P>
            Regionförvaltningsverket i Västra och Inre Finlands huvudkontor
            <br />
            Besöksadress: Wolffskavägen 35, Vasa
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
          <H2>Tidsfrist för begäran om omprövning</H2>
          <P>
            En begäran om omprövning ska lämnas in inom 30 dagar efter
            delgivningen av beslutet.
          </P>
          <H2>Delgivning av beslut</H2>
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
          <H2>Begäran om omprövning</H2>
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
              Vilka delar av beslutet som du anser ska omprövas och vilken
              ändring som söks
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
          <H2>Att sända begäran om omprövning</H2>
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
