// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector, t } from 'testcafe'
import config from '../../../config'

const url = `${config.employeeUrl}/applications`

const applicationStatus = Selector('[data-qa="application-status"]')

const actionsMenu = (applicationId: string) =>
  Selector(`[data-application-id="${applicationId}"]`).find(
    '[data-qa="application-actions-menu"]'
  )

const allApplications = Selector('[data-qa="application-status-filter-ALL"]')

const actionsMenuItemSelector = (id: string) =>
  Selector(`[data-qa="action-item-${id}"]`)

const actionsMenuItems = {
  verify: actionsMenuItemSelector('verify'),
  setVerified: actionsMenuItemSelector('set-verified'),
  createPlacement: actionsMenuItemSelector('placement-draft'),
  createDecision: actionsMenuItemSelector('decision'),
  acceptPlacementWihtoutDecision: actionsMenuItemSelector(
    'placement-without-decision'
  )
}

const areaFilter = Selector('[data-qa="area-filter"]')
const areaFilterInput = areaFilter.find('input')
const areaFilterFirstOption = areaFilter.find('[id^="react-select-2-option-"')

const toggleArea = async (areaName: string) => {
  await t.typeText(areaFilterInput, areaName)
  await t.click(areaFilterFirstOption)
}

const specialFilterItems = {
  duplicate: Selector('[data-for="application-basis-DUPLICATE_APPLICATION"]')
}

const unitFilter = Selector('[data-qa="unit-selector"] input')

const application = (id: string) =>
  Selector(`[data-application-id="${id}"]`, { timeout: 3000 })

const applications = Selector('[data-qa="table-of-applications"]').find(
  '[data-qa="table-application-row"]'
)

const voucherUnitFilter = {
  firstChoice: Selector('[data-qa="filter-voucher-first-choice"]'),
  voucherOnly: Selector('[data-qa="filter-voucher-all"]'),
  voucherHide: Selector('[data-qa="filter-voucher-hide"]'),
  noFilter: Selector('[data-qa="filter-voucher-no-filter"]')
}

export default {
  url,
  applicationStatus,
  actionsMenu,
  actionsMenuItems,
  allApplications,
  specialFilterItems,
  application,
  applications,
  toggleArea,
  unitFilter,
  voucherUnitFilter
}
