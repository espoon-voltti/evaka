// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import { FeeThresholds } from 'lib-common/api-types/finance'
import { RawElement, RawTextInput } from '../../utils/element'
import { waitUntilEqual } from 'e2e-playwright/utils'

export default class FinanceBasicsPage {
  constructor(private readonly page: Page) {}

  readonly feesSection = {
    root: new RawElement(this.page, '[data-qa="fees-section"]'),
    spinner: new RawElement(this.page, '[data-qa="fees-section-spinner"]'),
    createFeeThresholdsButton: new RawElement(
      this.page,
      '[data-qa="create-new-fee-thresholds"]'
    ),
    item: (index: number) => {
      const element = new RawElement(
        this.page,
        `[data-qa="fee-thresholds-item-${index}"]`
      )

      return {
        element,
        copy: async () => {
          await element.find('[data-qa="copy"]').click()
        },
        edit: async () => {
          await element.find('[data-qa="edit"]').click()
          await new RawElement(this.page, '[data-qa="modal-okBtn"]').click()
        },
        assertItemContains: async (thresholds: FeeThresholds) => {
          const expectValueToBe = async (
            key: keyof FeeThresholds,
            expected: string
          ) => {
            return expect(
              await element.find(`[data-qa="${key}"]`).innerText
            ).toBe(expected)
          }

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
            const key = `minIncomeThreshold${n}` as `minIncomeThreshold${typeof n}`
            return expectValueToBe(key, formatEuros(thresholds[key]))
          }, Promise.resolve())
          await ([2, 3, 4, 5, 6] as const).reduce(async (promise, n) => {
            await promise
            const key = `maxIncomeThreshold${n}` as `maxIncomeThreshold${typeof n}`
            return expectValueToBe(key, formatEuros(thresholds[key]))
          }, Promise.resolve())
          await ([2, 3, 4, 5, 6] as const).reduce(async (promise, n) => {
            await promise
            const key = `incomeMultiplier${n}` as `incomeMultiplier${typeof n}`
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
      validFromInput: new RawTextInput(this.page, '[data-qa="valid-from"]'),
      validToInput: new RawTextInput(this.page, '[data-qa="valid-to"]'),
      maxFeeInput: new RawTextInput(this.page, '[data-qa="max-fee"]'),
      minFeeInput: new RawTextInput(this.page, '[data-qa="min-fee"]'),
      minIncomeThreshold: (familySize: number) =>
        new RawTextInput(
          this.page,
          `[data-qa="min-income-threshold-${familySize}"]`
        ),
      maxIncomeThreshold: (familySize: number) =>
        new RawTextInput(
          this.page,
          `[data-qa="max-income-threshold-${familySize}"]`
        ),
      incomeMultiplier: (familySize: number) =>
        new RawTextInput(
          this.page,
          `[data-qa="income-multiplier-${familySize}"]`
        ),
      maxFeeError: (familySize: number) =>
        new RawTextInput(this.page, `[data-qa="max-fee-error-${familySize}"]`),
      incomeThresholdIncrease6Plus: new RawTextInput(
        this.page,
        '[data-qa="income-threshold-increase"]'
      ),
      siblingDiscount2: new RawTextInput(
        this.page,
        '[data-qa="sibling-discount-2"]'
      ),
      siblingDiscount2Plus: new RawTextInput(
        this.page,
        '[data-qa="sibling-discount-2-plus"]'
      ),
      saveButton: new RawElement(this.page, '[data-qa="save"]'),
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
              formatCents(
                feeThresholds[
                  `minIncomeThreshold${n}` as `minIncomeThreshold${typeof n}`
                ]
              )
            )
          await this.feesSection.editor
            .incomeMultiplier(n)
            .fill(
              formatDecimal(
                feeThresholds[
                  `incomeMultiplier${n}` as `incomeMultiplier${typeof n}`
                ]
              )
            )
          return this.feesSection.editor
            .maxIncomeThreshold(n)
            .fill(
              formatCents(
                feeThresholds[
                  `maxIncomeThreshold${n}` as `maxIncomeThreshold${typeof n}`
                ]
              )
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
      },
      assertSaveIsDisabled: async () => {
        await waitUntilEqual(
          () => this.feesSection.editor.saveButton.getAttribute('disabled'),
          ''
        )
      },
      save: async (retroactive: boolean) => {
        await this.feesSection.editor.saveButton.click()

        if (retroactive) {
          await new RawElement(this.page, '[data-qa="modal-okBtn"]').click()
        }

        await waitUntilEqual(
          () => this.feesSection.editor.saveButton.getAttribute('data-status'),
          'success'
        )
        await this.feesSection.spinner.waitUntilVisible()
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
