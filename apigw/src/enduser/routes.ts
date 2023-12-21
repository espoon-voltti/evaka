// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express from 'express'
import { createProxy } from '../shared/proxy-utils.js'

const router = express.Router()
const proxy = createProxy()

router.all('/citizen/*', createProxy())

// deprecated
router.get('/attachments/:applicationId/download/:filename', proxy)
router.get('/attachments/:attachmentId/download/:filename', proxy)

// deprecated
router.delete('/attachments/citizen/:id', proxy)

// deprecated
router.post('/attachments/citizen/applications/:applicationId', proxy)
router.post('/attachments/citizen/income-statements/:incomeStatementId?', proxy)

export default router
