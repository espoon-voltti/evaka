// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { UpdateStateFn } from 'lib-common/form-state'
import {
  AttendanceResponse,
  Child
} from 'lib-common/generated/api-types/attendance'
import {
  DaycareDailyNote,
  DaycareDailyNoteBody,
  daycareDailyNoteLevelInfoValues,
  daycareDailyNoteReminderValues
} from 'lib-common/generated/api-types/messaging'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
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
  useContext,
  useEffect,
  useState
} from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'
import {
  deleteDaycareDailyNote,
  insertDaycareDailyNoteForChild,
  updateDaycareDailyNoteForChild,
  upsertDaycareDailyNoteForGroup
} from '../../../api/attendances'
import { ChildAttendanceContext } from '../../../state/child-attendance'
import { useTranslation } from '../../../state/i18n'
import { renderResult } from '../../async-rendering'
import { ChipWrapper, TallContentArea } from '../../mobile/components'
import { BackButtonInline } from '../components'

type DailyNoteEdited = DaycareDailyNoteBody & {
  id: UUID | null
  sleepingHours: number | null
}

const dailyNoteEditedToDailyNoteBody = ({
  sleepingHours,
  sleepingMinutes,
  ...rest
}: DailyNoteEdited): DaycareDailyNoteBody => ({
  ...rest,
  sleepingMinutes:
    sleepingHours != null || sleepingMinutes != null
      ? (sleepingMinutes || 0) + (sleepingHours || 0) * 60
      : null
})

const dailyNoteToDailyNoteEdited = (
  note: DaycareDailyNote
): DailyNoteEdited => ({
  ...note,
  sleepingHours: note.sleepingMinutes
    ? Math.floor(note.sleepingMinutes / 60)
    : null,
  sleepingMinutes: note.sleepingMinutes ? note.sleepingMinutes % 60 : null
})

const dailyNoteIsEmpty = (dailyNote: DailyNoteEdited) =>
  !dailyNote.sleepingNote &&
  dailyNote.sleepingMinutes === null &&
  dailyNote.sleepingHours === null &&
  dailyNote.reminders.length === 0 &&
  !dailyNote.reminderNote &&
  !dailyNote.note &&
  !dailyNote.id &&
  !dailyNote.groupId &&
  !dailyNote.feedingNote

const initDailyNote = ({
  childId,
  groupId
}: {
  childId?: string
  groupId?: string
}): DailyNoteEdited => ({
  id: null,
  childId: childId ?? null,
  groupId: groupId ?? null,
  date: LocalDate.today(),
  note: '',
  feedingNote: null,
  sleepingNote: null,
  sleepingHours: null,
  sleepingMinutes: null,
  reminders: [],
  reminderNote: ''
})

const genNewGroupNote = (
  attendanceResponse: Result<AttendanceResponse>,
  groupId: string,
  groupNote: string | null
): DailyNoteEdited => ({
  childId: null,
  date: LocalDate.today(),
  groupId,
  note: groupNote,
  id: attendanceResponse
    .map(
      ({ groupNotes }) =>
        groupNotes.find((g) => g.groupId == groupId)?.dailyNote.id ?? null
    )
    .getOrElse(null),
  feedingNote: null,
  sleepingNote: null,
  sleepingHours: null,
  sleepingMinutes: null,
  reminders: [],
  reminderNote: null
})

