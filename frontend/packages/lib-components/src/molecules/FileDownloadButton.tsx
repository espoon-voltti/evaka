// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { Result } from '@evaka/lib-common/src/api'
import { downloadBlobAsFile } from '@evaka/lib-common/src/utils/file'
import {
  Attachment,
  AttachmentPreDownloadResponse
} from '@evaka/lib-common/src/api-types/application/ApplicationDetails'
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
  fileAvailableFn: (
    fileId: UUID
  ) => Promise<Result<AttachmentPreDownloadResponse>>
  onFileUnavailable: () => void
}

/**
 * Wrapper for handling file downloads' required pre-download checks
 * and the delivery of the file blob.
 */
export default React.memo(function FileDownloadButton({
  file,
  fileFetchFn,
  fileAvailableFn,
  onFileUnavailable
}: FileDownloadButtonProps) {
  const deliverBlob = async (file: Attachment) => {
    const result = await fileFetchFn(file.id)
    if (result.isSuccess) {
      downloadBlobAsFile(file.name, result.value)
    }
  }

  const getFileIfAvailable = async (file: Attachment) => {
    const result = await fileAvailableFn(file.id)
    if (result.isFailure) throw new Error(result.message)
    if (result.isLoading)
      throw new Error('Unexpected return before request completion')

    if (result.value.fileAvailable) {
      void deliverBlob(file)
    } else {
      onFileUnavailable()
    }
  }

  return (
    <DownloadButton onClick={() => getFileIfAvailable(file)}>
      {file.name}
    </DownloadButton>
  )
})
