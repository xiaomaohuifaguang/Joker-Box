package com.cat.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Test {

    public static void main(String[] args) throws IOException, InterruptedException, TimeoutException {

        String parentPath = "C:/Users/six6/todo/projects/pyScripts/crawler";
        String scriptPath = "ssq";
        String scriptName = "start.py";
        String[] command = {
                "cmd.exe", "/c",
                "conda", "activate", "be", "&&",
                "cd", "/d", parentPath+"/"+scriptPath, "&&",  // 切换到D盘tmp目录
                "python",
                parentPath+"/"+scriptPath+"/"+scriptName,
        };

        executeCommandWithTimeout(command, 30, TimeUnit.SECONDS);


        // 这里可以添加JSON解析代码
    }

    public static String executePythonScript(String[] command, long timeout, TimeUnit unit)
            throws IOException, InterruptedException, TimeoutException {

        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();

        // 使用StringBuilder收集输出
        StringBuilder output = new StringBuilder();

        // 启动线程读取输出（使用UTF-8编码）
        Thread readerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "GBK"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        readerThread.start();

        // 等待进程完成或超时
        boolean finished = process.waitFor(timeout, unit);

        if (!finished) {
            process.destroyForcibly();
            throw new TimeoutException("Python脚本执行超时");
        }

        // 确保读取线程完成
        readerThread.join(1000);

        if (process.exitValue() != 0) {
            throw new RuntimeException("Python脚本执行失败，退出码: " + process.exitValue());
        }

        return output.toString();
    }

    public static void windows() throws IOException, InterruptedException {
        // 根据操作系统选择不同的命令
        if (System.getProperty("os.name").toLowerCase().contains("win")) {

            String[] javaVersion = {"cmd.exe", "/c", "python", "C:\\Users\\six6\\todo\\projects\\be\\all-in\\crawler\\caijing.com.cn.py"};
            executeCommandWithTimeout(javaVersion, 10, TimeUnit.SECONDS);


            // 获取完整系统信息 (超时30秒)
//            String[] systemInfoCmd = {"cmd.exe", "/c", "systeminfo"};
//            executeCommandWithTimeout(systemInfoCmd, 30, TimeUnit.SECONDS);

            // 获取Windows版本信息 (超时10秒)
//            String[] winverCmd = {"cmd.exe", "/c", "winver"};
//            executeCommandWithTimeout(winverCmd, 10, TimeUnit.SECONDS);

            // 获取CPU信息 (超时15秒)
//            String[] cpuInfoCmd = {"cmd.exe", "/c", "wmic cpu get name,NumberOfCores,NumberOfLogicalProcessors"};
//            executeCommandWithTimeout(cpuInfoCmd, 15, TimeUnit.SECONDS);

            // 获取内存信息 (超时15秒)
//            String[] memoryInfoCmd = {"cmd.exe", "/c", "wmic memorychip get capacity"};
//            executeCommandWithTimeout(memoryInfoCmd, 15, TimeUnit.SECONDS);

            // 获取磁盘信息 (超时20秒)
//            String[] diskInfoCmd = {"cmd.exe", "/c", "wmic diskdrive get model,size & wmic logicaldisk get name,size,freespace"};
//            executeCommandWithTimeout(diskInfoCmd, 20, TimeUnit.SECONDS);

            // 获取显卡信息 (超时15秒)
//            String[] gpuInfoCmd = {"cmd.exe", "/c", "wmic path win32_VideoController get name"};
//            executeCommandWithTimeout(gpuInfoCmd, 15, TimeUnit.SECONDS);

            // 获取BIOS信息 (超时15秒)
//            String[] biosInfoCmd = {"cmd.exe", "/c", "wmic bios get manufacturer,name,version"};
//            executeCommandWithTimeout(biosInfoCmd, 15, TimeUnit.SECONDS);

            // 获取主板信息 (超时15秒)
//            String[] motherboardCmd = {"cmd.exe", "/c", "wmic baseboard get product,Manufacturer,version"};
//            executeCommandWithTimeout(motherboardCmd, 15, TimeUnit.SECONDS);

            // 获取网络配置信息 (超时15秒)
//            String[] networkConfigCmd = {"cmd.exe", "/c", "ipconfig /all"};
//            executeCommandWithTimeout(networkConfigCmd, 15, TimeUnit.SECONDS);

            // 获取网络连接信息 (超时20秒)
//            String[] networkConnCmd = {"cmd.exe", "/c", "netstat -ano"};
//            executeCommandWithTimeout(networkConnCmd, 20, TimeUnit.SECONDS);

            // 启动DirectX诊断工具 (超时10秒)
//            String[] dxdiagCmd = {"cmd.exe", "/c", "start dxdiag"};
//            executeCommandWithTimeout(dxdiagCmd, 10, TimeUnit.SECONDS);

            // 启动设备管理器 (超时10秒)
//            String[] devmgmtCmd = {"cmd.exe", "/c", "start devmgmt.msc"};
//            executeCommandWithTimeout(devmgmtCmd, 10, TimeUnit.SECONDS);

            // 将系统信息输出到文件 (超时30秒)
//            String[] systemInfoToFileCmd = {"cmd.exe", "/c", "systeminfo > D:\\tmp\\systeminfo.txt"};
//            executeCommandWithTimeout(systemInfoToFileCmd, 30, TimeUnit.SECONDS);

            // 将CPU信息输出到文件 (超时20秒)
//            String[] cpuInfoToFileCmd = {"cmd.exe", "/c", "wmic cpu list full > D:\\tmp\\cpuinfo.txt"};
//            executeCommandWithTimeout(cpuInfoToFileCmd, 20, TimeUnit.SECONDS);

            // 将DirectX信息输出到文件 (超时30秒)
//            String[] dxdiagToFileCmd = {"cmd.exe", "/c", "dxdiag /t D:\\tmp\\dxdiag_output.txt"};
//            executeCommandWithTimeout(dxdiagToFileCmd, 30, TimeUnit.SECONDS);

            // 使用PowerShell获取完整计算机信息 (超时30秒)
//            String[] psComputerInfoCmd = {"powershell.exe", "Get-ComputerInfo"};
//            executeCommandWithTimeout(psComputerInfoCmd, 30, TimeUnit.SECONDS);

            // 使用PowerShell获取详细CPU信息 (超时20秒)
//            String[] psCpuInfoCmd = {"powershell.exe", "Get-WmiObject Win32_Processor | Select-Object Name, NumberOfCores, NumberOfLogicalProcessors"};
//            executeCommandWithTimeout(psCpuInfoCmd, 20, TimeUnit.SECONDS);

            // 使用PowerShell获取详细内存信息 (超时20秒)
//            String[] psMemoryInfoCmd = {"powershell.exe", "Get-WmiObject Win32_PhysicalMemory | Select-Object Manufacturer, PartNumber, Capacity, Speed"};
//            executeCommandWithTimeout(psMemoryInfoCmd, 20, TimeUnit.SECONDS);


//            String[] javaVersion = {"cmd.exe", "/c", "java", "--version"};
//            executeCommandWithTimeout(javaVersion, 2, TimeUnit.SECONDS);
//
//            String[] pythonVersion = {"cmd.exe", "/c", "python", "--version"};
//            executeCommandWithTimeout(pythonVersion, 2, TimeUnit.SECONDS);


        } else {
            // Linux/Unix 命令
            String[] command = {"ls", "-l"};
            executeCommandWithTimeout(command, 3000, TimeUnit.MILLISECONDS);
        }
    }


    public static boolean executeCommandWithTimeout(String[] command, long timeout, TimeUnit unit)
            throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();

        // 启动线程读取输出（可选）
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "GBK"));) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(new String(line.getBytes()));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        if (!process.waitFor(timeout, unit)) {
            process.destroyForcibly();
            return false; // 超时
        }

        return process.exitValue() == 0;
    }
}
