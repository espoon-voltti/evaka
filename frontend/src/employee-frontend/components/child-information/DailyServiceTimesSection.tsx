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
} from '../../types/child'
import {
  deleteChildDailyServiceTimes,
  getChildDailyServiceTimes,
  putChildDailyServiceTimes
} from '../../api/child/daily-service-times'
import { faClock, faPen } from '../../../lib-icons'
import styled from 'styled-components'
import InlineButton from '../../../lib-components/atoms/buttons/InlineButton'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import Button from '../../../lib-components/atoms/buttons/Button'
import { defaultMargins, Gap } from 'lib-components/white-space'
import Radio from '../../../lib-components/atoms/form/Radio'
import InputField from '../../../lib-components/atoms/form/InputField'
import Checkbox from '../../../lib-components/atoms/form/Checkbox'
import { UIContext } from '../../state/ui'

interface Props {
  id: UUID
  open: boolean
}

interface TimeInputRange {
  start: string
  end: string
}

interface SelectableTimeInputRange extends TimeInputRange {
  selected: boolean
}

interface FormData {
  type: 'REGULAR' | 'IRREGULAR' | 'NOT_SET'
  regular: TimeInputRange
  monday: SelectableTimeInputRange
  tuesday: SelectableTimeInputRange
  wednesday: SelectableTimeInputRange
  thursday: SelectableTimeInputRange
  friday: SelectableTimeInputRange
}

const emptyForm: FormData = {
  type: 'NOT_SET',
  regular: {
    start: '',
    end: ''
  },
  monday: {
    selected: false,
    start: '',
    end: ''
  },
  tuesday: {
    selected: false,
    start: '',
    end: ''
  },
  wednesday: {
    selected: false,
    start: '',
    end: ''
  },
  thursday: {
    selected: false,
    start: '',
    end: ''
  },
  friday: {
    selected: false,
    start: '',
    end: ''
  }
}

interface ValidationResult {
  regular: {
    start: string | null
    end: string | null
  }
  monday: {
    start: string | null
    end: string | null
  }
  tuesday: {
    start: string | null
    end: string | null
  }
  wednesday: {
    start: string | null
    end: string | null
  }
  thursday: {
    start: string | null
    end: string | null
  }
  friday: {
    start: string | null
    end: string | null
  }
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
  const [editing, setEditing] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [
    validationResult,
    setValidationResult
  ] = useState<ValidationResult | null>(null)

  const loadData = useRestApi(getChildDailyServiceTimes, setApiData)
  useEffect(() => loadData(id), [id])

  const resetForm = () => {
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

  useEffect(resetForm, [apiData])

  const required = (v: string) =>
    v.trim().length === 0
      ? i18n.childInformation.dailyServiceTimes.errors.required
      : null

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
        monday: {
          start:
            formData.type === 'IRREGULAR' && formData.monday.selected
              ? required(formData.monday.start)
              : null,
          end:
            formData.type === 'IRREGULAR' && formData.monday.selected
              ? required(formData.monday.end)
              : null
        },
        tuesday: {
          start:
            formData.type === 'IRREGULAR' && formData.tuesday.selected
              ? required(formData.tuesday.start)
              : null,
          end:
            formData.type === 'IRREGULAR' && formData.tuesday.selected
              ? required(formData.tuesday.end)
              : null
        },
        wednesday: {
          start:
            formData.type === 'IRREGULAR' && formData.wednesday.selected
              ? required(formData.wednesday.start)
              : null,
          end:
            formData.type === 'IRREGULAR' && formData.wednesday.selected
              ? required(formData.wednesday.end)
              : null
        },
        thursday: {
          start:
            formData.type === 'IRREGULAR' && formData.thursday.selected
              ? required(formData.thursday.start)
              : null,
          end:
            formData.type === 'IRREGULAR' && formData.thursday.selected
              ? required(formData.thursday.end)
              : null
        },
        friday: {
          start:
            formData.type === 'IRREGULAR' && formData.friday.selected
              ? required(formData.friday.start)
              : null,
          end:
            formData.type === 'IRREGULAR' && formData.friday.selected
              ? required(formData.friday.end)
              : null
        }
      })
    }
  }

  useEffect(validate, [formData])

  const onSubmit = () => {
    validate()
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
            setEditing(false)
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
          {!editing && (
            <>
              <RightAlign>
                <InlineButton
                  text={'Muokkaa'}
                  icon={faPen}
                  onClick={() => {
                    resetForm()
                    setEditing(true)
                  }}
                />
              </RightAlign>

              {apiData.value === null ? (
                <Label>
                  {i18n.childInformation.dailyServiceTimes.types.notSet}
                </Label>
              ) : (
                <>
                  {isRegular(apiData.value) && (
                    <div>
                      {i18n.childInformation.dailyServiceTimes.types.regular}
                    </div>
                  )}
                  {isIrregular(apiData.value) && (
                    <div>
                      {i18n.childInformation.dailyServiceTimes.types.irregular}
                    </div>
                  )}
                </>
              )}
            </>
          )}

          {editing && (
            <>
              {formData !== null && (
                <div>
                  <FixedSpaceColumn>
                    <Radio
                      label={
                        i18n.childInformation.dailyServiceTimes.types.notSet
                      }
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
                        {([
                          'monday',
                          'tuesday',
                          'wednesday',
                          'thursday',
                          'friday'
                        ] as const).map((wd) => (
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
              )}

              <Gap size="s" />

              <RightAlign>
                <FixedSpaceRow>
                  <Button
                    text={i18n.common.cancel}
                    onClick={() => setEditing(false)}
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

export default DailyServiceTimesSection
