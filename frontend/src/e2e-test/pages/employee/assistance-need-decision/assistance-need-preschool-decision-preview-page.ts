// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from '../../../utils/page'

export default class AssistanceNeedPreschoolDecisionPreviewPage {
  constructor(private readonly page: Page) {}
  guardiansHeardOn = this.page.findByDataQa(`guardians-heard-on`)
  validFrom = this.page.findByDataQa('valid-from')
  selectedUnit = this.page.findByDataQa('unit')
  preparedBy1 = this.page.findByDataQa('preparer-1')
  decisionMaker = this.page.findByDataQa('decision-maker')

  readonly sendDecisionButton = this.page.findByDataQa('send-decision')
}
