// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import {
  faFileImage,
  faFilePdf,
  faFileWord,
  faFile
} from '@fortawesome/free-solid-svg-icons'
import { FixedSpaceRow } from '@evaka/lib-components/src/layout/flex-helpers'
import { ApplicationAttachment } from '@evaka/lib-common/src/api-types/application/ApplicationDetails'

const AttachmentContainer = styled.div`
  display: flex;
  justify-content: flex-start;
`

interface Props {
  attachment: ApplicationAttachment
  dataQa: string
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

function Attachment({ attachment, dataQa }: Props) {
  return (
    <AttachmentContainer className={`attachment`} data-qa={dataQa}>
      <FixedSpaceRow spacing={'xs'} alignItems={'center'}>
        <FontAwesomeIcon
          icon={contentTypeIcon(attachment.contentType)}
          className={'attachment-icon'}
          color={'Dodgerblue'}
        />
        <a
          href={`/api/internal/attachments/${attachment.id}/download`}
          target="_blank"
          rel="noreferrer"
        >
          <span>{attachment.name}</span>
        </a>
      </FixedSpaceRow>
    </AttachmentContainer>
  )
}

export default Attachment
