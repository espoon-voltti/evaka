// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useMemo, useRef } from 'react'

import type { Attachment } from 'lib-common/generated/api-types/attachment'
import type { IncomeStatementAttachmentType } from 'lib-common/generated/api-types/incomestatement'
import type {
  AttachmentId,
  IncomeStatementId
} from 'lib-common/generated/api-types/shared'
import type { IncomeStatementAttachments } from 'lib-common/income-statements/attachments'
import type { UploadHandler } from 'lib-components/molecules/FileUpload'

import { incomeStatementAttachment } from '../attachments'

import type { SetStateCallback } from './hooks'

export interface AttachmentHandler {
  hasAttachment: (attachmentType: IncomeStatementAttachmentType) => boolean
  fileUploadProps: (type: IncomeStatementAttachmentType) => {
    files: Attachment[]
    uploadHandler: UploadHandler
    onUploaded: (attachment: Attachment) => void
    onDeleted: (id: AttachmentId) => void
    getDownloadUrl: (id: AttachmentId) => string
  }
  setElement: (
    attachmentType: IncomeStatementAttachmentType,
    el: HTMLElement | null
  ) => void
  focus: (attachmentType: IncomeStatementAttachmentType) => void
}

/** Returns `undefined` if the income statement contains old untyped attachments */
export function useAttachmentHandler(
  id: IncomeStatementId | undefined,
  attachments: IncomeStatementAttachments,
  onChange: SetStateCallback<IncomeStatementAttachments>
): AttachmentHandler | undefined {
  const refs = useRef<
    Partial<Record<IncomeStatementAttachmentType, HTMLElement>>
  >({})
  return useMemo(() => {
    if (!attachments.typed) {
      // Has untyped attachments
      return undefined
    }
    const { attachmentsByType } = attachments
    return {
      hasAttachment: (attachmentType: IncomeStatementAttachmentType) =>
        !!attachmentsByType[attachmentType]?.length,
      fileUploadProps: (attachmentType: IncomeStatementAttachmentType) => {
        const files = attachmentsByType[attachmentType] ?? []
        return {
          files,
          uploadHandler: incomeStatementAttachment(id, attachmentType),
          onUploaded: (attachment: Attachment) => {
            onChange((prev) => {
              // Should not happen
              if (!prev.typed) return prev

              const { attachmentsByType } = prev
              if (attachmentsByType[attachmentType]) {
                return {
                  ...prev,
                  attachmentsByType: {
                    ...attachmentsByType,
                    [attachmentType]: [
                      ...attachmentsByType[attachmentType],
                      attachment
                    ]
                  }
                }
              } else {
                return {
                  ...prev,
                  attachmentsByType: {
                    ...attachmentsByType,
                    [attachmentType]: [attachment]
                  }
                }
              }
            })
          },
          onDeleted: (id: AttachmentId) => {
            onChange((prev) => {
              // Should not happen
              if (!prev.typed) return prev

              const { attachmentsByType } = prev
              if (attachmentsByType[attachmentType]) {
                return {
                  ...prev,
                  attachmentsByType: {
                    ...attachmentsByType,
                    [attachmentType]: attachmentsByType[attachmentType].filter(
                      (a) => a.id !== id
                    )
                  }
                }
              } else {
                return prev
              }
            })
          },
          getDownloadUrl: () => ''
        }
      },
      setElement: (
        attachmentType: IncomeStatementAttachmentType,
        el: HTMLElement | null
      ) => {
        if (el) {
          refs.current[attachmentType] = el
        } else {
          delete refs.current[attachmentType]
        }
      },
      focus: (attachmentType: IncomeStatementAttachmentType) => {
        const element = refs.current[attachmentType]
        if (element) element.focus()
      }
    }
  }, [attachments, id, onChange])
}
