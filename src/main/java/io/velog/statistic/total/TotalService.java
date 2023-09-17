package io.velog.statistic.total;

import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Service
@Slf4j
public class TotalService {
    private final WebClient webClient;

    public TotalService(WebClient webClient) {
        this.webClient = webClient;
    }

    public static final String USER_TAGS_QUERY = """
            query UserTags($username: String) {
              userTags(username: $username) {
                tags {
                  id
                  name
                  description
                  posts_count
                  thumbnail
                  __typename
                }
                posts_count
                __typename
              }
            }
            """;
    public static final String POSTS_QUERY = """
            query Posts($cursor: ID, $username: String, $temp_only: Boolean, $tag: String, $limit: Int) {
              posts(cursor: $cursor, username: $username, temp_only: $temp_only, tag: $tag, limit: $limit) {
                id
                title
                short_description
                thumbnail
                user {
                  id
                  username
                  profile {
                    id
                    thumbnail
                    __typename
                  }
                  __typename
                }
                url_slug
                released_at
                updated_at
                comments_count
                tags
                is_private
                likes
                __typename
              }
            }
            """;
    public static final String STATS_QUERY = """
            query GetStats($post_id: ID!) {
              getStats(post_id: $post_id) {
                total
                count_by_day {
                  count
                  day
                  __typename
                }
                __typename
              }
            }
            """;

    public Mono<Integer> getTotal(TotalRequest request) {
        String username = request.username();
        String cookie = request.cookie();
        HttpGraphQlClient graphQlClient = HttpGraphQlClient.create(webClient.mutate().defaultHeader("Cookie", cookie).build());

        return getUserTags(graphQlClient, username)
                .flatMap(tag -> getPostsForTag(graphQlClient, username, tag))
                .collectList()
                .map(posts -> posts.stream().distinct().collect(Collectors.toList()))
                .flatMapMany(Flux::fromIterable)
                .flatMap(post -> getTotalResponseForPostId(graphQlClient, post))
                .reduce(Integer::sum);
    }

    // User Tags
    private Flux<Tag> getUserTags(HttpGraphQlClient graphQlClient, String username) {
        return graphQlClient
                .document(USER_TAGS_QUERY)
                .operationName("UserTags")
                .variable("username", username)
                .retrieve("userTags.tags")
                .toEntityList(Tag.class)
                .flatMapMany(Flux::fromIterable);
    }

    // Posts for Tag
    private Flux<Post> getPostsForTag(HttpGraphQlClient graphQlClient, String username, Tag tag) {
        return graphQlClient
                .document(POSTS_QUERY)
                .operationName("Posts")
                .variable("username", username)
                .variable("tag", tag.name())
                .retrieve("posts")
                .toEntityList(Post.class)
                .flatMapMany(Flux::fromIterable);
    }

    // Total Response for Post Id
    private Mono<Integer> getTotalResponseForPostId(HttpGraphQlClient graphQlClient, Post post) {
        return graphQlClient
                .document(STATS_QUERY)
                .operationName("GetStats")
                .variable("post_id", post.id())
                .retrieve("getStats")
                .toEntity(TotalResponse.class)
                .map(TotalResponse::total);
    }
}
