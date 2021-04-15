// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Loading, Result } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { Label, P } from 'lib-components/typography'
import React, { useContext, useState } from 'react'
import { useEffect } from 'react'
import { useTranslation } from '../../state/i18n'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import {
  isRegular,
  DailyServiceTimes,
  isIrregular,
  RegularDailyServiceTimes,
  IrregularDailyServiceTimes
} from 'lib-common/api-types/child/common'
import {
  deleteChildDailyServiceTimes,
  getChildDailyServiceTimes,
  putChildDailyServiceTimes
} from '../../api/child/daily-service-times'
import { faClock, faPen } from 'lib-icons'
import styled from 'styled-components'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import Button from 'lib-components/atoms/buttons/Button'
import { defaultMargins, Gap } from 'lib-components/white-space'
import Radio from 'lib-components/atoms/form/Radio'
import InputField from 'lib-components/atoms/form/InputField'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { UIContext } from '../../state/ui'
import { RequireRole } from '../../utils/roles'
import { NullableValues } from '../../types'

const weekdays = [
  'monday',
  'tuesday',
  'wednesday',
  'thursday',
  'friday'
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
  type: 'REGULAR' | 'IRREGULAR' | 'NOT_SET'
  regular: TimeInputRange
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
  friday: emptyRange
}

interface ValidationResult {
  regular: NullableValues<TimeInputRange>
  irregular: true | null
  monday: NullableValues<TimeInputRange>
  tuesday: NullableValues<TimeInputRange>
  wednesday: NullableValues<TimeInputRange>
  thursday: NullableValues<TimeInputRange>
  friday: NullableValues<TimeInputRange>
}

interface Props {
  id: UUID
  open: boolean
}

