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

package team.idealstate.minecraft.next.common.context.boot.jedis;

import static team.idealstate.minecraft.next.common.function.Functional.lazy;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import team.idealstate.minecraft.next.common.context.annotation.component.Component;
import team.idealstate.minecraft.next.common.context.annotation.feature.Autowired;
import team.idealstate.minecraft.next.common.context.annotation.feature.Named;
import team.idealstate.minecraft.next.common.context.lifecycle.Destroyable;
import team.idealstate.minecraft.next.common.context.lifecycle.Initializable;
import team.idealstate.minecraft.next.common.function.Lazy;
import team.idealstate.minecraft.next.common.string.StringUtils;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

@Named("NextJedis")
@Component
public class Jedis implements NextJedis, Initializable, Destroyable {

    @NotNull @Override
    public JedisPool getJedisPool() {
        return getLazyJedisPool().get();
    }

    @Override
    public void initialize() {
        this.lazyJedisPool =
                lazy(
                        () -> {
                            JedisConfiguration configuration = getConfiguration();
                            String username = configuration.getUsername();
                            username = StringUtils.isNullOrBlank(username) ? null : username;
                            String password = configuration.getPassword();
                            password = StringUtils.isNullOrBlank(password) ? null : password;
                            return new JedisPool(
                                    new JedisPoolConfig(),
                                    configuration.getHost(),
                                    configuration.getPort(),
                                    configuration.getTimeout(),
                                    configuration.getTimeout(),
                                    password,
                                    configuration.getDatabase(),
                                    username,
                                    configuration.getSsl());
                        });
    }

    @Override
    public void destroy() {
        Lazy<JedisPool> jedisPool = getLazyJedisPool();
        if (jedisPool.isInitialized()) {
            jedisPool.get().destroy();
        }
    }

    private volatile Lazy<JedisPool> lazyJedisPool;

    @NotNull private Lazy<JedisPool> getLazyJedisPool() {
        return Validation.requireNotNull(lazyJedisPool, "lazy jedis pool must not be null.");
    }

    private volatile JedisConfiguration configuration;

    @Autowired
    public void setConfiguration(@NotNull JedisConfiguration configuration) {
        this.configuration = configuration;
    }

    @NotNull private JedisConfiguration getConfiguration() {
        return Validation.requireNotNull(configuration, "configuration must not be null.");
    }
}
