// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback, useMemo, useState } from 'react'
import { useNavigate } from 'react-router'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import type { Attachment } from 'lib-common/generated/api-types/attachment'
import type {
  Accountant,
  Entrepreneur,
  Gross,
  IncomeStatement,
  IncomeStatementAttachment,
  SetIncomeStatementHandledBody
} from 'lib-common/generated/api-types/incomestatement'
import {
  EstimatedIncome,
  incomeStatementAttachmentTypes
} from 'lib-common/generated/api-types/incomestatement'
import type {
  IncomeStatementId,
  PersonId
} from 'lib-common/generated/api-types/shared'
import {
  numAttachments,
  toIncomeStatementAttachments
} from 'lib-common/income-statements/attachments'
import {
  computeRequiredAttachments,
  fromIncomeStatement
} from 'lib-common/income-statements/form'
import { useQueryResult } from 'lib-common/query'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import FileDownloadButton from 'lib-components/molecules/FileDownloadButton'
import FileUpload, { fileIcon } from 'lib-components/molecules/FileUpload'
import { H1, H2, H3, Label, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

import {
  getAttachmentUrl,
  incomeStatementAttachment
} from '../../api/attachments'
import { personIdentityQuery } from '../../queries'
import type { Translations } from '../../state/i18n'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import {
  incomeStatementQuery,
  setIncomeStatementHandledMutation
} from './queries'

export default React.memo(function IncomeStatementPage() {
  const personId = useIdRouteParam<PersonId>('personId')
  const incomeStatementId =
    useIdRouteParam<IncomeStatementId>('incomeStatementId')
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const person = useQueryResult(personIdentityQuery({ personId }))
  const incomeStatement = useQueryResult(
    incomeStatementQuery({ incomeStatementId })
  )

  const navigateToPersonProfile = useCallback(
    ({ type }: IncomeStatement) =>
      navigate(
        `/${
          type !== 'CHILD_INCOME' ? 'profile' : 'child-information'
        }/${personId}`
      ),
    [navigate, personId]
  )

  return (
    <Container>
      {renderResult(
        combine(person, incomeStatement),
        ([person, incomeStatement]) => (
          <>
            <ContentArea opaque>
              <H1>{i18n.titles.incomeStatement}</H1>
              <H2>
                {person.firstName} {person.lastName}
              </H2>
              <Row
                label={i18n.incomeStatement.startDate}
                value={incomeStatement.startDate.format()}
              />
              <Row
                label={i18n.incomeStatement.feeBasis}
                value={
                  i18n.incomeStatement.statementTypes[incomeStatement.type]
                }
              />
              {incomeStatement.type === 'INCOME' && (
                <IncomeInfo incomeStatement={incomeStatement} />
              )}
              {incomeStatement.type === 'CHILD_INCOME' && (
                <ChildIncomeInfo incomeStatement={incomeStatement} />
              )}
            </ContentArea>
            {incomeStatement.type === 'INCOME' && (
              <>
                <Gap size="L" />
                <ContentArea opaque>
                  <EmployeeAttachments
                    incomeStatementId={incomeStatement.id}
                    attachments={incomeStatement.attachments.filter(
                      (attachment) => attachment.uploadedByEmployee
                    )}
                  />
                </ContentArea>
              </>
            )}
            <Gap size="L" />
            <ContentArea opaque>
              <HandlerNotesForm
                incomeStatementId={incomeStatementId}
                onSuccess={() => navigateToPersonProfile(incomeStatement)}
                initialValues={{
                  handled: incomeStatement.status === 'HANDLED',
                  handlerNote: incomeStatement.handlerNote
                }}
              />
            </ContentArea>
          </>
        )
      )}
    </Container>
  )
})

function IncomeInfo({
  incomeStatement
}: {
  incomeStatement: IncomeStatement.Income
}) {
  const { i18n } = useTranslation()
  const yesno = makeYesNo(i18n)
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
      <H2>{i18n.incomeStatement.otherInfoTitle}</H2>
      <Row
        label={i18n.incomeStatement.student}
        value={yesno(incomeStatement.student)}
      />
      <Row
        label={i18n.incomeStatement.alimonyPayer}
        value={yesno(incomeStatement.alimonyPayer)}
      />
      <Row
        label={i18n.incomeStatement.otherInfo}
        value={incomeStatement.otherInfo || '-'}
      />
      <HorizontalLine />
      <CitizenAttachments
        incomeStatement={incomeStatement}
        attachments={incomeStatement.attachments.filter(
          (attachment) => !attachment.uploadedByEmployee
        )}
      />
    </>
  )
}

