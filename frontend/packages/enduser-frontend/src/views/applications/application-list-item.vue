<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <li
    class="has-text-left application-list-item"
    :data-qa-type="application.type.value"
  >
    <div class="application-item-details">
      <c-title :size="4" :isGreyDark="true">{{ applicationTitle }}</c-title>
    </div>
    <div class="columns">
      <div class="application-info column">
        <div class="info-wrapper">
          <div class="left">{{ $t('general.state') }}</div>
          <div class="right" data-qa="application-status">
            <div class="application-status-wrapper">
              <div :class="['application-status-icon', getIcon.color]">
                <font-awesome-icon
                  :icon="['fal', getIcon.type]"
                ></font-awesome-icon>
              </div>
              <span class="application-status">{{ statusText }}</span>
            </div>
            <div v-if="waitingForConfirmation">
              <p>{{ $t('application-list.waiting-confirmation') }}</p>
            </div>
            <div v-if="waitingForPlacement">
              <p v-html="$t('application-list.status-info-sent')"></p>
            </div>
          </div>
        </div>

        <div class="info-wrapper">
          <div class="left"></div>
          <div class="right">
            <a
              href="#"
              class="decision-link"
              @click.prevent="navigateToDecisions"
              v-if="waitingForConfirmation"
              data-qa="btn-decisions"
            >
              <span>{{ $t('application-list.confirm-decision') }}</span>
              <font-awesome-icon
                :icon="['fal', 'arrow-right']"
                class="decision-link-icon"
              ></font-awesome-icon>
            </a>
          </div>
        </div>

        <div class="info-wrapper">
          <div class="left">{{ $t('general.created') }}</div>
          <div class="right">{{ application.createdDate | date }}</div>
        </div>
        <div class="info-wrapper">
          <div class="left">{{ $t('general.edited') }}</div>
          <div class="right">{{ application.modifiedDate | date }}</div>
        </div>
      </div>

      <confirm-modal
        :message="$t('application-list.confirm-modal.remove-text')"
        :header="$t('application-list.confirm-modal.remove-title')"
        :rejectText="$t('application-list.confirm-modal.cancel')"
        :acceptText="$t('application-list.confirm-modal.accept')"
        ref="confirmRemove"
      >
      </confirm-modal>
    </div>

    <div class="item-buttons has-text-right column is-narrow">
      <c-button
        v-if="canEdit"
        size="small"
        :primary="true"
        :borderless="true"
        :outlined="true"
        @click="onEdit"
        data-qa="btn-edit-application"
      >
        <font-awesome-icon
          :icon="['far', 'pencil']"
          size="2x"
        ></font-awesome-icon>
        <span class="item-buttons-label">{{
          $t('application-list.edit-application')
        }}</span>
      </c-button>
      <c-button
        v-if="canEdit && canDelete"
        size="small"
        :primary="true"
        :borderless="true"
        :outlined="true"
        @click="showConfirmModal"
      >
        <font-awesome-icon
          :icon="['far', 'trash']"
          size="lg"
        ></font-awesome-icon>
        <span class="item-buttons-label">{{
          $t('application-list.remove-application')
        }}</span>
      </c-button>
      <c-button
        v-if="!canEdit"
        size="small"
        :primary="true"
        :borderless="true"
        :outlined="true"
        @click="showPreview"
      >
        <font-awesome-icon
          :icon="['far', 'file-alt']"
          size="lg"
        ></font-awesome-icon>
        <span class="item-buttons-label">{{
          $t('application-list.show')
        }}</span>
      </c-button>
    </div>
  </li>
</template>

