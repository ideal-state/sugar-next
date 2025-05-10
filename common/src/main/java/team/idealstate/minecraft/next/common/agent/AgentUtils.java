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

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import team.idealstate.minecraft.next.common.banner.Banner;
import team.idealstate.minecraft.next.common.bundled.Bundled;
import team.idealstate.minecraft.next.common.context.ContextLibraryLoader;
import team.idealstate.minecraft.next.common.logging.Log;
import team.idealstate.minecraft.next.common.reflect.Reflection;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public abstract class AgentUtils {

    public static void premain(String arguments, Instrumentation instrumentation) {
        doMain(arguments, instrumentation);
    }

    public static void agentmain(String arguments, Instrumentation instrumentation) {
        doMain(arguments, instrumentation);
    }

    private static void doMain(String arguments, Instrumentation instrumentation) {
        Banner.lines(AgentUtils.class).forEach(Log::info);
        Bundled.release(AgentUtils.class, new File("./"));
        instrumentation.addTransformer(new ContextLibraryLoader(instrumentation));
        setInstrumentation(instrumentation);
    }

    private static volatile Instrumentation INSTRUMENTATION = null;

    private static void setInstrumentation(@NotNull Instrumentation instrumentation) {
        Validation.notNull(instrumentation, "instrumentation must not be null.");
        AgentUtils.INSTRUMENTATION = instrumentation;
    }

    @NotNull public static Instrumentation instrumentation() {
        Instrumentation instrumentation = AgentUtils.INSTRUMENTATION;
        return Validation.requireNotNull(instrumentation, "instrumentation must not be null.");
    }

    @NotNull public static String currentProcessId() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return name.split("@")[0];
    }

    public static void attach(@NotNull String agentPath) {
        Validation.notNullOrBlank(agentPath, "agentPath must not be null or blank.");
        String processId = currentProcessId();

        ReflectVirtualMachine reflectVirtualMachine =
                Reflection.reflect(null, ReflectVirtualMachine.class, null);
        Object virtualMachine = reflectVirtualMachine.attach(processId);

        reflectVirtualMachine =
                Reflection.reflect(null, ReflectVirtualMachine.class, virtualMachine);
        try {
            reflectVirtualMachine.loadAgent(agentPath);
        } finally {
            reflectVirtualMachine.detach();
        }
    }
}
