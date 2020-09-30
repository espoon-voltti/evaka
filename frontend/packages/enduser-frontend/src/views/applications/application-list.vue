<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <main class="application-list container section">
    <div class="columns is-mobile is-centered">
      <div class="column is-8-desktop" data-qa="children-wrapper">
        <c-title :size="2" :isGreyDark="true" class="has-text-centered">{{
          $t('application-list.title')
        }}</c-title>
        <p>{{ $t('application-list.data-source-info') }}</p>
        <spinner v-if="!applicationsLoaded"></spinner>
        <section
          class="child-container"
          v-for="child in sortedChildren"
          :key="child.id"
          v-else
        >
          <div class="child-details-wrapper">
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
            <div class="add-application">
              <c-button
                :primary="true"
                :borderless="true"
                :outlined="true"
                @click="selectApplicationType(child.id)"
                >{{ $t('application-list.new-application') }}
                <span class="icon">
                  <font-awesome-icon
                    :icon="['far', 'plus']"
                    size="lg"
                  ></font-awesome-icon>
                </span>
              </c-button>
            </div>
          </div>

          <ul
            class="content"
            id="application-list"
            v-if="
              applicationsByChild[child.id] &&
                applicationsByChild[child.id].length
            "
          >
            <application-list-item
              v-for="(application, index) in applicationsByChild[child.id]"
              :key="index"
              :application="application"
              class="application"
              :data-application-id="application.id"
            />
          </ul>

          <div class="no-applications column" v-else>
            <span>{{ $t('application-list.no-applications') }}</span>
          </div>
        </section>
      </div>
    </div>

    <select-application-type ref="selectApplicationType" />
  </main>
</template>

<script>
  import { mapGetters } from 'vuex'
  import ApplicationListItem from '@/views/applications/application-list-item.vue'
  import _ from 'lodash'

  import SelectApplicationType from './select-application-type'
  import { APPLICATION_TYPE } from '../../constants'
  import { config } from '@evaka/enduser-frontend/src/config'

  export default {
    components: {
      ApplicationListItem,
      SelectApplicationType
    },
    computed: {
      ...mapGetters(['applications', 'applicationsLoaded', 'children']),
      sortedChildren() {
        return _.sortBy(this.children, ['age'])
      },
      applicationsByChild() {
        const applicationsByChild = _.groupBy(this.applications, 'child.id')
        Object.keys(applicationsByChild).forEach((key) => {
          _.sortBy(applicationsByChild[key], ['createdDate'])
        })

        return applicationsByChild
      }
    },
    created() {
      this.$store.dispatch('loadApplications')
    },
    methods: {
      async newApplication(payload) {
        const removeLoader = await this.$store.dispatch('loader/add')
        await this.$store.dispatch('newApplication', payload)
        removeLoader()
      },
      selectApplicationType(childId) {
        if (config.feature.selectApplicationType === true) {
          this.$refs.selectApplicationType.open(childId).then(
            ({ type }) => {
              this.newApplication({
                childId,
                type
              })
            },
            () => {
              // noop
            }
          )
        } else {
          this.newApplication({
            childId,
            type: APPLICATION_TYPE.CLUB.value
          })
        }
      }
    }
  }
</script>

<style lang="scss" scoped>
  .application-list {
    max-width: 100vw;
    padding-top: 2rem;

    ul {
      margin-bottom: 1rem;
    }
  }

  .title {
    max-width: 43.75rem;
    margin: 2rem auto 2rem;
  }

  .tag {
    margin-bottom: 1rem;
  }

  .child-container {
    padding: 2rem 0rem;

    .child-details-wrapper {
      align-items: center;
      display: flex;
      justify-content: space-between;
      margin-bottom: 1.25rem;

      @include onMobile() {
        flex-direction: column;

        .add-application {
          display: flex;
          justify-content: flex-end;
        }
      }
    }

    .child-details {
      display: flex;
      text-align: left;
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

    .add-application {
      height: 3em;

      .icon {
        color: #ffffff;
        margin-left: 15px !important;
        padding: 20px;
        background-color: $blue;
        border-radius: 100%;
      }

      button {
        height: 3.4em;
      }
    }
  }

  .no-applications {
    border: 1px solid $grey-lighter;
    border-radius: 2px;
    color: $grey-dark;
    font-size: 15px;
    font-style: italic;
    margin-bottom: 1rem;
    padding: 3rem;
  }
</style>
