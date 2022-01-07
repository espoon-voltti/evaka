// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { VoucherValueDecisionType } from 'lib-common/generated/api-types/invoicing'
import Select from 'lib-components/atoms/dropdowns/Select'
import { useTranslation } from '../../state/i18n'

interface TypeSelectProps {
  selected: VoucherValueDecisionType
  changeDecisionType: (type: VoucherValueDecisionType) => void
  type: 'FEE_DECISION' | 'VALUE_DECISION'
}

const items: VoucherValueDecisionType[] = [
  'NORMAL',
  'RELIEF_ACCEPTED',
  'RELIEF_PARTLY_ACCEPTED',
  'RELIEF_REJECTED'
]

export const TypeSelect = ({
  selected,
  changeDecisionType,
  type
}: TypeSelectProps) => {
  const { i18n } = useTranslation()

  const labels =
    type === 'FEE_DECISION' ? i18n.feeDecision.type : i18n.valueDecision.type

  return (
    <Select
      items={items}
      selectedItem={selected}
      getItemLabel={(item) => labels[item]}
      onChange={(value) => value && changeDecisionType(value)}
    />
  )
}
