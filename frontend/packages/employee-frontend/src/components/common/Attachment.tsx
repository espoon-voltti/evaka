// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import styled from 'styled-components'
import {
  faFile,
  faFileImage,
  faFilePdf,
  faFileWord
} from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'

import { ApplicationAttachment } from '@evaka/lib-common/src/api-types/application/ApplicationDetails'
import { FixedSpaceRow } from '@evaka/lib-components/src/layout/flex-helpers'
import FileDownloadButton from '@evaka/lib-components/src/molecules/FileDownloadButton'
import { getAttachmentBlob } from '~api/applications'
import { useTranslation } from '~state/i18n'
import { UIContext } from '~state/ui'

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
  const { i18n } = useTranslation()
  const { setErrorMessage } = useContext(UIContext)

  return (
    <AttachmentContainer className={`attachment`} data-qa={dataQa}>
      <FixedSpaceRow spacing={'xs'} alignItems={'center'}>
        <FontAwesomeIcon
          icon={contentTypeIcon(attachment.contentType)}
          className={'attachment-icon'}
          color={'Dodgerblue'}
        />
        <FileDownloadButton
          file={attachment}
          fileFetchFn={getAttachmentBlob}
          onFileUnavailable={() =>
            setErrorMessage({
              type: 'warning',
              title: i18n.common.fileDownloadError.modalHeader,
              text: i18n.common.fileDownloadError.modalMessage,
              resolveLabel: i18n.common.ok
            })
          }
        />
      </FixedSpaceRow>
    </AttachmentContainer>
  )
}

export default Attachment
