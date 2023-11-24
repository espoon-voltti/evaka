// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { NavLink } from 'react-router-dom'
import styled, { css } from 'styled-components'

import { desktopMin } from '../breakpoints'
import Container from '../layout/Container'
import { fontWeights, NavLinkText } from '../typography'
import { BaseProps } from '../utils'
import { defaultMargins } from '../white-space'

interface Tab {
  id: string
  onClick: () => void
  label: string | React.JSX.Element
  counter?: number
}

interface TabsProps extends BaseProps {
  mobile?: boolean
  active: string
  tabs: Tab[]
  id?: string
}

export const Tabs = React.memo(function Tabs({
  mobile,
  'data-qa': dataQa,
  active,
  tabs,
  id
}: TabsProps) {
  const maxWidth = mobile ? `${100 / tabs.length}vw` : undefined
  return (
    <Container>
      <TabsContainer data-qa={dataQa} shadow={mobile} id={id}>
        {tabs.map(({ id, onClick, label, counter }) => (
          <Tab
            key={id}
            onClick={onClick}
            data-qa={`${id}-tab`}
            className={active === id ? 'active' : undefined}
            $maxWidth={maxWidth}
            $mobile={mobile}
          >
            <NavLinkText>{label}</NavLinkText>
            {counter ? <TabCounter>{counter}</TabCounter> : null}
          </Tab>
        ))}
      </TabsContainer>
    </Container>
  )
})

interface TabLink {
  id: string
  link: string
  label: string | React.JSX.Element
  counter?: number
}

interface TabLinksProps extends BaseProps {
  mobile?: boolean
  tabs: TabLink[]
  id?: string
}

export const TabLinks = React.memo(function TabLinks({
  mobile,
  'data-qa': dataQa,
  tabs,
  id
}: TabLinksProps) {
  const maxWidth = mobile ? `${100 / tabs.length}%` : undefined
  return (
    <Container>
      <TabsContainer data-qa={dataQa} shadow={mobile} id={id}>
        {tabs.map(({ id, link, label, counter }) => (
          <TabLink
            key={id}
            to={link}
            data-qa={`${id}-tab`}
            $maxWidth={maxWidth}
            $mobile={mobile}
          >
            <NavLinkText>{label}</NavLinkText>
            {counter ? <TabCounter>{counter}</TabCounter> : null}
          </TabLink>
        ))}
      </TabsContainer>
    </Container>
  )
})

const TabsContainer = styled.nav<{ shadow?: boolean }>`
  display: flex;
  flex-direction: row;
  ${(p) =>
    p.shadow
      ? `
      box-shadow: 0 2px 6px 0 ${p.theme.colors.grayscale.g15};
      margin-bottom: ${defaultMargins.xxs};
      `
      : ''}
`

const tabStyles = css<{
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
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  text-align: center;
  max-width: ${(p) => p.$maxWidth ?? 'unset'};

  min-height: 60px;
  @media (min-width: ${desktopMin}) {
    min-height: 48px;
  }

  outline: none;
  ${(p) =>
    p.$mobile
      ? css`
          border-bottom: 3px solid transparent;
        `
      : css`
          border: 2px solid transparent;

          :focus {
            border-color: ${p.theme.colors.main.m1};
          }
        `}

  &.active {
    background-color: ${(p) =>
      p.$mobile ? p.theme.colors.grayscale.g0 : `${p.theme.colors.main.m3}33`};

    ${(p) =>
      p.$mobile
        ? css`
            border-bottom: 3px solid ${p.theme.colors.main.m1};
          `
        : ''}

    ${NavLinkText} {
      color: ${(p) => p.theme.colors.main.m1};
      font-weight: ${fontWeights.bold};
    }
  }

  @media (hover: hover) {
    :hover {
      ${NavLinkText} {
        color: ${(p) => p.theme.colors.main.m1};
      }
    }
  }
`

const Tab = styled.div<{
  $maxWidth?: string
  $mobile?: boolean
}>`
  ${tabStyles}
`

const TabLink = styled(NavLink)<{
  $maxWidth?: string
  $mobile?: boolean
}>`
  ${tabStyles}
`

const TabCounter = styled.div`
  width: 1.5em;
  height: 1.5em;
  display: inline-flex;
  justify-content: center;
  align-items: center;

  border-radius: 50%;
  background-color: ${(p) => p.theme.colors.status.warning};
  color: ${(p) => p.theme.colors.grayscale.g0};
  margin-left: ${defaultMargins.xs};
  font-weight: ${fontWeights.bold};
  font-size: 1em;
`
