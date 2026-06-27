package com.aimedical.modules.commonmodule.service.impl;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.modules.commonmodule.dto.request.MenuCreateRequest;
import com.aimedical.modules.commonmodule.dto.request.MenuUpdateRequest;
import com.aimedical.modules.commonmodule.dto.response.MenuResponse;
import com.aimedical.modules.commonmodule.permission.PermissionFunction;
import com.aimedical.modules.commonmodule.permission.PermissionFunctionRepository;
import com.aimedical.modules.commonmodule.permission.Post;
import com.aimedical.modules.commonmodule.permission.User;
import com.aimedical.modules.commonmodule.permission.UserRepository;
import com.aimedical.modules.commonmodule.service.MenuService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MenuServiceImpl implements MenuService {

    private final UserRepository userRepository;
    private final PermissionFunctionRepository functionRepository;

    public MenuServiceImpl(UserRepository userRepository, PermissionFunctionRepository functionRepository) {
        this.userRepository = userRepository;
        this.functionRepository = functionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuResponse> getUserMenuTree(Long userId) {
        Optional<User> userOptional = userRepository.findWithDetailsForMenuById(userId);
        if (userOptional.isEmpty()) {
            return new ArrayList<>();
        }

        User user = userOptional.get();
        Set<PermissionFunction> functions = new HashSet<>();

        if (user.getPosts() != null) {
            for (Post post : user.getPosts()) {
                if (post.getFunctions() != null) {
                    functions.addAll(post.getFunctions());
                }
            }
        }

        List<MenuResponse> menus = functions.stream()
                .filter(f -> Boolean.TRUE.equals(f.getEnabled()))
                .map(this::convertToMenuResponse)
                .sorted(Comparator.comparing(MenuResponse::sort, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        Map<Long, Long> parentIdMap = new HashMap<>();
        functions.stream()
                .filter(f -> Boolean.TRUE.equals(f.getEnabled()))
                .forEach(f -> parentIdMap.put(f.getId(), f.getParent() != null ? f.getParent().getId() : null));

        return buildMenuTree(menus, parentIdMap);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuResponse> getAllMenus() {
        List<PermissionFunction> allPermissionFunctions = functionRepository.findAll();
        List<MenuResponse> menus = allPermissionFunctions.stream()
                .map(this::convertToMenuResponse)
                .sorted(Comparator.comparing(MenuResponse::sort, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        Map<Long, Long> parentIdMap = new HashMap<>();
        for (PermissionFunction f : allPermissionFunctions) {
            parentIdMap.put(f.getId(), f.getParent() != null ? f.getParent().getId() : null);
        }

        return buildMenuTree(menus, parentIdMap);
    }

    @Override
    public MenuResponse createMenu(MenuCreateRequest request) {
        if (functionRepository.existsByCode(request.permission())) {
            throw new BusinessException(GlobalErrorCode.PARAM_INVALID, "菜单编码已存在: " + request.permission());
        }

        PermissionFunction function = new PermissionFunction();
        function.setName(request.name());
        function.setCode(request.permission());

        if (request.parentId() != null) {
            PermissionFunction parent = functionRepository.findById(request.parentId())
                    .orElseThrow(() -> new BusinessException(GlobalErrorCode.PARAM_INVALID, "父菜单不存在"));
            function.setParent(parent);
        }

        function.setPath(request.path());
        function.setIcon(request.icon());
        function.setSortOrder(request.sort() != null ? request.sort() : 0);
        function.setVisible(request.visible() != null ? request.visible() : true);
        function.setDeleted(false);

        PermissionFunction savedPermissionFunction = functionRepository.save(function);
        return convertToMenuResponse(savedPermissionFunction);
    }

    @Override
    public MenuResponse updateMenu(Long id, MenuUpdateRequest request) {
        Optional<PermissionFunction> functionOptional = functionRepository.findById(id);
        if (functionOptional.isEmpty()) {
            return null;
        }

        PermissionFunction function = functionOptional.get();

        if (request.getName() != null) {
            function.setName(request.getName());
        }
        if (request.getPermission() != null) {
            if (!request.getPermission().equals(function.getCode())
                    && functionRepository.existsByCode(request.getPermission())) {
                throw new BusinessException(GlobalErrorCode.PARAM_INVALID, "菜单编码已存在: " + request.getPermission());
            }
            function.setCode(request.getPermission());
        }
        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new BusinessException(GlobalErrorCode.PARAM_INVALID, "父菜单不能是自身");
            }
            PermissionFunction parent = functionRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BusinessException(GlobalErrorCode.PARAM_INVALID, "父菜单不存在"));
            function.setParent(parent);
        }
        if (request.getPath() != null) {
            function.setPath(request.getPath());
        }
        if (request.getIcon() != null) {
            function.setIcon(request.getIcon());
        }
        if (request.getSort() != null) {
            function.setSortOrder(request.getSort());
        }
        if (request.getVisible() != null) {
            function.setVisible(request.getVisible());
        }

        PermissionFunction savedPermissionFunction = functionRepository.save(function);
        return convertToMenuResponse(savedPermissionFunction);
    }

    @Override
    public void deleteMenu(Long id) {
        List<PermissionFunction> children = functionRepository.findByParentId(id);
        if (children != null && !children.isEmpty()) {
            throw new BusinessException(GlobalErrorCode.CHILDREN_EXIST, "存在子菜单，无法删除，请先删除子菜单");
        }

        if (!functionRepository.existsById(id)) {
            throw new BusinessException(GlobalErrorCode.NOT_FOUND, "菜单不存在");
        }
        functionRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public MenuResponse getMenuById(Long id) {
        return functionRepository.findById(id)
                .map(this::convertToMenuResponse)
                .orElse(null);
    }

    private MenuResponse convertToMenuResponse(PermissionFunction function) {
        return new MenuResponse(
                function.getId(),
                function.getName(),
                function.getPath(),
                function.getComponent(),
                function.getIcon(),
                function.getCode(),
                function.getSortOrder(),
                null
        );
    }

    private List<MenuResponse> buildMenuTree(List<MenuResponse> menus, Map<Long, Long> parentIdMap) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, MenuResponse> idToMenu = new LinkedHashMap<>();
        for (MenuResponse menu : menus) {
            idToMenu.put(menu.id(), menu);
        }

        List<Long> rootIds = new ArrayList<>();
        for (MenuResponse menu : menus) {
            Long parentId = parentIdMap.get(menu.id());
            if (parentId == null) {
                rootIds.add(menu.id());
            } else {
                MenuResponse parent = idToMenu.get(parentId);
                if (parent != null) {
                    List<MenuResponse> newChildren = new ArrayList<>();
                    if (parent.children() != null) {
                        newChildren.addAll(parent.children());
                    }
                    newChildren.add(menu);
                    idToMenu.put(parent.id(), parent.withChildren(newChildren));
                } else {
                    rootIds.add(menu.id());
                }
            }
        }

        List<MenuResponse> result = new ArrayList<>();
        for (Long rootId : rootIds) {
            result.add(idToMenu.get(rootId));
        }
        return result;
    }
}
