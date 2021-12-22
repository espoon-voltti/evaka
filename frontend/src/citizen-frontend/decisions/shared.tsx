// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import colors, { accents, main, greyscale } from 'lib-customizations/common'
import {
  faGavel,
  faCheck,
  faTimes,
  faEnvelope,
  faPlay,
  faFile
} from 'lib-icons'
import styled from 'styled-components'
import { fontWeights } from 'lib-components/typography'
import { Decision, DecisionSummary } from '../decisions/types'
import { IconDefinition } from '@fortawesome/fontawesome-svg-core'

export const Status = styled.span`
  text-transform: uppercase;
  color: ${colors.greyscale.darkest}
  font-size: 16px;
  font-weight: ${fontWeights.normal};
`

export const decisionStatusIcon = {
  PENDING: {
    icon: faGavel,
    color: accents.warningOrange
  },
  ACCEPTED: {
    icon: faCheck,
    color: accents.successGreen
  },
  REJECTED: {
    icon: faTimes,
    color: accents.dangerRed
  }
}

export const applicationStatusIcon: {
  [key: string]: { icon: IconDefinition; color: string }
} = {
  PROCESSING: {
    icon: faPlay,
    color: main.dark
  },
  PENDING: {
    icon: faGavel,
    color: accents.warningOrange
  },
  ACCEPTED: {
    icon: faCheck,
    color: accents.successGreen
  },
  REJECTED: {
    icon: faTimes,
    color: accents.dangerRed
  },
  CREATED: {
    icon: faFile,
    color: greyscale.dark
  },
  SENT: {
    icon: faEnvelope,
    color: main.dark
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
