// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UpdateStateFn } from 'lib-common/form-state'
import {
  ChildDailyNote,
  ChildDailyNoteBody,
  ChildDailyNoteReminder
} from 'lib-common/generated/api-types/note'
import { UUID } from 'lib-common/types'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import ResponsiveInlineButton from 'lib-components/atoms/buttons/ResponsiveInlineButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import Radio from 'lib-components/atoms/form/Radio'
import TextArea from 'lib-components/atoms/form/TextArea'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H2, H3 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faTrash } from 'lib-icons'
import React, { useCallback, useState } from 'react'
import {
  deleteChildDailyNote,
  postChildDailyNote,
  putChildDailyNote
} from '../../../../api/daycare-notes'
import { useTranslation } from '../../../../state/i18n'

interface ChildDailyNoteFormData
  extends Omit<ChildDailyNoteBody, 'sleepingMinutes'> {
  sleepingHours: string
  sleepingMinutes: string
}

const initialFormData = (
  note: ChildDailyNote | null
): ChildDailyNoteFormData => {
  return note
    ? {
        ...note,
        sleepingHours: note.sleepingMinutes
          ? Math.floor(Number(note.sleepingMinutes) / 60).toString()
          : '',
        sleepingMinutes: note.sleepingMinutes
          ? Math.round(Number(note.sleepingMinutes) % 60).toString()
          : ''
      }
    : {
        reminders: [],
        feedingNote: null,
        note: '',
        reminderNote: '',
        sleepingHours: '',
        sleepingMinutes: '',
        sleepingNote: null
      }
}

const formDataToRequestBody = (
  form: ChildDailyNoteFormData
): ChildDailyNoteBody => ({
  feedingNote: form.feedingNote,
  note: form.note,
  reminderNote: form.reminderNote,
  reminders: form.reminders,
  sleepingMinutes:
    form.sleepingHours || form.sleepingMinutes
      ? 60 * (parseInt(form.sleepingHours) || 0) +
        (parseInt(form.sleepingMinutes) || 0)
      : null,
  sleepingNote: form.sleepingNote
})

interface Props {
  note: ChildDailyNote | null
  childId: UUID
  onSuccess: () => void
  onCancel: () => void
}

