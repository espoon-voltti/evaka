// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useMemo, useState } from 'react'
import { useNavigate } from 'react-router'

import { useBoolean } from 'lib-common/form/hooks'
import {
  AbsenceApplicationId,
  ChildId
} from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { StaticChip } from 'lib-components/atoms/Chip'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { Button } from 'lib-components/atoms/buttons/Button'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { H3, H4, Label, P } from 'lib-components/typography'
import colors from 'lib-customizations/common'
import { faExclamation } from 'lib-icons'
import { faPlus } from 'lib-icons'
import { faTrash } from 'lib-icons'

import { renderResult } from '../../../async-rendering'
import { useTranslation } from '../../../localization'
import ResponsiveWholePageCollapsible from '../../ResponsiveWholePageCollapsible'

import {
  deleteAbsenceApplicationMutation,
  getAbsenceApplicationsQuery
} from './queries'

interface Props {
  childId: ChildId
}

export const AbsenceApplicationsSection = (props: Props) => {
  const i18n = useTranslation()
  const [open, { toggle: toggleOpen }] = useBoolean(false)
  const navigate = useNavigate()

  return (
    <ResponsiveWholePageCollapsible
      title={i18n.children.absenceApplication.title}
      open={open}
      toggleOpen={toggleOpen}
      opaque
      data-qa="collapsible-absence-applications"
    >
      <H3>{i18n.children.absenceApplication.header}</H3>
      <P>{i18n.children.absenceApplication.info}</P>
      <Button
        text={i18n.children.absenceApplication.new}
        onClick={() =>
          navigate(`/children/${props.childId}/absence-application`)
        }
        icon={faPlus}
        appearance="link"
        data-qa="create-absence-application"
      />
      <H4>{i18n.children.absenceApplication.list}</H4>
      <AbsenceApplicationList {...props} />
    </ResponsiveWholePageCollapsible>
  )
}

const AbsenceApplicationList = (props: Props) => {
  const i18n = useTranslation()
  const absenceApplicationsResult = useQueryResult(
    getAbsenceApplicationsQuery({ childId: props.childId })
  )
  const sortedAbsenceApplicationsResult = useMemo(
    () =>
      absenceApplicationsResult.map((applications) =>
        orderBy(applications, (application) => [application.data.createdAt], [
          'desc'
        ])
      ),
    [absenceApplicationsResult]
  )
  const [deleteApplicationId, setDeleteApplicationId] =
    useState<AbsenceApplicationId | null>(null)

  return (
    <div data-qa="absence-applications-table">
      {deleteApplicationId !== null && (
        <DeleteAbsenceApplicationModal
          id={deleteApplicationId}
          close={() => setDeleteApplicationId(null)}
        />
      )}
      {renderResult(sortedAbsenceApplicationsResult, (applications) => (
        <>
          {applications.map((application) => (
            <React.Fragment key={application.data.id}>
              <FixedSpaceColumn data-qa="absence-application-row">
                <FixedSpaceRow>
                  <div>
                    {application.data.startDate.format()} -{' '}
                    {application.data.endDate.format()}
                  </div>
                  <StaticChip color={colors.main.m1}>
                    {
                      i18n.children.absenceApplication.status[
                        application.data.status
                      ]
                    }
                  </StaticChip>
                </FixedSpaceRow>
                <div>
                  <Label>{i18n.children.absenceApplication.description}</Label>
                  <div>{application.data.description}</div>
                </div>
                {application.data.rejectedReason !== null && (
                  <div>
                    <Label>
                      {i18n.children.absenceApplication.rejectedReason}
                    </Label>
                    <div>{application.data.rejectedReason}</div>
                  </div>
                )}
                {application.data.status === 'WAITING_DECISION' &&
                  application.actions.includes('DELETE') && (
                    <Button
                      icon={faTrash}
                      text={i18n.children.absenceApplication.cancel}
                      onClick={() =>
                        setDeleteApplicationId(application.data.id)
                      }
                      data-qa="delete-absence-application"
                    />
                  )}
              </FixedSpaceColumn>
              <HorizontalLine slim />
            </React.Fragment>
          ))}
        </>
      ))}
    </div>
  )
}

const DeleteAbsenceApplicationModal = (props: {
  id: AbsenceApplicationId
  close: () => void
}) => {
  const i18n = useTranslation()

  return (
    <MutateFormModal
      title={i18n.children.absenceApplication.cancelConfirmation}
      resolveMutation={deleteAbsenceApplicationMutation}
      resolveAction={() => ({ id: props.id })}
      resolveLabel={i18n.common.yes}
      onSuccess={props.close}
      rejectAction={props.close}
      rejectLabel={i18n.common.no}
      icon={faExclamation}
      type="warning"
      data-qa="delete-absence-application-modal"
    />
  )
}
