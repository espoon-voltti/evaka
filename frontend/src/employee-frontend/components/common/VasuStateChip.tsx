// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { StaticChip } from 'lib-components/atoms/Chip'
import colors from 'lib-customizations/common'
import { VasuDocumentState } from '../vasu/api'

const vasuStateChip: Record<VasuDocumentState, string> = {
  DRAFT: colors.accents.emerald,
  READY: colors.accents.violet,
  REVIEWED: colors.primary,
  CLOSED: colors.greyscale.medium
}

type ChipLabels = Record<VasuDocumentState, string>

interface StateChipProps {
  state: VasuDocumentState
  labels: ChipLabels
}

export function VasuStateChip({ labels, state }: StateChipProps) {
  return <StaticChip color={vasuStateChip[state]}>{labels[state]}</StaticChip>
}
