import api from './axios';
import type { LoginRequest, LoginResponse, RegisterRequest, UserProfile } from '../types/auth';

export const login = (data: LoginRequest) =>
  api.post<LoginResponse>('/api/auth/login', data);

export const register = (data: RegisterRequest) =>
  api.post('/api/auth/register', data);

export const getMe = () =>
  api.get<UserProfile>('/api/auth/users/me');
