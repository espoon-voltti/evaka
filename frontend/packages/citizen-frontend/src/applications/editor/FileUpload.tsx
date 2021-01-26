import React, { useState } from 'react'
import styled from 'styled-components'
import { useTranslation } from '~localization'
import { Gap } from '@evaka/lib-components/src/white-space'
import colors from '@evaka/lib-components/src/colors'
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
import { Attachment, FileObject } from '@evaka/lib-common/src/api-types/application/ApplicationDetails'
import { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import InfoModal from '@evaka/lib-components/src/molecules/modals/InfoModal'
import { getFileAvailability, getFileBlob } from '~applications/api'

export type FileUploadProps = {
  files: Attachment[]
  onUpload: (
    file: File,
    onUploadProgress: (progressEvent: ProgressEvent) => void
  ) => Promise<Result<UUID>>
  onDelete: (id: UUID) => Promise<Result<void>>
}

export type ProgressEvent = {
  loaded: number
  total: number
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
  text-align: center;

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
  font-size: 15px;
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
`
const FileDownloadButton = styled.button`
  border: none;
  background: none;
  color: ${colors.blues.primary};
  cursor: pointer;
`

const FileDeleteButton = styled(IconButton)`
  border: none;
  background: none;
  padding: 4px;
  margin-left: 12px;
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
  svg {
    color: ${colors.accents.orange};
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
    error: undefined
  }
}

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

  const getFileIfAvailable = async (file: FileObject) => {
    const result = await getFileAvailability(file)
    if (result.isSuccess) {
      result.value.fileAvailable
        ? void deliverBlob(file)
        : setModalVisible(true)
    }
  }

  const deliverBlob = async (file: FileObject) => {
    const result = await getFileBlob(file)
    if (result.isSuccess) {
      const url = URL.createObjectURL(new Blob([result.value]))
      const link = document.createElement('a')
      link.href = url
      link.target = '_blank'
      link.setAttribute('download', `${file.name}`)
      link.rel = 'noreferrer'
      document.body.appendChild(link)
      link.click()
      link.remove()
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
      result.isSuccess &&
        updateUploadedFile(
          {
            ...fileObject,
            progress: 100,
            id: result.value
          },
          fileObject.id
        )
    } catch (e) {
      console.error(e)
      updateUploadedFile({ ...fileObject, error: 'SERVER_ERROR' })
    }
  }

  return (
    <FileUploadContainer>
      {modalVisible && (
        <InfoModal
          title={t.fileUpload.modalHeader}
          text={t.fileUpload.modalMessage}
          close={() => setModalVisible(false)}
          icon={faInfo}
        />
      )}
      <FileInputLabel className="file-input-label" onDrop={onDrop}>
        <input
          type="file"
          accept="image/jpeg, image/png, application/pdf, application/msword, application/vnd.openxmlformats-officedocument.wordprocessingml.document, application/vnd.oasis.opendocument.text"
          onChange={onChange}
          data-qa="btn-upload-file"
        />
        <h4>{t.fileUpload.input.title}</h4>
        <p>{t.fileUpload.input.text.join('\n')}</p>
      </FileInputLabel>
      <Gap horizontal size={'s'} />
      <UploadedFiles>
        {uploadedFiles.map((file) => (
          <File key={file.key}>
            <FileIcon icon={fileIcon(file)}></FileIcon>
            <FileDetails>
              <FileHeader>
                {file.id ? (
                  <FileDownloadButton onClick={() => getFileIfAvailable(file)}>
                    {file.name}
                  </FileDownloadButton>
                ) : (
                  <span>{file.name}</span>
                )}
                <FileDeleteButton
                  icon={faTimes}
                  onClick={() => deleteFile(file)}
                />
              </FileHeader>
              {inProgress(file) && (
                <ProgressBarContainer>
                  <ProgressBar progress={file.progress}></ProgressBar>
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
