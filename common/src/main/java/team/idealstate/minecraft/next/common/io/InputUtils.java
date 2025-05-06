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

package team.idealstate.minecraft.next.common.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public abstract class InputUtils {

    @NotNull public static byte[] readStream(@NotNull InputStream inputStream) throws IOException {
        return readStream(inputStream, false);
    }

    @NotNull public static byte[] readStream(@NotNull InputStream inputStream, boolean close)
            throws IOException {
        Validation.notNull(inputStream, "inputStream must not be null.");
        int bufferSize = computeBufferSize(inputStream);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] data = new byte[bufferSize];
            int bytesRead;
            int readCount = 0;
            while ((bytesRead = inputStream.read(data, 0, bufferSize)) != -1) {
                outputStream.write(data, 0, bytesRead);
                readCount++;
            }
            outputStream.flush();
            if (readCount == 1) {
                return data;
            }
            return outputStream.toByteArray();
        } finally {
            if (close) {
                inputStream.close();
            }
        }
    }

    public static int computeBufferSize(final InputStream inputStream) throws IOException {
        int expectedLength = inputStream.available();
        if (expectedLength < 256) {
            return 4096;
        }
        return Math.min(expectedLength, 1048576);
    }
}
