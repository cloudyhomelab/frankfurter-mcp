# frankfurter-mcp

A [Model Context Protocol](https://modelcontextprotocol.io) server that lets AI agents query the
[Frankfurter](https://frankfurter.dev) exchange-rate API: current and historical mid-market rates
for ~200 currencies, sourced from ~98 central banks. No API key is needed.

Built with Java 21, Spring Boot 4 and Spring AI's MCP server starter. Supports the
**Streamable HTTP** transport (default) and **stdio** (for clients that launch the server as a subprocess).

## Tools

| Tool | What it does |
|---|---|
| `get_latest_rates` | Latest rates for a base currency, optionally filtered to some quote currencies |
| `get_historical_rates` | Rates for a base currency on a given date |
| `get_time_series` | Rates over a date range, optionally downsampled by `week` or `month` |
| `get_rate` | A single currency pair, latest or on a date |
| `convert_currency` | Convert an amount using the latest or a historical rate (conversion is computed locally) |
| `list_currencies` | Supported currencies with code, name, symbol and date coverage |
| `get_currency` | Details for one currency, including which providers publish it |
| `list_providers` | The central banks and official sources behind the data, with their keys |
| `get_provider` | Full details for one provider |

Every rate tool accepts an optional `providers` argument (e.g. `ECB`) to get one source's official
reference rate instead of the blended default. All tools are read-only and idempotent.

## Run

Requires JDK 21+ and Maven.

```bash
./mvnw -v 2>/dev/null || mvn -v      # check your toolchain
mvn test                             # unit + end-to-end tests (no network needed)
mvn package -DskipTests
java -jar target/frankfurter-mcp-0.1.0-SNAPSHOT.jar
```

The MCP endpoint is `http://localhost:8080/mcp`.

### stdio

```bash
java -jar target/frankfurter-mcp-0.1.0-SNAPSHOT.jar --spring.profiles.active=stdio
```

Logs go to stderr in this mode so stdout stays clean for the protocol.

### Docker

The repo ships a multi-stage `Dockerfile` and a `docker-bake.hcl` for multi-arch builds. The jar is built once
on the host platform and copied into a JRE image per target platform, so multi-arch builds do not run Maven
under emulation.

```bash
# image for this machine, loaded into the local engine
docker buildx bake local
docker run --rm -p 8080:8080 docker.io/binarycodes/frankfurter-mcp:0.1.0-SNAPSHOT

# multi-arch (amd64 + arm64), pushed to the registry
APP_VERSION="$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)" \
GIT_SHA="$(git rev-parse HEAD)" docker buildx bake --push
```

Bake variables: `REGISTRY` (`docker.io`), `NAMESPACE` (`binarycodes`), `TAG_NAME`, `APP_NAME`, `APP_VERSION`
and `GIT_SHA` (recorded as the `org.opencontainers.image.revision` label). The image runs as a non-root user
and has a Docker `HEALTHCHECK` on the MCP endpoint.

## Connect a client

Claude Code (Streamable HTTP):

```bash
claude mcp add --transport http frankfurter http://localhost:8080/mcp
```

Claude Desktop or any stdio client (`claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "frankfurter": {
      "command": "java",
      "args": ["-jar", "/path/to/frankfurter-mcp-0.1.0-SNAPSHOT.jar", "--spring.profiles.active=stdio"]
    }
  }
}
```

## Configuration

| Property | Default | Purpose |
|---|---|---|
| `frankfurter.base-url` | `https://api.frankfurter.dev/v2` | Point at a [self-hosted](https://frankfurter.dev/deploy/) instance |
| `server.port` | `8080` | HTTP port |
| `spring.ai.mcp.server.streamable-http.mcp-endpoint` | `/mcp` | MCP endpoint path |
| `spring.http.clients.read-timeout` | `15s` | Upstream read timeout |

Set them as `--property=value` flags, environment variables (`FRANKFURTER_BASE_URL=...`), or in `application.yml`.

## CI

- `.github/workflows/ci.yml` runs `mvn verify` on every pull request and push to `main`.
- On a push to `main`, and only after `verify` passes, it calls the reusable
  `.github/workflows/docker.yml`, which builds the `linux/amd64` + `linux/arm64` image with `docker buildx bake`,
  tags it with the Maven version and `latest`, pushes it to Docker Hub with provenance and SBOM attestations,
  and signs it with [cosign](https://github.com/sigstore/cosign) (keyless, via the workflow's OIDC identity).
- Repository secrets required: `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN`.
- Actions are pinned to commit SHAs; `.github/dependabot.yml` keeps them, Maven dependencies and base images
  up to date.

## Contributing

Commit subjects follow Conventional Commits

```bash
git config core.hooksPath .githooks
```

## Layout

```
src/main/java/dev/frankfurter/mcp/
  client/   typed RestClient wrapper over the Frankfurter v2 API
  tools/    @McpTool methods exposed to agents
```

Upstream API errors (for example `invalid currency: XXX`) are returned to the agent as MCP tool
errors with the API's message, not as protocol failures.

## License

This project is licensed under the **GNU General Public License v3.0 or later** (`GPL-3.0-or-later`); see
[`LICENSE`](LICENSE) for the full text.
