// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled, { css, useTheme } from 'styled-components'
import { useLocation } from 'wouter'

import type {
  ApplicationSummary,
  ApplicationSummaryPlacementDraft,
  PreferredUnit
} from 'lib-common/generated/api-types/application'
import type { UnitStub } from 'lib-common/generated/api-types/daycare'
import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import type LocalDate from 'lib-common/local-date'
import { cancelMutation, useMutation } from 'lib-common/query'
import PlacementCircle from 'lib-components/atoms/PlacementCircle'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Tooltip from 'lib-components/atoms/Tooltip'
import { Button } from 'lib-components/atoms/buttons/Button'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { MutateIconOnlyButton } from 'lib-components/atoms/buttons/MutateIconOnlyButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { H4, LabelLike, Light } from 'lib-components/typography'
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
import { faCheck, faPen, faTimes } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'
import { isPartDayPlacement } from '../../../utils/placements'
import { CareTypeChip } from '../../common/CareTypeLabel'
import {
  BasisFragment,
  DateOfBirthInfo,
  ServiceWorkerNoteModal
} from '../ApplicationsList'
import {
  deleteApplicationPlacementDraftMutation,
  upsertApplicationPlacementDraftMutation
} from '../queries'

export default React.memo(function ApplicationCard({
  application,
  shownDaycares,
  allUnits,
  onUpsertApplicationPlacementSuccess,
  onDeleteApplicationPlacementSuccess,
  onMutateApplicationPlacementFailure,
  onAddToShownDaycares
}: {
  application: ApplicationSummary
  shownDaycares: PreferredUnit[]
  allUnits: UnitStub[]
  onUpsertApplicationPlacementSuccess: (
    applicationId: ApplicationId,
    unit: PreferredUnit,
    startDate: LocalDate
  ) => void
  onDeleteApplicationPlacementSuccess: (applicationId: ApplicationId) => void
  onMutateApplicationPlacementFailure: () => void
  onAddToShownDaycares: (unit: PreferredUnit) => void
}) {
  const { i18n } = useTranslation()
  const { colors } = useTheme()
  const [, navigate] = useLocation()

  const {
    mutateAsync: upsertApplicationPlacementDraft,
    isPending: updatePending
  } = useMutation(upsertApplicationPlacementDraftMutation)

  const { isPending: deletePending } = useMutation(
    deleteApplicationPlacementDraftMutation
  )

  const [editingNote, setEditingNote] = useState(false)

  const [editingDate, setEditingDate] = useState(false)

  return (
    <Card $placed={application.placementDraft !== null}>
      {editingNote && (
        <ServiceWorkerNoteModal
          applicationId={application.id}
          serviceWorkerNote={application.serviceWorkerNote}
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
            <FixedSpaceColumn spacing="xxs" alignItems="center">
              <CareTypeChip type={application.placementType} />
              {application.transferApplication && (
                <Light>{i18n.applications.list.transfer}</Light>
              )}
            </FixedSpaceColumn>
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
                  onClick={() => setEditingNote(true)}
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
              <FixedSpaceRow key={index} alignItems="center">
                <UnitListItem
                  $selection={
                    application.placementDraft === null
                      ? 'none'
                      : application.placementDraft.unit.id === unit.id
                        ? 'this'
                        : 'other'
                  }
                >
                  {index + 1}. {unit.name}
                </UnitListItem>
                {application.placementDraft?.unit?.id === unit.id &&
                  (editingDate ? (
                    <DateEditor
                      applicationId={application.id}
                      placementDraft={application.placementDraft}
                      onUpsertApplicationPlacementSuccess={
                        onUpsertApplicationPlacementSuccess
                      }
                      onClose={() => setEditingDate(false)}
                    />
                  ) : (
                    <FixedSpaceRow spacing="xs">
                      <span>
                        {application.placementDraft.startDate.format()}
                      </span>
                      <IconOnlyButton
                        icon={faPen}
                        onClick={() => setEditingDate(true)}
                        aria-label={i18n.common.edit}
                      />
                    </FixedSpaceRow>
                  ))}
                {application.placementDraft?.unit?.id === unit.id &&
                  !editingDate && (
                    <MutateButton
                      appearance="inline"
                      text={
                        i18n.applications.placementDesktop.cancelPlacementDraft
                      }
                      icon={faUndo}
                      mutation={deleteApplicationPlacementDraftMutation}
                      onClick={() => ({
                        applicationId: application.id,
                        previousUnitId: unit.id
                      })}
                      onSuccess={() =>
                        onDeleteApplicationPlacementSuccess(application.id)
                      }
                      onFailure={onMutateApplicationPlacementFailure}
                      successTimeout={0}
                    />
                  )}
                {application.checkedByAdmin &&
                  application.placementDraft === null &&
                  !editingDate && (
                    <>
                      {shownDaycares.some(({ id }) => id === unit.id) ? (
                        <MutateButton
                          appearance="inline"
                          text={
                            i18n.applications.placementDesktop
                              .createPlacementDraft
                          }
                          icon={faArrowLeft}
                          mutation={upsertApplicationPlacementDraftMutation}
                          onClick={() => ({
                            applicationId: application.id,
                            previousUnitId: null,
                            body: {
                              unitId: unit.id,
                              startDate: null
                            }
                          })}
                          onSuccess={({ startDate }) =>
                            onUpsertApplicationPlacementSuccess(
                              application.id,
                              unit,
                              startDate
                            )
                          }
                          onFailure={onMutateApplicationPlacementFailure}
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
            {application.placementDraft &&
              !application.preferredUnits.some(
                ({ id }) => id === application.placementDraft?.unit?.id
              ) && (
                <FixedSpaceRow
                  justifyContent="space-between"
                  alignItems="center"
                >
                  <UnitListItem $selection="this">
                    {i18n.applications.placementDesktop.other}:{' '}
                    {application.placementDraft.unit.name}
                  </UnitListItem>
                  {shownDaycares.some(
                    (d) => d.id === application.placementDraft?.unit?.id
                  ) ? (
                    <MutateButton
                      appearance="inline"
                      text={
                        i18n.applications.placementDesktop.cancelPlacementDraft
                      }
                      icon={faUndo}
                      mutation={deleteApplicationPlacementDraftMutation}
                      onClick={() =>
                        application.placementDraft
                          ? {
                              applicationId: application.id,
                              previousUnitId: application.placementDraft.unit.id
                            }
                          : cancelMutation
                      }
                      onSuccess={() =>
                        onDeleteApplicationPlacementSuccess(application.id)
                      }
                      onFailure={onMutateApplicationPlacementFailure}
                      successTimeout={0}
                    />
                  ) : (
                    <Button
                      appearance="inline"
                      icon={faEye}
                      text={i18n.applications.placementDesktop.showUnit}
                      onClick={() => {
                        if (application.placementDraft) {
                          onAddToShownDaycares(application.placementDraft.unit)
                        }
                      }}
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
                    upsertApplicationPlacementDraft({
                      applicationId: application.id,
                      previousUnitId:
                        application.placementDraft?.unit?.id ?? null,
                      body: { unitId: unit.id, startDate: null }
                    })
                      .then(({ startDate }) =>
                        onUpsertApplicationPlacementSuccess(
                          application.id,
                          unit,
                          startDate
                        )
                      )
                      .catch(onMutateApplicationPlacementFailure)
                  }
                }}
                placeholder={
                  i18n.applications.placementDesktop
                    .createPlacementDraftToOtherUnit
                }
                getItemLabel={(unit) => unit.name}
                isLoading={updatePending || deletePending}
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
              primary={application.placementDraft !== null}
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

const DateEditor = React.memo(function DateEditor({
  applicationId,
  placementDraft,
  onUpsertApplicationPlacementSuccess,
  onClose
}: {
  applicationId: ApplicationId
  placementDraft: ApplicationSummaryPlacementDraft
  onUpsertApplicationPlacementSuccess: (
    applicationId: ApplicationId,
    unit: PreferredUnit,
    startDate: LocalDate
  ) => void
  onClose: () => void
}) {
  const { i18n, lang } = useTranslation()
  const [date, setDate] = useState(placementDraft.startDate)

  return (
    <FixedSpaceRow alignItems="center">
      <DatePicker
        date={date}
        onChange={(val) => {
          if (val) setDate(val)
        }}
        locale={lang}
      />
      <FixedSpaceRow spacing="xs" alignItems="center">
        <MutateIconOnlyButton
          icon={faCheck}
          mutation={upsertApplicationPlacementDraftMutation}
          onClick={() => ({
            applicationId: applicationId,
            previousUnitId: placementDraft.unit.id,
            body: { unitId: placementDraft.unit.id, startDate: date }
          })}
          onSuccess={({ startDate }) => {
            onUpsertApplicationPlacementSuccess(
              applicationId,
              placementDraft.unit,
              startDate
            )
            onClose()
          }}
          aria-label={i18n.common.edit}
        />
        <IconOnlyButton
          icon={faTimes}
          onClick={onClose}
          aria-label={i18n.common.cancel}
        />
      </FixedSpaceRow>
    </FixedSpaceRow>
  )
})

const Card = styled.div<{ $placed: boolean }>`
  min-width: 500px;
  max-width: 630px;
  width: 100%;
  ${(p) =>
    p.$placed
      ? css`
          border: 2px solid ${p.theme.colors.status.success};
        `
      : css`
          border: 1px solid ${p.theme.colors.grayscale.g35};
        `}
  border-radius: 4px;
  padding: ${defaultMargins.s};
  background-color: ${(p) => p.theme.colors.grayscale.g0};
`

const UnitListItem = styled.span<{ $selection: 'this' | 'other' | 'none' }>`
  width: 230px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding-left: ${defaultMargins.xs};

  ${(p) =>
    p.$selection === 'this'
      ? css`
          font-weight: 600;
        `
      : p.$selection === 'other'
        ? css`
            color: ${p.theme.colors.grayscale.g70};
          `
        : ''}
`
