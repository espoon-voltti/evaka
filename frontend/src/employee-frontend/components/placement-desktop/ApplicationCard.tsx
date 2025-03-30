import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import { useDrag } from 'react-dnd'
import styled, { css } from 'styled-components'

import { PlacementApplication } from 'lib-common/generated/api-types/placementdesktop'
import { DaycareId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import PlacementCircle from 'lib-components/atoms/PlacementCircle'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Tooltip from 'lib-components/atoms/Tooltip'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Label, Strong } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import {
  applicationBasisColors,
  careTypeColors,
  colors
} from 'lib-customizations/common'
import {
  faArrowsUpDownLeftRight,
  faFile,
  faLockAlt,
  faLockOpenAlt
} from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { isPartDayPlacement } from '../../utils/placements'
import { AgeIndicatorChip } from '../common/AgeIndicatorChip'
import { placementTypeToCareTypeLabel } from '../common/CareTypeLabel'

import {
  DropResult,
  ItemTypes,
  OnClickApplication,
  OnDropApplication,
  OnLockPlacement,
  OnUnlockPlacement
} from './common'

const ApplicationCardWrapper = styled.div<{
  isDragging: boolean
  state: ApplicationPlacementState
  selected: boolean
}>`
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  border: 2px solid ${(p) => p.theme.colors.grayscale.g100};
  border-radius: 8px;
  padding: ${defaultMargins.xs};

  ${(p) =>
    p.selected
      ? css`
          outline: 4px solid ${(p) => p.theme.colors.main.m3};
        `
      : ''}
  ${(p) =>
    p.state === 'trial'
      ? css`
          background-color: #f2ffed;
        `
      : p.state === 'planned'
        ? css`
            background-color: #fff1f4;
          `
        : ''}

  cursor: pointer;
  ${(p) =>
    p.isDragging
      ? css`
          visibility: hidden;
        `
      : ''}
`

type ApplicationPlacementState = 'planned' | 'trial' | 'unplaced'

interface ApplicationCardProps {
  application: PlacementApplication
  daycareNames: Map<DaycareId, string>
  placementState: ApplicationPlacementState
  selected: boolean
  occupancyDate: LocalDate
  onDrop: OnDropApplication
  onClick: OnClickApplication
  onLockPlacement: OnLockPlacement
  onUnlockPlacement: OnUnlockPlacement
}

export const ApplicationCard = React.memo(function ApplicationCard(
  p: ApplicationCardProps
) {
  const {
    application: {
      id: applicationId,
      firstName,
      lastName,
      dateOfBirth,
      placementType,
      assistanceNeed,
      primaryPreference,
      otherPreferences,
      serviceStart,
      serviceEnd
    },
    daycareNames,
    placementState,
    selected,
    occupancyDate,
    onDrop,
    onClick,
    onLockPlacement,
    onUnlockPlacement
  } = p

  const { i18n } = useTranslation()

  const [{ isDragging }, dragSource, preview] = useDrag(
    () => ({
      type: ItemTypes.APPLICATION_CARD,
      item: { applicationId },
      end: (item, monitor) => {
        const dropResult = monitor.getDropResult<DropResult>()
        if (item && dropResult) {
          onDrop(item.applicationId, dropResult.daycareId)
        }
      },
      collect: (monitor) => ({
        isDragging: monitor.isDragging()
      })
    }),
    [onDrop]
  )

  const ageYears = occupancyDate.differenceInYears(dateOfBirth)

  return (
    <ApplicationCardWrapper
      ref={preview}
      isDragging={isDragging}
      state={placementState}
      selected={selected}
      onClick={() => onClick(applicationId)}
    >
      <FixedSpaceColumn spacing="zero">
        <FixedSpaceRow justifyContent="space-between" alignItems="center">
          <FixedSpaceRow alignItems="center" spacing="xs">
            <Tooltip
              tooltip={<div>Syntymäaika: {dateOfBirth.format()}</div>}
              position="right"
            >
              <AgeIndicatorChip age={ageYears} />
            </Tooltip>
            <Label>
              {lastName} {firstName}
            </Label>
          </FixedSpaceRow>
          <FixedSpaceRow spacing="xs">
            <a
              href={`/employee/applications/${applicationId}`}
              target="_blank"
              rel="noopener noreferrer"
            >
              <IconOnlyButton
                icon={faFile}
                aria-label="Avaa"
                onClick={() => undefined}
              />
            </a>
            {placementState === 'trial' && (
              <IconOnlyButton
                icon={faLockOpenAlt}
                onClick={() => onLockPlacement(applicationId)}
                aria-label="Lukitse"
              />
            )}
            {placementState === 'planned' && (
              <IconOnlyButton
                icon={faLockAlt}
                onClick={() => onUnlockPlacement(applicationId)}
                aria-label="Avaa lukitus"
              />
            )}
          </FixedSpaceRow>
        </FixedSpaceRow>
        {placementState !== 'planned' && (
          <div>
            <Tooltip
              tooltip={
                <ol>
                  <li>{daycareNames.get(primaryPreference)}</li>
                  {otherPreferences.map((unit, i) => (
                    <li key={i}>{daycareNames.get(unit)}</li>
                  ))}
                </ol>
              }
            >
              1. {daycareNames.get(primaryPreference)}
            </Tooltip>
          </div>
        )}
        <Gap size="xs" />
        <FixedSpaceRow
          justifyContent="space-between"
          alignItems="center"
          style={{
            minHeight: placementState === 'planned' ? undefined : '26px'
          }}
        >
          <FixedSpaceRow spacing="xs" alignItems="center">
            <PlacementCircle
              type={isPartDayPlacement(placementType) ? 'half' : 'full'}
              label={
                <FixedSpaceColumn spacing="xs">
                  <div>{i18n.placement.type[placementType]}</div>
                  <div>
                    {serviceStart} - {serviceEnd}
                  </div>
                </FixedSpaceColumn>
              }
              tooltipPosition="right"
              color={
                careTypeColors[placementTypeToCareTypeLabel(placementType)]
              }
            />

            {!!assistanceNeed && (
              <Tooltip
                tooltip={
                  <div>
                    <Strong>Tuentarve hakemuksella:</Strong>{' '}
                    <span>{assistanceNeed}</span>
                  </div>
                }
                position="right"
                width="large"
              >
                <RoundIcon
                  content="T"
                  color={applicationBasisColors.ASSISTANCE_NEED}
                  size="m"
                  active
                />
              </Tooltip>
            )}
          </FixedSpaceRow>

          {placementState !== 'planned' && (
            <div
              ref={dragSource}
              style={{ cursor: isDragging ? 'grabbing' : 'grab' }}
            >
              <FontAwesomeIcon
                icon={faArrowsUpDownLeftRight}
                color={colors.main.m2}
                style={{ fontSize: '24px' }}
                onClick={(e) => e.stopPropagation()}
              />
            </div>
          )}
        </FixedSpaceRow>
      </FixedSpaceColumn>
    </ApplicationCardWrapper>
  )
})
