// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { Result } from '@evaka/lib-common/src/api'
import {
  AttachmentPreDownloadResponse,
  FileObject
} from '@evaka/lib-common/src/api-types/application/ApplicationDetails'
import { espooBrandColors } from '@evaka/lib-components/src/colors'

const DownloadButton = styled.button`
  border: none;
  background: none;
  color: ${espooBrandColors.espooTurquoise};
  text-decoration: none;
  cursor: pointer;
  font-size: 15px;
`

interface Props {
  file: FileObject
  fileFetchFn: (file: FileObject) => Promise<Result<BlobPart>>
  fileAvailableFn: (
    file: FileObject
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
}: Props) {
  const deliverBlob = async (file: FileObject) => {
    const result = await fileFetchFn(file)
    if (result.isSuccess) {
      const url = URL.createObjectURL(new Blob([result.value]))
      const link = document.createElement('a')
      link.href = url
      link.target = '_blank'
      link.setAttribute('download', `${file.name}`)
      link.rel = 'noreferrer'
      document.body.appendChild(link)
      link.click()
      link.remove()
    }
  }

  const getFileIfAvailable = async (file: FileObject) => {
    const result = await fileAvailableFn(file)
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
