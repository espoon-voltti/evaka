// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useRef, useState } from 'react'

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
} from '../../api/child/fee-alteration'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { FeeAlteration } from '../../types/fee-alteration'
import { renderResult } from '../async-rendering'

import FeeAlterationEditor from './fee-alteration/FeeAlterationEditor'
import FeeAlterationList from './fee-alteration/FeeAlterationList'

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
  const [feeAlterations, loadFeeAlterations] = useApiState(
    () => getFeeAlterations(id),
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
        <AddButtonRow
          text={i18n.childInformation.feeAlteration.create}
          onClick={() => {
            toggleUiMode(newFeeAlterationUiMode)
            scrollToRef(refSectionTop)
          }}
          disabled={uiMode === newFeeAlterationUiMode}
        />
        <Gap size="m" />
        {uiMode === newFeeAlterationUiMode ? (
          <FeeAlterationEditor
            personId={id}
            cancel={() => clearUiMode()}
            create={createFeeAlteration}
            onSuccess={onSuccess}
            onFailure={onFailure}
          />
        ) : null}
        {renderResult(feeAlterations, (feeAlterations) => (
          <FeeAlterationList
            feeAlterations={feeAlterations}
            toggleEditing={(id) => toggleUiMode(editFeeAlterationUiMode(id))}
            isEdited={(id) => uiMode === editFeeAlterationUiMode(id)}
            cancel={clearUiMode}
            update={updateFeeAlteration}
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
              deleteFeeAlteration(deleted.id).then((res) => {
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
              }),
            label: i18n.common.remove
          }}
        />
      ) : null}
    </div>
  )
})
