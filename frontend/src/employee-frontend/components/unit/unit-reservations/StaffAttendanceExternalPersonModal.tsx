// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React, { useCallback } from 'react'
import styled from 'styled-components'

import { Failure, wrapResult } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import { boolean, localDate, localTime, string } from 'lib-common/form/fields'
import {
  object,
  oneOf,
  required,
  transformed,
  validated,
  value
} from 'lib-common/form/form'
import { useForm, useFormField } from 'lib-common/form/hooks'
import {
  StateOf,
  ValidationError,
  ValidationSuccess
} from 'lib-common/form/types'
import { ExternalAttendanceBody } from 'lib-common/generated/api-types/attendance'
import { DaycareGroup } from 'lib-common/generated/api-types/daycare'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'
import StatusIcon from 'lib-components/atoms/StatusIcon'
import { Button } from 'lib-components/atoms/buttons/Button'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import {
  InputFieldF,
  InputFieldUnderRow
} from 'lib-components/atoms/form/InputField'
import { TimeInputF } from 'lib-components/atoms/form/TimeInput'
import { InlineAsyncButton } from 'lib-components/employee/notes/InlineAsyncButton'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { DatePickerF } from 'lib-components/molecules/date-picker/DatePicker'
import { PlainModal } from 'lib-components/molecules/modals/BaseModal'
import { fontWeights, H1, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faPlus } from 'lib-icons'

import { upsertDailyExternalRealtimeAttendances } from '../../../generated/api-clients/attendance'
import { useTranslation } from '../../../state/i18n'

const upsertDailyExternalRealtimeAttendancesResult = wrapResult(
  upsertDailyExternalRealtimeAttendances
)

type ExternalPersonModalProps = {
  onClose: () => void
  onSave: () => void
  unitId: UUID
  groups: DaycareGroup[]
  defaultGroupId: UUID
}

const externalPersonForm = transformed(
  object({
    unitId: value<string>(),
    date: validated(required(localDate()), (value) =>
      value.isAfter(LocalDate.todayInHelsinkiTz()) ? 'dateTooLate' : undefined
    ),
    arrivalTime: required(localTime()),
    departureTime: localTime(),
    name: validated(string(), (value) =>
      value.length < 3 ? 'required' : undefined
    ),
    group: required(oneOf<UUID>()),
    hasStaffOccupancyEffect: required(boolean())
  }),
  ({
    unitId,
    date,
    arrivalTime,
    departureTime,
    name,
    group,
    hasStaffOccupancyEffect
  }) => {
    const arrived = HelsinkiDateTime.fromLocal(date, arrivalTime)
    const departed = departureTime
      ? HelsinkiDateTime.fromLocal(date, departureTime)
      : null
    if (departed && !departed.isAfter(arrived)) {
      return ValidationError.of('timeRangeNotLinear')
    }
    if (arrived.isAfter(HelsinkiDateTime.now())) {
      return ValidationError.field('arrivalTime', 'futureTime')
    }
    if (departed && departed.isAfter(HelsinkiDateTime.now())) {
      return ValidationError.field('departureTime', 'futureTime')
    }
    const requestBody: ExternalAttendanceBody = {
      date,
      name,
      unitId,
      entries: [
        {
          id: null,
          arrived,
          departed,
          groupId: group,
          hasStaffOccupancyEffect: hasStaffOccupancyEffect
        }
      ]
    }
    return ValidationSuccess.of(requestBody)
  }
)

function initialFormState(
  unitId: UUID,
  groups: DaycareGroup[],
  defaultGroupId: UUID
): StateOf<typeof externalPersonForm> {
  const groupOptions = groups.filter(isGroupOpen).map((group) => ({
    value: group.id,
    label: group.name,
    domValue: group.id
  }))

  return {
    unitId,
    date: localDate.fromDate(LocalDate.todayInHelsinkiTz()),
    arrivalTime: LocalTime.nowInHelsinkiTz().format(),
    departureTime: '',
    name: '',
    group: {
      options: groupOptions,
      domValue: defaultGroupId
    },
    hasStaffOccupancyEffect: true
  }
}

