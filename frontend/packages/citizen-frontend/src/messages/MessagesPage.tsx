// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import { Loading, Paged, Result, Success } from '@evaka/lib-common/src/api'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'
import { tabletMin } from '@evaka/lib-components/src/breakpoints'
import Container from '@evaka/lib-components/src/layout/Container'
import AdaptiveFlex from '@evaka/lib-components/src/layout/AdaptiveFlex'
import { Gap } from '@evaka/lib-components/src/white-space'
import { ReceivedBulletin } from '../messages/types'
import { getBulletins, markBulletinRead } from '../messages/api'
import MessagesList from '../messages/MessagesList'
import MessageReadView from '../messages/MessageReadView'
import styled from 'styled-components'
import { HeaderState, HeaderContext } from './state'

export default React.memo(function MessagesPage() {
  const { refreshUnreadBulletinsCount } = useContext<HeaderState>(HeaderContext)
  const [bulletinsState, setBulletinsState] = useState<BulletinsState>(
    initialState
  )
  const [activeBulletin, setActiveBulletin] = useState<ReceivedBulletin | null>(
    null
  )
  const setBulletinsResult = useCallback(
    (result: Result<Paged<ReceivedBulletin>>) =>
      setBulletinsState((state) => {
        if (result.isSuccess) {
          return {
            ...state,
            bulletins: [...state.bulletins, ...result.value.data],
            nextPage: Success.of(undefined),
            total: result.value.total,
            pages: result.value.pages
          }
        }

        if (result.isFailure) {
          return {
            ...state,
            nextPage: result.map(() => undefined)
          }
        }

        return state
      }),
    [setBulletinsState]
  )

  const loadBulletins = useRestApi(getBulletins, setBulletinsResult)
  useEffect(() => {
    setBulletinsState((state) => ({ ...state, nextPage: Loading.of() }))
    loadBulletins(bulletinsState.currentPage)
  }, [bulletinsState.currentPage])

  const loadNextPage = () =>
    setBulletinsState((state) => {
      if (state.currentPage < state.pages) {
        return {
          ...state,
          currentPage: state.currentPage + 1
        }
      }
      return state
    })

  const openBulletin = (bulletin: ReceivedBulletin) => {
    setActiveBulletin(bulletin)

    if (bulletin.isRead) return

    void markBulletinRead(bulletin.id).then(() => {
      refreshUnreadBulletinsCount()
      setActiveBulletin((b) =>
        b?.id === bulletin.id ? { ...b, isRead: true } : b
      )

      setBulletinsState(({ bulletins, ...state }) => ({
        ...state,
        bulletins: bulletins.map((b) =>
          b.id === bulletin.id ? { ...b, isRead: true } : b
        )
      }))
    })
  }

  return (
    <Container>
      <Gap size="s" />
      <StyledFlex breakpoint={tabletMin} horizontalSpacing="L">
        <MessagesList
          bulletins={bulletinsState.bulletins}
          nextPage={bulletinsState.nextPage}
          activeBulletin={activeBulletin}
          onClickBulletin={openBulletin}
          onReturn={() => setActiveBulletin(null)}
          loadNextPage={loadNextPage}
        />
        {activeBulletin && <MessageReadView bulletin={activeBulletin} />}
      </StyledFlex>
    </Container>
  )
})

interface BulletinsState {
  bulletins: ReceivedBulletin[]
  nextPage: Result<void>
  currentPage: number
  pages: number
  total?: number
}

const initialState: BulletinsState = {
  bulletins: [],
  nextPage: Loading.of(),
  currentPage: 1,
  pages: 0
}

const StyledFlex = styled(AdaptiveFlex)`
  align-items: stretch;
`
