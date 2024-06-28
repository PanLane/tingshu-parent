package com.atguigu.tingshu.account.service.impl;

import com.atguigu.tingshu.account.mapper.RechargeInfoMapper;
import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class RechargeInfoServiceImpl extends ServiceImpl<RechargeInfoMapper, RechargeInfo> implements RechargeInfoService {

	@Autowired
	private RechargeInfoMapper rechargeInfoMapper;

	@Override
	public String submitRecharge(RechargeInfoVo rechargeInfoVo) {
		String orderNo = UUID.randomUUID().toString().replace("-", "");
		RechargeInfo rechargeInfo = new RechargeInfo();
		rechargeInfo.setUserId(AuthContextHolder.getUserId());
		rechargeInfo.setOrderNo(orderNo);
		rechargeInfo.setRechargeStatus(SystemConstant.ORDER_STATUS_UNPAID);
		rechargeInfo.setRechargeAmount(rechargeInfoVo.getAmount());
		rechargeInfo.setPayWay(rechargeInfoVo.getPayWay());
		rechargeInfoMapper.insert(rechargeInfo);
		return orderNo;
	}
}
