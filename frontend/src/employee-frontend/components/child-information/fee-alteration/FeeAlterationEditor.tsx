// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useState } from 'react'

import {
  getAttachmentUrl,
  saveFeeAlterationAttachment
} from 'employee-frontend/api/attachments'
import { Result, Success, wrapResult } from 'lib-common/api'
import { Attachment } from 'lib-common/api-types/attachment'
import { FeeAlteration } from 'lib-common/generated/api-types/invoicing'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import Title from 'lib-components/atoms/Title'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import { Button } from 'lib-components/atoms/buttons/Button'
import TextArea from 'lib-components/atoms/form/TextArea'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import FileUpload from 'lib-components/molecules/FileUpload'
import { P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import DateRangeInput from '../../../components/common/DateRangeInput'
import LabelValueList from '../../../components/common/LabelValueList'
import { deleteAttachment } from '../../../generated/api-clients/attachment'
import { useTranslation } from '../../../state/i18n'
import { PartialFeeAlteration } from '../../../types/fee-alteration'

import FeeAlterationRowInput from './FeeAlterationRowInput'

const deleteAttachmentResult = wrapResult(deleteAttachment)

const newFeeAlteration = (
  personId: UUID,
  feeAlterationId?: UUID
): PartialFeeAlteration => ({
  id: feeAlterationId ?? null,
  personId,
  type: 'DISCOUNT',
  amount: 0,
  isAbsolute: false,
  notes: '',
  validFrom: LocalDate.todayInSystemTz(),
  validTo: null,
  attachments: []
})

interface Props {
  personId: UUID
  baseFeeAlteration?: FeeAlteration
  cancel: () => void
  update?: (v: FeeAlteration) => Promise<Result<unknown>>
  create?: (v: PartialFeeAlteration) => Promise<Result<unknown>>
  onSuccess: () => void
  onFailure?: () => void
}

export default React.memo(function FeeAlterationEditor({
  personId,
  baseFeeAlteration,
  cancel,
  create = () => Promise.resolve(Success.of()),
  update = () => Promise.resolve(Success.of()),
  onSuccess,
  onFailure
}: Props) {
  const { i18n } = useTranslation()
  const [edited, setEdited] = useState(
    baseFeeAlteration || newFeeAlteration(personId)
  )

  const [validationErrors, setValidationErrors] = useState<
    Partial<{ [K in keyof FeeAlteration | 'dates']: boolean }>
  >({})

  useEffect(() => {
    edited.validTo && !edited.validFrom.isBefore(edited.validTo)
      ? setValidationErrors((prev) => ({ ...prev, validTo: true }))
      : setValidationErrors((prev) => ({ ...prev, validTo: false }))
  }, [edited])

  const onSubmit = useCallback(() => {
    if (Object.values(validationErrors).some(Boolean)) return
    return !baseFeeAlteration
      ? create(edited)
      : update({ ...baseFeeAlteration, ...edited })
  }, [baseFeeAlteration, create, edited, update, validationErrors])

  return (
    <div>
      <div className="separator" />
      <Title size={4}>
        {
          i18n.childInformation.feeAlteration.editor[
            !baseFeeAlteration ? 'titleNew' : 'titleEdit'
          ]
        }
      </Title>
      <form>
        <LabelValueList
          spacing="large"
          contents={[
            {
              label: i18n.childInformation.feeAlteration.editor.alterationType,
              value: (
                <FixedSpaceRow>
                  <FeeAlterationRowInput
                    edited={edited}
                    setEdited={setEdited}
                  />
                </FixedSpaceRow>
              )
            },
            {
              label: i18n.childInformation.feeAlteration.editor.validDuring,
              value: (
                <FixedSpaceRow>
                  <DateRangeInput
                    data-qa="fee-alteration-date-range-input"
                    start={edited.validFrom}
                    end={edited.validTo ? edited.validTo : undefined}
                    onChange={(start: LocalDate, end?: LocalDate) =>
                      setEdited((state) => ({
                        ...state,
                        validFrom: start,
                        validTo: end ?? null
                      }))
                    }
                    onValidationResult={(hasErrors) =>
                      setValidationErrors({ dates: hasErrors })
                    }
                    nullableEndDate
                  />
                </FixedSpaceRow>
              )
            },
            {
              label: i18n.childInformation.feeAlteration.editor.notes,
              value: (
                <TextArea
                  value={edited.notes}
                  onChange={(value) => setEdited({ ...edited, notes: value })}
                  data-qa="fee-alteration-notes-input"
                />
              )
            }
          ]}
        />
        <FeeAlterationAttachments
          feeAlterationId={baseFeeAlteration?.id ?? null}
          attachments={baseFeeAlteration?.attachments ?? []}
          onUploaded={(attachment) => {
            setEdited((prev) => ({
              ...prev,
              attachments: prev.attachments.concat(attachment)
            }))
          }}
          onDeleted={(deletedId) => {
            setEdited((prev) => ({
              ...prev,
              attachments: prev.attachments.filter(({ id }) => id !== deletedId)
            }))
          }}
        />
        <Gap size="m" />
        <FixedSpaceRow justifyContent="flex-end">
          <Button
            onClick={cancel}
            text={i18n.childInformation.feeAlteration.editor.cancel}
          />
          <AsyncButton
            primary
            data-qa="fee-alteration-editor-save-button"
            type="submit"
            onClick={onSubmit}
            onSuccess={onSuccess}
            onFailure={onFailure}
            disabled={Object.values(validationErrors).some(Boolean)}
            text={i18n.childInformation.feeAlteration.editor.save}
          />
        </FixedSpaceRow>
      </form>
      <div className="separator" />
    </div>
  )
})

function FeeAlterationAttachments({
  feeAlterationId,
  attachments,
  onUploaded,
  onDeleted
}: {
  feeAlterationId: UUID | null
  attachments: Attachment[]
  onUploaded: (attachment: Attachment) => void
  onDeleted: (id: UUID) => void
}) {
  const { i18n } = useTranslation()

  const handleUpload = useCallback(
    async (file: File, onUploadProgress: (percentage: number) => void) =>
      (
        await saveFeeAlterationAttachment(
          feeAlterationId,
          file,
          onUploadProgress
        )
      ).map((id) => {
        onUploaded({ id, name: file.name, contentType: file.type })
        return id
      }),
    [feeAlterationId, onUploaded]
  )

  const handleDelete = useCallback(
    async (id: UUID) =>
      (await deleteAttachmentResult({ attachmentId: id })).map(() => {
        onDeleted(id)
      }),
    [onDeleted]
  )

  return (
    <>
      <Title size={4}>
        {i18n.childInformation.feeAlteration.employeeAttachments.title}
      </Title>
      <P>
        {i18n.childInformation.feeAlteration.employeeAttachments.description}
      </P>
      <FileUpload
        data-qa="fee-alteration-attachment-upload"
        files={attachments}
        onUpload={handleUpload}
        onDelete={handleDelete}
        getDownloadUrl={getAttachmentUrl}
      />
    </>
  )
}
