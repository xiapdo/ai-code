package com.dzc.dzcaicodemother.service;

import java.util.List;

import com.dzc.dzcaicodemother.model.dto.chat.ChatRoomCreateRequest;
import com.dzc.dzcaicodemother.model.dto.chat.ChatRoomQueryRequest;
import com.dzc.dzcaicodemother.model.dto.chat.ChatRoomUpdateRequest;
import com.dzc.dzcaicodemother.model.entity.ChatRoom;
import com.dzc.dzcaicodemother.model.entity.ChatRoomMember;
import com.dzc.dzcaicodemother.model.entity.User;
import com.dzc.dzcaicodemother.model.vo.ChatRoomMemberVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;

/**
 * 群聊服务
 */
public interface ChatRoomService {
    /**
     * 创建群聊并生成邀请码（在同一事务中）
     */
    ChatRoom createRoomWithInviteCode(ChatRoomCreateRequest request, User loginUser);

    /**
     * 查询群聊
     */
    ChatRoom getById(Long id);

    /**
     * 分页获取我创建的群聊
     */
    Page<ChatRoom> listMyCreatedRooms(ChatRoomQueryRequest request, User loginUser);

    /**
     * 分页获取我加入的群聊
     */
    Page<ChatRoom> listMyJoinedRooms(ChatRoomQueryRequest request, User loginUser);

    /**
     * 分页获取我的所有群聊（创建的+加入的）
     */
    Page<ChatRoom> listMyAllRooms(ChatRoomQueryRequest request, User loginUser);

    /**
     * 获取群聊详情
     */
    ChatRoom getChatRoomDetail(Long roomId, User loginUser);

    /**
     * 解散群聊
     */
    boolean dissolveRoom(Long roomId, User loginUser);

    /**
     * 生成邀请链接（返回邀请码）
     */
    String generateInviteCode(Long roomId, User loginUser);

    /**
     * 验证邀请码是否有效
     */
    boolean validateInviteCode(String inviteCode);

    /**
     * 通过邀请码加入群聊
     */
    boolean joinRoomByInviteCode(String inviteCode, User loginUser);

    /**
     * 修改群聊信息
     */
    boolean updateRoomInfo(Long roomId, ChatRoomUpdateRequest request, User loginUser);

    /**
     * 踢出成员
     */
    boolean kickMember(Long roomId, Long memberUserId, User loginUser);

    /**
     * 获取群聊成员列表（包含用户信息）
     */
    List<ChatRoomMemberVO> listRoomMembersWithUserInfo(Long roomId);

    /**
     * 获取群聊成员列表
     */
    List<ChatRoomMember> listRoomMembers(Long roomId);

    /**
     * 判断用户是否是群成员
     */
    boolean isRoomMember(Long roomId, Long userId);

    /**
     * 退出群聊
     */
    boolean leaveRoom(Long roomId, User loginUser);

    /**
     * 获取查询构造器
     */
    QueryWrapper getQueryWrapper(ChatRoomQueryRequest request);

    /**
     * 统计用户创建的群聊数量
     */
    long countMyCreatedRooms(Long userId);
}
