// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import orderBy from 'lodash/orderBy'
import type { RefObject } from 'react'
import React, { useContext, useMemo } from 'react'
import styled, { css, keyframes, useTheme } from 'styled-components'

import { combine } from 'lib-common/api'
import { useBoolean } from 'lib-common/form/hooks'
import type {
  ApplicationSummary,
  PreferredUnit
} from 'lib-common/generated/api-types/application'
import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Tooltip from 'lib-components/atoms/Tooltip'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H3, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faTimes } from 'lib-icons'
import { faLineChart } from 'lib-icons'
import { fasExclamation } from 'lib-icons'
import { fasExclamationTriangle } from 'lib-icons'

import { ApplicationUIContext } from '../../../state/application-ui'
import { useTranslation } from '../../../state/i18n'
import { renderResult } from '../../async-rendering'
import { getPlacementDesktopDaycareQuery } from '../queries'

import DraftPlacementRow from './DraftPlacementRow'
import OccupancyModal from './OccupancyModal'

export default React.memo(function DaycareCard({
  daycare,
  highlighted,
  applications,
  onDeleteApplicationPlacementSuccess,
  onMutateApplicationPlacementFailure,
  onRemoveFromShownDaycares,
  ref
}: {
  daycare: PreferredUnit
  highlighted: boolean
  applications: ApplicationSummary[]
  onDeleteApplicationPlacementSuccess: (applicationId: ApplicationId) => void
  onMutateApplicationPlacementFailure: () => void
  onRemoveFromShownDaycares: () => void
  ref: RefObject<HTMLDivElement | null> | undefined
}) {
  const { i18n } = useTranslation()
  const { colors } = useTheme()
  const { occupancyPeriodStart } = useContext(ApplicationUIContext)
  const unitDetails = useQueryResult(
    getPlacementDesktopDaycareQuery({
      unitId: daycare.id,
      occupancyStart: occupancyPeriodStart
    })
  )

  const placementDraftsWithApplications = useMemo(
    () =>
      unitDetails.map((unitDetails) =>
        unitDetails.placementDrafts.map((placementDraft) => {
          const application = applications.find(
            (a) => a.id === placementDraft.applicationId
          )
          return {
            placementDraft,
            application
          }
        })
      ),
    [unitDetails, applications]
  )

  const [isGraphOpen, { on: openGraph, off: closeGraph }] = useBoolean(false)

  const [placementDraftsOpen, { toggle: togglePlacementDraftsOpen }] =
    useBoolean(true)

  return (
    <Card ref={ref} $highlighted={highlighted}>
      {isGraphOpen && unitDetails.isSuccess && (
        <OccupancyModal daycare={unitDetails.value} onClose={closeGraph} />
      )}
      <FixedSpaceColumn spacing="s">
        <FixedSpaceRow justifyContent="space-between">
          <FixedSpaceRow>
            <a
              href={`/employee/units/${daycare.id}/groups?period=${encodeURIComponent('3 months')}&startDate=${occupancyPeriodStart.formatIso()}`}
              target="_blank"
              rel="noreferrer"
            >
              <H3 noMargin style={{ color: colors.main.m1 }}>
                {daycare.name}
              </H3>
            </a>
            {unitDetails.isSuccess && !!unitDetails.value.serviceWorkerNote && (
              <Tooltip
                tooltip={unitDetails.value.serviceWorkerNote}
                width="large"
              >
                <RoundIcon
                  content={fasExclamation}
                  color={colors.status.warning}
                  textColor={colors.grayscale.g0}
                  size="m"
                />
              </Tooltip>
            )}
          </FixedSpaceRow>
          <Tooltip
            tooltip={i18n.applications.placementDesktop.hideUnit}
            delayed
          >
            <IconOnlyButton
              icon={faTimes}
              aria-label={i18n.applications.placementDesktop.hideUnit}
              onClick={onRemoveFromShownDaycares}
            />
          </Tooltip>
        </FixedSpaceRow>

        {renderResult(
          combine(unitDetails, placementDraftsWithApplications),
          ([unitDetails, placementDraftsWithApplications]) => (
            <>
              <OccupanciesArea>
                <FixedSpaceRow justifyContent="space-between">
                  <FixedSpaceRow>
                    <FixedSpaceColumn spacing="xs">
                      <OccupancyLabel>
                        {
                          i18n.applications.placementDesktop.occupancyTypes
                            .confirmed
                        }
                      </OccupancyLabel>
                      <OccupancyPercentage>
                        {unitDetails.occupancyConfirmed?.max?.percentage ??
                          '??'}{' '}
                        %
                      </OccupancyPercentage>
                    </FixedSpaceColumn>
                    <FixedSpaceColumn spacing="xs">
                      <OccupancyLabel>
                        {
                          i18n.applications.placementDesktop.occupancyTypes
                            .planned
                        }
                      </OccupancyLabel>
                      <OccupancyPercentage>
                        {unitDetails.occupancyPlanned?.max?.percentage ?? '??'}{' '}
                        %
                      </OccupancyPercentage>
                    </FixedSpaceColumn>
                    <FixedSpaceColumn spacing="xs">
                      <OccupancyLabel>
                        {
                          i18n.applications.placementDesktop.occupancyTypes
                            .draft
                        }
                      </OccupancyLabel>
                      {(unitDetails.occupancyDraft?.max?.percentage ?? 0) >=
                      105 ? (
                        <OccupancyWarning>
                          <FixedSpaceRow alignItems="center" spacing="xs">
                            <OccupancyPercentageDraft>
                              {unitDetails.occupancyDraft?.max?.percentage ??
                                '??'}{' '}
                              %
                            </OccupancyPercentageDraft>
                            <FontAwesomeIcon
                              icon={fasExclamationTriangle}
                              color={colors.status.danger}
                            />
                          </FixedSpaceRow>
                        </OccupancyWarning>
                      ) : (
                        <OccupancyPercentageDraft>
                          {unitDetails.occupancyDraft?.max?.percentage ?? '??'}{' '}
                          %
                        </OccupancyPercentageDraft>
                      )}
                    </FixedSpaceColumn>
                  </FixedSpaceRow>
                  <Tooltip
                    tooltip={i18n.applications.placementDesktop.openGraph}
                    delayed
                  >
                    <IconOnlyButton
                      icon={faLineChart}
                      aria-label={i18n.applications.placementDesktop.openGraph}
                      onClick={openGraph}
                    />
                  </Tooltip>
                </FixedSpaceRow>
              </OccupanciesArea>
              <Gap size="s" />
              <CollapsibleContentArea
                opaque
                open={placementDraftsOpen}
                toggleOpen={togglePlacementDraftsOpen}
                title={
                  <PlacementDraftsLabel>
                    {i18n.applications.placementDesktop.placementDrafts} (
                    {placementDraftsWithApplications.length})
                  </PlacementDraftsLabel>
                }
                slim
                paddingHorizontal="zero"
                paddingVertical="zero"
              >
                <Table
                  style={{
                    width: '100%',
                    maxWidth: '100%',
                    tableLayout: 'fixed'
                  }}
                >
                  <Tbody>
                    {orderBy(
                      placementDraftsWithApplications,
                      (pd) => pd.placementDraft.childName
                    ).map(({ placementDraft, application }) => (
                      <DraftPlacementRow
                        key={placementDraft.applicationId}
                        placementDraft={placementDraft}
                        application={application}
                        onDeleteApplicationPlacementSuccess={
                          onDeleteApplicationPlacementSuccess
                        }
                        onMutateApplicationPlacementFailure={
                          onMutateApplicationPlacementFailure
                        }
                      />
                    ))}
                  </Tbody>
                </Table>
              </CollapsibleContentArea>
            </>
          )
        )}
      </FixedSpaceColumn>
    </Card>
  )
})

