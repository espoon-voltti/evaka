// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import _ from 'lodash'
import { Loading, Result } from '@evaka/lib-common/src/api'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'
import { useDebounce } from '@evaka/lib-common/src/utils/useDebounce'
import Container from '@evaka/lib-components/src/layout/Container'
import Button from '@evaka/lib-components/src/atoms/buttons/Button'
import { Gap } from '@evaka/lib-components/src/white-space'
import { Bulletin, IdAndName } from './types'
import {
  deleteDraftBulletin,
  getGroups,
  getUnits,
  initNewBulletin,
  sendBulletin,
  updateDraftBulletin
} from './api'
import MessageEditor from './MessageEditor'
import { tabletMin } from '@evaka/lib-components/src/breakpoints'
import AdaptiveFlex from '@evaka/lib-components/src/layout/AdaptiveFlex'
import styled from 'styled-components'
import UnitsList from './UnitsList'

export default React.memo(function MessagesPage() {
  const [units, setUnits] = useState<Result<IdAndName[]>>(Loading.of())
  const [groups, setGroups] = useState<Result<IdAndName[]> | null>(null)
  const [editorMessage, setEditorMessage] = useState<Bulletin | null>()
  const debouncedMessage = useDebounce(editorMessage, 2000)

  const [unit, setUnit] = useState<IdAndName | null>(null)

  const loadUnits = useRestApi(getUnits, setUnits)
  useEffect(() => loadUnits(), [])

  const loadGroups = useRestApi(getGroups, setGroups)
  useEffect(() => {
    if (unit) loadGroups(unit.id)
  }, [unit])

  const onCreateNew = () => {
    if (editorMessage) return

    void initNewBulletin().then(
      (res) => res.isSuccess && setEditorMessage(res.value)
    )
  }

  useEffect(() => {
    if (debouncedMessage) {
      const { id, groupId, title, content } = debouncedMessage
      void updateDraftBulletin(id, groupId, title, content)
    }
  }, [debouncedMessage])

  const onDeleteDraft = () => {
    if (!editorMessage) return

    void deleteDraftBulletin(editorMessage.id).then(() =>
      setEditorMessage(null)
    )
  }

  const onSend = () => {
    if (!editorMessage) return

    const { id, groupId, title, content } = editorMessage
    void updateDraftBulletin(id, groupId, title, content)
      .then(() => sendBulletin(editorMessage.id))
      .then(() => setEditorMessage(null))
  }

  return (
    <Container>
      <Gap size="s" />
      <StyledFlex breakpoint={tabletMin} horizontalSpacing="L">
        <UnitsList units={units} activeUnit={unit} selectUnit={setUnit} />

        {editorMessage && groups?.isSuccess && (
          <MessageEditor
            bulletin={editorMessage}
            groups={groups.value}
            onChange={(change) =>
              setEditorMessage((old) => (old ? { ...old, ...change } : old))
            }
            onClose={() => setEditorMessage(null)}
            onDeleteDraft={onDeleteDraft}
            onSend={onSend}
          />
        )}
      </StyledFlex>

      <Gap size={'s'} />

      <Button
        text={'Uusi tiedote'}
        primary
        onClick={onCreateNew}
        disabled={!unit || groups?.isSuccess !== true}
      />
    </Container>
  )
})

const StyledFlex = styled(AdaptiveFlex)`
  align-items: stretch;
`
