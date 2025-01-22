// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useState } from 'react'
import styled from 'styled-components'

import { wrapResult } from 'lib-common/api'
import { Attachment } from 'lib-common/generated/api-types/attachment'
import { PedagogicalDocument } from 'lib-common/generated/api-types/pedagogicaldocument'
import {
  ChildId,
  PedagogicalDocumentId
} from 'lib-common/generated/api-types/shared'
import { EvakaUser } from 'lib-common/generated/api-types/user'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import Tooltip from 'lib-components/atoms/Tooltip'
import { Button } from 'lib-components/atoms/buttons/Button'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import TextArea from 'lib-components/atoms/form/TextArea'
import { Td, Tr } from 'lib-components/layout/Table'
import FileUpload from 'lib-components/molecules/FileUpload'
import { defaultMargins } from 'lib-components/white-space'
import { faPen, faTrash } from 'lib-icons'

import {
  getAttachmentUrl,
  pedagogicalDocumentAttachment
} from '../../api/attachments'
import { updatePedagogicalDocument } from '../../generated/api-clients/pedagogicaldocument'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'

const updatePedagogicalDocumentResult = wrapResult(updatePedagogicalDocument)

interface Props {
  id: PedagogicalDocumentId
  childId: ChildId
  attachments: Attachment[]
  description: string
  createdAt: HelsinkiDateTime
  createdBy: EvakaUser
  modifiedAt: HelsinkiDateTime
  modifiedBy: EvakaUser
  initInEditMode: boolean
  onReload: () => void
  onDelete: (d: PedagogicalDocument) => void
}

const PedagogicalDocumentRow = React.memo(function PedagogicalDocument({
  id,
  childId,
  attachments,
  description,
  createdAt,
  createdBy,
  modifiedAt,
  modifiedBy,
  initInEditMode,
  onReload,
  onDelete
}: Props) {
  const { i18n } = useTranslation()
  const t = i18n.childInformation.pedagogicalDocument

  const [pedagogicalDocument, setPedagogicalDocument] =
    useState<PedagogicalDocument>({
      id,
      childId,
      attachments,
      description,
      createdAt,
      createdBy,
      modifiedAt,
      modifiedBy
    })

  const [editMode, setEditMode] = useState(initInEditMode)
  const { clearUiMode, setErrorMessage } = useContext(UIContext)
  const [submitting, setSubmitting] = useState(false)

  const endEdit = useCallback(() => {
    setEditMode(false)
    clearUiMode()
  }, [clearUiMode])

  const updateDocument = useCallback(() => {
    const { childId, description } = pedagogicalDocument
    if (!description) return
    setSubmitting(true)
    void updatePedagogicalDocumentResult({
      documentId: id,
      body: { childId, description }
    }).then((res) => {
      setSubmitting(false)
      if (res.isSuccess) {
        endEdit()
        setPedagogicalDocument(res.value)
        onReload()
      } else {
        setErrorMessage({
          type: 'error',
          title: i18n.common.error.unknown,
          text: i18n.common.error.saveFailed,
          resolveLabel: i18n.common.ok
        })
      }
    })
  }, [endEdit, i18n, id, onReload, pedagogicalDocument, setErrorMessage])

  return (
    <Tr key={pedagogicalDocument.id} data-qa="table-pedagogical-document-row">
      <DateTd data-qa="pedagogical-document-start-date">
        <Tooltip
          tooltip={t.createdBy(pedagogicalDocument.createdBy.name)}
          position="right"
        >
          {pedagogicalDocument.createdAt.toLocalDate().format()}
        </Tooltip>
      </DateTd>
      <DateTd data-qa="pedagogical-document-modified-date">
        <Tooltip
          tooltip={t.lastModifiedBy(pedagogicalDocument.modifiedBy.name)}
          position="right"
        >
          {pedagogicalDocument.modifiedAt.format()}
        </Tooltip>
      </DateTd>
      <DescriptionTd data-qa="pedagogical-document-description">
        {editMode ? (
          <TextArea
            value={pedagogicalDocument.description}
            onChange={(description) =>
              setPedagogicalDocument((old) => ({ ...old, description }))
            }
          />
        ) : (
          pedagogicalDocument.description
        )}
      </DescriptionTd>
      <NameTd data-qa="pedagogical-document-document">
        <AttachmentsContainer>
          <FileUpload
            slim
            disabled={submitting}
            data-qa="upload-pedagogical-document-attachment-new"
            files={attachments}
            getDownloadUrl={getAttachmentUrl}
            uploadHandler={pedagogicalDocumentAttachment(id)}
            onUploaded={() => {
              setSubmitting(false)
              onReload()
            }}
            onDeleted={() =>
              setPedagogicalDocument(({ ...rest }) => ({
                ...rest,
                attachment: null
              }))
            }
            allowedFileTypes={['image', 'document', 'audio', 'video']}
          />
        </AttachmentsContainer>
      </NameTd>
      <ActionsTd data-qa="pedagogical-document-actions">
        {editMode ? (
          <InlineButtons>
            <Button
              appearance="inline"
              data-qa="pedagogical-document-button-save"
              onClick={updateDocument}
              text={i18n.common.save}
              disabled={!pedagogicalDocument.description.length || submitting}
            />
            <Button
              appearance="inline"
              data-qa="pedagogical-document-button-cancel"
              onClick={() => {
                endEdit()
              }}
              text={i18n.common.cancel}
              disabled={submitting}
            />
          </InlineButtons>
        ) : (
          <InlineButtons>
            <IconOnlyButton
              data-qa="pedagogical-document-button-edit"
              onClick={() => setEditMode(true)}
              icon={faPen}
              disabled={submitting}
              aria-label={i18n.common.edit}
            />
            <IconOnlyButton
              data-qa="pedagogical-document-button-delete"
              onClick={() => onDelete(pedagogicalDocument)}
              icon={faTrash}
              disabled={submitting}
              aria-label={i18n.common.remove}
            />
          </InlineButtons>
        )}
      </ActionsTd>
    </Tr>
  )
})

export default PedagogicalDocumentRow

const InlineButtons = styled.div`
  display: flex;
  justify-content: space-evenly;
  gap: ${defaultMargins.s};
`

const DateTd = styled(Td)`
  width: 15%;
`

const NameTd = styled(Td)`
  width: 20%;
`

const DescriptionTd = styled(Td)`
  width: 45%;
`

const ActionsTd = styled(Td)`
  width: 20%;
`
const AttachmentsContainer = styled.div`
  display: flex;
  flex-direction: column;
`
