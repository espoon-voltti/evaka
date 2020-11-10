<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <AppModal ref="modal" size="large">
    <c-title :size="2">
      {{ $t('form.select-application-type.title') }}
    </c-title>

    <div class="application-type-subtitle">
      {{ $t('form.select-application-type.subtitle') }}
    </div>
    <div class="type-options">
      <div class="type-option" v-if="showDaycareApplicationType">
        <div class="type-option-item">
          <c-radio
            id="radio-daycare"
            :disabled="disabledOptions.daycare"
            name="applicationType"
            :inputValue="types.DAYCARE"
            v-model="model"
            :label="$t('form.select-application-type.types.daycare.label')"
          >
            <c-instructions
              :instruction="
                $t('form.select-application-type.types.daycare.instruction')
              "
            >
            </c-instructions>
          </c-radio>
        </div>
        <c-disabled-label
          v-if="disabledOptions.daycare"
          class="selectApplication-disabledLabel"
        >
          {{
            $t('form.select-application-type.types.daycare.disabled', {
              range: 'DATERANGE',
              date: 'DATE'
            })
          }}
        </c-disabled-label>
      </div>
      <div class="type-option">
        <div class="type-option-item">
          <c-radio
            id="radio-preschool"
            :disabled="disabledOptions.preschool"
            name="applicationType"
            :inputValue="types.PRESCHOOL"
            v-model="model"
            :label="$t('form.select-application-type.types.preschool.label')"
            :description="
              $t('form.select-application-type.types.preschool.description')
            "
          >
            <c-instructions
              :instruction="
                $t('form.select-application-type.types.preschool.instruction')
              "
            >
            </c-instructions>
          </c-radio>
        </div>
        <c-disabled-label
          v-if="disabledOptions.preschool"
          class="selectApplication-disabledLabel"
        >
          {{
            $t('form.select-application-type.types.preschool.disabled', {
              range: 'DATERANGE'
            })
          }}
        </c-disabled-label>
      </div>
      <div class="type-option" v-if="!currentLocaleIsSwedish">
        <div class="type-option-item">
          <c-radio
            id="radio-club"
            :disabled="disabledOptions.club"
            name="applicationType"
            :inputValue="types.CLUB"
            v-model="model"
            :label="$t('form.select-application-type.types.club.label')"
          >
            <c-instructions
              :instruction="
                $t('form.select-application-type.types.club.instruction')
              "
            >
            </c-instructions>
          </c-radio>
        </div>
        <c-disabled-label
          v-if="disabledOptions.club"
          class="selectApplication-disabledLabel"
        >
          {{ $t('form.select-application-type.types.club.disabled') }}
        </c-disabled-label>
      </div>
    </div>

    <p
      v-for="(text, index) in $t('form.select-application-type.info')"
      :key="index"
    >
      <span v-html="text"></span>
    </p>

    <c-warning-box v-if="duplicateExists">
      {{ $t('form.select-application-type.duplicate-warning') }}
    </c-warning-box>

    <c-info-box v-else-if="childHasMatchingPlacement">
      <p
        v-if="this.model.applicationType === 'daycare'"
        v-html="
          $t('form.select-application-type.matching-placement.daycare-transfer')
        "
      ></p>
      <p
        v-if="
          this.model.applicationType === 'preschool' &&
          !this.childHasNoPreschoolDaycare
        "
        v-html="
          $t(
            'form.select-application-type.matching-placement.preschool-transfer-only'
          )
        "
      ></p>
      <p
        v-if="
          this.model.applicationType === 'preschool' &&
          this.childHasNoPreschoolDaycare
        "
        v-html="
          $t(
            'form.select-application-type.matching-placement.preschool-transfer-and-daycare'
          )
        "
      ></p>
    </c-info-box>

    <div class="columns is-centered selectApplication-buttonsWrapper">
      <div class="column is-narrow">
        <c-button @click="cancel" :primary="true" :borderless="true">{{
          $t('general.cancel')
        }}</c-button>
        <c-button
          data-qa="btn-select-application-type"
          @click="selectApplicationType"
          :disabled="disableSubmit"
          :primary="true"
          >{{
            $t('form.select-application-type.selectApplicationType')
          }}</c-button
        >
      </div>
    </div>
  </AppModal>
