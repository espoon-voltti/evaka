// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useEffect, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'

import Loader from 'lib-components/atoms/Loader'
import { faArrowLeft, faExclamation, faTrash } from 'lib-icons'
import { useRestApi } from 'lib-common/utils/useRestApi'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import Title from 'lib-components/atoms/Title'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H2, Label } from 'lib-components/typography'
import InputField, { TextArea } from 'lib-components/atoms/form/InputField'
import LocalDate from 'lib-common/local-date'
import { defaultMargins, Gap } from 'lib-components/white-space'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { ChoiceChip } from 'lib-components/atoms/Chip'
import { ContentArea } from 'lib-components/layout/Container'
import { Result } from 'lib-common/api'

import {
  AttendanceResponse,
  createOrUpdateDaycareDailyNoteForChild,
  DailyNote,
  DaycareDailyNoteLevelInfo,
  DaycareDailyNoteReminder,
  deleteDaycareDailyNote,
  getDaycareAttendances,
  upsertGroupDaycareDailyNote
} from '../../../api/attendances'
import { useTranslation } from '../../../state/i18n'
import { AttendanceUIContext } from '../../../state/attendance-ui'
import { TallContentArea, ChipWrapper } from '../../mobile/components'
import { UserContext } from '../../../state/user'
import { User } from '../../../types/index'
import { BackButtonInline } from '../components'
import InfoModal from 'lib-components/molecules/modals/InfoModal'

interface DailyNoteEdited {
  id: string | undefined
  childId: string | undefined
  groupId: string | undefined
  date: LocalDate
  note: string
  feedingNote: DaycareDailyNoteLevelInfo | undefined
  sleepingNote: DaycareDailyNoteLevelInfo | undefined
  sleepingHours: number | undefined
  sleepingMinutes: number | undefined
  reminders: DaycareDailyNoteReminder[]
  reminderNote: string
}

