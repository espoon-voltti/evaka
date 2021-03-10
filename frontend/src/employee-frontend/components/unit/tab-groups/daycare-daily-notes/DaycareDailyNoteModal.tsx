import React, { useState } from 'react'
import { useTranslation } from '../../../../state/i18n'
import { faExclamation, faTrash } from '@evaka/lib-icons'
import { DaycareDailyNote } from '../../../../types/unit'
import { formatName } from '../../../../utils'
import LocalDate from '@evaka/lib-common/local-date'
import {
  DaycareDailyNoteFormData,
  deleteDaycareDailyNote,
  upsertChildDaycareDailyNote
} from '../../../../api/unit'
import FormModal from '@evaka/lib-components/molecules/modals/FormModal'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from '@evaka/lib-components/layout/flex-helpers'
import IconButton from '@evaka/lib-components/atoms/buttons/IconButton'
import InputField from '@evaka/lib-components/atoms/form/InputField'

interface Props {
  note: DaycareDailyNote | null
  childId: string | null
  groupId: string | null
  childFirstName: string
  childLastName: string
  onClose: () => void
  reload: () => void
}

const initialFormData = (
  note: DaycareDailyNote | null,
  childId: string | null,
  groupId: string | null
): DaycareDailyNoteFormData => {
  return note != null
    ? { ...note, childId, groupId }
    : {
        childId,
        groupId,
        date: LocalDate.today(),
        reminders: []
      }
}

export default React.memo(function DaycareDailyNoteModal({
  note,
  childId,
  groupId,
  childFirstName,
  childLastName,
  onClose,
  reload
}: Props) {
  const { i18n } = useTranslation()

  const [form, setForm] = useState<DaycareDailyNoteFormData>(
    initialFormData(note, childId, groupId)
  )

  const updateForm = (updates: Partial<DaycareDailyNoteFormData>) => {
    const newForm = { ...form, ...updates }
    setForm(newForm)
  }

  const submit = () => {
    if (childId != null) {
      void upsertChildDaycareDailyNote(childId, form).then(() => {
        onClose()
        reload()
      })
    } else {
      // TODO group daily note
    }
  }

  const deleteNote = async (event: React.MouseEvent) => {
    event.preventDefault()
    if (note?.id) await deleteDaycareDailyNote(note.id)
    onClose()
    reload()
  }

  return (
    <FormModal
      data-qa={'daycare-daily-note-modal'}
      title={i18n.unit.groups.daycareDailyNote.header}
      icon={faExclamation}
      iconColour={'blue'}
      resolve={{
        action: () => {
          submit()
          onClose()
        },
        label: i18n.common.confirm
      }}
      reject={{
        action: () => {
          onClose()
        },
        label: i18n.common.cancel
      }}
    >
      <FixedSpaceColumn alignItems={'center'} fullWidth={true}>
        <FixedSpaceRow>
          {formatName(childFirstName, childLastName, i18n)}
        </FixedSpaceRow>
        <FixedSpaceRow
          fullWidth={true}
          justifyContent={'flex-end'}
          spacing={'s'}
        >
          <IconButton icon={faTrash} onClick={deleteNote} />
          <span>{i18n.common.remove}</span>
        </FixedSpaceRow>
        <FixedSpaceColumn>
          <div className="bold">
            {i18n.unit.groups.daycareDailyNote.notesHeader}
          </div>
          <InputField
            value={form.note || ''}
            onChange={(value) => updateForm({ note: value })}
            data-qa="note-input"
          />
        </FixedSpaceColumn>
      </FixedSpaceColumn>
    </FormModal>
  )
})
