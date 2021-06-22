{
  /*
SPDX-FileCopyrightText: 2017-2021 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import React, { useState } from 'react'
import styled from 'styled-components'
import { Result } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import LocalDate from 'lib-common/local-date'
import colors from 'lib-customizations/common'
import { H3LikeLabel, H4, Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Table, Tbody, Th, Thead, Td, Tr } from 'lib-components/layout/Table'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import InputField from 'lib-components/atoms/form/InputField'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import {
  createFeeThresholds,
  updateFeeThresholds
} from '../../api/finance-basics'
import { Translations } from '../../state/i18n'
import {
  familySizes,
  FamilySize,
  FeeThresholds,
  FeeThresholdsWithId
} from '../../types/finance-basics'
import { isValidCents, parseCents, parseCentsOrThrow } from '../../utils/money'
import { FormState } from './FeesSection'

export default React.memo(function FeeThresholdsEditor({
  i18n,
  id,
  initialState,
  close,
  reloadData,
  toggleSaveRetroactiveWarning,
  existingThresholds
}: {
  i18n: Translations
  id: string | undefined
  initialState: FormState
  close: () => void
  reloadData: () => void
  toggleSaveRetroactiveWarning: (cbs: {
    resolve: () => void
    reject: () => void
  }) => void
  existingThresholds: Result<FeeThresholdsWithId[]>
}) {
  const [editorState, setEditorState] = useState<FormState>(initialState)
  const validationResult = validateForm(
    i18n,
    editorState,
    existingThresholds.map((ts) => ts.filter((t) => t.id !== id))
  )
  const [saveError, setSaveError] = useState<string>()

  const handleSaveErrors = async (promise: Promise<Result<unknown>>) => {
    let result
    try {
      result = await promise
    } catch (e) {
      setSaveError('unknown')
      throw e
    }

    if (result.isFailure) {
      setSaveError(result.errorCode)
      throw Error(result.message)
    } else {
      setSaveError(undefined)
    }
  }

  const validationErrorInfo = (
    prop: keyof FormState
  ): { status: 'warning'; text: string } | undefined =>
    'errors' in validationResult && validationResult.errors[prop]
      ? {
          status: 'warning',
          text: ''
        }
      : undefined

  const dateOverlapInputInfo =
    'errors' in validationResult && validationResult.errors.dateOverlap
      ? ({
          status: 'warning',
          text: ''
        } as const)
      : undefined

  return (
    <>
      <div className="separator large" />
      <RowWithMargin alignItems="center" spacing="xs">
        <H3LikeLabel>{i18n.financeBasics.fees.validDuring}</H3LikeLabel>
        <DatePicker
          locale="fi"
          date={editorState.validFrom}
          onChange={(validFrom) =>
            setEditorState((previousState) => ({
              ...previousState,
              validFrom
            }))
          }
          info={validationErrorInfo('validFrom') ?? dateOverlapInputInfo}
          hideErrorsBeforeTouched
          data-qa="valid-from"
        />
        <span>-</span>
        <DatePicker
          locale="fi"
          date={editorState.validTo}
          onChange={(validTo) =>
            setEditorState((previousState) => ({
              ...previousState,
              validTo
            }))
          }
          info={validationErrorInfo('validTo') ?? dateOverlapInputInfo}
          hideErrorsBeforeTouched
          data-qa="valid-to"
        />
      </RowWithMargin>
      {'errors' in validationResult && validationResult.errors.dateOverlap ? (
        <ErrorDescription>
          {validationResult.errors.dateOverlap}
        </ErrorDescription>
      ) : null}
      <H4>{i18n.financeBasics.fees.thresholds}</H4>
      <RowWithMargin spacing="XL">
        <FixedSpaceColumn spacing="xxs">
          <Label htmlFor="maxFee">{i18n.financeBasics.fees.maxFee}</Label>
          <InputField
            id="maxFee"
            width="s"
            value={editorState.maxFee}
            onChange={(maxFee) =>
              setEditorState((previousState) => ({
                ...previousState,
                maxFee
              }))
            }
            placeholder="0"
            symbol={'€'}
            info={validationErrorInfo('maxFee')}
            hideErrorsBeforeTouched
            data-qa="max-fee"
          />
        </FixedSpaceColumn>
        <FixedSpaceColumn spacing="xxs">
          <Label htmlFor="minFee">{i18n.financeBasics.fees.minFee}</Label>
          <InputField
            id="minFee"
            width="s"
            value={editorState.minFee}
            onChange={(minFee) =>
              setEditorState((previousState) => ({
                ...previousState,
                minFee
              }))
            }
            placeholder="0"
            symbol={'€'}
            info={validationErrorInfo('minFee')}
            hideErrorsBeforeTouched
            data-qa="min-fee"
          />
        </FixedSpaceColumn>
      </RowWithMargin>
      <TableWithMargin>
        <Thead>
          <Tr>
            <Th>{i18n.financeBasics.fees.familySize}</Th>
            <Th>{i18n.financeBasics.fees.minThreshold}</Th>
            <Th>{i18n.financeBasics.fees.multiplier}</Th>
            <Th>{i18n.financeBasics.fees.maxThreshold}</Th>
          </Tr>
        </Thead>
        <Tbody>
          {familySizes.map((n) => {
            const maxFeeError =
              'errors' in validationResult &&
              validationResult.errors[`maxFee${n}`]
                ? validationResult.errors[`maxFee${n}` as `maxFee${typeof n}`]
                : undefined

            const maxFeeErrorInputInfo = maxFeeError
              ? ({ status: 'warning', text: '' } as const)
              : undefined

            return (
              <Tr key={n}>
                <Td>{n}</Td>
                <Td>
                  <InputField
                    width="s"
                    value={
                      editorState[
                        `minIncomeThreshold${n}` as `minIncomeThreshold${typeof n}`
                      ]
                    }
                    onChange={(value) =>
                      setEditorState((previousState) => ({
                        ...previousState,
                        [`minIncomeThreshold${n}`]: value
                      }))
                    }
                    symbol={'€'}
                    info={
                      validationErrorInfo(
                        `minIncomeThreshold${n}` as `minIncomeThreshold${typeof n}`
                      ) ?? maxFeeErrorInputInfo
                    }
                    hideErrorsBeforeTouched
                    data-qa={`min-income-threshold-${n}`}
                  />
                </Td>
                <Td>
                  <InputField
                    width="s"
                    value={
                      editorState[
                        `incomeMultiplier${n}` as `incomeMultiplier${typeof n}`
                      ]
                    }
                    onChange={(value) =>
                      setEditorState((previousState) => ({
                        ...previousState,
                        [`incomeMultiplier${n}`]: value
                      }))
                    }
                    symbol={'%'}
                    info={
                      validationErrorInfo(
                        `incomeMultiplier${n}` as `incomeMultiplier${typeof n}`
                      ) ?? maxFeeErrorInputInfo
                    }
                    hideErrorsBeforeTouched
                    data-qa={`income-multiplier-${n}`}
                  />
                </Td>
                <Td>
                  <InputField
                    width="s"
                    value={
                      editorState[
                        `maxIncomeThreshold${n}` as `maxIncomeThreshold${typeof n}`
                      ]
                    }
                    onChange={(value) =>
                      setEditorState((previousState) => ({
                        ...previousState,
                        [`maxIncomeThreshold${n}`]: value
                      }))
                    }
                    symbol={'€'}
                    info={
                      validationErrorInfo(
                        `maxIncomeThreshold${n}` as `maxIncomeThreshold${typeof n}`
                      ) ?? maxFeeErrorInputInfo
                    }
                    hideErrorsBeforeTouched
                    data-qa={`max-income-threshold-${n}`}
                  />
                  <MaxFeeError data-qa={`max-fee-error-${n}`}>
                    {maxFeeError
                      ? `${i18n.financeBasics.fees.maxFeeError} (${maxFeeError} €)`
                      : null}
                  </MaxFeeError>
                </Td>
              </Tr>
            )
          })}
        </Tbody>
      </TableWithMargin>
      <ColumnWithMargin spacing="xxs">
        <ExpandingInfo
          info={i18n.financeBasics.fees.thresholdIncreaseInfo}
          ariaLabel={i18n.common.openExpandingInfo}
        >
          <Label htmlFor="thresholdIncrease">
            {i18n.financeBasics.fees.thresholdIncrease}
          </Label>
        </ExpandingInfo>
        <InputField
          id="thresholdIncrease"
          width="s"
          value={editorState.incomeThresholdIncrease6Plus}
          onChange={(incomeThresholdIncrease6Plus) =>
            setEditorState((previousState) => ({
              ...previousState,
              incomeThresholdIncrease6Plus
            }))
          }
          placeholder="0"
          symbol={'€'}
          info={validationErrorInfo('incomeThresholdIncrease6Plus')}
          hideErrorsBeforeTouched
          data-qa="income-threshold-increase"
        />
      </ColumnWithMargin>
      <H4>{i18n.financeBasics.fees.siblingDiscounts}</H4>
      <RowWithMargin spacing="XL">
        <FixedSpaceColumn spacing="xxs">
          <Label htmlFor="siblingDiscount2">
            {i18n.financeBasics.fees.siblingDiscount2}
          </Label>
          <InputField
            width="s"
            value={editorState.siblingDiscount2}
            onChange={(siblingDiscount2) =>
              setEditorState((previousState) => ({
                ...previousState,
                siblingDiscount2
              }))
            }
            symbol={'%'}
            info={validationErrorInfo('siblingDiscount2')}
            hideErrorsBeforeTouched
            data-qa="sibling-discount-2"
          />
        </FixedSpaceColumn>
        <FixedSpaceColumn spacing="xxs">
          <Label htmlFor="siblingDiscount2Plus">
            {i18n.financeBasics.fees.siblingDiscount2Plus}
          </Label>
          <InputField
            width="s"
            value={editorState.siblingDiscount2Plus}
            onChange={(siblingDiscount2Plus) =>
              setEditorState((previousState) => ({
                ...previousState,
                siblingDiscount2Plus
              }))
            }
            symbol={'%'}
            info={validationErrorInfo('siblingDiscount2Plus')}
            hideErrorsBeforeTouched
            data-qa="sibling-discount-2-plus"
          />
        </FixedSpaceColumn>
      </RowWithMargin>
      {saveError ? (
        <SaveError>
          {i18n.financeBasics.fees.errors[saveError] ??
            i18n.common.error.unknown}
        </SaveError>
      ) : null}
      <ButtonRow>
        <Button text={i18n.common.cancel} onClick={close} />
        <AsyncButton
          primary
          text={i18n.common.save}
          onClick={async () => {
            if (!('payload' in validationResult)) {
              return
            }

            if (
              validationResult.payload.validDuring.start.isBefore(
                LocalDate.today()
              )
            ) {
              const resolved = await new Promise((resolve) =>
                toggleSaveRetroactiveWarning({
                  resolve: () => resolve(true),
                  reject: () => resolve(false)
                })
              )

              if (!resolved) return 'AsyncButton.cancel'
            }

            return await handleSaveErrors(
              id === undefined
                ? createFeeThresholds(validationResult.payload)
                : updateFeeThresholds(id, validationResult.payload)
            )
          }}
          onSuccess={() => {
            close()
            reloadData()
          }}
          disabled={'errors' in validationResult}
          data-qa="save"
        />
      </ButtonRow>
    </>
  )
})

const TableWithMargin = styled(Table)`
  margin: ${defaultMargins.m} 0;
`

const ColumnWithMargin = styled(FixedSpaceColumn)`
  margin: ${defaultMargins.m} 0;
`

const RowWithMargin = styled(FixedSpaceRow)`
  margin: ${defaultMargins.m} 0;
`

const ButtonRow = styled(FixedSpaceRow)`
  margin: ${defaultMargins.L} 0;
`

const ErrorDescription = styled.span`
  color: ${colors.greyscale.dark};
  font-style: italic;
`

const MaxFeeError = styled(ErrorDescription)`
  margin-left: ${defaultMargins.s};
`

const SaveError = styled.span`
  color: ${colors.accents.red};
`

type ValidationErrors = Partial<
  Record<keyof FormState, string> &
    Record<`maxFee${FamilySize}`, number> &
    Record<'dateOverlap', string>
>
type FeeThresholdsPayload = Omit<FeeThresholds, 'id'>

const centProps: readonly (keyof FormState)[] = [
  'maxFee',
  'minFee',
  'minIncomeThreshold2',
  'minIncomeThreshold3',
  'minIncomeThreshold4',
  'minIncomeThreshold5',
  'minIncomeThreshold6',
  'maxIncomeThreshold2',
  'maxIncomeThreshold3',
  'maxIncomeThreshold4',
  'maxIncomeThreshold5',
  'maxIncomeThreshold6',
  'incomeMultiplier2',
  'incomeMultiplier3',
  'incomeMultiplier4',
  'incomeMultiplier5',
  'incomeMultiplier6',
  'incomeThresholdIncrease6Plus',
  'siblingDiscount2',
  'siblingDiscount2Plus'
]

function validateForm(
  i18n: Translations,
  form: FormState,
  existingThresholds: Result<FeeThresholdsWithId[]>
): { errors: ValidationErrors } | { payload: FeeThresholdsPayload } {
  const validationErrors: ValidationErrors = {}

  centProps.forEach((prop) => {
    if (!isValidCents(form[prop])) {
      validationErrors[prop] = i18n.validationError.decimal
    }
  })

  const parsedValidFrom = LocalDate.parseFiOrNull(form.validFrom)
  if (parsedValidFrom === null) {
    validationErrors.validFrom = i18n.validationError.dateRange
  }

  const parsedValidTo = LocalDate.parseFiOrNull(form.validTo)
  if (form.validTo && parsedValidTo === null) {
    validationErrors.validTo = i18n.validationError.dateRange
  }

  if (
    parsedValidFrom !== null &&
    parsedValidTo !== null &&
    parsedValidTo.isBefore(parsedValidFrom)
  ) {
    validationErrors.validFrom = i18n.validationError.invertedDateRange
    validationErrors.validTo = i18n.validationError.invertedDateRange
  }

  const dateOverlap =
    existingThresholds
      .map((ts) =>
        ts.filter((t) => {
          if (parsedValidFrom === null) {
            return false
          }

          const dateRange = new DateRange(parsedValidFrom, parsedValidTo)

          if (t.thresholds.validDuring.end === null) {
            return (
              t.thresholds.validDuring.overlapsWith(dateRange) &&
              !t.thresholds.validDuring.start.isBefore(parsedValidFrom)
            )
          }

          return t.thresholds.validDuring.overlapsWith(dateRange)
        })
      )
      .getOrElse([]).length > 0

  if (dateOverlap) {
    validationErrors.dateOverlap =
      i18n.financeBasics.fees.errors['date-overlap']
  }

  familySizes.forEach((n) => {
    const maxFee = parseCents(form.maxFee)
    const minThreshold = parseCents(form[`minIncomeThreshold${n}`])
    const maxThreshold = parseCents(form[`maxIncomeThreshold${n}`])
    const multiplier = parseMultiplier(form[`incomeMultiplier${n}`])

    if (
      maxFee &&
      minThreshold &&
      maxThreshold &&
      multiplier &&
      minThreshold < maxThreshold
    ) {
      const computedMaxFee = calculateMaxFeeFromThresholds(
        minThreshold,
        maxThreshold,
        multiplier
      )

      if (maxFee !== computedMaxFee) {
        validationErrors[`maxFee${n}`] = computedMaxFee / 100
      }
    }
  })

  return Object.keys(validationErrors).length === 0
    ? { payload: parseFormOrThrow(form) }
    : { errors: validationErrors }
}

function parseMultiplier(dec: string): number {
  return Number(dec.replace(',', '.')) / 100
}

function parseFormOrThrow(form: FormState): FeeThresholdsPayload {
  return {
    validDuring: new DateRange(
      LocalDate.parseFiOrThrow(form.validFrom),
      form.validTo ? LocalDate.parseFiOrThrow(form.validTo) : null
    ),
    maxFee: parseCentsOrThrow(form.maxFee),
    minFee: parseCentsOrThrow(form.minFee),
    minIncomeThreshold2: parseCentsOrThrow(form.minIncomeThreshold2),
    minIncomeThreshold3: parseCentsOrThrow(form.minIncomeThreshold3),
    minIncomeThreshold4: parseCentsOrThrow(form.minIncomeThreshold4),
    minIncomeThreshold5: parseCentsOrThrow(form.minIncomeThreshold5),
    minIncomeThreshold6: parseCentsOrThrow(form.minIncomeThreshold6),
    maxIncomeThreshold2: parseCentsOrThrow(form.maxIncomeThreshold2),
    maxIncomeThreshold3: parseCentsOrThrow(form.maxIncomeThreshold3),
    maxIncomeThreshold4: parseCentsOrThrow(form.maxIncomeThreshold4),
    maxIncomeThreshold5: parseCentsOrThrow(form.maxIncomeThreshold5),
    maxIncomeThreshold6: parseCentsOrThrow(form.maxIncomeThreshold6),
    incomeMultiplier2: parseMultiplier(form.incomeMultiplier2),
    incomeMultiplier3: parseMultiplier(form.incomeMultiplier3),
    incomeMultiplier4: parseMultiplier(form.incomeMultiplier4),
    incomeMultiplier5: parseMultiplier(form.incomeMultiplier5),
    incomeMultiplier6: parseMultiplier(form.incomeMultiplier6),
    incomeThresholdIncrease6Plus: parseCentsOrThrow(
      form.incomeThresholdIncrease6Plus
    ),
    siblingDiscount2: parseMultiplier(form.siblingDiscount2),
    siblingDiscount2Plus: parseMultiplier(form.siblingDiscount2Plus)
  }
}

function calculateMaxFeeFromThresholds(
  minThreshold: number,
  maxThreshold: number,
  multiplier: number
) {
  return Math.round(((maxThreshold - minThreshold) * multiplier) / 100) * 100
}
