package com.puetsnao.heatmap.infrastructure.observability;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.info.GitProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnBean(GitProperties.class)
public class GitInfoContributor implements InfoContributor {

    private final GitProperties gitProperties;

    public GitInfoContributor(GitProperties gitProperties) {
        this.gitProperties = gitProperties;
    }

    @Override
    public void contribute(Info.Builder builder) {
        var commitId = gitProperties.getCommitId();
        if (commitId != null && !commitId.isBlank()) {
            builder.withDetail("commit", Map.of("id", commitId));
        }
    }
}
