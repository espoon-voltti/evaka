// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  faFile,
  faFileImage,
  faFilePdf,
  faFileWord
} from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'

import { ApplicationAttachment } from 'lib-common/generated/api-types/application'
import LocalDate from 'lib-common/local-date'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import FileDownloadButton from 'lib-components/molecules/FileDownloadButton'
import { Dimmed } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'

import { getAttachmentUrl } from '../../api/attachments'
import { useTranslation } from '../../state/i18n'

const AttachmentContainer = styled.div`
  display: flex;
  justify-content: flex-start;
`

const ReceivedAtText = styled(Dimmed)`
  font-style: italic;
  margin-left: ${defaultMargins.s};
`

interface Props {
  attachment: ApplicationAttachment
  'data-qa': string
  receivedAt: LocalDate
}

const contentTypeIcon = (contentType: string) => {
  switch (contentType) {
    case 'image/jpeg':
    case 'image/png':
      return faFileImage
    case 'application/pdf':
      return faFilePdf
    case 'application/msword':
      return faFileWord
    default:
      return faFile
  }
}

function Attachment({ attachment, 'data-qa': dataQa, receivedAt }: Props) {
  const { i18n } = useTranslation()

  return (
    <AttachmentContainer className="attachment" data-qa={dataQa}>
      <FixedSpaceRow spacing="xs" alignItems="center">
        <FontAwesomeIcon
          icon={contentTypeIcon(attachment.contentType)}
          className="attachment-icon"
          color="Dodgerblue"
        />
        <FileDownloadButton
          file={attachment}
          getFileUrl={getAttachmentUrl}
          data-qa="attachment-download"
        />
        <ReceivedAtText data-qa="attachment-received-at">
          {attachment.uploadedByEmployee
            ? i18n.application.attachments.receivedByPaperAt
            : i18n.application.attachments.receivedAt}{' '}
          {receivedAt.format()}
        </ReceivedAtText>
      </FixedSpaceRow>
    </AttachmentContainer>
  )
}

export default Attachment
