// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { Result } from 'lib-common/api'
import { downloadBlobAsFile, openBlobInBrowser } from 'lib-common/utils/file'
import { Attachment } from 'lib-common/api-types/attachment'
import { UUID } from 'lib-common/types'
import { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import { FixedSpaceRow } from '../layout/flex-helpers'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { fileIcon } from './FileUpload'

const DownloadButton = styled.button`
  background: none;
  border: none;
  color: ${({ theme: { colors } }) => colors.main.medium};
  cursor: pointer;
  font-size: 1rem;
  padding: 0;
  text-align: start;
  text-decoration: none;
`

interface FileDownloadButtonProps {
  file: Attachment
  fileFetchFn: (fileId: UUID) => Promise<Result<BlobPart>>
  afterFetch?: () => void
  onFileUnavailable: () => void
  icon?: IconDefinition | boolean
  'data-qa'?: string
  openInBrowser?: boolean
  text?: string
}

/**
 * Wrapper for handling known file download (e.g. attachments) error cases
 * and for delivering blobs as downloads.
 */
export default React.memo(function FileDownloadButton({
  file,
  fileFetchFn,
  afterFetch = () => undefined,
  onFileUnavailable,
  icon,
  'data-qa': dataQa,
  openInBrowser = false,
  text
}: FileDownloadButtonProps) {
  const getFileIfAvailable = async (file: Attachment) => {
    const result = await fileFetchFn(file.id)

    if (result.isLoading)
      throw new Error('Unexpected return before request completion')

    if (result.isFailure) {
      if (result.statusCode === 404) {
        return onFileUnavailable()
      }
      throw new Error(result.message)
    }

    if (openInBrowser)
      openBlobInBrowser(file.name, file.contentType, result.value)
    else downloadBlobAsFile(file.name, result.value)
  }

  return (
    <DownloadButton
      onClick={() => getFileIfAvailable(file).then(afterFetch)}
      data-qa={dataQa}
    >
      <FixedSpaceRow spacing="xs" alignItems="center" key={file.id}>
        {icon && (
          <FontAwesomeIcon icon={icon === true ? fileIcon(file) : icon} />
        )}
        <div>{text ?? file.name}</div>
      </FixedSpaceRow>
    </DownloadButton>
  )
})
