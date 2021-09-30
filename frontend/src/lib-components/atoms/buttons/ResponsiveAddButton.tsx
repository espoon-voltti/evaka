import React from 'react'
import AddButton, { AddButtonProps } from './AddButton'
import styled from 'styled-components'
import { tabletMin } from '../../breakpoints'

interface ResponsiveAddButtonProps extends AddButtonProps {
  breakpoint?: string
}

const ResponsiveText = styled.span<{ breakpoint: string }>`
  @media (max-width: ${(p) => p.breakpoint}) {
    display: none;
  }
`

function ResponsiveAddButton({
  breakpoint = tabletMin,
  text,
  ...props
}: ResponsiveAddButtonProps) {
  return (
    <AddButton
      text={<ResponsiveText breakpoint={breakpoint}>{text}</ResponsiveText>}
      {...props}
    />
  )
}

export default ResponsiveAddButton
