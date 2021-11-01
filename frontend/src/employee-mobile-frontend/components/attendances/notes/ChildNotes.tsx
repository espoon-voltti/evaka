// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { Child } from 'lib-common/generated/api-types/attendance'
import {
  ChildDailyNote,
  ChildStickyNote,
  GroupNote
} from 'lib-common/generated/api-types/note'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Title from 'lib-components/atoms/Title'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faArrowLeft } from 'lib-icons'
import React, { useContext, useMemo, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'
import { ChildAttendanceContext } from '../../../state/child-attendance'
import { useTranslation } from '../../../state/i18n'
import { renderResult } from '../../async-rendering'
import { TallContentArea } from '../../mobile/components'
import { BackButtonInline } from '../components'
import { ChildStickyNotesTab } from './ChildStickyNotesTab'
import { DailyNotesTab } from './DailyNotesTab'
import { GroupNotesTab } from './GroupNotesTab'
import { ChildDailyNoteFormData } from './notes'

type NoteType = 'NOTE' | 'STICKY' | 'GROUP'

interface NoteTypeTabProps {
  dataQa: string
  onClick: () => void
  noteCount: number
  selected: boolean
  title: string
}

const NoteTypeTab = ({
  onClick,
  selected,
  noteCount,
  title,
  dataQa
}: NoteTypeTabProps) => (
  <Tab selected={selected} onClick={onClick} data-qa={dataQa}>
    <TabTitle>{title}</TabTitle>
    {noteCount > 0 && (
      <RoundIcon
        data-qa={`${dataQa}-indicator`}
        content={String(noteCount)}
        color={colors.accents.orange}
        size="xs"
      />
    )}
  </Tab>
)

const childDailyNoteToFormData = ({
  feedingNote,
  note,
  reminderNote,
  reminders,
  sleepingMinutes,
  sleepingNote
}: ChildDailyNote): ChildDailyNoteFormData => ({
  feedingNote,
  note,
  reminderNote,
  reminders,
  sleepingHours: sleepingMinutes ? Math.floor(sleepingMinutes / 60) : null,
  sleepingMinutes: sleepingMinutes ? sleepingMinutes % 60 : null,
  sleepingNote
})

export default React.memo(function ChildNotes() {
  const { i18n } = useTranslation()
  const history = useHistory()

  const { childId, groupId } = useParams<{
    childId: string
    groupId: string
  }>()

  const { attendanceResponse } = useContext(ChildAttendanceContext)

  const [selectedTab, setSelectedTab] = useState<NoteType>('NOTE')

  const childResult: Result<Child> = useMemo(
    () =>
      attendanceResponse
        .map((v) => v.children.find((c) => c.id === childId))
        .chain((c) =>
          c ? Success.of(c) : Failure.of({ message: 'Child not found' })
        ),
    [attendanceResponse, childId]
  )

  const dailyNote = useMemo(
    () =>
      childResult
        .map((child) =>
          child.dailyNote
            ? {
                form: childDailyNoteToFormData(child.dailyNote),
                id: child.dailyNote.id
              }
            : undefined
        )
        .getOrElse(undefined),
    [childResult]
  )

  const stickyNotes = useMemo<ChildStickyNote[]>(
    () => childResult.map((c) => c.stickyNotes).getOrElse([]),
    [childResult]
  )

  const groupNotes = useMemo<GroupNote[]>(
    () => attendanceResponse.map((v) => v.groupNotes).getOrElse([]),
    [attendanceResponse]
  )

  const noteTabs = useMemo(() => {
    const tabs = [
      {
        type: 'NOTE' as const,
        title: i18n.attendances.notes.day,
        noteCount: dailyNote ? 1 : 0
      },
      {
        type: 'STICKY' as const,
        title: `${i18n.common.nb}!`,
        noteCount: stickyNotes.length
      },
      {
        type: 'GROUP' as const,
        title: i18n.common.group,
        noteCount: groupNotes.length
      }
    ]

    return tabs.map(({ type, title, noteCount }) => (
      <NoteTypeTab
        key={`tab-${type}`}
        dataQa={`tab-${type}`}
        onClick={() => setSelectedTab(type)}
        selected={selectedTab === type}
        title={title}
        noteCount={noteCount}
      />
    ))
  }, [dailyNote, i18n, stickyNotes.length, groupNotes.length, selectedTab])

  return renderResult(childResult, (child) => (
    <TallContentArea
      opaque={false}
      paddingHorizontal="zero"
      paddingVertical="zero"
    >
      <TopRow>
        <BackButtonInline
          onClick={() => history.goBack()}
          icon={faArrowLeft}
          text={`${child.firstName} ${child.lastName}`}
        />
      </TopRow>
      <FixedSpaceColumn>
        <TitleArea shadow opaque paddingHorizontal="s" paddingVertical="6px">
          <Title>{i18n.attendances.notes.dailyNotes}</Title>
        </TitleArea>

        <TabContainer
          shadow
          opaque
          paddingHorizontal="0px"
          paddingVertical="0px"
        >
          {noteTabs}
        </TabContainer>

        {selectedTab === 'NOTE' && (
          <DailyNotesTab
            childId={childId}
            dailyNoteId={dailyNote?.id}
            formData={dailyNote?.form}
          />
        )}
        {selectedTab === 'STICKY' && (
          <ChildStickyNotesTab childId={childId} notes={stickyNotes} />
        )}
        {selectedTab === 'GROUP' && (
          <GroupNotesTab groupId={groupId} notes={groupNotes} />
        )}
      </FixedSpaceColumn>
    </TallContentArea>
  ))
})

const TitleArea = styled(ContentArea)`
  text-align: center;

  h1 {
    margin: 0;
  }
`

const TabContainer = styled(ContentArea)`
  display: flex;
  height: 60px;
`

interface TabProps {
  selected: boolean
}

const Tab = styled.div<TabProps>`
  width: 100%;
  height: 100%;

  display: flex;
  justify-content: center;
  align-items: center;

  font-family: Montserrat, sans-serif;
  font-style: normal;
  font-weight: ${(props) => (props.selected ? 700 : 600)};
  font-size: 14px;
  line-height: 16px;
  text-align: center;
  letter-spacing: 0.08em;
  text-transform: uppercase;

  background: ${(props) =>
    props.selected ? colors.blues.lighter : colors.greyscale.white};

  color: ${(props) =>
    props.selected ? colors.blues.dark : colors.greyscale.dark};
`

const TopRow = styled.div`
  display: flex;
  justify-content: space-between;

  button {
    margin-right: ${defaultMargins.s};
  }
`

const TabTitle = styled.span`
  margin-right: ${defaultMargins.xxs};
`
