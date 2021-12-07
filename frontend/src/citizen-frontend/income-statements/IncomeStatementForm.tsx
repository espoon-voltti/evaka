// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useImperativeHandle, useMemo, useRef } from 'react'
import styled, { useTheme } from 'styled-components'
import { defaultMargins, Gap } from 'lib-components/white-space'
import {
  fontWeights,
  H1,
  H2,
  H3,
  H4,
  Label,
  P
} from 'lib-components/typography'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import { tabletMin } from 'lib-components/breakpoints'
import ListGrid from 'lib-components/layout/ListGrid'
import { useLang, useTranslation } from '../localization'
import Radio from 'lib-components/atoms/form/Radio'
import {
  FixedSpaceColumn,
  FixedSpaceFlexWrap,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import Button from 'lib-components/atoms/buttons/Button'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import AsyncButton, {
  AsyncClickCallback
} from 'lib-components/atoms/buttons/AsyncButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import InputField from 'lib-components/atoms/form/InputField'
import { AttachmentType } from './types/common'
import * as Form from './types/form'
import {
  required,
  validate,
  validateIf,
  validDate,
  validInt
} from 'lib-common/form-validation'
import FileUpload from 'lib-components/molecules/FileUpload'
import Container, { ContentArea } from 'lib-components/layout/Container'
import Footer from '../Footer'
import { Attachment } from 'lib-common/api-types/attachment'
import TextArea from 'lib-components/atoms/form/TextArea'
import { UUID } from 'lib-common/types'
import {
  deleteAttachment,
  getAttachmentBlob,
  saveIncomeStatementAttachment
} from '../attachments'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { fasExclamationTriangle } from 'lib-icons'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import LocalDate from 'lib-common/local-date'
import { otherIncome } from 'lib-common/api-types/incomeStatement'
import { errorToInputInfo } from '../input-info-helper'
import { OtherIncome } from 'lib-common/generated/enums'

const ActionContainer = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: ${defaultMargins.m} 0;

  > * {
    margin: 0 ${defaultMargins.m};

    &:not(:first-child) {
      margin-top: ${defaultMargins.s};
    }
  }

  @media (min-width: ${tabletMin}) {
    flex-direction: row;

    > * {
      margin: ${defaultMargins.s} ${defaultMargins.m};
    }
  }
`
const AssureCheckbox = styled.div`
  display: flex;
  align-items: center;
`

function identity<T>(value: T): T {
  return value
}

type SetStateCallback<T> = (fn: (prev: T) => T) => void

function useFieldSetState<T, K extends keyof T>(
  onChange: SetStateCallback<T>,
  key: K
): SetStateCallback<T[K]> {
  return useCallback<SetStateCallback<T[K]>>(
    (fn) => onChange((prev) => ({ ...prev, [key]: fn(prev[key]) })),
    [onChange, key]
  )
}

function useFieldDispatch<T, K extends keyof T>(
  onChange: SetStateCallback<T>,
  key: K
): (value: T[K]) => void {
  const setState = useFieldSetState(onChange, key)
  return useCallback((value: T[K]) => setState(() => value), [setState])
}

function useFieldSetter<T, K extends keyof T>(
  onChange: SetStateCallback<T>,
  key: K,
  value: T[K]
): () => void {
  const setState = useFieldSetState(onChange, key)
  return useCallback(() => setState(() => value), [setState, value])
}

interface Props {
  incomeStatementId: UUID | undefined
  formData: Form.IncomeStatementForm
  showFormErrors: boolean
  otherStartDates: LocalDate[]
  onChange: SetStateCallback<Form.IncomeStatementForm>
  onSave: AsyncClickCallback
  onSuccess: () => void
  onCancel: () => void
}

export interface IncomeStatementFormAPI {
  scrollToErrors: () => void
}

// eslint-disable-next-line react/display-name
export default React.memo(
  React.forwardRef(function IncomeStatementForm(
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

    const otherIncomeFormData = useMemo(
      () => ({
        alimonyPayer: formData.alimonyPayer,
        student: formData.student,
        otherInfo: formData.otherInfo
      }),
      [formData.alimonyPayer, formData.otherInfo, formData.student]
    )

    const onSelectIncomeType = useCallback(
      (incomeType: 'highestFee' | 'gross' | 'entrepreneur', value: boolean) =>
        onChange((prev) =>
          incomeType === 'highestFee'
            ? {
                ...prev,
                highestFee: value,
                gross: { ...prev.gross, selected: false },
                entrepreneur: { ...prev.entrepreneur, selected: false }
              }
            : incomeType === 'gross'
            ? {
                ...prev,
                gross: { ...prev.gross, selected: value }
              }
            : incomeType === 'entrepreneur'
            ? {
                ...prev,
                entrepreneur: { ...prev.entrepreneur, selected: value }
              }
            : (() => {
                throw new Error('not reached')
              })()
        ),
      [onChange]
    )

    useImperativeHandle(ref, () => ({
      scrollToErrors() {
        // Use requestAnimationFrame to make sure that the scroll target has been
        // added to the DOM
        requestAnimationFrame(() => {
          if (!scrollTarget.current) return
          const top =
            window.scrollY + scrollTarget.current.getBoundingClientRect().top
          window.scrollTo({ top, left: 0, behavior: 'smooth' })
        })
      }
    }))

    const showOtherInfo =
      formData.gross.selected || formData.entrepreneur.selected

    const requiredAttachments = useRequiredAttachments(formData)

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

    const saveButtonEnabled =
      (formData.highestFee ||
        formData.gross.selected ||
        formData.entrepreneur.selected) &&
      formData.assure

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
              />
            </>
          )}
          {showOtherInfo && (
            <>
              <Gap size="L" />
              <OtherInfo formData={otherIncomeFormData} onChange={onChange} />
              <Gap size="L" />
              <Attachments
                incomeStatementId={incomeStatementId}
                requiredAttachments={requiredAttachments}
                attachments={formData.attachments}
                onUploaded={onAttachmentUploaded}
                onDeleted={onAttachmentDeleted}
              />
            </>
          )}
          <ActionContainer>
            <AssureCheckbox>
              <Checkbox
                label={`${t.income.assure} *`}
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
  onSelect: (
    incomeType: 'highestFee' | 'gross' | 'entrepreneur',
    value: boolean
  ) => void,
  incomeType: 'highestFee' | 'gross' | 'entrepreneur'
) {
  return useCallback(
    (value: boolean) => onSelect(incomeType, value),
    [onSelect, incomeType]
  )
}

interface IncomeTypeSelectionData {
  startDate: string
  endDate: string
  highestFeeSelected: boolean
  grossSelected: boolean
  entrepreneurSelected: boolean
}

// eslint-disable-next-line react/display-name
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
      onSelect: (
        incomeType: 'highestFee' | 'gross' | 'entrepreneur',
        value: boolean
      ) => void
    },
    ref: React.ForwardedRef<HTMLElement>
  ) {
    const t = useTranslation()
    const [lang] = useLang()

    const onSelectHighestFee = useSelectIncomeType(onSelect, 'highestFee')
    const onSelectGross = useSelectIncomeType(onSelect, 'gross')
    const onSelectEntrepreneur = useSelectIncomeType(onSelect, 'entrepreneur')

    const startDateInputInfo = useMemo(
      () => errorToInputInfo(validDate(formData.startDate), t.validationErrors),
      [formData.startDate, t]
    )
    const isValidEndDate = useCallback(
      (date) => {
        const startDate = LocalDate.parseFiOrNull(formData.startDate)
        return startDate === null || startDate <= date
      },
      [formData.startDate]
    )
    const endDateInputInfo = useMemo(
      () =>
        errorToInputInfo(
          validateIf(formData.endDate != '', formData.endDate, validDate),
          t.validationErrors
        ),
      [formData.endDate, t]
    )

    return (
      <ContentArea opaque paddingVertical="L" ref={ref}>
        <FixedSpaceColumn spacing="zero">
          <H2 noMargin>{t.income.incomeInfo}</H2>
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
                hideErrorsBeforeTouched
                locale={lang}
                isValidDate={isValidStartDate}
              />
            </div>
            <div>
              <Label htmlFor="end-date">{t.income.incomeType.endDate}</Label>
              <Gap size="xs" />
              <DatePicker
                id="end-date"
                date={formData.endDate}
                onChange={useFieldDispatch(onChange, 'endDate')}
                isValidDate={isValidEndDate}
                info={endDateInputInfo}
                hideErrorsBeforeTouched
                locale={lang}
              />
            </div>
          </FixedSpaceRow>
          <Gap size="L" />
          <H3 noMargin>{t.income.incomeType.title}</H3>
          <Gap size="s" />
          <P noMargin>{t.income.incomeType.description}</P>
          <Gap size="s" />
          <Checkbox
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
          <Checkbox
            label={t.income.incomeType.grossIncome}
            checked={formData.grossSelected}
            data-qa="gross-income-checkbox"
            disabled={formData.highestFeeSelected}
            onChange={onSelectGross}
          />
          <Gap size="s" />
          <Checkbox
            label={t.income.incomeType.entrepreneurIncome}
            checked={formData.entrepreneurSelected}
            data-qa="entrepreneur-income-checkbox"
            disabled={formData.highestFeeSelected}
            onChange={onSelectEntrepreneur}
          />
        </FixedSpaceColumn>
      </ContentArea>
    )
  })
)

const GrossIncomeSelection = React.memo(function GrossIncomeSelection({
  formData,
  showFormErrors,
  onChange
}: {
  formData: Form.Gross
  showFormErrors: boolean
  onChange: SetStateCallback<Form.Gross>
}) {
  const t = useTranslation()
  const onOtherIncomeInfoChange = useFieldDispatch(onChange, 'otherIncomeInfo')
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
          label={t.income.incomesRegisterConsent}
          data-qa="incomes-register-consent-checkbox"
          checked={formData.incomeSource === 'INCOMES_REGISTER'}
          onChange={useFieldSetter(
            onChange,
            'incomeSource',
            'INCOMES_REGISTER'
          )}
        />
        <Gap size="s" />
        <Radio
          label={t.income.grossIncome.provideAttachments}
          checked={formData.incomeSource === 'ATTACHMENTS'}
          onChange={useFieldSetter(onChange, 'incomeSource', 'ATTACHMENTS')}
        />
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
              onChange={useFieldDispatch(onChange, 'estimatedMonthlyIncome')}
              hideErrorsBeforeTouched={!showFormErrors}
              info={errorToInputInfo(
                validate(formData.estimatedMonthlyIncome, required, validInt),
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
            options={otherIncome}
            getOptionId={identity}
            getOptionLabel={useCallback(
              (option: OtherIncome) =>
                t.income.grossIncome.otherIncomeTypes[option],
              [t]
            )}
            onChange={useFieldDispatch(onChange, 'otherIncome')}
            placeholder={t.income.grossIncome.choosePlaceholder}
          />
        </OtherIncomeWrapper>
        {formData.otherIncome.length > 0 && (
          <>
            <Gap size="s" />
            <Label>{t.income.grossIncome.otherIncomeInfoLabel}</Label>
            <Gap size="s" />
            <P noMargin>{t.income.grossIncome.otherIncomeInfoDescription}</P>
            <Gap size="s" />
            <InputField
              value={formData.otherIncomeInfo}
              onChange={onOtherIncomeInfoChange}
            />
          </>
        )}
      </FixedSpaceColumn>
    </ContentArea>
  )
})

const EntrepreneurIncomeSelection = React.memo(
  function EntrepreneurIncomeSelection({
    formData,
    showFormErrors,
    onChange
  }: {
    formData: Form.Entrepreneur
    showFormErrors: boolean
    onChange: SetStateCallback<Form.Entrepreneur>
  }) {
    const t = useTranslation()
    const [lang] = useLang()

    const onSelfEmployedChange = useFieldSetState(onChange, 'selfEmployed')
    const onLimitedCompanyChange = useFieldSetState(onChange, 'limitedCompany')
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
            onChange={useFieldSetter(onChange, 'fullTime', true)}
          />
          <Gap size="s" />
          <Radio
            label={t.income.entrepreneurIncome.partTime}
            data-qa="entrepreneur-part-time-option"
            checked={formData.fullTime === false}
            onChange={useFieldSetter(onChange, 'fullTime', false)}
          />
          <Gap size="L" />
          <Label htmlFor="entrepreneur-start-date">
            {t.income.entrepreneurIncome.startOfEntrepreneurship} *
          </Label>
          <Gap size="s" />
          <DatePicker
            date={formData.startOfEntrepreneurship}
            data-qa="entrepreneur-start-date"
            onChange={useFieldDispatch(onChange, 'startOfEntrepreneurship')}
            locale={lang}
            info={useMemo(
              () =>
                errorToInputInfo(
                  validate(
                    formData.startOfEntrepreneurship,
                    required,
                    validDate
                  ),
                  t.validationErrors
                ),
              [formData.startOfEntrepreneurship, t]
            )}
            hideErrorsBeforeTouched={!showFormErrors}
          />
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
            onChange={useFieldSetter(onChange, 'spouseWorksInCompany', true)}
          />
          <Gap size="s" />
          <Radio
            label={t.income.entrepreneurIncome.no}
            data-qa="entrepreneur-spouse-no"
            checked={formData.spouseWorksInCompany === false}
            onChange={useFieldSetter(onChange, 'spouseWorksInCompany', false)}
          />
          <Gap size="L" />
          <Label>{t.income.entrepreneurIncome.startupGrantLabel}</Label>
          <Gap size="s" />
          <Checkbox
            label={t.income.entrepreneurIncome.startupGrant}
            data-qa="entrepreneur-startup-grant"
            checked={formData.startupGrant}
            onChange={useFieldDispatch(onChange, 'startupGrant')}
          />
          <Gap size="L" />
          <Label>{t.income.entrepreneurIncome.checkupLabel}</Label>
          <Gap size="s" />
          <Checkbox
            label={t.income.entrepreneurIncome.checkupConsent}
            data-qa="entrepreneur-checkup-consent"
            checked={formData.checkupConsent}
            onChange={useFieldDispatch(onChange, 'checkupConsent')}
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
            onChange={useCallback(
              (value: boolean) =>
                onChange((prev) => ({
                  ...prev,
                  selfEmployed: { ...prev.selfEmployed, selected: value }
                })),
              [onChange]
            )}
          />
          {formData.selfEmployed.selected && (
            <>
              <Gap size="s" />
              <SelfEmployedIncomeSelection
                formData={formData.selfEmployed}
                showFormErrors={showFormErrors}
                onChange={onSelfEmployedChange}
              />
            </>
          )}
          <Gap size="m" />
          <Checkbox
            label={t.income.entrepreneurIncome.limitedCompany}
            data-qa="entrepreneur-llc"
            checked={formData.limitedCompany.selected}
            onChange={useCallback(
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
            )}
          />
          {formData.limitedCompany.selected && (
            <>
              <Gap size="s" />
              <LimitedCompanyIncomeSelection
                formData={formData.limitedCompany}
                showFormErrors={showFormErrors}
                onChange={onLimitedCompanyChange}
              />
            </>
          )}
          <Gap size="m" />
          <Checkbox
            label={t.income.entrepreneurIncome.partnership}
            data-qa="entrepreneur-partnership"
            checked={formData.partnership}
            onChange={useFieldDispatch(onChange, 'partnership')}
          />
          {formData.partnership && (
            <>
              <Gap size="s" />
              <Indent>{t.income.entrepreneurIncome.partnershipInfo}</Indent>
            </>
          )}
          <Gap size="m" />
          <Checkbox
            label={t.income.entrepreneurIncome.lightEntrepreneur}
            data-qa="entrepreneur-light-entrepreneur"
            checked={formData.lightEntrepreneur}
            onChange={useFieldDispatch(onChange, 'lightEntrepreneur')}
          />
          {formData.lightEntrepreneur && (
            <>
              <Gap size="s" />
              <Indent>
                {t.income.entrepreneurIncome.lightEntrepreneurInfo}
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
    onChange
  }: {
    formData: Form.SelfEmployed
    showFormErrors: boolean
    onChange: SetStateCallback<Form.SelfEmployed>
  }) {
    const t = useTranslation()
    const [lang] = useLang()

    const isValidEndDate = useCallback(
      (date) => {
        const incomeStartDate = LocalDate.parseFiOrNull(
          formData.incomeStartDate
        )
        return incomeStartDate === null || incomeStartDate <= date
      },
      [formData.incomeStartDate]
    )

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
            onChange={useFieldDispatch(onChange, 'attachments')}
          />
          <Checkbox
            label={t.income.selfEmployed.estimatedIncome}
            data-qa="self-employed-estimated-income"
            checked={formData.estimation}
            onChange={useFieldDispatch(onChange, 'estimation')}
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
                  onFocus={useFieldSetter(onChange, 'estimation', true)}
                  onChange={useFieldDispatch(
                    onChange,
                    'estimatedMonthlyIncome'
                  )}
                  hideErrorsBeforeTouched={!showFormErrors}
                  info={useMemo(
                    () =>
                      errorToInputInfo(
                        validateIf(
                          formData.estimation,
                          formData.estimatedMonthlyIncome,
                          required,
                          validInt
                        ),
                        t.validationErrors
                      ),
                    [formData.estimation, formData.estimatedMonthlyIncome, t]
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
                    onFocus={useFieldSetter(onChange, 'estimation', true)}
                    onChange={useFieldDispatch(onChange, 'incomeStartDate')}
                    locale={lang}
                    hideErrorsBeforeTouched={!showFormErrors}
                    info={useMemo(
                      () =>
                        errorToInputInfo(
                          validateIf(
                            formData.estimation,
                            formData.incomeStartDate,
                            required,
                            validDate
                          ),
                          t.validationErrors
                        ),
                      [formData.estimation, formData.incomeStartDate, t]
                    )}
                  />
                  <span>{' - '}</span>
                  <DatePicker
                    date={formData.incomeEndDate}
                    onFocus={useFieldSetter(onChange, 'estimation', true)}
                    onChange={useFieldDispatch(onChange, 'incomeEndDate')}
                    locale={lang}
                    isValidDate={isValidEndDate}
                    hideErrorsBeforeTouched={!showFormErrors}
                    info={useMemo(
                      () =>
                        errorToInputInfo(
                          validateIf(
                            formData.estimation && formData.incomeEndDate != '',
                            formData.incomeEndDate,
                            validDate
                          ),
                          t.validationErrors
                        ),
                      [formData.estimation, formData.incomeEndDate, t]
                    )}
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
    onChange
  }: {
    formData: Form.LimitedCompany
    showFormErrors: boolean
    onChange: SetStateCallback<Form.LimitedCompany>
  }) {
    const t = useTranslation()

    return (
      <Indent>
        <FixedSpaceColumn>
          <P noMargin>{t.income.limitedCompany.info}</P>
          {showFormErrors && formData.incomeSource === null && (
            <LabelError text={t.income.errors.choose} />
          )}
          <Radio
            label={t.income.limitedCompany.incomesRegister}
            data-qa="llc-incomes-register"
            checked={formData.incomeSource === 'INCOMES_REGISTER'}
            onChange={useFieldSetter(
              onChange,
              'incomeSource',
              'INCOMES_REGISTER'
            )}
          />
          <Radio
            label={t.income.limitedCompany.attachments}
            data-qa="llc-attachments"
            checked={formData.incomeSource === 'ATTACHMENTS'}
            onChange={useFieldSetter(onChange, 'incomeSource', 'ATTACHMENTS')}
          />
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
          onChange={useFieldDispatch(onChange, 'name')}
          hideErrorsBeforeTouched={!showFormErrors}
          info={useMemo(
            () =>
              errorToInputInfo(
                validate(formData.name, required),
                tr.validationErrors
              ),
            [formData.name, tr]
          )}
        />

        <Label>{t.phone} *</Label>
        <InputField
          placeholder={t.phonePlaceholder}
          data-qa="accountant-phone"
          width="L"
          value={formData.phone}
          onChange={useFieldDispatch(onChange, 'phone')}
          hideErrorsBeforeTouched={!showFormErrors}
          info={useMemo(
            () =>
              errorToInputInfo(
                validate(formData.phone, required),
                tr.validationErrors
              ),
            [formData.phone, tr]
          )}
        />

        <Label>{t.email} *</Label>
        <InputField
          placeholder={t.emailPlaceholder}
          data-qa="accountant-email"
          width="L"
          value={formData.email}
          onChange={useFieldDispatch(onChange, 'email')}
          hideErrorsBeforeTouched={!showFormErrors}
          info={useMemo(
            () =>
              errorToInputInfo(
                validate(formData.email, required),
                tr.validationErrors
              ),
            [formData.email, tr.validationErrors]
          )}
        />

        <Label>{t.address}</Label>
        <InputField
          placeholder={t.addressPlaceholder}
          width="L"
          value={formData.address}
          onChange={useFieldDispatch(onChange, 'address')}
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
  onChange
}: {
  formData: OtherInfoFormData
  onChange: SetStateCallback<Form.IncomeStatementForm>
}) {
  const t = useTranslation()

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
          onChange={useFieldDispatch(onChange, 'student')}
        />
        <Gap size="s" />
        <P noMargin>{t.income.moreInfo.studentInfo}</P>
        <Gap size="L" />
        <Label>{t.income.moreInfo.deductions}</Label>
        <Gap size="s" />
        <Checkbox
          label={t.income.moreInfo.alimony}
          data-qa="alimony-payer"
          checked={formData.alimonyPayer}
          onChange={useFieldDispatch(onChange, 'alimonyPayer')}
        />
        <Gap size="L" />
        <Label htmlFor="more-info">{t.income.moreInfo.otherInfoLabel}</Label>
        <Gap size="s" />
        <TextArea
          id="more-info"
          value={formData.otherInfo}
          onChange={useFieldDispatch(onChange, 'otherInfo')}
        />
      </FixedSpaceColumn>
    </ContentArea>
  )
})

const Attachments = React.memo(function Attachments({
  incomeStatementId,
  requiredAttachments,
  attachments,
  onUploaded,
  onDeleted
}: {
  incomeStatementId: UUID | undefined
  requiredAttachments: Set<AttachmentType>
  attachments: Attachment[]
  onUploaded: (attachment: Attachment) => void
  onDeleted: (attachmentId: UUID) => void
}) {
  const t = useTranslation()

  const handleUpload = useCallback(
    async (
      file: File,
      onUploadProgress: (progressEvent: ProgressEvent) => void
    ) => {
      return (
        await saveIncomeStatementAttachment(
          incomeStatementId,
          file,
          onUploadProgress
        )
      ).map((id) => {
        onUploaded({
          id,
          name: file.name,
          contentType: file.type
        })
        return id
      })
    },
    [incomeStatementId, onUploaded]
  )

  const handleDelete = useCallback(
    async (id: UUID) => {
      return (await deleteAttachment(id)).map(() => {
        onDeleted(id)
      })
    },
    [onDeleted]
  )

  return (
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
                  {t.income.attachments.attachmentNames[attachmentType]}
                </li>
              ))}
            </UnorderedList>
            <Gap size="L" />
          </>
        )}
        <FileUpload
          files={attachments}
          onUpload={handleUpload}
          onDelete={handleDelete}
          onDownloadFile={getAttachmentBlob}
          i18n={{ upload: t.fileUpload, download: t.fileDownload }}
        />
      </FixedSpaceColumn>
    </ContentArea>
  )
})

export function useRequiredAttachments(
  formData: Form.IncomeStatementForm
): Set<AttachmentType> {
  const { gross, entrepreneur, alimonyPayer, student } = formData

  return useMemo(() => {
    const result: Set<AttachmentType> = new Set()
    if (gross.selected) {
      if (gross.incomeSource === 'ATTACHMENTS') result.add('PAYSLIP')
      if (gross.otherIncome)
        gross.otherIncome.forEach((item) => result.add(item))
    }
    if (entrepreneur.selected) {
      if (entrepreneur.startupGrant) result.add('STARTUP_GRANT')
      if (
        entrepreneur.selfEmployed.selected &&
        !entrepreneur.selfEmployed.estimation
      ) {
        result.add('PROFIT_AND_LOSS_STATEMENT')
      }
      if (entrepreneur.limitedCompany.selected) {
        if (entrepreneur.limitedCompany.incomeSource === 'ATTACHMENTS') {
          result.add('PAYSLIP')
        }
        result.add('ACCOUNTANT_REPORT_LLC')
      }
      if (entrepreneur.partnership) {
        result.add('PROFIT_AND_LOSS_STATEMENT').add('ACCOUNTANT_REPORT')
      }
      if (entrepreneur.lightEntrepreneur) {
        result.add('SALARY')
      }
    }
    if (gross.selected || entrepreneur.selected) {
      if (student) result.add('PROOF_OF_STUDIES')
      if (alimonyPayer) result.add('ALIMONY_PAYOUT')
    }

    return result
  }, [
    alimonyPayer,
    entrepreneur.lightEntrepreneur,
    entrepreneur.limitedCompany.incomeSource,
    entrepreneur.limitedCompany.selected,
    entrepreneur.partnership,
    entrepreneur.selected,
    entrepreneur.selfEmployed.estimation,
    entrepreneur.selfEmployed.selected,
    entrepreneur.startupGrant,
    gross.incomeSource,
    gross.otherIncome,
    gross.selected,
    student
  ])
}

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

const LabelError = styled(
  React.memo(function LabelError({
    text,
    className
  }: {
    text: string
    className?: string
  }) {
    const { colors } = useTheme()
    return (
      <span className={className}>
        <FontAwesomeIcon
          icon={fasExclamationTriangle}
          color={colors.accents.orange}
        />
        {text}
      </span>
    )
  })
)`
  font-size: 14px;
  font-weight: ${fontWeights.semibold};
  color: ${(p) => p.theme.colors.accents.orangeDark};

  > :first-child {
    margin-right: ${defaultMargins.xs};
  }
`

const LabelWithError = React.memo(function LabelWithError({
  label,
  showError,
  errorText
}: {
  label: string
  showError: boolean
  errorText: string
}) {
  return (
    <FixedSpaceRow>
      <Label>{label}</Label>
      {showError ? <LabelError text={errorText} /> : null}
    </FixedSpaceRow>
  )
})
