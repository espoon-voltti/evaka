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
import AssistanceNeedRow from './assistance-need/AssistanceNeedRow'
import AssistanceNeedForm from '../../components/child-information/assistance-need/AssistanceNeedForm'
import { UIContext } from '../../state/ui'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import styled from 'styled-components'
import { scrollToRef } from '../../utils'
import {
  getAssistanceBasisOptions,
  getAssistanceNeeds
} from '../../api/child/assistance-needs'
import { AssistanceBasisOption } from 'employee-frontend/types/child'
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

function AssistanceNeed({ id }: Props) {
  const { i18n } = useTranslation()
  const { assistanceNeeds, setAssistanceNeeds } = useContext(ChildContext)
  const { uiMode, toggleUiMode } = useContext(UIContext)
  const refSectionTop = useRef(null)

  const loadData = () => {
    setAssistanceNeeds(Loading.of())
    void getAssistanceNeeds(id).then(setAssistanceNeeds)
  }

  useEffect(loadData, [id]) // eslint-disable-line react-hooks/exhaustive-deps

  const [assistanceBasisOptions, setAssistanceBasisOptions] = useState<
    Result<AssistanceBasisOption[]>
  >(Success.of([]))
  const loadAssistanceBasisOptions = useRestApi(
    getAssistanceBasisOptions,
    setAssistanceBasisOptions
  )
  useEffect(() => {
    loadAssistanceBasisOptions()
  }, [loadAssistanceBasisOptions])

  function renderAssistanceNeeds() {
    if (assistanceNeeds.isLoading || assistanceBasisOptions.isLoading) {
      return <Loader />
    } else if (assistanceNeeds.isFailure || assistanceBasisOptions.isFailure) {
      return <div>{i18n.common.loadingFailed}</div>
    } else {
      return assistanceNeeds.value.map((assistanceNeed) => (
        <AssistanceNeedRow
          key={assistanceNeed.id}
          assistanceNeed={assistanceNeed}
          onReload={loadData}
          assistanceBasisOptions={assistanceBasisOptions.value}
          refSectionTop={refSectionTop}
        />
      ))
    }
  }

  const duplicate =
    !!uiMode &&
    uiMode.startsWith('duplicate-assistance-need') &&
    assistanceNeeds
      .map((needs) => needs.find((an) => an.id == uiMode.split('_').pop()))
      .getOrElse(undefined)

  if (assistanceBasisOptions.isLoading) {
    return <Loader />
  }
  if (assistanceBasisOptions.isFailure) {
    return <div>{i18n.common.loadingFailed}</div>
  }

  return (
    <div ref={refSectionTop}>
      <TitleRow>
        <Title size={4}>{i18n.childInformation.assistanceNeed.title}</Title>
        <AddButton
          flipped
          text={i18n.childInformation.assistanceNeed.create}
          onClick={() => {
            toggleUiMode('create-new-assistance-need')
            scrollToRef(refSectionTop)
          }}
          disabled={uiMode === 'create-new-assistance-need'}
          data-qa="assistance-need-create-btn"
        />
      </TitleRow>
      {uiMode === 'create-new-assistance-need' && (
        <>
          <AssistanceNeedForm
            childId={id}
            onReload={loadData}
            assistanceBasisOptions={assistanceBasisOptions.value}
          />
          <div className="separator" />
        </>
      )}
      {duplicate && (
        <>
          <AssistanceNeedForm
            childId={id}
            assistanceNeed={duplicate}
            onReload={loadData}
            assistanceBasisOptions={assistanceBasisOptions.value}
          />
          <div className="separator" />
        </>
      )}
      {renderAssistanceNeeds()}
    </div>
  )
}

export default AssistanceNeed
