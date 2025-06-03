// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { getErrorCount } from 'lib-common/form-validation'
import type { ApplicationType } from 'lib-common/generated/api-types/application'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { PersonName } from 'lib-components/molecules/PersonNames'
import { H1, H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../localization'

import type { ApplicationFormDataErrors } from './validations'
import { applicationHasErrors } from './validations'

type HeadingProps = {
  type: ApplicationType
  firstName: string
  lastName: string
  errors?: ApplicationFormDataErrors
  transferApplication: boolean
}
export default React.memo(function Heading({
  type,
  errors,
  transferApplication,
  ...name
}: HeadingProps) {
  const t = useTranslation()

  return (
    <ContentArea opaque paddingVertical="L">
      <H1 noMargin data-qa="application-type-title">
        {t.applications.editor.heading.title[type]}
        {transferApplication && ` (${t.applicationsList.transferApplication})`}
      </H1>

      <H2 data-qa="application-child-name-title">
        <PersonName person={name} format="First Last" />
      </H2>

      {t.applications.editor.heading.info[type]}
      {errors && applicationHasErrors(errors) && (
        <>
          <Gap size="s" />
          <AlertBox
            message={
              <div>
                <span data-qa="application-has-errors-title">
                  {t.applications.editor.heading.hasErrors}
                </span>
                <Gap size="s" />
                <FixedSpaceColumn spacing="xxs">
                  {errors &&
                    (Object.keys(errors) as (keyof ApplicationFormDataErrors)[])
                      .filter((section) => getErrorCount(errors[section]) > 0)
                      .map((section) => (
                        <React.Fragment key={section}>
                          <div>
                            <strong>
                              {t.applications.editor[section].title}:{' '}
                            </strong>
                            {t.applications.editor.heading.errors(
                              getErrorCount(errors[section])
                            )}
                          </div>
                        </React.Fragment>
                      ))}
                </FixedSpaceColumn>
              </div>
            }
          />
        </>
      )}
    </ContentArea>
  )
})
