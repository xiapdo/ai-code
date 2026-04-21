package com.dzc.dzcaicodemother.service.impl;

import static com.dzc.dzcaicodemother.constant.UserConstant.USER_LOGIN_STATE;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import com.dzc.dzcaicodemother.exception.BusinessException;
import com.dzc.dzcaicodemother.exception.ErrorCode;
import com.dzc.dzcaicodemother.mapper.UserMapper;
import com.dzc.dzcaicodemother.model.dto.user.UserQueryRequest;
import com.dzc.dzcaicodemother.model.dto.user.UserRegisterRequest;
import com.dzc.dzcaicodemother.model.entity.User;
import com.dzc.dzcaicodemother.model.enums.UserRoleEnum;
import com.dzc.dzcaicodemother.model.vo.CheckinRecordVO;
import com.dzc.dzcaicodemother.model.vo.CheckinStatusVO;
import com.dzc.dzcaicodemother.model.vo.CoinRewardVO;
import com.dzc.dzcaicodemother.model.vo.LoginUserVO;
import com.dzc.dzcaicodemother.model.vo.UserVO;
import com.dzc.dzcaicodemother.service.UserService;
import com.dzc.dzcaicodemother.utils.CheckinCacheKeyUtils;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户 服务层实现。
 *
   @author dzc
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    // 基础版本：必须包含大写字母、小写字母和特殊字符
    private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\W).*$";
    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);
    
    /**
     * 日期格式
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * 签到数据保留天数（30 天）
     */
    private static final long CHECKIN_TTL_DAYS = 30;

    private final RedissonClient redissonClient;

    public UserServiceImpl(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String userName = userRegisterRequest.getUserName();
        String phone = userRegisterRequest.getPhone();
        String email = userRegisterRequest.getEmail();
        // 1. 校验参数
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        if (!pattern.matcher(userPassword).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "必须包含大写字母、小写字母和特殊字符");
        }

        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 2. 查询用户是否已存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 3. 加密密码
        String encryptPassword = getEncryptPassword(userPassword);
        // 4. 创建用户，插入数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName(userName);
        user.setUserRole(UserRoleEnum.USER.getValue());
        user.setPhone(phone);
        user.setEmail(email);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        // 确保 coins 和 chatRoomQuota 有默认值
        if (loginUserVO.getCoins() == null) {
            loginUserVO.setCoins(0);
        }
        if (loginUserVO.getChatRoomQuota() == null) {
            loginUserVO.setChatRoomQuota(0);
        }
        return loginUserVO;
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验参数
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度过短");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度过短");
        }
        // 2. 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 3. 查询用户是否存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 4. 如果用户存在，记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 5. 返回脱敏的用户信息
        return this.getLoginUserVO(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断用户是否登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询当前用户信息
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        // 先判断用户是否登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        // 从数据库查询当前用户信息
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        return currentUser;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 先判断用户是否登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id) // where id = ${id}
                .eq("userRole", userRole) // and userRole = ${userRole}
                .like("userAccount", userAccount)
                .like("userName", userName)
                .like("userProfile", userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    @Override
    public String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "dzc";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 金币兑换比例（7 枚金币兑换 1 个群聊名额）
     */
    private static final int COINS_PER_QUOTA = 7;

    @Override
    public CheckinStatusVO checkin(User user) {
        // 1. 校验参数
        if (user == null || user.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 2. 获取 Redis Set，存储签到日期
        String checkinKey = CheckinCacheKeyUtils.getCheckinKey(user.getId());
        RSet<String> checkinSet = redissonClient.getSet(checkinKey);

        // 3. 获取今日日期
        LocalDate today = LocalDate.now();
        String todayStr = today.format(DATE_FORMATTER);

        // 4. 检查今日是否已签到
        if (checkinSet.contains(todayStr)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "今日已签到");
        }

        // 5. 执行签到
        checkinSet.add(todayStr);

        // 6. 设置过期时间（30 天）
        checkinSet.expire(CHECKIN_TTL_DAYS, TimeUnit.DAYS);

        // 7. 计算连续签到天数
        int continuousDays = calculateContinuousDays(checkinSet, today);

        // 8. 发放金币（+1）
        Integer currentCoins = user.getCoins() == null ? 0 : user.getCoins();
        currentCoins += 1;
        
        // 9. 检查是否需要自动兑换（金币达到 7 枚）
        CoinRewardVO coinReward = CoinRewardVO.builder()
                .earnedCoins(1)
                .totalCoins(currentCoins)
                .needExchange(false)
                .exchangedQuota(0)
                .remainingCoins(currentCoins)
                .build();
        
        if (currentCoins >= COINS_PER_QUOTA) {
            // 自动兑换：每 7 枚金币兑换 1 个群聊名额
            int exchangeQuota = currentCoins / COINS_PER_QUOTA;
            int remainingCoins = currentCoins % COINS_PER_QUOTA;
            
            Integer currentQuota = user.getChatRoomQuota() == null ? 0 : user.getChatRoomQuota();
            user.setChatRoomQuota(currentQuota + exchangeQuota);
            user.setCoins(remainingCoins);
            
            coinReward.setNeedExchange(true);
            coinReward.setExchangedQuota(exchangeQuota);
            coinReward.setRemainingCoins(remainingCoins);
        } else {
            // 未达兑换标准，只更新金币数
            user.setCoins(currentCoins);
        }
        
        // 10. 更新用户信息到数据库
        updateById(user);

        // 11. 构建返回结果
        return CheckinStatusVO.builder()
                .checkedToday(true)
                .continuousDays(continuousDays)
                .totalDays(checkinSet.size())
                .recentRecords(buildRecentRecords(checkinSet, today))
                .coinReward(coinReward)
                .build();
    }

    @Override
    public CheckinStatusVO getCheckinStatus(User user) {
        // 1. 校验参数
        if (user == null || user.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 2. 获取 Redis Set
        String checkinKey = CheckinCacheKeyUtils.getCheckinKey(user.getId());
        RSet<String> checkinSet = redissonClient.getSet(checkinKey);

        // 3. 获取今日日期
        LocalDate today = LocalDate.now();
        String todayStr = today.format(DATE_FORMATTER);

        // 4. 检查今日是否已签到
        boolean checkedToday = checkinSet.contains(todayStr);

        // 5. 计算连续签到天数
        int continuousDays = calculateContinuousDays(checkinSet, today);

        // 6. 构建金币奖励信息（用于展示进度条）
        Integer currentCoins = user.getCoins() == null ? 0 : user.getCoins();
        CoinRewardVO coinReward = CoinRewardVO.builder()
                .earnedCoins(0)
                .totalCoins(currentCoins)
                .needExchange(currentCoins >= COINS_PER_QUOTA)
                .exchangedQuota(0)
                .remainingCoins(currentCoins)
                .build();

        // 7. 构建返回结果
        return CheckinStatusVO.builder()
                .checkedToday(checkedToday)
                .continuousDays(continuousDays)
                .totalDays(checkinSet.size())
                .recentRecords(buildRecentRecords(checkinSet, today))
                .coinReward(coinReward)
                .build();
    }

    /**
     * 计算连续签到天数
     * 
     * @param checkinSet 签到日期集合
     * @param fromDate   起始日期（通常为今天）
     * @return 连续签到天数
     */
    private int calculateContinuousDays(RSet<String> checkinSet, LocalDate fromDate) {
        int consecutiveDays = 0;
        LocalDate currentDate = fromDate;

        // 如果今天没有签到，从昨天开始往前数
        String todayStr = fromDate.format(DATE_FORMATTER);
        if (!checkinSet.contains(todayStr)) {
            currentDate = fromDate.minusDays(1);
        }

        // 从当前日期开始往前遍历，直到遇到未签到的日期为止
        while (true) {
            String dateStr = currentDate.format(DATE_FORMATTER);
            if (checkinSet.contains(dateStr)) {
                consecutiveDays++;
                currentDate = currentDate.minusDays(1);
            } else {
                // 遇到断签日期，停止计数
                break;
            }
        }

        return consecutiveDays;
    }

    /**
     * 构建最近 7 天签到记录
     * 
     * @param checkinSet 签到日期集合
     * @param today      今日日期
     * @return 最近 7 天签到记录列表
     */
    private List<CheckinRecordVO> buildRecentRecords(RSet<String> checkinSet, LocalDate today) {
        List<CheckinRecordVO> records = new ArrayList<>();
        Locale locale = Locale.CHINA;

        // 从 6 天前到今天，共 7 天
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.format(DATE_FORMATTER);
            String dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, locale);

            CheckinRecordVO record = CheckinRecordVO.builder()
                    .date(dateStr)
                    .checked(checkinSet.contains(dateStr))
                    .dayOfWeek(dayOfWeek)
                    .build();
            records.add(record);
        }

        return records;
    }
}
