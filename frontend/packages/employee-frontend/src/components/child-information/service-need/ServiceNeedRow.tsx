// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useRef, useState } from 'react'
import { useTranslation } from '~/state/i18n'
import { ServiceNeed } from '~types/child'
import { UIContext } from '~state/ui'
import ServiceNeedForm from '~components/child-information/service-need/ServiceNeedForm'
import { faQuestion } from '@evaka/icons'
import ToolbarAccordion from '~components/common/ToolbarAccordion'
import { formatDate, isActiveDateRange } from '~/utils/date'
import InfoModal from '~components/common/InfoModal'
import Toolbar from 'components/shared/molecules/Toolbar'
import LabelValueList from '~components/common/LabelValueList'
import { capitalizeFirstLetter, scrollToRef } from 'utils'
import { removeServiceNeed } from 'api/child/service-needs'
import { ALL_ROLES_BUT_STAFF } from 'utils/roles'

export interface Props {
  serviceNeed: ServiceNeed
  onReload: () => undefined | void
}

function ServiceNeedRow({ serviceNeed, onReload }: Props) {
  const { i18n } = useTranslation()
  const expandedAtStart = isActiveDateRange(
    serviceNeed.startDate,
    serviceNeed.endDate
  )
  const [toggled, setToggled] = useState(expandedAtStart)
  const { uiMode, toggleUiMode, clearUiMode } = useContext(UIContext)
  const refForm = useRef(null)

  const serviceNeedDetails = [
    serviceNeed.partDay &&
      i18n.childInformation.serviceNeed.services.partDayShort,
    serviceNeed.partWeek &&
      i18n.childInformation.serviceNeed.services.partWeekShort,
    serviceNeed.temporary &&
      i18n.childInformation.serviceNeed.services.temporary,
    serviceNeed.shiftCare &&
      i18n.childInformation.serviceNeed.services.shiftCareShort
  ].filter(Boolean)
  let serviceNeedDetailsCsv = capitalizeFirstLetter(
    serviceNeedDetails.join(', ').toLowerCase()
  )
  if (serviceNeedDetailsCsv.length > 0) serviceNeedDetailsCsv += '.'

  return (
    <div className="service-need-view-wrapper">
      {uiMode === `remove-service-need-${serviceNeed.id}` && (
        <InfoModal
          iconColour={'orange'}
          title={i18n.childInformation.serviceNeed.removeServiceNeed}
          text={`${i18n.common.period} ${serviceNeed.startDate.format()} - ${
            serviceNeed.endDate ? serviceNeed.endDate.format() : ''
          }`}
          resolveLabel={i18n.common.remove}
          rejectLabel={i18n.common.cancel}
          icon={faQuestion}
          reject={() => clearUiMode()}
          resolve={() =>
            removeServiceNeed(serviceNeed.id).then(() => {
              clearUiMode()
              onReload()
            })
          }
        />
      )}

      <ToolbarAccordion
        title={`${
          i18n.childInformation.serviceNeed.dateRange
        } ${serviceNeed.startDate.format()} - ${
          serviceNeed.endDate ? serviceNeed.endDate.format() : ''
        }`}
        onToggle={() => setToggled((prev) => !prev)}
        open={toggled}
        toolbar={
          <Toolbar
            dateRange={serviceNeed}
            onEdit={() => {
              toggleUiMode(`edit-service-need-${serviceNeed.id}`)
              setToggled(true)
              scrollToRef(refForm)
            }}
            editableFor={ALL_ROLES_BUT_STAFF}
            dataQaEdit="btn-edit-service-need"
            onDelete={() =>
              toggleUiMode(`remove-service-need-${serviceNeed.id}`)
            }
            deletableFor={ALL_ROLES_BUT_STAFF}
            dataQaDelete="btn-remove-service-need"
            disableAll={!!uiMode && uiMode.startsWith('edit-service-need')}
          />
        }
      >
        {uiMode === `edit-service-need-${serviceNeed.id}` ? (
          <div ref={refForm}>
            <ServiceNeedForm serviceNeed={serviceNeed} onReload={onReload} />
          </div>
        ) : (
          <LabelValueList
            spacing="large"
            contents={[
              {
                label: i18n.childInformation.serviceNeed.hoursPerWeek,
                value: (
                  <>
                    <span data-qa="service-need-value">
                      {serviceNeed.hoursPerWeek}
                    </span>
                    <span> </span>
                    <span>
                      {i18n.childInformation.serviceNeed.hoursInWeek}.{' '}
                      {serviceNeedDetailsCsv}
                    </span>
                  </>
                )
              },
              {
                label: i18n.childInformation.serviceNeed.createdByName,
                value: `${serviceNeed.updatedByName}, ${formatDate(
                  serviceNeed.updated
                )}`
              }
            ]}
          />
        )}
      </ToolbarAccordion>
    </div>
  )
}

export default ServiceNeedRow
