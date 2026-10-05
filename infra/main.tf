provider "aws" {
  region = "sa-east-1"
}

resource "aws_instance" "pedidos_api" {
  ami           = "ami-0abcdef1234567890"
  instance_type = "m6i.large"
}

resource "aws_s3_bucket" "extratos" {
  bucket = "virtualthreads-extratos"

  tags = {
    CostCenter = "4410"
    Name       = "extratos"
  }
}
