"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { loginCitizen, saveSession } from "@/lib/api";

export default function CitizenLoginPage() {
  const router = useRouter();
  const [form, setForm] = useState({ loginIdentifier: "", password: "" });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState("");
  const [loading, setLoading] = useState(false);

  function validate() {
    const e: Record<string, string> = {};
    if (!form.loginIdentifier.trim()) e.loginIdentifier = "Email, Phone, or Fayda ID is required.";
    if (!form.password) e.password = "Password is required.";
    return e;
  }

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    setForm({ ...form, [e.target.name]: e.target.value });
    setErrors({ ...errors, [e.target.name]: "" });
    setServerError("");
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length > 0) { setErrors(errs); return; }

    setLoading(true);
    try {
      const result = await loginCitizen(form);
      saveSession(result);
      router.push("/citizen/dashboard");
    } catch (err: unknown) {
      setServerError(err instanceof Error ? err.message : "Login failed. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-50 to-gray-100 px-4">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-md overflow-hidden">
        {/* Header */}
        <div className="bg-gradient-to-r from-green-600 to-green-500 px-8 py-6">
          <h1 className="text-2xl font-bold text-white">GRAMS Citizen Portal</h1>
          <p className="text-green-50 text-sm mt-1">Sign in to access your rental services</p>
        </div>

        <form onSubmit={handleSubmit} noValidate className="px-8 py-8 space-y-5">
          {serverError && (
            <div className="bg-red-50 border-l-4 border-red-500 text-red-700 px-4 py-3 rounded text-sm">
              {serverError}
            </div>
          )}

          <div className="space-y-1.5">
            <label className="text-sm font-semibold text-gray-700">
              Email / Phone / Fayda ID <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              name="loginIdentifier"
              value={form.loginIdentifier}
              onChange={handleChange}
              placeholder="Enter email, phone, or Fayda ID"
              className={`w-full border rounded-lg px-4 py-3 text-sm text-gray-800 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-green-500 transition ${
                errors.loginIdentifier ? "border-red-400 bg-red-50" : "border-gray-300 bg-white"
              }`}
            />
            {errors.loginIdentifier && <p className="text-xs text-red-600">{errors.loginIdentifier}</p>}
          </div>

          <div className="space-y-1.5">
            <label className="text-sm font-semibold text-gray-700">
              Password <span className="text-red-500">*</span>
            </label>
            <input
              type="password"
              name="password"
              value={form.password}
              onChange={handleChange}
              placeholder="Enter your password"
              className={`w-full border rounded-lg px-4 py-3 text-sm text-gray-800 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-green-500 transition ${
                errors.password ? "border-red-400 bg-red-50" : "border-gray-300 bg-white"
              }`}
            />
            {errors.password && <p className="text-xs text-red-600">{errors.password}</p>}
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full mt-6 py-3 bg-gradient-to-r from-green-600 to-green-500 text-white font-bold text-sm tracking-wide uppercase rounded-lg hover:from-green-700 hover:to-green-600 transition-all disabled:opacity-50 disabled:cursor-not-allowed shadow-lg"
          >
            {loading ? "Signing in..." : "Sign In"}
          </button>

          <p className="text-center text-sm text-gray-600 mt-6">
            Don&apos;t have an account?{" "}
            <Link href="/citizen/register" className="text-green-600 font-semibold hover:underline">
              Register as Citizen
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
}
