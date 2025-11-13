// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'

import { tabletMin } from '../breakpoints'
import { defaultMargins } from '../white-space'

type Props = {
  justify?: 'flex-start' | 'flex-end' | 'center' | 'space-between'
  children: React.ReactNode
}

const StyledContainer = styled.div<{
  justify?: 'flex-start' | 'flex-end' | 'center' | 'space-between'
}>`
  display: flex;
  flex-wrap: nowrap;
  align-items: stretch;

  flex-direction: column;
  justify-content: center;

  > * {
    margin: 0;
  }

  @media (min-width: ${tabletMin}) {
    flex-direction: row;
    justify-content: ${({ justify: align }) => align ?? 'flex-end'};

    > *:not(:last-child) {
      margin-right: ${defaultMargins.s};
    }
  }

  @media (max-width: calc(${tabletMin} + -1px)) {
    > *:not(:last-child) {
      margin-bottom: ${defaultMargins.s};
    }
  }
`

export default function ButtonContainer({ justify, children }: Props) {
  const [isWideScreen, setIsWideScreen] = useState(
    () => window.matchMedia(`(min-width: ${tabletMin})`).matches
  )

  useEffect(() => {
    const mediaQuery = window.matchMedia(`(min-width: ${tabletMin})`)
    const handleChange = (e: MediaQueryListEvent) => setIsWideScreen(e.matches)

    mediaQuery.addEventListener('change', handleChange)
    return () => mediaQuery.removeEventListener('change', handleChange)
  }, [])

  const orderedChildren = useMemo(
    () =>
      isWideScreen ? React.Children.toArray(children).reverse() : children,
    [isWideScreen, children]
  )

  return <StyledContainer justify={justify}>{orderedChildren}</StyledContainer>
}
