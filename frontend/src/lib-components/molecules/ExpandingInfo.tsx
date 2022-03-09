// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { ReactNode, useState } from 'react'
import styled, { useTheme } from 'styled-components'

import Container, { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { fasInfo, faTimes } from 'lib-icons'

import RoundIcon from '../atoms/RoundIcon'
import IconButton from '../atoms/buttons/IconButton'
import { desktopMin } from '../breakpoints'
import { defaultMargins, SpacingSize } from '../white-space'

const InfoBoxContainer = styled(Container)<{ fullWidth?: boolean }>`
  ${({ fullWidth }) => fullWidth && `width: 100%;`}

  @keyframes open {
    from {
      max-height: 0;
    }
    to {
      max-height: 100px;
    }
  }

  background-color: ${(p) => p.theme.colors.main.m4};
  overflow: hidden;
  ${({ fullWidth }) =>
    fullWidth
      ? `margin: ${defaultMargins.s} 0px;`
      : `margin: ${defaultMargins.s} -${defaultMargins.s} ${defaultMargins.xs};`}

  @media (min-width: ${desktopMin}) {
    animation-name: open;
    animation-duration: 0.2s;
    animation-timing-function: ease-out;
    ${({ fullWidth }) =>
      fullWidth
        ? `margin: ${defaultMargins.s} 0px;`
        : `margin: ${defaultMargins.s} -${defaultMargins.L} ${defaultMargins.xs};`}
  }
`

const InfoBoxContentArea = styled(ContentArea)`
  display: flex;
`

const InfoContainer = styled.div`
  flex-grow: 1;
  color: ${(p) => p.theme.colors.grayscale.g100};
  padding: 0 ${defaultMargins.s};
`

const RoundIconButton = styled.button<{ margin: SpacingSize }>`
  display: inline-flex;
  justify-content: center;
  align-items: center;
  padding: 0;
  margin: 0;
  margin-top: ${({ margin }) => defaultMargins[margin]};
  cursor: pointer;
  background-color: ${(p) => p.theme.colors.main.m2};
  color: ${(p) => p.theme.colors.grayscale.g0};
  border-radius: 100%;
  border: none;
  width: 20px;
  height: 20px;
  min-width: 20px;
  min-height: 20px;
  max-width: 20px;
  max-height: 20px;
  font-size: 12px;
  line-height: normal;

  @media (hover: hover) {
    &:hover {
      background-color: ${(p) => p.theme.colors.main.m2Hover};
    }
  }

  &:focus {
    box-shadow: 0 0 0 2px ${(p) => p.theme.colors.grayscale.g0},
      0 0 0 4px ${(p) => p.theme.colors.main.m2Focus};
  }
`

type ExpandingInfoProps = {
  children: React.ReactNode
  info: ReactNode
  ariaLabel: string
  margin?: SpacingSize
  fullWidth?: boolean
  'data-qa'?: string
}

export default React.memo(function ExpandingInfo({
  children,
  info,
  ariaLabel,
  margin,
  fullWidth,
  'data-qa': dataQa
}: ExpandingInfoProps) {
  const [expanded, setExpanded] = useState<boolean>(false)

  return (
    <span aria-live="polite">
      <FixedSpaceRow spacing="xs" alignItems="center">
        <div>{children}</div>
        <InfoButton
          onClick={() => setExpanded(!expanded)}
          aria-label={ariaLabel}
          margin={margin ?? 'zero'}
          data-qa={dataQa}
        />
      </FixedSpaceRow>
      {expanded && (
        <ExpandingInfoBox
          info={info}
          close={() => setExpanded(false)}
          fullWidth={fullWidth}
          data-qa={dataQa}
        />
      )}
    </span>
  )
})

export const InfoButton = React.memo(function InfoButton({
  onClick,
  'aria-label': ariaLabel,
  margin,
  className,
  'data-qa': dataQa
}: {
  onClick: () => void
  'aria-label': string
  margin?: SpacingSize
  className?: string
  'data-qa'?: string
}) {
  const { colors } = useTheme()

  return (
    <RoundIconButton
      className={className}
      data-qa={dataQa}
      margin={margin ?? 'zero'}
      color={colors.status.info}
      onClick={onClick}
      role="button"
      aria-label={ariaLabel}
    >
      <FontAwesomeIcon icon={fasInfo} />
    </RoundIconButton>
  )
})

export const ExpandingInfoBox = React.memo(function ExpandingInfoBox({
  info,
  close,
  fullWidth,
  className,
  'data-qa': dataQa
}: {
  info: ReactNode
  close: () => void
  fullWidth?: boolean
  className?: string
  'data-qa'?: string
}) {
  const { colors } = useTheme()

  return (
    <InfoBoxContainer className={className} fullWidth={fullWidth}>
      <InfoBoxContentArea opaque={false}>
        <RoundIcon content={fasInfo} color={colors.status.info} size="s" />

        <InfoContainer data-qa={dataQa ? `${dataQa}-text` : undefined}>
          {info}
        </InfoContainer>

        <IconButton onClick={close} icon={faTimes} gray />
      </InfoBoxContentArea>
    </InfoBoxContainer>
  )
})
