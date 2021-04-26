// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Router } from 'express'
import { createProxy } from '../shared/proxy-utils'

const router = Router()
const proxy = createProxy()

router.get('/units', proxy)
router.get('/public/units/*', proxy)
router.get('/public/club-terms', proxy)

export default router
