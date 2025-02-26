package com.novel.config;

import com.novel.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * 处理API密钥的加载和管理
 */
public class ApiKeyConfig {
    private static final Logger logger = LoggerFactory.getLogger(ApiKeyConfig.class);
    private String apiKey;

    /**
     * 从配置文件加载API密钥
     */
    public void loadApiKey() {
        try {
            if (!Files.exists(Paths.get(Constants.API_KEY_FILE))) {
                logger.error("API密钥文件不存在: {}", Constants.API_KEY_FILE);
                throw new IOException("API密钥文件不存在");
            }

            Yaml yaml = new Yaml();
            try (FileInputStream inputStream = new FileInputStream(Constants.API_KEY_FILE)) {
                Map<String, String> config = yaml.load(inputStream);
                if (config == null || !config.containsKey("api_key") || config.get("api_key").isEmpty()) {
                    logger.error("API密钥文件格式不正确或密钥为空");
                    throw new IOException("API密钥文件格式不正确或密钥为空");
                }
                this.apiKey = config.get("api_key");
                logger.info("成功加载API密钥");
            }
        } catch (Exception e) {
            logger.error("加载API密钥时出错", e);
            throw new RuntimeException("加载API密钥失败", e);
        }
    }

    /**
     * 获取API密钥
     */
    public String getApiKey() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("API密钥未加载或为空");
        }
        return apiKey;
    }
}
