# SPDX-FileCopyrightText: none
# SPDX-License-Identifier: CC0-1.0
# Start with a base image containing Java 17
FROM eclipse-temurin:17-jdk-focal

# Set environment variables
ENV VERTICLE_NAME=gov.nist.secauto.oscal.tools.server.OscalVerticle
ENV APP_HOME /usr/app

# Create the application directory
RUN mkdir -p $APP_HOME

# Set the working directory
WORKDIR $APP_HOME

# Copy the project files into the container
COPY . .

# Install Maven
RUN apt-get update && apt-get install -y maven

# Build the application
RUN mvn package

# Expose the port your application will run on
EXPOSE 8888

# Run the application using Maven
CMD ["mvn", "exec:java"]