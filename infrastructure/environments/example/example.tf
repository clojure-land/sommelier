terraform {
  backend "s3" {
    region = "eu-west-2"
  }
}

variable account {}

variable name {}

variable region {
  default = "eu-west-2"
}

variable version {}

variable security_groups {
  type = "list"
  default = [
    "sg-16173348"
  ]
}

variable subnet_ids {
  type = "list"
  default = [
    "subnet-c9ec00c7",
    "subnet-c01f2eee",
    "subnet-afb8c691",
    "subnet-a086adc7",
    "subnet-80e01acd",
    "subnet-481c2b14"
  ]
}

variable min_instance_count {
  default = "1"
}

variable max_instance_count {
  default = "1"
}

variable desired_instance_count {
  default = "1"
}

variable desired_app_count {
  default = "1"
}

//data "aws_kms_secrets" "example_rds" {
//  secret {
//    name = "rds_username"
//    payload = "AQICAHhcrUP7tfkBaSc5Ojbl9R4E27SYeQDe+KsyVw5j5JcpOQGCx0ouwKjZS4iSG8gAphZJAAAAajBoBgkqhkiG9w0BBwagWzBZAgEAMFQGCSqGSIb3DQEHATAeBglghkgBZQMEAS4wEQQMHSahAAm15wxfAphDAgEQgCcqBpDdvtaGszEZKo99AI4Z0I/cQdrLfKPzsc1vIixRABk0b6wW78c="
//  }
//
//  secret {
//    name = "rds_password"
//    payload = "AQICAHhcrUP7tfkBaSc5Ojbl9R4E27SYeQDe+KsyVw5j5JcpOQEFvwtvT8BaCa6qbLlwp4VKAAAAcjBwBgkqhkiG9w0BBwagYzBhAgEAMFwGCSqGSIb3DQEHATAeBglghkgBZQMEAS4wEQQMTFn+At584snd6uKzAgEQgC9S4hFUw+FKhxP4byGVfriTO5Wki/+ZcXdnyFa8GS452YMCYxZ5rh3t3VcpCPLmHw=="
//  }
//}

variable ami {
  default = "ami-4f8d912b"
}

module example-sommelier {
  source = "../../plan"

  region = "${var.region}"
  account = "${var.account}"
  env = "example"
  subenv = "${var.name}"
  app_version = "${var.version}"
  ami = "${var.ami}"
  instance_type = "t2.micro"
  cpu = 256
  memory = "250"
  security_groups = ["${var.security_groups}"]
  min_instance_count = "${var.min_instance_count}"
  max_instance_count = "${var.max_instance_count}"
  desired_instance_count = "${var.desired_instance_count}"
  desired_app_count = "${var.desired_app_count}"
  subnet_ids = ["${var.subnet_ids}"]
  zone_id = "" //<<<
  vpc_id = "vpc-7137ba16" //<<<
  ssl_cert_id = "arn:aws:acm:us-east-1:${var.account}:certificate/af757e51-da40-40c2-992a-8d234ecf5875" //<<<
}
