import sortBy from 'lodash/sortBy'
import React from 'react'
import { useDrop } from 'react-dnd'
import styled from 'styled-components'

import { Result } from 'lib-common/api'
import { PlacementApplication } from 'lib-common/generated/api-types/placementdesktop'
import { ApplicationId, DaycareId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'

import { renderResult } from '../async-rendering'

import { ApplicationCard } from './ApplicationCard'
import {
  DropResult,
  ItemTypes,
  OnClickApplication,
  OnDropApplication
} from './common'

const Wrapper = styled.div<{
  isOver: boolean
}>`
  border: 2px dashed
    ${(p) => (p.isOver ? p.theme.colors.main.m3 : p.theme.colors.grayscale.g15)};
  border-radius: 4px;
  flex-grow: 1;
  padding: 8px;
`

interface Props {
  applications: Result<PlacementApplication[]>
  daycareNames: Map<DaycareId, string>
  selectedApplicationId: ApplicationId | null
  occupancyDate: LocalDate
  onDropApplication: OnDropApplication
  onClickApplication: OnClickApplication
}

export const UnplacedApplicationsList = React.memo(
  function UnplacedApplicationsList(p: Props) {
    const {
      applications,
      daycareNames,
      selectedApplicationId,
      occupancyDate,
      onDropApplication,
      onClickApplication
    } = p

    const [{ isOver }, dropTarget] = useDrop(() => ({
      accept: ItemTypes.APPLICATION_CARD,
      drop: (): DropResult => ({ daycareId: null }),
      collect: (monitor) => ({
        isOver: monitor.isOver()
      })
    }))

    const sortedApplications = React.useMemo(
      () =>
        applications.map((applications) =>
          sortBy(
            applications,
            (a) => a.lastName,
            (a) => a.firstName,
            (a) => a.id
          )
        ),
      [applications]
    )

    return (
      <Wrapper ref={dropTarget} isOver={isOver}>
        {renderResult(sortedApplications, (sortedApplications) => (
          <FixedSpaceColumn spacing="xs" style={{ minHeight: '30vh' }}>
            {sortedApplications.map((application) => (
              <ApplicationCard
                key={application.id}
                application={application}
                placementState="unplaced"
                selected={selectedApplicationId === application.id}
                daycareNames={daycareNames}
                occupancyDate={occupancyDate}
                onDrop={onDropApplication}
                onClick={onClickApplication}
                onLockPlacement={() => undefined}
                onUnlockPlacement={() => undefined}
              />
            ))}
          </FixedSpaceColumn>
        ))}
      </Wrapper>
    )
  }
)