function GrossIncome({ gross }: { gross: Gross }) {
  const { i18n } = useTranslation()
  if (gross.type === 'INCOME') {
    return (
      <>
        <H2>{i18n.incomeStatement.grossTitle}</H2>
        <Row
          label={i18n.incomeStatement.incomeSource}
          value={
            gross.incomeSource === 'INCOMES_REGISTER'
              ? i18n.incomeStatement.incomesRegister
              : i18n.incomeStatement.attachmentsAndKela
          }
        />
        <Row
          label={i18n.incomeStatement.grossEstimatedIncome}
          value={gross.estimatedMonthlyIncome}
        />
        <Row
          label={i18n.incomeStatement.otherIncome}
          value={
            <>
              {gross.otherIncome.map((incomeType) => (
                <Item key={incomeType}>
                  {i18n.incomeStatement.otherIncomeTypes[incomeType]}
                </Item>
              ))}
              {gross.otherIncome.length === 0 && '-'}
            </>
          }
        />
        {gross.otherIncome.length > 0 && (
          <Row
            label={i18n.incomeStatement.otherIncomeInfo}
            value={gross.otherIncomeInfo}
          />
        )}
      </>
    )
  } else {
    return (
      <>
        <H2>{i18n.incomeStatement.grossTitle}</H2>
        <Row
          label={i18n.incomeStatement.incomeSource}
          value={i18n.incomeStatement.noIncomeTitle}
        />
        <Row
          label={i18n.incomeStatement.noIncomeDescription}
          value={gross.noIncomeDescription}
        />
      </>
    )
  }
}

function EstimatedIncome({
  estimatedIncome
}: {
  estimatedIncome: EstimatedIncome
}) {
  const { i18n } = useTranslation()
  return (
    <FixedSpaceRow>
      <FixedSpaceColumn>
        <Label>{i18n.incomeStatement.estimatedMonthlyIncome}</Label>
        <div>{estimatedIncome.estimatedMonthlyIncome}</div>
      </FixedSpaceColumn>
      <FixedSpaceColumn>
        <Label>{i18n.incomeStatement.timeRange}</Label>
        <div>
          {estimatedIncome.incomeStartDate.format()} -{' '}
          {estimatedIncome.incomeEndDate?.format()}
        </div>
      </FixedSpaceColumn>
    </FixedSpaceRow>
  )
}

