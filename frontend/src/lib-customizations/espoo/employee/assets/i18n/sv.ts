// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

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
      appealInstructionsTitle: 'Instruktioner för yrkande om korrigering'
    }
  }
}
