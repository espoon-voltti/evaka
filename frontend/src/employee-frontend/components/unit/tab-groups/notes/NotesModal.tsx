// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import type { Result } from 'lib-common/api'
import type { NotesByGroupResponse } from 'lib-common/generated/api-types/note'
import type {
  ChildId,
  ChildStickyNoteId,
  GroupId,
  GroupNoteId
} from 'lib-common/generated/api-types/shared'
import { useMutationResult } from 'lib-common/query'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { StickyNoteTab } from 'lib-components/employee/notes/StickyNoteTab'
import type { EditedNote } from 'lib-components/employee/notes/notes'
import { ContentArea } from 'lib-components/layout/Container'
import { PlainModal } from 'lib-components/molecules/modals/BaseModal'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faTimes } from 'lib-icons'

import type { Translations } from '../../../../state/i18n'
import { useTranslation } from '../../../../state/i18n'
import { renderResult } from '../../../async-rendering'

import ChildDailyNoteForm from './ChildDailyNoteForm'
import {
  createChildStickyNoteMutation,
  createGroupNoteMutation,
  deleteChildStickyNoteMutation,
  deleteGroupNoteMutation,
  updateChildStickyNoteMutation,
  updateGroupNoteMutation
} from './queries'

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
    validTo: i18n.common.validTo,
    lastModified: i18n.common.lastModified
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
  group: { id: GroupId; name: string }
  child?: { id: ChildId; name: string }
  notesByGroup: Result<NotesByGroupResponse>
  onClose: () => void
}

export default React.memo(function NotesModal({
  child,
  group,
  notesByGroup,
  onClose
}: Props) {
  const { i18n } = useTranslation()
  const { mutateAsync: doCreateGroupNote } = useMutationResult(
    createGroupNoteMutation
  )
  const { mutateAsync: doUpdateGroupNote } = useMutationResult(
    updateGroupNoteMutation
  )
  const { mutateAsync: doDeleteGroupNote } = useMutationResult(
    deleteGroupNoteMutation
  )
  const { mutateAsync: doCreateChildStickyNote } = useMutationResult(
    createChildStickyNoteMutation
  )
  const { mutateAsync: doUpdateChildStickyNote } = useMutationResult(
    updateChildStickyNoteMutation
  )
  const { mutateAsync: doDeleteChildStickyNote } = useMutationResult(
    deleteChildStickyNoteMutation
  )

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
  const saveGroupNote = useCallback(
    ({ id, ...body }: EditedNote<GroupNoteId>) =>
      id
        ? doUpdateGroupNote({ noteId: id, body })
        : doCreateGroupNote({ groupId: group.id, body }),
    [group.id, doUpdateGroupNote, doCreateGroupNote]
  )
  const removeGroupNote = useCallback(
    (id: GroupNoteId) => doDeleteGroupNote({ noteId: id }),
    [doDeleteGroupNote]
  )
  const saveStickyNote = useCallback(
    ({ id, ...body }: EditedNote<ChildStickyNoteId>) => {
      if (!child?.id) {
        return Promise.reject('invalid usage: childId was not provided')
      }
      return id
        ? doUpdateChildStickyNote({ noteId: id, body })
        : doCreateChildStickyNote({ childId: child.id, body })
    },
    [child, doUpdateChildStickyNote, doCreateChildStickyNote]
  )
  const removeStickyNote = useCallback(
    (id: ChildStickyNoteId) => doDeleteChildStickyNote({ noteId: id }),
    [doDeleteChildStickyNote]
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
    <PlainModal margin={`${defaultMargins.XL} auto`} onEscapeKey={onClose}>
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
                  onSuccess={onClose}
                  onRemove={onClose}
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
