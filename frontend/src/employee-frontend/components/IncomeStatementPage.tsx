// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback, useState } from 'react'
import { RouteComponentProps, useHistory } from 'react-router'
import styled from 'styled-components'

import { combine, Result } from 'lib-common/api'
import { Attachment } from 'lib-common/api-types/attachment'
import {
  Accountant,
  Entrepreneur,
  EstimatedIncome,
  Gross,
  Income,
  ChildIncome
} from 'lib-common/api-types/incomeStatement'
import { SetIncomeStatementHandledBody } from 'lib-common/generated/api-types/incomestatement'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import Container, { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import FileDownloadButton from 'lib-components/molecules/FileDownloadButton'
import FileUpload, { fileIcon } from 'lib-components/molecules/FileUpload'
import { H1, H2, H3, Label, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

import {
  deleteAttachment,
  getAttachmentBlob,
  saveIncomeStatementAttachment
} from '../api/attachments'
import {
  getIncomeStatement,
  updateIncomeStatementHandled
} from '../api/income-statement'
import { getPerson } from '../api/person'
import { Translations, useTranslation } from '../state/i18n'

import { renderResult } from './async-rendering'

export default React.memo(function IncomeStatementPage({
  match
}: RouteComponentProps<{ personId: UUID; incomeStatementId: UUID }>) {
  const { personId, incomeStatementId } = match.params
  const { i18n } = useTranslation()
  const history = useHistory()

  const [person] = useApiState(() => getPerson(personId), [personId])
  const [incomeStatement, loadIncomeStatement] = useApiState(
    () => getIncomeStatement(personId, incomeStatementId),
    [personId, incomeStatementId]
  )

  const onUpdateHandled = useCallback(
    (params: SetIncomeStatementHandledBody) =>
      updateIncomeStatementHandled(incomeStatementId, params),
    [incomeStatementId]
  )

  const navigateToPersonProfile = useCallback(
    () => history.push(`/profile/${personId}`),
    [history, personId]
  )

  return (
    <Container>
      {renderResult(
        combine(person, incomeStatement),
        ([person, incomeStatement]) => (
          <>
            <ContentArea opaque>
              <H1>{i18n.incomeStatement.title}</H1>
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
                    onUploaded={loadIncomeStatement}
                    onDeleted={loadIncomeStatement}
                  />
                </ContentArea>
              </>
            )}
            <Gap size="L" />
            <ContentArea opaque>
              <HandlerNotesForm
                onSave={onUpdateHandled}
                onSuccess={navigateToPersonProfile}
                initialValues={{
                  handled: incomeStatement.handled,
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

function IncomeInfo({ incomeStatement }: { incomeStatement: Income }) {
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
        attachments={incomeStatement.attachments.filter(
          (attachment) => !attachment.uploadedByEmployee
        )}
      />
    </>
  )
}

function GrossIncome({ gross }: { gross: Gross }) {
  const { i18n } = useTranslation()
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
        label={i18n.incomeStatement.fullTimeLabel}
        value={
          entrepreneur.fullTime
            ? i18n.incomeStatement.fullTime
            : i18n.incomeStatement.partTime
        }
      />
      <Row
        label={i18n.incomeStatement.startOfEntrepreneurship}
        value={entrepreneur.startOfEntrepreneurship.format()}
      />
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
  incomeStatement: ChildIncome
}) {
  const { i18n } = useTranslation()
  return (
    <>
      <H2>{i18n.incomeStatement.otherInfoTitle}</H2>
      <Row
        label={i18n.incomeStatement.otherInfo}
        value={incomeStatement.otherInfo || '-'}
      />
      <HorizontalLine />
      <CitizenAttachments
        attachments={incomeStatement.attachments.filter(
          (attachment) => !attachment.uploadedByEmployee
        )}
      />
    </>
  )
}

function CitizenAttachments({ attachments }: { attachments: Attachment[] }) {
  const { i18n } = useTranslation()
  return (
    <>
      <H2>{i18n.incomeStatement.citizenAttachments.title}</H2>
      {attachments.length === 0 ? (
        <p>{i18n.incomeStatement.citizenAttachments.noAttachments}</p>
      ) : (
        <Row
          label={`${i18n.incomeStatement.attachments}:`}
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
  color: ${(p) => p.theme.colors.main.m2};
  margin-right: ${defaultMargins.s};
`

function EmployeeAttachments({
  incomeStatementId,
  attachments,
  onUploaded,
  onDeleted
}: {
  incomeStatementId: UUID
  attachments: Attachment[]
  onUploaded: (attachment: Attachment) => void
  onDeleted: (id: UUID) => void
}) {
  const { i18n } = useTranslation()

  const handleUpload = useCallback(
    async (
      file: File,
      onUploadProgress: (progressEvent: ProgressEvent) => void
    ) =>
      (
        await saveIncomeStatementAttachment(
          incomeStatementId,
          file,
          onUploadProgress
        )
      ).map((id) => {
        onUploaded({ id, name: file.name, contentType: file.type })
        return id
      }),
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
    <>
      <H1>{i18n.incomeStatement.employeeAttachments.title}</H1>
      <P>{i18n.incomeStatement.employeeAttachments.description}</P>
      <FileUpload
        files={attachments}
        onUpload={handleUpload}
        onDelete={handleDelete}
        onDownloadFile={getAttachmentBlob}
        i18n={i18n.fileUpload}
      />
    </>
  )
}

function HandlerNotesForm({
  onSave,
  onSuccess,
  initialValues
}: {
  onSave: (params: SetIncomeStatementHandledBody) => Promise<Result<void>>
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

      <AsyncButton
        primary
        onClick={() => onSave(state)}
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

function makeYesNo(i18n: Translations) {
  return (value: boolean): string => {
    return value ? i18n.common.yes : i18n.common.no
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
