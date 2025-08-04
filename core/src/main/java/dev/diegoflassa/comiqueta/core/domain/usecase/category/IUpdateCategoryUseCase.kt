package dev.diegoflassa.comiqueta.core.domain.usecase.category

import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity

/**
 * Interface para o caso de uso de atualizar uma categoria.
 * Define o contrato público do caso de uso.
 */
interface IUpdateCategoryUseCase {
    /**
     * Atualiza uma categoria existente.
     *
     * @param category A entidade da categoria a ser atualizada.
     * @throws IllegalArgumentException se o nome da categoria estiver em branco.
     */
    suspend operator fun invoke(category: CategoryEntity)
}