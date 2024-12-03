// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import styled from 'styled-components'

import { ApplicationId } from 'lib-common/generated/api-types/shared'
import Checkbox from 'lib-components/atoms/form/Checkbox'

import { ApplicationUIContext } from '../../state/application-ui'

type Props = {
  applicationId: ApplicationId
}

export default React.memo(function ActionCheckbox({ applicationId }: Props) {
  const { checkedIds, setCheckedIds, showCheckboxes } =
    useContext(ApplicationUIContext)
  const updateCheckedIds = (applicationId: ApplicationId, checked: boolean) => {
    if (checked) {
      setCheckedIds(checkedIds.concat(applicationId))
    } else {
      setCheckedIds(checkedIds.filter((id) => id !== applicationId))
    }
  }

  return (
    <>
      {showCheckboxes ? (
        <CheckboxContainer onClick={(e) => e.stopPropagation()}>
          <Checkbox
            checked={checkedIds.includes(applicationId)}
            label="hidden"
            hiddenLabel={true}
            data-qa={'application-row-checkbox-' + applicationId}
            onChange={(checked) => updateCheckedIds(applicationId, checked)}
          />
        </CheckboxContainer>
      ) : null}
    </>
  )
})

const CheckboxContainer = styled.div`
  padding-left: 16px;
  display: flex;
`
