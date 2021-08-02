import React from 'react'
import styled from 'styled-components'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { H1, H2, H3, Label } from 'lib-components/typography'
import { useLang, useTranslation } from '../localization'
import Footer from '../Footer'
import Radio from 'lib-components/atoms/form/Radio'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import Button from 'lib-components/atoms/buttons/Button'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import DatePicker from '../../lib-components/molecules/date-picker/DatePicker'
import { formatDate } from '../../lib-common/date'
import Checkbox from '../../lib-components/atoms/form/Checkbox'
import MultiSelect from '../../lib-components/atoms/form/MultiSelect'
import InputField from '../../lib-components/atoms/form/InputField'
import { otherIncome } from './types/common'
import * as Form from './types/form'
import { errorToInputInfo, validDate, validInt } from '../form-validation'
import { createIncomeStatement } from './api'
import AsyncButton from '../../lib-components/atoms/buttons/AsyncButton'

const initialFormData: Form.IncomeStatementForm = {
  startDate: formatDate(new Date()),
  incomeType: null
}

function highestFeeIncome(startDate: string): Form.HighestFee {
  return { startDate, incomeType: 'HIGHEST_FEE' }
}

function grossIncome(startDate: string): Form.Gross {
  return {
    startDate,
    incomeType: 'GROSS',
    incomeSource: null,
    otherIncome: null
  }
}

function entrepreneurIncome(startDate: string): Form.Entrepreneur {
  return {
    startDate,
    incomeType: 'ENTREPRENEUR_EMPTY',
    estimatedMonthlyIncome: '',
    incomeStartDate: '',
    incomeEndDate: '',
    incomeSource: null
  }
}

export default function IncomeStatementForm() {
  const t = useTranslation()
  const [formData, setFormData] = React.useState(initialFormData)

  const validatedData = Form.toIncomeStatementBody(formData)
  const isValid = validatedData !== null

  const save = () =>
    validatedData ? createIncomeStatement(validatedData) : Promise.resolve()

  return (
    <>
      <Container>
        <Gap size="s" />
        <ContentArea opaque paddingVertical="L">
          <H1 noMargin>{t.income.title}</H1>
          {t.income.description}
        </ContentArea>
        <Gap size="s" />
        <ContentArea opaque paddingVertical="L">
          <FixedSpaceColumn>
            <H2>{t.income.incomeInfo}</H2>
            <IncomeTypeSelection formData={formData} onChange={setFormData} />
            {formData.incomeType === 'GROSS' && (
              <>
                <HorizontalLine slim />
                <GrossIncomeSelection
                  formData={formData}
                  onChange={setFormData}
                />
              </>
            )}
            {Form.isEntrepreneur(formData) && (
              <>
                <HorizontalLine slim />
                <EntrepreneurIncomeSelection
                  formData={formData}
                  onChange={setFormData}
                />
              </>
            )}
            <Gap size="L" />
            <FixedSpaceRow>
              <Button text={t.common.cancel} />
              <AsyncButton
                text={t.common.save}
                primary
                onClick={save}
                disabled={!isValid}
                onSuccess={() => undefined}
              />
            </FixedSpaceRow>
          </FixedSpaceColumn>
        </ContentArea>
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
    <>
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
      <Radio
        label={t.income.incomeType.agreeToHighestFee}
        checked={formData.incomeType === 'HIGHEST_FEE'}
        onChange={() => onChange(highestFeeIncome(formData.startDate))}
      />
      {formData.incomeType === 'HIGHEST_FEE' && (
        <HighestFeeInfo>{t.income.incomeType.highestFeeInfo}</HighestFeeInfo>
      )}
      <Radio
        label={t.income.incomeType.grossIncome}
        checked={formData.incomeType === 'GROSS'}
        onChange={() => onChange(grossIncome(formData.startDate))}
      />
      <Radio
        label={t.income.incomeType.entrepreneurIncome}
        checked={Form.isEntrepreneur(formData)}
        onChange={() => onChange(entrepreneurIncome(formData.startDate))}
      />
    </>
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
    <>
      <H3>{t.income.grossIncome.title}</H3>
      <p>{t.income.grossIncome.description}</p>
      <Radio
        label={t.income.grossIncome.consentIncomesRegister}
        checked={formData.incomeSource === 'INCOMES_REGISTER'}
        onChange={() =>
          onChange({ ...formData, incomeSource: 'INCOMES_REGISTER' })
        }
      />
      <Radio
        label={t.income.grossIncome.provideAttachments}
        checked={formData.incomeSource === 'ATTACHMENTS'}
        onChange={() => onChange({ ...formData, incomeSource: 'ATTACHMENTS' })}
      />
      <Gap size="L" />
      <p>{t.income.grossIncome.otherIncomeInfo}</p>
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
    </>
  )
}

