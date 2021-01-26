// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Home from '../../pages/home'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from '../../dev-api/data-init'
import {
  applicationFixture,
  applicationFixtureId,
  uuidv4
} from '../../dev-api/fixtures'
import { logConsoleMessages } from '../../utils/fixture'
import { deleteApplication, insertApplications } from '../../dev-api'
import { enduserRole } from '../../config/users'
import EnduserPage from '../../pages/enduser/enduser-navigation'
import { add, sub } from 'date-fns'
import { OtherGuardianAgreementStatus } from '../../dev-api/types'
import { ApplicationStatus } from '@evaka/lib-common/src/api-types/application/enums'

const home = new Home()
const enduserPage = new EnduserPage()

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

fixture('Enduser application validations')
  .meta({ type: 'regression', subType: 'application-validations' })
  .page(home.homePage('admin'))
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
  })
  .afterEach(async (t) => {
    await logConsoleMessages(t)
    await deleteApplication(applicationFixtureId)
  })
  .after(async () => {
    await cleanUp()
  })

test('Preferred units can only be selected after preferred start date is selected', async (t) => {
  await t.useRole(enduserRole)
  await enduserPage.navigateToApplicationsTab()
  await enduserPage.createApplication('DAYCARE')

  await enduserPage.assertSelectPreferredStartDateFirstWarningIsShown()
  await enduserPage.openServiceNeedSection()
  await enduserPage.selectPreferredDate(add(new Date(), { months: 1, days: 3 }))
  await enduserPage.assertSelectPreferredStartDateFirstWarningIsNotShown()
})

test('Created application preferred start date can be moved earlier', async (t) => {
  const fixture = applicationFixture(
    fixtures.enduserChildFixtureJari,
    fixtures.enduserGuardianFixture
  )

  const originalPreferredDate = add(new Date(), { months: 1, days: 3 })

  const createdApplication = {
    ...fixture,
    form: {
      ...fixture.form,
      preferredStartDate: originalPreferredDate.toISOString(),
      otherGuardianAgreementStatus: 'AGREED' as OtherGuardianAgreementStatus
    },
    status: 'CREATED' as ApplicationStatus,
    id: uuidv4()
  }

  await insertApplications([createdApplication])

  await t.useRole(enduserRole)
  await enduserPage.navigateToApplicationsTab()
  await enduserPage.editApplication(createdApplication.id)
  await enduserPage.selectPreferredDate(sub(originalPreferredDate, { days: 1 }))
  await enduserPage.assertNoValidationErrors()
})

test('Sent application preferred start date cannot be moved earlier', async (t) => {
  const fixture = applicationFixture(
    fixtures.enduserChildFixtureJari,
    fixtures.enduserGuardianFixture
  )

  const originalPreferredDate = add(new Date(), { months: 1, days: 3 })

  const createdApplication = {
    ...fixture,
    form: {
      ...fixture.form,
      preferredStartDate: originalPreferredDate.toISOString(),
      otherGuardianAgreementStatus: 'AGREED' as OtherGuardianAgreementStatus
    },
    status: 'SENT' as ApplicationStatus,
    id: uuidv4()
  }

  await insertApplications([createdApplication])

  await t.useRole(enduserRole)
  await enduserPage.navigateToApplicationsTab()
  await enduserPage.editApplication(createdApplication.id)
  await enduserPage.selectPreferredDate(sub(originalPreferredDate, { days: 1 }))
  await enduserPage.assertValidationErrorsExists([
    'Päivämäärä ei ole sallitulla välillä'
  ])
})
