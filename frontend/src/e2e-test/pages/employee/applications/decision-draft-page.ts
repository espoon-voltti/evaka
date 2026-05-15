// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { DecisionType } from 'lib-common/generated/api-types/decision'
import type { UUID } from 'lib-common/types'

import { expect } from '../../../playwright'
import type { Page } from '../../../utils/page'
import { Checkbox, Combobox } from '../../../utils/page'

export class DecisionDraftPage {
  constructor(private page: Page) {}

  async waitUntilLoaded() {
    await expect(this.page.findByDataQa('save-decisions-button')).toBeVisible()
  }

  async selectUnit(type: DecisionType, unitId: UUID) {
    const selector = new Combobox(this.page.findByDataQa(`unit-${type}`))
    await selector.fillAndSelectItem('', unitId)
  }

  async selectSharedUnit(unitId: UUID) {
    const selector = new Combobox(
      this.page.findByDataQa('header-unit-selector')
    )
    await selector.fillAndSelectItem('', unitId)
  }

  plannedCheckbox(decisionType: DecisionType): Checkbox {
    return new Checkbox(this.page.findByDataQa(`planned-${decisionType}`))
  }

  async save() {
    await this.page.findByDataQa('save-decisions-button').click()
  }

  async cancel() {
    await this.page.findByDataQa('cancel-decisions-button').click()
  }
}
