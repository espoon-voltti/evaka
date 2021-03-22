// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'
import { animated, useSpring } from 'react-spring'

import Loader from '@evaka/lib-components/atoms/Loader'
import { faArrowLeft, faExclamation, faTrash } from '@evaka/lib-icons'
import { useRestApi } from '@evaka/lib-common/utils/useRestApi'
import InlineButton from '@evaka/lib-components/atoms/buttons/InlineButton'
import colors from '@evaka/lib-components/colors'
import Radio from '@evaka/lib-components/atoms/form/Radio'
import Checkbox from '@evaka/lib-components/atoms/form/Checkbox'
import Title from '@evaka/lib-components/atoms/Title'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from '@evaka/lib-components/layout/flex-helpers'
import { Label } from '@evaka/lib-components/typography'
import InputField, {
  TextArea
} from '@evaka/lib-components/atoms/form/InputField'
import LocalDate from '@evaka/lib-common/local-date'
import AsyncButton from '@evaka/lib-components/atoms/buttons/AsyncButton'
import Button from '@evaka/lib-components/atoms/buttons/Button'
import { defaultMargins } from '@evaka/lib-components/white-space'
import ErrorSegment from '@evaka/lib-components/atoms/state/ErrorSegment'

import {
  createOrUpdateDaycareDailyNoteForChild,
  DailyNote,
  DaycareDailyNoteLevelInfo,
  DaycareDailyNoteReminder,
  deleteDaycareDailyNote,
  getDaycareAttendances
} from '../../../api/attendances'
import { useTranslation } from '../../../state/i18n'
import { AttendanceUIContext } from '../../../state/attendance-ui'
import { TallContentArea, ContentAreaWithShadow } from '../../mobile/components'
import { Actions } from '../components'
import { UserContext } from '../../../state/user'
import { User } from '../../../types/index'
import IconButton from '@evaka/lib-components/atoms/buttons/IconButton'
import RoundIcon from '@evaka/lib-components/atoms/RoundIcon'

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
    groupId: groupId,
    date: LocalDate.today(),
    note: '',
    feedingNote: undefined,
    sleepingNote: undefined,
    sleepingHours: undefined,
    reminders: [],
    reminderNote: ''
  })

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
    if (dailyNote.id) await deleteDaycareDailyNote(dailyNote.id)
    history.goBack()
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
                  dataQa="delete-daily-note-btn"
                />
              </Buttons>
            </Center>
          </ConfirmDialog>
          <TallContentArea
            opaque={false}
            paddingHorizontal={'zero'}
            paddingVertical={'zero'}
          >
            <BackButton
              onClick={() => history.goBack()}
              icon={faArrowLeft}
              text={
                child
                  ? `${child.firstName} ${child.lastName}`
                  : i18n.common.back
              }
            />
            <ContentAreaWithShadow
              opaque={true}
              paddingHorizontal={'s'}
              paddingVertical={'m'}
            >
              <Title>{i18n.attendances.notes.dailyNotes}</Title>
              {dailyNote.id && (
                <FixedSpaceRow
                  fullWidth={true}
                  justifyContent={'flex-end'}
                  spacing={'xxs'}
                >
                  <IconButton
                    icon={faTrash}
                    onClick={() => setShowDialog(!showDialog)}
                    data-qa="open-delete-dialog-btn"
                  />
                  <span>{i18n.common.clear}</span>
                </FixedSpaceRow>
              )}
              <FixedSpaceColumn spacing={'m'}>
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
                  <FixedSpaceColumn spacing={'xs'}>
                    {levelInfoValues.map((levelInfo) => (
                      <Radio
                        key={levelInfo}
                        label={i18n.attendances.notes.values[levelInfo]}
                        onChange={() =>
                          setDailyNote({ ...dailyNote, feedingNote: levelInfo })
                        }
                        checked={dailyNote.feedingNote === levelInfo}
                        dataQa={`feeding-note-${levelInfo}`}
                      />
                    ))}
                  </FixedSpaceColumn>
                </FixedSpaceColumn>

                <FixedSpaceColumn spacing={'s'}>
                  <Label>{i18n.attendances.notes.labels.sleepingNote}</Label>
                  <FixedSpaceColumn spacing={'xs'}>
                    {levelInfoValues.map((levelInfo) => (
                      <Radio
                        key={levelInfo}
                        label={i18n.attendances.notes.values[levelInfo]}
                        onChange={() =>
                          setDailyNote({
                            ...dailyNote,
                            sleepingNote: levelInfo
                          })
                        }
                        checked={dailyNote.sleepingNote === levelInfo}
                        dataQa={`sleeping-note-${levelInfo}`}
                      />
                    ))}
                  </FixedSpaceColumn>
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
                        dataQa={`reminders-${reminder}`}
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
                <Actions>
                  <FixedSpaceRow fullWidth>
                    <Button
                      text={i18n.common.cancel}
                      onClick={() => history.goBack()}
                    />
                    <AsyncButton
                      primary
                      text={i18n.common.confirm}
                      onClick={() =>
                        createOrUpdateDaycareDailyNoteForChild(
                          childId,
                          dailyNoteEditedToDailyNote(dailyNote, user)
                        )
                      }
                      onSuccess={() => history.goBack()}
                      data-qa="create-daily-note-btn"
                    />
                  </FixedSpaceRow>
                </Actions>
              </FixedSpaceColumn>
            </ContentAreaWithShadow>
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
