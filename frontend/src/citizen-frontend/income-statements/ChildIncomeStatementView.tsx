// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { Attachment } from 'lib-common/api-types/attachment'
import { IncomeStatement } from 'lib-common/generated/api-types/incomestatement'
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
import { H1, H2, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faPen } from 'lib-icons'

import { renderResult } from '../async-rendering'
import { getAttachmentUrl } from '../attachments'
import { useTranslation } from '../localization'

import { childIncomeStatementQuery } from './queries'

export default React.memo(function ChildIncomeStatementView() {
  const { childId, incomeStatementId } = useRouteParams([
    'childId',
    'incomeStatementId'
  ])
  const t = useTranslation()
  const navigate = useNavigate()
  const result = useQueryResult(
    childIncomeStatementQuery({ childId, id: incomeStatementId })
  )

  const handleEdit = useCallback(() => {
    navigate(`/child-income/${childId}/${incomeStatementId}/edit`)
  }, [navigate, childId, incomeStatementId])

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
                  data-qa="edit-button"
                />
              </EditButtonContainer>
            )}
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
  return (
    <>
      <HorizontalLine />
      <Row
        label={t.income.view.otherInfo}
        value={incomeStatement.otherInfo || '-'}
        data-qa="other-info"
      />
      <HorizontalLine />
      <CitizenAttachments attachments={incomeStatement.attachments} />
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
          <FileDownloadButton
            file={file}
            getFileUrl={getAttachmentUrl}
            data-qa="attachment-download-button"
          />
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
