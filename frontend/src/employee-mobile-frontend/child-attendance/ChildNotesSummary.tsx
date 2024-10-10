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
        <ContentArea
          shadow
          opaque={true}
          paddingHorizontal="s"
          paddingVertical="s"
          blue
        >
          <FixedSpaceRow>
            <span>
              <RoundIcon
                content={fasExclamation}
                color={colors.accents.a5orangeLight}
                size="m"
              />
            </span>
            <FixedSpaceColumn spacing="s">
              <H2 noMargin>{i18n.attendances.notes.childStickyNotes}</H2>
              {child.stickyNotes.map((gn) => (
                <P key={gn.id} noMargin>
                  {gn.note}
                </P>
              ))}
            </FixedSpaceColumn>
          </FixedSpaceRow>
        </ContentArea>
      )}

      {child.dailyNote && (
        <ContentArea
          shadow
          opaque={true}
          paddingHorizontal="s"
          paddingVertical="s"
          blue
        >
          <FixedSpaceRow>
            <span>
              <RoundIcon
                content={farStickyNote}
                color={colors.accents.a9pink}
                size="m"
              />
            </span>
            <FixedSpaceColumn spacing="s">
              <H2 noMargin>{i18n.attendances.notes.note}</H2>
              {!!child.dailyNote.note && <span>{child.dailyNote.note}</span>}
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
          </FixedSpaceRow>
        </ContentArea>
      )}

      {hasGroupNotes && (
        <ContentArea
          shadow
          opaque={true}
          paddingHorizontal="s"
          paddingVertical="s"
          blue
        >
          <FixedSpaceRow>
            <span>
              <RoundIcon content={farUsers} color={colors.main.m4} size="m" />
            </span>
            <FixedSpaceColumn spacing="s">
              <H2 noMargin>{i18n.attendances.notes.groupNote}</H2>
              {activeGroupNotes.map((gn) => (
                <p key={gn.id} data-qa="group-note">
                  {gn.note}
                </p>
              ))}
            </FixedSpaceColumn>
          </FixedSpaceRow>
        </ContentArea>
      )}
    </FixedSpaceColumn>
  )
})
