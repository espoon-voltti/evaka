// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useState } from 'react'

import type { Result } from 'lib-common/api'
import { Success } from 'lib-common/api'
import type { Attachment } from 'lib-common/generated/api-types/attachment'
import type { FeeAlteration } from 'lib-common/generated/api-types/invoicing'
import type {
  FeeAlterationId,
  PersonId
} from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { useMutationResult } from 'lib-common/query'
import type { UUID } from 'lib-common/types'
import Title from 'lib-components/atoms/Title'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import TextArea from 'lib-components/atoms/form/TextArea'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import FileUpload from 'lib-components/molecules/FileUpload'
import LabelValueList from 'lib-components/molecules/LabelValueList'
import DateRangePicker from 'lib-components/molecules/date-picker/DateRangePicker'
import { P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import {
  getAttachmentUrl,
  feeAlterationAttachment
} from '../../../api/attachments'
import { deleteAttachmentMutation } from '../../../queries'
import { useTranslation } from '../../../state/i18n'
import type {
  FeeAlterationForm,
  PartialFeeAlteration
} from '../../../types/fee-alteration'

import FeeAlterationRowInput from './FeeAlterationRowInput'

const newFeeAlteration = (
  personId: PersonId,
  feeAlterationId?: FeeAlterationId
): FeeAlterationForm => ({
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
  personId: PersonId
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
    Partial<Record<keyof FeeAlteration | 'dates', boolean>>
  >({})

  useEffect(() => {
    setValidationErrors((prev) => ({
      ...prev,
      validFrom: edited.validFrom === null,
      validTo:
        edited.validFrom !== null &&
        edited.validTo !== null &&
        !edited.validFrom.isEqualOrBefore(edited.validTo)
    }))
  }, [edited])

  const onSubmit = useCallback(() => {
    if (!edited.validFrom || Object.values(validationErrors).some(Boolean)) {
      return
    }
    const valid = { ...edited, validFrom: edited.validFrom }
    return !baseFeeAlteration
      ? create(valid)
      : update({ ...baseFeeAlteration, ...valid })
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
                  <DateRangePicker
                    start={edited.validFrom}
                    end={edited.validTo}
                    data-qa="fee-alteration-date-range-input"
                    onChange={(start, end) =>
                      setEdited((state) => ({
                        ...state,
                        validFrom: start,
                        validTo: end
                      }))
                    }
                    onValidationResult={(isValid) => {
                      setValidationErrors({ dates: !isValid })
                    }}
                    locale="fi"
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
          <LegacyButton
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
  feeAlterationId: FeeAlterationId | null
  attachments: Attachment[]
  onUploaded: (attachment: Attachment) => void
  onDeleted: (id: UUID) => void
}) {
  const { i18n } = useTranslation()
  const { mutateAsync: deleteAttachment } = useMutationResult(
    deleteAttachmentMutation
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
        uploadHandler={feeAlterationAttachment(
          feeAlterationId,
          deleteAttachment
        )}
        onUploaded={onUploaded}
        onDeleted={onDeleted}
        getDownloadUrl={getAttachmentUrl}
      />
    </>
  )
}
