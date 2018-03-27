variable "public_key_path" {
  description = <<DESCRIPTION
Path to the SSH public key to be used for authentication.
Ensure this keypair is added to your local SSH agent so provisioners can
connect.

Example: ~/.ssh/monitoring_aws.pub
DESCRIPTION
}

variable "key_name" {
  default = "monitoring-key"
  description = "Desired name of AWS key pair"
}

variable "region" {}

variable "ami" {}

variable "instance_types" {
  type = "map"
}

variable "num_instances" {
  type = "map"
}

variable "kinesis" {
  type = "map"
}

provider "aws" {
  region = "${var.region}"
}

variable "enable_dns_hostnames" {
  description = "Should be true to enable DNS hostnames in the VPC"
  default = true
}

# Create a VPC to launch our instances into
resource "aws_vpc" "monitoring_vpc" {
  cidr_block = "10.0.0.0/16"

  enable_dns_hostnames = true

  tags {
    Name = "Grafana-VPC"
  }
}

# Create an internet gateway to give our subnet access to the outside world
resource "aws_internet_gateway" "default" {
  vpc_id = "${aws_vpc.monitoring_vpc.id}"
}

# Grant the VPC internet access on its main route table
resource "aws_route" "internet_access" {
  route_table_id = "${aws_vpc.monitoring_vpc.main_route_table_id}"
  destination_cidr_block = "0.0.0.0/0"
  gateway_id = "${aws_internet_gateway.default.id}"
}

# Create a subnet to launch our instances into
resource "aws_subnet" "monitoring_subnet" {
  vpc_id = "${aws_vpc.monitoring_vpc.id}"
  cidr_block = "10.0.0.0/24"
  map_public_ip_on_launch = true
}

resource "aws_security_group" "monitoring_security_group" {
  name = "terraform"
  vpc_id = "${aws_vpc.monitoring_vpc.id}"

  # SSH access from anywhere
  ingress {
    from_port = 22
    to_port = 22
    protocol = "tcp"
    cidr_blocks = [
      "0.0.0.0/0"]
  }

  # Grafana UI
  ingress {
    from_port = 3000
    to_port = 3000
    protocol = "tcp"
    cidr_blocks = [
      "0.0.0.0/0"]
  }

  # Influx
  ingress {
    from_port = 8083
    to_port = 8083
    protocol = "tcp"
    cidr_blocks = [
      "0.0.0.0/0"]
  }

  # Influx
  ingress {
    from_port = 8086
    to_port = 8086
    protocol = "tcp"
    cidr_blocks = [
      "0.0.0.0/0"]
  }

  # All ports open within the VPC
  ingress {
    from_port = 0
    to_port = 65535
    protocol = "tcp"
    cidr_blocks = [
      "10.0.0.0/16"]
  }

  # outbound internet access
  egress {
    from_port = 0
    to_port = 0
    protocol = "-1"
    cidr_blocks = [
      "0.0.0.0/0"]
  }

  tags {
    Name = "Monitoring-Security-Group"
  }
}

resource "aws_key_pair" "auth" {
  key_name = "${var.key_name}"
  public_key = "${file(var.public_key_path)}"
}

resource "aws_instance" "grafana" {
  ami = "${var.ami}"
  instance_type = "${var.instance_types["grafana"]}"
  key_name = "${aws_key_pair.auth.id}"
  subnet_id = "${aws_subnet.monitoring_subnet.id}"
  vpc_security_group_ids = [
    "${aws_security_group.monitoring_security_group.id}"]
  count = "${var.num_instances["grafana"]}"
  monitoring = true

  tags {
    Name = "grafana-${count.index} influxdb"
  }
}

resource "aws_kinesis_stream" "test_stream" {
  name = "${var.kinesis["streamName"]}"
  shard_count = "${var.kinesis["shards"]}"
  retention_period = "${var.kinesis["retentionPeriod"]}"

  tags {
    Environment = "test"
  }
}

output "client_ssh_host" {
  value = "${aws_instance.grafana.0.public_ip}"
}

output "public_dns" {
  description = "List of public DNS names assigned to the instances. For EC2-VPC, this is only available if you've enabled DNS hostnames for your VPC"
  value = "${aws_instance.grafana.0.public_dns}"
}

output "public_ip" {
  description = "List of public IP addresses assigned to the instances, if applicable"
  value = [
    "${aws_instance.grafana.0.public_ip}"]
}
