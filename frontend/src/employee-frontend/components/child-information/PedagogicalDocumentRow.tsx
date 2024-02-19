// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useState } from 'react'
import styled from 'styled-components'

import { wrapResult } from 'lib-common/api'
import {
  Attachment,
  PedagogicalDocument
} from 'lib-common/generated/api-types/pedagogicaldocument'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { UUID } from 'lib-common/types'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import TextArea from 'lib-components/atoms/form/TextArea'
import { Td, Tr } from 'lib-components/layout/Table'
import FileUpload from 'lib-components/molecules/FileUpload'
import { defaultMargins } from 'lib-components/white-space'
import { faPen, faTrash } from 'lib-icons'

import {
  deleteAttachment,
  getAttachmentUrl,
  savePedagogicalDocumentAttachment
} from '../../api/attachments'
import { updatePedagogicalDocument } from '../../generated/api-clients/pedagogicaldocument'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'

const updatePedagogicalDocumentResult = wrapResult(updatePedagogicalDocument)

interface Props {
  id: UUID
  childId: UUID
  attachments: Attachment[]
  description: string
  created: HelsinkiDateTime
  updated: HelsinkiDateTime
  initInEditMode: boolean
  onReload: () => void
  onDelete: (d: PedagogicalDocument) => void
}

const PedagogicalDocumentRow = React.memo(function PedagogicalDocument({
  id,
  childId,
  attachments,
  description,
  created,
  updated,
  initInEditMode,
  onReload,
  onDelete
}: Props) {
  const { i18n } = useTranslation()
  const [pedagogicalDocument, setPedagogicalDocument] =
    useState<PedagogicalDocument>({
      id,
      childId,
      attachments,
      description,
      created,
      updated
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

  const handleAttachmentUpload = useCallback(
    async (file: File, onUploadProgress: (percentage: number) => void) => {
      setSubmitting(true)
      return (
        await savePedagogicalDocumentAttachment(id, file, onUploadProgress)
      ).map((id) => {
        setSubmitting(false)
        onReload()
        return id
      })
    },
    [id, onReload]
  )

  const handleAttachmentDelete = useCallback(
    async (id: UUID) =>
      (await deleteAttachment(id)).map(() =>
        setPedagogicalDocument(({ ...rest }) => ({
          ...rest,
          attachment: null
        }))
      ),
    []
  )

  return (
    <Tr key={pedagogicalDocument.id} data-qa="table-pedagogical-document-row">
      <DateTd data-qa="pedagogical-document-start-date">
        {pedagogicalDocument.created.toLocalDate().format()}
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
            onUpload={handleAttachmentUpload}
            onDelete={handleAttachmentDelete}
            allowedFileTypes={['image', 'document', 'audio', 'video']}
          />
        </AttachmentsContainer>
      </NameTd>
      <ActionsTd data-qa="pedagogical-document-actions">
        {editMode ? (
          <InlineButtons>
            <InlineButton
              data-qa="pedagogical-document-button-save"
              onClick={updateDocument}
              text={i18n.common.save}
              disabled={!pedagogicalDocument.description.length || submitting}
            />
            <InlineButton
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
            <IconButton
              data-qa="pedagogical-document-button-edit"
              onClick={() => setEditMode(true)}
              icon={faPen}
              disabled={submitting}
              aria-label={i18n.common.edit}
            />
            <IconButton
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
