// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { faPen, faTrash } from 'icon-set'
import IconButton from '~components/shared/atoms/buttons/IconButton'
import FeeAlterationEditor from './FeeAlterationEditor'
import { useTranslation } from '~state/i18n'
import { FeeAlteration } from '~types/fee-alteration'
import { UUID } from '~types'
import './FeeAlterationList.scss'
import { FixedSpaceRow } from '~components/shared/layout/flex-helpers'
import LabelValueList from '~components/common/LabelValueList'

interface Props {
  feeAlterations: FeeAlteration[]
  toggleEditing: (id: UUID) => void
  isEdited: (id: UUID) => boolean
  cancel: () => void
  update: (v: FeeAlteration) => void
  toggleDeleteModal: (v: FeeAlteration) => void
}

function FeeAlterationList({
  feeAlterations,
  toggleEditing,
  isEdited,
  cancel,
  update,
  toggleDeleteModal
}: Props) {
  const { i18n } = useTranslation()

  function contents() {
    return feeAlterations.map((feeAlteration) =>
      isEdited(feeAlteration.id)
        ? {
            value: (
              <FeeAlterationEditor
                key={feeAlteration.id}
                personId={feeAlteration.personId}
                baseFeeAlteration={feeAlteration}
                cancel={cancel}
                create={() => undefined}
                update={update}
              />
            ),
            onlyValue: true
          }
        : {
            label: `${
              i18n.childInformation.feeAlteration.types[feeAlteration.type]
            } ${feeAlteration.amount}${feeAlteration.isAbsolute ? '€' : '%'}`,
            value: (
              <div className="fee-alteration-item">
                <div className="description">
                  <span>
                    {`${feeAlteration.validFrom.format()} - ${
                      feeAlteration.validTo?.format() ?? ''
                    }`}
                  </span>
                  <span>{feeAlteration.notes}</span>
                </div>
                <FixedSpaceRow>
                  <IconButton
                    icon={faPen}
                    onClick={() => toggleEditing(feeAlteration.id)}
                  />
                  <IconButton
                    icon={faTrash}
                    onClick={() => toggleDeleteModal(feeAlteration)}
                  />
                </FixedSpaceRow>
              </div>
            )
          }
    )
  }

  return (
    <div
      className="fee-alteration-list-container"
      data-qa="fee-alteration-list"
    >
      <LabelValueList spacing={'small'} contents={contents()} />
    </div>
  )
}

export default FeeAlterationList
