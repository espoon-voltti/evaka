// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector } from 'testcafe'
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

const areaFilterItems = (areaShortName: string) =>
  Selector(`[data-qa="area-filter-${areaShortName}"]`)

const specialFilterItems = {
  duplicate: Selector('[data-for="application-basis-DUPLICATE_APPLICATION"]')
}

const application = (id: string) => Selector(`[data-application-id="${id}"]`)

const applications = Selector('[data-qa="table-of-applications"]').find(
  '[data-qa="table-application-row"]'
)

export default {
  url,
  applicationStatus,
  actionsMenu,
  actionsMenuItems,
  allApplications,
  specialFilterItems,
  application,
  applications,
  areaFilterItems
}
