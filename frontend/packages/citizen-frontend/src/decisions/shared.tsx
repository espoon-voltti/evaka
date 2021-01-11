// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { accentColors, greyscale } from '@evaka/lib-components/src/colors'
import { faGavel, faCheck, faTimes } from '@evaka/lib-icons'
import styled from 'styled-components'
import { Decision, DecisionSummary } from '~decisions/types'

export const Status = styled.span`
  text-transform: uppercase;
`

export const statusIcon = {
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
