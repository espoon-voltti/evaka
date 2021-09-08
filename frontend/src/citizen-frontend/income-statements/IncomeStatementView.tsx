import React from 'react'
import { RouteComponentProps } from 'react-router'
import { UUID } from 'lib-common/types'
import { Translations, useTranslation } from '../localization'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H1, H2, H3, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import Container, { ContentArea } from 'lib-components/layout/Container'
import styled from 'styled-components'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { getIncomeStatement } from './api'
import {
  Accountant,
  Entrepreneur,
  EstimatedIncome,
  Gross,
  Income,
  IncomeStatement
} from 'lib-common/api-types/incomeStatement'
import { Loading, Result } from 'lib-common/api'
import Loader from 'lib-components/atoms/Loader'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { Attachment } from 'lib-common/api-types/attachment'
import FileDownloadButton from 'lib-components/molecules/FileDownloadButton'
import { fileIcon } from 'lib-components/molecules/FileUpload'
import { getAttachmentBlob } from '../attachments'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'

export default React.memo(function IncomeStatementView({
  match
}: RouteComponentProps<{ incomeStatementId: UUID }>) {
  const { incomeStatementId } = match.params
  const t = useTranslation()
  const [incomeStatement, setIncomeStatement] = React.useState<
    Result<IncomeStatement>
  >(Loading.of())

  const loadIncomeStatement = useRestApi(getIncomeStatement, setIncomeStatement)

  React.useEffect(() => {
    loadIncomeStatement(incomeStatementId)
  }, [loadIncomeStatement, incomeStatementId])

  return incomeStatement.mapAll({
    loading() {
      return <Loader />
    },
    failure() {
      return <ErrorSegment />
    },
    success(incomeStatement) {
      return (
        <Container>
          <Gap size="s" />
          <ContentArea opaque>
            <H1>{t.income.view.title}</H1>
            <Row
              label={t.income.view.startDate}
              value={incomeStatement.startDate.format()}
            />
            <Row
              label={t.income.view.feeBasis}
              value={t.income.view.statementTypes[incomeStatement.type]}
            />
            {incomeStatement.type === 'INCOME' && (
              <IncomeInfo incomeStatement={incomeStatement} />
            )}
          </ContentArea>
        </Container>
      )
    }
  })
})

function IncomeInfo({ incomeStatement }: { incomeStatement: Income }) {
  const t = useTranslation()
  const yesno = makeYesNo(t)
  return (
    <>
      {incomeStatement.gross && (
        <>
          <HorizontalLine />
          <GrossIncome gross={incomeStatement.gross} />
        </>
      )}
      {incomeStatement.entrepreneur && (
        <>
          <HorizontalLine />
          <EntrepreneurIncome entrepreneur={incomeStatement.entrepreneur} />
        </>
      )}
      <HorizontalLine />
      <H2>{t.income.view.otherInfoTitle}</H2>
      <Row
        label={t.income.view.student}
        value={yesno(incomeStatement.student)}
      />
      <Row
        label={t.income.view.alimonyPayer}
        value={yesno(incomeStatement.alimonyPayer)}
      />
      <Row
        label={t.income.view.otherInfo}
        value={incomeStatement.otherInfo || '-'}
      />
      <HorizontalLine />
      <CitizenAttachments attachments={incomeStatement.attachments} />
    </>
  )
}

function GrossIncome({ gross }: { gross: Gross }) {
  const t = useTranslation()
  return (
    <>
      <H2>{t.income.view.grossTitle}</H2>
      <Row
        label={t.income.view.incomeSource}
        value={
          gross.incomeSource === 'INCOMES_REGISTER'
            ? t.income.view.incomesRegister
            : t.income.view.attachmentsAndKela
        }
      />
      {gross.estimatedIncome && (
        <Row
          label={t.income.view.grossEstimatedIncome}
          value={<EstimatedIncome estimatedIncome={gross.estimatedIncome} />}
        />
      )}
      <Row
        label={t.income.view.otherIncome}
        value={
          <>
            {gross.otherIncome.map((incomeType) => (
              <Item key={incomeType}>
                {t.income.grossIncome.otherIncomeTypes[incomeType]}
              </Item>
            ))}
          </>
        }
      />
    </>
  )
}

function EstimatedIncome({
  estimatedIncome
}: {
  estimatedIncome: EstimatedIncome
}) {
  const t = useTranslation()
  return (
    <FixedSpaceRow>
      <FixedSpaceColumn>
        <Label>{t.income.view.estimatedMonthlyIncome}</Label>
        <div>{estimatedIncome.estimatedMonthlyIncome}</div>
      </FixedSpaceColumn>
      <FixedSpaceColumn>
        <Label>{t.income.view.timeRange}</Label>
        <div>
          {estimatedIncome.incomeStartDate.format()} -{' '}
          {estimatedIncome.incomeEndDate?.format()}
        </div>
      </FixedSpaceColumn>
    </FixedSpaceRow>
  )
}

