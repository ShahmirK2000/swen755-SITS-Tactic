#!/bin/bash

# This file is used to place orders and is also how we cause the order service to die and the heartbeat detector to act.
# To induce a failure, see usage below

# Usage: ./place_order.sh PRODUCT QUANTITY
# Induce OrderService Failure: ./place_order.sh __KILL_SERVICE__ kill

# Update these paths for different order file location
DEFAULT_FIFO_WIN="C:\\temp\\orders-House-of-Zohran.fifo"
DEFAULT_FIFO_UNIX="/tmp/orders-House-of-Zohran.fifo"

if [ $# -ne 2 ]; then
    echo "Error, check order.sh for usage"
    exit 1
fi

PRODUCT="$1"
QUANTITY="$2"

OS_TYPE=$(uname | tr '[:upper:]' '[:lower:]')
IS_WINDOWS=false
IS_LINUX=false
IS_MAC=false

if [[ "$OS_TYPE" == *"linux"* ]]; then
    IS_LINUX=true
elif [[ "$OS_TYPE" == *"darwin"* ]]; then
    IS_MAC=true
elif [[ "$OS_TYPE" == *"mingw"* || "$OS_TYPE" == *"cygwin"* ]]; then
    IS_WINDOWS=true
fi

if [ "$PRODUCT" = "__KILL_SERVICE__" ]; then
    echo "Terminating OrderService processes."

    if $IS_WINDOWS; then
        echo "Detected Windows."
        taskkill //F //IM java.exe //T | grep OrderService || true
    else
        echo "Detected Unix."
        PIDS=$(pgrep -f "OrderService" || true)
        if [ -z "$PIDS" ]; then
            echo "No OrderService process found."
            exit 0
        fi
        for pid in $PIDS; do
            echo "Killing PID $pid"
            kill -9 "$pid" && echo "Killed $pid"
        done
    fi
    exit 0
fi

if $IS_WINDOWS; then
    FIFO_PATH="$DEFAULT_FIFO_WIN"
else
    FIFO_PATH="$DEFAULT_FIFO_UNIX"
fi

echo "ORDER:$PRODUCT:$QUANTITY" > "$FIFO_PATH"
echo "Order sent: $PRODUCT x $QUANTITY"