// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import {
  ChildDailyNote,
  ChildStickyNote
} from 'lib-common/generated/api-types/note'
import { useQuery, useQueryResult } from 'lib-common/query'
import useRouteParams from 'lib-common/useRouteParams'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Title from 'lib-components/atoms/Title'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { renderResult } from '../async-rendering'
import { childrenQuery } from '../child-attendance/queries'
import { useChild } from '../child-attendance/utils'
import ChildNameBackButton from '../common/ChildNameBackButton'
import { useTranslation } from '../common/i18n'
import { SelectedGroupId } from '../common/selected-group'
import { TallContentArea } from '../pairing/components'

import { ChildStickyNotesTab } from './ChildStickyNotesTab'
import { DailyNotesTab } from './DailyNotesTab'
import { GroupNotesTab } from './GroupNotesTab'
import { groupNotesQuery } from './queries'
import { ChildDailyNoteFormData } from './types'

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
        color={colors.status.warning}
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

export default React.memo(function ChildNotes({
  selectedGroupId
}: {
  selectedGroupId: SelectedGroupId
}) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const unitId = selectedGroupId.unitId
  const { childId } = useRouteParams(['childId'])
  const child = useChild(useQueryResult(childrenQuery(unitId)), childId)

  const [selectedTab, setSelectedTab] = useState<NoteType>('NOTE')

  const dailyNote = useMemo(
    () =>
      child
        .map((child) =>
          child.dailyNote
            ? {
                form: childDailyNoteToFormData(child.dailyNote),
                id: child.dailyNote.id
              }
            : undefined
        )
        .getOrElse(undefined),
    [child]
  )

  const stickyNotes = useMemo<ChildStickyNote[]>(
    () => child.map((c) => c.stickyNotes).getOrElse([]),
    [child]
  )

  const { data: groupNotes } = useQuery(
    groupNotesQuery(selectedGroupId.type === 'all' ? '' : selectedGroupId.id),
    {
      enabled: selectedGroupId.type !== 'all'
    }
  )

  const dailyNoteCount = dailyNote ? 1 : 0
  const stickyNoteCount = stickyNotes.length
  const groupNoteCount = groupNotes?.length ?? 0
  const noteTabs = useMemo(() => {
    const tabs = [
      {
        type: 'NOTE' as const,
        title: i18n.attendances.notes.day,
        noteCount: dailyNoteCount
      },
      {
        type: 'STICKY' as const,
        title: `${i18n.common.nb}!`,
        noteCount: stickyNoteCount
      },
      selectedGroupId.type !== 'all'
        ? {
            type: 'GROUP' as const,
            title: i18n.common.group,
            noteCount: groupNoteCount
          }
        : null
    ].flatMap((tab) => (tab ? [tab] : []))

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
  }, [
    i18n,
    selectedGroupId,
    dailyNoteCount,
    stickyNoteCount,
    groupNoteCount,
    selectedTab
  ])

  return renderResult(child, (child) => (
    <TallContentArea
      opaque={false}
      paddingHorizontal="zero"
      paddingVertical="zero"
    >
      <TopRow>
        <ChildNameBackButton child={child} onClick={() => navigate(-1)} />
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
            unitId={unitId}
            childId={childId}
            dailyNoteId={dailyNote?.id}
            formData={dailyNote?.form}
          />
        )}
        {selectedTab === 'STICKY' && (
          <ChildStickyNotesTab
            unitId={unitId}
            childId={childId}
            notes={stickyNotes}
          />
        )}
        {selectedGroupId.type !== 'all' && selectedTab === 'GROUP' && (
          <GroupNotesTab
            groupId={selectedGroupId.id}
            notes={groupNotes ?? []}
          />
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
    props.selected ? colors.main.m4 : colors.grayscale.g0};

  color: ${(props) => (props.selected ? colors.main.m1 : colors.grayscale.g70)};
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
