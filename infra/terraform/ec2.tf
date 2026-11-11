# ---------------------------------------------------------------------------
# EC2: 앱 서버 1대 (Amazon Linux 2023, x86_64)
# ---------------------------------------------------------------------------

# AWS가 관리하는 공개 SSM 파라미터로 최신 AL2023 AMI를 찾는다.
data "aws_ssm_parameter" "al2023_ami" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

# 인스턴스 IAM 역할: 액세스 키 없이 S3를 쓰고(AWS SDK 기본 자격증명 체인), SSM Session Manager로 접속한다.
data "aws_iam_policy_document" "ec2_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "ec2" {
  name               = "${local.name_prefix}-ec2"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume_role.json
}

resource "aws_iam_role_policy_attachment" "ec2_s3" {
  role       = aws_iam_role.ec2.name
  policy_arn = aws_iam_policy.app_s3.arn
}

resource "aws_iam_role_policy_attachment" "ec2_ssm" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "ec2" {
  name = "${local.name_prefix}-ec2"
  role = aws_iam_role.ec2.name
}

resource "aws_instance" "app" {
  ami                    = data.aws_ssm_parameter.al2023_ami.value
  instance_type          = var.instance_type
  subnet_id              = aws_subnet.public.id
  vpc_security_group_ids = [aws_security_group.app.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2.name
  key_name               = var.key_name

  # 항상 true로 둔다. 첫 부팅 스크립트(dnf, 이미지 pull)는 Elastic IP가 붙기 전에 시작되므로
  # 자동 할당 IP가 없으면 인터넷에 나가지 못해 실패한다. Elastic IP가 붙으면 자동 할당 IP는 해제된다.
  # (이 값을 바꾸면 인스턴스가 교체되어 DB 데이터가 사라지므로 변수로 빼지 않았다.)
  associate_public_ip_address = true

  # IMDSv2 필수. 컨테이너 안의 앱이 인스턴스 역할 자격증명을 받으려면 홉 제한이 2 이상이어야 한다.
  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 2
  }

  root_block_device {
    volume_type           = "gp3"
    volume_size           = var.root_volume_size_gb
    encrypted             = true
    delete_on_termination = true
  }

  user_data = templatefile("${path.module}/user_data.sh.tftpl", {
    region                  = var.region
    s3_bucket               = aws_s3_bucket.uploads.bucket
    swap_size_gb            = var.swap_size_gb
    docker_compose_version  = var.docker_compose_version
    bootstrap_data_services = var.bootstrap_data_services
  })

  tags = {
    Name = "${local.name_prefix}-app"
  }

  # PostgreSQL 데이터가 이 인스턴스의 디스크에 있다. 새 AMI가 나오거나 user_data를 고쳐도 인스턴스가
  # 교체(=데이터 삭제)되지 않도록 무시한다. 일부러 교체할 때만: terraform apply -replace=aws_instance.app
  lifecycle {
    ignore_changes = [ami, user_data]
  }

  depends_on = [
    aws_internet_gateway.main,
    aws_iam_role_policy_attachment.ec2_s3,
    aws_iam_role_policy_attachment.ec2_ssm,
  ]
}

# 고정 퍼블릭 IP. 인스턴스를 중지/시작해도 주소가 유지된다.
resource "aws_eip" "app" {
  count = var.create_elastic_ip ? 1 : 0

  domain   = "vpc"
  instance = aws_instance.app.id

  tags = {
    Name = "${local.name_prefix}-app"
  }

  depends_on = [aws_internet_gateway.main]
}

locals {
  app_public_ip = try(aws_eip.app[0].public_ip, aws_instance.app.public_ip)
}
