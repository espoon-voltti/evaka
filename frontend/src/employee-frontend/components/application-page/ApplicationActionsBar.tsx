// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { useNavigate } from 'react-router'
import styled from 'styled-components'

import { Success, wrapResult } from 'lib-common/api'
import {
  ApplicationDetails,
  ApplicationStatus
} from 'lib-common/generated/api-types/application'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import Radio from 'lib-components/atoms/form/Radio'
import StickyFooter from 'lib-components/layout/StickyFooter'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import {
  sendApplication,
  setApplicationVerified,
  updateApplication
} from '../../generated/api-clients/application'
import { useTranslation } from '../../state/i18n'

const updateApplicationResult = wrapResult(updateApplication)
const sendApplicationResult = wrapResult(sendApplication)
const setApplicationVerifiedResult = wrapResult(setApplicationVerified)

type Props = {
  applicationStatus: ApplicationStatus
  editing: boolean
  setEditing: (v: boolean) => void
  editedApplication: ApplicationDetails
  reloadApplication: () => void
  errors: boolean
}

export default React.memo(function ApplicationActionsBar({
  applicationStatus,
  editing,
  setEditing,
  editedApplication,
  reloadApplication,
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
          <AsyncButton
            onClick={() =>
              setApplicationVerifiedResult({
                applicationId: editedApplication.id,
                confidential: confidential
              })
            }
            disabled={
              editedApplication.confidential === null && confidential === null
            }
            onSuccess={reloadApplication}
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
        <LegacyButton
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
        <LegacyButton
          onClick={() => setEditing(false)}
          text={i18n.common.cancel}
        />
      )
    },
    {
      id: 'cancel-new-application',
      enabled: editing && applicationStatus === 'CREATED',
      component: (
        <LegacyButton onClick={() => navigate(-1)} text={i18n.common.cancel} />
      )
    },
    {
      id: 'save-application',
      enabled: editing,
      component: (
        <AsyncButton
          text={i18n.common.save}
          textInProgress={i18n.common.saving}
          textDone={i18n.common.saved}
          disabled={!editedApplication || errors}
          onClick={() =>
            updateApplicationResult({
              applicationId: editedApplication.id,
              body: editedApplication
            }).then((result) => {
              if (result.isSuccess) {
                return editedApplication.status === 'CREATED'
                  ? sendApplicationResult({
                      applicationId: editedApplication.id
                    })
                  : Success.of()
              } else {
                return result
              }
            })
          }
          onSuccess={() => {
            setEditing(false)
            reloadApplication()
          }}
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
