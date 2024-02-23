// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useRef, useState } from 'react'

import { ChildContext, ChildState } from 'employee-frontend/state/child'
import { wrapResult } from 'lib-common/api'
import { FeeAlteration } from 'lib-common/generated/api-types/invoicing'
import { UUID } from 'lib-common/types'
import { scrollToRef } from 'lib-common/utils/scrolling'
import { useApiState } from 'lib-common/utils/useRestApi'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faQuestion } from 'lib-icons'

import {
  createFeeAlteration,
  deleteFeeAlteration,
  getFeeAlterations,
  updateFeeAlteration
} from '../../generated/api-clients/invoicing'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { renderResult } from '../async-rendering'

import FeeAlterationEditor from './fee-alteration/FeeAlterationEditor'
import FeeAlterationList from './fee-alteration/FeeAlterationList'

const createFeeAlterationResult = wrapResult(createFeeAlteration)
const getFeeAlterationsResult = wrapResult(getFeeAlterations)
const updateFeeAlterationResult = wrapResult(updateFeeAlteration)
const deleteFeeAlterationResult = wrapResult(deleteFeeAlteration)

const newFeeAlterationUiMode = 'create-new-fee-alteration'
const editFeeAlterationUiMode = (id: UUID) => `edit-fee-alteration-${id}`

interface Props {
  id: UUID
  startOpen: boolean
}

export default React.memo(function FeeAlteration({ id, startOpen }: Props) {
  const { i18n } = useTranslation()
  const { uiMode, toggleUiMode, clearUiMode, setErrorMessage } =
    useContext(UIContext)
  const { permittedActions } = useContext<ChildState>(ChildContext)
  const [feeAlterations, loadFeeAlterations] = useApiState(
    () => getFeeAlterationsResult({ personId: id }),
    [id]
  )

  const [open, setOpen] = useState(startOpen)
  const [deleted, setDeleted] = useState<FeeAlteration>()
  const refSectionTop = useRef(null)

  const onSuccess = useCallback(() => {
    clearUiMode()
    void loadFeeAlterations()
  }, [clearUiMode, loadFeeAlterations])
  const onFailure = useCallback(() => {
    setErrorMessage({
      type: 'error',
      title: i18n.childInformation.feeAlteration.updateError,
      resolveLabel: i18n.common.ok
    })
  }, [i18n, setErrorMessage])

  return (
    <div ref={refSectionTop}>
      <CollapsibleContentArea
        title={<H2 noMargin>{i18n.childInformation.feeAlteration.title}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
        data-qa="fee-alteration-collapsible"
      >
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
            personId={id}
            cancel={() => clearUiMode()}
            create={(data) =>
              createFeeAlterationResult({
                body: { ...data, updatedBy: null, updatedAt: null }
              })
            }
            onSuccess={onSuccess}
            onFailure={onFailure}
          />
        ) : null}
        {renderResult(feeAlterations, (feeAlterations) => (
          <FeeAlterationList
            feeAlterations={feeAlterations}
            toggleEditing={(id) => toggleUiMode(editFeeAlterationUiMode(id))}
            isEdited={(id) => uiMode === editFeeAlterationUiMode(id)}
            cancel={onSuccess}
            update={(data) =>
              updateFeeAlterationResult({
                feeAlterationId: data.id!,
                body: data
              })
            }
            onSuccess={onSuccess}
            onFailure={onFailure}
            toggleDeleteModal={setDeleted}
          />
        ))}
      </CollapsibleContentArea>
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
              deleteFeeAlterationResult({ feeAlterationId: deleted.id }).then(
                (res) => {
                  setDeleted(undefined)
                  if (res.isSuccess) {
                    void loadFeeAlterations()
                  } else {
                    setErrorMessage({
                      type: 'error',
                      title: i18n.childInformation.feeAlteration.deleteError,
                      resolveLabel: i18n.common.ok
                    })
                  }
                }
              ),
            label: i18n.common.remove
          }}
        />
      ) : null}
    </div>
  )
})
