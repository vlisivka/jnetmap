#!/bin/sh

java \
         --add-opens=java.base/java.net=ALL-UNNAMED \
         --add-opens java.base/java.text=ALL-UNNAMED \
         --add-opens java.base/java.util=ALL-UNNAMED \
         --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
         --add-opens=java.desktop/java.awt=ALL-UNNAMED \
         --add-opens java.desktop/java.awt.font=ALL-UNNAMED \
         --add-opens=java.desktop/java.awt.geom=ALL-UNNAMED \
         --add-opens=java.desktop/javax.swing.event=ALL-UNNAMED \
         --add-opens=java.desktop/com.sun.java.swing.plaf.motif=ALL-UNNAMED \
         --add-opens=java.desktop/com.sun.java.swing.plaf.gtk=ALL-UNNAMED \
    -jar target/jnetmap-*-jar-with-dependencies.jar
