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
  updatePedagogicalDocument
} from '../../api/child/pedagogical-documents'
import { UIContext } from '../../state/ui'
import { defaultMargins } from 'lib-components/white-space'
import LocalDate from 'lib-common/local-date'

interface Props {
  id: UUID
  childId: UUID
  attachment: Attachment | null
  description: string
  created: Date
  updated: Date
  initInEditMode: boolean
  onReload: () => void
}

const PedagogicalDocumentRow = React.memo(function PedagogicalDocument({
  id,
  childId,
  attachment,
  description,
  created,
  updated,
  initInEditMode,
  onReload
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
  const { clearUiMode, setErrorMessage } = useContext(UIContext)
  const [submitting, setSubmitting] = useState(false)

  const updateDocument = () => {
    const { childId, description } = pedagogicalDocument
    setSubmitting(true)
    void updatePedagogicalDocument(id, { childId, description })
      .then((res) => {
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
  }

  const deleteDocument = () => {
    const attachmentId = pedagogicalDocument?.attachment?.id
    if (!attachmentId)
      void deletePedagogicalDocument(id).then(onReload)
    else {
      void deleteAttachment(attachmentId)
        .then(() => deletePedagogicalDocument(id))
        .then(onReload)
    }
  }

  const handleAttachmentUpload = useCallback(
    async (
      file: File,
      onUploadProgress: (progressEvent: ProgressEvent) => void
    ) => {
      setSubmitting(true)
      return (
        await savePedagogicalDocumentAttachment(id, file, onUploadProgress)
      ).map((id) => {
        setSubmitting(false)
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
            disabled={submitting}
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
              onClick={updateDocument}
              text={'Tallenna'}
              disabled={submitting}
            />
            <InlineButton
              onClick={() => {
                endEdit()
              }}
              text={'Peruuta'}
              disabled={submitting}
            />
          </InlineButtons>
        ) : (
          <InlineButtons>
            <IconButton onClick={() => setEditMode(true)} icon={faPen} disabled={submitting}/>
            <IconButton onClick={() => deleteDocument()} icon={faTrash} disabled={submitting}/>
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
