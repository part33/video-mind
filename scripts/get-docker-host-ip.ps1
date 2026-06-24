$ErrorActionPreference = 'SilentlyContinue'

$ip = $null

try {
    $addresses = [System.Net.Dns]::GetHostAddresses('host.docker.internal')
    foreach ($address in $addresses) {
        if ($address.AddressFamily -eq [System.Net.Sockets.AddressFamily]::InterNetwork) {
            $ip = $address.IPAddressToString
            break
        }
    }
} catch {
}

if (-not $ip) {
    $candidates = Get-NetIPAddress -AddressFamily IPv4 |
        Where-Object {
            $_.IPAddress -notlike '127.*' -and
            $_.InterfaceAlias -notmatch 'Loopback|vEthernet|Docker|Hyper-V|VMware|Bluetooth'
        } |
        Select-Object -ExpandProperty IPAddress

    foreach ($candidate in $candidates) {
        if ($candidate) {
            $ip = $candidate
            break
        }
    }
}

if ($ip) {
    Write-Output $ip
}
