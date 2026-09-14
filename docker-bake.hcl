variable "REGISTRY" { default = "docker.io" }
variable "NAMESPACE" { default = "binarycodes" }
variable "APP_NAME" { default = "frankfurter-mcp" }
variable "APP_VERSION" { default = "0.1.0-SNAPSHOT" }

variable "TAG_NAME" { default = "frankfurter-mcp" }
variable "GIT_SHA" { default = "" }

group "default" {
  targets = ["app"]
}

target "app" {
  context    = "."
  dockerfile = "Dockerfile"

  args = {
    APP_NAME    = APP_NAME
    APP_VERSION = APP_VERSION
    GIT_SHA     = GIT_SHA
  }

  tags = [
    "${REGISTRY}/${NAMESPACE}/${TAG_NAME}:${APP_VERSION}",
    "${REGISTRY}/${NAMESPACE}/${TAG_NAME}:latest",
  ]

  platforms = ["linux/amd64", "linux/arm64"]
}

# Host-platform image loaded into the local engine: `docker buildx bake local`.
target "local" {
  inherits  = ["app"]
  platforms = []
  output    = ["type=docker"]
}
