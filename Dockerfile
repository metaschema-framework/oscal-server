# SPDX-License-Identifier: CC0-1.0
FROM eclipse-temurin:17-jre-jammy

# Set environment variables
ENV APP_HOME=/usr/app
ENV PORT=8888

# Create app directory
WORKDIR $APP_HOME

# Install curl and unzip for downloading and extracting the release
RUN apt-get update && apt-get install -y curl unzip \
    && rm -rf /var/lib/apt/lists/*

# Download and extract the latest release
RUN RELEASE_DATA=$(curl -s https://api.github.com/repos/metaschema-framework/oscal-server/releases/latest) \
    && DOWNLOAD_URL=$(echo "$RELEASE_DATA" | grep -o 'https://.*oscal-server.*\.zip' | head -n1) \
    && curl -L -o oscal-server.zip "$DOWNLOAD_URL" \
    && unzip oscal-server.zip \
    && rm oscal-server.zip \
    # Make the executable runnable
    && find . -type f -path "*/bin/oscal-server" -exec chmod +x {} \;

# Find and set the executable path for CMD
RUN OSCAL_SERVER_PATH=$(find . -type f -path "*/bin/oscal-server") \
    && echo "#!/bin/sh" > /entrypoint.sh \
    && echo "exec $OSCAL_SERVER_PATH" >> /entrypoint.sh \
    && chmod +x /entrypoint.sh

# Expose the application port
EXPOSE $PORT

# Run the application
CMD ["/entrypoint.sh"]
