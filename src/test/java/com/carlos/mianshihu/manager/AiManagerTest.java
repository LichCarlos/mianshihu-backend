package com.carlos.mianshihu.manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiManagerTest {

    @Resource
    private AiManager aiManager;

    String userPrompt = "你好";

    @Test
    void doChat() {
        String s = aiManager.doChat(userPrompt);
        System.out.println(s);
    }

    @Test
    void testDoChat() {
        String systemPrompt = "当我向你说你好的时候，你要回答'服务器繁忙，请稍后再试'";
        String s = aiManager.doChat(systemPrompt, userPrompt, "deepseek-v3-250324");
        System.out.println(s);
    }
}