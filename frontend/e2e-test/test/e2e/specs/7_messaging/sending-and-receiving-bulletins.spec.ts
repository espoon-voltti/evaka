import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import { logConsoleMessages } from '../../utils/fixture'
import config from '../../config'
import {
  deleteEmployeeFixture,
  insertEmployeeFixture,
  setAclForDaycares
} from '../../dev-api'
import EmployeeHome from '../../pages/employee/home'

const home = new EmployeeHome()

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

fixture('Sending and receiving bulletins')
  .meta({ type: 'regression', subType: 'bulletins' })
  .page(config.adminUrl)
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
    await insertEmployeeFixture({
      externalId: config.supervisorExternalId,
      firstName: 'Seppo',
      lastName: 'Sorsa',
      email: 'seppo.sorsa@espoo.fi',
      roles: []
    })
    await setAclForDaycares(
      config.supervisorExternalId,
      fixtures.daycareFixture.id
    )
  })
  .afterEach(async (t) => {
    await logConsoleMessages(t)
  })
  .after(async () => {
    await deleteEmployeeFixture(config.supervisorExternalId)
    await cleanUp()
  })

test('Supervisor opens messages', async (t) => {
  await home.login({
    aad: config.supervisorAad,
    roles: []
  })
  await home.navigateToMessages()
})
