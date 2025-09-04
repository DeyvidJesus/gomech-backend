# Integração com IA Aprimorada - GoMech

## Visão Geral

A aplicação GoMech agora suporta dois tipos de IA:

1. **IA Padrão**: Spring AI com OpenAI GPT-4o-mini
2. **IA Aprimorada**: Serviço Python com LangChain + LlamaIndex + RAG (Retrieval Augmented Generation)

## Funcionalidades da IA Aprimorada

### Principais Vantagens

- **RAG (Retrieval Augmented Generation)**: Melhora a contextualização das respostas
- **Vector Store com PGVector**: Armazenamento e busca semântica eficiente
- **Geração de Gráficos**: Visualizações dinâmicas com Matplotlib
- **Fallback Automático**: Se o serviço Python não estiver disponível, usa a IA padrão

### Tecnologias Utilizadas

- **LangChain**: Framework para desenvolvimento de aplicações com LLM
- **LlamaIndex**: Indexação e recuperação de documentos
- **PGVector**: Extensão PostgreSQL para busca vetorial
- **OpenAI GPT-4o-mini**: Modelo de linguagem
- **Matplotlib**: Geração de gráficos

## Como Usar

### 1. Configuração do Serviço Python

Primeiro, configure o serviço Python:

```bash
# 1. Instale as dependências
pip install -r requirements.txt

# 2. Configure as variáveis de ambiente (.env)
OPENAI_API_KEY=sua_chave_openai
DB_URL=postgresql+psycopg2://user:password@host:port/database

# 3. Execute o serviço
uvicorn app:app --host 0.0.0.0 --port 8000
```

### 2. Configuração da Aplicação Java

Configure as variáveis de ambiente ou application.properties:

```properties
# URL do serviço Python (padrão: http://localhost:8000)
PYTHON_AI_SERVICE_URL=http://localhost:8000

# Habilitar IA aprimorada (padrão: true)
ENHANCED_AI_ENABLED=true
```

### 3. Endpoints Disponíveis

#### POST `/ai/chat` - Chat com IA

**Request Body:**
```json
{
  "prompt": "Sua pergunta aqui",
  "includeChart": true,
  "useEnhancedAi": true
}
```

**Parâmetros:**
- `prompt` (obrigatório): A pergunta ou comando
- `includeChart` (opcional): Se deve incluir gráfico na resposta (padrão: false)
- `useEnhancedAi` (opcional): Se deve usar IA aprimorada (padrão: false)

**Response:**
```json
{
  "content": "Resposta da IA",
  "status": "success",
  "chart": "base64_encoded_image_or_null",
  "aiType": "enhanced",
  "processingTime": 1500
}
```

#### GET `/ai/chat/status` - Status dos Serviços de IA

**Response:**
```json
{
  "standardAiAvailable": true,
  "enhancedAiAvailable": true,
  "enhancedAiStatus": "available",
  "recommendation": "Enhanced AI is available with RAG capabilities and charts"
}
```

## Exemplos de Uso

### 1. Pergunta Simples com IA Padrão
```bash
curl -X POST http://localhost:5080/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Quantos clientes temos cadastrados?",
    "useEnhancedAi": false
  }'
```

### 2. Pergunta Complexa com IA Aprimorada e Gráfico
```bash
curl -X POST http://localhost:5080/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Mostre a evolução das vendas nos últimos 6 meses",
    "includeChart": true,
    "useEnhancedAi": true
  }'
```

### 3. Verificar Status dos Serviços
```bash
curl -X GET http://localhost:5080/ai/chat/status
```

## Arquitetura da Solução

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │───▶│   GoMech API    │───▶│  Python AI      │
│                 │    │   (Spring Boot) │    │  (FastAPI)      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │                        │
                              ▼                        ▼
                       ┌─────────────────┐    ┌─────────────────┐
                       │   PostgreSQL    │    │   PGVector      │
                       │   (Dados)       │    │   (Vetores)     │
                       └─────────────────┘    └─────────────────┘
```

## Fluxo de Funcionamento

1. **Request chega no ChatController**
2. **Verifica se deve usar IA aprimorada**
3. **Se sim:**
   - Verifica se serviço Python está disponível
   - Faz chamada HTTP para o serviço Python
   - Retorna resposta com possível gráfico
4. **Se não ou se houve erro:**
   - Usa IA padrão (Spring AI + OpenAI)
   - Retorna resposta simples

## Fallback e Resiliência

- **Verificação de Saúde**: Antes de usar a IA aprimorada, verifica se o serviço está disponível
- **Fallback Automático**: Em caso de erro, automaticamente usa a IA padrão
- **Timeout Configurável**: Timeout de 2 minutos para perguntas complexas
- **Error Handling**: Tratamento robusto de erros com mensagens informativas

## Monitoramento

- **Processing Time**: Cada resposta inclui o tempo de processamento
- **AI Type**: Indica qual tipo de IA foi usado na resposta
- **Status Endpoint**: Endpoint dedicado para verificar saúde dos serviços

## Próximos Passos

1. **Implementar caching** para respostas frequentes
2. **Adicionar métricas** de uso e performance
3. **Implementar rate limiting** para proteger os serviços
4. **Adicionar mais tipos de gráficos** (pie charts, line charts, etc.)
5. **Implementar autenticação** entre os serviços 