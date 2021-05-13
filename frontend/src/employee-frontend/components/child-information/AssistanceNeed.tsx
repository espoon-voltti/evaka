// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useRef } from 'react'
import { useTranslation } from '../../state/i18n'
import { Loading } from 'lib-common/api'
import { ChildContext } from '../../state/child'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import { UUID } from '../../types'
import AssistanceNeedRow from './assistance-need/AssistanceNeedRow'
import AssistanceNeedForm from '../../components/child-information/assistance-need/AssistanceNeedForm'
import { UIContext } from '../../state/ui'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import styled from 'styled-components'
import { scrollToRef } from '../../utils'
import { getAssistanceNeeds } from '../../api/child/assistance-needs'

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

  function renderAssistanceNeeds() {
    if (assistanceNeeds.isLoading) {
      return <Loader />
    } else if (assistanceNeeds.isFailure) {
      return <div>{i18n.common.loadingFailed}</div>
    } else {
      return assistanceNeeds.value.map((assistanceNeed) => (
        <AssistanceNeedRow
          key={assistanceNeed.id}
          assistanceNeed={assistanceNeed}
          onReload={loadData}
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
          <AssistanceNeedForm childId={id} onReload={loadData} />
          <div className="separator" />
        </>
      )}
      {duplicate && (
        <>
          <AssistanceNeedForm
            childId={id}
            assistanceNeed={duplicate}
            onReload={loadData}
          />
          <div className="separator" />
        </>
      )}
      {renderAssistanceNeeds()}
    </div>
  )
}

export default AssistanceNeed
