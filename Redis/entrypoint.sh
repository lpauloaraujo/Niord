#!/bin/sh
# entrypoint.sh


#Setups password

redis-server redis.conf --requirepass "${REDIS_PASSWORD}"
