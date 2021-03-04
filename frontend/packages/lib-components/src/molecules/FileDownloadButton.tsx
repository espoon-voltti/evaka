// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { Result } from '@evaka/lib-common/src/api'
import { downloadBlobAsFile } from '@evaka/lib-common/src/utils/file'
import { Attachment } from '@evaka/lib-common/src/api-types/application/ApplicationDetails'
import { blueColors } from '@evaka/lib-components/src/colors'
import { UUID } from '@evaka/lib-common/src/types'

const DownloadButton = styled.button`
  background: none;
  border: none;
  color: ${blueColors.medium};
  cursor: pointer;
  font-size: 1rem;
  padding: 0;
  text-align: start;
  text-decoration: none;
`

interface FileDownloadButtonProps {
  file: Attachment
  fileFetchFn: (fileId: UUID) => Promise<Result<BlobPart>>
  onFileUnavailable: () => void
  dataQa?: string
}

/**
 * Wrapper for handling known file download (e.g. attachments) error cases
 * and for delivering blobs as downloads.
 */
export default React.memo(function FileDownloadButton({
  file,
  fileFetchFn,
  onFileUnavailable,
  dataQa
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

    downloadBlobAsFile(file.name, result.value)
  }

  return (
    <DownloadButton onClick={() => getFileIfAvailable(file)} data-qa={dataQa}>
      {file.name}
    </DownloadButton>
  )
})
