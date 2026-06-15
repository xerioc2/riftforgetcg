package com.riftforge.rest;

import com.riftforge.RiftforgeServerApplication;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
  private final ObjectProvider<BuildProperties> buildProperties;

  public HealthController(ObjectProvider<BuildProperties> buildProperties) {
    this.buildProperties = buildProperties;
  }

  @GetMapping("/api/health")
  public Map<String, String> health() {
    Map<String, String> payload = new LinkedHashMap<>();
    payload.put("status", "ok");
    BuildProperties build = buildProperties.getIfAvailable();
    if (build != null) {
      payload.put("serverVersion", build.getVersion());
      payload.put("serverBuildTime", build.getTime().toString());
      payload.put("serverGitSha", valueOrLocal(build.get("gitSha")));
      payload.put("serverFullGitSha", valueOrLocal(build.get("fullGitSha")));
      payload.put("serverBuildTimestamp", valueOrLocal(build.get("buildTimestamp")));
      payload.put("serverReleaseTag", valueOrLocal(build.get("releaseTag")));
    } else {
      payload.put("serverVersion", "local-dev");
      payload.put("serverBuildTime", "local-dev");
      payload.put("serverGitSha", "local-dev");
      payload.put("serverFullGitSha", "local-dev");
      payload.put("serverBuildTimestamp", "local-dev");
      payload.put("serverReleaseTag", "local-dev");
    }
    payload.put("serverJarSha256", runtimeJarSha256());
    return payload;
  }

  private String valueOrLocal(String value) {
    return value == null || value.isBlank() || value.startsWith("${") ? "local-dev" : value;
  }

  private String runtimeJarSha256() {
    try {
      URI location = RiftforgeServerApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI();
      Path path = Path.of(location);
      if (!Files.isRegularFile(path) || !path.getFileName().toString().endsWith(".jar")) {
        return "local-dev";
      }
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (var input = Files.newInputStream(path)) {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
          digest.update(buffer, 0, read);
        }
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (Exception ignored) {
      return "unavailable";
    }
  }
}
