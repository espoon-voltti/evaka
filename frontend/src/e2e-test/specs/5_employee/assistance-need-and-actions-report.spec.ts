// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import type {
  DaycareId,
  GroupId,
  PersonId
} from 'lib-common/generated/api-types/shared'
import { evakaUserId, randomId } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import {
  testDaycareGroup,
  Fixture,
  familyWithTwoGuardians,
  testCareArea,
  testDaycare
} from '../../dev-api/fixtures'
import {
  createDaycareGroups,
  createDefaultServiceNeedOptions,
  resetServiceState
} from '../../generated/api-clients'
import type { DevEmployee } from '../../generated/api-types'
import { AssistanceNeedsAndActionsReport } from '../../pages/employee/reports'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let childId: PersonId
let unitId: DaycareId
let admin: DevEmployee

const mockedTime = LocalDate.of(2024, 2, 19)

beforeEach(async () => {
  await resetServiceState()

  await testCareArea.save()
  await testDaycare.save()
  await familyWithTwoGuardians.save()
  await createDefaultServiceNeedOptions()
  await createDaycareGroups({ body: [testDaycareGroup] })

  unitId = testDaycare.id
  childId = familyWithTwoGuardians.children[0].id
  const placement = await Fixture.placement({
    childId,
    unitId: unitId,
    startDate: mockedTime,
    endDate: mockedTime
  }).save()

  await Fixture.groupPlacement({
    daycareGroupId: testDaycareGroup.id,
    daycarePlacementId: placement.id,
    startDate: mockedTime,
    endDate: mockedTime
  }).save()

  page = await Page.open({
    mockedTime: mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0))
  })
  admin = await Fixture.employee().admin().save()
  await employeeLogin(page, admin)
})

