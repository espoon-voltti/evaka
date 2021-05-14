import React from 'react'

import styled from 'styled-components'
import { StaticChip } from '../../../lib-components/atoms/Chip'
import { accentColors } from '../../../lib-components/colors'
import { useTranslation } from '../../state/i18n'
import { MessageType } from './types'

// TODO is the 20px line-height in StaticChip unintentional?
const Chip = styled(StaticChip)`
  line-height: 16px;
`

const chipColors = {
  MESSAGE: accentColors.yellow,
  BULLETIN: accentColors.water
}

export function MessageTypeChip({ type }: { type: MessageType }) {
  const { i18n } = useTranslation()
  return <Chip color={chipColors[type]}>{i18n.messages.types[type]}</Chip>
}
