// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'

import { client } from './client'

export async function uploadChildImage(
  childId: string,
  file: File
): Promise<Result<void>> {
  const formData = new FormData()
  formData.append('file', file)

  return client
    .put(`/children/${childId}/image`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function deleteChildImage(childId: string): Promise<Result<null>> {
  return client
    .delete(`/children/${childId}/image`)
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}
