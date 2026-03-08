package com.history.creation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.history.creation.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
