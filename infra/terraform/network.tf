locals {
  name_prefix = "${var.project}-${var.environment}"
}

# ---------------------------------------------------------------------------
# 네트워크: 전용 VPC + 퍼블릭 서브넷 1개
# 상시 과금되는 NAT Gateway는 만들지 않는다. EC2는 퍼블릭 IP로 직접 인터넷에 나간다.
# ---------------------------------------------------------------------------
data "aws_availability_zones" "available" {
  state = "available"
}

resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = "${local.name_prefix}-vpc"
  }
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "${local.name_prefix}-igw"
  }
}

resource "aws_subnet" "public" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = var.public_subnet_cidr
  availability_zone = data.aws_availability_zones.available.names[0]

  # 퍼블릭 IP는 인스턴스(또는 Elastic IP)에서 명시적으로 정한다.
  map_public_ip_on_launch = false

  tags = {
    Name = "${local.name_prefix}-public"
  }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = "${local.name_prefix}-public"
  }
}

resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

# ---------------------------------------------------------------------------
# 방화벽(보안 그룹)
# - 앱 포트(API + Swagger UI)만 app_allowed_cidrs에 연다.
# - SSH(22)는 기본적으로 닫혀 있다. 접속은 SSM Session Manager를 쓴다. ssh_allowed_cidrs를 지정하면 연다.
# - PostgreSQL(5432)·Redis(6379)는 규칙이 없어 인스턴스 밖에서 접근할 수 없다.
# ---------------------------------------------------------------------------
resource "aws_security_group" "app" {
  name        = "${local.name_prefix}-app"
  description = "HMC-BE app server"
  vpc_id      = aws_vpc.main.id

  tags = {
    Name = "${local.name_prefix}-app"
  }
}

resource "aws_vpc_security_group_ingress_rule" "app" {
  for_each = toset(var.app_allowed_cidrs)

  security_group_id = aws_security_group.app.id
  description       = "HMC-BE API and Swagger UI"
  ip_protocol       = "tcp"
  from_port         = var.app_port
  to_port           = var.app_port
  cidr_ipv4         = each.value
}

resource "aws_vpc_security_group_ingress_rule" "ssh" {
  for_each = toset(var.ssh_allowed_cidrs)

  security_group_id = aws_security_group.app.id
  description       = "SSH"
  ip_protocol       = "tcp"
  from_port         = 22
  to_port           = 22
  cidr_ipv4         = each.value
}

resource "aws_vpc_security_group_egress_rule" "all" {
  security_group_id = aws_security_group.app.id
  description       = "Allow all outbound (패키지 설치, 이미지 pull, S3, SSM)"
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"
}
