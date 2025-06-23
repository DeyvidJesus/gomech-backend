# Como Reativar a Segurança - Sistema Gomech

## 🚨 ATENÇÃO: Segurança Temporariamente Desabilitada

A segurança foi **temporariamente desabilitada** para fins de teste. 

**NÃO COLOQUE EM PRODUÇÃO** com essa configuração!

## 🔧 Para Reativar a Segurança:

### 1. SecurityConfiguration.java

Vá para `src/main/java/com/gomech/configuration/SecurityConfiguration.java`

**Substitua:**
```java
.authorizeHttpRequests(auth -> auth
    // TEMPORÁRIO: Desabilitando segurança para testes
    .anyRequest().permitAll()
    
    // CONFIGURAÇÃO ORIGINAL COMENTADA PARA REATIVAR DEPOIS:
    /*
    // ... código comentado ...
    */
);
// COMENTANDO O FILTRO TEMPORARIAMENTE
// .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class);
```

**Por:**
```java
.authorizeHttpRequests(auth -> auth
    // Rotas públicas
    .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
    .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
    
    // Rotas que requerem autenticação - GET (visualização) para USER e ADMIN
    .requestMatchers(HttpMethod.GET, "/api/clients/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
    .requestMatchers(HttpMethod.GET, "/api/vehicles/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
    
    // Rotas que requerem ADMIN - operações de escrita
    .requestMatchers(HttpMethod.POST, "/api/clients/**").hasAuthority("ROLE_ADMIN")
    .requestMatchers(HttpMethod.PUT, "/api/clients/**").hasAuthority("ROLE_ADMIN")
    .requestMatchers(HttpMethod.DELETE, "/api/clients/**").hasAuthority("ROLE_ADMIN")
    
    .requestMatchers(HttpMethod.POST, "/api/vehicles/**").hasAuthority("ROLE_ADMIN")
    .requestMatchers(HttpMethod.PUT, "/api/vehicles/**").hasAuthority("ROLE_ADMIN")
    .requestMatchers(HttpMethod.DELETE, "/api/vehicles/**").hasAuthority("ROLE_ADMIN")
    
    .anyRequest().authenticated()
)
.addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class);
```

### 2. application.properties (Opcional)

Para ativar logs de debug novamente:

**Descomente:**
```properties
logging.level.org.springframework.security=DEBUG
logging.level.com.gomech=DEBUG
```

## 🧪 Estado Atual de Teste:

### ✅ O que funciona sem autenticação:
- ✅ Todos os endpoints de clientes: GET, POST, PUT, DELETE `/api/clients/**`
- ✅ Todos os endpoints de veículos: GET, POST, PUT, DELETE `/api/vehicles/**`
- ✅ Endpoints de autenticação: POST `/api/auth/login`, `/api/auth/register`

### 📋 Endpoints disponíveis para teste:

```bash
# Clientes
GET    http://localhost:5080/api/clients
POST   http://localhost:5080/api/clients
GET    http://localhost:5080/api/clients/{id}
PUT    http://localhost:5080/api/clients/{id}
DELETE http://localhost:5080/api/clients/{id}

# Veículos
GET    http://localhost:5080/api/vehicles
POST   http://localhost:5080/api/vehicles
GET    http://localhost:5080/api/vehicles/{id}
PUT    http://localhost:5080/api/vehicles/{id}
DELETE http://localhost:5080/api/vehicles/{id}

# Autenticação (ainda funciona)
POST   http://localhost:5080/api/auth/login
POST   http://localhost:5080/api/auth/register
```

## 🔐 Usuários Disponíveis:

Mesmo com segurança desabilitada, os usuários ainda existem no banco:

- **admin@gomech.com** / **admin123** (ADMIN)
- **user@gomech.com** / **user123** (USER)

## ⚠️ Lembrete Importante:

**REMOVA ESTE ARQUIVO** antes de fazer deploy em produção!

---

**Data de desabilitação**: $(date)
**Desenvolvido para testes do Sistema Gomech** 🔧 