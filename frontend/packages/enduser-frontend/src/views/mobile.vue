<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="menu-mobile-wrapper">
    <div class="menu-bar" @click="toggle">
      <router-link to="/" exact>
        <div class="nav-logoWrapper">
          <img class="logo" src="../assets/espoo-logo.svg" alt="Espoo logo" />
        </div>
      </router-link>

      <div class="menu-actionButtons">
        <a
          v-if="isLoggedIn"
          id="logout-btn-mobile"
          type="button"
          class="logout-btn"
          :href="logoutAddress"
        >
          <div class="icon round">
            <font-awesome-icon :icon="['fal', 'sign-out']"></font-awesome-icon>
          </div>
          {{ $t('menu.logout') }}
        </a>

        <a
          id="login-btn-mobile"
          class="login-btn"
          :href="loginAddress"
          v-if="!isLoggedIn"
        >
          <font-awesome-icon :icon="['fal', 'sign-in']"></font-awesome-icon>
          <span>{{ $t('menu.login') }}</span>
        </a>
      </div>

      <span class="nav-toggle">
        &#9776;
      </span>
    </div>

    <div class="menu-overlay" v-show="isOpen" ref="menuOverlay" />
    <div class="menu-mobile" v-show="isOpen">
      <div class="menu-header">
        <div v-if="isLoggedIn" class="menu-user">
          <font-awesome-icon :icon="['far', 'user']"></font-awesome-icon>
          <span>{{ userName }}</span>
        </div>
        <div v-else class="menu-anon">
          <h1>{{ $t('menu.title') }}</h1>
          <p>{{ $t('menu.description') }}</p>

          <a class="login-btn" :href="loginAddress" v-if="!isLoggedIn">
            <font-awesome-icon :icon="['fal', 'sign-in']"></font-awesome-icon>
            <span>{{ $t('menu.login') }}</span>
          </a>
        </div>
        <div v-if="!isLoggedIn" class="menu-privacy-link">
          <a :href="$t('footer.privacy-policy-link')" target="_blank">
            {{ $t('footer.privacy-policy-label') }}
          </a>
        </div>
      </div>
      <div class="menu-container">
        <div class="menu-item languages-menu">
          <span
            @click="setlanguage(language.value)"
            class="language-item"
            :class="{
              'selected-language': selectedLanguage === language.value
            }"
            v-for="(language, index) in languages"
            :key="index"
            >{{ language.label }}</span
          >
        </div>

        <div v-if="isLoggedIn">
          <router-link
            :key="index"
            v-for="(item, index) in model"
            exact
            active-class="is-active"
            class="menu-item"
            :to="{ path: item.path }"
          >
            <font-awesome-icon :icon="item.icon"></font-awesome-icon>
            {{ item.label | uppercase }}
          </router-link>
          <a
            v-if="showCitizenFrontendLink"
            class="menu-item"
            :href="newDecisionsUrl"
            data-qa="nav-decisions"
          >
            <font-awesome-icon :icon="['fal', 'gavel']"></font-awesome-icon>
            {{ $t('menu.routes.newDecisions') | uppercase }}
          </a>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  import { mapGetters } from 'vuex'
  import { config } from '@evaka/enduser-frontend/src/config'

  export default {
    props: {
      loginAddress: {
        type: String,
        required: true
      },
      logoutAddress: {
        type: String,
        required: true
      },
      model: {
        type: Array,
        required: true
      },
      citizenFrontendUrl: {
        type: String,
        required: true
      }
    },
    data() {
      return {
        closeMenuListener: false,
        isOpen: false
      }
    },
    computed: {
      ...mapGetters(['isLoggedIn', 'userName']),
      selectedLanguage() {
        return this.$i18n.locale
      },
      languages() {
        return Object.values(this.$t('menu.language', { returnObjects: true }))
      },
      showCitizenFrontendLink() {
        return this.isLoggedIn && config.feature.citizenFrontend
      },
      newDecisionsUrl() {
        return `${this.citizenFrontendUrl}/decisions`
      }
    },
    methods: {
      setlanguage(language) {
        this.$i18n.locale = language
        localStorage.setItem('enduser_language', language)
      },
      toggle() {
        this.isOpen = !this.isOpen
      },
      close() {
        this.isOpen = false
      }
    },
    mounted() {
      const overlay = this.$refs.menuOverlay
      overlay.addEventListener('touchend', this.close)
      overlay.addEventListener('mouseup', this.close)
    },
    beforeDestroy() {
      const overlay = this.$refs.menuOverlay
      overlay.removeEventListener('touchend', this.close)
      overlay.removeEventListener('mouseup', this.close)
    },
    watch: {
      $route() {
        this.close()
      }
    }
  }
