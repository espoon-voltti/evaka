// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import mapValues from 'lodash/mapValues'
import pick from 'lodash/pick'
import React, { useCallback, useMemo, useState } from 'react'

import {
  createChildDailyServiceTimes,
  putChildDailyServiceTimes
} from 'employee-frontend/api/child/daily-service-times'
import { useTranslation } from 'employee-frontend/state/i18n'
import { Failure } from 'lib-common/api'
import { DailyServiceTimes, TimeRange } from 'lib-common/api-types/child/common'
import DateRange from 'lib-common/date-range'
import { ErrorKey, required, time, validate } from 'lib-common/form-validation'
import { DailyServiceTimesType } from 'lib-common/generated/enums'
import LocalDate from 'lib-common/local-date'
import { OmitInUnion, UUID } from 'lib-common/types'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import Radio from 'lib-components/atoms/form/Radio'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import { Table, Tbody, Td, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

interface IntervalFormData {
  type?: DailyServiceTimesType
  regularTimes: TimeRange
  monday: TimeRange
  tuesday: TimeRange
  wednesday: TimeRange
  thursday: TimeRange
  friday: TimeRange
  saturday: TimeRange
  sunday: TimeRange
}

interface CreationFormData extends IntervalFormData {
  validityPeriod: DateRange
}

type ModificationFormData = IntervalFormData

interface RangeValidationResult {
  start?: ErrorKey
  end?: ErrorKey
}

interface ValidationResult {
  type?: ErrorKey
  validityPeriod?: ErrorKey
  regularTimes?: RangeValidationResult
  monday?: RangeValidationResult
  tuesday?: RangeValidationResult
  wednesday?: RangeValidationResult
  thursday?: RangeValidationResult
  friday?: RangeValidationResult
  saturday?: RangeValidationResult
  sunday?: RangeValidationResult
}

function validateTimeRange(
  { start, end }: Partial<TimeRange>,
  required = false
): RangeValidationResult {
  if (required && !start && !end) {
    return {
      start: 'timeRequired',
      end: 'timeRequired'
    }
  }

  const errors: RangeValidationResult = {}

  if (!start && end) {
    errors.start = 'timeRequired'
  }

  if (start) {
    errors.start = errors.start ?? time(start)
  }

  if (!end && start) {
    errors.end = 'timeRequired'
  }

  if (end) {
    errors.end = errors.end ?? time(end)
  }

  return errors
}

const emptyTimeRange = {
  start: '',
  end: ''
}

function requireOrThrow<T>(value: T | undefined): T {
  if (typeof value === 'undefined') {
    throw Error('Value is undefined')
  }

  return value
}

function formDataToRequest(
  formData: IntervalFormData
): OmitInUnion<DailyServiceTimes, 'validityPeriod'> {
  const type = requireOrThrow(formData.type)

  switch (type) {
    case 'REGULAR':
      return {
        type: 'REGULAR',
        regularTimes: formData.regularTimes
      }
    case 'IRREGULAR':
      return {
        type: 'IRREGULAR',
        ...mapValues(pick(formData, weekdays), (tr) =>
          !tr.start || !tr.end ? null : tr
        )
      }
    case 'VARIABLE_TIME':
      return {
        type: 'VARIABLE_TIME'
      }
  }
}

export const DailyServiceTimesCreationForm = React.memo(
  function DailyServiceTimesCreationForm({
    onClose,
    childId
  }: {
    onClose: (shouldRefresh: boolean) => void
    childId: UUID
  }) {
    const { i18n, lang } = useTranslation()

    const [formData, setFormData] = useState<CreationFormData>({
      validityPeriod: new DateRange(LocalDate.todayInHelsinkiTz(), null),
      regularTimes: emptyTimeRange,
      monday: emptyTimeRange,
      tuesday: emptyTimeRange,
      wednesday: emptyTimeRange,
      thursday: emptyTimeRange,
      friday: emptyTimeRange,
      saturday: emptyTimeRange,
      sunday: emptyTimeRange
    })

    const updateField = useCallback(
      <K extends keyof CreationFormData>(
        fieldName: K,
        value: CreationFormData[K] | undefined
      ) =>
        setFormData({
          ...formData,
          [fieldName]: value
        }),
      [formData]
    )

    const validationResult = useMemo<ValidationResult>(() => {
      const commonFieldsResult = {
        type: validate(formData.type, required),
        validityPeriod: validate(formData.validityPeriod, required)
      }

      switch (formData.type) {
        case 'REGULAR':
          return {
            ...commonFieldsResult,
            regularTimes: validateTimeRange(formData.regularTimes, true)
          }
        case 'IRREGULAR':
          return {
            ...commonFieldsResult,
            monday: validateTimeRange(formData.monday),
            tuesday: validateTimeRange(formData.tuesday),
            wednesday: validateTimeRange(formData.wednesday),
            thursday: validateTimeRange(formData.thursday),
            friday: validateTimeRange(formData.friday),
            saturday: validateTimeRange(formData.saturday),
            sunday: validateTimeRange(formData.sunday)
          }
      }

      return commonFieldsResult
    }, [formData])

    const noValidationErrors = useMemo(
      () =>
        Object.values(validationResult).every(
          (value: ErrorKey | RangeValidationResult | undefined) =>
            typeof value === 'undefined' ||
            (typeof value !== 'string' &&
              typeof value.end === 'undefined' &&
              typeof value.start === 'undefined')
        ),
      [validationResult]
    )

    const sendCreationRequest = useCallback(() => {
      if (!noValidationErrors) {
        return Promise.resolve(
          Failure.of({
            message: 'Invalid form data'
          })
        )
      }

      return createChildDailyServiceTimes(childId, {
        ...formDataToRequest(formData),
        validityPeriod: formData.validityPeriod
      })
    }, [formData, noValidationErrors, childId])

    return (
      <form>
        <div>
          <Label>{i18n.childInformation.dailyServiceTimes.validFrom}</Label>
        </div>
        <div>
          <DatePicker
            date={formData.validityPeriod?.start ?? null}
            onChange={(periodStart) =>
              periodStart
                ? updateField(
                    'validityPeriod',
                    new DateRange(periodStart, null)
                  )
                : updateField('validityPeriod', undefined)
            }
            errorTexts={i18n.validationErrors}
            locale={lang}
            minDate={LocalDate.todayInHelsinkiTz()}
            hideErrorsBeforeTouched
            data-qa="daily-service-times-validity-period-start"
          />
        </div>
        <Gap size="m" />
        <div>
          <Label>
            {i18n.childInformation.dailyServiceTimes.dailyServiceTime}
          </Label>
        </div>
        <Gap size="s" />
        <DailyServiceTimesPicker
          formData={formData}
          onChange={(data) => setFormData({ ...formData, ...data })}
          validationResult={validationResult}
        />
        <FixedSpaceRow justifyContent="flex-end">
          <FixedSpaceRow spacing="s">
            <Button text={i18n.common.cancel} onClick={() => onClose(false)} />
            <AsyncButton
              text={i18n.common.confirm}
              primary
              onClick={sendCreationRequest}
              onSuccess={() => onClose(true)}
              data-qa="create-times-btn"
            />
          </FixedSpaceRow>
        </FixedSpaceRow>
      </form>
    )
  }
)

export const DailyServiceTimesModificationForm = React.memo(
  function DailyServiceTimesModificationForm({
    onClose,
    id,
    initialData
  }: {
    onClose: (shouldRefresh: boolean) => void
    id: UUID
    initialData: DailyServiceTimes
  }) {
    const { i18n } = useTranslation()

    const [formData, setFormData] = useState<ModificationFormData>({
      type: initialData.type,
      regularTimes: emptyTimeRange,
      monday: emptyTimeRange,
      tuesday: emptyTimeRange,
      wednesday: emptyTimeRange,
      thursday: emptyTimeRange,
      friday: emptyTimeRange,
      saturday: emptyTimeRange,
      sunday: emptyTimeRange,
      ...(initialData.type === 'REGULAR'
        ? { regularTimes: initialData.regularTimes }
        : {}),
      ...(initialData.type === 'IRREGULAR'
        ? {
            monday: initialData?.monday ?? emptyTimeRange,
            tuesday: initialData?.tuesday ?? emptyTimeRange,
            wednesday: initialData?.wednesday ?? emptyTimeRange,
            thursday: initialData?.thursday ?? emptyTimeRange,
            friday: initialData?.friday ?? emptyTimeRange,
            saturday: initialData?.saturday ?? emptyTimeRange,
            sunday: initialData?.sunday ?? emptyTimeRange
          }
        : {})
    })

    const validationResult = useMemo<ValidationResult>(() => {
      const commonFieldsResult = {
        type: validate(formData.type, required)
      }

      switch (formData.type) {
        case 'REGULAR':
          return {
            ...commonFieldsResult,
            regularTimes: validateTimeRange(formData.regularTimes, true)
          }
        case 'IRREGULAR':
          return {
            ...commonFieldsResult,
            monday: validateTimeRange(formData.monday),
            tuesday: validateTimeRange(formData.tuesday),
            wednesday: validateTimeRange(formData.wednesday),
            thursday: validateTimeRange(formData.thursday),
            friday: validateTimeRange(formData.friday),
            saturday: validateTimeRange(formData.saturday),
            sunday: validateTimeRange(formData.sunday)
          }
      }

      return commonFieldsResult
    }, [formData])

    const noValidationErrors = useMemo(
      () =>
        Object.values(validationResult).every(
          (value: ErrorKey | RangeValidationResult | undefined) =>
            typeof value === 'undefined' ||
            (typeof value !== 'string' &&
              typeof value.end === 'undefined' &&
              typeof value.start === 'undefined')
        ),
      [validationResult]
    )

    const sendModificationRequest = useCallback(() => {
      if (!noValidationErrors) {
        return Promise.resolve(
          Failure.of({
            message: 'Invalid form data'
          })
        )
      }

      return putChildDailyServiceTimes(id, {
        ...formDataToRequest(formData),
        validityPeriod: initialData.validityPeriod
      })
    }, [noValidationErrors, id, formData, initialData.validityPeriod])

    return (
      <form>
        <div>
          <Label>
            {i18n.childInformation.dailyServiceTimes.dailyServiceTime}
          </Label>
        </div>
        <Gap size="s" />
        <DailyServiceTimesPicker
          formData={formData}
          onChange={(data) => setFormData({ ...formData, ...data })}
          validationResult={validationResult}
        />
        {!initialData.validityPeriod.start.isAfter(
          LocalDate.todayInHelsinkiTz()
        ) && (
          <AlertBox
            message={
              <>
                {
                  i18n.childInformation.dailyServiceTimes
                    .retroactiveModificationWarning
                }
              </>
            }
          />
        )}
        <FixedSpaceRow justifyContent="flex-end">
          <FixedSpaceRow spacing="s">
            <Button text={i18n.common.cancel} onClick={() => onClose(false)} />
            <AsyncButton
              text={i18n.common.confirm}
              primary
              onClick={sendModificationRequest}
              onSuccess={() => onClose(true)}
              data-qa="modify-times-btn"
            />
          </FixedSpaceRow>
        </FixedSpaceRow>
      </form>
    )
  }
)

interface TimeRangeInputProps {
  value: TimeRange
  onChange: (value: TimeRange) => void
  error: RangeValidationResult | undefined
  dataQaPrefix: string
}

const TimeRangeInput = React.memo(function TimeRangeInput({
  value,
  onChange,
  error,
  dataQaPrefix
}: TimeRangeInputProps) {
  const { i18n } = useTranslation()

  return (
    <>
      <TimeInput
        value={value.start}
        onChange={(start) => onChange({ end: value.end, start })}
        required
        data-qa={`${dataQaPrefix}-start`}
        info={
          error?.start
            ? {
                status: 'warning',
                text: i18n.validationErrors[error.start]
              }
            : undefined
        }
        hideErrorsBeforeTouched
      />
      <span> – </span>
      <TimeInput
        value={value.end}
        onChange={(end) => onChange({ start: value.start, end })}
        required
        data-qa={`${dataQaPrefix}-end`}
        info={
          error?.end
            ? {
                status: 'warning',
                text: i18n.validationErrors[error.end]
              }
            : undefined
        }
        hideErrorsBeforeTouched
      />
    </>
  )
})

const weekdays = [
  'monday',
  'tuesday',
  'wednesday',
  'thursday',
  'friday',
  'saturday',
  'sunday'
] as const

const DailyServiceTimesPicker = React.memo(function DailyServiceTimesPicker({
  formData,
  onChange,
  validationResult
}: {
  formData: IntervalFormData
  onChange: (value: Partial<IntervalFormData>) => void
  validationResult: ValidationResult
}) {
  const { i18n } = useTranslation()

  return (
    <FixedSpaceColumn>
      <Radio
        label={i18n.childInformation.dailyServiceTimes.types.REGULAR}
        checked={formData.type === 'REGULAR'}
        onChange={() => onChange({ type: 'REGULAR' })}
        data-qa="radio-regular"
      />
      {formData.type === 'REGULAR' && (
        <FixedSpaceRow alignItems="center">
          <Gap horizontal size="L" />
          <span>
            {i18n.childInformation.dailyServiceTimes.weekdays.monday}–
            {i18n.childInformation.dailyServiceTimes.weekdays.friday}
          </span>
          <FixedSpaceRow>
            <TimeRangeInput
              value={formData.regularTimes}
              onChange={(range) => onChange({ regularTimes: range })}
              error={validationResult.regularTimes}
              dataQaPrefix="regular"
            />
          </FixedSpaceRow>
        </FixedSpaceRow>
      )}
      <Radio
        label={i18n.childInformation.dailyServiceTimes.types.IRREGULAR}
        checked={formData.type === 'IRREGULAR'}
        onChange={() => onChange({ type: 'IRREGULAR' })}
        data-qa="radio-irregular"
      />
      {formData.type === 'IRREGULAR' && (
        <FixedSpaceRow>
          <Gap horizontal size="L" />
          <Table>
            <Tbody>
              {weekdays.map((wd) => (
                <Tr key={wd} style={{ marginLeft: 0 /*defaultMargins.XXL*/ }}>
                  <Td
                    minimalWidth
                    borderStyle="none"
                    horizontalPadding="zero"
                    verticalAlign="middle"
                    verticalPadding="zero"
                  >
                    {i18n.childInformation.dailyServiceTimes.weekdays[wd]}
                  </Td>
                  <Td
                    borderStyle="none"
                    verticalAlign="middle"
                    verticalPadding="xxs"
                  >
                    <TimeRangeInput
                      value={formData[wd] ?? undefined}
                      onChange={(value) =>
                        onChange({
                          [wd]: value
                        })
                      }
                      error={validationResult[wd]}
                      dataQaPrefix={wd}
                    />
                  </Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        </FixedSpaceRow>
      )}
      <Radio
        label={i18n.childInformation.dailyServiceTimes.types.VARIABLE_TIME}
        checked={formData.type === 'VARIABLE_TIME'}
        onChange={() => onChange({ type: 'VARIABLE_TIME' })}
        data-qa="radio-variable_time"
      />
    </FixedSpaceColumn>
  )
})
