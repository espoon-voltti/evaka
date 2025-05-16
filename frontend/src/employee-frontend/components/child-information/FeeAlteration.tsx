// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useRef, useState } from 'react'

import type { FeeAlteration } from 'lib-common/generated/api-types/invoicing'
import type { ChildId } from 'lib-common/generated/api-types/shared'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import type { UUID } from 'lib-common/types'
import { scrollToRef } from 'lib-common/utils/scrolling'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { Gap } from 'lib-components/white-space'
import { faQuestion } from 'lib-icons'

import type { ChildState } from '../../components/child-information/state'
import { ChildContext } from '../../components/child-information/state'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { renderResult } from '../async-rendering'

import FeeAlterationEditor from './fee-alteration/FeeAlterationEditor'
import FeeAlterationList from './fee-alteration/FeeAlterationList'
import {
  createFeeAlterationMutation,
  deleteFeeAlterationMutation,
  getFeeAlterationsQuery,
  updateFeeAlterationMutation
} from './queries'

const newFeeAlterationUiMode = 'create-new-fee-alteration'
const editFeeAlterationUiMode = (id: UUID) => `edit-fee-alteration-${id}`

interface Props {
  childId: ChildId
}

export default React.memo(function FeeAlteration({ childId }: Props) {
  const { i18n } = useTranslation()
  const { uiMode, toggleUiMode, clearUiMode, setErrorMessage } =
    useContext(UIContext)
  const { permittedActions } = useContext<ChildState>(ChildContext)

  const [deleted, setDeleted] = useState<FeeAlteration>()
  const refSectionTop = useRef(null)

  const feeAlterations = useQueryResult(
    getFeeAlterationsQuery({ personId: childId })
  )
  const { mutateAsync: createFeeAlteration } = useMutationResult(
    createFeeAlterationMutation
  )
  const { mutateAsync: updateFeeAlteration } = useMutationResult(
    updateFeeAlterationMutation
  )
  const { mutateAsync: deleteFeeAlteration } = useMutationResult(
    deleteFeeAlterationMutation
  )

  const onFailure = useCallback(() => {
    setErrorMessage({
      type: 'error',
      title: i18n.childInformation.feeAlteration.updateError,
      resolveLabel: i18n.common.ok
    })
  }, [i18n, setErrorMessage])

  return (
    <div ref={refSectionTop}>
      {permittedActions.has('CREATE_FEE_ALTERATION') && (
        <AddButtonRow
          text={i18n.childInformation.feeAlteration.create}
          data-qa="create-fee-alteration-button"
          onClick={() => {
            toggleUiMode(newFeeAlterationUiMode)
            scrollToRef(refSectionTop)
          }}
          disabled={uiMode === newFeeAlterationUiMode}
        />
      )}
      <Gap size="m" />
      {uiMode === newFeeAlterationUiMode ? (
        <FeeAlterationEditor
          personId={childId}
          cancel={() => clearUiMode()}
          create={(data) =>
            createFeeAlteration({
              body: { ...data, modifiedBy: null, modifiedAt: null }
            })
          }
          onSuccess={clearUiMode}
          onFailure={onFailure}
        />
      ) : null}
      {renderResult(feeAlterations, (feeAlterations) => (
        <FeeAlterationList
          feeAlterations={feeAlterations}
          toggleEditing={(id) => toggleUiMode(editFeeAlterationUiMode(id))}
          isEdited={(id) => uiMode === editFeeAlterationUiMode(id)}
          cancel={clearUiMode}
          update={(data) =>
            updateFeeAlteration({
              feeAlterationId: data.id!,
              body: data
            })
          }
          onSuccess={clearUiMode}
          onFailure={onFailure}
          toggleDeleteModal={setDeleted}
        />
      ))}
      {deleted ? (
        <InfoModal
          type="warning"
          title={i18n.childInformation.feeAlteration.confirmDelete}
          text={`${
            i18n.childInformation.feeAlteration.types[deleted.type]
          } ${i18n.common.period.toLowerCase()} ${deleted.validFrom.format()} - ${
            deleted.validTo?.format() ?? ''
          }`}
          icon={faQuestion}
          reject={{
            action: () => setDeleted(undefined),
            label: i18n.common.cancel
          }}
          resolve={{
            action: () =>
              deleted &&
              deleted.id !== null &&
              deleteFeeAlteration({
                personId: childId,
                feeAlterationId: deleted.id
              }).then((res) => {
                setDeleted(undefined)
                if (!res.isSuccess) {
                  setErrorMessage({
                    type: 'error',
                    title: i18n.childInformation.feeAlteration.deleteError,
                    resolveLabel: i18n.common.ok
                  })
                }
              }),
            label: i18n.common.remove
          }}
        />
      ) : null}
    </div>
  )
})
