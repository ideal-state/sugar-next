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

package team.idealstate.minecraft.next.common.banner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public abstract class Banner {
    private static final String FILE_NAME = "/banner.txt";

    @NotNull public static List<String> lines() {
        return lines(Banner.class);
    }

    @NotNull public static List<String> lines(Class<?> owner) {
        InputStream inputStream = owner.getResourceAsStream(FILE_NAME);
        if (inputStream == null) {
            return Collections.emptyList();
        }
        try (BufferedReader it = new BufferedReader(new InputStreamReader(inputStream))) {
            return it.lines().collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
