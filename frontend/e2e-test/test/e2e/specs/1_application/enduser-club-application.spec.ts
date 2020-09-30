// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import ClubApplication from '../../pages/enduser/club-application-form'
import Home from '../../pages/home'
import Applications from '../../pages/enduser/applications'
import { APPLICATION_STATUS, APPLICATION_TYPE } from '../../const'
import { logConsoleMessages } from '../../utils/fixture'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import { deleteApplication } from '../../dev-api'

const home = new Home()
const applications = new Applications()

let cleanUp: () => Promise<void>
let applicationId: string | undefined

fixture('Club application')
  .meta({ type: 'regression', subType: 'club-application' })
  .page(home.homePage('enduser'))
  .before(async () => {
    ;[, cleanUp] = await initializeAreaAndPersonData()
  })
  .afterEach(logConsoleMessages)
  .after(async () => {
    applicationId ? await deleteApplication(applicationId) : false
    await cleanUp()
  })

test('Create and save Club application with minimum information', async () => {
  const clubApplication = new ClubApplication()
  await clubApplication.navigateToApplications()
  const applicationId = await applications.createClubApplication()

  await clubApplication.fillClubApplication()
  await clubApplication.save()
  await applications.verifyCreated(applicationId)
})

test('Edit & send Club application (with status == CREATED)', async () => {
  const clubApplication = new ClubApplication()
  await clubApplication.navigateToApplications()
  applicationId = await applications.getFirstEditableApplication(
    APPLICATION_TYPE.CLUB,
    APPLICATION_STATUS.CREATED
  )
  // await this.navigateToApplications()

  await applications.verifyCreated(applicationId)
  await applications.modifyApplicationById(applicationId)

  await clubApplication.modifyClubApplication()
  await clubApplication.checkAndSend()
  await applications.verifySent(applicationId)
})

test('Edit & send Club Application (with status == SENT)', async () => {
  const clubApplication = new ClubApplication()
  await clubApplication.navigateToApplications()
  const applicationId = await applications.getFirstEditableApplication(
    APPLICATION_TYPE.CLUB
  )
  // await this.navigateToApplications()

  await applications.verifySent(applicationId)
  await applications.modifyApplicationById(applicationId)

  await clubApplication.modifyClubApplication()
  await clubApplication.checkAndSend()
  await applications.verifySent(applicationId)
})
