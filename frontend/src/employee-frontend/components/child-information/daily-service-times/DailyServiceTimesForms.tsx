// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import mapValues from 'lodash/mapValues'
import pick from 'lodash/pick'
import React, { useCallback, useMemo, useState } from 'react'

import {
  createChildDailyServiceTimes,
  setChildDailyServiceTimesEndDate,
  updateChildDailyServiceTimes
} from 'employee-frontend/api/child/daily-service-times'
import { useTranslation } from 'employee-frontend/state/i18n'
import { Failure } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import { ErrorKey, required, time, validate } from 'lib-common/form-validation'
import {
  DailyServiceTimesType,
  DailyServiceTimesValue
} from 'lib-common/generated/api-types/dailyservicetimes'
import { TimeRange } from 'lib-common/generated/api-types/shared'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'
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
import { Label, LabelLike } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { DailyServiceTimesReadOnly } from './DailyServiceTimesRow'

interface FormState {
  startDate: LocalDate | null
  endDate: LocalDate | null
  type?: DailyServiceTimesType
  regularTimes: JsonOf<TimeRange>
  monday: JsonOf<TimeRange>
  tuesday: JsonOf<TimeRange>
  wednesday: JsonOf<TimeRange>
  thursday: JsonOf<TimeRange>
  friday: JsonOf<TimeRange>
  saturday: JsonOf<TimeRange>
  sunday: JsonOf<TimeRange>
}

type ValidationResult =
  | { type: 'valid'; data: DailyServiceTimesValue }
  | { type: 'error'; errors: ValidationErrors }

interface ValidationErrors {
  type?: ErrorKey
  startDate?: ErrorKey
  endDate?: ErrorKey
  regularTimes?: RangeValidationResult
  monday?: RangeValidationResult
  tuesday?: RangeValidationResult
  wednesday?: RangeValidationResult
  thursday?: RangeValidationResult
  friday?: RangeValidationResult
  saturday?: RangeValidationResult
  sunday?: RangeValidationResult
}

export type RangeErrorKey = ErrorKey | 'timeRangeNotLinear'

export interface RangeValidationResult {
  start?: RangeErrorKey
  end?: RangeErrorKey
}

function validateFormData(formData: FormState): ValidationResult {
  const commonFieldsResult = {
    type: validate(formData.type, required),
    startDate: validate(formData.startDate, required)
  }

  let validationErrors: ValidationErrors
  if (formData.type === 'REGULAR') {
    validationErrors = {
      ...commonFieldsResult,
      regularTimes: validateTimeRange(formData.regularTimes, true)
    }
  } else if (formData.type === 'IRREGULAR') {
    validationErrors = {
      ...commonFieldsResult,
      monday: validateTimeRange(formData.monday),
      tuesday: validateTimeRange(formData.tuesday),
      wednesday: validateTimeRange(formData.wednesday),
      thursday: validateTimeRange(formData.thursday),
      friday: validateTimeRange(formData.friday),
      saturday: validateTimeRange(formData.saturday),
      sunday: validateTimeRange(formData.sunday)
    }
  } else {
    validationErrors = commonFieldsResult
  }

  const hasValidationErrors = Object.values(validationErrors).some(
    (value: ErrorKey | RangeValidationResult | undefined) =>
      typeof value !== 'undefined' &&
      (typeof value === 'string' ||
        typeof value.end !== 'undefined' ||
        typeof value.start !== 'undefined')
  )

  if (
    !hasValidationErrors &&
    formData.startDate !== null &&
    formData.type !== undefined
  ) {
    const validityPeriod = new DateRange(formData.startDate, formData.endDate)
    switch (formData.type) {
      case 'REGULAR':
        return {
          type: 'valid',
          data: {
            validityPeriod,
            type: 'REGULAR',
            regularTimes: parseTimeRange(formData.regularTimes)
          }
        }
      case 'IRREGULAR':
        return {
          type: 'valid',
          data: {
            validityPeriod,
            type: 'IRREGULAR',
            ...mapValues(pick(formData, weekdays), (tr) =>
              !tr.start || !tr.end ? null : parseTimeRange(tr)
            )
          }
        }
      case 'VARIABLE_TIME':
        return {
          type: 'valid',
          data: {
            validityPeriod,
            type: 'VARIABLE_TIME'
          }
        }
    }
  } else {
    return { type: 'error', errors: validationErrors }
  }
}

