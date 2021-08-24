import React from 'react'
import styled, { useTheme } from 'styled-components'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { H1, H2, H3, H4, Label, P } from 'lib-components/typography'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import { useLang, useTranslation } from '../localization'
import Radio from 'lib-components/atoms/form/Radio'
import {
  FixedSpaceColumn,
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
import { AttachmentType, otherIncome } from './types/common'
import * as Form from './types/form'
import {
  errorToInputInfo,
  required,
  validate,
  validateIf,
  validDate,
  validInt
} from '../form-validation'
import FileUpload from 'lib-components/molecules/FileUpload'
import Container, { ContentArea } from 'lib-components/layout/Container'
import Footer from '../Footer'
import { Attachment } from 'lib-common/api-types/attachment'
import TextArea from 'lib-components/atoms/form/TextArea'
import { UUID } from 'lib-common/types'
import {
  deleteAttachment,
  getAttachmentBlob,
  saveAttachment
} from '../attachments'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { fasExclamationTriangle } from 'lib-icons'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import LocalDate from 'lib-common/local-date'

interface Props {
  formData: Form.IncomeStatementForm
  showFormErrors: boolean
  onChange: (
    fn: (prev: Form.IncomeStatementForm) => Form.IncomeStatementForm
  ) => void
  onSave: AsyncClickCallback
  onSuccess: () => void
  onCancel: () => void
}

export interface IncomeStatementFormAPI {
  scrollToErrors: () => void
}

export default React.forwardRef(function IncomeStatementForm(
  { formData, showFormErrors, onChange, onSave, onSuccess, onCancel }: Props,
  ref: React.ForwardedRef<IncomeStatementFormAPI>
) {
  const t = useTranslation()

  const handleChange = React.useCallback(
    (value: Form.IncomeStatementForm) => onChange(() => value),
    [onChange]
  )

  const scrollTarget = React.useRef<HTMLElement>(null)

  React.useImperativeHandle(ref, () => ({
    scrollToErrors() {
      // Use requestAnimationFrame to make sure that the scroll target has been
      // added to the DOM
      requestAnimationFrame(() => {
        if (!scrollTarget.current) return
        const top =
          window.pageYOffset + scrollTarget.current.getBoundingClientRect().top
        window.scrollTo({ top, left: 0, behavior: 'smooth' })
      })
    }
  }))

  const showOtherInfo =
    formData.gross.selected || formData.entrepreneur.selected

  const requiredAttachments = computeRequiredAttachments(formData)

  const handleAttachmentUploaded = React.useCallback(
    (attachment: Attachment) =>
      onChange((prev) => ({
        ...prev,
        attachments: [...prev.attachments, attachment]
      })),
    [onChange]
  )

  const handleAttachmentDeleted = React.useCallback(
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
          <FixedSpaceColumn spacing="zero">
            <H1 noMargin>{t.income.title}</H1>
            <Gap size="s" />
            <P noMargin>{t.income.description}</P>
          </FixedSpaceColumn>
        </ContentArea>
        <Gap size="s" />
        <IncomeTypeSelection
          formData={formData}
          showFormErrors={showFormErrors}
          onChange={handleChange}
          ref={scrollTarget}
        />
        {formData.gross.selected && (
          <>
            <Gap size="L" />
            <GrossIncomeSelection
              formData={formData.gross}
              showFormErrors={showFormErrors}
              onChange={(value) => handleChange({ ...formData, gross: value })}
            />
          </>
        )}
        {formData.entrepreneur.selected && (
          <>
            <Gap size="L" />
            <EntrepreneurIncomeSelection
              formData={formData.entrepreneur}
              showFormErrors={showFormErrors}
              onChange={(value) =>
                handleChange({ ...formData, entrepreneur: value })
              }
            />
          </>
        )}
        {showOtherInfo && (
          <>
            <Gap size="L" />
            <OtherInfo formData={formData} onChange={handleChange} />
            <Gap size="L" />
            <Attachments
              requiredAttachments={requiredAttachments}
              attachments={formData.attachments}
              onUploaded={handleAttachmentUploaded}
              onDeleted={handleAttachmentDeleted}
            />
          </>
        )}
        <Gap size="L" />
        <FixedSpaceRow
          alignItems="center"
          justifyContent="flex-end"
          spacing="L"
        >
          <Checkbox
            label={`${t.income.assure} *`}
            checked={formData.assure}
            onChange={(value) => handleChange({ ...formData, assure: value })}
          />
          <Button text={t.common.cancel} onClick={onCancel} />
          <AsyncButton
            text={t.common.save}
            primary
            onClick={onSave}
            disabled={!saveButtonEnabled}
            onSuccess={onSuccess}
          />
        </FixedSpaceRow>
      </Container>
      <Footer />
    </>
  )
})

const IncomeTypeSelection = React.forwardRef(function IncomeTypeSelection(
  {
    formData,
    showFormErrors,
    onChange
  }: {
    formData: Form.IncomeStatementForm
    showFormErrors: boolean
    onChange: (value: Form.IncomeStatementForm) => void
  },
  ref: React.ForwardedRef<HTMLElement>
) {
  const t = useTranslation()
  const [lang] = useLang()

  const startDate = LocalDate.parseFiOrNull(formData.startDate)

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
              onChange={(value) => onChange({ ...formData, startDate: value })}
              info={errorToInputInfo(
                validDate(formData.startDate),
                t.validationErrors
              )}
              hideErrorsBeforeTouched
              locale={lang}
            />
          </div>
          <div>
            <Label htmlFor="end-date">{t.income.incomeType.endDate}</Label>
            <Gap size="xs" />
            <DatePicker
              id="end-date"
              date={formData.endDate}
              onChange={(value) => onChange({ ...formData, endDate: value })}
              isValidDate={(date) => startDate === null || startDate <= date}
              info={errorToInputInfo(
                validateIf(formData.endDate != '', formData.endDate, validDate),
                t.validationErrors
              )}
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
          checked={formData.highestFee}
          onChange={(value) =>
            onChange({
              ...formData,
              highestFee: value,
              gross: { ...formData.gross, selected: false },
              entrepreneur: { ...formData.entrepreneur, selected: false }
            })
          }
        />
        {formData.highestFee && (
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
          checked={formData.gross.selected}
          disabled={formData.highestFee}
          onChange={(value) =>
            onChange({
              ...formData,
              gross: { ...formData.gross, selected: value }
            })
          }
        />
        <Gap size="s" />
        <Checkbox
          label={t.income.incomeType.entrepreneurIncome}
          checked={formData.entrepreneur.selected}
          disabled={formData.highestFee}
          onChange={(value) =>
            onChange({
              ...formData,
              entrepreneur: { ...formData.entrepreneur, selected: value }
            })
          }
        />
      </FixedSpaceColumn>
    </ContentArea>
  )
})

function GrossIncomeSelection({
  formData,
  showFormErrors,
  onChange
}: {
  formData: Form.Gross
  showFormErrors: boolean
  onChange: (value: Form.Gross) => void
}) {
  const t = useTranslation()
  const [lang] = useLang()
  const incomeStartDate = LocalDate.parseFiOrNull(formData.incomeStartDate)
  return (
    <ContentArea opaque paddingVertical="L">
      <FixedSpaceColumn spacing="zero">
        <H2 noMargin>{t.income.grossIncome.title}</H2>
        <Gap size="m" />
        <P noMargin>{t.income.grossIncome.description}</P>
        <Gap size="m" />
        <LabelWithError
          label={`${t.income.grossIncome.incomeSource} *`}
          showError={showFormErrors && formData.incomeSource === null}
          errorText={t.income.errors.choose}
        />
        <Gap size="s" />
        <Radio
          label={t.income.incomesRegisterConsent}
          checked={formData.incomeSource === 'INCOMES_REGISTER'}
          onChange={() =>
            onChange({ ...formData, incomeSource: 'INCOMES_REGISTER' })
          }
        />
        <Gap size="s" />
        <Radio
          label={t.income.grossIncome.provideAttachments}
          checked={formData.incomeSource === 'ATTACHMENTS'}
          onChange={() =>
            onChange({ ...formData, incomeSource: 'ATTACHMENTS' })
          }
        />
        <Gap size="L" />
        <Label>{t.income.grossIncome.estimate}</Label>
        <Gap size="m" />
        <FixedSpaceRow>
          <FixedSpaceColumn>
            <LightLabel htmlFor="estimated-monthly-income">
              {t.income.selfEmployed.estimatedMonthlyIncome}
            </LightLabel>
            <InputField
              id="estimated-monthly-income"
              value={formData.estimatedMonthlyIncome}
              onChange={(value) =>
                onChange({ ...formData, estimatedMonthlyIncome: value })
              }
              hideErrorsBeforeTouched
              info={
                formData.estimatedMonthlyIncome
                  ? errorToInputInfo(
                      validInt(formData.estimatedMonthlyIncome),
                      t.validationErrors
                    )
                  : undefined
              }
            />
          </FixedSpaceColumn>
          <FixedSpaceColumn>
            <LightLabel htmlFor="income-start-date">
              {t.income.selfEmployed.timeRange}
            </LightLabel>
            <FixedSpaceRow>
              <DatePicker
                id="income-start-date"
                date={formData.incomeStartDate}
                onChange={(value) =>
                  onChange({ ...formData, incomeStartDate: value })
                }
                locale={lang}
                hideErrorsBeforeTouched
                info={
                  formData.incomeStartDate
                    ? errorToInputInfo(
                        validDate(formData.incomeStartDate),
                        t.validationErrors
                      )
                    : undefined
                }
              />
              <span>{' - '}</span>
              <DatePicker
                date={formData.incomeEndDate}
                onChange={(value) =>
                  onChange({ ...formData, incomeEndDate: value })
                }
                isValidDate={(date) =>
                  incomeStartDate === null || incomeStartDate <= date
                }
                locale={lang}
                hideErrorsBeforeTouched
                info={
                  formData.incomeEndDate
                    ? errorToInputInfo(
                        validDate(formData.incomeEndDate),
                        t.validationErrors
                      )
                    : undefined
                }
              />
            </FixedSpaceRow>
          </FixedSpaceColumn>
        </FixedSpaceRow>
        <Gap size="L" />
        <Label>{t.income.grossIncome.otherIncome}</Label>
        <Gap size="s" />
        {t.income.grossIncome.otherIncomeInfo}
        <Gap size="s" />
        <OtherIncomeWrapper>
          <MultiSelect
            value={formData.otherIncome}
            options={otherIncome}
            getOptionId={(option) => option}
            getOptionLabel={(option) =>
              t.income.grossIncome.otherIncomeTypes[option]
            }
            onChange={(value) => onChange({ ...formData, otherIncome: value })}
            placeholder={t.income.grossIncome.choosePlaceholder}
          />
        </OtherIncomeWrapper>
      </FixedSpaceColumn>
    </ContentArea>
  )
}

function EntrepreneurIncomeSelection({
  formData,
  showFormErrors,
  onChange
}: {
  formData: Form.Entrepreneur
  showFormErrors: boolean
  onChange: (value: Form.Entrepreneur) => void
}) {
  const t = useTranslation()
  const [lang] = useLang()

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
          checked={formData.fullTime === true}
          onChange={() => onChange({ ...formData, fullTime: true })}
        />
        <Gap size="s" />
        <Radio
          label={t.income.entrepreneurIncome.partTime}
          checked={formData.fullTime === false}
          onChange={() => onChange({ ...formData, fullTime: false })}
        />
        <Gap size="L" />
        <Label htmlFor="entrepreneur-start-date">
          {t.income.entrepreneurIncome.startOfEntrepreneurship} *
        </Label>
        <Gap size="s" />
        <DatePicker
          date={formData.startOfEntrepreneurship}
          onChange={(value) =>
            onChange({ ...formData, startOfEntrepreneurship: value })
          }
          locale={lang}
          info={errorToInputInfo(
            validate(formData.startOfEntrepreneurship, required, validDate),
            t.validationErrors
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
          checked={formData.spouseWorksInCompany === true}
          onChange={() => onChange({ ...formData, spouseWorksInCompany: true })}
        />
        <Gap size="s" />
        <Radio
          label={t.income.entrepreneurIncome.no}
          checked={formData.spouseWorksInCompany === false}
          onChange={() =>
            onChange({ ...formData, spouseWorksInCompany: false })
          }
        />
        <Gap size="L" />
        <Label>{t.income.entrepreneurIncome.startupGrantLabel}</Label>
        <Gap size="s" />
        <Checkbox
          label={t.income.entrepreneurIncome.startupGrant}
          checked={formData.startupGrant}
          onChange={(value) =>
            onChange({
              ...formData,
              startupGrant: value
            })
          }
        />
        <Gap size="L" />
        <Label>{t.income.entrepreneurIncome.checkupLabel} *</Label>
        <Gap size="s" />
        <Checkbox
          label={t.income.entrepreneurIncome.checkupConsent}
          checked={formData.checkupConsent}
          onChange={(value) => onChange({ ...formData, checkupConsent: value })}
        />
        {showFormErrors && !formData.checkupConsent && (
          <AlertBox message={t.income.errors.consentRequired} />
        )}
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
          checked={formData.selfEmployed.selected}
          onChange={(value) =>
            onChange({
              ...formData,
              selfEmployed: { ...formData.selfEmployed, selected: value }
            })
          }
        />
        {formData.selfEmployed.selected && (
          <>
            <Gap size="s" />
            <SelfEmployedIncomeSelection
              formData={formData.selfEmployed}
              showFormErrors={showFormErrors}
              onChange={(value) =>
                onChange({
                  ...formData,
                  selfEmployed: { ...value, selected: true }
                })
              }
            />
          </>
        )}
        <Gap size="m" />
        <Checkbox
          label={t.income.entrepreneurIncome.limitedCompany}
          checked={formData.limitedCompany.selected}
          onChange={(value) =>
            onChange({
              ...formData,
              limitedCompany: {
                ...formData.limitedCompany,
                selected: value,
                ...(value ? {} : { incomeSource: null })
              }
            })
          }
        />
        {formData.limitedCompany.selected && (
          <>
            <Gap size="s" />
            <LimitedCompanyIncomeSelection
              formData={formData.limitedCompany}
              showFormErrors={showFormErrors}
              onChange={(value) =>
                onChange({
                  ...formData,
                  limitedCompany: { ...value, selected: true }
                })
              }
            />
          </>
        )}
        <Gap size="m" />
        <Checkbox
          label={t.income.entrepreneurIncome.partnership}
          checked={formData.partnership}
          onChange={(value) => onChange({ ...formData, partnership: value })}
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
          checked={formData.lightEntrepreneur}
          onChange={(value) =>
            onChange({
              ...formData,
              lightEntrepreneur: value
            })
          }
        />
        {formData.lightEntrepreneur && (
          <>
            <Gap size="s" />
            <Indent>{t.income.entrepreneurIncome.lightEntrepreneurInfo}</Indent>
          </>
        )}
        {(formData.limitedCompany.selected || formData.partnership) && (
          <>
            <Gap size="L" />
            <Accounting
              formData={formData.accountant}
              showFormErrors={showFormErrors}
              onChange={(value) => onChange({ ...formData, accountant: value })}
            />
          </>
        )}
      </FixedSpaceColumn>
    </ContentArea>
  )
}

function SelfEmployedIncomeSelection({
  formData,
  showFormErrors,
  onChange
}: {
  formData: Form.SelfEmployed
  showFormErrors: boolean
  onChange: (value: Form.SelfEmployed) => void
}) {
  const t = useTranslation()
  const [lang] = useLang()
  const incomeStartDate = LocalDate.parseFiOrNull(formData.incomeStartDate)
  return (
    <Indent>
      <FixedSpaceColumn>
        <P noMargin>{t.income.selfEmployed.info}</P>
        {showFormErrors && !formData.attachments && !formData.estimation && (
          <LabelError text={t.income.errors.chooseAtLeastOne} />
        )}
        <Checkbox
          label={t.income.selfEmployed.attachments}
          checked={formData.attachments}
          onChange={(value) => onChange({ ...formData, attachments: value })}
        />
        <Checkbox
          label={t.income.selfEmployed.estimatedIncome}
          checked={formData.estimation}
          onChange={(value) => onChange({ ...formData, estimation: value })}
        />
        <Indent>
          <FixedSpaceRow spacing="XL">
            <FixedSpaceColumn>
              <Label htmlFor="estimated-monthly-income">
                {t.income.selfEmployed.estimatedMonthlyIncome}
              </Label>
              <InputField
                id="estimated-monthly-income"
                value={formData.estimatedMonthlyIncome}
                onFocus={() => onChange({ ...formData, estimation: true })}
                onChange={(value) =>
                  onChange({ ...formData, estimatedMonthlyIncome: value })
                }
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
                  onFocus={() => onChange({ ...formData, estimation: true })}
                  onChange={(value) =>
                    onChange({ ...formData, incomeStartDate: value })
                  }
                  locale={lang}
                  hideErrorsBeforeTouched={!showFormErrors}
                  info={errorToInputInfo(
                    validateIf(
                      formData.estimation,
                      formData.incomeStartDate,
                      required,
                      validDate
                    ),
                    t.validationErrors
                  )}
                />
                <span>{' - '}</span>
                <DatePicker
                  date={formData.incomeEndDate}
                  onFocus={() => onChange({ ...formData, estimation: true })}
                  onChange={(value) =>
                    onChange({ ...formData, incomeEndDate: value })
                  }
                  locale={lang}
                  isValidDate={(date) =>
                    incomeStartDate === null || incomeStartDate <= date
                  }
                  hideErrorsBeforeTouched={!showFormErrors}
                  info={errorToInputInfo(
                    validateIf(
                      formData.estimation && formData.incomeEndDate != '',
                      formData.incomeEndDate,
                      validDate
                    ),
                    t.validationErrors
                  )}
                />
              </FixedSpaceRow>
            </FixedSpaceColumn>
          </FixedSpaceRow>
        </Indent>
      </FixedSpaceColumn>
    </Indent>
  )
}

function LimitedCompanyIncomeSelection({
  formData,
  showFormErrors,
  onChange
}: {
  formData: Form.LimitedCompany
  showFormErrors: boolean
  onChange: (value: Form.LimitedCompany) => void
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
          checked={formData.incomeSource === 'INCOMES_REGISTER'}
          onChange={() =>
            onChange({ ...formData, incomeSource: 'INCOMES_REGISTER' })
          }
        />
        <Radio
          label={t.income.limitedCompany.attachments}
          checked={formData.incomeSource === 'ATTACHMENTS'}
          onChange={() =>
            onChange({ ...formData, incomeSource: 'ATTACHMENTS' })
          }
        />
      </FixedSpaceColumn>
    </Indent>
  )
}

function Accounting({
  formData,
  showFormErrors,
  onChange
}: {
  formData: Form.Accountant
  showFormErrors: boolean
  onChange: (value: Form.Accountant) => void
}) {
  const tr = useTranslation()
  const t = tr.income.accounting
  return (
    <>
      <H3 noMargin>{t.title}</H3>
      <Gap size="s" />
      <FixedSpaceRow spacing="L" fullWidth maxWidth="400px">
        <FixedSpaceColumn spacing="zero" fullWidth>
          <Label>{t.accountant} *</Label>
          <Gap size="s" />
          <InputField
            placeholder={t.accountantPlaceholder}
            value={formData.name}
            onChange={(value) => onChange({ ...formData, name: value })}
            hideErrorsBeforeTouched={!showFormErrors}
            info={errorToInputInfo(
              validate(formData.name, required),
              tr.validationErrors
            )}
          />
          <Gap size="L" />
          <Label>{t.email} *</Label>
          <Gap size="s" />
          <InputField
            placeholder={t.emailPlaceholder}
            value={formData.email}
            onChange={(value) => onChange({ ...formData, email: value })}
            hideErrorsBeforeTouched={!showFormErrors}
            info={errorToInputInfo(
              validate(formData.email, required),
              tr.validationErrors
            )}
          />
          <Gap size="L" />
          <Label>{t.address}</Label>
          <Gap size="s" />
          <InputField
            placeholder={t.addressPlaceholder}
            value={formData.address}
            onChange={(value) => onChange({ ...formData, address: value })}
          />
        </FixedSpaceColumn>
        <FixedSpaceColumn spacing="zero" fullWidth>
          <Label>{t.phone} *</Label>
          <Gap size="s" />
          <InputField
            placeholder={t.phonePlaceholder}
            value={formData.phone}
            onChange={(value) => onChange({ ...formData, phone: value })}
            hideErrorsBeforeTouched={!showFormErrors}
            info={errorToInputInfo(
              validate(formData.phone, required),
              tr.validationErrors
            )}
          />
        </FixedSpaceColumn>
      </FixedSpaceRow>
    </>
  )
}

function OtherInfo({
  formData,
  onChange
}: {
  formData: Form.IncomeStatementForm
  onChange: (value: Form.IncomeStatementForm) => void
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
          checked={formData.student}
          onChange={(value) => onChange({ ...formData, student: value })}
        />
        <Gap size="s" />
        <P noMargin>{t.income.moreInfo.studentInfo}</P>
        <Gap size="L" />
        <Label>{t.income.moreInfo.deductions}</Label>
        <Gap size="s" />
        <Checkbox
          label={t.income.moreInfo.alimony}
          checked={formData.alimonyPayer}
          onChange={(value) => onChange({ ...formData, alimonyPayer: value })}
        />
        <Gap size="L" />
        <Label htmlFor="more-info">{t.income.moreInfo.otherInfoLabel}</Label>
        <Gap size="s" />
        <TextArea
          id="more-info"
          value={formData.otherInfo}
          onChange={(e) => onChange({ ...formData, otherInfo: e.target.value })}
        />
      </FixedSpaceColumn>
    </ContentArea>
  )
}

function Attachments({
  requiredAttachments,
  attachments,
  onUploaded,
  onDeleted
}: {
  requiredAttachments: Set<AttachmentType>
  attachments: Attachment[]
  onUploaded: (attachment: Attachment) => void
  onDeleted: (attachmentId: UUID) => void
}) {
  const t = useTranslation()

  const handleUpload = React.useCallback(
    async (
      file: File,
      onUploadProgress: (progressEvent: ProgressEvent) => void
    ) => {
      return (await saveAttachment(file, onUploadProgress)).map((id) => {
        onUploaded({
          id,
          name: file.name,
          contentType: file.type
        })
        return id
      })
    },
    [onUploaded]
  )

  const handleDelete = React.useCallback(
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
            <UnorderedList>
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
}

export function computeRequiredAttachments(
  formData: Form.IncomeStatementForm
): Set<AttachmentType> {
  const { gross, entrepreneur } = formData

  const result: Set<AttachmentType> = new Set()
  if (gross.selected) {
    if (gross.incomeSource === 'ATTACHMENTS') result.add('PAYSLIP')
    if (gross.otherIncome) gross.otherIncome.forEach((item) => result.add(item))
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
      result.add('ACCOUNTANT_REPORT')
    }
    if (entrepreneur.partnership) {
      result.add('PROFIT_AND_LOSS_STATEMENT').add('ACCOUNTANT_REPORT')
    }
  }
  if (gross.selected || entrepreneur.selected) {
    if (formData.student) result.add('PROOF_OF_STUDIES')
    if (formData.alimonyPayer) result.add('ALIMONY_PAYOUT')
  }

  return result
}

const HighestFeeInfo = styled(P).attrs({ noMargin: true })`
  margin-left: ${defaultMargins.XL};
`

const LightLabel = styled(Label)`
  font-weight: 400;
`

const Indent = styled.div`
  width: 100%;
  padding-left: ${defaultMargins.XL};
`

const OtherIncomeWrapper = styled.div`
  max-width: 480px;
`

const LabelError = styled(function ({
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
})`
  font-size: 14px;
  font-weight: 600;
  color: ${(p) => p.theme.colors.accents.orangeDark};

  > :first-child {
    margin-right: ${defaultMargins.xs};
  }
`

function LabelWithError({
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
}
