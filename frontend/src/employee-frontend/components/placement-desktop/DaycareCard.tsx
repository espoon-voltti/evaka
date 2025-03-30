import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import { useDrop } from 'react-dnd'
import styled from 'styled-components'

import { combine, Result } from 'lib-common/api'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import {
  PlacementApplication,
  PlacementDaycare
} from 'lib-common/generated/api-types/placementdesktop'
import { ServiceNeedOption } from 'lib-common/generated/api-types/serviceneed'
import { ApplicationId, DaycareId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import Tooltip from 'lib-components/atoms/Tooltip'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Bold, fontWeights, H3, H4 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { fasExclamationTriangle } from 'lib-icons'

import { renderResult } from '../async-rendering'

import { ApplicationCard } from './ApplicationCard'
import {
  DropResult,
  ItemTypes,
  OnClickApplication,
  OnDropApplication,
  OnLockPlacement,
  OnUnlockPlacement
} from './common'

interface DaycareCardProps {
  daycare: PlacementDaycare
  daycareNames: Map<DaycareId, string>
  plannedApplications: Result<PlacementApplication[]>
  trialApplications: Result<PlacementApplication[]>
  preferenceNumber: number | null
  selectedApplicationId: ApplicationId | null
  occupancyDate: LocalDate
  serviceNeedDefaults: Map<PlacementType, ServiceNeedOption>
  onDropApplication: OnDropApplication
  onClickApplication: OnClickApplication
  onLockPlacement: OnLockPlacement
  onUnlockPlacement: OnUnlockPlacement
}

const DaycareCardWrapper = styled.div`
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  border: 2px solid ${(p) => p.theme.colors.grayscale.g100};
  border-radius: 8px;
  padding: ${defaultMargins.xs};
  flex-grow: 1;
  min-width: 40%;
`

const ApplicationsWrapper = styled.div<{
  isOver: boolean
}>`
  border: 2px dashed
    ${(p) => (p.isOver ? p.theme.colors.main.m3 : p.theme.colors.grayscale.g15)};
  border-radius: 4px;
  min-height: 30px;
  flex-grow: 1;
  padding: 8px;
`

const PreferenceNumber = styled.span`
  font-weight: ${fontWeights.bold};
  font-size: 24px;
`

const OccupancyPercentage = styled.span`
  font-weight: ${fontWeights.bold};
  font-size: 24px;
`

const getOccupancyPercentage = (children: number, capacity: number) =>
  `${Math.round((1000 * children) / capacity) / 10}%`

export const DaycareCard = React.memo(function DaycareCard(
  p: DaycareCardProps
) {
  const {
    daycare: {
      id: daycareId,
      name,
      careAreaName,
      serviceWorkerNote,
      capacity,
      childOccupancyConfirmed,
      childOccupancyPlanned
    },
    daycareNames,
    plannedApplications,
    trialApplications,
    preferenceNumber,
    selectedApplicationId,
    occupancyDate,
    serviceNeedDefaults,
    onDropApplication,
    onClickApplication,
    onLockPlacement,
    onUnlockPlacement
  } = p

  const [{ isOver }, dropTarget] = useDrop(() => ({
    accept: ItemTypes.APPLICATION_CARD,
    drop: (): DropResult => ({ daycareId }),
    collect: (monitor) => ({
      isOver: monitor.isOver()
    })
  }))

  const trialOccupancySum = React.useMemo(
    () =>
      trialApplications.map(
        (applications) =>
          applications.reduce((sum, a) => {
            const serviceNeedOption = serviceNeedDefaults.get(a.placementType)!
            const childAge = occupancyDate.differenceInYears(a.dateOfBirth)
            const coefficient =
              childAge < 3
                ? serviceNeedOption.occupancyCoefficientUnder3y
                : serviceNeedOption.occupancyCoefficient
            return sum + coefficient
          }, 0) + childOccupancyPlanned
      ),
    [
      trialApplications,
      childOccupancyPlanned,
      occupancyDate,
      serviceNeedDefaults
    ]
  )

  return (
    <DaycareCardWrapper ref={dropTarget}>
      <FixedSpaceColumn spacing="xs" style={{ height: '100%' }}>
        <FixedSpaceRow alignItems="baseline" justifyContent="space-between">
          <FixedSpaceRow alignItems="baseline" spacing="xs">
            {preferenceNumber !== null && (
              <PreferenceNumber>{preferenceNumber})</PreferenceNumber>
            )}
            <H3 noMargin>{name}</H3>
            {!!serviceWorkerNote && (
              <Tooltip tooltip={serviceWorkerNote}>
                <FontAwesomeIcon
                  icon={fasExclamationTriangle}
                  color={colors.status.warning}
                  style={{ fontSize: '20px' }}
                />
              </Tooltip>
            )}
          </FixedSpaceRow>

          <FixedSpaceRow alignItems="baseline" spacing="xs">
            {trialOccupancySum.isSuccess ? (
              <Tooltip
                tooltip={
                  <FixedSpaceColumn>
                    <div>
                      <Bold>Vahvistettu:</Bold>{' '}
                      <span>
                        {getOccupancyPercentage(
                          childOccupancyConfirmed,
                          capacity
                        )}
                      </span>
                    </div>
                    <div>
                      <Bold>Suunniteltu:</Bold>{' '}
                      <span>
                        {getOccupancyPercentage(
                          childOccupancyPlanned,
                          capacity
                        )}
                      </span>
                    </div>
                    <div>
                      <Bold>Kokeilu:</Bold>{' '}
                      <span>
                        {getOccupancyPercentage(
                          trialOccupancySum.value,
                          capacity
                        )}
                      </span>
                    </div>
                  </FixedSpaceColumn>
                }
              >
                <OccupancyPercentage>
                  {getOccupancyPercentage(trialOccupancySum.value, capacity)}
                </OccupancyPercentage>
              </Tooltip>
            ) : (
              <OccupancyPercentage>... %</OccupancyPercentage>
            )}
          </FixedSpaceRow>
        </FixedSpaceRow>

        <H4 noMargin>{careAreaName}</H4>
        <ApplicationsWrapper isOver={isOver}>
          {renderResult(
            combine(plannedApplications, trialApplications),
            ([plannedApplications, trialApplications]) => (
              <FixedSpaceColumn spacing="xs">
                {trialApplications.map((application) => (
                  <ApplicationCard
                    key={application.id}
                    application={application}
                    selected={selectedApplicationId === application.id}
                    daycareNames={daycareNames}
                    occupancyDate={occupancyDate}
                    onDrop={onDropApplication}
                    onClick={onClickApplication}
                    onLockPlacement={onLockPlacement}
                    onUnlockPlacement={onUnlockPlacement}
                    placementState="trial"
                  />
                ))}
                {plannedApplications.map((application) => (
                  <ApplicationCard
                    key={application.id}
                    application={application}
                    selected={selectedApplicationId === application.id}
                    daycareNames={daycareNames}
                    occupancyDate={occupancyDate}
                    onDrop={onDropApplication}
                    onClick={onClickApplication}
                    onLockPlacement={onLockPlacement}
                    onUnlockPlacement={onUnlockPlacement}
                    placementState="planned"
                  />
                ))}
              </FixedSpaceColumn>
            )
          )}
        </ApplicationsWrapper>
      </FixedSpaceColumn>
    </DaycareCardWrapper>
  )
})