export default React.memo(function DailyNoteEditor() {
  const { i18n } = useTranslation()
  const history = useHistory()
  const { user } = useContext(UserContext)

  const [uiMode, setUiMode] = useState<
    'default' | 'confirmExit' | 'confirmDelete'
  >('default')

  const { childId, unitId, groupId } = useParams<{
    unitId: string
    childId: string
    groupId: string
  }>()

  const { attendanceResponse, setAttendanceResponse } = useContext(
    AttendanceUIContext
  )

  const [dirty, setDirty] = useState(false)

  const [dailyNote, setDailyNote] = useState<DailyNoteEdited>({
    id: undefined,
    childId: childId,
    groupId: undefined,
    date: LocalDate.today(),
    note: '',
    feedingNote: undefined,
    sleepingNote: undefined,
    sleepingHours: undefined,
    sleepingMinutes: undefined,
    reminders: [],
    reminderNote: ''
  })

  const [groupNote, setGroupNote] = useState<DailyNote>({
    childId: null,
    date: LocalDate.today(),
    groupId,
    note: '',
    id: null,
    feedingNote: null,
    sleepingNote: null,
    sleepingHours: null,
    reminders: [],
    reminderNote: null,
    modifiedAt: null,
    modifiedBy: user?.id ?? 'unknown user'
  })

  const [deleteType, setDeleteType] = useState<'NOTE' | 'GROUP_NOTE'>('NOTE')

  const loadDaycareAttendances = useRestApi(
    getDaycareAttendances,
    setAttendanceResponse
  )

  useEffect(() => {
    loadDaycareAttendances(unitId)
  }, [loadDaycareAttendances, unitId])

  useEffect(() => {
    if (attendanceResponse.isSuccess) {
      const child =
        attendanceResponse.isSuccess &&
        attendanceResponse.value.children.find((ac) => ac.id === childId)
      if (child && child.dailyNote) {
        setDailyNote(dailyNoteToDailyNoteEdited(child.dailyNote))
      }
      const gNote = attendanceResponse.value.unit.groups.find(
        (g) => g.id == groupId
      )?.dailyNote
      if (gNote) {
        setGroupNote(gNote)
      }
    }
  }, [attendanceResponse, childId, groupId])

  const child =
    attendanceResponse.isSuccess &&
    attendanceResponse.value.children.find((ac) => ac.id === childId)

  const levelInfoValues: DaycareDailyNoteLevelInfo[] = [
    'GOOD',
    'MEDIUM',
    'NONE'
  ]

  const reminderValues: DaycareDailyNoteReminder[] = [
    'DIAPERS',
    'CLOTHES',
    'LAUNDRY'
  ]
  const deleteNote = async () => {
    if (deleteType === 'NOTE') {
      if (dailyNote.id) await deleteDaycareDailyNote(dailyNote.id)
    } else {
      if (groupNote.id) await deleteDaycareDailyNote(groupNote.id)
    }
    history.goBack()
  }
  const saveNotes = async () => {
    if (groupNote && groupNote.note !== '') {
      const newGroupNote = genNewGroupNote(
        attendanceResponse,
        groupId,
        groupNote.note,
        user
      )
      if (dailyNoteIsEmpty(dailyNote)) {
        setDirty(false)
        return upsertGroupDaycareDailyNote(groupId, newGroupNote)
      } else {
        setDirty(false)
        return Promise.all([
          upsertGroupDaycareDailyNote(groupId, newGroupNote),
          createOrUpdateDaycareDailyNoteForChild(
            childId,
            dailyNoteEditedToDailyNote(dailyNote, user)
          )
        ])
      }
    } else {
      setDirty(false)
      return createOrUpdateDaycareDailyNoteForChild(
        childId,
        dailyNoteEditedToDailyNote(dailyNote, user)
      )
    }
  }

  const editNote = (dailyNote: DailyNoteEdited) => {
    setDailyNote(dailyNote)
    setDirty(true)
  }

  function DeleteAbsencesModal() {
    return (
      <InfoModal
        iconColour={'orange'}
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
        iconColour={'orange'}
        title={i18n.attendances.notes.confirmTitle}
        icon={faExclamation}
        reject={{
          action: () => {
            history.goBack()
          },
          label: i18n.attendances.notes.closeWithoutSaving
        }}
        resolve={{
          action: () => {
            void saveNotes().then(() => {
              history.goBack()
            })
          },
          label: i18n.common.save
        }}
        resolveDisabled={
          dailyNote.sleepingMinutes === undefined
            ? false
            : dailyNote.sleepingMinutes > 59
        }
      />
    )
  }

  return (
    <>
      {attendanceResponse.isLoading && <Loader />}
      {attendanceResponse.isFailure && <ErrorSegment />}
      {attendanceResponse.isSuccess && child && (
        <>
          <TallContentArea
            opaque={false}
            paddingHorizontal={'zero'}
            paddingVertical={'zero'}
          >
            <TopRow>
              <BackButtonInline
                onClick={() => {
                  if (dirty) {
                    setUiMode('confirmExit')
                  } else {
                    history.goBack()
                  }
                }}
                icon={faArrowLeft}
                text={
                  child
                    ? `${child.firstName} ${child.lastName}`
                    : i18n.common.back
                }
              />
              <InlineButton
                onClick={() =>
                  void saveNotes().then(() => {
                    history.goBack()
                  })
                }
                text={i18n.common.save}
                data-qa="create-daily-note-btn"
                disabled={
                  dailyNote.sleepingMinutes === undefined
                    ? false
                    : dailyNote.sleepingMinutes > 59
                }
              />
            </TopRow>
            <FixedSpaceColumn>
              <TitleArea
                shadow
                opaque
                paddingHorizontal={'s'}
                paddingVertical={'6px'}
              >
                <Title>{i18n.attendances.notes.dailyNotes}</Title>
              </TitleArea>

              <ContentArea shadow opaque paddingHorizontal={'s'}>
                <FixedSpaceColumn spacing={'m'}>
                  {groupNote.id && (
                    <>
                      <FixedSpaceRow
                        fullWidth={true}
                        justifyContent={'flex-end'}
                        spacing={'xxs'}
                      >
                        <IconButton
                          icon={faTrash}
                          onClick={() => {
                            setDeleteType('GROUP_NOTE')
                            setUiMode('confirmDelete')
                          }}
                          data-qa="open-delete-groupnote-dialog-btn"
                          gray
                        />
                        <span>{i18n.common.clear}</span>
                      </FixedSpaceRow>
                    </>
                  )}

                  <H2 noMargin>{i18n.attendances.notes.groupNote}</H2>

                  <FixedSpaceColumn spacing={'xxs'}>
                    <Label>
                      {i18n.attendances.notes.labels.groupNotesHeader}
                    </Label>
                    <TextArea
                      value={groupNote.note ?? ''}
                      onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => {
                        setDirty(true)
                        setGroupNote({ ...groupNote, note: e.target.value })
                      }}
                      placeholder={
                        i18n.attendances.notes.placeholders.groupNote
                      }
                      data-qa={'daily-note-group-note-input'}
                    />
                  </FixedSpaceColumn>
                </FixedSpaceColumn>
              </ContentArea>

              <ContentArea shadow opaque paddingHorizontal={'s'}>
                <FixedSpaceColumn spacing={'m'}>
                  {dailyNote.id && (
                    <FixedSpaceRow
                      fullWidth={true}
                      justifyContent={'flex-end'}
                      spacing={'xxs'}
                    >
                      <IconButton
                        icon={faTrash}
                        onClick={() => {
                          setDeleteType('NOTE')
                          setUiMode('confirmDelete')
                        }}
                        data-qa="open-delete-dialog-btn"
                        gray
                      />
                      <span>{i18n.common.clear}</span>
                    </FixedSpaceRow>
                  )}

                  <H2 noMargin>{i18n.attendances.notes.note}</H2>

                  <FixedSpaceColumn spacing={'xxs'}>
                    <Label>{i18n.attendances.notes.labels.note}</Label>
                    <TextArea
                      value={dailyNote.note}
                      onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) =>
                        editNote({ ...dailyNote, note: e.target.value })
                      }
                      placeholder={i18n.attendances.notes.placeholders.note}
                      data-qa={'daily-note-note-input'}
                    />
                  </FixedSpaceColumn>
                  <FixedSpaceColumn spacing={'s'}>
                    <Label>{i18n.attendances.notes.labels.feedingNote}</Label>
                    <ChipWrapper $noMargin>
                      {levelInfoValues.map((levelInfo) => (
                        <Fragment key={levelInfo}>
                          <ChoiceChip
                            text={
                              i18n.attendances.notes.feedingValues[levelInfo]
                            }
                            selected={dailyNote.feedingNote === levelInfo}
                            onChange={() => {
                              editNote({
                                ...dailyNote,
                                feedingNote:
                                  dailyNote.feedingNote === levelInfo
                                    ? undefined
                                    : levelInfo
                              })
                            }}
                            data-qa={`feeding-note-${levelInfo}`}
                          />
                          <Gap horizontal size={'xxs'} />
                        </Fragment>
                      ))}
                    </ChipWrapper>
                  </FixedSpaceColumn>
                  <FixedSpaceColumn spacing={'s'}>
                    <Label>{i18n.attendances.notes.labels.sleepingNote}</Label>
                    <ChipWrapper $noMargin>
                      {levelInfoValues.map((levelInfo) => (
                        <Fragment key={levelInfo}>
                          <ChoiceChip
                            text={
                              i18n.attendances.notes.sleepingValues[levelInfo]
                            }
                            selected={dailyNote.sleepingNote === levelInfo}
                            onChange={() => {
                              editNote({
                                ...dailyNote,
                                sleepingNote:
                                  dailyNote.sleepingNote === levelInfo
                                    ? undefined
                                    : levelInfo
                              })
                            }}
                            data-qa={`sleeping-note-${levelInfo}`}
                          />
                          <Gap horizontal size={'xxs'} />
                        </Fragment>
                      ))}
                    </ChipWrapper>
                  </FixedSpaceColumn>
                  <Time>
                    <InputField
                      value={
                        dailyNote.sleepingHours
                          ? dailyNote.sleepingHours.toString()
                          : ''
                      }
                      onChange={(value) =>
                        editNote({
                          ...dailyNote,
                          sleepingHours: parseFloat(value)
                        })
                      }
                      placeholder={i18n.attendances.notes.placeholders.hours}
                      data-qa="sleeping-time-hours-input"
                      width={'s'}
                      type={'number'}
                    />
                    <span>{i18n.common.hourShort}</span>
                    <InputField
                      value={
                        dailyNote.sleepingMinutes
                          ? dailyNote.sleepingMinutes.toString()
                          : ''
                      }
                      onChange={(value) =>
                        editNote({
                          ...dailyNote,
                          sleepingMinutes: parseFloat(value)
                        })
                      }
                      placeholder={i18n.attendances.notes.placeholders.minutes}
                      data-qa="sleeping-time-minutes-input"
                      width={'s'}
                      type={'number'}
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
                  <FixedSpaceColumn spacing={'s'}>
                    <Label>{i18n.attendances.notes.labels.reminderNote}</Label>
                    <FixedSpaceColumn spacing={'xs'}>
                      {reminderValues.map((reminder) => (
                        <Checkbox
                          key={reminder}
                          label={i18n.attendances.notes.reminders[reminder]}
                          onChange={(checked) => {
                            checked
                              ? editNote({
                                  ...dailyNote,
                                  reminders: [...dailyNote.reminders, reminder]
                                })
                              : editNote({
                                  ...dailyNote,
                                  reminders: dailyNote.reminders.filter(
                                    (v) => v !== reminder
                                  )
                                })
                          }}
                          checked={dailyNote.reminders.includes(reminder)}
                          data-qa={`reminders-${reminder}`}
                        />
                      ))}
                      <TextArea
                        value={dailyNote.reminderNote}
                        onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) =>
                          editNote({
                            ...dailyNote,
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
            </FixedSpaceColumn>
          </TallContentArea>
        </>
      )}
      {uiMode === `confirmDelete` && <DeleteAbsencesModal />}
      {uiMode === `confirmExit` && <ConfirmExitModal />}
    </>
  )
})

