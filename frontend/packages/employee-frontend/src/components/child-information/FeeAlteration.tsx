// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useRef, useState } from 'react'
import { faMoneyCheckEdit, faQuestion } from 'icon-set'
import Loader from '~components/shared/atoms/Loader'
import InfoModal from '~components/common/InfoModal'
import FeeAlterationList from './fee-alteration/FeeAlterationList'
import FeeAlterationEditor from './fee-alteration/FeeAlterationEditor'
import { useTranslation } from '~state/i18n'
import { UIContext } from '~state/ui'
import { ChildContext } from '~state'
import { UUID } from '~types'
import { FeeAlteration, PartialFeeAlteration } from '~types/fee-alteration'
import { isFailure, isLoading, isSuccess, Loading, Result } from '~api'
import {
  createFeeAlteration,
  deleteFeeAlteration,
  getFeeAlterations,
  updateFeeAlteration
} from 'api/child/fee-alteration'
import { AddButtonRow } from 'components/shared/atoms/buttons/AddButton'
import { scrollToRef } from 'utils'
import CollapsibleSection from 'components/shared/molecules/CollapsibleSection'

const newFeeAlterationUiMode = 'create-new-fee-alteration'
const editFeeAlterationUiMode = (id: UUID) => `edit-fee-alteration-${id}`

interface Props {
  id: UUID
  open: boolean
}

const FeeAlteration = React.memo(function FeeAlteration({ id, open }: Props) {
  const { i18n } = useTranslation()
  const { uiMode, toggleUiMode, clearUiMode, setErrorMessage } = useContext(
    UIContext
  )
  const { feeAlterations, setFeeAlterations } = useContext(ChildContext)
  const [deleted, setDeleted] = useState<FeeAlteration>()
  const refSectionTop = useRef(null)

  const loadFeeAlterations = () => {
    setFeeAlterations(Loading())
    void getFeeAlterations(id).then(setFeeAlterations)
  }

  useEffect(() => {
    loadFeeAlterations()
    return () => setFeeAlterations(Loading())
  }, [id])

  const handleChange = (result: Result<void>) => {
    if (isSuccess(result)) {
      clearUiMode()
      loadFeeAlterations()
    } else {
      setErrorMessage({
        type: 'error',
        title: i18n.childInformation.feeAlteration.updateError
      })
    }
  }

  const content = () => {
    if (isLoading(feeAlterations)) return <Loader />
    if (isFailure(feeAlterations))
      return <div>{i18n.childInformation.feeAlteration.error}</div>

    return (
      <FeeAlterationList
        feeAlterations={feeAlterations.data}
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
      <CollapsibleSection
        icon={faMoneyCheckEdit}
        title={i18n.childInformation.feeAlteration.title}
        startCollapsed={!open}
      >
        <AddButtonRow
          text={i18n.childInformation.feeAlteration.create}
          onClick={() => {
            toggleUiMode(newFeeAlterationUiMode)
            scrollToRef(refSectionTop)
          }}
          disabled={uiMode === newFeeAlterationUiMode}
        />
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
      </CollapsibleSection>
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
          reject={() => setDeleted(undefined)}
          resolve={() =>
            deleted &&
            deleteFeeAlteration(deleted.id).then((res) => {
              setDeleted(undefined)
              if (isSuccess(res)) {
                loadFeeAlterations()
              } else {
                setErrorMessage({
                  type: 'error',
                  title: i18n.childInformation.feeAlteration.deleteError
                })
              }
            })
          }
          rejectLabel={i18n.common.cancel}
          resolveLabel={i18n.common.remove}
        />
      ) : null}
    </div>
  )
})

export default FeeAlteration
