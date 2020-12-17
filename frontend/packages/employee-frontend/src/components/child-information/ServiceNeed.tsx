// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useRef } from 'react'
import { useTranslation } from '~/state/i18n'
import { Loading } from '~/api'
import { ChildContext } from '~/state/child'
import { faClock } from '@evaka/lib-icons'
import Loader from '@evaka/lib-components/src/atoms/Loader'
import { UUID } from '~/types'
import ServiceNeedRow from './service-need/ServiceNeedRow'
import ServiceNeedForm from '~components/child-information/service-need/ServiceNeedForm'
import { UIContext } from '~state/ui'
import { AddButtonRow } from '@evaka/lib-components/src/atoms/buttons/AddButton'
import { scrollToRef } from 'utils'
import { getServiceNeeds } from 'api/child/service-needs'
import { getPlacements } from 'api/child/placements'
import CollapsibleSection from 'components/shared/molecules/CollapsibleSection'
import { RequireRole } from 'utils/roles'

export interface Props {
  id: UUID
  open: boolean
}

const ServiceNeed = React.memo(function ServiceNeed({ id, open }: Props) {
  const { i18n } = useTranslation()
  const { serviceNeeds, setServiceNeeds, setPlacements } = useContext(
    ChildContext
  )
  const { uiMode, toggleUiMode } = useContext(UIContext)
  const refSectionTop = useRef(null)

  const loadData = () => {
    setServiceNeeds(Loading.of())
    void getServiceNeeds(id).then(setServiceNeeds)
  }

  // FIXME: ServiceNeed shouldn't know about placements' dependency on it
  const reload = () => {
    loadData()
    setPlacements(Loading.of())
    void getPlacements(id).then(setPlacements)
  }

  useEffect(loadData, [id, setServiceNeeds])

  function renderServiceNeeds() {
    if (serviceNeeds.isLoading) {
      return <Loader />
    } else if (serviceNeeds.isFailure) {
      return <div>{i18n.common.loadingFailed}</div>
    } else {
      return serviceNeeds.value.map((serviceNeed) => (
        <div key={serviceNeed.id} data-qa="service-need">
          <ServiceNeedRow serviceNeed={serviceNeed} onReload={reload} />
        </div>
      ))
    }
  }

  return (
    <div ref={refSectionTop}>
      <CollapsibleSection
        icon={faClock}
        title={i18n.childInformation.serviceNeed.title}
        startCollapsed={!open}
        dataQa="service-need-collapsible"
      >
        <RequireRole
          oneOf={[
            'SERVICE_WORKER',
            'UNIT_SUPERVISOR',
            'FINANCE_ADMIN',
            'ADMIN'
          ]}
        >
          <AddButtonRow
            text={i18n.childInformation.serviceNeed.create}
            onClick={() => {
              toggleUiMode('create-new-service-need')
              scrollToRef(refSectionTop)
            }}
            disabled={uiMode === 'create-new-service-need'}
            dataQa="btn-create-new-service-need"
          />
        </RequireRole>
        {uiMode === 'create-new-service-need' && (
          <>
            <ServiceNeedForm childId={id} onReload={reload} />
            <div className="separator" />
          </>
        )}
        {renderServiceNeeds()}
      </CollapsibleSection>
    </div>
  )
})

export default ServiceNeed
