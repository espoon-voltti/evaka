// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import styled from 'styled-components'
import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import { AbsenceType } from 'lib-common/generated/enums'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { ChoiceChip } from 'lib-components/atoms/Chip'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import DatePicker, {
  DatePickerSpacer
} from 'lib-components/molecules/date-picker/DatePicker'
import {
  FixedSpaceColumn,
  FixedSpaceFlexWrap,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { useLang, useTranslation } from '../localization'
import { ErrorsOf, getErrorCount } from 'lib-common/form-validation'
import { AbsencesRequest, postAbsences, ReservationChild } from './api'
import { errorToInputInfo } from '../input-info-helper'

interface Props {
  close: () => void
  reload: () => void
  availableChildren: ReservationChild[]
}

export default React.memo(function AbsenceModal({
  close,
  reload,
  availableChildren
}: Props) {
  const i18n = useTranslation()
  const [lang] = useLang()

  const [form, setForm] = useState<Form>({
    startDate: LocalDate.today().format(),
    endDate: '',
    selectedChildren: availableChildren.map(({ id }) => id)
  })
  const updateForm = useCallback(
    (partialForm: Partial<Form>) =>
      setForm((previous) => ({ ...previous, ...partialForm })),
    [setForm]
  )

  const [showAllErrors, setShowAllErrors] = useState(false)
  const [request, errors] = validateForm(form)

  return (
    <AsyncFormModal
      mobileFullScreen
      title={i18n.calendar.absenceModal.title}
      resolve={{
        action: () => {
          if (!request) {
            setShowAllErrors(true)
            return Promise.resolve('AsyncButton.cancel')
          }
          return postAbsences(request)
        },
        onSuccess: () => {
          close()
          reload()
        },
        label: i18n.common.confirm
      }}
      reject={{
        action: close,
        label: i18n.common.cancel
      }}
    >
      <Label>{i18n.calendar.absenceModal.selectedChildren}</Label>
      <Gap size="s" />
      <FixedSpaceColumn>
        {availableChildren.map((child) => (
          <Checkbox
            key={child.id}
            label={child.preferredName || child.firstName.split(' ')[0]}
            checked={form.selectedChildren.includes(child.id)}
            onChange={(checked) =>
              updateForm({
                selectedChildren: checked
                  ? [...form.selectedChildren, child.id]
                  : form.selectedChildren.filter((id) => id !== child.id)
              })
            }
            data-qa={`child-${child.id}`}
          />
        ))}
      </FixedSpaceColumn>
      <Gap size="m" />
      <Label>{i18n.calendar.absenceModal.dateRange}</Label>
      <FixedSpaceRow alignItems="flex-start" spacing="xs">
        <DatePicker
          date={form.startDate}
          onChange={(date) => updateForm({ startDate: date })}
          locale={lang}
          isValidDate={(date) => LocalDate.today().isEqualOrBefore(date)}
          info={
            errors && errorToInputInfo(errors.startDate, i18n.validationErrors)
          }
          hideErrorsBeforeTouched={!showAllErrors}
          data-qa="start-date"
        />
        <DatePickerSpacer />
        <DatePicker
          date={form.endDate}
          onChange={(date) => updateForm({ endDate: date })}
          locale={lang}
          isValidDate={(date) => LocalDate.today().isEqualOrBefore(date)}
          info={
            errors && errorToInputInfo(errors.endDate, i18n.validationErrors)
          }
          hideErrorsBeforeTouched={!showAllErrors}
          initialMonth={
            LocalDate.parseFiOrNull(form.startDate) ?? LocalDate.today()
          }
          data-qa="end-date"
        />
      </FixedSpaceRow>
      <Gap size="m" />
      <Label>{i18n.calendar.absenceModal.absenceType}</Label>
      <Gap size="s" />
      <FixedSpaceFlexWrap verticalSpacing="xs">
        {(['SICKLEAVE', 'OTHER_ABSENCE', 'PLANNED_ABSENCE'] as const).map(
          (type) => (
            <ChoiceChip
              key={type}
              text={i18n.calendar.absenceModal.absenceTypes[type]}
              selected={form.absenceType === type}
              onChange={(selected) =>
                updateForm({ absenceType: selected ? type : undefined })
              }
              data-qa={`absence-${type}`}
            />
          )
        )}
      </FixedSpaceFlexWrap>
      {showAllErrors && !form.absenceType ? (
        <Warning>{i18n.validationErrors.requiredSelection}</Warning>
      ) : null}
    </AsyncFormModal>
  )
})

interface Form {
  startDate: string
  endDate: string
  selectedChildren: string[]
  absenceType?: AbsenceType
}

const validateForm = (
  form: Form
): [AbsencesRequest] | [undefined, ErrorsOf<Form>] => {
  const startDate = LocalDate.parseFiOrNull(form.startDate)
  const endDate = LocalDate.parseFiOrNull(form.endDate)

  const errors: ErrorsOf<Form> = {
    startDate:
      form.startDate === ''
        ? 'required'
        : startDate === null
        ? 'validDate'
        : startDate.isBefore(LocalDate.today())
        ? 'dateTooEarly'
        : undefined,
    endDate:
      form.endDate === ''
        ? 'required'
        : endDate === null
        ? 'validDate'
        : startDate && endDate.isBefore(startDate)
        ? 'dateTooEarly'
        : undefined,
    absenceType: form.absenceType === undefined ? 'required' : undefined
  }

  if (getErrorCount(errors) > 0) {
    return [undefined, errors]
  }

  return [
    {
      childIds: form.selectedChildren,
      // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
      dateRange: new FiniteDateRange(startDate!, endDate!),
      // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
      absenceType: form.absenceType!
    }
  ]
}

const Warning = styled.div`
  color: ${(p) => p.theme.colors.accents.orangeDark};
`
