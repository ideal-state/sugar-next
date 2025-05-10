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

package team.idealstate.sugar.next.boot.mybatis;

import static team.idealstate.sugar.next.function.Functional.lazy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import team.idealstate.sugar.logging.Log;
import team.idealstate.sugar.next.boot.mybatis.log.LogImpl;
import team.idealstate.sugar.next.context.Bean;
import team.idealstate.sugar.next.context.Context;
import team.idealstate.sugar.next.context.annotation.component.Component;
import team.idealstate.sugar.next.context.annotation.feature.Autowired;
import team.idealstate.sugar.next.context.annotation.feature.Named;
import team.idealstate.sugar.next.context.aware.ContextAware;
import team.idealstate.sugar.next.context.lifecycle.Initializable;
import team.idealstate.sugar.next.database.DataSourceProvider;
import team.idealstate.sugar.next.database.DatabaseSession;
import team.idealstate.sugar.next.database.TransactionSession;
import team.idealstate.sugar.next.database.exception.TransactionException;
import team.idealstate.sugar.next.function.Lazy;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;

@Named("NextMyBatis")
@Component
@SuppressWarnings("unused")
public class MyBatis implements NextMyBatis, Initializable, ContextAware {

    public static final int EXECUTION_MODE_SIMPLE = 0;
    public static final int EXECUTION_MODE_REUSE = 1;
    public static final int EXECUTION_MODE_BATCH = 2;
    private static final List<ExecutorType> EXECUTION_MODES =
            Collections.unmodifiableList(Arrays.asList(ExecutorType.SIMPLE, ExecutorType.REUSE, ExecutorType.BATCH));

    public static final int ISOLATION_LEVEL_NONE = 0;
    public static final int ISOLATION_LEVEL_READ_COMMITTED = 1;
    public static final int ISOLATION_LEVEL_READ_UNCOMMITTED = 2;
    public static final int ISOLATION_LEVEL_REPEATABLE_READ = 3;
    public static final int ISOLATION_LEVEL_SERIALIZABLE = 4;
    public static final int ISOLATION_LEVEL_SQL_SERVER_SNAPSHOT = 5;
    private static final List<TransactionIsolationLevel> ISOLATION_LEVELS = Collections.unmodifiableList(Arrays.asList(
            TransactionIsolationLevel.NONE,
            TransactionIsolationLevel.READ_COMMITTED,
            TransactionIsolationLevel.READ_UNCOMMITTED,
            TransactionIsolationLevel.REPEATABLE_READ,
            TransactionIsolationLevel.SERIALIZABLE,
            TransactionIsolationLevel.SQL_SERVER_SNAPSHOT));

    @NotNull
    @Override
    public DatabaseSession openSession(int executionMode, int isolationLevel) {
        SqlSession sqlSession;
        SqlSessionFactory sqlSessionFactory = getLazySqlSessionFactory().get();
        if (executionMode == DEFAULT_EXECUTION_MODE && isolationLevel == DEFAULT_ISOLATION_LEVEL) {
            sqlSession = sqlSessionFactory.openSession();
        } else if (executionMode == DEFAULT_EXECUTION_MODE) {
            sqlSession = sqlSessionFactory.openSession(ISOLATION_LEVELS.get(isolationLevel));
        } else if (isolationLevel == DEFAULT_ISOLATION_LEVEL) {
            sqlSession = sqlSessionFactory.openSession(EXECUTION_MODES.get(executionMode));
        } else {
            sqlSession = sqlSessionFactory.openSession(
                    EXECUTION_MODES.get(executionMode), ISOLATION_LEVELS.get(isolationLevel));
        }
        return new MyBatisSession(sqlSession);
    }

    private final Map<Thread, TransactionSession> transactionSessions = new ConcurrentHashMap<>();

    @SuppressWarnings("resource")
    @NotNull
    @Override
    public TransactionSession openTransaction(int executionMode, int isolationLevel) {
        return transactionSessions
                .computeIfAbsent(
                        Thread.currentThread(),
                        thread -> new TransactionSession(
                                openSession(executionMode, isolationLevel), () -> transactionSessions.remove(thread)))
                .open();
    }

    @NotNull
    @Override
    public <T> T getRepository(@NotNull Class<T> repositoryType) throws TransactionException {
        TransactionSession transactionSession = transactionSessions.get(Thread.currentThread());
        if (transactionSession == null) {
            throw new TransactionException("transaction session is not opened.");
        }
        return transactionSession.getRepository(repositoryType);
    }

    @Override
    public void initialize() {
        this.lazySqlSessionFactory = lazy(() -> {
            MyBatisConfiguration configuration = getConfiguration();
            Context context = getContext();
            Configuration myBatisConfig = new Configuration(new Environment.Builder(context.getEnvironment())
                    .dataSource(getDatabaseSourceProvider().getDataSource())
                    .transactionFactory(new JdbcTransactionFactory())
                    .build());
            if (configuration.getLog()) {
                myBatisConfig.setLogImpl(LogImpl.class);
            }
            myBatisConfig.setLocalCacheScope(LocalCacheScope.STATEMENT);
            MyBatisConfiguration.Cache cache = configuration.getCache();
            myBatisConfig.setCacheEnabled(false);
            Boolean cacheEnabled = cache.getEnabled();
            if (cacheEnabled) {
                myBatisConfig.setCacheEnabled(true);
                List<Bean<Cache>> beans = context.getBeans(Cache.class);
                if (beans.isEmpty()) {
                    Log.warn("No MyBatis cache bean found, disable and skip.");
                    myBatisConfig.setCacheEnabled(false);
                } else {
                    for (Bean<Cache> bean : beans) {
                        myBatisConfig.addCache(bean.getInstance());
                    }
                }
            }
            for (Bean<Mapper> bean : context.getBeans(Mapper.class)) {
                myBatisConfig.addMapper(bean.getMarked());
            }
            return new SqlSessionFactoryBuilder().build(myBatisConfig);
        });
    }

    private volatile Lazy<SqlSessionFactory> lazySqlSessionFactory;

    @NotNull
    private Lazy<SqlSessionFactory> getLazySqlSessionFactory() {
        return Validation.requireNotNull(lazySqlSessionFactory, "lazy sql session factory must not be null.");
    }

    private volatile Context context;

    @Override
    public void setContext(@NotNull Context context) {
        this.context = context;
    }

    @NotNull
    private Context getContext() {
        return Validation.requireNotNull(context, "context must not be null.");
    }

    private volatile MyBatisConfiguration configuration;

    @Autowired
    public void setConfiguration(@NotNull MyBatisConfiguration configuration) {
        this.configuration = configuration;
    }

    @NotNull
    private MyBatisConfiguration getConfiguration() {
        return Validation.requireNotNull(configuration, "configuration must not be null.");
    }

    private volatile DataSourceProvider dataSourceProvider;

    @Autowired
    public void setDatabaseSourceProvider(@NotNull DataSourceProvider dataSourceProvider) {
        this.dataSourceProvider = dataSourceProvider;
    }

    @NotNull
    private DataSourceProvider getDatabaseSourceProvider() {
        return Validation.requireNotNull(dataSourceProvider, "database source provider must not be null.");
    }
}
