// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { ContactInfoFormData } from 'lib-common/api-types/application/ApplicationFormData'
import { ApplicationType } from 'lib-common/generated/api-types/application'
import ListGrid from 'lib-components/layout/ListGrid'
import { H2, H3, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../../localization'

import ContactInfoSecondGuardian from './ContactInfoSecondGuardian'
import { ApplicationDataGridLabelWidth } from './const'

type ContactInfoProps = {
  formData: ContactInfoFormData
  type: ApplicationType
  showFridgeFamilySection: boolean
}

export default React.memo(function ContactInfoSection({
  formData,
  type,
  showFridgeFamilySection
}: ContactInfoProps) {
  const t = useTranslation()
  const tLocal = t.applications.editor.verification.contactInfo

  const otherChildren = [
    ...formData.vtjSiblings
      .filter((s) => s.selected)
      .map((s) => ({
        firstName: s.firstName,
        lastName: s.lastName,
        socialSecurityNumber: s.socialSecurityNumber
      })),
    ...(formData.otherChildrenExists ? formData.otherChildren : [])
  ]

  return (
    <div>
      <H2 noMargin>{tLocal.title}</H2>

      <Gap size="s" />

      <H3>{tLocal.child.title}</H3>
      <ListGrid
        labelWidth={ApplicationDataGridLabelWidth}
        rowGap="s"
        columnGap="L"
      >
        <Label>{tLocal.child.name}</Label>
        <span translate="no">
          {formData.childFirstName} {formData.childLastName}
        </span>

        <Label>{tLocal.child.ssn}</Label>
        <span>{formData.childSSN}</span>

        <Label>{tLocal.child.streetAddress}</Label>
        <span translate="no">{formData.childStreet}</span>

        <Label>{tLocal.child.isAddressChanging}</Label>
        <span>
          {formData.childFutureAddressExists
            ? tLocal.child.hasFutureAddress
            : t.applications.editor.verification.no}
        </span>

        {formData.childFutureAddressExists && (
          <>
            <Label>{tLocal.child.addressChangesAt}</Label>
            <span>{formData.childMoveDate?.format()}</span>

            <Label>{tLocal.child.newAddress}</Label>
            <span translate="no">
              {formData.childFutureStreet} {formData.childFuturePostalCode}{' '}
              {formData.childFuturePostOffice}
            </span>
          </>
        )}
      </ListGrid>

      <Gap size="m" />

      <H3>{tLocal.guardian.title}</H3>
      <ListGrid
        labelWidth={ApplicationDataGridLabelWidth}
        rowGap="s"
        columnGap="L"
      >
        <Label>{tLocal.guardian.name}</Label>
        <span translate="no">
          {formData.guardianFirstName} {formData.guardianLastName}
        </span>
        <Label>{tLocal.guardian.tel}</Label>
        <span>{formData.guardianPhone}</span>
        <Label>{tLocal.guardian.email}</Label>
        <span translate="no">{formData.guardianEmail}</span>
        <Label>{tLocal.guardian.streetAddress}</Label>
        <span>{formData.guardianHomeAddress}</span>

        <Label>{tLocal.guardian.isAddressChanging}</Label>
        <span>
          {formData.guardianFutureAddressExists
            ? tLocal.guardian.hasFutureAddress
            : t.applications.editor.verification.no}
        </span>

        {formData.guardianFutureAddressExists && (
          <>
            <Label>{tLocal.guardian.addressChangesAt}</Label>
            <span>{formData.guardianMoveDate?.format()}</span>

            <Label>{tLocal.guardian.newAddress}</Label>
            <span translate="no">
              {formData.guardianFutureStreet}{' '}
              {formData.guardianFuturePostalCode}{' '}
              {formData.guardianFuturePostOffice}
            </span>
          </>
        )}
      </ListGrid>

      {type !== 'CLUB' && (
        <>
          <Gap size="m" />
          <H3>{tLocal.secondGuardian.title}</H3>
          {!!formData.otherGuardianAgreementStatus && (
            <ContactInfoSecondGuardian formData={formData} />
          )}

          {showFridgeFamilySection && (
            <>
              <Gap size="m" />
              <H3>{tLocal.fridgePartner.title}</H3>
              {formData.otherPartnerExists ? (
                <ListGrid
                  labelWidth={ApplicationDataGridLabelWidth}
                  rowGap="s"
                  columnGap="L"
                >
                  <Label>{tLocal.fridgePartner.name}</Label>
                  <span translate="no">
                    {formData.otherPartnerFirstName}{' '}
                    {formData.otherPartnerLastName}
                  </span>

                  <Label>{tLocal.fridgePartner.ssn}</Label>
                  <span>{formData.otherPartnerSSN}</span>
                </ListGrid>
              ) : (
                <span>{t.applications.editor.verification.no}</span>
              )}

              <Gap size="m" />
              <H3>{tLocal.fridgeChildren.title}</H3>
              {otherChildren.length > 0 ? (
                otherChildren.map(
                  ({ firstName, lastName, socialSecurityNumber }) => (
                    <ListGrid
                      key={socialSecurityNumber}
                      labelWidth={ApplicationDataGridLabelWidth}
                      rowGap="s"
                      columnGap="L"
                    >
                      <Label>{tLocal.fridgeChildren.name}</Label>
                      <span translate="no">
                        {firstName} {lastName}
                      </span>

                      <Label>{tLocal.fridgeChildren.ssn}</Label>
                      <span>{socialSecurityNumber}</span>
                      <Gap size="m" />
                    </ListGrid>
                  )
                )
              ) : (
                <span>{tLocal.fridgeChildren.noOtherChildren}</span>
              )}
            </>
          )}
        </>
      )}
    </div>
  )
})
