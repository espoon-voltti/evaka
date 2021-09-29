// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useState } from 'react'
import { useTranslation } from '../../state/i18n'
import { UUID } from '../../types'
import { Td, Tr } from 'lib-components/layout/Table'
import {
  Attachment,
  PedagogicalDocument
} from 'lib-common/generated/api-types/pedadocument'
import FileUpload from 'lib-components/molecules/FileUpload'
import {
  deleteAttachment,
  getAttachmentBlob,
  savePedagogicalDocumentAttachment
} from '../../api/attachments'
import TextArea from 'lib-components/atoms/form/TextArea'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { faPen, faTrash } from 'lib-icons'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import styled from 'styled-components'
import {
  deletePedagogicalDocument,
  getPedagogicalDocument,
  updatePedagogicalDocument
} from '../../api/child/pedagogical-documents'
import { UIContext } from '../../state/ui'
import { defaultMargins } from 'lib-components/white-space'
import LocalDate from "../../../lib-common/local-date";

interface Props {
  id: UUID
  childId: UUID
  attachment: Attachment | null
  description: string
  created: Date
  updated: Date
  initInEditMode: boolean
  handleRemovedDocument: () => void
}

const PedagogicalDocumentRow = React.memo(function PedagogicalDocument({
  id,
  childId,
  attachment,
  description,
  created,
  updated,
  initInEditMode,
  handleRemovedDocument
}: Props) {
  const { i18n } = useTranslation()
  const [pedagogicalDocument, setPedagogicalDocument] =
    useState<PedagogicalDocument>({
      id,
      childId,
      attachment,
      description,
      created,
      updated
    })

  const [editMode, setEditMode] = useState(initInEditMode)
  const { clearUiMode } = useContext(UIContext)

  const updateDocument = () => {
    const { childId, description } = pedagogicalDocument
    void updatePedagogicalDocument(id, { childId, description })
      .then(() => getPedagogicalDocument(id))
      .then((res) => res.map(setPedagogicalDocument))
  }

  const cancelEdit = () => {
    void getPedagogicalDocument(id).then((res) =>
      res.map(setPedagogicalDocument)
    )
  }

  const deleteDocument = () => {
    const attachmentId = pedagogicalDocument?.attachment?.id
    if (!attachmentId)
      void deletePedagogicalDocument(id).then(handleRemovedDocument)
    else {
      void deleteAttachment(attachmentId)
        .then(() => deletePedagogicalDocument(id))
        .then(handleRemovedDocument)
    }
  }

  const handleAttachmentUpload = useCallback(
    async (
      file: File,
      onUploadProgress: (progressEvent: ProgressEvent) => void
    ) => {
      return (
        await savePedagogicalDocumentAttachment(id, file, onUploadProgress)
      ).map((id) => {
        setPedagogicalDocument(({ ...rest }) => ({
          ...rest,
          attachment: { id, name: file.name, contentType: file.type }
        }))
        return id
      })
    },
    [id]
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

  const endEdit = () => {
    setEditMode(false)
    clearUiMode()
  }

  return (
    <Tr
      key={`${pedagogicalDocument.id}`}
      data-qa="table-pedagogical-document-row"
    >
      <DateTd data-qa="pedagogical-document-start-date">
        {LocalDate.fromSystemTzDate(pedagogicalDocument.created).format()}
      </DateTd>
      <NameTd data-qa="pedagogical-document-document">
        {
          <FileUpload
            slimSingleFile
            disabled={false}
            data-qa="upload-pedagogical-document-attachment"
            files={
              pedagogicalDocument.attachment
                ? [pedagogicalDocument.attachment]
                : []
            }
            i18n={i18n.fileUpload}
            onDownloadFile={getAttachmentBlob}
            onUpload={handleAttachmentUpload}
            onDelete={handleAttachmentDelete}
          />
        }
      </NameTd>
      <DescriptionTd data-qa="pedagogical-document-description">
        {editMode ? (
          <TextArea
            value={pedagogicalDocument.description}
            onChange={(e) =>
              setPedagogicalDocument((old) => ({
                ...old,
                description: e.target.value
              }))
            }
          />
        ) : (
          pedagogicalDocument.description
        )}
      </DescriptionTd>
      <ActionsTd data-qa="pedagogical-document-actions">
        {editMode ? (
          <InlineButtons>
            <InlineButton
              onClick={() => {
                updateDocument()
                endEdit()
              }}
              text={'Tallenna'}
            />
            <InlineButton
              onClick={() => {
                cancelEdit()
                endEdit()
              }}
              text={'Peruuta'}
            />
          </InlineButtons>
        ) : (
          <InlineButtons>
            <IconButton onClick={() => setEditMode(true)} icon={faPen} />
            <IconButton onClick={() => deleteDocument()} icon={faTrash} />
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
