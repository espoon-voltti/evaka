// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, Element } from '../../../utils/page'

export default class AssistanceNeedPreschoolDecisionPreviewPage {
  guardiansHeardOn: Element
  validFrom: Element
  selectedUnit: Element
  preparedBy1: Element
  decisionMaker: Element
  sendDecisionButton: Element
  constructor(private readonly page: Page) {
    this.guardiansHeardOn = page.findByDataQa(`guardians-heard-on`)
    this.validFrom = page.findByDataQa('valid-from')
    this.selectedUnit = page.findByDataQa('unit')
    this.preparedBy1 = page.findByDataQa('preparer-1')
    this.decisionMaker = page.findByDataQa('decision-maker')
    this.sendDecisionButton = page.findByDataQa('send-decision')
  }
}
