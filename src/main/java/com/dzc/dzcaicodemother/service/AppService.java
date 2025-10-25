package com.dzc.dzcaicodemother.service;

import com.dzc.dzcaicodemother.model.dto.app.AppQueryRequest;
import com.dzc.dzcaicodemother.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.dzc.dzcaicodemother.model.entity.App;

import java.util.List;

/**
 *  服务层。
 *
 * @author dzc
 */
public interface AppService extends IService<App> {

    /**
     * App 关联信息的方法
     * @param app
     * @return
     */
    AppVO getAppVO(App app);

    /**
     * 构造App 查询对象
     * @param appQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 构造分页查询对象
     * @param appList
     * @return
     */
    List<AppVO> getAppVOList(List<App> appList);
}
