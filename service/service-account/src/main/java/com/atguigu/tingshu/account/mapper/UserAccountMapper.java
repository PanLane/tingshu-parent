package com.atguigu.tingshu.account.mapper;

import com.atguigu.tingshu.model.account.UserAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    /**
     * 校验账户余额
     * @param userId
     * @param amount
     * @return
     */
    @Select("select * from user_account where user_id = #{userId} and available_amount >= #{amount} and is_deleted = 0 for update")
    UserAccount check(@Param("userId") Long userId,@Param("amount") BigDecimal amount);

    /**
     * 锁定账户余额
     * @param userId
     * @param amount
     * @return
     */
    @Update("update user_account set lock_amount = lock_amount + #{amount},available_amount = available_amount - #{amount} where user_id = #{userId} and is_deleted = 0")
    int lock(@Param("userId") Long userId,@Param("amount") BigDecimal amount);

    /**
     * 解锁账户余额
     * @param userId
     * @param amount
     * @return
     */
    @Update("""
                update user_account set lock_amount = lock_amount - #{amount},available_amount = available_amount + #{amount},total_pay_amount = total_pay_amount + #{amount}
                where user_id = #{userId} and is_deleted = 0
            """)
    int unlock(@Param("userId") Long userId,@Param("amount") BigDecimal amount);

    /**
     * 扣减账户余额
     * @param userId
     * @param amount
     * @return
     */
    @Update("update user_account set total_amount = total_amount - #{amount},lock_amount = lock_amount - #{amount} where user_id = #{userId} and is_deleted = 0")
    int minus(@Param("userId") Long userId,@Param("amount") BigDecimal amount);

    /**
     * 充值成功后,更新账户余额
     */
    @Update("""
        update user_account 
        set total_amount = total_amount+#{amount},available_amount = available_amount+#{amount},total_income_amount = total_income_amount + #{amount}
        where user_id = #{userId} and is_deleted = 0
        """)
    int updateUserAccount(@Param("userId") Long userId,@Param("amount") BigDecimal amount);

    /**
     * 检查并扣减账户余额
     * @param userId
     * @param amount
     * @return
     */
    @Update("""
        update user_account set total_amount = total_amount - #{amount},available_amount = available_amount - #{amount},total_pay_amount = total_pay_amount + #{amount}
        where user_id = #{userId} and available_amount >= #{amount} and is_deleted = 0
        """)
    int checkAndMinus(Long userId, BigDecimal amount);
}
