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

package team.idealstate.sugar.next.databind.codec;

import java.io.File;
import java.io.OutputStream;
import java.io.Writer;
import team.idealstate.sugar.next.databind.codec.exception.CodecException;
import team.idealstate.sugar.validate.annotation.NotNull;

public interface Serializer {

    void serialize(Object object, @NotNull File file) throws CodecException;

    void serialize(Object object, @NotNull Writer writer) throws CodecException;

    void serialize(Object object, @NotNull OutputStream outputStream) throws CodecException;

    byte[] serialize(Object object) throws CodecException;
}