function dailyNoteEditedToDailyNote(
  dailyNoteEdited: DailyNoteEdited,
  user: User | undefined
): DailyNote {
  return {
    ...dailyNoteEdited,
    id: dailyNoteEdited.id ? dailyNoteEdited.id : null,
    childId: dailyNoteEdited.childId ? dailyNoteEdited.childId : null,
    groupId: dailyNoteEdited.groupId ? dailyNoteEdited.groupId : null,
    feedingNote: dailyNoteEdited.feedingNote
      ? dailyNoteEdited.feedingNote
      : null,
    sleepingNote: dailyNoteEdited.sleepingNote
      ? dailyNoteEdited.sleepingNote
      : null,
    sleepingHours:
      dailyNoteEdited.sleepingHours || dailyNoteEdited.sleepingMinutes
        ? (dailyNoteEdited.sleepingHours ?? 0) +
          (dailyNoteEdited.sleepingMinutes ?? 0) / 60
        : null,
    modifiedBy: user?.id ?? 'unknown user',
    modifiedAt: null
  }
}

function dailyNoteToDailyNoteEdited(dailyNote: DailyNote): DailyNoteEdited {
  return {
    ...dailyNote,
    id: dailyNote.id ? dailyNote.id : undefined,
    childId: dailyNote.childId ? dailyNote.childId : undefined,
    groupId: dailyNote.groupId ? dailyNote.groupId : undefined,
    note: dailyNote.note ? dailyNote.note : '',
    reminderNote: dailyNote.reminderNote ? dailyNote.reminderNote : '',
    feedingNote: dailyNote.feedingNote ? dailyNote.feedingNote : undefined,
    sleepingNote: dailyNote.sleepingNote ? dailyNote.sleepingNote : undefined,
    sleepingHours: dailyNote.sleepingHours
      ? Math.floor(dailyNote.sleepingHours)
      : undefined,
    sleepingMinutes: dailyNote.sleepingHours
      ? Math.round((dailyNote.sleepingHours % 1) * 60)
      : undefined
  }
}

