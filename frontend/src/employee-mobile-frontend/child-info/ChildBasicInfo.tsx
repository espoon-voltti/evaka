// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'

import { ChildBasicInformation } from 'lib-common/generated/api-types/sensitive'
import { getAge } from 'lib-common/utils/local-date'
import Title from 'lib-components/atoms/Title'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { fontWeights } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faPhone } from 'lib-icons'

import { useTranslation } from '../common/i18n'

const Key = styled.span`
  font-weight: ${fontWeights.semibold};
  font-size: 16px;
  margin-bottom: 4px;
`

const KeyValue = styled.div`
  display: flex;
  flex-direction: column;
`

const Divider = styled.div`
  border-bottom: 1px solid #d8d8d8;
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
  child: ChildBasicInformation
}

export default React.memo(function ChildBasicInfo({ child }: Props) {
  const { i18n } = useTranslation()
  return (
    <FixedSpaceColumn spacing="m">
      <ContentArea shadow opaque>
        <CollapsibleSection fitted title={i18n.childInfo.personalInfoHeader}>
          <Gap size="s" />
          <FixedSpaceColumn>
            <KeyValue>
              <Key>{i18n.childInfo.childName}</Key>
              <span data-qa="child-info-name">{`${child.firstName} ${child.lastName}`}</span>
            </KeyValue>

            {renderKeyValue(
              i18n.childInfo.preferredName,
              child.preferredName,
              'child-info-preferred-name'
            )}

            {renderKeyValue(
              i18n.childInfo.dateOfBirth,
              `${child.dateOfBirth.format()}, ${getAge(child.dateOfBirth)}${
                i18n.common.yearsShort
              }.`,
              'child-info-dob'
            )}

            {renderKeyValue(
              i18n.childInfo.type,
              child.placementType
                ? i18n.common.placement[child.placementType]
                : '-',
              'child-info-placement-type'
            )}
          </FixedSpaceColumn>
        </CollapsibleSection>
      </ContentArea>

      <ContentArea shadow opaque>
        <CollapsibleSection fitted title={i18n.childInfo.contactInfoHeader}>
          <Gap size="s" />
          <FixedSpaceColumn>
            {child.contacts.map((contact, index) => (
              <div key={contact.id}>
                <Title size={4} noMargin>{`${i18n.childInfo.contact} ${
                  index + 1
                }`}</Title>
                <Gap size="s" />
                <FixedSpaceColumn>
                  <KeyValue>
                    <Key>{i18n.childInfo.name}</Key>
                    <span data-qa={`child-info-contact${index + 1}-name`}>{`${
                      contact.firstName || ''
                    } ${contact.lastName || ''}`}</span>
                  </KeyValue>

                  {renderKeyValue(
                    i18n.childInfo.phone,
                    contact.phone,
                    `child-info-contact${index + 1}-phone`,
                    true
                  )}

                  {renderKeyValue(
                    i18n.childInfo.backupPhone,
                    contact.backupPhone,
                    `child-info-contact${index + 1}-backup-phone`,
                    true
                  )}

                  {renderKeyValue(
                    i18n.childInfo.email,
                    contact.email,
                    `child-info-contact${index + 1}-email`
                  )}

                  {index !== child.contacts.length - 1 && <Divider />}
                </FixedSpaceColumn>
              </div>
            ))}
          </FixedSpaceColumn>

          {child.backupPickups?.map((backupPickup, index) => (
            <div key={backupPickup.id}>
              <Title size={4}>
                {`${i18n.childInfo.backupPickup} ${index + 1}`}
              </Title>
              <FixedSpaceColumn>
                {renderKeyValue(
                  i18n.childInfo.backupPickupName,
                  backupPickup.firstName,
                  `child-info-backup-pickup${index + 1}-name`
                )}

                {renderKeyValue(
                  i18n.childInfo.phone,
                  backupPickup.phone,
                  `child-info-backup-pickup${index + 1}-phone`,
                  true
                )}

                <Divider />
              </FixedSpaceColumn>
            </div>
          ))}
        </CollapsibleSection>
      </ContentArea>
    </FixedSpaceColumn>
  )
})
