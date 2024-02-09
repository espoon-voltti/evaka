// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { ApplicationType } from 'lib-common/generated/api-types/application'
import { useQuery, useQueryResult } from 'lib-common/query'
import useRouteParams from 'lib-common/useRouteParams'
import Main from 'lib-components/atoms/Main'
import Button from 'lib-components/atoms/buttons/Button'
import MutateButton, {
  cancelMutation
} from 'lib-components/atoms/buttons/MutateButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Radio from 'lib-components/atoms/form/Radio'
import ButtonContainer from 'lib-components/layout/ButtonContainer'
import { Container, ContentArea } from 'lib-components/layout/Container'
import ExpandingInfo, {
  ExpandingInfoButtonSlot
} from 'lib-components/molecules/ExpandingInfo'
import { AlertBox, InfoBox } from 'lib-components/molecules/MessageBoxes'
import { fontWeights, H1, H2 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/citizen'

import Footer from '../Footer'
import { useTranslation } from '../localization'
import useTitle from '../useTitle'

import {
  activePlacementsByApplicationTypeQuery,
  applicationChildrenQuery,
  createApplicationMutation,
  duplicateApplicationsQuery
} from './queries'

export default React.memo(function ApplicationCreation() {
  const navigate = useNavigate()
  const { childId } = useRouteParams(['childId'])
  const t = useTranslation()
  useTitle(t, t.applications.creation.title)
  const children = useQueryResult(applicationChildrenQuery())
  const childName = useMemo(
    () =>
      children.map((children) => {
        const child = children.find(({ id }) => id === childId)
        if (child === undefined) {
          return undefined
        }
        return `${child.firstName} ${child.lastName}`
      }),
    [childId, children]
  )
  const [selectedType, setSelectedType] = useState<ApplicationType>()

  const { data: duplicates } = useQuery(duplicateApplicationsQuery({ childId }))
  const duplicateExists =
    selectedType !== undefined &&
    duplicates !== undefined &&
    duplicates[selectedType]

  const { data: transferApplicationTypes } = useQuery(
    activePlacementsByApplicationTypeQuery({ childId })
  )
  const shouldUseTransferApplication =
    (selectedType === 'DAYCARE' || selectedType === 'PRESCHOOL') &&
    transferApplicationTypes !== undefined &&
    transferApplicationTypes[selectedType]

  if (!childName.isLoading && childName.getOrElse(undefined) === undefined) {
    return <Navigate replace to="/applications" />
  }

  return (
    <>
      <Container>
        <ReturnButton label={t.common.return} />
        <Main>
          <ContentArea
            opaque
            paddingVertical="L"
            data-qa="application-options-area"
          >
            <H1 noMargin>{t.applications.creation.title}</H1>
            <Gap size="m" />
            <H2 noMargin>{childName.getOrElse('')}</H2>
            <Gap size="XL" />
            <ExpandingInfo
              data-qa="daycare-expanding-info"
              info={t.applications.creation.daycareInfo}
            >
              <Radio
                checked={selectedType === 'DAYCARE'}
                onChange={() => setSelectedType('DAYCARE')}
                label={t.applications.creation.daycareLabel}
                data-qa="type-radio-DAYCARE"
              />
            </ExpandingInfo>
            <Gap size="s" />
            {featureFlags.preschool && (
              <>
                <Radio
                  checked={selectedType === 'PRESCHOOL'}
                  onChange={() => setSelectedType('PRESCHOOL')}
                  label={t.applications.creation.preschoolLabel}
                  data-qa="type-radio-PRESCHOOL"
                />
                <ExpandingInfo info={t.applications.creation.preschoolInfo}>
                  <PreschoolDaycareInfo>
                    {t.applications.creation.preschoolDaycareInfo}
                    <ExpandingInfoButtonSlot />
                  </PreschoolDaycareInfo>
                </ExpandingInfo>
                <Gap size="s" />
              </>
            )}
            {!featureFlags.hideClubApplication && (
              <ExpandingInfo
                data-qa="club-expanding-info"
                info={t.applications.creation.clubInfo}
              >
                <Radio
                  checked={selectedType === 'CLUB'}
                  onChange={() => setSelectedType('CLUB')}
                  label={t.applications.creation.clubLabel}
                  data-qa="type-radio-CLUB"
                />
              </ExpandingInfo>
            )}
            <Gap size="m" />
            {duplicateExists ? (
              <>
                <AlertBox
                  data-qa="duplicate-application-notification"
                  message={t.applications.creation.duplicateWarning}
                  noMargin
                />
                <Gap size="m" />
              </>
            ) : null}
            {!duplicateExists && shouldUseTransferApplication && (
              <>
                <InfoBox
                  thin
                  data-qa="transfer-application-notification"
                  message={
                    t.applications.creation.transferApplicationInfo[
                      selectedType === 'DAYCARE' ? 'DAYCARE' : 'PRESCHOOL'
                    ]
                  }
                />
                <Gap size="m" />
              </>
            )}
            {t.applications.creation.applicationInfo}
          </ContentArea>
          <ContentArea opaque={false} paddingVertical="L">
            <ButtonContainer justify="center">
              <MutateButton
                primary
                text={t.applications.creation.create}
                disabled={selectedType === undefined || duplicateExists}
                mutation={createApplicationMutation}
                onClick={() =>
                  selectedType !== undefined
                    ? { body: { childId, type: selectedType } }
                    : cancelMutation
                }
                onSuccess={(id) => navigate(`/applications/${id}/edit`)}
                data-qa="submit"
              />
              <Button
                text={t.common.cancel}
                onClick={() => navigate('/applications')}
              />
            </ButtonContainer>
          </ContentArea>
        </Main>
      </Container>
      <Footer />
    </>
  )
})

const PreschoolDaycareInfo = styled.p`
  margin: 0;
  margin-left: calc(
    36px + ${defaultMargins.s}
  ); // width of the radio input's icon + the margin on label
  font-weight: ${fontWeights.semibold};
  font-size: 0.875em;
`
