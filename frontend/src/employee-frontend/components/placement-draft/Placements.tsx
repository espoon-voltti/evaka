// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'

import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { fontWeights, H2, InformationText } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { fasExclamationTriangle } from 'lib-icons'

import StatusLabel from '../../components/common/StatusLabel'
import { useTranslation } from '../../state/i18n'
import { getStatusLabelByDateRange } from '../../utils/date'

import { PlacementSummaryWithOverlaps } from './PlacementDraft'

const Type = styled.span`
  display: inline-block;
  width: 225px;
  font-weight: ${fontWeights.semibold};
`

const Name = styled.span`
  display: inline-block;
  width: 285px;
`

const Dates = styled.span`
  display: inline-block;
  width: 220px;
`

const StatusWrapper = styled.span`
  display: inline-flex;
  width: 90px;
  justify-content: flex-end;
`

const PlacementRow = styled.div`
  margin-bottom: 5px;
`

interface Props {
  placements: PlacementSummaryWithOverlaps[]
}

export default React.memo(function Placements({ placements }: Props) {
  const { i18n } = useTranslation()
  const hasOverlap = placements.some((p) => p.overlap)
  return (
    <section>
      <H2 noMargin>{i18n.placementDraft.currentPlacements}</H2>
      <Gap size="s" />
      {placements.length === 0 && (
        <InformationText>
          {i18n.placementDraft.noCurrentPlacements}
        </InformationText>
      )}
      {placements.map((placement) => (
        <PlacementRow key={placement.id}>
          <Type>{i18n.placement.type[placement.type]}</Type>
          <Name>{placement.unit.name}</Name>
          <Dates>
            {placement.startDate.format()}-{placement.endDate.format()}
            {placement.overlap && (
              <>
                <Gap size="xxs" horizontal />
                <FontAwesomeIcon
                  icon={fasExclamationTriangle}
                  color={colors.status.warning}
                />
              </>
            )}
          </Dates>
          <StatusWrapper>
            <StatusLabel status={getStatusLabelByDateRange(placement)} />
          </StatusWrapper>
        </PlacementRow>
      ))}
      {hasOverlap && (
        <AlertBox message={i18n.placementDraft.placementOverlapError} wide />
      )}
    </section>
  )
})
