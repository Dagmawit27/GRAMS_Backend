const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8081/api";

async function parseResponse(res: Response) {
  const text = await res.text();
  try {
    return text ? JSON.parse(text) : {};
  } catch {
    return {};
  }
}

export async function registerUser(data: {
  faydaId: string;
  firstName: string;
  middleName?: string;
  lastName: string;
  gender: string;
  dob: string;
  phone: string;
  email: string;
  username: string;
  password: string;
}): Promise<{ token: string; username: string }> {
  const res = await fetch(`${BASE_URL}/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
  const json = await parseResponse(res);
  if (!res.ok) throw new Error(json.message || "Registration failed.");
  return json;
}

export async function loginUser(data: {
  username: string;
  password: string;
}): Promise<{ token: string; username: string }> {
  const res = await fetch(`${BASE_URL}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
  const json = await parseResponse(res);
  if (!res.ok) throw new Error(json.message || "Invalid username or password.");
  return json;
}
