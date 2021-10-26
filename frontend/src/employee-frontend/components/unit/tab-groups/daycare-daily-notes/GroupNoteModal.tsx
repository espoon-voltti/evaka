// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { GroupNote, GroupNoteBody } from 'lib-common/generated/api-types/note'
import {
  deleteGroupNote,
  postGroupNote,
  putGroupNote
} from '../../../../api/daycare-notes'
import { useTranslation } from '../../../../state/i18n'
import { faExclamation, faTrash } from 'lib-icons'
import FormModal from 'lib-components/molecules/modals/FormModal'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import TextArea from 'lib-components/atoms/form/TextArea'
import { UUID } from 'lib-common/types'
import { UpdateStateFn } from 'lib-common/form-state'
import LocalDate from 'lib-common/local-date'

type GroupNoteFormData = GroupNoteBody

const initialFormData = (note: GroupNote | null): GroupNoteFormData => {
  return note
    ? { ...note }
    : { note: '', expires: LocalDate.today().addDays(7) }
}

const formDataToRequestBody = (form: GroupNoteFormData): GroupNoteBody => ({
  note: form.note,
  expires: form.expires
})

interface Props {
  groupNote: GroupNote | null
  groupId: UUID
  onClose: () => void
  reload: () => void
}

export default React.memo(function GroupNoteModal({
  groupNote,
  groupId,
  onClose,
  reload
}: Props) {
  const { i18n } = useTranslation()

  const [form, setForm] = useState<GroupNoteFormData>(
    initialFormData(groupNote)
  )

  const updateForm: UpdateStateFn<GroupNoteFormData> = (updates) => {
    setForm((prev) => ({ ...prev, ...updates }))
  }

  const [submitting, setSubmitting] = useState(false)

  const submit = () => {
    // TODO error handling
    setSubmitting(true)
    const body = formDataToRequestBody(form)
    const promise = groupNote
      ? putGroupNote(groupNote.id, body)
      : postGroupNote(groupId, body)
    void promise
      .then(() => {
        onClose()
        reload()
      })
      .finally(() => setSubmitting(false))
  }

  const deleteNote = async (event: React.MouseEvent) => {
    event.preventDefault()
    if (groupNote) await deleteGroupNote(groupNote.id)
    onClose()
    reload()
  }

  return (
    <FormModal
      data-qa={'daycare-daily-group-note-modal'}
      title={i18n.unit.groups.daycareDailyNote.groupNoteHeader}
      icon={faExclamation}
      iconColour={'blue'}
      resolve={{
        action: () => {
          submit()
        },
        disabled: submitting,
        label: i18n.common.confirm
      }}
      reject={{
        action: () => {
          onClose()
        },
        label: i18n.common.cancel
      }}
    >
      <FixedSpaceColumn>
        <FixedSpaceColumn alignItems={'center'} fullWidth={true}>
          <FixedSpaceRow
            fullWidth={true}
            justifyContent={'flex-end'}
            spacing={'s'}
          >
            <IconButton
              icon={faTrash}
              onClick={deleteNote}
              data-qa={'btn-delete-note'}
            />
            <span>{i18n.common.remove}</span>
          </FixedSpaceRow>
        </FixedSpaceColumn>
        <FixedSpaceColumn alignItems={'left'} fullWidth={true} spacing={'L'}>
          <FixedSpaceColumn>
            <div className="bold">
              {i18n.unit.groups.daycareDailyNote.groupNotesHeader}
            </div>
            <TextArea
              value={form.note || ''}
              placeholder={i18n.unit.groups.daycareDailyNote.groupNoteHint}
              onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) =>
                updateForm({ note: e.target.value })
              }
              data-qa="group-note-input"
            />
          </FixedSpaceColumn>
        </FixedSpaceColumn>
      </FixedSpaceColumn>
    </FormModal>
  )
})
