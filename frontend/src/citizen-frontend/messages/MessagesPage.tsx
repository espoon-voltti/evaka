// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { tabletMin } from 'lib-components/breakpoints'
import AdaptiveFlex from 'lib-components/layout/AdaptiveFlex'
import Container from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'
import React, { useContext, useEffect } from 'react'
import styled from 'styled-components'
import ErrorSegment from '../../lib-components/atoms/state/ErrorSegment'
import { SpinnerSegment } from '../../lib-components/atoms/state/Spinner'
import { MessageContext } from './state'
import ThreadList from './ThreadList'
import ThreadView from './ThreadView'

export default React.memo(function MessagesPage() {
  const { account, loadAccount, selectedThread } = useContext(MessageContext)
  useEffect(() => {
    if (!account.isSuccess) {
      loadAccount()
    }
  }, [account, loadAccount])

  return (
    <Container>
      <Gap size="s" />
      <StyledFlex breakpoint={tabletMin} horizontalSpacing="L">
        {account.mapAll({
          failure() {
            return <ErrorSegment />
          },
          loading() {
            return <SpinnerSegment />
          },
          success(acc) {
            return (
              <>
                <ThreadList account={acc} />
                {selectedThread && (
                  <ThreadView account={acc} thread={selectedThread} />
                )}
              </>
            )
          }
        })}
      </StyledFlex>
    </Container>
  )
})

const StyledFlex = styled(AdaptiveFlex)`
  align-items: stretch;
`
