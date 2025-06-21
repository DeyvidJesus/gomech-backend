# Guia de Implementação de Roles no Frontend - Sistema Gomech

Este documento descreve como implementar o sistema de roles no frontend, integrando com o backend que já possui controle de acesso baseado em roles.

## 📋 Visão Geral do Sistema de Roles

### Roles Disponíveis:
- **USER**: Apenas visualização de dados (somente leitura)
- **ADMIN**: Acesso completo (criar, editar, deletar e visualizar)

### Usuários Padrão:
- **admin@gomech.com** / **admin123** (ADMIN)
- **user@gomech.com** / **user123** (USER)

## 🔐 Autenticação e Token JWT

### 1. Login (POST /api/auth/login)

**Request:**
```json
{
  "email": "admin@gomech.com",
  "password": "admin123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "admin@gomech.com",
  "role": "ADMIN",
  "userId": 1
}
```

### 2. Token JWT
O token contém as seguintes informações:
- `sub`: Email do usuário
- `role`: Nome da role (USER/ADMIN)
- `authorities`: Authorities do Spring Security (ROLE_USER/ROLE_ADMIN)
- `userId`: ID do usuário
- `exp`: Data de expiração (2 horas)

## 🛡️ Implementação no Frontend

### 1. Gerenciamento de Estado (Context/Redux)

```typescript
// AuthContext.tsx
interface AuthState {
  isAuthenticated: boolean;
  user: {
    id: number;
    email: string;
    role: 'USER' | 'ADMIN';
  } | null;
  token: string | null;
}

const AuthContext = createContext<AuthState | null>(null);
```

### 2. Interceptador HTTP (Axios)

```typescript
// api.ts
import axios from 'axios';

const api = axios.create({
  baseURL: '/api'
});

// Interceptador para adicionar token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Interceptador para tratar erros 403 (Forbidden)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 403) {
      alert('Você não tem permissão para realizar esta ação');
    }
    return Promise.reject(error);
  }
);
```

### 3. Hook de Autorização

```typescript
// useAuth.ts
import { useContext } from 'react';
import { AuthContext } from './AuthContext';

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth deve ser usado dentro de AuthProvider');
  }
  return context;
};

export const useRole = () => {
  const { user } = useAuth();
  
  const isAdmin = () => user?.role === 'ADMIN';
  const isUser = () => user?.role === 'USER';
  const canCreate = () => isAdmin();
  const canEdit = () => isAdmin();
  const canDelete = () => isAdmin();
  const canView = () => isAdmin() || isUser();
  
  return {
    isAdmin,
    isUser,
    canCreate,
    canEdit,
    canDelete,
    canView
  };
};
```

### 4. Componente de Proteção de Rota

```typescript
// ProtectedRoute.tsx
interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRole?: 'USER' | 'ADMIN';
  fallback?: React.ReactNode;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  requiredRole,
  fallback = <div>Acesso negado</div>
}) => {
  const { user, isAuthenticated } = useAuth();
  
  if (!isAuthenticated) {
    return <Navigate to="/login" />;
  }
  
  if (requiredRole && user?.role !== requiredRole && user?.role !== 'ADMIN') {
    return fallback;
  }
  
  return <>{children}</>;
};
```

### 5. Componente Condicional por Role

```typescript
// RoleGuard.tsx
interface RoleGuardProps {
  children: React.ReactNode;
  roles: ('USER' | 'ADMIN')[];
  fallback?: React.ReactNode;
}

export const RoleGuard: React.FC<RoleGuardProps> = ({
  children,
  roles,
  fallback = null
}) => {
  const { user } = useAuth();
  
  const hasAccess = user && (
    roles.includes(user.role) || 
    user.role === 'ADMIN' // ADMIN sempre tem acesso
  );
  
  return hasAccess ? <>{children}</> : fallback;
};
```

## 🎨 Interface do Usuário

### 1. Componente de Lista com Controle de Acesso

```typescript
// ClientList.tsx
export const ClientList = () => {
  const { canCreate, canEdit, canDelete } = useRole();
  const [clients, setClients] = useState([]);
  
  return (
    <div>
      <div className="header">
        <h1>Clientes</h1>
        <RoleGuard roles={['ADMIN']}>
          <Button onClick={handleCreate}>
            Novo Cliente
          </Button>
        </RoleGuard>
      </div>
      
      <Table>
        {clients.map(client => (
          <TableRow key={client.id}>
            <TableCell>{client.name}</TableCell>
            <TableCell>
              <Button onClick={() => handleView(client.id)}>
                Ver
              </Button>
              
              <RoleGuard roles={['ADMIN']}>
                <Button onClick={() => handleEdit(client.id)}>
                  Editar
                </Button>
                <Button 
                  onClick={() => handleDelete(client.id)}
                  variant="danger"
                >
                  Excluir
                </Button>
              </RoleGuard>
            </TableCell>
          </TableRow>
        ))}
      </Table>
    </div>
  );
};
```

