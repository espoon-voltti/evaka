import React, { useState } from 'react'
import styled from 'styled-components'
import DateRange from 'lib-common/date-range'
import LocalDate from 'lib-common/local-date'
import { H3LikeLabel, Label } from 'lib-components/typography'
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
import { FeeThresholds } from '../../types/finance-basics'
import { isValidCents, parseCentsOrThrow } from '../../utils/money'
import { FormState } from './FeesSection'
import { createFeeThresholds } from 'employee-frontend/api/finance-basics'

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
      </FixedSpaceRow>
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
          {(['2', '3', '4', '5', '6'] as const).map((n) => {
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
                    info={validationErrorInfo(
                      `minIncomeThreshold${n}` as `minIncomeThreshold${typeof n}`
                    )}
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
                    info={validationErrorInfo(
                      `incomeMultiplier${n}` as `incomeMultiplier${typeof n}`
                    )}
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
                    info={validationErrorInfo(
                      `maxIncomeThreshold${n}` as `maxIncomeThreshold${typeof n}`
                    )}
                    hideErrorsBeforeTouched
                  />
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

type ValidationErrors = Partial<Record<keyof FormState, string>>
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
  'incomeThresholdIncrease6Plus'
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
    siblingDiscount2: 0.5,
    siblingDiscount2Plus: 0.8
  }
}
