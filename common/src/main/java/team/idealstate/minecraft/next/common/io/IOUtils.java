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

import static team.idealstate.minecraft.next.common.function.Functional.functional;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.List;
import java.util.stream.Collectors;

import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public abstract class IOUtils {

    @NotNull public static byte[] readAllBytes(@NotNull InputStream inputStream) throws IOException {
        Validation.notNull(inputStream, "inputStream must not be null.");
        return functional(new ByteArrayOutputStream())
                .use(
                        byte[].class,
                        output -> {
                            try {
                                transferTo(inputStream, output);
                                return output.toByteArray();
                            } finally {
                                inputStream.close();
                            }
                        });
    }

    public static final int DEFAULT_BUFFER_SIZE = 4096;

    public static long transferTo(
            @NotNull InputStream inputStream, @NotNull OutputStream outputStream) {
        Validation.notNull(inputStream, "inputStream must not be null.");
        Validation.notNull(outputStream, "outputStream must not be null.");
        return functional(inputStream)
                .use(
                        Long.class,
                        input ->
                                functional(outputStream)
                                        .use(
                                                Long.class,
                                                output -> {
                                                    long transferred = 0;
                                                    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                                                    int read;
                                                    while ((read =
                                                                    input.read(
                                                                            buffer,
                                                                            0,
                                                                            DEFAULT_BUFFER_SIZE))
                                                            >= 0) {
                                                        output.write(buffer, 0, read);
                                                        if (transferred < Long.MAX_VALUE) {
                                                            try {
                                                                transferred =
                                                                        Math.addExact(
                                                                                transferred, read);
                                                            } catch (ArithmeticException ignore) {
                                                                transferred = Long.MAX_VALUE;
                                                            }
                                                        }
                                                        output.flush();
                                                    }
                                                    return transferred;
                                                }));
    }

    @NotNull
    public static List<String> readAllLines(@NotNull InputStream inputStream) {
        return readAllLines(new InputStreamReader(inputStream));
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static List<String> readAllLines(@NotNull Reader reader) {
        return functional(reader)
                .convert(it -> {
                    if (it instanceof BufferedReader) {
                        return (BufferedReader) it;
                    }
                    return new BufferedReader(it);
                })
                .use(List.class, it -> it.lines().collect(Collectors.toList()));
    }
}
