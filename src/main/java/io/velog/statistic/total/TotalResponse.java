package io.velog.statistic.total;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TotalResponse(Integer total) {
}
