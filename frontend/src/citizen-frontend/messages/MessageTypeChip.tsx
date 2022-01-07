// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { MessageType } from 'lib-common/generated/enums'
import { StaticChip } from 'lib-components/atoms/Chip'
import { colors } from 'lib-customizations/common'

// TODO is the 20px line-height in StaticChip unintentional?
const Chip = styled(StaticChip)`
  line-height: 16px;
`

const chipColors: Record<MessageType, string> = {
  MESSAGE: colors.accents.violet,
  BULLETIN: colors.main.dark
}

interface Props {
  type: MessageType
  labels: Record<MessageType, string>
}

export function MessageTypeChip({ type, labels }: Props) {
  return <Chip color={chipColors[type]}>{labels[type]}</Chip>
}
