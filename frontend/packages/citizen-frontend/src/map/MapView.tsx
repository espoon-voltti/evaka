import React, { useEffect, useState } from 'react'
import styled from 'styled-components'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import UnitSearchPanel from '~map/UnitSearchPanel'
import MapBox from '~map/MapBox'
import { Failure, Loading, Result, Success } from '@evaka/lib-common/src/api'
import { PublicUnit } from '@evaka/lib-common/src/api-types/units/PublicUnit'
import { client } from '~api-client'
import { JsonOf } from '@evaka/lib-common/src/json'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'
import AdaptiveFlex from '@evaka/lib-components/src/layout/AdaptiveFlex'
import { mapViewBreakpoint, MobileMode } from '~map/const'
import { headerHeight } from '~header/const'

async function fetchUnits(): Promise<Result<PublicUnit[]>> {
  return client
    .get<JsonOf<PublicUnit[]>>('/public/units/all')
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export default React.memo(function MapView() {
  const [unitsResult, setUnitsResult] = useState<Result<PublicUnit[]>>(
    Loading.of()
  )
  const [mobileMode, setMobileMode] = useState<MobileMode>('map')

  const loadUnits = useRestApi(fetchUnits, setUnitsResult)
  useEffect(loadUnits, [])

  return (
    <FullScreen>
      <FlexContainer
        className={`mobile-mode-${mobileMode}`}
        breakpoint={mapViewBreakpoint}
        horizontalSpacing="zero"
        verticalSpacing="zero"
      >
        <UnitSearchPanel
          unitsResult={unitsResult}
          mobileMode={mobileMode}
          setMobileMode={setMobileMode}
        />
        <MapContainer>
          <MapBox />
        </MapContainer>
      </FlexContainer>
    </FullScreen>
  )
})

const FullScreen = styled.div`
  position: absolute;
  top: ${headerHeight};
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  align-items: stretch;
`

const FlexContainer = styled(AdaptiveFlex)`
  margin-top: ${defaultMargins.L};
  margin-bottom: ${defaultMargins.m};
  align-items: stretch;

  width: 1520px;

  @media (max-width: 1900px) {
    width: 80%;
  }

  @media (max-width: 1700px) {
    width: 90%;
  }

  @media (max-width: 1500px) {
    width: 95%;
  }

  @media (max-width: 1200px) {
    width: 98%;
  }

  @media (max-width: ${mapViewBreakpoint}) {
    width: 100%;
    margin-top: 0;
    margin-bottom: 0;
    &.mobile-mode-map {
      .unit-list {
        display: none;
      }
    }
    &.mobile-mode-list {
      .map-box {
        display: none;
      }
    }
  }
`

const MapContainer = styled.div`
  min-width: 300px;
  flex-grow: 99;
`
