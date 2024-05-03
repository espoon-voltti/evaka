// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import styled from 'styled-components'

import LabelValueList from 'employee-frontend/components/common/LabelValueList'
import { useTranslation } from 'employee-frontend/state/i18n'
import { UIContext } from 'employee-frontend/state/ui'
import { Failure } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { UpdateStateFn } from 'lib-common/form-state'
import { AssistanceNeedVoucherCoefficient } from 'lib-common/generated/api-types/assistanceneed'
import LocalDate from 'lib-common/local-date'
import { useMutationResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import InputField from 'lib-components/atoms/form/InputField'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import DateRangePicker from 'lib-components/molecules/date-picker/DateRangePicker'
import { LabelLike } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import {
  createAssistanceNeedVoucherCoefficientMutation,
  updateAssistanceNeedVoucherCoefficientMutation
} from '../../queries'

const CoefficientInputContainer = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: left;
  width: 100px;
  position: relative;

  > div.field {
    margin-bottom: 0;
    padding-bottom: 0;

    div.is-error span.icon {
      display: none;
    }
  }
`

const coefficientRegex = /^\d+([.,]\d{1,2})?$/

interface CommonProps {
  onSuccess: () => void
  coefficients: AssistanceNeedVoucherCoefficient[]
}

interface CreateProps extends CommonProps {
  childId: UUID
}

interface UpdateProps extends CommonProps {
  childId: UUID
  coefficient: AssistanceNeedVoucherCoefficient
}

type Props = CreateProps | UpdateProps

function isUpdate(props: Props): props is UpdateProps {
  return 'coefficient' in props
}

interface Form {
  start: LocalDate | null
  end: LocalDate | null
  coefficient: string
}

const hasFullOverlap = (
  range: FiniteDateRange,
  existingRanges: FiniteDateRange[]
) => existingRanges.some((r) => range.contains(r))
const hasPreviousOverlap = (
  range: FiniteDateRange,
  existingRanges: FiniteDateRange[]
) =>
  existingRanges.find(
    (r) =>
      range.start.isEqualOrBefore(r.end) && range.start.isEqualOrAfter(r.start)
  )
const hasUpcomingOverlap = (
  range: FiniteDateRange,
  existingRanges: FiniteDateRange[]
) =>
  existingRanges.find(
    (r) => range.end.isEqualOrAfter(r.start) && range.end.isEqualOrBefore(r.end)
  )

export default React.memo(function AssistanceNeedVoucherCoefficientForm(
  props: Props
) {
  const {
    i18n: {
      childInformation: { assistanceNeedVoucherCoefficient: t },
      ...i18n
    },
    lang
  } = useTranslation()
  const { clearUiMode } = useContext(UIContext)

  const { mutateAsync: createAssistanceNeedVoucherCoefficient } =
    useMutationResult(createAssistanceNeedVoucherCoefficientMutation)
  const { mutateAsync: updateAssistanceNeedVoucherCoefficient } =
    useMutationResult(updateAssistanceNeedVoucherCoefficientMutation)

  const [form, setForm] = useState<Form>(
    isUpdate(props)
      ? {
          start: props.coefficient.validityPeriod.start,
          end: props.coefficient.validityPeriod.end,
          coefficient: props.coefficient.coefficient.toString()
        }
      : {
          start: LocalDate.todayInHelsinkiTz(),
          end: null,
          coefficient: ''
        }
  )

  const [warning, setWarning] = useState<string>()

  const updateFormState: UpdateStateFn<Form> = (values) => {
    const newState = { ...form, ...values }
    setForm(newState)
  }

  useEffect(() => {
    setWarning(undefined)

    if (!form.start || !form.end) return

    const validityPeriod = new FiniteDateRange(form.start, form.end)

    const relevantExistingCoefficients = (
      isUpdate(props)
        ? props.coefficients.filter((c) => c.id !== props.coefficient.id)
        : props.coefficients
    ).map((c) => c.validityPeriod)

    const overlapWarning = [
      hasFullOverlap(validityPeriod, relevantExistingCoefficients) &&
        t.form.errors.fullOverlap,
      hasPreviousOverlap(validityPeriod, relevantExistingCoefficients) &&
        t.form.errors.previousOverlap,
      hasUpcomingOverlap(validityPeriod, relevantExistingCoefficients) &&
        t.form.errors.upcomingOverlap
    ].find((warning): warning is string => !!warning)

    if (overlapWarning) {
      setWarning(overlapWarning)
    }
  }, [form, props, t])

  const coefficientError = useMemo(() => {
    if (!coefficientRegex.test(form.coefficient)) {
      return i18n.validationErrors.format
    }

    const parsed = parseFloat(form.coefficient.replace(',', '.'))
    if (parsed > 10 || parsed < 1) {
      return t.form.errors.coefficientRange
    }

    return undefined
  }, [form.coefficient, i18n, t])

  const isValid = useMemo(
    () => !coefficientError && !!form.start && !!form.end,
    [coefficientError, form.end, form.start]
  )

  const submitForm = useCallback(() => {
    if (!isValid || !form.start || !form.end) {
      return Promise.resolve(
        Failure.of({
          message: 'Invalid form'
        })
      )
    }

    const validityPeriod = new FiniteDateRange(form.start, form.end)
    const data = {
      validityPeriod,
      coefficient: parseFloat(form.coefficient.replace(',', '.'))
    }

    if (isUpdate(props)) {
      return updateAssistanceNeedVoucherCoefficient({
        childId: props.childId,
        id: props.coefficient.id,
        body: data
      })
    } else {
      return createAssistanceNeedVoucherCoefficient({
        childId: props.childId,
        body: data
      })
    }
  }, [
    form.coefficient,
    form.end,
    form.start,
    isValid,
    props,
    createAssistanceNeedVoucherCoefficient,
    updateAssistanceNeedVoucherCoefficient
  ])

  return (
    <form onSubmit={submitForm}>
      {!isUpdate(props) && (
        <>
          <ExpandingInfo info={<div>{t.form.titleInfo}</div>} width="full">
            <LabelLike>{t.form.title}</LabelLike>
          </ExpandingInfo>
          <Gap size="s" />
        </>
      )}
      <LabelValueList
        spacing="large"
        contents={[
          {
            label: t.form.coefficient,
            value: (
              <>
                <CoefficientInputContainer>
                  <InputField
                    value={form.coefficient}
                    onChange={(value) =>
                      updateFormState({
                        coefficient: value
                      })
                    }
                    type="number"
                    min={1}
                    max={10}
                    step={0.1}
                    info={
                      coefficientError
                        ? {
                            text: coefficientError,
                            status: 'warning'
                          }
                        : undefined
                    }
                    hideErrorsBeforeTouched
                    data-qa="input-assistance-need-voucher-coefficient"
                  />
                </CoefficientInputContainer>
              </>
            )
          },
          {
            label: t.form.validityPeriod,
            value: (
              <DateRangePicker
                start={form.start}
                end={form.end}
                onChange={(start, end) => updateFormState({ start, end })}
                locale={lang}
                hideErrorsBeforeTouched
                data-qa="input-assistance-need-voucher-coefficient-validity-period"
              />
            )
          }
        ]}
      />

      {!!warning && (
        <>
          <Gap size="xs" />
          <AlertBox message={warning} />
        </>
      )}

      <Gap size="s" />
      <FixedSpaceRow spacing="s">
        <Button onClick={clearUiMode} text={i18n.common.cancel} />
        <AsyncButton
          primary
          type="submit"
          disabled={!isValid}
          data-qa="assistance-need-voucher-coefficient-save"
          text={i18n.common.save}
          onClick={submitForm}
          onSuccess={() => {
            props.onSuccess()
            clearUiMode()
          }}
        />
      </FixedSpaceRow>
    </form>
  )
})
