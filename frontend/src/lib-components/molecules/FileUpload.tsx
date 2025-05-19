// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useRef, useState } from 'react'
import styled from 'styled-components'

import type { Failure, Result, Success } from 'lib-common/api'
import type { Attachment } from 'lib-common/generated/api-types/attachment'
import type { AttachmentId } from 'lib-common/generated/api-types/shared'
import { randomId } from 'lib-common/id-type'
import type { UUID } from 'lib-common/types'
import { useUniqueId } from 'lib-common/utils/useUniqueId'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import FileDownloadButton from 'lib-components/molecules/FileDownloadButton'
import { H4, InformationText, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import {
  faCheck,
  faExclamationTriangle,
  faFile,
  faFileImage,
  faFilePdf,
  faFileWord,
  faPaperclip,
  faPlus,
  faTimes
} from 'lib-icons'

import { Button } from '../atoms/buttons/Button'
import { useTranslations } from '../i18n'

const fileUploadErrorKeys = {
  FILE_TOO_LARGE: undefined,
  EXTENSION_INVALID: undefined,
  EXTENSION_MISSING: undefined,
  SERVER_ERROR: undefined
}

type FileUploadError = keyof typeof fileUploadErrorKeys

interface FileObject extends Attachment {
  key: string
  file: File | undefined
  error: FileUploadError | undefined
  validationResult: React.ReactNode | null
  /**
   * Percentage of upload done.
   * NOTE: 100 does not mean the file upload has finished processing,
   * check "uploaded" truthyness instead.
   */
  progress: number
  /**
   * Marker to separate transfer and processing readiness
   */
  uploaded: boolean
  /**
   * Marker to indicate delete processing
   */
  deleteInProgress: boolean
}

const isErrorCode = (code: string | undefined): code is FileUploadError =>
  !!code && code in fileUploadErrorKeys
const getErrorCode = (res: Failure<unknown>): FileUploadError =>
  isErrorCode(res.errorCode) ? res.errorCode : 'SERVER_ERROR'

export interface UploadStatus {
  success: number
  inProgress: number
  error: number
}

export const initialUploadStatus: UploadStatus = {
  success: 0,
  inProgress: 0,
  error: 0
}

export type ValidateHandler<T> = {
  validate: (
    file: File,
    onUploadProgress: (percentage: number) => void
  ) => Promise<Result<T>>
}

export type UploadHandler = {
  upload: (
    file: File,
    onUploadProgress: (percentage: number) => void
  ) => Promise<Result<AttachmentId>>
  delete: (request: { attachmentId: AttachmentId }) => Promise<Result<void>>
}

interface FileUploadProps<T> {
  files: Attachment[]
  validateHandler?: ValidateHandler<T>
  getValidationResult?: (validation: T) => React.ReactNode
  uploadHandler: UploadHandler
  onUploaded?: (attachment: Attachment) => void
  onDeleted?: (id: AttachmentId) => void
  onStateChange?: (status: UploadStatus) => void
  getDownloadUrl: (id: AttachmentId, fileName: string) => string
  disabled?: boolean
  slim?: boolean
  'data-qa'?: string
  slimSingleFile?: boolean
  allowedFileTypes?: FileType[]
  buttonText?: string
  id?: string
}

const FileUploadContainer = styled.div<{
  $slim: boolean
}>`
  display: flex;
  flex-direction: ${(p) => (p.$slim ? 'column' : 'row')};
  flex-wrap: wrap;
  align-items: flex-start;
`

const FileInputLabel = styled.label`
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: ${(p) => p.theme.colors.grayscale.g4};
  border: 1px dashed ${(p) => p.theme.colors.grayscale.g35};
  border-radius: 8px;
  width: min(500px, 100%);
  padding: 24px;
  text-align: center;

  & input {
    display: none;
  }

  h4 {
    margin-top: 0;
  }

  p {
    margin: 0;
  }
`

const SingleFileInputLabel = styled.label`
  padding: ${defaultMargins.xxs} 0;

  & input {
    display: none;
  }

  .file-input-button {
    display: flex;
    align-items: center;
  }
`

const SlimInputLabel = styled.label`
  padding: ${defaultMargins.xs} 0;

  & input {
    display: none;
  }
`

const Icon = styled(FontAwesomeIcon)`
  height: 1rem !important;
  width: 1rem !important;
  margin-right: 10px;
`

const UploadedFiles = styled.div`
  display: flex;
  flex-direction: column;
  font-size: 15px;
  margin-top: 5px;

  > *:not(:last-child) {
    margin-bottom: 20px;
  }
`

const File = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  color: ${(p) => p.theme.colors.grayscale.g70};
`

const FileIcon = styled(FontAwesomeIcon)`
  margin-right: ${defaultMargins.xs};
  font-size: 20px;
  flex: 0;
  color: ${(p) => p.theme.colors.main.m2};
`

const FileDetails = styled.div`
  flex: 1;
  display: flex;
  flex-direction: column;
  flex-wrap: nowrap;
`

const FileHeader = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: space-between;
  text-align: left;
`

const ProgressBarContainer = styled.div`
  position: relative;
  background: ${(p) => p.theme.colors.grayscale.g35};
  height: 3px;
  border-radius: 1px;
`

interface ProgressBarProps {
  progress: number
}

const ProgressBar = styled.div<ProgressBarProps>`
  width: ${(props) => props.progress}%;
  position: absolute;
  top: 0;
  left: 0;
  background: ${(p) => p.theme.colors.main.m1};
  height: 3px;
  border-radius: 1px;
  transition: width 0.5s ease-out;
  margin-bottom: 3px;
`

const ValidationResult = styled.span`
  margin-left: 12px;
  color: ${(p) => p.theme.colors.grayscale.g100};
`

const FileActionButton = styled(IconOnlyButton)`
  border: none;
  background: none;
  padding: 4px;
  margin-left: 12px;
  min-width: 32px;
  color: ${(p) => p.theme.colors.grayscale.g35};

  &:hover {
    color: ${(p) => p.theme.colors.main.m1};
  }

  &:disabled {
    color: ${(p) => p.theme.colors.grayscale.g15};
    cursor: not-allowed;
  }
`

const ProgressBarDetails = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  color: ${(p) => p.theme.colors.grayscale.g70};
`

const ProgressBarError = styled.div`
  color: ${(p) => p.theme.colors.accents.a2orangeDark};
  margin-top: 3px;

  svg {
    color: ${(p) => p.theme.colors.status.warning};
    margin-left: 8px;
  }
`

const attachmentToFile = (attachment: Attachment): FileObject => ({
  id: attachment.id,
  file: undefined,
  key: attachment.id,
  name: attachment.name,
  contentType: attachment.contentType,
  progress: 100,
  error: undefined,
  validationResult: undefined,
  uploaded: true,
  deleteInProgress: false
})

export type FileType = 'document' | 'image' | 'audio' | 'video' | 'csv'

const contentTypes: Record<FileType, string[]> = {
  document: [
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.oasis.opendocument.text',
    'application/pdf'
  ],
  image: ['image/jpeg', 'image/png'],
  audio: ['audio/*'],
  video: ['video/*'],
  csv: ['text/csv']
}

const defaultAllowedFileTypes: FileType[] = ['image', 'document']

export const fileIcon = (file: Attachment): IconDefinition => {
  switch (file.contentType) {
    case 'image/jpeg':
    case 'image/png':
      return faFileImage
    case 'application/pdf':
      return faFilePdf
    case 'application/msword':
    case 'application/vnd.openxmlformats-officedocument.wordprocessingml.document':
    case 'application/vnd.oasis.opendocument.text':
      return faFileWord
    default:
      return faFile
  }
}

const inProgress = (file: FileObject): boolean => !file.uploaded

function FileUpload<T>(
  {
    files,
    validateHandler,
    getValidationResult,
    uploadHandler,
    onUploaded,
    onDeleted,
    onStateChange,
    getDownloadUrl,
    slimSingleFile = false,
    slim = false,
    disabled = false,
    'data-qa': dataQa,
    allowedFileTypes = defaultAllowedFileTypes,
    buttonText,
    id
  }: FileUploadProps<T>,
  ref: React.ForwardedRef<HTMLSpanElement>
) {
  const i18n = useTranslations().fileUpload

  const ariaId = useUniqueId('file-upload')
  const inputRef = useRef<HTMLInputElement>(null)

  const [uploadedFiles, setUploadedFiles] = useState<FileObject[]>(
    files.map(attachmentToFile)
  )

  const setUploadedFilesAndNotify = (files: FileObject[]) => {
    setUploadedFiles(files)
    if (onStateChange) {
      onStateChange({
        success: files.filter(
          (f) => f.uploaded && !f.error && !f.deleteInProgress
        ).length,
        inProgress: files.filter(
          (f) => (!f.uploaded && !f.error) || f.deleteInProgress
        ).length,
        error: files.filter((f) => f.error).length
      })
    }
  }

  const onChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (event.target.files && event.target.files.length > 0) {
      void addAttachment(event.target.files[0])
      if (event.target.value) event.target.value = ''
    }
  }

  const onDrop = (event: React.DragEvent<HTMLLabelElement>) => {
    event.stopPropagation()
    event.preventDefault()
    if (event.dataTransfer && event.dataTransfer.files[0]) {
      void addAttachment(event.dataTransfer.files[0])
    }
  }

  const onDragOver = (event: React.DragEvent<HTMLLabelElement>) => {
    // without this, the browser wants to open the file in the current window
    event.preventDefault()
  }

  const errorMessage = ({ error }: FileObject) => error && i18n.error[error]

  const processFile = async <T,>(
    fileObject: FileObject,
    request: () => Promise<Result<T>>,
    successHandler: (result: Success<T>) => void
  ) => {
    try {
      const result = await request()
      if (result.isFailure) {
        updateUploadedFile({
          ...fileObject,
          error: getErrorCode(result)
        })
      } else if (result.isSuccess) {
        successHandler(result)
      }
    } catch (e) {
      updateUploadedFile({ ...fileObject, error: 'SERVER_ERROR' })
    }
  }

  const uploadFile = (fileObject: FileObject, file: File) =>
    processFile(
      fileObject,
      () => uploadHandler.upload(file, updateProgress(fileObject)),
      (result) => {
        onUploaded?.({
          id: result.value,
          name: file.name,
          contentType: file.type
        })
        updateUploadedFile(
          {
            ...fileObject,
            validationResult: undefined,
            progress: 100,
            uploaded: true,
            id: result.value
          },
          fileObject.id
        )
      }
    )

  const deleteFile = async (file: FileObject) => {
    setUploadedFilesAndNotify(
      uploadedFiles.map((item) =>
        item.id === file.id ? { ...item, deleteInProgress: true } : item
      )
    )
    try {
      let success = false
      if (file.error === undefined) {
        const result = await uploadHandler.delete({ attachmentId: file.id })
        if (result.isSuccess) {
          onDeleted?.(file.id)
          success = true
        }
      } else {
        success = true
      }
      if (success) {
        setUploadedFilesAndNotify(
          uploadedFiles.filter((item) => item.id !== file.id)
        )
      }
    } catch (e) {
      console.error(e)
      setUploadedFilesAndNotify(
        uploadedFiles.map((item) =>
          item.id === file.id ? { ...item, deleteInProgress: false } : item
        )
      )
    }
  }

  const updateProgress = (fileObject: FileObject) => (percentage: number) => {
    updateUploadedFile({ ...fileObject, progress: percentage })
  }

  const updateUploadedFile = (
    file: FileObject,
    id: UUID | undefined = undefined
  ) => {
    const others = uploadedFiles.filter((item) => item.id !== (id ?? file.id))
    setUploadedFilesAndNotify([...others, file])
  }

  const MAX_ATTACHMENT_SIZE = 25 * 1024 * 1024 // 25 MB

  const addAttachment = async (file: File) => {
    const error = file.size > MAX_ATTACHMENT_SIZE ? 'FILE_TOO_LARGE' : undefined
    const pseudoId = randomId<AttachmentId>()
    const fileObject: FileObject = {
      id: pseudoId,
      key: pseudoId,
      file,
      name: file.name,
      contentType: file.type,
      validationResult: undefined,
      progress: 0,
      uploaded: false,
      deleteInProgress: false,
      error
    }

    updateUploadedFile(fileObject)

    if (error) {
      return
    }

    if (validateHandler === undefined) {
      await uploadFile(fileObject, file)
    } else {
      await processFile(
        fileObject,
        () => validateHandler.validate(file, updateProgress(fileObject)),
        (result) =>
          updateUploadedFile(
            {
              ...fileObject,
              validationResult: getValidationResult?.(result.value),
              progress: 100,
              uploaded: true
            },
            fileObject.id
          )
      )
    }
  }

  function onKeyDown(e: React.KeyboardEvent) {
    if (e.key === ' ' || e.key === 'Enter') {
      e.preventDefault()
      inputRef?.current?.click()
    }
  }

  const fileInput = (
    <input
      disabled={disabled}
      type="file"
      accept={allowedFileTypes.flatMap((t) => contentTypes[t]).join(',')}
      onChange={onChange}
      data-qa="btn-upload-file"
      ref={inputRef}
      id={ariaId}
    />
  )

  return (
    <FileUploadContainer data-qa={dataQa} $slim={slim} id={id}>
      {slimSingleFile ? (
        uploadedFiles.length > 0 ? (
          <></>
        ) : (
          <SingleFileInputLabel
            className="file-input-label"
            htmlFor={ariaId}
            onKeyDown={onKeyDown}
          >
            <Button
              appearance="inline"
              className="file-input-button"
              disabled={disabled}
              icon={faPlus}
              text={buttonText ?? i18n.input.title}
              onClick={() => inputRef?.current?.click()}
            />
            {fileInput}
          </SingleFileInputLabel>
        )
      ) : slim ? (
        <SlimInputLabel
          className="file-input-label"
          htmlFor={ariaId}
          onKeyDown={onKeyDown}
        >
          <Button
            appearance="inline"
            disabled={disabled}
            icon={faPaperclip}
            text={buttonText ?? i18n.input.title}
            onClick={() => inputRef?.current?.click()}
          />
          {fileInput}
        </SlimInputLabel>
      ) : (
        <FileInputLabel
          className="file-input-label"
          onDrop={onDrop}
          onDragOver={onDragOver}
          htmlFor={ariaId}
          onKeyDown={onKeyDown}
        >
          <span role="button" tabIndex={0} ref={ref}>
            {fileInput}
            <H4>{buttonText ?? i18n.input.title}</H4>
            <P>
              <InformationText>
                {i18n.input.text(allowedFileTypes)}
              </InformationText>
            </P>
          </span>
        </FileInputLabel>
      )}
      {uploadedFiles.length > 0 && (
        <>
          <Gap horizontal size={slimSingleFile ? 'zero' : 's'} />
          <UploadedFiles data-qa="uploaded-files">
            {uploadedFiles.map((file) => (
              <File data-qa="uploaded-file" key={file.key}>
                <FileIcon icon={fileIcon(file)} />
                <FileDetails>
                  <FileHeader>
                    {!inProgress(file) &&
                    !file.error &&
                    !file.deleteInProgress ? (
                      <FileDownloadButton
                        file={file}
                        getFileUrl={getDownloadUrl}
                        data-qa="file-download-button"
                      />
                    ) : (
                      <span>{file.name}</span>
                    )}
                    {(!inProgress(file) || file.error) && (
                      <>
                        {file.validationResult !== undefined && (
                          <>
                            <ValidationResult>
                              {file.validationResult}
                            </ValidationResult>
                            <FileActionButton
                              icon={faCheck}
                              disabled={file.deleteInProgress}
                              onClick={() => uploadFile(file, file.file!)}
                              aria-label={`${i18n.uploadFile} ${file.name}`}
                            />
                          </>
                        )}
                        <FileActionButton
                          icon={faTimes}
                          disabled={file.deleteInProgress}
                          onClick={() => deleteFile(file)}
                          aria-label={`${i18n.deleteFile} ${file.name}`}
                          data-qa="file-delete-button"
                        />
                      </>
                    )}
                  </FileHeader>
                  {inProgress(file) && (
                    <ProgressBarContainer>
                      <ProgressBar progress={file.progress} />
                      {file.error && (
                        <ProgressBarError>
                          <span>{errorMessage(file)}</span>
                          <Icon icon={faExclamationTriangle} />
                        </ProgressBarError>
                      )}
                      {!file.error && (
                        <ProgressBarDetails>
                          <span>
                            {inProgress(file) ? i18n.loading : i18n.loaded}
                          </span>
                          <span>{file.progress} %</span>
                        </ProgressBarDetails>
                      )}
                    </ProgressBarContainer>
                  )}
                </FileDetails>
              </File>
            ))}
          </UploadedFiles>
        </>
      )}
    </FileUploadContainer>
  )
}

export default React.memo(React.forwardRef(FileUpload)) as <T>(
  props: FileUploadProps<T> & { ref?: React.Ref<HTMLSpanElement> }
) => React.ReactElement
