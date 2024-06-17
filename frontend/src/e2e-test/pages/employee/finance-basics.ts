// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FeeThresholds } from 'lib-common/generated/api-types/invoicing'

import { waitUntilTrue } from '../../utils'
import { AsyncButton, Page, TextInput } from '../../utils/page'

export default class FinanceBasicsPage {
  constructor(private readonly page: Page) {}

  readonly feesSection = {
    root: this.page.findByDataQa('fees-section'),
    rootLoaded: this.page.find(
      '[data-qa="fees-section"][data-isloading="false"]'
    ),
    createFeeThresholdsButton: this.page.findByDataQa(
      'create-new-fee-thresholds'
    ),
    item: (index: number) => {
      const element = this.page.findByDataQa(`fee-thresholds-item-${index}`)

      return {
        element,
        copy: async () => {
          await element.find('[data-qa="copy"]').click()
        },
        edit: async () => {
          await element.find('[data-qa="edit"]').click()
          await this.page.findByDataQa('modal-okBtn').click()
        },
        assertItemContains: async (thresholds: FeeThresholds) => {
          const expectValueToBe = async (
            key: keyof FeeThresholds,
            expected: string
          ) =>
            expect(await element.find(`[data-qa="${key}"]`).text).toBe(expected)

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
      validFromInput: new TextInput(this.page.findByDataQa('valid-from')),
      validToInput: new TextInput(this.page.findByDataQa('valid-to')),
      maxFeeInput: new TextInput(this.page.findByDataQa('max-fee')),
      minFeeInput: new TextInput(this.page.findByDataQa('min-fee')),
      minIncomeThreshold: (familySize: number) =>
        new TextInput(
          this.page.findByDataQa(`min-income-threshold-${familySize}`)
        ),
      maxIncomeThreshold: (familySize: number) =>
        new TextInput(
          this.page.findByDataQa(`max-income-threshold-${familySize}`)
        ),
      incomeMultiplier: (familySize: number) =>
        new TextInput(
          this.page.findByDataQa(`income-multiplier-${familySize}`)
        ),
      maxFeeError: (familySize: number) =>
        new TextInput(this.page.findByDataQa(`max-fee-error-${familySize}`)),
      incomeThresholdIncrease6Plus: new TextInput(
        this.page.findByDataQa('income-threshold-increase')
      ),
      siblingDiscount2: new TextInput(
        this.page.findByDataQa('sibling-discount-2')
      ),
      siblingDiscount2Plus: new TextInput(
        this.page.findByDataQa('sibling-discount-2-plus')
      ),
      temporaryFee: new TextInput(this.page.findByDataQa('temporary-fee')),
      temporaryFeePartDay: new TextInput(
        this.page.findByDataQa('temporary-fee-part-day')
      ),
      temporaryFeeSibling: new TextInput(
        this.page.findByDataQa('temporary-fee-sibling')
      ),
      temporaryFeeSiblingPartDay: new TextInput(
        this.page.findByDataQa('temporary-fee-sibling-part-day')
      ),
      saveButton: new AsyncButton(this.page.findByDataQa('save')),
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
            .fill(formatCents(feeThresholds[`minIncomeThreshold${n}` as const]))
          await this.feesSection.editor
            .incomeMultiplier(n)
            .fill(formatDecimal(feeThresholds[`incomeMultiplier${n}` as const]))
          return this.feesSection.editor
            .maxIncomeThreshold(n)
            .fill(formatCents(feeThresholds[`maxIncomeThreshold${n}` as const]))
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
          await this.page.findByDataQa('modal-okBtn').click()
        }

        await this.feesSection.editor.saveButton.waitUntilHidden()
        await this.feesSection.rootLoaded.waitUntilVisible()
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