<script>
  import { mapGetters } from 'vuex'
  import ConfirmModal from '@/components/modal/confirm.vue'
  import { APPLICATION_STATUS } from '@evaka/enduser-frontend/src/constants'

  export default {
    data() {
      return {
        nameMissing: 'Lapsen nimi puuttuu hakemukselta'
      }
    },
    components: {
      ConfirmModal
    },
    props: ['application'],
    computed: {
      ...mapGetters(['applicationsLoaded', 'decisions', 'decisionsLoaded']),
      applicationType() {
        const types = this.$t('constants.application-type', {
          returnObjects: true
        })
        return this.application.type
          ? types[this.application.type.value].label.toUpperCase()
          : null
      },
      // application-list.application-type
      applicationTitle() {
        const titles = this.$t('application-list.application-title', {
          returnObjects: true
        })
        const isTransferApplication = this.application.transferApplication
        return this.application.type
          ? titles[`${this.application.type.value}${isTransferApplication ? '-transfer' : ''}`]
          : null
      },
      canEdit() {
        return (
          this.application.status.value === APPLICATION_STATUS.CREATED.value ||
          this.application.status.value === APPLICATION_STATUS.SENT.value
        )
      },
      canDelete() {
        return (
          this.application.status.value === APPLICATION_STATUS.CREATED.value
        )
      },
      statusText() {
        const statuses = this.$t('constants.application-status', {
          returnObjects: true
        })
        return statuses[this.application.status.value].label.toUpperCase()
      },
      waitingForPlacement() {
        return (
          this.application.status.value ===
          APPLICATION_STATUS.WAITING_PLACEMENT.value
        )
      },
      waitingForConfirmation() {
        return (
          this.application.status.value ===
          APPLICATION_STATUS.WAITING_CONFIRMATION.value
        )
      },
      decision() {
        let decision = null
        if (this.decisionsLoaded && this.applicationsLoaded) {
          decision = this.decisions.find(
            (d) => d.applicationId === this.application.id
          )
        }
        return decision
      },
      getIcon() {
        return {
          [APPLICATION_STATUS.CREATED.value]: { type: 'file', color: 'teal' },
          [APPLICATION_STATUS.SENT.value]: { type: 'envelope', color: 'blue' },
          [APPLICATION_STATUS.WAITING_PLACEMENT.value]: {
            type: 'play',
            color: 'blue'
          },
          [APPLICATION_STATUS.WAITING_UNIT_CONFIRMATION.value]: {
            type: 'play',
            color: 'blue'
          },
          [APPLICATION_STATUS.WAITING_DECISION.value]: {
            type: 'play',
            color: 'blue'
          },
          [APPLICATION_STATUS.WAITING_CONFIRMATION.value]: {
            type: 'gavel',
            color: 'orange'
          },
          [APPLICATION_STATUS.ACCEPTED.value]: {
            type: 'check',
            color: 'green'
          },
          [APPLICATION_STATUS.ACTIVE.value]: { type: 'check', color: 'green' },
          [APPLICATION_STATUS.REJECTED.value]: { type: 'times', color: 'grey' },
          [APPLICATION_STATUS.CANCELLED.value]: { type: 'trash', color: 'grey' },
          [APPLICATION_STATUS.TERMINATED.value]: { type: 'times', color: 'grey' }
        }[this.application.status.value]
      }
    },
    methods: {
      showConfirmModal() {
        this.$refs.confirmRemove.open().then(this.removeApplication, null)
      },
      removeApplication() {
        const m = this.$t('application-list.confirm-modal.success-title')
        this.$store
          .dispatch('removeApplication', {
            type: this.application.type.value,
            applicationId: this.application.id
          })
          .then(this.showSuccessMessage(m), () => {
            // noop
          })
      },
      showSuccessMessage(m) {
        return () => {
          this.$store.dispatch('modals/message', {
            type: 'success',
            title: m
          })
        }
      },
      onEdit() {
        this.$store.dispatch('editApplication', {
          id: this.application.id,
          type: this.application.type.value
        })
      },
      showPreview() {
        this.$store.dispatch('previewApplication', {
          id: this.application.id,
          type: this.application.type.value
        })
      },
      navigateToDecisions() {
        this.$store.dispatch('viewDecision', this.decision.id)
      }
    }
  }
</script>

<style lang="scss" scoped>
  .application-list-item {
    margin-bottom: 25px;
    border: 1px solid $grey;
    padding: 2rem 1.8rem 0 1.8rem;
    border-radius: 2px;

    h4.is-4 {
      font-weight: 300;
    }

    .columns {
      margin-bottom: 0;
    }

    .application-label {
      color: $grey-dark;
      font-size: 20px;
      font-weight: 600;
      line-height: 25px;
    }

    .application-item-details {
      margin-bottom: 1.5rem;
    }

    .application-info {
      padding-bottom: 0;
    }

    .application {
      &-info {
        padding-top: 0;
      }

      &-item-details {
        margin-bottom: 1.5rem;
      }

      &-label {
        color: $grey-dark;
        margin-bottom: 0.325rem;
        font-size: 1.325rem;
        font-weight: 500;
      }

      &-status {
        color: $grey-darker;
        font-size: 1rem;
        font-weight: 600;
        padding-left: 0.5rem;

        &-wrapper {
          display: flex;
        }
      }

      &-status-icon {
        height: 1.5rem;
        width: 1.5rem;
        border-radius: 50%;

        svg {
          color: $white;
          margin-left: 25%;
          vertical-align: -0.1em;
          width: 12.25px;
        }

        &.blue {
          background-color: $hard-blue;
        }
        &.teal {
          background-color: $rock-blue;
        }
        &.orange {
          background-color: $orange;
        }
        &.green {
          background-color: $green;
        }
        &.grey {
          background-color: $grey;
        }
      }
    }
  }

  .info-wrapper {
    display: flex;
    margin: 10px 0;
    font-size: 14px;

    .left {
      color: $grey-darker;
      font-size: 1rem;
      font-weight: 600;
      width: 150px;
    }

    .right {
      color: $grey-darker;
      font-size: 15px;
      max-width: 28rem;

      .decision-link {
        color: rgb(50, 115, 201);
        cursor: pointer;
        display: flex;
        font-size: 1rem;
        font-weight: 600;
        padding: 0.5rem 0;
      }

      .decision-link-icon {
        font-size: 1.5rem;
        margin-left: 0.25rem;
        padding: 0;
      }
    }

    .info-section {
      margin-top: 10px;
    }
  }

  .name-missing {
    font-size: 0.925rem;
    color: #999;
    font-style: italic;
  }

  .item-buttons {
    padding-top: 0;

    button {
      padding: 0;
    }

    &-label {
      font-size: 15px;
    }
  }
</style>
