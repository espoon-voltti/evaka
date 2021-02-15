// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  ApplicationAttachment,
  FileObject
} from '../api-types/application/ApplicationDetails'

export const downloadBlobAsFile = (fileName: string, blobPart: BlobPart) => {
  const url = URL.createObjectURL(new Blob([blobPart]))
  const link = document.createElement('a')
  link.href = url
  link.target = '_blank'
  link.setAttribute('download', fileName)
  link.rel = 'noreferrer'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

export const attachmentToFile = (
  attachment: ApplicationAttachment
): FileObject => {
  return {
    id: attachment.id,
    file: undefined,
    key: Math.random(),
    name: attachment.name,
    contentType: attachment.contentType,
    progress: 100,
    error: undefined
  }
}
