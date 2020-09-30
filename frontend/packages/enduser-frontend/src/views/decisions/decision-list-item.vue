<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <li class="has-text-left decision-list-item">
    <div class="decision-item-details">
      <h5 class="decision-label">{{ decisionLabel }}</h5>
    </div>
    <div class="columns">
      <div class="decision-info column">
        <div class="info-wrapper">
          <div class="left">{{ $t('decision-list-item.sentDate') }}</div>
          <div class="right">{{ decision.sentDate | date }}</div>
        </div>
        <div class="info-wrapper" v-if="!decisionPending">
          <div class="left">{{ $t('decision-list-item.confirmedDate') }}</div>
          <div class="right">{{ decision.resolved | date }}</div>
        </div>
        <div class="info-wrapper">
          <div class="left">{{ $t('decision-list-item.status') }}</div>
          <div class="right">
            <div class="decision-status-wrapper">
              <div :class="['decision-status-icon', iconColor]">
                <font-awesome-icon
                  :icon="['fal', iconType]"
                ></font-awesome-icon>
              </div>
              <span class="decision-status">{{ decisionStatusText }}</span>
            </div>
            <div v-if="decisionPending">
              <p class="decision-description">{{ decisionDescription }}</p>
              <p>
                <b>{{ $t('decision-list-item.open-info') }}</b>
              </p>
              <c-button
                @click="onViewDecision"
                :primary="true"
                data-qa="btn-decisions"
              >
                {{ $t('decision-list-item.open') }}
              </c-button>
            </div>
          </div>
        </div>
      </div>
      <div
        class="item-buttons is-one-third is-4-desktop column"
        v-if="!decisionPending"
      >
        <c-button
          @click="onViewDecision"
          :primary="true"
          :borderless="true"
          size="small"
        >
          <font-awesome-icon
            :icon="['far', 'file-alt']"
            size="lg"
          ></font-awesome-icon>
          {{ $t('decision-list-item.show-decision') }}
        </c-button>
      </div>
    </div>
  </li>
</template>

<script lang="ts">
  import { LabeledValue } from '@/constants'
  import Vue from 'vue'
  import { DecisionSummary } from '@/store/modules/decisions'

  export default Vue.extend({
    props: {
      decision: {
        type: Object
      } as Vue.PropOptions<DecisionSummary>
    },
    computed: {
      decisionType(): DecisionSummary['type'] {
        return this.decision.type
      },
      decisionStatus(): DecisionSummary['status'] {
        return this.decision.status
      },
      decisionAccepted(): boolean {
        return this.decisionStatus === 'ACCEPTED'
      },
      decisionPending(): boolean {
        return this.decisionStatus === 'PENDING'
      },
      decisionRejected(): boolean {
        return this.decisionStatus === 'REJECTED'
      },
      decisionLabel(): LabeledValue | undefined {
        switch (this.decisionType) {
          case 'CLUB':
            return this.$t('decision-list-item.decision-type.club')
          case 'DAYCARE':
          case 'DAYCARE_PART_TIME':
            return this.$t('decision-list-item.decision-type.daycare')
          case 'PRESCHOOL':
            return this.$t('decision-list-item.decision-type.preschool')
          case 'PRESCHOOL_DAYCARE':
            return this.$t(
              'decision-list-item.decision-type.connecting-daycare'
            )
          case 'PREPARATORY_EDUCATION':
            return this.$t('decision-list-item.decision-type.preparatory')
          default:
            return undefined
        }
      },
      decisionDescription(): LabeledValue | undefined {
        switch (this.decisionType) {
          case 'CLUB_DECISION':
            return this.$t('decision-view.text_club')
          case 'DAYCARE':
          case 'DAYCARE_PART_TIME':
            return this.$t('decision-view.text_daycare')
          case 'PRESCHOOL':
          case 'PRESCHOOL_DAYCARE':
          case 'PREPARATORY_EDUCATION':
            return this.$t('decision-view.text_preschool_general')
          default:
            return undefined
        }
      },
      decisionStatusText(): string {
        const status = this.decisionRejected
          ? this.$t('decision-list-item.status-cancelled')
          : this.decisionAccepted
          ? this.$t('decision-list-item.status-accepted')
          : this.$t('decision-list-item.status-pending')
        return status.toUpperCase()
      },
      iconColor(): string {
        return this.decisionPending
          ? 'orange'
          : this.decisionAccepted
          ? 'green'
          : 'grey'
      },
      iconType(): string {
        return this.decisionPending
          ? 'gavel'
          : this.decisionAccepted
          ? 'check'
          : 'times'
      }
    },
    methods: {
      onViewDecision() {
        this.$store.dispatch('viewDecision', this.decision.id)
      }
    }
  })
</script>

<style lang="scss" scoped>
  .decision-list-item {
    margin-bottom: 1rem;
    border: 1px solid $grey;
    padding: 1rem 2rem 2rem 2rem;
    border-radius: 2px;

    .decision {
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
          vertical-align: -0.2em;
          width: 12.25px;
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

      &-description {
        margin-top: 2rem;
      }
    }
  }

  .decision-info {
    .info-wrapper {
      display: flex;
      margin: 10px 0;
      font-size: 14px;

      .left {
        font-weight: 600;
        min-width: 150px;
      }

      .left,
      .right {
        font-size: 1rem;
      }

      @include onMobile() {
        flex-direction: column;

        .left,
        .right {
          width: auto;
          display: block;
          max-width: none;
        }
      }
    }
  }

  .name-missing {
    font-size: 0.925rem;
    color: #999;
    font-style: italic;
  }

  .item-buttons {
    display: flex;
    align-items: flex-end;
    flex-direction: column;
    align-self: flex-end;

    .tag:not(:last-of-type) {
      margin-bottom: 0.425rem;
    }

    @include onMobile() {
      align-items: center;
      justify-content: center;
      flex-direction: row;

      .tag:not(:last-of-type) {
        margin: 0 0.425rem 0 0;
      }
    }
  }
</style>
