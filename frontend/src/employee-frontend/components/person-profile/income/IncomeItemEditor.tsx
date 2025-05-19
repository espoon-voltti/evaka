// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isEqual from 'lodash/isEqual'
import omit from 'lodash/omit'
import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import type { Failure, Result } from 'lib-common/api'
import { incomeEffects } from 'lib-common/api-types/income'
import DateRange from 'lib-common/date-range'
import type { Attachment } from 'lib-common/generated/api-types/attachment'
import type {
  Income,
  IncomeCoefficient,
  IncomeEffect,
  IncomeRequest,
  IncomeTypeOptions,
  IncomeValue
} from 'lib-common/generated/api-types/invoicing'
import type { IncomeId, PersonId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { parseCents } from 'lib-common/money'
import type { UUID } from 'lib-common/types'
import Title from 'lib-components/atoms/Title'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
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

import { getAttachmentUrl, incomeAttachment } from '../../../api/attachments'
import { useTranslation } from '../../../state/i18n'
import type { IncomeFields } from '../../../types/income'
import RetroactiveConfirmation, {
  isChangeRetroactive
} from '../../common/RetroactiveConfirmation'

import type { IncomeTableData } from './IncomeTable'
import IncomeTable, { tableDataFromIncomeFields } from './IncomeTable'

const ButtonsContainer = styled(FixedSpaceRow)`
  margin: 20px 0;
`

export interface IncomeForm {
  effect: IncomeEffect
  data: IncomeTableData
  isEntrepreneur: boolean
  worksAtECHA: boolean
  validFrom: LocalDate | null
  validTo: LocalDate | null
  notes: string
  attachments: Attachment[]
}

function incomeFormFromIncome(value: IncomeRequest): IncomeForm {
  return { ...value, data: tableDataFromIncomeFields(value.data) }
}

const emptyIncome: IncomeForm = {
  effect: 'INCOME',
  data: {} as IncomeTableData,
  isEntrepreneur: false,
  worksAtECHA: false,
  notes: '',
  validFrom: null,
  validTo: null,
  attachments: []
}

const calculateAmounts = (
  amount: string,
  coefficient: IncomeCoefficient,
  multiplier: number | undefined
): IncomeValue | undefined => {
  const parsed = parseCents(amount)
  if (parsed === undefined || multiplier === undefined) return undefined
  return {
    amount: parsed,
    coefficient,
    monthlyAmount: Math.round(parsed * multiplier),
    multiplier
  }
}

function updateIncomeData(
  data: IncomeTableData,
  coefficientMultipliers: Partial<Record<IncomeCoefficient, number>>
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
  coefficientMultipliers: Partial<Record<IncomeCoefficient, number>>,
  personId: PersonId
): IncomeRequest | undefined {
  if (form.validFrom === null) return undefined

  const result: IncomeFields = {}

  for (const [key, value] of Object.entries(form.data)) {
    if (!value) continue
    const { amount, coefficient } = value
    if (amount) {
      const item = calculateAmounts(
        amount,
        coefficient,
        coefficientMultipliers[coefficient]
      )
      if (item) {
        result[key] = item
      }
    }
  }

  return {
    ...form,
    validFrom: form.validFrom,
    validTo: form.validTo,
    data: result,
    personId
  }
}

interface CommonProps {
  personId: PersonId
  incomeTypeOptions: IncomeTypeOptions
  coefficientMultipliers: Partial<Record<IncomeCoefficient, number>>
  cancel: () => void
  onSuccess: () => void
  onFailure: (value: Failure<unknown>) => void
}

interface CreateProps extends CommonProps {
  create: (income: IncomeRequest) => Promise<Result<unknown>>
}

interface UpdateProps extends CommonProps {
  baseIncome: Income
  update: (income: IncomeRequest) => Promise<Result<unknown>>
}

type Props = CreateProps | UpdateProps

function isUpdate(props: Props): props is UpdateProps {
  return 'baseIncome' in props
}

const IncomeItemEditor = React.memo(function IncomeItemEditor(props: Props) {
  const {
    personId,
    incomeTypeOptions,
    coefficientMultipliers,
    cancel,
    onSuccess,
    onFailure
  } = props

  const { i18n, lang } = useTranslation()

  const initialForm = useMemo(
    () =>
      isUpdate(props) ? incomeFormFromIncome(props.baseIncome) : emptyIncome,
    [props]
  )
  const [editedIncome, setEditedIncome] = useState<IncomeForm>(initialForm)
  const [validationErrors, setValidationErrors] = useState<
    Partial<Record<keyof Income | 'dates', boolean>>
  >({})
  const retroactive = useMemo(() => {
    const editedContent = omit(editedIncome, [
      'validFrom',
      'validTo',
      'attachments'
    ])
    const initialContent = omit(initialForm, [
      'validFrom',
      'validTo',
      'attachments'
    ])
    const editedRange = editedIncome.validFrom
      ? new DateRange(editedIncome.validFrom, editedIncome.validTo)
      : null
    const initialRange = initialForm.validFrom
      ? new DateRange(initialForm.validFrom, initialForm.validTo)
      : null
    return (
      editedIncome.validFrom !== null &&
      isChangeRetroactive(
        editedRange,
        initialRange,
        !isEqual(editedContent, initialContent),
        LocalDate.todayInHelsinkiTz()
      )
    )
  }, [editedIncome, initialForm])
  const [confirmedRetroactive, setConfirmedRetroactive] = useState(false)

  const [prevValidFrom, setPrevValidFrom] = useState(editedIncome.validFrom)
  const onRangeChange = useCallback(
    (from: LocalDate | null, to: LocalDate | null) => {
      const fromWasChanged =
        (prevValidFrom === null && from !== null) ||
        (prevValidFrom !== null && from === null) ||
        (prevValidFrom !== null &&
          from !== null &&
          !from.isEqual(prevValidFrom))
      setEditedIncome((prev) => ({
        ...prev,
        validFrom: from,
        validTo:
          from !== null && fromWasChanged ? from.addYears(1).subDays(1) : to
      }))
      setPrevValidFrom(from)
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
          required
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
      {isUpdate(props) ? (
        <>
          <Gap size="L" />
          <ListGrid labelWidth="fit-content(40%)" rowGap="xs" columnGap="L">
            <Label>{i18n.personProfile.income.details.created}</Label>
            <span>{props.baseIncome.createdAt.toLocalDate().format()}</span>
            <Label>{i18n.personProfile.income.details.handler}</Label>
            <span>{props.baseIncome.createdBy.name}</span>
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
        incomeId={isUpdate(props) ? props.baseIncome.id : null}
        attachments={isUpdate(props) ? props.baseIncome.attachments : []}
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
        <LegacyButton
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
            editedIncome.validFrom === null ||
            (retroactive && !confirmedRetroactive)
          }
          onClick={(): Promise<Result<unknown>> | void => {
            const body = formToIncomeBody(
              editedIncome,
              coefficientMultipliers,
              personId
            )
            if (!body) return
            return isUpdate(props) ? props.update(body) : props.create(body)
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
  incomeId: IncomeId | null
  attachments: Attachment[]
  onUploaded: (attachment: Attachment) => void
  onDeleted: (id: UUID) => void
}) {
  const { i18n } = useTranslation()

  return (
    <>
      <H1>{i18n.incomeStatement.employeeAttachments.title}</H1>
      <P>{i18n.incomeStatement.employeeAttachments.description}</P>
      <FileUpload
        data-qa="income-attachment-upload"
        files={attachments}
        uploadHandler={incomeAttachment(incomeId)}
        onUploaded={onUploaded}
        onDeleted={onDeleted}
        getDownloadUrl={getAttachmentUrl}
      />
    </>
  )
}

export default IncomeItemEditor
