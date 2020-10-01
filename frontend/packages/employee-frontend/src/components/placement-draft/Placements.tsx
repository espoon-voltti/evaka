// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useState } from 'react'
import styled from 'styled-components'
import { Title } from '~components/shared/alpha'
import { faChevronUp } from '@evaka/icons'
import { fasExclamationTriangle } from '@evaka/icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'

import { useTranslation } from '~state/i18n'
import { isSuccess } from '~api'
import { PlacementDraftPlacement } from '~types/placementdraft'
import {
  PlacementDraftState,
  PlacementDraftContext
} from '~state/placementdraft'
import { EspooColours } from '~utils/colours'
import { getStatusLabelByDateRange } from '~utils/date'
import StatusLabel from '~components/common/StatusLabel'
import { AlertBox } from '~components/common/MessageBoxes'

interface PlacementsContainerProps {
  open: boolean
  placements: number
  hasOverlap: () => boolean
}

const PlacementsContainer = styled.div`
  max-height: ${(props: PlacementsContainerProps) =>
    props.open
      ? '0px'
      : `calc(${props.placements} * 30px + ${
          props.hasOverlap() ? '111px' : '0px'
        })`};
  transform-origin: top;
  overflow: hidden;
  transition: ${(props: PlacementsContainerProps) =>
    props.open ? 'all 0.3s ease-out' : 'all 0.3s ease-out'};
`

const Type = styled.span`
  display: inline-block;
  width: 225px;
  font-weight: 600;
`

const Name = styled.span`
  display: inline-block;
  width: 285px;
`

const Dates = styled.span`
  display: inline-block;
  width: 220px;

  svg {
    color: ${EspooColours.orange};
    margin-left: 5px;
  }
`

const StatusWrapper = styled.span`
  display: inline-flex;
  width: 90px;
  justify-content: flex-end;
`

const ToggleHeader = styled.div`
  margin-bottom: 26px;
  cursor: pointer;

  svg {
    transition: all 0.2s ease-out;
    transform: rotate(0deg);
    margin-left: 10px;
  }

  svg.rotate {
    transform: rotate(180deg);
    transition: all 0.2s ease-out;
  }

  h2.is-4 {
    font-weight: normal;
    font-size: 18px;
  }
`

const PlacementRow = styled.div`
  margin-bottom: 5px;
`

const Container = styled.section`
  margin-bottom: 75px;
`

function Placements() {
  const { i18n } = useTranslation()

  const [open, setOpen] = useState(false)

  const { placementDraft } = useContext<PlacementDraftState>(
    PlacementDraftContext
  )

  function hasOverlaps() {
    if (isSuccess(placementDraft)) {
      return !!placementDraft.data.placements.find(
        (placement) => placement.overlap
      )
    }
    return false
  }

  return (
    <Container>
      {isSuccess(placementDraft) && (
        <Fragment>
          <ToggleHeader onClick={() => setOpen(!open)}>
            <Title size={4} tag={2}>
              {i18n.placementDraft.currentPlacements}
              <FontAwesomeIcon
                icon={faChevronUp}
                className={open ? 'rotate' : ''}
              />
            </Title>
          </ToggleHeader>
        </Fragment>
      )}
      {isSuccess(placementDraft) && (
        <PlacementsContainer
          open={open}
          placements={placementDraft.data.placements.length}
          hasOverlap={hasOverlaps}
        >
          {placementDraft.data.placements.map(
            (placement: PlacementDraftPlacement) => (
              <PlacementRow key={placement.id}>
                <Type>{i18n.common.types[placement.type]}</Type>
                <Name>{placement.unit.name}</Name>
                <Dates>
                  {placement.startDate.format()}-{placement.endDate.format()}
                  {placement.overlap && (
                    <FontAwesomeIcon icon={fasExclamationTriangle} />
                  )}
                </Dates>
                <StatusWrapper>
                  <StatusLabel status={getStatusLabelByDateRange(placement)} />
                </StatusWrapper>
              </PlacementRow>
            )
          )}
          <AlertBox message={i18n.placementDraft.placementOverlapError} wide />
        </PlacementsContainer>
      )}
    </Container>
  )
}

export default Placements
