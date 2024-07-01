// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, Element } from '../../utils/page'

export default class AssistanceNeedDecisionPage {
  structuralMotivationDescription: Element
  otherRepresentativeDetails: Element
  constructor(private readonly page: Page) {
    this.structuralMotivationDescription = page.findByDataQa(
      'structural-motivation-description'
    )
    this.otherRepresentativeDetails = page.findByDataQa(
      'other-representative-details'
    )
  }

  private getLabelledValue(label: string) {
    return () => this.page.findByDataQa(`labelled-value-${label}`).text
  }

  readonly pedagogicalMotivation = this.getLabelledValue(
    'pedagogical-motivation'
  )
  readonly assertStructuralMotivationOption = (opt: string) =>
    this.page
      .findByDataQa('structural-motivation-section')
      .findByDataQa(`list-option-${opt}`)
      .waitUntilVisible()
  readonly careMotivation = this.getLabelledValue('care-motivation')
  readonly assertServiceOption = (opt: string) =>
    this.page
      .findByDataQa('services-section')
      .findByDataQa(`list-option-${opt}`)
      .waitUntilVisible()
  readonly guardiansHeardOn = this.getLabelledValue('guardians-heard-at')
  readonly viewOfGuardians = this.getLabelledValue('view-of-the-guardians')
  readonly futureLevelOfAssistance = this.getLabelledValue(
    'future-level-of-assistance'
  )
  readonly startDate = this.getLabelledValue('start-date')
  readonly selectedUnit = this.getLabelledValue('selected-unit')
  readonly motivationForDecision = this.getLabelledValue(
    'motivation-for-decision'
  )
  readonly preparedBy1 = this.getLabelledValue('prepared-by-1')
  readonly decisionMaker = this.getLabelledValue('decision-maker')
}
