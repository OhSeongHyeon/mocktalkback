#!/usr/bin/env bash
set -euo pipefail

ENV_FILE=".env.dev"
PROFILE="dev"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --env-file)
      ENV_FILE="$2"
      shift 2
      ;;
    --profile)
      PROFILE="$2"
      shift 2
      ;;
    -h|--help)
      cat <<'EOF'
사용법:
  ./scripts/dev-run.sh [--env-file .env.dev] [--profile dev]
EOF
      exit 0
      ;;
    *)
      echo "알 수 없는 옵션: $1" >&2
      exit 1
      ;;
  esac
done

# 스크립트 위치 기준으로 프로젝트 루트를 계산한다.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if [[ "$ENV_FILE" = /* ]]; then
  ENV_PATH="$ENV_FILE"
else
  ENV_PATH="$REPO_ROOT/$ENV_FILE"
fi

if [[ ! -f "$ENV_PATH" ]]; then
  echo "환경변수 파일을 찾을 수 없습니다: $ENV_PATH" >&2
  exit 1
fi

# .env 형식(KEY=VALUE)을 현재 프로세스 환경변수로 주입한다.
while IFS= read -r RAW_LINE || [[ -n "$RAW_LINE" ]]; do
  LINE="${RAW_LINE%$'\r'}"

  # 앞뒤 공백을 제거한다.
  LINE="${LINE#"${LINE%%[![:space:]]*}"}"
  LINE="${LINE%"${LINE##*[![:space:]]}"}"

  [[ -z "$LINE" ]] && continue
  [[ "${LINE:0:1}" == "#" ]] && continue
  [[ "$LINE" != *"="* ]] && continue

  KEY="${LINE%%=*}"
  VALUE="${LINE#*=}"

  KEY="${KEY#"${KEY%%[![:space:]]*}"}"
  KEY="${KEY%"${KEY##*[![:space:]]}"}"
  VALUE="${VALUE#"${VALUE%%[![:space:]]*}"}"
  VALUE="${VALUE%%[[:space:]]#*}"
  VALUE="${VALUE%"${VALUE##*[![:space:]]}"}"

  if [[ -z "$KEY" ]]; then
    continue
  fi

  # 따옴표로 감싼 값은 감싼 따옴표를 제거한다.
  if [[ "${#VALUE}" -ge 2 ]]; then
    if [[ "${VALUE:0:1}" == "\"" && "${VALUE: -1}" == "\"" ]]; then
      VALUE="${VALUE:1:${#VALUE}-2}"
    elif [[ "${VALUE:0:1}" == "'" && "${VALUE: -1}" == "'" ]]; then
      VALUE="${VALUE:1:${#VALUE}-2}"
    fi
  fi

  export "$KEY=$VALUE"
done < "$ENV_PATH"

export SPRING_PROFILES_ACTIVE="$PROFILE"

cd "$REPO_ROOT"
./gradlew --no-daemon bootRun --args="--spring.profiles.active=$PROFILE"
