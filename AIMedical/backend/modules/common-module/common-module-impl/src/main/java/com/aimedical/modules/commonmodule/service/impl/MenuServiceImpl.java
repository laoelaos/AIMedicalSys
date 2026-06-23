package com.aimedical.modules.commonmodule.service.impl;

import com.aimedical.modules.commonmodule.dto.request.MenuCreateRequest;
import com.aimedical.modules.commonmodule.dto.request.MenuUpdateRequest;
import com.aimedical.modules.commonmodule.dto.response.MenuResponse;
import com.aimedical.modules.commonmodule.permission.Function;
import com.aimedical.modules.commonmodule.permission.FunctionRepository;
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
import org.springframework.transaction.annotation.Transactional;

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
@Transactional
public class MenuServiceImpl implements MenuService {

    private final UserRepository userRepository;
    private final FunctionRepository functionRepository;

    public MenuServiceImpl(UserRepository userRepository, FunctionRepository functionRepository) {
        this.userRepository = userRepository;
        this.functionRepository = functionRepository;
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public List<MenuResponse> getAllMenus() {
        return functionRepository.findAll().stream()
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                .map(this::convertToMenuResponse)
                .sorted(Comparator.comparing(MenuResponse::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public MenuResponse createMenu(MenuCreateRequest request) {
        Function function = new Function();
        function.setName(request.getName());
        function.setCode(request.getCode());
        function.setDescription(request.getDescription());

        // 设置父菜单
        if (request.getParentId() != null) {
            function.setParent(functionRepository.findById(request.getParentId()).orElse(null));
        }

        function.setPath(request.getPath());
        function.setIcon(request.getIcon());
        function.setType(request.getType());
        function.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        function.setVisible(request.getVisible() != null ? request.getVisible() : true);
        function.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        function.setDeleted(false);

        Function savedFunction = functionRepository.save(function);
        return convertToMenuResponse(savedFunction);
    }

    @Override
    public MenuResponse updateMenu(Long id, MenuUpdateRequest request) {
        Optional<Function> functionOptional = functionRepository.findById(id);
        if (functionOptional.isEmpty()) {
            return null;
        }

        Function function = functionOptional.get();

        if (request.getName() != null) {
            function.setName(request.getName());
        }
        if (request.getCode() != null) {
            function.setCode(request.getCode());
        }
        if (request.getDescription() != null) {
            function.setDescription(request.getDescription());
        }
        if (request.getParentId() != null) {
            function.setParent(functionRepository.findById(request.getParentId()).orElse(null));
        }
        if (request.getPath() != null) {
            function.setPath(request.getPath());
        }
        if (request.getIcon() != null) {
            function.setIcon(request.getIcon());
        }
        if (request.getType() != null) {
            function.setType(request.getType());
        }
        if (request.getSortOrder() != null) {
            function.setSortOrder(request.getSortOrder());
        }
        if (request.getVisible() != null) {
            function.setVisible(request.getVisible());
        }
        if (request.getEnabled() != null) {
            function.setEnabled(request.getEnabled());
        }

        Function savedFunction = functionRepository.save(function);
        return convertToMenuResponse(savedFunction);
    }

    @Override
    public void deleteMenu(Long id) {
        functionRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public MenuResponse getMenuById(Long id) {
        return functionRepository.findById(id)
                .map(this::convertToMenuResponse)
                .orElse(null);
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
        response.setPath(function.getPath());
        response.setIcon(function.getIcon());
        response.setPermission(function.getCode());
        response.setSortOrder(function.getSortOrder());
        response.setChildren(null);
        return response;
    }
}