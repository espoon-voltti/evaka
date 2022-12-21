// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'

import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'

export default class UnitListPage {
  constructor(private readonly page: Page) {}

  async assertStaffCount(unitId: UUID, present: number, allocated: number) {
    const staffCount = this.page
      .findByDataQa(`unit-${unitId}`)
      .findByDataQa('staff-count')
    await waitUntilEqual(() => staffCount.text, `${present}/${allocated}`)
  }
}
