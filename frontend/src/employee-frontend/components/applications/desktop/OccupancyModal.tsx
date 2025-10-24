// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo } from 'react'
import styled from 'styled-components'

import type { PlacementDesktopDaycare } from 'lib-common/generated/api-types/application'
import type { OccupancyResponse } from 'lib-common/generated/api-types/occupancy'
import {
  FixedSpaceFlexWrap,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { ApplicationUIContext } from '../../../state/application-ui'
import { useTranslation } from '../../../state/i18n'
import { GraphWrapper } from '../../unit/tab-unit-information/occupancy/OccupanciesForDateRange'
import OccupancyGraph, {
  occupancyGraphColors
} from '../../unit/tab-unit-information/occupancy/OccupancyGraph'

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
      <StyledSubtitle>
        {i18n.applications.placementDesktop.occupancies}
      </StyledSubtitle>
      <FixedSpaceFlexWrap verticalSpacing="xs" horizontalSpacing="m">
        {(['confirmed', 'planned', 'draft'] as const).map((type) => (
          <FixedSpaceRow key={type} alignItems="center" spacing="xs">
            <LegendSquare color={occupancyGraphColors[type]} />
            <div>{i18n.applications.placementDesktop.occupancyTypes[type]}</div>
          </FixedSpaceRow>
        ))}
      </FixedSpaceFlexWrap>
      <Gap />
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

const StyledSubtitle = styled(H2)`
  margin-top: -32px;
  text-align: center;
`

const LegendSquare = styled.div<{ color: string }>`
  width: 16px;
  height: 16px;
  background-color: ${(p) => p.color};
  display: inline-block;
`
