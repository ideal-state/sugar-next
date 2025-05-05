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

package team.idealstate.minecraft.next.common.agent;

import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import team.idealstate.minecraft.next.common.reflect.Reflection;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public abstract class AgentUtils {

    private static volatile Instrumentation _inst = null;

    public static void premain(String agentArgs, Instrumentation inst) {
        _inst = inst;
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        _inst = inst;
    }

    @NotNull public static Instrumentation instrumentation() {
        Instrumentation instrumentation = _inst;
        Validation.notNull(instrumentation, "Instrumentation must not be null");
        return instrumentation;
    }

    private static String currentProcessId() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return name.split("@")[0];
    }

    public static void attach() {
        if (_inst == null) {
            synchronized (AgentUtils.class) {
                if (_inst == null) {
                    String path =
                            AgentUtils.class
                                    .getProtectionDomain()
                                    .getCodeSource()
                                    .getLocation()
                                    .getPath();
                    String processId = currentProcessId();

                    ReflectVirtualMachine reflectVirtualMachine =
                            Reflection.reflect(null, ReflectVirtualMachine.class, null);
                    Object virtualMachine = reflectVirtualMachine.attach(processId);

                    reflectVirtualMachine =
                            Reflection.reflect(null, ReflectVirtualMachine.class, virtualMachine);
                    try {
                        reflectVirtualMachine.loadAgent(path);
                    } finally {
                        reflectVirtualMachine.detach();
                    }
                }
            }
        }
    }
}
