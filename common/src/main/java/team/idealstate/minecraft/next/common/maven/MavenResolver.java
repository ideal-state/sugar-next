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

package team.idealstate.minecraft.next.common.maven;

import static team.idealstate.minecraft.next.common.function.Functional.functional;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.yaml.snakeyaml.Yaml;
import team.idealstate.minecraft.next.common.logging.Log;
import team.idealstate.minecraft.next.common.maven.exception.MavenException;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public class MavenResolver {

    public static final File CONFIG_FILE = new File("./maven/config.yml");
    private final RepositorySystem system;
    private final DefaultRepositorySystemSession session;
    private final List<RemoteRepository> repositories;

    public MavenResolver() {
        this(
                functional(new Yaml())
                        .use(
                                MavenConfiguration.class,
                                yaml ->
                                        functional(Files.newBufferedReader(CONFIG_FILE.toPath()))
                                                .use(
                                                        MavenConfiguration.class,
                                                        reader ->
                                                                yaml.loadAs(
                                                                        reader,
                                                                        MavenConfiguration
                                                                                .class))));
    }

    public MavenResolver(@NotNull MavenConfiguration configuration) {
        this(configuration, new File(CONFIG_FILE.getParentFile(), "repository"));
    }

    protected MavenResolver(
            @NotNull MavenConfiguration configuration, @NotNull File localRepository) {
        Validation.notNull(configuration, "configuration must not be null.");
        Validation.notNull(localRepository, "localRepository must not be null.");
        RepositorySystemSupplier supplier = new RepositorySystemSupplier();
        this.system = supplier.get();
        this.session = MavenRepositorySystemUtils.newSession();
        if (!localRepository.exists()) {
            //noinspection ResultOfMethodCallIgnored
            localRepository.mkdirs();
        }
        session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_FAIL);
        session.setLocalRepositoryManager(
                system.newLocalRepositoryManager(session, new LocalRepository(localRepository)));
        session.setTransferListener(new TransferLog());
        session.setReadOnly();
        Map<String, MavenConfiguration.Repository> repos = configuration.getRepositories();
        Set<RemoteRepository> remoteRepositories = new LinkedHashSet<>(repos.size() + 1);
        for (Map.Entry<String, MavenConfiguration.Repository> entry : repos.entrySet()) {
            String id = entry.getKey();
            MavenConfiguration.Repository repository = entry.getValue();
            remoteRepositories.add(
                    new RemoteRepository.Builder(id, "default", repository.getUrl()).build());
        }
        this.repositories =
                system.newResolutionRepositories(session, new ArrayList<>(remoteRepositories));
    }

    @NotNull public List<Artifact> resolve(@NotNull List<Dependency> dependencies) {
        return resolve(dependencies, JavaScopes.COMPILE, JavaScopes.RUNTIME);
    }

    /**
     * @param scopes {@link JavaScopes}
     */
    @NotNull public List<Artifact> resolve(
            @NotNull List<Dependency> dependencies, @NotNull String... scopes) {
        Validation.notNull(dependencies, "dependencies must not be null.");
        Validation.notNull(scopes, "scopes must not be null.");
        if (dependencies.isEmpty() || scopes.length == 0) {
            return Collections.emptyList();
        }
        dependencies =
                dependencies.stream()
                        .filter(
                                d -> {
                                    for (String scope : scopes) {
                                        if (d.getScope().equalsIgnoreCase(scope)) {
                                            return true;
                                        }
                                    }
                                    return false;
                                })
                        .collect(Collectors.toList());
        if (dependencies.isEmpty()) {
            return Collections.emptyList();
        }

        DependencyResult dependencyResult;
        try {
            dependencyResult =
                    system.resolveDependencies(
                            session,
                            new DependencyRequest(
                                    new CollectRequest(
                                            (Dependency) null, dependencies, repositories),
                                    null));
        } catch (DependencyResolutionException e) {
            throw new MavenException("the dependency resolution failed", e);
        }
        return dependencyResult.getArtifactResults().stream()
                .map(ArtifactResult::getArtifact)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static class TransferLog extends AbstractTransferListener {

        @Override
        public void transferStarted(TransferEvent event) throws TransferCancelledException {
            TransferResource resource = event.getResource();
            Log.info(
                    String.format(
                            "downloading %s",
                            resource.getRepositoryUrl() + resource.getResourceName()));
        }

        @Override
        public void transferSucceeded(TransferEvent event) {
            TransferResource resource = event.getResource();
            Log.info(
                    String.format(
                            "downloaded %s",
                            resource.getRepositoryUrl() + resource.getResourceName()));
        }

        @Override
        public void transferFailed(TransferEvent event) {
            TransferResource resource = event.getResource();
            Log.error(
                    String.format(
                            "failed to download %s",
                            resource.getRepositoryUrl() + resource.getResourceName()));
        }
    }
}
