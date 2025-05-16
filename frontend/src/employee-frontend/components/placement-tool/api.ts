// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { AxiosProgressEvent } from 'axios'

import type { Result } from 'lib-common/api'
import { Failure, Success } from 'lib-common/api'
import type {
  UploadHandler,
  ValidateHandler
} from 'lib-components/molecules/FileUpload'

import {
  createPlacementToolApplications,
  validatePlacementToolApplications
} from '../../generated/api-clients/application'

const upload =
  <T>(
    upload: (
      file: File,
      onUploadProgress: (event: AxiosProgressEvent) => void
    ) => Promise<T>
  ) =>
  async (
    file: File,
    onUploadProgress: (percentage: number) => void
  ): Promise<Result<T>> => {
    try {
      const data = await upload(file, ({ loaded, total }) =>
        onUploadProgress(
          total !== undefined && total !== 0
            ? Math.round((loaded / total) * 100)
            : 0
        )
      )
      return Success.of(data)
    } catch (e) {
      return Failure.fromError(e)
    }
  }

export interface PlacementToolValidation {
  count: number
  existing: number
}

export const placementFileValidate: ValidateHandler<PlacementToolValidation> = {
  validate: upload((file, onUploadProgress) =>
    validatePlacementToolApplications({ file }, { onUploadProgress })
  )
}

export const placementFileUpload: UploadHandler = {
  upload: upload((file, onUploadProgress) =>
    createPlacementToolApplications({ file }, { onUploadProgress })
  ),
  delete: () => Promise.resolve(Success.of(undefined))
}
