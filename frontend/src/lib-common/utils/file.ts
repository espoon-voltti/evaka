// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

function openLink(link: HTMLAnchorElement, url: string) {
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)

  // Firefox at least needs a bit of time to trigger the download activation event
  setTimeout(() => {
    // Revoking an object URL should be safe as long as the download has been
    // started as browsers should keep the object in their Blob store at least
    // for the duration of the download: https://w3c.github.io/FileAPI/#dfn-revokeObjectURL
    // If the URL were not revoked, the blob would be kept in memory until the
    // page is closed, which would lead to significant memory usage if a user
    // downloads multiple attachments in a single session.
    window.URL.revokeObjectURL(url)
  }, 100)
}

function createLink(url: string) {
  const link = document.createElement('a')
  link.style.display = 'none'
  link.href = url
  link.rel = 'noreferrer'
  return link
}

export const downloadBlobAsFile = (fileName: string, blobPart: BlobPart) => {
  const url = URL.createObjectURL(new Blob([blobPart]))
  const link = createLink(url)

  link.setAttribute('download', fileName)
  // Safari and Testcafe think _blank anchors are pop-ups.
  // We only want to set _blank target if the browser does not support the HTML5
  // download attribute. This allows you to download files in desktop safari if
  // pop up blocking is enabled.
  if (typeof link.download === 'undefined') {
    link.target = '_blank'
  }

  openLink(link, url)
}

export const openBlobInBrowser = (
  fileName: string,
  fileType: string,
  blobPart: BlobPart
) => {
  const url = URL.createObjectURL(new Blob([blobPart], { type: fileType }))
  const link = createLink(url)
  link.target = '_blank'
  openLink(link, url)
}
