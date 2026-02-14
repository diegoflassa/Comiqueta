# Protocolos: Comiqueta
[Voltar ao Índice](./INDEX.md)

Regras de conduta operacional para o Agente de IA neste projeto.

## 📄 Gerenciamento de Arquivos Grandes (>500 linhas)

Para evitar poluição de contexto e otimizar a análise de arquivos extensos, o Agente segue o **Protocolo de Três Camadas**:

### 1. Camada de Mapa (Outline-First)
- **Ação**: Nunca ler o arquivo completo de primeira.
- **Ferramenta**: `view_file_outline`.
- **Objetivo**: Mapear classes, métodos e intervalos de linhas.

### 2. Camada de Foco (Surgical Reading)
- **Ação**: Ler apenas os blocos identificados na Camada 1.
- **Ferramenta**: `view_file` com `StartLine` e `EndLine`.
- **Objetivo**: Analisar a lógica de forma isolada e precisa.

### 3. Camada de Memória (Knowledge Items)
- **Ação**: Atualizar ou criar KIs (Knowledge Items).
- **Ferramenta**: Criação de documentos markdown estruturados.
- **Objetivo**: Manter um "resumo executivo" da lógica para futuras consultas sem necessidade de re-leitura do código fonte.

---
Status: **Ativo** (Via GEMINI.md global)
Última Atualização: 2026-02-08
