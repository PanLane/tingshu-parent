package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.album.service.TrackStatService;
import com.atguigu.tingshu.model.album.TrackStat;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class TrackStatServiceImpl extends ServiceImpl<TrackStatMapper, TrackStat> implements TrackStatService {
}
