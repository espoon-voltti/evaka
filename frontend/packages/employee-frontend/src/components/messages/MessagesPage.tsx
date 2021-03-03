// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useState } from 'react'
import styled from 'styled-components'
import FocusTrap from 'focus-trap-react'
import { Loading, Result } from '@evaka/lib-common/src/api'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'
import { useDebounce } from '@evaka/lib-common/src/utils/useDebounce'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import Container from '@evaka/lib-components/src/layout/Container'
import { FixedSpaceRow } from '@evaka/lib-components/src/layout/flex-helpers'
import { Bulletin, IdAndName } from './types'
import {
  deleteDraftBulletin,
  getDraftBulletins,
  getGroups,
  getSentBulletins,
  getUnits,
  initNewBulletin,
  sendBulletin,
  updateDraftBulletin
} from './api'
import MessageEditor from './MessageEditor'
import UnitsList from './UnitsList'
import MessageBoxes, { MessageBoxType } from './MessageBoxes'
import MessageList from './MessageList'
import MessageReadView from './MessageReadView'

export default React.memo(function MessagesPage() {
  const [units, setUnits] = useState<Result<IdAndName[]>>(Loading.of())
  const [groups, setGroups] = useState<Result<IdAndName[]> | null>(null)
  const [messageCounts, setMessageCounts] = useState<
    Record<MessageBoxType, number>
  >(initialMessageCounts)
  const [messageOpen, setMessageOpen] = useState<Bulletin | null>()
  const [messageUnderEdit, setMessageUnderEdit] = useState<Bulletin | null>()
  const [activeMessageBox, setActiveMessageBox] = useState<MessageBoxType>(
    'SENT'
  )
  const debouncedMessage = useDebounce(messageUnderEdit, 2000)

  const [unit, setUnit] = useState<IdAndName | null>(null)
  const selectUnit = (unit: IdAndName) => {
    setMessageCounts(initialMessageCounts)
    setActiveMessageBox('SENT')
    setUnit(unit)
  }

  const loadUnits = useRestApi(getUnits, setUnits)
  useEffect(() => loadUnits(), [])

  const loadGroups = useRestApi(getGroups, setGroups)
  useEffect(() => {
    if (unit) loadGroups(unit.id)
  }, [unit])

  const setMessageCountResult = useCallback(
    (result: Result<Record<MessageBoxType, number>>) =>
      result.isSuccess ? setMessageCounts(result.value) : undefined,
    [setMessageCounts]
  )
  const loadMessageCounts = useRestApi(
    (unitId: string) =>
      Promise.all([
        getDraftBulletins(unitId, 1, 1),
        getSentBulletins(unitId, 1, 1)
      ]).then(([drafts, sent]) =>
        drafts.chain((ds) =>
          sent.map((ss) => ({ SENT: ss.total, DRAFT: ds.total }))
        )
      ),
    setMessageCountResult
  )

  useEffect(() => {
    if (unit) loadMessageCounts(unit.id)
  }, [unit])

  const onCreateNew = () => {
    if (!unit || messageUnderEdit) return

    void initNewBulletin(unit.id)
      .then((res) => res.isSuccess && setMessageUnderEdit(res.value))
      .then(() => loadMessageCounts(unit.id))
  }

  useEffect(() => {
    if (debouncedMessage) {
      const { id, groupId, title, content } = debouncedMessage
      void updateDraftBulletin(id, groupId, title, content)
    }
  }, [debouncedMessage])

  const resetUI = () => {
    setMessageUnderEdit(null)
    setMessageOpen(null)
    if (unit) {
      loadMessageCounts(unit.id)
    }
  }

  const onDeleteDraft = () => {
    if (!messageUnderEdit) return

    void deleteDraftBulletin(messageUnderEdit.id).then(resetUI)
  }

  const onSend = () => {
    if (!messageUnderEdit) return

    const { id, groupId, title, content } = messageUnderEdit
    void updateDraftBulletin(id, groupId, title, content)
      .then(() => sendBulletin(messageUnderEdit.id))
      .then(resetUI)
  }

  const onClose = () => {
    if (!messageUnderEdit) return

    const { id, groupId, title, content } = messageUnderEdit
    void updateDraftBulletin(id, groupId, title, content).then(resetUI)
  }

  return (
    <Container>
      <StyledFlex spacing="L" tabIndex={messageUnderEdit ? -1 : undefined}>
        <UnitsList units={units} activeUnit={unit} selectUnit={selectUnit} />

        {unit && (
          <>
            <MessageBoxes
              activeMessageBox={activeMessageBox}
              selectMessageBox={(box) => {
                setActiveMessageBox(box)
                setMessageOpen(null)
              }}
              messageCounts={messageCounts}
              onCreateNew={onCreateNew}
              createNewDisabled={!unit || groups?.isSuccess !== true}
            />

            {messageOpen ? (
              <MessageReadView
                message={messageOpen}
                onEdit={() => {
                  setMessageUnderEdit(messageOpen)
                  setMessageOpen(null)
                }}
              />
            ) : (
              <MessageList
                unitId={unit.id}
                messageBoxType={activeMessageBox}
                selectMessage={setMessageOpen}
                groups={groups?.isSuccess === true ? groups.value : []}
              />
            )}
          </>
        )}
      </StyledFlex>

      {messageUnderEdit && groups?.isSuccess && (
        <>
          <Dimmer />
          <FocusTrap>
            <div>
              <MessageEditor
                bulletin={messageUnderEdit}
                groups={groups.value}
                onChange={(change) =>
                  setMessageUnderEdit((old) =>
                    old ? { ...old, ...change } : old
                  )
                }
                onClose={onClose}
                onDeleteDraft={onDeleteDraft}
                onSend={onSend}
              />
            </div>
          </FocusTrap>
        </>
      )}
    </Container>
  )
})

const initialMessageCounts = { SENT: 0, DRAFT: 0 }

const StyledFlex = styled(FixedSpaceRow)`
  align-items: stretch;
  margin: ${defaultMargins.s} 0;
  height: calc(100vh - 192px);
`

const Dimmer = styled.div`
  position: absolute;
  top: -1000px;
  bottom: -1000px;
  left: -1000px;
  right: -1000px;
  z-index: 10;
  backdrop-filter: blur(4px);
  opacity: 0.7;
`
