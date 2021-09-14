// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import styled from 'styled-components'
import LabelValueList from '../../components/common/LabelValueList'
import { Gap } from 'lib-components/white-space'
import { H3, H5 } from 'lib-components/typography'
import { useTranslation } from '../../state/i18n'
import { Income } from '../../types/income'
import { VoucherValueDecisionDetailed } from '../../types/invoicing'
import { formatCents } from '../../utils/money'
import { formatName, formatPercent } from '../../utils'
import { Result, Success } from 'lib-common/api'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { getIncomeOptions, IncomeTypeOptions } from '../../api/income'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'

type Props = {
  decision: VoucherValueDecisionDetailed
}

export default React.memo(function VoucherValueDecisionIncomeSection({
  decision
}: Props) {
  const { i18n } = useTranslation()
  const [incomeOptions, setIncomeOptions] = useState<Result<IncomeTypeOptions>>(
    Success.of([[], []])
  )
  const loadIncomeOptions = useRestApi(getIncomeOptions, setIncomeOptions)
  useEffect(() => {
    loadIncomeOptions()
  }, [loadIncomeOptions])

  if (incomeOptions.isLoading) {
    return <SpinnerSegment />
  }
  if (incomeOptions.isFailure) {
    return <ErrorSegment />
  }

  const [incomeTypes] = incomeOptions.value

  const personIncome = (income: Income | null) => {
    if (!income || income.effect !== 'INCOME') {
      return (
        <span>
          {
            i18n.valueDecision.summary.income.details[
              income ? income.effect : 'NOT_AVAILABLE'
            ]
          }
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
            {i18n.valueDecision.summary.income.income}
            {nonZeroIncomes.length > 0
              ? `: ${nonZeroIncomes.join(', ').toLowerCase()}`
              : null}
          </span>
          <Money>{formatCents(income.totalIncome)} €</Money>
        </IncomeItem>
        {income.totalExpenses > 0 ? (
          <IncomeItem>
            <span>{i18n.valueDecision.summary.income.expenses}</span>
            <Money>{formatCents(income.totalExpenses)} €</Money>
          </IncomeItem>
        ) : null}
      </div>
    )
  }

  return (
    <section>
      <H3 noMargin>{i18n.valueDecision.summary.income.familyComposition}</H3>
      <Gap size="s" />
      <LabelValueList
        spacing="small"
        contents={[
          {
            label: i18n.valueDecision.summary.income.familySize,
            value: `${decision.familySize} ${i18n.valueDecision.summary.income.persons}`
          },
          {
            label: i18n.valueDecision.summary.income.feePercent,
            value: `${
              formatPercent(decision.feeThresholds.incomeMultiplier * 100) ?? ''
            } %`
          },
          {
            label: i18n.valueDecision.summary.income.minThreshold,
            value: `${
              formatCents(decision.feeThresholds.minIncomeThreshold) ?? ''
            } €`
          }
        ]}
      />
      <Gap size="m" />
      <H3 noMargin>{i18n.valueDecision.summary.income.title}</H3>
      <Gap size="s" />
      <LabelValueList
        spacing="small"
        contents={[
          {
            label: i18n.valueDecision.summary.income.effect.label,
            value:
              i18n.valueDecision.summary.income.effect[decision.incomeEffect]
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
          <IncomeTotal>
            <IncomeTotalTitle noMargin>
              {i18n.valueDecision.summary.income.total}
            </IncomeTotalTitle>
            <b>{formatCents(decision.totalIncome)} €</b>
          </IncomeTotal>
        </>
      ) : null}
    </section>
  )
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

const IncomeTotalTitle = styled(H5)`
  font-weight: 600;
`
