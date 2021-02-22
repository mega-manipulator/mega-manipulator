package com.github.jensim.megamanipulatior.actions.search

@SuppressWarnings("ConstructorParameterNaming")
object SearchTypes {

    data class GraphQlRequest(val variables: SearchVaraibles) {
        val query = graphQLSearch
    }

    data class SearchVaraibles(val query: String) {
        val version = "V2"
        val patternType = "literal"
    }

    data class GraphQLResponse(val data: GraphQLData?, val errors: List<GraphQLError>?)
    data class GraphQLData(val search: GraphQLSearch)
    data class GraphQLError(val message: String?, val location: GraphQLErrorLocation?)
    data class GraphQLErrorLocation(val line: Long?, val column: Long?)
    data class GraphQLSearch(val results: GraphQLSearchResults)
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

    data class Missing(val name: String?)
    data class Timeout(val name: String?)
    data class Alert(
        val title: String?,
        val description: String?,
        val proposedQueries: List<ProposedQueries>?,
    )

    data class ProposedQueries(
        val description: String?,
        val query: String?,
    )

    data class PageInfo(
        val endCursor: String?,
        val hasNextPage: Boolean,
    )

    data class GraphQLSearchResult(
        val __typename: String?,
        val file: File?,
        val repository: Repository?,
    )

    data class File(
        val path: String?
    )

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
