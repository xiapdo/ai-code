package com.dzc.dzcaicodemother.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dzc.dzcaicodemother.annotation.AuthCheck;
import com.dzc.dzcaicodemother.common.BaseResponse;
import com.dzc.dzcaicodemother.common.DeleteRequest;
import com.dzc.dzcaicodemother.common.ResultUtils;
import com.dzc.dzcaicodemother.constant.UserConstant;
import com.dzc.dzcaicodemother.exception.ErrorCode;
import com.dzc.dzcaicodemother.exception.ThrowUtils;
import com.dzc.dzcaicodemother.mapper.ChatRoomMapper;
import com.dzc.dzcaicodemother.model.dto.chat.ChatInviteJoinRequest;
import com.dzc.dzcaicodemother.model.dto.chat.ChatMessageSendRequest;
import com.dzc.dzcaicodemother.model.dto.chat.ChatRoomCreateRequest;
import com.dzc.dzcaicodemother.model.dto.chat.ChatRoomQueryRequest;
import com.dzc.dzcaicodemother.model.dto.chat.ChatRoomUpdateRequest;
import com.dzc.dzcaicodemother.model.entity.ChatRoom;
import com.dzc.dzcaicodemother.model.entity.User;
import com.dzc.dzcaicodemother.model.vo.ChatRoomMemberVO;
import com.dzc.dzcaicodemother.model.vo.ChatRoomMessageVO;
import com.dzc.dzcaicodemother.model.vo.ChatRoomVO;
import com.dzc.dzcaicodemother.model.vo.InviteInfoVO;
import com.dzc.dzcaicodemother.model.vo.UserVO;
import com.dzc.dzcaicodemother.ratelimter.annotation.RateLimit;
import com.dzc.dzcaicodemother.ratelimter.enums.RateLimitType;
import com.dzc.dzcaicodemother.service.ChatRoomService;
import com.dzc.dzcaicodemother.service.ChatService;
import com.dzc.dzcaicodemother.service.UserService;
import com.dzc.dzcaicodemother.websocket.disruptor.ChatMessageDisruptorProducer;
import com.dzc.dzcaicodemother.websocket.vo.ChatMessageVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 群聊控制器
 *
 * @author dzc
 */
@RestController
@RequestMapping("/chat-room")
@Slf4j
public class ChatRoomController {

    @Resource
    private ChatRoomService chatRoomService;

    @Resource
    private ChatRoomMapper chatRoomMapper;

    @Resource
    private ChatService chatService;

    @Resource
    private UserService userService;

    @Resource
    private ChatMessageDisruptorProducer messageProducer;

    /**
     * 创建群聊
     *
     * @param chatRoomCreateRequest 创建请求
     * @param request HTTP请求
     * @return 群聊ID
     */
    @PostMapping("/create")
    @RateLimit(limitType = RateLimitType.USER, rate = 10, rateInterval = 60, message = "创建群聊过于频繁，请稍后再试")
    public BaseResponse<ChatRoom> createChatRoom(@RequestBody ChatRoomCreateRequest chatRoomCreateRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(chatRoomCreateRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);

        // 1. 检查是否达到创建上限（8 个）
        long createdCount = chatRoomService.countMyCreatedRooms(loginUser.getId());
        ThrowUtils.throwIf(createdCount >= 8, ErrorCode.OPERATION_ERROR, "你已达到创建群聊上限，请联系工作人员处理");

        // 2. 检查用户是否为管理员（管理员不受名额限制）
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        if (!isAdmin) {
            // 3. 普通用户检查是否有群聊名额
            Integer quota = loginUser.getChatRoomQuota() == null ? 0 : loginUser.getChatRoomQuota();
            ThrowUtils.throwIf(quota <= 0, ErrorCode.OPERATION_ERROR, "金币不足，请签到积累金币兑换群聊名额");

            // 4. 消耗 1 个名额
            loginUser.setChatRoomQuota(quota - 1);
            userService.updateById(loginUser);
        }

        // 5. 创建群聊
        ChatRoom chatRoom = chatRoomService.createRoomWithInviteCode(chatRoomCreateRequest, loginUser);

        return ResultUtils.success(chatRoom);
    }

