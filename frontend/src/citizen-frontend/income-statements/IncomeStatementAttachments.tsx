// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback, useMemo, useRef } from 'react'
import styled from 'styled-components'

import { Attachment } from 'lib-common/generated/api-types/attachment'
import {
  IncomeStatementAttachmentType,
  incomeStatementAttachmentTypes
} from 'lib-common/generated/api-types/incomestatement'
import {
  AttachmentId,
  IncomeStatementId
} from 'lib-common/generated/api-types/shared'
import {
  IncomeStatementAttachments,
  numAttachments
} from 'lib-common/income-statements'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import { Button } from 'lib-components/atoms/buttons/Button'
import { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Tr } from 'lib-components/layout/Table'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import FileDownloadButton from 'lib-components/molecules/FileDownloadButton'
import FileUpload, {
  fileIcon,
  UploadHandler
} from 'lib-components/molecules/FileUpload'
import { H2, H3, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faCheck } from 'lib-icons'

import { getAttachmentUrl, incomeStatementAttachment } from '../attachments'
import { useTranslation } from '../localization'

import {
  LabelWithError,
  Row,
  SetStateCallback
} from './IncomeStatementComponents'

function attachmentSectionDataQa(type: IncomeStatementAttachmentType): string {
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
        ref={(el) => attachmentHandler.setElement(attachmentType, el)}
        data-qa={attachmentSectionDataQa(attachmentType)}
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
          <UnorderedList data-qa="missing-attachments">
            {missingAttachments.map((attachmentType) => {
              return (
                <li
                  key={attachmentType}
                  data-qa={`attachment-${attachmentType}`}
                >
                  <Button
                    appearance="link"
                    onClick={() => {
                      attachmentHandler.focus(attachmentType)
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
    attachments,
    onChange
  }: {
    incomeStatementId: IncomeStatementId | undefined
    attachments: IncomeStatementAttachments
    onChange: SetStateCallback<IncomeStatementAttachments>
  }) {
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
      <FileUpload
        files={attachments.untypedAttachments}
        uploadHandler={incomeStatementAttachment(incomeStatementId, null)}
        onUploaded={onUploaded}
        onDeleted={onDeleted}
        getDownloadUrl={getAttachmentUrl}
      />
    )
  }
)

export const CitizenAttachments = React.memo(function CitizenAttachments({
  incomeStatementAttachments
}: {
  incomeStatementAttachments: IncomeStatementAttachments
}) {
  const t = useTranslation()
  const noAttachments = numAttachments(incomeStatementAttachments) === 0
  return (
    <>
      <H2>{t.income.view.citizenAttachments.title}</H2>
      {noAttachments ? (
        <p>{t.income.view.citizenAttachments.noAttachments}</p>
      ) : !incomeStatementAttachments.typed ? (
        <Row
          label={`${t.income.view.attachments}:`}
          value={
            <UploadedFiles
              files={incomeStatementAttachments.untypedAttachments}
            />
          }
        />
      ) : (
        <Table>
          <Tbody>
            {incomeStatementAttachmentTypes.flatMap((attachmentType) => {
              const attachments =
                incomeStatementAttachments.attachmentsByType[attachmentType]
              if (!attachments?.length) return []
              return (
                <Tr
                  key={attachmentType}
                  data-qa={`attachments-${attachmentType}`}
                >
                  <Td>
                    {t.income.attachments.attachmentNames[attachmentType]}
                  </Td>
                  <Td>
                    <UploadedFiles files={attachments} />
                  </Td>
                </Tr>
              )
            })}
          </Tbody>
        </Table>
      )}
    </>
  )
})

const UploadedFiles = React.memo(function UploadedFiles({
  files
}: {
  files: Attachment[]
}) {
  return (
    <FixedSpaceColumn>
      {files.map((file) => (
        <div key={file.id}>
          <FileIcon icon={fileIcon(file)} />
          <FileDownloadButton
            file={file}
            getFileUrl={getAttachmentUrl}
            data-qa={`file-${file.name}`}
          />
        </div>
      ))}
    </FixedSpaceColumn>
  )
})

const FileIcon = styled(FontAwesomeIcon)`
  color: ${(p) => p.theme.colors.main.m2};
  margin-right: ${defaultMargins.s};
`

export const CitizenAttachmentsWithUpload = React.memo(
  function CitizenAttachmentsWithUpload({
    incomeStatementId,
    requiredAttachments,
    incomeStatementAttachments,
    onChange
  }: {
    incomeStatementId: IncomeStatementId
    requiredAttachments: Set<IncomeStatementAttachmentType>
    incomeStatementAttachments: IncomeStatementAttachments
    onChange: SetStateCallback<IncomeStatementAttachments>
  }) {
    const t = useTranslation()
    const attachmentHandler = useAttachmentHandler(
      incomeStatementId,
      incomeStatementAttachments,
      onChange
    )

    if (!incomeStatementAttachments.typed) {
      return (
        <>
          <H2>{t.income.view.citizenAttachments.title}</H2>
          <IncomeStatementUntypedAttachments
            incomeStatementId={incomeStatementId}
            attachments={incomeStatementAttachments}
            onChange={onChange}
          />
        </>
      )
    }

    return (
      <>
        {incomeStatementAttachmentTypes.map((attachmentType) =>
          requiredAttachments.has(attachmentType) ||
          attachmentType === 'OTHER' ? (
            <AttachmentSection
              key={attachmentType}
              attachmentType={attachmentType}
              showFormErrors={false}
              attachmentHandler={attachmentHandler}
              labelKey="attachmentNames"
              optional={attachmentType === 'OTHER'}
            />
          ) : null
        )}
      </>
    )
  }
)
