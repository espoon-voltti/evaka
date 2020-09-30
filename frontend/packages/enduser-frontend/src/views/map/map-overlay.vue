<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <section>
    <a
      href="#"
      class="button is-info is-outlined map-info"
      @click.prevent="toggleNotification()"
    >
      <font-awesome-icon :icon="['fal', 'map-marker-alt']"></font-awesome-icon>
      {{ $t('general.info') }}
    </a>
    <div v-if="isVisible" class="notification-wrapper">
      <div class="overlay" @click="hideNotification"></div>

      <div class="notification">
        <button
          class="delete"
          @click="hideNotification"
          aria-label="close-notification"
        ></button>

        <h4 class="title is-4" v-html="$t('map-view.info-title')"></h4>

        <p v-html="$t('map-view.info-description')"></p>

        <ul class="unit-markers">
          <li
            v-for="(unit, index) in getUnits"
            :key="index"
            class="unit-marker"
          >
            <font-awesome-icon
              :icon="['fas', 'map-marker-alt']"
              size="2x"
              :class="unit.class"
            ></font-awesome-icon>
            <span>{{ unit.text }}</span>
          </li>
          <li class="unit-marker">
            <div>
              <font-awesome-layers class="fa-lg fa-fw">
                <font-awesome-icon
                  :icon="['fas', 'circle']"
                  transform="grow-12 right-2 down-4"
                  class="icon-dark-blue"
                />
                <font-awesome-icon
                  :icon="['fas', 'home']"
                  transform="right-2 down-4"
                  class="icon-white"
                />
              </font-awesome-layers>
            </div>
            <span>{{ this.$t('care-type.address-distance') }}</span>
          </li>
        </ul>
      </div>
    </div>
  </section>
</template>

<script>
  export default {
    data() {
      return {
        isVisible: true
      }
    },
    computed: {
      getUnits() {
        return [
          { class: 'icon-blue', text: this.$t('care-type.day-care') },
          { class: 'icon-green', text: this.$t('care-type.pre-school') },
          { class: 'icon-orange', text: this.$t('care-type.club') },
          { class: 'icon-red', text: this.$t('care-type.family-day-care') },
          {
            class: 'icon-dark-blue',
            text: this.$t('care-type.group-family-day-care')
          }
        ]
      }
    },
    methods: {
      hideNotification() {
        this.isVisible = ''
        localStorage.setItem('notification', this.isVisible)
      },
      toggleNotification() {
        this.isVisible = !this.isVisible
      }
    },
    mounted() {
      if (localStorage.getItem('notification') === '') {
        this.isVisible = localStorage.getItem('notification')
      }
    }
  }
</script>

<style lang="scss" scoped>
  .map-info.is-outlined {
    position: absolute;
    top: 1.5rem;
    left: 1rem;
    padding-left: 2rem;
    padding-right: 2rem;
    background-color: white;
    box-shadow: 0 0 5px 0 rgba(70, 70, 70, 0.35);
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s ease-out;

    :first-child {
      margin-right: 3px;
    }

    &:hover {
      background: rgba(white, 0.75);
    }
  }

  .notification-wrapper {
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    z-index: 101;
    width: 100%;
    height: 100%;

    @media all and (min-width: 960px) {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
    }
  }
  .overlay {
    position: absolute;
    display: block;
    width: 100%;
    height: 100%;
    top: 0;
    left: 0;
    background-color: rgba(black, 0.65);
    cursor: pointer;
  }

  .notification {
    z-index: 101;
    padding: 1.75rem 2.5rem 2rem 2rem;

    @media all and (min-width: 960px) {
      margin-top: -4rem;
      max-width: 60%;
    }

    ul {
      li {
        line-height: 2;
        img {
          max-height: 32px;
          vertical-align: middle;
        }
      }
    }

    .disclaimer {
      padding-top: 1.25rem;
    }
  }

  .unit-markers {
    margin-top: 2rem;

    .unit-marker {
      align-items: center;
      display: flex;
      margin-bottom: 0.8rem;

      & > svg {
        height: 42px;
        width: auto;
      }

      & > :nth-child(2) {
        margin-left: 1rem;
      }
      .icon-blue {
        color: tint($blue, 20%);
      }
      .icon-green {
        color: $observatory;
      }
      .icon-orange {
        color: $orange;
      }
      .icon-red {
        color: $red;
      }
    }
    .icon-white {
      color: $white;
    }
    .icon-dark-blue {
      color: shade($blue, 25%);
    }

    #distance-container {
      background-color: shade($blue, 25%);
      border-radius: 1rem;
      height: 2rem;
      width: 2rem;
    }
  }
</style>
