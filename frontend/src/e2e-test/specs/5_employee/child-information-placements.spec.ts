// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import type { DaycareId, PersonId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { evakaUserId } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import {
  testDaycareGroup,
  familyWithTwoGuardians,
  Fixture,
  testCareArea,
  testDaycare,
  preschoolTerm2023
} from '../../dev-api/fixtures'
import {
  createDaycareGroups,
  createDefaultServiceNeedOptions,
  resetServiceState,
  terminatePlacement
} from '../../generated/api-clients'
import ChildInformationPage from '../../pages/employee/child-information'
import { test, expect } from '../../playwright'
import type { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

test.beforeEach(async (): Promise<void> => resetServiceState())

const mockToday = LocalDate.of(2023, 9, 6)
const mockedTime = HelsinkiDateTime.fromLocal(mockToday, LocalTime.of(9, 35))

async function openChildPlacements(page: Page, childId: PersonId) {
  await page.goto(config.employeeUrl + '/child-information/' + childId)
  const childInformationPage = new ChildInformationPage(page)
  await childInformationPage.waitUntilLoaded()
  return await childInformationPage.openCollapsible('placements')
}

test.describe('Child Information placement info', () => {
  let page: Page
  let childId: PersonId
  let unitId: DaycareId

  test.use({
    evakaOptions: { mockedTime }
  })

  test.beforeEach(async ({ evaka }) => {
    await testCareArea.save()
    await testDaycare.save()
    await familyWithTwoGuardians.save()
    await createDefaultServiceNeedOptions()
    await createDaycareGroups({ body: [testDaycareGroup] })

    unitId = testDaycare.id
    childId = familyWithTwoGuardians.children[0].id
    const unitSupervisor = await Fixture.employee()
      .unitSupervisor(unitId)
      .save()

    page = evaka
    await employeeLogin(page, unitSupervisor)
  })

  test('A terminated placement is indicated', async () => {
    const placement = await Fixture.placement({
      childId,
      unitId,
      startDate: mockToday,
      endDate: mockToday,
      type: 'DAYCARE'
    }).save()

    let childPlacements = await openChildPlacements(page, childId)
    await childPlacements.assertTerminatedByGuardianIsNotShown(placement.id)

    await terminatePlacement({
      body: {
        placementId: placement.id,
        endDate: mockToday,
        terminationRequestedDate: mockToday,
        terminatedBy: evakaUserId(familyWithTwoGuardians.guardian.id)
      }
    })

    childPlacements = await openChildPlacements(page, childId)
    await childPlacements.assertTerminatedByGuardianIsShown(placement.id)
  })

  test('Placement source and creator are shown', async () => {
    const placement = await Fixture.placement({
      childId,
      unitId,
      startDate: mockToday,
      endDate: mockToday,
      type: 'DAYCARE',
      source: 'MANUAL'
    }).save()

    const childPlacements = await openChildPlacements(page, childId)
    await childPlacements.assertSource(placement.id, 'Työntekijä manuaalisesti')
    await childPlacements.assertCreatedBy(placement.id, 'eVaka')
  })

  test('placement edit shows errors on dates outside the extended term', async () => {
    const preschoolTerms = await preschoolTerm2023.save()
    const placement = await Fixture.placement({
      childId,
      unitId,
      startDate: preschoolTerms.extendedTerm.start,
      endDate: preschoolTerms.finnishPreschool.end,
      type: 'PRESCHOOL_DAYCARE'
    }).save()

    const childPlacements = await openChildPlacements(page, childId)
    const row = childPlacements.placementRow(placement.id)
    await row.editButton.click()

    await row.startDateInput.fill(preschoolTerms.extendedTerm.start.subDays(1))
    await expect(row.preschoolTermError).toBeVisible()
    await row.confirmRetroactive.check()
    await row.saveButton.assertDisabled(true)

    // The end date is invalid, so fixing the start date must not clear the error
    await row.endDateInput.fill(preschoolTerms.extendedTerm.end.addDays(1))
    await row.startDateInput.fill(preschoolTerms.extendedTerm.start)
    await expect(row.preschoolTermError).toBeVisible()
    await row.saveButton.assertDisabled(true)

    await row.endDateInput.fill(preschoolTerms.extendedTerm.end)
    await expect(row.preschoolTermError).toBeHidden()
    await row.saveButton.click()
    await expect(row.endDate).toHaveText(
      preschoolTerms.extendedTerm.end.format()
    )
  })

  test('placement edit shows an overlap warning on the date that was moved over another placement', async () => {
    await Fixture.placement({
      childId,
      unitId,
      startDate: LocalDate.of(2023, 11, 1),
      endDate: LocalDate.of(2023, 11, 30)
    }).save()
    const placement = await Fixture.placement({
      childId,
      unitId,
      startDate: LocalDate.of(2024, 1, 1),
      endDate: LocalDate.of(2024, 3, 31)
    }).save()
    await Fixture.placement({
      childId,
      unitId,
      startDate: LocalDate.of(2024, 5, 1),
      endDate: LocalDate.of(2024, 5, 31)
    }).save()

    const childPlacements = await openChildPlacements(page, childId)
    const row = childPlacements.placementRow(placement.id)
    await row.editButton.click()

    await row.endDateInput.fill(LocalDate.of(2024, 5, 15))
    await expect(row.endDateOverlapWarning).toBeVisible()
    await expect(row.startDateOverlapWarning).toBeHidden()

    // Moving the start date does not affect the overlap caused by the end date
    await row.startDateInput.fill(LocalDate.of(2024, 2, 1))
    await expect(row.endDateOverlapWarning).toBeVisible()
    await expect(row.startDateOverlapWarning).toBeHidden()

    await row.endDateInput.fill(LocalDate.of(2024, 3, 31))
    await expect(row.endDateOverlapWarning).toBeHidden()

    await row.startDateInput.fill(LocalDate.of(2023, 11, 15))
    await expect(row.startDateOverlapWarning).toBeVisible()
    await expect(row.endDateOverlapWarning).toBeHidden()
  })

  test('placement edit shows no backup care warning before the dates are changed', async () => {
    const placement = await Fixture.placement({
      childId,
      unitId,
      startDate: LocalDate.of(2024, 1, 1),
      endDate: LocalDate.of(2024, 3, 31)
    }).save()
    await Fixture.backupCare({
      childId,
      unitId,
      period: new FiniteDateRange(
        LocalDate.of(2024, 1, 1),
        LocalDate.of(2024, 3, 31)
      )
    }).save()

    const childPlacements = await openChildPlacements(page, childId)
    const row = childPlacements.placementRow(placement.id)
    await row.editButton.click()
    // The backup care covers the whole placement, but nothing has been edited yet
    await expect(row.startDateInput).toBeVisible()
    await expect(row.backupCareConflictWarning).toBeHidden()

    await row.endDateInput.fill(LocalDate.of(2024, 3, 20))
    await expect(row.backupCareConflictWarning).toBeVisible()
  })

  test('placement edit backup care warning reflects both edited dates', async () => {
    const placement = await Fixture.placement({
      childId,
      unitId,
      startDate: LocalDate.of(2024, 1, 1),
      endDate: LocalDate.of(2024, 3, 31)
    }).save()
    await Fixture.backupCare({
      childId,
      unitId,
      period: new FiniteDateRange(
        LocalDate.of(2024, 2, 1),
        LocalDate.of(2024, 2, 14)
      )
    }).save()

    const childPlacements = await openChildPlacements(page, childId)
    const row = childPlacements.placementRow(placement.id)
    await row.editButton.click()

    await row.endDateInput.fill(LocalDate.of(2024, 2, 10))
    await expect(row.backupCareConflictWarning).toBeVisible()
    // The end date still cuts the backup care, so moving the start date must not clear the warning
    await row.startDateInput.fill(LocalDate.of(2024, 1, 5))
    await expect(row.backupCareConflictWarning).toBeVisible()

    // Cancelling resets the dates, so the warning must not remain when editing again
    await row.cancelButton.click()
    await row.editButton.click()
    await expect(row.backupCareConflictWarning).toBeHidden()

    await row.startDateInput.fill(LocalDate.of(2024, 2, 5))
    await expect(row.backupCareConflictWarning).toBeVisible()
    // The start date still cuts the backup care, so moving the end date must not clear the warning
    await row.endDateInput.fill(LocalDate.of(2024, 3, 20))
    await expect(row.backupCareConflictWarning).toBeVisible()
  })
})

async function loginAndCreateUnitAndChild(page: Page) {
  const admin = await Fixture.employee().admin().save()
  const area = await Fixture.careArea().save()
  const unit = await Fixture.daycare({ areaId: area.id }).save()
  const child = await Fixture.person().saveChild({ updateMockVtj: true })
  await employeeLogin(page, admin)
  return { unitName: unit.name, childId: child.id }
}

test.describe('Child Information placement create (feature flag place guarantee = true)', () => {
  let page: Page
  let unitName: string
  let childId: PersonId

  test.use({
    evakaOptions: {
      mockedTime,
      employeeCustomizations: { featureFlags: { placementGuarantee: true } }
    }
  })

  test.beforeEach(async ({ evaka }) => {
    page = evaka
    const setup = await loginAndCreateUnitAndChild(page)
    unitName = setup.unitName
    childId = setup.childId
  })

  test('place guarantee can be set with create modal', async () => {
    const childPlacements = await openChildPlacements(page, childId)

    await childPlacements.createNewPlacement({
      unitName,
      startDate: mockToday.subDays(2).format(),
      endDate: mockToday.subDays(2).format(),
      placeGuarantee: false
    })
    await childPlacements.createNewPlacement({
      unitName,
      startDate: mockToday.subDays(1).format(),
      endDate: mockToday.subDays(1).format(),
      placeGuarantee: true
    })
    await childPlacements.createNewPlacement({
      unitName,
      startDate: mockToday.addDays(1).format(),
      endDate: mockToday.addDays(1).format(),
      placeGuarantee: true
    })
    await childPlacements.createNewPlacement({
      unitName,
      startDate: mockToday.addDays(2).format(),
      endDate: mockToday.addDays(2).format(),
      placeGuarantee: false
    })

    await childPlacements.assertPlacementRows([
      { unitName, period: '08.09.2023 - 08.09.2023', status: 'Tulossa' },
      { unitName, period: '07.09.2023 - 07.09.2023', status: 'Takuupaikka' },
      { unitName, period: '05.09.2023 - 05.09.2023', status: 'Päättynyt' },
      { unitName, period: '04.09.2023 - 04.09.2023', status: 'Päättynyt' }
    ])
  })

  test('place guarantee placement shows correctly active status', async () => {
    const childPlacements = await openChildPlacements(page, childId)

    await childPlacements.createNewPlacement({
      unitName,
      startDate: mockToday.format(),
      endDate: mockToday.format(),
      placeGuarantee: true
    })

    await childPlacements.assertPlacementRows([
      { unitName, period: '06.09.2023 - 06.09.2023', status: 'Aktiivinen' }
    ])
  })

  test('non place guarantee placement shows correctly active status', async () => {
    const childPlacements = await openChildPlacements(page, childId)

    await childPlacements.createNewPlacement({
      unitName,
      startDate: mockToday.format(),
      endDate: mockToday.format(),
      placeGuarantee: false
    })

    await childPlacements.assertPlacementRows([
      { unitName, period: '06.09.2023 - 06.09.2023', status: 'Aktiivinen' }
    ])
  })
})

test.describe('Child Information placement create (feature flag place guarantee = false)', () => {
  let page: Page
  let unitName: string
  let childId: PersonId

  test.use({
    evakaOptions: {
      mockedTime,
      employeeCustomizations: { featureFlags: { placementGuarantee: false } }
    }
  })

  test.beforeEach(async ({ evaka }) => {
    page = evaka
    const setup = await loginAndCreateUnitAndChild(page)
    unitName = setup.unitName
    childId = setup.childId
  })

  test('placement create works', async () => {
    const childPlacements = await openChildPlacements(page, childId)

    await childPlacements.createNewPlacement({
      unitName,
      startDate: mockToday.subDays(1).format(),
      endDate: mockToday.subDays(1).format()
    })
    await childPlacements.createNewPlacement({
      unitName,
      startDate: mockToday.format(),
      endDate: mockToday.format()
    })
    await childPlacements.createNewPlacement({
      unitName,
      startDate: mockToday.addDays(1).format(),
      endDate: mockToday.addDays(1).format()
    })

    await childPlacements.assertPlacementRows([
      { unitName, period: '07.09.2023 - 07.09.2023', status: 'Tulossa' },
      { unitName, period: '06.09.2023 - 06.09.2023', status: 'Aktiivinen' },
      { unitName, period: '05.09.2023 - 05.09.2023', status: 'Päättynyt' }
    ])
  })

  test('placement end date is initially empty but mandatory', async () => {
    const childPlacements = await openChildPlacements(page, childId)
    const modal = await childPlacements.openCreatePlacementModal()

    await modal.unit.fillAndSelectFirst(unitName)
    await expect(modal.endDate).toHaveText('')
    await modal.submitButton.assertDisabled(true)
  })

  test('placement create dialog shows errors on dates outside preschool and extended terms', async () => {
    const preschoolTerms = await preschoolTerm2023.save()

    const childPlacements = await openChildPlacements(page, childId)
    const modal = await childPlacements.openCreatePlacementModal()

    await modal.type.selectOption('PRESCHOOL')
    await modal.unit.fillAndSelectFirst(unitName)
    await modal.startDate.click()
    await modal.startDate.fill(preschoolTerms.finnishPreschool.start)
    await modal.endDate.fill(preschoolTerms.finnishPreschool.end)
    await modal.confirmRetroactive.check()
    await modal.submitButton.assertDisabled(false)

    // Placement starts a day before the term
    await modal.startDate.fill(preschoolTerms.finnishPreschool.start.subDays(1))
    await expect(modal.preschoolTermError).toBeVisible()
    await modal.submitButton.assertDisabled(true)

    // A day before preschool term the extended term is valid so placement can be created
    await modal.type.selectOption('PRESCHOOL_DAYCARE')
    await modal.submitButton.assertDisabled(false)

    // Placement starts a day before the term so it is invalid
    await modal.startDate.fill(preschoolTerms.extendedTerm.start.subDays(1))
    await expect(modal.preschoolTermError).toBeVisible()
    await modal.submitButton.assertDisabled(true)

    await modal.startDate.fill(preschoolTerms.extendedTerm.start)
    await modal.submit()
    await childPlacements.assertPlacementRows([
      {
        unitName,
        period: `${preschoolTerms.extendedTerm.start.format()} - ${preschoolTerms.finnishPreschool.end.format()}`,
        status: 'Aktiivinen'
      }
    ])
  })
})
