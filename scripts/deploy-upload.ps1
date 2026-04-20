# 部署上传脚本
$Password = 'Hfyl,.123456'

# 使用 .NET Process 启动 scp 并自动输入密码
function Send-FileWithPassword {
    param(
        [string]$LocalPath,
        [string]$RemotePath
    )
    
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = "scp"
    $psi.Arguments = "-o StrictHostKeyChecking=no -o ConnectTimeout=30 `"$LocalPath`" root@43.138.7.82:$RemotePath"
    $psi.RedirectStandardInput = $true
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $psi
    $process.Start() | Out-Null
    
    # 等待密码提示
    Start-Sleep -Milliseconds 1000
    
    # 发送密码
    $process.StandardInput.WriteLine($Password)
    $process.StandardInput.Close()
    
    # 等待完成
    $process.WaitForExit()
    
    $stdout = $process.StandardOutput.ReadToEnd()
    $stderr = $process.StandardError.ReadToEnd()
    
    Write-Host "Exit Code: $($process.ExitCode)"
    if ($stdout) { Write-Host "Output: $stdout" }
    if ($stderr) { Write-Host "Error: $stderr" }
    
    return $process.ExitCode -eq 0
}

Write-Host "开始上传文件..."

Write-Host "1. 上传 JAR 包..."
$result1 = Send-FileWithPassword -LocalPath 'e:\Hfnew\backend\target\hfnew-backend-1.4.0.jar' -RemotePath '/tmp/hfnew-backend-1.4.0.jar'

Write-Host "2. 上传前端压缩包..."
$result2 = Send-FileWithPassword -LocalPath 'e:\Hfnew\frontend\frontend-dist.zip' -RemotePath '/tmp/frontend-dist.zip'

Write-Host "3. 上传 Nginx 配置..."
$result3 = Send-FileWithPassword -LocalPath 'e:\Hfnew\deploy\nginx.conf' -RemotePath '/tmp/nginx.conf'

Write-Host "上传完成"
Write-Host "Results: JAR=$result1, Frontend=$result2, Nginx=$result3"
