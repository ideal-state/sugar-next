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

package team.idealstate.sugar.next.boot.hikaricp;

import static team.idealstate.sugar.next.function.Functional.functional;
import static team.idealstate.sugar.next.function.Functional.lazy;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import team.idealstate.sugar.next.context.Context;
import team.idealstate.sugar.next.context.annotation.component.Component;
import team.idealstate.sugar.next.context.annotation.feature.Autowired;
import team.idealstate.sugar.next.context.annotation.feature.Named;
import team.idealstate.sugar.next.context.aware.ContextAware;
import team.idealstate.sugar.next.context.lifecycle.Destroyable;
import team.idealstate.sugar.next.context.lifecycle.Initializable;
import team.idealstate.sugar.next.function.Lazy;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;

@Named("NextHikariCP")
@Component
public class HikariCP implements NextHikariCP, ContextAware, Initializable, Destroyable {
    @NotNull
    @Override
    public DataSource getDataSource() {
        return lazyDataSource.get();
    }

    @Override
    public void initialize() {
        this.lazyDataSource = lazy(() -> {
            HikariCPConfiguration configuration = getConfiguration();
            Map<String, Object> properties = configuration.getProperties();
            Object path = properties.get("path");
            if (path instanceof String && ((String) path).startsWith(".")) {
                String absolutePath = new File(getContext().getDataFolder(), (String) path).getAbsolutePath();
                properties.put("path", absolutePath.replace("\\", "/"));
            }
            HikariConfig hikariConfig = asHikariConfig(configuration);
            return new HikariDataSource(hikariConfig);
        });
    }

    @Override
    public void destroy() {
        Lazy<HikariDataSource> dataSource = getLazyDataSource();
        if (dataSource.isInitialized()) {
            dataSource.get().close();
        }
    }

    private volatile Lazy<HikariDataSource> lazyDataSource;

    @NotNull
    private Lazy<HikariDataSource> getLazyDataSource() {
        return Validation.requireNotNull(this.lazyDataSource, "lazy data source must not be null.");
    }

    @NotNull
    private static HikariConfig asHikariConfig(HikariCPConfiguration configuration) {
        Map<String, Object> properties = configuration.getProperties();
        HikariConfig hikariConfig = new HikariConfig(
                functional(new Properties()).apply(it -> it.putAll(properties)).it());
        hikariConfig.setDriverClassName(configuration.getDriver());
        String url = configuration.getUrl();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            url = url.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(configuration.getUsername());
        hikariConfig.setPassword(configuration.getPassword());
        return hikariConfig;
    }

    private volatile HikariCPConfiguration configuration;

    @Autowired
    public void setConfiguration(@NotNull HikariCPConfiguration configuration) {
        this.configuration = configuration;
    }

    @NotNull
    private HikariCPConfiguration getConfiguration() {
        return Validation.requireNotNull(configuration, "configuration must not be null.");
    }

    private volatile Context context;

    public void setContext(@NotNull Context context) {
        this.context = context;
    }

    @NotNull
    private Context getContext() {
        return Validation.requireNotNull(context, "context must not be null.");
    }
}
