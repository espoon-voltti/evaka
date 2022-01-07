// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { Fragment, useMemo, useState } from 'react'
import styled from 'styled-components'
import Title from 'lib-components/atoms/Title'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { fontWeights } from 'lib-components/typography'
import colors from 'lib-customizations/common'
import { faChevronUp, fasExclamationTriangle } from 'lib-icons'

import StatusLabel from '../../components/common/StatusLabel'
import { useTranslation } from '../../state/i18n'
import {
  PlacementDraft,
  PlacementDraftPlacement
} from '../../types/placementdraft'
import { getStatusLabelByDateRange } from '../../utils/date'

interface PlacementsContainerProps {
  open: boolean
  placements: number
  hasOverlap: boolean
}

const PlacementsContainer = styled.div`
  max-height: ${(props: PlacementsContainerProps) =>
    props.open
      ? '0px'
      : `calc(${props.placements} * 30px + ${
          props.hasOverlap ? '120px' : '0px'
        })`};
  transform-origin: top;
  overflow: hidden;
  transition: ${(props: PlacementsContainerProps) =>
    props.open ? 'all 0.3s ease-out' : 'all 0.3s ease-out'};
`

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

  svg {
    color: ${colors.accents.warningOrange};
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
    font-weight: ${fontWeights.normal};
    font-size: 18px;
  }
`

const PlacementRow = styled.div`
  margin-bottom: 5px;
`

const Container = styled.section`
  margin-bottom: 75px;
`

function hasOverlaps(placementDraft: PlacementDraft) {
  return !!placementDraft.placements.find((placement) => placement.overlap)
}

interface Props {
  placementDraft: PlacementDraft
}

function Placements({ placementDraft }: Props) {
  const { i18n } = useTranslation()

  const [open, setOpen] = useState(false)
  const hasOverlap = useMemo(
    () => hasOverlaps(placementDraft),
    [placementDraft]
  )

  return (
    <Container>
      <Fragment>
        <ToggleHeader onClick={() => setOpen(!open)}>
          <Title size={4}>
            {i18n.placementDraft.currentPlacements}
            <FontAwesomeIcon
              icon={faChevronUp}
              className={open ? 'rotate' : ''}
            />
          </Title>
        </ToggleHeader>
      </Fragment>
      <PlacementsContainer
        open={open}
        placements={placementDraft.placements.length}
        hasOverlap={hasOverlap}
      >
        {placementDraft.placements.map((placement: PlacementDraftPlacement) => (
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
        ))}
        <AlertBox message={i18n.placementDraft.placementOverlapError} wide />
      </PlacementsContainer>
    </Container>
  )
}

export default Placements
