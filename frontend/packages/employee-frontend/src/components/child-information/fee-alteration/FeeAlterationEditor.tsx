// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState, FormEvent, useEffect } from 'react'
import LocalDate from '@evaka/lib-common/src/local-date'
import {
  Button,
  Buttons,
  LabelValueList,
  LabelValueListItem,
  TextArea,
  Title
} from '~components/shared/alpha'
import DateRangeInput from '~components/common/DateRangeInput'
import FeeAlterationRowInput from './FeeAlterationRowInput'
import { useTranslation } from '~state/i18n'
import {
  FeeAlteration,
  feeAlterationTypes,
  PartialFeeAlteration
} from '~types/fee-alteration'
import { UUID } from '~types'
import './FeeAlterationEditor.scss'

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
  update: (v: FeeAlteration) => void
  create: (v: PartialFeeAlteration) => void
}

function FeeAlterationEditor({
  personId,
  baseFeeAlteration,
  cancel,
  create,
  update
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

  const onSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (!Object.values(validationErrors).some(Boolean)) {
      !baseFeeAlteration
        ? create(edited)
        : update({ ...baseFeeAlteration, ...edited })
    }
  }

  return (
    <div className="fee-alteration-editor">
      <div className="separator" />
      <Title size={4}>
        {
          i18n.childInformation.feeAlteration.editor[
            !baseFeeAlteration ? 'titleNew' : 'titleEdit'
          ]
        }
      </Title>
      <form onSubmit={onSubmit}>
        <LabelValueList>
          <LabelValueListItem
            label={i18n.childInformation.feeAlteration.editor.alterationType}
            value={
              <div className="row-input-container">
                <FeeAlterationRowInput
                  edited={edited}
                  setEdited={setEdited}
                  typeOptions={feeAlterationTypes.map((type) => ({
                    id: type,
                    label: i18n.childInformation.feeAlteration.types[type]
                  }))}
                />
              </div>
            }
            dataQa="fee-alteration-type-input"
          />
          <LabelValueListItem
            label={i18n.childInformation.feeAlteration.editor.validDuring}
            value={
              <div className="row-input-container">
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
              </div>
            }
            dataQa="fee-alteration-duration-input"
          />
          <LabelValueListItem
            label={i18n.childInformation.feeAlteration.editor.notes}
            value={
              <TextArea
                value={edited.notes}
                onChange={(e) =>
                  setEdited({ ...edited, notes: e.target.value })
                }
              />
            }
            dataQa="fee-alteration-notes-input"
          />
        </LabelValueList>
        <Buttons>
          <Button onClick={cancel}>
            {i18n.childInformation.feeAlteration.editor.cancel}
          </Button>
          <Button
            primary
            type="submit"
            disabled={Object.values(validationErrors).some(Boolean)}
          >
            {i18n.childInformation.feeAlteration.editor.save}
          </Button>
        </Buttons>
      </form>
      <div className="separator" />
    </div>
  )
}

export default FeeAlterationEditor
