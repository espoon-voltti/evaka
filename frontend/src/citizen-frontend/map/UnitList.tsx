// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useEffect, useState } from 'react'
import styled from 'styled-components'

import { Result } from 'lib-common/api'
import { PublicUnit } from 'lib-common/generated/api-types/daycare'
import { Button } from 'lib-components/atoms/buttons/Button'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import { ContentArea } from 'lib-components/layout/Container'
import { fontWeights, H3, H4 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faAngleDown } from 'lib-icons'

import { useTranslation } from '../localization'

import { MapAddress } from './MapView'
import UnitListItem from './UnitListItem'
import { formatDistance, UnitWithDistance } from './distances'

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
    ? sortBy(
        unitsWithDistances.value.filter((u) => u.drivingDistance !== null),
        (u) => u.drivingDistance
      ).slice(0, 10)
    : []
  const otherUnits = unitsWithDistances.isSuccess
    ? sortBy(
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
                  <Button
                    appearance="inline"
                    data-qa="toggle-show-more-units"
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
  flex-grow: 1;
`

const Info = styled.div`
  font-size: 14px;
  font-weight: ${fontWeights.semibold};
  color: ${colors.grayscale.g35};
`

const Centered = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
`
