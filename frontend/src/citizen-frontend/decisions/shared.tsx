// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

import type { DecisionStatus } from 'lib-common/generated/api-types/decision'
import type { IconChipVisualProps } from 'lib-components/atoms/IconChip'
import { fontWeights } from 'lib-components/typography'
import { colors } from 'lib-customizations/common'
import { faGavel, faCheck, faTimes } from 'lib-icons'

export const Status = styled.span`
  text-transform: uppercase;
  color: ${colors.grayscale.g100};
  font-size: 16px;
  font-weight: ${fontWeights.normal};
`

export const iconPropsByStatus: Record<DecisionStatus, IconChipVisualProps> = {
  PENDING: {
    icon: faGavel,
    backgroundColor: '#ffeee0',
    textColor: colors.accents.a2orangeDark,
    iconColor: colors.grayscale.g0,
    iconBackgroundColor: colors.status.warning
  },
  ACCEPTED: {
    icon: faCheck,
    backgroundColor: '#e9f6ea',
    textColor: colors.accents.a1greenDark,
    iconColor: colors.grayscale.g0,
    iconBackgroundColor: '#3c963f'
  },
  REJECTED: {
    icon: faTimes,
    backgroundColor: '#ffe0e2',
    textColor: '#990007',
    iconColor: colors.grayscale.g0,
    iconBackgroundColor: '#ff000c'
  }
}

export const decisionStatusIcon: Record<
  DecisionStatus,
  { icon: typeof faGavel; color: string }
> = {
  PENDING: {
    icon: faGavel,
    color: colors.status.warning
  },
  ACCEPTED: {
    icon: faCheck,
    color: colors.status.success
  },
  REJECTED: {
    icon: faTimes,
    color: colors.status.danger
  }
}
