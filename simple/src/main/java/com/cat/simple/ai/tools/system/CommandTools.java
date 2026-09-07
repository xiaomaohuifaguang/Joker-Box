package com.cat.simple.ai.tools.system;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Component
public class CommandTools {

    private static final boolean IS_WINDOWS =
            System.getProperty("os.name").toLowerCase().contains("win");

    private static final long TIMEOUT_SECONDS = 60;
    private static final int MAX_OUTPUT_CHARS = 10_000;

    // 想碰危险命令的描述行必须和实际行为一致，不能让模型按"30s"规划行为
    private static final String[] DANGEROUS_PATTERNS = {
            "rm -rf", "sudo ", "curl ", "wget ", "shutdown", "reboot",
            "mkfs", "chmod 777", "ssh ", "nc ", "curl -X", ">/dev/",
            // Windows 侧破坏命令
            "format ", "del /f", "del /s", "rd /s", "reg delete", "reg add",
            "taskkill /f", "net user", "powershell -enc", "Remove-Item -Recurse"
    };

    @Value("${custom.ai.workspace}")
    private String WORK_DIR;

    // chcp 探测结果缓存：code page 在 JVM 生命周期内不变，没必要每次起进程
    private static volatile Charset cachedWindowsCharset;

    @Tool("""
            Executes a command in the skill workspace and returns its output.
            Use this to run scripts bundled with the active skill.

            Rules for writing the command:
            - Use forward slashes in paths, e.g. 'java --version'
            - Use only RELATIVE paths inside the workspace; absolute paths
              (e.g. C:/Users/...) are rejected
            - Avoid shell-specific syntax: no quotes, pipes, or redirection
            - One command per call; if you need chained steps, call this tool multiple times
            - Each time this tool is invoked, it will revert to the default behavior of the workspace and will not remember the directory where it was last invoked.
            Constraints: 60-second timeout, output truncated to 10k chars.
            """)
    public String runCommand(
            @P("The command to execute with arguments, e.g. 'java --version'")
            String command) {

        String normalizedCmd = command.trim();

        // 1. 危险命令黑名单（第一道闸）
        String rejection = checkBlacklist(normalizedCmd);
        if (rejection != null) return rejection;

        // 2. 路径越界检查（第二道闸）
        Path root = Path.of(WORK_DIR).toAbsolutePath().normalize();
        rejection = checkPathEscape(normalizedCmd, root);
        if (rejection != null) return rejection;

        try {
            ProcessBuilder pb = new ProcessBuilder(buildCommand(normalizedCmd));
            pb.directory(root.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // 3. 先排空管道，再 waitFor —— 修复死锁
            //    边读边 waitFor，输出超过管道缓冲区时子进程不会卡死在 write 上
            Charset outputCharset = getOutputCharset();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), outputCharset))) {
                char[] buf = new char[8192];
                int n;
                long limit = MAX_OUTPUT_CHARS * 2L;  // 预留截断标记的空间
                while ((n = reader.read(buf)) != -1 && sb.length() < limit) {
                    sb.append(buf, 0, n);
                }
            } // try-with-resources 关闭读端，子进程如仍写会收到 broken pipe

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                // 给 kill 一点时间，避免僵尸进程
                process.waitFor(2, TimeUnit.SECONDS);
                return "Command timed out after " + TIMEOUT_SECONDS + "s: " + normalizedCmd;
            }

            String output = sb.toString().trim();

            if (output.length() > MAX_OUTPUT_CHARS) {
                output = output.substring(0, MAX_OUTPUT_CHARS)
                        + "\n...[truncated, " + output.length() + " chars total]";
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return "Command failed (exit " + exitCode + "):\n" + output;
            }
            return output.isEmpty() ? "(no output)" : output;

        } catch (IOException e) {
            return "Failed to start command: " + e.getMessage()
                    + (IS_WINDOWS
                    ? "\n(hint: check if the program is in PATH, or that it exists in the workspace)"
                    : "");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Command execution was interrupted";
        }
    }

    // ---------- 安全校验 ----------

    private String checkBlacklist(String cmd) {
        String lower = cmd.toLowerCase();
        for (String danger : DANGEROUS_PATTERNS) {
            if (lower.contains(danger)) {
                return "Command rejected: contains forbidden pattern '" + danger + "'";
            }
        }
        return null;
    }

    private String checkPathEscape(String cmd, Path root) {
        // shell 跳层 / 分隔命令元字符：直接拒绝（描述已要求不用它们）
        for (String bad : new String[]{"&&", "||", ";", "|", ">", "<", "`"}) {
            if (cmd.contains(bad)) {
                return "Command rejected: shell metacharacter '" + bad
                        + "' is not allowed; split into multiple runCommand calls";
            }
        }

        // 逐 token 检查路径形态
        for (String token : cmd.split("\\s+")) {
            if (token.isBlank()) continue;
            // 绝对路径（Windows 盘符 / Unix 根）
            if (token.matches("(?i)[a-z]:.*") || token.startsWith("/")) {
                return "Command rejected: absolute path not allowed, use relative: "
                        + token + " (workspace root: " + root + ")";
            }
            // 相对路径必须 normalize 后仍落在 root 内
            if (token.contains("/") || token.contains("\\")) {
                Path resolved = root.resolve(token).normalize();
                if (!resolved.startsWith(root)) {
                    return "Command rejected: path escapes workspace: " + token;
                }
            }
        }
        return null;
    }

    // ---------- 平台适配 ----------

    private String[] buildCommand(String cmd) {
        if (IS_WINDOWS) {
            return new String[]{"cmd.exe", "/c", cmd};
        }
        return new String[]{"/bin/bash", "-c", cmd};
    }

    private Charset getOutputCharset() {
        if (!IS_WINDOWS) return StandardCharsets.UTF_8;
        if (cachedWindowsCharset == null) {
            synchronized (CommandTools.class) {
                if (cachedWindowsCharset == null) {
                    cachedWindowsCharset = detectWindowsConsoleCharset();
                }
            }
        }
        return cachedWindowsCharset;
    }

    private Charset detectWindowsConsoleCharset() {
        try {
            Process p = new ProcessBuilder("cmd.exe", "/c", "chcp").start();
            String chcpOut = new String(p.getInputStream().readAllBytes());
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(\\d+)").matcher(chcpOut);
            if (m.find()) {
                int cp = Integer.parseInt(m.group(1));
                if (cp == 65001) return StandardCharsets.UTF_8;   // ← 原来那行改成这个
                return Charset.forName("GBK");
            }
        } catch (Exception ignored) {
        }
        return Charset.forName("GBK");
    }
}
