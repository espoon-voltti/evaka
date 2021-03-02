// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export const downloadBlobAsFile = (fileName: string, blobPart: BlobPart) => {
  const url = URL.createObjectURL(new Blob([blobPart]))
  const link = document.createElement('a')
  link.style.display = 'none'
  link.href = url
  link.setAttribute('download', fileName)
  // Safari and Testcafe think _blank anchors are pop-ups.
  // We only want to set _blank target if the browser does not support the HTML5
  // download attribute. This allows you to download files in desktop safari if
  // pop up blocking is enabled.
  if (typeof link.download === 'undefined') {
    link.target = '_blank'
  }
  link.rel = 'noreferrer'

  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}
