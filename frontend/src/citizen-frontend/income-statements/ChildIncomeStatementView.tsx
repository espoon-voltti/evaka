// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useMemo, useState } from 'react'
import { useNavigate } from 'react-router'
import styled from 'styled-components'

import { Attachment } from 'lib-common/generated/api-types/attachment'
import {
  IncomeStatement,
  IncomeStatementAttachmentType
} from 'lib-common/generated/api-types/incomestatement'
import { IncomeStatementId } from 'lib-common/generated/api-types/shared'
import {
  collectAttachmentIds,
  IncomeStatementAttachments,
  numAttachments,
  toIncomeStatementAttachments
} from 'lib-common/income-statements'
import { useQueryResult } from 'lib-common/query'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Main from 'lib-components/atoms/Main'
import { Button } from 'lib-components/atoms/buttons/Button'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import InputField from 'lib-components/atoms/form/InputField'
import Container, { ContentArea } from 'lib-components/layout/Container'
import ListGrid from 'lib-components/layout/ListGrid'
import { Table, Tbody, Td, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import FileDownloadButton from 'lib-components/molecules/FileDownloadButton'
import { fileIcon } from 'lib-components/molecules/FileUpload'
import { H1, H2, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

import { renderResult } from '../async-rendering'
import { getAttachmentUrl } from '../attachments'
import { useTranslation } from '../localization'

import {
  AttachmentSection,
  IncomeStatementUntypedAttachments,
  makeAttachmentHandler
} from './IncomeStatementAttachments'
import { SetStateCallback } from './IncomeStatementComponents'
import {
  incomeStatementQuery,
  updateSentIncomeStatementMutation
} from './queries'

export default React.memo(function ChildIncomeStatementView() {
  const incomeStatementId =
    useIdRouteParam<IncomeStatementId>('incomeStatementId')
  const t = useTranslation()
  const result = useQueryResult(incomeStatementQuery({ incomeStatementId }))

  return renderResult(result, (incomeStatement) => (
    <Container>
      <ReturnButton label={t.common.return} />
      <Main>
        <ContentArea opaque>
          <FixedSpaceRow spacing="L">
            <H1>{t.income.view.title}</H1>
          </FixedSpaceRow>
          <Row
            label={t.income.view.startDate}
            value={incomeStatement.startDate.format()}
            data-qa="start-date"
          />
          <Row
            label={t.income.view.feeBasis}
            value={t.income.view.statementTypes[incomeStatement.type]}
          />
          {incomeStatement.type === 'CHILD_INCOME' && (
            <ChildIncomeInfo incomeStatement={incomeStatement} />
          )}
        </ContentArea>
      </Main>
    </Container>
  ))
})

const ChildIncomeInfo = React.memo(function IncomeInfo({
  incomeStatement
}: {
  incomeStatement: IncomeStatement.ChildIncome
}) {
  const t = useTranslation()
  const navigate = useNavigate()

  const editable = incomeStatement.status !== 'HANDLED'

  const [otherInfo, setOtherInfo] = useState(() => incomeStatement.otherInfo)
  const [incomeStatementAttachments, setIncomeStatementAttachments] = useState(
    () => toIncomeStatementAttachments(incomeStatement.attachments)
  )

  return (
    <>
      <HorizontalLine />
      <Row
        label={t.income.view.otherInfo}
        value={editable ? '' : incomeStatement.otherInfo || '-'}
      />
      {editable && (
        <InputField
          value={otherInfo}
          onChange={setOtherInfo}
          placeholder={t.common.write}
        />
      )}
      <HorizontalLine />
      {editable ? (
        <CitizenAttachmentsWithUpload
          incomeStatementId={incomeStatement.id}
          incomeStatementAttachments={incomeStatementAttachments}
          onChange={setIncomeStatementAttachments}
        />
      ) : (
        <CitizenAttachments
          incomeStatementAttachments={incomeStatementAttachments}
        />
      )}
      {editable && (
        <>
          <Gap size="L" />
          <FixedSpaceRow justifyContent="flex-end">
            <Button
              text={t.common.cancel}
              onClick={() => navigate('/income')}
            />
            <MutateButton
              primary
              text={t.common.save}
              mutation={updateSentIncomeStatementMutation}
              onClick={() => ({
                incomeStatementId: incomeStatement.id,
                body: {
                  otherInfo,
                  attachmentIds: collectAttachmentIds(
                    incomeStatementAttachments
                  )
                }
              })}
              onSuccess={() => navigate('/income')}
            />
          </FixedSpaceRow>
        </>
      )}
    </>
  )
})

const CitizenAttachments = React.memo(function CitizenAttachments({
  incomeStatementAttachments
}: {
  incomeStatementAttachments: IncomeStatementAttachments
}) {
  const t = useTranslation()
  const noAttachments = numAttachments(incomeStatementAttachments) === 0
  return (
    <>
      <H2>{t.income.view.citizenAttachments.title}</H2>
      {noAttachments ? (
        <p>{t.income.view.citizenAttachments.noAttachments}</p>
      ) : !incomeStatementAttachments.typed ? (
        <Row
          label={`${t.income.view.attachments}:`}
          value={
            <UploadedFiles
              files={incomeStatementAttachments.untypedAttachments}
            />
          }
        />
      ) : (
        <Table>
          <Tbody>
            {Object.entries(incomeStatementAttachments.attachmentsByType).map(
              ([type, attachments]) => {
                const attachmentType = type as IncomeStatementAttachmentType
                return (
                  <Tr key={attachmentType}>
                    <Td>
                      {t.income.attachments.attachmentNames[attachmentType]}
                    </Td>
                    <Td>
                      <UploadedFiles files={attachments} />
                    </Td>
                  </Tr>
                )
              }
            )}
          </Tbody>
        </Table>
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

const CitizenAttachmentsWithUpload = React.memo(function CitizenAttachments({
  incomeStatementId,
  incomeStatementAttachments,
  onChange
}: {
  incomeStatementId: IncomeStatementId
  incomeStatementAttachments: IncomeStatementAttachments
  onChange: SetStateCallback<IncomeStatementAttachments>
}) {
  const t = useTranslation()
  const attachmentHandler = useMemo(
    () =>
      makeAttachmentHandler(
        incomeStatementId,
        incomeStatementAttachments,
        onChange
      ),
    [incomeStatementAttachments, incomeStatementId, onChange]
  )
  return (
    <>
      <H2>{t.income.view.citizenAttachments.title}</H2>
      {!incomeStatementAttachments.typed ? (
        <IncomeStatementUntypedAttachments
          incomeStatementId={incomeStatementId}
          requiredAttachments={new Set()}
          attachments={incomeStatementAttachments}
          onChange={onChange}
        />
      ) : (
        Object.keys(incomeStatementAttachments.attachmentsByType).map(
          (type) => {
            const attachmentType = type as IncomeStatementAttachmentType
            return (
              <AttachmentSection
                key={attachmentType}
                attachmentType={attachmentType}
                showFormErrors={false}
                attachmentHandler={attachmentHandler}
                labelKey="attachmentNames"
              />
            )
          }
        )
      )}
    </>
  )
})

const FileIcon = styled(FontAwesomeIcon)`
  color: ${(p) => p.theme.colors.main.m2};
  margin-right: ${defaultMargins.s};
`

const Row = React.memo(function Row({
  label,
  light,
  value,
  'data-qa': dataQa
}: {
  label: string
  light?: boolean
  value: React.ReactNode
  'data-qa'?: string
}) {
  return (
    <>
      <ListGrid>
        <LabelColumn light={light}>{label}</LabelColumn>
        <div data-qa={dataQa}>{value}</div>
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
