// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Vue from 'vue'

import cDisabledLabel from '@/components/common/c-disabled-label.vue'
import cViewHeader from '@/components/common/c-view-header.vue'
import cViewContent from '@/components/common/c-view-content.vue'
import cViewContainer from '@/components/common/c-view-container.vue'
import cMessageBox from '@/components/common/c-message-box.vue'
import cViewToolbar from '@/components/common/c-view-toolbar.vue'
import cTextContent from '@/components/common/c-text-content.vue'
import ClubTermSelect from '@/components/common/club-term-select.vue'
import FileUpload from '@/components/common/file-upload.vue'
import selectButtonGroup from '@/components/common/select-button-group.vue'
import selectList from '@/components/common/select-list.vue'
import spinner from '@/components/common/spinner.vue'
import cTag from '@/components/common/c-tag.vue'
import cView from '@/components/common/c-view.vue'

Vue.component('cDisabledLabel', cDisabledLabel)
Vue.component('cViewHeader', cViewHeader)
Vue.component('cViewContent', cViewContent)
Vue.component('cViewContainer', cViewContainer)
Vue.component('cMessageBox', cMessageBox)
Vue.component('cTextContent', cTextContent)
Vue.component('cViewToolbar', cViewToolbar)
Vue.component('ClubTermSelect', ClubTermSelect)
Vue.component('FileUpload', FileUpload)
Vue.component('selectButtonGroup', selectButtonGroup)
Vue.component('selectList', selectList)
Vue.component('spinner', spinner)
Vue.component('cTag', cTag)
Vue.component('cView', cView)
