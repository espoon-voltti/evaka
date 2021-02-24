// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { Loading, Result, Success } from '@evaka/lib-common/src/api'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'
import Container from '@evaka/lib-components/src/layout/Container'
import AdaptiveFlex from '@evaka/lib-components/src/layout/AdaptiveFlex'
import { Gap } from '@evaka/lib-components/src/white-space'
import { messagesBreakpoint } from '~messages/const'
import { ReceivedBulletin } from '~messages/types'
import { getBulletins, markBulletinRead } from '~messages/api'
import MessagesList from '~messages/MessagesList'
import MessageReadView from '~messages/MessageReadView'

function MessagesPage() {
  const [bulletins, setBulletins] = useState<Result<ReceivedBulletin[]>>(
    Loading.of()
  )
  const [activeBulletin, setActiveBulletin] = useState<ReceivedBulletin | null>(
    null
  )

  const loadBulletins = useRestApi(getBulletins, setBulletins)
  useEffect(() => loadBulletins(), [])

  const openBulletin = (bulletin: ReceivedBulletin) => {
    markBulletinRead(bulletin.id)

    setActiveBulletin({
      ...bulletin,
      isRead: true
    })

    if (bulletins.isSuccess) {
      setBulletins(
        Success.of(
          bulletins.value.map((b) =>
            b.id === bulletin.id ? { ...b, isRead: true } : b
          )
        )
      )
    }
  }

  return (
    <Container>
      <Gap size="s" />
      <AdaptiveFlex breakpoint={messagesBreakpoint} horizontalSpacing="L">
        <MessagesList
          bulletins={bulletins}
          activeBulletin={activeBulletin}
          onClickBulletin={openBulletin}
          onReturn={() => setActiveBulletin(null)}
        />
        {activeBulletin && <MessageReadView bulletin={activeBulletin} />}
      </AdaptiveFlex>
    </Container>
  )
}

export default React.memo(MessagesPage)
