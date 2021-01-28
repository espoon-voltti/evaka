// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { AbsenceTypes } from '~types/absence'
import ColourInfoItem from '~components/common/ColourInfoItem'

export default React.memo(function ColorInfo() {
  return (
    <Container>
      {AbsenceTypes.concat('TEMPORARY_RELOCATION').map((absenceType, index) => (
        <ColourInfoItem type={absenceType} key={index} />
      ))}
    </Container>
  )
})

const Container = styled.div`
  display: flex;
  justify-content: flex-start;
  padding: 0 20px;
  flex-wrap: wrap;
  font-size: 15px;
  margin: 20px auto;

  @media print {
    font-size: 12px;
    padding: 0;
  }

  > div {
    margin: 10px;

    @media print {
      margin: 0 6px 0 0;
    }
  }
`
