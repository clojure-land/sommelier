variable "account" {}
variable "region" {}
variable "env" {}
variable "subenv" {}
variable "app_version" {}
variable "ami" {}
variable "security_groups" {
  type = "list"
}
variable "ssh_key" {}
variable instance_type {}
variable "min_instance_count" {}
variable "max_instance_count" {}
variable "desired_instance_count" {}
variable "desired_app_count" {}
variable "subnet_ids" {
  type = "list"
}
variable "vpc_id" {}
variable "ssl_cert_id" {}

provider aws {
  region = "${var.region}"
}

variable appname {
  default = "app"
}

resource "aws_ecs_cluster" "cluster" {
  name = "${var.env}-${var.subenv}-${var.appname}"
}

resource "aws_iam_instance_profile" "api-instance-profile" {
  name = "${var.env}-${var.subenv}-${var.appname}"
  role = "ecsInstanceRole"
}

resource "aws_launch_configuration" "launch_config" {
  name_prefix = "${var.env}-${var.subenv}-${var.appname}"
  image_id = "${var.ami}"

  instance_type = "${var.instance_type}"
  iam_instance_profile = "${aws_iam_instance_profile.api-instance-profile.name}"

  security_groups = ["${var.security_groups}"]
  key_name = "${var.ssh_key}"
  associate_public_ip_address = true

  lifecycle {
    create_before_destroy = true
  }

  user_data = <<EOF
#!/bin/bash
echo ECS_CLUSTER=${aws_ecs_cluster.cluster.name} >> /etc/ecs/ecs.config
EOF
}

resource "aws_autoscaling_group" "ecs" {
  name                 = "${aws_launch_configuration.launch_config.name}"
  launch_configuration = "${aws_launch_configuration.launch_config.name}"

  vpc_zone_identifier = ["${var.subnet_ids}"]

  min_size             = "${var.min_instance_count}"
  max_size             = "${var.max_instance_count}"
  desired_capacity     = "${var.desired_instance_count}"

  tag = {
    key = "Name"
    value = "${var.env}-${var.subenv}-${var.appname}"
    propagate_at_launch = true
  }

  tag = {
    key = "Env"
    value = "${var.env}"
    propagate_at_launch = true
  }
  tag = {
    key = "SubEnv"
    value = "${var.subenv}"
    propagate_at_launch = true
  }

  tag = {
    key = "Service"
    value = "${var.appname}"
    propagate_at_launch = true
  }
}

resource "aws_ecs_service" "app" {
  name = "${var.env}-${var.subenv}-${var.appname}"
  cluster = "${aws_ecs_cluster.cluster.id}"
  task_definition = "" //"${aws_ecs_task_definition.app.arn}"
  desired_count = "${var.desired_app_count}"
  deployment_minimum_healthy_percent = 50
  deployment_maximum_percent = 200
  iam_role = "arn:aws:iam::${var.account}:role/ecsServiceRole"

  load_balancer {
    target_group_arn = "${aws_lb_target_group.app-alb-tg.arn}"
    container_name = "${var.env}-${var.subenv}-${var.appname}"
    container_port = 3000
  }
}

resource "aws_alb" "app-alb" {
  name = "${var.env}-${var.subenv}-${var.appname}"
  subnets = "${var.subnet_ids}"
  security_groups = ["${var.security_groups}"]
  internal = false
  idle_timeout = 600
}

resource "aws_lb_listener" "app-alb-listener" {
  load_balancer_arn = "${aws_alb.app-alb.arn}"
  port = "443"
  protocol = "HTTPS"
  ssl_policy = "ELBSecurityPolicy-2015-05"
  certificate_arn = "${var.ssl_cert_id}"

  default_action {
    type = "forward"
    target_group_arn = "${aws_lb_target_group.app-alb-tg.arn}"
  }
}

resource "aws_lb_target_group" "app-alb-tg" {
  name = "${var.env}-${var.subenv}-${var.appname}"
  port = 3000
  health_check {
    path = "/v1/health"
  }
  protocol = "HTTP"
  vpc_id = "${var.vpc_id}"
  depends_on = [
    "aws_alb.app-alb"]
}



//resource "aws_ecs_task_definition" "app" {
//  family = "${var.env}-${var.subenv}-${var.appname}"
//  container_definitions = <<EOF
//  [
//	{
//	  "name": "${var.env}-${var.subenv}-${var.appname}",
//	  "image": "${var.account}.dkr.ecr.${var.region}.amazonaws.com/app:${var.app_version}",
//	  "cpu": ${var.cpu},
//	  "memoryReservation": ${var.memory},
//	  "essential": true,
//      "portMappings": [{
//        "containerPort": 3000,
//        "hostPort": 3000
//      }],
//      "command": ["run"],
//      "environment": [
//        { "name": "ENVIRONMENT", "value": "${var.env}"},
//        { "name": "SUBENVIRONMENT", "value": "${var.subenv}"},
//        { "name": "DB_HOST", "value": "${var.db_host}"},
//        { "name": "DB_PORT", "value": "${var.db_port}"},
//        { "name": "DB_NAME", "value": "${var.db_name}"},
//        { "name": "DB_USER", "value": "${var.db_user}"},
//        { "name": "DB_PASSWORD", "value": "${var.db_password}"}
//      ],
//	  "logConfiguration": {
//		"logDriver": "syslog",
//        "options": {
//          "syslog-address": "tcp://sumologic-shared.prod.octane.mediamath.com:514",
//          "tag": "${var.env}-${var.subenv}-${var.appname}/${var.app_version}/{{.ID}}"
//        }
//      },
//      "ulimits": [{
//        "name": "nofile",
//        "softLimit": 65536,
//        "hardLimit": 65536
//      }]
//    }
//  ]
//  EOF
//}

//variable "cpu" {}
//variable "memory" {}
//variable "zone_id" {}
//variable "db_host" {}
//variable "db_name" {}
//variable "db_port" {}
//variable "db_user" {}
//variable "db_password" {}

//output "app-dns" {
//  value = "${aws_route53_record.dns.name}"
//}
