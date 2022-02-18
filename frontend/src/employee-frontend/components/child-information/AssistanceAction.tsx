// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo, useRef } from 'react'
import styled from 'styled-components'

import { ChildState, ChildContext } from 'employee-frontend/state/child'
import { combine } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import { scrollToRef } from 'lib-common/utils/scrolling'
import { useApiState } from 'lib-common/utils/useRestApi'
import Title from 'lib-components/atoms/Title'
import AddButton from 'lib-components/atoms/buttons/AddButton'

import {
  getAssistanceActionOptions,
  getAssistanceActions
} from '../../api/child/assistance-actions'
import AssistanceActionForm from '../../components/child-information/assistance-action/AssistanceActionForm'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { renderResult } from '../async-rendering'

import AssistanceActionRow from './assistance-action/AssistanceActionRow'

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

export default React.memo(function AssistanceAction({ id }: Props) {
  const { i18n } = useTranslation()
  const { permittedActions } = useContext<ChildState>(ChildContext)
  const [assistanceActions, loadData] = useApiState(
    () => getAssistanceActions(id),
    [id]
  )
  const { uiMode, toggleUiMode } = useContext(UIContext)
  const refSectionTop = useRef(null)

  const [assistanceActionOptions] = useApiState(getAssistanceActionOptions, [])

  const duplicate = useMemo(
    () =>
      !!uiMode && uiMode.startsWith('duplicate-assistance-action')
        ? assistanceActions
            .map((actions) =>
              actions.find((an) => an.action.id == uiMode.split('_').pop())
            )
            .getOrElse(undefined)
        : undefined,
    [assistanceActions, uiMode]
  )

  return renderResult(
    combine(assistanceActions, assistanceActionOptions),
    ([assistanceActions, assistanceActionOptions]) => (
      <div ref={refSectionTop}>
        <TitleRow>
          <Title size={4}>{i18n.childInformation.assistanceAction.title}</Title>
          {permittedActions.has('CREATE_ASSISTANCE_ACTION') && (
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
          )}
        </TitleRow>
        {uiMode === 'create-new-assistance-action' && (
          <>
            <AssistanceActionForm
              childId={id}
              onReload={loadData}
              assistanceActions={assistanceActions}
              assistanceActionOptions={assistanceActionOptions}
            />
            <div className="separator" />
          </>
        )}
        {duplicate && (
          <>
            <AssistanceActionForm
              childId={id}
              assistanceAction={duplicate.action}
              onReload={loadData}
              assistanceActions={assistanceActions}
              assistanceActionOptions={assistanceActionOptions}
            />
            <div className="separator" />
          </>
        )}
        {assistanceActions.map((assistanceAction) => (
          <AssistanceActionRow
            key={assistanceAction.action.id}
            assistanceAction={assistanceAction.action}
            permittedActions={assistanceAction.permittedActions}
            onReload={loadData}
            assistanceActions={assistanceActions}
            assistanceActionOptions={assistanceActionOptions}
            refSectionTop={refSectionTop}
          />
        ))}
      </div>
    )
  )
})
