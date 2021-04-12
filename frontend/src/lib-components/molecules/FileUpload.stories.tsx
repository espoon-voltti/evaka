// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { storiesOf } from '@storybook/react'
import React from 'react'
import { Failure, Result, Success } from '../../lib-common/api'
import { UUID } from '../../lib-common/types'
import FileUpload from './FileUpload'

const mockDownload = (_: string): Promise<Result<BlobPart>> =>
  Promise.resolve(
    Success.of(
      new Blob([JSON.stringify({ hello: 'world' }, null, 2)], {
        type: 'application/json'
      })
    )
  )

const mockDownloadFails = (_: string): Promise<Result<BlobPart>> =>
  Promise.resolve(Failure.of({ message: 'no can do', statusCode: 404 }))

const mockDelete = (_: string): Promise<Result<void>> =>
  Promise.resolve(Success.of(void 0))

const delay = async (ms: number): Promise<void> =>
  new Promise<void>((resolve) => setTimeout(resolve, ms))

const mockUpload = async (
  _file: File,
  onUploadProgress: (progressEvent: ProgressEvent) => void
): Promise<Result<UUID>> => {
  for (const val of [25, 50, 75, 100]) {
    await delay(500)
    onUploadProgress({ loaded: val, total: 100 } as never)
  }

  return Success.of('123')
}

const mockUploadFails = (
  _file: File,
  _onUploadProgress: (progressEvent: ProgressEvent) => void
): Promise<Result<UUID>> => Promise.resolve(Failure.of(new Error('no can do')))

const i18n = {
  upload: {
    loading: 'Ladataan...',
    loaded: 'Ladattu',
    error: {
      FILE_TOO_LARGE: 'Liian suuri tiedosto (max. 10MB)',
      SERVER_ERROR: 'Lataus ei onnistunut'
    },
    input: {
      title: 'Lisää liite',
      text: [
        'Paina tästä tai raahaa liite laatikkoon yksi kerrallaan.',
        'Tiedoston maksimikoko: 10MB.',
        'Sallitut tiedostomuodot:',
        'PDF, JPEG/JPG, PNG ja DOC/DOCX'
      ]
    },
    deleteFile: 'Poista tiedosto'
  },
  download: {
    modalHeader: 'Tiedoston käsittely on kesken',
    modalMessage:
      'Tiedosto ei ole juuri nyt avattavissa. Kokeile hetken kuluttua uudelleen.'
  }
}

storiesOf('evaka/molecules/FileUpload', module)
  .add('empty state', () => (
    <FileUpload
      files={[]}
      onDownloadFile={mockDownload}
      onDelete={mockDelete}
      onUpload={mockUpload}
      i18n={i18n}
    />
  ))
  .add('with initial files', () => (
    <FileUpload
      files={[
        { contentType: 'image/jpeg', id: '123', name: 'doge.jpg' },
        { contentType: 'application/pdf', name: 'TOS_v2_final.pdf', id: '456' }
      ]}
      onDownloadFile={mockDownload}
      onDelete={mockDelete}
      onUpload={mockUpload}
      i18n={i18n}
    />
  ))
  .add('upload and download fails', () => (
    <FileUpload
      files={[
        { contentType: 'application/pdf', name: 'TOS_v2_final.pdf', id: '456' }
      ]}
      onDownloadFile={mockDownloadFails}
      onDelete={mockDelete}
      onUpload={mockUploadFails}
      i18n={i18n}
    />
  ))
