package com.github.jensim.megamanipulator.actions.search

import kotlinx.serialization.Serializable

@SuppressWarnings("ConstructorParameterNaming")
object SearchTypes {

    @Serializable
    data class GraphQlRequest(val variables: SearchVaraibles) {
        val query = graphQLSearch
    }

    @Serializable
    data class SearchVaraibles(val query: String) {
        val version = "V2"
        val patternType = "literal"
    }

    @Serializable
    data class GraphQLResponse(val data: GraphQLData?, val errors: List<GraphQLError>?)
    @Serializable
    data class GraphQLData(val search: GraphQLSearch)
    @Serializable
    data class GraphQLError(val message: String?, val location: GraphQLErrorLocation?)
    @Serializable
    data class GraphQLErrorLocation(val line: Long?, val column: Long?)
    @Serializable
    data class GraphQLSearch(val results: GraphQLSearchResults)
    @Serializable
    data class GraphQLSearchResults(
        val __typename: String,
        val pageInfo: PageInfo,
        val limitHit: Boolean,
        val matchCount: Long?,
        val approximateResultCount: String?,
        val missing: List<Missing>?,
        val repositoriesCount: Long?,
        val timedout: List<Timeout>?,
        val alert: List<Alert>?,
        val elapsedMilliseconds: Long?,
        val results: List<GraphQLSearchResult>?,
    )

    @Serializable
    data class Missing(val name: String?)
    @Serializable
    data class Timeout(val name: String?)
    @Serializable
    data class Alert(
        val title: String?,
        val description: String?,
        val proposedQueries: List<ProposedQueries>?,
    )

    @Serializable
    data class ProposedQueries(
        val description: String?,
        val query: String?,
    )

    @Serializable
    data class PageInfo(
        val endCursor: String?,
        val hasNextPage: Boolean,
    )

    @Serializable
    data class GraphQLSearchResult(
        val __typename: String?,
        val file: File?,
        val repository: Repository?,
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
