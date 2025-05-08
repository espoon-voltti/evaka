// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { PersonId } from 'lib-common/generated/api-types/shared'

import config from '../../config'
import { Fixture, familyWithTwoGuardians } from '../../dev-api/fixtures'
import {
  getNekkuSpecialDietChoices,
  resetServiceState
} from '../../generated/api-clients'
import { DevEmployee } from '../../generated/api-types'
import ChildInformationPage, {
  AdditionalInformationSection
} from '../../pages/employee/child-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let childInformationPage: ChildInformationPage
let childId: PersonId
let admin: DevEmployee

beforeEach(async () => {
  await resetServiceState()

  await familyWithTwoGuardians.save()
  admin = await Fixture.employee().admin().save()

  childId = familyWithTwoGuardians.children[0].id

  await Fixture.nekkuSpecialDiets([
    {
      id: '2',
      name: 'Testiruokavalio',
      fields: [
        {
          id: 'a',
          name: 'a',
          type: 'TEXT',
          options: null
        },
        {
          id: 'b',
          name: 'b',
          type: 'CHECKBOXLIST',
          options: [
            {
              weight: 1,
              key: 'b1',
              value: 'b1'
            },
            {
              weight: 2,
              key: 'b2',
              value: 'b2'
            },
            {
              weight: 3,
              key: 'b3',
              value: 'b3'
            }
          ]
        }
      ]
    }
  ]).save()

  page = await Page.open({
    employeeCustomizations: {
      featureFlags: {
        nekkuIntegration: true
      }
    }
  })
  await employeeLogin(page, admin)
  await page.goto(config.employeeUrl + '/child-information/' + childId)
  childInformationPage = new ChildInformationPage(page)
  await childInformationPage.waitUntilLoaded()
})

describe('Nekku fields are editable', () => {
  let section: AdditionalInformationSection
  beforeEach(() => {
    section = childInformationPage.additionalInformationSection()
  })

  test('Nekku eats breakfast information can be unset', async () => {
    await section.nekkuEatsBreakfast.assertTextEquals('Kyllä')

    await section.editBtn.click()
    await section.nekkuEatsBreakfastCheckbox.uncheck()
    await section.confirmBtn.click()
    await section.nekkuEatsBreakfast.assertTextEquals('Ei')
  })

  test('Nekku diet can be edited', async () => {
    await section.nekkuDiet.assertTextEquals('Seka')

    await section.editBtn.click()
    await section.nekkuDietSelect.fillAndSelectFirst('V')
    await section.confirmBtn.click()
    await section.nekkuDiet.assertTextEquals('Vegaani')
  })

  test('Special diet fields are rendered correctly', async () => {
    await section.editBtn.click()

    const specialDietEditor = section.getNekkuSpecialDietEditor()
    await specialDietEditor.getCheckBox('2', 'b', 'b1').waitUntilVisible()
    await specialDietEditor.getCheckBox('2', 'b', 'b2').waitUntilVisible()
    await specialDietEditor.getCheckBox('2', 'b', 'b3').waitUntilVisible()
    await specialDietEditor.getTextField('2', 'a').waitUntilVisible()
  })

  test('Special diet fields can be edited', async () => {
    await section.editBtn.click()

    const specialDietEditor = section.getNekkuSpecialDietEditor()
    await specialDietEditor.getCheckBox('2', 'b', 'b1').check()
    await specialDietEditor.getCheckBox('2', 'b', 'b3').check()
    await specialDietEditor.getTextField('2', 'a').fill('Foo')

    await section.confirmBtn.click()

    await specialDietEditor
      .getCheckBoxValue('2', 'b')
      .assertTextEquals('b1, b3')
    await specialDietEditor.getTextValue('2', 'a').assertTextEquals('a: Foo')
  })

  test('Special diets are saved to the database correctly', async () => {
    await section.editBtn.click()

    const specialDietEditor = section.getNekkuSpecialDietEditor()
    await specialDietEditor.getCheckBox('2', 'b', 'b1').check()
    await specialDietEditor.getCheckBox('2', 'b', 'b3').check()
    await specialDietEditor.getTextField('2', 'a').fill('Foo')

    await section.confirmBtn.click()

    // we don't really care about this assertion, it just ensures that the
    // data has been updated before we call getNekkuSpecialDietChoices()
    await specialDietEditor.getTextValue('2', 'a').assertTextEquals('a: Foo')

    const savedValues = await getNekkuSpecialDietChoices({ childId })
    if (savedValues.length !== 3) throw Error('Excepted 3 special diet choices')

    const expectedValues = [
      {
        dietId: '2',
        fieldId: 'a',
        value: 'Foo'
      },
      {
        dietId: '2',
        fieldId: 'b',
        value: 'b1'
      },
      {
        dietId: '2',
        fieldId: 'b',
        value: 'b3'
      }
    ]

    for (const value of expectedValues) {
      if (
        savedValues.find(
          (element) =>
            element.dietId === value.dietId &&
            element.fieldId === value.fieldId &&
            element.value === value.value
        ) === undefined
      )
        throw Error(`Expected ${value.value} in special diet choices`)
    }
  })
})
