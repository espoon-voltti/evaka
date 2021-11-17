// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo, useRef } from 'react'
import { useTranslation } from '../../state/i18n'
import Title from 'lib-components/atoms/Title'
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
import { useApiState } from 'lib-common/utils/useRestApi'
import { UUID } from 'lib-common/types'
import { renderResult } from '../async-rendering'
import { combine } from 'lib-common/api'

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
              actions.find((an) => an.id == uiMode.split('_').pop())
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
              assistanceAction={duplicate}
              onReload={loadData}
              assistanceActions={assistanceActions}
              assistanceActionOptions={assistanceActionOptions}
            />
            <div className="separator" />
          </>
        )}
        {assistanceActions.map((assistanceAction) => (
          <AssistanceActionRow
            key={assistanceAction.id}
            assistanceAction={assistanceAction}
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
