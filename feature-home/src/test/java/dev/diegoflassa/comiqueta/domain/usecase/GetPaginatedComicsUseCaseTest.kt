package dev.diegoflassa.comiqueta.domain.usecase

import androidx.paging.PagingData
import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.comiqueta.core.data.enums.ComicFlags
import dev.diegoflassa.comiqueta.core.data.repository.IComicsRepository
import dev.diegoflassa.comiqueta.core.domain.model.Comic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * The use case exists to translate [PaginatedComicsParams] into a repository call. The thing that
 * can silently break is the mapping of each field, so every test pins the full argument list.
 *
 * The suite used to mock `Uri` statically because [Comic] carried `Uri` paths. It carries plain
 * `String` paths now, so the static mocking is gone along with the reason for it.
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class GetPaginatedComicsUseCaseTest {

    @Mock
    private lateinit var mockComicsRepository: IComicsRepository

    private lateinit var getPaginatedComicsUseCase: IGetPaginatedComicsUseCase

    @Before
    fun setUp() {
        getPaginatedComicsUseCase = GetPaginatedComicsUseCase(mockComicsRepository)
    }

    private fun comic(path: String) = Comic(
        filePath = path,
        title = path.substringAfterLast('/'),
        coverPath = "$path.cover"
    )

    @Test
    fun `a category and flags are both forwarded, and page size stays the repository default`() =
        runTest {
            val flags = setOf(ComicFlags.FAVORITE)
            val expected = PagingData.from(listOf(comic("test/path1.cbz")))
            whenever(
                mockComicsRepository.getComicsPaginated(
                    eq(1L),
                    eq(flags),
                    eq(IComicsRepository.DEFAULT_PAGE_SIZE),
                    isNull()
                )
            ).thenReturn(flowOf(expected))

            val result =
                getPaginatedComicsUseCase(PaginatedComicsParams(categoryId = 1L, flags = flags))

            assertThat(result.first()).isSameInstanceAs(expected)
            // The use case deliberately does not choose a page size; pinning the default here is
            // what catches someone hard-coding one at this layer.
            verify(mockComicsRepository).getComicsPaginated(
                eq(1L),
                eq(flags),
                eq(IComicsRepository.DEFAULT_PAGE_SIZE),
                isNull()
            )
        }

    @Test
    fun `a null category stays null instead of collapsing to a real id`() = runTest {
        val expected = PagingData.from(listOf(comic("test/path2.cbz")))
        whenever(
            mockComicsRepository.getComicsPaginated(
                isNull(),
                eq(emptySet()),
                eq(IComicsRepository.DEFAULT_PAGE_SIZE),
                isNull()
            )
        ).thenReturn(flowOf(expected))

        val result = getPaginatedComicsUseCase(
            PaginatedComicsParams(categoryId = null, flags = emptySet())
        )

        // null means "every category". Turning it into 0 would quietly show an empty library.
        assertThat(result.first()).isSameInstanceAs(expected)
        verify(mockComicsRepository).getComicsPaginated(
            isNull(),
            eq(emptySet()),
            eq(IComicsRepository.DEFAULT_PAGE_SIZE),
            isNull()
        )
    }

    @Test
    fun `a search query is forwarded alongside the flags it must combine with`() = runTest {
        val flags = setOf(ComicFlags.NEW, ComicFlags.READ)
        val expected = PagingData.from(listOf(comic("test/path3.cbz")))
        whenever(
            mockComicsRepository.getComicsPaginated(
                isNull(),
                eq(flags),
                eq(IComicsRepository.DEFAULT_PAGE_SIZE),
                eq("batman")
            )
        ).thenReturn(flowOf(expected))

        val result = getPaginatedComicsUseCase(
            PaginatedComicsParams(flags = flags, searchQuery = "batman")
        )

        // Search and flags have to reach the repository together; dropping either one turns a
        // filtered search into a full-library listing.
        assertThat(result.first()).isSameInstanceAs(expected)
        verify(mockComicsRepository).getComicsPaginated(
            isNull(),
            eq(flags),
            eq(IComicsRepository.DEFAULT_PAGE_SIZE),
            eq("batman")
        )
    }
}
