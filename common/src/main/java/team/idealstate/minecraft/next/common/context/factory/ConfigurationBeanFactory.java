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

package team.idealstate.minecraft.next.common.context.factory;

import static team.idealstate.minecraft.next.common.function.Functional.functional;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import org.yaml.snakeyaml.Yaml;
import team.idealstate.minecraft.next.common.context.Context;
import team.idealstate.minecraft.next.common.context.annotation.component.Configuration;
import team.idealstate.minecraft.next.common.context.exception.ContextException;
import team.idealstate.minecraft.next.common.function.Lazy;
import team.idealstate.minecraft.next.common.io.IOUtils;
import team.idealstate.minecraft.next.common.logging.Log;
import team.idealstate.minecraft.next.common.string.StringUtils;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;
import team.idealstate.minecraft.next.common.validate.annotation.Nullable;

public class ConfigurationBeanFactory extends AbstractBeanFactory<Configuration> {

    private final Lazy<Yaml> lazy = Lazy.of(Yaml::new);

    public ConfigurationBeanFactory() {
        super(Configuration.class);
    }

    @Override
    protected boolean doValidate(
            @NotNull Context context,
            @NotNull String beanName,
            @NotNull Configuration metadata,
            @NotNull Class<?> marked) {
        String uri = metadata.uri();
        try {
            new URI(uri);
        } catch (URISyntaxException e) {
            Log.warn(
                    String.format(
                            "%s: Invalid configuration uri '%s'. (%s)",
                            getMetadataType().getSimpleName(), uri, e.getMessage()));
            return false;
        }
        String release = metadata.release().replace("\\", "/");
        File file;
        try {
            if (!StringUtils.isNullOrBlank(release)
                    && (file = getFile(context, uri)) != null
                    && !file.exists()) {
                Validation.is(
                        release.charAt(release.length() - 1) != '/',
                        String.format(
                                "%s: Invalid file configuration release uri '%s'.",
                                getMetadataType().getSimpleName(), release));
                InputStream resource = context.getResource(release, marked.getClassLoader());
                Validation.notNull(
                        resource,
                        String.format(
                                "%s: Resource '%s' not found.",
                                getMetadataType().getSimpleName(), release));
                assert resource != null;
                //noinspection ResultOfMethodCallIgnored
                file.getParentFile().mkdirs();
                //noinspection ResultOfMethodCallIgnored
                file.createNewFile();
                IOUtils.transferTo(resource, Files.newOutputStream(file.toPath()));
            }
        } catch (IOException e) {
            throw new ContextException(e);
        }
        return true;
    }

    @NotNull @Override
    @SuppressWarnings("unchecked")
    protected <T> T doCreate(
            @NotNull Context context,
            @NotNull String beanName,
            @NotNull Configuration metadata,
            @NotNull Class<T> marked) {
        try {
            ClassLoader markedClassLoader = marked.getClassLoader();
            String uri = metadata.uri().replace("\\", "/");
            Validation.is(
                    uri.charAt(uri.length() - 1) != '/',
                    String.format(
                            "%s: Invalid file configuration uri '%s'.",
                            getMetadataType().getSimpleName(), uri));
            InputStream resource = context.getResource(uri, markedClassLoader);
            String release;
            if (resource == null
                    && StringUtils.isNullOrBlank(release = metadata.release().replace("\\", "/"))) {
                Validation.is(
                        release.charAt(release.length() - 1) != '/',
                        String.format(
                                "%s: Invalid file configuration release uri '%s'.",
                                getMetadataType().getSimpleName(), release));
                resource = context.getResource(release, markedClassLoader);
                Validation.notNull(
                        resource,
                        String.format(
                                "%s: Resource '%s' not found.",
                                getMetadataType().getSimpleName(), release));
                assert resource != null;
                File file = getFile(context, uri);
                if (file != null) {
                    if (!file.exists()) {
                        //noinspection ResultOfMethodCallIgnored
                        file.getParentFile().mkdirs();
                        //noinspection ResultOfMethodCallIgnored
                        file.createNewFile();
                    }
                    Files.write(file.toPath(), IOUtils.readAllBytes(resource));
                    resource = Files.newInputStream(file.toPath());
                }
            } else {
                Validation.notNull(
                        resource,
                        String.format(
                                "%s: Resource '%s' not found.",
                                getMetadataType().getSimpleName(), uri));
            }
            return (T)
                    functional(resource)
                            .convert(InputStreamReader::new)
                            .use(Object.class, reader -> lazy.get().loadAs(reader, marked));
        } catch (IOException e) {
            throw new ContextException(e);
        }
    }

    @Nullable private static File getFile(@NotNull Context context, String uri) {
        URI location;
        try {
            location = new URI(uri);
        } catch (URISyntaxException e) {
            throw new ContextException(e);
        }
        File file = null;
        if (!location.isAbsolute()) {
            file = new File(context.getDataFolder(), uri);
        } else if ("file".equals(location.getScheme())) {
            file = new File(location);
        }
        return file;
    }
}
