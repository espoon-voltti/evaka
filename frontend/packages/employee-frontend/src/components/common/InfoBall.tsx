// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faInfo } from '@fortawesome/free-solid-svg-icons'
import { EspooColours } from '~utils/colours'

interface ContainerProps {
  inline: boolean
}
const Container = styled.div<ContainerProps>`
  display: inline-block;
  padding: ${(props) => (props.inline ? '0 8px' : '8px')};
  cursor: pointer;
`

const Background = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  background: ${EspooColours.espooTurqoise};
  height: 20px;
  width: 20px;
  border-radius: 100%;
`

type Props = {
  text: string
  inline?: boolean
}

const InfoBall = React.memo(function InfoBall({ text, inline = false }: Props) {
  return (
    <Container title={text} inline={inline}>
      <Background>
        <FontAwesomeIcon size="xs" icon={faInfo} color={EspooColours.white} />
      </Background>
    </Container>
  )
})

export default InfoBall
