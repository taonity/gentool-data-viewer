#!/bin/sh

set -eu

schedule_time="${BACKUP_SCHEDULE_TIME:-01:00}"
case "$schedule_time" in
  [0-1][0-9]:[0-5][0-9]|2[0-3]:[0-5][0-9]) ;;
  *)
    echo "BACKUP_SCHEDULE_TIME must use 24-hour HH:MM format." >&2
    exit 2
    ;;
esac

run_export() {
  sh /opt/backup/export-postgres.sh
}

run_export

while true; do
  now="$(date +%s)"
  next_run="$(date -d "today $schedule_time" +%s)"
  if [ "$next_run" -le "$now" ]; then
    next_run="$(date -d "tomorrow $schedule_time" +%s)"
  fi

  sleep_seconds=$((next_run - now))
  echo "Next PostgreSQL export is scheduled in $sleep_seconds seconds."
  sleep "$sleep_seconds"
  run_export
done