export default React.memo(function DailyNoteEditor() {
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

  const [dailyNote, setDailyNote] = useState<DailyNoteEdited>(
    initDailyNote({ childId })
  )

  const [groupNote, setGroupNote] = useState<DailyNoteEdited>(
    initDailyNote({ groupId })
  )

  type NoteType = 'NOTE' | 'GROUP_NOTE'

  const [deleteType, setDeleteType] = useState<NoteType>('NOTE')

  const [selectedTab, setSelectedTab] = useState<NoteType>('NOTE')

  useEffect(() => {
    attendanceResponse.map(({ children, groupNotes }) => {
      const child = children.find((ac) => ac.id === childId)
      if (child?.dailyNote) {
        setDailyNote(dailyNoteToDailyNoteEdited(child.dailyNote))
      }
      const gNote = groupNotes.find((g) => g.groupId == groupId)?.dailyNote
      if (gNote) {
        setGroupNote(dailyNoteToDailyNoteEdited(gNote))
      }
    })
  }, [attendanceResponse, childId, groupId])

  const deleteNote = async () => {
    if (deleteType === 'NOTE') {
      if (dailyNote.id) await deleteDaycareDailyNote(dailyNote.id)
    } else {
      if (groupNote.id) await deleteDaycareDailyNote(groupNote.id)
    }
    reloadAttendances()
    history.goBack()
  }
  const saveNotes = async () => {
    const promises = []
    if (groupNote.note) {
      const newGroupNote = genNewGroupNote(
        attendanceResponse,
        groupId,
        groupNote.note
      )
      promises.push(upsertDaycareDailyNoteForGroup(groupId, newGroupNote))
    }
    if (!dailyNoteIsEmpty(dailyNote)) {
      const body = dailyNoteEditedToDailyNoteBody(dailyNote)
      promises.push(
        dailyNote.id
          ? updateDaycareDailyNoteForChild(childId, dailyNote.id, body)
          : insertDaycareDailyNoteForChild(childId, body)
      )
    }
    setDirty(false)
    await Promise.all(promises)
  }

  const editNote: UpdateStateFn<DailyNoteEdited> = (changes) => {
    setDailyNote((prev) => ({ ...prev, ...changes }))
    setDirty(true)
  }

  function DeleteAbsencesModal() {
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
            void deleteNote()
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
            void saveNotes().then(() => {
              reloadAttendances()
              history.goBack()
            })
          },
          label: i18n.common.save,
          disabled: (dailyNote?.sleepingMinutes || 0) > 59
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

  interface NoteTypeTabProps {
    type: NoteType
    children?: React.ReactNode
    dataQa: string
  }

  const NoteTypeTab = ({ type, children, dataQa }: NoteTypeTabProps) => (
    <Tab
      selected={type == selectedTab}
      onClick={() => setSelectedTab(type)}
      data-qa={dataQa}
    >
      {children}
    </Tab>
  )

  const goBackWithConfirm = () => {
    if (dirty) {
      setUiMode('confirmExit')
    } else {
      history.goBack()
    }
  }

  return renderResult(
    attendanceResponse
      .map((v) => v.children.find((c) => c.id === childId))
      .chain<Child>((c) =>
        c ? Success.of(c) : Failure.of({ message: 'Child not found' })
      ),
    (child) => (
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
              <NoteTypeTab type="NOTE" dataQa="tab-note">
                <TabTitle>{i18n.common.child}</TabTitle>
                {dailyNote.id && (
                  <RoundIcon
                    content="1"
                    color={colors.accents.orange}
                    size="xs"
                  />
                )}
              </NoteTypeTab>
              <NoteTypeTab type="GROUP_NOTE" dataQa="tab-group-note">
                <TabTitle>{i18n.common.group}</TabTitle>
                {groupNote.id && (
                  <RoundIcon
                    content="1"
                    color={colors.accents.orange}
                    size="xs"
                  />
                )}
              </NoteTypeTab>
            </TabContainer>

            {selectedTab == 'NOTE' && (
              <ContentArea shadow opaque paddingHorizontal="s">
                <FixedSpaceColumn spacing="m">
                  {dailyNote.id && (
                    <FixedSpaceRow fullWidth justifyContent="flex-end">
                      <InlineButton
                        onClick={() => {
                          setDeleteType('NOTE')
                          setUiMode('confirmDelete')
                        }}
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
                    onChange={(e) => editNote({ note: e.target.value })}
                    placeholder={i18n.attendances.notes.placeholders.note}
                    data-qa="daily-note-note-input"
                  />

                  <H3 primary smaller noMargin>
                    {i18n.attendances.notes.otherThings}
                  </H3>

                  <FixedSpaceColumn spacing="s">
                    <Label>{i18n.attendances.notes.labels.feedingNote}</Label>
                    <ChipWrapper $noMargin>
                      {daycareDailyNoteLevelInfoValues.map((level) => (
                        <Fragment key={level}>
                          <ChoiceChip
                            text={i18n.attendances.notes.feedingValues[level]}
                            selected={dailyNote.feedingNote === level}
                            onChange={() => {
                              editNote({
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
                      {daycareDailyNoteLevelInfoValues.map((level) => (
                        <Fragment key={level}>
                          <ChoiceChip
                            text={i18n.attendances.notes.sleepingValues[level]}
                            selected={dailyNote.sleepingNote === level}
                            onChange={() => {
                              editNote({
                                sleepingNote:
                                  dailyNote.sleepingNote === level
                                    ? null
                                    : level
                              })
                            }}
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
                        editNote({
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
                        editNote({ sleepingMinutes: parseInt(value) })
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
                      {daycareDailyNoteReminderValues.map((reminder) => (
                        <Checkbox
                          key={reminder}
                          label={i18n.attendances.notes.reminders[reminder]}
                          onChange={(checked) =>
                            editNote({
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
                          editNote({
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
            )}

            {selectedTab == 'GROUP_NOTE' && (
              <ContentArea shadow opaque paddingHorizontal="s">
                <FixedSpaceColumn spacing="m">
                  {groupNote.id && (
                    <FixedSpaceRow fullWidth justifyContent="flex-end">
                      <InlineButton
                        onClick={() => {
                          setDeleteType('GROUP_NOTE')
                          setUiMode('confirmDelete')
                        }}
                        text={i18n.common.clear}
                        icon={faTrash}
                        data-qa="open-delete-group-note-dialog-btn"
                      />
                    </FixedSpaceRow>
                  )}

                  <WithNoteInfo>
                    <H2 primary noMargin>
                      {i18n.attendances.notes.groupNote}
                    </H2>
                  </WithNoteInfo>

                  <TextArea
                    value={groupNote?.note ?? ''}
                    onChange={(e) => {
                      setDirty(true)
                      setGroupNote((prev) => ({
                        ...prev,
                        note: e.target.value
                      }))
                    }}
                    placeholder={i18n.attendances.notes.placeholders.groupNote}
                    data-qa="daily-note-group-note-input"
                  />
                </FixedSpaceColumn>
              </ContentArea>
            )}

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
                  onClick={saveNotes}
                  onSuccess={() => {
                    reloadAttendances()
                    history.goBack()
                  }}
                  text={i18n.common.save}
                  data-qa="create-daily-note-btn"
                />
              </FixedSpaceRow>
            </StickyActionContainer>
          </FixedSpaceColumn>
        </TallContentArea>
        {uiMode === 'confirmDelete' && <DeleteAbsencesModal />}
        {uiMode === 'confirmExit' && <ConfirmExitModal />}
      </>
    )
  )
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
