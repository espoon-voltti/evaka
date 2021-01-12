export interface Colors {
  primaryColors: {
    dark: string
    medium: string
    primary: string
    light: string
  }
}

export interface CitizenLocalization {
  header: {
    nav: {
      map: string
      applications: string
      decisions: string
      newDecisions: string
    }
    lang: {
      fi: string
      sv: string
      en: string
    }
    logout: string
  }
  footer: {
    espooLabel: string
    privacyPolicy: string
    privacyPolicyLink: string
    sendFeedback: string
    sendFeedbackLink: string
  }
  decisions: {
    title: string
    summary: string
    unconfirmedDecisions: (n: number) => string
    pageLoadError: string
    applicationDecisions: {
      decision: string
      type: {
        CLUB: string
        DAYCARE: string
        DAYCARE_PART_TIME: string
        PRESCHOOL: string
        PRESCHOOL_DAYCARE: string
        PREPARATORY_EDUCATION: string
      }
      childName: string
      unit: string
      period: string
      sentDate: string
      resolved: string
      statusLabel: string
      summary: string
      status: {
        PENDING: string
        ACCEPTED: string
        REJECTED: string
      }
      confirmationInfo: {
        preschool: string
        default: string
      }
      goToConfirmation: string
      confirmationLink: string
      response: {
        title: string
        accept1: string
        accept2: string
        reject: string
        cancel: string
        submit: string
        disabledInfo: string
      }
      openPdf: string
      warnings: {
        decisionWithNoResponseWarning: {
          title: string
          text: string
          resolveLabel: string
          rejectLabel: string
        }
        doubleRejectWarning: {
          title: string
          text: string
          resolveLabel: string
          rejectLabel: string
        }
      }
      errors: {
        pageLoadError: string
        submitFailure: string
      }
      returnToPreviousPage: string
    }
  }
}

export default interface Customizations {
  colors: Colors
  localization: {
    citizen: {
      fi: CitizenLocalization
      sv: CitizenLocalization
      en: CitizenLocalization
    }
  }
}
