package dev.diegoflassa.comiqueta.core.domain.usecase.permission

/**
 * Interface para o caso de uso de obter as permissões de OS relevantes
 * com base na versão do SDK do Android.
 * Define o contrato público do caso de uso.
 */
interface IGetRelevantOsPermissionsUseCase {
    /**
     * Retorna uma lista de permissões de OS relevantes.
     *
     * @return Uma lista de strings representando as permissões necessárias.
     */
    operator fun invoke(): List<String>
}