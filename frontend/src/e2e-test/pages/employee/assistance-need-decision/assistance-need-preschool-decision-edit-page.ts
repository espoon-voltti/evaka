// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, TextInput } from '../../../utils/page'

export default class AssistanceNeedPreschoolDecisionEditPage {
  constructor(private readonly page: Page) {}

  readonly autoSaveIndicator = this.page.findByDataQa('autosave-indicator')
  readonly status = this.page.findByDataQa('status')
  readonly decisionNumber = this.page.findByDataQa('decision-number')
  readonly primaryGroupInput = new TextInput(
    this.page.findByDataQa('primary-group')
  )
}
