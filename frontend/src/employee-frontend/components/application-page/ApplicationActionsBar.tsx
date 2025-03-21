// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { useNavigate } from 'react-router'
import styled from 'styled-components'

import {
  ApplicationDetails,
  ApplicationStatus
} from 'lib-common/generated/api-types/application'
import { Button } from 'lib-components/atoms/buttons/Button'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import Radio from 'lib-components/atoms/form/Radio'
import StickyFooter from 'lib-components/layout/StickyFooter'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'

import {
  setApplicationVerifiedMutation,
  updateAndSendApplicationMutation,
  updateApplicationMutation
} from './queries'

type Props = {
  applicationStatus: ApplicationStatus
  editing: boolean
  setEditing: (v: boolean) => void
  editedApplication: ApplicationDetails
  errors: boolean
}

export default React.memo(function ApplicationActionsBar({
  applicationStatus,
  editing,
  setEditing,
  editedApplication,
  errors
}: Props) {
  const navigate = useNavigate()
  const { i18n } = useTranslation()

  const [confidential, setConfidential] = useState<boolean | null>(null)

  const actions = [
    {
      id: 'set-verified',
      enabled:
        !editing &&
        !editedApplication.checkedByAdmin &&
        applicationStatus === 'WAITING_PLACEMENT',
      component: (
        <FixedSpaceRow spacing="X3L">
          {editedApplication.confidential === null && (
            <FixedSpaceColumn spacing="xs">
              <Label>{i18n.application.selectConfidentialityLabel} *</Label>
              <FixedSpaceRow spacing="XL">
                <Radio
                  checked={confidential === true}
                  label={i18n.common.yes}
                  onChange={() => setConfidential(true)}
                  data-qa="confidential-yes"
                />
                <Radio
                  checked={confidential === false}
                  label={i18n.common.no}
                  onChange={() => setConfidential(false)}
                  data-qa="confidential-no"
                />
              </FixedSpaceRow>
            </FixedSpaceColumn>
          )}
          <MutateButton
            mutation={setApplicationVerifiedMutation}
            onClick={() => ({
              applicationId: editedApplication.id,
              confidential: confidential
            })}
            disabled={
              editedApplication.confidential === null && confidential === null
            }
            onSuccess={() => undefined}
            text={i18n.applications.actions.setVerified}
            primary
            data-qa="set-verified-btn"
          />
        </FixedSpaceRow>
      )
    },
    {
      id: 'start-editing',
      enabled:
        !editing &&
        (applicationStatus === 'CREATED' || applicationStatus === 'SENT'),
      component: (
        <Button
          onClick={() => setEditing(true)}
          text={i18n.common.edit}
          primary
          data-qa="edit-application"
        />
      )
    },
    {
      id: 'cancel-editing',
      enabled: editing && applicationStatus !== 'CREATED',
      component: (
        <Button onClick={() => setEditing(false)} text={i18n.common.cancel} />
      )
    },
    {
      id: 'cancel-new-application',
      enabled: editing && applicationStatus === 'CREATED',
      component: (
        <Button onClick={() => navigate(-1)} text={i18n.common.cancel} />
      )
    },
    {
      id: 'save-application',
      enabled: editing,
      component: (
        <MutateButton
          mutation={
            editedApplication.status === 'CREATED'
              ? updateAndSendApplicationMutation
              : updateApplicationMutation
          }
          onClick={() => ({
            applicationId: editedApplication.id,
            body: editedApplication
          })}
          text={i18n.common.save}
          textInProgress={i18n.common.saving}
          textDone={i18n.common.saved}
          disabled={!editedApplication || errors}
          onSuccess={() => setEditing(false)}
          primary
          data-qa="save-application"
        />
      )
    }
  ].filter(({ enabled }) => enabled)

  return actions.length > 0 ? (
    <StickyFooter>
      <ContentContainer>
        {actions.map(({ id, component }, index) => (
          <React.Fragment key={id}>
            {index === 0 ? null : <Gap size="s" horizontal />}
            {component}
          </React.Fragment>
        ))}
      </ContentContainer>
    </StickyFooter>
  ) : null
})

const ContentContainer = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: flex-end;
`
