// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo } from 'react'

import type { PlacementDesktopDaycare } from 'lib-common/generated/api-types/application'
import type { OccupancyResponse } from 'lib-common/generated/api-types/occupancy'
import InfoModal from 'lib-components/molecules/modals/InfoModal'

import { ApplicationUIContext } from '../../../state/application-ui'
import { useTranslation } from '../../../state/i18n'
import { GraphWrapper } from '../../unit/tab-unit-information/occupancy/OccupanciesForDateRange'
import OccupancyGraph from '../../unit/tab-unit-information/occupancy/OccupancyGraph'

export default React.memo(function OccupancyModal({
  daycare,
  onClose
}: {
  daycare: PlacementDesktopDaycare
  onClose: () => void
}) {
  const { i18n } = useTranslation()
  const { occupancyPeriodStart } = useContext(ApplicationUIContext)
  const occupancyPeriodEnd = useMemo(
    () => occupancyPeriodStart.addMonths(3),
    [occupancyPeriodStart]
  )

  return (
    <InfoModal
      title={daycare.name}
      close={onClose}
      closeLabel={i18n.common.close}
      resolve={{
        label: i18n.common.close,
        action: onClose
      }}
    >
      <GraphWrapper>
        <OccupancyGraph
          occupancies={daycare.occupancyConfirmed ?? emptyOccupancies}
          plannedOccupancies={daycare.occupancyPlanned ?? emptyOccupancies}
          draftOccupancies={daycare.occupancyDraft ?? emptyOccupancies}
          realizedOccupancies={{
            min: null,
            max: null,
            occupancies: []
          }}
          confirmed={true}
          planned={true}
          draft={true}
          realized={false}
          startDate={occupancyPeriodStart.toSystemTzDate()}
          endDate={occupancyPeriodEnd.toSystemTzDate()}
        />
      </GraphWrapper>
    </InfoModal>
  )
})

const emptyOccupancies: OccupancyResponse = {
  min: null,
  max: null,
  occupancies: []
}
