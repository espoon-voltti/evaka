// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { VasuDocumentState } from 'lib-common/generated/api-types/vasu'
import { StaticChip } from 'lib-components/atoms/Chip'
import colors from 'lib-customizations/common'

const vasuStateChip: Record<VasuDocumentState, string> = {
  DRAFT: colors.accents.peach,
  READY: colors.accents.violet,
  REVIEWED: colors.main.dark,
  CLOSED: colors.greyscale.lighter
}

type ChipLabels = Record<VasuDocumentState, string>

interface StateChipProps {
  state: VasuDocumentState
  labels: ChipLabels
}

export function VasuStateChip({ labels, state }: StateChipProps) {
  return (
    <StaticChip color={vasuStateChip[state]} data-qa="vasu-state-chip">
      {labels[state]}
    </StaticChip>
  )
}
