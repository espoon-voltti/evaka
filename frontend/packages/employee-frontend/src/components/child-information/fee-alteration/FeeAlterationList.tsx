// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment } from 'react'
import styled from 'styled-components'
import { faPen, faTrash } from '@evaka/lib-icons'
import IconButton from '@evaka/lib-components/src/atoms/buttons/IconButton'
import ListGrid from '@evaka/lib-components/src/layout/ListGrid'
import { FixedSpaceRow } from '@evaka/lib-components/src/layout/flex-helpers'
import { Label } from '@evaka/lib-components/src/typography'
import { useTranslation } from '~state/i18n'
import { FeeAlteration } from '~types/fee-alteration'
import { UUID } from '~types'
import FeeAlterationEditor from './FeeAlterationEditor'

interface Props {
  feeAlterations: FeeAlteration[]
  toggleEditing: (id: UUID) => void
  isEdited: (id: UUID) => boolean
  cancel: () => void
  update: (v: FeeAlteration) => void
  toggleDeleteModal: (v: FeeAlteration) => void
}

export default React.memo(function FeeAlterationList({
  feeAlterations,
  toggleEditing,
  isEdited,
  cancel,
  update,
  toggleDeleteModal
}: Props) {
  const { i18n } = useTranslation()

  return (
    <ListGrid
      labelWidth="fit-content(30%)"
      columnGap="L"
      data-qa="fee-alteration-list"
    >
      {feeAlterations.map((feeAlteration) =>
        isEdited(feeAlteration.id) ? (
          <EditorWrapper key={feeAlteration.id}>
            <FeeAlterationEditor
              key={feeAlteration.id}
              personId={feeAlteration.personId}
              baseFeeAlteration={feeAlteration}
              cancel={cancel}
              create={() => undefined}
              update={update}
            />
          </EditorWrapper>
        ) : (
          <Fragment key={feeAlteration.id}>
            <Label>{`${
              i18n.childInformation.feeAlteration.types[feeAlteration.type]
            } ${feeAlteration.amount}${
              feeAlteration.isAbsolute ? '€' : '%'
            }`}</Label>
            <FixedSpaceRow justifyContent="space-between">
              <FixedSpaceRow spacing="L">
                <Dates>{`${feeAlteration.validFrom.format()} - ${
                  feeAlteration.validTo?.format() ?? ''
                }`}</Dates>
                <span>{feeAlteration.notes}</span>
              </FixedSpaceRow>
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
            </FixedSpaceRow>
          </Fragment>
        )
      )}
    </ListGrid>
  )
})

const EditorWrapper = styled.div`
  grid-column: 1 / 3;
`

const Dates = styled.span`
  white-space: nowrap;
`
