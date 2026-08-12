const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8081/api/v1";

async function parseResponse(res: Response) {
  const text = await res.text();
  try {
    return text ? JSON.parse(text) : {};
  } catch {
    return {};
  }
}

export interface UserSummary {
  id: string;
  firstName: string;
  middleName?: string;
  lastName: string;
  gender: string;
  dateOfBirth?: string;
  phoneNumber: string;
  email: string;
  createdAt: string;
  roles: string[];
  userType: "CITIZEN" | "GOVERNMENT_EMPLOYEE";
  governmentEmployee: boolean;
  employeeNumber?: string;
  positionTitle?: string;
}

export interface AuthResult {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserSummary;
}

export async function registerCitizen(data: {
  faydaId: number;
  firstName: string;
  middleName?: string;
  lastName: string;
  gender: "MALE" | "FEMALE";
  dateOfBirth: string;
  phoneNumber: string;
  email: string;
  password: string;
  rolePreference?: string;
}): Promise<AuthResult> {
  const res = await fetch(`${BASE_URL}/auth/register/citizen`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
  const json = await parseResponse(res);
  if (!res.ok) throw new Error(json.message || "Registration failed.");
  return json;
}

export async function loginCitizen(data: {
  loginIdentifier: string;
  password: string;
}): Promise<AuthResult> {
  const res = await fetch(`${BASE_URL}/auth/login/citizen`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
  const json = await parseResponse(res);
  if (!res.ok) throw new Error(json.message || "Invalid credentials.");
  return json;
}

export async function getMe(token: string): Promise<UserSummary> {
  const res = await fetch(`${BASE_URL}/users/me`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const json = await parseResponse(res);
  if (!res.ok) throw new Error("Session expired.");
  return json;
}

export function saveSession(result: AuthResult) {
  localStorage.setItem("accessToken", result.accessToken);
  localStorage.setItem("user", JSON.stringify(result.user));
}

export function clearSession() {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("user");
}

export function getSession(): { token: string; user: UserSummary } | null {
  const token = localStorage.getItem("accessToken");
  const raw = localStorage.getItem("user");
  if (!token || !raw) return null;
  try {
    return { token, user: JSON.parse(raw) };
  } catch {
    return null;
  }
}

// ── Property ─────────────────────────────────────────────────────────────────

export type PropertyStatus =
  | "PENDING"
  | "VERIFIED"
  | "REJECTED"
  | "LISTED"
  | "RENTED"
  | "UNLISTED";

export interface AddressRequest {
  city: string;
  subCity: string;
  woreda: string;
  kebele?: string;
  street?: string;
  houseNumber?: string;
  latitude?: number;
  longitude?: number;
}

export interface PropertyRequest {
  propertyType: string;
  address: AddressRequest;
  houseNumber?: string;
  floorNumber?: string;
  bedroomCount?: number;
  bathroomCount?: number;
  areaSqMeter?: number;
  monthlyRent: number;
  furnishingStatus?: string;
  description?: string;
}

export interface PropertyResponse {
  id: string;
  propertyCode: string;
  propertyType: string;
  address: {
    id: string;
    city: string;
    subCity: string;
    woreda: string;
    kebele?: string;
    street?: string;
    houseNumber?: string;
    latitude?: number;
    longitude?: number;
  };
  houseNumber?: string;
  floorNumber?: string;
  bedroomCount?: number;
  bathroomCount?: number;
  areaSqMeter?: number;
  monthlyRent: number;
  furnishingStatus?: string;
  description?: string;
  status: PropertyStatus;
  landlordId: string;
  images: { id: string; imageUrl: string; isCover: boolean; uploadedAt: string }[];
  ownershipDocuments: {
    id: string;
    documentNumber: string;
    documentType: string;
    filePath: string;
    issueDate?: string;
    expiryDate?: string;
  }[];
  createdAt: string;
}

export async function registerProperty(
  token: string,
  data: PropertyRequest,
  images?: File[],
  documents?: File[]
): Promise<PropertyResponse> {
  const form = new FormData();
  form.append(
    "property",
    new Blob([JSON.stringify(data)], { type: "application/json" })
  );
  images?.forEach((f) => form.append("images", f));
  documents?.forEach((f) => form.append("documents", f));

  const res = await fetch(`${BASE_URL}/properties`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: form,
  });
  const json = await parseResponse(res);
  if (!res.ok) throw new Error(json.message || "Failed to register property.");
  return json;
}

export async function getMyProperties(
  token: string
): Promise<PropertyResponse[]> {
  const res = await fetch(`${BASE_URL}/properties/my`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const json = await parseResponse(res);
  if (!res.ok) throw new Error(json.message || "Failed to load properties.");
  return json;
}

export async function getPropertiesByStatus(
  status: PropertyStatus = "LISTED",
  token?: string
): Promise<PropertyResponse[]> {
  const res = await fetch(`${BASE_URL}/properties?status=${status}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  const json = await parseResponse(res);
  if (!res.ok) throw new Error(json.message || "Failed to load properties.");
  return json;
}

export async function updatePropertyStatus(
  token: string,
  id: string,
  status: PropertyStatus,
  remarks?: string
): Promise<PropertyResponse> {
  const res = await fetch(`${BASE_URL}/properties/${id}/status`, {
    method: "PATCH",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ status, remarks }),
  });
  const json = await parseResponse(res);
  if (!res.ok) throw new Error(json.message || "Failed to update status.");
  return json;
}
