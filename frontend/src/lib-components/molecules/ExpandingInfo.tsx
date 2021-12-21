// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { fasInfo, faTimes } from 'lib-icons'
import React, { ReactNode, useState } from 'react'
import styled, { useTheme } from 'styled-components'
import RoundIcon from '../atoms/RoundIcon'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { defaultMargins, SpacingSize } from '../white-space'
import { tabletMin } from '../breakpoints'
import IconButton from '../atoms/buttons/IconButton'

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

  background-color: ${({ theme: { colors } }) => colors.main.lighter};
  overflow: hidden;
  ${({ fullWidth }) =>
    fullWidth
      ? `margin: ${defaultMargins.s} 0px;`
      : `margin: ${defaultMargins.s} -${defaultMargins.s} ${defaultMargins.xs};`}

  @media (min-width: ${tabletMin}) {
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
  color: ${({ theme: { colors } }) => colors.greyscale.darkest};
  padding: 0 ${defaultMargins.s};
`

const RoundIconWithMargin = styled(RoundIcon)<{ margin: SpacingSize }>`
  margin-top: ${({ margin }) => defaultMargins[margin]};
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
      <FixedSpaceRow spacing="xs" alignItems={'center'}>
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
    <RoundIconWithMargin
      className={className}
      data-qa={dataQa}
      margin={margin ?? 'zero'}
      content={fasInfo}
      color={colors.main.light}
      size="s"
      onClick={onClick}
      tabindex={0}
      role="button"
      aria-label={ariaLabel}
    />
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
        <RoundIcon content={fasInfo} color={colors.main.light} size="s" />

        <InfoContainer data-qa={dataQa ? `${dataQa}-text` : undefined}>
          {info}
        </InfoContainer>

        <IconButton onClick={close} icon={faTimes} gray />
      </InfoBoxContentArea>
    </InfoBoxContainer>
  )
})
