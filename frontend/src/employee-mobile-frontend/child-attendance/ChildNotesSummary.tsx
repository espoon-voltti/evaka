// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'
import styled from 'styled-components'

import { AttendanceChild } from 'lib-common/generated/api-types/attendance'
import { GroupNote } from 'lib-common/generated/api-types/note'
import LocalDate from 'lib-common/local-date'
import { constantQuery, useQueryResult } from 'lib-common/query'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H3, Label } from 'lib-components/typography'
import colors from 'lib-customizations/common'
import { fasExclamation, farStickyNote, farUsers } from 'lib-icons'

import { renderResult } from '../async-rendering'
import { groupNotesQuery } from '../child-notes/queries'
import { useTranslation } from '../common/i18n'

import { ChildImage } from './ChildListItem'

export default React.memo(function ChildNotesSummary({
  child
}: {
  child: AttendanceChild
}) {
  const groupNotes = useQueryResult(
    child.groupId
      ? groupNotesQuery({ groupId: child.groupId })
      : constantQuery([])
  )

  return renderResult(groupNotes, (groupNotes) => (
    <ChildNotesSummaryInner child={child} groupNotes={groupNotes} />
  ))
})

const ChildNotesSummaryInner = React.memo(function ChildNotesSummaryInner({
  child,
  groupNotes
}: {
  child: AttendanceChild
  groupNotes: GroupNote[]
}) {
  const { i18n } = useTranslation()

  const hasStickyNotes = child.stickyNotes.length > 0
  const hasDailyNote = child.dailyNote !== null
  const activeGroupNotes = useMemo(
    () =>
      groupNotes.filter((note) =>
        note.expires.isAfter(LocalDate.todayInHelsinkiTz())
      ),
    [groupNotes]
  )
  const hasGroupNotes = activeGroupNotes.length > 0

  return (
    <ContentArea
      shadow
      opaque={true}
      paddingHorizontal="s"
      paddingVertical="s"
      blue
    >
      <FixedSpaceColumn spacing="m">
        <FixedSpaceRow spacing="m" alignItems="center">
          <ChildImage child={child} />
          <ChildName>
            <H3 primary noMargin>
              {child.firstName} {child.lastName}
              {child.preferredName ? ` (${child.preferredName})` : null}
            </H3>
          </ChildName>
        </FixedSpaceRow>

        {hasStickyNotes && (
          <FixedSpaceColumn spacing="xs">
            <FixedSpaceRow>
              <span>
                <RoundIcon
                  content={fasExclamation}
                  color={colors.accents.a5orangeLight}
                  size="m"
                />
              </span>
              <Label>{i18n.attendances.notes.childStickyNotes}</Label>
            </FixedSpaceRow>
            {child.stickyNotes.length === 1 ? (
              <div data-qa="sticky-note">{child.stickyNotes[0].note}</div>
            ) : (
              <NoMarginList>
                {child.stickyNotes.map((note) => (
                  <li key={note.id} data-qa="sticky-note">
                    {note.note}
                  </li>
                ))}
              </NoMarginList>
            )}
          </FixedSpaceColumn>
        )}

        {child.dailyNote && (
          <FixedSpaceColumn spacing="xs">
            <FixedSpaceRow>
              <span>
                <RoundIcon
                  content={farStickyNote}
                  color={colors.accents.a9pink}
                  size="m"
                />
              </span>
              <Label>{i18n.attendances.notes.note}</Label>
            </FixedSpaceRow>
            {!!child.dailyNote.note && (
              <span data-qa="daily-note">{child.dailyNote.note}</span>
            )}
            {child.dailyNote.feedingNote && (
              <FixedSpaceColumn spacing="xxs">
                <Label>{i18n.attendances.notes.labels.feedingNote}</Label>
                <span>
                  {
                    i18n.attendances.notes.feedingValues[
                      child.dailyNote.feedingNote
                    ]
                  }
                </span>
              </FixedSpaceColumn>
            )}
            {child.dailyNote.sleepingNote && (
              <FixedSpaceColumn spacing="xxs">
                <Label>{i18n.attendances.notes.labels.sleepingNote}</Label>
                <span>
                  {
                    i18n.attendances.notes.sleepingValues[
                      child.dailyNote.sleepingNote
                    ]
                  }
                  {child.dailyNote.sleepingMinutes
                    ? child.dailyNote.sleepingMinutes / 60 >= 1
                      ? `. ${Math.floor(
                          child.dailyNote.sleepingMinutes / 60
                        )}h ${Math.round(
                          child.dailyNote.sleepingMinutes % 60
                        )}min.`
                      : `${Math.round(child.dailyNote.sleepingMinutes % 60)}min.`
                    : ''}
                </span>
              </FixedSpaceColumn>
            )}
            {(!!child.dailyNote.reminderNote ||
              child.dailyNote.reminders.length > 0) && (
              <FixedSpaceColumn spacing="xxs">
                <Label>{i18n.attendances.notes.labels.reminderNote}</Label>
                <span>
                  {child.dailyNote.reminders.map((reminder) => (
                    <span
                      key={reminder}
                    >{`${i18n.attendances.notes.reminders[reminder]}. `}</span>
                  ))}{' '}
                  {child.dailyNote.reminderNote}
                </span>
              </FixedSpaceColumn>
            )}
          </FixedSpaceColumn>
        )}

        {hasGroupNotes && (
          <FixedSpaceColumn spacing="xs">
            <FixedSpaceRow>
              <span>
                <RoundIcon content={farUsers} color={colors.main.m4} size="m" />
              </span>
              <Label>{i18n.attendances.notes.groupNote}</Label>
            </FixedSpaceRow>
            {activeGroupNotes.length === 1 ? (
              <div data-qa="group-note">{activeGroupNotes[0].note}</div>
            ) : (
              <NoMarginList>
                {activeGroupNotes.map((note) => (
                  <li key={note.id} data-qa="group-note">
                    {note.note}
                  </li>
                ))}
              </NoMarginList>
            )}
          </FixedSpaceColumn>
        )}

        {!hasStickyNotes && !hasDailyNote && !hasGroupNotes && (
          <div>{i18n.attendances.notes.noNotes}</div>
        )}
      </FixedSpaceColumn>
    </ContentArea>
  )
})

const ChildName = styled.div`
  flex-grow: 1;
`

const NoMarginList = styled.ul`
  margin-block-start: 0;
`
