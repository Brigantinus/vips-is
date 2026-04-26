#!/bin/sh

export VIPS_LIB_PATH=$(find /usr/lib -name "libvips.so.42" | head -n 1)
export GLIB_LIB_PATH=$(find /usr/lib -name "libglib-2.0.so.0" | head -n 1)
export GOBJ_LIB_PATH=$(find /usr/lib -name "libgobject-2.0.so.0" | head -n 1)

echo "--- ImageServer Startup ---"
echo "VIPS: $VIPS_LIB_PATH  GLIB: $GLIB_LIB_PATH  GOBJ: $GOBJ_LIB_PATH"

exec java \
    --enable-native-access=ALL-UNNAMED \
    -Dvipsffm.libpath.vips.override="$VIPS_LIB_PATH" \
    -Dvipsffm.libpath.glib.override="$GLIB_LIB_PATH" \
    -Dvipsffm.libpath.gobject.override="$GOBJ_LIB_PATH" \
    -Dquarkus.http.host=0.0.0.0 \
    -jar /deployments/quarkus-run.jar
