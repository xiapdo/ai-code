package com.dzc.dzcaicodemother.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dzc.dzcaicodemother.annotation.AuthCheck;
import com.dzc.dzcaicodemother.common.BaseResponse;
import com.dzc.dzcaicodemother.common.DeleteRequest;
import com.dzc.dzcaicodemother.common.ResultUtils;
import com.dzc.dzcaicodemother.constant.UserConstant;
import com.dzc.dzcaicodemother.exception.BusinessException;
import com.dzc.dzcaicodemother.exception.ErrorCode;
import com.dzc.dzcaicodemother.exception.ThrowUtils;
import com.dzc.dzcaicodemother.mannger.CosManager;
import com.dzc.dzcaicodemother.model.dto.user.UserAddRequest;
import com.dzc.dzcaicodemother.model.dto.user.UserEditRequest;
import com.dzc.dzcaicodemother.model.dto.user.UserLoginRequest;
import com.dzc.dzcaicodemother.model.dto.user.UserQueryRequest;
import com.dzc.dzcaicodemother.model.dto.user.UserRegisterRequest;
import com.dzc.dzcaicodemother.model.dto.user.UserUpdateRequest;
import com.dzc.dzcaicodemother.model.entity.User;
import com.dzc.dzcaicodemother.model.vo.CheckinStatusVO;
import com.dzc.dzcaicodemother.model.vo.LoginUserVO;
import com.dzc.dzcaicodemother.model.vo.UserVO;
import com.dzc.dzcaicodemother.service.UserService;
import com.dzc.dzcaicodemother.utils.ValidationUtils;
import com.mybatisflex.core.paginate.Page;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户 控制层。
 *
   @author dzc
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private CosManager cosManager;

    /**
     * 用户注册
     *
     * @param userRegisterRequest 用户注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        ValidationUtils.validatePhone(userRegisterRequest.getPhone());
        ValidationUtils.validateEmail(userRegisterRequest.getEmail());
        long result = userService.userRegister(userRegisterRequest);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest 用户登录请求
     * @param request          请求对象
     * @return 脱敏后的用户登录信息
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户注销
     *
     * @param request 请求对象
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 上传用户头像
     *
     * @param file 文件
     * @param request 请求对象
     * @return 头像 URL
     */
    @PostMapping("/upload/avatar")
    public BaseResponse<String> uploadAvatar(@RequestPart("file") MultipartFile file, HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }
        User loginUser = userService.getLoginUser(request);
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".png";
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String key = String.format("/avatar/%s/%s%s", loginUser.getId(), uuid, suffix);
        try {
            File tempFile = File.createTempFile("avatar-", suffix);
            file.transferTo(tempFile);
            String avatarUrl = cosManager.uploadFile(key, tempFile);
            tempFile.delete();
            log.info("用户 {} 上传头像成功: {}", loginUser.getId(), avatarUrl);
            return ResultUtils.success(avatarUrl);
        } catch (IOException e) {
            log.error("上传头像失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传头像失败");
        }
    }

    /**
     * 创建用户
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        // 默认密码 12345678
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 编辑用户信息（支持用户和管理员）
     *
     * @param userEditRequest 编辑请求（包含需要更新的字段）
     * @param request HTTP 请求
     * @return 是否成功
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editUser(@RequestBody UserEditRequest userEditRequest, HttpServletRequest request) {
        if (userEditRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ValidationUtils.validatePhone(userEditRequest.getPhone());
        ValidationUtils.validateEmail(userEditRequest.getEmail());
        User loginUser = userService.getLoginUser(request);
        User user = new User();
        BeanUtils.copyProperties(userEditRequest, user);
        user.setId(loginUser.getId());
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     *
     * @param userQueryRequest 查询请求参数
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = userQueryRequest.getPageNum();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(Page.of(pageNum, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        // 数据脱敏
        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

    /**
     * 用户签到
     *
     * @param request 请求对象
     * @return 签到状态信息
     */
    @PostMapping("/checkin")
    public BaseResponse<CheckinStatusVO> userCheckin(HttpServletRequest request) {
        // 1. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        
        // 2. 执行签到
        CheckinStatusVO checkinStatus = userService.checkin(loginUser);
        
        return ResultUtils.success(checkinStatus);
    }

    /**
     * 获取用户签到状态
     *
     * @param request 请求对象
     * @return 签到状态信息
     */
    @GetMapping("/checkin/status")
    public BaseResponse<CheckinStatusVO> getCheckinStatus(HttpServletRequest request) {
        // 1. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        
        // 2. 获取签到状态
        CheckinStatusVO checkinStatus = userService.getCheckinStatus(loginUser);
        
        return ResultUtils.success(checkinStatus);
    }
}