</script>

<style lang="scss" scoped>
  .menu-mobile-wrapper {
    display: none;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;

    @include onMobile() {
      display: block;
    }
  }

  .menu-bar {
    display: flex;
    flex-direction: row;
    @include onMobile() {
      align-items: center;
      justify-content: space-between;
    }
    height: 52px;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    z-index: 800;

    background-color: $blue;
    box-shadow: 0 0 20px 0 rgba(0, 0, 0, 0.3);

    .nav-logoWrapper {
      height: 52px;
      margin-left: 1em;

      background-color: white;

      @include onMobile() {
        background: none;
      }

      img {
        height: 100%;
      }
    }

    .nav-toggle {
      position: absolute;
      @include onMobile() {
        position: relative;
      }
      right: 0;
      top: -2px;
      width: 52px;
      height: 52px;
      display: flex;
      flex-direction: row;
      justify-content: center;
      align-items: center;
      color: white;
      font-size: 26px;
      cursor: pointer;
    }
  }

  .menu-mobile {
    position: absolute;
    top: 51px;
    left: 0;
    right: 0;
    display: flex;
    flex-direction: column;

    z-index: 700;

    background-color: $dark-blue-transparent;
    box-shadow: 0 0 20px 0 rgba(0, 0, 0, 0.3);

    .languages-menu {
      justify-content: space-evenly;

      .language-item {
        cursor: pointer;
      }

      .selected-language:before {
        content: '✔ ';
      }
    }
  }

  .menu-header {
    padding: 2rem 0 0 0;
    @include onMobile() {
      padding: 0;
    }
  }

  .menu-anon {
    @include onMobile() {
      color: white;
      text-align: center;
      h1 {
        font-size: 120%;
        margin: 3rem 3rem 1.5rem;
      }
      p {
        font-size: 0.8rem;
        margin: 1.5rem 4rem;
      }
      .login-btn {
        border-radius: 0px;
        padding: 1rem;
        background: white;
        border: none;
        color: $blue;
        text-transform: uppercase;
        font-weight: 600;
        display: inline-block;
        margin-bottom: 2rem;
      }
    }
  }

  .menu-actionButtons {
    text-align: center;
    @include onMobile() {
      font-size: 0.8rem;
    }

    .login-btn,
    .logout-btn {
      border: none;
      background-color: transparent;
      color: white;
      display: flex;
      flex-direction: row;
      align-items: center;
      font-size: 0.8rem;
      height: 52px;
      padding-left: 1rem;
      padding-right: 1rem;
      cursor: pointer;
    }
  }

  .menu-user {
    color: white;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    margin: 1.5rem 0 1em;
  }

  .menu-container {
    top: 52px;
  }

  .menu-item {
    display: flex;
    flex-direction: row;
    align-items: center;

    border: none;
    background-color: transparent;
    color: white;
    border-top: 1px solid white;
    width: 100%;

    height: 52px;
    padding-left: 1rem;
    padding-right: 1rem;
    font-size: 1rem;

    &.is-active {
      background-color: $dark-blue;
    }
  }

  svg {
    font-size: 1rem;
    margin-right: 8px;
  }

  .menu-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
  }

  .menu-privacy-link {
    text-align: center;
    margin: 0 0 20px 0;
    font-size: 0.8rem;
    a {
      color: $link-invert;
    }
  }
</style>
