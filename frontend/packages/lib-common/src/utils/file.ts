// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

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
