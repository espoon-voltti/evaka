// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express from 'express'
import { createHeaders } from '../../shared/auth'
import { client } from '../../shared/service-client'

export async function getUserDetails(req: express.Request) {
  return client
    .get(`/persondetails/uuid/${req.user?.id}`, {
      headers: createHeaders(req)
    })
    .then((res) => {
      return res.data
    })
}
