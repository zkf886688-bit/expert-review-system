package com.review.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.review.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("""
            SELECT r.code FROM sys_role r
            INNER JOIN sys_user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
            """)
    List<String> selectRoleCodesByUserId(Long userId);

    @Select("SELECT id FROM sys_user WHERE expert_id = #{expertId} AND status = 1 LIMIT 1")
    Long selectUserIdByExpertId(Long expertId);
}
