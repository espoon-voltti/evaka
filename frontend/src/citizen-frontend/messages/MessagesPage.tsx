// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { tabletMin } from 'lib-components/breakpoints'
import AdaptiveFlex from 'lib-components/layout/AdaptiveFlex'
import Container from 'lib-components/layout/Container'
import { defaultMargins } from 'lib-components/white-space'
import React, { useContext, useEffect } from 'react'
import styled from 'styled-components'
import ErrorSegment from '../../lib-components/atoms/state/ErrorSegment'
import { SpinnerSegment } from '../../lib-components/atoms/state/Spinner'
import { headerHeight } from '../header/const'
import { MessageContext } from './state'
import ThreadList from './ThreadList'
import ThreadView from './ThreadView'

const FullHeightContainer = styled(Container)`
  height: calc(100vh - ${headerHeight});
`

const StyledFlex = styled(AdaptiveFlex)`
  align-items: stretch;
  position: absolute;
  top: ${defaultMargins.s};
  right: 0;
  bottom: 0;
  left: 0;
`

export default React.memo(function MessagesPage() {
  const { accountId, loadAccount, selectedThread } = useContext(MessageContext)
  useEffect(() => {
    if (!accountId.isSuccess) {
      loadAccount()
    }
  }, [accountId, loadAccount])

  return (
    <FullHeightContainer>
      <StyledFlex breakpoint={tabletMin} horizontalSpacing="L">
        {accountId.mapAll({
          failure() {
            return <ErrorSegment />
          },
          loading() {
            return <SpinnerSegment />
          },
          success(id) {
            return (
              <>
                <ThreadList accountId={id} />
                {selectedThread && (
                  <ThreadView accountId={id} thread={selectedThread} />
                )}
              </>
            )
          }
        })}
      </StyledFlex>
    </FullHeightContainer>
  )
})
