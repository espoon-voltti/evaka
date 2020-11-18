// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  faAngleDown,
  faAngleRight,
  faAngleUp,
  faArrowRight,
  faAt,
  faCalendar,
  faCalendarAlt,
  faCheck,
  faChild,
  faCircle,
  faClock,
  faEnvelope,
  faEuroSign,
  faExclamation,
  faExclamationCircle,
  faExclamationTriangle,
  faFile,
  faFileAlt,
  faFileImage,
  faFilePdf,
  faFileWord,
  faGavel,
  faHands,
  faHome,
  faIdCard,
  faInfo,
  faInfoCircle,
  faLevelUpAlt,
  faLink,
  faList,
  faMap,
  faMapMarkerAlt,
  faMobileAlt,
  faPencilAlt,
  faPlay,
  faPlus,
  faSearch,
  faSignInAlt,
  faSignOutAlt,
  faSpinner,
  faStar,
  faTimes,
  faTrash,
  faUser,
  faUsers
} from '@fortawesome/free-solid-svg-icons'

import { IconDefinition, IconName, IconPrefix } from '@fortawesome/fontawesome-svg-core'

import { IconSet } from '../icon-set'

/**
 * Helper function for converting the free solid icon set to be interpreted as
 * the different pro variations by vue-fontawesome
 */
const convert = (
  icon: IconDefinition,
  prefix: IconPrefix,
  customName: IconName | undefined = undefined
): IconDefinition => ({
  ...icon,
  prefix,
  iconName: customName ?? icon.iconName
})

const icons: IconSet = {
  fasCircle: convert(faCircle, 'fas'),
  fasExclamationCircle: convert(faExclamationCircle, 'fas'),
  fasHome: convert(faHome, 'fas'),
  fasInfoCircle: convert(faInfoCircle, 'fas'),
  fasMapMarkerAlt: convert(faMapMarkerAlt, 'fas'),
  farAngleRight: convert(faAngleRight, 'far'),
  farAt: convert(faAt, 'far'),
  farCalendar: convert(faCalendar, 'far'),
  farCalendarAlt: convert(faCalendarAlt, 'far'),
  farCheck: convert(faCheck, 'far'),
  farFileAlt: convert(faFileAlt, 'far'),
  farFilePdf: convert(faFilePdf, 'far'),
  farGavel: convert(faGavel, 'far'),
  farHands: convert(faHands, 'far'),
  farHome: convert(faHome, 'far'),
  farIdCard: convert(faIdCard, 'far'),
  farLevelUp: convert(faLevelUpAlt, 'far', 'level-up'),
  farList: convert(faList, 'far'),
  farMap: convert(faMap, 'far'),
  farMobile: convert(faMobileAlt, 'far', 'mobile'),
  farMobileAlt: convert(faMobileAlt, 'far'),
  farPencil: convert(faPencilAlt, 'far', 'pencil'),
  farPlus: convert(faPlus, 'far'),
  farSearch: convert(faSearch, 'far'),
  farSpinner: convert(faSpinner, 'far'),
  farTrash: convert(faTrash, 'far'),
  farUser: convert(faUser, 'far'),
  falAngleDown: convert(faAngleDown, 'fal'),
  falAngleUp: convert(faAngleUp, 'fal'),
  falArrowRight: convert(faArrowRight, 'fal'),
  falCalendar: convert(faCalendar, 'fal'),
  falCheck: convert(faCheck, 'fal'),
  falChild: convert(faChild, 'fal'),
  falClock: convert(faClock, 'fal'),
  falEnvelope: convert(faEnvelope, 'fal'),
  falEuroSign: convert(faEuroSign, 'fal'),
  falExclamation: convert(faExclamation, 'fal'),
  falExclamationTriangle: convert(faExclamationTriangle, 'fal'),
  falFile: convert(faFile, 'fal'),
  falFileImage: convert(faFileImage, 'fal'),
  falFilePdf: convert(faFilePdf, 'fal'),
  falFileWord: convert(faFileWord, 'fal'),
  falGavel: convert(faGavel, 'fal'),
  falHands: convert(faHands, 'fal'),
  falInfo: convert(faInfo, 'fal'),
  falLink: convert(faLink, 'fal'),
  falMapMarkerAlt: convert(faMapMarkerAlt, 'fal'),
  falPlay: convert(faPlay, 'fal'),
  falSignIn: convert(faSignInAlt, 'fal', 'sign-in'),
  falSignOut: convert(faSignOutAlt, 'fal', 'sign-out'),
  falStar: convert(faStar, 'fal'),
  falTimes: convert(faTimes, 'fal'),
  falTrash: convert(faTrash, 'fal'),
  falUser: convert(faUser, 'fal'),
  falUsers: convert(faUsers, 'fal')
}

export default icons
