// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useLayoutEffect, useState } from 'react'
import styled from 'styled-components'
import FocusTrap from 'focus-trap-react'
import { Loading, Paged, Result, Success } from 'lib-common/api'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { useDebounce } from 'lib-common/utils/useDebounce'
import { defaultMargins } from 'lib-components/white-space'
import Container from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Bulletin, IdAndName, ReceiverTriplet } from './types'
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
import ReceiverSelection from './ReceiverSelection'

export default React.memo(function MessagesPage() {
  const { units, groups, selectedUnit, setSelectedUnit } = useUnitsState()
  const { messageCounts, reloadMessageCounts } = useMessageCountState(
    selectedUnit?.id
  )
  const [activeMessageBox, setActiveMessageBox] = useState<MessageBoxType>(
    'SENT'
  )
  const {
    bulletins,
    nextPage,
    loadNextPage,
    setMessagesState,
    resetMessagesState
  } = useMessagesState(activeMessageBox, selectedUnit?.id)
  const [messageOpen, setMessageOpen] = useState<Bulletin | null>()
  const [messageUnderEdit, setMessageUnderEdit] = useState<Bulletin | null>()
  const [receiverSelectionShown, setReceiverSelectionShown] = useState<boolean>(
    false
  )
  const debouncedMessage = useDebounce(messageUnderEdit, 2000)
  const [receiverTriplets, setReceiverTriplets] = useState<ReceiverTriplet[]>(
    []
  )

  const selectUnit = (unit: IdAndName) => {
    setActiveMessageBox('SENT')
    setSelectedUnit(unit)
  }

  const onCreateNew = () => {
    if (!selectedUnit || messageUnderEdit) return

    void initNewBulletin(selectedUnit.id)
      .then((res) => res.isSuccess && setMessageUnderEdit(res.value))
      .then(() => {
        reloadMessageCounts()
        if (activeMessageBox === 'DRAFT') {
          resetMessagesState()
        }
      })
  }

  useEffect(() => {
    if (debouncedMessage) {
      const { id, title, content } = debouncedMessage
      void updateDraftBulletin(id, receiverTriplets, title, content).then(() => {
        if (activeMessageBox === 'DRAFT') {
          setMessagesState((state) => ({
            ...state,
            bulletins: state.bulletins.map((bulletin) =>
              bulletin.id === id
                ? { ...bulletin, title, content }
                : bulletin
            )
          }))
        }
      })
    }
  }, [debouncedMessage])

  const resetUI = () => {
    setMessageUnderEdit(null)
    setMessageOpen(null)
    if (selectedUnit) {
      reloadMessageCounts()
      resetMessagesState()
    }
  }

  const onDeleteDraft = () => {
    if (!messageUnderEdit) return

    void deleteDraftBulletin(messageUnderEdit.id).then(resetUI)
  }

  const onSend = () => {
    if (!messageUnderEdit) return

    const { id, title, content } = messageUnderEdit
    void updateDraftBulletin(id, receiverTriplets, title, content)
      .then(() => sendBulletin(messageUnderEdit.id))
      .then(resetUI)
  }
  const onClose = () => {
    if (!messageUnderEdit) return

    const { id, title, content } = messageUnderEdit
    void updateDraftBulletin(id, receiverTriplets, title, content).then(() =>
      setMessageUnderEdit(null)
    )
  }

  return (
    <Container>
      <StyledFlex spacing="L" tabIndex={messageUnderEdit ? -1 : undefined}>
        <UnitsList
          units={units}
          activeUnit={selectedUnit}
          selectUnit={selectUnit}
        />

        {selectedUnit && (
          <>
            <MessageBoxes
              activeMessageBox={activeMessageBox}
              selectMessageBox={(box) => {
                setActiveMessageBox(box)
                setMessageOpen(null)
                setReceiverSelectionShown(false)
              }}
              messageCounts={messageCounts}
              onCreateNew={onCreateNew}
              createNewDisabled={!selectUnit || groups?.isSuccess !== true}
              showReceiverSelection={() => {
                setReceiverSelectionShown(true)
              }}
              receiverSelectionShown={receiverSelectionShown}
            />

            {}

            {receiverSelectionShown ? (
              <ReceiverSelection
                unitId={selectedUnit.id}
                setReceiverTriplets={setReceiverTriplets}
              />
            ) : messageOpen ? (
              <MessageReadView
                message={messageOpen}
                onEdit={() => {
                  setMessageUnderEdit(messageOpen)
                  setMessageOpen(null)
                }}
              />
            ) : (
              <MessageList
                bulletins={bulletins}
                nextPage={nextPage}
                loadNextPage={loadNextPage}
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

const useUnitsState = () => {
  const [units, setUnits] = useState<Result<IdAndName[]>>(Loading.of())
  const [groups, setGroups] = useState<Result<IdAndName[]>>(Loading.of())
  const [selectedUnit, setSelectedUnit] = useState<IdAndName | null>(null)

  const loadUnits = useRestApi(getUnits, setUnits)
  useEffect(() => loadUnits(), [])

  useEffect(() => {
    if (units.isSuccess && !selectedUnit) {
      setSelectedUnit(units.value[0])
    }
  }, [units])

  const loadGroups = useRestApi(getGroups, setGroups)
  useEffect(() => {
    if (selectedUnit) loadGroups(selectedUnit.id)
  }, [selectedUnit])

  return { units, groups, selectedUnit, setSelectedUnit }
}

const initialMessageCounts = { SENT: 0, DRAFT: 0 }

const useMessageCountState = (unitId?: string) => {
  const [messageCounts, setMessageCounts] = useState<
    Record<MessageBoxType, number>
  >(initialMessageCounts)

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

  const reloadMessageCounts = () => {
    if (unitId) {
      setMessageCounts(initialMessageCounts)
      loadMessageCounts(unitId)
    }
  }

  useLayoutEffect(() => {
    reloadMessageCounts()
  }, [unitId])

  return { messageCounts, reloadMessageCounts }
}

interface MessagesState {
  bulletins: Bulletin[]
  nextPage: Result<void>
  currentPage: number
  pages: number
  total?: number
}

const initialMessagesState: MessagesState = {
  bulletins: [],
  nextPage: Loading.of(),
  currentPage: 1,
  pages: 0
}

const useMessagesState = (type: MessageBoxType, unitId?: string) => {
  const [messagesState, setMessagesState] = useState<MessagesState>(
    initialMessagesState
  )

  const setMessagesResult = useCallback(
    (result: Result<Paged<Bulletin>>) =>
      setMessagesState((state) => {
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
    [setMessagesState]
  )

  const loadMessages = useRestApi(
    (unit: string, type: MessageBoxType, page: number) => {
      switch (type) {
        case 'SENT':
          return getSentBulletins(unit, page)
        case 'DRAFT':
          return getDraftBulletins(unit, page)
      }
    },
    setMessagesResult
  )

  const resetMessagesState = () => {
    if (unitId) {
      setMessagesState(initialMessagesState)
      loadMessages(unitId, type, initialMessagesState.currentPage)
    }
  }

  useEffect(() => {
    resetMessagesState()
  }, [unitId, type])

  useEffect(() => {
    if (unitId) {
      setMessagesState((state) => ({ ...state, nextPage: Loading.of() }))
      loadMessages(unitId, type, messagesState.currentPage)
    }
  }, [messagesState.currentPage])

  const loadNextPage = () =>
    setMessagesState((state) => {
      if (state.currentPage < state.pages) {
        return {
          ...state,
          currentPage: state.currentPage + 1
        }
      }
      return state
    })

  return {
    bulletins: messagesState.bulletins,
    nextPage: messagesState.nextPage,
    loadNextPage,
    setMessagesState,
    resetMessagesState
  }
}

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
