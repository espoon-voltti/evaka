import React from 'react'
import styled from 'styled-components'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { H1, H2, H3, H4, Label } from 'lib-components/typography'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import { useLang, useTranslation } from '../localization'
import Radio from 'lib-components/atoms/form/Radio'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import Button from 'lib-components/atoms/buttons/Button'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { formatDate } from 'lib-common/date'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import InputField from 'lib-components/atoms/form/InputField'
import { AttachmentType, otherIncome } from './types/common'
import * as Form from './types/form'
import { validateIncomeStatementBody } from './types/body'
import { errorToInputInfo, validDate, validInt } from '../form-validation'
import { createIncomeStatement } from './api'
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

const initialFormData: Form.IncomeStatementForm = {
  startDate: formatDate(new Date()),
  highestFee: false,
  gross: {
    selected: false,
    incomeSource: null,
    otherIncome: null,
    student: false,
    alimony: false
  },
  entrepreneur: {
    selected: false,
    fullTime: null,
    startOfEntrepreneurship: '',
    spouseWorksInCompany: null,
    selfEmployed: {
      selected: false,
      estimation: null,
      estimatedMonthlyIncome: '',
      incomeStartDate: '',
      incomeEndDate: '',
      kelaConsent: false
    },
    limitedCompany: {
      selected: false,
      incomeSource: null
    },
    partnership: {
      selected: false,
      lookupConsent: false
    },
    startupGrant: false
  },
  otherInfo: '',
  attachments: [],
  assure: false
}

export default function IncomeStatementForm() {
  const t = useTranslation()
  const [formData, setFormData] =
    React.useState<Form.IncomeStatementForm>(initialFormData)

  const validatedData = validateIncomeStatementBody(formData)
  const isValid = validatedData !== null
  const showOtherInfo =
    formData.gross.selected || formData.entrepreneur.selected

  const requiredAttachments = computeRequiredAttachments(formData)

  const save = React.useCallback(
    () =>
      validatedData ? createIncomeStatement(validatedData) : Promise.resolve(),
    [validatedData]
  )

  const handleAttachmentUploaded = React.useCallback(
    (attachment: Attachment) =>
      setFormData((prev) => ({
        ...prev,
        attachments: [...prev.attachments, attachment]
      })),
    []
  )

  const handleAttachmentDeleted = React.useCallback(
    (id: UUID) =>
      setFormData((prev) => ({
        ...prev,
        attachments: prev.attachments.filter((a) => a.id !== id)
      })),
    []
  )

  return (
    <>
      <Container>
        <Gap size="s" />
        <ContentArea opaque paddingVertical="L">
          <FixedSpaceColumn>
            <H1 noMargin>{t.income.title}</H1>
            {t.income.description}
          </FixedSpaceColumn>
        </ContentArea>
        <Gap size="s" />
        <IncomeTypeSelection formData={formData} onChange={setFormData} />
        {formData.gross.selected && (
          <>
            <Gap size="L" />
            <GrossIncomeSelection
              formData={formData.gross}
              onChange={(value) => setFormData({ ...formData, gross: value })}
            />
          </>
        )}
        {formData.entrepreneur.selected && (
          <>
            <Gap size="L" />
            <EntrepreneurIncomeSelection
              formData={formData.entrepreneur}
              onChange={(value) =>
                setFormData({ ...formData, entrepreneur: value })
              }
            />
          </>
        )}
        {showOtherInfo && (
          <>
            <Gap size="L" />
            <ContentArea opaque>
              <FixedSpaceColumn>
                <H2>{t.income.moreInfo.title}</H2>
                <label htmlFor="more-info">
                  <strong>{t.income.moreInfo.description}</strong>
                </label>
                <TextArea
                  id="more-info"
                  value={formData.otherInfo}
                  onChange={(e) =>
                    setFormData({ ...formData, otherInfo: e.target.value })
                  }
                />
              </FixedSpaceColumn>
            </ContentArea>
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
            label={t.income.assure}
            checked={formData.assure}
            onChange={(value) => setFormData({ ...formData, assure: value })}
          />
          <Button text={t.common.cancel} />
          <AsyncButton
            text={t.common.save}
            primary
            onClick={save}
            disabled={!isValid}
            onSuccess={() => undefined}
          />
        </FixedSpaceRow>
      </Container>
      <Footer />
    </>
  )
}

function IncomeTypeSelection({
  formData,
  onChange
}: {
  formData: Form.IncomeStatementForm
  onChange: (value: Form.IncomeStatementForm) => void
}) {
  const t = useTranslation()
  const [lang] = useLang()

  return (
    <ContentArea opaque>
      <FixedSpaceColumn>
        <H2>{t.income.incomeInfo}</H2>
        <Label htmlFor="start-date">{t.income.incomeType.startDate}</Label>
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
        <H3>{t.income.incomeType.title}</H3>
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
          <HighestFeeInfo>{t.income.incomeType.highestFeeInfo}</HighestFeeInfo>
        )}
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
        <Gap size="s" />
      </FixedSpaceColumn>
    </ContentArea>
  )
}

