// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useCallback, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { UpdateStateFn } from 'lib-common/form-state'
import {
  ChildDailyNoteBody,
  childDailyNoteLevelValues,
  childDailyNoteReminderValues
} from 'lib-common/generated/api-types/note'
import { useMutation, useMutationResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { ChipWrapper, ChoiceChip } from 'lib-components/atoms/Chip'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import ResponsiveInlineButton from 'lib-components/atoms/buttons/ResponsiveInlineButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import TextArea from 'lib-components/atoms/form/TextArea'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H2, H3, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faExclamation, faTrash } from 'lib-icons'

import { useTranslation } from '../common/i18n'

import {
  createChildDailyNoteMutation,
  deleteChildDailyNoteMutation,
  updateChildDailyNoteMutation
} from './queries'
import { ChildDailyNoteFormData } from './types'

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

const StickyActionContainer = styled.section`
  position: sticky;
  bottom: 0;
  background-color: ${(p) => p.theme.colors.grayscale.g4};
  padding: ${defaultMargins.s};
`

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

const childDailyNoteIsEmpty = (dailyNote: ChildDailyNoteFormData) =>
  !dailyNote.sleepingNote &&
  dailyNote.sleepingMinutes === null &&
  dailyNote.sleepingHours === null &&
  dailyNote.reminders.length === 0 &&
  !dailyNote.reminderNote &&
  !dailyNote.note &&
  !dailyNote.feedingNote

const emptyNote = () => ({
  note: '',
  feedingNote: null,
  sleepingNote: null,
  sleepingHours: null,
  sleepingMinutes: null,
  reminders: [],
  reminderNote: ''
})

interface Props {
  unitId: UUID
  childId: UUID
  formData?: ChildDailyNoteFormData
  dailyNoteId: UUID | undefined
}

export const DailyNotesTab = React.memo(function DailyNotesTab({
  unitId,
  childId,
  formData,
  dailyNoteId
}: Props) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const [uiMode, setUiMode] = useState<
    'default' | 'confirmExit' | 'confirmDelete'
  >('default')

  const [dirty, setDirty] = useState(false)
  const [dailyNote, setDailyNote] = useState<ChildDailyNoteFormData>(
    formData ?? emptyNote()
  )
  const editNote: UpdateStateFn<ChildDailyNoteFormData> = useCallback(
    (changes) => {
      setDailyNote((prev) => ({ ...prev, ...changes }))
      setDirty(true)
    },
    []
  )

  const { mutateAsync: createDailyNote } = useMutationResult(
    createChildDailyNoteMutation
  )
  const { mutateAsync: updateDailyNote } = useMutationResult(
    updateChildDailyNoteMutation
  )
  const { mutateAsync: deleteDailyNote } = useMutation(
    deleteChildDailyNoteMutation
  )

  const saveChildDailyNote = useCallback(() => {
    if (childDailyNoteIsEmpty(dailyNote)) {
      return Promise.reject()
    }
    const body = childDailyNoteFormDataToRequestBody(dailyNote)
    return dailyNoteId
      ? updateDailyNote({ unitId, noteId: dailyNoteId, body })
      : createDailyNote({ unitId, childId, body })
  }, [
    updateDailyNote,
    createDailyNote,
    childId,
    dailyNote,
    dailyNoteId,
    unitId
  ])

  function DeleteNoteModal() {
    return (
      <InfoModal
        type="warning"
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
            if (dailyNoteId) {
              void deleteDailyNote({ unitId, noteId: dailyNoteId }).then(() => {
                navigate(-1)
              })
            }
          },
          label: i18n.common.clear
        }}
      />
    )
  }

  function ConfirmExitModal() {
    return (
      <InfoModal
        type="warning"
        title={i18n.attendances.notes.confirmTitle}
        icon={faExclamation}
        close={() => setUiMode('default')}
        reject={{
          action: () => {
            navigate(-1)
          },
          label: i18n.attendances.notes.closeWithoutSaving
        }}
        resolve={{
          action: () => {
            void saveChildDailyNote().then(() => {
              navigate(-1)
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

  const goBackWithConfirm = useCallback(() => {
    if (dirty) {
      setUiMode('confirmExit')
    } else {
      navigate(-1)
    }
  }, [dirty, navigate])

  return (
    <>
      <ContentArea shadow opaque paddingHorizontal="s">
        <FixedSpaceColumn spacing="m">
          <FixedSpaceRow fullWidth justifyContent="space-between">
            <H2 primary noMargin>
              {i18n.attendances.notes.note}
            </H2>
            {!!dailyNoteId && (
              <ResponsiveInlineButton
                onClick={() => setUiMode('confirmDelete')}
                text={i18n.common.clear}
                icon={faTrash}
                data-qa="open-delete-dialog-btn"
              />
            )}
          </FixedSpaceRow>

          <TextArea
            value={dailyNote.note || ''}
            onChange={(value) => editNote({ note: value })}
            placeholder={i18n.attendances.notes.placeholders.note}
            data-qa="daily-note-note-input"
          />

          <H3 primary smaller noMargin>
            {i18n.attendances.notes.otherThings}
          </H3>

          <FixedSpaceColumn spacing="s">
            <Label>{i18n.attendances.notes.labels.feedingNote}</Label>
            <ChipWrapper margin="zero">
              {childDailyNoteLevelValues.map((level) => (
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
            <ChipWrapper margin="zero">
              {childDailyNoteLevelValues.map((level) => (
                <Fragment key={level}>
                  <ChoiceChip
                    text={i18n.attendances.notes.sleepingValues[level]}
                    selected={dailyNote.sleepingNote === level}
                    onChange={() =>
                      editNote({
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
                editNote({
                  sleepingMinutes: parseInt(value)
                })
              }
              placeholder={i18n.attendances.notes.placeholders.minutes}
              data-qa="sleeping-time-minutes-input"
              width="s"
              type="number"
              info={
                dailyNote.sleepingMinutes && dailyNote.sleepingMinutes > 59
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
                    editNote({
                      reminders: checked
                        ? [...dailyNote.reminders, reminder]
                        : dailyNote.reminders.filter((v) => v !== reminder)
                    })
                  }
                  checked={dailyNote.reminders.includes(reminder)}
                  data-qa={`reminders-${reminder}`}
                />
              ))}
              <TextArea
                value={dailyNote.reminderNote ?? ''}
                onChange={(value) => editNote({ reminderNote: value })}
                placeholder={i18n.attendances.notes.placeholders.reminderNote}
                data-qa="reminder-note-input"
              />
            </FixedSpaceColumn>
          </FixedSpaceColumn>
        </FixedSpaceColumn>
      </ContentArea>
      <StickyActionContainer>
        <FixedSpaceRow fullWidth justifyContent="space-evenly" spacing="xxs">
          <LegacyButton
            onClick={goBackWithConfirm}
            text={i18n.common.cancel}
            data-qa="cancel-daily-note-btn"
          />

          <AsyncButton
            primary
            onClick={saveChildDailyNote}
            onSuccess={() => {
              navigate(-1)
            }}
            text={i18n.common.save}
            data-qa="create-daily-note-btn"
          />
        </FixedSpaceRow>
      </StickyActionContainer>
      {uiMode === 'confirmDelete' && <DeleteNoteModal />}
      {uiMode === 'confirmExit' && <ConfirmExitModal />}
    </>
  )
})
