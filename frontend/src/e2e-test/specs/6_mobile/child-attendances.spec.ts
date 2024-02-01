// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { BrowserContextOptions } from 'playwright'

import { PlacementType } from 'lib-common/generated/api-types/placement'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'

import { EvakaBrowserContextOptions } from '../../browser'
import { insertDefaultServiceNeedOptions, resetDatabase } from '../../dev-api'
import {
  careAreaFixture,
  daycare2Fixture,
  daycareFixture,
  daycareGroupFixture,
  enduserChildFixtureJari,
  enduserChildFixtureKaarina,
  enduserChildFixturePorriHatterRestricted,
  familyWithTwoGuardians,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import { DaycarePlacement } from '../../dev-api/types'
import ChildAttendancePage from '../../pages/mobile/child-attendance-page'
import MobileChildPage from '../../pages/mobile/child-page'
import MobileListPage from '../../pages/mobile/list-page'
import MobileNotePage from '../../pages/mobile/note-page'
import { waitUntilEqual } from '../../utils'
import { pairMobileDevice } from '../../utils/mobile'
import { Page } from '../../utils/page'

let page: Page
let listPage: MobileListPage
let childPage: MobileChildPage
let childAttendancePage: ChildAttendancePage

let now = HelsinkiDateTime.of(2024, 5, 17, 13, 0, 0)
let today = now.toLocalDate()

const group2 = {
  id: uuidv4(),
  name: '#2',
  daycareId: daycareFixture.id,
  startDate: LocalDate.of(2021, 1, 1)
}

const openPage = async (
  options?: BrowserContextOptions & EvakaBrowserContextOptions
) => {
  page = await Page.open(options ?? { mockedTime: now })
  listPage = new MobileListPage(page)
  childPage = new MobileChildPage(page)
  childAttendancePage = new ChildAttendancePage(page)
}

beforeEach(async () => {
  await resetDatabase()
  await insertDefaultServiceNeedOptions()
  await Fixture.preschoolTerm().save()

  const careArea = await Fixture.careArea().with(careAreaFixture).save()
  await Fixture.daycare().with(daycareFixture).careArea(careArea).save()
  await Fixture.daycareGroup().with(daycareGroupFixture).save()
  await Fixture.daycareGroup().with(group2).save()

  await Fixture.person().with(enduserChildFixtureKaarina).save()
  await Fixture.child(enduserChildFixtureKaarina.id).save()

  await Fixture.person().with(enduserChildFixtureJari).save()
  await Fixture.child(enduserChildFixtureJari.id).save()

  await Fixture.person().with(familyWithTwoGuardians.children[0]).save()
  await Fixture.child(familyWithTwoGuardians.children[0].id).save()

  await Fixture.person().with(enduserChildFixturePorriHatterRestricted).save()
  await Fixture.child(enduserChildFixturePorriHatterRestricted.id).save()

  await Fixture.employee()
    .with({ roles: ['ADMIN'] })
    .save()
})

async function createPlacements(
  childId: string,
  groupId: string = daycareGroupFixture.id,
  placementType: PlacementType = 'DAYCARE'
) {
  const daycarePlacementFixture = await Fixture.placement()
    .with({
      childId,
      unitId: daycareFixture.id,
      type: placementType,
      startDate: today.subMonths(3),
      endDate: today.addMonths(3)
    })
    .save()
  await Fixture.groupPlacement()
    .with({
      daycarePlacementId: daycarePlacementFixture.data.id,
      daycareGroupId: groupId,
      startDate: daycarePlacementFixture.data.startDate,
      endDate: daycarePlacementFixture.data.endDate
    })
    .save()
  return daycarePlacementFixture.data
}

const createPlacementAndReload = async (
  placementType: PlacementType
): Promise<DaycarePlacement> => {
  const daycarePlacementFixture = await createPlacements(
    familyWithTwoGuardians.children[0].id,
    daycareGroupFixture.id,
    placementType
  )

  const mobileSignupUrl = await pairMobileDevice(daycareFixture.id)
  await page.goto(mobileSignupUrl)

  return daycarePlacementFixture
}

const markChildArrivedAndDeparted = async (
  arrivalTime = '08:15',
  departureTime = '16:00',
  nonBillableAbsenceExpected: boolean,
  billableAbsenceExpected: boolean
) => {
  await listPage.selectChild(familyWithTwoGuardians.children[0].id)
  await childPage.markPresentLink.click()
  await childAttendancePage.setTime(arrivalTime)
  await childAttendancePage.selectMarkPresent()
  await childAttendancePage.selectPresentTab()
  await childAttendancePage.selectChildLink(0)
  await childAttendancePage.selectMarkDepartedLink()

  await childAttendancePage.setTime(departureTime)

  if (nonBillableAbsenceExpected) {
    await childAttendancePage
      .markAbsentByCategoryAndTypeButton('NONBILLABLE', 'OTHER_ABSENCE')
      .click()
  }
  if (billableAbsenceExpected) {
    await childAttendancePage
      .markAbsentByCategoryAndTypeButton('BILLABLE', 'OTHER_ABSENCE')
      .click()
  }
  await childAttendancePage.markDepartedButton.click()

  if (nonBillableAbsenceExpected || billableAbsenceExpected) {
    await childAttendancePage.assertChildStatusLabelIsShown('Lähtenyt')
  } else {
    await childAttendancePage.assertNoChildrenPresentIndicatorIsShown()
  }
}

describe('Child mobile attendances', () => {
  test('Child a full day in daycare placement is not required to mark absence types', async () => {
    await openPage()
    await createPlacementAndReload('DAYCARE')
    await markChildArrivedAndDeparted('08:00', '16:00', false, false)
  })

  test('Child a part day in daycare placement is not required to mark absence types', async () => {
    await openPage()
    await createPlacementAndReload('DAYCARE_PART_TIME')
    await markChildArrivedAndDeparted('08:00', '11:00', false, false)
  })

  test('Child a full day in preschool placement is not required to mark absence types', async () => {
    await openPage()
    await createPlacementAndReload('PRESCHOOL')
    await markChildArrivedAndDeparted('08:50', '12:58', false, false)
  })

  test('Child in preschool placement for less than 1 hour is required to mark absence type', async () => {
    await openPage()
    await createPlacementAndReload('PRESCHOOL')
    await markChildArrivedAndDeparted('12:10', '13:15', true, false)
  })

  test('Child in preschool daycare placement is not required to mark absence types', async () => {
    await openPage()
    await createPlacementAndReload('PRESCHOOL_DAYCARE')
    await markChildArrivedAndDeparted('08:00', '16:00', false, false)
  })

  test('Child in preschool daycare placement for preschool only is required to mark billable absence type', async () => {
    await openPage()
    await createPlacementAndReload('PRESCHOOL_DAYCARE')
    await markChildArrivedAndDeparted('08:55', '13:05', false, true)
  })

  test('Child in preschool daycare placement late for preschool is required to mark nonbillable absence type', async () => {
    await openPage()
    await createPlacementAndReload('PRESCHOOL_DAYCARE')
    await markChildArrivedAndDeparted('12:35', '16:05', true, false)
  })

  test('Child in preschool daycare placement for very short time is required to mark absence types', async () => {
    await openPage()
    await createPlacementAndReload('PRESCHOOL_DAYCARE')
    await markChildArrivedAndDeparted('08:55', '09:05', true, true)
  })

  test('noAbsenceType feature flag allows marking no absence despite expected absence', async () => {
    await openPage({
      mockedTime: now,
      employeeCustomizations: { featureFlags: { noAbsenceType: true } }
    })

    await createPlacementAndReload('PRESCHOOL_DAYCARE')
    await listPage.selectChild(familyWithTwoGuardians.children[0].id)
    await childPage.markPresentLink.click()
    await childAttendancePage.setTime('07:00')
    await childAttendancePage.selectMarkPresent()
    await childAttendancePage.selectPresentTab()
    await childAttendancePage.selectChildLink(0)
    await childAttendancePage.selectMarkDepartedLink()
    await childAttendancePage.setTime('08:55')

    await childAttendancePage
      .markAbsentByCategoryAndTypeButton('NONBILLABLE', 'NO_ABSENCE')
      .click()
    await childAttendancePage.markDepartedButton.click()
    await childAttendancePage.assertChildStatusLabelIsShown('Lähtenyt')
  })
})

const assertAttendanceCounts = async (
  coming: number,
  present: number,
  departed: number,
  absent: number,
  total: number
) =>
  await waitUntilEqual(() => listPage.getAttendanceCounts(), {
    coming,
    present,
    departed,
    absent,
    total
  })

describe('Child mobile attendance list', () => {
  test('Child can be marked as present and as departed', async () => {
    await openPage()
    const child1 = enduserChildFixtureKaarina.id
    await createPlacements(child1)
    await createPlacements(enduserChildFixturePorriHatterRestricted.id)
    await createPlacements(enduserChildFixtureJari.id, group2.id)

    const mobileSignupUrl = await pairMobileDevice(daycareFixture.id)
    await page.goto(mobileSignupUrl)
    await assertAttendanceCounts(3, 0, 0, 0, 3)
    await listPage.comingChildrenTab.click()
    await listPage.selectChild(child1)
    await childPage.markPresentLink.click()
    await childAttendancePage.setTime('08:00')
    await childAttendancePage.selectMarkPresent()

    await assertAttendanceCounts(2, 1, 0, 0, 3)

    await listPage.presentChildrenTab.click()
    await listPage.selectChild(child1)
    await childPage.markDepartedLink.click()
    await childAttendancePage.setTime('14:00')
    await childAttendancePage.markDepartedButton.click()

    await assertAttendanceCounts(2, 0, 1, 0, 3)

    await listPage.departedChildrenTab.click()
    await listPage.assertChildExists(child1)
  })

  test('Child can be marked as absent for the whole day', async () => {
    await openPage()
    const child = enduserChildFixtureKaarina.id
    await createPlacements(child)

    const mobileSignupUrl = await pairMobileDevice(daycareFixture.id)
    await page.goto(mobileSignupUrl)

    await listPage.selectChild(child)
    await childPage.markAbsentLink.click()
    await childAttendancePage.selectMarkAbsentByType('OTHER_ABSENCE')
    await childAttendancePage.selectMarkAbsentButton()
    await childPage.goBack.click()
    await listPage.absentChildrenTab.click()
    await listPage.assertChildExists(child)
  })

  test('Child can be marked present and returned to coming', async () => {
    await openPage()
    const child = enduserChildFixtureKaarina.id
    await createPlacements(child)

    const mobileSignupUrl = await pairMobileDevice(daycareFixture.id)
    await page.goto(mobileSignupUrl)

    await listPage.selectChild(child)
    await childPage.markPresentLink.click()
    await childAttendancePage.setTime('08:00')
    await childAttendancePage.selectMarkPresent()

    await listPage.presentChildrenTab.click()
    await listPage.selectChild(child)
    await childPage.cancelArrivalButton.click()
    await childPage.modalOkButton.click()

    await listPage.comingChildrenTab.click()
    await listPage.assertChildExists(child)
  })

  test('User can undo the whole flow of marking present at 08:30 and leaving at 16:00', async () => {
    await openPage()
    const child = enduserChildFixtureKaarina.id
    await createPlacements(child)

    const mobileSignupUrl = await pairMobileDevice(daycareFixture.id)
    await page.goto(mobileSignupUrl)

    await listPage.selectChild(child)
    await childPage.markPresentLink.click()
    await childAttendancePage.setTime('08:30')
    await childAttendancePage.selectMarkPresent()

    await listPage.presentChildrenTab.click()
    await listPage.selectChild(child)
    await childPage.markDepartedLink.click()
    await childAttendancePage.setTime('16:00')
    await childAttendancePage.markDepartedButton.click()

    await listPage.departedChildrenTab.click()
    await listPage.selectChild(child)
    await childPage.returnToPresentButton.click()
    await childPage.returnToPresentButton.click()

    await listPage.presentChildrenTab.click()
    await listPage.selectChild(child)
    await childPage.cancelArrivalButton.click()
    await childPage.modalOkButton.click()

    await listPage.comingChildrenTab.click()
    await listPage.assertChildExists(child)
  })

  test('Child can have multiple attendances in one day', async () => {
    await openPage()
    const child = enduserChildFixtureKaarina.id
    await createPlacements(child)

    const mobileSignupUrl = await pairMobileDevice(daycareFixture.id)
    await page.goto(mobileSignupUrl)

    await listPage.selectChild(child)
    await childPage.markPresentLink.click()
    await childAttendancePage.setTime('08:30')
    await childAttendancePage.selectMarkPresent()

    await listPage.presentChildrenTab.click()
    await listPage.selectChild(child)
    await childPage.markDepartedLink.click()
    await childAttendancePage.setTime('15:00')
    await childAttendancePage.markDepartedButton.click()

    await listPage.departedChildrenTab.click()
    await listPage.selectChild(child)
    await childPage.returnToPresentButton.click()

    // Cannot overlap previous departure
    await childAttendancePage.setTime('15:00')
    await childAttendancePage.assertMarkPresentButtonDisabled(true)

    await childAttendancePage.setTime('15:15')
    await childAttendancePage.selectMarkPresent()

    await listPage.presentChildrenTab.click()
    await listPage.selectChild(child)
    await childPage.assertArrivalTimeInfoIsShown(
      'Saapumisaika08:30,Saapumisaika15:15'
    )
    await childPage.assertDepartureTimeInfoIsShown('Lähtöaika15:00')
    await childPage.markDepartedLink.click()

    await childAttendancePage.setTime('15:15')
    await childAttendancePage.setTimeInfo.assertTextEquals('Saapui 15:15')
    await childAttendancePage.setTime('15:20')
    await childAttendancePage.markDepartedButton.click()
  })

  test('Group selector works consistently', async () => {
    await openPage()
    const child1 = enduserChildFixtureKaarina.id
    await createPlacements(child1)
    await createPlacements(enduserChildFixturePorriHatterRestricted.id)
    await createPlacements(enduserChildFixtureJari.id, group2.id)

    const mobileSignupUrl = await pairMobileDevice(daycareFixture.id)
    await page.goto(mobileSignupUrl)

    await assertAttendanceCounts(3, 0, 0, 0, 3)

    await listPage.selectChild(child1)
    await childPage.markPresentLink.click()
    await childAttendancePage.setTime('08:00')
    await childAttendancePage.selectMarkPresent()

    await assertAttendanceCounts(2, 1, 0, 0, 3)

    await listPage.selectGroup(group2.id)
    await assertAttendanceCounts(1, 0, 0, 0, 1)

    await listPage.selectGroup('all')
    await assertAttendanceCounts(2, 1, 0, 0, 3)
  })

  test('Group name is visible only when all-group selected', async () => {
    await openPage()
    const childId = enduserChildFixtureKaarina.id
    await createPlacements(childId)
    await createPlacements(enduserChildFixturePorriHatterRestricted.id)
    await createPlacements(enduserChildFixtureJari.id, group2.id)

    const mobileSignupUrl = await pairMobileDevice(daycareFixture.id)
    await page.goto(mobileSignupUrl)

    await listPage.selectGroup('all')
    await waitUntilEqual(
      () => listPage.readChildGroupName(childId),
      daycareGroupFixture.name.toUpperCase()
    )

    await listPage.selectGroup(daycareGroupFixture.id)
    await waitUntilEqual(() => listPage.readChildGroupName(childId), '')
  })

  test('Child will not be visible in two groups at the same time', async () => {
    await openPage()
    const childId = enduserChildFixtureKaarina.id

    await Fixture.daycare()
      .with({ ...daycare2Fixture, areaId: careAreaFixture.id })
      .save()

    const daycareGroup2Fixture = (
      await Fixture.daycareGroup()
        .with({
          id: uuidv4(),
          name: 'testgroup',
          daycareId: daycare2Fixture.id,
          startDate: LocalDate.of(2022, 1, 1)
        })
        .save()
    ).data

    const placement1StartDate = today.subMonths(5)
    const placement1EndDate = today.subMonths(1)

    const placement2StartDate = placement1EndDate.addDays(1)
    const placement2EndDate = today.addMonths(3)

    const daycarePlacementFixture = await Fixture.placement()
      .with({
        childId,
        unitId: daycareFixture.id,
        startDate: placement1StartDate,
        endDate: placement1EndDate
      })
      .save()

    const daycarePlacement2Fixture = await Fixture.placement()
      .with({
        childId,
        unitId: daycare2Fixture.id,
        startDate: placement2StartDate,
        endDate: placement2EndDate
      })
      .save()

    await Fixture.groupPlacement()
      .with({
        daycarePlacementId: daycarePlacementFixture.data.id,
        daycareGroupId: daycareGroupFixture.id,
        startDate: placement1StartDate,
        endDate: placement2EndDate
      })
      .save()

    await Fixture.groupPlacement()
      .with({
        daycarePlacementId: daycarePlacement2Fixture.data.id,
        daycareGroupId: daycareGroup2Fixture.id,
        startDate: placement1StartDate,
        endDate: placement2EndDate
      })
      .save()

    await page.goto(await pairMobileDevice(daycareFixture.id))
    await assertAttendanceCounts(0, 0, 0, 0, 0)

    await page.goto(await pairMobileDevice(daycare2Fixture.id))
    await assertAttendanceCounts(1, 0, 0, 0, 1)
  })

  test('Fixed schedule child\'s "reservation" is correct', async () => {
    await openPage()
    const child = enduserChildFixtureKaarina.id
    await createPlacements(child, daycareGroupFixture.id, 'PRESCHOOL')

    const mobileSignupUrl = await pairMobileDevice(daycareFixture.id)
    await page.goto(mobileSignupUrl)

    await assertAttendanceCounts(1, 0, 0, 0, 1)
    await listPage.selectChild(child)
    await childPage.reservation.assertTextEquals('Läsnä')
  })

  test('Term break child is shown in absent list', async () => {
    // change mocked now to be during term break
    now = HelsinkiDateTime.of(2024, 1, 1, 13, 0, 0)
    today = now.toLocalDate()
    await openPage({ mockedTime: now })

    const child = enduserChildFixtureKaarina.id
    await createPlacements(child, daycareGroupFixture.id, 'PRESCHOOL')

    const mobileSignupUrl = await pairMobileDevice(daycareFixture.id)
    await page.goto(mobileSignupUrl)

    await assertAttendanceCounts(0, 0, 0, 1, 1)
    await listPage.absentChildrenTab.click()
    await listPage.selectChild(child)
    await childPage.termBreak.waitUntilVisible()
  })
})

describe('Notes on child departure page', () => {
  test('All group notes are shown on the child departure page', async () => {
    await openPage()
    const child1 = enduserChildFixtureKaarina.id
    await createPlacements(child1)
    await createPlacements(enduserChildFixturePorriHatterRestricted.id)
    await createPlacements(enduserChildFixtureJari.id, group2.id)

    const mobileSignupUrl = await pairMobileDevice(daycareFixture.id)
    await page.goto(mobileSignupUrl)

    await listPage.selectChild(child1)
    childPage = new MobileChildPage(page)
    await childPage.openNotes()
    const notePage = new MobileNotePage(page)
    await notePage.selectTab('group')
    await notePage.typeAndSaveStickyNote('This is a group note')
    await notePage.initNewStickyNote()
    await notePage.typeAndSaveStickyNote('This is another group note')
    await notePage.assertStickyNote('This is another group note', 1)

    await page.goto(mobileSignupUrl)
    await listPage.comingChildrenTab.click()
    await listPage.selectChild(child1)
    await childPage.markPresentLink.click()
    await childAttendancePage.setTime('08:00')
    await childAttendancePage.selectMarkPresent()

    await listPage.presentChildrenTab.click()
    await listPage.selectChild(child1)
    await childPage.markDepartedLink.click()
    await waitUntilEqual(() => childAttendancePage.groupNote.count(), 2)
  })
})
