#!/usr/bin/env bash
set -e

echo "=========================================================="
echo " Starting Automated Android & Development Setup"
echo "=========================================================="

# 1. Install prerequisites
echo "--> Installing system packages..."
sudo apt-get update -y
sudo apt-get install -y --no-install-recommends \
    curl wget unzip zip git openjdk-17-jdk libglu1-mesa \
    clang cmake ninja-build pkg-config libgtk-3-dev

# 2. Setup Android SDK
echo "--> Setting up Android SDK..."
export ANDROID_HOME="/usr/local/android-sdk"
export ANDROID_SDK_ROOT="/usr/local/android-sdk"
sudo mkdir -p "${ANDROID_HOME}/cmdline-tools"

CMDLINE_VERSION="11076708_latest"
if [ ! -d "${ANDROID_HOME}/cmdline-tools/latest" ]; then
    echo "Downloading Android Command-line Tools..."
    curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_VERSION}.zip" -o /tmp/cmdline-tools.zip
    sudo unzip -q /tmp/cmdline-tools.zip -d "${ANDROID_HOME}/cmdline-tools"
    sudo mv "${ANDROID_HOME}/cmdline-tools/cmdline-tools" "${ANDROID_HOME}/cmdline-tools/latest"
    rm -f /tmp/cmdline-tools.zip
fi

sudo chown -R vscode:vscode "${ANDROID_HOME}"

export PATH="${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/build-tools/35.0.0"

echo "--> Accepting Android Licenses..."
yes | sdkmanager --licenses > /dev/null 2>&1 || true

echo "--> Installing Android SDK platform 35 and build tools..."
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"

# 3. Setup Flutter SDK
echo "--> Setting up Flutter SDK..."
if [ ! -d "/usr/local/flutter" ]; then
    sudo git clone -b stable --depth 1 https://github.com/flutter/flutter.git /usr/local/flutter
    sudo chown -R vscode:vscode /usr/local/flutter
fi
export PATH="/usr/local/flutter/bin:${PATH}"
git config --global --add safe.directory /usr/local/flutter
flutter precache --android || true
yes | flutter doctor --android-licenses > /dev/null 2>&1 || true

# 4. Setup Bun & OpenCode
echo "--> Setting up Bun & OpenCode AI..."
if ! command -v bun &> /dev/null; then
    curl -fsSL https://bun.sh/install | bash
fi

if ! command -v opencode &> /dev/null; then
    sudo npm install -g opencode-ai
fi

# 5. Pre-cache Gradle for Pixel-Pal
echo "--> Pre-caching Gradle wrapper and dependencies..."
if [ -f "/workspaces/Pixel-Pal-/gradlew" ]; then
    chmod +x /workspaces/Pixel-Pal-/gradlew
    cd /workspaces/Pixel-Pal-
    ./gradlew --version
fi

# 6. Make environment variables permanent for all shells
ENV_FILE="/etc/profile.d/android-dev.sh"
sudo tee "$ENV_FILE" > /dev/null << 'EOF'
export ANDROID_HOME="/usr/local/android-sdk"
export ANDROID_SDK_ROOT="/usr/local/android-sdk"
export PATH="$PATH:/usr/local/android-sdk/cmdline-tools/latest/bin:/usr/local/android-sdk/platform-tools:/usr/local/android-sdk/build-tools/35.0.0:/usr/local/flutter/bin:$HOME/.bun/bin"
EOF
sudo chmod +x "$ENV_FILE"

# Append to current user bashrc / zshrc as well
cat << 'EOF' >> "$HOME/.bashrc"
export ANDROID_HOME="/usr/local/android-sdk"
export ANDROID_SDK_ROOT="/usr/local/android-sdk"
export PATH="$PATH:/usr/local/android-sdk/cmdline-tools/latest/bin:/usr/local/android-sdk/platform-tools:/usr/local/android-sdk/build-tools/35.0.0:/usr/local/flutter/bin:$HOME/.bun/bin"
EOF

echo "=========================================================="
echo " Automated Setup Completed Successfully!"
echo "=========================================================="
