import React from 'react'
import InlineButton, { InlineButtonProps } from './InlineButton'
import styled from 'styled-components'
import { tabletMin } from '../../breakpoints'

interface ResponsiveInlineButtonProps extends InlineButtonProps {
  breakpoint?: string
}

const ResponsiveText = styled.span<{ breakpoint: string }>`
  @media (max-width: ${(p) => p.breakpoint}) {
    display: none;
  }
`

function ResponsiveInlineButton({
  breakpoint = tabletMin,
  text,
  ...props
}: ResponsiveInlineButtonProps) {
  return (
    <InlineButton
      text={<ResponsiveText breakpoint={breakpoint}>{text}</ResponsiveText>}
      {...props}
    />
  )
}

export default ResponsiveInlineButton