function EntrepreneurIncome({ entrepreneur }: { entrepreneur: Entrepreneur }) {
  const { i18n } = useTranslation()
  const yesno = makeYesNo(i18n)

  return (
    <>
      <H2>{i18n.incomeStatement.entrepreneurTitle}</H2>
      <Row
        label={i18n.incomeStatement.startOfEntrepreneurship}
        value={entrepreneur.startOfEntrepreneurship.format()}
      />
      {entrepreneur.companyName !== '' && (
        <Row
          label={i18n.incomeStatement.companyName}
          value={entrepreneur.companyName}
        />
      )}
      {entrepreneur.businessId !== '' && (
        <Row
          label={i18n.incomeStatement.businessId}
          value={entrepreneur.businessId}
        />
      )}
      <Row
        label={i18n.incomeStatement.spouseWorksInCompany}
        value={yesno(entrepreneur.spouseWorksInCompany)}
      />
      <Row
        label={i18n.incomeStatement.startupGrant}
        value={yesno(entrepreneur.startupGrant)}
      />
      {entrepreneur.checkupConsent && (
        <Row
          label={i18n.incomeStatement.checkupConsentLabel}
          value={i18n.incomeStatement.checkupConsent}
        />
      )}
      <H3>{i18n.incomeStatement.companyInfoTitle}</H3>
      <Row
        label={i18n.incomeStatement.companyType}
        value={
          <>
            {entrepreneur.selfEmployed && (
              <Item>{i18n.incomeStatement.selfEmployed}</Item>
            )}
            {entrepreneur.limitedCompany && (
              <Item>{i18n.incomeStatement.limitedCompany}</Item>
            )}
            {entrepreneur.partnership && (
              <Item>{i18n.incomeStatement.partnership}</Item>
            )}
            {entrepreneur.lightEntrepreneur && (
              <Item>{i18n.incomeStatement.lightEntrepreneur}</Item>
            )}
          </>
        }
      />
      <Row label={i18n.incomeStatement.incomeSource} value="" />
      {entrepreneur.selfEmployed && (
        <Row
          light
          label={i18n.incomeStatement.selfEmployed}
          value={
            <>
              {entrepreneur.selfEmployed.attachments && (
                <Item>{i18n.incomeStatement.selfEmployedAttachments}</Item>
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
          label={i18n.incomeStatement.limitedCompany}
          value={
            entrepreneur.limitedCompany.incomeSource === 'INCOMES_REGISTER'
              ? i18n.incomeStatement.limitedCompanyIncomesRegister
              : i18n.incomeStatement.limitedCompanyAttachments
          }
        />
      )}
      {entrepreneur.partnership && (
        <Row
          light
          label={i18n.incomeStatement.partnership}
          value={i18n.incomeStatement.attachments}
        />
      )}
      {entrepreneur.lightEntrepreneur && (
        <Row
          light
          label={i18n.incomeStatement.lightEntrepreneur}
          value={i18n.incomeStatement.attachments}
        />
      )}
      {entrepreneur.accountant && (
        <AccountantInfo accountant={entrepreneur.accountant} />
      )}
    </>
  )
}

function AccountantInfo({ accountant }: { accountant: Accountant }) {
  const { i18n } = useTranslation()
  return (
    <>
      <H3>{i18n.incomeStatement.accountantTitle}</H3>
      <Row label={i18n.incomeStatement.accountant} value={accountant.name} />
      <Row label={i18n.incomeStatement.email} value={accountant.email} />
      <Row label={i18n.incomeStatement.phone} value={accountant.phone} />
      <Row label={i18n.incomeStatement.address} value={accountant.address} />
    </>
  )
}

function ChildIncomeInfo({
  incomeStatement
}: {
  incomeStatement: IncomeStatement.ChildIncome
}) {
  const { i18n } = useTranslation()
  return (
    <>
      <H2>{i18n.incomeStatement.otherInfoTitle}</H2>
      <Row
        label={i18n.incomeStatement.otherInfo}
        value={incomeStatement.otherInfo || '-'}
        dataQa="other-info"
      />
      <HorizontalLine />
      <CitizenAttachments
        incomeStatement={incomeStatement}
        attachments={incomeStatement.attachments.filter(
          (attachment) => !attachment.uploadedByEmployee
        )}
      />
    </>
  )
}

const CitizenAttachments = React.memo(function CitizenAttachments({
  incomeStatement,
  attachments
}: {
  incomeStatement: IncomeStatement
  attachments: IncomeStatementAttachment[]
}) {
  const { i18n } = useTranslation()
  const incomeStatementAttachments = toIncomeStatementAttachments(attachments)
  const requiredAttachments = useMemo(
    () => computeRequiredAttachments(fromIncomeStatement(incomeStatement)),
    [incomeStatement]
  )
  const noAttachments = numAttachments(incomeStatementAttachments) === 0
  return (
    <>
      <H2>{i18n.incomeStatement.citizenAttachments.title}</H2>
      {noAttachments ? (
        <p data-qa="no-attachments">
          {i18n.incomeStatement.citizenAttachments.noAttachments}
        </p>
      ) : !incomeStatementAttachments.typed ? (
        <Row
          label={`${i18n.incomeStatement.attachments}:`}
          value={<UploadedFiles files={attachments} />}
          dataQa="attachments"
        />
      ) : (
        <Table>
          <Tbody>
            {incomeStatementAttachmentTypes.map((attachmentType) => {
              const attachments =
                incomeStatementAttachments.attachmentsByType[attachmentType]
              const attachmentMissing = !attachments?.length

              if (
                attachmentMissing &&
                !requiredAttachments.has(attachmentType)
              ) {
                return null
              } else {
                return (
                  <Tr key={attachmentType}>
                    <Td>
                      {i18n.incomeStatement.attachmentNames[attachmentType]}
                    </Td>
                    <Td>
                      {attachmentMissing ? (
                        i18n.incomeStatement.citizenAttachments
                          .attachmentMissing
                      ) : (
                        <UploadedFiles files={attachments} />
                      )}
                    </Td>
                  </Tr>
                )
              }
            })}
          </Tbody>
        </Table>
      )}
    </>
  )
})

function UploadedFiles({ files }: { files: Attachment[] }) {
  return (
    <FixedSpaceColumn>
      {files.map((file) => (
        <div key={file.id} data-qa="attachment">
          <FileIcon icon={fileIcon(file)} />
          <FileDownloadButton file={file} getFileUrl={getAttachmentUrl} />
        </div>
      ))}
    </FixedSpaceColumn>
  )
}

const FileIcon = styled(FontAwesomeIcon)`
  color: ${(p) => p.theme.colors.main.m2};
  margin-right: ${defaultMargins.s};
`

function EmployeeAttachments({
  incomeStatementId,
  attachments
}: {
  incomeStatementId: IncomeStatementId
  attachments: Attachment[]
}) {
  const { i18n } = useTranslation()

  return (
    <>
      <H1>{i18n.incomeStatement.employeeAttachments.title}</H1>
      <P>{i18n.incomeStatement.employeeAttachments.description}</P>
      <FileUpload
        files={attachments}
        uploadHandler={incomeStatementAttachment(incomeStatementId, 'OTHER')}
        getDownloadUrl={getAttachmentUrl}
      />
    </>
  )
}

function HandlerNotesForm({
  incomeStatementId,
  onSuccess,
  initialValues
}: {
  incomeStatementId: IncomeStatementId
  onSuccess: () => void
  initialValues: SetIncomeStatementHandledBody
}) {
  const { i18n } = useTranslation()
  const [state, setState] = useState(initialValues)

  return (
    <FixedSpaceColumn data-qa="handler-notes-form">
      <H2>{i18n.incomeStatement.handlerNotesForm.title}</H2>

      <Checkbox
        label={i18n.incomeStatement.handlerNotesForm.handled}
        checked={state.handled}
        onChange={(handled) => setState((old) => ({ ...old, handled }))}
        data-qa="set-handled"
      />

      <Label htmlFor="handler-note">
        {i18n.incomeStatement.handlerNotesForm.handlerNote}
      </Label>
      <InputField
        id="handler-note"
        type="text"
        width="L"
        value={state.handlerNote}
        onChange={(handlerNote) => setState((old) => ({ ...old, handlerNote }))}
      />

      <MutateButton
        mutation={setIncomeStatementHandledMutation}
        primary
        onClick={() => ({
          incomeStatementId,
          body: state
        })}
        onSuccess={onSuccess}
        text={i18n.common.save}
        textInProgress={i18n.common.saving}
      />
    </FixedSpaceColumn>
  )
}

function Row({
  label,
  light,
  value,
  dataQa
}: {
  label: string
  light?: boolean
  value: React.ReactNode
  dataQa?: string
}) {
  return (
    <>
      <FixedSpaceRow>
        <LabelColumn light={light}>{label}</LabelColumn>
        <div data-qa={dataQa}>{value}</div>
      </FixedSpaceRow>
      <Gap size="s" />
    </>
  )
}

function makeYesNo(i18n: Translations) {
  return (value: boolean): string => (value ? i18n.common.yes : i18n.common.no)
}

const LabelColumn = styled(Label)<{ light?: boolean }>`
  flex: 0 0 auto;
  width: 250px;
  ${(p) => (p.light ? 'font-weight: 400;' : '')}
`

const Item = styled.div`
  margin-bottom: ${defaultMargins.xs};
`
