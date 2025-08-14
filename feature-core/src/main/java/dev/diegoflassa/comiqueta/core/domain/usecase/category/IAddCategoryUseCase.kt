package dev.diegoflassa.comiqueta.core.domain.usecase.category

/**
 * Interface para o caso de uso de adicionar uma nova categoria.
 * Define o contrato público do caso de uso.
 */
interface IAddCategoryUseCase {
    /**
     * Adiciona uma nova categoria com o nome fornecido.
     *
     * @param categoryName O nome da categoria a ser adicionada.
     * @return O ID da linha da nova categoria inserida, ou -1 se a inserção falhar.
     * @throws IllegalArgumentException se o nome da categoria estiver em branco.
     */
    suspend operator fun invoke(categoryName: String): Long
}