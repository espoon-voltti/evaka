import React, { useState } from 'react'
import { useTranslation } from '../../../../state/i18n'
import { faExclamation, faTrash } from '@evaka/lib-icons'
import {
  DaycareDailyNote,
  DaycareDailyNoteReminder
} from '../../../../types/unit'
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
import Checkbox from '@evaka/lib-components/atoms/form/Checkbox'
import Radio from '@evaka/lib-components/atoms/form/Radio'

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

  const toggleReminder = (reminder: DaycareDailyNoteReminder) => {
    const newReminders = form.reminders.some((r) => r == reminder)
      ? note?.reminders.filter((r) => r != reminder)
      : [...form.reminders, reminder]
    updateForm({ reminders: newReminders })
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
      <FixedSpaceColumn>
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
        </FixedSpaceColumn>
        <FixedSpaceColumn alignItems={'left'} fullWidth={true}>
          <div className="bold">
            {i18n.unit.groups.daycareDailyNote.notesHeader}
          </div>
          <InputField
            value={form.note || ''}
            placeholder={i18n.unit.groups.daycareDailyNote.notesHint}
            onChange={(value) => updateForm({ note: value })}
            data-qa="note-input"
          />
          <div className="bold">
            {i18n.unit.groups.daycareDailyNote.sleepingHeader}
          </div>
          <Radio
            checked={form.sleepingNote == 'GOOD'}
            label={i18n.unit.groups.daycareDailyNote.level.GOOD}
            onChange={() => updateForm({ sleepingNote: 'GOOD' })}
            dataQa={'sleeping-level-good'}
          />
          <Radio
            checked={form.sleepingNote == 'MEDIUM'}
            label={i18n.unit.groups.daycareDailyNote.level.MEDIUM}
            onChange={() => updateForm({ sleepingNote: 'MEDIUM' })}
            dataQa={'sleeping-level-medium'}
          />
          <Radio
            checked={form.sleepingNote == 'NONE'}
            label={i18n.unit.groups.daycareDailyNote.level.NONE}
            onChange={() => updateForm({ sleepingNote: 'NONE' })}
            dataQa={'sleeping-level-none'}
          />
          <FixedSpaceRow
            fullWidth={true}
            justifyContent={'flex-start'}
            spacing={'s'}
          >
            <InputField
              type={'number'}
              placeholder={i18n.unit.groups.daycareDailyNote.sleepingHint}
              value={form.sleepingHours || ''}
              onChange={(value) => updateForm({ sleepingHours: value })}
              data-qa="sleeping-hours-input"
            />
            <span>{i18n.unit.groups.daycareDailyNote.sleepingHours}</span>
          </FixedSpaceRow>
          <FixedSpaceColumn>
            <div className="bold">
              {i18n.unit.groups.daycareDailyNote.reminderHeader}
            </div>
            <Checkbox
              label={i18n.unit.groups.daycareDailyNote.reminderType.DIAPERS}
              checked={
                form.reminders.some((reminder) => reminder == 'DIAPERS') ||
                false
              }
              onChange={() => toggleReminder('DIAPERS')}
              dataQa="checkbox-diapers"
            />
            <Checkbox
              label={i18n.unit.groups.daycareDailyNote.reminderType.CLOTHES}
              checked={
                form.reminders.some((reminder) => reminder == 'CLOTHES') ||
                false
              }
              onChange={() => toggleReminder('CLOTHES')}
              dataQa="checkbox-clothes"
            />
            <Checkbox
              label={i18n.unit.groups.daycareDailyNote.reminderType.LAUNDRY}
              checked={
                form.reminders.some((reminder) => reminder == 'LAUNDRY') ||
                false
              }
              onChange={() => toggleReminder('LAUNDRY')}
              dataQa="checkbox-laundry"
            />
          </FixedSpaceColumn>
          <InputField
            type={'text'}
            placeholder={
              i18n.unit.groups.daycareDailyNote.otherThingsToRememberHeader
            }
            value={form.reminderNote || ''}
            onChange={(value) => updateForm({ reminderNote: value })}
            data-qa="reminder-note-input"
          />
        </FixedSpaceColumn>
      </FixedSpaceColumn>
    </FormModal>
  )
})
