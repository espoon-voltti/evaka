// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback, useContext } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'

import { Attachment } from 'lib-common/api-types/attachment'
import { ChildIncome } from 'lib-common/api-types/incomeStatement'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
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
import { getAttachmentBlob } from '../attachments'
import { useTranslation } from '../localization'
import { OverlayContext } from '../overlay/state'

import { getChildIncomeStatement } from './api'

export default React.memo(function ChildIncomeStatementView() {
  const { childId, incomeStatementId } =
    useParams<{ childId: UUID; incomeStatementId: UUID }>()
  const t = useTranslation()
  const history = useHistory()
  const [result] = useApiState(
    () => getChildIncomeStatement(childId, incomeStatementId),
    [childId, incomeStatementId]
  )

  const handleEdit = useCallback(() => {
    history.push(`/child-income/${childId}/${incomeStatementId}/edit`)
  }, [history, childId, incomeStatementId])

  return renderResult(result, (incomeStatement) => (
    <Container>
      <ReturnButton label={t.common.return} />
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
    </Container>
  ))
})

const ChildIncomeInfo = React.memo(function IncomeInfo({
  incomeStatement
}: {
  incomeStatement: ChildIncome
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
  const { setErrorMessage } = useContext(OverlayContext)
  const t = useTranslation()
  const onFileUnavailable = () =>
    setErrorMessage({
      title: t.fileDownload.modalHeader,
      text: t.fileDownload.modalMessage,
      type: 'error'
    })
  return (
    <FixedSpaceColumn>
      {files.map((file) => (
        <div key={file.id}>
          <FileIcon icon={fileIcon(file)} />
          <FileDownloadButton
            file={file}
            fileFetchFn={getAttachmentBlob}
            onFileUnavailable={onFileUnavailable}
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
