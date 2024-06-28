package com.atguigu.tingshu.account.service.impl;

import com.atguigu.tingshu.account.mapper.RechargeInfoMapper;
import com.atguigu.tingshu.account.mapper.UserAccountDetailMapper;
import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.atguigu.tingshu.vo.account.AccountMinusVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

	@Autowired
	private UserAccountMapper userAccountMapper;
	@Autowired
	private RedisTemplate redisTemplate;
	@Autowired
	private UserAccountDetailMapper userAccountDetailMapper;
	@Autowired
	private RechargeInfoMapper rechargeInfoMapper;

	@Override
	@Transactional
	public AccountLockResultVo checkAndLock(AccountLockVo accountLockVo) {
		//获取订单号
		String orderNo = accountLockVo.getOrderNo();
		//检验订单号
		if(!StringUtils.hasText(orderNo)) throw new GuiguException(ResultCodeEnum.DATA_ERROR);

		//定义变量
		AccountLockResultVo accountLockResultVo = new AccountLockResultVo();//返回结果
		Long userId = accountLockVo.getUserId();//获取用户id
		BigDecimal amount = accountLockVo.getAmount();
		String lockKey = "checkAndLock" + orderNo;//防止表单重复提交的key
		String dataKey = "account:lock:" + orderNo;//通过这个key获取已提交数据
		//判断表单是否重复提交
		Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1",1, TimeUnit.HOURS);
		if(!locked){
			//表单重复提交，看是否能获取到数据
			 accountLockResultVo = (AccountLockResultVo) redisTemplate.opsForValue().get(dataKey);
			 if(accountLockResultVo == null){
				 //没获取到结果，抛异常
				 throw new GuiguException(ResultCodeEnum.ACCOUNT_LOCK_REPEAT);
			 }
			 //获取到结果，直接返回
			 return accountLockResultVo;
		}
		//校验账户余额 select * from user_account where user_id = ? and available_amount >= ? and is_deleted = 0 for update
		UserAccount userAccount = userAccountMapper.check(userId, amount);

		if(userAccount == null){
			//账户余额不足，删除防止重复提交key，抛异常
			redisTemplate.delete(lockKey);
			throw new GuiguException(ResultCodeEnum.ACCOUNT_LESS);
		}

		//锁定账户,update user_account set lock_amount = lock_amount + ?,available_amount = available_amount - ? where user_id = ? and available_amount >= ? and is_deleted = 0
		int lock = userAccountMapper.lock(userId, amount);
		if(lock<=0){
			//锁定失败，删除防止重复提交key，抛异常
			redisTemplate.delete(lockKey);
			throw new GuiguException(ResultCodeEnum.ACCOUNT_LOCK_ERROR);
		}

		//给结果对象赋值
		accountLockResultVo.setUserId(userId);
		accountLockResultVo.setContent(accountLockVo.getContent());
		accountLockResultVo.setAmount(amount);

		//将结果存储到redis中
		redisTemplate.opsForValue().set(dataKey,accountLockResultVo,1,TimeUnit.HOURS);

		//记录账户锁定记录
		this.log(accountLockResultVo,"lock:"+accountLockVo.getContent(),SystemConstant.ACCOUNT_TRADE_TYPE_LOCK,orderNo);

		//返回数据
		return accountLockResultVo;
	}

	@Override
	public void registerAccount(Long userId) {
		UserAccount userAccount = new UserAccount();
		userAccount.setUserId(userId);
		userAccount.setTotalAmount(new BigDecimal(1000));
		userAccount.setAvailableAmount(new BigDecimal(1000));
		userAccount.setTotalIncomeAmount(new BigDecimal(1000));
		userAccountMapper.insert(userAccount);
	}

	@Override
	public void unlockAccount(String orderNo) {
		String lockKey = "unlock:" + orderNo;//防止表单重复提交的key
		String dataKey = "account:lock:" + orderNo;//通过这个key获取已提交数据
		Boolean notUnlocked = redisTemplate.opsForValue().setIfAbsent(lockKey, orderNo, 1, TimeUnit.HOURS);
		if(!notUnlocked){
			//已经解锁过，结束
			return;
		}
		//从redis中获取数据
		AccountLockResultVo accountLockResultVo = (AccountLockResultVo) redisTemplate.opsForValue().get(dataKey);
		if(accountLockResultVo == null){
			//没获取到数据，结束
			return;
		}

		//解锁账户
		int unlock = userAccountMapper.unlock(accountLockResultVo.getUserId(), accountLockResultVo.getAmount());
		if(unlock<=0){
			//解锁失败，记录日志
			log.error("-----------解锁失败----------");
			//删除防止重复提交key
			redisTemplate.delete(lockKey);
		}

		//记录账户解锁记录
		this.log(accountLockResultVo,"unlock:"+accountLockResultVo.getContent(),SystemConstant.ACCOUNT_TRADE_TYPE_UNLOCK,orderNo);
		//删除缓存中的数据
		redisTemplate.delete(dataKey);
	}

	@Override
	public void minusAccount(String orderNo) {
		String lockKey = "minus:" + orderNo;//防止表单重复提交的key
		String dataKey = "account:lock:" + orderNo;//通过这个key获取已提交数据
		Boolean unmisnused = redisTemplate.opsForValue().setIfAbsent(lockKey, orderNo, 1, TimeUnit.HOURS);
		if(!unmisnused){
			//已经解锁过，结束
			return;
		}
		//从redis中获取数据
		AccountLockResultVo accountLockResultVo = (AccountLockResultVo) redisTemplate.opsForValue().get(dataKey);
		if(accountLockResultVo == null){
			//没获取到数据，结束
			return;
		}

		//扣款
		int minus = userAccountMapper.minus(accountLockResultVo.getUserId(), accountLockResultVo.getAmount());

		if(minus<=0){
			//扣款失败，记录日志
			log.error("-----------扣款失败----------");
			//删除防止重复提交key
			redisTemplate.delete(lockKey);
		}

		log.info("-----------扣款成功----------");
		//记录账户扣款记录
		this.log(accountLockResultVo,"minus:"+accountLockResultVo.getContent(),SystemConstant.ACCOUNT_TRADE_TYPE_MINUS,orderNo);
		//删除缓存中的数据
		redisTemplate.delete(dataKey);
	}

	@Override
	@Transactional
	public void rechargePaySuccess(String orderNo) {
		//修改充值状态
		RechargeInfo rechargeInfo = rechargeInfoMapper.selectOne(new LambdaQueryWrapper<RechargeInfo>().eq(RechargeInfo::getOrderNo, orderNo));
		rechargeInfo.setRechargeStatus(SystemConstant.ORDER_STATUS_PAID);
		rechargeInfoMapper.updateById(rechargeInfo);
		//充值成功后,更新账户余额
		userAccountMapper.updateUserAccount(rechargeInfo.getUserId(), rechargeInfo.getRechargeAmount());
		//添加账户流水
		log(new AccountLockResultVo(rechargeInfo.getUserId(),rechargeInfo.getRechargeAmount(),""),"成功充值:"+rechargeInfo.getRechargeAmount(),SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT,orderNo);
	}

	@Override
	public IPage<UserAccountDetail> findUserConsumePage(Page<UserAccountDetail> page) {
		return userAccountDetailMapper.selectUserConsumePage(page,AuthContextHolder.getUserId());
	}

	@Override
	public IPage<UserAccountDetail> findUserRechargePage(Page<UserAccountDetail> page) {
		return userAccountDetailMapper.selectUserRechargePage(page,AuthContextHolder.getUserId());
	}

	@Override
	@Transactional
	public AccountLockResultVo checkAndMinus(AccountMinusVo accountMinusVo) {
		//获取订单号
		String orderNo = accountMinusVo.getOrderNo();
		//检验订单号
		if(!StringUtils.hasText(orderNo)) throw new GuiguException(ResultCodeEnum.DATA_ERROR);

		//定义变量
		AccountLockResultVo accountLockResultVo = new AccountLockResultVo();//返回结果
		Long userId = accountMinusVo.getUserId();//获取用户id
		BigDecimal amount = accountMinusVo.getAmount();
		String lockKey = "accountMinusVo" + orderNo;//防止表单重复提交的key
		//判断表单是否重复提交
		Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1",1, TimeUnit.HOURS);
		if(!locked){
			//表单重复提交，抛异常
			throw new GuiguException(ResultCodeEnum.ACCOUNT_LOCK_REPEAT);
		}

		//检查并扣减账户金额
		int minus = userAccountMapper.checkAndMinus(userId, amount);
		if(minus<=0){
			//失败，删除防止重复提交key，抛异常
			redisTemplate.delete(lockKey);
			throw new GuiguException(ResultCodeEnum.ACCOUNT_LOCK_ERROR);
		}

		//记录账户锁定记录
		accountLockResultVo.setUserId(userId);
		accountLockResultVo.setContent("lockAndMinus"+accountMinusVo.getContent());
		accountLockResultVo.setAmount(amount);
		this.log(accountLockResultVo,"checkAndMinus:"+accountMinusVo.getContent(),SystemConstant.ACCOUNT_TRADE_TYPE_MINUS,orderNo);

		//返回数据
		return accountLockResultVo;
	}

	/**
	 * 记录账户资金流向日志
	 * @param accountLockResultVo
	 */
	private void log(AccountLockResultVo accountLockResultVo,String title,String tradeType,String OrderNo) {
		UserAccountDetail userAccountDetail = new UserAccountDetail();
		userAccountDetail.setUserId(accountLockResultVo.getUserId());
		userAccountDetail.setTitle(title);
		userAccountDetail.setTradeType(tradeType);
		userAccountDetail.setAmount(accountLockResultVo.getAmount());
		userAccountDetail.setOrderNo(OrderNo);
		userAccountDetailMapper.insert(userAccountDetail);
	}


}