function adjustEntrepreneurFormRadioButtons(
  formData: Form.Entrepreneur
): Form.Entrepreneur {
  if (formData.incomeType.startsWith('ENTREPRENEUR_SELF_EMPLOYED_')) {
    return { ...formData, incomeSource: null }
  }
  if (formData.incomeType === 'ENTREPRENEUR_PARTNERSHIP') {
    return { ...formData, incomeSource: null }
  }
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
  const handleChange = (value: Form.Entrepreneur) => {
    onChange(adjustEntrepreneurFormRadioButtons(value))
  }
  return (
    <>
      <H3>{t.income.entrepreneurIncome.title}</H3>
      <Radio
        label={t.income.entrepreneurIncome.selfEmployed}
        checked={formData.incomeType.startsWith('ENTREPRENEUR_SELF_EMPLOYED_')}
        onChange={() =>
          handleChange({
            ...formData,
            incomeType: 'ENTREPRENEUR_SELF_EMPLOYED_EMPTY'
          })
        }
      />
      <SelfEmployedIncomeSelection
        formData={formData}
        onChange={handleChange}
      />
      <Radio
        label={t.income.entrepreneurIncome.limitedCompany}
        checked={formData.incomeType === 'ENTREPRENEUR_LIMITED_COMPANY'}
        onChange={() =>
          handleChange({
            ...formData,
            incomeType: 'ENTREPRENEUR_LIMITED_COMPANY'
          })
        }
      />
      <LimitedCompanyIncomeSelection
        formData={formData}
        onChange={handleChange}
      />
      <Radio
        label={t.income.entrepreneurIncome.partnership}
        checked={formData.incomeType === 'ENTREPRENEUR_PARTNERSHIP'}
        onChange={() =>
          handleChange({ ...formData, incomeType: 'ENTREPRENEUR_PARTNERSHIP' })
        }
      />
    </>
  )
}

function SelfEmployedIncomeSelection({
  formData,
  onChange
}: {
  formData: Form.Entrepreneur
  onChange: (value: Form.Entrepreneur) => void
}) {
  const t = useTranslation()
  const [lang] = useLang()
  return (
    <Indent>
      <FixedSpaceColumn>
        <Radio
          label={t.income.selfEmployed.attachments}
          checked={
            formData.incomeType === 'ENTREPRENEUR_SELF_EMPLOYED_ATTACHMENTS'
          }
          onChange={() =>
            onChange({
              ...formData,
              incomeType: 'ENTREPRENEUR_SELF_EMPLOYED_ATTACHMENTS'
            })
          }
        />
        <Radio
          label={t.income.selfEmployed.estimation}
          checked={
            formData.incomeType === 'ENTREPRENEUR_SELF_EMPLOYED_ESTIMATION'
          }
          onChange={() =>
            onChange({
              ...formData,
              incomeType: 'ENTREPRENEUR_SELF_EMPLOYED_ESTIMATION'
            })
          }
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
                onFocus={() =>
                  onChange({
                    ...formData,
                    incomeType: 'ENTREPRENEUR_SELF_EMPLOYED_ESTIMATION'
                  })
                }
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
                  onFocus={() =>
                    onChange({
                      ...formData,
                      incomeType: 'ENTREPRENEUR_SELF_EMPLOYED_ESTIMATION'
                    })
                  }
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
                  onFocus={() =>
                    onChange({
                      ...formData,
                      incomeType: 'ENTREPRENEUR_SELF_EMPLOYED_ESTIMATION'
                    })
                  }
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
      </FixedSpaceColumn>
    </Indent>
  )
}

function LimitedCompanyIncomeSelection({
  formData,
  onChange
}: {
  formData: Form.Entrepreneur
  onChange: (value: Form.Entrepreneur) => void
}) {
  const t = useTranslation()
  return (
    <Indent>
      <FixedSpaceColumn>
        <div>{t.income.limitedCompany.info}</div>
        <Radio
          label={t.income.limitedCompany.incomesRegister}
          checked={formData.incomeSource === 'INCOMES_REGISTER'}
          onChange={() =>
            onChange({
              ...formData,
              incomeType: 'ENTREPRENEUR_LIMITED_COMPANY',
              incomeSource: 'INCOMES_REGISTER'
            })
          }
        />
        <Radio
          label={t.income.limitedCompany.attachments}
          checked={formData.incomeSource === 'ATTACHMENTS'}
          onChange={() =>
            onChange({
              ...formData,
              incomeType: 'ENTREPRENEUR_LIMITED_COMPANY',
              incomeSource: 'ATTACHMENTS'
            })
          }
        />
      </FixedSpaceColumn>
    </Indent>
  )
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
