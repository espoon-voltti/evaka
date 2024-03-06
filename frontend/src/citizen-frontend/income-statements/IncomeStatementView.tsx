// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { Attachment } from 'lib-common/api-types/attachment'
import {
  Accountant,
  Entrepreneur,
  EstimatedIncome,
  Gross,
  IncomeStatement
} from 'lib-common/generated/api-types/incomestatement'
import { useQueryResult } from 'lib-common/query'
import useRouteParams from 'lib-common/useRouteParams'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Main from 'lib-components/atoms/Main'
import ResponsiveInlineButton from 'lib-components/atoms/buttons/ResponsiveInlineButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Container, { ContentArea } from 'lib-components/layout/Container'
import ListGrid from 'lib-components/layout/ListGrid'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import FileDownloadButton from 'lib-components/molecules/FileDownloadButton'
import { fileIcon } from 'lib-components/molecules/FileUpload'
import { H1, H2, H3, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faPen } from 'lib-icons'

import { renderResult } from '../async-rendering'
import { getAttachmentUrl } from '../attachments'
import { useTranslation } from '../localization'

import { incomeStatementQuery } from './queries'

export default React.memo(function IncomeStatementView() {
  const { incomeStatementId } = useRouteParams(['incomeStatementId'])
  const t = useTranslation()
  const navigate = useNavigate()
  const result = useQueryResult(incomeStatementQuery(incomeStatementId))

  const handleEdit = useCallback(() => {
    navigate('edit')
  }, [navigate])

  return renderResult(result, (incomeStatement) => (
    <Container>
      <ReturnButton label={t.common.return} />
      <Main>
        <ContentArea opaque>
          <FixedSpaceRow spacing="L">
            <H1>{t.income.view.title}</H1>
            {!incomeStatement.handled && (
              <EditButtonContainer>
                <ResponsiveInlineButton
                  text={t.common.edit}
                  icon={faPen}
                  onClick={handleEdit}
                />
              </EditButtonContainer>
            )}
          </FixedSpaceRow>
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
      </Main>
    </Container>
  ))
})

const IncomeInfo = React.memo(function IncomeInfo({
  incomeStatement
}: {
  incomeStatement: IncomeStatement.Income
}) {
  const t = useTranslation()
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
        value={t.common.yesno(incomeStatement.student)}
      />
      <Row
        label={t.income.view.alimonyPayer}
        value={t.common.yesno(incomeStatement.alimonyPayer)}
      />
      <Row
        label={t.income.view.otherInfo}
        value={incomeStatement.otherInfo || '-'}
      />
      <HorizontalLine />
      <CitizenAttachments attachments={incomeStatement.attachments} />
    </>
  )
})

const GrossIncome = React.memo(function GrossIncome({
  gross
}: {
  gross: Gross
}) {
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
      <Row
        label={t.income.view.grossEstimatedIncome}
        value={gross.estimatedMonthlyIncome}
      />
      <Row
        label={t.income.view.otherIncome}
        value={
          <>
            {gross.otherIncome.map((incomeType) => (
              <Item key={incomeType}>
                {t.income.grossIncome.otherIncomeTypes[incomeType]}
              </Item>
            ))}
            {gross.otherIncome.length === 0 && '-'}
          </>
        }
      />
      {gross.otherIncome.length > 0 && (
        <>
          <Row
            label={t.income.view.otherIncomeInfo}
            value={gross.otherIncomeInfo}
          />
        </>
      )}
    </>
  )
})

const EstimatedIncome = React.memo(function EstimatedIncome({
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
})

const EntrepreneurIncome = React.memo(function EntrepreneurIncome({
  entrepreneur
}: {
  entrepreneur: Entrepreneur
}) {
  const t = useTranslation()

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
        value={t.common.yesno(entrepreneur.spouseWorksInCompany)}
      />
      <Row
        label={t.income.view.startupGrant}
        value={t.common.yesno(entrepreneur.startupGrant)}
      />
      {entrepreneur.checkupConsent && (
        <Row
          label={t.income.view.checkupConsentLabel}
          value={t.income.view.checkupConsent}
        />
      )}
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
})

const AccountantInfo = React.memo(function AccountantInfo({
  accountant
}: {
  accountant: Accountant
}) {
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
})

const CitizenAttachments = React.memo(function CitizenAttachments({
  attachments
}: {
  attachments: Attachment[]
}) {
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
})

const UploadedFiles = React.memo(function UploadedFiles({
  files
}: {
  files: Attachment[]
}) {
  return (
    <FixedSpaceColumn>
      {files.map((file) => (
        <div key={file.id}>
          <FileIcon icon={fileIcon(file)} />
          <FileDownloadButton file={file} getFileUrl={getAttachmentUrl} />{' '}
        </div>
      ))}
    </FixedSpaceColumn>
  )
})

const FileIcon = styled(FontAwesomeIcon)`
  color: ${(p) => p.theme.colors.main.m2};
  margin-right: ${defaultMargins.s};
`

const EditButtonContainer = styled.div`
  flex: 1 0 auto;
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
`

const Row = React.memo(function Row({
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
      <ListGrid>
        <LabelColumn light={light}>{label}</LabelColumn>
        <div>{value}</div>
      </ListGrid>
      <Gap size="s" />
    </>
  )
})

const LabelColumn = styled(Label)<{ light?: boolean }>`
  flex: 0 0 auto;
  width: 250px;
  ${(p) => (p.light ? 'font-weight: 400;' : '')}
`

const Item = styled.div`
  margin-bottom: ${defaultMargins.xs};
`
