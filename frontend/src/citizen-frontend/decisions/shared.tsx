// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import styled from 'styled-components'
import { fontWeights } from 'lib-components/typography'
import { colors } from 'lib-customizations/common'
import {
  faGavel,
  faCheck,
  faTimes,
  faEnvelope,
  faPlay,
  faFile
} from 'lib-icons'
import { Decision, DecisionSummary } from '../decisions/types'

export const Status = styled.span`
  text-transform: uppercase;
  color: ${colors.greyscale.darkest}
  font-size: 16px;
  font-weight: ${fontWeights.normal};
`

export const decisionStatusIcon = {
  PENDING: {
    icon: faGavel,
    color: colors.accents.warningOrange
  },
  ACCEPTED: {
    icon: faCheck,
    color: colors.accents.successGreen
  },
  REJECTED: {
    icon: faTimes,
    color: colors.accents.dangerRed
  }
}

export const applicationStatusIcon: {
  [key: string]: { icon: IconDefinition; color: string }
} = {
  PROCESSING: {
    icon: faPlay,
    color: colors.main.dark
  },
  PENDING: {
    icon: faGavel,
    color: colors.accents.warningOrange
  },
  ACCEPTED: {
    icon: faCheck,
    color: colors.accents.successGreen
  },
  REJECTED: {
    icon: faTimes,
    color: colors.accents.dangerRed
  },
  CREATED: {
    icon: faFile,
    color: colors.greyscale.dark
  },
  SENT: {
    icon: faEnvelope,
    color: colors.main.dark
  }
}

type ComparableDecision = Decision | DecisionSummary

export const decisionOrderComparator = (
  decisionA: ComparableDecision,
  decisionB: ComparableDecision
) => {
  if (decisionA.type === 'PRESCHOOL_DAYCARE') {
    return 1
  } else if (decisionB.type === 'PRESCHOOL_DAYCARE') {
    return -1
  } else {
    return 0
  }
}
