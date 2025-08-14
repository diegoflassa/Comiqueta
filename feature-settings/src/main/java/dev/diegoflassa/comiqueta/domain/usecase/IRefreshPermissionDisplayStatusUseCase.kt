package dev.diegoflassa.comiqueta.domain.usecase

import android.app.Activity
import dev.diegoflassa.comiqueta.data.model.PermissionDisplayStatus

/**
 * Interface para o caso de uso de atualizar o status de exibição das permissões.
 * Define o contrato público do caso de uso.
 */
interface IRefreshPermissionDisplayStatusUseCase {
    /**
     * Retorna um mapa do nome da permissão para seu [PermissionDisplayStatus] correspondente.
     *
     * @param activity A Activity atual, usada para verificar o status da permissão.
     * @return Um mapa onde a chave é o nome da permissão (String) e o valor é o [PermissionDisplayStatus].
     */
    operator fun invoke(activity: Activity): Map<String, PermissionDisplayStatus>
}