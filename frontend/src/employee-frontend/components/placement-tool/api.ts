// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { AttachmentId } from 'lib-common/generated/api-types/shared'

import { client } from '../../api/client'

export async function uploadPlacementFile(
  file: File,
  onUploadProgress: (percentage: number) => void
): Promise<Result<AttachmentId>> {
  const formData = new FormData()
  formData.append('file', file)

  try {
    const { data } = await client.post<AttachmentId>(
      '/employee/placement-tool',
      formData,
      {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: ({ loaded, total }) =>
          onUploadProgress(
            total !== undefined && total !== 0
              ? Math.round((loaded / total) * 100)
              : 0
          )
      }
    )
    return Success.of(data)
  } catch (e) {
    return Failure.fromError(e)
  }
}

export function deletePlacementFile(): Promise<Result<void>> {
  return new Promise<Result<void>>((resolve) => {
    resolve(Success.of(void 0))
  })
}
