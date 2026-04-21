package com.dzc.dzcaicodemother.service;
import java.util.List;

import com.dzc.dzcaicodemother.model.dto.user.UserQueryRequest;
import com.dzc.dzcaicodemother.model.dto.user.UserRegisterRequest;
import com.dzc.dzcaicodemother.model.entity.User;
import com.dzc.dzcaicodemother.model.vo.CheckinStatusVO;
import com.dzc.dzcaicodemother.model.vo.LoginUserVO;
import com.dzc.dzcaicodemother.model.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户 服务层。
 *
   @author dzc
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request 请求
     * @return 当前登录用户，未登录返回 null
     */
    User getLoginUserPermitNull(HttpServletRequest request);

    /**
     * 获取脱敏后的用户信息
     *
     * @param user 用户信息
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的用户信息（分页）
     *
     * @param userList 用户列表
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 用户注销
     *
     * @param request
     * @return 退出登录是否成功
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 根据查询条件构造数据查询参数
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 加密
     *
     * @param userPassword 用户密码
     * @return 加密后的用户密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 用户签到
     *
     * @param user 当前登录用户
     * @return 签到状态信息
     */
    CheckinStatusVO checkin(User user);

    /**
     * 获取用户签到状态
     *
     * @param user 当前登录用户
     * @return 签到状态信息
     */
    CheckinStatusVO getCheckinStatus(User user);
}
