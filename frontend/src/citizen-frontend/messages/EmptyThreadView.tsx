// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faInbox } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'

import { tabletMin } from 'lib-components/breakpoints'
import { H3 } from 'lib-components/typography'
import colors from 'lib-customizations/common'

import { useTranslation } from '../localization'

interface Props {
  inboxEmpty: boolean
}

export default React.memo(function EmptyThreadView({ inboxEmpty }: Props) {
  const i18n = useTranslation()
  return inboxEmpty ? (
    <EmptyThreadViewContainer data-qa="inbox-empty">
      <FontAwesomeIcon icon={faInbox} size="7x" color={colors.grayscale.g35} />
      <H3>{i18n.messages.emptyInbox}</H3>
    </EmptyThreadViewContainer>
  ) : (
    <EmptyThreadViewContainer>
      <H3>{i18n.messages.noSelectedMessage}</H3>
    </EmptyThreadViewContainer>
  )
})

const EmptyThreadViewContainer = styled.div`
  text-align: center;
  width: 100%;
  background: ${colors.grayscale.g0};
  padding-top: 10%;

  display: none;
  @media (min-width: ${tabletMin}) {
    display: block;
  }
`
