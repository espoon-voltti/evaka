// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useTranslation } from '../../state/i18n'
import SimpleSelect from 'lib-components/atoms/form/SimpleSelect'
import React from 'react'

interface TypeSelectProps {
  selected: string
  changeDecisionType: (type: string) => void
  type: 'FEE_DECISION' | 'VALUE_DECISION'
}

export const TypeSelect = ({
  selected,
  changeDecisionType,
  type
}: TypeSelectProps) => {
  const { i18n } = useTranslation()

  const labels =
    type === 'FEE_DECISION' ? i18n.feeDecision.type : i18n.valueDecision.type

  const options = [
    { value: 'NORMAL', label: labels.NORMAL },
    {
      value: 'RELIEF_ACCEPTED',
      label: labels.RELIEF_ACCEPTED
    },
    {
      value: 'RELIEF_PARTLY_ACCEPTED',
      label: labels.RELIEF_PARTLY_ACCEPTED
    },
    {
      value: 'RELIEF_REJECTED',
      label: labels.RELIEF_REJECTED
    }
  ]

  return (
    <SimpleSelect
      value={selected}
      options={options}
      onChange={(e) => changeDecisionType(e.target.value)}
    />
  )
}
