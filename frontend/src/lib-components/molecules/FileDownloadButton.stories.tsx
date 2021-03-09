// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { storiesOf } from '@storybook/react'
import { action } from '@storybook/addon-actions'
import FileDownloadButton from './FileDownloadButton'
import { Attachment } from '../../lib-common/api-types/application/ApplicationDetails'
import { Failure, Result, Success } from '../../lib-common/api'

const file: Attachment = {
  id: 'abc123',
  name: 'evaka.jpg',
  contentType: 'image/png'
}

const mockSuccessfulFetch = (_: string): Promise<Result<BlobPart>> =>
  Promise.resolve(Success.of(new Blob()))

const mockUnavailableFetch = (_: string): Promise<Result<BlobPart>> =>
  Promise.resolve(Failure.of({ message: 'example', statusCode: 404 }))

const mockFailedFetch = (_: string): Promise<Result<BlobPart>> =>
  Promise.resolve(Failure.fromError(new Error('example')))

storiesOf('evaka/molecules/FileDownloadButton', module)
  .add('success', () => (
    <FileDownloadButton
      file={file}
      fileFetchFn={mockSuccessfulFetch}
      onFileUnavailable={action('file unavailable triggered')}
    />
  ))
  .add('file unavailable', () => (
    <FileDownloadButton
      file={file}
      fileFetchFn={mockUnavailableFetch}
      onFileUnavailable={action('file unavailable triggered')}
    />
  ))
  .add('fetch failed', () => (
    <>
      <p>See the browser&apos;s console for uncaught error output</p>
      <FileDownloadButton
        file={file}
        fileFetchFn={mockFailedFetch}
        onFileUnavailable={action('file unavailable triggered')}
      />
    </>
  ))
