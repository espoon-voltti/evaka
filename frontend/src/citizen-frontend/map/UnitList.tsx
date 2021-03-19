// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import styled from 'styled-components'
import _ from 'lodash'
import { Result } from 'lib-common/api'
import { PublicUnit } from 'lib-common/api-types/units/PublicUnit'
import colors from 'lib-components/colors'
import { H3, H4 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { ContentArea } from 'lib-components/layout/Container'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { faAngleDown } from 'lib-icons'
import { useTranslation } from '../localization'
import UnitListItem from '../map/UnitListItem'
import { formatDistance, UnitWithDistance } from '../map/distances'
import { MapAddress } from '../map/MapView'

type Props = {
  selectedAddress: MapAddress | null
  filteredUnits: Result<PublicUnit[]>
  unitsWithDistances: Result<UnitWithDistance[]>
  setSelectedUnit: (u: PublicUnit) => void
}

export default React.memo(function UnitList({
  selectedAddress,
  filteredUnits,
  unitsWithDistances,
  setSelectedUnit
}: Props) {
  const t = useTranslation()

  const [showMoreUnits, setShowMoreUnits] = useState<boolean>(false)
  useEffect(() => setShowMoreUnits(false), [selectedAddress])

  const accurateUnits = unitsWithDistances.isSuccess
    ? _.sortBy(
        unitsWithDistances.value.filter((u) => u.drivingDistance !== null),
        (u) => u.drivingDistance
      ).slice(0, 10)
    : []
  const otherUnits = unitsWithDistances.isSuccess
    ? _.sortBy(
        unitsWithDistances.value.filter(
          (u) => !accurateUnits.find((u2) => u2.id === u.id)
        ),
        (u) => u.straightDistance
      )
    : []

  if (filteredUnits.isSuccess && filteredUnits.value.length === 0) {
    return (
      <Wrapper opaque className="unit-list">
        <span>{t.map.noResults}</span>
      </Wrapper>
    )
  }

  if (selectedAddress && !unitsWithDistances.isFailure) {
    return (
      <Wrapper opaque className="unit-list">
        {unitsWithDistances.isLoading && <SpinnerSegment />}
        {unitsWithDistances.isSuccess && (
          <>
            <H3 noMargin>{t.map.nearestUnits}</H3>
            <Gap size="xxs" />
            <Info>{t.map.distanceWalking}</Info>
            <Gap size="s" />
            {accurateUnits.map((unit) => (
              <UnitListItem
                key={unit.id}
                unit={unit}
                distance={formatDistance(unit.drivingDistance)}
                onClick={() => setSelectedUnit(unit)}
              />
            ))}
            {showMoreUnits ? (
              <>
                <Gap size="s" />
                <H4 noMargin>{t.map.moreUnits}</H4>
                <Gap size="s" />
                {otherUnits.map((unit) => (
                  <UnitListItem
                    key={unit.id}
                    unit={unit}
                    distance={null}
                    onClick={() => setSelectedUnit(unit)}
                  />
                ))}
              </>
            ) : (
              <>
                <Gap size="s" />

                <Centered>
                  <InlineButton
                    onClick={() => setShowMoreUnits(true)}
                    text={t.map.showMore}
                    icon={faAngleDown}
                  />
                </Centered>
              </>
            )}
          </>
        )}
      </Wrapper>
    )
  } else {
    return (
      <Wrapper opaque className="unit-list">
        {filteredUnits.isLoading && <SpinnerSegment />}
        {filteredUnits.isFailure && (
          <ErrorSegment title={t.common.errors.genericGetError} />
        )}
        {filteredUnits.isSuccess &&
          filteredUnits.value.map((unit) => (
            <UnitListItem
              key={unit.id}
              unit={unit}
              distance={null}
              onClick={() => setSelectedUnit(unit)}
            />
          ))}
      </Wrapper>
    )
  }
})

const Wrapper = styled(ContentArea)`
  box-sizing: border-box;
  width: 100%;
  overflow-y: auto;
  flex-grow: 1;
  padding-right: ${defaultMargins.s};
`

const Info = styled.div`
  font-size: 14px;
  font-weight: 600;
  color: ${colors.greyscale.medium};
`

const Centered = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
`
