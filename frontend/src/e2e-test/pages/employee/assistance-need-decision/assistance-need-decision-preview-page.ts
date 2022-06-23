// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from '../../../utils/page'

export default class AssistanceNeedDecisionPreviewPage {
  constructor(private readonly page: Page) {}

  private getLabelledValue(label: string) {
    return this.page.findByDataQa(`labelled-value-${label}`).innerText
  }

  readonly pedagogicalMotivation = this.getLabelledValue(
    'Pedagogiset tuen muodot ja perustelut'
  )
  readonly assertStructuralMotivationOption = (opt: string) =>
    this.page
      .findByDataQa('structural-motivation-section')
      .findByDataQa(`list-option-${opt}`)
      .waitUntilVisible()
  readonly structuralMotivationDescription = this.page.findByDataQa(
    'structural-motivation-description'
  ).innerText
  readonly careMotivation = this.getLabelledValue(
    'Hoidolliset tuen muodot ja perustelut'
  )
  readonly assertServiceOption = (opt: string) =>
    this.page
      .findByDataQa('services-section')
      .findByDataQa(`list-option-${opt}`)
      .waitUntilVisible()
  readonly guardiansHeardOn = this.getLabelledValue(
    'Huoltajien kuulemisen päivämäärä'
  )
  readonly heardGuardian = (id: string) =>
    this.page
      .findByDataQa('guardians-heard-section')
      .findByDataQa(`guardian-${id}`).innerText
  readonly otherRepresentativeDetails = this.page.findByDataQa(
    'other-representative-details'
  ).innerText
  readonly viewOfGuardians = this.getLabelledValue(
    'Huoltajien näkemys esitetystä tuesta'
  )
  readonly futureLevelOfAssistance = this.getLabelledValue(
    'Lapsen tuen taso jatkossa'
  )
  readonly startDate = this.getLabelledValue('Päätös voimassa alkaen')
  readonly selectedUnit = this.getLabelledValue(
    'Päätökselle valittu varhaiskasvatusyksikkö'
  )
  readonly motivationForDecision = this.getLabelledValue(
    'Perustelut päätökselle'
  )
  readonly preparedBy1 = this.page.findByDataQa('prepared-by-1').innerText
  readonly decisionMaker = this.getLabelledValue('Päättäjä')
}
