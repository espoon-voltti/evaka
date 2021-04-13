import { Failure, Result, Success } from 'lib-common/api'
import { AttachmentType } from 'lib-common/api-types/application/enums'
import { UUID } from 'lib-common/types'
import { client } from './client'

export async function saveAttachment(
  applicationId: UUID,
  file: File,
  type: AttachmentType,
  onUploadProgress: (progressEvent: ProgressEvent) => void
): Promise<Result<UUID>> {
  const formData = new FormData()
  formData.append('file', file)

  try {
    const { data } = await client.post<UUID>(
      `/attachments/applications/${applicationId}`,
      formData,
      {
        headers: { 'Content-Type': 'multipart/form-data' },
        params: { type },
        onUploadProgress
      }
    )
    return Success.of(data)
  } catch (e) {
    return Failure.fromError(e)
  }
}

export const deleteAttachment = (id: UUID): Promise<Result<void>> =>
  client
    .delete(`/attachments/${id}`)
    .then(() => Success.of(void 0))
    .catch((e) => Failure.fromError(e))
