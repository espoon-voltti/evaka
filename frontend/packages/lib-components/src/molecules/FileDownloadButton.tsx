// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { Result } from '@evaka/lib-common/src/api'
import { downloadBlobAsFile } from '@evaka/lib-common/src/utils/file'
import { Attachment } from '@evaka/lib-common/src/api-types/application/ApplicationDetails'
import { espooBrandColors } from '@evaka/lib-components/src/colors'
import { UUID } from '@evaka/lib-common/src/types'

const DownloadButton = styled.button`
  border: none;
  text-align: start;
  padding: 0;
  background: none;
  color: ${espooBrandColors.espooTurquoise};
  text-decoration: none;
  cursor: pointer;
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
