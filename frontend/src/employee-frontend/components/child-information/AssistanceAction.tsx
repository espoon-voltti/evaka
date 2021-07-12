// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useRef, useState } from 'react'
import { useTranslation } from '../../state/i18n'
import { Loading, Result, Success } from 'lib-common/api'
import { ChildContext } from '../../state'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import { UUID } from '../../types'
import AssistanceActionRow from './assistance-action/AssistanceActionRow'
import AssistanceActionForm from '../../components/child-information/assistance-action/AssistanceActionForm'
import { UIContext } from '../../state/ui'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import styled from 'styled-components'
import { scrollToRef } from '../../utils'
import {
  getAssistanceActionOptions,
  getAssistanceActions
} from '../../api/child/assistance-actions'
import { AssistanceActionOption } from 'employee-frontend/types/child'
import { useRestApi } from 'lib-common/utils/useRestApi'

const TitleRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 30px 0;

  .title {
    margin: 0;
  }
`

export interface Props {
  id: UUID
}

function AssistanceAction({ id }: Props) {
  const { i18n } = useTranslation()
  const { assistanceActions, setAssistanceActions } = useContext(ChildContext)
  const { uiMode, toggleUiMode } = useContext(UIContext)
  const refSectionTop = useRef(null)

  const loadData = () => {
    setAssistanceActions(Loading.of())
    void getAssistanceActions(id).then(setAssistanceActions)
  }

  useEffect(loadData, [id]) // eslint-disable-line react-hooks/exhaustive-deps

  const [assistanceActionOptions, setAssistanceActionOptions] = useState<
    Result<AssistanceActionOption[]>
  >(Success.of([]))
  const loadAssistanceActionOptions = useRestApi(
    getAssistanceActionOptions,
    setAssistanceActionOptions
  )
  useEffect(() => {
    loadAssistanceActionOptions()
  }, [loadAssistanceActionOptions])

  function renderAssistanceActions() {
    if (assistanceActions.isLoading || assistanceActionOptions.isLoading) {
      return <Loader />
    } else if (
      assistanceActions.isFailure ||
      assistanceActionOptions.isFailure
    ) {
      return <div>{i18n.common.loadingFailed}</div>
    } else {
      return assistanceActions.value.map((assistanceAction) => (
        <AssistanceActionRow
          key={assistanceAction.id}
          assistanceAction={assistanceAction}
          onReload={loadData}
          assistanceActionOptions={assistanceActionOptions.value}
          refSectionTop={refSectionTop}
        />
      ))
    }
  }

  const duplicate =
    !!uiMode &&
    uiMode.startsWith('duplicate-assistance-action') &&
    assistanceActions
      .map((actions) => actions.find((an) => an.id == uiMode.split('_').pop()))
      .getOrElse(undefined)

  if (assistanceActionOptions.isLoading) {
    return <Loader />
  }
  if (assistanceActionOptions.isFailure) {
    return <div>{i18n.common.loadingFailed}</div>
  }

  return (
    <div ref={refSectionTop}>
      <TitleRow>
        <Title size={4}>{i18n.childInformation.assistanceAction.title}</Title>
        <AddButton
          flipped
          text={i18n.childInformation.assistanceAction.create}
          onClick={() => {
            toggleUiMode('create-new-assistance-action')
            scrollToRef(refSectionTop)
          }}
          disabled={uiMode === 'create-new-assistance-action'}
          data-qa="assistance-action-create-btn"
        />
      </TitleRow>
      {uiMode === 'create-new-assistance-action' && (
        <>
          <AssistanceActionForm
            childId={id}
            onReload={loadData}
            assistanceActionOptions={assistanceActionOptions.value}
          />
          <div className="separator" />
        </>
      )}
      {duplicate && (
        <>
          <AssistanceActionForm
            childId={id}
            assistanceAction={duplicate}
            onReload={loadData}
            assistanceActionOptions={assistanceActionOptions.value}
          />
          <div className="separator" />
        </>
      )}
      {renderAssistanceActions()}
    </div>
  )
}

export default AssistanceAction
