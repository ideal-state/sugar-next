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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import team.idealstate.sugar.next.context.annotation.component.Configuration;

@Configuration(uri = "/database/MyBatis.yml", release = "bundled:/database/MyBatis.yml")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyBatisConfiguration {

    @NonNull
    private Boolean log;

    @NonNull
    private Cache cache;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cache {
        @NonNull
        private Boolean enabled;

        @NonNull
        private Integer expired;
    }
}
