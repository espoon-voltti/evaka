// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import FormModal from '@evaka/lib-components/src/molecules/modals/FormModal'
import { useTranslation } from '~state/i18n'
import { UIContext } from '~state/ui'
import { DatePicker } from '@evaka/lib-components/src/molecules/DatePicker'
import InputField from '@evaka/lib-components/src/atoms/form/InputField'
import { allPropertiesTrue } from '~utils/validation/validations'
import '~components/unit/tab-groups/groups/GroupModal.scss'
import { createGroup } from '~api/unit'
import { EVAKA_START } from '~constants'
import LocalDate from '@evaka/lib-common/src/local-date'
import { FixedSpaceColumn } from '@evaka/lib-components/src/layout/flex-helpers'

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
  const [
    validationResult,
    setValidationResult
  ] = useState<FormValidationResult>({
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
      resolve={{
        action: submit,
        label: i18n.unit.groups.createModal.confirmButton
      }}
      reject={{
        action: clearUiMode,
        label: i18n.unit.groups.createModal.cancelButton
      }}
    >
      <FixedSpaceColumn spacing={'m'}>
        <section>
          <div className="bold">{i18n.unit.groups.createModal.name}</div>
          <InputField
            value={form.name}
            onChange={(value) => assignForm({ name: value })}
            dataQa={'new-group-name-input'}
            info={
              !validationResult.fields.name
                ? {
                    text: i18n.unit.groups.createModal.errors.nameRequired,
                    status: 'warning'
                  }
                : undefined
            }
          />
        </section>
        <section>
          <div className="bold">{i18n.common.form.startDate}</div>
          <DatePicker
            date={form.startDate}
            onChange={(startDate) => assignForm({ startDate })}
            type="full-width"
          />
        </section>
        <section>
          <div className="bold">
            {i18n.unit.groups.createModal.initialCaretakers}
          </div>
          <InputField
            value={form.initialCaretakers.toString()}
            type={'number'}
            min={0}
            step={1}
            onChange={(value) =>
              assignForm({ initialCaretakers: Number(value) })
            }
            dataQa={'new-group-name-input'}
          />
        </section>
      </FixedSpaceColumn>
    </FormModal>
  )
}

export default GroupModal