function parseTimeRange(range: JsonOf<TimeRange>): TimeRange {
  return {
    start: LocalTime.parse(range.start),
    end: LocalTime.parse(range.end)
  }
}

function formatTimeRange(range: TimeRange): JsonOf<TimeRange> {
  return {
    start: range.start.format(),
    end: range.end.format()
  }
}

function validateTimeRange(
  { start, end }: Partial<JsonOf<TimeRange>>,
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

  if (start && end && !errors.start && !errors.end) {
    const s = LocalTime.tryParse(start)
    const e = LocalTime.tryParse(end)
    if (s !== undefined && e !== undefined && s.isAfter(e)) {
      errors.start = 'timeRangeNotLinear'
      errors.end = 'timeRangeNotLinear'
    }
  }

  return errors
}

const emptyTimeRange: JsonOf<TimeRange> = {
  start: '',
  end: ''
}

export interface CreateProps {
  onClose: (shouldRefresh: boolean) => void
  childId: UUID
  hasActiveOrUpcomingServiceTimes: boolean
}

export const DailyServiceTimesCreationForm = React.memo(
  function DailyServiceTimesCreationForm({
    onClose,
    childId,
    hasActiveOrUpcomingServiceTimes
  }: CreateProps) {
    const { i18n, lang } = useTranslation()

    const [formData, setFormData] = useState<FormState>({
      startDate: LocalDate.todayInHelsinkiTz().addDays(1),
      endDate: null,
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
      <K extends keyof FormState>(
        fieldName: K,
        value: FormState[K] | undefined
      ) =>
        setFormData({
          ...formData,
          [fieldName]: value
        }),
      [formData]
    )

    const validationResult = useMemo(
      () => validateFormData(formData),
      [formData]
    )

    const sendCreationRequest = useCallback(() => {
      if (validationResult.type !== 'valid') {
        return Promise.resolve(
          Failure.of({
            message: 'Invalid form data'
          })
        )
      }

      return createChildDailyServiceTimes(childId, validationResult.data)
    }, [childId, validationResult])

    return (
      <form>
        <div>
          <Label>{i18n.childInformation.dailyServiceTimes.validFrom}</Label>
        </div>
        <div>
          <DatePicker
            date={formData.startDate}
            onChange={(startDate) => updateField('startDate', startDate)}
            locale={lang}
            minDate={LocalDate.todayInHelsinkiTz().addDays(1)}
            hideErrorsBeforeTouched
            data-qa="daily-service-times-validity-period-start"
          />
        </div>
        {hasActiveOrUpcomingServiceTimes && (
          <AlertBox
            message={
              i18n.childInformation.dailyServiceTimes.preferExtensionWarning
            }
          />
        )}
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

export interface EditProps {
  onClose: (shouldRefresh: boolean) => void
  id: UUID
  initialData: DailyServiceTimesValue
}

export const DailyServiceTimesEditForm = React.memo(
  function DailyServiceTimesEditForm({ onClose, id, initialData }: EditProps) {
    const hasStarted = !initialData.validityPeriod.start.isAfter(
      LocalDate.todayInHelsinkiTz()
    )
    if (hasStarted) {
      return (
        <DailyServiceTimesEditEndForm
          onClose={onClose}
          id={id}
          initialData={initialData}
        />
      )
    } else {
      return (
        <DailyServiceTimesEditFullForm
          onClose={onClose}
          id={id}
          initialData={initialData}
        />
      )
    }
  }
)

const DailyServiceTimesEditFullForm = React.memo(
  function DailyServiceTimesEditFullForm({
    onClose,
    id,
    initialData
  }: EditProps) {
    const { i18n, lang } = useTranslation()

    const [formData, setFormData] = useState<FormState>({
      startDate: initialData.validityPeriod.start,
      endDate: initialData.validityPeriod.end,
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
        ? { regularTimes: formatTimeRange(initialData.regularTimes) }
        : {}),
      ...(initialData.type === 'IRREGULAR'
        ? {
            monday: initialData.monday
              ? formatTimeRange(initialData.monday)
              : emptyTimeRange,
            tuesday: initialData.tuesday
              ? formatTimeRange(initialData.tuesday)
              : emptyTimeRange,
            wednesday: initialData.wednesday
              ? formatTimeRange(initialData.wednesday)
              : emptyTimeRange,
            thursday: initialData.thursday
              ? formatTimeRange(initialData.thursday)
              : emptyTimeRange,
            friday: initialData.friday
              ? formatTimeRange(initialData.friday)
              : emptyTimeRange,
            saturday: initialData.saturday
              ? formatTimeRange(initialData.saturday)
              : emptyTimeRange,
            sunday: initialData.sunday
              ? formatTimeRange(initialData.sunday)
              : emptyTimeRange
          }
        : {})
    })

    const validationResult = useMemo(
      () => validateFormData(formData),
      [formData]
    )

    const updateField = useCallback(
      <K extends keyof FormState>(
        fieldName: K,
        value: FormState[K] | undefined
      ) =>
        setFormData({
          ...formData,
          [fieldName]: value
        }),
      [formData]
    )

    const sendModificationRequest = useCallback(() => {
      if (validationResult.type !== 'valid') {
        return Promise.resolve(
          Failure.of({
            message: 'Invalid form data'
          })
        )
      }

      return updateChildDailyServiceTimes(id, validationResult.data)
    }, [id, validationResult])

    return (
      <form>
        <div>
          <Label>
            {i18n.childInformation.dailyServiceTimes.validityPeriod}
          </Label>
        </div>
        <div>
          <DatePicker
            date={formData.startDate}
            onChange={(startDate) => updateField('startDate', startDate)}
            locale={lang}
            minDate={LocalDate.todayInHelsinkiTz().addDays(1)}
            maxDate={formData.endDate ?? undefined}
            hideErrorsBeforeTouched
            data-qa="daily-service-times-validity-period-start"
          />
          {' - '}
          <DatePicker
            date={formData.endDate}
            onChange={(endDate) => updateField('endDate', endDate)}
            locale={lang}
            minDate={formData.startDate ?? undefined}
            hideErrorsBeforeTouched
            data-qa="daily-service-times-validity-period-end"
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

const DailyServiceTimesEditEndForm = React.memo(
  function DailyServiceTimesEditEndForm({
    id,
    onClose,
    initialData
  }: EditProps) {
    const { i18n, lang } = useTranslation()

    const [endDate, setEndDate] = useState<LocalDate | null>(
      initialData.validityPeriod.end
    )

    const save = useCallback(
      async () => setChildDailyServiceTimesEndDate(id, { endDate }),
      [id, endDate]
    )

    return (
      <form>
        <div>
          <Label>{i18n.childInformation.dailyServiceTimes.validUntil}</Label>
        </div>
        <div>
          <DatePicker
            date={endDate}
            onChange={setEndDate}
            locale={lang}
            minDate={LocalDate.todayInHelsinkiTz().addDays(1)}
            hideErrorsBeforeTouched
            data-qa="daily-service-times-validity-period-end"
          />
        </div>
        <Gap size="m" />
        <LabelLike>
          {i18n.childInformation.dailyServiceTimes.types[initialData.type]}
        </LabelLike>
        <DailyServiceTimesReadOnly times={initialData} />
        <FixedSpaceRow justifyContent="flex-end">
          <FixedSpaceRow spacing="s">
            <Button text={i18n.common.cancel} onClick={() => onClose(false)} />
            <AsyncButton
              text={i18n.common.confirm}
              primary
              onClick={save}
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
  value: JsonOf<TimeRange>
  onChange: (value: JsonOf<TimeRange>) => void
  error: RangeValidationResult | undefined
  dataQaPrefix: string
  hideErrorsBeforeTouched?: boolean
}

export const TimeRangeInput = React.memo(function TimeRangeInput({
  value,
  onChange,
  error,
  dataQaPrefix,
  hideErrorsBeforeTouched = true
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
        hideErrorsBeforeTouched={hideErrorsBeforeTouched}
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
        hideErrorsBeforeTouched={hideErrorsBeforeTouched}
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
  formData: FormState
  onChange: (value: Partial<FormState>) => void
  validationResult: ValidationResult
}) {
  const { i18n } = useTranslation()

  const fieldError = useCallback(
    <K extends keyof ValidationErrors>(key: K): ValidationErrors[K] =>
      validationResult.type === 'error'
        ? validationResult.errors[key]
        : undefined,
    [validationResult]
  )

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
              error={fieldError('regularTimes')}
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
                      error={fieldError(wd)}
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
