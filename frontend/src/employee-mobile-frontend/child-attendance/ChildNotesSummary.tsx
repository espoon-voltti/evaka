// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'

import { AttendanceChild } from 'lib-common/generated/api-types/attendance'
import { GroupNote } from 'lib-common/generated/api-types/note'
import LocalDate from 'lib-common/local-date'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H2, Label, P } from 'lib-components/typography'
import colors from 'lib-customizations/common'
import { fasExclamation, farStickyNote, farUsers } from 'lib-icons'

import { useTranslation } from '../common/i18n'

export default React.memo(function ChildNotesSummary({
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

  if (!hasStickyNotes && !hasDailyNote && !hasGroupNotes) return null

  return (
    <FixedSpaceColumn spacing="m">
      {hasStickyNotes && (
        <NoteSection
          icon={fasExclamation}
          iconColor={colors.accents.a5orangeLight}
          title={i18n.attendances.notes.childStickyNotes}
        >
          {child.stickyNotes.map((gn) => (
            <P key={gn.id} noMargin data-qa="sticky-note">
              {gn.note}
            </P>
          ))}
        </NoteSection>
      )}

      {child.dailyNote && (
        <NoteSection
          icon={farStickyNote}
          iconColor={colors.accents.a9pink}
          title={i18n.attendances.notes.note}
        >
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
        </NoteSection>
      )}

      {hasGroupNotes && (
        <NoteSection
          icon={farUsers}
          iconColor={colors.main.m4}
          title={i18n.attendances.notes.groupNote}
        >
          {activeGroupNotes.map((gn) => (
            <P key={gn.id} noMargin data-qa="group-note">
              {gn.note}
            </P>
          ))}
        </NoteSection>
      )}
    </FixedSpaceColumn>
  )
})

const NoteSection = React.memo(function NoteSection({
  icon,
  iconColor,
  title,
  children
}: {
  icon: IconDefinition
  iconColor: string
  title: string
  children: React.ReactNode
}) {
  return (
    <ContentArea
      shadow
      opaque={true}
      paddingHorizontal="s"
      paddingVertical="s"
      blue
    >
      <FixedSpaceRow>
        <span>
          <RoundIcon content={icon} color={iconColor} size="m" />
        </span>
        <FixedSpaceColumn spacing="s">
          <H2 noMargin>{title}</H2>
          {children}
        </FixedSpaceColumn>
      </FixedSpaceRow>
    </ContentArea>
  )
})