function GrossIncomeSelection({
  formData,
  onChange
}: {
  formData: Form.Gross
  onChange: (value: Form.Gross) => void
}) {
  const t = useTranslation()
  return (
    <ContentArea opaque>
      <FixedSpaceColumn>
        <H3>{t.income.grossIncome.title}</H3>
        <p>{t.income.grossIncome.description}</p>
        <Radio
          label={t.income.incomesRegisterConsent}
          checked={formData.incomeSource === 'INCOMES_REGISTER'}
          onChange={() =>
            onChange({ ...formData, incomeSource: 'INCOMES_REGISTER' })
          }
        />
        <Radio
          label={t.income.grossIncome.provideAttachments}
          checked={formData.incomeSource === 'ATTACHMENTS'}
          onChange={() =>
            onChange({ ...formData, incomeSource: 'ATTACHMENTS' })
          }
        />
        <Gap size="s" />
        <Checkbox
          label={t.income.grossIncome.otherIncome}
          checked={formData.otherIncome !== null}
          onChange={(checked) =>
            onChange({
              ...formData,
              otherIncome: checked ? [] : null
            })
          }
        />
        <Indent>{t.income.grossIncome.otherIncomeInfo}</Indent>
        <OtherIncomeWrapper>
          <MultiSelect
            value={formData.otherIncome ?? []}
            options={otherIncome}
            getOptionId={(option) => option}
            getOptionLabel={(option) =>
              t.income.grossIncome.otherIncomeTypes[option]
            }
            onChange={(value) => onChange({ ...formData, otherIncome: value })}
            placeholder={t.income.grossIncome.choosePlaceholder}
          />
        </OtherIncomeWrapper>
        <Gap size="s" />
        <Checkbox
          label={t.income.grossIncome.student}
          checked={formData.student}
          onChange={(value) => onChange({ ...formData, student: value })}
        />
        <Indent>{t.income.grossIncome.studentInfo}</Indent>
        <H3>{t.income.grossIncome.deductions}</H3>
        <Checkbox
          label={t.income.grossIncome.alimony}
          checked={formData.alimony}
          onChange={(value) => onChange({ ...formData, alimony: value })}
        />
        <Gap size="s" />
      </FixedSpaceColumn>
    </ContentArea>
  )
}

function adjustEntrepreneurFormRadioButtons(
  formData: Form.Entrepreneur
): Form.Entrepreneur {
  // if (formData.incomeType.startsWith('ENTREPRENEUR_SELF_EMPLOYED_')) {
  //   return { ...formData, incomeSource: null }
  // }
  // if (formData.incomeType === 'ENTREPRENEUR_PARTNERSHIP') {
  //   return { ...formData, incomeSource: null }
  // }
  return formData
}

