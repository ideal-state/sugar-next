/*
 *    Copyright 2025 ideal-state
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package team.idealstate.minecraft.next.common.bundled;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import team.idealstate.minecraft.next.common.logging.Log;

public abstract class Bundled {
    private static final int BUFFER_SIZE = 1024;
    private static final String BUNDLED_DIR_PATH = "bundled";
    private static final int BUNDLED_DIR_PATH_LENGTH = BUNDLED_DIR_PATH.length();

    public static void release(File destDir) {
        release(Bundled.class, destDir, "/", false);
    }

    public static void release(Class<?> owner, File destDir) {
        release(owner, destDir, "/", false);
    }

    public static void release(Class<?> owner, File destDir, String path, boolean overlay) {
        String mode = overlay ? " (overlay)" : "";
        Log.debug("release bundled" + mode);
        //        if (!destDir.exists()) {
        //            destDir.mkdirs();
        //        }
        String normalizedPath = Paths.get(BUNDLED_DIR_PATH, path).normalize().toString();
        URL location = owner.getProtectionDomain().getCodeSource().getLocation();
        File file;
        try {
            file = Paths.get(location.toURI()).toFile();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        try (JarFile jar = new JarFile(file)) {
            Enumeration<JarEntry> entries = jar.entries();
            if (entries.hasMoreElements()) {
                byte[] buffer = new byte[BUFFER_SIZE];
                do {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    if (entryName.startsWith(normalizedPath)) {
                        entryName = entryName.substring(BUNDLED_DIR_PATH_LENGTH);
                        Log.debug("bundled entry: " + entryName);
                        File destFile = new File(destDir, entryName);
                        if (entry.isDirectory()) {
                            if (!destFile.exists()) {
                                destFile.mkdirs();
                            }
                            continue;
                        }
                        File parentFile = destFile.getParentFile();
                        if (!parentFile.exists()) {
                            parentFile.mkdirs();
                        }
                        boolean exists = destFile.exists();
                        if (overlay || !exists) {
                            if (exists || destFile.createNewFile()) {
                                try (InputStream input = jar.getInputStream(entry)) {
                                    int read = input.read(buffer);
                                    if (read != -1) {
                                        try (FileOutputStream output =
                                                new FileOutputStream(destFile)) {
                                            do {
                                                output.write(buffer, 0, read);
                                                output.flush();
                                                read = input.read(buffer);
                                            } while (read != -1);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } while (entries.hasMoreElements());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Log.debug("release bundled done");
    }
}
