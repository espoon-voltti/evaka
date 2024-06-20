// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, Element } from '../../../utils/page'

export default class AssistanceNeedDecisionPreviewPage {
  sendDecisionButton: Element
  revertToUnsent: Element
  constructor(private readonly page: Page) {
    this.sendDecisionButton = page.findByDataQa('send-decision')
    this.revertToUnsent = page.findByDataQa('revert-to-unsent')
  }

  private getLabelledValue(label: string) {
    return this.page.findByDataQa(`labelled-value-${label}`).text
  }

  get pedagogicalMotivation() {
    return this.getLabelledValue('pedagogical-motivation')
  }

  async assertStructuralMotivationOption(opt: string) {
    await this.page
      .findByDataQa('structural-motivation-section')
      .findByDataQa(`list-option-${opt}`)
      .waitUntilVisible()
  }

  get structuralMotivationDescription() {
    return this.page.findByDataQa('structural-motivation-description').text
  }

  get careMotivation() {
    return this.getLabelledValue('care-motivation')
  }

  async assertServiceOption(opt: string) {
    await this.page
      .findByDataQa('services-section')
      .findByDataQa(`list-option-${opt}`)
      .waitUntilVisible()
  }

  get guardiansHeardOn() {
    return this.getLabelledValue('guardians-heard-at')
  }

  async heardGuardian(id: string) {
    return this.page
      .findByDataQa('guardians-heard-section')
      .findByDataQa(`guardian-${id}`).text
  }

  get otherRepresentativeDetails() {
    return this.page.findByDataQa('other-representative-details').text
  }

  get viewOfGuardians() {
    return this.getLabelledValue('view-of-the-guardians')
  }

  get futureLevelOfAssistance() {
    return this.getLabelledValue('future-level-of-assistance')
  }

  get startDate() {
    return this.getLabelledValue('start-date')
  }

  get selectedUnit() {
    return this.getLabelledValue('selected-unit')
  }

  get motivationForDecision() {
    return this.getLabelledValue('motivation-for-decision')
  }

  get preparedBy1() {
    return this.getLabelledValue('prepared-by-1')
  }

  get decisionMaker() {
    return this.getLabelledValue('decision-maker')
  }

  get decisionSentAt() {
    return this.page.findByDataQa('decision-sent-at')
  }

  async assertPageTitle(title: string): Promise<void> {
    await this.page.findByDataQa('page-title').assertTextEquals(title)
  }
}