const Card = styled.div<{ $highlighted: boolean }>`
  min-width: 400px;
  border: 1px solid ${(p) => p.theme.colors.grayscale.g15};
  border-radius: 4px;
  padding: ${defaultMargins.s};
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  ${(p) =>
    p.$highlighted
      ? css`
          animation: ${blinkShadow} 1s ease-in-out 1;
        `
      : ''}
`

const blinkShadow = keyframes`
  0% {
    box-shadow: 0 0 0 0 rgba(77, 127, 204, 0);
  }
  30% {
    box-shadow: 0 0 0 4px rgba(77, 127, 204, 0.8);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(77, 127, 204, 0);
  }
`

const OccupanciesArea = styled.div`
  background-color: ${(p) => p.theme.colors.grayscale.g4};
  padding: ${defaultMargins.xs};
`

const OccupancyLabel = styled(Label)`
  font-size: 14px;
  font-weight: 600;
  color: ${(p) => p.theme.colors.grayscale.g70};
`

const OccupancyPercentage = styled.span`
  font-size: 16px;
  font-weight: 400;
`

const OccupancyPercentageDraft = styled(OccupancyPercentage)`
  font-weight: 600;
`

const OccupancyWarning = styled.div`
  background-color: #ffe5e6;
  border: 1px solid ${(p) => p.theme.colors.status.danger};
  border-radius: 4px;
  padding: 2px ${defaultMargins.xs};
  margin-top: -2px;
`

const PlacementDraftsLabel = styled(Label)`
  font-size: 14px;
  font-weight: 600;
  color: ${(p) => p.theme.colors.grayscale.g70};
`
