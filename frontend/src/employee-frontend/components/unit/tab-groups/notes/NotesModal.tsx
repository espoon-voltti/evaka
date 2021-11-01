// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Result } from 'lib-common/api'
import { NotesByGroupResponse } from 'lib-common/generated/api-types/note'
import { UUID } from 'lib-common/types'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { EditedNote } from 'lib-components/employee/notes/notes'
import { StickyNoteTab } from 'lib-components/employee/notes/StickyNoteTab'
import { ContentArea } from 'lib-components/layout/Container'
import { PlainModal } from 'lib-components/molecules/modals/BaseModal'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faTimes } from 'lib-icons'
import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'
import {
  deleteChildStickyNote,
  deleteGroupNote,
  postChildStickyNote,
  postGroupNote,
  putChildStickyNote,
  putGroupNote
} from '../../../../api/daycare-notes'
import { Translations, useTranslation } from '../../../../state/i18n'
import { renderResult } from '../../../async-rendering'
import { ChildDailyNoteForm } from './ChildDailyNoteForm'

const getLabels = (i18n: Translations, title: string) => ({
  addNew: i18n.common.add,
  editor: {
    cancel: i18n.common.cancel,
    placeholder: '',
    save: i18n.common.save
  },
  static: {
    edit: i18n.common.edit,
    remove: i18n.common.remove,
    validTo: i18n.common.validTo
  },
  title
})

const HeaderContainer = styled.div`
  display: flex;
  justify-content: space-between;
`

const CloseContainer = styled.div`
  flex-grow: 0;
  padding: ${defaultMargins.s};
`

const Tabs = styled.div`
  display: flex;
  justify-content: space-evenly;
  flex-grow: 1;
`

const Tab = styled.div<{ active?: boolean }>`
  flex-grow: 1;
  display: flex;
  justify-content: center;
  align-items: center;

  border-bottom-width: 2px;
  border-bottom-style: solid;
  border-bottom-color: ${({ active, theme }) =>
    active ? theme.colors.brand.primary : 'transparent'};

  font-size: 15px;
  font-weight: ${fontWeights.bold};
  color: ${({ theme }) => theme.colors.greyscale.dark};
  text-transform: uppercase;
  cursor: pointer;
`

interface Props {
  groupId: UUID
  childId?: UUID
  notesByGroup: Result<NotesByGroupResponse>
  reload: () => void
  onClose: () => void
}

export const NotesModal = React.memo(function NotesModal({
  childId,
  groupId,
  notesByGroup,
  onClose,
  reload
}: Props) {
  const { i18n } = useTranslation()

  const notes = useMemo(
    () =>
      notesByGroup.map((v) => ({
        groupNotes: v.groupNotes,
        childDailyNotes: childId
          ? v.childDailyNotes.filter((note) => note.childId === childId)
          : [],
        childStickyNotes: childId
          ? v.childStickyNotes.filter((note) => note.childId === childId)
          : []
      })),
    [childId, notesByGroup]
  )

  const counts = useMemo(
    () =>
      notes
        .map((v) => ({
          child: v.childDailyNotes.length,
          sticky: v.childStickyNotes.length,
          group: v.groupNotes.length
        }))
        .getOrElse({ child: 0, sticky: 0, group: 0 }),
    [notes]
  )

  type TabType = 'child' | 'sticky' | 'group'
  const [tab, setTab] = useState<TabType>(
    counts.child > 0
      ? 'child'
      : counts.sticky > 0
      ? 'sticky'
      : !childId || counts.group > 0
      ? 'group'
      : 'child'
  )

  const stickyNoteLabels = useMemo(
    () => getLabels(i18n, i18n.unit.groups.daycareDailyNote.stickyNotesHeader),
    [i18n]
  )
  const groupNoteLabels = useMemo(
    () => getLabels(i18n, i18n.unit.groups.daycareDailyNote.groupNotesHeader),
    [i18n]
  )
  const reloadAndClose = useCallback(() => {
    reload()
    onClose()
  }, [onClose, reload])

  const reloadOnSuccess = useCallback(
    (res: Result<unknown>) => res.map(() => reload()),
    [reload]
  )
  const saveGroupNote = useCallback(
    ({ id, ...body }: EditedNote) =>
      (id ? putGroupNote(id, body) : postGroupNote(groupId, body)).then(
        reloadOnSuccess
      ),
    [groupId, reloadOnSuccess]
  )
  const removeGroupNote = useCallback(
    (id) => deleteGroupNote(id).then(reloadOnSuccess),
    [reloadOnSuccess]
  )
  const saveStickyNote = useCallback(
    ({ id, ...body }: EditedNote) => {
      if (!childId) {
        return Promise.reject('invalid usage: childId was not provided')
      }
      const promise = id
        ? putChildStickyNote(id, body)
        : postChildStickyNote(childId, body)
      return promise.then(reloadOnSuccess)
    },
    [childId, reloadOnSuccess]
  )
  const removeStickyNote = useCallback(
    (id) => deleteChildStickyNote(id).then(reloadOnSuccess),
    [reloadOnSuccess]
  )

  const tabs = useMemo(
    () =>
      [
        ...(childId
          ? [
              {
                type: 'child' as const,
                title: i18n.common.day,
                indicator: counts.child > 0
              },
              {
                type: 'sticky' as const,
                title: i18n.common.nb,
                indicator: counts.sticky > 0
              }
            ]
          : []),
        {
          type: 'group' as const,
          title: i18n.common.group,
          indicator: counts.group > 0
        }
      ].map(({ type, title, indicator }) => (
        <Tab
          key={type}
          data-qa={`tab-${type}`}
          active={tab === type}
          onClick={() => setTab(type)}
        >
          {title}
          {indicator && (
            <>
              <Gap horizontal size="xs" />
              <RoundIcon
                content=""
                color={colors.brandEspoo.espooTurquoise}
                size="xs"
              />
            </>
          )}
        </Tab>
      )),
    [childId, i18n, tab, counts]
  )

  return (
    <PlainModal>
      <HeaderContainer>
        <Tabs>{tabs}</Tabs>
        <CloseContainer>
          <IconButton icon={faTimes} onClick={onClose} data-qa="modal-close" />
        </CloseContainer>
      </HeaderContainer>

      {renderResult(
        notes,
        ({ childStickyNotes, groupNotes, childDailyNotes }) => (
          <>
            {tab === 'child' && childId && (
              <ContentArea opaque={false} paddingHorizontal="s">
                <ChildDailyNoteForm
                  note={childDailyNotes[0] ?? null}
                  childId={childId}
                  onCancel={onClose}
                  onSuccess={reloadAndClose}
                />
              </ContentArea>
            )}
            {tab === 'sticky' && (
              <StickyNoteTab
                labels={stickyNoteLabels}
                notes={childStickyNotes}
                onSave={saveStickyNote}
                onRemove={removeStickyNote}
              />
            )}
            {tab === 'group' && (
              <StickyNoteTab
                labels={groupNoteLabels}
                notes={groupNotes}
                onSave={saveGroupNote}
                onRemove={removeGroupNote}
              />
            )}
          </>
        )
      )}
    </PlainModal>
  )
})
