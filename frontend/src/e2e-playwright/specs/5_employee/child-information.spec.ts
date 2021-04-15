import { Page } from 'playwright'
import config from 'e2e-test-common/config'
import {
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  resetDatabase
} from 'e2e-test-common/dev-api'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareGroupFixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import { UUID } from 'e2e-test-common/dev-api/types'
import EmployeeNav from 'e2e-playwright/pages/employee/employee-nav'
import ChildInformationPage, {
  DailyServiceTimeSection
} from 'e2e-playwright/pages/employee/child-information'
import { newBrowserContext } from 'e2e-playwright/browser'

let page: Page
let childInformationPage: ChildInformationPage
let childId: UUID

beforeEach(async () => {
  await resetDatabase()

  const [fixtures] = await initializeAreaAndPersonData()
  await insertDaycareGroupFixtures([daycareGroupFixture])

  const unitId = fixtures.daycareFixture.id
  childId = fixtures.familyWithTwoGuardians.children[0].id

  const daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    childId,
    unitId
  )
  await insertDaycarePlacementFixtures([daycarePlacementFixture])

  page = await (await newBrowserContext()).newPage()
  await page.goto(config.employeeUrl)
  const nav = new EmployeeNav(page)
  await nav.login('admin')
  await page.goto(config.employeeUrl + '/child-information/' + childId)
  childInformationPage = new ChildInformationPage(page)
})

describe('Child Information - daily service times', () => {
  let section: DailyServiceTimeSection
  beforeEach(async () => {
    section = await childInformationPage.openDailyServiceTimesCollapsible()
  })

  test('no service times initially', async () => {
    expect(await section.typeText).toEqual('Ei asetettu')
    expect(await section.hasTimesText).toEqual(false)
  })

  test('cannot save regular without setting times', async () => {
    const editor = await section.edit()
    await editor.selectRegularTime()
    expect(await editor.submitIsDisabled).toEqual(true)
  })

  test('set regular daily service times', async () => {
    const editor = await section.edit()
    await editor.selectRegularTime()
    await editor.fillTimeRange('regular', '09:00', '17:00')
    await editor.submit()

    expect(await section.typeText).toEqual('Säännöllinen varhaiskasvatusaika')
    expect(await section.timesText).toEqual('maanantai-perjantai 09:00–17:00')
  })

  test('cannot save irregular without setting times', async () => {
    const editor = await section.edit()
    await editor.selectIrregularTime()
    expect(await editor.submitIsDisabled).toEqual(true)
  })

  test('set irregular daily service times', async () => {
    const editor = await section.edit()
    await editor.selectIrregularTime()
    await editor.selectDay('monday')
    await editor.fillTimeRange('monday', '08:15', '16:45')
    await editor.selectDay('friday')
    await editor.fillTimeRange('friday', '09:00', '13:30')
    await editor.submit()

    expect(await section.typeText).toEqual(
      'Epäsäännöllinen varhaiskasvatusaika'
    )
    expect(await section.timesText).toEqual(
      'maanantai 08:15-16:45, perjantai 09:00-13:30'
    )
  })
})
