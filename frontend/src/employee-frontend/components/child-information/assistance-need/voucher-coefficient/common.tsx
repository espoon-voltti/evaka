// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useTranslation } from 'employee-frontend/state/i18n'
import { StaticChip } from 'lib-components/atoms/Chip'
import { theme } from 'lib-customizations/common'

export type AssistanceNeedVoucherCoefficientStatus =
  | 'ACTIVE'
  | 'ENDED'
  | 'UPCOMING'

export const AssistanceNeedVoucherCoefficientStatusChip = React.memo(
  function AssistanceNeedVoucherCoefficientStatusChip({
    status,
    'data-qa': dataQa
  }: {
    status: AssistanceNeedVoucherCoefficientStatus
    'data-qa'?: string
  }) {
    const { i18n } = useTranslation()
    const statusColor = {
      ACTIVE: theme.colors.accents.a3emerald,
      ENDED: theme.colors.grayscale.g15,
      UPCOMING: theme.colors.accents.a7mint
    }

    return (
      <StaticChip
        color={statusColor[status]}
        fitContent
        data-qa={dataQa}
        data-qa-status={status}
      >
        {
          i18n.childInformation.assistanceNeedVoucherCoefficient.statuses[
            status
          ]
        }
      </StaticChip>
    )
  }
)
