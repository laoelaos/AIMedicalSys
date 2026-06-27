package com.aimedical.modules.commonmodule.auth.converter;

import com.aimedical.modules.commonmodule.auth.UserInfoResponse;
import com.aimedical.modules.commonmodule.permission.PermissionFunction;
import com.aimedical.modules.commonmodule.permission.Post;
import com.aimedical.modules.commonmodule.permission.Role;
import com.aimedical.modules.commonmodule.permission.User;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserConverter {

    public UserInfoResponse toUserInfoResponse(User user) {
        String role = resolveRole(user);
        String position = resolvePosition(user);
        Set<String> permissions = resolvePermissions(user);

        return new UserInfoResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getPhone(),
                user.getEmail(),
                role,
                position,
                permissions
        );
    }

    private String resolveRole(User user) {
        Set<Role> roles = user.getRoles();
        if (roles == null || roles.isEmpty()) {
            return "";
        }
        return roles.stream()
                .filter(Role::getEnabled)
                .min(Comparator.comparing(Role::getSort, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(Role::getCode)
                .orElse("");
    }

    private String resolvePosition(User user) {
        Set<Post> posts = user.getPosts();
        if (posts == null || posts.isEmpty()) {
            return "";
        }
        return posts.stream()
                .filter(p -> p != null && p.getCode() != null)
                .findFirst()
                .map(Post::getCode)
                .orElse("");
    }

    private Set<String> resolvePermissions(User user) {
        Set<String> permissions = new HashSet<>();
        Set<Post> posts = user.getPosts();
        if (posts != null) {
            for (Post post : posts) {
                if (post != null && post.getFunctions() != null) {
                    for (PermissionFunction function : post.getFunctions()) {
                        if (function != null && Boolean.TRUE.equals(function.getEnabled()) && function.getCode() != null) {
                            permissions.add(function.getCode());
                        }
                    }
                }
            }
        }
        Set<Role> roles = user.getRoles();
        if (roles != null) {
            for (Role role : roles) {
                if (role != null && role.getPosts() != null) {
                    for (Post post : role.getPosts()) {
                        if (post != null && post.getFunctions() != null) {
                            for (PermissionFunction function : post.getFunctions()) {
                                if (function != null && Boolean.TRUE.equals(function.getEnabled()) && function.getCode() != null) {
                                    permissions.add(function.getCode());
                                }
                            }
                        }
                    }
                }
            }
        }
        return permissions;
    }
}
