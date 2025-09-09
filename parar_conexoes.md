# 🛑 GUIA COMPLETO: Como Parar Todas as Conexões

## 1. 🔥 PARAR APLICAÇÃO JAVA (MAIS EFETIVO)

### No Windows (CMD ou PowerShell):
```cmd
# Listar processos Java
jps

# Ou buscar por nome
tasklist | findstr java

# Matar processo específico (substitua PID pelo número encontrado)
taskkill /F /PID [PID_NUMERO]

# Ou matar todos os processos Java
taskkill /F /IM java.exe
```

### No Git Bash (Linux-style):
```bash
# Encontrar processos Java
ps aux | grep java

# Matar processo específico
kill -9 [PID]

# Ou matar todos os Java
pkill -f java
```

### No IntelliJ/IDE:
- Clique no botão "Stop" (quadrado vermelho) na aba de execução
- Ou pressione `Ctrl+F2`

## 2. 🔌 PARAR CONEXÕES NO BANCO SUPABASE

### Via Dashboard Supabase:
1. Acesse: https://supabase.com/dashboard
2. Selecione seu projeto
3. Vá em "Settings" → "Database"
4. Role até "Connection pooling"
5. Clique em "Reset all connections"

### Via SQL (se tiver acesso direto):
```sql
-- Ver conexões ativas
SELECT pid, usename, application_name, client_addr, state 
FROM pg_stat_activity 
WHERE state = 'active';

-- Matar conexões específicas do Gomech
SELECT pg_terminate_backend(pid) 
FROM pg_stat_activity 
WHERE application_name LIKE '%Gomech%' OR application_name LIKE '%HikariCP%';

-- Matar TODAS as conexões (cuidado!)
SELECT pg_terminate_backend(pid) 
FROM pg_stat_activity 
WHERE pid <> pg_backend_pid();
```

## 3. 🔄 SEQUÊNCIA RECOMENDADA

### Passo a Passo:
```bash
# 1. Parar aplicação Java
taskkill /F /IM java.exe

# 2. Aguardar 30 segundos
timeout 30

# 3. Verificar se ainda há processos
jps

# 4. Se necessário, matar processos restantes
# (repetir comando acima)

# 5. Aguardar mais 60 segundos para timeout no banco
timeout 60

# 6. Restart a aplicação
mvn spring-boot:run
```

## 4. 🚨 PREVENÇÃO FUTURA

### Configurações já aplicadas no application.properties:
- `maximum-pool-size=5` - Máximo 5 conexões
- `leak-detection-threshold=60000` - Detecta vazamentos
- `idle-timeout=300000` - Fecha conexões ociosas em 5min

### Boas práticas:
1. **Sempre pare a aplicação antes de restart**
2. **Use apenas uma instância por vez em desenvolvimento**
3. **Monitore conexões no dashboard Supabase**
4. **Se desenvolver com múltiplas IDEs, pare todas antes de rodar**

## 5. 🔍 MONITORAMENTO

### Verificar conexões ativas:
```bash
# No terminal da aplicação, procure por logs tipo:
# "HikariPool-1 - Start completed"
# "HikariPool-1 - Pool stats"
```

### No Supabase Dashboard:
- "Settings" → "Database" → "Connection info"
- Monitore "Active connections" vs "Max connections"

## 6. 📞 SCRIPT DE EMERGÊNCIA

Salve este comando para usar quando necessário:

### Windows (CMD):
```cmd
@echo off
echo Parando todas as conexoes Java...
taskkill /F /IM java.exe
echo Aguardando 60 segundos...
timeout 60
echo Pronto para reiniciar!
```

### Git Bash:
```bash
#!/bin/bash
echo "Parando todas as conexões Java..."
pkill -f java
echo "Aguardando 60 segundos..."
sleep 60
echo "Pronto para reiniciar!"
```

---

## ⚡ COMANDO RÁPIDO
Para uso imediato, execute no terminal:

**Windows:**
```cmd
taskkill /F /IM java.exe && timeout 60
```

**Git Bash:**
```bash
pkill -f java && sleep 60
```

Depois restart sua aplicação normalmente!
