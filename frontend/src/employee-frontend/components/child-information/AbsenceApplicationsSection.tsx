// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import partition from 'lodash/partition'
import React, { useMemo, useState } from 'react'
import styled from 'styled-components'

import { string } from 'lib-common/form/fields'
import { object, validated } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { nonBlank } from 'lib-common/form/validators'
import type {
  AbsenceApplicationId,
  ChildId
} from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { Button } from 'lib-components/atoms/buttons/Button'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { H4, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import {
  acceptAbsenceApplicationMutation,
  getAbsenceApplicationsQuery,
  rejectAbsenceApplicationMutation
} from './queries'

interface Props {
  childId: ChildId
}

export const AbsenceApplicationsSection = (props: Props) => {
  const { i18n } = useTranslation()
  const absenceApplicationsResult = useQueryResult(
    getAbsenceApplicationsQuery({ childId: props.childId })
  )
  const sortedAbsenceApplicationsResult = useMemo(
    () =>
      absenceApplicationsResult.map((applications) =>
        partition(
          orderBy(applications, (application) => [application.data.createdAt], [
            'desc'
          ]),
          (application) => application.data.status === 'WAITING_DECISION'
        )
      ),
    [absenceApplicationsResult]
  )
  const [rejectApplicationId, setRejectApplicationId] =
    useState<AbsenceApplicationId | null>(null)

  return renderResult(
    sortedAbsenceApplicationsResult,
    ([incompleted, completed]) => (
      <>
        {rejectApplicationId !== null && (
          <RejectAbsenceApplicationModal
            id={rejectApplicationId}
            close={() => setRejectApplicationId(null)}
          />
        )}
        <div data-qa="absence-applications-incompleted">
          {incompleted.map((application) => (
            <IncompleteApplicationContainer key={application.data.id}>
              <FixedSpaceColumn data-qa="absence-applications-incompleted-row">
                <H4 noMargin>
                  {i18n.childInformation.absenceApplications.absenceApplication}{' '}
                  {application.data.createdAt.toLocalDate().format()}
                </H4>
                <FixedSpaceRow gap={defaultMargins.XXL}>
                  <div>
                    <Label>
                      {i18n.childInformation.absenceApplications.range}
                    </Label>
                    <div>
                      {application.data.startDate.format()} -{' '}
                      {application.data.endDate.format()}
                    </div>
                  </div>
                  <div>
                    <Label>
                      {i18n.childInformation.absenceApplications.createdBy}
                    </Label>
                    <div>
                      {application.data.createdBy.name} (
                      {
                        i18n.childInformation.absenceApplications.userType[
                          application.data.createdBy.type
                        ]
                      }
                      )
                    </div>
                  </div>
                </FixedSpaceRow>
                <div>
                  <Label>
                    {i18n.childInformation.absenceApplications.description}
                  </Label>
                  <div>{application.data.description}</div>
                </div>
                {application.data.status === 'WAITING_DECISION' &&
                  application.actions.includes('DECIDE') && (
                    <>
                      <AlertBox
                        message={
                          i18n.childInformation.absenceApplications.acceptInfo
                        }
                        noMargin
                      />
                      <Gap size="xs" />
                      <FixedSpaceRow justifyContent="flex-end">
                        <Button
                          text={
                            i18n.childInformation.absenceApplications.reject
                          }
                          onClick={() =>
                            setRejectApplicationId(application.data.id)
                          }
                          data-qa="reject-absence-application"
                        />
                        <MutateButton
                          text={
                            i18n.childInformation.absenceApplications.accept
                          }
                          mutation={acceptAbsenceApplicationMutation}
                          onClick={() => ({
                            id: application.data.id
                          })}
                          primary
                          data-qa="accept-absence-application"
                        />
                      </FixedSpaceRow>
                    </>
                  )}
              </FixedSpaceColumn>
            </IncompleteApplicationContainer>
          ))}
        </div>
        <H4>{i18n.childInformation.absenceApplications.list}</H4>
        <Table data-qa="absence-applications-completed">
          <Thead>
            <Tr>
              <Th>{i18n.childInformation.absenceApplications.range}</Th>
              <Th>{i18n.childInformation.absenceApplications.description}</Th>
              <Th>{i18n.childInformation.absenceApplications.createdBy}</Th>
              <Th>{i18n.childInformation.absenceApplications.status}</Th>
            </Tr>
          </Thead>
          <Tbody>
            {completed.map((application) => (
              <Tr
                key={application.data.id}
                data-qa="absence-applications-completed-row"
              >
                <Td>
                  {application.data.startDate.format()} -{' '}
                  {application.data.endDate.format()}
                </Td>
                <Td>{application.data.description}</Td>
                <Td>
                  {application.data.createdBy.name} (
                  {
                    i18n.childInformation.absenceApplications.userType[
                      application.data.createdBy.type
                    ]
                  }
                  )
                </Td>
                <Td>
                  {
                    i18n.childInformation.absenceApplications.statusText[
                      application.data.status
                    ]
                  }
                  {application.data.decidedAt !== null
                    ? `, ${application.data.decidedAt.toLocalDate().format()}`
                    : ''}
                  {application.data.rejectedReason !== null ? (
                    <>
                      <br />
                      {i18n.childInformation.absenceApplications.rejectedReason}
                      : {application.data.rejectedReason}
                    </>
                  ) : (
                    ''
                  )}
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      </>
    )
  )
}

const IncompleteApplicationContainer = styled.div`
  border: 1px solid ${(p) => p.theme.colors.main.m1};
  padding: ${defaultMargins.L};
`

const rejectAbsenceApplicationForm = object({
  reason: validated(string(), nonBlank)
})

const RejectAbsenceApplicationModal = (props: {
  id: AbsenceApplicationId
  close: () => void
}) => {
  const { i18n } = useTranslation()
  const form = useForm(
    rejectAbsenceApplicationForm,
    () => ({ reason: '' }),
    i18n.validationErrors
  )
  const { reason } = useFormFields(form)

  return (
    <MutateFormModal
      title={i18n.childInformation.absenceApplications.rejectModal.title}
      resolveMutation={rejectAbsenceApplicationMutation}
      resolveAction={() => ({
        id: props.id,
        body: form.value()
      })}
      resolveLabel={i18n.common.continue}
      onSuccess={props.close}
      rejectAction={props.close}
      rejectLabel={i18n.common.cancel}
      resolveDisabled={!form.isValid()}
      data-qa="reject-absence-application-modal"
    >
      <FixedSpaceColumn>
        <div>
          <Label>
            {i18n.childInformation.absenceApplications.rejectModal.reason}
          </Label>
          <InputFieldF bind={reason} hideErrorsBeforeTouched data-qa="reason" />
        </div>
      </FixedSpaceColumn>
    </MutateFormModal>
  )
}