function EntrepreneurIncomeSelection({
  formData,
  onChange
}: {
  formData: Form.Entrepreneur
  onChange: (value: Form.Entrepreneur) => void
}) {
  const t = useTranslation()
  const [lang] = useLang()

  const handleChange = (value: Form.Entrepreneur) => {
    onChange(adjustEntrepreneurFormRadioButtons(value))
  }

  return (
    <ContentArea opaque>
      <FixedSpaceColumn>
        <H3>{t.income.entrepreneurIncome.title}</H3>
        {t.income.entrepreneurIncome.description}
        <Gap size="s" />
        <Label>{t.income.entrepreneurIncome.fullTimeLabel}</Label>
        <Radio
          label={t.income.entrepreneurIncome.fullTime}
          checked={formData.fullTime === true}
          onChange={() => handleChange({ ...formData, fullTime: true })}
        />
        <Radio
          label={t.income.entrepreneurIncome.partTime}
          checked={formData.fullTime === false}
          onChange={() => handleChange({ ...formData, fullTime: false })}
        />
        <Gap size="s" />
        <Label htmlFor="entrepreneur-start-date">
          {t.income.entrepreneurIncome.startOfEntrepreneurship}
        </Label>
        <DatePicker
          date={formData.startOfEntrepreneurship}
          onChange={(value) =>
            handleChange({ ...formData, startOfEntrepreneurship: value })
          }
          locale={lang}
          info={errorToInputInfo(
            validDate(formData.startOfEntrepreneurship),
            t.validationErrors
          )}
          hideErrorsBeforeTouched
        />
        <Gap size="s" />
        <Label>{t.income.entrepreneurIncome.spouseWorksInCompany}</Label>
        <Radio
          label={t.income.entrepreneurIncome.yes}
          checked={formData.spouseWorksInCompany === true}
          onChange={() =>
            handleChange({ ...formData, spouseWorksInCompany: true })
          }
        />
        <Radio
          label={t.income.entrepreneurIncome.no}
          checked={formData.spouseWorksInCompany === false}
          onChange={() =>
            handleChange({ ...formData, spouseWorksInCompany: false })
          }
        />
        <Gap size="s" />
        <Checkbox
          label={t.income.entrepreneurIncome.startupGrant}
          checked={formData.startupGrant}
          onChange={(value) =>
            handleChange({
              ...formData,
              startupGrant: value
            })
          }
        />
        <Gap size="xs" />
        <H3>Yritysmuoto</H3>
        <Checkbox
          label={t.income.entrepreneurIncome.selfEmployed}
          checked={formData.selfEmployed.selected}
          onChange={(value) =>
            handleChange({
              ...formData,
              selfEmployed: {
                ...formData.selfEmployed,
                selected: value,
                ...(value ? {} : { estimation: null })
              }
            })
          }
        />
        <SelfEmployedIncomeSelection
          formData={formData.selfEmployed}
          onChange={(value) =>
            handleChange({
              ...formData,
              selfEmployed: { ...value, selected: true }
            })
          }
        />
        <Gap size="s" />
        <Checkbox
          label={t.income.entrepreneurIncome.limitedCompany}
          checked={formData.limitedCompany.selected}
          onChange={(value) =>
            handleChange({
              ...formData,
              limitedCompany: {
                ...formData.limitedCompany,
                selected: value,
                ...(value ? {} : { incomeSource: null })
              }
            })
          }
        />
        <LimitedCompanyIncomeSelection
          formData={formData.limitedCompany}
          onChange={(value) =>
            handleChange({
              ...formData,
              limitedCompany: { ...value, selected: true }
            })
          }
        />
        <Gap size="s" />
        <Checkbox
          label={t.income.entrepreneurIncome.partnership}
          checked={formData.partnership.selected}
          onChange={(value) =>
            handleChange({
              ...formData,
              partnership: {
                ...formData.partnership,
                selected: value,
                ...(value ? {} : { incomeSource: null })
              }
            })
          }
        />
        <PartnershipIncomeSelection
          formData={formData.partnership}
          onChange={(value) =>
            handleChange({
              ...formData,
              partnership: {
                ...value,
                selected: true,
                ...(value ? {} : { lookupConsent: false })
              }
            })
          }
        />
        <Gap size="s" />
      </FixedSpaceColumn>
    </ContentArea>
  )
}

