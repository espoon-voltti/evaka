# SPDX-FileCopyrightText: 2017-2025 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# shellcheck shell=bash

# Shell environment for eVaka multi-instance support
# Source this file in your shell: source /path/to/evaka-env.sh
# Then use: evaka use 1, evaka deactivate, evaka status

_evaka_calculate_ports() {
    local instance=$1
    export EVAKA_INSTANCE=$instance
    export EVAKA_DB_PORT=$((5432 + instance))
    export EVAKA_REDIS_PORT=$((6379 + instance))
    export EVAKA_S3_PORT=$((9876 + instance))
    export EVAKA_SFTP_PORT=$((2222 + instance))
    export EVAKA_IDP_PORT=$((9090 + instance))
    export EVAKA_APIGW_PORT=$((3000 + instance))
    export EVAKA_BACK_PORT=$((8888 + instance))
    export EVAKA_FRONTEND_PORT=$((9099 + instance))
    export EVAKA_DATABASE_PORT=$EVAKA_DB_PORT

    export COMPOSE_PROJECT_NAME="evaka-${instance}"
    export PM2_HOME="${HOME}/.pm2-evaka-${instance}"
}

_evaka_clear_env() {
    unset EVAKA_INSTANCE
    unset EVAKA_DB_PORT
    unset EVAKA_REDIS_PORT
    unset EVAKA_S3_PORT
    unset EVAKA_SFTP_PORT
    unset EVAKA_IDP_PORT
    unset EVAKA_APIGW_PORT
    unset EVAKA_BACK_PORT
    unset EVAKA_FRONTEND_PORT
    unset EVAKA_DATABASE_PORT
    unset COMPOSE_PROJECT_NAME
    unset PM2_HOME
}

_evaka_show_status() {
    if [ -n "${EVAKA_INSTANCE:-}" ] && [ "$EVAKA_INSTANCE" != "0" ]; then
        echo "eVaka instance: $EVAKA_INSTANCE"
        echo ""
        echo "Ports:"
        echo "  PostgreSQL: $EVAKA_DB_PORT"
        echo "  Redis:      $EVAKA_REDIS_PORT"
        echo "  S3-mock:    $EVAKA_S3_PORT"
        echo "  SFTP:       $EVAKA_SFTP_PORT"
        echo "  dummy-idp:  $EVAKA_IDP_PORT"
        echo "  API GW:     $EVAKA_APIGW_PORT"
        echo "  Backend:    $EVAKA_BACK_PORT"
        echo "  Frontend:   $EVAKA_FRONTEND_PORT"
        echo ""
        echo "Docker Compose project: $COMPOSE_PROJECT_NAME"
        echo "PM2 home:              $PM2_HOME"
    else
        echo "eVaka instance: default (no instance active)"
    fi
}

_evaka_update_prompt() {
    if [ -n "${EVAKA_INSTANCE:-}" ] && [ "$EVAKA_INSTANCE" != "0" ]; then
        if [ -z "${_EVAKA_OLD_PS1:-}" ]; then
            export _EVAKA_OLD_PS1="$PS1"
        fi
        export PS1="(evaka:$EVAKA_INSTANCE) ${_EVAKA_OLD_PS1}"
    elif [ -n "${_EVAKA_OLD_PS1:-}" ]; then
        export PS1="$_EVAKA_OLD_PS1"
        unset _EVAKA_OLD_PS1
    fi
}

evaka() {
    local cmd="${1:-}"

    case "$cmd" in
        use)
            local instance="${2:-}"
            if [ -z "$instance" ]; then
                echo "Deactivating eVaka instance environment..."
                _evaka_clear_env
                _evaka_update_prompt
                echo "Done. Using default instance."
                return 0
            fi

            if ! [[ "$instance" =~ ^[0-9]+$ ]]; then
                echo "Error: Instance must be a number"
                return 1
            fi

            if [ "$instance" -eq 0 ]; then
                echo "Deactivating eVaka instance environment..."
                _evaka_clear_env
                _evaka_update_prompt
                echo "Done. Using default instance."
                return 0
            fi

            echo "Activating eVaka instance $instance..."
            _evaka_calculate_ports "$instance"
            _evaka_update_prompt
            echo ""
            _evaka_show_status
            ;;

        deactivate)
            echo "Deactivating eVaka instance environment..."
            _evaka_clear_env
            _evaka_update_prompt
            echo "Done. Using default instance."
            ;;

        status)
            _evaka_show_status
            ;;

        *)
            echo "Usage: evaka <command>"
            echo ""
            echo "Commands:"
            echo "  use <n>      Activate instance n (sets all environment variables)"
            echo "  use          Deactivate (return to default instance)"
            echo "  deactivate   Deactivate (return to default instance)"
            echo "  status       Show current instance and port configuration"
            echo ""
            echo "After activation, docker compose and pm2 commands automatically"
            echo "use the correct project name and PM2 home directory."
            echo ""
            echo "Example:"
            echo "  evaka use 1"
            echo "  docker compose up -d    # uses evaka-1 project"
            echo "  pm2 start               # uses ~/.pm2-evaka-1"
            echo "  evaka deactivate"
            ;;
    esac
}
