// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import FiniteDateRange from 'lib-common/finite-date-range'
import { ErrorsOf, getErrorCount } from 'lib-common/form-validation'
import { AbsenceType } from 'lib-common/generated/api-types/daycare'
import {
  AbsenceRequest,
  ReservationChild
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { formatPreferredName } from 'lib-common/names'
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
import { featureFlags } from 'lib-customizations/citizen'

import ModalAccessibilityWrapper from '../ModalAccessibilityWrapper'
import { errorToInputInfo } from '../input-info-helper'
import { useLang, useTranslation } from '../localization'

import { postAbsences } from './api'

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
      startDate: initialDate,
      endDate: initialDate,
      selectedChildren
    }
  }
  return {
    startDate: LocalDate.todayInSystemTz(),
    endDate: null,
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

  const showShiftCareAbsenceType = useMemo(
    () =>
      featureFlags.citizenShiftCareAbsenceEnabled
        ? availableChildren.some(({ inShiftCareUnit }) => inShiftCareUnit)
        : false,
    [availableChildren]
  )

  return (
    <ModalAccessibilityWrapper>
      <AsyncFormModal
        mobileFullScreen
        title={i18n.calendar.absenceModal.title}
        resolveAction={() => {
          if (!request) {
            setShowAllErrors(true)
            return
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
              text={formatPreferredName(child)}
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
            minDate={LocalDate.todayInSystemTz()}
            info={
              errors &&
              errorToInputInfo(errors.startDate, i18n.validationErrors)
            }
            hideErrorsBeforeTouched={!showAllErrors}
            data-qa="start-date"
            errorTexts={i18n.validationErrors}
          />
          <DatePickerSpacer />
          <DatePicker
            date={form.endDate}
            onChange={(date) => updateForm({ endDate: date })}
            locale={lang}
            minDate={LocalDate.todayInSystemTz()}
            info={
              errors && errorToInputInfo(errors.endDate, i18n.validationErrors)
            }
            hideErrorsBeforeTouched={!showAllErrors}
            initialMonth={form.startDate ?? LocalDate.todayInSystemTz()}
            data-qa="end-date"
            errorTexts={i18n.validationErrors}
          />
        </FixedSpaceRow>
        <Gap size="m" />
        <Label>{i18n.calendar.absenceModal.absenceType}</Label>
        <Gap size="s" />
        <FixedSpaceFlexWrap verticalSpacing="xs">
          <ChoiceChip
            text={i18n.calendar.absenceModal.absenceTypes.SICKLEAVE}
            selected={form.absenceType === 'SICKLEAVE'}
            onChange={(selected) =>
              updateForm({ absenceType: selected ? 'SICKLEAVE' : undefined })
            }
            data-qa="absence-SICKLEAVE"
          />
          <ChoiceChip
            text={i18n.calendar.absenceModal.absenceTypes.OTHER_ABSENCE}
            selected={form.absenceType === 'OTHER_ABSENCE'}
            onChange={(selected) =>
              updateForm({
                absenceType: selected ? 'OTHER_ABSENCE' : undefined
              })
            }
            data-qa="absence-OTHER_ABSENCE"
          />
          {showShiftCareAbsenceType && (
            <ChoiceChip
              text={i18n.calendar.absenceModal.absenceTypes.PLANNED_ABSENCE}
              selected={form.absenceType === 'PLANNED_ABSENCE'}
              onChange={(selected) =>
                updateForm({
                  absenceType: selected ? 'PLANNED_ABSENCE' : undefined
                })
              }
              data-qa="absence-PLANNED_ABSENCE"
            />
          )}
        </FixedSpaceFlexWrap>
        {showAllErrors && !form.absenceType ? (
          <Warning>{i18n.validationErrors.requiredSelection}</Warning>
        ) : null}
      </AsyncFormModal>
    </ModalAccessibilityWrapper>
  )
})

interface Form {
  startDate: LocalDate | null
  endDate: LocalDate | null
  selectedChildren: string[]
  absenceType?: AbsenceType
}

const validateForm = (
  form: Form
): [AbsenceRequest] | [undefined, ErrorsOf<Form>] => {
  const errors: ErrorsOf<Form> = {
    startDate: !form.startDate ? 'required' : undefined,
    endDate: !form.endDate ? 'required' : undefined,
    absenceType: form.absenceType === undefined ? 'required' : undefined
  }

  if (getErrorCount(errors) > 0) {
    return [undefined, errors]
  }

  return [
    {
      childIds: form.selectedChildren,
      // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
      dateRange: new FiniteDateRange(form.startDate!, form.endDate!),
      // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
      absenceType: form.absenceType!
    }
  ]
}

const Warning = styled.div`
  color: ${(p) => p.theme.colors.accents.a2orangeDark};
`
