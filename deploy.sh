#!/bin/bash

set -e

# Nom du framework
FRAMEWORK_NAME="MonFramework"

# Dossiers
SRC_DIR="src"
BUILD_DIR="build"
DIST_DIR="dist"
LIB_DIR="lib"

# API Jakarta Servlet
SERVLET_API_JAR="$LIB_DIR/servlet-api.jar"

# Vérification du JAR
if [ ! -f "$SERVLET_API_JAR" ]; then
    echo "Erreur : $SERVLET_API_JAR introuvable"
    exit 1
fi

# Nettoyage
rm -rf "$BUILD_DIR"
rm -rf "$DIST_DIR"

mkdir -p "$BUILD_DIR"
mkdir -p "$DIST_DIR"

# Liste des sources
find "$SRC_DIR" -name "*.java" > sources.txt

echo "Compilation..."

javac \
    -cp "$SERVLET_API_JAR" \
    -d "$BUILD_DIR" \
    @sources.txt

rm sources.txt

echo "Création du JAR..."

# Création du MANIFEST
mkdir -p tmp

cat > tmp/MANIFEST.MF << EOF
Manifest-Version: 1.0
Created-By: Framework Personnel
EOF

jar cfm "$DIST_DIR/$FRAMEWORK_NAME.jar" \
    tmp/MANIFEST.MF \
    -C "$BUILD_DIR" .

rm -rf tmp

echo ""
echo "JAR généré : $DIST_DIR/$FRAMEWORK_NAME.jar"
echo ""