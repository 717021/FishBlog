package com.dreamfish.fishblog.core.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.dreamfish.fishblog.core.entity.Stat;
import com.dreamfish.fishblog.core.entity.User;
import com.dreamfish.fishblog.core.mapper.PostMapper;
import com.dreamfish.fishblog.core.mapper.StatIpMapper;
import com.dreamfish.fishblog.core.mapper.StatMapper;
import com.dreamfish.fishblog.core.repository.StatDayLogRepository;
import com.dreamfish.fishblog.core.service.StatService;
import com.dreamfish.fishblog.core.utils.Result;
import com.dreamfish.fishblog.core.utils.ResultCodeEnum;
import com.dreamfish.fishblog.core.utils.StringUtils;
import com.dreamfish.fishblog.core.utils.request.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 用户访问数据服务
 */
@Service
public class StatServiceImpl implements StatService {

    @Autowired
    private StatMapper statMapper = null;
    @Autowired
    private StatIpMapper statIpMapper = null;
    @Autowired
    private StatDayLogRepository statDayLogRepository = null;
    @Autowired
    private PostMapper postMapper = null;

    @Override
    public void setStartUpDate() {
        SimpleDateFormat sdf =  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        statMapper.updateStat("startupDate", sdf.format(new Date()));
    }

    /**
     * 总计数更新入口
     * @param data 前端URL参数JSON
     * @return 返回操作结果
     */
    @Override
    public Result updateStat(JSONObject data, HttpServletRequest request) {

        String url = data.getString("url");
        if(StringUtils.isBlank(url)) return Result.failure(ResultCodeEnum.BAD_REQUEST);

        String ip = IpUtil.getIpAddr(request);
        Integer oid = statIpMapper.isStaIpExists(ip);
        if(oid == null || oid <= 0) {
            //更新 IP 总计数
            statMapper.updateStatIncrease("ipToday");
            //插入新IP
            statIpMapper.addStatIp(ip);
        }

        statIpMapper.updateStayIpIncrease(ip);

        //更新 PV 总计数
        statMapper.updateStatIncrease("pvToday");

        //更新页访问计数
        Integer oldId = statMapper.findTodatStatPage(url);
        if(oldId != null && oldId > 0) statMapper.updateStatPageIncrease(oldId);
        else statMapper.addStatPage(url);//没有此页则添加记录

        return Result.success();
    }

    @Override
    public Result getStatRunDay() {

        Integer runDay = 0;
        Date startupDate = new Date();
        Date nowDate = new Date();

        String startupDateString = statMapper.getStat("startupDate").getData();
        SimpleDateFormat sdf =  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            startupDate = sdf.parse(startupDateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        runDay = (int) ((nowDate.getTime() - startupDate.getTime()) / (24 * 60 * 60 * 1000));

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("runDay", runDay);
        resultData.put("startupDate", startupDate);
        return Result.success(resultData);
    }

    /**
     * 获取控制台简单仪表盘参数
     * @return 简单仪表盘参数
     */
    @Override
    public Result getStatSimple() {

        Map<String, Integer> data = new HashMap<>();
        List<Stat> fdata = statMapper.getStats("'pvToday','ipToday','pvYesterday','ipYesterday','commentYesterday','commentToday'");
        data.put("pvToday", fdata.get(0).getIntData());
        data.put("ipToday", fdata.get(1).getIntData());
        data.put("pvYesterday", fdata.get(2).getIntData());
        data.put("ipYesterday", fdata.get(3).getIntData());
        data.put("commentYesterday", fdata.get(4).getIntData());
        data.put("commentToday", fdata.get(5).getIntData());
        data.put("visitorCount", statMapper.getUserCountWithLevel(User.LEVEL_GUEST));
        data.put("authorCount", statMapper.getUserCountWithLevel(User.LEVEL_WRITER) + 1);

        return Result.success(data);
    }


    @Override
    public Result getStatIpPv(String startDate, Integer maxCount) {

        Integer start = 0;
        Integer row = statMapper.getStatDayLogMonthRowNum(startDate);
        if(row != null && row >= 0)
            start = row;

        return Result.success(statMapper.getStatDayLogMonth(start, maxCount));
    }

    @Override
    public Result getStatIpPv(Integer page, Integer pageSize) {
        return Result.success(statDayLogRepository.findAllByOrderByDateDesc(PageRequest.of(page, pageSize)));
    }

    @Override
    public Result getStatIpPv() {
        return Result.success(statMapper.getStatDayLogMonthThisMonth());
    }

    @Override
    public Result getStatToday() {
        Map<String, Integer> data = new HashMap<>();
        data.put("pv", statMapper.getStat("pvToday").getIntData());
        data.put("ip", statMapper.getStat("ipToday").getIntData());
        data.put("count", postMapper.getAllPostCount());
        return Result.success(data);
    }

    @Override
    public Result getStatTopPage(Integer maxCount) {
        return Result.success(statMapper.getStatTodayTopPage(maxCount));
    }

    @Override
    public Result getStatTopPost(Integer maxCount) {
        return Result.success(statMapper.getStatTodayTopPost(maxCount));
    }
}
