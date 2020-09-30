// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.config

import fi.espoo.voltti.logging.filter.BasicMdcFilter
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(BasicMdcFilter::class)
class LoggingConfig
