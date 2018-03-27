public_key_path = "~/.ssh/monitoring_aws.pub"
region = "us-east-1"
ami = "ami-1853ac65" //Amazon Linux

instance_types = {
  "grafana" = "t2.micro"
  "consumer" = "t2.micro"
}

num_instances = {
  "grafana" = 1
  "consumer" = 1
}

kinesis = {
  "streamName" = "SensorStream"
  "shards" = 1
  "retentionPeriod" = 48
}
