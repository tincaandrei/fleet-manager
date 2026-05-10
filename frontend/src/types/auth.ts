export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  username: string;
  role: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  phone: string;
  address: string;
}

export interface UserProfile {
  userId: number;
  username: string;
  email: string;
  phone: string;
  address: string;
  role: string;
}