function EntrepreneurIncome({ entrepreneur }: { entrepreneur: Entrepreneur }) {
  const t = useTranslation()
  const yesno = makeYesNo(t)

  return (
    <>
      <H2>{t.income.view.entrepreneurTitle}</H2>
      <Row
        label={t.income.view.fullTimeLabel}
        value={
          entrepreneur.fullTime
            ? t.income.view.fullTime
            : t.income.view.partTime
        }
      />
      <Row
        label={t.income.view.startOfEntrepreneurship}
        value={entrepreneur.startOfEntrepreneurship.format()}
      />
      <Row
        label={t.income.view.spouseWorksInCompany}
        value={yesno(entrepreneur.spouseWorksInCompany)}
      />
      <Row
        label={t.income.view.startupGrant}
        value={yesno(entrepreneur.startupGrant)}
      />
      <Row
        label={t.income.view.kelaInspectionConsent}
        value={yesno(entrepreneur.checkupConsent)}
      />
      <H3>{t.income.view.companyInfoTitle}</H3>
      <Row
        label={t.income.view.companyType}
        value={
          <>
            {entrepreneur.selfEmployed && (
              <Item>{t.income.view.selfEmployed}</Item>
            )}
            {entrepreneur.limitedCompany && (
              <Item>{t.income.view.limitedCompany}</Item>
            )}
            {entrepreneur.partnership && (
              <Item>{t.income.view.partnership}</Item>
            )}
            {entrepreneur.lightEntrepreneur && (
              <Item>{t.income.view.lightEntrepreneur}</Item>
            )}
          </>
        }
      />
      <Row label={t.income.view.incomeSource} value="" />
      {entrepreneur.selfEmployed && (
        <Row
          light
          label={t.income.view.selfEmployed}
          value={
            <>
              {entrepreneur.selfEmployed.attachments && (
                <Item>{t.income.view.selfEmployedAttachments}</Item>
              )}
              {entrepreneur.selfEmployed.estimatedIncome && (
                <EstimatedIncome
                  estimatedIncome={entrepreneur.selfEmployed.estimatedIncome}
                />
              )}
            </>
          }
        />
      )}
      {entrepreneur.limitedCompany && (
        <Row
          light
          label={t.income.view.limitedCompany}
          value={
            entrepreneur.limitedCompany.incomeSource === 'INCOMES_REGISTER'
              ? t.income.view.limitedCompanyIncomesRegister
              : t.income.view.limitedCompanyAttachments
          }
        />
      )}
      {entrepreneur.partnership && (
        <Row
          light
          label={t.income.view.partnership}
          value={t.income.view.attachments}
        />
      )}
      {entrepreneur.lightEntrepreneur && (
        <Row
          light
          label={t.income.view.lightEntrepreneur}
          value={t.income.view.attachments}
        />
      )}
      {entrepreneur.accountant && (
        <AccountantInfo accountant={entrepreneur.accountant} />
      )}
    </>
  )
}

function AccountantInfo({ accountant }: { accountant: Accountant }) {
  const t = useTranslation()
  return (
    <>
      <H3>{t.income.view.accountantTitle}</H3>
      <Row label={t.income.view.accountant} value={accountant.name} />
      <Row label={t.income.view.email} value={accountant.email} />
      <Row label={t.income.view.phone} value={accountant.phone} />
      <Row label={t.income.view.address} value={accountant.address} />
    </>
  )
}

function CitizenAttachments({ attachments }: { attachments: Attachment[] }) {
  const t = useTranslation()
  return (
    <>
      <H2>{t.income.view.citizenAttachments.title}</H2>
      {attachments.length === 0 ? (
        <p>{t.income.view.citizenAttachments.noAttachments}</p>
      ) : (
        <Row
          label={`${t.income.view.attachments}:`}
          value={<UploadedFiles files={attachments} />}
        />
      )}
    </>
  )
}

function UploadedFiles({ files }: { files: Attachment[] }) {
  return (
    <FixedSpaceColumn>
      {files.map((file) => (
        <div key={file.id}>
          <FileIcon icon={fileIcon(file)} />
          <FileDownloadButton
            file={file}
            fileFetchFn={getAttachmentBlob}
            onFileUnavailable={() => undefined}
          />
        </div>
      ))}
    </FixedSpaceColumn>
  )
}

const FileIcon = styled(FontAwesomeIcon)`
  color: ${(p) => p.theme.colors.main.primary};
  margin-right: ${defaultMargins.s};
`

function Row({
  label,
  light,
  value
}: {
  label: string
  light?: boolean
  value: React.ReactNode
}) {
  return (
    <>
      <FixedSpaceRow>
        <LabelColumn light={light}>{label}</LabelColumn>
        <div>{value}</div>
      </FixedSpaceRow>
      <Gap size="s" />
    </>
  )
}

function makeYesNo(t: Translations) {
  return (value: boolean): string => {
    return value ? t.common.yes : t.common.no
  }
}

const LabelColumn = styled(Label)<{ light?: boolean }>`
  flex: 0 0 auto;
  width: 250px;
  ${(p) => (p.light ? 'font-weight: 400;' : '')}
`

const Item = styled.div`
  margin-bottom: ${defaultMargins.xs};
`
