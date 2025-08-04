package dev.diegoflassa.comiqueta.viewer.domain.usecase

import android.net.Uri
import dev.diegoflassa.comiqueta.viewer.model.ComicInfo

/**
 * Interface para o caso de uso de obter informações detalhadas sobre um arquivo de quadrinho.
 * Define o contrato público do caso de uso.
 */
interface IGetComicInfoUseCase {
    /**
     * Retorna informações sobre um arquivo de quadrinho, como título, contagem de páginas e tipo de arquivo.
     *
     * @param uri A URI do arquivo do quadrinho.
     * @return Um objeto [ComicInfo] contendo os detalhes do quadrinho.
     * @throws IOException se o arquivo não puder ser acessado ou o tipo de arquivo não for suportado.
     */
    suspend operator fun invoke(uri: Uri): ComicInfo
}