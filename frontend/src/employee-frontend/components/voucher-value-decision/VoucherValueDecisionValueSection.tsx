// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import type { VoucherValueDecisionDetailed } from 'lib-common/generated/api-types/invoicing'
import { formatCents } from 'lib-common/money'
import { formatDecimal } from 'lib-common/utils/number'
import { H3 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'

type Props = {
  decision: VoucherValueDecisionDetailed
}

export default React.memo(function VoucherValueDecisionValueSection({
  decision: {
    childAge,
    baseValue,
    assistanceNeedCoefficient,
    placement,
    serviceNeed,
    voucherValue
  }
}: Props) {
  const { i18n } = useTranslation()

  const mainDescription = `${
    i18n.valueDecision.summary.age[childAge < 3 ? 'LESS_THAN_3' : 'OVER_3']
  } (${formatCents(baseValue)} €), ${i18n.placement.type[
    placement.type
  ].toLowerCase()} ${serviceNeed.voucherValueDescriptionFi} (${
    serviceNeed.voucherValueCoefficient * 100
  } %)${
    assistanceNeedCoefficient !== 1
      ? `, ${
          i18n.valueDecision.summary.assistanceNeedCoefficient
        } ${formatDecimal(assistanceNeedCoefficient)}`
      : ''
  }`

  return (
    <section>
      <H3 noMargin>{i18n.valueDecision.summary.value}</H3>
      <Gap size="s" />
      <PartRow>
        <span>{mainDescription}</span>
        <b>{`${formatCents(voucherValue) ?? ''} €`}</b>
      </PartRow>
    </section>
  )
})

const PartRow = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  margin: 0 30px;
`