function SelfEmployedIncomeSelection({
  formData,
  onChange
}: {
  formData: Form.SelfEmployed
  onChange: (value: Form.SelfEmployed) => void
}) {
  const t = useTranslation()
  const [lang] = useLang()
  return (
    <Indent>
      <FixedSpaceColumn>
        <Radio
          label={t.income.selfEmployed.attachments}
          checked={formData.estimation === false}
          onChange={() => onChange({ ...formData, estimation: false })}
        />
        <Radio
          label={t.income.selfEmployed.estimation}
          checked={formData.estimation === true}
          onChange={() => onChange({ ...formData, estimation: true })}
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
                hideErrorsBeforeTouched
                info={errorToInputInfo(
                  validInt(formData.estimatedMonthlyIncome),
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
                  hideErrorsBeforeTouched
                  info={errorToInputInfo(
                    validDate(formData.incomeStartDate),
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
                  hideErrorsBeforeTouched
                  info={
                    formData.incomeEndDate
                      ? errorToInputInfo(
                          validDate(formData.incomeStartDate),
                          t.validationErrors
                        )
                      : undefined
                  }
                />
              </FixedSpaceRow>
            </FixedSpaceColumn>
          </FixedSpaceRow>
        </Indent>
        <Checkbox
          label={`${t.income.selfEmployed.kelaConsent} *`}
          checked={formData.kelaConsent}
          onChange={(value) => onChange({ ...formData, kelaConsent: value })}
        />
      </FixedSpaceColumn>
    </Indent>
  )
}

function LimitedCompanyIncomeSelection({
  formData,
  onChange
}: {
  formData: Form.LimitedCompany
  onChange: (value: Form.LimitedCompany) => void
}) {
  const t = useTranslation()
  return (
    <Indent>
      <FixedSpaceColumn>
        <div>{t.income.limitedCompany.info}</div>
        <Radio
          label={t.income.incomesRegisterConsent}
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

function PartnershipIncomeSelection({
  formData,
  onChange
}: {
  formData: Form.Partnership
  onChange: (value: Form.Partnership) => void
}) {
  const t = useTranslation()
  return (
    <Indent>
      <FixedSpaceColumn>
        <Checkbox
          label={`${t.income.incomesRegisterConsent} *`}
          checked={formData.lookupConsent}
          onChange={(value) => onChange({ ...formData, lookupConsent: value })}
        />
      </FixedSpaceColumn>
    </Indent>
  )
}

function Attachments({
  requiredAttachments,
  attachments,
  onUploaded,
  onDeleted
}: {
  requiredAttachments: AttachmentType[]
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
    <ContentArea opaque>
      <FixedSpaceColumn>
        <H3>{t.income.attachments.title}</H3>
        <p>{t.income.attachments.description}</p>
        {requiredAttachments.length > 0 && (
          <>
            <H4>{t.income.attachments.required.title}</H4>
            <UnorderedList>
              {requiredAttachments.map((attachmentType) => (
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
        <Gap size="s" />
      </FixedSpaceColumn>
    </ContentArea>
  )
}

export function computeRequiredAttachments(
  formData: Form.IncomeStatementForm
): AttachmentType[] {
  const { gross, entrepreneur } = formData

  const result: AttachmentType[] = []
  if (gross.selected) {
    if (gross.incomeSource === 'ATTACHMENTS') result.push('PAYSLIP')
    if (gross.otherIncome) result.push(...gross.otherIncome)
    if (gross.alimony) result.push('ALIMONY_PAYOUT')
  }
  if (entrepreneur.selected) {
    if (entrepreneur.startupGrant) result.push('STARTUP_GRANT')
    if (
      entrepreneur.selfEmployed.selected &&
      !entrepreneur.selfEmployed.estimation
    ) {
      result.push('PROFIT_AND_LOSS_STATEMENT')
    }
    if (
      entrepreneur.limitedCompany.selected &&
      entrepreneur.limitedCompany.incomeSource === 'ATTACHMENTS'
    ) {
      result.push('PAYSLIP', 'ACCOUNTANT_REPORT')
    }
    if (entrepreneur.partnership.selected) {
      result.push('PROFIT_AND_LOSS_STATEMENT', 'ACCOUNTANT_REPORT')
    }
  }
  return result
}

const HighestFeeInfo = styled.div`
  background-color: ${(props) => props.theme.colors.brand.secondaryLight};
  margin: 0 -${defaultMargins.L} ${defaultMargins.s} -${defaultMargins.L};
  padding: ${defaultMargins.s} ${defaultMargins.s} ${defaultMargins.s}
    ${defaultMargins.XXL};
`

const Indent = styled.div`
  width: 100%;
  padding-left: ${defaultMargins.XL};
`

const OtherIncomeWrapper = styled(Indent)`
  max-width: 480px;
`
