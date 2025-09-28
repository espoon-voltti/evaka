// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled, { css, useTheme } from 'styled-components'
import { useLocation } from 'wouter'

import type {
  ApplicationSummary,
  PreferredUnit
} from 'lib-common/generated/api-types/application'
import type { UnitStub } from 'lib-common/generated/api-types/daycare'
import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import { useMutationResult } from 'lib-common/query'
import PlacementCircle from 'lib-components/atoms/PlacementCircle'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Tooltip from 'lib-components/atoms/Tooltip'
import { Button } from 'lib-components/atoms/buttons/Button'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H4, LabelLike } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faEye } from 'lib-icons'
import {
  faArrowLeft,
  faCommentAlt,
  faFile,
  faPlay,
  fasCommentAltLines,
  faSection
} from 'lib-icons'
import { faUndo } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'
import { isPartDayPlacement } from '../../../utils/placements'
import { CareTypeChip } from '../../common/CareTypeLabel'
import {
  BasisFragment,
  DateOfBirthInfo,
  ServiceWorkerNoteModal
} from '../ApplicationsList'
import { updateApplicationPlacementDraftMutation } from '../queries'

export default React.memo(function ApplicationCard({
  application,
  shownDaycares,
  allUnits,
  onUpdateApplicationPlacementSuccess,
  onUpdateApplicationPlacementFailure,
  onAddToShownDaycares
}: {
  application: ApplicationSummary
  shownDaycares: PreferredUnit[]
  allUnits: UnitStub[]
  onUpdateApplicationPlacementSuccess: (
    applicationId: ApplicationId,
    unit: PreferredUnit | null
  ) => void
  onUpdateApplicationPlacementFailure: () => void
  onAddToShownDaycares: (unit: PreferredUnit) => void
}) {
  const { i18n } = useTranslation()
  const { colors } = useTheme()
  const [, navigate] = useLocation()

  const { mutateAsync: updateApplicationPlacementDraft, isPending } =
    useMutationResult(updateApplicationPlacementDraftMutation)

  const [editingNote, setEditingNote] = React.useState(false)

  return (
    <Card>
      {editingNote && (
        <ServiceWorkerNoteModal
          application={application}
          onClose={() => setEditingNote(false)}
        />
      )}
      <FixedSpaceColumn spacing="s">
        <FixedSpaceRow alignItems="flex-start" justifyContent="space-between">
          <FixedSpaceRow alignItems="center" spacing="xs">
            <PlacementCircle
              type={
                isPartDayPlacement(application.placementType) ? 'half' : 'full'
              }
              label={
                application.serviceNeed !== null
                  ? application.serviceNeed.nameFi
                  : i18n.placement.type[application.placementType]
              }
            />
            <H4 noMargin>
              {application.lastName} {application.firstName}
            </H4>
          </FixedSpaceRow>
          <FixedSpaceRow spacing="L" alignItems="center">
            <CareTypeChip type={application.placementType} />
            <FixedSpaceRow spacing="xs" alignItems="center">
              <Tooltip
                tooltip={
                  application.serviceWorkerNote ? (
                    <span>{application.serviceWorkerNote}</span>
                  ) : (
                    <i>{i18n.applications.list.addNote}</i>
                  )
                }
              >
                <IconOnlyButton
                  icon={
                    application.serviceWorkerNote
                      ? fasCommentAltLines
                      : faCommentAlt
                  }
                  onClick={(e) => {
                    e.stopPropagation()
                    setEditingNote(true)
                  }}
                  aria-label={
                    application.serviceWorkerNote
                      ? i18n.common.edit
                      : i18n.applications.list.addNote
                  }
                  data-qa="service-worker-note"
                />
              </Tooltip>
              <a
                href={`/employee/applications/${application.id}`}
                target="_blank"
                rel="noreferrer"
              >
                <IconOnlyButton icon={faFile} aria-label={i18n.common.open} />
              </a>
            </FixedSpaceRow>
          </FixedSpaceRow>
        </FixedSpaceRow>
        <FixedSpaceRow>
          <div style={{ width: '22%' }}>
            <DateOfBirthInfo application={application} />
          </div>
          <FixedSpaceRow
            spacing="xs"
            alignItems="center"
            style={{ width: '22%' }}
          >
            <RoundIcon content={faSection} color={colors.main.m1} size="m" />
            <div>{application.dueDate?.format() ?? '-'}</div>
          </FixedSpaceRow>
          <FixedSpaceRow
            spacing="xs"
            alignItems="center"
            style={{ width: '22%' }}
          >
            <RoundIcon content={faPlay} color={colors.main.m1} size="m" />
            <div>{application.startDate?.format() ?? '-'}</div>
          </FixedSpaceRow>
          <FixedSpaceRow
            spacing="xs"
            alignItems="center"
            justifyContent="flex-end"
            style={{ flexGrow: 1 }}
          >
            <BasisFragment application={application} />
          </FixedSpaceRow>
        </FixedSpaceRow>

        <FixedSpaceColumn spacing="xs">
          <LabelLike>
            {i18n.applications.placementDesktop.preferences}
          </LabelLike>
          <FixedSpaceColumn spacing="xs">
            {application.preferredUnits.map((unit, index) => (
              <FixedSpaceRow
                key={index}
                justifyContent="space-between"
                alignItems="center"
              >
                <UnitListItem
                  $current={application.placementDraftUnit?.id === unit.id}
                >
                  {index + 1}. {unit.name}
                </UnitListItem>
                {application.checkedByAdmin && (
                  <>
                    {shownDaycares.some(({ id }) => id === unit.id) ? (
                      <MutateButton
                        appearance="inline"
                        text={
                          application.placementDraftUnit?.id === unit.id
                            ? i18n.applications.placementDesktop
                                .cancelPlacementDraft
                            : i18n.applications.placementDesktop
                                .createPlacementDraft
                        }
                        icon={
                          application.placementDraftUnit?.id === unit.id
                            ? faUndo
                            : faArrowLeft
                        }
                        mutation={updateApplicationPlacementDraftMutation}
                        onClick={() => ({
                          applicationId: application.id,
                          previousUnitId:
                            application.placementDraftUnit?.id ?? null,
                          body: {
                            unitId:
                              application.placementDraftUnit?.id === unit.id
                                ? null
                                : unit.id
                          }
                        })}
                        onSuccess={() => {
                          onUpdateApplicationPlacementSuccess(
                            application.id,
                            application.placementDraftUnit?.id === unit.id
                              ? null
                              : unit
                          )
                        }}
                        onFailure={() => {
                          onUpdateApplicationPlacementFailure()
                        }}
                        successTimeout={0}
                      />
                    ) : (
                      <Button
                        appearance="inline"
                        icon={faEye}
                        text={i18n.applications.placementDesktop.showUnit}
                        onClick={() => onAddToShownDaycares(unit)}
                      />
                    )}
                  </>
                )}
              </FixedSpaceRow>
            ))}
            {application.placementDraftUnit &&
              !application.preferredUnits.some(
                ({ id }) => id === application.placementDraftUnit?.id
              ) && (
                <FixedSpaceRow
                  justifyContent="space-between"
                  alignItems="center"
                >
                  <UnitListItem $current>
                    *. {application.placementDraftUnit.name}
                  </UnitListItem>
                  {shownDaycares.some(
                    (d) => d.id === application.placementDraftUnit?.id
                  ) ? (
                    <MutateButton
                      appearance="inline"
                      text={
                        i18n.applications.placementDesktop.cancelPlacementDraft
                      }
                      icon={faUndo}
                      mutation={updateApplicationPlacementDraftMutation}
                      onClick={() => ({
                        applicationId: application.id,
                        previousUnitId:
                          application.placementDraftUnit?.id ?? null,
                        body: { unitId: null }
                      })}
                      onSuccess={() => {
                        onUpdateApplicationPlacementSuccess(
                          application.id,
                          null
                        )
                      }}
                      onFailure={() => {
                        onUpdateApplicationPlacementFailure()
                      }}
                      successTimeout={0}
                    />
                  ) : (
                    <Button
                      appearance="inline"
                      icon={faEye}
                      text={i18n.applications.placementDesktop.showUnit}
                      onClick={() =>
                        onAddToShownDaycares(application.placementDraftUnit!)
                      }
                    />
                  )}
                </FixedSpaceRow>
              )}
          </FixedSpaceColumn>
        </FixedSpaceColumn>

        <FixedSpaceRow justifyContent="space-between" alignItems="center">
          <div style={{ width: '360px' }}>
            {application.checkedByAdmin && (
              <Combobox
                items={allUnits}
                selectedItem={null}
                onChange={(unit) => {
                  if (unit) {
                    onAddToShownDaycares(unit)
                    updateApplicationPlacementDraft({
                      applicationId: application.id,
                      previousUnitId:
                        application.placementDraftUnit?.id ?? null,
                      body: { unitId: unit.id }
                    })
                      .then(() =>
                        onUpdateApplicationPlacementSuccess(
                          application.id,
                          unit
                        )
                      )
                      .catch(onUpdateApplicationPlacementFailure)
                  }
                }}
                placeholder={
                  i18n.applications.placementDesktop
                    .createPlacementDraftToOtherUnit
                }
                getItemLabel={(unit) => unit.name}
                isLoading={isPending}
                fullWidth
              />
            )}
          </div>
          {application.checkedByAdmin ? (
            <Button
              appearance="button"
              text={i18n.applications.placementDesktop.toPlacementPlan}
              onClick={() =>
                navigate(`/applications/${application.id}/placement`)
              }
            />
          ) : (
            <a
              href={`/employee/applications/${application.id}`}
              target="_blank"
              rel="noreferrer"
            >
              <Button
                appearance="button"
                text={i18n.applications.placementDesktop.checkApplication}
              />
            </a>
          )}
        </FixedSpaceRow>
      </FixedSpaceColumn>
    </Card>
  )
})

const Card = styled.div`
  width: 580px;
  border: 1px solid ${(p) => p.theme.colors.grayscale.g35};
  border-radius: 4px;
  padding: ${defaultMargins.s};
  background-color: ${(p) => p.theme.colors.grayscale.g0};
`

const UnitListItem = styled.span<{ $current: boolean }>`
  flex-grow: 1;
  max-width: 360px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding-left: ${defaultMargins.xs};

  ${(p) =>
    p.$current
      ? css`
          font-weight: 600;
        `
      : ''}
`
