package com.atguigu.tingshu.user.mapper;

import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserPaidTrackMapper extends BaseMapper<UserPaidTrack> {

    /**
     * 根据专辑id和用户id查询用户已购买的声音id集合
     * @param albumId
     * @param userId
     * @return
     */
    @Select("select track_id from user_paid_track where is_deleted = 0 and album_id=#{albumId} and  user_id = #{userId}")
    List<Long> selectPaidTrackIdtList(@Param("albumId") Long albumId,@Param("userId") Long userId);
}
