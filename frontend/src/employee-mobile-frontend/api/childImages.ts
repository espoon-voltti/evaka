import { client } from './client'
import { Failure, Result, Success } from '../../lib-common/api'

export async function uploadChildImage(
  childId: string,
  file: File
): Promise<Result<null>> {
  const formData = new FormData()
  formData.append('file', file)

  return client
    .put(`/children/${childId}/image`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

export async function deleteChildImage(childId: string): Promise<Result<null>> {
  return client
    .delete(`/children/${childId}/image`)
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}
