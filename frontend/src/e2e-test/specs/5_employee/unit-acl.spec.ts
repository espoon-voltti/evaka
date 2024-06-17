// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'

import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import { daycareFixture, Fixture, uuidv4 } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import { DevEmployee } from '../../generated/api-types'
import {
  EmployeeRowEditModal,
  UnitInfoPage,
  UnitPage
} from '../../pages/employee/units/unit'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let daycareId: UUID
const groupId: UUID = uuidv4()

const eskoId = uuidv4()
const esko = {
  id: eskoId,
  externalId: `espoo-ad:${eskoId}`,
  firstName: 'Esko',
  lastName: 'Esimies',
  email: 'esko@evaka.test'
}
const pete = {
  id: uuidv4(),
  firstName: 'Pete',
  lastName: 'Päiväkoti',
  email: 'pete@evaka.test'
}
const yrjo = {
  id: uuidv4(),
  firstName: 'Yrjö',
  lastName: 'Yksikkö',
  email: 'yrjo@evaka.test'
}
let admin: DevEmployee

beforeEach(async () => {
  await resetServiceState()

  const fixtures = await initializeAreaAndPersonData()
  daycareId = fixtures.daycareFixture.id

  await Fixture.employeeUnitSupervisor(daycareFixture.id).with(esko).save()
  await Fixture.employee().with(pete).save()
  await Fixture.employee().with(yrjo).save()
  admin = (await Fixture.employeeAdmin().save()).data
})

const expectedAclRows = {
  esko: {
    id: esko.id,
    name: 'Esko Esimies',
    email: 'esko@evaka.test',
    groups: [],
    occupancyCoefficient: false
  },
  pete: {
    id: pete.id,
    name: 'Pete Päiväkoti',
    email: 'pete@evaka.test',
    groups: [],
    occupancyCoefficient: true
  },
  yrjo: {
    id: yrjo.id,
    name: 'Yrjö Yksikkö',
    email: 'yrjo@evaka.test',
    groups: [],
    occupancyCoefficient: false
  }
}

