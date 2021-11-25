// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { NavLink } from 'react-router-dom'
import styled from 'styled-components'
import { fontWeights } from '../typography'
import { defaultMargins } from '../white-space'
import Container from '../layout/Container'

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
    <Background>
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
                  <TabTitle $mobile={mobile}>{label}</TabTitle>
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
                  <TabTitle $mobile={mobile}>{label}</TabTitle>
                  {counter ? <TabCounter>{counter}</TabCounter> : null}
                </TabLinkContainer>
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
  ${({ $mobile }) => ($mobile ? `line-height: 16px;` : '')};

  &.active {
    font-weight: ${fontWeights.bold};
  }
`

const TabCounter = styled.div`
  width: 1.5em;
  height: 1.5em;
  display: inline-flex;
  justify-content: center;
  align-items: center;

  border-radius: 50%;
  background-color: ${({ theme: { colors } }) => colors.accents.orange};
  color: ${({ theme: { colors } }) => colors.greyscale.white};
  margin-left: ${defaultMargins.xs};
  font-weight: ${fontWeights.bold};
  font-size: 1em;
`
