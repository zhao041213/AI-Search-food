#!/usr/bin/env bash
set -euo pipefail

JRE_LINK="/opt/java/jre17"
JRE_ARCHIVE="/tmp/temurin-jre17.tar.gz"
JRE_URL="https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jre/hotspot/normal/eclipse"

mkdir -p /opt/java /opt/ai-search-food /etc/ai-search-food

if ! id ai-search-food >/dev/null 2>&1; then
    useradd --system --home /opt/ai-search-food --shell /sbin/nologin ai-search-food
fi

if [ ! -x "${JRE_LINK}/bin/java" ]; then
    if [ ! -s "${JRE_ARCHIVE}" ] || [ "$(stat -c%s "${JRE_ARCHIVE}")" -lt 40000000 ]; then
        curl -fL "${JRE_URL}" -o "${JRE_ARCHIVE}"
    fi
    tar -xzf "${JRE_ARCHIVE}" -C /opt/java
    JRE_DIR="$(find /opt/java -maxdepth 1 -type d -name 'jdk-17*-jre' | sort | tail -n 1)"
    if [ -z "${JRE_DIR}" ]; then
        echo "JRE extraction failed: no jdk-17*-jre directory found" >&2
        exit 1
    fi
    ln -sfn "${JRE_DIR}" "${JRE_LINK}"
fi

if [ ! -f /etc/ai-search-food/ai-search-food.env ]; then
    cat > /etc/ai-search-food/ai-search-food.env <<'EOF'
DEEPSEEK_API_KEY=
DEEPSEEK_MODEL=deepseek-v4-flash
EOF
fi

chown -R ai-search-food:ai-search-food /opt/ai-search-food
chmod 750 /opt/ai-search-food
chmod 640 /etc/ai-search-food/ai-search-food.env

"${JRE_LINK}/bin/java" -version