    /**
     * 获取群聊详情
     *
     * @param id 群聊ID
     * @param request HTTP请求
     * @return 群聊详情
     */
    @GetMapping("/get")
    public BaseResponse<ChatRoom> getChatRoomById(@RequestParam Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "群聊ID错误");
        User loginUser = userService.getLoginUser(request);
        ChatRoom chatRoom = chatRoomService.getChatRoomDetail(id, loginUser);
        return ResultUtils.success(chatRoom);
    }

    /**
     * 获取我创建的群聊列表
     *
     * @param chatRoomQueryRequest 查询请求
     * @param request HTTP请求
     * @return 群聊列表
     */
    @GetMapping("/list/my-created")
    public BaseResponse<Page<ChatRoom>> listMyCreatedRooms(ChatRoomQueryRequest chatRoomQueryRequest,
                                                           HttpServletRequest request) {
        ThrowUtils.throwIf(chatRoomQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Page<ChatRoom> page = chatRoomService.listMyCreatedRooms(chatRoomQueryRequest, loginUser);
        return ResultUtils.success(page);
    }

    /**
     * 获取我加入的群聊列表
     *
     * @param chatRoomQueryRequest 查询请求
     * @param request HTTP请求
     * @return 群聊列表
     */
    @GetMapping("/list/my-joined")
    public BaseResponse<Page<ChatRoom>> listMyJoinedRooms(ChatRoomQueryRequest chatRoomQueryRequest,
                                                          HttpServletRequest request) {
        ThrowUtils.throwIf(chatRoomQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Page<ChatRoom> page = chatRoomService.listMyJoinedRooms(chatRoomQueryRequest, loginUser);
        return ResultUtils.success(page);
    }

    /**
     * 获取我的所有群聊列表（创建的+加入的）
     *
     * @param chatRoomQueryRequest 查询请求
     * @param request HTTP请求
     * @return 群聊列表
     */
    @GetMapping("/list/my-all")
    public BaseResponse<Page<ChatRoom>> listMyAllRooms(ChatRoomQueryRequest chatRoomQueryRequest,
                                                       HttpServletRequest request) {
        ThrowUtils.throwIf(chatRoomQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Page<ChatRoom> page = chatRoomService.listMyAllRooms(chatRoomQueryRequest, loginUser);
        return ResultUtils.success(page);
    }

    /**
     * 更新群聊信息（只有群主可以更新）
     *
     * @param chatRoomUpdateRequest 更新请求
     * @param request HTTP请求
     * @return 更新结果
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateChatRoom(@RequestBody ChatRoomUpdateRequest chatRoomUpdateRequest,
                                                HttpServletRequest request) {
        ThrowUtils.throwIf(chatRoomUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(chatRoomUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR, "群聊ID不能为空");
        User loginUser = userService.getLoginUser(request);
        boolean updated = chatRoomService.updateRoomInfo(chatRoomUpdateRequest.getId(), chatRoomUpdateRequest, loginUser);
        return ResultUtils.success(updated);
    }

    /**
     * 解散群聊（只有群主可以解散）
     *
     * @param deleteRequest 删除请求
     * @param request HTTP请求
     * @return 解散结果
     */
    @PostMapping("/dissolve")
    public BaseResponse<Boolean> dissolveRoom(@RequestBody DeleteRequest deleteRequest,
                                              HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(deleteRequest.getId() == null, ErrorCode.PARAMS_ERROR, "群聊ID不能为空");
        User loginUser = userService.getLoginUser(request);
        boolean dissolved = chatRoomService.dissolveRoom(deleteRequest.getId(), loginUser);
        return ResultUtils.success(dissolved);
    }

    /**
     * 生成邀请码（只有群主可以生成）
     *
     * @param roomId 群聊ID
     * @param request HTTP请求
     * @return 邀请码
     */
    @PostMapping("/invite/generate")
    @RateLimit(limitType = RateLimitType.USER, rate = 5, rateInterval = 60, message = "生成邀请码过于频繁，请稍后再试")
    public BaseResponse<String> generateInviteCode(@RequestParam Long roomId, HttpServletRequest request) {
        ThrowUtils.throwIf(roomId == null || roomId <= 0, ErrorCode.PARAMS_ERROR, "群聊ID错误");
        User loginUser = userService.getLoginUser(request);
        String inviteCode = chatRoomService.generateInviteCode(roomId, loginUser);
        return ResultUtils.success(inviteCode);
    }

    /**
     * 获取邀请码信息（用于分享页面）
     *
     * @param inviteCode 邀请码
     * @return 邀请码信息
     */
    @GetMapping("/invite/info")
    public BaseResponse<InviteInfoVO> getInviteInfo(@RequestParam String inviteCode) {
        ThrowUtils.throwIf(StrUtil.isBlank(inviteCode), ErrorCode.PARAMS_ERROR, "邀请码不能为空");

        // 验证邀请码是否有效
        boolean valid = chatRoomService.validateInviteCode(inviteCode);
        ThrowUtils.throwIf(!valid, ErrorCode.PARAMS_ERROR, "邀请码无效或已过期");

        // 查询群聊信息
        QueryWrapper queryWrapper = QueryWrapper.create()
                .and(ChatRoom::getInviteCode).eq(inviteCode);
        ChatRoom chatRoom = chatRoomMapper.selectOneByQuery(queryWrapper);
        ThrowUtils.throwIf(chatRoom == null, ErrorCode.NOT_FOUND_ERROR, "群聊不存在");

        // 查询群主信息
        User owner = userService.getById(chatRoom.getOwnerUserId());

        // 返回邀请码信息（前端展示）
        InviteInfoVO inviteInfoVO = new InviteInfoVO();
        inviteInfoVO.setInviteCode(inviteCode);
        inviteInfoVO.setValid(true);
        inviteInfoVO.setExpiryMinutes(5);
        inviteInfoVO.setRoomId(chatRoom.getId());
        inviteInfoVO.setRoomName(chatRoom.getRoomName());
        inviteInfoVO.setOwnerName(owner != null ? owner.getUserName() : "未知");
        inviteInfoVO.setOwnerUserId(chatRoom.getOwnerUserId());
        inviteInfoVO.setMemberCount(chatRoom.getMemberCount());
        inviteInfoVO.setDescription(chatRoom.getDescription());

        return ResultUtils.success(inviteInfoVO);
    }

    /**
     * 通过邀请码加入群聊
     *
     * @param chatInviteJoinRequest 加入请求
     * @param request HTTP请求
     * @return 加入结果
     */
    @PostMapping("/join")
    public BaseResponse<Boolean> joinRoomByInviteCode(@RequestBody ChatInviteJoinRequest chatInviteJoinRequest,
                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(chatInviteJoinRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(chatInviteJoinRequest.getInviteCode()), ErrorCode.PARAMS_ERROR, "邀请码不能为空");
        User loginUser = userService.getLoginUser(request);
        boolean joined = chatRoomService.joinRoomByInviteCode(chatInviteJoinRequest.getInviteCode(), loginUser);
        return ResultUtils.success(joined);
    }

    /**
     * 获取群聊成员列表
     *
     * @param roomId 群聊ID
     * @param request HTTP请求
     * @return 成员列表
     */
    @GetMapping("/members/list")
    public BaseResponse<List<ChatRoomMemberVO>> listRoomMembers(@RequestParam Long roomId, HttpServletRequest request) {
        ThrowUtils.throwIf(roomId == null || roomId <= 0, ErrorCode.PARAMS_ERROR, "群聊ID错误");
        User loginUser = userService.getLoginUser(request);

        ChatRoom chatRoom = chatRoomService.getById(roomId);
        if (!chatRoom.getOwnerUserId().equals(loginUser.getId())) {
            boolean isMember = chatRoomService.isRoomMember(roomId, loginUser.getId());
            ThrowUtils.throwIf(!isMember, ErrorCode.NO_AUTH_ERROR, "您不在该群聊中");
        }

        List<ChatRoomMemberVO> members = chatRoomService.listRoomMembersWithUserInfo(roomId);
        return ResultUtils.success(members);
    }

    /**
     * 踢出成员（只有群主可以踢人）
     *
     * @param roomId 群聊ID
     * @param memberUserId 被踢用户ID
     * @param request HTTP请求
     * @return 踢出结果
     */
    @PostMapping("/member/kick")
    public BaseResponse<Boolean> kickMember(@RequestParam Long roomId,
                                            @RequestParam Long memberUserId,
                                            HttpServletRequest request) {
        ThrowUtils.throwIf(roomId == null || roomId <= 0, ErrorCode.PARAMS_ERROR, "群聊ID错误");
        ThrowUtils.throwIf(memberUserId == null || memberUserId <= 0, ErrorCode.PARAMS_ERROR, "用户ID错误");
        User loginUser = userService.getLoginUser(request);
        boolean kicked = chatRoomService.kickMember(roomId, memberUserId, loginUser);
        return ResultUtils.success(kicked);
    }

    /**
     * 退出群聊（群成员退出，群主不能退出）
     *
     * @param roomId 群聊ID
     * @param request HTTP请求
     * @return 退出结果
     */
    @PostMapping("/leave")
    public BaseResponse<Boolean> leaveRoom(@RequestParam Long roomId, HttpServletRequest request) {
        ThrowUtils.throwIf(roomId == null || roomId <= 0, ErrorCode.PARAMS_ERROR, "群聊ID错误");
        User loginUser = userService.getLoginUser(request);
        boolean left = chatRoomService.leaveRoom(roomId, loginUser);
        return ResultUtils.success(left);
    }

    /**
     * 获取群聊消息历史
     *
     * @param roomId 群聊ID
     * @param request HTTP请求
     * @return 消息列表
     */
    @GetMapping("/messages")
    public BaseResponse<List<ChatRoomMessageVO>> getChatMessages(@RequestParam Long roomId, HttpServletRequest request) {
        ThrowUtils.throwIf(roomId == null || roomId <= 0, ErrorCode.PARAMS_ERROR, "群聊ID错误");
        User loginUser = userService.getLoginUser(request);
        ChatRoom chatRoom = chatRoomService.getById(roomId);
        if (!chatRoom.getOwnerUserId().equals(loginUser.getId())) {
            boolean isMember = chatService.isRoomMember(roomId, loginUser.getId());
            ThrowUtils.throwIf(!isMember, ErrorCode.NO_AUTH_ERROR, "您不在该群聊中");
        }
        List<ChatRoomMessageVO> messages = chatService.getRecentMessagesWithUserInfo(roomId, 100);
        return ResultUtils.success(messages);
    }

    /**
     * 发送消息
     *
     * @param messageSendRequest 消息发送请求
     * @param request HTTP请求
     * @return 消息ID
     */
    @PostMapping("/message/send")
    public BaseResponse<Long> sendChatMessage(@RequestBody ChatMessageSendRequest messageSendRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(messageSendRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(messageSendRequest.getRoomId() == null || messageSendRequest.getRoomId() <= 0, ErrorCode.PARAMS_ERROR, "群聊ID错误");
        ThrowUtils.throwIf(StrUtil.isBlank(messageSendRequest.getMessageContent()), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        User loginUser = userService.getLoginUser(request);

        // 1. 保存消息到数据库
        String messageType = StrUtil.isBlank(messageSendRequest.getMessageType()) ? "text" : messageSendRequest.getMessageType();
        Long messageId = chatService.sendMessage(messageSendRequest.getRoomId(), messageSendRequest.getMessageContent(), messageType, loginUser);

        // 2. 通过 WebSocket 广播消息（让在线用户实时收到）
        ChatMessageVO messageVO = new ChatMessageVO();
        messageVO.setId(messageId);
        messageVO.setRoomId(messageSendRequest.getRoomId());
        messageVO.setUserId(loginUser.getId());
        messageVO.setUserName(loginUser.getUserName());
        messageVO.setUserAvatar(loginUser.getUserAvatar());
        messageVO.setMessageContent(messageSendRequest.getMessageContent());
        messageVO.setMessageType(messageType);
        messageVO.setCreateTime(java.time.LocalDateTime.now().toString());
        messageVO.setIsRevoked(0);
        messageProducer.publish(messageSendRequest.getRoomId(), messageVO);

        return ResultUtils.success(messageId);
    }

    // ==================== 管理员接口 ====================

    /**
     * 管理员分页获取群聊列表
     *
     * @param chatRoomQueryRequest 查询请求
     * @return 群聊列表（含群主信息）
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatRoomVO>> listChatRoomByPageForAdmin(
            @RequestBody ChatRoomQueryRequest chatRoomQueryRequest) {
        ThrowUtils.throwIf(chatRoomQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = chatRoomQueryRequest.getPageNum();
        long pageSize = chatRoomQueryRequest.getPageSize();
        QueryWrapper queryWrapper = chatRoomService.getQueryWrapper(chatRoomQueryRequest);
        Page<ChatRoom> chatRoomPage = chatRoomMapper.paginate(Page.of(pageNum, pageSize), queryWrapper);
        Page<ChatRoomVO> voPage = new Page<>(pageNum, pageSize, chatRoomPage.getTotalRow());
        List<ChatRoomVO> voList = chatRoomPage.getRecords().stream().map(room -> {
            ChatRoomVO vo = new ChatRoomVO();
            vo.setId(room.getId());
            vo.setRoomName(room.getRoomName());
            vo.setRoomAvatar(room.getRoomAvatar());
            vo.setDescription(room.getDescription());
            vo.setInviteCode(room.getInviteCode());
            vo.setInviteExpiryTime(room.getInviteExpiryTime());
            vo.setOwnerUserId(room.getOwnerUserId());
            vo.setMemberCount(room.getMemberCount());
            vo.setStatus(room.getStatus());
            vo.setCreateTime(room.getCreateTime());
            vo.setUpdateTime(room.getUpdateTime());
            User owner = userService.getById(room.getOwnerUserId());
            if (owner != null) {
                UserVO ownerVO = new UserVO();
                ownerVO.setId(owner.getId());
                ownerVO.setUserName(owner.getUserName());
                ownerVO.setUserAvatar(owner.getUserAvatar());
                ownerVO.setUserAccount(owner.getUserAccount());
                vo.setOwner(ownerVO);
            }
            return vo;
        }).toList();
        voPage.setRecords(voList);
        return ResultUtils.success(voPage);
    }

    /**
     * 管理员解散群聊
     *
     * @param deleteRequest 删除请求
     * @return 解散结果
     */
    @PostMapping("/admin/dissolve")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> dissolveRoomByAdmin(@RequestBody DeleteRequest deleteRequest,
                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(deleteRequest.getId() == null, ErrorCode.PARAMS_ERROR, "群聊ID不能为空");
        Long roomId = deleteRequest.getId();
        ChatRoom chatRoom = chatRoomService.getById(roomId);
        ThrowUtils.throwIf(chatRoom == null, ErrorCode.NOT_FOUND_ERROR, "群聊不存在");

        ChatRoom update = new ChatRoom();
        update.setId(roomId);
        update.setStatus(2);
        boolean updated = chatRoomMapper.update(update) > 0;

        if (updated) {
            log.info("管理员解散群聊成功：roomId={}", roomId);
        }
        return ResultUtils.success(updated);
    }

    /**
     * 管理员获取群聊详情（含成员列表）
     *
     * @param id 群聊ID
     * @return 群聊详情
     */
    @GetMapping("/admin/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<ChatRoomVO> getChatRoomVOByAdmin(@RequestParam Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "群聊ID错误");
        ChatRoom room = chatRoomService.getById(id);
        ThrowUtils.throwIf(room == null, ErrorCode.NOT_FOUND_ERROR, "群聊不存在");
        ChatRoomVO vo = new ChatRoomVO();
        vo.setId(room.getId());
        vo.setRoomName(room.getRoomName());
        vo.setRoomAvatar(room.getRoomAvatar());
        vo.setDescription(room.getDescription());
        vo.setInviteCode(room.getInviteCode());
        vo.setInviteExpiryTime(room.getInviteExpiryTime());
        vo.setOwnerUserId(room.getOwnerUserId());
        vo.setMemberCount(room.getMemberCount());
        vo.setStatus(room.getStatus());
        vo.setCreateTime(room.getCreateTime());
        vo.setUpdateTime(room.getUpdateTime());
        User owner = userService.getById(room.getOwnerUserId());
        if (owner != null) {
            UserVO ownerVO = new UserVO();
            ownerVO.setId(owner.getId());
            ownerVO.setUserName(owner.getUserName());
            ownerVO.setUserAvatar(owner.getUserAvatar());
            ownerVO.setUserAccount(owner.getUserAccount());
            vo.setOwner(ownerVO);
        }
        return ResultUtils.success(vo);
    }
}
