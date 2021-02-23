import React, { useEffect, useState } from 'react'
import Container from '@evaka/lib-components/src/layout/Container'
import { Gap } from '@evaka/lib-components/src/white-space'
import { FixedSpaceRow } from '@evaka/lib-components/src/layout/flex-helpers'
import MessagesList from '~messages/MessagesList'
import MessageReadView from '~messages/MessageReadView'
import { Loading, Result } from '@evaka/lib-common/src/api'
import { ReceivedBulletin } from '~messages/types'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'
import { getBulletins } from '~messages/api'

function MessagesPage() {
  const [bulletins, setBulletins] = useState<Result<ReceivedBulletin[]>>(
    Loading.of()
  )
  const [activeBulletin, setActiveBulletin] = useState<ReceivedBulletin | null>(
    null
  )

  const loadBulletins = useRestApi(getBulletins, setBulletins)
  useEffect(() => loadBulletins(), [])

  return (
    <Container>
      <Gap size="s" />
      <FixedSpaceRow>
        <MessagesList
          bulletins={bulletins}
          onClickBulletin={setActiveBulletin}
        />
        {activeBulletin && <MessageReadView bulletin={activeBulletin} />}
      </FixedSpaceRow>
    </Container>
  )
}

export default React.memo(MessagesPage)
