// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import styled from 'styled-components'
import { faExclamation } from 'lib-icons'
import LocalDate from 'lib-common/local-date'
import { Td, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import SimpleSelect from 'lib-components/atoms/form/SimpleSelect'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { Placement } from '../../../../types/child'
import { useTranslation } from '../../../../state/i18n'
import { UIContext } from '../../../../state/ui'
import {
  createNewServiceNeed,
  updateNewServiceNeed
} from '../../../../api/child/new-service-needs'
import { UUID } from '../../../../types'

interface NewServiceNeedCreateRowProps {
  placement: Placement
  options: DropdownOption[]
  initialForm: FormData
  onSuccess: () => void
  onCancel: () => void
  editingId?: string
}
function NewServiceNeedEditorRow({
  placement,
  options,
  initialForm,
  onSuccess,
  onCancel,
  editingId
}: NewServiceNeedCreateRowProps) {
  const { i18n } = useTranslation()
  const t = i18n.childInformation.placements.serviceNeeds

  const { setErrorMessage } = useContext(UIContext)
  const [form, setForm] = useState<FormData>(initialForm)
  const [overlapWarning, setOverlapWarning] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  const formIsValid =
    form.startDate &&
    form.endDate &&
    form.optionId &&
    !form.endDate.isBefore(form.startDate)

  const onSubmit = () => {
    const startDate = form.startDate
    const endDate = form.endDate
    if (startDate !== undefined && endDate !== undefined && form.optionId) {
      if (
        placement.serviceNeeds.find(
          (sn) =>
            sn.id !== editingId &&
            sn.startDate.isEqualOrBefore(endDate) &&
            sn.endDate.isEqualOrAfter(startDate)
        ) !== undefined
      ) {
        setOverlapWarning(true)
      } else {
        onConfirmSave()
      }
    }
  }

  const onConfirmSave = () => {
    if (form.startDate && form.endDate && form.optionId) {
      setSubmitting(true)

      const request = editingId
        ? updateNewServiceNeed(editingId, {
            startDate: form.startDate,
            endDate: form.endDate,
            optionId: form.optionId,
            shiftCare: form.shiftCare
          })
        : createNewServiceNeed({
            placementId: placement.id,
            startDate: form.startDate,
            endDate: form.endDate,
            optionId: form.optionId,
            shiftCare: form.shiftCare
          })

      void request
        .then((res) => {
          if (res.isSuccess) {
            onSuccess()
          } else {
            setErrorMessage({
              type: 'error',
              title: i18n.common.error.unknown,
              text: i18n.common.error.saveFailed,
              resolveLabel: i18n.common.ok
            })
          }
        })
        .finally(() => setSubmitting(false))
    }
  }

  return (
    <>
      <Tr>
        <StyledTd>
          <FixedSpaceRow spacing="xs" alignItems="center">
            <DatePickerDeprecated
              date={form.startDate}
              onChange={(date) => setForm({ ...form, startDate: date })}
              minDate={placement.startDate}
              maxDate={placement.endDate}
              type="short"
            />
            <span>-</span>
            <DatePickerDeprecated
              date={form.endDate}
              onChange={(date) => setForm({ ...form, endDate: date })}
              minDate={placement.startDate}
              maxDate={placement.endDate}
              type="short"
            />
          </FixedSpaceRow>
        </StyledTd>
        <StyledTd>
          <SimpleSelect
            options={options}
            value={form.optionId}
            onChange={(e) => setForm({ ...form, optionId: e.target.value })}
            placeholder={t.optionPlaceholder}
          />
        </StyledTd>
        <StyledTd>
          <Checkbox
            label={t.shiftCare}
            hiddenLabel
            checked={form.shiftCare}
            onChange={(checked) => setForm({ ...form, shiftCare: checked })}
          />
        </StyledTd>
        <StyledTd />
        <StyledTd>
          <FixedSpaceRow justifyContent="flex-end" spacing="m">
            <InlineButton
              onClick={onCancel}
              text={i18n.common.cancel}
              disabled={submitting}
            />
            <InlineButton
              onClick={onSubmit}
              text={i18n.common.save}
              disabled={submitting || !formIsValid}
            />
          </FixedSpaceRow>
        </StyledTd>
      </Tr>

      {overlapWarning && (
        <InfoModal
          title={t.overlapWarning.title}
          text={t.overlapWarning.message}
          iconColour={'orange'}
          icon={faExclamation}
          resolve={{
            action: onConfirmSave,
            label: i18n.common.confirm
          }}
          reject={{
            action: () => setOverlapWarning(false),
            label: i18n.common.cancel
          }}
        />
      )}
    </>
  )
}

interface FormData {
  startDate: LocalDate | undefined
  endDate: LocalDate | undefined
  optionId: UUID | undefined
  shiftCare: boolean
}

interface DropdownOption {
  label: string
  value: string
}

const StyledTd = styled(Td)`
  vertical-align: middle;
`

export default NewServiceNeedEditorRow
