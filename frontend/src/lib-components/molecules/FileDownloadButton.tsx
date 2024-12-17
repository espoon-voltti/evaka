// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback } from 'react'
import styled from 'styled-components'

import { Attachment } from 'lib-common/api-types/attachment'
import { AttachmentId } from 'lib-common/generated/api-types/shared'

import { FixedSpaceRow } from '../layout/flex-helpers'

import { fileIcon } from './FileUpload'

const DownloadButton = styled.button`
  background: none;
  border: none;
  color: ${(p) => p.theme.colors.main.m1};
  cursor: pointer;
  font-size: 1rem;
  padding: 0;
  text-align: start;
  text-decoration: none;
`

interface FileDownloadButtonProps {
  file: Attachment
  getFileUrl: (fileId: AttachmentId, fileName: string) => string
  afterOpen?: () => void
  icon?: IconDefinition | boolean
  'data-qa'?: string
  text?: string
}

export default React.memo(function FileDownloadButton({
  file,
  getFileUrl,
  afterOpen,
  icon,
  'data-qa': dataQa,
  text
}: FileDownloadButtonProps) {
  const url = getFileUrl(file.id, file.name)

  const handleClick = useCallback(() => {
    window.open(url)
    afterOpen?.()
  }, [url, afterOpen])

  return (
    <DownloadButton onClick={handleClick} data-qa={dataQa}>
      <FixedSpaceRow spacing="xs" alignItems="center" key={file.id}>
        {icon && (
          <FontAwesomeIcon icon={icon === true ? fileIcon(file) : icon} />
        )}
        <div>{text ?? file.name}</div>
      </FixedSpaceRow>
    </DownloadButton>
  )
})
