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

package team.idealstate.sugar.next.boot.mybatis.cache;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.ibatis.cache.Cache;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;
import team.idealstate.sugar.internal.org.yaml.snakeyaml.Yaml;
import team.idealstate.sugar.next.boot.jedis.NextJedis;
import team.idealstate.sugar.next.context.Context;
import team.idealstate.sugar.next.context.ContextProperty;
import team.idealstate.sugar.next.context.annotation.component.Component;
import team.idealstate.sugar.next.context.annotation.feature.Autowired;
import team.idealstate.sugar.next.context.annotation.feature.DependsOn;
import team.idealstate.sugar.next.context.annotation.feature.DependsOn.Property;
import team.idealstate.sugar.next.context.aware.ContextAware;
import team.idealstate.sugar.next.context.lifecycle.Initializable;
import team.idealstate.sugar.next.function.Lazy;
import team.idealstate.sugar.next.function.closure.Function;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;
import team.idealstate.sugar.validate.annotation.Nullable;

@Component
@DependsOn(
        beans = {"NextJedis", "NextMyBatis"},
        classes = {
            "team.idealstate.sugar.next.boot.jedis.NextJedis",
            "team.idealstate.sugar.next.boot.mybatis.NextMyBatis"
        },
        properties = {
            @Property(key = "team.idealstate.sugar.next.boot.jedis.annotation.EnableJedis", strict = false),
            @Property(key = "team.idealstate.sugar.next.boot.mybatis.annotation.EnableMyBatis", strict = false)
        })
public class JedisMyBatisCache implements Cache, ContextAware, Initializable {

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private Object execute(Function<Jedis, Object> callback) {
        try (Jedis jedis = getJedisPool().getResource()) {
            return callback.call(jedis);
        } catch (Throwable e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new JedisException(e);
        }
    }

    @Override
    public int getSize() {
        return (Integer) execute(jedis -> {
            Map<byte[], byte[]> result = jedis.hgetAll(getId().getBytes());
            return result.size();
        });
    }

    @Override
    public void putObject(final Object key, final Object value) {
        execute(jedis -> {
            final byte[] idBytes = getId().getBytes();
            jedis.hset(idBytes, key.toString().getBytes(), serialize(value));
            Integer timeout = getTimeout();
            if (timeout != null && jedis.ttl(idBytes) == -1) {
                jedis.expire(idBytes, timeout);
            }
            return null;
        });
    }

    @Override
    public Object getObject(final Object key) {
        return execute(jedis ->
                deserialize(jedis.hget(getId().getBytes(), key.toString().getBytes())));
    }

    @Override
    public Object removeObject(final Object key) {
        return execute(jedis -> jedis.hdel(getId(), key.toString()));
    }

    @Override
    public void clear() {
        execute(jedis -> {
            jedis.del(getId());
            return null;
        });
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return readWriteLock;
    }

    private final Lazy<Yaml> lazyYaml = Lazy.of(Yaml::new);

    private byte[] serialize(Object value) {
        return lazyYaml.get().dump(value).getBytes(getCharset());
    }

    private <T> T deserialize(byte[] value) {
        return lazyYaml.get().load(new String(value, getCharset()));
    }

    @Override
    public void initialize() {
        Context context = getContext();
        ContextProperty property = context.getProperty(PROPERTY_KEY_TIMEOUT);
        this.timeout = property == null ? null : property.asInt();
        property = context.getProperty(PROPERTY_KEY_CHARSET);
        this.charset = property == null ? StandardCharsets.UTF_8 : Charset.forName(property.getValue());
    }

    private volatile Context context;

    @Override
    public void setContext(@NotNull Context context) {
        Validation.requireNotNull(context, "context must not be null.");
        this.context = context;
    }

    @NotNull
    private Context getContext() {
        return Validation.requireNotNull(context, "context must not be null.");
    }

    @Override
    public String getId() {
        return getContext().getName();
    }

    private volatile NextJedis nextJedis;

    @Autowired
    public void setNextJedis(@NotNull NextJedis jedis) {
        Validation.requireNotNull(jedis, "next jedis must not be null.");
        this.nextJedis = jedis;
    }

    @NotNull
    private NextJedis getNextJedis() {
        return Validation.requireNotNull(nextJedis, "next jedis must not be null.");
    }

    @NotNull
    private JedisPool getJedisPool() {
        return Validation.requireNotNull(getNextJedis().getJedisPool(), "jedis pool must not be null.");
    }

    public static final String PROPERTY_KEY_TIMEOUT = JedisMyBatisCache.class.getName() + ".timeout";
    public static final String PROPERTY_KEY_CHARSET = JedisMyBatisCache.class.getName() + ".charset";
    private volatile Integer timeout = null;

    @Nullable
    private Integer getTimeout() {
        return timeout;
    }

    private volatile Charset charset = StandardCharsets.UTF_8;

    @NotNull
    private Charset getCharset() {
        return Validation.requireNotNull(charset, "charset must not be null.");
    }
}