function genNewGroupNote(
  attendanceResponse: Result<AttendanceResponse>,
  groupId: string,
  groupNote: string | null,
  user: User | undefined
) {
  const oldGroupNote =
    attendanceResponse.isSuccess &&
    attendanceResponse.value.unit.groups.find((g) => g.id == groupId)?.dailyNote
  let newGroupNote: DailyNote = {
    childId: null,
    date: LocalDate.today(),
    groupId,
    note: groupNote,
    id: null,
    feedingNote: null,
    sleepingNote: null,
    sleepingHours: null,
    reminders: [],
    reminderNote: null,
    modifiedAt: null,
    modifiedBy: user?.id ?? 'unknown user'
  }
  if (oldGroupNote) {
    newGroupNote = {
      ...newGroupNote,
      id: oldGroupNote.id
    }
  }
  return newGroupNote
}

function dailyNoteIsEmpty(dailyNote: DailyNoteEdited) {
  if (
    dailyNote.feedingNote === undefined &&
    dailyNote.groupId === undefined &&
    dailyNote.id === undefined &&
    dailyNote.note === '' &&
    dailyNote.reminderNote === '' &&
    dailyNote.reminders.length === 0 &&
    dailyNote.sleepingHours === undefined &&
    dailyNote.sleepingMinutes === undefined &&
    dailyNote.sleepingNote === undefined
  )
    return true
  return false
}

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
const TopRow = styled.div`
  display: flex;
  justify-content: space-between;

  button {
    margin-right: ${defaultMargins.s};
  }
`
