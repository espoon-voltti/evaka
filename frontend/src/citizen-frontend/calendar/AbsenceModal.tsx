// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import styled from 'styled-components'
import FiniteDateRange from 'lib-common/finite-date-range'
import { ErrorsOf, getErrorCount } from 'lib-common/form-validation'
import { ReservationChild } from 'lib-common/generated/api-types/reservations'
import { AbsenceType } from 'lib-common/generated/enums'
import LocalDate from 'lib-common/local-date'
import { ChoiceChip, SelectionChip } from 'lib-components/atoms/Chip'
import {
  FixedSpaceFlexWrap,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import DatePicker, {
  DatePickerSpacer
} from 'lib-components/molecules/date-picker/DatePicker'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { errorToInputInfo } from '../input-info-helper'
import { useLang, useTranslation } from '../localization'
import { AbsencesRequest, postAbsences } from './api'

interface Props {
  close: () => void
  reload: () => void
  availableChildren: ReservationChild[]
  initialDate: LocalDate | undefined
}

function initialForm(
  initialDate: LocalDate | undefined,
  availableChildren: ReservationChild[]
): Form {
  const selectedChildren = availableChildren.map((child) => child.id)
  if (initialDate) {
    return {
      startDate: initialDate.format(),
      endDate: initialDate.format(),
      selectedChildren
    }
  }
  return {
    startDate: LocalDate.today().format(),
    endDate: '',
    selectedChildren
  }
}

export default React.memo(function AbsenceModal({
  close,
  reload,
  availableChildren,
  initialDate
}: Props) {
  const i18n = useTranslation()
  const [lang] = useLang()

  const [form, setForm] = useState<Form>(() =>
    initialForm(initialDate, availableChildren)
  )

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
      resolveAction={(cancel) => {
        if (!request) {
          setShowAllErrors(true)
          return cancel()
        }
        return postAbsences(request)
      }}
      onSuccess={() => {
        close()
        reload()
      }}
      resolveLabel={i18n.common.confirm}
      rejectAction={close}
      rejectLabel={i18n.common.cancel}
    >
      <Label>{i18n.calendar.absenceModal.selectedChildren}</Label>
      <P>{i18n.calendar.absenceModal.selectChildrenInfo}</P>
      <Gap size="xs" />
      <FixedSpaceFlexWrap>
        {availableChildren.map((child) => (
          <SelectionChip
            key={child.id}
            text={child.preferredName || child.firstName.split(' ')[0]}
            selected={form.selectedChildren.includes(child.id)}
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
      </FixedSpaceFlexWrap>
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
