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

package team.idealstate.sugar.next.boot.mybatis.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.apache.ibatis.annotations.Mapper;
import team.idealstate.sugar.next.boot.mybatis.factory.MapperBeanFactory;
import team.idealstate.sugar.next.boot.mybatis.factory.RepositoryBeanFactory;
import team.idealstate.sugar.next.boot.mybatis.factory.TransactionComponentBeanFactory;
import team.idealstate.sugar.next.context.annotation.component.Component;
import team.idealstate.sugar.next.context.annotation.component.Repository;
import team.idealstate.sugar.next.context.annotation.feature.RegisterFactory;
import team.idealstate.sugar.next.context.annotation.feature.RegisterProperty;
import team.idealstate.sugar.next.context.annotation.feature.Scan;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Scan("team.idealstate.sugar.next.boot.mybatis")
@RegisterFactory(metadata = Component.class, beanFactory = TransactionComponentBeanFactory.class)
@RegisterFactory(metadata = Repository.class, beanFactory = RepositoryBeanFactory.class)
@RegisterFactory(metadata = Mapper.class, beanFactory = MapperBeanFactory.class)
@RegisterProperty(key = "team.idealstate.sugar.next.boot.mybatis.annotation.EnableMyBatis")
public @interface EnableMyBatis {}
