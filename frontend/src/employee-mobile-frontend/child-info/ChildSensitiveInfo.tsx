// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'

import type { ChildId, DaycareId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { fontWeights } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faPhone } from 'lib-icons'

import { renderPinRequiringResult } from '../auth/renderPinRequiringResult'
import { useTranslation } from '../common/i18n'

import { childSensitiveInfoQuery } from './queries'

const Key = styled.span`
  font-weight: ${fontWeights.semibold};
  font-size: 16px;
  margin-bottom: 4px;
`

const KeyValue = styled.div`
  display: flex;
  flex-direction: column;
`

const Phone = styled.div`
  display: flex;
  justify-content: space-between;
`

const renderKeyValue = (
  label: string,
  value: string | null,
  dataQa: string,
  phone?: boolean
) =>
  value ? (
    <KeyValue>
      <Key>{label}</Key>
      {phone ? (
        <Phone>
          <span data-qa={dataQa}>{value}</span>
          <a href={`tel:${value}`}>
            <FontAwesomeIcon icon={faPhone} />
          </a>
        </Phone>
      ) : (
        <span data-qa={dataQa}>{value}</span>
      )}
    </KeyValue>
  ) : null

interface Props {
  childId: ChildId
  unitId: DaycareId
}

export default React.memo(function ChildSensitiveInfo({
  childId,
  unitId
}: Props) {
  const { i18n } = useTranslation()

  const childSensitiveResult = useQueryResult(
    childSensitiveInfoQuery({ childId })
  )

  return (
    <>
      {renderPinRequiringResult(childSensitiveResult, unitId, (child) => (
        <>
          <ContentArea $shadow $opaque>
            <CollapsibleSection $fitted title={i18n.childInfo.otherInfoHeader}>
              <Gap $size="s" />
              <FixedSpaceColumn>
                {renderKeyValue(
                  i18n.childInfo.address,
                  child.childAddress,
                  'child-info-child-address'
                )}

                {renderKeyValue(
                  i18n.childInfo.additionalInfo,
                  child.additionalInfo,
                  'child-info-additional-info'
                )}

                {renderKeyValue(
                  i18n.childInfo.allergies,
                  child.allergies,
                  'child-info-allergies'
                )}

                {renderKeyValue(
                  i18n.childInfo.diet,
                  child.diet,
                  'child-info-diet'
                )}

                {renderKeyValue(
                  i18n.childInfo.medication,
                  child.medication,
                  'child-info-medication'
                )}
              </FixedSpaceColumn>
            </CollapsibleSection>
          </ContentArea>
        </>
      ))}
    </>
  )
})
