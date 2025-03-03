# Makefile for OSCAL Server Release Assembly

# Default target
.PHONY: all
all: init-submodules build-liboscal build-metaschema clean build-client compile package

# Initialize and update all submodules (including nested ones)
.PHONY: init-submodules
init-submodules:
	@echo "Initializing and updating all submodules..."
	git submodule update --init --recursive
	cd lib/liboscal-java && git submodule update --init --recursive
	cd lib/metaschema-java && git submodule update --init --recursive


# Build and install liboscal-java
.PHONY: build-liboscal
build-liboscal:
	@echo "Building and installing liboscal-java..."
	cd lib/liboscal-java 
	mvn clean install -DskipTests 

# Build and install metaschema-java
.PHONY: build-metaschema
build-metaschema:
	@echo "Building and installing metaschema-java..."
	cd lib/metaschema-java && \
	mvn clean install -DskipTests 

# Clean the main project
.PHONY: clean
clean:
	@echo "Cleaning the main project..."
	mvn clean

# Build the client
.PHONY: build-client
build-client:
	@echo "Building the client..."
	cd client && npm ci && npm run build && cd ..

# Compile the main project with the correct versions
.PHONY: compile
compile:
	@echo "Compiling the main project..."
	. ./.env-liboscal && . ./.env-metaschema && \
	mvn compile \
	  -Dliboscal-java.version=$$LIBOSCAL_VERSION \
	  -Dmetaschema-framework.version=$$METASCHEMA_VERSION

# Package the main project with the correct versions
.PHONY: package
package:
	@echo "Packaging the main project..."
	. ./.env-liboscal && . ./.env-metaschema && \
	mvn package \
	  -Dliboscal-java.version=$$LIBOSCAL_VERSION \
	  -Dmetaschema-framework.version=$$METASCHEMA_VERSION

# Create a release (for testing purposes)
.PHONY: create-release
create-release:
	@echo "Creating a test release..."
	. ./.env-liboscal && . ./.env-metaschema && \
	VERSION=$$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout \
	  -Dliboscal-java.version=$$LIBOSCAL_VERSION \
	  -Dmetaschema-framework.version=$$METASCHEMA_VERSION) && \
	echo "Project version: $$VERSION" && \
	echo "Release artifact: ./target/server-$$VERSION-oscal-server.zip"

# Help target
.PHONY: help
help:
	@echo "OSCAL Server Release Assembly Makefile"
	@echo ""
	@echo "Available targets:"
	@echo "  all               - Run the complete build process (default)"
	@echo "  init-submodules   - Initialize and update all submodules"
	@echo "  build-liboscal    - Build and install liboscal-java"
	@echo "  build-metaschema  - Build and install metaschema-java"
	@echo "  clean             - Clean the main project"
	@echo "  build-client      - Build the client"
	@echo "  compile           - Compile the main project"
	@echo "  package           - Package the main project"
	@echo "  create-release    - Create a test release"
	@echo "  help              - Show this help message"
	@echo ""
	@echo "Usage example:"
	@echo "  make              - Run the complete build process"
	@echo "  make help         - Show this help message"
