// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FeeThresholds } from 'lib-common/generated/api-types/invoicing'

import { waitUntilTrue } from '../../utils'
import { Element, AsyncButton, Page, TextInput } from '../../utils/page'

export default class FinanceBasicsPage {
  feesSection: {
    root: Element
    rootLoaded: Element
    createFeeThresholdsButton: Element
    item: (index: number) => {
      element: Element
      copy: () => Promise<void>
      edit: () => Promise<void>
      assertItemContains: (thresholds: FeeThresholds) => Promise<void>
    }
    editor: {
      validFromInput: TextInput
      validToInput: TextInput
      maxFeeInput: TextInput
      minFeeInput: TextInput
      minIncomeThreshold: (familySize: number) => TextInput
      maxIncomeThreshold: (familySize: number) => TextInput
      incomeMultiplier: (familySize: number) => TextInput
      maxFeeError: (familySize: number) => TextInput
      incomeThresholdIncrease6Plus: TextInput
      siblingDiscount2: TextInput
      siblingDiscount2Plus: TextInput
      temporaryFee: TextInput
      temporaryFeePartDay: TextInput
      temporaryFeeSibling: TextInput
      temporaryFeeSiblingPartDay: TextInput
      saveButton: AsyncButton
      fillInThresholds: (feeThresholds: FeeThresholds) => Promise<void>
      assertSaveIsDisabled: () => Promise<void>
      save: (retroactive: boolean) => Promise<void>
    }
  }
  constructor(page: Page) {
    this.feesSection = {
      root: page.findByDataQa('fees-section'),
      rootLoaded: page.find('[data-qa="fees-section"][data-isloading="false"]'),
      createFeeThresholdsButton: page.findByDataQa('create-new-fee-thresholds'),
      item: (index: number) => {
        const element = page.findByDataQa(`fee-thresholds-item-${index}`)

        return {
          element,
          copy: async () => {
            await element.find('[data-qa="copy"]').click()
          },
          edit: async () => {
            await element.find('[data-qa="edit"]').click()
            await page.findByDataQa('modal-okBtn').click()
          },
          assertItemContains: async (thresholds: FeeThresholds) => {
            const expectValueToBe = async (
              key: keyof FeeThresholds,
              expected: string
            ) =>
              expect(await element.find(`[data-qa="${key}"]`).text).toBe(
                expected
              )

            const formatEuros = (cents: number) => `${formatCents(cents)} €`
            const formatPercents = (decimal: number) =>
              `${formatDecimal(decimal)} %`

            await expectValueToBe(
              'validDuring',
              thresholds.validDuring.format().trimEnd()
            )
            await expectValueToBe('maxFee', formatEuros(thresholds.maxFee))
            await expectValueToBe('minFee', formatEuros(thresholds.minFee))
            await ([2, 3, 4, 5, 6] as const).reduce(async (promise, n) => {
              await promise
              const key = `minIncomeThreshold${n}` as const
              return expectValueToBe(key, formatEuros(thresholds[key]))
            }, Promise.resolve())
            await ([2, 3, 4, 5, 6] as const).reduce(async (promise, n) => {
              await promise
              const key = `maxIncomeThreshold${n}` as const
              return expectValueToBe(key, formatEuros(thresholds[key]))
            }, Promise.resolve())
            await ([2, 3, 4, 5, 6] as const).reduce(async (promise, n) => {
              await promise
              const key = `incomeMultiplier${n}` as const
              return expectValueToBe(key, formatPercents(thresholds[key]))
            }, Promise.resolve())
            await expectValueToBe(
              'incomeThresholdIncrease6Plus',
              formatEuros(thresholds.incomeThresholdIncrease6Plus)
            )
            await expectValueToBe(
              'siblingDiscount2',
              formatPercents(thresholds.siblingDiscount2)
            )
            await expectValueToBe(
              'siblingDiscount2Plus',
              formatPercents(thresholds.siblingDiscount2Plus)
            )
          }
        }
      },
      editor: {
        validFromInput: new TextInput(page.findByDataQa('valid-from')),
        validToInput: new TextInput(page.findByDataQa('valid-to')),
        maxFeeInput: new TextInput(page.findByDataQa('max-fee')),
        minFeeInput: new TextInput(page.findByDataQa('min-fee')),
        minIncomeThreshold: (familySize: number) =>
          new TextInput(
            page.findByDataQa(`min-income-threshold-${familySize}`)
          ),
        maxIncomeThreshold: (familySize: number) =>
          new TextInput(
            page.findByDataQa(`max-income-threshold-${familySize}`)
          ),
        incomeMultiplier: (familySize: number) =>
          new TextInput(page.findByDataQa(`income-multiplier-${familySize}`)),
        maxFeeError: (familySize: number) =>
          new TextInput(page.findByDataQa(`max-fee-error-${familySize}`)),
        incomeThresholdIncrease6Plus: new TextInput(
          page.findByDataQa('income-threshold-increase')
        ),
        siblingDiscount2: new TextInput(
          page.findByDataQa('sibling-discount-2')
        ),
        siblingDiscount2Plus: new TextInput(
          page.findByDataQa('sibling-discount-2-plus')
        ),
        temporaryFee: new TextInput(page.findByDataQa('temporary-fee')),
        temporaryFeePartDay: new TextInput(
          page.findByDataQa('temporary-fee-part-day')
        ),
        temporaryFeeSibling: new TextInput(
          page.findByDataQa('temporary-fee-sibling')
        ),
        temporaryFeeSiblingPartDay: new TextInput(
          page.findByDataQa('temporary-fee-sibling-part-day')
        ),
        saveButton: new AsyncButton(page.findByDataQa('save')),
        fillInThresholds: async (feeThresholds: FeeThresholds) => {
          await this.feesSection.editor.validFromInput.fill(
            feeThresholds.validDuring.start.format()
          )
          await this.feesSection.editor.validToInput.fill(
            feeThresholds.validDuring.end?.format() ?? ''
          )
          await this.feesSection.editor.maxFeeInput.fill(
            formatCents(feeThresholds.maxFee)
          )
          await this.feesSection.editor.minFeeInput.fill(
            formatCents(feeThresholds.minFee)
          )
          await ([2, 3, 4, 5, 6] as const).reduce(async (promise, n) => {
            await promise
            await this.feesSection.editor
              .minIncomeThreshold(n)
              .fill(
                formatCents(feeThresholds[`minIncomeThreshold${n}` as const])
              )
            await this.feesSection.editor
              .incomeMultiplier(n)
              .fill(
                formatDecimal(feeThresholds[`incomeMultiplier${n}` as const])
              )
            return this.feesSection.editor
              .maxIncomeThreshold(n)
              .fill(
                formatCents(feeThresholds[`maxIncomeThreshold${n}` as const])
              )
          }, Promise.resolve())
          await this.feesSection.editor.incomeThresholdIncrease6Plus.fill(
            formatCents(feeThresholds.incomeThresholdIncrease6Plus)
          )
          await this.feesSection.editor.siblingDiscount2.fill(
            formatDecimal(feeThresholds.siblingDiscount2)
          )
          await this.feesSection.editor.siblingDiscount2Plus.fill(
            formatDecimal(feeThresholds.siblingDiscount2Plus)
          )
          await this.feesSection.editor.temporaryFee.fill(
            formatDecimal(feeThresholds.temporaryFee)
          )
          await this.feesSection.editor.temporaryFeePartDay.fill(
            formatDecimal(feeThresholds.temporaryFeePartDay)
          )
          await this.feesSection.editor.temporaryFeeSibling.fill(
            formatDecimal(feeThresholds.temporaryFeeSibling)
          )
          await this.feesSection.editor.temporaryFeeSiblingPartDay.fill(
            formatDecimal(feeThresholds.temporaryFeeSiblingPartDay)
          )
        },
        assertSaveIsDisabled: async () => {
          await waitUntilTrue(() => this.feesSection.editor.saveButton.disabled)
        },
        save: async (retroactive: boolean) => {
          await this.feesSection.editor.saveButton.click()

          if (retroactive) {
            await page.findByDataQa('modal-okBtn').click()
          }

          await this.feesSection.editor.saveButton.waitUntilHidden()
          await this.feesSection.rootLoaded.waitUntilVisible()
        }
      }
    }
  }
}

function formatCents(cents: number) {
  return (cents / 100).toString().replace('.', ',')
}

function formatDecimal(decimals: number) {
  return (decimals * 100).toString().replace('.', ',')
}
