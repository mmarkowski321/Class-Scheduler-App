$KeyName = "eduscheduler-key"
$Region = "eu-north-1"
$SSHPath = "$env:USERPROFILE\.ssh"

Write-Host "Creating SSH Key Pair for AWS EC2" -ForegroundColor Cyan
Write-Host ""
if (-not (Test-Path $SSHPath)) {
    Write-Host "Creating .ssh directory..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Force -Path $SSHPath | Out-Null
}
$PrivateKeyPath = "$SSHPath\$KeyName"
$PublicKeyPath = "$SSHPath\$KeyName.pub"

if (Test-Path $PrivateKeyPath) {
    Write-Host "Warning: Key already exists locally: $PrivateKeyPath" -ForegroundColor Yellow
    $overwrite = Read-Host "Do you want to overwrite it? (y/N)"
    if ($overwrite -ne "y" -and $overwrite -ne "Y") {
        Write-Host "Cancelled." -ForegroundColor Red
        exit 1
    }
    Remove-Item $PrivateKeyPath -Force -ErrorAction SilentlyContinue
    Remove-Item $PublicKeyPath -Force -ErrorAction SilentlyContinue
}

Write-Host "Checking if key exists in AWS..." -ForegroundColor Yellow
$existingKey = aws ec2 describe-key-pairs --key-names $KeyName --region $Region --query 'KeyPairs[0].KeyName' --output text 2>$null

if ($existingKey -eq $KeyName) {
    Write-Host "Warning: Key already exists in AWS: $KeyName" -ForegroundColor Yellow
    $delete = Read-Host "Do you want to delete it from AWS and create new? (y/N)"
    if ($delete -eq "y" -or $delete -eq "Y") {
        Write-Host "Deleting key from AWS..." -ForegroundColor Yellow
        aws ec2 delete-key-pair --key-name $KeyName --region $Region 2>$null
        Write-Host "Deleted from AWS" -ForegroundColor Green
    } else {
        Write-Host "Cancelled." -ForegroundColor Red
        exit 1
    }
}

Write-Host "Generating SSH key pair..." -ForegroundColor Yellow
ssh-keygen -t rsa -b 4096 -f $PrivateKeyPath -N '""' -q

if (-not (Test-Path $PrivateKeyPath)) {
    Write-Host "Error: Failed to generate SSH key pair" -ForegroundColor Red
    exit 1
}

Write-Host "SSH key pair generated:" -ForegroundColor Green
Write-Host "Private key: $PrivateKeyPath" -ForegroundColor Gray
Write-Host "Public key:  $PublicKeyPath" -ForegroundColor Gray

Write-Host ""
Write-Host "Importing public key to AWS..." -ForegroundColor Yellow

$publicKeyContent = (Get-Content $PublicKeyPath -Raw).Trim()
$tempPublicKeyFile = [System.IO.Path]::GetTempFileName()

try {
    [System.IO.File]::WriteAllText($tempPublicKeyFile, $publicKeyContent, [System.Text.UTF8Encoding]::new($false))
    $importResult = aws ec2 import-key-pair `
        --key-name $KeyName `
        --public-key-material fileb://$tempPublicKeyFile `
        --region $Region 2>&1
    
    if ($LASTEXITCODE -ne 0) {
        throw $importResult
    }
    
    Write-Host "Public key imported to AWS successfully!" -ForegroundColor Green
} catch {
    Write-Host "Error: Failed to import key to AWS" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    if (Test-Path $tempPublicKeyFile) {
        Remove-Item $tempPublicKeyFile -Force
    }
    exit 1
} finally {
    if (Test-Path $tempPublicKeyFile) {
        Remove-Item $tempPublicKeyFile -Force
    }
}

Write-Host ""
Write-Host "Verifying..." -ForegroundColor Yellow
$verifyKey = aws ec2 describe-key-pairs --key-names $KeyName --region $Region --query 'KeyPairs[0].KeyName' --output text 2>$null

if ($verifyKey -eq $KeyName) {
    Write-Host "Key pair verified in AWS!" -ForegroundColor Green
} else {
    Write-Host "Warning: Key not found in AWS after import" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Key Pair Created Successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "Key Name:     $KeyName"
Write-Host "Region:       $Region"
Write-Host "Private Key:  $PrivateKeyPath"
Write-Host ""
Write-Host "Keep your private key safe!"
Write-Host "Next: ./aws/create-vm-simple.sh $KeyName"
Write-Host ""

