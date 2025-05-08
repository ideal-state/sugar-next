package team.idealstate.minecraft.next.common.database;

import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

import java.io.Closeable;

public interface DatabaseSession extends Closeable {

    @NotNull
    <T> T getRepository(@NotNull Class<T> repositoryType);

    void commit();

    void rollback();

    @Override
    void close();
}
