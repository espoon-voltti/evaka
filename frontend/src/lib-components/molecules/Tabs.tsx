// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { NavLink } from 'react-router-dom'
import styled from 'styled-components'
import { defaultMargins } from '../white-space'
import Container from '../layout/Container'

type Props = {
  tabs: Array<{
    id: string
    link: string
    label: string | JSX.Element
    counter?: number
  }>
  mobile?: boolean
}

export default React.memo(function Tabs({ tabs, mobile }: Props) {
  const maxWidth = mobile ? `${100 / tabs.length}vw` : undefined
  return (
    <Background>
      <Container>
        <TabsContainer>
          {tabs.map(({ id, link, label, counter }) => (
            <TabContainer
              key={id}
              to={link}
              data-qa={`${id}-tab`}
              $maxWidth={maxWidth}
              $mobile={mobile}
            >
              <TabTitle $mobile={mobile}>{label}</TabTitle>
              {counter ? <TabCounter>{counter}</TabCounter> : null}
            </TabContainer>
          ))}
        </TabsContainer>
      </Container>
    </Background>
  )
})

const Background = styled.div`
  background-color: ${({ theme: { colors } }) => colors.greyscale.white};
`

const TabsContainer = styled.div`
  display: flex;
  flex-direction: row;
`

interface TabContainerProps {
  $maxWidth?: string
  $mobile?: boolean
}

const TabContainer = styled(NavLink)<TabContainerProps>`
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-content: center;
  padding: 12px;
  flex-basis: content;
  flex-grow: 1;
  background-color: ${({ theme: { colors } }) => colors.greyscale.white};
  font-family: ${(p) =>
    p.$mobile ? 'Open Sans, sans-serif' : 'Montserrat, sans-serif'};
  font-size: ${(p) => (p.$mobile ? '14px' : '15px')};
  text-align: center;
  text-transform: uppercase;
  letter-spacing: 1px;
  max-width: ${(p) => (p.$maxWidth ? p.$maxWidth : 'none')};

  &.active {
    background-color: ${({ theme: { colors }, ...p }) =>
      p.$mobile ? colors.greyscale.white : `${colors.main.light}33`};
    border-bottom: ${({ theme: { colors }, ...p }) =>
      p.$mobile ? `3px solid ${colors.main.medium}` : 'none'};

    span {
      div {
        color: ${({ theme: { colors }, ...p }) =>
          p.$mobile ? colors.main.medium : colors.greyscale.dark};
      }
    }
  }
`

const TabTitle = styled.span<TabContainerProps>`
  color: ${({ theme: { colors } }) => colors.greyscale.dark};

  &.active {
    font-weight: 700;
  }
`

const TabCounter = styled.span`
  height: 21px;
  padding: 0 8px;
  border-radius: 10px;
  background-color: ${({ theme: { colors } }) => colors.accents.orange};
  color: ${({ theme: { colors } }) => colors.greyscale.white};
  margin-left: ${defaultMargins.s};
  font-weight: 700;
`