const DailyServiceTimesSection = React.memo(function DailyServiceTimesSection({
  id,
  open
}: Props) {
  const { i18n } = useTranslation()
  const { setErrorMessage } = useContext(UIContext)

  const [apiData, setApiData] = useState<Result<DailyServiceTimes | null>>(
    Loading.of()
  )
  const [formData, setFormData] = useState<FormData | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [
    validationResult,
    setValidationResult
  ] = useState<ValidationResult | null>(null)

  const loadData = useRestApi(getChildDailyServiceTimes, setApiData)
  useEffect(() => loadData(id), [id])

  const startEditing = () => {
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
          }
        })
      }
    } else {
      setFormData(null)
    }
  }

  const required = (v: string) =>
    v.trim().length === 0
      ? i18n.childInformation.dailyServiceTimes.errors.required
      : null

  const validateWeekday = (
    data: FormData,
    day: Weekday
  ): NullableValues<TimeInputRange> => ({
    start:
      data.type === 'IRREGULAR' && data[day].selected
        ? required(data[day].start)
        : null,
    end:
      data.type === 'IRREGULAR' && data[day].selected
        ? required(data[day].end)
        : null
  })

  const validate = () => {
    if (formData === null) {
      setValidationResult(null)
    } else {
      setValidationResult({
        regular: {
          start:
            formData.type === 'REGULAR'
              ? required(formData.regular.start)
              : null,
          end:
            formData.type === 'REGULAR' ? required(formData.regular.end) : null
        },
        irregular:
          formData.type === 'IRREGULAR' &&
          !weekdays.some((day) => formData[day].selected)
            ? true
            : null,
        monday: validateWeekday(formData, 'monday'),
        tuesday: validateWeekday(formData, 'tuesday'),
        wednesday: validateWeekday(formData, 'wednesday'),
        thursday: validateWeekday(formData, 'thursday'),
        friday: validateWeekday(formData, 'friday')
      })
    }
  }

  useEffect(validate, [formData])

  const onSubmit = () => {
    if (!formIsValid || !formData) return

    setSubmitting(true)

    let apiCall
    if (formData.type === 'NOT_SET') {
      apiCall = deleteChildDailyServiceTimes(id)
    } else if (formData.type === 'REGULAR') {
      const data: RegularDailyServiceTimes = {
        regular: true,
        regularTimes: {
          start: formData.regular.start,
          end: formData.regular.end
        }
      }
      apiCall = putChildDailyServiceTimes(id, data)
    } else if (formData.type === 'IRREGULAR') {
      const data: IrregularDailyServiceTimes = {
        regular: false,
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
          : null
      }
      apiCall = putChildDailyServiceTimes(id, data)
    }

    if (apiCall) {
      apiCall
        .then((res) => {
          if (res.isSuccess) {
            setFormData(null)
            loadData(id)
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
    } else {
      setSubmitting(false)
    }
  }

  const formIsValid =
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
    Object.values(validationResult.friday).find((v) => v !== null) === undefined

  return (
    <CollapsibleSection
      data-qa="child-daily-service-times-collapsible"
      icon={faClock}
      title={i18n.childInformation.dailyServiceTimes.title}
      startCollapsed={!open}
    >
      <P>
        {i18n.childInformation.dailyServiceTimes.info}
        <br />
        {i18n.childInformation.dailyServiceTimes.info2}
      </P>

      {apiData.isLoading && <SpinnerSegment />}
      {apiData.isFailure && <ErrorSegment title={i18n.common.loadingFailed} />}
      {apiData.isSuccess && (
        <>
          {!formData && (
            <>
              <RequireRole oneOf={['ADMIN', 'UNIT_SUPERVISOR']}>
                <RightAlign>
                  <InlineButton
                    text={i18n.common.edit}
                    icon={faPen}
                    onClick={() => {
                      startEditing()
                    }}
                  />
                </RightAlign>

                <Gap size="s" />
              </RequireRole>

              {apiData.value === null ? (
                <FixedWidthLabel>
                  {i18n.childInformation.dailyServiceTimes.types.notSet}
                </FixedWidthLabel>
              ) : (
                <>
                  {isRegular(apiData.value) && (
                    <FixedSpaceRow>
                      <FixedWidthLabel>
                        {i18n.childInformation.dailyServiceTimes.types.regular}
                      </FixedWidthLabel>
                      <div>
                        {i18n.childInformation.dailyServiceTimes.weekdays.monday.toLowerCase()}
                        -
                        {i18n.childInformation.dailyServiceTimes.weekdays.friday.toLowerCase()}{' '}
                        {apiData.value.regularTimes.start}–
                        {apiData.value.regularTimes.end}
                      </div>
                    </FixedSpaceRow>
                  )}
                  {isIrregular(apiData.value) && (
                    <FixedSpaceRow>
                      <FixedWidthLabel>
                        {
                          i18n.childInformation.dailyServiceTimes.types
                            .irregular
                        }
                      </FixedWidthLabel>
                      <div>
                        {weekdays
                          .map((wd) =>
                            apiData.value &&
                            isIrregular(apiData.value) &&
                            apiData.value[wd]
                              ? `${i18n.childInformation.dailyServiceTimes.weekdays[
                                  wd
                                ].toLowerCase()} ${
                                  apiData.value[wd]?.start ?? ''
                                }-${apiData.value[wd]?.end ?? ''}`
                              : null
                          )
                          .filter((s) => s !== null)
                          .join(', ')}
                      </div>
                    </FixedSpaceRow>
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
                    onChange={() =>
                      setFormData((old) =>
                        old !== null
                          ? {
                              ...old,
                              type: 'NOT_SET'
                            }
                          : null
                      )
                    }
                  />
                  <Radio
                    label={
                      i18n.childInformation.dailyServiceTimes.types.regular
                    }
                    checked={formData.type === 'REGULAR'}
                    onChange={() =>
                      setFormData((old) =>
                        old !== null
                          ? {
                              ...old,
                              type: 'REGULAR'
                            }
                          : null
                      )
                    }
                  />
                  {formData.type === 'REGULAR' && (
                    <FixedSpaceRow style={{ marginLeft: defaultMargins.XXL }}>
                      <span>
                        {i18n.childInformation.dailyServiceTimes.weekdays.monday.toLowerCase()}
                        -
                        {i18n.childInformation.dailyServiceTimes.weekdays.friday.toLowerCase()}
                      </span>
                      <FixedSpaceRow>
                        <InputField
                          value={formData.regular.start}
                          onChange={(value) =>
                            setFormData((old) =>
                              old !== null
                                ? {
                                    ...old,
                                    regular: {
                                      ...old.regular,
                                      start: value
                                    }
                                  }
                                : null
                            )
                          }
                          type="time"
                          required
                          dataQa="regular-start"
                          info={
                            validationResult?.regular?.start
                              ? {
                                  status: 'warning',
                                  text: validationResult.regular.start
                                }
                              : undefined
                          }
                          width="s"
                        />
                        <span> - </span>
                        <InputField
                          value={formData.regular.end}
                          onChange={(value) =>
                            setFormData((old) =>
                              old !== null
                                ? {
                                    ...old,
                                    regular: {
                                      ...old.regular,
                                      end: value
                                    }
                                  }
                                : null
                            )
                          }
                          type="time"
                          required
                          dataQa="regular-end"
                          info={
                            validationResult?.regular?.end
                              ? {
                                  status: 'warning',
                                  text: validationResult.regular.end
                                }
                              : undefined
                          }
                          width="s"
                        />
                      </FixedSpaceRow>
                    </FixedSpaceRow>
                  )}
                  <Radio
                    label={
                      i18n.childInformation.dailyServiceTimes.types.irregular
                    }
                    checked={formData.type === 'IRREGULAR'}
                    onChange={() =>
                      setFormData((old) =>
                        old !== null
                          ? {
                              ...old,
                              type: 'IRREGULAR'
                            }
                          : null
                      )
                    }
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
                              onChange={(checked) =>
                                setFormData((old) =>
                                  old !== null
                                    ? {
                                        ...old,
                                        [wd]: {
                                          ...old[wd],
                                          selected: checked
                                        }
                                      }
                                    : null
                                )
                              }
                            />
                          </div>
                          <FixedSpaceRow>
                            <InputField
                              value={formData[wd].start}
                              onChange={(value) =>
                                setFormData((old) =>
                                  old !== null
                                    ? {
                                        ...old,
                                        [wd]: {
                                          ...old[wd],
                                          start: value
                                        }
                                      }
                                    : null
                                )
                              }
                              type="time"
                              required
                              dataQa={`${wd}-start`}
                              info={
                                validationResult && validationResult[wd].start
                                  ? {
                                      status: 'warning',
                                      text: validationResult[wd].start || ''
                                    }
                                  : undefined
                              }
                              width="s"
                            />
                            <span> - </span>
                            <InputField
                              value={formData[wd].end}
                              onChange={(value) =>
                                setFormData((old) =>
                                  old !== null
                                    ? {
                                        ...old,
                                        [wd]: {
                                          ...old[wd],
                                          end: value
                                        }
                                      }
                                    : null
                                )
                              }
                              type="time"
                              required
                              dataQa={`${wd}-end`}
                              info={
                                validationResult && validationResult[wd].end
                                  ? {
                                      status: 'warning',
                                      text: validationResult[wd].end || ''
                                    }
                                  : undefined
                              }
                              width="s"
                            />
                          </FixedSpaceRow>
                        </FixedSpaceRow>
                      ))}
                    </FixedSpaceColumn>
                  )}
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
                  <Button
                    text={i18n.common.confirm}
                    onClick={onSubmit}
                    disabled={submitting || !formIsValid}
                    primary
                  />
                </FixedSpaceRow>
              </RightAlign>
            </>
          )}
        </>
      )}
    </CollapsibleSection>
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