export const ChildDailyNoteForm = React.memo(function ChildDailyNoteForm({
  note,
  childId,
  onCancel,
  onSuccess
}: Props) {
  const { i18n } = useTranslation()

  const [form, setForm] = useState<ChildDailyNoteFormData>(
    initialFormData(note)
  )
  const updateForm: UpdateStateFn<ChildDailyNoteFormData> = useCallback(
    (updates) => {
      setForm((prev) => ({ ...prev, ...updates }))
    },
    []
  )
  const toggleReminder = (reminder: ChildDailyNoteReminder) => {
    const reminders = form.reminders.some((r) => r == reminder)
      ? form.reminders.filter((r) => r != reminder)
      : [...form.reminders, reminder]
    updateForm({ reminders })
  }

  const [submitting, setSubmitting] = useState(false)
  const submit = useCallback(() => {
    setSubmitting(true)
    const body = formDataToRequestBody(form)
    const promise = note
      ? putChildDailyNote(note.id, body)
      : postChildDailyNote(childId, body)
    return promise
      .then((res) => res.map(() => onSuccess()))
      .finally(() => setSubmitting(false))
  }, [childId, form, note, onSuccess])

  const [deleting, setDeleting] = useState(false)
  const deleteNote = useCallback(() => {
    if (!note?.id) {
      return Promise.reject()
    }
    setDeleting(true)
    return deleteChildDailyNote(note.id)
      .then((res) => res.map(() => onSuccess()))
      .finally(() => setDeleting(false))
  }, [note, onSuccess])

  return (
    <>
      <FixedSpaceRow fullWidth justifyContent="space-between" spacing="s">
        <H2 primary noMargin>
          {i18n.unit.groups.daycareDailyNote.header}
        </H2>

        {note && (
          <ResponsiveInlineButton
            icon={faTrash}
            onClick={deleteNote}
            disabled={deleting || submitting}
            text={i18n.common.clear}
            data-qa="btn-delete"
          />
        )}
      </FixedSpaceRow>

      <Gap />

      <FixedSpaceColumn alignItems="left" fullWidth spacing="L">
        <TextArea
          value={form.note || ''}
          placeholder={i18n.unit.groups.daycareDailyNote.notesHint}
          onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) =>
            updateForm({ note: e.target.value })
          }
          data-qa="note-input"
        />

        <H3 primary noMargin>
          {i18n.unit.groups.daycareDailyNote.otherThings}
        </H3>

        <FixedSpaceColumn>
          <div className="bold">
            {i18n.unit.groups.daycareDailyNote.feedingHeader}
          </div>
          <Radio
            checked={form.feedingNote == 'GOOD'}
            label={i18n.unit.groups.daycareDailyNote.level.GOOD}
            onChange={() => updateForm({ feedingNote: 'GOOD' })}
            data-qa="feeding-level-good"
          />
          <Radio
            checked={form.feedingNote == 'MEDIUM'}
            label={i18n.unit.groups.daycareDailyNote.level.MEDIUM}
            onChange={() => updateForm({ feedingNote: 'MEDIUM' })}
            data-qa="feeding-level-medium"
          />
          <Radio
            checked={form.feedingNote == 'NONE'}
            label={i18n.unit.groups.daycareDailyNote.level.NONE}
            onChange={() => updateForm({ feedingNote: 'NONE' })}
            data-qa="feeding-level-none"
          />
        </FixedSpaceColumn>

        <FixedSpaceColumn>
          <div className="bold">
            {i18n.unit.groups.daycareDailyNote.sleepingHeader}
          </div>
          <Radio
            checked={form.sleepingNote == 'GOOD'}
            label={i18n.unit.groups.daycareDailyNote.level.GOOD}
            onChange={() => updateForm({ sleepingNote: 'GOOD' })}
            data-qa="sleeping-level-good"
          />
          <Radio
            checked={form.sleepingNote == 'MEDIUM'}
            label={i18n.unit.groups.daycareDailyNote.level.MEDIUM}
            onChange={() => updateForm({ sleepingNote: 'MEDIUM' })}
            data-qa="sleeping-level-medium"
          />
          <Radio
            checked={form.sleepingNote == 'NONE'}
            label={i18n.unit.groups.daycareDailyNote.level.NONE}
            onChange={() => updateForm({ sleepingNote: 'NONE' })}
            data-qa="sleeping-level-none"
          />
        </FixedSpaceColumn>

        <FixedSpaceRow fullWidth justifyContent="flex-start" spacing="s">
          <InputField
            type="number"
            placeholder={i18n.unit.groups.daycareDailyNote.sleepingHoursHint}
            value={form.sleepingHours || ''}
            onChange={(value) => updateForm({ sleepingHours: value })}
            data-qa="sleeping-hours-input"
          />
          <span>{i18n.unit.groups.daycareDailyNote.sleepingHours}</span>
          <InputField
            type="number"
            placeholder={i18n.unit.groups.daycareDailyNote.sleepingMinutesHint}
            value={form.sleepingMinutes || ''}
            onChange={(value) => updateForm({ sleepingMinutes: value })}
            data-qa="sleeping-minutes-input"
            info={
              Number(form.sleepingMinutes) > 59
                ? {
                    text: i18n.common.error.minutes,
                    status: 'warning'
                  }
                : undefined
            }
          />
          <span>{i18n.unit.groups.daycareDailyNote.sleepingMinutes}</span>
        </FixedSpaceRow>

        <FixedSpaceColumn>
          <div className="bold">
            {i18n.unit.groups.daycareDailyNote.reminderHeader}
          </div>
          <Checkbox
            label={i18n.unit.groups.daycareDailyNote.reminderType.DIAPERS}
            checked={
              form.reminders.some((reminder) => reminder == 'DIAPERS') || false
            }
            onChange={() => toggleReminder('DIAPERS')}
            data-qa="checkbox-diapers"
          />
          <Checkbox
            label={i18n.unit.groups.daycareDailyNote.reminderType.CLOTHES}
            checked={
              form.reminders.some((reminder) => reminder == 'CLOTHES') || false
            }
            onChange={() => toggleReminder('CLOTHES')}
            data-qa="checkbox-clothes"
          />
          <Checkbox
            label={i18n.unit.groups.daycareDailyNote.reminderType.LAUNDRY}
            checked={
              form.reminders.some((reminder) => reminder == 'LAUNDRY') || false
            }
            onChange={() => toggleReminder('LAUNDRY')}
            data-qa="checkbox-laundry"
          />
        </FixedSpaceColumn>

        <TextArea
          type="text"
          placeholder={
            i18n.unit.groups.daycareDailyNote.otherThingsToRememberHeader
          }
          value={form.reminderNote || ''}
          onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) =>
            updateForm({ reminderNote: e.target.value })
          }
          data-qa="reminder-note-input"
        />
        <FixedSpaceRow justifyContent="space-around">
          <Button
            onClick={onCancel}
            text={i18n.common.cancel}
            data-qa="btn-cancel"
          />
          <AsyncButton
            primary
            onClick={submit}
            text={i18n.common.save}
            onSuccess={onSuccess}
            data-qa="btn-submit"
          />
        </FixedSpaceRow>
      </FixedSpaceColumn>
    </>
  )
})
