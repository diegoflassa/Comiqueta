@file:Suppress("UnusedFlow")

package dev.diegoflassa.comiqueta.domain.usecase

import android.net.Uri
import androidx.core.net.toUri
import androidx.paging.PagingData
import com.google.common.truth.Truth
import dev.diegoflassa.comiqueta.core.domain.model.Comic
import dev.diegoflassa.comiqueta.core.data.repository.IComicsRepository
import dev.diegoflassa.comiqueta.core.data.enums.ComicFlags
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.ArgumentMatchers.anyString
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class GetPaginatedComicsUseCaseTest {

    @Mock
    private lateinit var mockComicsRepository: IComicsRepository

    private lateinit var getPaginatedComicsUseCase: IGetPaginatedComicsUseCase

    private lateinit var mockedUri: MockedStatic<Uri>

    @Before
    fun setUp() {
        mockedUri = mockStatic(Uri::class.java, Mockito.RETURNS_DEEP_STUBS)
        mockedUri.`when`<Uri> { Uri.parse(anyString()) }.thenReturn(mock(Uri::class.java))
        getPaginatedComicsUseCase = GetPaginatedComicsUseCase(mockComicsRepository)
    }

    @After
    fun tearDown() {
        mockedUri.close()
    }

    @Test
    fun `invoke with categoryId and flags should call repository and return its flow`() = runTest {
        // Arrange
        val categoryId = 1L
        val flags = setOf(ComicFlags.FAVORITE)
        // For this params, searchQuery is null (default in PaginatedComicsParams)
        val params = PaginatedComicsParams(categoryId = categoryId, flags = flags)
        val comicInstance = Comic(
            filePath = "test/path1.cbz".toUri(),
            title = "Test Comic 1",
            coverPath = mock(Uri::class.java)
        )
        val expectedPagingData = PagingData.from(listOf(comicInstance))
        val expectedFlow = flowOf(expectedPagingData)

        whenever(
            mockComicsRepository.getComicsPaginated(
                eq(categoryId),// 1. categoryId
                eq(flags),          // 2. flags
                anyInt(),               // 3. pageSize (use case doesn't specify, so repo uses default)
                isNull()             // 4. searchQuery (null from params default)
            )
        ).thenReturn(expectedFlow)

        // Act
        val resultFlow = getPaginatedComicsUseCase(params)

        // Assert
        Truth.assertThat(resultFlow.first()).isEqualTo(expectedPagingData)
        verify(mockComicsRepository).getComicsPaginated(
            eq(categoryId),
            eq(flags),
            anyInt(),
            isNull()
        )
    }

    @Test
    fun `invoke with null categoryId and empty flags should call repository`() = runTest {
        // Arrange
        // For this params, searchQuery is null (default in PaginatedComicsParams)
        val params = PaginatedComicsParams(categoryId = null, flags = emptySet())
        val comicInstance = Comic(
            filePath = "test/path2.cbz".toUri(),
            title = "Test Comic 2",
            coverPath = mock(Uri::class.java)
        )
        val expectedPagingData = PagingData.from(listOf(comicInstance))
        val expectedFlow = flowOf(expectedPagingData)

        whenever(
            mockComicsRepository.getComicsPaginated(
                isNull(),
                eq(emptySet()),
                anyInt(),
                isNull()
            )
        ).thenReturn(expectedFlow)

        // Act
        val resultFlow = getPaginatedComicsUseCase(params)

        // Assert
        Truth.assertThat(resultFlow.first()).isEqualTo(expectedPagingData)
        verify(mockComicsRepository).getComicsPaginated(
            isNull(),
            eq(emptySet()),
            anyInt(),
            isNull()
        )
    }

    @Test
    fun `invoke with only flags should call repository`() = runTest {
        // Arrange
        val flags = setOf(ComicFlags.NEW, ComicFlags.READ)
        // For this params, categoryId is null and searchQuery is null (defaults in PaginatedComicsParams)
        val params = PaginatedComicsParams(flags = flags)
        val comicInstance = Comic(
            filePath = "test/path3.cbz".toUri(),
            title = "Test Comic 3",
            coverPath = mock(Uri::class.java)
        )
        val expectedPagingData = PagingData.from(listOf(comicInstance))
        val expectedFlow = flowOf(expectedPagingData)

        whenever(
            mockComicsRepository.getComicsPaginated(
                isNull(),
                eq(flags),
                anyInt(),
                isNull()
            )
        ).thenReturn(expectedFlow)

        // Act
        val resultFlow = getPaginatedComicsUseCase(params)

        // Assert
        Truth.assertThat(resultFlow.first()).isEqualTo(expectedPagingData)
        verify(mockComicsRepository).getComicsPaginated(
            isNull(),
            eq(flags),
            anyInt(),
            isNull()
        )
    }
}
