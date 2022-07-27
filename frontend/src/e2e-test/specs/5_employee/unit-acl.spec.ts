// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { UUID } from 'lib-common/types'

import { resetDatabase } from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import { daycareFixture, Fixture, uuidv4 } from '../../dev-api/fixtures'
import type { EmployeeDetail } from '../../dev-api/types'
import { UnitPage } from '../../pages/employee/units/unit'
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
let admin: EmployeeDetail

beforeEach(async () => {
  await resetDatabase()

  const fixtures = await initializeAreaAndPersonData()
  daycareId = fixtures.daycareFixture.id

  await Fixture.employeeUnitSupervisor(daycareFixture.id).with(esko).save()
  await Fixture.employee().with(pete).save()
  await Fixture.employee().with(yrjo).save()
  admin = (await Fixture.employeeAdmin().save()).data

  page = await Page.open()
})

const expectedAclRows = {
  esko: { id: esko.id, name: 'Esko Esimies', email: 'esko@evaka.test' },
  pete: { id: pete.id, name: 'Pete Päiväkoti', email: 'pete@evaka.test' },
  yrjo: { id: yrjo.id, name: 'Yrjö Yksikkö', email: 'yrjo@evaka.test' }
}

describe('Employee - unit ACL', () => {
  test('Unit supervisors can be added/deleted', async () => {
    await employeeLogin(page, admin)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()
    await unitInfoPage.supervisorAcl.assertRows([expectedAclRows.esko])

    await unitInfoPage.supervisorAcl.addAcl(yrjo.email)
    await unitInfoPage.supervisorAcl.assertRows([
      expectedAclRows.esko,
      expectedAclRows.yrjo
    ])

    await unitInfoPage.supervisorAcl.addAcl(pete.email)
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

    await unitInfoPage.specialEducationTeacherAcl.addAcl(pete.email)
    await unitInfoPage.specialEducationTeacherAcl.assertRows([
      expectedAclRows.pete
    ])

    await unitInfoPage.specialEducationTeacherAcl.addAcl(yrjo.email)
    await unitInfoPage.specialEducationTeacherAcl.assertRows([
      expectedAclRows.pete,
      expectedAclRows.yrjo
    ])

    await unitInfoPage.specialEducationTeacherAcl.deleteAcl(pete.id)
    await unitInfoPage.specialEducationTeacherAcl.deleteAcl(yrjo.id)

    await unitInfoPage.specialEducationTeacherAcl.assertRows([])
  })

  test('Staff can be added and assigned/removed to/from groups', async () => {
    async function toggleGroups() {
      const row = unitInfoPage.staffAcl.getRow(pete.id)
      const rowEditor = await row.edit()
      await rowEditor.toggleStaffGroups([groupId])
      await rowEditor.save()
    }

    await Fixture.daycareGroup()
      .with({ id: groupId, daycareId, name: 'Testailijat' })
      .save()

    await employeeLogin(page, esko)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    const unitInfoPage = await unitPage.openUnitInformation()
    await unitInfoPage.staffAcl.addAcl(expectedAclRows.pete.email)

    await unitInfoPage.staffAcl.assertRows([
      { ...expectedAclRows.pete, groups: [] }
    ])
    await toggleGroups()
    await unitInfoPage.staffAcl.assertRows([
      { ...expectedAclRows.pete, groups: ['Testailijat'] }
    ])
    await toggleGroups()
    await unitInfoPage.staffAcl.assertRows([
      { ...expectedAclRows.pete, groups: [] }
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
