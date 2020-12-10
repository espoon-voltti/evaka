// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { NavLink } from 'react-router-dom'
import Colors from '~components/shared/Colors'
import { DefaultMargins } from '~components/shared/layout/white-space'
import Container from '~components/shared/layout/Container'

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
  background-color: ${Colors.greyscale.white};
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
  background-color: ${Colors.greyscale.white};
  font-family: ${(p) =>
    p.$mobile ? 'Open Sans, sans-serif' : 'Montserrat, sans-serif'};
  font-size: ${(p) => (p.$mobile ? '14px' : '15px')};
  text-align: center;
  text-transform: uppercase;
  letter-spacing: 1px;
  max-width: ${(p) => (p.$maxWidth ? p.$maxWidth : 'none')};

  &.active {
    background-color: ${(p) =>
      p.$mobile ? Colors.greyscale.white : `${Colors.blues.light}33`};
    border-bottom: ${(p) =>
      p.$mobile ? `3px solid ${Colors.blues.medium}` : 'none'};

    span {
      div {
        color: ${(p) =>
          p.$mobile ? Colors.blues.medium : Colors.greyscale.dark};
      }
    }
  }
`

const TabTitle = styled.span<TabContainerProps>`
  color: ${Colors.greyscale.dark};

  &.active {
    font-weight: 700;
  }
`

const TabCounter = styled.span`
  height: 21px;
  padding: 0 8px;
  border-radius: 10px;
  background-color: ${Colors.accents.orange};
  color: ${Colors.greyscale.white};
  margin-left: ${DefaultMargins.s};
  font-weight: 700;
`
