// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isEqual from 'lodash/isEqual'
import omit from 'lodash/omit'
import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import { Failure, Result } from 'lib-common/api'
import { Attachment } from 'lib-common/api-types/attachment'
import {
  IncomeCoefficient,
  IncomeEffect,
  incomeEffects,
  IncomeValue
} from 'lib-common/api-types/income'
import DateRange from 'lib-common/date-range'
import LocalDate from 'lib-common/local-date'
import { parseCents } from 'lib-common/money'
import { UUID } from 'lib-common/types'
import Title from 'lib-components/atoms/Title'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import Radio from 'lib-components/atoms/form/Radio'
import ListGrid from 'lib-components/layout/ListGrid'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import FileUpload from 'lib-components/molecules/FileUpload'
import DateRangePicker from 'lib-components/molecules/date-picker/DateRangePicker'
import { H1, Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import {
  deleteAttachment,
  getAttachmentUrl,
  saveIncomeAttachment
} from '../../../api/attachments'
import {
  IncomeCoefficientMultipliers,
  IncomeTypeOptions
} from '../../../api/income'
import { useTranslation } from '../../../state/i18n'
import { Income, IncomeBody, IncomeFields } from '../../../types/income'
import RetroactiveConfirmation, {
  isChangeRetroactive
} from '../../common/RetroactiveConfirmation'

import IncomeTable, {
  IncomeTableData,
  tableDataFromIncomeFields
} from './IncomeTable'

const ButtonsContainer = styled(FixedSpaceRow)`
  margin: 20px 0;
`

export interface IncomeForm {
  effect: IncomeEffect
  data: IncomeTableData
  isEntrepreneur: boolean
  worksAtECHA: boolean
  validFrom: LocalDate
  validTo?: LocalDate
  notes: string
  attachments: Attachment[]
}

function incomeFormFromIncome(value: IncomeBody): IncomeForm {
  return { ...value, data: tableDataFromIncomeFields(value.data) }
}

const emptyIncome: IncomeForm = {
  effect: 'INCOME',
  data: {} as IncomeTableData,
  isEntrepreneur: false,
  worksAtECHA: false,
  notes: '',
  validFrom: LocalDate.todayInSystemTz(),
  validTo: undefined,
  attachments: []
}

const calculateAmounts = (
  amount: string,
  coefficient: IncomeCoefficient,
  multiplier: number
): IncomeValue | undefined => {
  const parsed = parseCents(amount)
  if (parsed === undefined) return undefined
  return {
    amount: parsed,
    coefficient,
    monthlyAmount: Math.round(parsed * multiplier)
  }
}

function updateIncomeData(
  data: IncomeTableData,
  coefficientMultipliers: IncomeCoefficientMultipliers
): [IncomeTableData, boolean] {
  let allValid = true
  const result: IncomeTableData = {}

  for (const [key, value] of Object.entries(data)) {
    if (!value) continue
    const { amount, coefficient } = value

    const item = calculateAmounts(
      amount,
      coefficient,
      coefficientMultipliers[coefficient]
    )
    if (!item && amount !== '') allValid = false

    result[key] = {
      amount,
      coefficient,
      monthlyAmount: item?.monthlyAmount ?? 0
    }
  }

  return [result, allValid]
}

function formToIncomeBody(
  form: IncomeForm,
  coefficientMultipliers: IncomeCoefficientMultipliers
): IncomeBody | undefined {
  const result: IncomeFields = {}

  for (const [key, value] of Object.entries(form.data)) {
    if (!value) continue
    const { amount, coefficient } = value
    if (!amount) {
      // Blank amount => delete the field
      result[key] = undefined
    } else {
      const item = calculateAmounts(
        amount,
        coefficient,
        coefficientMultipliers[coefficient]
      )
      if (!item) {
        // Invalid amount, should not happen because the form has been validated
        return undefined
      }
      result[key] = item
    }
  }

  return { ...form, data: result }
}

interface Props {
  baseIncome?: Income
  incomeTypeOptions: IncomeTypeOptions
  coefficientMultipliers: IncomeCoefficientMultipliers
  cancel: () => void
  update: (income: Income) => Promise<Result<unknown>> | void
  create: (income: IncomeBody) => Promise<Result<unknown>>
  onSuccess: () => void
  onFailure: (value: Failure<unknown>) => void
}

const IncomeItemEditor = React.memo(function IncomeItemEditor({
  baseIncome,
  incomeTypeOptions,
  coefficientMultipliers,
  cancel,
  update,
  create,
  onSuccess,
  onFailure
}: Props) {
  const { i18n, lang } = useTranslation()

  const initialForm = useMemo(
    () => (baseIncome ? incomeFormFromIncome(baseIncome) : emptyIncome),
    [baseIncome]
  )
  const [editedIncome, setEditedIncome] = useState<IncomeForm>(initialForm)
  const [validationErrors, setValidationErrors] = useState<
    Partial<{ [K in keyof Income | 'dates']: boolean }>
  >({})
  const retroactive = useMemo(() => {
    const initialContent = omit(initialForm, ['validFrom', 'validTo'])
    const editedContent = omit(editedIncome, ['validFrom', 'validTo'])
    return isChangeRetroactive(
      new DateRange(editedIncome.validFrom, editedIncome.validTo ?? null),
      new DateRange(initialForm.validFrom, initialForm.validTo ?? null),
      !isEqual(initialContent, editedContent),
      LocalDate.todayInHelsinkiTz()
    )
  }, [editedIncome, initialForm])
  const [confirmedRetroactive, setConfirmedRetroactive] = useState(false)

  const [prevValidFrom, setPrevValidFrom] = useState(editedIncome.validFrom)
  const onRangeChange = useCallback(
    (from: LocalDate | null, to: LocalDate | null) => {
      if (from) {
        setEditedIncome((prev) => ({
          ...prev,
          validFrom: from,
          validTo:
            to || from.isEqual(prevValidFrom)
              ? to ?? undefined
              : from.addYears(1).subDays(1)
        }))
        setPrevValidFrom(from)
      }
    },
    [prevValidFrom]
  )

  const setIncomeData = useCallback(
    (data: IncomeTableData) => {
      const [updatedData, isValid] = updateIncomeData(
        data,
        coefficientMultipliers
      )
      setEditedIncome((prev) => ({ ...prev, data: updatedData }))
      setValidationErrors((prev) => ({ ...prev, data: !isValid }))
    },
    [coefficientMultipliers]
  )

  const setValidationResult = useCallback(
    (isValid: boolean) =>
      setValidationErrors((prev) => ({ ...prev, dates: !isValid })),
    [setValidationErrors]
  )

  return (
    <>
      <div data-qa="income-date-range">
        <Label>{i18n.personProfile.income.details.dateRange}</Label>
        <Gap size="m" />
        <DateRangePicker
          start={editedIncome.validFrom}
          end={editedIncome.validTo || null}
          onChange={onRangeChange}
          onValidationResult={setValidationResult}
          locale={lang}
        />
      </div>
      <Gap size="L" />

      <Label>{i18n.personProfile.income.details.effect}</Label>
      <Gap size="m" />
      <FixedSpaceColumn alignItems="flex-start" data-qa="income-effect">
        {incomeEffects.map((effect) => (
          <Radio
            key={effect}
            label={i18n.personProfile.income.details.effectOptions[effect]}
            checked={editedIncome.effect === effect}
            onChange={() => setEditedIncome((prev) => ({ ...prev, effect }))}
            data-qa={`income-effect-${effect}`}
          />
        ))}
      </FixedSpaceColumn>
      <Gap size="L" />

      <Label>{i18n.personProfile.income.details.miscTitle}</Label>
      <Gap size="m" />
      <FixedSpaceColumn>
        <Checkbox
          label={i18n.personProfile.income.details.echa}
          checked={editedIncome.worksAtECHA}
          onChange={() =>
            setEditedIncome((prev) => ({
              ...prev,
              worksAtECHA: !prev.worksAtECHA
            }))
          }
        />
        <Checkbox
          label={i18n.personProfile.income.details.entrepreneur}
          checked={editedIncome.isEntrepreneur}
          onChange={() =>
            setEditedIncome((prev) => ({
              ...prev,
              isEntrepreneur: !prev.isEntrepreneur
            }))
          }
        />
      </FixedSpaceColumn>
      <Gap size="L" />
      <div data-qa="income-notes">
        <Label>{i18n.personProfile.income.details.notes}</Label>
        <Gap size="m" />
        <InputField
          width="L"
          value={editedIncome.notes}
          onChange={(value) =>
            setEditedIncome((prev) => ({ ...prev, notes: value }))
          }
        />
      </div>
      {baseIncome ? (
        <>
          <Gap size="L" />
          <ListGrid labelWidth="fit-content(40%)" rowGap="xs" columnGap="L">
            <Label>{i18n.personProfile.income.details.updated}</Label>
            <span>{baseIncome.updatedAt.toLocalDate().format()}</span>

            <Label>{i18n.personProfile.income.details.handler}</Label>
            <span>{baseIncome.updatedBy}</span>
          </ListGrid>
        </>
      ) : null}
      {editedIncome.effect === 'INCOME' ? (
        <>
          <div className="separator" />
          <Title size={4}>
            {i18n.personProfile.income.details.incomeTitle}
          </Title>
          <IncomeTable
            incomeTypeOptions={incomeTypeOptions}
            data={editedIncome.data}
            onChange={setIncomeData}
            type="income"
          />
          <Title size={4}>
            {i18n.personProfile.income.details.expensesTitle}
          </Title>
          <IncomeTable
            incomeTypeOptions={incomeTypeOptions}
            data={editedIncome.data}
            onChange={setIncomeData}
            type="expenses"
          />
        </>
      ) : null}
      <IncomeAttachments
        incomeId={baseIncome?.id ?? null}
        attachments={baseIncome?.attachments ?? []}
        onUploaded={(attachment) => {
          setEditedIncome((prev) => ({
            ...prev,
            attachments: prev.attachments.concat(attachment)
          }))
        }}
        onDeleted={(deletedId) => {
          setEditedIncome((prev) => ({
            ...prev,
            attachments: prev.attachments.filter(({ id }) => id !== deletedId)
          }))
        }}
      />
      {retroactive && (
        <>
          <Gap size="m" />
          <RetroactiveConfirmation
            confirmed={confirmedRetroactive}
            setConfirmed={setConfirmedRetroactive}
          />
        </>
      )}
      <ButtonsContainer>
        <Button
          onClick={cancel}
          text={i18n.common.cancel}
          data-qa="cancel-income-edit"
        />
        <AsyncButton
          primary
          text={i18n.common.save}
          textInProgress={i18n.common.saving}
          textDone={i18n.common.saved}
          disabled={
            Object.values(validationErrors).some(Boolean) ||
            (retroactive && !confirmedRetroactive)
          }
          onClick={(): Promise<Result<unknown>> | void => {
            const body = formToIncomeBody(editedIncome, coefficientMultipliers)
            if (!body) return
            return !baseIncome
              ? create(body)
              : update({ ...baseIncome, ...body })
          }}
          onSuccess={onSuccess}
          onFailure={onFailure}
          data-qa="save-income"
        />
      </ButtonsContainer>
    </>
  )
})

function IncomeAttachments({
  incomeId,
  attachments,
  onUploaded,
  onDeleted
}: {
  incomeId: UUID | null
  attachments: Attachment[]
  onUploaded: (attachment: Attachment) => void
  onDeleted: (id: UUID) => void
}) {
  const { i18n } = useTranslation()

  const handleUpload = useCallback(
    async (file: File, onUploadProgress: (percentage: number) => void) =>
      (await saveIncomeAttachment(incomeId, file, onUploadProgress)).map(
        (id) => {
          onUploaded({ id, name: file.name, contentType: file.type })
          return id
        }
      ),
    [incomeId, onUploaded]
  )

  const handleDelete = useCallback(
    async (id: UUID) =>
      (await deleteAttachment(id)).map(() => {
        onDeleted(id)
      }),
    [onDeleted]
  )

  return (
    <>
      <H1>{i18n.incomeStatement.employeeAttachments.title}</H1>
      <P>{i18n.incomeStatement.employeeAttachments.description}</P>
      <FileUpload
        data-qa="income-attachment-upload"
        files={attachments}
        onUpload={handleUpload}
        onDelete={handleDelete}
        getDownloadUrl={getAttachmentUrl}
      />
    </>
  )
}

export default IncomeItemEditor
