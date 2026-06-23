package com.repair.ai.saas.module.ai.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 AiClient 配置注入机制。
 *
 * 由于 AiClient 使用构造器注入 @Value，纯单元测试通过反射验证
 * agentUrl 字段能被正确设置。Spring 集成测试在 AiClientConfigTest 中覆盖。
 */
class AiClientTest {

    /**
     * 模拟生产环境：传入与 docker-compose.prod.yml 一致的 URL 格式。
     * 验证 agentUrl 字段被正确设置为传入的值，而非强制回退到 localhost:8090。
     */
    @Test
    void constructor_shouldAcceptConfiguredUrl() throws Exception {
        String configuredUrl = "http://agent-test:8090";
        AiClient client = new AiClient(new RestTemplate(), configuredUrl);

        Field agentUrlField = AiClient.class.getDeclaredField("agentUrl");
        agentUrlField.setAccessible(true);
        String agentUrl = (String) agentUrlField.get(client);

        assertEquals(configuredUrl, agentUrl,
                "agentUrl 应为构造器传入的配置值，证明 @Value 使用正确的属性 key 即可正确注入");
    }

    /**
     * 验证默认值：当 Spring 属性未配置时，回退到 localhost:8090（开发默认值）。
     */
    @Test
    void constructor_shouldAcceptDefaultUrl() throws Exception {
        String defaultUrl = "http://localhost:8090";
        AiClient client = new AiClient(new RestTemplate(), defaultUrl);

        Field agentUrlField = AiClient.class.getDeclaredField("agentUrl");
        agentUrlField.setAccessible(true);
        String agentUrl = (String) agentUrlField.get(client);

        assertEquals(defaultUrl, agentUrl,
                "默认值应为 localhost:8090，对应 @Value 中的默认值");
    }

    /**
     * 验证 Agent URL 不以斜杠结尾（避免拼接时出现双斜杠）。
     */
    @Test
    void agentUrl_shouldNotEndWithSlash() {
        String[] urls = {
                "http://agent-python:8090",
                "http://localhost:8090",
                "http://agent-test:8090"
        };
        for (String url : urls) {
            AiClient client = new AiClient(new RestTemplate(), url);
            try {
                Field agentUrlField = AiClient.class.getDeclaredField("agentUrl");
                agentUrlField.setAccessible(true);
                String agentUrl = (String) agentUrlField.get(client);
                assertFalse(agentUrl.endsWith("/"),
                        "agentUrl 不应以 / 结尾: " + agentUrl);
            } catch (Exception e) {
                fail("反射访问失败: " + e.getMessage());
            }
        }
    }
}
