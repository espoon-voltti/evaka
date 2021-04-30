// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useRef, useState } from 'react'
import { useTranslation } from '../../state/i18n'
import { Loading } from 'lib-common/api'
import { ChildContext } from '../../state'
import Loader from 'lib-components/atoms/Loader'
import { UUID } from '../../types'
import ServiceNeedRow from './service-need/ServiceNeedRow'
import ServiceNeedForm from '../../components/child-information/service-need/ServiceNeedForm'
import { UIContext } from '../../state/ui'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { scrollToRef } from '../../utils'
import { getServiceNeeds } from '../../api/child/service-needs'
import { getPlacements } from '../../api/child/placements'
import { RequireRole } from '../../utils/roles'
import { CollapsibleContentArea } from '../../../lib-components/layout/Container'
import { H2 } from '../../../lib-components/typography'

export interface Props {
  id: UUID
  startOpen: boolean
}

const ServiceNeed = React.memo(function ServiceNeed({ id, startOpen }: Props) {
  const { i18n } = useTranslation()
  const { serviceNeeds, setServiceNeeds, setPlacements } = useContext(
    ChildContext
  )
  const { uiMode, toggleUiMode } = useContext(UIContext)
  const refSectionTop = useRef(null)

  const [open, setOpen] = useState(startOpen)

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
      <CollapsibleContentArea
        title={<H2 noMargin>{i18n.childInformation.serviceNeed.title}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
        data-qa="service-need-collapsible"
      >
        <RequireRole oneOf={['UNIT_SUPERVISOR', 'ADMIN']}>
          <AddButtonRow
            text={i18n.childInformation.serviceNeed.create}
            onClick={() => {
              toggleUiMode('create-new-service-need')
              scrollToRef(refSectionTop)
            }}
            disabled={uiMode === 'create-new-service-need'}
            data-qa="btn-create-new-service-need"
          />
        </RequireRole>
        {uiMode === 'create-new-service-need' && (
          <>
            <ServiceNeedForm childId={id} onReload={reload} />
            <div className="separator" />
          </>
        )}
        {renderServiceNeeds()}
      </CollapsibleContentArea>
    </div>
  )
})

export default ServiceNeed
