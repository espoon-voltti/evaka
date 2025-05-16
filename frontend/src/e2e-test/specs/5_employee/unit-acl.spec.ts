// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { DaycareId, GroupId } from 'lib-common/generated/api-types/shared'
import { randomId } from 'lib-common/id-type'
import type { UUID } from 'lib-common/types'

import { testDaycare, Fixture, testCareArea } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import type { DevEmployee } from '../../generated/api-types'
import type { AclRole, UnitInfoPage } from '../../pages/employee/units/unit'
import { UnitPage } from '../../pages/employee/units/unit'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let daycareId: DaycareId
const groupId = randomId<GroupId>()

const eskoEmail = 'esko@evaka.test'
const esko = Fixture.employee({
  firstName: 'Esko',
  lastName: 'Esimies',
  email: eskoEmail
})
const peteEmail = 'pete@evaka.test'
const pete = Fixture.employee({
  firstName: 'Pete',
  lastName: 'Päiväkoti',
  email: peteEmail
})
const yrjoEmail = 'yrjo@evaka.test'
const yrjo = Fixture.employee({
  firstName: 'Yrjö',
  lastName: 'Yksikkö',
  email: yrjoEmail
})
let admin: DevEmployee

beforeEach(async () => {
  await resetServiceState()

  await testCareArea.save()
  await testDaycare.save()
  daycareId = testDaycare.id

  await esko.unitSupervisor(testDaycare.id).save()
  await pete.save()
  await yrjo.save()
  admin = await Fixture.employee().admin().save()
})

const eskoRow = (role: AclRole) => ({
  id: esko.id,
  name: 'Esko Esimies',
  email: 'esko@evaka.test',
  role,
  groups: [],
  occupancyCoefficient: false
})

const peteRow = (
  role: AclRole,
  groups: string[] = [],
  occupancyCoefficient = false
) => ({
  id: pete.id,
  name: 'Pete Päiväkoti',
  email: 'pete@evaka.test',
  role,
  groups,
  occupancyCoefficient
})

const yrjoRow = (
  role: AclRole,
  groups: string[] = [],
  occupancyCoefficient = false
) => ({
  id: yrjo.id,
  name: 'Yrjö Yksikkö',
  email: 'yrjo@evaka.test',
  role,
  groups,
  occupancyCoefficient
})

describe('Employee - unit ACL', () => {
  beforeEach(async () => {
    page = await Page.open()
  })

  test('Unit supervisors can be added/deleted', async () => {
    await employeeLogin(page, admin)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()
    await unitInfoPage.activeAcl.assertRows([eskoRow('Johtaja')])

    await unitInfoPage.activeAcl.addAcl('Johtaja', yrjoEmail, [], false)
    await unitInfoPage.activeAcl.assertRows([
      eskoRow('Johtaja'),
      yrjoRow('Johtaja')
    ])

    await unitInfoPage.activeAcl.addAcl('Johtaja', peteEmail, [], true)
    await unitInfoPage.activeAcl.assertRows([
      eskoRow('Johtaja'),
      yrjoRow('Johtaja'),
      peteRow('Johtaja', [], true)
    ])

    await unitInfoPage.activeAcl.deleteAcl(yrjo.id)
    await unitInfoPage.activeAcl.assertRows([
      eskoRow('Johtaja'),
      peteRow('Johtaja', [], true)
    ])

    await unitInfoPage.activeAcl.deleteAcl(esko.id)
    await unitInfoPage.activeAcl.assertRows([peteRow('Johtaja', [], true)])
  })

  test('User can add and delete special education teachers', async () => {
    await employeeLogin(page, admin)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()
    await unitInfoPage.activeAcl.assertRows([eskoRow('Johtaja')])

    await unitInfoPage.activeAcl.addAcl('Erityisopettaja', peteEmail, [], true)
    await unitInfoPage.activeAcl.assertRows([
      eskoRow('Johtaja'),
      peteRow('Erityisopettaja', [], true)
    ])

    await unitInfoPage.activeAcl.addAcl('Erityisopettaja', yrjoEmail, [], false)
    await unitInfoPage.activeAcl.assertRows([
      eskoRow('Johtaja'),
      peteRow('Erityisopettaja', [], true),
      yrjoRow('Erityisopettaja')
    ])

    await unitInfoPage.activeAcl.deleteAcl(pete.id)
    await unitInfoPage.activeAcl.deleteAcl(yrjo.id)

    await unitInfoPage.activeAcl.assertRows([eskoRow('Johtaja')])
  })

  test('Staff can be added and assigned/removed to/from groups', async () => {
    async function toggleGroups(groups: UUID[]) {
      const row = unitInfoPage.activeAcl.getRow(pete.id)
      await row.edit()
      const editModal = unitInfoPage.activeAcl.editModal
      await editModal.toggleGroups(groups)
      await editModal.submit()
    }

    await Fixture.daycareGroup({
      id: groupId,
      daycareId,
      name: 'Testailijat'
    }).save()

    await employeeLogin(page, esko)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()
    await unitInfoPage.activeAcl.addAcl(
      'Henkilökunta',
      peteEmail,
      [groupId],
      true
    )

    await unitInfoPage.activeAcl.assertRows([
      eskoRow('Johtaja'),
      peteRow('Henkilökunta', ['Testailijat'], true)
    ])
    await toggleGroups([groupId])
    await unitInfoPage.activeAcl.assertRows([
      eskoRow('Johtaja'),
      peteRow('Henkilökunta', [], true)
    ])
    await toggleGroups([groupId])
    await unitInfoPage.activeAcl.assertRows([
      eskoRow('Johtaja'),
      peteRow('Henkilökunta', ['Testailijat'], true)
    ])
  })

  test('Staff member coefficient can be set on and off', async () => {
    async function setCoefficient(coefficientValue: boolean) {
      const row = unitInfoPage.activeAcl.getRow(pete.id)
      await row.edit()
      const editModal = unitInfoPage.activeAcl.editModal
      await (coefficientValue
        ? editModal.coefficientCheckbox.check()
        : editModal.coefficientCheckbox.uncheck())
      await editModal.submit()
    }

    await employeeLogin(page, esko)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()
    await unitInfoPage.activeAcl.addAcl('Henkilökunta', peteEmail, [], true)

    await unitInfoPage.activeAcl.assertRows([
      eskoRow('Johtaja'),
      peteRow('Henkilökunta', [], true)
    ])

    await setCoefficient(false)
    await unitInfoPage.activeAcl.assertRows([
      eskoRow('Johtaja'),
      peteRow('Henkilökunta', [], false)
    ])

    await setCoefficient(true)
    await unitInfoPage.activeAcl.assertRows([
      eskoRow('Johtaja'),
      peteRow('Henkilökunta', [], true)
    ])
  })

  test('User can add a mobile device unit side', async () => {
    await employeeLogin(page, admin)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()

    await unitInfoPage.mobileAcl.addMobileDevice('Testilaite')
    await unitInfoPage.mobileAcl.assertDeviceExists('Testilaite')
  })
})

