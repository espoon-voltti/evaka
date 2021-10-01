// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useRef, useState } from 'react'
import styled from 'styled-components'
import { IconDefinition } from '@fortawesome/fontawesome-svg-core'

import { defaultMargins, Gap } from 'lib-components/white-space'
import FileDownloadButton from 'lib-components/molecules/FileDownloadButton'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import {
  faExclamationTriangle,
  faFile,
  faFileImage,
  faFilePdf,
  faFileWord,
  faInfo,
  faPaperclip,
  faTimes
} from 'lib-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { UUID } from 'lib-common/types'
import { Result, Success } from 'lib-common/api'
import { Attachment } from 'lib-common/api-types/attachment'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { useUniqueId } from 'lib-common/utils/useUniqueId'
import InlineButton from '../atoms/buttons/InlineButton'

type FileUploadError = 'FILE_TOO_LARGE' | 'SERVER_ERROR'

interface FileObject extends Attachment {
  key: number
  file: File | undefined
  error: FileUploadError | undefined
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

interface FileUploadI18n {
  download: {
    modalHeader: string
    modalMessage: string
  }
  upload: {
    deleteFile: string
    input: {
      title: string
      text: string[]
    }
    loading: string
    loaded: string
    error: Record<FileUploadError, string>
  }
}

export interface FileUploadProps {
  files: Attachment[]
  onUpload: (
    file: File,
    onUploadProgress: (progressEvent: ProgressEvent) => void
  ) => Promise<Result<UUID>>
  onDelete: (id: UUID) => Promise<Result<void>>
  onDownloadFile: (id: UUID) => Promise<Result<BlobPart>>
  i18n: FileUploadI18n
  disabled?: boolean
  slim?: boolean
  'data-qa'?: string
  slimSingleFile?: boolean
}

const FileUploadContainer = styled.div<{ slim: boolean }>`
  display: flex;
  flex-direction: ${({ slim }) => (slim ? 'column' : 'row')};
  flex-wrap: wrap;
  align-items: flex-start;
`

const FileInputLabel = styled.label`
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: ${({ theme: { colors } }) => colors.greyscale.lightest};
  border: 1px dashed ${({ theme: { colors } }) => colors.greyscale.medium};
  border-radius: 8px;
  width: min(500px, 70vw);
  padding: 24px;

  & input {
    display: none;
  }

  & h4 {
    font-size: 18px;
    margin-bottom: 14px;
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
  color: ${({ theme: { colors } }) => colors.greyscale.dark};
`

const FileIcon = styled(FontAwesomeIcon)`
  margin-right: 16px;
  font-size: 20px;
  flex: 0;
  color: ${({ theme: { colors } }) => colors.main.primary};
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
  background: ${({ theme: { colors } }) => colors.greyscale.medium};
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
  background: ${({ theme: { colors } }) => colors.main.medium};
  height: 3px;
  border-radius: 1px;
  transition: width 0.5s ease-out;
  margin-bottom: 3px;
`

const FileDeleteButton = styled(IconButton)`
  border: none;
  background: none;
  padding: 4px;
  margin-left: 12px;
  min-width: 32px;
  color: ${({ theme: { colors } }) => colors.greyscale.medium};

  &:hover {
    color: ${({ theme: { colors } }) => colors.main.medium};
  }

  &:disabled {
    color: ${({ theme: { colors } }) => colors.greyscale.lighter};
    cursor: not-allowed;
  }
`

const ProgressBarDetails = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  color: ${({ theme: { colors } }) => colors.greyscale.dark};
`

const ProgressBarError = styled.div`
  color: ${({ theme: { colors } }) => colors.accents.orangeDark};
  margin-top: 3px;
  svg {
    color: ${({ theme: { colors } }) => colors.accents.orange};
    margin-left: 8px;
  }
`

const attachmentToFile = (attachment: Attachment): FileObject => {
  return {
    id: attachment.id,
    file: undefined,
    key: Math.random(),
    name: attachment.name,
    contentType: attachment.contentType,
    progress: 100,
    error: undefined,
    uploaded: true,
    deleteInProgress: false
  }
}

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

const inProgress = (file: FileObject): boolean => file.uploaded === false

export default React.memo(function FileUpload({
  i18n,
  files,
  onUpload,
  onDelete,
  onDownloadFile,
  slimSingleFile = false,
  slim = false,
  disabled = false,
  'data-qa': dataQa
}: FileUploadProps) {
  const ariaId = useUniqueId('file-upload')
  const inputRef = useRef<HTMLInputElement>(null)

  const [unavailableModalVisible, setUnavailableModalVisible] =
    useState<boolean>(false)

  const [uploadedFiles, setUploadedFiles] = useState<FileObject[]>(
    files.map(attachmentToFile)
  )

  const onChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (event.target.files && event.target.files.length > 0) {
      void addAttachment(event.target.files[0], onUpload)
      if (event.target.value) event.target.value = ''
    }
  }

  const onDrop = (event: React.DragEvent<HTMLLabelElement>) => {
    event.stopPropagation()
    event.preventDefault()
    if (event.dataTransfer && event.dataTransfer.files[0]) {
      void addAttachment(event.dataTransfer.files[0], onUpload)
    }
  }

  const onDragOver = (event: React.DragEvent<HTMLLabelElement>) => {
    // without this, the browser wants to open the file in the current window
    event.preventDefault()
  }

  const errorMessage = ({ error }: FileObject) => {
    return error && i18n.upload.error[error]
  }

  const deleteFile = async (file: FileObject) => {
    setUploadedFiles((old) =>
      old.map((item) =>
        item.id === file.id ? { ...item, deleteInProgress: true } : item
      )
    )
    try {
      const { isSuccess } = file.error
        ? Success.of(true)
        : await onDelete(file.id)
      if (isSuccess) {
        setUploadedFiles((old) => old.filter((item) => item.id !== file.id))
      }
    } catch (e) {
      console.error(e)
      setUploadedFiles((old) =>
        old.map((item) =>
          item.id === file.id ? { ...item, deleteInProgress: false } : item
        )
      )
    }
  }

  const updateUploadedFile = (
    file: FileObject,
    id: UUID | undefined = undefined
  ) =>
    setUploadedFiles((old) => {
      const others = old.filter((item) => item.id !== (id ?? file.id))
      return [...others, file]
    })

  const addAttachment = async (
    file: File,
    onUpload: (
      file: File,
      onUploadProgress: (progressEvent: ProgressEvent) => void
    ) => Promise<Result<UUID>>
  ) => {
    const error = file.size > 10000000 ? 'FILE_TOO_LARGE' : undefined
    const pseudoId = Math.random()
    const fileObject: FileObject = {
      id: pseudoId.toString(),
      key: pseudoId,
      file,
      name: file.name,
      contentType: file.type,
      progress: 0,
      uploaded: false,
      deleteInProgress: false,
      error
    }

    updateUploadedFile(fileObject)

    if (error) {
      return
    }

    const updateProgress = ({ loaded, total }: ProgressEvent) => {
      const progress = Math.round((loaded / total) * 100)
      updateUploadedFile({ ...fileObject, progress })
    }

    try {
      const result = await onUpload(file, updateProgress)
      if (result.isFailure) throw new Error(result.message)
      if (result.isSuccess)
        updateUploadedFile(
          {
            ...fileObject,
            progress: 100,
            uploaded: true,
            id: result.value
          },
          fileObject.id
        )
    } catch (e) {
      updateUploadedFile({ ...fileObject, error: 'SERVER_ERROR' })
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
      accept="image/jpeg, image/png, application/pdf, application/msword, application/vnd.openxmlformats-officedocument.wordprocessingml.document, application/vnd.oasis.opendocument.text"
      onChange={onChange}
      data-qa="btn-upload-file"
      ref={inputRef}
      id={ariaId}
    />
  )

  return (
    <FileUploadContainer data-qa={dataQa} slim={slim}>
      {unavailableModalVisible && (
        <InfoModal
          title={i18n.download.modalHeader}
          text={i18n.download.modalMessage}
          close={() => setUnavailableModalVisible(false)}
          icon={faInfo}
        />
      )}
      {slim || (slimSingleFile && uploadedFiles.length == 0) ? (
        <SlimInputLabel
          className="file-input-label"
          htmlFor={ariaId}
          onKeyDown={onKeyDown}
        >
          <InlineButton
            disabled={disabled}
            icon={faPaperclip}
            text={i18n.upload.input.title}
            onClick={() => inputRef?.current?.click()}
          />
          {fileInput}
        </SlimInputLabel>
      ) : slimSingleFile && uploadedFiles.length > 0 ? (
        <></>
      ) : (
        <FileInputLabel
          className="file-input-label"
          onDrop={onDrop}
          onDragOver={onDragOver}
          htmlFor={ariaId}
          onKeyDown={onKeyDown}
        >
          <span role="button" tabIndex={0}>
            {fileInput}
            <h4>{i18n.upload.input.title}</h4>
            <p>{i18n.upload.input.text.join('\n')}</p>
          </span>
        </FileInputLabel>
      )}
      {uploadedFiles.length > 0 && (
        <>
          <Gap horizontal size={'s'} />
          <UploadedFiles data-qa={'uploaded-files'}>
            {uploadedFiles.map((file) => (
              <File key={file.key}>
                <FileIcon icon={fileIcon(file)} />
                <FileDetails>
                  <FileHeader>
                    {!inProgress(file) &&
                    !file.error &&
                    !file.deleteInProgress ? (
                      <FileDownloadButton
                        file={file}
                        fileFetchFn={onDownloadFile}
                        onFileUnavailable={() =>
                          setUnavailableModalVisible(true)
                        }
                        data-qa={'file-download-button'}
                      />
                    ) : (
                      <span data-qa={'file-download-unavailable-text'}>
                        {file.name}
                      </span>
                    )}
                    <FileDeleteButton
                      icon={faTimes}
                      disabled={file.deleteInProgress}
                      onClick={() => deleteFile(file)}
                      altText={`${i18n.upload.deleteFile} ${file.name}`}
                      data-qa={`file-delete-button-${file.name}`}
                    />
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
                            {inProgress(file)
                              ? i18n.upload.loading
                              : i18n.upload.loaded}
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
})
