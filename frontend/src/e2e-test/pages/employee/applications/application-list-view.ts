// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector } from 'testcafe'
import config from '../../../config'
import { selectFirstOption } from '../../../utils/helpers'

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
const toggleArea = async (areaName: string) => {
  await selectFirstOption(areaFilter, areaName)
}

const specialFilterItems = {
  duplicate: Selector('[data-for="application-basis-DUPLICATE_APPLICATION"]')
}

const unitFilter = Selector('[data-qa="unit-selector"]')
const toggleUnit = async (unitName: string) => {
  await selectFirstOption(unitFilter, unitName)
}

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
  toggleUnit,
  voucherUnitFilter
}
