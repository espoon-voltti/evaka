// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useEffect, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'
import { animated, useSpring } from 'react-spring'

import Loader from 'lib-components/atoms/Loader'
import { faArrowLeft, faExclamation, faTrash } from 'lib-icons'
import { useRestApi } from 'lib-common/utils/useRestApi'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import colors from 'lib-components/colors'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import Title from 'lib-components/atoms/Title'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H2, Label } from 'lib-components/typography'
import InputField, { TextArea } from 'lib-components/atoms/form/InputField'
import LocalDate from 'lib-common/local-date'
// import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
// import Button from 'lib-components/atoms/buttons/Button'
import { defaultMargins, Gap } from 'lib-components/white-space'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import RoundIcon from 'lib-components/atoms/RoundIcon'
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
// import { Actions } from '../components'
import { UserContext } from '../../../state/user'
import { User } from '../../../types/index'

interface DailyNoteEdited {
  id: string | undefined
  childId: string | undefined
  groupId: string | undefined
  date: LocalDate
  note: string
  feedingNote: DaycareDailyNoteLevelInfo | undefined
  sleepingNote: DaycareDailyNoteLevelInfo | undefined
  sleepingHours: number | undefined
  reminders: DaycareDailyNoteReminder[]
  reminderNote: string
}

export default React.memo(function DailyNoteEditor() {
  const { i18n } = useTranslation()
  const history = useHistory()
  const { user } = useContext(UserContext)

  const [showDialog, setShowDialog] = useState<boolean>(false)
  const dialogSpring = useSpring<{ x: number }>({ x: showDialog ? 1 : 0 })

  const { childId, unitId, groupId } = useParams<{
    unitId: string
    childId: string
    groupId: string
  }>()

  const { attendanceResponse, setAttendanceResponse } = useContext(
    AttendanceUIContext
  )

  const [dailyNote, setDailyNote] = useState<DailyNoteEdited>({
    id: undefined,
    childId: childId,
    groupId: undefined,
    date: LocalDate.today(),
    note: '',
    feedingNote: undefined,
    sleepingNote: undefined,
    sleepingHours: undefined,
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
  }, [])

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
  }, [attendanceResponse])

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
        return upsertGroupDaycareDailyNote(groupId, newGroupNote)
      } else {
        return Promise.all([
          upsertGroupDaycareDailyNote(groupId, newGroupNote),
          createOrUpdateDaycareDailyNoteForChild(
            childId,
            dailyNoteEditedToDailyNote(dailyNote, user)
          )
        ])
      }
    } else {
      return createOrUpdateDaycareDailyNoteForChild(
        childId,
        dailyNoteEditedToDailyNote(dailyNote, user)
      )
    }
  }

  return (
    <>
      {attendanceResponse.isLoading && <Loader />}
      {attendanceResponse.isFailure && <ErrorSegment />}
      {attendanceResponse.isSuccess && child && (
        <>
          <ConfirmDialog
            style={{
              height: dialogSpring.x.interpolate((x) => `${300 * x}px`)
            }}
          >
            <Center>
              <RoundIcon content={faExclamation} color={'orange'} size="XL" />
              <DialogTitle>{i18n.attendances.notes.clearTitle}</DialogTitle>
              <Buttons>
                <InlineButton
                  text={i18n.common.cancel}
                  onClick={() => setShowDialog(false)}
                />
                <InlineButton
                  text={i18n.common.clear}
                  onClick={deleteNote}
                  data-qa="delete-daily-note-btn"
                />
              </Buttons>
            </Center>
          </ConfirmDialog>
          <TallContentArea
            opaque={false}
            paddingHorizontal={'zero'}
            paddingVertical={'zero'}
          >
            <TopRow>
              <BackButton
                onClick={() => history.goBack()}
                icon={faArrowLeft}
                text={
                  child
                    ? `${child.firstName} ${child.lastName}`
                    : i18n.common.back
                }
              />
              <InlineButton
                onClick={() =>
                  saveNotes().then(() => {
                    history.goBack()
                  })
                }
                text={i18n.common.save}
                data-qa="create-daily-note-btn"
              />
            </TopRow>
            <FixedSpaceColumn>
              <TitleArea
                shadow
                opaque
                paddingHorizontal={'s'}
                paddingVertical={'xs'}
              >
                <Title>{i18n.attendances.notes.dailyNotes}</Title>
              </TitleArea>

              {/* <ActionsWithPadding>
                <FixedSpaceRow fullWidth>
                  <Button
                    text={i18n.common.cancel}
                    onClick={() => history.goBack()}
                  />
                  <AsyncButton
                    primary
                    text={i18n.common.save}
                    onClick={() => saveNotes()}
                    onSuccess={() => history.goBack()}
                    data-qa="create-daily-note-btn"
                  />
                </FixedSpaceRow>
              </ActionsWithPadding> */}

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
                            setShowDialog(!showDialog)
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
                      onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) =>
                        setGroupNote({ ...groupNote, note: e.target.value })
                      }
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
                          setShowDialog(!showDialog)
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
                        setDailyNote({ ...dailyNote, note: e.target.value })
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
                              setDailyNote({
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
                              setDailyNote({
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
                        setDailyNote({
                          ...dailyNote,
                          sleepingHours: parseFloat(value)
                        })
                      }
                      placeholder={
                        i18n.attendances.notes.placeholders.sleepingTime
                      }
                      data-qa="sleeping-time-input"
                      width={'s'}
                      type={'number'}
                    />
                    <span>{i18n.common.hours}</span>
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
                              ? setDailyNote({
                                  ...dailyNote,
                                  reminders: [...dailyNote.reminders, reminder]
                                })
                              : setDailyNote({
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
                          setDailyNote({
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
    sleepingHours: dailyNoteEdited.sleepingHours
      ? dailyNoteEdited.sleepingHours
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
    sleepingHours: dailyNote.sleepingHours ? dailyNote.sleepingHours : undefined
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
    dailyNote.sleepingNote === undefined
  )
    return true
  return false
}

const BackButton = styled(InlineButton)`
  color: ${colors.blues.dark};
  margin-top: ${defaultMargins.s};
  margin-left: ${defaultMargins.s};
  margin-bottom: ${defaultMargins.s};
`

const Time = styled.div`
  display: flex;
  align-items: center;

  span {
    margin-left: ${defaultMargins.xs};
  }
`

const ConfirmDialog = animated(styled.div`
  position: absolute;
  background: ${colors.greyscale.white};
  width: 100vw;
  overflow: hidden;
  z-index: 2;
  box-shadow: 0px 4px 4px 0px ${colors.greyscale.lighter};
`)

const Center = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  padding: 32px 40px;
`

const Buttons = styled.div`
  display: flex;
  width: 100%;
  justify-content: space-between;
`

const DialogTitle = styled.h2`
  font-family: Montserrat, sans-serif;
  font-style: normal;
  font-weight: 500;
  font-size: 18px;
  line-height: 27px;
  text-align: center;
  color: ${colors.blues.dark};
  margin-top: 32px;
  margin-bottom: 40px;
`

// const ActionsWithPadding = styled(Actions)`
//   padding: 0 ${defaultMargins.s};
// `

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
