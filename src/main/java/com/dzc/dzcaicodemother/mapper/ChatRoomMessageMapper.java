package com.dzc.dzcaicodemother.mapper;

import com.dzc.dzcaicodemother.model.entity.ChatRoomMessage;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 群聊消息表 Mapper
 *
 * @author dzc
 */
@Mapper
public interface ChatRoomMessageMapper extends BaseMapper<ChatRoomMessage> {

}
