// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useRef } from 'react'
import { useTranslation } from '~/state/i18n'
import { Loading } from '~/api'
import { ChildContext } from '~/state/child'
import Loader from '@evaka/lib-components/src/atoms/Loader'
import Title from '@evaka/lib-components/src/atoms/Title'
import { UUID } from '~/types'
import AssistanceActionRow from './assistance-action/AssistanceActionRow'
import AssistanceActionForm from '~components/child-information/assistance-action/AssistanceActionForm'
import { UIContext } from '~state/ui'
import AddButton from '@evaka/lib-components/src/atoms/buttons/AddButton'
import styled from 'styled-components'
import { scrollToRef } from 'utils'
import { getAssistanceActions } from 'api/child/assistance-actions'

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

  useEffect(loadData, [id])

  function renderAssistanceActions() {
    if (assistanceActions.isLoading) {
      return <Loader />
    } else if (assistanceActions.isFailure) {
      return <div>{i18n.common.loadingFailed}</div>
    } else {
      return assistanceActions.value.map((assistanceAction) => (
        <AssistanceActionRow
          key={assistanceAction.id}
          assistanceAction={assistanceAction}
          onReload={loadData}
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
          dataQa="assistance-action-create-btn"
        />
      </TitleRow>
      {uiMode === 'create-new-assistance-action' && (
        <>
          <AssistanceActionForm childId={id} onReload={loadData} />
          <div className="separator" />
        </>
      )}
      {duplicate && (
        <>
          <AssistanceActionForm
            childId={id}
            assistanceAction={duplicate}
            onReload={loadData}
          />
          <div className="separator" />
        </>
      )}
      {renderAssistanceActions()}
    </div>
  )
}

export default AssistanceAction
