@file:Suppress("UnusedFlow")

package dev.diegoflassa.comiqueta.domain.usecase

import android.net.Uri
import androidx.core.net.toUri
import androidx.paging.PagingData
import com.google.common.truth.Truth
import dev.diegoflassa.comiqueta.core.data.model.Comic
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
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.ArgumentMatchers.anyString
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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
        // Mock Uri static methods. RETURNS_DEEP_STUBS ensures static methods like Uri.parse()
        // return a mock Uri. It does NOT make the static field Uri.EMPTY non-null.
        mockedUri = mockStatic(Uri::class.java, Mockito.RETURNS_DEEP_STUBS)

        // Ensure Uri.parse(anyString()) returns a simple mock for .toUri() calls.
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
        val params = GetPaginatedComicsParams(categoryId = categoryId, flags = flags)
        val comicInstance = Comic(
            filePath = "test/path1.cbz".toUri(),
            title = "Test Comic 1",
            coverPath = mock(Uri::class.java) 
        )
        val expectedPagingData = PagingData.from(listOf(comicInstance))
        val expectedFlow = flowOf(expectedPagingData)

        // Assuming this might be a 2-arg or 3-arg call depending on use case logic
        // If it becomes a 3-arg call, matchers would be: eq(categoryId), eq(flags), any()
        whenever(
            mockComicsRepository.getComicsPaginated(
                categoryId,
                flags
            )
        ).thenReturn(expectedFlow)

        // Act
        val resultFlow = getPaginatedComicsUseCase(params)

        // Assert
        Truth.assertThat(resultFlow.first()).isEqualTo(expectedPagingData)
        verify(mockComicsRepository).getComicsPaginated(categoryId, flags /* If 3-arg: , any() */)
    }

    @Test
    fun `invoke with null categoryId and empty flags should call repository`() = runTest {
        // Arrange
        val params =
            GetPaginatedComicsParams(categoryId = null, flags = emptySet()) 
        val comicInstance = Comic(
            filePath = "test/path2.cbz".toUri(),
            title = "Test Comic 2",
            coverPath = mock(Uri::class.java) 
        )
        val expectedPagingData = PagingData.from(listOf(comicInstance))
        val expectedFlow = flowOf(expectedPagingData)

        // Assuming this is a 3-arg call due to previous errors, use explicit matchers
        whenever(
            mockComicsRepository.getComicsPaginated(
                isNull(), 
                eq(emptySet()),
                any()
            )
        ).thenReturn(expectedFlow) 

        // Act
        val resultFlow = getPaginatedComicsUseCase(params)

        // Assert
        Truth.assertThat(resultFlow.first()).isEqualTo(expectedPagingData)
        verify(mockComicsRepository).getComicsPaginated(isNull(), eq(emptySet()), any())
    }

    @Test
    fun `invoke with only flags should call repository`() = runTest {
        // Arrange
        val flags = setOf(ComicFlags.NEW, ComicFlags.READ)
        val params = GetPaginatedComicsParams(flags = flags)
        val comicInstance = Comic(
            filePath = "test/path3.cbz".toUri(),
            title = "Test Comic 3",
            coverPath = mock(Uri::class.java) 
        )
        val expectedPagingData = PagingData.from(listOf(comicInstance))
        val expectedFlow = flowOf(expectedPagingData)

        // Using anyOrNull<Long>() for the first argument, eq(flags) for the second, any() for the third
        whenever(mockComicsRepository.getComicsPaginated(anyOrNull<Long>(), eq(flags), any())).thenReturn(expectedFlow)

        // Act
        val resultFlow = getPaginatedComicsUseCase(params)

        // Assert
        Truth.assertThat(resultFlow.first()).isEqualTo(expectedPagingData)
        // Using explicit matchers for verification too
        verify(mockComicsRepository).getComicsPaginated(anyOrNull<Long>(), eq(flags), any())
    }
}
