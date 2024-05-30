// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import { Result, wrapResult } from 'lib-common/api'
import { NotesByGroupResponse } from 'lib-common/generated/api-types/note'
import { UUID } from 'lib-common/types'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { StickyNoteTab } from 'lib-components/employee/notes/StickyNoteTab'
import { EditedNote } from 'lib-components/employee/notes/notes'
import { ContentArea } from 'lib-components/layout/Container'
import { PlainModal } from 'lib-components/molecules/modals/BaseModal'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faTimes } from 'lib-icons'

import {
  createChildStickyNote,
  createGroupNote,
  deleteChildStickyNote,
  deleteGroupNote,
  updateChildStickyNote,
  updateGroupNote
} from '../../../../generated/api-clients/note'
import { Translations, useTranslation } from '../../../../state/i18n'
import { renderResult } from '../../../async-rendering'

import ChildDailyNoteForm from './ChildDailyNoteForm'

const createGroupNoteResult = wrapResult(createGroupNote)
const updateGroupNoteResult = wrapResult(updateGroupNote)
const deleteGroupNoteResult = wrapResult(deleteGroupNote)
const createChildStickyNoteResult = wrapResult(createChildStickyNote)
const updateChildStickyNoteResult = wrapResult(updateChildStickyNote)
const deleteChildStickyNoteResult = wrapResult(deleteChildStickyNote)

const getLabels = (i18n: Translations, title: string, placeholder: string) => ({
  addNew: i18n.common.addNew,
  editor: {
    cancel: i18n.common.cancel,
    save: i18n.common.save,
    placeholder
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
    active ? theme.colors.main.m2 : 'transparent'};

  font-size: 15px;
  font-weight: ${fontWeights.bold};
  color: ${(p) => p.theme.colors.grayscale.g70};
  text-transform: uppercase;
  cursor: pointer;
`

interface Props {
  group: { id: UUID; name: string }
  child?: { id: UUID; name: string }
  notesByGroup: Result<NotesByGroupResponse>
  reload: () => void
  onClose: () => void
}

export default React.memo(function NotesModal({
  child,
  group,
  notesByGroup,
  onClose,
  reload
}: Props) {
  const { i18n } = useTranslation()

  const notes = useMemo(
    () =>
      notesByGroup.map((v) => ({
        groupNotes: v.groupNotes,
        childDailyNotes: child?.id
          ? v.childDailyNotes.filter((note) => note.childId === child.id)
          : [],
        childStickyNotes: child?.id
          ? v.childStickyNotes.filter((note) => note.childId === child.id)
          : []
      })),
    [child, notesByGroup]
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
  const [tab, setTab] = useState<TabType>(child ? 'child' : 'group')

  const stickyNoteLabels = useMemo(
    () =>
      getLabels(
        i18n,
        i18n.unit.groups.daycareDailyNote.stickyNotesHeader,
        i18n.unit.groups.daycareDailyNote.childStickyNoteHint
      ),
    [i18n]
  )
  const groupNoteLabels = useMemo(
    () =>
      getLabels(
        i18n,
        i18n.unit.groups.daycareDailyNote.groupNotesHeader,
        i18n.unit.groups.daycareDailyNote.groupNoteHint
      ),
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
      (id
        ? updateGroupNoteResult({ noteId: id, body })
        : createGroupNoteResult({ groupId: group.id, body })
      ).then(reloadOnSuccess),
    [group.id, reloadOnSuccess]
  )
  const removeGroupNote = useCallback(
    (id: string) => deleteGroupNoteResult({ noteId: id }).then(reloadOnSuccess),
    [reloadOnSuccess]
  )
  const saveStickyNote = useCallback(
    ({ id, ...body }: EditedNote) => {
      if (!child?.id) {
        return Promise.reject('invalid usage: childId was not provided')
      }
      const promise = id
        ? updateChildStickyNoteResult({ noteId: id, body })
        : createChildStickyNoteResult({ childId: child.id, body })
      return promise.then(reloadOnSuccess)
    },
    [child, reloadOnSuccess]
  )
  const removeStickyNote = useCallback(
    (id: string) =>
      deleteChildStickyNoteResult({ noteId: id }).then(reloadOnSuccess),
    [reloadOnSuccess]
  )

  const tabs = useMemo(
    () =>
      [
        ...(child
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
              <RoundIcon content="" color={colors.main.m3} size="xs" />
            </>
          )}
        </Tab>
      )),
    [child, i18n, tab, counts]
  )

  return (
    <PlainModal margin={`${defaultMargins.XL} auto`}>
      <HeaderContainer>
        <Tabs>{tabs}</Tabs>
        <CloseContainer>
          <IconOnlyButton
            icon={faTimes}
            onClick={onClose}
            data-qa="modal-close"
            aria-label={i18n.common.closeModal}
          />
        </CloseContainer>
      </HeaderContainer>

      {renderResult(
        notes,
        ({ childStickyNotes, groupNotes, childDailyNotes }) => (
          <>
            {tab === 'child' && child && (
              <ContentArea opaque={false} paddingHorizontal="s">
                <ChildDailyNoteForm
                  note={childDailyNotes[0] ?? null}
                  childId={child.id}
                  childName={child.name}
                  onCancel={onClose}
                  onSuccess={reloadAndClose}
                  onRemove={reload}
                />
              </ContentArea>
            )}
            {tab === 'sticky' && child && (
              <StickyNoteTab
                labels={stickyNoteLabels}
                subHeading={child.name}
                notes={childStickyNotes}
                onSave={saveStickyNote}
                onRemove={removeStickyNote}
              />
            )}
            {tab === 'group' && (
              <StickyNoteTab
                labels={groupNoteLabels}
                subHeading={group.name}
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
