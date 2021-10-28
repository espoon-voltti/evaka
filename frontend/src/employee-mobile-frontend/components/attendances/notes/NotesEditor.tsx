// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { UpdateStateFn } from 'lib-common/form-state'
import { Child } from 'lib-common/generated/api-types/attendance'
import {
  ChildDailyNote,
  ChildDailyNoteBody,
  childDailyNoteLevelValues,
  childDailyNoteReminderValues,
  ChildStickyNote,
  GroupNote
} from 'lib-common/generated/api-types/note'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { ChoiceChip } from 'lib-components/atoms/Chip'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import TextArea from 'lib-components/atoms/form/TextArea'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Title from 'lib-components/atoms/Title'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H2, H3, Label, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faArrowLeft, faExclamation, faTrash } from 'lib-icons'
import React, {
  Fragment,
  ReactNode,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'
import {
  deleteChildDailyNote,
  postChildDailyNote,
  putChildDailyNote
} from '../../../api/notes'
import { ChildAttendanceContext } from '../../../state/child-attendance'
import { useTranslation } from '../../../state/i18n'
import { renderResult } from '../../async-rendering'
import { ChipWrapper, TallContentArea } from '../../mobile/components'
import { BackButtonInline } from '../components'
import { ChildStickyNotesTab } from './ChildStickyNotesTab'
import { GroupNotesTab } from './GroupNotesTab'

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

type ChildDailyNoteFormData = Omit<ChildDailyNoteBody, 'sleepingMinutes'> & {
  sleepingHours: number | null
  sleepingMinutes: number | null
}

const childDailyNoteFormDataToRequestBody = ({
  feedingNote,
  note,
  reminderNote,
  reminders,
  sleepingHours,
  sleepingMinutes,
  sleepingNote
}: ChildDailyNoteFormData): ChildDailyNoteBody => ({
  feedingNote,
  note,
  reminderNote,
  reminders,
  sleepingMinutes:
    sleepingHours != null || sleepingMinutes != null
      ? (sleepingMinutes || 0) + (sleepingHours || 0) * 60
      : null,
  sleepingNote
})

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

const childDailyNoteIsEmpty = (dailyNote: ChildDailyNoteFormData) =>
  !dailyNote.sleepingNote &&
  dailyNote.sleepingMinutes === null &&
  dailyNote.sleepingHours === null &&
  dailyNote.reminders.length === 0 &&
  !dailyNote.reminderNote &&
  !dailyNote.note &&
  !dailyNote.feedingNote

export default React.memo(function NotesEditor() {
  const { i18n } = useTranslation()
  const history = useHistory()

  const [uiMode, setUiMode] = useState<
    'default' | 'confirmExit' | 'confirmDelete'
  >('default')

  const { childId, groupId } = useParams<{
    childId: string
    groupId: string
  }>()

  const { attendanceResponse, reloadAttendances } = useContext(
    ChildAttendanceContext
  )

  const [dirty, setDirty] = useState(false)

  const [dailyNote, setDailyNote] = useState<ChildDailyNoteFormData>({
    note: '',
    feedingNote: null,
    sleepingNote: null,
    sleepingHours: null,
    sleepingMinutes: null,
    reminders: [],
    reminderNote: ''
  })

  const [selectedTab, setSelectedTab] = useState<NoteType>('NOTE')

  useEffect(() => {
    attendanceResponse.map(({ children }) => {
      const child = children.find((ac) => ac.id === childId)
      if (child?.dailyNote) {
        setDailyNote(childDailyNoteToFormData(child.dailyNote))
      }
    })
  }, [attendanceResponse, childId])

  const childResult: Result<Child> = attendanceResponse
    .map((v) => v.children.find((c) => c.id === childId))
    .chain((c) =>
      c ? Success.of(c) : Failure.of({ message: 'Child not found' })
    )

  const dailyNoteId = childResult
    .map((c) => c.dailyNote?.id)
    .getOrElse(undefined)

  const deleteDailyNote: () => Promise<Result<void>> = useCallback(
    () => (dailyNoteId ? deleteChildDailyNote(dailyNoteId) : Promise.reject()),
    [dailyNoteId]
  )

  const saveChildDailyNote = useCallback(() => {
    if (childDailyNoteIsEmpty(dailyNote)) {
      return Promise.reject()
    }
    const body = childDailyNoteFormDataToRequestBody(dailyNote)
    return dailyNoteId
      ? putChildDailyNote(dailyNoteId, body)
      : postChildDailyNote(childId, body)
  }, [childId, dailyNote, dailyNoteId])

  const editChildDailyNote: UpdateStateFn<ChildDailyNoteFormData> = useCallback(
    (changes) => {
      setDailyNote((prev) => ({ ...prev, ...changes }))
      setDirty(true)
    },
    []
  )

  function DeleteNoteModal() {
    return (
      <InfoModal
        iconColour="orange"
        title={i18n.attendances.notes.clearTitle}
        icon={faExclamation}
        reject={{
          action: () => {
            setUiMode('default')
          },
          label: i18n.common.cancel
        }}
        resolve={{
          action: () => {
            void deleteDailyNote().then((res) => {
              if (res.isSuccess) {
                reloadAttendances()
                history.goBack()
              }
            })
          },
          label: i18n.common.clear
        }}
      />
    )
  }

  function ConfirmExitModal() {
    return (
      <InfoModal
        iconColour="orange"
        title={i18n.attendances.notes.confirmTitle}
        icon={faExclamation}
        close={() => setUiMode('default')}
        reject={{
          action: () => {
            history.goBack()
          },
          label: i18n.attendances.notes.closeWithoutSaving
        }}
        resolve={{
          action: () => {
            void saveChildDailyNote().then((res) => {
              if (res.isSuccess) {
                reloadAttendances()
                history.goBack()
              }
            })
          },
          label: i18n.common.save,
          disabled:
            childDailyNoteIsEmpty(dailyNote) ||
            (dailyNote.sleepingMinutes || 0) > 59
        }}
      />
    )
  }

  function WithNoteInfo({ children }: { children: ReactNode }) {
    return (
      <ExpandingInfo
        info={
          <InfoText noMargin>
            {i18n.attendances.notes.noteInfo.join('\n\n')}
          </InfoText>
        }
        ariaLabel={i18n.common.openExpandingInfo}
      >
        {children}
      </ExpandingInfo>
    )
  }

  const goBackWithConfirm = useCallback(() => {
    if (dirty) {
      setUiMode('confirmExit')
    } else {
      history.goBack()
    }
  }, [dirty, history])

  const stickyNotes = useMemo<ChildStickyNote[]>(
    () => childResult.map((c) => c.stickyNotes).getOrElse([]),
    [childResult]
  )

  const groupNotes = useMemo<GroupNote[]>(
    () => attendanceResponse.map((v) => v.groupNotes).getOrElse([]),
    [attendanceResponse]
  )

  function renderTabContent(): React.ReactNode {
    switch (selectedTab) {
      case 'STICKY':
        return <ChildStickyNotesTab childId={childId} notes={stickyNotes} />
      case 'GROUP':
        return <GroupNotesTab groupId={groupId} notes={groupNotes} />
      case 'NOTE':
        return (
          <>
            <ContentArea shadow opaque paddingHorizontal="s">
              <FixedSpaceColumn spacing="m">
                {dailyNoteId && (
                  <FixedSpaceRow fullWidth justifyContent="flex-end">
                    <InlineButton
                      onClick={() => setUiMode('confirmDelete')}
                      text={i18n.common.clear}
                      icon={faTrash}
                      data-qa="open-delete-dialog-btn"
                    />
                  </FixedSpaceRow>
                )}

                <WithNoteInfo>
                  <H2 primary noMargin>
                    {i18n.attendances.notes.note}
                  </H2>
                </WithNoteInfo>

                <TextArea
                  value={dailyNote.note || ''}
                  onChange={(e) => editChildDailyNote({ note: e.target.value })}
                  placeholder={i18n.attendances.notes.placeholders.note}
                  data-qa="daily-note-note-input"
                />

                <H3 primary smaller noMargin>
                  {i18n.attendances.notes.otherThings}
                </H3>

                <FixedSpaceColumn spacing="s">
                  <Label>{i18n.attendances.notes.labels.feedingNote}</Label>
                  <ChipWrapper $noMargin>
                    {childDailyNoteLevelValues.map((level) => (
                      <Fragment key={level}>
                        <ChoiceChip
                          text={i18n.attendances.notes.feedingValues[level]}
                          selected={dailyNote.feedingNote === level}
                          onChange={() => {
                            editChildDailyNote({
                              feedingNote:
                                dailyNote.feedingNote === level ? null : level
                            })
                          }}
                          data-qa={`feeding-note-${level}`}
                        />
                        <Gap horizontal size="xxs" />
                      </Fragment>
                    ))}
                  </ChipWrapper>
                </FixedSpaceColumn>
                <FixedSpaceColumn spacing="s">
                  <Label>{i18n.attendances.notes.labels.sleepingNote}</Label>
                  <ChipWrapper $noMargin>
                    {childDailyNoteLevelValues.map((level) => (
                      <Fragment key={level}>
                        <ChoiceChip
                          text={i18n.attendances.notes.sleepingValues[level]}
                          selected={dailyNote.sleepingNote === level}
                          onChange={() =>
                            editChildDailyNote({
                              sleepingNote:
                                dailyNote.sleepingNote === level ? null : level
                            })
                          }
                          data-qa={`sleeping-note-${level}`}
                        />
                        <Gap horizontal size="xxs" />
                      </Fragment>
                    ))}
                  </ChipWrapper>
                </FixedSpaceColumn>
                <Time>
                  <InputField
                    value={dailyNote.sleepingHours?.toString() ?? ''}
                    onChange={(value) =>
                      editChildDailyNote({
                        sleepingHours: parseInt(value)
                      })
                    }
                    placeholder={i18n.attendances.notes.placeholders.hours}
                    data-qa="sleeping-time-hours-input"
                    width="s"
                    type="number"
                  />
                  <span>{i18n.common.hourShort}</span>
                  <InputField
                    value={dailyNote.sleepingMinutes?.toString() ?? ''}
                    onChange={(value) =>
                      editChildDailyNote({
                        sleepingMinutes: parseInt(value)
                      })
                    }
                    placeholder={i18n.attendances.notes.placeholders.minutes}
                    data-qa="sleeping-time-minutes-input"
                    width="s"
                    type="number"
                    info={
                      dailyNote.sleepingMinutes &&
                      dailyNote.sleepingMinutes > 59
                        ? {
                            text: i18n.common.errors.minutes,
                            status: 'warning'
                          }
                        : undefined
                    }
                  />
                  <span>{i18n.common.minuteShort}</span>
                </Time>
                <FixedSpaceColumn spacing="s">
                  <Label>{i18n.attendances.notes.labels.reminderNote}</Label>
                  <FixedSpaceColumn spacing="xs">
                    {childDailyNoteReminderValues.map((reminder) => (
                      <Checkbox
                        key={reminder}
                        label={i18n.attendances.notes.reminders[reminder]}
                        onChange={(checked) =>
                          editChildDailyNote({
                            reminders: checked
                              ? [...dailyNote.reminders, reminder]
                              : dailyNote.reminders.filter(
                                  (v) => v !== reminder
                                )
                          })
                        }
                        checked={dailyNote.reminders.includes(reminder)}
                        data-qa={`reminders-${reminder}`}
                      />
                    ))}
                    <TextArea
                      value={dailyNote.reminderNote ?? ''}
                      onChange={(e) =>
                        editChildDailyNote({
                          reminderNote: e.target.value
                        })
                      }
                      placeholder={
                        i18n.attendances.notes.placeholders.reminderNote
                      }
                      data-qa="reminder-note-input"
                    />
                  </FixedSpaceColumn>
                </FixedSpaceColumn>
              </FixedSpaceColumn>
            </ContentArea>
            <StickyActionContainer>
              <FixedSpaceRow
                fullWidth
                justifyContent="space-evenly"
                spacing="xxs"
              >
                <Button
                  onClick={goBackWithConfirm}
                  text={i18n.common.cancel}
                  data-qa="cancel-daily-note-btn"
                />

                <AsyncButton
                  primary
                  onClick={saveChildDailyNote}
                  onSuccess={() => {
                    reloadAttendances()
                    history.goBack()
                  }}
                  text={i18n.common.save}
                  data-qa="create-daily-note-btn"
                />
              </FixedSpaceRow>
            </StickyActionContainer>
          </>
        )
    }
  }

  const noteTabs = useMemo(() => {
    const tabs = [
      {
        type: 'NOTE' as const,
        title: i18n.common.child,
        noteCount: dailyNoteId ? 1 : 0
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
  }, [dailyNoteId, i18n, stickyNotes.length, groupNotes.length, selectedTab])

  return renderResult(childResult, (child) => {
    return (
      <>
        <TallContentArea
          opaque={false}
          paddingHorizontal="zero"
          paddingVertical="zero"
        >
          <TopRow>
            <BackButtonInline
              onClick={goBackWithConfirm}
              icon={faArrowLeft}
              text={`${child.firstName} ${child.lastName}`}
            />
          </TopRow>
          <FixedSpaceColumn>
            <TitleArea
              shadow
              opaque
              paddingHorizontal="s"
              paddingVertical="6px"
            >
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

            {renderTabContent()}
          </FixedSpaceColumn>
        </TallContentArea>
        {uiMode === 'confirmDelete' && <DeleteNoteModal />}
        {uiMode === 'confirmExit' && <ConfirmExitModal />}
      </>
    )
  })
})

const Time = styled.div`
  display: flex;
  align-items: center;

  span {
    margin-left: ${defaultMargins.xs};
  }

  div:nth-child(2) {
    position: absolute;
    div:nth-child(2) {
      position: relative;
    }
  }
`

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

const InfoText = styled(P)`
  white-space: pre-line;
`

const StickyActionContainer = styled.section`
  position: sticky;
  bottom: 0;
  background-color: ${({ theme }) => theme.colors.greyscale.lightest};
  padding: ${defaultMargins.s};
`
