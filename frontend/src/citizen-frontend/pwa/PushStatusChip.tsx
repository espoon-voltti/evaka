// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import IconChip from 'lib-components/atoms/IconChip'
import { farBan, farBellSlash, farCheckCircle } from 'lib-icons'

import * as chipColors from '../chipColors'
import { useTranslation } from '../localization'

export type PushStatus = 'enabled' | 'disabled' | 'blocked'

const chips = {
  enabled: { icon: farCheckCircle, colors: chipColors.green },
  disabled: { icon: farBellSlash, colors: chipColors.orange },
  blocked: { icon: farBan, colors: chipColors.orange }
} as const

export const PushStatusChip = React.memo(function PushStatusChip({
  status,
  'data-qa': dataQa
}: {
  status: PushStatus
  'data-qa'?: string
}) {
  const i18n = useTranslation()
  const { icon, colors } = chips[status]
  return (
    <IconChip
      label={i18n.pwa.pushStatus[status]}
      icon={icon}
      textColor={colors.fg}
      backgroundColor={colors.bg}
      iconColor={colors.fg}
      iconBackgroundColor="transparent"
      data-qa={dataQa}
    />
  )
})
