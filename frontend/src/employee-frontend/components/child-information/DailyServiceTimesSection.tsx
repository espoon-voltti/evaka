// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useMemo, useState } from 'react'
import { useEffect } from 'react'
import styled from 'styled-components'

import { Result } from 'lib-common/api'
import {
  isRegular,
  isIrregular,
  RegularDailyServiceTimes,
  IrregularDailyServiceTimes,
  TimeRange,
  isVariableTime,
  VariableDailyServiceTimes
} from 'lib-common/api-types/child/common'
import { DailyServiceTimesType } from 'lib-common/generated/enums'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import Radio from 'lib-components/atoms/form/Radio'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H2, Label, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faPen } from 'lib-icons'

import {
  deleteChildDailyServiceTimes,
  getChildDailyServiceTimes,
  putChildDailyServiceTimes
} from '../../api/child/daily-service-times'
import { ChildContext, ChildState } from '../../state/child'
import { Translations, useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { NullableValues } from '../../types'
import { renderResult } from '../async-rendering'

const weekdays = [
  'monday',
  'tuesday',
  'wednesday',
  'thursday',
  'friday',
  'saturday',
  'sunday'
] as const

type Weekday = typeof weekdays[number]

interface TimeInputRange {
  start: string
  end: string
}

interface SelectableTimeInputRange extends TimeInputRange {
  selected: boolean
}

interface FormData extends Record<Weekday, SelectableTimeInputRange> {
  type: DailyServiceTimesType | 'NOT_SET'
  regular: TimeInputRange
  variableTimes: boolean
}

const emptyRange = {
  selected: false,
  start: '',
  end: ''
}

const emptyForm: FormData = {
  type: 'NOT_SET',
  regular: { start: '', end: '' },
  monday: emptyRange,
  tuesday: emptyRange,
  wednesday: emptyRange,
  thursday: emptyRange,
  friday: emptyRange,
  saturday: emptyRange,
  sunday: emptyRange,
  variableTimes: false
}

interface ValidationResult {
  regular: NullableValues<TimeInputRange>
  irregular: true | null
  monday: NullableValues<TimeInputRange>
  tuesday: NullableValues<TimeInputRange>
  wednesday: NullableValues<TimeInputRange>
  thursday: NullableValues<TimeInputRange>
  friday: NullableValues<TimeInputRange>
  saturday: NullableValues<TimeInputRange>
  sunday: NullableValues<TimeInputRange>
  variableTimes: boolean
}

function required(i18n: Translations, v: string): string | null {
  return v.trim().length === 0
    ? i18n.childInformation.dailyServiceTimes.errors.required
    : null
}

function validateWeekday(
  i18n: Translations,
  data: FormData,
  day: Weekday
): NullableValues<TimeInputRange> {
  return {
    start:
      data.type === 'IRREGULAR' && data[day].selected
        ? required(i18n, data[day].start)
        : null,
    end:
      data.type === 'IRREGULAR' && data[day].selected
        ? required(i18n, data[day].end)
        : null
  }
}

function validate(
  i18n: Translations,
  formData: FormData | null
): ValidationResult | null {
  if (formData === null) {
    return null
  } else {
    return {
      regular: {
        start:
          formData.type === 'REGULAR'
            ? required(i18n, formData.regular.start)
            : null,
        end:
          formData.type === 'REGULAR'
            ? required(i18n, formData.regular.end)
            : null
      },
      irregular:
        formData.type === 'IRREGULAR' &&
        !weekdays.some((day) => formData[day].selected)
          ? true
          : null,
      monday: validateWeekday(i18n, formData, 'monday'),
      tuesday: validateWeekday(i18n, formData, 'tuesday'),
      wednesday: validateWeekday(i18n, formData, 'wednesday'),
      thursday: validateWeekday(i18n, formData, 'thursday'),
      friday: validateWeekday(i18n, formData, 'friday'),
      saturday: validateWeekday(i18n, formData, 'saturday'),
      sunday: validateWeekday(i18n, formData, 'sunday'),
      variableTimes: formData.type === 'VARIABLE_TIME'
    }
  }
}

function saveFormData(id: UUID, formData: FormData): Promise<Result<void>> {
  switch (formData.type) {
    case 'NOT_SET':
      return deleteChildDailyServiceTimes(id)
    case 'REGULAR': {
      const data: RegularDailyServiceTimes = {
        type: 'REGULAR',
        regularTimes: {
          start: formData.regular.start,
          end: formData.regular.end
        }
      }
      return putChildDailyServiceTimes(id, data)
    }
    case 'IRREGULAR': {
      const data: IrregularDailyServiceTimes = {
        type: 'IRREGULAR',
        monday: formData.monday.selected
          ? {
              start: formData.monday.start,
              end: formData.monday.end
            }
          : null,
        tuesday: formData.tuesday.selected
          ? {
              start: formData.tuesday.start,
              end: formData.tuesday.end
            }
          : null,
        wednesday: formData.wednesday.selected
          ? {
              start: formData.wednesday.start,
              end: formData.wednesday.end
            }
          : null,
        thursday: formData.thursday.selected
          ? {
              start: formData.thursday.start,
              end: formData.thursday.end
            }
          : null,
        friday: formData.friday.selected
          ? {
              start: formData.friday.start,
              end: formData.friday.end
            }
          : null,
        saturday: formData.saturday.selected
          ? {
              start: formData.saturday.start,
              end: formData.saturday.end
            }
          : null,
        sunday: formData.sunday.selected
          ? {
              start: formData.sunday.start,
              end: formData.sunday.end
            }
          : null
      }
      return putChildDailyServiceTimes(id, data)
    }
    case 'VARIABLE_TIME': {
      const data: VariableDailyServiceTimes = {
        type: 'VARIABLE_TIME'
      }
      return putChildDailyServiceTimes(id, data)
    }
  }
}

interface Props {
  id: UUID
  startOpen: boolean
}

const DailyServiceTimesSection = React.memo(function DailyServiceTimesSection({
  id,
  startOpen
}: Props) {
  const { i18n } = useTranslation()
  const { setErrorMessage } = useContext(UIContext)
  const { permittedActions } = useContext<ChildState>(ChildContext)

  const [open, setOpen] = useState(startOpen)

  const [apiData, loadData] = useApiState(
    () => getChildDailyServiceTimes(id),
    [id]
  )
  const [formData, setFormData] = useState<FormData | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [validationResult, setValidationResult] =
    useState<ValidationResult | null>(null)

  const startEditing = useCallback(() => {
    if (apiData.isSuccess) {
      if (apiData.value === null) {
        setFormData(emptyForm)
      } else if (isRegular(apiData.value)) {
        setFormData({
          ...emptyForm,
          type: 'REGULAR',
          regular: {
            start: apiData.value.regularTimes.start,
            end: apiData.value.regularTimes.end
          }
        })
      } else if (isIrregular(apiData.value)) {
        setFormData({
          ...emptyForm,
          type: 'IRREGULAR',
          monday: {
            selected: apiData.value.monday !== null,
            start: apiData.value.monday?.start ?? '',
            end: apiData.value.monday?.end ?? ''
          },
          tuesday: {
            selected: apiData.value.tuesday !== null,
            start: apiData.value.tuesday?.start ?? '',
            end: apiData.value.tuesday?.end ?? ''
          },
          wednesday: {
            selected: apiData.value.wednesday !== null,
            start: apiData.value.wednesday?.start ?? '',
            end: apiData.value.wednesday?.end ?? ''
          },
          thursday: {
            selected: apiData.value.thursday !== null,
            start: apiData.value.thursday?.start ?? '',
            end: apiData.value.thursday?.end ?? ''
          },
          friday: {
            selected: apiData.value.friday !== null,
            start: apiData.value.friday?.start ?? '',
            end: apiData.value.friday?.end ?? ''
          },
          saturday: {
            selected: apiData.value.saturday !== null,
            start: apiData.value.saturday?.start ?? '',
            end: apiData.value.saturday?.end ?? ''
          },
          sunday: {
            selected: apiData.value.sunday !== null,
            start: apiData.value.sunday?.start ?? '',
            end: apiData.value.sunday?.end ?? ''
          }
        })
      } else if (isVariableTime(apiData.value)) {
        setFormData({
          ...emptyForm,
          type: 'VARIABLE_TIME'
        })
      }
    } else {
      setFormData(null)
    }
  }, [apiData])

  const formIsValid = useMemo(
    () =>
      validationResult !== null &&
      validationResult.irregular === null &&
      Object.values(validationResult.regular).find((v) => v !== null) ===
        undefined &&
      Object.values(validationResult.monday).find((v) => v !== null) ===
        undefined &&
      Object.values(validationResult.tuesday).find((v) => v !== null) ===
        undefined &&
      Object.values(validationResult.wednesday).find((v) => v !== null) ===
        undefined &&
      Object.values(validationResult.thursday).find((v) => v !== null) ===
        undefined &&
      Object.values(validationResult.friday).find((v) => v !== null) ===
        undefined,
    [validationResult]
  )

  useEffect(() => {
    setValidationResult(validate(i18n, formData))
  }, [i18n, formData])

  const onSubmit = useCallback(async () => {
    if (!formIsValid || !formData) return
    setSubmitting(true)
    return saveFormData(id, formData)
  }, [formData, formIsValid, id])

  const onSuccess = useCallback(() => {
    setSubmitting(false)
    setFormData(null)
    loadData()
  }, [loadData])

  const onFailure = useCallback(() => {
    setSubmitting(false)
    setErrorMessage({
      type: 'error',
      title: i18n.common.error.unknown,
      text: i18n.common.error.saveFailed,
      resolveLabel: i18n.common.ok
    })
  }, [i18n, setErrorMessage])

  const setType = (type: 'NOT_SET' | DailyServiceTimesType) => () =>
    setFormData((old) => (old !== null ? { ...old, type } : null))

  const setTimes = (wd: 'regular' | Weekday) => (value: TimeRange) =>
    setFormData((old) => (old !== null ? { ...old, [wd]: value } : null))

  const setWeekdaySelected = (wd: Weekday) => (value: boolean) =>
    setFormData((old) =>
      old !== null ? { ...old, [wd]: { ...old[wd], selected: value } } : null
    )

  return (
    <CollapsibleContentArea
      title={<H2 noMargin>{i18n.childInformation.dailyServiceTimes.title}</H2>}
      open={open}
      toggleOpen={() => setOpen(!open)}
      opaque
      paddingVertical="L"
      data-qa="child-daily-service-times-collapsible"
    >
      <P>
        {i18n.childInformation.dailyServiceTimes.info}
        <br />
        {i18n.childInformation.dailyServiceTimes.info2}
      </P>

      {renderResult(apiData, (apiData) => (
        <>
          {!formData && (
            <>
              {permittedActions.has('UPDATE_DAILY_SERVICE_TIMES') && (
                <>
                  <RightAlign>
                    <InlineButton
                      text={i18n.common.edit}
                      icon={faPen}
                      onClick={startEditing}
                      data-qa="edit-button"
                    />
                  </RightAlign>

                  <Gap size="s" />
                </>
              )}

              {apiData === null ? (
                <FixedWidthLabel data-qa="times-type">
                  {i18n.childInformation.dailyServiceTimes.types.notSet}
                </FixedWidthLabel>
              ) : (
                <>
                  {isRegular(apiData) && (
                    <FixedSpaceRow>
                      <FixedWidthLabel data-qa="times-type">
                        {i18n.childInformation.dailyServiceTimes.types.regular}
                      </FixedWidthLabel>
                      <div data-qa="times">
                        {i18n.childInformation.dailyServiceTimes.weekdays.monday.toLowerCase()}
                        -
                        {i18n.childInformation.dailyServiceTimes.weekdays.friday.toLowerCase()}{' '}
                        {apiData.regularTimes.start}–{apiData.regularTimes.end}
                      </div>
                    </FixedSpaceRow>
                  )}
                  {isIrregular(apiData) && (
                    <FixedSpaceRow>
                      <FixedWidthLabel data-qa="times-type">
                        {
                          i18n.childInformation.dailyServiceTimes.types
                            .irregular
                        }
                      </FixedWidthLabel>
                      <div data-qa="times">
                        {weekdays
                          .map((wd) =>
                            apiData && isIrregular(apiData) && apiData[wd]
                              ? `${i18n.childInformation.dailyServiceTimes.weekdays[
                                  wd
                                ].toLowerCase()} ${apiData[wd]?.start ?? ''}-${
                                  apiData[wd]?.end ?? ''
                                }`
                              : null
                          )
                          .filter((s) => s !== null)
                          .join(', ')}
                      </div>
                    </FixedSpaceRow>
                  )}
                  {isVariableTime(apiData) && (
                    <FixedWidthLabel data-qa="times-type">
                      {
                        i18n.childInformation.dailyServiceTimes.types
                          .variableTimes
                      }
                    </FixedWidthLabel>
                  )}
                </>
              )}
            </>
          )}

          {formData && (
            <>
              <div>
                <FixedSpaceColumn>
                  <Radio
                    label={i18n.childInformation.dailyServiceTimes.types.notSet}
                    checked={formData.type === 'NOT_SET'}
                    onChange={setType('NOT_SET')}
                    data-qa="radio-not-set"
                  />
                  <Radio
                    label={
                      i18n.childInformation.dailyServiceTimes.types.regular
                    }
                    checked={formData.type === 'REGULAR'}
                    onChange={setType('REGULAR')}
                    data-qa="radio-regular"
                  />
                  {formData.type === 'REGULAR' && (
                    <FixedSpaceRow style={{ marginLeft: defaultMargins.XXL }}>
                      <span>
                        {i18n.childInformation.dailyServiceTimes.weekdays.monday.toLowerCase()}
                        -
                        {i18n.childInformation.dailyServiceTimes.weekdays.friday.toLowerCase()}
                      </span>
                      <FixedSpaceRow>
                        <TimeRangeInput
                          value={formData.regular}
                          onChange={setTimes('regular')}
                          error={validationResult?.regular}
                          dataQaPrefix="regular"
                        />
                      </FixedSpaceRow>
                    </FixedSpaceRow>
                  )}
                  <Radio
                    label={
                      i18n.childInformation.dailyServiceTimes.types.irregular
                    }
                    checked={formData.type === 'IRREGULAR'}
                    onChange={setType('IRREGULAR')}
                    data-qa="radio-irregular"
                  />
                  {formData.type === 'IRREGULAR' && (
                    <FixedSpaceColumn>
                      {weekdays.map((wd) => (
                        <FixedSpaceRow
                          key={wd}
                          style={{ marginLeft: defaultMargins.XXL }}
                        >
                          <div style={{ width: '140px' }}>
                            <Checkbox
                              label={
                                i18n.childInformation.dailyServiceTimes
                                  .weekdays[wd]
                              }
                              checked={formData[wd].selected}
                              onChange={setWeekdaySelected(wd)}
                              data-qa={`${wd}-checkbox`}
                            />
                          </div>
                          <FixedSpaceRow
                            style={
                              !formData[wd].selected
                                ? { display: 'none' }
                                : undefined
                            }
                          >
                            <TimeRangeInput
                              value={formData[wd]}
                              onChange={setTimes(wd)}
                              error={
                                validationResult
                                  ? validationResult[wd]
                                  : undefined
                              }
                              dataQaPrefix={wd}
                            />
                          </FixedSpaceRow>
                        </FixedSpaceRow>
                      ))}
                    </FixedSpaceColumn>
                  )}
                  <Radio
                    label={
                      i18n.childInformation.dailyServiceTimes.types
                        .variableTimes
                    }
                    checked={formData.type === 'VARIABLE_TIME'}
                    onChange={setType('VARIABLE_TIME')}
                    data-qa="radio-variable-time"
                  />
                </FixedSpaceColumn>
              </div>

              <Gap size="s" />

              <RightAlign>
                <FixedSpaceRow>
                  <Button
                    text={i18n.common.cancel}
                    onClick={() => setFormData(null)}
                    disabled={submitting}
                  />
                  <AsyncButton
                    text={i18n.common.confirm}
                    onClick={onSubmit}
                    onSuccess={onSuccess}
                    onFailure={onFailure}
                    disabled={!formIsValid}
                    primary
                    data-qa="submit-button"
                  />
                </FixedSpaceRow>
              </RightAlign>
            </>
          )}
        </>
      ))}
    </CollapsibleContentArea>
  )
})

interface TimeRangeInputProps {
  value: TimeRange
  onChange: (value: TimeRange) => void
  error: NullableValues<TimeRange> | undefined
  dataQaPrefix: string
}

const TimeRangeInput = React.memo(function TimeRangeInput({
  value,
  onChange,
  error,
  dataQaPrefix
}: TimeRangeInputProps) {
  return (
    <>
      <TimeInput
        value={value.start}
        onChange={(start) => onChange({ ...value, start })}
        required
        data-qa={`${dataQaPrefix}-start`}
        info={
          error && error.start
            ? {
                status: 'warning',
                text: error.start
              }
            : undefined
        }
        hideErrorsBeforeTouched
      />
      <span> - </span>
      <TimeInput
        value={value.end}
        onChange={(end) => onChange({ ...value, end })}
        required
        data-qa={`${dataQaPrefix}-end`}
        info={
          error && error.end
            ? {
                status: 'warning',
                text: error.end
              }
            : undefined
        }
        hideErrorsBeforeTouched
      />
    </>
  )
})

const RightAlign = styled.div`
  display: flex;
  justify-content: flex-end;
`

const FixedWidthLabel = styled(Label)`
  width: 220px;
`

export default DailyServiceTimesSection
