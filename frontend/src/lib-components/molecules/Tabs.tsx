// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { NavLink } from 'react-router-dom'
import styled from 'styled-components'
import Container from '../layout/Container'
import { fontWeights, NavLinkText } from '../typography'
import { defaultMargins } from '../white-space'

interface CommonProps {
  mobile?: boolean
  dataQa?: string
}

interface LinkTabProps extends CommonProps {
  tabs: Array<{
    id: string
    link: string
    label: string | JSX.Element
    counter?: number
  }>
  type?: 'links'
}

interface ButtonTabProps extends CommonProps {
  tabs: Array<{
    id: string
    onClick: () => void
    label: string | JSX.Element
    active: boolean
    counter?: number
  }>
  type: 'buttons'
}

type Props = LinkTabProps | ButtonTabProps

function usesButtons(props: Props): props is ButtonTabProps {
  return props.type === 'buttons'
}

export default React.memo(function Tabs(props: Props) {
  const { mobile, dataQa } = props
  const maxWidth = mobile ? `${100 / props.tabs.length}vw` : undefined
  return (
    <Container>
      <TabsContainer data-qa={dataQa}>
        {usesButtons(props)
          ? props.tabs.map(({ id, onClick, active, label, counter }) => (
              <TabButtonContainer
                key={id}
                onClick={onClick}
                data-qa={`${id}-tab`}
                $maxWidth={maxWidth}
                $mobile={mobile}
                className={active ? 'active' : undefined}
              >
                <NavLinkText>{label}</NavLinkText>
                {counter ? <TabCounter>{counter}</TabCounter> : null}
              </TabButtonContainer>
            ))
          : props.tabs.map(({ id, link, label, counter }) => (
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

const TabsContainer = styled.div`
  display: flex;
  flex-direction: row;
`

interface TabContainerProps {
  $maxWidth?: string
  $mobile?: boolean
}

const TabLinkContainer = styled(NavLink)<TabContainerProps>`
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
  max-width: ${(p) => p.$maxWidth ?? 'none'};

  &.active {
    background-color: ${({ theme: { colors }, ...p }) =>
      p.$mobile ? colors.greyscale.white : `${colors.main.light}33`};
    border-bottom: ${({ theme: { colors }, ...p }) =>
      p.$mobile ? `3px solid ${colors.main.dark}` : 'none'};

    ${NavLinkText} {
      color: ${(p) => p.theme.colors.main.dark};
      font-weight: ${fontWeights.bold};
    }
  }
`

const TabButtonContainer = styled.button<TabContainerProps>`
  outline: none !important;
  border: none;

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
      p.$mobile ? `3px solid ${colors.main.dark}` : 'none'};

    ${NavLinkText} {
      color: ${(p) => p.theme.colors.main.dark};
      font-weight: ${fontWeights.bold};
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
