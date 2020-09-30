<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <main class="decision-list container section">
    <div class="columns is-centered">
      <div class="is-three-quarters column">
        <c-title :size="2" class="has-text-centered decision-title" isGreyDark>
          {{ $t('decision-list.title') }}
        </c-title>

        <spinner v-if="!decisionsLoaded"></spinner>

        <section
          class="child-container"
          v-for="child in children"
          :key="child.id"
          v-else
        >
          <div class="child-details">
            <span class="child-icon">
              <font-awesome-icon
                :icon="['fal', 'child']"
                size="xs"
              ></font-awesome-icon>
            </span>
            <div class="child-name">
              <p>{{ child.firstName }} {{ child.lastName }}</p>
            </div>
          </div>

          <div class="decision-wrapper columns">
            <div class="child-decision column">
              <ul
                class="content"
                id="decision-list"
                v-if="getDecisions(child.id).length > 0"
              >
                <decision-list-item
                  v-for="(decision, index) in getDecisions(child.id)"
                  :key="index"
                  :decision="decision"
                  class="decision"
                  :data-decision-id="decision.id"
                />
              </ul>

              <div v-else class="no-decisions column">
                <span>{{ $t('decision-list.no-decisions') }}</span>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  </main>
</template>

<script lang="ts">
  import { mapGetters } from 'vuex'
  import DecisionListItem from '@/views/decisions/decision-list-item.vue'
  import _ from 'lodash'
  import Vue from 'vue'
  import { DecisionSummary } from '@/store/modules/decisions'
  import { UUID } from '@/types'

  export default Vue.extend({
    components: {
      DecisionListItem
    },
    computed: {
      ...mapGetters(['decisionsLoaded']),
      children() {
        return _.sortBy(this.$store.getters.children, (c) => c.id)
      },
      decisionsByChild(): Record<UUID, DecisionSummary[]> {
        if (this.$route.params.id) {
          return {}
        }
        const decisionSummaries: DecisionSummary[] = this.$store.getters
          .decisionSummaries
        const summaries = _.sortBy(decisionSummaries, (d) => d.sentDate)
        return _.groupBy(summaries, (d) => d.childId)
      }
    },
    created() {
      this.$store.dispatch('loadDecisions')
    },
    methods: {
      getDecisions(childId: UUID): DecisionSummary[] {
        return this.decisionsByChild[childId] || []
      }
    }
  })
</script>

<style lang="scss" scoped>
  .decision-title {
    margin-top: 2rem;
  }

  .decision-list {
    padding-top: 2rem;

    ul {
      margin-bottom: 1rem;
    }
  }

  .-title {
    max-width: 600px;
    color: #4d4d4d;
    margin: 0.5rem auto 2rem;
    line-height: 1.4;
  }

  .tag {
    margin-bottom: 1rem;
  }

  .child-container {
    padding: 2rem 0rem;

    &:not(:last-child) {
      border-bottom: 1px solid #dbdbdb;
    }

    .child-details {
      display: flex;
      text-align: left;
      margin-bottom: 1.25rem;
      padding-left: 2rem;

      .child-icon {
        position: relative;
        font-size: 3rem;
        line-height: 0;

        svg {
          color: $grey-dark;
        }

        i {
          padding: 0 0.625rem;
          text-align: center;
          color: #3273dc;
        }
      }

      .child-name {
        display: flex;
        flex-direction: column;
        justify-content: flex-end;

        p {
          color: $grey-dark;
          font-size: 1.3rem;
          line-height: 1.4;
          display: block;
          margin-bottom: 0;
          padding: 0 0.5rem 0.125rem 1rem;
        }
      }
    }
  }

  .no-decisions {
    border: 1px solid $grey-lighter;
    border-radius: 2px;
    color: #6d6d6d;
    font-size: 15px;
    font-style: italic;
    margin-bottom: 1rem;
    padding: 3rem;
  }
</style>
