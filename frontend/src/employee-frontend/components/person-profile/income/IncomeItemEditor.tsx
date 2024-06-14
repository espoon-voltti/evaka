// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isEqual from 'lodash/isEqual'
import omit from 'lodash/omit'
import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import { Failure, Result, wrapResult } from 'lib-common/api'
import { Attachment } from 'lib-common/api-types/attachment'
import { incomeEffects } from 'lib-common/api-types/income'
import DateRange from 'lib-common/date-range'
import {
  Income,
  IncomeCoefficient,
  IncomeEffect,
  IncomeRequest,
  IncomeTypeOptions,
  IncomeValue
} from 'lib-common/generated/api-types/invoicing'
import LocalDate from 'lib-common/local-date'
import { parseCents } from 'lib-common/money'
import { UUID } from 'lib-common/types'
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

import {
  getAttachmentUrl,
  saveIncomeAttachment
} from '../../../api/attachments'
import { deleteAttachment } from '../../../generated/api-clients/attachment'
import { useTranslation } from '../../../state/i18n'
import { IncomeFields } from '../../../types/income'
import RetroactiveConfirmation, {
  isChangeRetroactive
} from '../../common/RetroactiveConfirmation'

import IncomeTable, {
  IncomeTableData,
  tableDataFromIncomeFields
} from './IncomeTable'

const deleteAttachmentResult = wrapResult(deleteAttachment)

const ButtonsContainer = styled(FixedSpaceRow)`
  margin: 20px 0;
`

export interface IncomeForm {
  effect: IncomeEffect
  data: IncomeTableData
  isEntrepreneur: boolean
  worksAtECHA: boolean
  validFrom: LocalDate
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
  validFrom: LocalDate.todayInSystemTz(),
  validTo: null,
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
    monthlyAmount: Math.round(parsed * multiplier),
    multiplier
  }
}

function updateIncomeData(
  data: IncomeTableData,
  coefficientMultipliers: Record<IncomeCoefficient, number>
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
  coefficientMultipliers: Record<IncomeCoefficient, number>,
  personId: UUID
): IncomeRequest | undefined {
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

  return { ...form, data: result, personId }
}

interface CommonProps {
  personId: UUID
  incomeTypeOptions: IncomeTypeOptions
  coefficientMultipliers: Record<IncomeCoefficient, number>
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
            to || from.isEqual(prevValidFrom) ? to : from.addYears(1).subDays(1)
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
      {isUpdate(props) ? (
        <>
          <Gap size="L" />
          <ListGrid labelWidth="fit-content(40%)" rowGap="xs" columnGap="L">
            <Label>{i18n.personProfile.income.details.updated}</Label>
            <span>{props.baseIncome.updatedAt.toLocalDate().format()}</span>

            <Label>{i18n.personProfile.income.details.handler}</Label>
            <span>{props.baseIncome.updatedBy}</span>
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
      (await deleteAttachmentResult({ attachmentId: id })).map(() => {
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
