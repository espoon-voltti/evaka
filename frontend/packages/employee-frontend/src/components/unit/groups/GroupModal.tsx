// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import FormModal from '~components/common/FormModal'
import { useTranslation } from '~state/i18n'
import { UIContext } from '~state/ui'
import { Section } from '~components/shared/alpha'
import { DatePicker } from '~components/common/DatePicker'
import { Input } from '~components/shared/alpha'
import { allPropertiesTrue } from '~utils/validation/validations'
import '~components/unit/groups/GroupModal.scss'
import { createGroup } from '~api/unit'
import { EVAKA_START } from '~constants'
import LocalDate from '@evaka/lib-common/src/local-date'

interface Props {
  unitId: string
  reload: () => void
}

interface FormValidationResult {
  valid: boolean
  fields: {
    name: boolean
  }
}

interface CreateGroupForm {
  name: string
  startDate: LocalDate
  initialCaretakers: number
}

function GroupModal({ unitId, reload }: Props) {
  const { i18n } = useTranslation()
  const { clearUiMode } = useContext(UIContext)

  const initialForm: CreateGroupForm = {
    name: '',
    startDate: EVAKA_START,
    initialCaretakers: 3
  }
  const [form, setForm] = useState<CreateGroupForm>(initialForm)
  const [validationResult, setValidationResult] = useState<
    FormValidationResult
  >({
    valid: true,
    fields: {
      name: true
    }
  })

  useEffect(() => {
    const fields = {
      name: form.name.length > 0
    }
    setValidationResult({
      valid: allPropertiesTrue(fields),
      fields
    })
  }, [form])

  const assignForm = (values: Partial<CreateGroupForm>) => {
    setForm({ ...form, ...values })
  }

  const submit = () => {
    if (!unitId) return

    void createGroup(
      unitId,
      form.name,
      form.startDate,
      form.initialCaretakers
    ).then(() => {
      reload()
      clearUiMode()
    })
  }
  return (
    <FormModal
      title={i18n.unit.groups.createModal.title}
      resolveLabel={i18n.unit.groups.createModal.confirmButton}
      resolve={submit}
      reject={() => clearUiMode()}
      rejectLabel={i18n.unit.groups.createModal.cancelButton}
    >
      <Section>
        <div className="bold">{i18n.unit.groups.createModal.name}</div>
        <Input
          value={form.name}
          onChange={(e) => assignForm({ name: e.target.value })}
          dataQa={'new-group-name-input'}
        />
        {!validationResult.fields.name && (
          <div className="error">
            {i18n.unit.groups.createModal.errors.nameRequired}
          </div>
        )}
      </Section>
      <Section>
        <div className="bold">{i18n.common.form.startDate}</div>
        <DatePicker
          date={form.startDate}
          onChange={(startDate) => assignForm({ startDate })}
          type="full-width"
        />
      </Section>
      <Section>
        <div className="bold">
          {i18n.unit.groups.createModal.initialCaretakers}
        </div>
        <Input
          value={form.initialCaretakers.toString()}
          type="number"
          min={0}
          step={0.1}
          onChange={(e) =>
            assignForm({ initialCaretakers: Number(e.target.value) })
          }
          dataQa={'new-group-name-input'}
        />
      </Section>
    </FormModal>
  )
}

export default GroupModal
