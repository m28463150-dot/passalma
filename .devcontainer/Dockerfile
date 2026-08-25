FROM mcr.microsoft.com/devcontainers/base:ubuntu-24.04

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libgl1 \
        libnss3 \
        libpulse0 \
        libx11-6 \
        libx11-xcb1 \
        libxcb1 \
        mesa-vulkan-drivers \
        qemu-kvm \
    && groupadd --system kvm 2>/dev/null || true \
    && (id -u vscode >/dev/null 2>&1 && usermod -aG kvm vscode || true) \
    && (id -u codespace >/dev/null 2>&1 && usermod -aG kvm codespace || true) \
    && rm -rf /var/lib/apt/lists/*
