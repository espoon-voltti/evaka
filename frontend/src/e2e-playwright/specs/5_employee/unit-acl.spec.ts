// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UnitPage } from 'e2e-playwright/pages/employee/units/unit'
import { employeeLogin } from 'e2e-playwright/utils/user'
import { resetDatabase } from 'e2e-test-common/dev-api'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import {
  daycareFixture,
  Fixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import { EmployeeDetail } from 'e2e-test-common/dev-api/types'
import { UUID } from 'lib-common/types'
import { Page } from '../../utils/page'

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
    await unitPage.supervisorAcl.assertRows([expectedAclRows.esko])

    await unitPage.supervisorAcl.addAcl(yrjo.email)
    await unitPage.supervisorAcl.assertRows([
      expectedAclRows.esko,
      expectedAclRows.yrjo
    ])

    await unitPage.supervisorAcl.addAcl(pete.email)
    await unitPage.supervisorAcl.assertRows([
      expectedAclRows.esko,
      expectedAclRows.yrjo,
      expectedAclRows.pete
    ])

    await unitPage.supervisorAcl.deleteAcl(yrjo.id)
    await unitPage.supervisorAcl.assertRows([
      expectedAclRows.esko,
      expectedAclRows.pete
    ])

    await unitPage.supervisorAcl.deleteAcl(esko.id)
    await unitPage.supervisorAcl.assertRows([expectedAclRows.pete])
  })

  test('User can add and delete special education teachers', async () => {
    await employeeLogin(page, admin)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    await unitPage.supervisorAcl.assertRows([expectedAclRows.esko])

    await unitPage.specialEducationTeacherAcl.assertRows([])

    await unitPage.specialEducationTeacherAcl.addAcl(pete.email)
    await unitPage.specialEducationTeacherAcl.assertRows([expectedAclRows.pete])

    await unitPage.specialEducationTeacherAcl.addAcl(yrjo.email)
    await unitPage.specialEducationTeacherAcl.assertRows([
      expectedAclRows.pete,
      expectedAclRows.yrjo
    ])

    await unitPage.specialEducationTeacherAcl.deleteAcl(pete.id)
    await unitPage.specialEducationTeacherAcl.deleteAcl(yrjo.id)

    await unitPage.specialEducationTeacherAcl.assertRows([])
  })

  test('Staff can be added and assigned/removed to/from groups', async () => {
    async function toggleGroups() {
      const row = unitPage.staffAcl.getRow(pete.id)
      const rowEditor = await row.edit()
      await rowEditor.toggleStaffGroups([groupId])
      await rowEditor.save()
    }

    await Fixture.daycareGroup()
      .with({ id: groupId, daycareId, name: 'Testailijat' })
      .save()

    await employeeLogin(page, esko)
    const unitPage = await UnitPage.openUnit(page, daycareId)
    await unitPage.staffAcl.addAcl(expectedAclRows.pete.email)

    await unitPage.staffAcl.assertRows([
      { ...expectedAclRows.pete, groups: [] }
    ])
    await toggleGroups()
    await unitPage.staffAcl.assertRows([
      { ...expectedAclRows.pete, groups: ['Testailijat'] }
    ])
    await toggleGroups()
    await unitPage.staffAcl.assertRows([
      { ...expectedAclRows.pete, groups: [] }
    ])
  })

  test('User can add a mobile device unit side', async () => {
    await employeeLogin(page, admin)
    const unitPage = await UnitPage.openUnit(page, daycareId)

    await unitPage.mobileAcl.addMobileDevice('Testilaite')
    await unitPage.mobileAcl.assertDeviceExists('Testilaite')
  })
})
