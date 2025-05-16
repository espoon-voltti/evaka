// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import styled from 'styled-components'

import type { DecisionSummary } from 'lib-common/generated/api-types/application'
import { fontWeights } from 'lib-components/typography'
import { colors } from 'lib-customizations/common'
import {
  faCheck,
  faEnvelope,
  faFile,
  faGavel,
  faPlay,
  faTimes
} from 'lib-icons'

export const Status = styled.span`
  text-transform: uppercase;
  color: ${colors.grayscale.g100};
  font-size: 16px;
  font-weight: ${fontWeights.normal};
`

export const decisionStatusIcon = {
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

export const applicationStatusIcon: Record<
  string,
  { icon: IconDefinition; color: string }
> = {
  PROCESSING: {
    icon: faPlay,
    color: colors.main.m1
  },
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
  },
  CREATED: {
    icon: faFile,
    color: colors.grayscale.g70
  },
  SENT: {
    icon: faEnvelope,
    color: colors.main.m1
  }
}

export const applicationDecisionIsUnread = (decision: DecisionSummary) =>
  decision.status === 'PENDING'
