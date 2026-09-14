// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { Chip } from 'lib-components/atoms/Chip'
import { farBan, farBellSlash, farCheckCircle } from 'lib-icons'

import { useTranslation } from '../localization'

export type PushStatus = 'enabled' | 'disabled' | 'blocked'

const chips = {
  enabled: { icon: farCheckCircle, colorPalette: 'green' },
  disabled: { icon: farBellSlash, colorPalette: 'orange' },
  blocked: { icon: farBan, colorPalette: 'red' }
} as const

export const PushStatusChip = React.memo(function PushStatusChip({
  status,
  'data-qa': dataQa
}: {
  status: PushStatus
  'data-qa'?: string
}) {
  const i18n = useTranslation()
  return (
    <Chip
      size="small"
      label={i18n.pwa.pushStatus[status]}
      icon={chips[status].icon}
      colorPalette={chips[status].colorPalette}
      data-qa={dataQa}
    />
  )
})