export default React.memo(function StaffAttendanceExternalPersonModal({
  onClose,
  onSave,
  unitId,
  groups,
  defaultGroupId
}: ExternalPersonModalProps) {
  const { i18n, lang } = useTranslation()

  const form = useForm(
    externalPersonForm,
    () => initialFormState(unitId, groups, defaultGroupId),
    i18n.validationErrors
  )

  const submit = useCallback(() => {
    if (!form.isValid()) {
      return Promise.resolve(Failure.of({ message: 'Form not valid' }))
    }

    return upsertDailyExternalRealtimeAttendancesResult({ body: form.value() })
  }, [form])

  const date = useFormField(form, 'date')
  const arrivalTime = useFormField(form, 'arrivalTime')
  const departureTime = useFormField(form, 'departureTime')
  const name = useFormField(form, 'name')
  const group = useFormField(form, 'group')
  const groupError = group.validationError()
  const hasStaffOccupancyEffect = useFormField(form, 'hasStaffOccupancyEffect')

  return (
    <PlainModal margin="auto" data-qa="staff-attendance-add-person-modal">
      <Content>
        <Centered>
          <IconWrapper>
            <FontAwesomeIcon icon={faPlus} />
          </IconWrapper>
          <H1 noMargin>{i18n.unit.staffAttendance.addPerson}</H1>
          {i18n.unit.staffAttendance.addPersonModal.description}
        </Centered>

        <div>
          <FieldLabel>
            {i18n.unit.staffAttendance.addPersonModal.arrival}
          </FieldLabel>
          <FixedSpaceRow>
            <DatePickerF
              bind={date}
              locale={lang}
              data-qa="add-person-arrival-date-picker"
            />
            <TimeInputF
              bind={arrivalTime}
              data-qa="add-person-arrival-time-input"
            />
            {' – '}
            <TimeInputF
              bind={departureTime}
              data-qa="add-person-departure-time-input"
            />
          </FixedSpaceRow>
        </div>
        <div>
          <FieldLabel>
            {i18n.unit.staffAttendance.addPersonModal.name}
          </FieldLabel>
          <InputFieldF
            bind={name}
            placeholder={
              i18n.unit.staffAttendance.addPersonModal.namePlaceholder
            }
            data-qa="add-person-name-input"
          />
        </div>

        <div>
          <FieldLabel>
            {i18n.unit.staffAttendance.addPersonModal.group}
          </FieldLabel>
          <SelectF
            bind={group}
            placeholder={i18n.common.select}
            data-qa="add-person-group-select"
          />
          {groupError && (
            <InputFieldUnderRow className={classNames('warning')}>
              <span>{group.inputInfo()?.text}</span>
              <StatusIcon status="warning" />
            </InputFieldUnderRow>
          )}
        </div>

        <div>
          <CheckboxF
            label={i18n.unit.staffOccupancies.title}
            bind={hasStaffOccupancyEffect}
            data-qa="has-staff-occupancy-effect"
          />
        </div>

        <Gap size="xs" />
        <FixedSpaceRow justifyContent="space-between">
          <Button
            appearance="inline"
            text={i18n.common.cancel}
            data-qa="add-person-cancel-btn"
            onClick={onClose}
          />
          <InlineAsyncButton
            text={i18n.unit.staffAttendance.addPerson}
            data-qa="add-person-save-btn"
            disabled={!form.isValid()}
            onClick={submit}
            onSuccess={onSave}
          />
        </FixedSpaceRow>
      </Content>
    </PlainModal>
  )
})

function isGroupOpen(group: DaycareGroup) {
  return new DateRange(group.startDate, group.endDate).includes(
    LocalDate.todayInHelsinkiTz()
  )
}

const Content = styled.div`
  padding: ${defaultMargins.XL};
  display: flex;
  flex-direction: column;
  gap: ${defaultMargins.s};
`
const Centered = styled(FixedSpaceColumn)`
  align-self: center;
  text-align: center;
  gap: ${defaultMargins.L};
`
const IconWrapper = styled.div`
  align-self: center;
  height: 64px !important;
  width: 64px !important;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 0;

  font-size: 40px;
  color: ${(p) => p.theme.colors.grayscale.g0};
  font-weight: ${fontWeights.normal};
  background: ${(p) => p.theme.colors.main.m2};
  border-radius: 100%;
`
const FieldLabel = styled(Label)`
  margin-bottom: 8px;
`
