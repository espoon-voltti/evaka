// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { customColours, EspooColours } from 'utils/colours'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faCheck } from '@evaka/lib-icons'

export const Chips = styled.div`
  > button {
    margin-left: 10px;
  }

  > button:first-child {
    margin-left: 0;
  }
`

const ChipContainer = styled.button<{ active: boolean }>`
  background: ${({ active }) =>
    active ? customColours.bluePrimary : EspooColours.white};
  color: ${({ active }) =>
    active ? EspooColours.white : customColours.bluePrimary};
  height: 30px;
  border: ${customColours.bluePrimary} solid 1px;
  border-radius: 15px;
  padding: 0 10px;
  cursor: pointer;

  svg {
    margin-right: 8px;
  }

  span {
    font-weight: 600;
  }
`

interface Props {
  text: string
  active: boolean
  onClick?: () => undefined | void
  dataQa?: string
}

function Chip({ text, active, onClick, dataQa }: Props) {
  return (
    <ChipContainer active={active} onClick={onClick} data-qa={dataQa}>
      {active && (
        <FontAwesomeIcon icon={faCheck} color={EspooColours.white} size="lg" />
      )}
      <span>{text}</span>
    </ChipContainer>
  )
}

export default Chip
