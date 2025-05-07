package team.idealstate.minecraft.next.common.database;

import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public interface DatabaseSessionFactory {

    int DEFAULT_EXECUTION_MODE = 0;
    int DEFAULT_ISOLATION_LEVEL = 0;

    /**
     * @return 数据库会话，应使用默认的会话参数
     */
    @NotNull
    DatabaseSession openSession();

    /**
     * @param executionMode 执行模式
     * @param isolationLevel 隔离级别
     * @return 数据库会话，会话参数是否生效应查看其实现类，这只是标准而不是必须的
     * @see #DEFAULT_EXECUTION_MODE 实现类的默认执行模式应该使用此值
     * @see #DEFAULT_ISOLATION_LEVEL 实现类的默认隔离级别应该使用此值
     */
    @NotNull
    DatabaseSession openSession(int executionMode, int isolationLevel);
}