describe('Employee - unit ACL', () => {
  beforeEach(async () => {
    page = await Page.open()
  })

  test('Unit supervisors can be added/deleted', async () => {
    await employeeLogin(page, admin)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()
    await unitInfoPage.supervisorAcl.assertRows([expectedAclRows.esko])

    await unitInfoPage.supervisorAcl.addAcl(yrjo.email, [], false)
    await unitInfoPage.supervisorAcl.assertRows([
      expectedAclRows.esko,
      expectedAclRows.yrjo
    ])

    await unitInfoPage.supervisorAcl.addAcl(pete.email, [], true)
    await unitInfoPage.supervisorAcl.assertRows([
      expectedAclRows.esko,
      expectedAclRows.yrjo,
      expectedAclRows.pete
    ])

    await unitInfoPage.supervisorAcl.deleteAcl(yrjo.id)
    await unitInfoPage.supervisorAcl.assertRows([
      expectedAclRows.esko,
      expectedAclRows.pete
    ])

    await unitInfoPage.supervisorAcl.deleteAcl(esko.id)
    await unitInfoPage.supervisorAcl.assertRows([expectedAclRows.pete])
  })

  test('User can add and delete special education teachers', async () => {
    await employeeLogin(page, admin)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()
    await unitInfoPage.supervisorAcl.assertRows([expectedAclRows.esko])

    await unitInfoPage.specialEducationTeacherAcl.assertRows([])

    await unitInfoPage.specialEducationTeacherAcl.addAcl(pete.email, [], true)
    await unitInfoPage.specialEducationTeacherAcl.assertRows([
      expectedAclRows.pete
    ])

    await unitInfoPage.specialEducationTeacherAcl.addAcl(yrjo.email, [], false)
    await unitInfoPage.specialEducationTeacherAcl.assertRows([
      expectedAclRows.pete,
      expectedAclRows.yrjo
    ])

    await unitInfoPage.specialEducationTeacherAcl.deleteAcl(pete.id)
    await unitInfoPage.specialEducationTeacherAcl.deleteAcl(yrjo.id)

    await unitInfoPage.specialEducationTeacherAcl.assertRows([])
  })

  test('Staff can be added and assigned/removed to/from groups', async () => {
    async function toggleGroups(page: Page, groups: UUID[]) {
      const row = unitInfoPage.staffAcl.getRow(pete.id)
      await row.edit()
      const editModal = new EmployeeRowEditModal(
        page.findByDataQa('employee-row-edit-person-modal')
      )
      await editModal.toggleGroups(groups)
      await editModal.saveButton.click()
    }

    await Fixture.daycareGroup()
      .with({ id: groupId, daycareId, name: 'Testailijat' })
      .save()

    await employeeLogin(page, esko)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()
    await unitInfoPage.staffAcl.addAcl(
      expectedAclRows.pete.email,
      [groupId],
      true
    )

    await unitInfoPage.staffAcl.assertRows([
      { ...expectedAclRows.pete, groups: ['Testailijat'] }
    ])

    await toggleGroups(page, [groupId])
    await unitInfoPage.staffAcl.assertRows([
      { ...expectedAclRows.pete, groups: [] }
    ])
    await toggleGroups(page, [groupId])
    await unitInfoPage.staffAcl.assertRows([
      { ...expectedAclRows.pete, groups: ['Testailijat'] }
    ])
  })

  test('Staff member coefficient can be set on and off', async () => {
    async function setCoefficient(page: Page, coefficientValue: boolean) {
      const row = unitInfoPage.staffAcl.getRow(pete.id)
      await row.edit()
      const editModal = new EmployeeRowEditModal(
        page.findByDataQa('employee-row-edit-person-modal')
      )
      await editModal.setCoefficient(coefficientValue)
      await editModal.saveButton.click()
    }

    await employeeLogin(page, esko)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()
    await unitInfoPage.staffAcl.addAcl(expectedAclRows.pete.email, [], true)

    await unitInfoPage.staffAcl.assertRows([expectedAclRows.pete])

    await setCoefficient(page, false)
    await unitInfoPage.staffAcl.assertRows([
      { ...expectedAclRows.pete, occupancyCoefficient: false }
    ])

    await setCoefficient(page, true)
    await unitInfoPage.staffAcl.assertRows([expectedAclRows.pete])
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

  test('Temporary employee selector is hidden in supervisor acl', async () => {
    await employeeLogin(page, admin)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()

    await unitInfoPage.supervisorAcl.assertTemporaryEmployeeHidden()
  })

  test('Temporary employee selector is hidden in special education teacher acl', async () => {
    await employeeLogin(page, admin)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()

    await unitInfoPage.specialEducationTeacherAcl.assertTemporaryEmployeeHidden()
  })

  test('Temporary employee selector is hidden in early childhood education secretary acl', async () => {
    await employeeLogin(page, admin)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()

    await unitInfoPage.earlyChildhoodEducationSecretary.assertTemporaryEmployeeHidden()
  })

  test('Temporary employee selector is visible in staff acl', async () => {
    await employeeLogin(page, admin)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()

    await unitInfoPage.staffAcl.assertTemporaryEmployeeVisible()
  })

  async function openEditModalByIndex(
    unitInfoPage: UnitInfoPage,
    index: number
  ) {
    const row = unitInfoPage.staffAcl.getRowByIndex(index)
    await row.edit()
    return new EmployeeRowEditModal(
      page.findByDataQa('employee-row-edit-person-modal')
    )
  }

  test('Temporary staff member name can be changed', async () => {
    await employeeLogin(page, admin)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()

    await unitInfoPage.staffAcl.addTemporaryAcl('Salli', 'Sijainen', [], '2394')
    await unitInfoPage.staffAcl.assertRowsExactly([
      {
        name: 'Salli Sijainen',
        email: '',
        groups: [],
        occupancyCoefficient: false
      }
    ])

    const editModal = await openEditModalByIndex(unitInfoPage, 0)
    await editModal.submitWithTemporaryEmployee({
      firstName: 'Väiski',
      lastName: 'Väliaikainen'
    })
    await unitInfoPage.staffAcl.assertRowsExactly([
      {
        name: 'Väiski Väliaikainen',
        email: '',
        groups: [],
        occupancyCoefficient: false
      }
    ])
  })

  test('Temporary staff can be added and assigned/removed to/from groups', async () => {
    await Fixture.daycareGroup()
      .with({ id: groupId, daycareId, name: 'Testailijat' })
      .save()

    await employeeLogin(page, admin)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()

    await unitInfoPage.staffAcl.addTemporaryAcl(
      'Salli',
      'Sijainen',
      [groupId],
      '2394'
    )
    await unitInfoPage.staffAcl.assertRowsExactly([
      {
        name: 'Salli Sijainen',
        email: '',
        groups: ['Testailijat'],
        occupancyCoefficient: false
      }
    ])

    const editModal1 = await openEditModalByIndex(unitInfoPage, 0)
    await editModal1.submitWithTemporaryEmployee({
      toggleGroups: [groupId]
    })
    await unitInfoPage.staffAcl.assertRowsExactly([
      {
        name: 'Salli Sijainen',
        email: '',
        groups: [],
        occupancyCoefficient: false
      }
    ])

    const editModal2 = await openEditModalByIndex(unitInfoPage, 0)
    await editModal2.submitWithTemporaryEmployee({
      toggleGroups: [groupId]
    })
    await unitInfoPage.staffAcl.assertRowsExactly([
      {
        name: 'Salli Sijainen',
        email: '',
        groups: ['Testailijat'],
        occupancyCoefficient: false
      }
    ])
  })

  test('Temporary staff member coefficient can be set on and off', async () => {
    await employeeLogin(page, admin)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()

    await unitInfoPage.staffAcl.addTemporaryAcl('Salli', 'Sijainen', [], '2394')
    await unitInfoPage.staffAcl.assertRowsExactly([
      {
        name: 'Salli Sijainen',
        email: '',
        groups: [],
        occupancyCoefficient: false
      }
    ])

    const editModal1 = await openEditModalByIndex(unitInfoPage, 0)
    await editModal1.submitWithTemporaryEmployee({
      coefficient: true
    })
    await unitInfoPage.staffAcl.assertRowsExactly([
      {
        name: 'Salli Sijainen',
        email: '',
        groups: [],
        occupancyCoefficient: true
      }
    ])

    const editModal2 = await openEditModalByIndex(unitInfoPage, 0)
    await editModal2.submitWithTemporaryEmployee({
      coefficient: false
    })
    await unitInfoPage.staffAcl.assertRowsExactly([
      {
        name: 'Salli Sijainen',
        email: '',
        groups: [],
        occupancyCoefficient: false
      }
    ])
  })

  test('Temporary staff member pin code can be changed', async () => {
    await employeeLogin(page, admin)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()

    await unitInfoPage.staffAcl.addTemporaryAcl('Salli', 'Sijainen', [], '2394')
    await unitInfoPage.staffAcl.assertRowsExactly([
      {
        name: 'Salli Sijainen',
        email: '',
        groups: [],
        occupancyCoefficient: false
      }
    ])

    const editModal1 = await openEditModalByIndex(unitInfoPage, 0)
    await editModal1.submitWithTemporaryEmployee({
      pinCode: '9328'
    })
    await unitInfoPage.staffAcl.assertRowsExactly([
      {
        name: 'Salli Sijainen',
        email: '',
        groups: [],
        occupancyCoefficient: false
      }
    ])
    const editModal2 = await openEditModalByIndex(unitInfoPage, 0)
    await editModal2.assertPinCode('9328')
  })

  test('Multiple temporary staff members can be added', async () => {
    await employeeLogin(page, admin)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()

    await unitInfoPage.staffAcl.addTemporaryAcl('Salli', 'Sijainen', [], '2394')
    await unitInfoPage.staffAcl.addTemporaryAcl(
      'Väiski',
      'Väliaikainen',
      [],
      '9327'
    )
    await unitInfoPage.staffAcl.assertRowsExactly([
      {
        name: 'Salli Sijainen',
        email: '',
        groups: [],
        occupancyCoefficient: false
      },
      {
        name: 'Väiski Väliaikainen',
        email: '',
        groups: [],
        occupancyCoefficient: false
      }
    ])
  })

  test('Temporary staff member can be deleted', async () => {
    await Fixture.daycareGroup()
      .with({ id: groupId, daycareId, name: 'Testailijat' })
      .save()

    await employeeLogin(page, admin)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()
    await unitInfoPage.staffAcl.addTemporaryAcl(
      'Salli',
      'Sijainen',
      [groupId],
      '2394'
    )

    await unitInfoPage.staffAcl.deleteAclByIndex(0)
    await unitInfoPage.staffAcl.assertRowsExactly([])
    await unitInfoPage.staffAcl.assertPreviousTemporaryEmployeeRowsExactly([
      {
        name: 'Salli Sijainen',
        email: '',
        groups: [],
        occupancyCoefficient: false
      }
    ])

    await unitInfoPage.staffAcl.deleteTemporaryEmployeeByIndex(0)
    await unitInfoPage.staffAcl.assertRowsExactly([])
    await unitInfoPage.staffAcl.assertPreviousTemporaryEmployeeRowsExactly([])
  })
})
