// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useRef, useState } from 'react'
import styled from 'styled-components'
import { IconDefinition } from '@fortawesome/fontawesome-svg-core'

import { useTranslation } from '../../localization'
import { Gap } from '@evaka/lib-components/src/white-space'
import colors from '@evaka/lib-components/src/colors'
import FileDownloadButton from '@evaka/lib-components/src/molecules/FileDownloadButton'
import IconButton from '@evaka/lib-components/src/atoms/buttons/IconButton'
import {
  faExclamationTriangle,
  faFile,
  faFileImage,
  faFilePdf,
  faFileWord,
  faInfo,
  faTimes
} from '@evaka/lib-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { UUID } from '@evaka/lib-common/src/types'
import { Result } from '@evaka/lib-common/src/api'
import { attachmentToFile } from '@evaka/lib-common/src/utils/file'
import {
  ApplicationAttachment,
  FileObject
} from '@evaka/lib-common/src/api-types/application/ApplicationDetails'
import InfoModal from '@evaka/lib-components/src/molecules/modals/InfoModal'
import { getFileAvailability, getFileBlob } from '../../applications/api'

export type FileUploadProps = {
  files: ApplicationAttachment[]
  onUpload: (
    file: File,
    onUploadProgress: (progressEvent: ProgressEvent) => void
  ) => Promise<Result<UUID>>
  onDelete: (id: UUID) => Promise<Result<void>>
}

const FileUploadContainer = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  align-items: flex-start;
`

const FileInputLabel = styled.label`
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: ${colors.greyscale.lightest};
  border: 1px dashed ${colors.greyscale.medium};
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
  color: ${colors.greyscale.dark};
`

const FileIcon = styled(FontAwesomeIcon)`
  margin-right: 16px;
  font-size: 20px;
  flex: 0;
  color: ${colors.blues.primary};
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
  background: ${colors.greyscale.medium};
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
  background: ${colors.blues.medium};
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
  color: ${colors.greyscale.medium};

  &:hover {
    color: ${colors.blues.medium};
  }

  &:disabled {
    color: ${colors.greyscale.lighter};
    cursor: not-allowed;
  }
`

const ProgressBarDetails = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  color: ${colors.greyscale.dark};
`

const ProgressBarError = styled.div`
  color: ${colors.accents.orangeDark};
  margin-top: 3px;
  svg {
    color: ${colors.accents.orange};
    margin-left: 8px;
  }
`

const fileIcon = (file: FileObject): IconDefinition => {
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

const inProgress = (file: FileObject): boolean => file.progress !== 100

export default React.memo(function FileUpload({
  files,
  onUpload,
  onDelete
}: FileUploadProps) {
  const t = useTranslation()

  const ariaId = Math.random().toString(36).substring(2, 15)
  const inputRef = useRef<HTMLInputElement>(null)

  const [modalVisible, setModalVisible] = useState<boolean>(false)

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
    if (event.dataTransfer && event.dataTransfer.files[0]) {
      void addAttachment(event.dataTransfer.files[0], onUpload)
    }
  }

  const errorMessage = ({ error }: FileObject) => {
    const messages = t.fileUpload.error
    return error && messages[error]
  }

  const deleteFile = async (file: FileObject) => {
    try {
      if (!file.error) await onDelete(file.id)
      setUploadedFiles((old: FileObject[]) => {
        return old.filter((item) => item.id !== file.id)
      })
    } catch (e) {
      console.error(e)
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

  return (
    <FileUploadContainer>
      {modalVisible && (
        <InfoModal
          title={t.fileDownload.modalHeader}
          text={t.fileDownload.modalMessage}
          close={() => setModalVisible(false)}
          icon={faInfo}
        />
      )}
      <FileInputLabel
        className="file-input-label"
        onDrop={onDrop}
        htmlFor={ariaId}
        onKeyDown={onKeyDown}
      >
        <span role="button" tabIndex={0}>
          <input
            type="file"
            accept="image/jpeg, image/png, application/pdf, application/msword, application/vnd.openxmlformats-officedocument.wordprocessingml.document, application/vnd.oasis.opendocument.text"
            onChange={onChange}
            data-qa="btn-upload-file"
            ref={inputRef}
            id={ariaId}
          />
          <h4>{t.fileUpload.input.title}</h4>
          <p>{t.fileUpload.input.text.join('\n')}</p>
        </span>
      </FileInputLabel>
      <Gap horizontal size={'s'} />
      <UploadedFiles>
        {uploadedFiles.map((file) => (
          <File key={file.key}>
            <FileIcon icon={fileIcon(file)} />
            <FileDetails>
              <FileHeader>
                {!inProgress(file) && !file.error ? (
                  <FileDownloadButton
                    file={file}
                    fileAvailableFn={getFileAvailability}
                    fileFetchFn={getFileBlob}
                    onFileUnavailable={() => setModalVisible(true)}
                  />
                ) : (
                  <span>{file.name}</span>
                )}
                <FileDeleteButton
                  icon={faTimes}
                  onClick={() => deleteFile(file)}
                  altText={`${t.fileUpload.deleteFile} ${file.name}`}
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
                          ? t.fileUpload.loading
                          : t.fileUpload.loaded}
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
    </FileUploadContainer>
  )
})
