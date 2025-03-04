// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useImperativeHandle, useMemo, useRef } from 'react'
import styled from 'styled-components'

import { Result } from 'lib-common/api'
import {
  required,
  validate,
  validateIf,
  validInt
} from 'lib-common/form-validation'
import {
  IncomeStatementStatus,
  OtherIncome,
  otherIncomes
} from 'lib-common/generated/api-types/incomestatement'
import { IncomeStatementId } from 'lib-common/generated/api-types/shared'
import * as Form from 'lib-common/income-statements/form'
import LocalDate from 'lib-common/local-date'
import { scrollToRef } from 'lib-common/utils/scrolling'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { Button } from 'lib-components/atoms/buttons/Button'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import Radio from 'lib-components/atoms/form/Radio'
import TextArea from 'lib-components/atoms/form/TextArea'
import { tabletMin } from 'lib-components/breakpoints'
import Container, { ContentArea } from 'lib-components/layout/Container'
import ListGrid from 'lib-components/layout/ListGrid'
import {
  FixedSpaceColumn,
  FixedSpaceFlexWrap,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { AlertBox, InfoBox } from 'lib-components/molecules/MessageBoxes'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import {
  fontWeights,
  H1,
  H2,
  H3,
  H4,
  Label,
  P
} from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

import Footer from '../Footer'
import { errorToInputInfo } from '../input-info-helper'
import { useLang, useTranslation } from '../localization'

import {
  IncomeStatementUntypedAttachments,
  IncomeStatementMissingAttachments,
  AttachmentHandler,
  AttachmentSection,
  useAttachmentHandler
} from './IncomeStatementAttachments'
import {
  ActionContainer,
  AssureCheckbox,
  identity,
  IncomeStatementFormAPI,
  LabelError,
  LabelWithError,
  SetStateCallback,
  useFieldDispatch,
  useFieldSetState
} from './IncomeStatementComponents'

interface Props {
  incomeStatementId: IncomeStatementId | undefined
  status: IncomeStatementStatus
  formData: Form.IncomeStatementForm
  showFormErrors: boolean
  otherStartDates: LocalDate[]
  draftSaveEnabled: boolean
  onChange: SetStateCallback<Form.IncomeStatementForm>
  onSave: (draft: boolean) => Promise<Result<unknown>> | undefined
  onSuccess: () => void
  onCancel: () => void
}

export default React.memo(
  React.forwardRef(function IncomeStatementForm(
    {
      incomeStatementId,
      status,
      formData,
      showFormErrors,
      otherStartDates,
      draftSaveEnabled,
      onChange,
      onSave,
      onSuccess,
      onCancel
    }: Props,
    ref: React.ForwardedRef<IncomeStatementFormAPI>
  ) {
    const t = useTranslation()
    const onGrossChange = useFieldSetState(onChange, 'gross')
    const onEntrepreneurChange = useFieldSetState(onChange, 'entrepreneur')
    const scrollTarget = useRef<HTMLElement>(null)

    const isValidStartDate = useCallback(
      (date: LocalDate) => otherStartDates.every((d) => !d.isEqual(date)),
      [otherStartDates]
    )

    const incomeTypeSelectionFormData = useMemo(
      () => ({
        startDate: formData.startDate,
        endDate: formData.endDate,
        highestFeeSelected: formData.highestFee,
        grossSelected: formData.gross.selected
      }),
      [
        formData.endDate,
        formData.gross.selected,
        formData.highestFee,
        formData.startDate
      ]
    )

    const otherIncomeFormData = useMemo(
      () => ({
        alimonyPayer: formData.alimonyPayer,
        student: formData.student,
        otherInfo: formData.otherInfo
      }),
      [formData.alimonyPayer, formData.otherInfo, formData.student]
    )

    const onSelectIncomeType = useCallback(
      (incomeType: 'highestFee' | 'gross') =>
        onChange((prev) =>
          incomeType === 'highestFee'
            ? {
                ...prev,
                highestFee: true,
                gross: { ...prev.gross, selected: false },
                entrepreneur: { ...prev.entrepreneur, selected: false }
              }
            : incomeType === 'gross'
              ? {
                  ...prev,
                  highestFee: false,
                  gross: { ...prev.gross, selected: true }
                }
              : (() => {
                  throw new Error('not reached')
                })()
        ),
      [onChange]
    )

    const onSelectEntrepreneur = useCallback(
      (value: boolean) =>
        onChange((prev) => ({
          ...prev,
          entrepreneur: { ...prev.entrepreneur, selected: value }
        })),
      [onChange]
    )

    useImperativeHandle(ref, () => ({
      scrollToErrors() {
        scrollToRef(scrollTarget)
      }
    }))

    const showOtherInfo =
      formData.gross.selected || formData.entrepreneur.selected

    const requiredAttachments = useMemo(
      () => Form.computeRequiredAttachments(formData),
      [formData]
    )

    const onAttachmentChange = useFieldSetState(onChange, 'attachments')
    const attachmentHandler = useAttachmentHandler(
      incomeStatementId,
      formData.attachments,
      onAttachmentChange
    )

    const sendButtonEnabled = useMemo(
      () =>
        formData.startDate &&
        (formData.highestFee ||
          ((formData.gross.selected || formData.entrepreneur.selected) &&
            formData.endDate &&
            formData.endDate <= formData.startDate.addYears(1))) &&
        formData.assure,
      [formData]
    )

    return (
      <>
        <Container>
          <Gap size="s" />
          <ContentArea opaque paddingVertical="L">
            <ResponsiveFixedSpaceRow>
              <FixedSpaceColumn spacing="zero">
                <H1 noMargin>{t.income.formTitle}</H1>
                {t.income.formDescription}
              </FixedSpaceColumn>
              <Confidential>{t.income.confidential}</Confidential>
            </ResponsiveFixedSpaceRow>
          </ContentArea>
          <Gap size="s" />
          <IncomeTypeSelection
            formData={incomeTypeSelectionFormData}
            isValidStartDate={isValidStartDate}
            showFormErrors={showFormErrors}
            onChange={onChange}
            onSelect={onSelectIncomeType}
            ref={scrollTarget}
          />
          {formData.gross.selected && (
            <>
              <Gap size="L" />
              <GrossIncomeSelection
                formData={formData.gross}
                showFormErrors={showFormErrors}
                onChange={onGrossChange}
                entrepreneurSelected={formData.entrepreneur.selected}
                onSelectEntrepreneur={onSelectEntrepreneur}
                attachmentHandler={attachmentHandler}
              />
            </>
          )}
          {formData.entrepreneur.selected && (
            <>
              <Gap size="L" />
              <EntrepreneurIncomeSelection
                formData={formData.entrepreneur}
                showFormErrors={showFormErrors}
                onChange={onEntrepreneurChange}
                attachmentHandler={attachmentHandler}
              />
            </>
          )}
          {showOtherInfo && (
            <>
              <Gap size="L" />
              <OtherInfo
                formData={otherIncomeFormData}
                onChange={onChange}
                showFormErrors={showFormErrors}
                attachmentHandler={attachmentHandler}
              />
              <Gap size="L" />
              {attachmentHandler ? (
                <IncomeStatementMissingAttachments
                  requiredAttachments={requiredAttachments}
                  attachmentHandler={attachmentHandler}
                />
              ) : (
                <ContentArea opaque paddingVertical="L">
                  <FixedSpaceColumn spacing="zero">
                    <H3 noMargin>{t.income.attachments.title}</H3>
                    <Gap size="s" />
                    <P noMargin>{t.income.attachments.description}</P>
                    <Gap size="L" />
                    {requiredAttachments.size > 0 && (
                      <>
                        <H4 noMargin>{t.income.attachments.required.title}</H4>
                        <Gap size="s" />
                        <UnorderedList data-qa="required-attachments">
                          {[...requiredAttachments].map((attachmentType) => (
                            <li key={attachmentType}>
                              {
                                t.income.attachments.attachmentNames[
                                  attachmentType
                                ]
                              }
                            </li>
                          ))}
                        </UnorderedList>
                        <Gap size="L" />
                      </>
                    )}
                    <IncomeStatementUntypedAttachments
                      incomeStatementId={incomeStatementId}
                      attachments={formData.attachments}
                      onChange={onAttachmentChange}
                    />
                  </FixedSpaceColumn>
                </ContentArea>
              )}
            </>
          )}
          <Gap />
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
              {status === 'DRAFT' && draftSaveEnabled && (
                <AsyncButton
                  text={t.income.saveAsDraft}
                  onClick={() => onSave(true)}
                  onSuccess={onSuccess}
                  data-qa="save-draft-btn"
                />
              )}
              <AsyncButton
                text={status === 'DRAFT' ? t.income.send : t.income.updateSent}
                primary
                onClick={() => onSave(false)}
                disabled={!sendButtonEnabled}
                onSuccess={onSuccess}
              />
            </FixedSpaceRow>
          </ActionContainer>
        </Container>
        <Footer />
      </>
    )
  })
)

function useSelectIncomeType(
  onSelect: (incomeType: 'highestFee' | 'gross') => void,
  incomeType: 'highestFee' | 'gross'
) {
  return useCallback(() => onSelect(incomeType), [onSelect, incomeType])
}

interface IncomeTypeSelectionData {
  startDate: LocalDate | null
  endDate: LocalDate | null
  highestFeeSelected: boolean
  grossSelected: boolean
}

const IncomeTypeSelection = React.memo(
  React.forwardRef(function IncomeTypeSelection(
    {
      formData,
      isValidStartDate,
      showFormErrors,
      onChange,
      onSelect
    }: {
      formData: IncomeTypeSelectionData
      isValidStartDate: (date: LocalDate) => boolean
      showFormErrors: boolean
      onChange: SetStateCallback<Form.IncomeStatementForm>
      onSelect: (incomeType: 'highestFee' | 'gross') => void
    },
    ref: React.ForwardedRef<HTMLElement>
  ) {
    const t = useTranslation()
    const [lang] = useLang()

    const onStartDateChange = useFieldDispatch(onChange, 'startDate')
    const onEndDateChange = useFieldDispatch(onChange, 'endDate')
    const onSelectHighestFee = useSelectIncomeType(onSelect, 'highestFee')
    const onSelectGross = useSelectIncomeType(onSelect, 'gross')

    const startDateInputInfo = useMemo(
      () =>
        errorToInputInfo(
          formData.startDate
            ? formData.startDate.isBefore(
                LocalDate.todayInSystemTz().subMonths(12)
              )
              ? 'dateTooEarly'
              : undefined
            : 'validDate',
          t.validationErrors
        ),
      [formData.startDate, t]
    )

    const isEndDateRequired = formData.grossSelected
    const invalidDateRange = useMemo(
      () =>
        isEndDateRequired && formData.endDate && formData.startDate
          ? formData.endDate > formData.startDate.addYears(1)
          : false,
      [isEndDateRequired, formData.startDate, formData.endDate]
    )

    const validateEndDate = useCallback(
      (endDate: LocalDate | null) => {
        const status = !formData.startDate
          ? undefined
          : endDate && formData.startDate > endDate
            ? 'validDate'
            : formData.highestFeeSelected
              ? undefined
              : isEndDateRequired && !endDate
                ? 'required'
                : undefined
        return status
      },
      [formData.highestFeeSelected, formData.startDate, isEndDateRequired]
    )

    const endDateInputInfo = useMemo(
      () =>
        errorToInputInfo(validateEndDate(formData.endDate), t.validationErrors),
      [formData, validateEndDate, t]
    )

    return (
      <ContentArea opaque paddingVertical="L" ref={ref}>
        <FixedSpaceColumn spacing="zero">
          <H2 noMargin data-qa="title">
            {t.income.incomeInfo}
          </H2>
          <Gap size="s" />
          {showFormErrors && (
            <>
              <AlertBox
                noMargin
                message={t.income.errors.invalidForm}
                data-qa="invalid-form"
              />
              <Gap size="s" />
            </>
          )}
          <P noMargin>{t.income.incomeInstructions}</P>
          <Gap size="s" />
          <FixedSpaceRow spacing="XL">
            <div>
              <Label htmlFor="start-date">
                {t.income.incomeType.startDate} *
              </Label>
              <Gap size="xs" />
              <DatePicker
                id="start-date"
                data-qa="income-start-date"
                date={formData.startDate}
                onChange={onStartDateChange}
                info={startDateInputInfo}
                hideErrorsBeforeTouched={!showFormErrors}
                locale={lang}
                required={true}
                isInvalidDate={(d) =>
                  isValidStartDate(d)
                    ? null
                    : t.validationErrors.unselectableDate
                }
              />
            </div>
            <div>
              <Label htmlFor="end-date">{`${t.income.incomeType.endDate}${isEndDateRequired ? ' *' : ''}`}</Label>
              <Gap size="xs" />
              <DatePicker
                id="end-date"
                data-qa="income-end-date"
                date={formData.endDate}
                onChange={onEndDateChange}
                minDate={formData.startDate ?? undefined}
                hideErrorsBeforeTouched={false}
                locale={lang}
                info={endDateInputInfo}
                isInvalidDate={(d) =>
                  errorToInputInfo(validateEndDate(d), t.validationErrors)
                    ?.text || null
                }
                required={isEndDateRequired}
              />
            </div>
          </FixedSpaceRow>
          {invalidDateRange && (
            <>
              <Gap size="s" />
              <InfoBox
                message={t.income.errors.dateRangeInvalid}
                thin
                data-qa="date-range-info"
              />
            </>
          )}
          <Gap size="L" />
          <H3 noMargin>{t.income.incomeType.title}</H3>
          <Gap size="s" />
          <P noMargin>{t.income.incomeType.description}</P>
          <Gap size="s" />
          <Radio
            label={t.income.incomeType.agreeToHighestFee}
            data-qa="highest-fee-checkbox"
            checked={formData.highestFeeSelected}
            onChange={onSelectHighestFee}
          />
          {formData.highestFeeSelected && (
            <>
              <Gap size="s" />
              <HighestFeeInfo>
                {t.income.incomeType.highestFeeInfo}
              </HighestFeeInfo>
            </>
          )}
          <Gap size="s" />
          <Radio
            label={t.income.incomeType.grossIncome}
            checked={formData.grossSelected}
            data-qa="gross-income-checkbox"
            onChange={onSelectGross}
          />
        </FixedSpaceColumn>
      </ContentArea>
    )
  })
)

const GrossIncomeSelection = React.memo(function GrossIncomeSelection({
  formData,
  showFormErrors,
  onChange,
  entrepreneurSelected,
  onSelectEntrepreneur,
  attachmentHandler
}: {
  formData: Form.Gross
  showFormErrors: boolean
  onChange: SetStateCallback<Form.Gross>
  entrepreneurSelected: boolean | null
  onSelectEntrepreneur: (value: boolean) => void
  attachmentHandler: AttachmentHandler | undefined
}) {
  const t = useTranslation()

  const onIncomeSourceChange = useFieldDispatch(onChange, 'incomeSource')
  const onNoIncomeDescriptionChange = useFieldDispatch(
    onChange,
    'noIncomeDescription'
  )
  const onEstimatedMonthlyIncomeChange = useFieldDispatch(
    onChange,
    'estimatedMonthlyIncome'
  )
  const onOtherIncomeChange = useFieldDispatch(onChange, 'otherIncome')
  const onOtherIncomeInfoChange = useFieldDispatch(onChange, 'otherIncomeInfo')

  const entrepreneurYes = useCallback(
    () => onSelectEntrepreneur(true),
    [onSelectEntrepreneur]
  )
  const entrepreneurNo = useCallback(
    () => onSelectEntrepreneur(false),
    [onSelectEntrepreneur]
  )

  return (
    <ContentArea opaque paddingVertical="L">
      <FixedSpaceColumn spacing="zero">
        <H2 noMargin>{t.income.grossIncome.title}</H2>
        <Gap size="m" />
        {t.income.grossIncome.description}
        <Gap size="m" />
        <LabelWithError
          label={`${t.income.grossIncome.incomeSource} *`}
          showError={showFormErrors && formData.incomeSource === null}
          errorText={t.income.errors.choose}
        />
        <Gap size="s" />
        <Radio
          label={t.income.grossIncome.incomesRegisterConsent}
          data-qa="incomes-register-consent-checkbox"
          checked={formData.incomeSource === 'INCOMES_REGISTER'}
          onChange={() => onIncomeSourceChange('INCOMES_REGISTER')}
        />
        <Gap size="s" />
        <Radio
          label={t.income.grossIncome.provideAttachments}
          checked={formData.incomeSource === 'ATTACHMENTS'}
          onChange={() => onIncomeSourceChange('ATTACHMENTS')}
        />
        <Gap size="s" />
        <Radio
          label={t.income.grossIncome.noIncome}
          checked={formData.incomeSource === 'NO_INCOME'}
          onChange={() => onIncomeSourceChange('NO_INCOME')}
        />
        <Gap size="s" />
        <InfoBox
          message={t.income.grossIncome.attachmentsVerificationInfo}
          thin
        />
        {formData.incomeSource === 'NO_INCOME' && (
          <>
            <Gap size="m" />
            <Label htmlFor="no-income-description">
              {t.income.grossIncome.noIncomeDescription} *
            </Label>
            <Gap size="s" />
            <InputField
              id="no-income-description"
              data-qa="no-income-description"
              value={formData.noIncomeDescription}
              onChange={onNoIncomeDescriptionChange}
              hideErrorsBeforeTouched={!showFormErrors}
              info={errorToInputInfo(
                validate(formData.noIncomeDescription, required),
                t.validationErrors
              )}
            />
          </>
        )}
        {formData.incomeSource === 'ATTACHMENTS' && (
          <>
            <Gap size="s" />
            <AttachmentSection
              attachmentType="PAYSLIP_GROSS"
              attachmentHandler={attachmentHandler}
              showFormErrors={showFormErrors}
            />
          </>
        )}
        {(formData.incomeSource === 'INCOMES_REGISTER' ||
          formData.incomeSource === 'ATTACHMENTS') && (
          <>
            <Gap size="L" />
            <Label>{t.income.grossIncome.estimate}</Label>
            <Gap size="m" />
            <FixedSpaceRow>
              <FixedSpaceColumn>
                <LightLabel htmlFor="estimated-monthly-income">
                  {t.income.grossIncome.estimatedMonthlyIncome} *
                </LightLabel>
                <InputField
                  id="estimated-monthly-income"
                  data-qa="gross-monthly-income-estimate"
                  value={formData.estimatedMonthlyIncome}
                  onChange={onEstimatedMonthlyIncomeChange}
                  hideErrorsBeforeTouched={!showFormErrors}
                  info={errorToInputInfo(
                    validate(
                      formData.estimatedMonthlyIncome,
                      required,
                      validInt
                    ),
                    t.validationErrors
                  )}
                />
              </FixedSpaceColumn>
            </FixedSpaceRow>
            <Gap size="L" />
            <Label>{t.income.grossIncome.otherIncome}</Label>
            <Gap size="s" />
            {t.income.grossIncome.otherIncomeDescription}
            <Gap size="s" />
            <OtherIncomeWrapper>
              <MultiSelect
                value={formData.otherIncome}
                options={otherIncomes}
                getOptionId={identity}
                getOptionLabel={(option: OtherIncome) =>
                  t.income.grossIncome.otherIncomeTypes[option]
                }
                onChange={onOtherIncomeChange}
                placeholder={t.income.grossIncome.choosePlaceholder}
              />
            </OtherIncomeWrapper>
            {formData.otherIncome.map((incomeType) => (
              <AttachmentSection
                key={incomeType}
                attachmentType={incomeType}
                showFormErrors={showFormErrors}
                attachmentHandler={attachmentHandler}
              />
            ))}
            {formData.otherIncome.length > 0 && (
              <>
                <Gap size="s" />
                <Label>{t.income.grossIncome.otherIncomeInfoLabel}</Label>
                <Gap size="s" />
                <P noMargin>
                  {t.income.grossIncome.otherIncomeInfoDescription}
                </P>
                <Gap size="s" />
                <InputField
                  value={formData.otherIncomeInfo}
                  onChange={onOtherIncomeInfoChange}
                />
              </>
            )}
          </>
        )}
        <Gap size="L" />
        <LabelWithError
          label={`${t.income.entrepreneurIncome.entrepreneurSelectTitle} *`}
          showError={showFormErrors && entrepreneurSelected === null}
          errorText={t.income.errors.choose}
        />
        <Gap size="s" />
        <Radio
          label={t.income.entrepreneurIncome.entrepreneurYes}
          checked={entrepreneurSelected === true}
          onChange={entrepreneurYes}
        />
        <Gap size="s" />
        <Radio
          label={t.income.entrepreneurIncome.entrepreneurNo}
          checked={entrepreneurSelected === false}
          onChange={entrepreneurNo}
        />
      </FixedSpaceColumn>
    </ContentArea>
  )
})

const EntrepreneurIncomeSelection = React.memo(
  function EntrepreneurIncomeSelection({
    formData,
    showFormErrors,
    onChange,
    attachmentHandler
  }: {
    formData: Form.Entrepreneur
    showFormErrors: boolean
    onChange: SetStateCallback<Form.Entrepreneur>
    attachmentHandler: AttachmentHandler | undefined
  }) {
    const t = useTranslation()
    const [lang] = useLang()

    const onFullTimeChange = useFieldDispatch(onChange, 'fullTime')
    const onStartOfEntrepreneurshipChange = useFieldDispatch(
      onChange,
      'startOfEntrepreneurship'
    )
    const onCompanyNameChange = useFieldDispatch(onChange, 'companyName')
    const onBusinessIdChange = useFieldDispatch(onChange, 'businessId')
    const onSpouseWorksInCompanyChange = useFieldDispatch(
      onChange,
      'spouseWorksInCompany'
    )
    const onStartupGrantChange = useFieldDispatch(onChange, 'startupGrant')
    const onCheckupConsentChange = useFieldDispatch(onChange, 'checkupConsent')
    const onSelfEmployedSelectedChange = useCallback(
      (value: boolean) =>
        onChange((prev) => ({
          ...prev,
          selfEmployed: { ...prev.selfEmployed, selected: value }
        })),
      [onChange]
    )
    const onSelfEmployedChange = useFieldSetState(onChange, 'selfEmployed')
    const onLimitedCompanySelectedChange = useCallback(
      (value: boolean) =>
        onChange((prev) => ({
          ...prev,
          limitedCompany: {
            ...prev.limitedCompany,
            selected: value,
            ...(value ? {} : { incomeSource: null })
          }
        })),
      [onChange]
    )
    const onLimitedCompanyChange = useFieldSetState(onChange, 'limitedCompany')
    const onPartnershipChange = useFieldDispatch(onChange, 'partnership')
    const onLightEntrepreneurChange = useFieldDispatch(
      onChange,
      'lightEntrepreneur'
    )
    const onAccountantChange = useFieldSetState(onChange, 'accountant')

    return (
      <ContentArea opaque paddingVertical="L">
        <FixedSpaceColumn spacing="zero">
          <H2 noMargin>{t.income.entrepreneurIncome.title}</H2>
          <Gap size="s" />
          <P noMargin>{t.income.entrepreneurIncome.description}</P>
          <Gap size="L" />
          <LabelWithError
            label={`${t.income.entrepreneurIncome.fullTimeLabel} *`}
            showError={showFormErrors && formData.fullTime === null}
            errorText={t.income.errors.choose}
          />
          <Gap size="s" />
          <Radio
            label={t.income.entrepreneurIncome.fullTime}
            data-qa="entrepreneur-full-time-option"
            checked={formData.fullTime === true}
            onChange={() => onFullTimeChange(true)}
          />
          <Gap size="s" />
          <Radio
            label={t.income.entrepreneurIncome.partTime}
            data-qa="entrepreneur-part-time-option"
            checked={formData.fullTime === false}
            onChange={() => onFullTimeChange(false)}
          />
          <Gap size="L" />
          <Label htmlFor="entrepreneur-start-date">
            {t.income.entrepreneurIncome.startOfEntrepreneurship} *
          </Label>
          <Gap size="s" />
          <DatePicker
            id="entrepreneur-start-date"
            data-qa="entrepreneur-start-date"
            date={formData.startOfEntrepreneurship}
            onChange={onStartOfEntrepreneurshipChange}
            locale={lang}
            info={errorToInputInfo(
              formData.startOfEntrepreneurship ? undefined : 'validDate',
              t.validationErrors
            )}
            hideErrorsBeforeTouched={!showFormErrors}
          />
          <Gap size="L" />
          <Label htmlFor="entrepreneur-company-name">
            {t.income.entrepreneurIncome.companyName}
          </Label>
          <Gap size="s" />
          <FixedSpaceRow>
            <FixedSpaceColumn>
              <InputField
                id="entrepreneur-company-name"
                value={formData.companyName}
                onChange={onCompanyNameChange}
              />
            </FixedSpaceColumn>
          </FixedSpaceRow>
          <Gap size="L" />
          <Label htmlFor="entrepreneur-business-id">
            {t.income.entrepreneurIncome.businessId}
          </Label>
          <Gap size="s" />
          <FixedSpaceRow>
            <FixedSpaceColumn>
              <InputField
                id="entrepreneur-business-id"
                value={formData.businessId}
                onChange={onBusinessIdChange}
              />
            </FixedSpaceColumn>
          </FixedSpaceRow>
          <Gap size="L" />
          <LabelWithError
            label={`${t.income.entrepreneurIncome.spouseWorksInCompany} *`}
            showError={showFormErrors && formData.spouseWorksInCompany === null}
            errorText={t.income.errors.choose}
          />
          <Gap size="s" />
          <Radio
            label={t.income.entrepreneurIncome.yes}
            data-qa="entrepreneur-spouse-yes"
            checked={formData.spouseWorksInCompany === true}
            onChange={() => onSpouseWorksInCompanyChange(true)}
          />
          <Gap size="s" />
          <Radio
            label={t.income.entrepreneurIncome.no}
            data-qa="entrepreneur-spouse-no"
            checked={formData.spouseWorksInCompany === false}
            onChange={() => onSpouseWorksInCompanyChange(false)}
          />
          <Gap size="L" />
          <Label>{t.income.entrepreneurIncome.startupGrantLabel}</Label>
          <Gap size="s" />
          <Checkbox
            label={t.income.entrepreneurIncome.startupGrant}
            data-qa="entrepreneur-startup-grant"
            checked={formData.startupGrant}
            onChange={onStartupGrantChange}
          />
          {formData.startupGrant && (
            <AttachmentSection
              attachmentType="STARTUP_GRANT"
              showFormErrors={showFormErrors}
              attachmentHandler={attachmentHandler}
            />
          )}
          <Gap size="L" />
          <Label>{t.income.entrepreneurIncome.checkupLabel}</Label>
          <Gap size="s" />
          <Checkbox
            label={t.income.entrepreneurIncome.checkupConsent}
            data-qa="entrepreneur-checkup-consent"
            checked={formData.checkupConsent}
            onChange={onCheckupConsentChange}
          />
          <Gap size="XL" />
          <H3 noMargin>{t.income.entrepreneurIncome.companyInfo}</H3>
          <Gap size="L" />
          <LabelWithError
            label={`${t.income.entrepreneurIncome.companyForm} *`}
            showError={
              showFormErrors &&
              !formData.selfEmployed.selected &&
              !formData.limitedCompany.selected &&
              !formData.partnership &&
              !formData.lightEntrepreneur
            }
            errorText={t.income.errors.chooseAtLeastOne}
          />
          <Gap size="s" />
          <Checkbox
            label={t.income.entrepreneurIncome.selfEmployed}
            data-qa="entrepreneur-self-employed"
            checked={formData.selfEmployed.selected}
            onChange={onSelfEmployedSelectedChange}
          />
          {formData.selfEmployed.selected && (
            <>
              <Gap size="s" />
              <SelfEmployedIncomeSelection
                formData={formData.selfEmployed}
                showFormErrors={showFormErrors}
                onChange={onSelfEmployedChange}
                attachmentHandler={attachmentHandler}
              />
            </>
          )}
          <Gap size="m" />
          <Checkbox
            label={t.income.entrepreneurIncome.limitedCompany}
            data-qa="entrepreneur-llc"
            checked={formData.limitedCompany.selected}
            onChange={onLimitedCompanySelectedChange}
          />
          {formData.limitedCompany.selected && (
            <>
              <Gap size="s" />
              <LimitedCompanyIncomeSelection
                formData={formData.limitedCompany}
                showFormErrors={showFormErrors}
                onChange={onLimitedCompanyChange}
                attachmentHandler={attachmentHandler}
              />
            </>
          )}
          <Gap size="m" />
          <Checkbox
            label={t.income.entrepreneurIncome.partnership}
            data-qa="entrepreneur-partnership"
            checked={formData.partnership}
            onChange={onPartnershipChange}
          />
          {formData.partnership && (
            <>
              <Gap size="s" />
              <Indent>
                <P noMargin>{t.income.entrepreneurIncome.partnershipInfo}</P>
                <AttachmentSection
                  attachmentType="PROFIT_AND_LOSS_STATEMENT_PARTNERSHIP"
                  showFormErrors={showFormErrors}
                  attachmentHandler={attachmentHandler}
                />
                <AttachmentSection
                  attachmentType="ACCOUNTANT_REPORT_PARTNERSHIP"
                  showFormErrors={showFormErrors}
                  attachmentHandler={attachmentHandler}
                />
              </Indent>
            </>
          )}
          <Gap size="m" />
          <Checkbox
            label={t.income.entrepreneurIncome.lightEntrepreneur}
            data-qa="entrepreneur-light-entrepreneur"
            checked={formData.lightEntrepreneur}
            onChange={onLightEntrepreneurChange}
          />
          {formData.lightEntrepreneur && (
            <>
              <Gap size="s" />
              <Indent>
                <P noMargin>
                  {t.income.entrepreneurIncome.lightEntrepreneurInfo}
                </P>
                <AttachmentSection
                  attachmentType="SALARY"
                  showFormErrors={showFormErrors}
                  attachmentHandler={attachmentHandler}
                />
              </Indent>
            </>
          )}
          {(formData.limitedCompany.selected ||
            formData.selfEmployed.selected ||
            formData.partnership) && (
            <>
              <Gap size="L" />
              <Accounting
                formData={formData.accountant}
                showFormErrors={showFormErrors}
                onChange={onAccountantChange}
              />
            </>
          )}
        </FixedSpaceColumn>
      </ContentArea>
    )
  }
)

const SelfEmployedIncomeSelection = React.memo(
  function SelfEmployedIncomeSelection({
    formData,
    showFormErrors,
    onChange,
    attachmentHandler
  }: {
    formData: Form.SelfEmployed
    showFormErrors: boolean
    onChange: SetStateCallback<Form.SelfEmployed>
    attachmentHandler: AttachmentHandler | undefined
  }) {
    const t = useTranslation()
    const [lang] = useLang()

    const onAttachmentsChange = useFieldDispatch(onChange, 'attachments')
    const onEstimationChange = useFieldDispatch(onChange, 'estimation')
    const onEstimatedMonthlyIncomeChange = useFieldDispatch(
      onChange,
      'estimatedMonthlyIncome'
    )
    const onIncomeStartDateChange = useFieldDispatch(
      onChange,
      'incomeStartDate'
    )
    const onIncomeEndDateChange = useFieldDispatch(onChange, 'incomeEndDate')

    return (
      <Indent>
        <FixedSpaceColumn>
          <P noMargin>{t.income.selfEmployed.info}</P>
          {showFormErrors && !formData.attachments && !formData.estimation && (
            <LabelError text={t.income.errors.chooseAtLeastOne} />
          )}
          <Checkbox
            label={t.income.selfEmployed.attachments}
            data-qa="self-employed-attachments"
            checked={formData.attachments}
            onChange={onAttachmentsChange}
          />
          {formData.attachments && (
            <AttachmentSection
              attachmentType="PROFIT_AND_LOSS_STATEMENT_SELF_EMPLOYED"
              showFormErrors={showFormErrors}
              attachmentHandler={attachmentHandler}
              dense
            />
          )}
          <Checkbox
            label={t.income.selfEmployed.estimatedIncome}
            data-qa="self-employed-estimated-income"
            checked={formData.estimation}
            onChange={onEstimationChange}
          />
          <Indent>
            <FixedSpaceFlexWrap>
              <FixedSpaceColumn>
                <Label htmlFor="estimated-monthly-income">
                  {t.income.selfEmployed.estimatedMonthlyIncome}
                </Label>
                <InputField
                  id="estimated-monthly-income"
                  value={formData.estimatedMonthlyIncome}
                  onFocus={() => onEstimationChange(true)}
                  onChange={onEstimatedMonthlyIncomeChange}
                  hideErrorsBeforeTouched={!showFormErrors}
                  info={errorToInputInfo(
                    validateIf(
                      formData.estimation,
                      formData.estimatedMonthlyIncome,
                      required,
                      validInt
                    ),
                    t.validationErrors
                  )}
                />
              </FixedSpaceColumn>
              <FixedSpaceColumn>
                <Label htmlFor="income-start-date">
                  {t.income.selfEmployed.timeRange}
                </Label>
                <FixedSpaceRow>
                  <DatePicker
                    id="income-start-date"
                    date={formData.incomeStartDate}
                    onFocus={() => onEstimationChange(true)}
                    onChange={onIncomeStartDateChange}
                    locale={lang}
                    hideErrorsBeforeTouched={!showFormErrors}
                    info={errorToInputInfo(
                      validateIf(
                        formData.estimation,
                        formData.incomeStartDate,
                        required
                      ),
                      t.validationErrors
                    )}
                  />
                  <span>{' - '}</span>
                  <DatePicker
                    date={formData.incomeEndDate}
                    onFocus={() => onEstimationChange(true)}
                    onChange={onIncomeEndDateChange}
                    locale={lang}
                    minDate={formData.incomeStartDate ?? undefined}
                    hideErrorsBeforeTouched={!showFormErrors}
                  />
                </FixedSpaceRow>
              </FixedSpaceColumn>
            </FixedSpaceFlexWrap>
          </Indent>
        </FixedSpaceColumn>
      </Indent>
    )
  }
)

const LimitedCompanyIncomeSelection = React.memo(
  function LimitedCompanyIncomeSelection({
    formData,
    showFormErrors,
    onChange,
    attachmentHandler
  }: {
    formData: Form.LimitedCompany
    showFormErrors: boolean
    onChange: SetStateCallback<Form.LimitedCompany>
    attachmentHandler: AttachmentHandler | undefined
  }) {
    const t = useTranslation()

    const onIncomeSourceChange = useFieldDispatch(onChange, 'incomeSource')

    return (
      <Indent>
        <FixedSpaceColumn>
          <P noMargin>{t.income.limitedCompany.info}</P>
          {showFormErrors && formData.incomeSource === null && (
            <LabelError text={t.income.errors.choose} />
          )}
          <Gap size="xs" />
          <AttachmentSection
            attachmentType="ACCOUNTANT_REPORT_LLC"
            showFormErrors={showFormErrors}
            attachmentHandler={attachmentHandler}
            dense
          />
          <Radio
            label={t.income.limitedCompany.incomesRegister}
            data-qa="llc-incomes-register"
            checked={formData.incomeSource === 'INCOMES_REGISTER'}
            onChange={() => onIncomeSourceChange('INCOMES_REGISTER')}
          />
          <Radio
            label={t.income.limitedCompany.attachments}
            data-qa="llc-attachments"
            checked={formData.incomeSource === 'ATTACHMENTS'}
            onChange={() => onIncomeSourceChange('ATTACHMENTS')}
          />
          {formData.incomeSource === 'ATTACHMENTS' && (
            <AttachmentSection
              attachmentType="PAYSLIP_LLC"
              showFormErrors={showFormErrors}
              attachmentHandler={attachmentHandler}
              dense
            />
          )}
        </FixedSpaceColumn>
      </Indent>
    )
  }
)

const Accounting = React.memo(function Accounting({
  formData,
  showFormErrors,
  onChange
}: {
  formData: Form.Accountant
  showFormErrors: boolean
  onChange: SetStateCallback<Form.Accountant>
}) {
  const tr = useTranslation()
  const t = tr.income.accounting

  const onNameChange = useFieldDispatch(onChange, 'name')
  const onPhoneChange = useFieldDispatch(onChange, 'phone')
  const onEmailChange = useFieldDispatch(onChange, 'email')
  const onAddressChange = useFieldDispatch(onChange, 'address')

  return (
    <>
      <H3 noMargin>{t.title}</H3>
      <Gap size="s" />
      <ListGrid>
        <Label>{t.accountant} *</Label>
        <InputField
          placeholder={t.accountantPlaceholder}
          data-qa="accountant-name"
          width="L"
          value={formData.name}
          onChange={onNameChange}
          hideErrorsBeforeTouched={!showFormErrors}
          info={errorToInputInfo(
            validate(formData.name, required),
            tr.validationErrors
          )}
        />

        <Label>{t.phone} *</Label>
        <InputField
          placeholder={t.phonePlaceholder}
          data-qa="accountant-phone"
          width="L"
          value={formData.phone}
          onChange={onPhoneChange}
          hideErrorsBeforeTouched={!showFormErrors}
          info={errorToInputInfo(
            validate(formData.phone, required),
            tr.validationErrors
          )}
        />

        <Label>{t.email} *</Label>
        <InputField
          placeholder={t.emailPlaceholder}
          data-qa="accountant-email"
          width="L"
          value={formData.email}
          onChange={onEmailChange}
          hideErrorsBeforeTouched={!showFormErrors}
          info={errorToInputInfo(
            validate(formData.email, required),
            tr.validationErrors
          )}
        />

        <Label>{t.address}</Label>
        <InputField
          placeholder={t.addressPlaceholder}
          width="L"
          value={formData.address}
          onChange={onAddressChange}
        />
      </ListGrid>
    </>
  )
})

interface OtherInfoFormData {
  alimonyPayer: boolean
  student: boolean
  otherInfo: string
}

const OtherInfo = React.memo(function OtherInfo({
  formData,
  onChange,
  showFormErrors,
  attachmentHandler
}: {
  formData: OtherInfoFormData
  onChange: SetStateCallback<Form.IncomeStatementForm>
  showFormErrors: boolean
  attachmentHandler: AttachmentHandler | undefined
}) {
  const t = useTranslation()

  const onStudentChange = useFieldDispatch(onChange, 'student')
  const onAlimonyPayerChange = useFieldDispatch(onChange, 'alimonyPayer')
  const onOtherInfoChange = useFieldDispatch(onChange, 'otherInfo')

  return (
    <ContentArea opaque paddingVertical="L">
      <FixedSpaceColumn spacing="zero">
        <H2 noMargin>{t.income.moreInfo.title}</H2>
        <Gap size="m" />
        <Label>{t.income.moreInfo.studentLabel}</Label>
        <Gap size="s" />
        <Checkbox
          label={t.income.moreInfo.student}
          data-qa="student"
          checked={formData.student}
          onChange={onStudentChange}
        />
        {formData.student && (
          <AttachmentSection
            attachmentType="PROOF_OF_STUDIES"
            showFormErrors={showFormErrors}
            attachmentHandler={attachmentHandler}
          />
        )}
        <Gap size="s" />
        <P noMargin>{t.income.moreInfo.studentInfo}</P>
        <Gap size="L" />
        <Label>{t.income.moreInfo.deductions}</Label>
        <Gap size="s" />
        <Checkbox
          label={t.income.moreInfo.alimony}
          data-qa="alimony-payer"
          checked={formData.alimonyPayer}
          onChange={onAlimonyPayerChange}
        />
        {formData.alimonyPayer && (
          <AttachmentSection
            attachmentType="ALIMONY_PAYOUT"
            showFormErrors={showFormErrors}
            attachmentHandler={attachmentHandler}
          />
        )}
        <Gap size="L" />
        <Label htmlFor="more-info">{t.income.moreInfo.otherInfoLabel}</Label>
        <Gap size="s" />
        <TextArea
          id="more-info"
          value={formData.otherInfo}
          onChange={onOtherInfoChange}
        />
        <AttachmentSection
          attachmentType="OTHER"
          showFormErrors={showFormErrors}
          attachmentHandler={attachmentHandler}
          infoText={t.income.moreInfo.otherAttachmentInfo}
          optional
        />
      </FixedSpaceColumn>
    </ContentArea>
  )
})

const HighestFeeInfo = styled(P).attrs({ noMargin: true })`
  margin-left: ${defaultMargins.XL};
`

const LightLabel = styled(Label)`
  font-weight: ${fontWeights.normal};
`

const Indent = styled.div`
  width: 100%;
  padding-left: ${defaultMargins.s};
  @media (min-width: ${tabletMin}) {
    padding-left: ${defaultMargins.XL};
  }
`

const OtherIncomeWrapper = styled.div`
  max-width: 480px;
`

const ResponsiveFixedSpaceRow = styled(FixedSpaceRow)`
  @media (max-width: 900px) {
    display: block;
  }
`

const Confidential = styled.div`
  flex: 0 0 auto;
`
