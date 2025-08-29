// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { PlacementType } from 'lib-common/generated/api-types/placement'
import type {
  DaycareId,
  PersonId,
  PlacementId
} from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { evakaUserId, randomId } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import type { UUID } from 'lib-common/types'

import config from '../../config'
import {
  createDaycarePlacementFixture,
  testDaycareGroup,
  familyWithTwoGuardians,
  Fixture,
  testCareArea,
  testDaycare,
  preschoolTerm2023
} from '../../dev-api/fixtures'
import {
  createDaycareGroups,
  createDaycarePlacements,
  createDefaultServiceNeedOptions,
  resetServiceState,
  terminatePlacement
} from '../../generated/api-clients'
import ChildInformationPage from '../../pages/employee/child-information'
import {
  Checkbox,
  Combobox,
  DatePicker,
  Modal,
  Page,
  Select
} from '../../utils/page'
import { employeeLogin } from '../../utils/user'

beforeEach(async (): Promise<void> => resetServiceState())

const setupPlacement = async (
  placementId: PlacementId,
  childId: PersonId,
  unitId: DaycareId,
  childPlacementType: PlacementType
) => {
  await createDaycarePlacements({
    body: [
      createDaycarePlacementFixture(
        placementId,
        childId,
        unitId,
        LocalDate.todayInSystemTz(),
        LocalDate.todayInSystemTz(),
        childPlacementType
      )
    ]
  })
}

async function openChildPlacements(page: Page, childId: UUID) {
  await page.goto(config.employeeUrl + '/child-information/' + childId)
  const childInformationPage = new ChildInformationPage(page)
  await childInformationPage.waitUntilLoaded()
  return await childInformationPage.openCollapsible('placements')
}

describe('Child Information placement info', () => {
  let page: Page
  let childId: PersonId
  let unitId: DaycareId

  beforeEach(async () => {
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

    page = await Page.open()
    await employeeLogin(page, unitSupervisor)
  })

  test('A terminated placement is indicated', async () => {
    const placementId = randomId<PlacementId>()
    await setupPlacement(placementId, childId, unitId, 'DAYCARE')

    let childPlacements = await openChildPlacements(page, childId)
    await childPlacements.assertTerminatedByGuardianIsNotShown(placementId)

    await terminatePlacement({
      body: {
        placementId,
        endDate: LocalDate.todayInSystemTz(),
        terminationRequestedDate: LocalDate.todayInSystemTz(),
        terminatedBy: evakaUserId(familyWithTwoGuardians.guardian.id)
      }
    })

    childPlacements = await openChildPlacements(page, childId)
    await childPlacements.assertTerminatedByGuardianIsShown(placementId)
  })
})

