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
//    "sg-b6fed0cd",
//    "sg-010112f99d8eec969"
  ]
}
variable subnet_ids {
  type = "list"
  default = [
//    "subnet-331a3619",
//    "subnet-931408e5",
//    "subnet-fe5c5bc3",
//    "subnet-a12d03f9"
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
  default = "ami-71ef560b"
}

module example-app {
  source = "..\/..\/plan"

  region = "${var.region}"
  account = "${var.account}"
  env = "example"
  subenv = "${var.name}"
  app_version = "${var.version}"
  ami = "${var.ami}"
  instance_type = "t2.micro"
//  cpu = 256
//  memory = "250"
//  db_host = "example-shared-octane-s2s-sftp.crhvhlvhwshi.us-east-1.rds.amazonaws.com"
//  db_name = "${var.db_name}"
//  db_port = 3306
//  db_user = "${data.aws_kms_secrets.example_rds.plaintext["rds_username"]}"
//  db_password = "${data.aws_kms_secrets.example_rds.plaintext["rds_password"]}"
  security_groups = ["${var.security_groups}"]
  ssh_key = "" //<<<
  min_instance_count = "${var.min_instance_count}"
  max_instance_count = "${var.max_instance_count}"
  desired_instance_count = "${var.desired_instance_count}"
  desired_app_count = "${var.desired_app_count}"
  subnet_ids = ["${var.subnet_ids}"]
  zone_id = "" //<<<
  vpc_id = "vpc-7137ba16" //<<<
  ssl_cert_id = "arn:aws:acm:us-east-1:${var.account}:certificate/af757e51-da40-40c2-992a-8d234ecf5875" //<<<
}
