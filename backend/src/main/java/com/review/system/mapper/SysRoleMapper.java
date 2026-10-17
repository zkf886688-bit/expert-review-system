package com.review.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.review.system.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("SELECT * FROM sys_role ORDER BY id")
    List<SysRole> selectAllRoles();
}
