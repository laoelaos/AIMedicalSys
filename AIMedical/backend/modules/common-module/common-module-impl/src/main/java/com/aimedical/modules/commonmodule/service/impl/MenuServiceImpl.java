package com.aimedical.modules.commonmodule.service.impl;

import com.aimedical.modules.commonmodule.dto.response.MenuResponse;
import com.aimedical.modules.commonmodule.permission.Function;
import com.aimedical.modules.commonmodule.permission.Post;
import com.aimedical.modules.commonmodule.permission.User;
import com.aimedical.modules.commonmodule.permission.UserRepository;
import com.aimedical.modules.commonmodule.service.MenuService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

/**
 * 菜单服务实现
 *
 * <p>实现菜单相关的业务逻辑。
 * Phase1版本：基于Function实体构建菜单树，支持按岗位过滤。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Service
public class MenuServiceImpl implements MenuService {

    private final UserRepository userRepository;

    public MenuServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<MenuResponse> getUserMenuTree(Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return new ArrayList<>();
        }

        User user = userOptional.get();
        Set<Function> functions = new HashSet<>();

        // 获取用户所有岗位的功能
        if (user.getPosts() != null) {
            for (Post post : user.getPosts()) {
                if (post.getFunctions() != null) {
                    functions.addAll(post.getFunctions());
                }
            }
        }

        // 转换为菜单响应列表
        List<MenuResponse> menus = functions.stream()
                .filter(f -> Boolean.TRUE.equals(f.getEnabled()))
                .map(this::convertToMenuResponse)
                .sorted(Comparator.comparing(MenuResponse::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        // Phase1: 返回扁平列表（暂不支持树形结构）
        return menus;
    }

    @Override
    public List<MenuResponse> getAllMenus() {
        // Phase1: 返回空列表（管理员菜单管理功能待后续实现）
        return new ArrayList<>();
    }

    /**
     * 将Function实体转换为MenuResponse
     *
     * @param function 功能实体
     * @return 菜单响应
     */
    private MenuResponse convertToMenuResponse(Function function) {
        MenuResponse response = new MenuResponse();
        response.setId(function.getId());
        response.setName(function.getName());
        response.setPath("/" + function.getCode().toLowerCase().replace("_", "-"));
        response.setIcon(null);
        response.setPermission(function.getCode());
        response.setSortOrder(0);
        response.setChildren(null);
        return response;
    }
}