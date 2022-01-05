// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { NavLink } from 'react-router-dom'
import styled from 'styled-components'
import { desktopMin } from '../breakpoints'
import Container from '../layout/Container'
import { fontWeights, NavLinkText } from '../typography'
import { BaseProps } from '../utils'
import { defaultMargins } from '../white-space'

interface Tab {
  id: string
  link: string
  label: string | JSX.Element
  counter?: number
}

interface Props extends BaseProps {
  mobile?: boolean
  tabs: Tab[]
}

export default React.memo(function Tabs({
  mobile,
  'data-qa': dataQa,
  tabs
}: Props) {
  const maxWidth = mobile ? `${100 / tabs.length}vw` : undefined
  return (
    <Container>
      <TabsContainer data-qa={dataQa} shadow={mobile}>
        {tabs.map(({ id, link, label, counter }) => (
          <TabLinkContainer
            key={id}
            to={link}
            data-qa={`${id}-tab`}
            $maxWidth={maxWidth}
            $mobile={mobile}
          >
            <NavLinkText>{label}</NavLinkText>
            {counter ? <TabCounter>{counter}</TabCounter> : null}
          </TabLinkContainer>
        ))}
      </TabsContainer>
    </Container>
  )
})

const TabsContainer = styled.div<{ shadow?: boolean }>`
  display: flex;
  flex-direction: row;
  ${(p) =>
    p.shadow
      ? `
      box-shadow: 0 2px 6px 0 ${p.theme.colors.greyscale.lighter};
      margin-bottom: ${defaultMargins.xxs};
      `
      : ''}
`

const TabLinkContainer = styled(NavLink)<{
  $maxWidth?: string
  $mobile?: boolean
}>`
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  padding: 12px;
  flex-basis: content;
  flex-grow: 1;
  background-color: ${(p) => p.theme.colors.greyscale.white};
  text-align: center;
  max-width: ${(p) => p.$maxWidth ?? 'unset'};

  min-height: 60px;
  @media (min-width: ${desktopMin}) {
    min-height: 48px;
  }

  border-bottom: ${(p) => (p.$mobile ? '3px solid transparent' : 'unset')};

  &.active {
    background-color: ${({ theme: { colors }, ...p }) =>
      p.$mobile ? colors.greyscale.white : `${colors.main.light}33`};
    border-bottom: ${({ theme: { colors }, ...p }) =>
      p.$mobile ? `3px solid ${colors.main.dark}` : 'unset'};

    ${NavLinkText} {
      color: ${(p) => p.theme.colors.main.dark};
      font-weight: ${fontWeights.bold};
    }
  }

  :hover {
    ${NavLinkText} {
      color: ${(p) => p.theme.colors.main.dark};
    }
  }
`

const TabCounter = styled.div`
  width: 1.5em;
  height: 1.5em;
  display: inline-flex;
  justify-content: center;
  align-items: center;

  border-radius: 50%;
  background-color: ${({ theme: { colors } }) => colors.accents.warningOrange};
  color: ${({ theme: { colors } }) => colors.greyscale.white};
  margin-left: ${defaultMargins.xs};
  font-weight: ${fontWeights.bold};
  font-size: 1em;
`
