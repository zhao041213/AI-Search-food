#!/bin/sh
set -eu

for directory in /app/data/finished-dish-reviews /app/data/user-avatars; do
    mkdir -p "$directory"
    chown app:app "$directory"
done

exec runuser -u app -- java -jar /app/app.jar