### 2. Menu de Navegação com Base em Roles

```typescript
// Navigation.tsx
export const Navigation = () => {
  const { user } = useAuth();
  
  return (
    <nav>
      <NavLink to="/dashboard">Dashboard</NavLink>
      <NavLink to="/clients">Clientes</NavLink>
      <NavLink to="/vehicles">Veículos</NavLink>
      
      <RoleGuard roles={['ADMIN']}>
        <NavLink to="/admin">Administração</NavLink>
        <NavLink to="/users">Usuários</NavLink>
      </RoleGuard>
      
      <div className="user-info">
        {user?.email} ({user?.role})
      </div>
    </nav>
  );
};
```

## 🔄 Rotas e Permissões

### Configuração de Rotas

```typescript
// App.tsx
function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Rotas públicas */}
        <Route path="/login" element={<Login />} />
        
        {/* Rotas protegidas */}
        <Route path="/" element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }>
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="clients" element={<ClientList />} />
          <Route path="vehicles" element={<VehicleList />} />
          
          {/* Rotas apenas para ADMIN */}
          <Route path="admin/*" element={
            <ProtectedRoute requiredRole="ADMIN">
              <AdminRoutes />
            </ProtectedRoute>
          } />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
```

## 📊 Tratamento de Erros

### 1. Interceptador de Respostas HTTP

```typescript
// Tratar erros 403 (Forbidden)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 403) {
      // Exibir mensagem de erro
      toast.error('Você não tem permissão para realizar esta ação');
      
      // Ou redirecionar
      // navigate('/unauthorized');
    }
    
    if (error.response?.status === 401) {
      // Token expirado ou inválido
      localStorage.removeItem('token');
      navigate('/login');
    }
    
    return Promise.reject(error);
  }
);
```

### 2. Feedback Visual

```typescript
// Componente de botão com loading
export const ActionButton = ({ onClick, children, disabled }) => {
  const [loading, setLoading] = useState(false);
  
  const handleClick = async () => {
    try {
      setLoading(true);
      await onClick();
    } catch (error) {
      if (error.response?.status === 403) {
        toast.error('Ação não permitida para seu perfil');
      }
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <Button onClick={handleClick} disabled={disabled || loading}>
      {loading ? 'Carregando...' : children}
    </Button>
  );
};
```

## 🧪 Testes

### Teste de Componente com Roles

```typescript
// ClientList.test.tsx
describe('ClientList', () => {
  it('deve mostrar botão criar apenas para ADMIN', () => {
    const mockUser = { id: 1, email: 'admin@test.com', role: 'ADMIN' };
    
    render(
      <AuthContext.Provider value={{ user: mockUser, isAuthenticated: true }}>
        <ClientList />
      </AuthContext.Provider>
    );
    
    expect(screen.getByText('Novo Cliente')).toBeInTheDocument();
  });
  
  it('não deve mostrar botão criar para USER', () => {
    const mockUser = { id: 1, email: 'user@test.com', role: 'USER' };
    
    render(
      <AuthContext.Provider value={{ user: mockUser, isAuthenticated: true }}>
        <ClientList />
      </AuthContext.Provider>
    );
    
    expect(screen.queryByText('Novo Cliente')).not.toBeInTheDocument();
  });
});
```

## 📝 Checklist de Implementação

### ✅ Backend (Já implementado)
- [x] Modelo Role e User
- [x] Autenticação JWT com informações de role
- [x] Configuração de segurança com controle de acesso
- [x] Endpoints protegidos por role

### ⏳ Frontend (A implementar)
- [ ] Context/Provider de autenticação
- [ ] Interceptadores HTTP
- [ ] Hooks de autorização (useAuth, useRole)
- [ ] Componentes de proteção (ProtectedRoute, RoleGuard)
- [ ] Interface condicional baseada em roles
- [ ] Tratamento de erros 403/401
- [ ] Testes unitários
- [ ] Documentação das telas

## 🚀 Próximos Passos

1. **Implementar Context de Autenticação**
2. **Configurar interceptadores HTTP**
3. **Criar componentes de proteção**
4. **Atualizar interfaces existentes**
5. **Implementar tratamento de erros**
6. **Escrever testes**
7. **Testar com usuários de diferentes roles**

## 📞 Suporte

Para dúvidas sobre a implementação, consulte:
- Código backend no diretório `src/main/java/com/gomech/`
- Scripts SQL em `setup_roles.sql`
- Esta documentação completa

---

**Desenvolvido para o Sistema Gomech** 🔧 