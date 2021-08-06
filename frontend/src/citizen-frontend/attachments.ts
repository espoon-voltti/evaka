import { Failure, Result, Success } from 'lib-common/api'
import { AttachmentType } from 'lib-common/api-types/application/enums'
import { UUID } from 'lib-common/types'
import { client } from './api-client'

async function doSaveAttachment(
  url: string,
  file: File,
  onUploadProgress: (progressEvent: ProgressEvent) => void
): Promise<Result<UUID>> {
  const formData = new FormData()
  formData.append('file', file)

  try {
    const { data } = await client.post<string>(url, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress
    })
    return Success.of(data)
  } catch (e) {
    return Failure.fromError(e)
  }
}

export async function saveAttachment(
  file: File,
  onUploadProgress: (progressEvent: ProgressEvent) => void
): Promise<Result<UUID>> {
  return doSaveAttachment('/attachments/citizen', file, onUploadProgress)
}

export async function saveApplicationAttachment(
  applicationId: UUID,
  file: File,
  attachmentType: AttachmentType,
  onUploadProgress: (progressEvent: ProgressEvent) => void
): Promise<Result<UUID>> {
  return doSaveAttachment(
    `/attachments/citizen/applications/${applicationId}?type=${attachmentType}`,
    file,
    onUploadProgress
  )
}

export async function deleteAttachment(id: UUID): Promise<Result<void>> {
  try {
    await client.delete(`/attachments/citizen/${id}`)
    return Success.of(void 0)
  } catch (e) {
    return Failure.fromError(e)
  }
}

export async function getAttachmentBlob(
  attachmentId: UUID
): Promise<Result<BlobPart>> {
  try {
    const result = await client({
      url: `/attachments/${attachmentId}/download`,
      method: 'GET',
      responseType: 'blob'
    })
    return Success.of(result.data)
  } catch (e) {
    return Failure.fromError(e)
  }
}
