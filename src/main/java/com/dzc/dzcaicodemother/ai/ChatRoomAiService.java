package com.dzc.dzcaicodemother.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface ChatRoomAiService {

    @SystemMessage(fromResource = "prompt/chatroom-ai-chat-prompt.txt")
    TokenStream chat(@UserMessage String userMessage);

    @SystemMessage(fromResource = "prompt/chatroom-ai-frontend-prompt.txt")
    TokenStream chatFrontend(@UserMessage String userMessage);
}
