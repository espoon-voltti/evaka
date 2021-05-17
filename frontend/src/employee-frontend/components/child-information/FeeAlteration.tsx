// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useRef, useState } from 'react'
import { faQuestion } from 'lib-icons'
import Loader from 'lib-components/atoms/Loader'
import { Gap } from 'lib-components/white-space'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import FeeAlterationList from './fee-alteration/FeeAlterationList'
import FeeAlterationEditor from './fee-alteration/FeeAlterationEditor'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { ChildContext } from '../../state'
import { UUID } from '../../types'
import { FeeAlteration, PartialFeeAlteration } from '../../types/fee-alteration'
import { Loading, Result } from 'lib-common/api'
import {
  createFeeAlteration,
  deleteFeeAlteration,
  getFeeAlterations,
  updateFeeAlteration
} from '../../api/child/fee-alteration'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { scrollToRef } from '../../utils'
import { CollapsibleContentArea } from '../../../lib-components/layout/Container'
import { H2 } from '../../../lib-components/typography'

const newFeeAlterationUiMode = 'create-new-fee-alteration'
const editFeeAlterationUiMode = (id: UUID) => `edit-fee-alteration-${id}`

interface Props {
  id: UUID
  startOpen: boolean
}

const FeeAlteration = React.memo(function FeeAlteration({
  id,
  startOpen
}: Props) {
  const { i18n } = useTranslation()
  const { uiMode, toggleUiMode, clearUiMode, setErrorMessage } = useContext(
    UIContext
  )
  const { feeAlterations, setFeeAlterations } = useContext(ChildContext)

  const [open, setOpen] = useState(startOpen)
  const [deleted, setDeleted] = useState<FeeAlteration>()
  const refSectionTop = useRef(null)

  const loadFeeAlterations = () => {
    setFeeAlterations(Loading.of())
    void getFeeAlterations(id).then(setFeeAlterations)
  }

  useEffect(() => {
    loadFeeAlterations()
    return () => setFeeAlterations(Loading.of())
  }, [id]) // eslint-disable-line react-hooks/exhaustive-deps

  const handleChange = (result: Result<void>) => {
    if (result.isSuccess) {
      clearUiMode()
      loadFeeAlterations()
    } else {
      setErrorMessage({
        type: 'error',
        title: i18n.childInformation.feeAlteration.updateError,
        resolveLabel: i18n.common.ok
      })
    }
  }

  const content = () => {
    if (feeAlterations.isLoading) return <Loader />
    if (feeAlterations.isFailure)
      return <div>{i18n.childInformation.feeAlteration.error}</div>

    return (
      <FeeAlterationList
        feeAlterations={feeAlterations.value}
        toggleEditing={(id) => toggleUiMode(editFeeAlterationUiMode(id))}
        isEdited={(id) => uiMode === editFeeAlterationUiMode(id)}
        cancel={() => clearUiMode()}
        update={(value: FeeAlteration) =>
          updateFeeAlteration(value).then(handleChange)
        }
        toggleDeleteModal={(feeAlteration) => setDeleted(feeAlteration)}
      />
    )
  }

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
            update={() => undefined}
            create={(value: PartialFeeAlteration) =>
              createFeeAlteration(value).then(handleChange)
            }
          />
        ) : null}
        {content()}
      </CollapsibleContentArea>
      {deleted ? (
        <InfoModal
          iconColour={'orange'}
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
                  loadFeeAlterations()
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

export default FeeAlteration
