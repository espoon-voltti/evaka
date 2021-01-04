// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {useEffect, useState} from 'react'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faClock } from '@evaka/lib-icons'
import {Decision} from "~decisions/types";
import {client} from "~api-client";

const getDecisions = async () : Promise<Decision[]> => {
  const { data } = await client.get<Decision[]>('/citizen/decisions')
  return data
}

export default React.memo(function Decisions() {

  const [decisions, setDecisions] = useState<Decision[]>([])

  useEffect(() => {
    getDecisions().then(setDecisions)
  }, [])

  return (
    <Title>
      hello word <FontAwesomeIcon icon={faClock} />
      { decisions.map(decision => <span>{decision.id}</span>) }
    </Title>
  )
})

const Title = styled.h1`
  color: pink;
`
