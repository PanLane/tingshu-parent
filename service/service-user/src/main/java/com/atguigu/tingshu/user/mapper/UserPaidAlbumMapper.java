package com.atguigu.tingshu.user.mapper;

import com.atguigu.tingshu.model.user.UserPaidAlbum;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.security.core.parameters.P;

import java.util.List;

@Mapper
public interface UserPaidAlbumMapper extends BaseMapper<UserPaidAlbum> {
}
