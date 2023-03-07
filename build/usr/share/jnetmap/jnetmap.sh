#!/bin/sh

if test -n "$(which java)"; then
    echo "using java executable in \$PATH"
    _java=java
elif test -n "$JAVA_HOME" && test -x "$JAVA_HOME/bin/java";  then
    echo "using java executable in \$JAVA_HOME"
    _java="$JAVA_HOME/bin/java"
else
    echo "java not found in \$PATH or \$JAVA_HOME"
    exit 1
fi

version=$("$_java" -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
echo "java version $version"
if test "$version" -lt 8; then
  echo "java 8 or later is required"
  exit 1
elif  test "$version" -eq 8; then
  $_java -jar /usr/share/jnetmap/jNetMap.jar "$@"
else
  $_java --add-opens=java.base/java.net=ALL-UNNAMED \
        --add-opens=java.desktop/java.awt=ALL-UNNAMED \
        --add-opens=java.desktop/java.awt.geom=ALL-UNNAMED \
        --add-opens=java.desktop/javax.swing.event=ALL-UNNAMED \
        --add-opens=java.desktop/com.sun.java.swing.plaf.motif=ALL-UNNAMED \
        --add-opens=java.desktop/com.sun.java.swing.plaf.gtk=ALL-UNNAMED \
        -jar /usr/share/jnetmap/jNetMap.jar "$@"
fi
