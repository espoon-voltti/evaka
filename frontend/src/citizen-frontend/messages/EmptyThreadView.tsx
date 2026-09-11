// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faInbox } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'

import type { Result } from 'lib-common/api'
import type { CitizenMessageThread } from 'lib-common/generated/api-types/messaging'
import { tabletMin } from 'lib-components/breakpoints'
import { H2 } from 'lib-components/typography'
import colors from 'lib-customizations/common'

import { renderResult } from '../async-rendering'
import { useTranslation } from '../localization'

export default React.memo(function EmptyThreadView({
  threads
}: {
  threads: Result<CitizenMessageThread[]>
}) {
  const i18n = useTranslation()
  return (
    <EmptyThreadViewContainer>
      {renderResult(threads, (threads, isReloading) =>
        threads.length === 0 ? (
          <>
            <FontAwesomeIcon
              icon={faInbox}
              size="7x"
              color={colors.grayscale.g35}
            />
            <H2 data-qa="inbox-empty" data-loading={isReloading}>
              {i18n.messages.emptyInbox}
            </H2>
          </>
        ) : (
          <H2>{i18n.messages.noSelectedMessage}</H2>
        )
      )}
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
