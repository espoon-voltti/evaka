// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { addYears, format } from 'date-fns'
import EnduserPage from '../../pages/enduser/enduser-navigation'
import { ApplicationWorkbenchPage } from '../../pages/admin/application-workbench-page'
import DaycareApplication from '../../pages/enduser/daycare-application'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from '../../dev-api/data-init'
import { logConsoleMessages } from '../../utils/fixture'
import {
  deleteApplication,
  insertDaycarePlacementFixtures
} from '../../dev-api'
import { enduserRole, seppoAdminRole } from '../../config/users'

const enduserPage = new EnduserPage()
const applicationWorkbench = new ApplicationWorkbenchPage()

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>
const daycarePlacementId = '2fdc7644-d1ca-4c48-b8be-d6ff64a601b2'

fixture('New transfer preschool application')
  .meta({ type: 'regression', subType: 'transfer-preschool-application' })
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
    await insertDaycarePlacementFixtures([
      {
        id: daycarePlacementId,
        childId: fixtures.enduserChildFixtureJari.id,
        type: 'PRESCHOOL',
        unitId: fixtures.preschoolFixture.id,
        startDate: format(new Date(), 'yyyy-MM-dd'),
        endDate: format(addYears(new Date(), 1), 'yyyy-MM-dd')
      }
    ])
  })
  .afterEach(async (t) => {
    await logConsoleMessages(t)
    applicationId ? await deleteApplication(applicationId) : false
  })
  .after(async () => {
    await cleanUp()
  })

let applicationId: string

test('Enduser sends a preschool application for a child with a preschool placement, admin sees it as a transfer application', async (t) => {
  // send the application as enduser
  await t.useRole(enduserRole)

  await enduserPage.navigateToApplicationsTab()
  await enduserPage.createApplication('PRESCHOOL')
  const daycareApplication = new DaycareApplication()

  applicationId = await daycareApplication.getApplicationId()
  await daycareApplication.fillInBasicPreschoolApplicationDetails(
    fixtures.preschoolFixture.name
  )

  await daycareApplication.fillOtherGuardianAgreedInformation()

  await daycareApplication.checkAndSend()
  await daycareApplication.assertApplicationStatus(applicationId, 'LÄHETETTY')

  // check the result as service worker
  await t.useRole(seppoAdminRole)

  await applicationWorkbench.searchFilter.filterByTransferOnly()
  await applicationWorkbench.openApplicationById(applicationId)

  // todo: not visible on new application view?
  // await t.expect(applicationWorkbench.transferApplicationIndicator.exists).ok()
})

test('Enduser sends a preschool daycare application for a child with a preschool placement, admin does not see it as a transfer application', async (t) => {
  // send the application as enduser
  await t.useRole(enduserRole)

  await enduserPage.navigateToApplicationsTab()
  await enduserPage.createApplication('PRESCHOOL')
  const daycareApplication = new DaycareApplication()

  applicationId = await daycareApplication.getApplicationId()
  await daycareApplication.fillInBasicPreschoolApplicationDetails(
    fixtures.preschoolFixture.name,
    { preschoolDaycare: true }
  )

  await daycareApplication.fillOtherGuardianAgreedInformation()

  await daycareApplication.checkAndSend()
  await daycareApplication.assertApplicationStatus(applicationId, 'LÄHETETTY')

  // check the result as service worker
  await t.useRole(seppoAdminRole)

  await applicationWorkbench.searchFilter.filterByTransferOnly()
  await t.expect(applicationWorkbench.applicationList.visible).ok()
  await applicationWorkbench.waitUntilApplicationsLoaded()
  await t.expect(applicationWorkbench.application.count).eql(0)
})
