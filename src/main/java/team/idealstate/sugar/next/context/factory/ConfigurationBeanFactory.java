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

package team.idealstate.sugar.next.context.factory;

import static team.idealstate.sugar.next.function.Functional.functional;
import static team.idealstate.sugar.next.function.Functional.lazy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import team.idealstate.sugar.internal.com.fasterxml.jackson.databind.ObjectMapper;
import team.idealstate.sugar.internal.com.fasterxml.jackson.databind.json.JsonMapper;
import team.idealstate.sugar.internal.com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import team.idealstate.sugar.internal.org.yaml.snakeyaml.Yaml;
import team.idealstate.sugar.logging.Log;
import team.idealstate.sugar.next.context.Context;
import team.idealstate.sugar.next.context.annotation.component.Configuration;
import team.idealstate.sugar.next.context.exception.ContextException;
import team.idealstate.sugar.next.function.Lazy;
import team.idealstate.sugar.next.io.IOUtils;
import team.idealstate.sugar.string.StringUtils;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;
import team.idealstate.sugar.validate.annotation.Nullable;

public class ConfigurationBeanFactory extends AbstractBeanFactory<Configuration> {

    public static final Set<String> SUPPORTED_FILE_EXTENSIONS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(".yml", ".yaml", ".json")));

    private final Lazy<Yaml> snakeyaml = lazy(Yaml::new);
    private final Lazy<ObjectMapper> yaml = lazy(() -> new YAMLMapper().findAndRegisterModules());
    private final Lazy<ObjectMapper> json = lazy(() -> new JsonMapper().findAndRegisterModules());

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
            Log.warn(String.format(
                    "%s: Invalid configuration uri '%s'. (%s)",
                    getMetadataType().getSimpleName(), uri, e.getMessage()));
            return false;
        }
        if (!SUPPORTED_FILE_EXTENSIONS.contains(uri.substring(uri.lastIndexOf('.')))) {
            Log.warn(String.format(
                    "%s: Invalid configuration extension '%s'.",
                    getMetadataType().getSimpleName(), uri));
            return false;
        }
        String release = metadata.release().replace("\\", "/");
        if (!SUPPORTED_FILE_EXTENSIONS.contains(release.substring(release.lastIndexOf('.')))) {
            Log.warn(String.format(
                    "%s: Invalid configuration extension '%s'.",
                    getMetadataType().getSimpleName(), release));
            return false;
        }
        File file;
        try {
            if (!StringUtils.isNullOrBlank(release) && (file = getFile(context, uri)) != null && !file.exists()) {
                Validation.is(
                        release.charAt(release.length() - 1) != '/',
                        String.format(
                                "%s: Invalid file configuration release uri '%s'.",
                                getMetadataType().getSimpleName(), release));
                InputStream resource = context.getResource(release, marked, marked.getClassLoader());
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

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    protected <T> T doCreate(
            @NotNull Context context,
            @NotNull String beanName,
            @NotNull Configuration metadata,
            @NotNull Class<T> marked) {
        try {
            ClassLoader markedClassLoader = marked.getClassLoader();
            String uri = metadata.uri().replace("\\", "/");
            String extension;
            Validation.is(
                    uri.charAt(uri.length() - 1) != '/',
                    String.format(
                            "%s: Invalid file configuration uri '%s'.",
                            getMetadataType().getSimpleName(), uri));
            InputStream resource = context.getResource(uri, marked, markedClassLoader);
            String release;
            if (resource == null
                    && StringUtils.isNullOrBlank(release = metadata.release().replace("\\", "/"))) {
                Validation.is(
                        release.charAt(release.length() - 1) != '/',
                        String.format(
                                "%s: Invalid file configuration release uri '%s'.",
                                getMetadataType().getSimpleName(), release));
                resource = context.getResource(release, marked, markedClassLoader);
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
                extension = release.substring(release.lastIndexOf('.'));
            } else {
                Validation.notNull(
                        resource,
                        String.format(
                                "%s: Resource '%s' not found.",
                                getMetadataType().getSimpleName(), uri));
                extension = uri.substring(uri.lastIndexOf('.'));
            }
            return (T) functional(resource).use(Object.class, input -> {
                if (extension.equals(".yml") || extension.equals(".yaml")) {
                    return yaml.get().convertValue(snakeyaml.get().load(input), marked);
                } else if (extension.equals(".json")) {
                    return json.get().readValue(input, marked);
                }
                throw new ContextException(String.format(
                        "%s: Unsupported file configuration extension '%s'.",
                        getMetadataType().getSimpleName(), extension));
            });
        } catch (IOException e) {
            throw new ContextException(e);
        }
    }

    @Nullable
    private static File getFile(@NotNull Context context, String uri) {
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