describe('Child Information placement create (feature flag place guarantee = true)', () => {
  const mockedTime = HelsinkiDateTime.fromLocal(
    LocalDate.of(2023, 9, 6),
    LocalTime.of(9, 35)
  )

  test('place guarantee can be set with create modal', async () => {
    const admin = await Fixture.employee().admin().save()
    const area = await Fixture.careArea().save()
    const unit = await Fixture.daycare({ areaId: area.id }).save()
    const { name: unitName } = unit
    const child = await Fixture.person().saveChild({ updateMockVtj: true })
    const childId = child.id

    const page = await openPage()
    await employeeLogin(page, admin)
    const childPlacements = await openChildPlacements(page, childId)

    await childPlacements.createNewPlacement({
      unitName,
      startDate: mockedTime.toLocalDate().subDays(2).format(),
      endDate: mockedTime.toLocalDate().subDays(2).format(),
      placeGuarantee: false
    })
    await childPlacements.createNewPlacement({
      unitName,
      startDate: mockedTime.toLocalDate().subDays(1).format(),
      endDate: mockedTime.toLocalDate().subDays(1).format(),
      placeGuarantee: true
    })
    await childPlacements.createNewPlacement({
      unitName,
      startDate: mockedTime.toLocalDate().addDays(1).format(),
      endDate: mockedTime.toLocalDate().addDays(1).format(),
      placeGuarantee: true
    })
    await childPlacements.createNewPlacement({
      unitName,
      startDate: mockedTime.toLocalDate().addDays(2).format(),
      endDate: mockedTime.toLocalDate().addDays(2).format(),
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
    const admin = await Fixture.employee().admin().save()
    const area = await Fixture.careArea().save()
    const unit = await Fixture.daycare({ areaId: area.id }).save()
    const { name: unitName } = unit
    const child = await Fixture.person().saveChild({ updateMockVtj: true })
    const childId = child.id

    const page = await openPage()
    await employeeLogin(page, admin)
    const childPlacements = await openChildPlacements(page, childId)

    await childPlacements.createNewPlacement({
      unitName,
      startDate: mockedTime.toLocalDate().format(),
      endDate: mockedTime.toLocalDate().format(),
      placeGuarantee: true
    })

    await childPlacements.assertPlacementRows([
      { unitName, period: '06.09.2023 - 06.09.2023', status: 'Aktiivinen' }
    ])
  })

  test('non place guarantee placement shows correctly active status', async () => {
    const admin = await Fixture.employee().admin().save()
    const area = await Fixture.careArea().save()
    const unit = await Fixture.daycare({ areaId: area.id }).save()
    const { name: unitName } = unit
    const child = await Fixture.person().saveChild({ updateMockVtj: true })
    const childId = child.id

    const page = await openPage()
    await employeeLogin(page, admin)
    const childPlacements = await openChildPlacements(page, childId)

    await childPlacements.createNewPlacement({
      unitName,
      startDate: mockedTime.toLocalDate().format(),
      endDate: mockedTime.toLocalDate().format(),
      placeGuarantee: false
    })

    await childPlacements.assertPlacementRows([
      { unitName, period: '06.09.2023 - 06.09.2023', status: 'Aktiivinen' }
    ])
  })

  async function openPage() {
    return await Page.open({
      mockedTime,
      employeeCustomizations: { featureFlags: { placementGuarantee: true } }
    })
  }
})

describe('Child Information placement create (feature flag place guarantee = false)', () => {
  const mockedTime = HelsinkiDateTime.fromLocal(
    LocalDate.of(2023, 9, 6),
    LocalTime.of(9, 35)
  )

  test('placement create works', async () => {
    const admin = await Fixture.employee().admin().save()
    const area = await Fixture.careArea().save()
    const unit = await Fixture.daycare({ areaId: area.id }).save()
    const unitName = unit.name
    const child = await Fixture.person().saveChild({ updateMockVtj: true })
    const childId = child.id

    const page = await openPage()
    await employeeLogin(page, admin)

    const childPlacements = await openChildPlacements(page, childId)

    await childPlacements.createNewPlacement({
      unitName,
      startDate: mockedTime.toLocalDate().subDays(1).format(),
      endDate: mockedTime.toLocalDate().subDays(1).format()
    })
    await childPlacements.createNewPlacement({
      unitName,
      startDate: mockedTime.toLocalDate().format(),
      endDate: mockedTime.toLocalDate().format()
    })
    await childPlacements.createNewPlacement({
      unitName,
      startDate: mockedTime.toLocalDate().addDays(1).format(),
      endDate: mockedTime.toLocalDate().addDays(1).format()
    })

    await childPlacements.assertPlacementRows([
      { unitName, period: '07.09.2023 - 07.09.2023', status: 'Tulossa' },
      { unitName, period: '06.09.2023 - 06.09.2023', status: 'Aktiivinen' },
      { unitName, period: '05.09.2023 - 05.09.2023', status: 'Päättynyt' }
    ])
  })

  test('placement end date is initially empty but mandatory', async () => {
    const admin = await Fixture.employee().admin().save()
    const area = await Fixture.careArea().save()
    const unit = await Fixture.daycare({ areaId: area.id }).save()
    const unitName = unit.name
    const child = await Fixture.person().saveChild({ updateMockVtj: true })
    const childId = child.id

    const page = await openPage()
    await employeeLogin(page, admin)

    await openChildPlacements(page, childId)
    await page.findByDataQa('create-new-placement-button').click()

    const modal = new Modal(page.findByDataQa('modal'))
    const unitSelect = new Combobox(modal.find('[data-qa="unit-select"]'))
    await unitSelect.fillAndSelectFirst(unitName)
    await modal.findByDataQa('create-placement-end-date').assertTextEquals('')
    await modal.submitButton.assertDisabled(true)
  })

  test('placement create dialog shows errors on dates outside preschool and extended terms', async () => {
    const preschoolTerms = await preschoolTerm2023.save()

    const admin = await Fixture.employee().admin().save()
    const area = await Fixture.careArea().save()
    const unit = await Fixture.daycare({ areaId: area.id }).save()
    const unitName = unit.name
    const child = await Fixture.person().saveChild({ updateMockVtj: true })
    const childId = child.id

    const page = await openPage()
    await employeeLogin(page, admin)

    await openChildPlacements(page, childId)
    await page.findByDataQa('create-new-placement-button').click()

    const modal = new Modal(page.findByDataQa('modal'))

    const placementTypeSelect = new Select(
      modal.find('[data-qa="placement-type-select"]')
    )
    await placementTypeSelect.selectOption('PRESCHOOL')

    const unitSelect = new Combobox(modal.find('[data-qa="unit-select"]'))
    await unitSelect.fillAndSelectFirst(unitName)
    const start = new DatePicker(
      modal.findByDataQa('create-placement-start-date')
    )
    await start.click()
    await start.fill(preschoolTerms.finnishPreschool.start)

    const end = new DatePicker(modal.findByDataQa('create-placement-end-date'))
    await end.fill(preschoolTerms.finnishPreschool.end)

    await new Checkbox(modal.findByDataQa('confirm-retroactive')).check()

    await modal.submitButton.assertDisabled(false)

    // Placement starts a day before the term
    await start.fill(preschoolTerms.finnishPreschool.start.subDays(1))
    await modal
      .findText('Sijoituksen tulee olla esiopetuskaudella')
      .waitUntilVisible()
    await modal.submitButton.assertDisabled(true)

    // A day before preschool term the extended term is valid so placement can be created
    await placementTypeSelect.selectOption('PRESCHOOL_DAYCARE')
    await modal.submitButton.assertDisabled(false)

    // Placement starts a day before the term so it is invalid
    await start.fill(preschoolTerms.extendedTerm.start.subDays(1))
    await modal
      .findText('Sijoituksen tulee olla esiopetuskaudella')
      .waitUntilVisible()
    await modal.submitButton.assertDisabled(true)

    // Create a valid placement
    await start.fill(preschoolTerms.extendedTerm.start)
    await modal.submitButton.click()

    const placements = page.findByDataQa('child-placements-collapsible')
    await placements.findByDataQa('btn-edit-placement').click()

    const editedStart = new DatePicker(
      placements.findByDataQa('placement-start-date-input')
    )
    await editedStart.fill(preschoolTerms.extendedTerm.start.subDays(1))

    await placements
      .findText('Sijoituksen tulee olla esiopetuskaudella')
      .waitUntilVisible()
  })

  async function openPage() {
    return await Page.open({
      mockedTime,
      employeeCustomizations: { featureFlags: { placementGuarantee: false } }
    })
  }
})
