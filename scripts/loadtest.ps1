param(
  [string]$BaseUrl = "http://localhost:8080",
  [string]$Token,
  [int]$Iterations = 50
)

if ([string]::IsNullOrWhiteSpace($Token)) {
  Write-Host "Token is required. Example: .\scripts\loadtest.ps1 -Token <access_token>"
  exit 1
}

$headers = @{ Authorization = "Bearer $Token" }
$endpoints = @(
  "/api/warehouse/stocks?page=1&pageSize=10",
  "/api/pharmacy/expiry-warnings",
  "/api/dashboard/fee-warnings",
  "/api/notifications/summary"
)

function Test-Endpoint([string]$url, [int]$n) {
  $sw = New-Object System.Diagnostics.Stopwatch
  $times = New-Object System.Collections.Generic.List[double]
  for ($i = 1; $i -le $n; $i++) {
    $sw.Restart()
    try {
      Invoke-WebRequest -UseBasicParsing -Headers $headers -Uri $url -Method GET | Out-Null
      $sw.Stop()
      $times.Add($sw.Elapsed.TotalMilliseconds)
    } catch {
      $sw.Stop()
      $times.Add([double]::NaN)
    }
  }

  $ok = $times | Where-Object { -not [double]::IsNaN($_) }
  $fail = $times.Count - $ok.Count
  $avg = if ($ok.Count -gt 0) { ($ok | Measure-Object -Average).Average } else { [double]::NaN }
  $p95 = if ($ok.Count -gt 0) {
    $sorted = $ok | Sort-Object
    $idx = [math]::Ceiling(0.95 * $sorted.Count) - 1
    $sorted[[math]::Max(0, $idx)]
  } else { [double]::NaN }

  [pscustomobject]@{
    Url = $url
    Iterations = $n
    Failures = $fail
    AvgMs = [math]::Round($avg, 2)
    P95Ms = [math]::Round($p95, 2)
  }
}

$results = @()
foreach ($ep in $endpoints) {
  $url = $BaseUrl.TrimEnd("/") + $ep
  Write-Host "Testing $url ($Iterations requests)..."
  $results += Test-Endpoint -url $url -n $Iterations
}

$results | Format-Table -AutoSize

