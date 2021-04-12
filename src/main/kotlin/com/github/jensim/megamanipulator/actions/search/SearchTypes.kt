package com.github.jensim.megamanipulator.actions.search

import kotlinx.serialization.Serializable

@SuppressWarnings("ConstructorParameterNaming")
object SearchTypes {

    @Serializable
    data class GraphQlRequest(val variables: SearchVaraibles) {
        val query = graphQLSearch
    }

    @Serializable
    data class SearchVaraibles(
        val query: String,
    ) {
        val version = "V2"
        val patternType = "literal"
    }

    @Serializable
    data class GraphQLResponse(
        val data: GraphQLData? = null,
        val errors: List<GraphQLError>? = null,
    )

    @Serializable
    data class GraphQLData(
        val search: GraphQLSearch,
    )

    @Serializable
    data class GraphQLError(
        val message: String? = null,
        val location: GraphQLErrorLocation? = null,
    )

    @Serializable
    data class GraphQLErrorLocation(
        val line: Long? = null,
        val column: Long? = null,
    )

    @Serializable
    data class GraphQLSearch(val results: GraphQLSearchResults)

    @Serializable
    data class GraphQLSearchResults(
        val __typename: String,
        val pageInfo: PageInfo,
        val limitHit: Boolean,
        val matchCount: Long? = null,
        val approximateResultCount: String? = null,
        val missing: List<Missing>? = null,
        val repositoriesCount: Long? = null,
        val timedout: List<Timeout>? = null,
        val alert: List<Alert>? = null,
        val elapsedMilliseconds: Long? = null,
        val results: List<GraphQLSearchResult>? = null,
    )

    @Serializable
    data class Missing(
        val name: String? = null,
    )

    @Serializable
    data class Timeout(
        val name: String? = null,
    )

    @Serializable
    data class Alert(
        val title: String? = null,
        val description: String? = null,
        val proposedQueries: List<ProposedQueries>? = null,
    )

    @Serializable
    data class ProposedQueries(
        val description: String? = null,
        val query: String? = null,
    )

    @Serializable
    data class PageInfo(
        val endCursor: String? = null,
        val hasNextPage: Boolean,
    )

    @Serializable
    data class GraphQLSearchResult(
        val __typename: String? = null,
        val file: File? = null,
        val repository: Repository? = null,
    )

    @Serializable
    data class File(
        val path: String?
    )

    @Serializable
    data class Repository(
        val name: String?
    )

    private val graphQLSearch = """query Search(
    ${"$"}query          : String!
    ${"$"}version        : SearchVersion!
    ${"$"}patternType    : SearchPatternType!
    ${"$"}versionContext : String
) {
    search(
        query:          ${"$"}query
        version:        ${"$"}version
        patternType:    ${"$"}patternType
        versionContext: ${"$"}versionContext
    ) {
        results {
            __typename
            ... on SearchResults {
                pageInfo {
                    endCursor
                    hasNextPage
                }
            }
            limitHit
            matchCount
            approximateResultCount
            missing {
                name
            }
            repositoriesCount
            timedout {
                name
            }
            alert {
                title
                description
                proposedQueries {
                    description
                    query
                }
            }
            elapsedMilliseconds
            results {
                __typename
                ... on FileMatch {
                    file {
                        path
                    }
                    repository {
                        name
                    }
                }
                
            }
            
        }
    }
}
    """.trimIndent()
}
