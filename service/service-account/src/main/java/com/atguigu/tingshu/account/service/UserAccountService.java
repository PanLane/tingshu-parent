package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.atguigu.tingshu.vo.account.AccountMinusVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserAccountService extends IService<UserAccount> {

    /**
     * 检查及锁定账户余额
     * @param accountLockVo
     * @return
     */
    AccountLockResultVo checkAndLock(AccountLockVo accountLockVo);

    /**
     * 初始化账户
     * @param userId
     */
    void registerAccount(Long userId);

    /**
     * 解锁账户
     * @param orderNo
     */
    void unlockAccount(String orderNo);

    /**
     * 扣款
     * @param orderNo
     */
    void minusAccount(String orderNo);

    /**
     * 充值支付成功，更新账户信息
     * @param orderNo
     */
    void rechargePaySuccess(String orderNo);

    /**
     * 分页查询用户下的消费记录
     * @param page
     * @return
     */
    IPage<UserAccountDetail> findUserConsumePage(Page<UserAccountDetail> page);

    /**
     * 分页查询用户下的充值记录
     * @param page
     * @return
     */
    IPage<UserAccountDetail> findUserRechargePage(Page<UserAccountDetail> page);

    /**
     * 检查及扣减账户余额
     * @param checkAndMinus
     * @return
     */
    AccountLockResultVo checkAndMinus(AccountMinusVo checkAndMinus);
}
