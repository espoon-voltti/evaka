// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import styled from 'styled-components'

import { desktopMin } from 'lib-components/breakpoints'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'

import { useUser } from '../auth/state'
import { headerHeightMobile } from '../navigation/const'
import { OverlayContext } from '../overlay/state'

import type { Suggestion } from './dismissal'
import { dismissSuggestion, isSuggestionDismissed } from './dismissal'

export function useSuggestionStage(suggestion: Suggestion) {
  const user = useUser()
  const { modalOpen } = useContext(OverlayContext)
  const [stage, setStage] = useState<'suggestion' | 'note' | 'hidden'>(
    'suggestion'
  )

  // Hide while any modal is open, for example until the "Application sent"
  // modal is dismissed
  const hidden =
    modalOpen || !user || isSuggestionDismissed(suggestion, user.id)

  return {
    stage: hidden ? 'hidden' : stage,
    showNote: () => setStage('note'),
    dismiss: () => {
      if (user) dismissSuggestion(suggestion, user.id)
      setStage('hidden')
    }
  }
}

export const SuggestionNote = React.memo(function SuggestionNote({
  text,
  onClick,
  'data-qa': dataQa
}: {
  text: string
  onClick: () => void
  'data-qa': string
}) {
  return (
    <Banner data-qa={dataQa}>
      <Note onClick={onClick} data-qa={`${dataQa}-action`}>
        {text}
      </Note>
    </Banner>
  )
})

/** The card pinned below the header that suggests installing the app or enabling push */
export const Banner = styled.div`
  position: sticky;
  top: ${headerHeightMobile}px;
  z-index: 10;
  background-color: ${(p) => p.theme.colors.main.m4};
  padding: ${defaultMargins.s};

  @media (min-width: ${desktopMin}) {
    top: 0;
  }

  html[data-standalone] & {
    top: 0;
  }
`

const Note = styled.button`
  width: 100%;
  text-align: left;
  padding: 0;
  border: none;
  background: none;
  font: inherit;
  color: inherit;
  cursor: pointer;
`

export const Row = styled.div`
  display: flex;
  align-items: flex-start;
  gap: ${defaultMargins.s};
`

export const Texts = styled.div`
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: ${defaultMargins.xs};
  flex-grow: 1;
`

export const Title = styled.span`
  font-weight: ${fontWeights.semibold};
`

export const Panel = styled.div`
  margin-top: ${defaultMargins.s};
  padding: ${defaultMargins.s};
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  border-radius: 8px;
`
