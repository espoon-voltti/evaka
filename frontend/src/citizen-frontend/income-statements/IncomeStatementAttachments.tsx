// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback } from 'react'

import { Attachment } from 'lib-common/generated/api-types/attachment'
import { IncomeStatementAttachmentType } from 'lib-common/generated/api-types/incomestatement'
import {
  AttachmentId,
  IncomeStatementId
} from 'lib-common/generated/api-types/shared'
import { IncomeStatementAttachments } from 'lib-common/income-statements'
import { scrollToElement } from 'lib-common/utils/scrolling'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import { Button } from 'lib-components/atoms/buttons/Button'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import FileUpload, { UploadHandler } from 'lib-components/molecules/FileUpload'
import { H3, H4, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faCheck } from 'lib-icons'

import { getAttachmentUrl, incomeStatementAttachment } from '../attachments'
import { useTranslation } from '../localization'

import { LabelWithError, SetStateCallback } from './IncomeStatementComponents'

function attachmentSectionId(type: IncomeStatementAttachmentType): string {
  return `attachment-section-${type}`
}

export interface AttachmentHandler {
  hasAttachment: (attachmentType: IncomeStatementAttachmentType) => boolean
  fileUploadProps: (type: IncomeStatementAttachmentType) => {
    files: Attachment[]
    uploadHandler: UploadHandler
    onUploaded: (attachment: Attachment) => void
    onDeleted: (id: AttachmentId) => void
    getDownloadUrl: (id: AttachmentId) => string
  }
}

/** Returns `undefined` if the income statement contains old untyped attachments */
export function makeAttachmentHandler(
  id: IncomeStatementId | undefined,
  attachments: IncomeStatementAttachments,
  onChange: SetStateCallback<IncomeStatementAttachments>
): AttachmentHandler | undefined {
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
    }
  }
}

export const AttachmentSection = React.memo(function AttachmentSection({
  attachmentType,
  showFormErrors,
  attachmentHandler,
  infoText = '',
  dense = false,
  optional = false,
  labelKey = 'addAttachment'
}: {
  attachmentType: IncomeStatementAttachmentType
  showFormErrors: boolean
  attachmentHandler: AttachmentHandler | undefined
  infoText?: string
  dense?: boolean
  optional?: boolean
  labelKey?: 'addAttachment' | 'attachmentNames'
}) {
  const t = useTranslation()
  if (!attachmentHandler) return null

  const { hasAttachment, fileUploadProps } = attachmentHandler

  const label = (
    <LabelWithError
      label={`${t.income.attachments[labelKey][attachmentType]}${optional ? '' : ' *'}`}
      showError={!optional && showFormErrors && !hasAttachment(attachmentType)}
      errorText={t.income.errors.attachmentMissing}
    />
  )

  const labelAndInfo = infoText ? (
    <ExpandingInfo info={infoText}>{label}</ExpandingInfo>
  ) : (
    label
  )

  return (
    <>
      {!dense && <Gap size="s" />}
      {labelAndInfo}
      {!dense && <Gap size="xs" />}
      <FileUpload
        id={attachmentSectionId(attachmentType)}
        {...fileUploadProps(attachmentType)}
      />
    </>
  )
})

export const IncomeStatementMissingAttachments = React.memo(
  function IncomeStatementMissingAttachments({
    requiredAttachments,
    attachmentHandler
  }: {
    requiredAttachments: Set<IncomeStatementAttachmentType>
    attachmentHandler: AttachmentHandler
  }) {
    const t = useTranslation()
    const missingAttachments = [...requiredAttachments].filter(
      (attachmentType) => !attachmentHandler.hasAttachment(attachmentType)
    )
    return (
      <ContentArea opaque paddingVertical="L">
        <H3>{t.income.attachments.missingAttachments}</H3>
        {missingAttachments.length > 0 ? (
          <UnorderedList>
            {missingAttachments.map((attachmentType) => {
              return (
                <li key={attachmentType}>
                  <Button
                    appearance="link"
                    onClick={() => {
                      const element = document.getElementById(
                        attachmentSectionId(attachmentType)
                      )
                      if (element) {
                        scrollToElement(element, 0, 'center')
                      }
                    }}
                    text={t.income.attachments.attachmentNames[attachmentType]}
                  />
                </li>
              )
            })}
          </UnorderedList>
        ) : (
          <P>
            <FontAwesomeIcon icon={faCheck} color={colors.status.info} />{' '}
            {t.income.attachments.noMissingAttachments}
          </P>
        )}
      </ContentArea>
    )
  }
)

export const IncomeStatementUntypedAttachments = React.memo(
  function IncomeStatementUntypedAttachments({
    incomeStatementId,
    requiredAttachments,
    attachments,
    onChange
  }: {
    incomeStatementId: IncomeStatementId | undefined
    requiredAttachments: Set<IncomeStatementAttachmentType>
    attachments: IncomeStatementAttachments
    onChange: SetStateCallback<IncomeStatementAttachments>
  }) {
    const t = useTranslation()

    const onUploaded = useCallback(
      (attachment: Attachment) =>
        onChange((prev) =>
          prev.typed
            ? prev
            : {
                ...prev,
                untypedAttachments: [...prev.untypedAttachments, attachment]
              }
        ),
      [onChange]
    )

    const onDeleted = useCallback(
      (id: AttachmentId) =>
        onChange((prev) =>
          prev.typed
            ? prev
            : {
                ...prev,
                untypedAttachments: prev.untypedAttachments.filter(
                  (a) => a.id !== id
                )
              }
        ),
      [onChange]
    )

    if (attachments.typed) return null

    return (
      <ContentArea opaque paddingVertical="L">
        <FixedSpaceColumn spacing="zero">
          <H3 noMargin>{t.income.attachments.title}</H3>
          <Gap size="s" />
          <P noMargin>{t.income.attachments.description}</P>
          <Gap size="L" />
          {requiredAttachments.size > 0 && (
            <>
              <H4 noMargin>{t.income.attachments.required.title}</H4>
              <Gap size="s" />
              <UnorderedList data-qa="required-attachments">
                {[...requiredAttachments].map((attachmentType) => (
                  <li key={attachmentType}>
                    {t.income.attachments.attachmentNames[attachmentType]}
                  </li>
                ))}
              </UnorderedList>
              <Gap size="L" />
            </>
          )}
          <FileUpload
            files={attachments.untypedAttachments}
            uploadHandler={incomeStatementAttachment(incomeStatementId, null)}
            onUploaded={onUploaded}
            onDeleted={onDeleted}
            getDownloadUrl={getAttachmentUrl}
          />
        </FixedSpaceColumn>
      </ContentArea>
    )
  }
)
