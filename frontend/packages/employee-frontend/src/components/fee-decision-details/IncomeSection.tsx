// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import LabelValueList from '~components/common/LabelValueList'
import { Gap } from '~components/shared/layout/white-space'
import Section from '~components/shared/layout/Section'
import { H3, H4 } from '~components/shared/Typography'
import { useTranslation } from '../../state/i18n'
import { formatCents } from '../../utils/money'
import { Income, IncomeType, incomeTypes } from '../../types/income'
import { FeeDecisionDetailed } from '../../types/invoicing'
import { formatName, formatPercent } from '~utils'

interface Props {
  decision: FeeDecisionDetailed
}

function IncomeSection({ decision }: Props) {
  const { i18n } = useTranslation()

  const personIncome = (income: Income | null) => {
    if (!income || income.effect !== 'INCOME') {
      return (
        <div>
          {
            i18n.feeDecision.form.summary.income.details[
              income ? income.effect : 'NOT_AVAILABLE'
            ]
          }
        </div>
      )
    }

    const nonZeroIncomes = incomeTypes
      .filter((key) => !!income.data[key]) // also filters 0s as expected
      .map(
        (key) => i18n.feeDecision.form.summary.income.types[key as IncomeType]
      )

    return (
      <div className="income-person-list">
        <div className="income-person-value">
          <span>
            <span>{i18n.feeDecision.form.summary.income.income}</span>
            {nonZeroIncomes.length > 0 ? (
              <span>: {nonZeroIncomes.join(', ').toLowerCase()}</span>
            ) : null}
          </span>
          <span className="money">
            <b>{formatCents(income.totalIncome)} €</b>
          </span>
        </div>
        {income.totalExpenses > 0 ? (
          <div className="income-person-value">
            <span>{i18n.feeDecision.form.summary.income.expenses}</span>
            <span className="money">
              <b>{formatCents(income.totalExpenses)} €</b>
            </span>
          </div>
        ) : null}
      </div>
    )
  }

  return (
    <Section>
      <H3>{i18n.feeDecision.form.summary.income.familyComposition}</H3>
      <LabelValueList
        spacing="small"
        contents={[
          {
            label: i18n.feeDecision.form.summary.income.familySize,
            value: `${decision.familySize} ${i18n.feeDecision.form.summary.income.persons}`
          },
          {
            label: i18n.feeDecision.form.summary.income.feePercent,
            value: `${formatPercent(decision.feePercent) ?? ''} %`
          },
          {
            label: i18n.feeDecision.form.summary.income.minThreshold,
            value: `${formatCents(decision.minThreshold) ?? ''} €`
          }
        ]}
      />
      <Gap size="m" />
      <H3>{i18n.feeDecision.form.summary.income.title}</H3>
      <LabelValueList
        spacing="small"
        contents={[
          {
            label: i18n.feeDecision.form.summary.income.effect.label,
            value:
              i18n.feeDecision.form.summary.income.effect[decision.incomeEffect]
          },
          {
            label: formatName(
              decision.headOfFamily.firstName,
              decision.headOfFamily.lastName,
              i18n
            ),
            value: personIncome(decision.headOfFamilyIncome),
            valueWidth: '100%'
          },
          ...(decision.partner
            ? [
                {
                  label: formatName(
                    decision.partner.firstName,
                    decision.partner.lastName,
                    i18n
                  ),
                  value: personIncome(decision.partnerIncome),
                  valueWidth: '100%'
                }
              ]
            : [])
        ]}
      />
      {decision.totalIncome && decision.totalIncome > 0 ? (
        <>
          <Gap size="s" />
          <div
            className="total-price slim"
            data-qa="decision-summary-total-income"
          >
            <div>
              <H4 noMargin>
                <b>{i18n.feeDecision.form.summary.income.total}</b>
              </H4>
            </div>
            <div>
              <b>{formatCents(decision.totalIncome)} €</b>
            </div>
          </div>
        </>
      ) : null}
    </Section>
  )
}

export default IncomeSection