</template>

<script>
  import { mapGetters } from 'vuex'
  import { APPLICATION_TYPE, LANGUAGES } from '../../constants'
  import { config } from '@evaka/enduser-frontend/src/config'

  export default {
    name: 'SelectApplicationType',
    data() {
      return {
        model: {
          childId: null,
          applicationType: null
        }
      }
    },
    computed: {
      ...mapGetters(['children', 'applications']),
      locale() {
        return this.$i18n.locale
      },
      currentLocaleIsSwedish() {
        return this.locale.toLowerCase() === LANGUAGES.SV
      },
      showDaycareApplicationType() {
        return config.feature.daycareApplication === true
      },
      types() {
        return Object.keys(APPLICATION_TYPE)
          .map((k) => [k, APPLICATION_TYPE[k].value])
          .reduce((r, [k, type]) => {
            return {
              ...r,
              [k]: type
            }
          }, {})
      },
      child() {
        return this.children.find((c) => this.model.childId === c.id)
      },
      // TODO: get data from backend
      disabledOptions() {
        const { types } = this
        return {
          [types.DAYCARE]: false,
          [types.PRESCHOOL]: false,
          [types.CLUB]: false
        }
      },
      duplicateExists() {
        return this.applications
          .map(app => ({
            child: app.childId,
            type: app.type,
            status: app.status
          }))
          .filter(({status}) => !['REJECTED', 'ACTIVE', 'CANCELLED', 'ARCHIVED', 'TERMINATED'].includes(status))
          .some(({child, type}) =>
            child === this.model.childId &&
            this.model.applicationType !== APPLICATION_TYPE.CLUB.value &&
            type === this.model.applicationType
          )
      },
      childHasMatchingPlacement() {
        return this.child
          ? this.child.existingPlacements.some(({ type }) => {
              if (this.model.applicationType === 'daycare') {
                return ['DAYCARE', 'DAYCARE_PART_TIME'].includes(type)
              }
              if (this.model.applicationType === 'preschool') {
                return ['PRESCHOOL', 'PRESCHOOL_DAYCARE'].includes(type)
              }
              return false
            })
          : false
      },
      childHasNoPreschoolDaycare() {
        return this.child
          ? this.child.existingPlacements.some(
              ({ type }) => ['PRESCHOOL', 'PREPARATORY'].includes(type)
            ) &&
              !this.child.existingPlacements.some(({ type }) =>
                ['PRESCHOOL_DAYCARE', 'PREPARATORY_DAYCARE'].includes(type)
              )
          : false
      },
      disableSubmit() {
        return !this.model.applicationType || this.duplicateExists
      }
    },
    methods: {
      open(childId) {
        this.model.childId = childId
        this.model.applicationType = null
        return this.$refs.modal.open()
      },
      selectApplicationType() {
        this.$refs.modal.resolveModal({
          type: this.model.applicationType,
          childId: this.model.childId
        })
      },
      cancel() {
        this.$refs.modal.rejectModal()
      }
    }
  }
</script>

<style lang="scss" scoped>
  .selectApplication {
    &-disabledLabel {
      margin-left: 52px;
    }

    &-buttonsWrapper {
      margin-top: 3em;
    }
  }
  @media (max-width: $tablet) {
    .selectApplication {
      &-buttonsWrapper {
        margin-top: 0em;
      }
    }
  }

  .title {
    color: $grey-dark;
    margin-bottom: 1em;
  }

  .application-type-subtitle {
    font-weight: 600;
  }

  .type-options {
    display: flex;
    flex-direction: column;
    margin-top: 2em;
  }

  .type-option {
    margin-bottom: 1em;
    display: flex;
    flex-direction: column;

    &-item {
      display: flex;
      flex-direction: row;
    }
  }
</style>
