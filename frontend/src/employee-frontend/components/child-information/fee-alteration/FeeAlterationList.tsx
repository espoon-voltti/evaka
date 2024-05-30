// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { Fragment } from 'react'
import styled from 'styled-components'

import { getAttachmentUrl } from 'employee-frontend/api/attachments'
import { Result } from 'lib-common/api'
import { Attachment } from 'lib-common/api-types/attachment'
import {
  FeeAlteration,
  FeeAlterationWithPermittedActions
} from 'lib-common/generated/api-types/invoicing'
import { UUID } from 'lib-common/types'
import { IconButton } from 'lib-components/atoms/buttons/IconButton'
import ListGrid from 'lib-components/layout/ListGrid'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import FileDownloadButton from 'lib-components/molecules/FileDownloadButton'
import { fileIcon } from 'lib-components/molecules/FileUpload'
import { H4, Label, fontSizesMobile } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faPen, faTrash } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'

import FeeAlterationEditor from './FeeAlterationEditor'

interface Props {
  feeAlterations: FeeAlterationWithPermittedActions[]
  toggleEditing: (id: UUID) => void
  isEdited: (id: UUID) => boolean
  cancel: () => void
  update: (v: FeeAlteration) => Promise<Result<unknown>>
  onSuccess: () => void
  onFailure?: () => void
  toggleDeleteModal: (v: FeeAlteration) => void
}

export default React.memo(function FeeAlterationList({
  feeAlterations,
  toggleEditing,
  isEdited,
  cancel,
  update,
  onSuccess,
  onFailure,
  toggleDeleteModal
}: Props) {
  const { i18n } = useTranslation()

  return (
    <ListGrid
      labelWidth="fit-content(30%)"
      columnGap="L"
      rowGap="m"
      data-qa="fee-alteration-list"
    >
      {feeAlterations.map(({ data: feeAlteration, permittedActions }) =>
        feeAlteration.id !== null && isEdited(feeAlteration.id) ? (
          <EditorWrapper key={feeAlteration.id}>
            <FeeAlterationEditor
              key={feeAlteration.id}
              personId={feeAlteration.personId}
              baseFeeAlteration={feeAlteration}
              cancel={cancel}
              update={update}
              onSuccess={onSuccess}
              onFailure={onFailure}
            />
          </EditorWrapper>
        ) : (
          <Fragment key={feeAlteration.id}>
            <Label data-qa="fee-alteration-amount">{`${
              i18n.childInformation.feeAlteration.types[feeAlteration.type]
            } ${feeAlteration.amount}${
              feeAlteration.isAbsolute ? '€' : '%'
            }`}</Label>
            <FixedSpaceRow justifyContent="space-between">
              <FixedSpaceRow spacing="L">
                <Dates data-qa="fee-alteration-dates">{`${feeAlteration.validFrom.format()} - ${
                  feeAlteration.validTo?.format() ?? ''
                }`}</Dates>
                <span>{feeAlteration.notes}</span>
              </FixedSpaceRow>
              <FixedSpaceRow>
                {permittedActions.includes('UPDATE') && (
                  <IconButton
                    icon={faPen}
                    onClick={() => {
                      if (feeAlteration.id !== null) {
                        toggleEditing(feeAlteration.id)
                      }
                    }}
                    aria-label={i18n.common.edit}
                  />
                )}
                {permittedActions.includes('DELETE') && (
                  <IconButton
                    icon={faTrash}
                    onClick={() => toggleDeleteModal(feeAlteration)}
                    aria-label={i18n.common.remove}
                  />
                )}
              </FixedSpaceRow>
            </FixedSpaceRow>
            {feeAlteration.attachments.length > 0 && (
              <FeeAlterationAttachments
                attachments={feeAlteration.attachments}
              />
            )}
          </Fragment>
        )
      )}
    </ListGrid>
  )
})

function FeeAlterationAttachments({
  attachments
}: {
  attachments: Attachment[]
}) {
  const { i18n } = useTranslation()
  return (
    <>
      <IndentedAttachmentTitle>
        {i18n.childInformation.feeAlteration.attachmentsTitle}
      </IndentedAttachmentTitle>
      <AttachmentContainer>
        {attachments.map((file) => (
          <div key={file.id} data-qa="attachment">
            <FileIcon icon={fileIcon(file)} />
            <FileDownloadButton file={file} getFileUrl={getAttachmentUrl} />
          </div>
        ))}
      </AttachmentContainer>
    </>
  )
}

const AttachmentContainer = styled(FixedSpaceColumn)`
  @media (max-width: 600px) {
    margin-top: 8px;
    margin-left: ${defaultMargins.L};
  }
`

const IndentedAttachmentTitle = styled(H4)`
  font-size: ${fontSizesMobile.h4};
  font-weight: 400;
  margin: 0 0 0 ${defaultMargins.s};
`

const EditorWrapper = styled.div`
  grid-column: 1 / 3;
`

const Dates = styled.span`
  white-space: nowrap;
`
const FileIcon = styled(FontAwesomeIcon)`
  color: ${(p) => p.theme.colors.main.m2};
  margin-right: ${defaultMargins.s};
`
