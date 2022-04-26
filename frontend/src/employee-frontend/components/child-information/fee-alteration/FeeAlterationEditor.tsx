// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useState } from 'react'

import { Result, Success } from 'lib-common/api'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import Title from 'lib-components/atoms/Title'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import TextArea from 'lib-components/atoms/form/TextArea'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'

import DateRangeInput from '../../../components/common/DateRangeInput'
import LabelValueList from '../../../components/common/LabelValueList'
import { useTranslation } from '../../../state/i18n'
import {
  FeeAlteration,
  PartialFeeAlteration
} from '../../../types/fee-alteration'

import FeeAlterationRowInput from './FeeAlterationRowInput'

const newFeeAlteration = (personId: UUID): PartialFeeAlteration => ({
  personId,
  type: 'DISCOUNT',
  amount: 0,
  isAbsolute: false,
  notes: '',
  validFrom: LocalDate.today(),
  validTo: null
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
        <Gap size="m" />
        <FixedSpaceRow justifyContent="flex-end">
          <Button
            onClick={cancel}
            text={i18n.childInformation.feeAlteration.editor.cancel}
          />
          <AsyncButton
            primary
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
