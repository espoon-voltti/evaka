// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import colors from 'lib-customizations/common'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import Title from 'lib-components/atoms/Title'
import { Gap } from 'lib-components/white-space'
import { Label } from 'lib-components/typography'

import { AttendanceChild, DailyNote } from '../../../api/attendances'
import { useTranslation } from '../../../state/i18n'

interface Props {
  child: AttendanceChild | undefined
  groupNote: DailyNote | undefined
}

export default React.memo(function DailyNote({ child, groupNote }: Props) {
  const { i18n } = useTranslation()

  return (
    <DailyNoteWrapper>
      <NotesTitle size={2}>{i18n.attendances.notes.dailyNotes}</NotesTitle>
      <Gap size={'xs'} />
      {child && child.dailyNote ? (
        <FixedSpaceColumn spacing={'xs'}>
          {child.dailyNote.note && (
            <FixedSpaceColumn spacing={'xxs'}>
              <Label>{i18n.attendances.notes.labels.note}</Label>
              <span>{child.dailyNote.note}</span>
            </FixedSpaceColumn>
          )}
          {child.dailyNote.feedingNote && (
            <FixedSpaceColumn spacing={'xxs'}>
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
            <FixedSpaceColumn spacing={'xxs'}>
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
          {(child.dailyNote.reminderNote ||
            child.dailyNote.reminders.length > 0) && (
            <FixedSpaceColumn spacing={'xxs'}>
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
          {groupNote && (
            <FixedSpaceColumn spacing={'xxs'}>
              <Label>{i18n.attendances.notes.labels.groupNotesHeader}</Label>
              <span>{groupNote.note}</span>
            </FixedSpaceColumn>
          )}
        </FixedSpaceColumn>
      ) : groupNote ? (
        <FixedSpaceColumn spacing={'xxs'}>
          <Label>{i18n.attendances.notes.labels.groupNotesHeader}</Label>
          <span>{groupNote.note}</span>
        </FixedSpaceColumn>
      ) : (
        <div>{i18n.attendances.notes.noNotes}</div>
      )}
    </DailyNoteWrapper>
  )
})

const NotesTitle = styled(Title)`
  font-family: Montserrat, sans-serif;
  font-size: 18px;
  font-weight: 500;
  color: ${colors.blues.dark};
  margin-top: 0;
  margin-bottom: 0;
`

const DailyNoteWrapper = styled.span`
  margin-left: 16px;
  color: ${colors.blues.dark};
`
