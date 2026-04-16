provider "aws" {
  region = var.region
}

# ---------------------------------------------------------------------------
# Backend
# ---------------------------------------------------------------------------

terraform {
  backend "s3" {}
}

# ---------------------------------------------------------------------------
# Variables
# ---------------------------------------------------------------------------

variable "region" {
  description = "AWS region to deploy into"
  type        = string
}

# ---------------------------------------------------------------------------
# ECR Repositories
# ---------------------------------------------------------------------------

resource "aws_ecr_repository" "cdb_control_plane" {
  name                 = "cdb-control-plane"
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name = "cdb-control-plane"
  }
}

resource "aws_ecr_repository" "cdb_tx_managers" {
  name                 = "cdb-tx-managers"
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name = "cdb-tx-managers"
  }
}

resource "aws_ecr_repository" "cdb_storage_engines" {
  name                 = "cdb-storage-engines"
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name = "cdb-storage-engines"
  }
}

resource "aws_ecr_repository" "cdb_appliers" {
  name                 = "cdb-appliers"
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name = "cdb-appliers"
  }
}

# ---------------------------------------------------------------------------
# Outputs
# ---------------------------------------------------------------------------

output "cdb_control_plane_ecr_url" {
  value = aws_ecr_repository.cdb_control_plane.repository_url
}

output "cdb_tx_managers_ecr_url" {
  value = aws_ecr_repository.cdb_tx_managers.repository_url
}

output "cdb_storage_engines_ecr_url" {
  value = aws_ecr_repository.cdb_storage_engines.repository_url
}

output "cdb_appliers_ecr_url" {
  value = aws_ecr_repository.cdb_appliers.repository_url
}