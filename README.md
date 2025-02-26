# 小说阅读与分析工具

这是一个基于Java的小说阅读与分析工具，使用Google Gemini LLM对长篇小说进行智能分析。

## 功能特点

1. **章节验证**：自动识别小说中的章节，并验证章节是否按顺序递增，是否存在重复或缺失的章节。
2. **小说分割**：将小说按章节分割，每10个章节合并为一个文件。
3. **Gemini API调用**：使用Google Gemini API对分割后的章节进行智能分析。
4. **多线程处理**：使用10个线程并发调用API，提高处理效率。
5. **限流控制**：严格遵守API调用限制，每分钟最多调用15次。
6. **结果合并**：将所有分析结果按章节顺序合并到一个最终文件中。

## 使用方法

### 前提条件

- Java 11或更高版本
- Maven
- Google Gemini API密钥

### 配置API密钥

在项目根目录下创建`apikey.yml`文件，内容如下：

```yaml
api_key: 您的Gemini API密钥
```

### 运行程序

1. 将小说文件（UTF-8编码的TXT文件）放在项目根目录下，命名为`牧神记.txt`
2. 使用Maven编译项目：

```bash
mvn clean package
```

3. 运行程序：

```bash
java -jar target/novel-reader-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### 输出结果

程序运行后，将在`output`目录下生成以下内容：

- `split_chapters`目录：包含分割后的章节文件
- `api_results`目录：包含每个分割文件的API调用结果（JSON格式）
- `final_analysis.txt`：合并后的最终分析结果

## 自定义配置

如果需要自定义程序行为，可以修改`Constants.java`文件中的常量：

- `CHAPTERS_PER_FILE`：每个分割文件包含的章节数
- `MAX_REQUESTS_PER_MINUTE`：每分钟最大API调用次数
- `THREAD_COUNT`：并发线程数
- `GEMINI_MODEL`：使用的Gemini模型
- `MAX_OUTPUT_TOKENS`：API输出的最大token数
- `TEMPERATURE`：生成文本的随机性（0-1之间）

## 提示词自定义

可以修改项目根目录下的`prompt.txt`文件来自定义提示词，以获得不同的分析结果。

## 注意事项

- 小说文件必须是UTF-8编码的TXT文件
- API调用可能会产生费用，请注意控制使用量
- 处理大型小说可能需要较长时间，请耐心等待
