// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import type { MessageType } from 'lib-common/generated/api-types/messaging'
import { StaticChip } from 'lib-components/atoms/Chip'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { colors } from 'lib-customizations/common'

// TODO is the 20px line-height in StaticChip unintentional?
const Chip = styled(StaticChip)`
  line-height: 16px;
`

const chipColors: Record<MessageType, string> = {
  MESSAGE: colors.accents.a4violet,
  BULLETIN: colors.main.m1
}

interface Props {
  type: MessageType
  urgent: boolean
  labels: Record<MessageType, string>
}

export function MessageCharacteristics({ type, urgent, labels }: Props) {
  return (
    <FixedSpaceRow spacing="xs" alignItems="center">
      {urgent && (
        <RoundIcon
          data-qa="urgent"
          size="m"
          color={colors.status.danger}
          content="!"
        />
      )}
      <Chip color={chipColors[type]}>{labels[type]}</Chip>
    </FixedSpaceRow>
  )
}
