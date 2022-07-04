// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { VasuDocumentState } from 'lib-common/generated/api-types/vasu'
import { StaticChip } from 'lib-components/atoms/Chip'
import colors from 'lib-customizations/common'

const vasuStateChip: Record<VasuDocumentState, string> = {
  DRAFT: colors.accents.a7mint,
  READY: colors.accents.a4violet,
  REVIEWED: colors.main.m1,
  CLOSED: colors.grayscale.g15
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
