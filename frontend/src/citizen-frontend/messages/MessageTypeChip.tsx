import { StaticChip } from 'lib-components/atoms/Chip'
import { accentColors } from 'lib-customizations/common'
import React from 'react'
import styled from 'styled-components'
import { MessageType } from './types'

// TODO is the 20px line-height in StaticChip unintentional?
const Chip = styled(StaticChip)`
  line-height: 16px;
`

const chipColors: Record<MessageType, string> = {
  MESSAGE: accentColors.yellow,
  BULLETIN: accentColors.water
}

interface Props {
  type: MessageType
  labels: Record<MessageType, string>
}

export function MessageTypeChip({ type, labels }: Props) {
  return <Chip color={chipColors[type]}>{labels[type]}</Chip>
}
