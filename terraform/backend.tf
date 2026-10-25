terraform {
  backend "s3" {
    bucket         = "codeduels-tf-state-1416"
    key            = "global/s3/terraform.tfstate"
    region         = "ap-south-1"
    dynamodb_table = "terraform-state-lock"
    encrypt        = true
  }
}
