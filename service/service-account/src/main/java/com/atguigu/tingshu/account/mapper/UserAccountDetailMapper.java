package com.atguigu.tingshu.account.mapper;

import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserAccountDetailMapper extends BaseMapper<UserAccountDetail> {

    /**
     * 获取用户消费记录
     * @param page
     * @param userId
     * @return
     */
    @Select("select * from user_account_detail where is_deleted = 0 and user_id = #{userId} and trade_type = 1204 order by id desc")
    IPage<UserAccountDetail> selectUserConsumePage(Page<UserAccountDetail> page, Long userId);

    /**
     * 获取用户充值记录
     * @param page
     * @param userId
     * @return
     */
    @Select("select * from user_account_detail where is_deleted = 0 and user_id = #{userId} and trade_type = 1201 order by id desc")
    IPage<UserAccountDetail> selectUserRechargePage(Page<UserAccountDetail> page, Long userId);
}
