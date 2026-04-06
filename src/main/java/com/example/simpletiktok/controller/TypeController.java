package com.example.simpletiktok.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.simpletiktok.pojo.dto.TypeSaveDTO;
import com.example.simpletiktok.pojo.entity.Type;
import com.example.simpletiktok.pojo.entity.User;
import com.example.simpletiktok.pojo.vo.TypeVO;
import com.example.simpletiktok.service.ITypeService;
import com.example.simpletiktok.util.R;
import com.example.simpletiktok.util.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/type")
@RequiredArgsConstructor
public class TypeController {

    private final ITypeService typeService;

    /**
     * 保存分类。
     * 1. dto.id 为空：新增分类。
     * 2. dto.id 不为空：编辑分类（复用同一接口）。
     */
    @PostMapping("/save")
    public R<?> save(@RequestBody TypeSaveDTO dto) {
        if (dto == null || StrUtil.isBlank(dto.getName())) {
            return R.error().message("分类名称不能为空");
        }

        String name = dto.getName().trim();
        Long id = dto.getId();
        boolean isUpdate = id != null && id > 0;

        if (!isUpdate) {
            boolean exists = typeService.exists(
                    Wrappers.<Type>lambdaQuery()
                            .eq(Type::getName, name)
                            .eq(Type::getIsDeleted, 0)
            );
            if (exists) {
                return R.error().message("分类名称已存在");
            }

            Type type = BeanUtil.copyProperties(dto, Type.class);
            type.setName(name);
            if (type.getOpen() == null) {
                type.setOpen(1);
            }
            if (type.getSort() == null) {
                type.setSort(0);
            }
            type.setIsDeleted(0);
            boolean ok = typeService.save(type);
            return ok ? R.ok().data(type.getId()) : R.error().message("新增分类失败");
        }

        Type oldType = typeService.getOne(
                Wrappers.<Type>lambdaQuery()
                        .eq(Type::getId, id)
                        .eq(Type::getIsDeleted, 0)
        );
        if (oldType == null) {
            return R.error().message("分类不存在");
        }

        boolean duplicate = typeService.exists(
                Wrappers.<Type>lambdaQuery()
                        .eq(Type::getName, name)
                        .eq(Type::getIsDeleted, 0)
                        .ne(Type::getId, id)
        );
        if (duplicate) {
            return R.error().message("分类名称已存在");
        }

        Type type = BeanUtil.copyProperties(dto, Type.class);
        type.setId(id);
        type.setName(name);
        if (type.getOpen() == null) {
            type.setOpen(oldType.getOpen());
        }
        if (type.getSort() == null) {
            type.setSort(oldType.getSort());
        }
        boolean ok = typeService.updateById(type);
        return ok ? R.ok().data(type.getId()) : R.error().message("编辑分类失败");
    }
    /**
     * 删除分类。
     * 仅删除分类表当前行，不处理标签表和向量数据库。
     */
    @PostMapping("/delete/{id}")
    public R<?> delete(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return R.error().message("分类 ID 非法");
        }

        boolean exists = typeService.exists(
                Wrappers.<Type>lambdaQuery()
                        .eq(Type::getId, id)
                        .eq(Type::getIsDeleted, 0)
        );
        if (!exists) {
            return R.error().message("分类不存在");
        }

        boolean ok = typeService.removeById(id);
        return ok ? R.ok().message("删除成功") : R.error().message("删除失败");
    }

    /**
     * 查询所有公开分类。
     */
    @GetMapping("/list")
    public R<?> list() {
        List<Type> list = typeService.listOpen();
        List<TypeVO> result = list.stream()
                .map(item -> BeanUtil.copyProperties(item, TypeVO.class))
                .collect(Collectors.toList());
        return R.ok().data(result);
    }

    /**
     * 查询分类详情（仅公开分类）。
     */
    @GetMapping("/{id}")
    public R<?> detail(@PathVariable Long id) {
        Type type = typeService.getOpenById(id);
        if (type == null) {
            return R.error().message("分类不存在");
        }
        return R.ok().data(BeanUtil.copyProperties(type, TypeVO.class));
    }

    /**
     * 查询当前用户已订阅的分类 ID 列表。
     */
    @GetMapping("/subscribe")
    public R<?> listSubscribe() {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("用户未登录");
        }
        List<Long> ids = typeService.listUserSubscribeTypeIds(current.getId());
        return R.ok().data(ids);
    }

    /**
     * 查询当前用户未订阅的公开分类列表。
     */
    @GetMapping("/noSubscribe")
    public R<?> listNoSubscribe() {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("用户未登录");
        }
        List<Type> list = typeService.listUserNoSubscribeTypes(current.getId());
        List<TypeVO> result = list.stream()
                .map(item -> BeanUtil.copyProperties(item, TypeVO.class))
                .collect(Collectors.toList());
        return R.ok().data(result);
    }

    /**
     * 批量覆盖当前用户订阅分类。
     * 参数示例：types=1,2,5。
     */
    @PostMapping("/subscribe/batch")
    public R<?> subscribeBatch(@RequestParam(required = false) String types) {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("用户未登录");
        }

        List<Long> typeIds = new ArrayList<>();
        if (StrUtil.isNotBlank(types)) {
            String[] arr = types.split(",");
            for (String item : arr) {
                String value = item == null ? "" : item.trim();
                if (value.isEmpty()) {
                    continue;
                }
                try {
                    typeIds.add(Long.parseLong(value));
                } catch (NumberFormatException e) {
                    return R.error().message("types 参数格式错误");
                }
            }
        }

        boolean ok = typeService.replaceSubscribeTypes(current.getId(), typeIds);
        return ok ? R.ok().message("保存成功") : R.error().message("保存失败");
    }

    /**
     * 订阅分类。
     */
    @PostMapping("/subscribe/{typeId}")
    public R<?> subscribe(@PathVariable Long typeId) {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("用户未登录");
        }
        Type type = typeService.getOpenById(typeId);
        if (type == null) {
            return R.error().message("分类不存在");
        }
        boolean ok = typeService.subscribeType(current.getId(), typeId);
        return ok ? R.ok() : R.error().message("订阅失败");
    }

    /**
     * 取消订阅分类。
     */
    @PostMapping("/unsubscribe/{typeId}")
    public R<?> unsubscribe(@PathVariable Long typeId) {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("用户未登录");
        }
        boolean ok = typeService.unsubscribeType(current.getId(), typeId);
        return ok ? R.ok() : R.error().message("取消订阅失败");
    }
}

