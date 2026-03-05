<#
.SYNOPSIS
    Nexus Microservices Lab Infrastructure Manager
    Mục đích: Quản lý hạ tầng Lab một cách chuyên nghiệp và tự động.
#>

param (
    [Parameter(Mandatory=$true)]
    [ValidateSet("start", "stop", "restart", "logs", "status", "clean")]
    $Action,

    [Parameter(Mandatory=$false)]
    $Service = ""
)

$ComposeFile = "docker-compose.yml"

Write-Host "`n--- [ Nexus Lab Manager ] ---" -ForegroundColor Cyan

switch ($Action) {
    "start" {
        Write-Host "🚀 Khởi động toàn bộ hạ tầng Lab..." -ForegroundColor Green
        docker-compose -f $ComposeFile up -d
        Write-Host "✅ Đã gửi lệnh khởi động. Vui lòng đợi các container 'Healthy'." -ForegroundColor Yellow
    }
    
    "stop" {
        Write-Host "🛑 Đang dừng toàn bộ các dịch vụ..." -ForegroundColor Red
        docker-compose -f $ComposeFile stop
    }

    "restart" {
        Write-Host "🔄 Đang khởi động lại dịch vụ: $($Service -if $Service -else 'ALL')" -ForegroundColor Cyan
        if ($Service) {
            docker-compose -f $ComposeFile restart $Service
        } else {
            docker-compose -f $ComposeFile restart
        }
    }

    "logs" {
        if ($Service) {
            docker-compose -f $ComposeFile logs -f $Service
        } else {
            Write-Host "💡 Gợi ý: Dùng `.\manage.ps1 logs <service_name>` để xem log cụ thể."
            docker-compose -f $ComposeFile logs --tail=100 -f
        }
    }

    "status" {
        Write-Host "📊 Trạng thái các Container:" -ForegroundColor White
        docker-compose -f $ComposeFile ps
    }

    "clean" {
        Write-Host "⚠️ CẢNH BÁO: Thao tác này sẽ xóa toàn bộ Container và DỮ LIỆU (Volumes)!" -ForegroundColor Red
        $confirmation = Read-Host "Bạn có chắc chắn muốn tiếp tục? (y/N)"
        if ($confirmation -eq 'y') {
            docker-compose -f $ComposeFile down -v
            Write-Host "🧹 Đã dọn dẹp sạch sẽ tài nguyên." -ForegroundColor Green
        }
    }
}

Write-Host "----------------------------`n"
