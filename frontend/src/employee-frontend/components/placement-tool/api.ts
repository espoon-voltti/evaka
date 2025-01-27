// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import {
  UploadHandler,
  ValidateHandler
} from 'lib-components/molecules/FileUpload'

import { client } from '../../api/client'

const upload =
  (url: string) =>
  async <T>(
    file: File,
    onUploadProgress: (percentage: number) => void
  ): Promise<Result<T>> => {
    const formData = new FormData()
    formData.append('file', file)

    try {
      const { data } = await client.post<T>(url, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: ({ loaded, total }) =>
          onUploadProgress(
            total !== undefined && total !== 0
              ? Math.round((loaded / total) * 100)
              : 0
          )
      })
      return Success.of(data)
    } catch (e) {
      return Failure.fromError(e)
    }
  }

export interface PlacementToolValidation {
  count: number
}

export const placementFileValidate: ValidateHandler<PlacementToolValidation> = {
  validate: upload('/employee/placement-tool/validation')
}

export const placementFileUpload: UploadHandler = {
  upload: upload('/employee/placement-tool'),
  delete: () => Promise.resolve(Success.of(undefined))
}
