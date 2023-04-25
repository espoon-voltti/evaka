// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type { VoucherValueDecisionType } from 'lib-common/generated/api-types/invoicing'
import Select from 'lib-components/atoms/dropdowns/Select'
import { voucherValueDecisionTypes } from 'lib-customizations/employee'

import { useTranslation } from '../../state/i18n'

interface TypeSelectProps {
  selected: VoucherValueDecisionType
  changeDecisionType: (type: VoucherValueDecisionType) => void
  type: 'FEE_DECISION' | 'VALUE_DECISION'
}

const items = voucherValueDecisionTypes

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
