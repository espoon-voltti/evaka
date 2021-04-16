import path from 'path'
import {
  deleteApplication,
  insertApplications
} from '../../../e2e-test-common/dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../../e2e-test-common/dev-api/data-init'
import {
  applicationFixture,
  applicationFixtureId,
  Fixture
} from '../../../e2e-test-common/dev-api/fixtures'
import { seppoAdminRole } from '../../config/users'
import ApplicationEditView from '../../pages/employee/applications/application-edit-view'
import ApplicationReadView from '../../pages/employee/applications/application-read-view'
import Home from '../../pages/home'
import { logConsoleMessages } from '../../utils/fixture'

const testFilePath = '../../assets/test_file.png'
const testFileName = path.basename(testFilePath)

const home = new Home()
const applicationReadView = new ApplicationReadView()
const applicationEditView = new ApplicationEditView()

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

fixture('Employee attachments')
  .meta({ type: 'regression', subType: 'attachments' })
  .page(home.homePage('admin'))
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
  })
  .beforeEach(async (t) => {
    await t.useRole(seppoAdminRole)

    const fixture = applicationFixture(
      fixtures.enduserChildFixtureJari,
      fixtures.enduserGuardianFixture
    )

    await insertApplications([fixture])
  })
  .afterEach(async (t) => {
    await logConsoleMessages(t)
    await deleteApplication(applicationFixtureId)
  })
  .after(async () => {
    await Fixture.cleanup()
    await cleanUp()
  })

test('Employee can add and remove attachments', async (t) => {
  await applicationReadView.openApplicationByLink(applicationFixtureId)
  await applicationReadView.startEditing()

  await applicationEditView.setUrgent()
  await applicationEditView.uploadUrgentFile(testFilePath)
  await applicationEditView.assertUploadedUrgentFile(testFileName)

  await applicationEditView.setShiftCareNeeded()
  await applicationEditView.uploadShiftCareFile(testFilePath)
  await applicationEditView.assertUploadedShiftCareFile(testFileName)

  await applicationEditView.deleteShiftCareFile(testFileName)
  await applicationEditView.assertUploadedShiftCareFile(testFileName, false)
  await applicationEditView.saveApplication()

  await applicationReadView.openApplicationByLink(applicationFixtureId)
  await applicationReadView.assertUrgencyAttachmentReceivedAtVisible(
    testFileName
  )
})
