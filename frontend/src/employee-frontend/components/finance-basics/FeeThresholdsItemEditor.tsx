import React, { useState } from 'react'
import styled from 'styled-components'
import DateRange from 'lib-common/date-range'
import LocalDate from 'lib-common/local-date'
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
import { Translations } from '../../state/i18n'
import {
  familySizes,
  FamilySize,
  FeeThresholds
} from '../../types/finance-basics'
import { isValidCents, parseCents, parseCentsOrThrow } from '../../utils/money'
import { FormState } from './FeesSection'
import { createFeeThresholds } from 'employee-frontend/api/finance-basics'
import colors from 'lib-customizations/common'

export default React.memo(function FeeThresholdsItemEditor({
  i18n,
  initialState,
  close,
  reloadData
}: {
  i18n: Translations
  initialState: FormState
  close: () => void
  reloadData: () => void
}) {
  const [editorState, setEditorState] = useState<FormState>(initialState)
  const validationResult = validateForm(i18n, editorState)

  const validationErrorInfo = (
    prop: keyof FormState
  ): { status: 'warning'; text: string } | undefined =>
    'errors' in validationResult && validationResult.errors[prop]
      ? {
          status: 'warning',
          text: ''
        }
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
          info={validationErrorInfo('validFrom')}
          hideErrorsBeforeTouched
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
          info={validationErrorInfo('validTo')}
          hideErrorsBeforeTouched
        />
      </RowWithMargin>
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
                  />
                  <MaxFeeError>
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
          />
        </FixedSpaceColumn>
      </RowWithMargin>
      <ButtonRow>
        <Button text={i18n.common.cancel} onClick={close} />
        <AsyncButton
          primary
          text={i18n.common.save}
          onClick={() =>
            'payload' in validationResult
              ? createFeeThresholds(validationResult.payload)
              : Promise.resolve()
          }
          onSuccess={() => {
            close()
            reloadData()
          }}
          disabled={'errors' in validationResult}
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

const MaxFeeError = styled.span`
  color: ${colors.greyscale.dark};
  font-style: italic;
  margin-left: ${defaultMargins.s};
`

type ValidationErrors = Partial<Record<keyof FormState, string>> &
  Partial<Record<`maxFee${FamilySize}`, number>>
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
  form: FormState
): { errors: ValidationErrors } | { payload: FeeThresholdsPayload } {
  const validationErrors: ValidationErrors = {}

  centProps.forEach((prop) => {
    if (!isValidCents(form[prop])) {
      validationErrors[prop] = i18n.validationError.decimal
    }
  })

  if (LocalDate.parseFiOrNull(form.validFrom) === null) {
    validationErrors.validFrom = i18n.validationError.dateRange
  }

  if (form.validTo && LocalDate.parseFiOrNull(form.validTo) === null) {
    validationErrors.validTo = i18n.validationError.dateRange
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
