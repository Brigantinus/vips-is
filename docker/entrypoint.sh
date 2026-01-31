#!/bin/sh

# Dynamically find the library paths regardless of architecture
export VIPS_LIB_PATH=$(find /usr/lib -name "libvips.so.42" | head -n 1)
export GLIB_LIB_PATH=$(find /usr/lib -name "libglib-2.0.so.0" | head -n 1)
export GOBJ_LIB_PATH=$(find /usr/lib -name "libgobject-2.0.so.0" | head -n 1)

echo "--- Vips-IS Startup ---"
echo "VIPS Path:  $VIPS_LIB_PATH"
echo "GLIB Path:  $GLIB_LIB_PATH"
echo "GOBJ Path:  $GOBJ_LIB_PATH"

# Execute Java with the discovered paths
exec java \
    --enable-native-access=ALL-UNNAMED \
    -Dvipsffm.libpath.vips.override="$VIPS_LIB_PATH" \
    -Dvipsffm.libpath.glib.override="$GLIB_LIB_PATH" \
    -Dvipsffm.libpath.gobject.override="$GOBJ_LIB_PATH" \
    -Dquarkus.http.host=0.0.0.0 \
    -jar /deployments/quarkus-run.jar