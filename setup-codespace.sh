#!/bin/bash
set -e

echo "Setting up Temurin JDK 21 for GitHub Codespace..."

# Download and install Eclipse Temurin JDK 21
echo "Installing Eclipse Temurin JDK 21..."
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo apt-key add -
echo "deb https://packages.adoptium.net/artifactory/deb $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt-get update
sudo apt-get install -y temurin-21-jdk

# Create Maven toolchains.xml file
echo "Configuring Maven toolchains..."
mkdir -p "$HOME/.m2"

# First create the toolchains.xml file
cat > "$HOME/.m2/toolchains.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<toolchains xmlns="http://maven.apache.org/TOOLCHAINS/1.1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://maven.apache.org/TOOLCHAINS/1.1.0 http://maven.apache.org/xsd/toolchains-1.1.0.xsd">
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>21</version>
      <vendor>temurin</vendor>
    </provides>
    <configuration>
      <jdkHome>/usr/lib/jvm/temurin-21-jdk</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
EOF

# Ensure proper permissions on the toolchains.xml file
chmod 644 "$HOME/.m2/toolchains.xml"

# Create a Maven settings file to reference the toolchain if it doesn't exist
if [ ! -f "$HOME/.m2/settings.xml" ]; then
  echo "Creating Maven settings.xml file..."
  cat > "$HOME/.m2/settings.xml" << EOF
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <profiles>
    <profile>
      <id>default-tools</id>
      <properties>
        <toolchain.jdk.vendor>temurin</toolchain.jdk.vendor>
        <toolchain.jdk.version>21</toolchain.jdk.version>
      </properties>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>default-tools</activeProfile>
  </activeProfiles>
</settings>
EOF
  chmod 644 "$HOME/.m2/settings.xml"
fi

# Set JAVA_HOME environment variable
echo "Setting JAVA_HOME environment variable..."
echo "export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk" >> "$HOME/.bashrc"
echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> "$HOME/.bashrc"

# Create .devcontainer/devcontainer.json for future Codespace setups
mkdir -p .devcontainer
cat > .devcontainer/devcontainer.json << EOF
{
  "name": "Java Development",
  "image": "mcr.microsoft.com/devcontainers/base:ubuntu",
  "features": {
    "ghcr.io/devcontainers/features/java:1": {
      "version": "21",
      "distribution": "temurin"
    },
    "ghcr.io/devcontainers/features/maven:1": {
      "version": "latest"
    }
  },
  "postCreateCommand": "bash setup-codespace.sh",
  "customizations": {
    "vscode": {
      "extensions": [
        "vscjava.vscode-java-pack",
        "redhat.vscode-xml"
      ]
    }
  }
}
EOF

# Add verification steps
echo "Verifying installation..."
echo "Java version:"
/usr/lib/jvm/temurin-21-jdk/bin/java -version

echo "Toolchains file location:"
ls -la "$HOME/.m2/toolchains.xml"

echo "Toolchains file content:"
cat "$HOME/.m2/toolchains.xml"

echo "Creating a simple verification script..."
cat > verify-toolchain.sh << 'EOF'
#!/bin/bash
set -e

echo "Verifying Maven can find the toolchain..."
mvn --version

echo "Creating a test Maven project..."
mkdir -p mvn-test
cd mvn-test

cat > pom.xml << 'INNER'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.test</groupId>
    <artifactId>toolchain-test</artifactId>
    <version>1.0-SNAPSHOT</version>
    
    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-toolchains-plugin</artifactId>
                <version>3.2.0</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>toolchain</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <toolchains>
                        <jdk>
                            <version>21</version>
                            <vendor>temurin</vendor>
                        </jdk>
                    </toolchains>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
INNER

echo "Running Maven with toolchain plugin to verify it works..."
mvn clean validate -B

echo "Toolchain verification complete!"
cd ..
rm -rf mvn-test
EOF

chmod +x verify-toolchain.sh

echo "Setup complete! Please run the following commands to activate and verify your environment:"
echo "1. source ~/.bashrc"
echo "2. ./verify-toolchain.sh"
echo ""
echo "Your Maven build should now be able to find the Temurin JDK 21 toolchain."