describe('Employee - unit ACL - temporary employee', () => {
  beforeEach(async () => {
    page = await Page.open()
  })

  async function openEditModalByIndex(
    unitInfoPage: UnitInfoPage,
    index: number
  ) {
    const row = unitInfoPage.temporaryEmployees.getRowByIndex(index)
    await row.edit()
    return unitInfoPage.temporaryEmployees.editModal
  }

  test('Temporary staff member name can be changed', async () => {
    await employeeLogin(page, admin)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()

    await unitInfoPage.temporaryEmployees.addTemporaryAcl(
      'Salli',
      'Sijainen',
      [],
      '2394'
    )
    await unitInfoPage.temporaryEmployees.assertRowsExactly([
      {
        name: 'Salli Sijainen',
        groups: [],
        occupancyCoefficient: false
      }
    ])

    const editModal = await openEditModalByIndex(unitInfoPage, 0)
    await editModal.employeeFirstName.fill('Väiski')
    await editModal.employeeLastName.fill('Väliaikainen')
    await editModal.submit()
    await unitInfoPage.temporaryEmployees.assertRowsExactly([
      {
        name: 'Väiski Väliaikainen',
        groups: [],
        occupancyCoefficient: false
      }
    ])
  })

  test('Temporary staff can be added and assigned/removed to/from groups', async () => {
    await Fixture.daycareGroup({
      id: groupId,
      daycareId,
      name: 'Testailijat'
    }).save()

    await employeeLogin(page, admin)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()

    await unitInfoPage.temporaryEmployees.addTemporaryAcl(
      'Salli',
      'Sijainen',
      [groupId],
      '2394'
    )
    await unitInfoPage.temporaryEmployees.assertRowsExactly([
      {
        name: 'Salli Sijainen',
        groups: ['Testailijat'],
        occupancyCoefficient: false
      }
    ])

    const editModal1 = await openEditModalByIndex(unitInfoPage, 0)
    await editModal1.toggleGroups([groupId])
    await editModal1.submit()
    await unitInfoPage.temporaryEmployees.assertRowsExactly([
      {
        name: 'Salli Sijainen',
        groups: [],
        occupancyCoefficient: false
      }
    ])

    const editModal2 = await openEditModalByIndex(unitInfoPage, 0)
    await editModal2.toggleGroups([groupId])
    await editModal2.submit()
    await unitInfoPage.temporaryEmployees.assertRowsExactly([
      {
        name: 'Salli Sijainen',
        groups: ['Testailijat'],
        occupancyCoefficient: false
      }
    ])
  })

  test('Temporary staff member coefficient can be set on and off', async () => {
    await employeeLogin(page, admin)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()

    await unitInfoPage.temporaryEmployees.addTemporaryAcl(
      'Salli',
      'Sijainen',
      [],
      '2394'
    )
    await unitInfoPage.temporaryEmployees.assertRowsExactly([
      {
        name: 'Salli Sijainen',
        groups: [],
        occupancyCoefficient: false
      }
    ])

    const editModal1 = await openEditModalByIndex(unitInfoPage, 0)
    await editModal1.coefficientCheckbox.check()
    await editModal1.submit()
    await unitInfoPage.temporaryEmployees.assertRowsExactly([
      {
        name: 'Salli Sijainen',
        groups: [],
        occupancyCoefficient: true
      }
    ])

    const editModal2 = await openEditModalByIndex(unitInfoPage, 0)
    await editModal2.coefficientCheckbox.uncheck()
    await editModal2.submit()
    await unitInfoPage.temporaryEmployees.assertRowsExactly([
      {
        name: 'Salli Sijainen',
        groups: [],
        occupancyCoefficient: false
      }
    ])
  })

  test('Temporary staff member pin code can be changed', async () => {
    await employeeLogin(page, admin)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()

    await unitInfoPage.temporaryEmployees.addTemporaryAcl(
      'Salli',
      'Sijainen',
      [],
      '2394'
    )
    await unitInfoPage.temporaryEmployees.assertRowsExactly([
      {
        name: 'Salli Sijainen',
        groups: [],
        occupancyCoefficient: false
      }
    ])

    const editModal1 = await openEditModalByIndex(unitInfoPage, 0)
    await editModal1.pinCode.fill('9328')
    await unitInfoPage.temporaryEmployees.assertRowsExactly([
      {
        name: 'Salli Sijainen',
        groups: [],
        occupancyCoefficient: false
      }
    ])
    // current pin code is not shown in the UI so not easy to assert
  })

  test('Multiple temporary staff members can be added', async () => {
    await employeeLogin(page, admin)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()

    await unitInfoPage.temporaryEmployees.addTemporaryAcl(
      'Salli',
      'Sijainen',
      [],
      '2394'
    )
    await unitInfoPage.temporaryEmployees.addTemporaryAcl(
      'Väiski',
      'Väliaikainen',
      [],
      '9327'
    )
    await unitInfoPage.temporaryEmployees.assertRowsExactly([
      {
        name: 'Salli Sijainen',
        groups: [],
        occupancyCoefficient: false
      },
      {
        name: 'Väiski Väliaikainen',
        groups: [],
        occupancyCoefficient: false
      }
    ])
  })

  test('Temporary staff member can be deleted', async () => {
    await Fixture.daycareGroup({
      id: groupId,
      daycareId,
      name: 'Testailijat'
    }).save()

    await employeeLogin(page, admin)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()
    await unitInfoPage.temporaryEmployees.addTemporaryAcl(
      'Salli',
      'Sijainen',
      [groupId],
      '2394'
    )

    await unitInfoPage.temporaryEmployees.previousTemporaryEmployeeTableRows.assertCount(
      0
    )
    await unitInfoPage.temporaryEmployees.softDeleteTemporaryEmployeeByIndex(0)
    await unitInfoPage.temporaryEmployees.assertRowsExactly([])
    await unitInfoPage.temporaryEmployees.previousTemporaryEmployeeTableRows.assertCount(
      1
    )

    await unitInfoPage.temporaryEmployees.hardDeleteTemporaryEmployeeByIndex(0)
    await unitInfoPage.temporaryEmployees.assertRowsExactly([])
    await unitInfoPage.temporaryEmployees.previousTemporaryEmployeeTableRows.assertCount(
      0
    )
  })

  test('Previous temporary staff member can be reactivated', async () => {
    await Fixture.daycareGroup({
      id: groupId,
      daycareId,
      name: 'Testailijat'
    }).save()

    await employeeLogin(page, admin)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()
    await unitInfoPage.temporaryEmployees.addTemporaryAcl(
      'Salli',
      'Sijainen',
      [groupId],
      '2394'
    )

    await unitInfoPage.temporaryEmployees.previousTemporaryEmployeeTableRows.assertCount(
      0
    )
    await unitInfoPage.temporaryEmployees.softDeleteTemporaryEmployeeByIndex(0)
    await unitInfoPage.temporaryEmployees.assertRowsExactly([])
    await unitInfoPage.temporaryEmployees.previousTemporaryEmployeeTableRows.assertCount(
      1
    )

    await unitInfoPage.temporaryEmployees.reactivateTemporaryEmployeeByIndex(0)
    await unitInfoPage.temporaryEmployees.assertRowsExactly([
      {
        name: 'Salli Sijainen',
        groups: [],
        occupancyCoefficient: false
      }
    ])
    await unitInfoPage.temporaryEmployees.previousTemporaryEmployeeTableRows.assertCount(
      0
    )
  })
})
