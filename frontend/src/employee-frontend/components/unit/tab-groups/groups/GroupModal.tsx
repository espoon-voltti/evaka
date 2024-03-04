// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'

import { UpdateStateFn } from 'lib-common/form-state'
import LocalDate from 'lib-common/local-date'
import InputField from 'lib-components/atoms/form/InputField'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'

import { EVAKA_START } from '../../../../constants'
import { useTranslation } from '../../../../state/i18n'
import { UIContext } from '../../../../state/ui'
import { allPropertiesTrue } from '../../../../utils/validation/validations'
import { createGroupMutation } from '../../queries'

interface Props {
  unitId: string
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

export default React.memo(function GroupModal({ unitId }: Props) {
  const { i18n } = useTranslation()
  const { clearUiMode } = useContext(UIContext)

  const initialForm: CreateGroupForm = {
    name: '',
    startDate: EVAKA_START,
    initialCaretakers: 3
  }
  const [form, setForm] = useState<CreateGroupForm>(initialForm)
  const [validationResult, setValidationResult] =
    useState<FormValidationResult>({
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

  const assignForm: UpdateStateFn<CreateGroupForm> = (values) => {
    setForm({ ...form, ...values })
  }

  return (
    <MutateFormModal
      title={i18n.unit.groups.createModal.title}
      resolveMutation={createGroupMutation}
      resolveAction={() => ({
        daycareId: unitId,
        body: {
          name: form.name,
          startDate: form.startDate,
          initialCaretakers: form.initialCaretakers
        }
      })}
      onSuccess={clearUiMode}
      resolveLabel={i18n.unit.groups.createModal.confirmButton}
      rejectAction={clearUiMode}
      rejectLabel={i18n.unit.groups.createModal.cancelButton}
    >
      <FixedSpaceColumn spacing="m">
        <div>
          <Label>{i18n.unit.groups.createModal.name}</Label>
          <InputField
            value={form.name}
            onChange={(value) => assignForm({ name: value })}
            data-qa="new-group-name-input"
            info={
              !validationResult.fields.name
                ? {
                    text: i18n.unit.groups.createModal.errors.nameRequired,
                    status: 'warning'
                  }
                : undefined
            }
          />
        </div>
        <div>
          <Label>{i18n.common.form.startDate}</Label>
          <DatePickerDeprecated
            date={form.startDate}
            onChange={(startDate) => assignForm({ startDate })}
            type="full-width"
          />
        </div>
        <div>
          <Label>{i18n.unit.groups.createModal.initialCaretakers}</Label>
          <InputField
            value={form.initialCaretakers.toString()}
            type="number"
            min={0}
            step={1}
            onChange={(value) =>
              assignForm({ initialCaretakers: Number(value) })
            }
            data-qa="new-group-name-input"
          />
        </div>
      </FixedSpaceColumn>
    </MutateFormModal>
  )
})
