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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.List;
import team.idealstate.sugar.logging.Log;
import team.idealstate.sugar.next.context.Bean;
import team.idealstate.sugar.next.context.Context;
import team.idealstate.sugar.next.context.annotation.component.Configuration;
import team.idealstate.sugar.next.context.annotation.component.Serialization;
import team.idealstate.sugar.next.context.exception.ContextException;
import team.idealstate.sugar.next.databind.codec.Codec;
import team.idealstate.sugar.next.io.IOUtils;
import team.idealstate.sugar.string.StringUtils;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;
import team.idealstate.sugar.validate.annotation.Nullable;

public class ConfigurationBeanFactory extends AbstractBeanFactory<Configuration> {

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
        List<Bean<Codec>> beans = context.getBeans(Codec.class);
        if (beans.isEmpty()) {
            throw new ContextException(String.format(
                    "%s: No codec found with '%s'.", getMetadataType().getSimpleName(), marked.getName()));
        }
        String extension = uri.substring(uri.lastIndexOf('.') + 1);
        String release = metadata.release().replace("\\", "/");
        String releaseExtension = release.substring(release.lastIndexOf('.') + 1);
        boolean isSupported = false;
        boolean isSupportedRelease = false;
        for (Bean<Codec> bean : beans) {
            Annotation annotation = bean.getMetadata();
            if (!(annotation instanceof Serialization)) {
                continue;
            }
            String support = ((Serialization) annotation).value();
            if (!isSupported && support.equals(extension)) {
                isSupported = true;
            }
            if (!isSupportedRelease && support.equals(releaseExtension)) {
                isSupportedRelease = true;
            }
            if (isSupported && isSupportedRelease) {
                break;
            }
        }
        if (!isSupported) {
            Log.warn(String.format(
                    "%s: Invalid configuration extension '%s'.",
                    getMetadataType().getSimpleName(), uri));
            return false;
        }
        if (!isSupportedRelease) {
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
                extension = release.substring(release.lastIndexOf('.') + 1);
            } else {
                Validation.notNull(
                        resource,
                        String.format(
                                "%s: Resource '%s' not found.",
                                getMetadataType().getSimpleName(), uri));
                extension = uri.substring(uri.lastIndexOf('.') + 1);
            }
            List<Bean<Codec>> beans = context.getBeans(Codec.class);
            for (Bean<Codec> bean : beans) {
                Annotation annotation = bean.getMetadata();
                if (!(annotation instanceof Serialization)) {
                    continue;
                }
                Codec codec = bean.getInstance();
                if (((Serialization) annotation).value().equals(extension)) {
                    return (T) functional(resource).use(Object.class, input -> codec.deserialize(input, marked));
                }
            }
            throw new ContextException(String.format(
                    "%s: No codec found with '%s'.", getMetadataType().getSimpleName(), marked.getName()));
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