describe('Assistance need and actions report', () => {
  test('Shows assistance needs', async () => {
    const validDuring = new FiniteDateRange(mockedTime, mockedTime)

    await Fixture.daycareAssistance({
      childId,
      validDuring
    }).save()

    await Fixture.otherAssistanceMeasure({
      childId,
      validDuring
    }).save()

    await Fixture.assistanceActionOption({
      value: 'ASSISTANCE_SERVICE_CHILD'
    }).save()

    await Fixture.assistanceAction({
      childId,
      modifiedBy: evakaUserId(admin.id),
      startDate: validDuring.start,
      endDate: validDuring.end,
      actions: ['ASSISTANCE_SERVICE_CHILD']
    }).save()

    await Fixture.assistanceNeedVoucherCoefficient({
      childId,
      validityPeriod: new FiniteDateRange(validDuring.start, validDuring.end),
      coefficient: 1.5
    }).save()

    await page.goto(
      `${config.employeeUrl}/reports/assistance-needs-and-actions`
    )
    const report = new AssistanceNeedsAndActionsReport(page)
    await report.needsAndActionsRows
      .nth(0)
      .assertTextEquals(
        'Superkeskus\n' + '\t\t1\t0\t0\t0\t1\t0\t0\t1\t0\t0\t1\t0\t0'
      )
    await report.selectCareAreaFilter('Superkeskus')
    await report.openUnit('Alkuräjähdyksen päiväkoti')
    await report.childRows
      .nth(0)
      .assertTextEquals(
        'Antero Onni Leevi Aatu Högfors\tKosmiset Vakiot\t10\t1\t0\t0\t0\t1\t0\t0\ta test assistance action option\t1.5\t0\t0'
      )
  })
  test('Column filters', async () => {
    await Fixture.assistanceActionOption({
      category: 'DAYCARE',
      value: 'v1',
      nameFi: 'Vaka 1'
    }).save()
    await Fixture.assistanceActionOption({
      category: 'DAYCARE',
      value: 'v2',
      nameFi: 'Vaka 2'
    }).save()
    await Fixture.assistanceActionOption({
      category: 'DAYCARE',
      value: 'v3',
      nameFi: 'Vaka 3'
    }).save()
    await Fixture.assistanceActionOption({
      category: 'PRESCHOOL',
      value: 'e1',
      nameFi: 'Eskari 1'
    }).save()
    await Fixture.assistanceActionOption({
      category: 'PRESCHOOL',
      value: 'e2',
      nameFi: 'Eskari 2'
    }).save()
    await Fixture.assistanceActionOption({
      category: 'PRESCHOOL',
      value: 'e3',
      nameFi: 'Eskari 3'
    }).save()

    await page.goto(
      `${config.employeeUrl}/reports/assistance-needs-and-actions`
    )
    const report = new AssistanceNeedsAndActionsReport(page)

    await report.needsAndActionsHeader.assertTextEquals(
      'TOIMINTAYKSIKÖT ALUEITTAIN\tRYHMÄ\tYLEINEN TUKI, EI PÄÄTÖSTÄ\tYLEINEN TUKI, PÄÄTÖS TUKIPALVELUISTA\tTEHOSTETTU TUKI\tERITYINEN TUKI\tKULJETUSETU (ESIOPPILAILLA KOSKI-TIETO)\tLAPSEN KOTOUTUMISEN TUKI (ELY)\tOPETUKSEN POIKKEAVA ALOITTAMISAJANKOHTA\tVAKA 1\tVAKA 2\tVAKA 3\tMUU TUKITOIMI\tTUKITOIMI PUUTTUU\tKOROTETTU PS-KERROIN\tAKTIIVISET VARHAISKASVATUKSEN TUEN PÄÄTÖKSET\tAKTIIVISET ESIOPETUKSEN TUEN PÄÄTÖKSET'
    )
    await report.daycareAssistanceLevelSelect.fillAndSelectFirst(
      'Tehostettu tuki'
    )
    await report.daycareAssistanceLevelSelect.fillAndSelectFirst('Kuljetusetu')
    await report.assistanceActionOptionSelect.fillAndSelectFirst('Vaka 2')
    await report.needsAndActionsHeader.assertTextEquals(
      'TOIMINTAYKSIKÖT ALUEITTAIN\tRYHMÄ\tTEHOSTETTU TUKI\tKULJETUSETU (ESIOPPILAILLA KOSKI-TIETO)\tVAKA 2\tMUU TUKITOIMI\tTUKITOIMI PUUTTUU\tKOROTETTU PS-KERROIN\tAKTIIVISET VARHAISKASVATUKSEN TUEN PÄÄTÖKSET\tAKTIIVISET ESIOPETUKSEN TUEN PÄÄTÖKSET'
    )

    await report.typeSelect.fillAndSelectFirst('esiopetuksessa')
    await report.preschoolAssistanceLevelSelect.fillAndSelectFirst(
      'Erityinen tuki ilman pidennettyä oppivelvollisuutta'
    )
    await report.preschoolAssistanceLevelSelect.fillAndSelectFirst(
      'Lapsen kotoutumisen tuki'
    )
    await report.needsAndActionsHeader.assertTextEquals(
      'TOIMINTAYKSIKÖT ALUEITTAIN\tRYHMÄ\tERITYINEN TUKI ILMAN PIDENNETTYÄ OPPIVELVOLLISUUTTA\tKULJETUSETU (ESIOPPILAILLA KOSKI-TIETO)\tLAPSEN KOTOUTUMISEN TUKI (ELY)\tESKARI 1\tESKARI 2\tESKARI 3\tMUU TUKITOIMI\tTUKITOIMI PUUTTUU\tKOROTETTU PS-KERROIN\tAKTIIVISET VARHAISKASVATUKSEN TUEN PÄÄTÖKSET\tAKTIIVISET ESIOPETUKSEN TUEN PÄÄTÖKSET'
    )
    await report.assistanceActionOptionSelect.fillAndSelectFirst('Eskari 1')
    await report.assistanceActionOptionSelect.fillAndSelectFirst('Eskari 3')
    await report.needsAndActionsHeader.assertTextEquals(
      'TOIMINTAYKSIKÖT ALUEITTAIN\tRYHMÄ\tERITYINEN TUKI ILMAN PIDENNETTYÄ OPPIVELVOLLISUUTTA\tKULJETUSETU (ESIOPPILAILLA KOSKI-TIETO)\tLAPSEN KOTOUTUMISEN TUKI (ELY)\tESKARI 1\tESKARI 3\tMUU TUKITOIMI\tTUKITOIMI PUUTTUU\tKOROTETTU PS-KERROIN\tAKTIIVISET VARHAISKASVATUKSEN TUEN PÄÄTÖKSET\tAKTIIVISET ESIOPETUKSEN TUEN PÄÄTÖKSET'
    )

    await report.typeSelect.fillAndSelectFirst('varhaiskasvatuksessa')
    await report.needsAndActionsHeader.assertTextEquals(
      'TOIMINTAYKSIKÖT ALUEITTAIN\tRYHMÄ\tTEHOSTETTU TUKI\tKULJETUSETU (ESIOPPILAILLA KOSKI-TIETO)\tLAPSEN KOTOUTUMISEN TUKI (ELY)\tVAKA 2\tMUU TUKITOIMI\tTUKITOIMI PUUTTUU\tKOROTETTU PS-KERROIN\tAKTIIVISET VARHAISKASVATUKSEN TUEN PÄÄTÖKSET\tAKTIIVISET ESIOPETUKSEN TUEN PÄÄTÖKSET'
    )
  })
  test('Counts actions only if child has selected assistance', async () => {
    const validDuring = new FiniteDateRange(mockedTime, mockedTime)

    await Fixture.daycareAssistance({
      childId,
      validDuring,
      level: 'GENERAL_SUPPORT'
    }).save()
    await Fixture.preschoolAssistance({
      childId,
      validDuring,
      level: 'INTENSIFIED_SUPPORT'
    }).save()
    await Fixture.assistanceActionOption({
      category: 'DAYCARE',
      value: 'DAYCARE_ASSISTANCE_SERVICE_CHILD'
    }).save()
    await Fixture.assistanceActionOption({
      category: 'PRESCHOOL',
      value: 'PRESCHOOL_ASSISTANCE_SERVICE_CHILD'
    }).save()
    await Fixture.assistanceAction({
      childId,
      modifiedBy: evakaUserId(admin.id),
      startDate: validDuring.start,
      endDate: validDuring.end,
      actions: [
        'DAYCARE_ASSISTANCE_SERVICE_CHILD',
        'PRESCHOOL_ASSISTANCE_SERVICE_CHILD'
      ]
    }).save()

    await page.goto(
      `${config.employeeUrl}/reports/assistance-needs-and-actions`
    )
    const report = new AssistanceNeedsAndActionsReport(page)

    await report.typeSelect.fillAndSelectFirst('esiopetuksessa')
    await report.needsAndActionsRows
      .nth(0)
      .assertTextEquals(
        'Superkeskus\n' + '\t\t1\t0\t0\t0\t0\t0\t0\t1\t0\t0\t0\t0\t0'
      )
    await report.preschoolAssistanceLevelSelect.fillAndSelectFirst(
      'Tehostettu tuki'
    )
    await report.needsAndActionsRows
      .nth(0)
      .assertTextEquals('Superkeskus\n' + '\t\t1\t1\t0\t0\t0\t0\t0')
    await report.preschoolAssistanceLevelSelect.fillAndSelectFirst(
      'Tehostettu tuki'
    )
    await report.preschoolAssistanceLevelSelect.fillAndSelectFirst(
      'Erityinen tuki ilman pidennettyä oppivelvollisuutta'
    )
    await report.needsAndActionsRows
      .nth(0)
      .assertTextEquals('Superkeskus\n' + '\t\t0\t0\t0\t0\t0\t0\t0')

    await report.typeSelect.fillAndSelectFirst('varhaiskasvatuksessa')
    await report.needsAndActionsRows
      .nth(0)
      .assertTextEquals(
        'Superkeskus\n' + '\t\t1\t0\t0\t0\t0\t0\t0\t1\t0\t0\t0\t0\t0'
      )
    await report.daycareAssistanceLevelSelect.fillAndSelectFirst(
      'Yleinen tuki, ei päätöstä'
    )
    await report.needsAndActionsRows
      .nth(0)
      .assertTextEquals('Superkeskus\n' + '\t\t1\t1\t0\t0\t0\t0\t0')
    await report.daycareAssistanceLevelSelect.fillAndSelectFirst(
      'Yleinen tuki, ei päätöstä'
    )
    await report.daycareAssistanceLevelSelect.fillAndSelectFirst(
      'Yleinen tuki, päätös tukipalveluista'
    )
    await report.needsAndActionsRows
      .nth(0)
      .assertTextEquals('Superkeskus\n' + '\t\t0\t0\t0\t0\t0\t0\t0')
  })
  test('Shows actions only if child has selected assistance', async () => {
    const validDuring = new FiniteDateRange(mockedTime, mockedTime)

    await Fixture.daycareAssistance({
      childId,
      validDuring,
      level: 'GENERAL_SUPPORT'
    }).save()
    await Fixture.preschoolAssistance({
      childId,
      validDuring,
      level: 'INTENSIFIED_SUPPORT'
    }).save()
    await Fixture.assistanceActionOption({
      category: 'DAYCARE',
      value: 'DAYCARE_ASSISTANCE_SERVICE_CHILD'
    }).save()
    await Fixture.assistanceActionOption({
      category: 'PRESCHOOL',
      value: 'PRESCHOOL_ASSISTANCE_SERVICE_CHILD'
    }).save()
    await Fixture.assistanceAction({
      childId,
      modifiedBy: evakaUserId(admin.id),
      startDate: validDuring.start,
      endDate: validDuring.end,
      actions: [
        'DAYCARE_ASSISTANCE_SERVICE_CHILD',
        'PRESCHOOL_ASSISTANCE_SERVICE_CHILD'
      ]
    }).save()

    await page.goto(
      `${config.employeeUrl}/reports/assistance-needs-and-actions`
    )
    const report = new AssistanceNeedsAndActionsReport(page)
    await report.selectCareAreaFilter('Superkeskus')
    await report.unitSelect.fillAndSelectFirst('Alkuräjähdyksen päiväkoti')

    await report.typeSelect.fillAndSelectFirst('esiopetuksessa')
    await report.childRows
      .nth(0)
      .assertTextEquals(
        'Antero Onni Leevi Aatu Högfors\tKosmiset Vakiot\t10\t1\t0\t0\t0\t0\t0\t0\ta test assistance action option\t1\t0\t0'
      )
    await report.preschoolAssistanceLevelSelect.fillAndSelectFirst(
      'Tehostettu tuki'
    )
    await report.childRows
      .nth(0)
      .assertTextEquals(
        'Antero Onni Leevi Aatu Högfors\tKosmiset Vakiot\t10\t1\ta test assistance action option\t1\t0\t0'
      )
    await report.preschoolAssistanceLevelSelect.fillAndSelectFirst(
      'Tehostettu tuki'
    )
    await report.preschoolAssistanceLevelSelect.fillAndSelectFirst(
      'Erityinen tuki ilman pidennettyä oppivelvollisuutta'
    )
    await report.childRows
      .nth(0)
      .assertTextEquals(
        'Antero Onni Leevi Aatu Högfors\tKosmiset Vakiot\t10\t0\t\t1\t0\t0'
      )

    await report.typeSelect.fillAndSelectFirst('varhaiskasvatuksessa')
    await report.childRows
      .nth(0)
      .assertTextEquals(
        'Antero Onni Leevi Aatu Högfors\tKosmiset Vakiot\t10\t1\t0\t0\t0\t0\t0\t0\ta test assistance action option\t1\t0\t0'
      )
    await report.daycareAssistanceLevelSelect.fillAndSelectFirst(
      'Yleinen tuki, ei päätöstä'
    )
    await report.childRows
      .nth(0)
      .assertTextEquals(
        'Antero Onni Leevi Aatu Högfors\tKosmiset Vakiot\t10\t1\ta test assistance action option\t1\t0\t0'
      )
    await report.daycareAssistanceLevelSelect.fillAndSelectFirst(
      'Yleinen tuki, ei päätöstä'
    )
    await report.daycareAssistanceLevelSelect.fillAndSelectFirst(
      'Yleinen tuki, päätös tukipalveluista'
    )
    await report.childRows
      .nth(0)
      .assertTextEquals(
        'Antero Onni Leevi Aatu Högfors\tKosmiset Vakiot\t10\t0\t\t1\t0\t0'
      )
  })
  test('Shows assistance decision counts', async () => {
    const validDuring = new FiniteDateRange(mockedTime, mockedTime)
    await Fixture.daycareAssistance({
      childId,
      validDuring
    }).save()

    await Fixture.otherAssistanceMeasure({
      childId,
      validDuring
    }).save()

    await Fixture.assistanceActionOption({
      value: 'ASSISTANCE_SERVICE_CHILD'
    }).save()

    await Fixture.assistanceAction({
      childId,
      modifiedBy: evakaUserId(admin.id),
      startDate: validDuring.start,
      endDate: validDuring.end,
      actions: ['ASSISTANCE_SERVICE_CHILD']
    }).save()

    await Fixture.assistanceNeedVoucherCoefficient({
      childId,
      validityPeriod: new FiniteDateRange(validDuring.start, validDuring.end),
      coefficient: 1.5
    }).save()
    await Fixture.assistanceNeedDecision({
      childId,
      validityPeriod: validDuring.asDateRange(),
      status: 'ACCEPTED',
      decisionMaker: {
        employeeId: admin.id,
        title: 'regional director',
        name: null,
        phoneNumber: null
      },
      sentForDecision: validDuring.start,
      selectedUnit: unitId
    }).save()

    await Fixture.assistanceNeedPreschoolDecision({
      childId: childId
    })
      .withRequiredFieldsFilled(testDaycare.id, admin.id, admin.id)
      .with({
        status: 'ACCEPTED',
        sentForDecision: validDuring.start,
        decisionMade: validDuring.start,
        unreadGuardianIds: []
      })
      .withForm({
        type: 'NEW',
        validFrom: validDuring.start,
        validTo: validDuring.end
      })
      .save()

    await page.goto(
      `${config.employeeUrl}/reports/assistance-needs-and-actions`
    )
    const report = new AssistanceNeedsAndActionsReport(page)
    await report.needsAndActionsRows
      .nth(0)
      .assertTextEquals(
        'Superkeskus\n' + '\t\t1\t0\t0\t0\t1\t0\t0\t1\t0\t0\t1\t1\t1'
      )
    await report.selectCareAreaFilter('Superkeskus')
    await report.openUnit('Alkuräjähdyksen päiväkoti')
    await report.childRows
      .nth(0)
      .assertTextEquals(
        'Antero Onni Leevi Aatu Högfors\tKosmiset Vakiot\t10\t1\t0\t0\t0\t1\t0\t0\ta test assistance action option\t1.5\t1\t1'
      )
  })

  test('Provider type filtering works', async () => {
    const validDuring = new FiniteDateRange(mockedTime, mockedTime)
    await Fixture.daycareAssistance({
      childId,
      validDuring
    }).save()

    const voucherDaycare = await Fixture.daycare({
      id: randomId<DaycareId>(),
      areaId: testCareArea.id,
      name: 'Voucher daycare',
      type: ['CENTRE'],
      dailyPreschoolTime: null,
      dailyPreparatoryTime: null,
      costCenter: '31501',
      visitingAddress: {
        streetAddress: 'Kamreerintie 2',
        postalCode: '02210',
        postOffice: 'Espoo'
      },
      decisionCustomization: {
        daycareName: 'Päiväkoti 2 päätöksellä',
        preschoolName: 'Päiväkoti 2 päätöksellä',
        handler: 'Käsittelijä 2',
        handlerAddress: 'Käsittelijän 2 osoite'
      },
      providerType: 'PRIVATE_SERVICE_VOUCHER',
      enabledPilotFeatures: ['MESSAGING', 'MOBILE', 'RESERVATIONS']
    }).save()

    await Fixture.daycareGroup({
      id: randomId<GroupId>(),
      name: 'Voucher group',
      daycareId: voucherDaycare.id
    }).save()

    const municipalAndTotalGroupRowExpectation =
      'Superkeskus\n' + '\t\t1\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0'
    const voucherGroupRowExpectation =
      'Superkeskus\n' + '\t\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0'
    await page.goto(
      `${config.employeeUrl}/reports/assistance-needs-and-actions`
    )
    const report = new AssistanceNeedsAndActionsReport(page)
    await report.needsAndActionsRows
      .nth(0)
      .assertTextEquals(municipalAndTotalGroupRowExpectation)

    await report.providerTypeSelect.fillAndSelectFirst('Palveluseteli')
    await report.unitSelect.click()
    await report.unitSelect.assertOptions(['Kaikki', voucherDaycare.name])

    await report.needsAndActionsRows
      .nth(0)
      .assertTextEquals(voucherGroupRowExpectation)

    await report.providerTypeSelect.fillAndSelectFirst('Kunnallinen')
    await report.unitSelect.click()
    await report.unitSelect.assertOptions(['Kaikki', testDaycare.name])
    await report.needsAndActionsRows
      .nth(0)
      .assertTextEquals(municipalAndTotalGroupRowExpectation)

    await report.providerTypeSelect.fillAndSelectFirst('Kaikki')
    await report.selectCareAreaFilter('Superkeskus')
    await report.unitSelect.click()
    await report.unitSelect.assertOptions([
      'Kaikki',
      testDaycare.name,
      voucherDaycare.name
    ])

    await report.openUnit('Alkuräjähdyksen päiväkoti')
    await report.childRows
      .nth(0)
      .assertTextEquals(
        'Antero Onni Leevi Aatu Högfors\tKosmiset Vakiot\t10\t1\t0\t0\t0\t0\t0\t0\t\t1\t0\t0'
      )

    await report.providerTypeSelect.fillAndSelectFirst('Palveluseteli')
    await report.childRows.assertCount(0)
  })

  test('Placement type filtering works', async () => {
    const validDuring = new FiniteDateRange(mockedTime, mockedTime)
    await Fixture.daycareAssistance({
      childId,
      validDuring
    }).save()

    await Fixture.otherAssistanceMeasure({
      childId,
      validDuring
    }).save()

    await Fixture.assistanceActionOption({
      value: 'ASSISTANCE_SERVICE_CHILD'
    }).save()

    await Fixture.assistanceAction({
      childId,
      modifiedBy: evakaUserId(admin.id),
      startDate: validDuring.start,
      endDate: validDuring.end,
      actions: ['ASSISTANCE_SERVICE_CHILD']
    }).save()

    await Fixture.assistanceNeedVoucherCoefficient({
      childId,
      validityPeriod: new FiniteDateRange(validDuring.start, validDuring.end),
      coefficient: 1.5
    }).save()
    await Fixture.assistanceNeedDecision({
      childId,
      validityPeriod: validDuring.asDateRange(),
      status: 'ACCEPTED',
      decisionMaker: {
        employeeId: admin.id,
        title: 'regional director',
        name: null,
        phoneNumber: null
      },
      sentForDecision: validDuring.start,
      selectedUnit: unitId
    }).save()

    await Fixture.assistanceNeedPreschoolDecision({
      childId: childId
    })
      .withRequiredFieldsFilled(testDaycare.id, admin.id, admin.id)
      .with({
        status: 'ACCEPTED',
        sentForDecision: validDuring.start,
        decisionMade: validDuring.start,
        unreadGuardianIds: []
      })
      .withForm({
        type: 'NEW',
        validFrom: validDuring.start,
        validTo: validDuring.end
      })
      .save()

    const child2 = await Fixture.person({
      id: randomId<PersonId>(),
      ssn: '071013A960A',
      firstName: 'Lisä',
      lastName: 'Lapsi',
      email: '',
      phone: '',
      language: 'fi',
      dateOfBirth: LocalDate.of(2013, 10, 7),
      streetAddress: 'Kamreerintie 4',
      postalCode: '02100',
      postOffice: 'Espoo',
      nationalities: ['FI'],
      restrictedDetailsEnabled: false,
      restrictedDetailsEndDate: null
    }).saveChild()

    const placement2 = await Fixture.placement({
      childId: child2.id,
      unitId: unitId,
      startDate: mockedTime,
      endDate: mockedTime,
      type: 'PRESCHOOL'
    }).save()

    await Fixture.groupPlacement({
      daycareGroupId: testDaycareGroup.id,
      daycarePlacementId: placement2.id,
      startDate: mockedTime,
      endDate: mockedTime
    }).save()
    await Fixture.daycareAssistance({
      childId: child2.id,
      validDuring
    }).save()

    await page.goto(
      `${config.employeeUrl}/reports/assistance-needs-and-actions`
    )
    const report = new AssistanceNeedsAndActionsReport(page)

    const anteroRow =
      'Antero Onni Leevi Aatu Högfors\tKosmiset Vakiot\t10\t1\t0\t0\t0\t1\t0\t0\ta test assistance action option\t1.5\t1\t1'
    const lisaRow =
      'Lisä Lapsi\tKosmiset Vakiot\t10\t1\t0\t0\t0\t0\t0\t0\t\t1\t0\t0'

    //Group view count check
    await report.needsAndActionsRows
      .nth(0)
      .assertTextEquals(
        'Superkeskus\n' + '\t\t2\t0\t0\t0\t1\t0\t0\t1\t0\t0\t1\t1\t1'
      )
    await report.placementTypeSelect.click()
    await report.placementTypeSelect.selectItem('DAYCARE')
    await report.needsAndActionsRows
      .nth(0)
      .assertTextEquals(
        'Superkeskus\n' + '\t\t1\t0\t0\t0\t1\t0\t0\t1\t0\t0\t1\t1\t1'
      )

    //Child view row checks
    await report.selectCareAreaFilter('Superkeskus')
    await report.openUnit('Alkuräjähdyksen päiväkoti')
    await report.childRows.nth(0).assertTextEquals(anteroRow)
    await report.placementTypeSelect.click()
    await report.placementTypeSelect.selectItem('PRESCHOOL')
    await report.openUnit('Alkuräjähdyksen päiväkoti')
    await report.childRows.nth(0).assertTextEquals(anteroRow)
    await report.childRows.nth(1).assertTextEquals(lisaRow)
    await report.placementTypeSelect.click()
    //remove selection
    await report.placementTypeSelect.selectItem('DAYCARE')
    await report.openUnit('Alkuräjähdyksen päiväkoti')
    await report.childRows.nth(0).assertTextEquals(lisaRow)
  })
})
