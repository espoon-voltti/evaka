// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import type {
  DecisionIncome,
  FeeDecisionDetailed,
  IncomeOption
} from 'lib-common/generated/api-types/invoicing'
import { formatCents } from 'lib-common/money'
import { formatPersonName } from 'lib-common/names'
import { useQueryResult } from 'lib-common/query'
import LabelValueList from 'lib-components/molecules/LabelValueList'
import { H3, H4 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { formatPercent } from '../../utils'
import { renderResult } from '../async-rendering'
import { incomeTypeOptionsQuery } from '../person-profile/queries'

interface Props {
  decision: FeeDecisionDetailed
}

export default React.memo(function IncomeSection({ decision }: Props) {
  const { i18n } = useTranslation()

  const incomeTypeOptions = useQueryResult(incomeTypeOptionsQuery())

  const personIncome = (
    incomeTypes: IncomeOption[],
    income: DecisionIncome | null
  ) => {
    if (!income) {
      return (
        <span>
          {i18n.feeDecision.form.summary.income.details.NOT_AVAILABLE}
        </span>
      )
    }
    if (income.effect !== 'INCOME') {
      return (
        <span>
          {i18n.feeDecision.form.summary.income.details[income.effect]}
        </span>
      )
    }

    const nonZeroIncomes = incomeTypes
      .filter((type) => !!income.data[type.value]) // also filters 0s as expected
      .map((type) => type.nameFi)

    return (
      <div>
        <IncomeItem>
          <span>
            {i18n.feeDecision.form.summary.income.income}
            {nonZeroIncomes.length > 0
              ? `: ${nonZeroIncomes.join(', ').toLowerCase()}`
              : null}
          </span>
          <Money>{formatCents(income.totalIncome)} €</Money>
        </IncomeItem>
        {income.totalExpenses > 0 ? (
          <IncomeItem>
            <span>{i18n.feeDecision.form.summary.income.expenses}</span>
            <Money>{formatCents(income.totalExpenses)} €</Money>
          </IncomeItem>
        ) : null}
      </div>
    )
  }

  return renderResult(incomeTypeOptions, ({ incomeTypes }) => (
    <section>
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
            value: `${
              formatPercent(decision.feeThresholds.incomeMultiplier * 100) ?? ''
            } %`
          },
          {
            label: i18n.feeDecision.form.summary.income.minThreshold,
            value: `${
              formatCents(decision.feeThresholds.minIncomeThreshold) ?? ''
            } €`
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
            label: formatPersonName(decision.headOfFamily, 'First Last'),
            value: personIncome(incomeTypes, decision.headOfFamilyIncome),
            valueWidth: '100%'
          },
          ...(decision.partner
            ? [
                {
                  label: formatPersonName(decision.partner, 'First Last'),
                  value: personIncome(incomeTypes, decision.partnerIncome),
                  valueWidth: '100%',
                  dataQa: 'partner-income'
                }
              ]
            : [])
        ].concat(
          decision.children
            .filter(
              (child) => child.childIncome && child.childIncome.totalIncome > 0
            )
            .map((childWithIncome) => ({
              label: formatPersonName(childWithIncome.child, 'First Last'),
              value: personIncome(incomeTypes, childWithIncome.childIncome),
              valueWidth: '100%',
              dataQa: 'child-income'
            }))
        )}
      />
      {decision.totalIncome && decision.totalIncome > 0 ? (
        <>
          <Gap size="s" />
          <IncomeTotal data-qa="decision-summary-total-income">
            <H4 noMargin>{i18n.feeDecision.form.summary.income.total}</H4>
            <Money>{formatCents(decision.totalIncome)} €</Money>
          </IncomeTotal>
        </>
      ) : null}
    </section>
  ))
})

const IncomeItem = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: space-between;
  margin-right: 30px;
`

const Money = styled.b`
  white-space: nowrap;
`

const IncomeTotal = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  background: ghostwhite;
  padding: 16px 30px;
`
