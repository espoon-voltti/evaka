// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { ApplicationUIContext } from '../../state/application-ui'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import styled from 'styled-components'

type Props = {
  applicationId: string
}

export default React.memo(function ActionCheckbox({ applicationId }: Props) {
  const { checkedIds, setCheckedIds, showCheckboxes } =
    useContext(ApplicationUIContext)
  const updateCheckedIds = (applicationId: string, checked: boolean) => {
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
            label={'hidden'}
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
