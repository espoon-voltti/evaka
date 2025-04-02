// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useQueryClient } from '@tanstack/react-query'
import React, { useState } from 'react'
import styled from 'styled-components'

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
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import TextArea from 'lib-components/atoms/form/TextArea'
import { Td, Tr } from 'lib-components/layout/Table'
import { ConfirmedMutation } from 'lib-components/molecules/ConfirmedMutation'
import FileUpload from 'lib-components/molecules/FileUpload'
import { defaultMargins } from 'lib-components/white-space'
import { faPen, faTrash } from 'lib-icons'

import {
  getAttachmentUrl,
  pedagogicalDocumentAttachment
} from '../../api/attachments'
import { useTranslation } from '../../state/i18n'

import {
  childPedagogicalDocumentsQuery,
  deletePedagogicalDocumentMutation,
  updatePedagogicalDocumentMutation
} from './queries'

interface Props {
  id: PedagogicalDocumentId
  childId: ChildId
  attachments: Attachment[]
  description: string
  createdAt: HelsinkiDateTime
  createdBy: EvakaUser
  modifiedAt: HelsinkiDateTime
  modifiedBy: EvakaUser
  editing: boolean
  onStartEditing: () => void
  onStopEditing: () => void
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
  editing,
  onStartEditing,
  onStopEditing
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

  const queryClient = useQueryClient()

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
        {editing ? (
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
            data-qa="upload-pedagogical-document-attachment-new"
            files={attachments}
            getDownloadUrl={getAttachmentUrl}
            uploadHandler={pedagogicalDocumentAttachment(id)}
            onUploaded={() => {
              void queryClient.invalidateQueries({
                queryKey: childPedagogicalDocumentsQuery({ childId }),
                type: 'all'
              })
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
        {editing ? (
          <InlineButtons>
            <MutateButton
              appearance="inline"
              text={i18n.common.save}
              mutation={updatePedagogicalDocumentMutation}
              onClick={() => ({
                documentId: id,
                body: {
                  childId,
                  description: pedagogicalDocument.description
                }
              })}
              onSuccess={onStopEditing}
              disabled={!pedagogicalDocument.description.length}
              data-qa="pedagogical-document-button-save"
            />
            <Button
              appearance="inline"
              data-qa="pedagogical-document-button-cancel"
              onClick={onStopEditing}
              text={i18n.common.cancel}
            />
          </InlineButtons>
        ) : (
          <InlineButtons>
            <IconOnlyButton
              data-qa="pedagogical-document-button-edit"
              onClick={onStartEditing}
              icon={faPen}
              aria-label={i18n.common.edit}
            />
            <ConfirmedMutation
              buttonStyle="ICON"
              icon={faTrash}
              buttonAltText={i18n.common.remove}
              confirmationTitle={
                i18n.childInformation.pedagogicalDocument.removeConfirmation
              }
              confirmationText={
                i18n.childInformation.pedagogicalDocument.removeConfirmationText
              }
              mutation={deletePedagogicalDocumentMutation}
              onClick={() => ({
                documentId: id,
                childId
              })}
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
