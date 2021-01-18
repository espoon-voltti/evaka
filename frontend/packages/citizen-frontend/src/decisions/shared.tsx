// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import colors, {
  accentColors,
  blueColors,
  greyscale
} from '@evaka/lib-components/src/colors'
import { faGavel, faCheck, faTimes, faEnvelope, faPlay } from '@evaka/lib-icons'
import styled from 'styled-components'
import { Decision, DecisionSummary } from '~decisions/types'
import { IconDefinition } from '@fortawesome/fontawesome-svg-core'

export const Status = styled.span`
  text-transform: uppercase;
  color: ${colors.greyscale.darkest}
  font-size: 16px;
  font-weight: 400;
  padding-left: 0.5rem;
`

export const statusIcon: {
  [key: string]: { icon: IconDefinition; color: string }
} = {
  PROCESSING: {
    icon: faPlay,
    color: blueColors.dark
  },
  PENDING: {
    icon: faGavel,
    color: accentColors.orange
  },
  ACCEPTED: {
    icon: faCheck,
    color: accentColors.green
  },
  REJECTED: {
    icon: faTimes,
    color: greyscale.lighter
  },
  CREATED: {
    icon: faGavel,
    color: greyscale.lighter
  },
  SENT: {
    icon: faEnvelope,
    color: blueColors.dark
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
