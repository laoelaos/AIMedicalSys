package com.aimedical.modules.commonmodule.service.impl;

import com.aimedical.common.base.MenuType;
import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.GlobalErrorCode;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

        // 过滤启用的功能并转换为响应列表
        // 注：deleted 过滤由 BaseEntity 的 @SQLRestriction("deleted = false") 在 SQL 层自动处理，无需 Java 层重复过滤
        List<MenuResponse> menus = functions.stream()
                .filter(f -> Boolean.TRUE.equals(f.getEnabled()))
                .map(this::convertToMenuResponse)
                .sorted(Comparator.comparing(MenuResponse::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        // 构建树形结构
        return buildMenuTree(menus);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuResponse> getAllMenus() {
        // 注：deleted 过滤由 BaseEntity 的 @SQLRestriction("deleted = false") 在 SQL 层自动处理
        List<MenuResponse> menus = functionRepository.findAll().stream()
                .map(this::convertToMenuResponse)
                .sorted(Comparator.comparing(MenuResponse::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        // 构建树形结构
        return buildMenuTree(menus);
    }

    @Override
    public MenuResponse createMenu(MenuCreateRequest request) {
        // code 唯一性预校验，避免触发 DB 约束违例
        if (functionRepository.existsByCode(request.getCode())) {
            throw new BusinessException(GlobalErrorCode.PARAM_INVALID, "菜单编码已存在: " + request.getCode());
        }

        Function function = new Function();
        function.setName(request.getName());
        function.setCode(request.getCode());
        function.setDescription(request.getDescription());

        // 设置父菜单
        if (request.getParentId() != null) {
            Function parent = functionRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BusinessException(GlobalErrorCode.PARAM_INVALID, "父菜单不存在"));
            function.setParent(parent);
        }

        function.setPath(request.getPath());
        function.setIcon(request.getIcon());
        function.setType(request.getType().getCode());
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
            // code 唯一性预校验（排除自身）
            if (!request.getCode().equals(function.getCode())
                    && functionRepository.existsByCode(request.getCode())) {
                throw new BusinessException(GlobalErrorCode.PARAM_INVALID, "菜单编码已存在: " + request.getCode());
            }
            function.setCode(request.getCode());
        }
        if (request.getDescription() != null) {
            function.setDescription(request.getDescription());
        }
        if (request.getParentId() != null) {
            // 自引用校验：parentId 不能等于当前菜单 id
            if (request.getParentId().equals(id)) {
                throw new BusinessException(GlobalErrorCode.PARAM_INVALID, "父菜单不能是自身");
            }
            Function parent = functionRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BusinessException(GlobalErrorCode.PARAM_INVALID, "父菜单不存在"));
            function.setParent(parent);
        }
        if (request.getPath() != null) {
            function.setPath(request.getPath());
        }
        if (request.getIcon() != null) {
            function.setIcon(request.getIcon());
        }
        if (request.getType() != null) {
            function.setType(request.getType().getCode());
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
        // 检查是否存在子功能，避免产生孤立记录
        List<Function> children = functionRepository.findByParentId(id);
        if (children != null && !children.isEmpty()) {
            throw new BusinessException(GlobalErrorCode.PARAM_INVALID, "存在子菜单，无法删除，请先删除子菜单");
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

    /**
     * 将Function实体转换为MenuResponse
     *
     * <p>注意：本方法只填充自身字段，children 由 buildMenuTree 统一构建。
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
        response.setParentId(function.getParent() != null ? function.getParent().getId() : null);
        response.setType(function.getType());
        response.setVisible(function.getVisible());
        response.setEnabled(function.getEnabled());
        return response;
    }

    /**
     * 构建菜单树
     *
     * <p>将扁平的菜单列表按 parentId 组装为树形结构。
     * 顶层节点为 parentId 为 null 的菜单。
     *
     * <p>children 语义（T9 修复）：
     * <ul>
     *   <li>叶子节点（无子菜单）：children=null，JSON 中省略该字段，语义明确</li>
     *   <li>父节点（有子菜单）：children=非空列表</li>
     * </ul>
     * 前端可通过 children 是否为 null 判断是否为叶子节点，无需依赖空列表的歧义语义。
     *
     * @param menus 扁平菜单列表
     * @return 树形菜单列表
     */
    private List<MenuResponse> buildMenuTree(List<MenuResponse> menus) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }

        // 第一遍：建立 id -> menu 映射，children 保持 null（叶子节点语义）
        Map<Long, MenuResponse> idToMenu = new LinkedHashMap<>();
        for (MenuResponse menu : menus) {
            idToMenu.put(menu.getId(), menu);
        }

        // 第二遍：按 parentId 挂载子节点，仅父节点才初始化 children 列表
        List<MenuResponse> roots = new ArrayList<>();
        for (MenuResponse menu : menus) {
            Long parentId = menu.getParentId();
            if (parentId == null) {
                roots.add(menu);
            } else {
                MenuResponse parent = idToMenu.get(parentId);
                if (parent != null) {
                    // 父节点首次挂载子节点时初始化 children 列表
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(menu);
                } else {
                    // 父节点不在当前列表中（可能无权限），作为顶层节点处理
                    roots.add(menu);
                }
            }
        }

        return roots;
    }
}
