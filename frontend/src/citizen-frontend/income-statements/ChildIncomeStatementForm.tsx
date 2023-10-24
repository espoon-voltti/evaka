// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useImperativeHandle, useMemo, useRef } from 'react'
import styled from 'styled-components'

import { Result } from 'lib-common/api'
import { Attachment } from 'lib-common/api-types/attachment'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { scrollToRef } from 'lib-common/utils/scrolling'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import TextArea from 'lib-components/atoms/form/TextArea'
import Container, { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { H1, H2, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { errorToInputInfo } from '../input-info-helper'
import { useLang, useTranslation } from '../localization'

import ChildIncomeStatementAttachments from './ChildIncomeStatementAttachments'
import {
  ActionContainer,
  AssureCheckbox,
  IncomeStatementFormAPI,
  LabelError,
  SetStateCallback,
  useFieldDispatch
} from './IncomeStatementComponents'
import * as Form from './types/form'

interface Props {
  incomeStatementId: UUID | undefined
  formData: Form.IncomeStatementForm
  showFormErrors: boolean
  otherStartDates: LocalDate[]
  onChange: SetStateCallback<Form.IncomeStatementForm>
  onSave: () => Promise<Result<unknown>> | undefined
  onSuccess: () => void
  onCancel: () => void
}

const OtherInfoContainer = styled.div`
  max-width: 716px;
`

const ChildIncome = React.memo(function ChildIncome({
  incomeStatementId,
  formData,
  onChange
}: {
  incomeStatementId: UUID | undefined
  formData: Form.IncomeStatementForm
  onChange: SetStateCallback<Form.IncomeStatementForm>
}) {
  const t = useTranslation()

  const onAttachmentUploaded = useCallback(
    (attachment: Attachment) =>
      onChange((prev) => ({
        ...prev,
        attachments: [...prev.attachments, attachment]
      })),
    [onChange]
  )

  const onAttachmentDeleted = useCallback(
    (id: UUID) =>
      onChange((prev) => ({
        ...prev,
        attachments: prev.attachments.filter((a) => a.id !== id)
      })),
    [onChange]
  )

  return (
    <>
      <Label>{t.income.childIncome.childAttachments}</Label>
      <Gap size="s" />
      {formData.childIncome && formData.attachments.length === 0 && (
        <>
          <LabelError text={t.components.fileUpload.input.title} />
          <Gap size="L" />
        </>
      )}
      <ChildIncomeStatementAttachments
        incomeStatementId={incomeStatementId}
        attachments={formData.attachments}
        onUploaded={onAttachmentUploaded}
        onDeleted={onAttachmentDeleted}
      />

      <Gap size="L" />

      <Label>{t.income.childIncome.additionalInfo}</Label>

      <OtherInfoContainer>
        <TextArea
          placeholder={t.income.childIncome.write}
          value={formData.otherInfo}
          onChange={useFieldDispatch(onChange, 'otherInfo')}
          data-qa="other-info"
        />
      </OtherInfoContainer>
    </>
  )
})

interface ChildIncomeTypeSelectionData {
  startDate: LocalDate | null
  endDate: LocalDate | null
  highestFeeSelected: boolean
  grossSelected: boolean
  entrepreneurSelected: boolean
}

// eslint-disable-next-line react/display-name
const ChildIncomeTimeRangeSelection = React.memo(
  React.forwardRef(function ChildIncomeTypeSelection(
    {
      formData,
      isValidStartDate,
      showFormErrors,
      onChange
    }: {
      formData: ChildIncomeTypeSelectionData
      isValidStartDate: (date: LocalDate) => boolean
      showFormErrors: boolean
      onChange: SetStateCallback<Form.IncomeStatementForm>
    },
    ref: React.ForwardedRef<HTMLDivElement>
  ) {
    const t = useTranslation()
    const [lang] = useLang()

    const startDateInputInfo = useMemo(
      () =>
        errorToInputInfo(
          formData.startDate ? undefined : 'validDate',
          t.validationErrors
        ),
      [formData.startDate, t]
    )

    return (
      <FixedSpaceColumn spacing="zero" ref={ref}>
        <H2 noMargin>{t.income.incomeInfo}</H2>
        <Gap size="s" />
        <Label>{t.income.childIncomeInfo}</Label>
        <Gap size="s" />
        {showFormErrors && (
          <>
            <AlertBox noMargin message={t.income.errors.invalidForm} />
            <Gap size="s" />
          </>
        )}
        <FixedSpaceRow spacing="XL">
          <div>
            <Label htmlFor="start-date">
              {t.income.incomeType.startDate} *
            </Label>
            <Gap size="xs" />
            <DatePicker
              id="start-date"
              date={formData.startDate}
              onChange={useFieldDispatch(onChange, 'startDate')}
              info={startDateInputInfo}
              hideErrorsBeforeTouched={!showFormErrors}
              locale={lang}
              isInvalidDate={(d) =>
                isValidStartDate(d) ? null : t.validationErrors.unselectableDate
              }
              data-qa="start-date"
            />
          </div>
          <div>
            <Label htmlFor="end-date">{t.income.incomeType.endDate}</Label>
            <Gap size="xs" />
            <DatePicker
              id="end-date"
              date={formData.endDate}
              onChange={useFieldDispatch(onChange, 'endDate')}
              minDate={formData.startDate ?? undefined}
              hideErrorsBeforeTouched
              locale={lang}
              data-qa="end-date"
            />
          </div>
        </FixedSpaceRow>
      </FixedSpaceColumn>
    )
  })
)

const ResponsiveFixedSpaceRow = styled(FixedSpaceRow)`
  @media (max-width: 900px) {
    display: block;
  }
`

const Confidential = styled.div`
  flex: 0 0 auto;
`

// eslint-disable-next-line react/display-name
export default React.memo(
  React.forwardRef(function ChildIncomeStatementForm(
    {
      incomeStatementId,
      formData,
      showFormErrors,
      otherStartDates,
      onChange,
      onSave,
      onSuccess,
      onCancel
    }: Props,
    ref: React.ForwardedRef<IncomeStatementFormAPI>
  ) {
    const t = useTranslation()
    const scrollTarget = useRef<HTMLDivElement>(null)

    const isValidStartDate = useCallback(
      (date: LocalDate) => otherStartDates.every((d) => !d.isEqual(date)),
      [otherStartDates]
    )

    const incomeTypeSelectionFormData = useMemo(
      () => ({
        startDate: formData.startDate,
        endDate: formData.endDate,
        highestFeeSelected: formData.highestFee,
        grossSelected: formData.gross.selected,
        entrepreneurSelected: formData.entrepreneur.selected
      }),
      [
        formData.endDate,
        formData.entrepreneur.selected,
        formData.gross.selected,
        formData.highestFee,
        formData.startDate
      ]
    )

    useImperativeHandle(ref, () => ({
      scrollToErrors() {
        scrollToRef(scrollTarget)
      }
    }))

    const saveButtonEnabled = formData.attachments.length > 0 && formData.assure

    return (
      <>
        <Container>
          <Gap size="s" />
          <ContentArea opaque paddingVertical="L">
            <ResponsiveFixedSpaceRow>
              <FixedSpaceColumn spacing="zero">
                <H1 noMargin>{t.income.childFormTitle}</H1>
                {t.income.childFormDescription}
              </FixedSpaceColumn>
              <Confidential>{t.income.confidential}</Confidential>
            </ResponsiveFixedSpaceRow>
          </ContentArea>

          <Gap size="s" />

          <ContentArea opaque>
            <ChildIncomeTimeRangeSelection
              formData={incomeTypeSelectionFormData}
              isValidStartDate={isValidStartDate}
              showFormErrors={showFormErrors}
              onChange={onChange}
              ref={scrollTarget}
            />
            <Gap size="L" />
            <ChildIncome
              incomeStatementId={incomeStatementId}
              formData={formData}
              onChange={onChange}
            />
          </ContentArea>
          <ActionContainer>
            <AssureCheckbox>
              <Checkbox
                label={t.income.assure}
                checked={formData.assure}
                data-qa="assure-checkbox"
                onChange={useFieldDispatch(onChange, 'assure')}
              />
            </AssureCheckbox>
            <FixedSpaceRow>
              <Button text={t.common.cancel} onClick={onCancel} />
              <AsyncButton
                text={t.common.save}
                primary
                onClick={onSave}
                disabled={!saveButtonEnabled}
                onSuccess={onSuccess}
                data-qa="save-btn"
              />
            </FixedSpaceRow>
          </ActionContainer>
        </Container>
      </>
    )
  })
)
