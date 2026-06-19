import http from './http';

export interface LoginRequest {
  tenantCode: string;
  username: string;
  password: string;
}

export interface LoginResult {
  token: string;
  tenantCode: string;
  userId: number;
  username: string;
  role: string;
  realName: string;
}

export function login(data: LoginRequest): Promise<LoginResult> {
  return http.post('/api/public/login', data);
}
