provider "aws" {
  region = var.region
}

# ---------------------------------------------------------------------------
# Variables
# ---------------------------------------------------------------------------

variable "region" {
  description = "AWS region to deploy into"
  type        = string
}

variable "control_plane_port" {
  description = "cdb control plane port"
  type        = number
}

variable "ami" {
  description = "AMI for the EC2 instance"
  type        = string
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
}

variable "replica_ami" {
  description = "replica AMI for the EC2 instance"
  type        = string
}

variable "replica_instance_type" {
  description = "replica EC2 instance type"
  type        = string
}

# ---------------------------------------------------------------------------
# Shared infrastructure (VPC, subnet) from cdb-shared-infra
# ---------------------------------------------------------------------------

data "aws_caller_identity" "current" {}

data "terraform_remote_state" "shared_infra" {
  backend = "s3"

  config = {
    bucket = "cdb-tf-state-${data.aws_caller_identity.current.account_id}"
    key    = "shared-infra/terraform.tfstate"
    region = var.region
  }
}

data "terraform_remote_state" "bootstrap" {
  backend = "s3"

  config = {
    bucket = "cdb-tf-state-${data.aws_caller_identity.current.account_id}"
    key    = "control-plane/bootstrap/terraform.tfstate"
    region = var.region
  }
}

# ---------------------------------------------------------------------------
# Security Group
# ---------------------------------------------------------------------------

resource "aws_security_group" "cdb_control_plane_sg" {
  name        = "cdb-control-plane-sg"
  description = "Allow public access from anywhere to control plane server"
  vpc_id      = data.terraform_remote_state.shared_infra.outputs.cdb_vpc_id

  ingress {
    description = "Allow HTTP app traffic"
    from_port   = var.control_plane_port
    to_port     = var.control_plane_port
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "cdb-control-plane-sg"
  }
}

resource "aws_security_group" "cdb_replica_sg" {
  name        = "cdb-replica-sg"
  description = "Allow public Redis access from anywhere"
  vpc_id      = data.terraform_remote_state.shared_infra.outputs.cdb_vpc_id

  ingress {
    description = "Allow Redis traffic"
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "cdb-replica-sg"
  }
}

# ---------------------------------------------------------------------------
# Build & Push Image to ECR
# ---------------------------------------------------------------------------

resource "null_resource" "build_and_push" {
  triggers = {
    dockerfile_hash = filemd5("Dockerfile")
    pom_hash        = filemd5("pom.xml")
  }

  provisioner "local-exec" {
    interpreter = ["/bin/bash", "-c"]
    command     = join(" && ", [
      "aws ecr get-login-password --region ${var.region} | docker login --username AWS --password-stdin ${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.region}.amazonaws.com",
      "docker build -t cdb-control-plane .",
      "docker tag cdb-control-plane:latest ${data.terraform_remote_state.bootstrap.outputs.cdb_control_plane_ecr_url}:latest",
      "docker push ${data.terraform_remote_state.bootstrap.outputs.cdb_control_plane_ecr_url}:latest"
    ])
  }
}

# ---------------------------------------------------------------------------
# EC2 Instance
# ---------------------------------------------------------------------------

locals {
  ecr_image_uri = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.region}.amazonaws.com/cdb-control-plane:latest"
}

resource "aws_instance" "cdb_control_plane" {
  ami                         = var.ami
  instance_type               = var.instance_type
  subnet_id                   = data.terraform_remote_state.shared_infra.outputs.cdb_public_subnet_id
  vpc_security_group_ids      = [aws_security_group.cdb_control_plane_sg.id]
  associate_public_ip_address = true
  iam_instance_profile        = aws_iam_instance_profile.cdb_control_plane.name

  user_data = <<-EOF
    #!/bin/bash
    set -e

    # Install Docker
    apt-get update -y
    apt-get install -y docker.io unzip
    curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o /tmp/awscliv2.zip
    unzip /tmp/awscliv2.zip -d /tmp
    /tmp/aws/install
    systemctl enable docker
    systemctl start docker

    # Install Docker Compose
    mkdir -p /usr/local/lib/docker/cli-plugins
    curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
      -o /usr/local/lib/docker/cli-plugins/docker-compose
    chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

    # Write docker-compose.yml
    echo "${base64encode(file("docker-compose.yml"))}" | base64 -d > /home/ubuntu/docker-compose.yml

    # Set environment variables
    export ECR_IMAGE_URI="${local.ecr_image_uri}"
    export CONTROL_PLANE_PORT="${var.control_plane_port}"
    export AWS_ACCOUNT_ID="${data.aws_caller_identity.current.account_id}"
    export AWS_REGION="${var.region}"
    export AWS_REPLICA_AMI_ID="${var.replica_ami}"
    export AWS_REPLICA_INSTANCE_TYPE="${var.replica_instance_type}"
    export AWS_REPLICA_SUBNET_ID="${data.terraform_remote_state.shared_infra.outputs.cdb_public_subnet_id}"
    export AWS_REPLICA_SECURITY_GROUP_ID="${aws_security_group.cdb_replica_sg.id}"
    export AWS_REPLICA_IAM_INSTANCE_PROFILE_NAME="${aws_iam_instance_profile.cdb_replica.name}"

    # Login to ECR and pull image
    aws ecr get-login-password --region ${var.region} \
      | docker login --username AWS --password-stdin \
        ${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.region}.amazonaws.com

    # Start service
    cd /home/ubuntu && docker compose pull && docker compose up -d
  EOF

  depends_on = [null_resource.build_and_push]

  tags = {
    Name = "cdb-control-plane"
  }
}

resource "aws_iam_role" "cdb_control_plane" {
  name = "cdb-control-plane"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role" "cdb_replica" {
  name = "cdb-replica"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy" "cdb_control_plane_pass_role" {
  name = "cdb-control-plane-pass-replica-role"
  role = aws_iam_role.cdb_control_plane.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = "iam:PassRole"
      Resource = aws_iam_role.cdb_replica.arn
    }]
  })
}

resource "aws_iam_role_policy_attachment" "cdb_control_plane_ssm" {
  role       = aws_iam_role.cdb_control_plane.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_role_policy_attachment" "cdb_chronicle_service_ecr" {
  role       = aws_iam_role.cdb_control_plane.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

resource "aws_iam_role_policy_attachment" "cdb_control_plane_dynamodb" {
  role       = aws_iam_role.cdb_control_plane.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess"
}

resource "aws_iam_role_policy_attachment" "cdb_control_plane_ec2" {
  role       = aws_iam_role.cdb_control_plane.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2FullAccess"
}

resource "aws_iam_role_policy_attachment" "cdb_replica_ssm" {
  role       = aws_iam_role.cdb_replica.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_role_policy_attachment" "cdb_replica_ecr" {
  role       = aws_iam_role.cdb_replica.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

resource "aws_iam_instance_profile" "cdb_control_plane" {
  name = "cdb-control-plane-profile"
  role = aws_iam_role.cdb_control_plane.name
}

resource "aws_iam_instance_profile" "cdb_replica" {
  name = "cdb-replica-profile"
  role = aws_iam_role.cdb_replica.name
}

# ---------------------------------------------------------------------------
# Outputs
# ---------------------------------------------------------------------------

output "cdb_control_plane_private_ip" {
  value = aws_instance.cdb_control_plane.private_ip
}
output "cdb_control_plane_public_ip" {
  value = aws_instance.cdb_control_plane.public_ip
}