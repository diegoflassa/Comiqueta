package dev.diegoflassa.comiqueta.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.comiqueta.core.domain.model.Category
import dev.diegoflassa.comiqueta.core.domain.repository.ICategoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * The use case is a pass-through onto [ICategoryRepository]. What is worth pinning is exactly that:
 * it must hand back the repository's own flow without filtering, sorting or re-wrapping it, because
 * the Home screen relies on the repository's ordering.
 *
 * This suite previously mocked `CategoryEntity`, the Room row. The contract has been the domain
 * [Category] for some time, so it no longer compiled — see CORE_RULES §6 on stale artefacts.
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class LoadCategoriesUseCaseTest {

    @Mock
    private lateinit var mockCategoryRepository: ICategoryRepository

    private lateinit var loadCategoriesUseCase: ILoadCategoriesUseCase

    @Before
    fun setUp() {
        loadCategoriesUseCase = LoadCategoriesUseCase(mockCategoryRepository)
    }

    @Test
    fun `invoke returns the repository categories untouched and in order`() = runTest {
        val expectedCategories = listOf(
            Category(id = 1L, name = "DC Comics", createdAt = 1_000L),
            Category(id = 2L, name = "Marvel", createdAt = 2_000L)
        )
        whenever(mockCategoryRepository.getAllCategories()).thenReturn(flowOf(expectedCategories))

        val result = loadCategoriesUseCase().first()

        // Order matters: the screen renders the chips in emission order, so an accidental sort here
        // would silently reshuffle the filter row.
        assertThat(result).containsExactlyElementsIn(expectedCategories).inOrder()
        verify(mockCategoryRepository).getAllCategories()
    }

    @Test
    fun `invoke propagates an empty list rather than swallowing the emission`() = runTest {
        whenever(mockCategoryRepository.getAllCategories()).thenReturn(flowOf(emptyList()))

        val result = loadCategoriesUseCase().first()

        // "No categories yet" and "categories never loaded" render differently, so the empty
        // emission has to survive the use case.
        assertThat(result).isEmpty()
        verify(mockCategoryRepository).getAllCategories()
    }
}
