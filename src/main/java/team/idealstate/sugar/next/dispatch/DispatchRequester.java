package team.idealstate.sugar.next.dispatch;

import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;

import java.util.Set;

public interface DispatchRequester<T> {

    /**
     * @return 该调度请求的实际持有者
     */
    @NotNull
    T getHolder();

    /**
     * @return 该调度请求者是否为管理员
     */
    boolean isAdmin();

    /**
     * @param permission 待检查的权限
     * @return 该调度请求者是否拥有该权限
     */
    default boolean hasPermission(@NotNull String permission) {
        Validation.notNull(permission, "permission must not be null.");
        return getPermissions().contains(permission);
    }

    /**
     * @param permissions 待检查的权限列表
     * @return 该调度请求者是否拥有该权限
     */
    default boolean hasPermissions(@NotNull String... permissions) {
        Validation.notNull(permissions, "permission must not be null.");
        Set<String> has = getPermissions();
        if (has.isEmpty()) {
            return permissions.length == 0;
        }
        for (String permission : permissions) {
            if (!has.contains(permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return 该调度请求者所拥有的权限列表
     */
    @NotNull
    Set<String> getPermissions();
}
